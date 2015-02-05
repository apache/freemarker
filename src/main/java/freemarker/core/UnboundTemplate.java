/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Template.WrongEncodingException;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.NullArgumentException;

/**
 * The parsed representation of a template that's not yet bound to the {@link Template} properties that doesn't
 * influence the result of the parsing. This information wasn't separated from {@link Template} in FreeMarker 2.3.x,
 * and was factored out from it into thus class in 2.4.0, to allow more efficient caching.
 * 
 * @since 2.4.0
 */
public class UnboundTemplate {

    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String NO_NS_PREFIX = "N";

    /**
     * This is only non-null during parsing. It's used internally to make some information available through the
     * Template API-s earlier than the parsing was finished.
     */
    private transient FMParser parser;

    private final String sourceName;
    private final Configuration cfg;
    
    /** Attributes added via {@code <#ftl attributes=...>}. */
    private HashMap<String, Object> customAttributes;
    
    private Map macros = new HashMap();
    private List imports = new Vector();
    private TemplateElement rootElement;
    private String defaultNS;
    private int actualTagSyntax;
    private final Version templateLanguageVersion;
    
    private final ArrayList lines = new ArrayList();
    
    private Map prefixToNamespaceURILookup = new HashMap();
    private Map namespaceURIToPrefixLookup = new HashMap();

    private UnboundTemplate(String sourceName, Configuration cfg) {
        this.sourceName = sourceName;
        
        NullArgumentException.check(cfg);
        this.cfg = cfg;
        
        this.templateLanguageVersion = normalizeTemplateLanguageVersion(cfg.getIncompatibleImprovements());
    }
    
    /**
     * @param reader
     *            Reads the template source code
     * @param cfg
     *            The FreeMarker configuration settings; some of them influences parsing, also the resulting
     *            {@link UnboundTemplate} will be bound to this.
     * @param assumedEncoding
     *            This is the name of the charset that we are supposed to be using. This is only needed to check if the
     *            encoding specified in the {@code #ftl} header (if any) matches this. If this is non-{@code null} and
     *            they don't match, a {@link WrongEncodingException} will be thrown by the parser.
     * @param sourceName
     *            Shown in error messages as the template "file" location.
     */
    public UnboundTemplate(Reader reader, String sourceName, Configuration cfg, String assumedEncoding)
            throws IOException {
        this(sourceName, cfg);

        try {
            if (!(reader instanceof BufferedReader)) {
                reader = new BufferedReader(reader, 0x1000);
            }
            reader = new LineTableBuilder(reader);

            try {
                parser = new FMParser(this,
                        reader, assumedEncoding,
                        cfg.getStrictSyntaxMode(),
                        cfg.getWhitespaceStripping(),
                        cfg.getTagSyntax(),
                        cfg.getIncompatibleImprovements().intValue());
                this.rootElement = parser.Root();
                this.actualTagSyntax = parser._getLastTagSyntax();
            } catch (TokenMgrError exc) {
                // TokenMgrError VS ParseException is not an interesting difference for the user, so we just convert it
                // to ParseException
                throw exc.toParseException(this);
            } finally {
                parser = null;
            }
        } catch (ParseException e) {
            e.setTemplateName(getSourceName());
            throw e;
        } finally {
            reader.close();
        }

        namespaceURIToPrefixLookup = Collections.unmodifiableMap(namespaceURIToPrefixLookup);
        prefixToNamespaceURILookup = Collections.unmodifiableMap(prefixToNamespaceURILookup);
    }
    
    private static Version normalizeTemplateLanguageVersion(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        int v = incompatibleImprovements.intValue();
        if (v < _TemplateAPI.VERSION_INT_2_3_19) {
            return Configuration.VERSION_2_3_0;
        } else if (v > _TemplateAPI.VERSION_INT_2_3_21) {
            return Configuration.VERSION_2_3_21;
        } else { // if 2.3.19 or 2.3.20 or 2.3.21
            return incompatibleImprovements;
        }
    }
    
    static public UnboundTemplate createPlainTextTemplate(String sourceName, String content, Configuration config) {
        UnboundTemplate unboundTemplate = new UnboundTemplate(sourceName, config);
        unboundTemplate.rootElement = new TextBlock(content);
        unboundTemplate.actualTagSyntax = config.getTagSyntax();
        return unboundTemplate;
    }

    /**
     * Returns a string representing the raw template text in canonical form.
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            dump(sw);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
        return sw.toString();
    }

    /**
     * The name that was actually used to load this template from the {@link TemplateLoader} (or from other custom
     * storage mechanism). This is what should be shown in error messages as the error location.
     * 
     * @see Template#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Return the template language (FTL) version used by this template. For now (2.3.21) this is the same as
     * {@link Configuration#getIncompatibleImprovements()}, except that it's normalized to the lowest version where the
     * template language was changed.
     */
    public Version getTemplateLanguageVersion() {
        return templateLanguageVersion;
    }

    /**
     * Returns the tag syntax the parser has chosen for this template. If the syntax could be determined, it's
     * {@link Configuration#SQUARE_BRACKET_TAG_SYNTAX} or {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}. If the syntax
     * couldn't be determined (like because there was no tags in the template, or it was a plain text template), this
     * returns whatever the default is in the current configuration, so it's maybe
     * {@link Configuration#AUTO_DETECT_TAG_SYNTAX}.
     */
    public int getActualTagSyntax() {
        return actualTagSyntax;
    }
    
    public Configuration getConfiguration() {
        return cfg;
    }

    /**
     * Dump the raw template in canonical form.
     */
    public void dump(PrintStream ps) {
        ps.print(rootElement.getCanonicalForm());
    }

    /**
     * Dump the raw template in canonical form.
     */
    public void dump(Writer out) throws IOException {
        out.write(rootElement.getCanonicalForm());
    }

    /**
     * Called by code internally to maintain a table of macros
     */
    void addMacro(Macro macro) {
        macros.put(macro.getName(), macro);
    }

    /**
     * Called by code internally to maintain a list of imports
     */
    void addImport(LibraryLoad ll) {
        imports.add(ll);
    }

    /**
     * Returns the template source at the location specified by the coordinates given, or {@code null} if unavailable.
     * 
     * @param beginColumn
     *            the first column of the requested source, 1-based
     * @param beginLine
     *            the first line of the requested source, 1-based
     * @param endColumn
     *            the last column of the requested source, 1-based
     * @param endLine
     *            the last line of the requested source, 1-based
     * @see freemarker.core.TemplateObject#getSource()
     */
    public String getSource(int beginColumn,
            int beginLine,
            int endColumn,
            int endLine)
    {
        if (beginLine < 1 || endLine < 1) return null; // dynamically ?eval-ed expressions has no source available

        // Our container is zero-based.
        --beginLine;
        --beginColumn;
        --endColumn;
        --endLine;
        StringBuffer buf = new StringBuffer();
        for (int i = beginLine; i <= endLine; i++) {
            if (i < lines.size()) {
                buf.append(lines.get(i));
            }
        }
        int lastLineLength = lines.get(endLine).toString().length();
        int trailingCharsToDelete = lastLineLength - endColumn - 1;
        buf.delete(0, beginColumn);
        buf.delete(buf.length() - trailingCharsToDelete, buf.length());
        return buf.toString();
    }

    /**
     * This is a helper class that builds up the line table info for us.
     */
    private class LineTableBuilder extends FilterReader {

        StringBuffer lineBuf = new StringBuffer();
        int lastChar;

        /**
         * @param r
         *            the character stream to wrap
         */
        LineTableBuilder(Reader r) {
            super(r);
        }

        public int read() throws IOException {
            int c = in.read();
            handleChar(c);
            return c;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            int numchars = in.read(cbuf, off, len);
            for (int i = off; i < off + numchars; i++) {
                char c = cbuf[i];
                handleChar(c);
            }
            return numchars;
        }

        public void close() throws IOException {
            if (lineBuf.length() > 0) {
                lines.add(lineBuf.toString());
                lineBuf.setLength(0);
            }
            super.close();
        }

        private void handleChar(int c) {
            if (c == '\n' || c == '\r') {
                if (lastChar == '\r' && c == '\n') { // CRLF under Windoze
                    int lastIndex = lines.size() - 1;
                    String lastLine = (String) lines.get(lastIndex);
                    lines.set(lastIndex, lastLine + '\n');
                } else {
                    lineBuf.append((char) c);
                    lines.add(lineBuf.toString());
                    lineBuf.setLength(0);
                }
            }
            else if (c == '\t') {
                int numSpaces = 8 - (lineBuf.length() % 8);
                for (int i = 0; i < numSpaces; i++) {
                    lineBuf.append(' ');
                }
            }
            else {
                lineBuf.append((char) c);
            }
            lastChar = c;
        }
    }

    /**
     * Used internally by the parser.
     */
    void setCustomAttribute(String key, Object value) {
        HashMap<String, Object> attrs = customAttributes;
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
            customAttributes = attrs;
        }
        attrs.put(key, value);
    }

    Object getCustomAttribute(String name) {
        HashMap<String, Object> attrs = customAttributes;
        return attrs != null ? attrs.get(name) : null;
    }

    Set<String> getCustomAttributeNames() {
        HashMap<String, Object> attrs = customAttributes;
        return attrs != null ? attrs.keySet() : Collections.<String>emptySet();
    }
    
    /**
     * @return the root TemplateElement object.
     */
    public TemplateElement getRootTreeNode() {
        return rootElement;
    }

    public Map getMacros() {
        return macros;
    }

    public List getImports() {
        return imports;
    }

    /**
     * This is used internally.
     */
    public void addPrefixNSMapping(String prefix, String nsURI) {
        if (nsURI.length() == 0) {
            throw new IllegalArgumentException("Cannot map empty string URI");
        }
        if (prefix.length() == 0) {
            throw new IllegalArgumentException("Cannot map empty string prefix");
        }
        if (prefix.equals(NO_NS_PREFIX)) {
            throw new IllegalArgumentException("The prefix: " + prefix
                    + " cannot be registered, it's reserved for special internal use.");
        }
        if (prefixToNamespaceURILookup.containsKey(prefix)) {
            throw new IllegalArgumentException("The prefix: '" + prefix + "' was repeated. This is illegal.");
        }
        if (namespaceURIToPrefixLookup.containsKey(nsURI)) {
            throw new IllegalArgumentException("The namespace URI: " + nsURI
                    + " cannot be mapped to 2 different prefixes.");
        }
        if (prefix.equals(DEFAULT_NAMESPACE_PREFIX)) {
            this.defaultNS = nsURI;
        } else {
            prefixToNamespaceURILookup.put(prefix, nsURI);
            namespaceURIToPrefixLookup.put(nsURI, prefix);
        }
    }

    public String getDefaultNS() {
        return this.defaultNS;
    }

    /**
     * @return the NamespaceUri mapped to this prefix in this template. (Or null if there is none.)
     */
    public String getNamespaceForPrefix(String prefix) {
        if (prefix.equals("")) {
            return defaultNS == null ? "" : defaultNS;
        }
        return (String) prefixToNamespaceURILookup.get(prefix);
    }

    /**
     * @return the prefix mapped to this nsURI in this template. (Or null if there is none.)
     */
    public String getPrefixForNamespace(String nsURI) {
        if (nsURI == null) {
            return null;
        }
        if (nsURI.length() == 0) {
            return defaultNS == null ? "" : NO_NS_PREFIX;
        }
        if (nsURI.equals(defaultNS)) {
            return "";
        }
        return (String) namespaceURIToPrefixLookup.get(nsURI);
    }

    /**
     * @return the prefixed name, based on the ns_prefixes defined in this template's header for the local name and node
     *         namespace passed in as parameters.
     */
    public String getPrefixedName(String localName, String nsURI) {
        if (nsURI == null || nsURI.length() == 0) {
            if (defaultNS != null) {
                return NO_NS_PREFIX + ":" + localName;
            } else {
                return localName;
            }
        }
        if (nsURI.equals(defaultNS)) {
            return localName;
        }
        String prefix = getPrefixForNamespace(nsURI);
        if (prefix == null) {
            return null;
        }
        return prefix + ":" + localName;
    }

    /**
     * @return an array of the {@link TemplateElement}s containing the given column and line numbers.
     * @param column
     *            the column
     * @param line
     *            the line
     */
    public List containingElements(int column, int line) {
        final ArrayList elements = new ArrayList();
        TemplateElement element = rootElement;
        mainloop: while (element.contains(column, line)) {
            elements.add(element);
            for (Enumeration enumeration = element.children(); enumeration.hasMoreElements();) {
                TemplateElement elem = (TemplateElement) enumeration.nextElement();
                if (elem.contains(column, line)) {
                    element = elem;
                    continue mainloop;
                }
            }
            break;
        }
        return elements.isEmpty() ? null : elements;
    }

}

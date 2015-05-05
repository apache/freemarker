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
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * and was factored out from it into this class in 2.4.0, to allow more efficient caching.
 * 
 * @since 2.4.0
 */
public final class UnboundTemplate {

    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String NO_NS_PREFIX = "N";

    private final String sourceName;
    private final Configuration cfg;
    private final Version templateLanguageVersion;
    
    /** Attributes added via {@code <#ftl attributes=...>}. */
    private LinkedHashMap<String, Object> customAttributes;
    private final Map<String, UnboundCallable> unboundCallables = new HashMap<String, UnboundCallable>(0);
    // Earlier it was a Vector, so I thought the safest is to keep it synchronized:
    private final List<LibraryLoad> imports = Collections.synchronizedList(new ArrayList<LibraryLoad>(0));
    private final TemplateElement rootElement;
    private String defaultNamespaceURI;
    private final int actualTagSyntax;
    private final int actualNamingConvention;
    
    private final String templateSpecifiedEncoding;
    
    private final ArrayList lines = new ArrayList();
    
    private Map<String, String> prefixToNamespaceURIMapping;
    private Map<String, String> namespaceURIToPrefixMapping;

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
    UnboundTemplate(Reader reader, String sourceName, Configuration cfg, String assumedEncoding)
            throws IOException {
        NullArgumentException.check(cfg);
        this.cfg = cfg;
        this.sourceName = sourceName;
        this.templateLanguageVersion = normalizeTemplateLanguageVersion(cfg.getIncompatibleImprovements());

        LineTableBuilder ltbReader;
        try {
            if (!(reader instanceof BufferedReader)) {
                reader = new BufferedReader(reader, 0x1000);
            }
            ltbReader = new LineTableBuilder(reader);
            reader = ltbReader;

            try {
                FMParser parser = new FMParser(this,
                        reader, assumedEncoding,
                        cfg.getStrictSyntaxMode(),
                        cfg.getWhitespaceStripping(),
                        cfg.getTagSyntax(),
                        cfg.getNamingConvention(),
                        cfg.getIncompatibleImprovements().intValue());
                
                TemplateElement rootElement;
                try {
                    rootElement = parser.Root();
                } catch (IndexOutOfBoundsException exc) {
                    // There's a JavaCC bug where the Reader throws a RuntimeExcepton and then JavaCC fails with
                    // IndexOutOfBoundsException. If that wasn't the case, we just rethrow. Otherwise we suppress the
                    // IndexOutOfBoundsException and let the real cause to be thrown later. 
                    if (!ltbReader.hasFailure()) {
                        throw exc;
                    }
                    rootElement = null;
                }
                this.rootElement = rootElement;
                
                this.actualTagSyntax = parser._getLastTagSyntax();
                this.actualNamingConvention = parser._getLastNamingConvention();
                this.templateSpecifiedEncoding = parser._getTemplateSpecifiedEncoding();
            } catch (TokenMgrError exc) {
                // TokenMgrError VS ParseException is not an interesting difference for the user, so we just convert it
                // to ParseException
                throw exc.toParseException(this);
            }
        } catch (ParseException e) {
            e.setTemplateName(getSourceName());
            throw e;
        } finally {
            reader.close();
        }
        
        // Throws any exception that JavaCC has silently treated as EOF:
        ltbReader.throwFailure();

        if (prefixToNamespaceURIMapping != null) {
            prefixToNamespaceURIMapping = Collections.unmodifiableMap(prefixToNamespaceURIMapping);
            namespaceURIToPrefixMapping = Collections.unmodifiableMap(namespaceURIToPrefixMapping);
        }
    }
    
    /**
     * Creates a plain text (unparsed) template. 
     */
    static UnboundTemplate newPlainTextUnboundTemplate(String content, String sourceName, Configuration cfg) {
        return new UnboundTemplate(content, sourceName, cfg);
    }
    
    /**
     * Creates a plain text (unparsed) template. 
     */
    private UnboundTemplate(String content, String sourceName, Configuration cfg) {
        NullArgumentException.check(cfg);
        this.cfg = cfg;
        this.sourceName = sourceName;
        this.templateLanguageVersion = normalizeTemplateLanguageVersion(cfg.getIncompatibleImprovements());
        this.templateSpecifiedEncoding = null;
        
        rootElement = new TextBlock(content);
        actualTagSyntax = cfg.getTagSyntax();
        actualNamingConvention = cfg.getNamingConvention();
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
     * See {@link Template#getActualTagSyntax()}.
     */
    public int getActualTagSyntax() {
        return actualTagSyntax;
    }
    
    /**
     * See {@link Template#getActualNamingConvention()}.
     */
    public int getActualNamingConvention() {
        return actualNamingConvention;
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
    void addUnboundCallable(UnboundCallable unboundCallable) {
        unboundCallables.put(unboundCallable.getName(), unboundCallable);
    }

    /**
     * Called by code internally to maintain a list of imports
     */
    void addImport(LibraryLoad libLoad) {
        imports.add(libLoad);
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
     * Used internally by the parser.
     */
    void setCustomAttribute(String key, Object value) {
        LinkedHashMap<String, Object> attrs = customAttributes;
        if (attrs == null) {
            attrs = new LinkedHashMap<String, Object>();
            customAttributes = attrs;
        }
        attrs.put(key, value);
    }

    /**
     * Returns the {@link Map} of custom attributes that are normally coming from the {@code #ftl} header, or
     * {@code null} if there was none. The returned {@code Map} must not be modified, and might changes during
     * template parsing as new attributes are added by the parser (i.e., it's not a snapshot).
     */
    Map<String, ?> getCustomAttributes() {
        return this.customAttributes;
    }

    /**
     * @return the root TemplateElement object.
     */
    TemplateElement getRootTreeNode() {
        return rootElement;
    }

    Map<String, UnboundCallable> getUnboundCallables() {
        return unboundCallables;
    }

    List<LibraryLoad> getImports() {
        return imports;
    }

    /**
     * This is used internally.
     */
    void addPrefixToNamespaceURIMapping(String prefix, String nsURI) {
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
        
        if (prefixToNamespaceURIMapping != null) {
            if (prefixToNamespaceURIMapping.containsKey(prefix)) {
                throw new IllegalArgumentException("The prefix: '" + prefix + "' was repeated. This is illegal.");
            }
            if (namespaceURIToPrefixMapping.containsKey(nsURI)) {
                throw new IllegalArgumentException("The namespace URI: " + nsURI
                        + " cannot be mapped to 2 different prefixes.");
            }
        }
        
        if (prefix.equals(DEFAULT_NAMESPACE_PREFIX)) {
            this.defaultNamespaceURI = nsURI;
        } else {
            if (prefixToNamespaceURIMapping == null) {
                prefixToNamespaceURIMapping = new HashMap<String, String>();                
                namespaceURIToPrefixMapping = new HashMap<String, String>();
            }
            prefixToNamespaceURIMapping.put(prefix, nsURI);
            namespaceURIToPrefixMapping.put(nsURI, prefix);
        }
    }

    public String getDefaultNamespaceURI() {
        return this.defaultNamespaceURI;
    }

    /**
     * @return The namespace URI mapped to this node value prefix, or {@code null}.
     */
    public String getNamespaceURIForPrefix(String prefix) {
        if (prefix.equals("")) {
            return defaultNamespaceURI == null ? "" : defaultNamespaceURI;
        }
        
        final Map<String, String> m = prefixToNamespaceURIMapping;
        return m != null ? m.get(prefix) : null;
    }
    
    /**
     * The encoding (charset name) specified by the template itself (as of 2.3.22, via {@code <#ftl encoding=...>}), or
     * {@code null} if none was specified.
     */
    public String getTemplateSpecifiedEncoding() {
        return templateSpecifiedEncoding;
    }

    /**
     * @return the prefix mapped to this nsURI in this template. (Or null if there is none.)
     */
    public String getPrefixForNamespaceURI(String nsURI) {
        if (nsURI == null) {
            return null;
        }
        if (nsURI.length() == 0) {
            return defaultNamespaceURI == null ? "" : NO_NS_PREFIX;
        }
        if (nsURI.equals(defaultNamespaceURI)) {
            return "";
        }
        
        final Map<String, String> m = namespaceURIToPrefixMapping;
        return m != null ? m.get(nsURI) : null;
    }

    /**
     * @return the prefixed name, based on the ns_prefixes defined in this template's header for the local name and node
     *         namespace passed in as parameters.
     */
    public String getPrefixedName(String localName, String nsURI) {
        if (nsURI == null || nsURI.length() == 0) {
            if (defaultNamespaceURI != null) {
                return NO_NS_PREFIX + ":" + localName;
            } else {
                return localName;
            }
        }
        if (nsURI.equals(defaultNamespaceURI)) {
            return localName;
        }
        String prefix = getPrefixForNamespaceURI(nsURI);
        if (prefix == null) {
            return null;
        }
        return prefix + ":" + localName;
    }

    /**
     * @return an array of the {@link TemplateElement}s containing the given column and line numbers.
     */
    List<TemplateElement> containingElements(int column, int line) {
        final ArrayList<TemplateElement> elements = new ArrayList<TemplateElement>();
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

    /**
     * Reader that builds up the line table info for us, and also helps in working around JavaCC's exception
     * suppression.
     */
    private class LineTableBuilder extends FilterReader {
        
        private final StringBuffer lineBuf = new StringBuffer();
        int lastChar;
        boolean closed;
        
        /** Needed to work around JavaCC behavior where it silently treats any errors as EOF. */ 
        private Exception failure; 

        /**
         * @param r the character stream to wrap
         */
        LineTableBuilder(Reader r) {
            super(r);
        }
        
        public boolean hasFailure() {
            return failure != null;
        }

        public void throwFailure() throws IOException {
            if (failure != null) {
                if (failure instanceof IOException) {
                    throw (IOException) failure;
                }
                if (failure instanceof RuntimeException) {
                    throw (RuntimeException) failure;
                }
                throw new UndeclaredThrowableException(failure);
            }
        }

        public int read() throws IOException {
            try {
                int c = in.read();
                handleChar(c);
                return c;
            } catch (Exception e) {
                throw rememberException(e);
            }
        }

        private IOException rememberException(Exception e) throws IOException {
            // JavaCC used to read from the Reader after it was closed. So we must not treat that as a failure. 
            if (!closed) {
                failure = e;
            }
            if (e instanceof IOException) {
                return (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new UndeclaredThrowableException(e);
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            try {
                int numchars = in.read(cbuf, off, len);
                for (int i=off; i < off+numchars; i++) {
                    char c = cbuf[i];
                    handleChar(c);
                }
                return numchars;
            } catch (Exception e) {
                throw rememberException(e);
            }
        }

        public void close() throws IOException {
            if (lineBuf.length() >0) {
                lines.add(lineBuf.toString());
                lineBuf.setLength(0);
            }
            super.close();
            closed = true;
        }

        private void handleChar(int c) {
            if (c == '\n' || c == '\r') {
                if (lastChar == '\r' && c == '\n') { // CRLF under Windoze
                    int lastIndex = lines.size() -1;
                    String lastLine = (String) lines.get(lastIndex);
                    lines.set(lastIndex, lastLine + '\n');
                } else {
                    lineBuf.append((char) c);
                    lines.add(lineBuf.toString());
                    lineBuf.setLength(0);
                }
            }
            else if (c == '\t') {
                int numSpaces = 8 - (lineBuf.length() %8);
                for (int i=0; i<numSpaces; i++) {
                    lineBuf.append(' ');
                }
            }
            else {
                lineBuf.append((char) c);
            }
            lastChar = c;
        }
    }

}

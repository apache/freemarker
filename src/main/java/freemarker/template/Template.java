/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.tree.TreePath;

import freemarker.cache.TemplateLoader;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.FMParser;
import freemarker.core.LibraryLoad;
import freemarker.core.Macro;
import freemarker.core.ParseException;
import freemarker.core.TemplateElement;
import freemarker.core.TextBlock;
import freemarker.core.TokenMgrError;
import freemarker.debug.impl.DebuggerService;

/**
 * <p>Stores an already parsed template, ready to be processed (rendered) for unlimited times, possibly from
 * multiple threads.
 * 
 * <p>Typically, you will use {@link Configuration#getTemplate(String)} to create/get {@link Template} objects, so
 * you don't construct them directly. But you can also construct a template from a {@link Reader} or a {@link String}
 * that contains the template source code. But then it's
 * important to know that while the resulting {@link Template} is efficient for later processing, creating a new
 * {@link Template} itself is relatively expensive. So try to re-use {@link Template} objects if possible.
 * {@link Configuration#getTemplate(String)} does that (caching {@link Template}-s) for you, but the constructor of
 * course doesn't, so it's up to you to solve then.
 * 
 * <p>Objects of this class meant to be handled as immutable and thus thread-safe. However, it has some setter methods
 * for changing FreeMarker settings. Those must not be used while the template is being processed, or if the
 * template object is already accessible from multiple threads.
 */
public class Template extends Configurable {
    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String NO_NS_PREFIX = "N";
    
    /** This is only non-null during parsing. It's used internally to make some information available through the
     *  Template API-s earlier than the parsing was finished. */
    private transient FMParser parser;

    private Map macros = new HashMap();
    private List imports = new Vector();
    private TemplateElement rootElement;
    private String encoding, defaultNS;
    private int actualTagSyntax;
    private final String name;
    private final ArrayList lines = new ArrayList();
    private Map prefixToNamespaceURILookup = new HashMap();
    private Map namespaceURIToPrefixLookup = new HashMap();

    /**
     * A prime constructor to which all other constructors should
     * delegate directly or indirectly.
     */
    private Template(String name, Configuration cfg)
    {
        super(cfg != null ? cfg : Configuration.getDefaultConfiguration());
        this.name = name;
    }
    

    /**
     * Constructs a template from a character stream. Note that this is a relatively expensive operation; where
     * higher performance matters, you should re-use (cache) {@link Template} instances instead of re-creating them from
     * the same source again and again.
     *
     * @param name the path of the template file relatively to the (virtual) directory that you use to store
     *        the templates. Shouldn't start with {@code '/'}. Should use {@code '/'}, not {@code '\'}.
     *        Check {@link #getName()} to see how the name will be used. The name should be independent of the
     *        actual storage mechanism and physical location as far as possible. Even when the templates are stored
     *        straightforwardly in real files (they often aren't; see {@link TemplateLoader}), the name shouldn't be an
     *        absolute file path. Like if the template is stored in {@code "/www/templates/forum/main.ftl"}, and you
     *        are using {@code "/www/templates/"} as the template root directory via
     *        {@link Configuration#setDirectoryForTemplateLoading(java.io.File)}, then the template name will be
     *        {@code "forum/main.ftl"}.
     * @param reader the character stream to read from. It will always be closed ({@link Reader#close()}).
     * @param cfg the Configuration object that this Template is associated with.
     *        If this is {@code null}, the "default" {@link Configuration} object is used,
     *        which is highly discouraged, because it can easily lead to
     *        erroneous, unpredictable behavior.
     *        (See more {@link Configuration#getDefaultConfiguration() here...})
     */
    public Template(String name, Reader reader, Configuration cfg) throws IOException {
        this(name, reader, cfg, null);
    }
    
    /**
     * Convenience constructor for {@link #Template(String, Reader, Configuration)
     * Template(name, new StringReader(reader), cfg)}.
     */
    public Template(String name, String sourceCode, Configuration cfg) throws IOException {
        this(name, new StringReader(sourceCode), cfg, null);
    }
    
    /**
     * Same as {@link #Template(String, Reader, Configuration)}, but also specifies the template's encoding.
     *
     * @param encoding This is the encoding that we are supposed to be using. But it's not really necessary because we
     *        have a {@link Reader} which is already decoded, but it's kept as meta-info. It also has an impact when
     *        {@code #include}-ing/{@code #import}-ing another template from this template, as its default encoding will
     *        be this. But this behavior of said directives is considered to be harmful, and will be probably phased
     *        out. Until that, it's better to leave this on {@code null}, so that the encoding will come from the
     *        {@link Configuration}. Note that if this is non-{@code null} and there's an {@code #ftl} header with
     *        encoding, they must match, or else a {@link WrongEncodingException} is thrown. 
     *        
     * @deprecated Use {@link #Template(String, Reader, Configuration)} instead.
     */
    public Template(String name, Reader reader, Configuration cfg, String encoding)
    throws IOException
    {
        this(name, cfg);
        this.encoding = encoding;
        try {
            if (!(reader instanceof BufferedReader)) {
                reader = new BufferedReader(reader, 0x1000);
            }
            reader = new LineTableBuilder(reader);
            
            try {
                parser = new FMParser(this, reader,
                        getConfiguration().getStrictSyntaxMode(),
                        getConfiguration().getWhitespaceStripping(),
                        getConfiguration().getTagSyntax(),
                        getConfiguration().getIncompatibleImprovements().intValue());
                this.rootElement = parser.Root();
                this.actualTagSyntax = parser._getLastTagSyntax();
            }
            catch (TokenMgrError exc) {
                // TokenMgrError VS ParseException is not an interesting difference for the user, so we just convert it
                // to ParseException
                throw exc.toParseException(this);
            }
            finally {
                parser = null;
            }
        }
        catch(ParseException e) {
            e.setTemplateName(name);
            throw e;
        }
        finally {
            reader.close();
        }
        DebuggerService.registerTemplate(this);
        namespaceURIToPrefixLookup = Collections.unmodifiableMap(namespaceURIToPrefixLookup);
        prefixToNamespaceURILookup = Collections.unmodifiableMap(prefixToNamespaceURILookup);
    }

    /**
     * Equivalent to {@link #Template(String, Reader, Configuration)
     * Template(name, reader, null)}.
     * 
     * @deprecated This constructor uses the "default" {@link Configuration}
     * instance, which can easily lead to erroneous, unpredictable behavior.
     * See more {@link Configuration#getDefaultConfiguration() here...}.
     */
    public Template(String name, Reader reader) throws IOException {
        this(name, reader, null);
    }

    /**
     * Only used internally.
     */
    Template(String name, TemplateElement root, Configuration config) {
        this(name, config);
        this.rootElement = root;
        DebuggerService.registerTemplate(this);
    }

    /**
     * Returns a trivial template, one that is just a single block of
     * plain text, no dynamic content. (Used by the cache module to create
     * unparsed templates.)
     * @param name the path of the template file relative to the directory what you use to store
     *        the templates. See {@link #getName} for more details.
     * @param content the block of text that this template represents
     * @param config the configuration to which this template belongs
     */
    static public Template getPlainTextTemplate(String name, String content, Configuration config) {
        Template template = new Template(name, config);
        TextBlock block = new TextBlock(content);
        template.rootElement = block;
        DebuggerService.registerTemplate(template);
        return template;
    }

    /**
     * Executes template, using the data-model provided, writing the generated output
     * to the supplied {@link Writer}.
     * 
     * <p>For finer control over the runtime environment setup, such as per-HTTP-request configuring of FreeMarker
     * settings, you may need to use {@link #createProcessingEnvironment(Object, Writer)} instead. 
     * 
     * @param dataModel the holder of the variables visible from the template (name-value pairs); usually a
     *     {@code Map<String, Object>} or a JavaBean (where the JavaBean properties will be the variables).
     *     Can be any object that the {@link ObjectWrapper} in use turns into a {@link TemplateHashModel}.
     *     You can also use an object that already implements {@link TemplateHashModel}; in that case it won't be
     *     wrapped. If it's {@code null}, an empty data model is used.
     * @param out The {@link Writer} where the output of the template will go. Note that unless you have used
     *    {@link Configuration#setAutoFlush(boolean)} to disable this, {@link Writer#flush()} will be called at the
     *    when the template processing was finished. {@link Writer#close()} is not called.
     * 
     * @throws TemplateException if an exception occurs during template processing
     * @throws IOException if an I/O exception occurs during writing to the writer.
     */
    public void process(Object dataModel, Writer out)
    throws TemplateException, IOException
    {
        createProcessingEnvironment(dataModel, out, null).process();
    }

    /**
     * Like {@link #process(Object, Writer)}, but also sets a (XML-)node to be recursively processed by the template.
     * That node is accessed in the template with <tt>.node</tt>, <tt>#recurse</tt>, etc. See the
     * <a href="http://freemarker.org/docs/xgui_declarative.html" target="_blank">Declarative XML Processing</a> as a
     * typical example of recursive node processing.
     * 
     * @param rootNode The root node for recursive processing or {@code null}.
     * 
     * @throws TemplateException if an exception occurs during template processing
     * @throws IOException if an I/O exception occurs during writing to the writer.
     */
    public void process(Object dataModel, Writer out, ObjectWrapper wrapper, TemplateNodeModel rootNode)
    throws TemplateException, IOException
    {
        Environment env = createProcessingEnvironment(dataModel, out, wrapper);
        if (rootNode != null) {
            env.setCurrentVisitorNode(rootNode);
        }
        env.process();
    }
    
    /**
     * Like {@link #process(Object, Writer)}, but overrides the {@link Configuration#getObjectWrapper()}.
     * 
     * @param wrapper The {@link ObjectWrapper} to be used instead of what {@link Configuration#getObjectWrapper()}
     *      provides, or {@code null} if you don't want to override that. 
     */
    public void process(Object dataModel, Writer out, ObjectWrapper wrapper)
    throws TemplateException, IOException
    {
        createProcessingEnvironment(dataModel, out, wrapper).process();
    }
    
   /**
    * Creates a {@link freemarker.core.Environment Environment} object, using this template, the data-model provided as
    * parameter. You have to call {@link Environment#process()} on the return value to set off the actual rendering.
    * 
    * <p>Use this method if you want to do some special initialization on the {@link Environment} before template
    * processing, or if you want to read the {@link Environment} after template processing. Otherwise using
    * {@link Template#process(Object, Writer)} is simpler.
    *
    * <p>Example:
    *
    * <pre>
    * Environment env = myTemplate.createProcessingEnvironment(root, out, null);
    * env.process();</pre>
    * 
    * <p>The above is equivalent with this:
    * 
    * <pre>
    * myTemplate.process(root, out);</pre>
    * 
    * <p>But with <tt>createProcessingEnvironment</tt>, you can manipulate the environment
    * before and after the processing:
    * 
    * <pre>
    * Environment env = myTemplate.createProcessingEnvironment(root, out);
    * 
    * env.setLocale(myUsersPreferredLocale);
    * env.setTimeZone(myUsersPreferredTimezone);
    * 
    * env.process();  // output is rendered here
    * 
    * TemplateModel x = env.getVariable("x");  // read back a variable set by the template</pre>
    *
    * @param dataModel the holder of the variables visible from all templates; see {@link #process(Object, Writer)} for
    *     more details.
    * @param wrapper The {@link ObjectWrapper} to use to wrap objects into {@link TemplateModel}
    *     instances. Normally you left it {@code null}, in which case {@link Configurable#getObjectWrapper()} will be
    *     used.
    * @param out The {@link Writer} where the output of the template will go; see {@link #process(Object, Writer)} for
    *     more details.
    *     
    * @return the {@link Environment} object created for processing. Call {@link Environment#process()} to process the
    *    template.
    * 
    * @throws TemplateException if an exception occurs while setting up the Environment object.
    * @throws IOException if an exception occurs doing any auto-imports
    */
    public Environment createProcessingEnvironment(Object dataModel, Writer out, ObjectWrapper wrapper)
    throws TemplateException, IOException {
        final TemplateHashModel dataModelHash;
        if (dataModel instanceof TemplateHashModel) {
            dataModelHash = (TemplateHashModel) dataModel;
        } else {
            if(wrapper == null) {
                wrapper = getObjectWrapper();
            }

            if (dataModel == null) {
                dataModelHash = new SimpleHash(wrapper);
            } else {
                TemplateModel wrappedDataModel = wrapper.wrap(dataModel);
                if (wrappedDataModel instanceof TemplateHashModel) {
                    dataModelHash = (TemplateHashModel) wrappedDataModel;
                } else if (wrappedDataModel == null) {
                    throw new IllegalArgumentException(
                            wrapper.getClass().getName() + " converted " + dataModel.getClass().getName() + " to null.");
                } else {
                    throw new IllegalArgumentException(
                            wrapper.getClass().getName() + " didn't convert " + dataModel.getClass().getName()
                            + " to a TemplateHashModel. Generally, you want to use a Map<String, Object> or a "
                            + "JavaBean as the root-map (aka. data-model) parameter. The Map key-s or JavaBean "
                            + "property names will be the variable names in the template.");
                }
            }
        }
        return new Environment(this, dataModelHash, out);
    }

    /**
     * Same as {@link #createProcessingEnvironment(Object, Writer, ObjectWrapper)
     * createProcessingEnvironment(dataModel, out, null)}.
     */
    public Environment createProcessingEnvironment(Object dataModel, Writer out)
    throws TemplateException, IOException
    {
        return createProcessingEnvironment(dataModel, out, null);
    }
    
    /**
     * Returns a string representing the raw template
     * text in canonical form.
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
     * The usually path-like (or URL-like) identifier of the template, or possibly {@code null} for non-stored
     * templates. It usually looks like a relative UN*X path; it should use {@code /}, not {@code \}, and shouldn't
     * start with {@code /} (but there are no hard guarantees). It's not a real path in a file-system, it's just
     * a name that a {@link TemplateLoader} used to load the backing resource. Or, it can also be a name that was never
     * used to load the template (directly created with {@link #Template(String, Reader, Configuration)}).
     * Even if the templates are stored straightforwardly in files, this is relative to the base directory of the
     * {@link TemplateLoader}. So it really could be anything, except that it has importance in these situations:
     * 
     * <ul>
     *   <li><p>Relative paths to other templates in this template will be resolved relatively to the directory part of
     *       this. Like if the template name is {@link "foo/this.ftl"}, then {@code <#include "other.ftl">} gets
     *       the template with name {@link "foo/other.ftl"}.
     *   <li><p>It's shown in error messages. So it should be something based on which the user can find the template.
     *   <li><p>Some tools, like an IDE plugin, uses this to identify (and find) templates.
     * </ul>
     * 
     * <p>Some frameworks use URL-like template names like {@code "someSchema://foo/bar.ftl"}. FreeMarker understands
     * this notation, so an absolute path like {@code "/baaz.ftl"} in that template will be resolved too
     * {@code "someSchema://baaz.ftl"}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Configuration object associated with this template.
     */
    public Configuration getConfiguration() {
        return (Configuration) getParent();
    }

    /**
     * Sets the character encoding to use for
     * included files. Usually you don't set this value manually,
     * instead it's assigned to the template upon loading.
     */

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the character encoding used for reading included files.
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * Returns the tag syntax the parser has chosen for this template: {@link Configuration#SQUARE_BRACKET_TAG_SYNTAX}
     * or {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}. If the syntax couldn't be determined (like because there was
     * no tags in the template), this returns whatever the default is in the current configuration.
     */
    public int getActualTagSyntax() {
        return actualTagSyntax;
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
     * Called by code internally to maintain
     * a table of macros
     */
    public void addMacro(Macro macro) {
        macros.put(macro.getName(), macro);
    }

    /**
     * Called by code internally to maintain
     * a list of imports
     */
    public void addImport(LibraryLoad ll) {
        imports.add(ll);
    }

    /**
     * Returns the template source at the location specified by the coordinates given, or {@code null} if unavailable.
     * @param beginColumn the first column of the requested source, 1-based
     * @param beginLine the first line of the requested source, 1-based
     * @param endColumn the last column of the requested source, 1-based
     * @param endLine the last line of the requested source, 1-based
     * @see freemarker.core.TemplateObject#getSource()
     */
    public String getSource(int beginColumn,
                            int beginLine,
                            int endColumn,
                            int endLine)
    {
        if (beginLine < 1 || endLine < 1) return null;  // dynamically ?eval-ed expressions has no source available
        
        // Our container is zero-based.
        --beginLine;
        --beginColumn;
        --endColumn;
        --endLine;
        StringBuffer buf = new StringBuffer();
        for (int i = beginLine ; i<=endLine; i++) {
            if (i < lines.size()) {
                buf.append(lines.get(i));
            }
        }
        int lastLineLength = lines.get(endLine).toString().length();
        int trailingCharsToDelete = lastLineLength - endColumn -1;
        buf.delete(0, beginColumn);
        buf.delete(buf.length() - trailingCharsToDelete, buf.length());
        return buf.toString();
    }

    /**
     * This is a helper class that builds up the line table
     * info for us.
     */
    private class LineTableBuilder extends FilterReader {

        StringBuffer lineBuf = new StringBuffer();
        int lastChar;

        /**
         * @param r the character stream to wrap
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
            for (int i=off; i < off+numchars; i++) {
                char c = cbuf[i];
                handleChar(c);
            }
            return numchars;
        }

        public void close() throws IOException {
            if (lineBuf.length() >0) {
                lines.add(lineBuf.toString());
                lineBuf.setLength(0);
            }
            super.close();
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

    /**
     *  @return the root TemplateElement object.
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
            throw new IllegalArgumentException("The prefix: " + prefix + " cannot be registered, it's reserved for special internal use.");
        }
        if (prefixToNamespaceURILookup.containsKey(prefix)) {
            throw new IllegalArgumentException("The prefix: '" + prefix + "' was repeated. This is illegal.");
        }
        if (namespaceURIToPrefixLookup.containsKey(nsURI)) {
            throw new IllegalArgumentException("The namespace URI: " + nsURI + " cannot be mapped to 2 different prefixes.");
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
     * @return the prefixed name, based on the ns_prefixes defined
     * in this template's header for the local name and node namespace
     * passed in as parameters.
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
     * @return an array of the elements containing the given column and line numbers.
     * @param column the column
     * @param line the line
     */
    public TreePath containingElements(int column, int line) {
        ArrayList elements = new ArrayList();
        TemplateElement element = rootElement;
mainloop:
        while (element.contains(column, line)) {
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
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return new TreePath(elements.toArray());
    }

    static public class WrongEncodingException extends ParseException {

        public String specifiedEncoding;

        public WrongEncodingException(String specifiedEncoding) {
            this.specifiedEncoding = specifiedEncoding;
        }

    }
}


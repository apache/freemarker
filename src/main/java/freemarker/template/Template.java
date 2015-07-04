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

import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
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
    private Object customLookupCondition;
    private int actualTagSyntax;
    private int actualNamingConvention;
    private final String name;
    private final String sourceName;
    private final ArrayList lines = new ArrayList();
    private Map prefixToNamespaceURILookup = new HashMap();
    private Map namespaceURIToPrefixLookup = new HashMap();
    private Version templateLanguageVersion;

    /**
     * A prime constructor to which all other constructors should
     * delegate directly or indirectly.
     */
    private Template(String name, String sourceName, Configuration cfg, boolean overloadSelector)
    {
        super(toNonNull(cfg));
        this.name = name;
        this.sourceName = sourceName;
        this.templateLanguageVersion = normalizeTemplateLanguageVersion(toNonNull(cfg).getIncompatibleImprovements());
    }

    private static Configuration toNonNull(Configuration cfg) {
        return cfg != null ? cfg : Configuration.getDefaultConfiguration();
    }

    /**
     * Same as {@link #Template(String, String, Reader, Configuration)} with {@code null} {@code sourceName} parameter.
     */
    public Template(String name, Reader reader, Configuration cfg) throws IOException {
        this(name, null, reader, cfg);
    }

    /**
     * Convenience constructor for {@link #Template(String, Reader, Configuration)
     * Template(name, new StringReader(reader), cfg)}.
     * 
     * @since 2.3.20
     */
    public Template(String name, String sourceCode, Configuration cfg) throws IOException {
        this(name, new StringReader(sourceCode), cfg);
    }

    /**
     * Convenience constructor for {@link #Template(String, String, Reader, Configuration, String) Template(name, null,
     * reader, cfg, encoding)}.
     * 
     * @deprecated In most applications, use {@link #Template(String, Reader, Configuration)} instead, which doesn't
     *             specify the encoding.
     */
    public Template(String name, Reader reader, Configuration cfg, String encoding) throws IOException {
        this(name, null, reader, cfg, encoding);
    }

    /**
     * Constructs a template from a character stream. Note that this is a relatively expensive operation; where higher
     * performance matters, you should re-use (cache) {@link Template} instances instead of re-creating them from the
     * same source again and again. ({@link Configuration#getTemplate(String) and its overloads already do such reuse.})
     * 
     * @param name
     *            The path of the template file relatively to the (virtual) directory that you use to store the
     *            templates (except if {@link #Template(String, String, Reader, Configuration, String) sourceName}
     *            differs from it). Shouldn't start with {@code '/'}. Should use {@code '/'}, not {@code '\'}. Check
     *            {@link #getName()} to see how the name will be used. The name should be independent of the actual
     *            storage mechanism and physical location as far as possible. Even when the templates are stored
     *            straightforwardly in real files (they often aren't; see {@link TemplateLoader}), the name shouldn't be
     *            an absolute file path. Like if the template is stored in {@code "/www/templates/forum/main.ftl"}, and
     *            you are using {@code "/www/templates/"} as the template root directory via
     *            {@link Configuration#setDirectoryForTemplateLoading(java.io.File)}, then the template name will be
     *            {@code "forum/main.ftl"}. The name can be {@code null} (should be used for template made on-the-fly
     *            instead of being loaded from somewhere), in which case relative paths in it will be relative to
     *            the template root directory (and here again, it's the {@link TemplateLoader} that knows what that
     *            "physically" means).
     * @param sourceName
     *            See {@link #getSourceName()} for the meaning. Can be {@code null}, in which case
     *            {@link #getSourceName()} will return the same as {@link #getName()}.
     * @param reader
     *            The character stream to read from. It will always be closed ({@link Reader#close()}) by this method.
     * @param cfg
     *            The Configuration object that this Template is associated with. If this is {@code null}, the "default"
     *            {@link Configuration} object is used, which is highly discouraged, because it can easily lead to
     *            erroneous, unpredictable behavior. (See more {@link Configuration#getDefaultConfiguration() here...})
     * 
     * @since 2.3.22
     */
   public Template(
           String name, String sourceName, Reader reader, Configuration cfg) throws IOException {
       this(name, sourceName, reader, cfg, null);
   }
    
    /**
     * Same as {@link #Template(String, String, Reader, Configuration)}, but also specifies the template's encoding (not
     * recommended).
     *
     * @param encoding
     *            This is the encoding that we are supposed to be using. But it's not really necessary because we have a
     *            {@link Reader} which is already decoded, but it's kept as meta-info. It also has an impact when
     *            {@code #include}-ing/{@code #import}-ing another template from this template, as its default encoding
     *            will be this. But this behavior of said directives is considered to be harmful, and will be probably
     *            phased out. Until that, it's better to leave this on {@code null}, so that the encoding will come from
     *            the {@link Configuration}. Note that if this is non-{@code null} and there's an {@code #ftl} header
     *            with encoding, they must match, or else a {@link WrongEncodingException} is thrown.
     * 
     * @deprecated In most applications, use {@link #Template(String, String, Reader, Configuration)} instead, which
     *             doesn't specify the encoding.
     * 
     * @since 2.3.22
     */
    public Template(
            String name, String sourceName, Reader reader, Configuration cfg, String encoding) throws IOException {
        this(name, sourceName, cfg, true);
        
        this.encoding = encoding;
        LineTableBuilder ltbReader;
        try {
            if (!(reader instanceof BufferedReader)) {
                reader = new BufferedReader(reader, 0x1000);
            }
            ltbReader = new LineTableBuilder(reader);
            reader = ltbReader;
            
            try {
                final Configuration actualCfg = getConfiguration();
                parser = new FMParser(this, reader,
                        actualCfg.getStrictSyntaxMode(),
                        actualCfg.getWhitespaceStripping(),
                        actualCfg.getTagSyntax(),
                        actualCfg.getNamingConvention(),
                        actualCfg.getIncompatibleImprovements().intValue());
                this.rootElement = parser.Root();
                this.actualTagSyntax = parser._getLastTagSyntax();
                this.actualNamingConvention = parser._getLastNamingConvention();
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
            e.setTemplateName(getSourceName());
            throw e;
        }
        finally {
            reader.close();
        }
        
        // Throws any exception that JavaCC has silently treated as EOF:
        ltbReader.throwFailure();
        
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
        this(name, reader, (Configuration) null);
    }

    /**
     * Only meant to be used internally.
     * 
     * @deprecated Has problems setting actualTagSyntax and templateLanguageVersion; will be removed in 2.4.
     */
    // [2.4] remove this
    Template(String name, TemplateElement root, Configuration cfg) {
        this(name, null, cfg, true);
        this.rootElement = root;
        DebuggerService.registerTemplate(this);
    }
    
    /**
     * Same as {@link #getPlainTextTemplate(String, String, String, Configuration)} with {@code null} {@code sourceName}
     * argument.
     */
    static public Template getPlainTextTemplate(String name, String content, Configuration config) {
        return getPlainTextTemplate(name, null, content, config);
    }
    
    /**
     * Creates a {@link Template} that only contains a single block of static text, no dynamic content.
     * 
     * @param name
     *            See {@link #getName} for more details.
     * @param sourceName
     *            See {@link #getSourceName} for more details. If {@code null}, it will be the same as the {@code name}.
     * @param content
     *            the block of text that this template represents
     * @param config
     *            the configuration to which this template belongs
     * 
     * @since 2.3.22
     */
    static public Template getPlainTextTemplate(String name, String sourceName, String content, Configuration config) {
        Template template = new Template(name, sourceName, config, true);
        template.rootElement = new TextBlock(content);
        template.actualTagSyntax = config.getTagSyntax();
        DebuggerService.registerTemplate(template);
        return template;
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
     * start with {@code /} (but there are no hard guarantees). It's not a real path in a file-system, it's just a name
     * that a {@link TemplateLoader} used to load the backing resource (in simple cases; actually that name is
     * {@link #getSourceName()}, but see it there). Or, it can also be a name that was never used to load the template
     * (directly created with {@link #Template(String, Reader, Configuration)}). Even if the templates are stored
     * straightforwardly in files, this is relative to the base directory of the {@link TemplateLoader}. So it really
     * could be anything, except that it has importance in these situations:
     * 
     * <p>
     * Relative paths to other templates in this template will be resolved relatively to the directory part of this.
     * Like if the template name is {@code "foo/this.ftl"}, then {@code <#include "other.ftl">} gets the template with
     * name {@code "foo/other.ftl"}.
     * </p>
     * 
     * <p>
     * You should not use this name to indicate error locations, or to find the actual templates in general, because
     * localized lookup, acquisition and other lookup strategies can transform names before they get to the
     * {@link TemplateLoader} (the template storage) mechanism. Use {@link #getSourceName()} for these purposes.
     * </p>
     * 
     * <p>
     * Some frameworks use URL-like template names like {@code "someSchema://foo/bar.ftl"}. FreeMarker understands this
     * notation, so an absolute path like {@code "/baaz.ftl"} in that template will be resolved too
     * {@code "someSchema://baaz.ftl"}.
     */
    public String getName() {
        return name;
    }

    /**
     * The name that was actually used to load this template from the {@link TemplateLoader} (or from other custom
     * storage mechanism). This is what should be shown in error messages as the error location. This is usually the
     * same as {@link #getName()}, except when localized lookup, template acquisition ({@code *} step in the name), or
     * other {@link TemplateLookupStrategy} transforms the requested name ({@link #getName()}) to a different final
     * {@link TemplateLoader}-level name. For example, when you get a template with name {@code "foo.ftl"} then because
     * of localized lookup, it's possible that something like {@code "foo_en.ftl"} will be loaded behind the scenes.
     * While the template name will be still the same as the requested template name ({@code "foo.ftl"}), errors should
     * point to {@code "foo_de.ftl"}. Note that relative paths are always resolved relatively to the {@code name}, not
     * to the {@code sourceName}.
     * 
     * @since 2.3.22
     */
    public String getSourceName() {
        return sourceName != null ? sourceName : getName();
    }

    /**
     * Returns the Configuration object associated with this template.
     */
    public Configuration getConfiguration() {
        return (Configuration) getParent();
    }
    
    /**
     * Return the template language (FTL) version used by this template.
     * For now (2.3.21) this is the same as {@link Configuration#getIncompatibleImprovements()}, except
     * that it's normalized to the lowest version where the template language was changed.
     */
    Version getTemplateLanguageVersion() {
        return templateLanguageVersion;
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the default character encoding used for reading included files.
     */
    public String getEncoding() {
        return this.encoding;
    }
    
    /**
     * Gets the custom lookup condition with which this template was found. See the {@code customLookupCondition}
     * parameter of {@link Configuration#getTemplate(String, java.util.Locale, Object, String, boolean, boolean)} for
     * more explanation.
     * 
     * @since 2.3.22
     */
    public Object getCustomLookupCondition() {
        return customLookupCondition;
    }

    /**
     * Mostly only used internally; setter pair of {@link #getCustomLookupCondition()}. This meant to be called directly
     * after instantiating the template with its constructor, after a successfull lookup that used this condition. So
     * this should only be called from code that deals with creating new {@code Template} objects, like from
     * {@link TemplateCache}.
     * 
     * @since 2.3.22
     */
    public void setCustomLookupCondition(Object customLookupCondition) {
        this.customLookupCondition = customLookupCondition;
    }

    /**
     * Returns the tag syntax the parser has chosen for this template. If the syntax could be determined, it's
     * {@link Configuration#SQUARE_BRACKET_TAG_SYNTAX} or {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}. If the syntax
     * couldn't be determined (like because there was no tags in the template, or it was a plain text template), this
     * returns whatever the default is in the current configuration, so it's maybe
     * {@link Configuration#AUTO_DETECT_TAG_SYNTAX}.
     * 
     * @since 2.3.20
     */
    public int getActualTagSyntax() {
        return actualTagSyntax;
    }
    
    /**
     * Returns the naming convention the parser has chosen for this template. If it could be determined, it's
     * {@link Configuration#LEGACY_NAMING_CONVENTION} or {@link Configuration#CAMEL_CASE_NAMING_CONVENTION}. If it
     * couldn't be determined (like because there no identifier that's part of the template language was used where
     * the naming convention matters), this returns whatever the default is in the current configuration, so it's maybe
     * {@link Configuration#AUTO_DETECT_TAG_SYNTAX}.
     * 
     * @since 2.3.23
     */
    public int getActualNamingConvention() {
        return actualNamingConvention;
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
     * 
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public void addMacro(Macro macro) {
        macros.put(macro.getName(), macro);
    }

    /**
     * Called by code internally to maintain a list of imports
     * 
     * @deprecated Should only be used internally, and might will be removed later.
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
     * Reader that builds up the line table info for us, and also helps in working around JavaCC's exception
     * suppression.
     */
    private class LineTableBuilder extends FilterReader {
        
        private final StringBuffer lineBuf = new StringBuffer();
        int lastChar;
        boolean closed;
        
        /** Needed to work around JavaCC behavior where it silently treats any errors as EOF. */ 
        private IOException failure; 

        /**
         * @param r the character stream to wrap
         */
        LineTableBuilder(Reader r) {
            super(r);
        }

        public void throwFailure() throws IOException {
            if (failure != null) {
                throw failure;
            }
        }

        public int read() throws IOException {
            try {
                int c = in.read();
                handleChar(c);
                return c;
            } catch (IOException e) {
                throw rememberException(e);
            }
        }

        private IOException rememberException(IOException e) throws IOException {
            // JavaCC used to read from the Reader after it was closed. So we must not treat that as a failure. 
            if (!closed) {
                failure = e;
            }
            return e;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            try {
                int numchars = in.read(cbuf, off, len);
                for (int i=off; i < off+numchars; i++) {
                    char c = cbuf[i];
                    handleChar(c);
                }
                return numchars;
            } catch (IOException e) {
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

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public TemplateElement getRootTreeNode() {
        return rootElement;
    }
    
    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public Map getMacros() {
        return macros;
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public List getImports() {
        return imports;
    }

    /**
     * This is used internally.
     * 
     * @deprecated Should only be used internally, and might will be removed later.
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
     * @return an array of the {@link TemplateElement}s containing the given column and line numbers.
     * @deprecated Should only be used internally, and might will be removed later.
     */
    public TreePath containingElements(int column, int line) {
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
        if (elements.isEmpty()) {
            return null;
        }
        return new TreePath(elements.toArray());
    }

    /**
     * Thrown by the {@link Template} constructors that specify a non-{@code null} encoding whoch doesn't match the
     * encoding specified in the {@code #ftl} header of the template.
     */
    static public class WrongEncodingException extends ParseException {
        private static final long serialVersionUID = 1L;

        /** @deprecated Use {@link #getTemplateSpecifiedEncoding()} instead. */
        public String specifiedEncoding;
        
        private final String constructorSpecifiedEncoding;

        /**
         * @deprecated Use {@link #WrongEncodingException(String, String)}.
         */
        public WrongEncodingException(String templateSpecifiedEncoding) {
            this(templateSpecifiedEncoding, null);
        }

        /**
         * @since 2.3.22
         */
        public WrongEncodingException(String templateSpecifiedEncoding, String constructorSpecifiedEncoding) {
            this.specifiedEncoding = templateSpecifiedEncoding;
            this.constructorSpecifiedEncoding = constructorSpecifiedEncoding;
        }
        
        public String getMessage() {
            return "Encoding specified inside the template (" + specifiedEncoding
                    + ") doesn't match the encoding specified for the Template constructor"
                    + (constructorSpecifiedEncoding != null ? " (" + constructorSpecifiedEncoding + ")." : ".");
        }

        /**
         * @since 2.3.22
         */
        public String getTemplateSpecifiedEncoding() {
            return specifiedEncoding;
        }

        /**
         * @since 2.3.22
         */
        public String getConstructorSpecifiedEncoding() {
            return constructorSpecifiedEncoding;
        }

    }
}


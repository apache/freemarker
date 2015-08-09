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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.LibraryLoad;
import freemarker.core.Macro;
import freemarker.core.ParseException;
import freemarker.core.ParserConfiguration;
import freemarker.core.TemplateConfigurer;
import freemarker.core.TemplateElement;
import freemarker.core.UnboundTemplate;
import freemarker.core._CoreAPI;
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
    public static final String DEFAULT_NAMESPACE_PREFIX = UnboundTemplate.DEFAULT_NAMESPACE_PREFIX;
    public static final String NO_NS_PREFIX = UnboundTemplate.NO_NS_PREFIX;
    
    private final UnboundTemplate unboundTemplate;
    private final String name;
    private String encoding;
    private Object customLookupCondition;

    /**
     * A prime constructor to which all other constructors should
     * delegate directly or indirectly.
     */
    private Template(UnboundTemplate unboundTemplate, String name, Configuration cfg) {
        super(toNonNull(cfg));
        this.unboundTemplate = unboundTemplate; 
        this.name = name;
    }

    /**
     * To be used internally only!
     */
    Template(UnboundTemplate unboundTemplate,
            String name, Locale locale, Object customLookupCondition,
            Configuration cfg) {
        this(unboundTemplate, name, cfg);
        this.setLocale(locale);
        this.setCustomLookupCondition(customLookupCondition);
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
    @Deprecated
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
    @Deprecated
    public Template(
            String name, String sourceName, Reader reader, Configuration cfg, String encoding) throws IOException {
        this(name, sourceName, reader, cfg, null, encoding);
    }

    /**
     * Same as {@link #Template(String, String, Reader, Configuration, String)}, but also specifies a
     * {@link TemplateConfigurer}. This is mostly meant to be used by FreeMarker internally, but advanced users might
     * still find this useful.
     * 
     * @param customParserCfg
     *            Overrides the parsing related configuration settings of the {@link Configuration} parameter; can be
     *            {@code null}. This is useful as the {@link Configuration} is normally a singleton shared by all
     *            templates, and so it's not good for specifying template-specific settings. (While
     *            {@link Template} itself has methods to specify settings just for that template, those don't influence
     *            the parsing, and you only have opportunity to call them after the parsing anyway.) This objects is
     *            often a {@link TemplateConfigurer} whose parent is the {@link Configuration} parameter, and then it
     *            practically just overrides some of the parser settings, as the others are inherited from the
     *            {@link Configuration}. Note that if this is a {@link TemplateConfigurer}, you will also want to call
     *            {@link TemplateConfigurer#configure(Template)} on the resulting {@link Template} so that
     *            {@link Configurable} settings will be set too, because this constructor only uses it as a
     *            {@link ParserConfiguration}.  
     * @param encoding
     *            Same as in {@link #Template(String, String, Reader, Configuration, String)}. When it's non-{@code
     *            null}, it overrides the value coming from the {@code TemplateConfigurer#getEncoding()} method of the
     *            {@code templateConfigurer} parameter.
     * 
     * @since 2.3.24
     */
    public Template(
            String name, String sourceName, Reader reader, Configuration cfg, ParserConfiguration customParserCfg,
            String encoding) throws IOException {
        this(
                _CoreAPI.newUnboundTemplate(
                        reader,
                        sourceName != null ? sourceName : name,
                        toNonNull(cfg),
                        customParserCfg,
                        encoding),
                name, cfg);
        this.encoding = encoding;
        DebuggerService.registerTemplate(this);
    }
    
    /**
     * Equivalent to {@link #Template(String, Reader, Configuration)
     * Template(name, reader, null)}.
     * 
     * @deprecated This constructor uses the "default" {@link Configuration}
     * instance, which can easily lead to erroneous, unpredictable behavior.
     * See more {@link Configuration#getDefaultConfiguration() here...}.
     */
    @Deprecated
    public Template(String name, Reader reader) throws IOException {
        this(name, reader, (Configuration) null);
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
        Template t = new Template(
                _CoreAPI.newPlainTextUnboundTemplate(content, sourceName != null ? sourceName : name, config),
                name, config);
        DebuggerService.registerTemplate(t);
        return t;
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
    throws TemplateException, IOException {
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
    throws TemplateException, IOException {
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
    throws TemplateException, IOException {
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
            if (wrapper == null) {
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
    throws TemplateException, IOException {
        return createProcessingEnvironment(dataModel, out, null);
    }
    
    /**
     * Returns a string representing the raw template
     * text in canonical form.
     */
    @Override
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
     * Returns the {@link UnboundTemplate} that this {@link Template} is based on.
     * 
     * @since 2.4.0
     */
    public UnboundTemplate getUnboundTemplate() {
        return unboundTemplate;
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
        return unboundTemplate.getSourceName();
    }

    /**
     * Returns the Configuration object associated with this template.
     */
    public Configuration getConfiguration() {
        return (Configuration) getParent();
    }
    
    /**
     * Returns the value passed in as the parameter of
     * {@link #Template(String, String, Reader, Configuration, ParserConfiguration, String)}.
     * 
     * @since 2.3.24
     */
    public ParserConfiguration getCustomParserConfiguration() {
        return getUnboundTemplate().getCustomParserConfiguration();
    }

    /**
     * Returns the {@link ParserConfiguration} that was used for parsing this template. This is most often the same
     * object as {@link #getConfiguration()}, but sometimes it's a {@link TemplateConfigurer}, or something else. It's
     * never {@code null}.
     * 
     * @since 2.3.24
     */
    public ParserConfiguration getEffectiveParserConfiguration() {
        ParserConfiguration customParserCfg = unboundTemplate.getCustomParserConfiguration();
        return customParserCfg != null ? customParserCfg : getConfiguration();
    }
    
    /**
     * Return the template language (FTL) version used by this template.
     * For now (2.4.0) this is the same as {@link Configuration#getIncompatibleImprovements()}, except
     * that it's normalized to the lowest version where the template language was changed.
     * 
     * @since 2.4.0
     */
    public Version getTemplateLanguageVersion() {
        return unboundTemplate.getTemplateLanguageVersion();
    }

    /**
     * @param encoding
     *            The encoding that was used to read this template. When this template {@code #include}-s or
     *            {@code #import}-s another template, by default it will use this encoding for those. For backward
     *            compatibility, this can be {@code null}, which will unset this setting.
     * 
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the name of the charset used for reading included/imported template files by default.
     * 
     * <p>
     * At least when FreeMarker is built-in template loading mechanism is used, by default this setting is set to the
     * same value as the {@link UnboundTemplate#getTemplateSpecifiedEncoding()} of the wrapped {@link UnboundTemplate},
     * if that's non-{@code null}.
     * 
     * <p>
     * While "inheriting" charset from the referring template is not seen as a good idea anymore, it's still used by
     * FreeMarker for backward compatibility (at least by default; as of 2.3.22 no setting exists yet to change that).
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
        return unboundTemplate.getActualTagSyntax();
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
        return unboundTemplate.getActualNamingConvention();
    }

    /**
     * Dump the raw template in canonical form.
     */
    public void dump(PrintStream ps) {
        unboundTemplate.dump(ps);
    }

    /**
     * Dump the raw template in canonical form.
     */
    public void dump(Writer out) throws IOException {
        unboundTemplate.dump(out);
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public void addMacro(Macro macro) {
        _CoreAPI.addMacro(unboundTemplate, macro);
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public void addImport(LibraryLoad libLoad) {
        _CoreAPI.addImport(unboundTemplate, libLoad);
    }

    /**
     * Returns the template source at the location specified by the coordinates given, or {@code null} if unavailable.
     * @param beginColumn the first column of the requested source, 1-based
     * @param beginLine the first line of the requested source, 1-based
     * @param endColumn the last column of the requested source, 1-based
     * @param endLine the last line of the requested source, 1-based
     * @see freemarker.core.TemplateObject#getSource()
     */
    public String getSource(int beginColumn, int beginLine, int endColumn, int endLine) {
        return unboundTemplate.getSource(beginColumn, beginLine, endColumn, endLine);
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public TemplateElement getRootTreeNode() {
        return _CoreAPI.getRootTreeNode(unboundTemplate);
    }
    
    /**
     * For 2.3 backward compatibility. Initialized on demand.
     */
    private volatile Map<String, Macro> legacyMacroMap;
    
    /**
     * Returns the {@link Map} that maps the macro names to the actual macros. This map shouldn't be modified; if you
     * absolutely has to use these deprecated API-s for adding a macro, at least use {@link #addMacro(Macro)}.
     * (Specifying the {@link Map} key has no purpose anyway, as the macro will be always defined with its original
     * name, as returned by {@link Macro#getName()}.) 
     * 
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public Map getMacros() {
        Map<String, Macro> legacyMacroMap = this.legacyMacroMap;
        if (legacyMacroMap == null) {
            synchronized (this) {
                legacyMacroMap = this.legacyMacroMap;
                if (legacyMacroMap == null) {
                    legacyMacroMap = _CoreAPI.createAdapterMacroMapForUnboundCallables(unboundTemplate);
                    this.legacyMacroMap = legacyMacroMap;
                }
            }
        }
        return legacyMacroMap;
    }

    /**
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public List getImports() {
        return _CoreAPI.getImports(unboundTemplate);
    }

    /**
     * This is used internally.
     * 
     * @deprecated Should only be used internally, and might will be removed later.
     */
    @Deprecated
    public void addPrefixNSMapping(String prefix, String nsURI) {
        _CoreAPI.addPrefixNSMapping(unboundTemplate, prefix, nsURI);
    }
    
    public String getDefaultNS() {
        return unboundTemplate.getDefaultNamespaceURI();
    }
    
    /**
     * @return the NamespaceUri mapped to this prefix in this template. (Or null if there is none.)
     */
    public String getNamespaceForPrefix(String prefix) {
        return unboundTemplate.getNamespaceURIForPrefix(prefix);
    }
    
    /**
     * @return the prefix mapped to this nsURI in this template. (Or null if there is none.)
     */
    public String getPrefixForNamespace(String nsURI) {
        return unboundTemplate.getPrefixForNamespaceURI(nsURI);
    }
    
    /**
     * @return the prefixed name, based on the ns_prefixes defined
     * in this template's header for the local name and node namespace
     * passed in as parameters.
     */
    public String getPrefixedName(String localName, String nsURI) {
        return unboundTemplate.getPrefixedName(localName, nsURI);
    }
    
    /**
     * @return an array of the {@link TemplateElement}s containing the given column and line numbers.
     * @deprecated Should only be used internally, and might will be removed later.
     * 
     * @deprecated The objects building up templates aren't part of the published API, and are subject to change.
     */
    @Deprecated
    public List containingElements(int column, int line) {
        return _CoreAPI.containingElements(unboundTemplate, column, line);
    }

    @Override
    protected Map<String, ?> getInitialCustomAttributes() {
        return _CoreAPI.getCustomAttributes(unboundTemplate);
    }

    /**
     * Thrown by the {@link Template} constructors that specify a non-{@code null} encoding which doesn't match the
     * encoding specified in the {@code #ftl} header of the template.
     */
    static public class WrongEncodingException extends ParseException {
        private static final long serialVersionUID = 1L;

        /** @deprecated Use {@link #getTemplateSpecifiedEncoding()} instead. */
        @Deprecated
        public String specifiedEncoding;
        
        private final String constructorSpecifiedEncoding;

        /**
         * @deprecated Use {@link #WrongEncodingException(String, String)}.
         */
        @Deprecated
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
        
        @Override
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.debug._DebuggerService;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * <p>
 * Stores an already parsed template, ready to be processed (rendered) for unlimited times, possibly from multiple
 * threads.
 * 
 * <p>
 * Typically, you will use {@link Configuration#getTemplate(String)} to invoke/get {@link Template} objects, so you
 * don't construct them directly. But you can also construct a template from a {@link Reader} or a {@link String} that
 * contains the template source code. But then it's important to know that while the resulting {@link Template} is
 * efficient for later processing, creating a new {@link Template} itself is relatively expensive. So try to re-use
 * {@link Template} objects if possible. {@link Configuration#getTemplate(String)} (and its overloads) does that
 * (caching {@link Template}-s) for you, but the constructor of course doesn't, so it's up to you to solve then.
 * 
 * <p>
 * Objects of this class meant to be handled as immutable and thus thread-safe. However, it has some setter methods for
 * changing FreeMarker settings. Those must not be used while the template is being processed, or if the template object
 * is already accessible from multiple threads. If some templates need different settings that those coming from the
 * shared {@link Configuration}, and you are using {@link Configuration#getTemplate(String)} (or its overloads), then
 * use {@link Configuration#setTemplateConfigurations(org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory)} to achieve that.
 */
public class Template extends MutableProcessingConfiguration<Template> implements CustomStateScope {
    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String NO_NS_PREFIX = "N";

    private static final int READER_BUFFER_SIZE = 8192;
    
    private Map macros = new HashMap();
    private List imports = new Vector();
    private ASTElement rootElement;
    private Charset sourceEncoding;
    String defaultNS;
    private Serializable customLookupCondition;
    private int actualTagSyntax;
    private int actualNamingConvention;
    private boolean autoEscaping;
    private OutputFormat outputFormat;
    private final String name;
    private final String sourceName;
    private final ArrayList lines = new ArrayList();
    private final ParserConfiguration parserConfiguration;
    private Map prefixToNamespaceURILookup = new HashMap();
    private Map namespaceURIToPrefixLookup = new HashMap();
    private Version templateLanguageVersion;

    private final Object lock = new Object();
    private final ConcurrentHashMap<CustomStateKey, Object> customStateMap = new ConcurrentHashMap<>(0);

    /**
     * A prime constructor to which all other constructors should
     * delegate directly or indirectly.
     */
    private Template(String name, String sourceName, Configuration cfg, ParserConfiguration customParserConfiguration) {
        super(cfg);
        _NullArgumentException.check("cfg", cfg);
        this.name = name;
        this.sourceName = sourceName;
        templateLanguageVersion = normalizeTemplateLanguageVersion(cfg.getIncompatibleImprovements());
        if (customParserConfiguration instanceof TemplateConfiguration.Builder) {
            throw new IllegalArgumentException("Using TemplateConfiguration.Builder as Template constructor "
                    + "argument is not allowed; the TemplateConfiguration that it has built is needed instead.");
        }
        parserConfiguration = customParserConfiguration != null ? customParserConfiguration : getConfiguration();
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
     * Convenience constructor for {@link #Template(String, String, Reader, Configuration, Charset) Template(name, null,
     * reader, cfg, sourceEncoding)}.
     */
    public Template(String name, Reader reader, Configuration cfg, Charset sourceEncoding) throws IOException {
        this(name, null, reader, cfg, sourceEncoding);
    }

    /**
     * Constructs a template from a character stream. Note that this is a relatively expensive operation; where higher
     * performance matters, you should re-use (cache) {@link Template} instances instead of re-creating them from the
     * same source again and again. ({@link Configuration#getTemplate(String) and its overloads already do such reuse.})
     * 
     * @param name
     *            The path of the template file relatively to the (virtual) directory that you use to store the
     *            templates (except if {@link #Template(String, String, Reader, Configuration, Charset) sourceName}
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
     *            The character stream to read from. The {@link Reader} is <em>not</em> closed by this method (unlike
     *            in FreeMarker 2.x.x), so be sure that it's closed somewhere. (Except of course, readers like
     *            {@link StringReader} need not be closed.) The {@link Reader} need not be buffered, because this
     *            method ensures that it will be read in few kilobyte chunks, not byte by byte.
     * @param cfg
     *            The Configuration object that this Template is associated with. Can't be {@code null}.
     * 
     * @since 2.3.22
     */
   public Template(
           String name, String sourceName, Reader reader, Configuration cfg) throws IOException {
       this(name, sourceName, reader, cfg, null);
   }
    
    /**
     * Same as {@link #Template(String, String, Reader, Configuration)}, but also specifies the template's source
     * encoding.
     *
     * @param sourceEncoding
     *            This is the charset that was used to read the template. This can be {@code null} if the template
     *            was loaded from a source that returns it already as text. If this is not {@code null} and there's an
     *            {@code #ftl} header with {@code encoding} parameter, they must match, or else a
     *            {@link WrongTemplateCharsetException} is thrown.
     * 
     * @since 2.3.22
     */
   public Template(
           String name, String sourceName, Reader reader, Configuration cfg, Charset sourceEncoding) throws
           IOException {
       this(name, sourceName, reader, cfg, null, sourceEncoding);
   }
   
    /**
     * Same as {@link #Template(String, String, Reader, Configuration, Charset)}, but also specifies a
     * {@link TemplateConfiguration}. This is mostly meant to be used by FreeMarker internally, but advanced users might
     * still find this useful.
     * 
     * @param customParserConfiguration
     *            Overrides the parsing related configuration settings of the {@link Configuration} parameter; can be
     *            {@code null}. This is useful as the {@link Configuration} is normally a singleton shared by all
     *            templates, and so it's not good for specifying template-specific settings. (While {@link Template}
     *            itself has methods to specify settings just for that template, those don't influence the parsing, and
     *            you only have opportunity to call them after the parsing anyway.) This objects is often a
     *            {@link TemplateConfiguration} whose parent is the {@link Configuration} parameter, and then it
     *            practically just overrides some of the parser settings, as the others are inherited from the
     *            {@link Configuration}. Note that if this is a {@link TemplateConfiguration}, you will also want to
     *            call {@link TemplateConfiguration#apply(Template)} on the resulting {@link Template} so that
     *            {@link MutableProcessingConfiguration} settings will be set too, because this constructor only uses it as a
     *            {@link ParserConfiguration}.
     * @param sourceEncoding
     *            Same as in {@link #Template(String, String, Reader, Configuration, Charset)}.
     * 
     * @since 2.3.24
     */
   public Template(
           String name, String sourceName, Reader reader,
           Configuration cfg, ParserConfiguration customParserConfiguration,
           Charset sourceEncoding) throws IOException {
       this(name, sourceName, reader, cfg, customParserConfiguration, sourceEncoding, null);
    }

    /**
     * Same as {@link #Template(String, String, Reader, Configuration, ParserConfiguration, Charset)}, but allows
     * specifying the {@code streamToUnmarkWhenEncEstabd} {@link InputStream}.
     *
     * @param streamToUnmarkWhenEncEstabd
     *         If not {@code null}, when during the parsing we reach a point where we know that no {@link
     *         WrongTemplateCharsetException} will be thrown, {@link InputStream#mark(int) mark(0)} will be called on this.
     *         This is meant to be used when the reader parameter is a {@link InputStreamReader}, and this parameter is
     *         the underlying {@link InputStream}, and you have a mark at the beginning of the {@link InputStream} so
     *         that you can retry if a {@link WrongTemplateCharsetException} is thrown without extra I/O. As keeping that
     *         mark consumes some resources, so you may want to release it as soon as possible.
     */
   public Template(
           String name, String sourceName, Reader reader,
           Configuration cfg, ParserConfiguration customParserConfiguration,
           Charset sourceEncoding, InputStream streamToUnmarkWhenEncEstabd) throws IOException, ParseException {
        this(name, sourceName, cfg, customParserConfiguration);

       setSourceEncoding(sourceEncoding);
        LineTableBuilder ltbReader;
        try {
            ParserConfiguration actualParserConfiguration = getParserConfiguration();
            
            // Ensure that the parameter Reader is only read in bigger chunks, as we don't know if the it's buffered.
            // In particular, inside the FreeMarker code, we assume that the stream stages need not be buffered.
            if (!(reader instanceof BufferedReader) && !(reader instanceof StringReader)) {
                reader = new BufferedReader(reader, READER_BUFFER_SIZE);
            }
            
            ltbReader = new LineTableBuilder(reader, actualParserConfiguration);
            reader = ltbReader;
            
            try {
                FMParser parser = new FMParser(
                        this, reader, actualParserConfiguration, streamToUnmarkWhenEncEstabd);
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
                actualTagSyntax = parser._getLastTagSyntax();
                actualNamingConvention = parser._getLastNamingConvention();
            } catch (TokenMgrError exc) {
                // TokenMgrError VS ParseException is not an interesting difference for the user, so we just convert it
                // to ParseException
                throw exc.toParseException(this);
            }
        } catch (ParseException e) {
            e.setTemplateName(getSourceName());
            throw e;
        }
        
        // Throws any exception that JavaCC has silently treated as EOF:
        ltbReader.throwFailure();
        
        _DebuggerService.registerTemplate(this);
        namespaceURIToPrefixLookup = Collections.unmodifiableMap(namespaceURIToPrefixLookup);
        prefixToNamespaceURILookup = Collections.unmodifiableMap(prefixToNamespaceURILookup);
    }

    /**
     * Same as {@link #createPlainTextTemplate(String, String, String, Configuration, Charset)} with {@code null}
     * {@code sourceName} argument.
     */
    static public Template createPlainTextTemplate(String name, String content, Configuration config) {
        return createPlainTextTemplate(name, null, content, config, null);
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
     * @param sourceEncoding
     * @since 2.3.22
     */
    static public Template createPlainTextTemplate(String name, String sourceName, String content, Configuration config,
               Charset sourceEncoding) {
        Template template;
        try {
            template = new Template(name, sourceName, new StringReader("X"), config);
        } catch (IOException e) {
            throw new BugException("Plain text template creation failed", e);
        }
        ((ASTStaticText) template.rootElement).replaceText(content);
        template.setSourceEncoding(sourceEncoding);

        _DebuggerService.registerTemplate(template);

        return template;
    }

    private static Version normalizeTemplateLanguageVersion(Version incompatibleImprovements) {
        _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        return Configuration.VERSION_3_0_0;
    }

    /**
     * Executes template, using the data-model provided, writing the generated output to the supplied {@link Writer}.
     * 
     * <p>
     * For finer control over the runtime environment setup, such as per-HTTP-request configuring of FreeMarker
     * settings, you may need to use {@link #createProcessingEnvironment(Object, Writer)} instead.
     * 
     * @param dataModel
     *            the holder of the variables visible from the template (name-value pairs); usually a
     *            {@code Map<String, Object>} or a JavaBean (where the JavaBean properties will be the variables). Can
     *            be any object that the {@link ObjectWrapper} in use turns into a {@link TemplateHashModel}. You can
     *            also use an object that already implements {@link TemplateHashModel}; in that case it won't be
     *            wrapped. If it's {@code null}, an empty data model is used.
     * @param out
     *            The {@link Writer} where the output of the template will go. Note that unless you have used
     *            {@link Configuration#setAutoFlush(boolean)} to disable this, {@link Writer#flush()} will be called at
     *            the when the template processing was finished. {@link Writer#close()} is not called. Can't be
     *            {@code null}.
     * 
     * @throws TemplateException
     *             if an exception occurs during template processing
     * @throws IOException
     *             if an I/O exception occurs during writing to the writer.
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
    * Creates a {@link org.apache.freemarker.core.Environment Environment} object, using this template, the data-model provided as
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
    *     instances. Normally you left it {@code null}, in which case {@link MutableProcessingConfiguration#getObjectWrapper()} will be
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
     * Returns the {@link ParserConfiguration} that was used for parsing this template. This is most often the same
     * object as {@link #getConfiguration()}, but sometimes it's a {@link TemplateConfiguration}, or something else.
     * It's never {@code null}.
     * 
     * @since 2.3.24
     */
    public ParserConfiguration getParserConfiguration() {
        return parserConfiguration;
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
     * @param sourceEncoding
     *            The sourceEncoding that was used to read this template, or {@code null} if the source of the template
     *            already gives back text (as opposed to binary data), so no decoding with a charset was needed.
     */
    void setSourceEncoding(Charset sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * The sourceEncoding that was used to read this template, or {@code null} if the source of the template
     * already gives back text (as opposed to binary data), so no decoding with a charset was needed.
     */
    public Charset getSourceEncoding() {
        return sourceEncoding;
    }
    
    /**
     * Gets the custom lookup condition with which this template was found. See the {@code customLookupCondition}
     * parameter of {@link Configuration#getTemplate(String, Locale, Serializable, boolean)} for more
     * explanation.
     */
    public Serializable getCustomLookupCondition() {
        return customLookupCondition;
    }

    /**
     * Mostly only used internally; setter pair of {@link #getCustomLookupCondition()}. This meant to be called directly
     * after instantiating the template with its constructor, after a successfull lookup that used this condition. So
     * this should only be called from code that deals with creating new {@code Template} objects, like from
     * {@link DefaultTemplateResolver}.
     */
    public void setCustomLookupCondition(Serializable customLookupCondition) {
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
     * Returns the output format (see {@link Configuration#setOutputFormat(OutputFormat)}) used for this template.
     * The output format of a template can come from various places, in order of increasing priority:
     * {@link Configuration#getOutputFormat()}, {@link ParserConfiguration#getOutputFormat()} (which is usually
     * provided by {@link Configuration#getTemplateConfigurations()}) and the {@code #ftl} header's {@code output_format}
     * option in the template.
     * 
     * @since 2.3.24
     */
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }
    
    /**
     * Meant to be called by the parser only. 
     */
    void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    /**
     * Returns if the template actually uses auto-escaping (see {@link Configuration#setAutoEscapingPolicy(int)}). This value
     * is decided by the parser based on the actual {@link OutputFormat}, and the auto-escaping enums, in order of
     * increasing priority: {@link Configuration#getAutoEscapingPolicy()}, {@link ParserConfiguration#getAutoEscapingPolicy()}
     * (which is usually provided by {@link Configuration#getTemplateConfigurations()}), and finally on the {@code #ftl}
     * header's {@code auto_esc} option in the template.
     * 
     * @since 2.3.24
     */
    public boolean getAutoEscaping() {
        return autoEscaping;
    }

    /**
     * Meant to be called by the parser only. 
     */
    void setAutoEscaping(boolean autoEscaping) {
        this.autoEscaping = autoEscaping;
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

    void addMacro(ASTDirMacro macro) {
        macros.put(macro.getName(), macro);
    }

    void addImport(ASTDirImport ll) {
        imports.add(ll);
    }

    /**
     * Returns the template source at the location specified by the coordinates given, or {@code null} if unavailable.
     * A strange legacy in the behavior of this method is that it replaces tab characters with spaces according the
     * value of {@link Template#getParserConfiguration()}/{@link ParserConfiguration#getTabSize()} (which usually
     * comes from {@link Configuration#getTabSize()}), because tab characters move the column number with more than
     * 1 in error messages. However, if you set the tab size to 1, this method leaves the tab characters as is.
     * 
     * @param beginColumn the first column of the requested source, 1-based
     * @param beginLine the first line of the requested source, 1-based
     * @param endColumn the last column of the requested source, 1-based
     * @param endLine the last line of the requested source, 1-based
     * 
     * @see org.apache.freemarker.core.ASTNode#getSource()
     */
    public String getSource(int beginColumn,
                            int beginLine,
                            int endColumn,
                            int endLine) {
        if (beginLine < 1 || endLine < 1) return null;  // dynamically ?eval-ed expressions has no source available
        
        // Our container is zero-based.
        --beginLine;
        --beginColumn;
        --endColumn;
        --endLine;
        StringBuilder buf = new StringBuilder();
        for (int i = beginLine ; i <= endLine; i++) {
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

    @Override
    protected Locale getInheritedLocale() {
        return getParent().getLocale();
    }

    @Override
    protected TimeZone getInheritedTimeZone() {
        return getParent().getTimeZone();
    }

    @Override
    protected TimeZone getInheritedSQLDateAndTimeTimeZone() {
        return getParent().getSQLDateAndTimeTimeZone();
    }

    @Override
    protected String getInheritedNumberFormat() {
        return getParent().getNumberFormat();
    }

    @Override
    protected Map<String, TemplateNumberFormatFactory> getInheritedCustomNumberFormats() {
        return getParent().getCustomNumberFormats();
    }

    @Override
    protected TemplateNumberFormatFactory getInheritedCustomNumberFormat(String name) {
        return getParent().getCustomNumberFormat(name);
    }

    @Override
    protected boolean getInheritedHasCustomFormats() {
        return getParent().hasCustomFormats();
    }

    @Override
    protected String getInheritedBooleanFormat() {
        return getParent().getBooleanFormat();
    }

    @Override
    protected String getInheritedTimeFormat() {
        return getParent().getTimeFormat();
    }

    @Override
    protected String getInheritedDateFormat() {
        return getParent().getDateFormat();
    }

    @Override
    protected String getInheritedDateTimeFormat() {
        return getParent().getDateTimeFormat();
    }

    @Override
    protected Map<String, TemplateDateFormatFactory> getInheritedCustomDateFormats() {
        return getParent().getCustomDateFormats();
    }

    @Override
    protected TemplateDateFormatFactory getInheritedCustomDateFormat(String name) {
        return getParent().getCustomDateFormat(name);
    }

    @Override
    protected TemplateExceptionHandler getInheritedTemplateExceptionHandler() {
        return getParent().getTemplateExceptionHandler();
    }

    @Override
    protected ArithmeticEngine getInheritedArithmeticEngine() {
        return getParent().getArithmeticEngine();
    }

    @Override
    protected ObjectWrapper getInheritedObjectWrapper() {
        return getParent().getObjectWrapper();
    }

    @Override
    protected Charset getInheritedOutputEncoding() {
        return getParent().getOutputEncoding();
    }

    @Override
    protected Charset getInheritedURLEscapingCharset() {
        return getParent().getURLEscapingCharset();
    }

    @Override
    protected TemplateClassResolver getInheritedNewBuiltinClassResolver() {
        return getParent().getNewBuiltinClassResolver();
    }

    @Override
    protected boolean getInheritedAutoFlush() {
        return getParent().getAutoFlush();
    }

    @Override
    protected boolean getInheritedShowErrorTips() {
        return getParent().getShowErrorTips();
    }

    @Override
    protected boolean getInheritedAPIBuiltinEnabled() {
        return getParent().getAPIBuiltinEnabled();
    }

    @Override
    protected boolean getInheritedLogTemplateExceptions() {
        return getParent().getLogTemplateExceptions();
    }

    @Override
    protected boolean getInheritedLazyImports() {
        return getParent().getLazyImports();
    }

    @Override
    protected Boolean getInheritedLazyAutoImports() {
        return getParent().getLazyAutoImports();
    }

    @Override
    protected Map<String, String> getInheritedAutoImports() {
        return getParent().getAutoImports();
    }

    @Override
    protected List<String> getInheritedAutoIncludes() {
        return getParent().getAutoIncludes();
    }

    @Override
    protected Object getInheritedCustomAttribute(Object name) {
        return getParent().getCustomAttribute(name);
    }

    /**
     * Reader that builds up the line table info for us, and also helps in working around JavaCC's exception
     * suppression.
     */
    private class LineTableBuilder extends FilterReader {
        
        private final int tabSize;
        private final StringBuilder lineBuf = new StringBuilder();
        int lastChar;
        boolean closed;
        
        /** Needed to work around JavaCC behavior where it silently treats any errors as EOF. */ 
        private Exception failure; 

        /**
         * @param r the character stream to wrap
         */
        LineTableBuilder(Reader r, ParserConfiguration parserConfiguration) {
            super(r);
            tabSize = parserConfiguration.getTabSize();
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

        @Override
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

        @Override
        public int read(char cbuf[], int off, int len) throws IOException {
            try {
                int numchars = in.read(cbuf, off, len);
                for (int i = off; i < off + numchars; i++) {
                    char c = cbuf[i];
                    handleChar(c);
                }
                return numchars;
            } catch (Exception e) {
                throw rememberException(e);
            }
        }

        @Override
        public void close() throws IOException {
            if (lineBuf.length() > 0) {
                lines.add(lineBuf.toString());
                lineBuf.setLength(0);
            }
            super.close();
            closed = true;
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
            } else if (c == '\t' && tabSize != 1) {
                int numSpaces = tabSize - (lineBuf.length() % tabSize);
                for (int i = 0; i < numSpaces; i++) {
                    lineBuf.append(' ');
                }
            } else {
                lineBuf.append((char) c);
            }
            lastChar = c;
        }
    }

    ASTElement getRootASTNode() {
        return rootElement;
    }
    
    Map getMacros() {
        return macros;
    }

    List getImports() {
        return imports;
    }

    void addPrefixNSMapping(String prefix, String nsURI) {
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
            defaultNS = nsURI;
        } else {
            prefixToNamespaceURILookup.put(prefix, nsURI);
            namespaceURIToPrefixLookup.put(nsURI, prefix);
        }
    }
    
    public String getDefaultNS() {
        return defaultNS;
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCustomState(CustomStateKey<T> customStateKey) {
        T customState = (T) customStateMap.get(customStateKey);
        if (customState == null) {
            synchronized (lock) {
                customState = (T) customStateMap.get(customStateKey);
                if (customState == null) {
                    customState = customStateKey.create();
                    if (customState == null) {
                        throw new IllegalStateException("CustomStateKey.create() must not return null (for key: "
                                + customStateKey + ")");
                    }
                    customStateMap.put(customStateKey, customState);
                }
            }
        }
        return customState;
    }

}


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

import java.io.BufferedInputStream;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.debug._DebuggerService;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.impl.SimpleHash;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;
import org.apache.freemarker.core.util._CollectionUtils;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Stores an already parsed template, ready to be processed (rendered) for unlimited times, possibly from multiple
 * threads.
 * <p>
 * Typically, you will use {@link Configuration#getTemplate(String)} to invoke/get {@link Template} objects, so you
 * don't construct them directly. But you can also construct a template from a {@link Reader} or a {@link String} that
 * contains the template source code. But then it's important to know that while the resulting {@link Template} is
 * efficient for later processing, creating a new {@link Template} itself is relatively expensive. So try to re-use
 * {@link Template} objects if possible. {@link Configuration#getTemplate(String)} (and its overloads) does that
 * (caching {@link Template}-s) for you, but the constructor of course doesn't, so it's up to you to solve then.
 * <p>
 * The {@link ProcessingConfiguration} reader methods of this class don't throw {@link CoreSettingValueNotSetException}
 * because unset settings are ultimately inherited from {@link Configuration}.
 * <p>
 * Objects of this class are immutable and thus thread-safe.
 */
// TODO [FM3] Try to make Template serializable for distributed caching. Transient fields will have to be restored.
public class Template implements ProcessingConfiguration, CustomStateScope {
    public static final String DEFAULT_NAMESPACE_PREFIX = "D";
    public static final String NO_NS_PREFIX = "N";

    private static final int READER_BUFFER_SIZE = 8192;

    private ASTElement rootElement;
    private Map macros = new HashMap(); // TODO Don't create new object if it remains empty.
    private List imports = new ArrayList(); // TODO Don't create new object if it remains empty.

    // Source (TemplateLoader) related information:
    private final String sourceName;
    // TODO [FM3] Get rid of this...
    private final ArrayList<String> lines = new ArrayList<>();

    // TODO [FM3] We want to get rid of these, then the same Template object could be reused for different lookups.
    // Template lookup parameters:
    private final String lookupName;
    private Locale lookupLocale;
    private Serializable customLookupCondition;

    // Inherited settings:
    private final transient Configuration cfg;
    private final transient TemplateConfiguration tCfg;
    private final transient ParsingConfiguration pCfg;

    // Values from the template content (#ftl header parameters usually), as opposed to from the TemplateConfiguration:
    private transient OutputFormat outputFormat; // TODO Deserialization: use the name of the output format
    private String defaultNS;
    private Map prefixToNamespaceURILookup = new HashMap();
    private Map namespaceURIToPrefixLookup = new HashMap();
    /** Custom settings specified inside the template with the #ftl directive. Maybe {@code null}. */
    private Map<String, Serializable> headerCustomSettings;
    /**
     * In case {@link #headerCustomSettings} is not {@code null} and the {@link TemplateConfiguration} also specifies
     * custom settings, this is the two set of custom settings merged. Otherwise it's {@code null}.
     */
    private transient Map<Serializable, Object> tcAndHeaderCustomSettings;

    private AutoEscapingPolicy autoEscapingPolicy;
    // Values from template content that are detected automatically:
    private Charset actualSourceEncoding;
    TagSyntax actualTagSyntax; // TODO [FM3][FREEMARKER-99] Should be private
    private InterpolationSyntax interpolationSyntax;

    // Custom state:
    private final Object customStateMapLock = new Object();
    private final ConcurrentHashMap<CustomStateKey, Object> customStateMap = new ConcurrentHashMap<>(0);

    /**
     * Indicates that the Template constructor has run completely.
     */
    private boolean writeProtected;

    /**
     * Same as {@link #Template(String, String, Reader, Configuration)} with {@code null} {@code sourceName} parameter.
     */
    public Template(String lookupName, Reader reader, Configuration cfg) throws IOException {
        this(lookupName, null, reader, cfg);
    }

    /**
     * Convenience constructor for {@link #Template(String, Reader, Configuration)
     * Template(lookupName, new StringReader(reader), cfg)}.
     */
    public Template(String lookupName, String sourceCode, Configuration cfg) throws IOException {
        this(lookupName, new StringReader(sourceCode), cfg);
    }

    /**
     * Convenience constructor for {@link #Template(String, String, Reader, Configuration, TemplateConfiguration,
     * Charset) Template(lookupName, null, new StringReader(reader), cfg), tc, null}.
     */
    public Template(String lookupName, String sourceCode, Configuration cfg, TemplateConfiguration tc) throws IOException {
        this(lookupName, null, new StringReader(sourceCode), cfg, tc, null);
    }

    /**
     * Convenience constructor for {@link #Template(String, String, Reader, Configuration, Charset) Template(lookupName, null,
     * reader, cfg, sourceEncoding)}.
     */
    public Template(String lookupName, Reader reader, Configuration cfg, Charset sourceEncoding) throws IOException {
        this(lookupName, null, reader, cfg, sourceEncoding);
    }

    /**
     * Constructs a template from a character stream. Note that this is a relatively expensive operation; where higher
     * performance matters, you should re-use (cache) {@link Template} instances instead of re-creating them from the
     * same source again and again. ({@link Configuration#getTemplate(String) and its overloads already do such reuse.})
     * 
     * @param lookupName
     *            The name (path) with which the template was get (usually via
     *            {@link Configuration#getTemplate(String)}), after basic normalization. (Basic normalization means
     *            things that doesn't require accessing the backing storage, such as {@code "/a/../b/foo.f3ah"}
     *            becomes to {@code "b/foo.f3ah"}).
     *            This is usually the path of the template file relatively to the (virtual) directory that you use to
     *            store the templates (except if the {@link #getSourceName()}  sourceName} differs from it).
     *            Shouldn't start with {@code '/'}. Should use {@code '/'}, not {@code '\'}. Check
     *            {@link #getLookupName()} to see how the name will be used. The name should be independent of the actual
     *            storage mechanism and physical location as far as possible. Even when the templates are stored
     *            straightforwardly in real files (they often aren't; see {@link TemplateLoader}), the name shouldn't be
     *            an absolute file path. Like if the template is stored in {@code "/www/templates/forum/main.f3ah"}, and
     *            you are using {@code "/www/templates/"} as the template root directory via
     *            {@link FileTemplateLoader#FileTemplateLoader(java.io.File)}, then the template name will be
     *            {@code "forum/main.f3ah"}. The name can be {@code null} (should be used for template made on-the-fly
     *            instead of being loaded from somewhere), in which case relative paths in it will be relative to
     *            the template root directory (and here again, it's the {@link TemplateLoader} that knows what that
     *            "physically" means).
     * @param sourceName
     *            Often the same as the {@code lookupName}; see {@link #getSourceName()} for more. Can be
     *            {@code null}, in which case error messages will fall back to use {@link #getLookupName()}.
     * @param reader
     *            The character stream to read from. The {@link Reader} is <em>not</em> closed by this method (unlike
     *            in FreeMarker 2.x.x), so be sure that it's closed somewhere. (Except of course, readers like
     *            {@link StringReader} need not be closed.) The {@link Reader} need not be buffered, because this
     *            method ensures that it will be read in few kilobyte chunks, not byte by byte.
     * @param cfg
     *            The Configuration object that this Template is associated with. Can't be {@code null}.
     */
   public Template(
           String lookupName, String sourceName, Reader reader, Configuration cfg) throws IOException {
       this(lookupName, sourceName, reader, cfg, null);
   }

    /**
     * Same as {@link #Template(String, String, Reader, Configuration)}, but also specifies the template's source
     * encoding.
     *
     * @param actualSourceEncoding
     *            This is the charset that was used to read the template. This sould be {@code null} if the template
     *            was loaded from a source that returns it already as text. If this is not {@code null} and there's an
     *            {@code #ftl} header with {@code encoding} parameter, they must match, or else a
     *            {@link WrongTemplateCharsetException} is thrown.
     */
   public Template(
           String lookupName, String sourceName, Reader reader, Configuration cfg, Charset actualSourceEncoding) throws
           IOException {
       this(lookupName, sourceName, reader, cfg, null, actualSourceEncoding);
   }

    /**
     * Same as {@link #Template(String, String, Reader, Configuration, Charset)}, but also specifies a
     * {@link TemplateConfiguration}. This is mostly meant to be used by FreeMarker internally, but advanced users might
     * still find this useful.
     * 
     * @param templateConfiguration
     *            Overrides the configuration settings of the {@link Configuration} parameter; can be
     *            {@code null}. This is useful as the {@link Configuration} is normally a singleton shared by all
     *            templates, and so it's not good for specifying template-specific settings. Settings that influence
     *            parsing always have an effect, while settings that influence processing only have effect when the
     *            template is the main template of the {@link Environment}.
     */
    public Template(
            String lookupName, String sourceName,
            Reader reader,
            Configuration cfg, TemplateConfiguration templateConfiguration, Charset actualSourceEncoding)
                    throws IOException, ParseException {
        this(lookupName, sourceName,
                null, actualSourceEncoding, reader,
                cfg, templateConfiguration,
                null, null);
    }

    /**
     * See {@link #Template(String, String, Reader, Configuration, TemplateConfiguration, Charset)}, except that this
     * one expects an {@link InputStream} and an initial {@link Charset}.
     * 
     * @param initialEncoding
     *            The {@link Charset} we try to decode the {@link InputStream} with. If the template language specifies
     *            its own encoding, this will be overridden by that (hence it's just "inital"), however, that requires
     *            that the template is still parseable with the initial encoding (in practice, that at least the
     *            US-ASCII characters are decoded correctly).
     */
    public Template(
            String lookupName, String sourceName,
            InputStream inputStream, Charset initialEncoding,
            Configuration cfg, TemplateConfiguration templateConfiguration) throws IOException, ParseException {
        this(lookupName, sourceName,
                inputStream, initialEncoding, null,
                cfg, templateConfiguration,
                null, null);
    }
    
    /**
     * Same as the other overloads, but allows specifying the output format and the auto escaping policy, with similar
     * effect as if they were specified in the template content (like in the #ftl header).
     * <p>
     * <p>
     * This method is currently only used internally, as it's not generalized enough and so it carries too much backward
     * compatibility risk. Also, the same functionality can be achieved by constructing an appropriate
     * {@link TemplateConfiguration}, only that's somewhat slower.
     *
     * @param inputStream
     *            Exactly one of this and the {@code reader} must be non-null. This is normally used if the source is
     *            binary (not textual), that is, we have to use a charset to decode it to text.
     * @param initialEncoding
     *            If {@code inputStream} is not-{@code null} then this is the charset we try to use (and so it can't be
     *            {@code null}), but might will be overridden in the template header, in which case this method manages
     *            the re-decoding internally, and the caller need not worry about it. If {@code reader} is
     *            non-{@code null}, and this is non-{@code null}, it's used to check if the charset specified in the
     *            template (if any) matches, and if not, a {@link WrongTemplateCharsetException} is thrown that the
     *            caller is expected to handle. If the template source code comes from a textual source, and so you
     *            don't care about the charset specified in the template, then use {@code null} here (with a
     *            non-{@code null} {@code reader} parameter of course), and then the check should be omitted by the
     *            {@link TemplateLanguage} implementation, so you need not expect a
     *            {@link WrongTemplateCharsetException}.
     * @param reader
     *            Exactly one of this and the {@code inputStream} must be non-null. This is normally used if the source
     *            is textual (not binary), that is, we don't have to worry about the charset to interpret it.
     * @param contextOutputFormat
     *            The output format of the enclosing lexical context, used when a template snippet is parsed on runtime.
     *            If not {@code null}, this will override the value coming from the {@link TemplateConfiguration} or the
     *            {@link Configuration}.
     * @param contextAutoEscapingPolicy
     *            Similar to {@code contextOutputFormat}; usually this and the that is set together.
     */
    Template(
            String lookupName, String sourceName,
            InputStream inputStream, Charset initialEncoding, Reader reader,
            Configuration configuration, TemplateConfiguration templateConfiguration,
            OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy)
            throws IOException, ParseException {
        _NullArgumentException.check("configuration", configuration);
        this.cfg = configuration;
        this.tCfg = templateConfiguration;
        // this.pCfg = ...
        {
            ParsingConfiguration pCfgWithFallback = tCfg != null
                    ? new ParsingConfigurationWithFallback(tCfg, cfg) : cfg;
            TemplateLanguage tempLangFromExt;
            this.pCfg = new FinalParsingConfiguration(
                    pCfgWithFallback,
                    pCfgWithFallback.getRecognizeStandardFileExtensions()
                            && (tempLangFromExt = configuration.getTemplateLanguageForTemplateName(
                                    sourceName != null ? sourceName : lookupName)) != null
                            ? tempLangFromExt
                            : pCfgWithFallback.getTemplateLanguage(),
                    contextOutputFormat, contextAutoEscapingPolicy,
                    cfg);
        }
        
        // The template can change these with some header directive, so they are stored in the Template as well, not
        // only in the ParsingConfiguration:
        setOutputFormat(pCfg.getOutputFormat());
        setAutoEscapingPolicy(pCfg.getAutoEscapingPolicy());
        
        this.lookupName = lookupName;
        this.sourceName = sourceName;
        
        boolean inputStreamMarked;
        Charset guessedEncoding = initialEncoding;
        boolean closeReader;
        if (reader != null) {
            if (inputStream != null) {
                throw new IllegalArgumentException("Both the Reader and InputStream was non-null.");
            }
            closeReader = false; // It wasn't created by this method, so it's not our responsibility
            inputStreamMarked = false; // N/A
        } else if (inputStream != null) {
            if (pCfg.getTemplateLanguage().getCanSpecifyEncodingInContent()) {
                // We need mark support, to restart if the charset suggested by the template content differs
                // from the one we use initially.
                if (!inputStream.markSupported()) {
                    inputStream = new BufferedInputStream(inputStream);
                }
                inputStream.mark(Integer.MAX_VALUE); // Mark will be released as soon as we know the charset for sure.
                inputStreamMarked = true;
            } else {
                inputStreamMarked = false;
            }
            // Regarding buffering worries: On the Reader side we should only read in chunks (like through a
            // BufferedReader), so there shouldn't be a problem if the InputStream is not buffered. (Also, at least
            // on Oracle JDK and OpenJDK 7 the InputStreamReader itself has an internal ~8K buffer.)
            reader = new InputStreamReader(inputStream, initialEncoding);
            closeReader = true; // Because it was created here.
        } else {
            throw new IllegalArgumentException("Both the Reader and InputStream was null.");
        }

        try {
            try {
                parseWithEncoding(reader, guessedEncoding, inputStreamMarked ? inputStream : null);
            } catch (WrongTemplateCharsetException charsetException) {
                final Charset templateSpecifiedEncoding = charsetException.getTemplateSpecifiedEncoding();

                if (inputStreamMarked) {
                    // We restart InputStream to re-decode it with the new charset.
                    inputStream.reset();

                    // Don't close `reader`; it's an InputStreamReader that would close the wrapped InputStream.
                    reader = new InputStreamReader(inputStream, templateSpecifiedEncoding);
                } else {
                    // We can't handle it, the caller should.
                    throw charsetException;
                }
                parseWithEncoding(reader, templateSpecifiedEncoding, inputStream);
            }
        } finally {
            if (closeReader) {
                reader.close();
            }
        }
        
        _DebuggerService.registerTemplate(this);
        namespaceURIToPrefixLookup = _CollectionUtils.unmodifiableMap(namespaceURIToPrefixLookup);
        prefixToNamespaceURILookup = _CollectionUtils.unmodifiableMap(prefixToNamespaceURILookup);

       finishConstruction();
    }

    /**
     * This was extracted from the {@link Template} constructor, so that we can do this again if the attempted charset
     * was incorrect.
     * 
     * @param streamToUnmarkWhenEncEstabd
     *            When the parser meets a directive that established the charset, this is where it should release the
     *            mark (so we don't buffer unnecessarily). Maybe {@code null}.
     */
    private void parseWithEncoding(
            Reader reader,
            Charset attemptedCharset, InputStream streamToUnmarkWhenEncEstabd) throws ParseException, IOException {
        setActualSourceEncoding(attemptedCharset);
        LineTableBuilder ltbReader;
        try {
            // Ensure that the parameter Reader is only read in bigger chunks, as we don't know if the it's buffered.
            // In particular, inside the FreeMarker code, we assume that the stream stages need not be buffered.
            if (!(reader instanceof BufferedReader) && !(reader instanceof StringReader)) {
                reader = new BufferedReader(reader, READER_BUFFER_SIZE);
            }
            
            lines.clear();
            ltbReader = new LineTableBuilder(reader, pCfg, lines);
            reader = ltbReader;
            
            try {
                try {
                    rootElement = pCfg.getTemplateLanguage().parse(this, reader, pCfg, streamToUnmarkWhenEncEstabd);
                } catch (IndexOutOfBoundsException exc) {
                    // There's a JavaCC bug where the Reader throws a RuntimeExcepton and then JavaCC fails with
                    // IndexOutOfBoundsException. If that wasn't the case, we just rethrow. Otherwise we suppress the
                    // IndexOutOfBoundsException and let the real cause to be thrown later. 
                    if (!ltbReader.hasFailure()) {
                        throw exc;
                    }
                    rootElement = null;
                }
            } catch (TokenMgrError exc) {
                // TokenMgrError VS ParseException is not an interesting difference for the user, so we just convert it
                // to ParseException
                throw exc.toParseException(this);
            }
        } catch (ParseException e) {
            e.setTemplate(this);
            throw e;
        }
        
        // Throws any exception that JavaCC has silently treated as EOF:
        ltbReader.throwFailure();
    }

    /**
     * {@link Template} is technically mutable (to simplify internals), but it has to be finalized and then write
     * protected when construction is done.
     */
    private void finishConstruction() {
        headerCustomSettings = _CollectionUtils.unmodifiableMap(headerCustomSettings);
        tcAndHeaderCustomSettings = _CollectionUtils.unmodifiableMap(tcAndHeaderCustomSettings);
        writeProtected = true;
    }

    private void checkWritable() {
        if (writeProtected) {
            throw new IllegalStateException("Template can't be modified anymore");
        }
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
     *            The {@link Writer} where the output of the template will go. Note that unless you have set
     *            {@link ProcessingConfiguration#getAutoFlush() autoFlush} to {@code false} to disable this,
     *            {@link Writer#flush()} will be called at the when the template processing was finished.
     *            {@link Writer#close()} is not called. Can't be {@code null}.
     *
     * @throws TemplateException
     *             if an exception occurs during template processing
     * @throws ObjectWrappingException
     *             if the {@code dataModel} couldn't be wrapped; note that this extends {@link TemplateException}
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
    *     instances. Normally you left it {@code null}, in which case {@link Configuration#getObjectWrapper()} will be
    *     used.
    * @param out The {@link Writer} where the output of the template will go; see {@link #process(Object, Writer)} for
    *     more details.
    *     
    * @return the {@link Environment} object created for processing. Call {@link Environment#process()} to process the
    *    template.
    *
    * @throws TemplateException
    *             if an exception occurs during template processing
    * @throws ObjectWrappingException
    *             if the {@code dataModel} couldn't be wrapped; note that this extends {@link TemplateException}
    * @throws IOException
    *             if an I/O exception occurs during writing to the writer.
    */
    public Environment createProcessingEnvironment(Object dataModel, Writer out, ObjectWrapper wrapper)
    throws TemplateException, IOException {
        final TemplateHashModel dataModelHash;
        if (dataModel instanceof TemplateHashModel) {
            dataModelHash = (TemplateHashModel) dataModel;
        } else {
            if (wrapper == null) {
                wrapper = getConfiguration().getObjectWrapper();
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
     * The usually path-like (or URL-like) normalized identifier of the template, with which the template was get
     * (usually via {@link Configuration#getTemplate(String)}), or possibly {@code null} for non-stored templates.
     * It usually looks like a relative UN*X path; it should use {@code /}, not {@code \}, and shouldn't
     * start with {@code /} (but there are no hard guarantees). It's not a real path in a file-system, it's just a name
     * that a {@link TemplateLoader} used to load the backing resource (in simple cases; actually that name is
     * {@link #getSourceName()}, but see it there). Or, it can also be a name that was never used to load the template
     * (directly created with {@link #Template(String, Reader, Configuration)}). Even if the templates are stored
     * straightforwardly in files, this is relative to the base directory of the {@link TemplateLoader}. So it really
     * could be anything, except that it has importance in these situations:
     * 
     * <p>
     * Relative paths to other templates in this template will be resolved relatively to the directory part of this.
     * Like if the template name is {@code "foo/this.f3ah"}, then {@code <#include "other.f3ah">} gets the template with
     * name {@code "foo/other.f3ah"}.
     * </p>
     * 
     * <p>
     * You should not use this name to indicate error locations, or to find the actual templates in general, because
     * localized template lookup, acquisition and other lookup strategies can transform names before they get to the
     * {@link TemplateLoader} (the template storage) mechanism. Use {@link #getSourceName()} for these purposes.
     * </p>
     * 
     * <p>
     * Some frameworks use URL-like template names like {@code "someSchema://foo/bar.f3ah"}. FreeMarker understands this
     * notation, so an absolute path like {@code "/baaz.f3ah"} in that template will be resolved too
     * {@code "someSchema://baaz.f3ah"}.
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * The name that was actually used to load this template from the {@link TemplateLoader} (or from other custom
     * storage mechanism), or possibly {@code null} for non-stored templates. This is what should be shown in error
     * messages as the error location. This is usually the same as {@link #getLookupName()}, except when localized
     * template lookup, template acquisition ({@code *} step in the name), or other {@link TemplateLookupStrategy}
     * transforms the requested name ({@link #getLookupName()}) to a different final {@link TemplateLoader}-level
     * name. For example, when you get a template with name {@code "foo .f3ah"} then because of localized template
     * lookup, it's possible that something like {@code "foo_en.f3ah"} will be loaded behind the scenes. While the
     * template name will be still the same as the requested template name ({@code "foo .f3ah"}), errors should point
     * to {@code "foo_de.f3ah"}. Note that relative paths are always resolved relatively to the {@code name}, not to
     * the {@code sourceName}.
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Returns the {@linkplain #getSourceName() source name}, or if that's {@code null} then the
     * {@linkplain #getLookupName() lookup name}. This name is primarily meant to be used in error messages.
     */
    public String getSourceOrLookupName() {
        return getSourceName() != null ? getSourceName() : getLookupName();
    }

    /**
     * Returns the Configuration object associated with this template.
     */
    public Configuration getConfiguration() {
        return cfg;
    }

    /**
     * The {@link TemplateConfiguration} associated to this template, or {@code null} if there was none.
     */
    public TemplateConfiguration getTemplateConfiguration() {
        return tCfg;
    }

    /**
     * The {@link ParsingConfiguration} that was actually used to parse this template; this is deduced from the
     * other configurations, and the {@link TemplateLanguage} detected from the file extension.
     */
    public ParsingConfiguration getParsingConfiguration() {
        return pCfg;
    }


    /**
     * @param actualSourceEncoding
     *            The sourceEncoding that was used to read this template, or {@code null} if the source of the template
     *            already gives back text (as opposed to binary data), so no decoding with a charset was needed.
     */
    void setActualSourceEncoding(Charset actualSourceEncoding) {
        checkWritable();
        this.actualSourceEncoding = actualSourceEncoding;
    }

    /**
     * The charset that was actually used to read this template from the binary source, or {@code null} if that
     * information is not known.
     * When using {@link DefaultTemplateResolver}, this is {@code null} exactly if the {@link TemplateLoader}
     * returns text instead of binary content, which should only be the case for data sources that naturally return
     * text (such as varchar and CLOB columns in a database).
     */
    public Charset getActualSourceEncoding() {
        return actualSourceEncoding;
    }
    
    /**
     * Gets the custom lookup condition with which this template was found. See the {@code customLookupCondition}
     * parameter of {@link Configuration#getTemplate(String, Locale, Serializable, boolean)} for more
     * explanation.
     */
    public Serializable getCustomLookupCondition() {
        return customLookupCondition;
    }

    // TODO [FM3] Should be gone; see comment above the lookupLocale field
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
     * Returns the output format (see {@link Configuration#getOutputFormat()}) used for this template.
     * The output format of a template can come from various places, in order of increasing priority:
     * {@link Configuration#getOutputFormat()}, {@link ParsingConfiguration#getOutputFormat()} (which is usually
     * provided by {@link Configuration#getTemplateConfigurations()}) and the {@code #ftl} header's
     * {@code outputFormat} option in the template.
     */
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * Should be called by the parser, for example to apply the output format specified in the #ftl header.
     */
    void setOutputFormat(OutputFormat outputFormat) {
        checkWritable();
        this.outputFormat = outputFormat;
    }
    
    /**
     * Returns the {@link Configuration#getAutoEscapingPolicy()} autoEscapingPolicy) that this template uses.
     * This is decided from these, in increasing priority:
     * {@link Configuration#getAutoEscapingPolicy()}, {@link ParsingConfiguration#getAutoEscapingPolicy()},
     * {@code #ftl} header's {@code auto_esc} option in the template.
     */
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy
                : tCfg != null && tCfg.isAutoEscapingPolicySet() ? tCfg.getAutoEscapingPolicy()
                : cfg.getAutoEscapingPolicy();
    }

    /**
     * Should be called by the parser, for example to apply the auto escaping policy specified in the #ftl header.
     */
    void setAutoEscapingPolicy(AutoEscapingPolicy autoEscapingPolicy) {
        checkWritable();
        this.autoEscapingPolicy = autoEscapingPolicy;
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
        out.write(rootElement != null ? rootElement.getCanonicalForm() : "Unfinished template");
    }

    void addMacro(ASTDirMacroOrFunction macro) {
        macros.put(macro.getName(), macro);
    }

    void addImport(ASTDirImport ll) {
        imports.add(ll);
    }

    /**
     * Returns the template source at the location specified by the coordinates given, or {@code null} if unavailable.
     * A strange legacy in the behavior of this method is that it replaces tab characters with spaces according the
     * value of {@link Template#getParsingConfiguration()}/{@link ParsingConfiguration#getTabSize()} (which usually
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
    public Locale getLocale() {
        // TODO [FM3] Temporary hack; See comment above the locale field
        if (lookupLocale != null) {
            return lookupLocale;
        }

        return tCfg != null && tCfg.isLocaleSet() ? tCfg.getLocale() : cfg.getLocale();
    }

    // TODO [FM3] Temporary hack; See comment above the lookupLocale field
    public void setLookupLocale(Locale lookupLocale) {
        this.lookupLocale = lookupLocale;
    }

    @Override
    public boolean isLocaleSet() {
        return tCfg != null && tCfg.isLocaleSet();
    }

    @Override
    public TimeZone getTimeZone() {
        return tCfg != null && tCfg.isTimeZoneSet() ? tCfg.getTimeZone() : cfg.getTimeZone();
    }

    @Override
    public boolean isTimeZoneSet() {
        return tCfg != null && tCfg.isTimeZoneSet();
    }

    @Override
    public TimeZone getSQLDateAndTimeTimeZone() {
        return tCfg != null && tCfg.isSQLDateAndTimeTimeZoneSet() ? tCfg.getSQLDateAndTimeTimeZone() : cfg.getSQLDateAndTimeTimeZone();
    }

    @Override
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return tCfg != null && tCfg.isSQLDateAndTimeTimeZoneSet();
    }

    @Override
    public String getNumberFormat() {
        return tCfg != null && tCfg.isNumberFormatSet() ? tCfg.getNumberFormat() : cfg.getNumberFormat();
    }

    @Override
    public boolean isNumberFormatSet() {
        return tCfg != null && tCfg.isNumberFormatSet();
    }

    @Override
    public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
        return tCfg != null && tCfg.isCustomNumberFormatsSet() ? tCfg.getCustomNumberFormats()
                : cfg.getCustomNumberFormats();
    }

    @Override
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        if (tCfg != null && tCfg.isCustomNumberFormatsSet()) {
            TemplateNumberFormatFactory value = tCfg.getCustomNumberFormats().get(name);
            if (value != null) {
                return value;
            }
        }
        return cfg.getCustomNumberFormat(name);
    }

    @Override
    public boolean isCustomNumberFormatsSet() {
        return tCfg != null && tCfg.isCustomNumberFormatsSet();
    }

    @Override
    public String getBooleanFormat() {
        return tCfg != null && tCfg.isBooleanFormatSet() ? tCfg.getBooleanFormat() : cfg.getBooleanFormat();
    }

    @Override
    public boolean isBooleanFormatSet() {
        return tCfg != null && tCfg.isBooleanFormatSet();
    }

    @Override
    public String getTimeFormat() {
        return tCfg != null && tCfg.isTimeFormatSet() ? tCfg.getTimeFormat() : cfg.getTimeFormat();
    }

    @Override
    public boolean isTimeFormatSet() {
        return tCfg != null && tCfg.isTimeFormatSet();
    }

    @Override
    public String getDateFormat() {
        return tCfg != null && tCfg.isDateFormatSet() ? tCfg.getDateFormat() : cfg.getDateFormat();
    }

    @Override
    public boolean isDateFormatSet() {
        return tCfg != null && tCfg.isDateFormatSet();
    }

    @Override
    public String getDateTimeFormat() {
        return tCfg != null && tCfg.isDateTimeFormatSet() ? tCfg.getDateTimeFormat() : cfg.getDateTimeFormat();
    }

    @Override
    public boolean isDateTimeFormatSet() {
        return tCfg != null && tCfg.isDateTimeFormatSet();
    }

    @Override
    public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
        return tCfg != null && tCfg.isCustomDateFormatsSet() ? tCfg.getCustomDateFormats() : cfg.getCustomDateFormats();
    }

    @Override
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        if (tCfg != null && tCfg.isCustomDateFormatsSet()) {
            TemplateDateFormatFactory value = tCfg.getCustomDateFormats().get(name);
            if (value != null) {
                return value;
            }
        }
        return cfg.getCustomDateFormat(name);
    }

    @Override
    public boolean isCustomDateFormatsSet() {
        return tCfg != null && tCfg.isCustomDateFormatsSet();
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return tCfg != null && tCfg.isTemplateExceptionHandlerSet() ? tCfg.getTemplateExceptionHandler()
                : cfg.getTemplateExceptionHandler();
    }

    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return tCfg != null && tCfg.isTemplateExceptionHandlerSet();
    }

    @Override
    public AttemptExceptionReporter getAttemptExceptionReporter() {
        return tCfg != null && tCfg.isAttemptExceptionReporterSet() ? tCfg.getAttemptExceptionReporter()
                : cfg.getAttemptExceptionReporter();
    }

    @Override
    public boolean isAttemptExceptionReporterSet() {
        return tCfg != null && tCfg.isAttemptExceptionReporterSet();
    }
    
    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return tCfg != null && tCfg.isArithmeticEngineSet() ? tCfg.getArithmeticEngine() : cfg.getArithmeticEngine();
    }

    @Override
    public boolean isArithmeticEngineSet() {
        return tCfg != null && tCfg.isArithmeticEngineSet();
    }

    @Override
    public Charset getOutputEncoding() {
        return tCfg != null && tCfg.isOutputEncodingSet() ? tCfg.getOutputEncoding() : cfg.getOutputEncoding();
    }

    @Override
    public boolean isOutputEncodingSet() {
        return tCfg != null && tCfg.isOutputEncodingSet();
    }

    @Override
    public Charset getURLEscapingCharset() {
        return tCfg != null && tCfg.isURLEscapingCharsetSet() ? tCfg.getURLEscapingCharset() : cfg.getURLEscapingCharset();
    }

    @Override
    public boolean isURLEscapingCharsetSet() {
        return tCfg != null && tCfg.isURLEscapingCharsetSet();
    }

    @Override
    public TemplateClassResolver getNewBuiltinClassResolver() {
        return tCfg != null && tCfg.isNewBuiltinClassResolverSet() ? tCfg.getNewBuiltinClassResolver() : cfg.getNewBuiltinClassResolver();
    }

    @Override
    public boolean isNewBuiltinClassResolverSet() {
        return tCfg != null && tCfg.isNewBuiltinClassResolverSet();
    }

    @Override
    public boolean getAPIBuiltinEnabled() {
        return tCfg != null && tCfg.isAPIBuiltinEnabledSet() ? tCfg.getAPIBuiltinEnabled() : cfg.getAPIBuiltinEnabled();
    }

    @Override
    public boolean isAPIBuiltinEnabledSet() {
        return tCfg != null && tCfg.isAPIBuiltinEnabledSet();
    }

    @Override
    public boolean getAutoFlush() {
        return tCfg != null && tCfg.isAutoFlushSet() ? tCfg.getAutoFlush() : cfg.getAutoFlush();
    }

    @Override
    public boolean isAutoFlushSet() {
        return tCfg != null && tCfg.isAutoFlushSet();
    }

    @Override
    public boolean getShowErrorTips() {
        return tCfg != null && tCfg.isShowErrorTipsSet() ? tCfg.getShowErrorTips() : cfg.getShowErrorTips();
    }

    @Override
    public boolean isShowErrorTipsSet() {
        return tCfg != null && tCfg.isShowErrorTipsSet();
    }

    @Override
    public boolean getLazyImports() {
        return tCfg != null && tCfg.isLazyImportsSet() ? tCfg.getLazyImports() : cfg.getLazyImports();
    }

    @Override
    public boolean isLazyImportsSet() {
        return tCfg != null && tCfg.isLazyImportsSet();
    }

    @Override
    public Boolean getLazyAutoImports() {
        return tCfg != null && tCfg.isLazyAutoImportsSet() ? tCfg.getLazyAutoImports() : cfg.getLazyAutoImports();
    }

    @Override
    public boolean isLazyAutoImportsSet() {
        return tCfg != null && tCfg.isLazyAutoImportsSet();
    }

    @Override
    public Map<String, String> getAutoImports() {
        return tCfg != null && tCfg.isAutoImportsSet() ? tCfg.getAutoImports() : cfg.getAutoImports();
    }

    @Override
    public boolean isAutoImportsSet() {
        return tCfg != null && tCfg.isAutoImportsSet();
    }

    @Override
    public List<String> getAutoIncludes() {
        return tCfg != null && tCfg.isAutoIncludesSet() ? tCfg.getAutoIncludes() : cfg.getAutoIncludes();
    }

    @Override
    public boolean isAutoIncludesSet() {
        return tCfg != null && tCfg.isAutoIncludesSet();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Map<Serializable, Object> getCustomSettings(boolean includeInherited) {
        boolean nonInheritedAttrsFinal;
        Map<? extends Serializable, ? extends Object> nonInheritedAttrs;
        if (tcAndHeaderCustomSettings != null) {
            nonInheritedAttrs = tcAndHeaderCustomSettings;
            nonInheritedAttrsFinal = writeProtected;
        } else if (headerCustomSettings != null) {
            nonInheritedAttrs = headerCustomSettings;
            nonInheritedAttrsFinal = writeProtected;
        } else if (tCfg != null) {
            nonInheritedAttrs =  tCfg.getCustomSettings(false);
            nonInheritedAttrsFinal = true;
        } else {
            nonInheritedAttrs = Collections.emptyMap();
            nonInheritedAttrsFinal = true;
        }

        Map<Serializable, Object> inheritedAttrs = includeInherited ? cfg.getCustomSettings(true)
                : Collections.<Serializable, Object>emptyMap();

        LinkedHashMap<Serializable, Object> mergedAttrs;
        if (nonInheritedAttrs.isEmpty()) {
            return inheritedAttrs;
        } else if (inheritedAttrs.isEmpty() && nonInheritedAttrsFinal) {
            return (Map) nonInheritedAttrs;
        } else {
            LinkedHashMap<Serializable, Object> result = new LinkedHashMap<>(
                    (inheritedAttrs.size() + nonInheritedAttrs.size()) * 4 / 3 + 1, 0.75f);
            result.putAll(inheritedAttrs);
            result.putAll(nonInheritedAttrs);
            return Collections.unmodifiableMap(result);
        }
    }

    @Override
    public boolean isCustomSettingSet(Serializable key) {
        if (tcAndHeaderCustomSettings != null) {
            return tcAndHeaderCustomSettings.containsKey(key);
        }
        return headerCustomSettings != null && headerCustomSettings.containsKey(key)
                || tCfg != null && tCfg.isCustomSettingSet(key);
    }

    @Override
    public Object getCustomSetting(Serializable key) {
        return getCustomSetting(key, null, false);
    }

    @Override
    public Object getCustomSetting(Serializable key, Object defaultValue) {
        return getCustomSetting(key, defaultValue, true);
    }

    private Object getCustomSetting(Serializable key, Object defaultValue, boolean useDefaultValue) {
        if (tcAndHeaderCustomSettings != null) {
            Object value = tcAndHeaderCustomSettings.get(key);
            if (value != null || tcAndHeaderCustomSettings.containsKey(key)) {
                return value;
            }
        } else {
            if (headerCustomSettings != null) {
                Object value = headerCustomSettings.get(key);
                if (value != null || headerCustomSettings.containsKey(key)) {
                    return value;
                }
            }
            if (tCfg != null) {
                Object value = tCfg.getCustomSetting(key, MISSING_VALUE_MARKER);
                if (value != MISSING_VALUE_MARKER) {
                    return value;
                }
            }
        }
        return useDefaultValue ? cfg.getCustomSetting(key, defaultValue) : cfg.getCustomSetting(key);
    }

    /**
     * Should be called by the parser, for example to add the attributes specified in the #ftl header.
     */
    void setHeaderCustomSetting(String attName, Serializable attValue) {
        checkWritable();

        if (headerCustomSettings == null) {
            headerCustomSettings = new LinkedHashMap<>();
        }
        headerCustomSettings.put(attName, attValue);

        if (tCfg != null) {
            Map<Serializable, Object> tcCustAttrs = tCfg.getCustomSettings(false);
            if (!tcCustAttrs.isEmpty()) {
                if (tcAndHeaderCustomSettings == null) {
                    tcAndHeaderCustomSettings = new LinkedHashMap<>(tcCustAttrs);
                }
                tcAndHeaderCustomSettings.put(attName, attValue);
            }
        }
    }

    /**
     * Reader that builds up the line table info for us, and also helps in working around JavaCC's exception
     * suppression.
     */
    private static class LineTableBuilder extends FilterReader {
        
        private final int tabSize;
        private final StringBuilder lineBuf = new StringBuilder();
        private final List<String> lines;
        
        int lastChar;
        boolean closed;
        
        /** Needed to work around JavaCC behavior where it silently treats any errors as EOF. */ 
        private Exception failure; 

        /**
         * @param r the character stream to wrap
         */
        LineTableBuilder(Reader r, ParsingConfiguration parserConfiguration, List<String> lines) {
            super(r);
            tabSize = parserConfiguration.getTabSize();
            this.lines = lines;
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
                    String lastLine = lines.get(lastIndex);
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
    @SuppressFBWarnings("AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION")
    public <T> T getCustomState(CustomStateKey<T> customStateKey) {
        T customState = (T) customStateMap.get(customStateKey);
        if (customState == null) {
            synchronized (customStateMapLock) {
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


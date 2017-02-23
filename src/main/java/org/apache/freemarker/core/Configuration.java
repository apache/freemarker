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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperBuilder;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.GetTemplateResult;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MruCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CaptureOutput;
import org.apache.freemarker.core.util.HtmlEscape;
import org.apache.freemarker.core.util.NormalizeNewlines;
import org.apache.freemarker.core.util.StandardCompress;
import org.apache.freemarker.core.util.XmlEscape;
import org.apache.freemarker.core.util._ClassUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._SecurityUtil;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.util._UnmodifiableCompositeSet;

/**
 * <b>The main entry point into the FreeMarker API</b>; encapsulates the configuration settings of FreeMarker,
 * also serves as a central template-loading and caching service.
 *
 * <p>This class is meant to be used in a singleton pattern. That is, you create an instance of this at the beginning of
 * the application life-cycle, set its {@link #setSetting(String, String) configuration settings} there (either with the
 * setter methods like {@link #setTemplateLoader(TemplateLoader)} or by loading a {@code .properties} file), and then
 * use that single instance everywhere in your application. Frequently re-creating {@link Configuration} is a typical
 * and grave mistake from performance standpoint, as the {@link Configuration} holds the template templateResolver, and often also
 * the class introspection templateResolver, which then will be lost. (Note that, naturally, having multiple long-lived instances,
 * like one per component that internally uses FreeMarker is fine.)  
 * 
 * <p>The basic usage pattern is like:
 * 
 * <pre>
 *  // Where the application is initialized; in general you do this ONLY ONCE in the application life-cycle!
 *  Configuration cfg = new Configuration(VERSION_<i>X</i>_<i>Y</i>_<i>Z</i>));
 *  // Where X, Y, Z enables the not-100%-backward-compatible fixes introduced in
 *  // FreeMarker version X.Y.Z  and earlier (see {@link #Configuration(Version)}).
 *  cfg.set<i>SomeSetting</i>(...);
 *  cfg.set<i>OtherSetting</i>(...);
 *  ...
 *  
 *  // Later, whenever the application needs a template (so you may do this a lot, and from multiple threads):
 *  {@link Template Template} myTemplate = cfg.{@link #getTemplate(String) getTemplate}("myTemplate.html");
 *  myTemplate.{@link Template#process(Object, java.io.Writer) process}(dataModel, out);</pre>
 * 
 * <p>A couple of settings that you should not leave on its default value are:
 * <ul>
 *   <li>{@link #setTemplateLoader(TemplateLoader) template_loader}: The default value is deprecated and in fact quite
 *       useless. (For the most common cases you can use the convenience methods,
 *       {@link #setDirectoryForTemplateLoading(File)} and {@link #setClassForTemplateLoading(Class, String)} and
 *       {@link #setClassLoaderForTemplateLoading(ClassLoader, String)} too.)
 *   <li>{@link #setDefaultEncoding(String) default_encoding}: The default value is system dependent, which makes it
 *       fragile on servers, so it should be set explicitly, like to "UTF-8" nowadays. 
 *   <li>{@link #setTemplateExceptionHandler(TemplateExceptionHandler) template_exception_handler}: For developing
 *       HTML pages, the most convenient value is {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}. For production,
 *       {@link TemplateExceptionHandler#RETHROW_HANDLER} is safer to use.
 *   <!-- 2.4: recommend the new object wrapper here -->
 * </ul>
 * 
 * <p>A {@link Configuration} object is thread-safe only after you have stopped modifying the configuration settings,
 * and you have <b>safely published</b> it (see JSR 133 and related literature) to other threads. Generally, you set
 * everything directly after you have instantiated the {@link Configuration} object, then you don't change the settings
 * anymore, so then it's safe to make it accessible (again, via a "safe publication" technique) from multiple threads.
 * The methods that aren't for modifying settings, like {@link #getTemplate(String)}, are thread-safe.
 */
public class Configuration extends Configurable implements Cloneable, ParserConfiguration {
    
    private static final String VERSION_PROPERTIES_PATH = "org/apache/freemarker/core/version.properties";
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String DEFAULT_ENCODING_KEY_SNAKE_CASE = "default_encoding"; 
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String DEFAULT_ENCODING_KEY_CAMEL_CASE = "defaultEncoding"; 
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String DEFAULT_ENCODING_KEY = DEFAULT_ENCODING_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALIZED_LOOKUP_KEY_SNAKE_CASE = "localized_lookup";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALIZED_LOOKUP_KEY_CAMEL_CASE = "localizedLookup";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String LOCALIZED_LOOKUP_KEY = LOCALIZED_LOOKUP_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String WHITESPACE_STRIPPING_KEY_SNAKE_CASE = "whitespace_stripping";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String WHITESPACE_STRIPPING_KEY_CAMEL_CASE = "whitespaceStripping";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String WHITESPACE_STRIPPING_KEY = WHITESPACE_STRIPPING_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
    public static final String OUTPUT_FORMAT_KEY_SNAKE_CASE = "output_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
    public static final String OUTPUT_FORMAT_KEY_CAMEL_CASE = "outputFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String OUTPUT_FORMAT_KEY = OUTPUT_FORMAT_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
    public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE = "recognize_standard_file_extensions";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
    public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE = "recognizeStandardFileExtensions";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY
            = RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
    public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE = "registered_custom_output_formats";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
    public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE = "registeredCustomOutputFormats";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY = REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
    public static final String AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE = "auto_escaping_policy";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
    public static final String AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE = "autoEscapingPolicy";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String AUTO_ESCAPING_POLICY_KEY = AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String CACHE_STORAGE_KEY_SNAKE_CASE = "cache_storage";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String CACHE_STORAGE_KEY_CAMEL_CASE = "cacheStorage";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String CACHE_STORAGE_KEY = CACHE_STORAGE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE = "template_update_delay";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_UPDATE_DELAY_KEY_CAMEL_CASE = "templateUpdateDelay";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_UPDATE_DELAY_KEY = TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_INCLUDE_KEY_SNAKE_CASE = "auto_include";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_INCLUDE_KEY_CAMEL_CASE = "autoInclude";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String AUTO_INCLUDE_KEY = AUTO_INCLUDE_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TAG_SYNTAX_KEY_SNAKE_CASE = "tag_syntax";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TAG_SYNTAX_KEY_CAMEL_CASE = "tagSyntax";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TAG_SYNTAX_KEY = TAG_SYNTAX_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String NAMING_CONVENTION_KEY_SNAKE_CASE = "naming_convention";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String NAMING_CONVENTION_KEY_CAMEL_CASE = "namingConvention";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String NAMING_CONVENTION_KEY = NAMING_CONVENTION_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
    public static final String TAB_SIZE_KEY_SNAKE_CASE = "tab_size";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
    public static final String TAB_SIZE_KEY_CAMEL_CASE = "tabSize";
    /** Alias to the {@code ..._SNAKE_CASE} variation. @since 2.3.25 */
    public static final String TAB_SIZE_KEY = TAB_SIZE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_LOADER_KEY_SNAKE_CASE = "template_loader";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_LOADER_KEY_CAMEL_CASE = "templateLoader";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_LOADER_KEY = TEMPLATE_LOADER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE = "template_lookup_strategy";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE = "templateLookupStrategy";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_LOOKUP_STRATEGY_KEY = TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE = "template_name_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE = "templateNameFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_NAME_FORMAT_KEY = TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE;

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
    public static final String TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE = "template_configurations";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
    public static final String TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE = "templateConfigurations";
    /** Alias to the {@code ..._SNAKE_CASE} variation. @since 2.3.24 */
    public static final String TEMPLATE_CONFIGURATIONS_KEY = TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE = "incompatible_improvements";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE = "incompatibleImprovements";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String INCOMPATIBLE_IMPROVEMENTS_KEY = INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE;
    
    private static final String[] SETTING_NAMES_SNAKE_CASE = new String[] {
        // Must be sorted alphabetically!
        AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE,
        CACHE_STORAGE_KEY_SNAKE_CASE,
        DEFAULT_ENCODING_KEY_SNAKE_CASE,
        INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE,
        LOCALIZED_LOOKUP_KEY_SNAKE_CASE,
        NAMING_CONVENTION_KEY_SNAKE_CASE,
        OUTPUT_FORMAT_KEY_SNAKE_CASE,
        RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE,
        REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE,
        TAB_SIZE_KEY_SNAKE_CASE,
        TAG_SYNTAX_KEY_SNAKE_CASE,
        TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE,
        TEMPLATE_LOADER_KEY_SNAKE_CASE,
        TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE,
        TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE,
        TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE,
        WHITESPACE_STRIPPING_KEY_SNAKE_CASE,
    };

    private static final String[] SETTING_NAMES_CAMEL_CASE = new String[] {
        // Must be sorted alphabetically!
        AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE,
        CACHE_STORAGE_KEY_CAMEL_CASE,
        DEFAULT_ENCODING_KEY_CAMEL_CASE,
        INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE,
        LOCALIZED_LOOKUP_KEY_CAMEL_CASE,
        NAMING_CONVENTION_KEY_CAMEL_CASE,
        OUTPUT_FORMAT_KEY_CAMEL_CASE,
        RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE,
        REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
        TAB_SIZE_KEY_CAMEL_CASE,
        TAG_SYNTAX_KEY_CAMEL_CASE,
        TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE,
        TEMPLATE_LOADER_KEY_CAMEL_CASE,
        TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE,
        TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE,
        TEMPLATE_UPDATE_DELAY_KEY_CAMEL_CASE,
        WHITESPACE_STRIPPING_KEY_CAMEL_CASE
    };
    
    private static final Map<String, OutputFormat> STANDARD_OUTPUT_FORMATS;
    static {
        STANDARD_OUTPUT_FORMATS = new HashMap<>();
        STANDARD_OUTPUT_FORMATS.put(UndefinedOutputFormat.INSTANCE.getName(), UndefinedOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(HTMLOutputFormat.INSTANCE.getName(), HTMLOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(XHTMLOutputFormat.INSTANCE.getName(), XHTMLOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(XMLOutputFormat.INSTANCE.getName(), XMLOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(RTFOutputFormat.INSTANCE.getName(), RTFOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(PlainTextOutputFormat.INSTANCE.getName(), PlainTextOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(CSSOutputFormat.INSTANCE.getName(), CSSOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(JavaScriptOutputFormat.INSTANCE.getName(), JavaScriptOutputFormat.INSTANCE);
        STANDARD_OUTPUT_FORMATS.put(JSONOutputFormat.INSTANCE.getName(), JSONOutputFormat.INSTANCE);
    }
    
    public static final int AUTO_DETECT_TAG_SYNTAX = 0;
    public static final int ANGLE_BRACKET_TAG_SYNTAX = 1;
    public static final int SQUARE_BRACKET_TAG_SYNTAX = 2;

    public static final int AUTO_DETECT_NAMING_CONVENTION = 10;
    public static final int LEGACY_NAMING_CONVENTION = 11;
    public static final int CAMEL_CASE_NAMING_CONVENTION = 12;

    /**
     * Don't enable auto-escaping, regardless of what the {@link OutputFormat} is. Note that a {@code 
     * <#ftl auto_esc=true>} in the template will override this.
     */
    public static final int DISABLE_AUTO_ESCAPING_POLICY = 20;
    /**
     * Enable auto-escaping if the output format supports it and {@link MarkupOutputFormat#isAutoEscapedByDefault()} is
     * {@code true}.
     */
    public static final int ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY = 21;
    /** Enable auto-escaping if the {@link OutputFormat} supports it. */
    public static final int ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY = 22;
    
    /** FreeMarker version 3.0.0 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_3_0_0 = new Version(3, 0, 0);
    
    /** The default of {@link #getIncompatibleImprovements()}, currently {@link #VERSION_3_0_0}. */
    public static final Version DEFAULT_INCOMPATIBLE_IMPROVEMENTS = Configuration.VERSION_3_0_0;
    
    private static final String NULL = "null";
    private static final String DEFAULT = "default";
    
    private static final Version VERSION;
    static {
        try {
            Properties vp = new Properties();
            InputStream ins = Configuration.class.getClassLoader()
                    .getResourceAsStream(VERSION_PROPERTIES_PATH);
            if (ins == null) {
                throw new RuntimeException("Version file is missing.");
            } else {
                try {
                    vp.load(ins);
                } finally {
                    ins.close();
                }
                
                String versionString  = getRequiredVersionProperty(vp, "version");
                
                Date buildDate;
                {
                    String buildDateStr = getRequiredVersionProperty(vp, "buildTimestamp");
                    if (buildDateStr.endsWith("Z")) {
                        buildDateStr = buildDateStr.substring(0, buildDateStr.length() - 1) + "+0000";
                    }
                    try {
                        buildDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(buildDateStr);
                    } catch (java.text.ParseException e) {
                        buildDate = null;
                    }
                }
                
                final Boolean gaeCompliant = Boolean.valueOf(getRequiredVersionProperty(vp, "isGAECompliant"));
                
                VERSION = new Version(versionString, gaeCompliant, buildDate);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load and parse " + VERSION_PROPERTIES_PATH, e);
        }
    }
    
    private volatile boolean localizedLookup = true;
    private boolean whitespaceStripping = true;
    private int autoEscapingPolicy = ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY;
    private OutputFormat outputFormat = UndefinedOutputFormat.INSTANCE;
    private boolean outputFormatExplicitlySet;
    private Boolean recognizeStandardFileExtensions;
    private Map<String, ? extends OutputFormat> registeredCustomOutputFormats = Collections.emptyMap(); 
    private Version incompatibleImprovements;
    private int tagSyntax = ANGLE_BRACKET_TAG_SYNTAX;
    private int namingConvention = AUTO_DETECT_NAMING_CONVENTION;
    private int tabSize = 8;  // Default from JavaCC 3.x 

    private DefaultTemplateResolver templateResolver;
    
    private boolean templateLoaderExplicitlySet;
    private boolean templateLookupStrategyExplicitlySet;
    private boolean templateNameFormatExplicitlySet;
    private boolean cacheStorageExplicitlySet;
    
    private boolean objectWrapperExplicitlySet;
    private boolean templateExceptionHandlerExplicitlySet;
    private boolean logTemplateExceptionsExplicitlySet;
    
    private HashMap/*<String, TemplateModel>*/ sharedVariables = new HashMap();

    /**
     * Needed so that it doesn't mater in what order do you call {@link #setSharedVaribles(Map)}
     * and {@link #setObjectWrapper(ObjectWrapper)}. When the user configures FreeMarker from Spring XML, he has no
     * control over the order, so it has to work on both ways.
     */
    private HashMap<String, Object> rewrappableSharedVariables = null;
    
    private String defaultEncoding = _SecurityUtil.getSystemProperty("file.encoding", "utf-8");
    private ConcurrentMap localeToCharsetMap = new ConcurrentHashMap();
    
    /**
     * @deprecated Use {@link #Configuration(Version)} instead. Note that the version can be still modified later with
     *     {@link Configuration#setIncompatibleImprovements(Version)} (or
     *     {@link Configuration#setSettings(Properties)}).  
     */
    @Deprecated
    public Configuration() {
        this(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }

    /**
     * Creates a new instance and sets which of the non-backward-compatible bugfixes/improvements should be enabled.
     * Note that the specified versions corresponds to the {@code incompatible_improvements} configuration setting, and
     * can be changed later, with {@link #setIncompatibleImprovements(Version)} for example. 
     *
     * <p><b>About the "incompatible improvements" setting</b>
     *
     * <p>This setting value is the FreeMarker version number where the not 100% backward compatible bug fixes and
     * improvements that you want to enable were already implemented. In new projects you should set this to the
     * FreeMarker version that you are actually using. In older projects it's also usually better to keep this high,
     * however you better check the changes activated (find them below), at least if not only the 3rd version number
     * (the micro version) of {@code incompatibleImprovements} is increased. Generally, as far as you only increase the
     * last version number of this setting, the changes are always low risk. The default value is 2.3.0 to maximize
     * backward compatibility, but that value isn't recommended.
     * 
     * <p>Bugfixes and improvements that are fully backward compatible, also those that are important security fixes,
     * are enabled regardless of the incompatible improvements setting.
     * 
     * <p>An important consequence of setting this setting is that now your application will check if the stated minimum
     * FreeMarker version requirement is met. Like if you set this setting to 2.3.22, but accidentally the application
     * is deployed with FreeMarker 2.3.21, then FreeMarker will fail, telling that a higher version is required. After
     * all, the fixes/improvements you have requested aren't available on a lower version.
     * 
     * <p>Note that as FreeMarker's minor (2nd) or major (1st) version number increments, it's possible that emulating
     * some of the old bugs will become unsupported, that is, even if you set this setting to a low value, it silently
     * wont bring back the old behavior anymore. Information about that will be present here.
     * 
     * <p>Currently the effects of this setting are:
     * <ul>
     *   <li><p>
     *     3.0.0: This is the lowest supported value in FreeMarker 3.
     *   </li>
     * </ul>
     * 
     * @throws IllegalArgumentException
     *             If {@code incompatibleImmprovements} refers to a version that wasn't released yet when the currently
     *             used FreeMarker version was released, or is less than 3.0.0, or is {@code null}.
     * 
     * @since 2.3.21
     */
    public Configuration(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        
        _NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
        this.incompatibleImprovements = incompatibleImprovements;
        
        createTemplateResolver();
        loadBuiltInSharedVariables();
    }

    private void createTemplateResolver() {
        templateResolver = new DefaultTemplateResolver(
                null,
                getDefaultCacheStorage(),
                getDefaultTemplateLookupStrategy(),
                getDefaultTemplateNameFormat(),
                null,
                this);
        templateResolver.clearTemplateCache(); // for fully BC behavior
        templateResolver.setTemplateUpdateDelayMilliseconds(5000);
    }
    
    private void recreateTemplateResolverWith(
            TemplateLoader loader, CacheStorage storage,
            TemplateLookupStrategy templateLookupStrategy, TemplateNameFormat templateNameFormat,
            TemplateConfigurationFactory templateConfigurations) {
        DefaultTemplateResolver oldCache = templateResolver;
        templateResolver = new DefaultTemplateResolver(
                loader, storage, templateLookupStrategy, templateNameFormat, templateConfigurations, this);
        templateResolver.clearTemplateCache(false);
        templateResolver.setTemplateUpdateDelayMilliseconds(oldCache.getTemplateUpdateDelayMilliseconds());
        templateResolver.setLocalizedLookup(localizedLookup);
    }
    
    private void recreateTemplateResolver() {
        recreateTemplateResolverWith(templateResolver.getTemplateLoader(), templateResolver.getCacheStorage(),
                templateResolver.getTemplateLookupStrategy(), templateResolver.getTemplateNameFormat(),
                getTemplateConfigurations());
    }
    
    private TemplateLookupStrategy getDefaultTemplateLookupStrategy() {
        return getDefaultTemplateLookupStrategy(getIncompatibleImprovements());
    }
    
    static TemplateLookupStrategy getDefaultTemplateLookupStrategy(Version incompatibleImprovements) {
        return DefaultTemplateLookupStrategy.INSTANCE;
    }
    
    private TemplateNameFormat getDefaultTemplateNameFormat() {
        return getDefaultTemplateNameFormat(getIncompatibleImprovements());
    }
    
    static TemplateNameFormat getDefaultTemplateNameFormat(Version incompatibleImprovements) {
        return DefaultTemplateNameFormatFM2.INSTANCE;
    }
    
    private CacheStorage getDefaultCacheStorage() {
        return createDefaultCacheStorage(getIncompatibleImprovements(), getCacheStorage()); 
    }
    
    static CacheStorage createDefaultCacheStorage(Version incompatibleImprovements, CacheStorage existingCacheStorage) {
        if (existingCacheStorage instanceof DefaultSoftCacheStorage) {
            return existingCacheStorage;
        }
        return new DefaultSoftCacheStorage(); 
    }
    
    static CacheStorage createDefaultCacheStorage(Version incompatibleImprovements) {
        return createDefaultCacheStorage(incompatibleImprovements, null); 
    }
    
    private static class DefaultSoftCacheStorage extends SoftCacheStorage {
        // Nothing to override
    }

    private TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
        return getDefaultTemplateExceptionHandler(getIncompatibleImprovements());
    }
    
    private ObjectWrapper getDefaultObjectWrapper() {
        return getDefaultObjectWrapper(getIncompatibleImprovements());
    }
    
    // Package visible as Configurable needs this to initialize the field defaults.
    static TemplateExceptionHandler getDefaultTemplateExceptionHandler(Version incompatibleImprovements) {
        return TemplateExceptionHandler.DEBUG_HANDLER;
    }
    
    @Override
    public Object clone() {
        try {
            Configuration copy = (Configuration) super.clone();
            copy.sharedVariables = new HashMap(sharedVariables);
            copy.localeToCharsetMap = new ConcurrentHashMap(localeToCharsetMap);
            copy.recreateTemplateResolverWith(
                    templateResolver.getTemplateLoader(), templateResolver.getCacheStorage(),
                    templateResolver.getTemplateLookupStrategy(), templateResolver.getTemplateNameFormat(),
                    templateResolver.getTemplateConfigurations());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new BugException("Cloning failed", e);
        }
    }
    
    private void loadBuiltInSharedVariables() {
        sharedVariables.put("capture_output", new CaptureOutput());
        sharedVariables.put("compress", StandardCompress.INSTANCE);
        sharedVariables.put("html_escape", new HtmlEscape());
        sharedVariables.put("normalize_newlines", new NormalizeNewlines());
        sharedVariables.put("xml_escape", new XmlEscape());
    }
    
    /**
     * Loads a preset language-to-encoding map, similarly as if you have called
     * {@link #clearEncodingMap()} and then did multiple {@link #setEncoding(Locale, String)} calls.
     * It assumes the usual character encodings for most languages.
     * The previous content of the encoding map will be lost.
     * This default map currently contains the following mappings:
     * 
     * <table style="width: auto; border-collapse: collapse" border="1" summary="preset language to encoding mapping">
     *   <tr><td>ar</td><td>ISO-8859-6</td></tr>
     *   <tr><td>be</td><td>ISO-8859-5</td></tr>
     *   <tr><td>bg</td><td>ISO-8859-5</td></tr>
     *   <tr><td>ca</td><td>ISO-8859-1</td></tr>
     *   <tr><td>cs</td><td>ISO-8859-2</td></tr>
     *   <tr><td>da</td><td>ISO-8859-1</td></tr>
     *   <tr><td>de</td><td>ISO-8859-1</td></tr>
     *   <tr><td>el</td><td>ISO-8859-7</td></tr>
     *   <tr><td>en</td><td>ISO-8859-1</td></tr>
     *   <tr><td>es</td><td>ISO-8859-1</td></tr>
     *   <tr><td>et</td><td>ISO-8859-1</td></tr>
     *   <tr><td>fi</td><td>ISO-8859-1</td></tr>
     *   <tr><td>fr</td><td>ISO-8859-1</td></tr>
     *   <tr><td>hr</td><td>ISO-8859-2</td></tr>
     *   <tr><td>hu</td><td>ISO-8859-2</td></tr>
     *   <tr><td>is</td><td>ISO-8859-1</td></tr>
     *   <tr><td>it</td><td>ISO-8859-1</td></tr>
     *   <tr><td>iw</td><td>ISO-8859-8</td></tr>
     *   <tr><td>ja</td><td>Shift_JIS</td></tr>
     *   <tr><td>ko</td><td>EUC-KR</td></tr>    
     *   <tr><td>lt</td><td>ISO-8859-2</td></tr>
     *   <tr><td>lv</td><td>ISO-8859-2</td></tr>
     *   <tr><td>mk</td><td>ISO-8859-5</td></tr>
     *   <tr><td>nl</td><td>ISO-8859-1</td></tr>
     *   <tr><td>no</td><td>ISO-8859-1</td></tr>
     *   <tr><td>pl</td><td>ISO-8859-2</td></tr>
     *   <tr><td>pt</td><td>ISO-8859-1</td></tr>
     *   <tr><td>ro</td><td>ISO-8859-2</td></tr>
     *   <tr><td>ru</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sh</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sk</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sl</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sq</td><td>ISO-8859-2</td></tr>
     *   <tr><td>sr</td><td>ISO-8859-5</td></tr>
     *   <tr><td>sv</td><td>ISO-8859-1</td></tr>
     *   <tr><td>tr</td><td>ISO-8859-9</td></tr>
     *   <tr><td>uk</td><td>ISO-8859-5</td></tr>
     *   <tr><td>zh</td><td>GB2312</td></tr>
     *   <tr><td>zh_TW</td><td>Big5</td></tr>
     * </table>
     * 
     * @see #clearEncodingMap()
     * @see #setEncoding(Locale, String)
     * @see #setDefaultEncoding(String)
     */
    public void loadBuiltInEncodingMap() {
        localeToCharsetMap.clear();
        localeToCharsetMap.put("ar", "ISO-8859-6");
        localeToCharsetMap.put("be", "ISO-8859-5");
        localeToCharsetMap.put("bg", "ISO-8859-5");
        localeToCharsetMap.put("ca", "ISO-8859-1");
        localeToCharsetMap.put("cs", "ISO-8859-2");
        localeToCharsetMap.put("da", "ISO-8859-1");
        localeToCharsetMap.put("de", "ISO-8859-1");
        localeToCharsetMap.put("el", "ISO-8859-7");
        localeToCharsetMap.put("en", "ISO-8859-1");
        localeToCharsetMap.put("es", "ISO-8859-1");
        localeToCharsetMap.put("et", "ISO-8859-1");
        localeToCharsetMap.put("fi", "ISO-8859-1");
        localeToCharsetMap.put("fr", "ISO-8859-1");
        localeToCharsetMap.put("hr", "ISO-8859-2");
        localeToCharsetMap.put("hu", "ISO-8859-2");
        localeToCharsetMap.put("is", "ISO-8859-1");
        localeToCharsetMap.put("it", "ISO-8859-1");
        localeToCharsetMap.put("iw", "ISO-8859-8");
        localeToCharsetMap.put("ja", "Shift_JIS");
        localeToCharsetMap.put("ko", "EUC-KR");    
        localeToCharsetMap.put("lt", "ISO-8859-2");
        localeToCharsetMap.put("lv", "ISO-8859-2");
        localeToCharsetMap.put("mk", "ISO-8859-5");
        localeToCharsetMap.put("nl", "ISO-8859-1");
        localeToCharsetMap.put("no", "ISO-8859-1");
        localeToCharsetMap.put("pl", "ISO-8859-2");
        localeToCharsetMap.put("pt", "ISO-8859-1");
        localeToCharsetMap.put("ro", "ISO-8859-2");
        localeToCharsetMap.put("ru", "ISO-8859-5");
        localeToCharsetMap.put("sh", "ISO-8859-5");
        localeToCharsetMap.put("sk", "ISO-8859-2");
        localeToCharsetMap.put("sl", "ISO-8859-2");
        localeToCharsetMap.put("sq", "ISO-8859-2");
        localeToCharsetMap.put("sr", "ISO-8859-5");
        localeToCharsetMap.put("sv", "ISO-8859-1");
        localeToCharsetMap.put("tr", "ISO-8859-9");
        localeToCharsetMap.put("uk", "ISO-8859-5");
        localeToCharsetMap.put("zh", "GB2312");
        localeToCharsetMap.put("zh_TW", "Big5");
    }

    /**
     * Clears language-to-encoding map.
     * @see #loadBuiltInEncodingMap
     * @see #setEncoding
     */
    public void clearEncodingMap() {
        localeToCharsetMap.clear();
    }
    
    /**
     * Sets a {@link TemplateLoader} that is used to look up and load templates;
     * as a side effect the template templateResolver will be emptied.
     * By providing your own {@link TemplateLoader} implementation, you can load templates from whatever kind of
     * storages, like from relational databases, NoSQL-storages, etc.
     * 
     * <p>Convenience methods exists to install commonly used loaders, instead of using this method:
     * {@link #setClassForTemplateLoading(Class, String)}, 
     * {@link #setClassLoaderForTemplateLoading(ClassLoader, String)}, 
     * {@link #setDirectoryForTemplateLoading(File)}, and
     * {@link #setServletContextForTemplateLoading(Object, String)}.
     * 
     * <p>You can chain several {@link TemplateLoader}-s together with {@link MultiTemplateLoader}.
     * 
     * <p>Default value: You should always set the template loader instead of relying on the default value.
     * (But if you still care what it is, before "incompatible improvements" 2.3.21 it's a {@link FileTemplateLoader}
     * that uses the current directory as its root; as it's hard tell what that directory will be, it's not very useful
     * and dangerous. Starting with "incompatible improvements" 2.3.21 the default is {@code null}.)   
     */
    public void setTemplateLoader(TemplateLoader templateLoader) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (templateResolver.getTemplateLoader() != templateLoader) {
                recreateTemplateResolverWith(templateLoader, templateResolver.getCacheStorage(),
                        templateResolver.getTemplateLookupStrategy(), templateResolver.getTemplateNameFormat(),
                        templateResolver.getTemplateConfigurations());
            }
            templateLoaderExplicitlySet = true;
        }
    }
    
    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isTemplateLoaderExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetTemplateLoader() {
        if (templateLoaderExplicitlySet) {
            setTemplateLoader(null);
            templateLoaderExplicitlySet = false;
        }
    }

    /**
     * Tells if {@link #setTemplateLoader(TemplateLoader)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.22
     */
    public boolean isTemplateLoaderExplicitlySet() {
        return templateLoaderExplicitlySet;
    }

    /**
     * The getter pair of {@link #setTemplateLoader(TemplateLoader)}.
     */
    public TemplateLoader getTemplateLoader() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateLoader();
    }
    
    /**
     * Sets a {@link TemplateLookupStrategy} that is used to look up templates based on the requested name; as a side
     * effect the template templateResolver will be emptied. The default value is
     * {@link DefaultTemplateLookupStrategy#INSTANCE}.
     * 
     * @since 2.3.22
     */
    public void setTemplateLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
        if (templateResolver.getTemplateLookupStrategy() != templateLookupStrategy) {
            recreateTemplateResolverWith(templateResolver.getTemplateLoader(), templateResolver.getCacheStorage(),
                    templateLookupStrategy, templateResolver.getTemplateNameFormat(),
                    templateResolver.getTemplateConfigurations());
        }
        templateLookupStrategyExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isTemplateLookupStrategyExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetTemplateLookupStrategy() {
        if (templateLookupStrategyExplicitlySet) {
            setTemplateLookupStrategy(getDefaultTemplateLookupStrategy());
            templateLookupStrategyExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setTemplateLookupStrategy(TemplateLookupStrategy)} (or equivalent) was already called on this
     * instance.
     * 
     * @since 2.3.22
     */
    public boolean isTemplateLookupStrategyExplicitlySet() {
        return templateLookupStrategyExplicitlySet;
    }
    
    /**
     * The getter pair of {@link #setTemplateLookupStrategy(TemplateLookupStrategy)}.
     */
    public TemplateLookupStrategy getTemplateLookupStrategy() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateLookupStrategy();
    }
    
    /**
     * Sets the template name format used. The default is {@link DefaultTemplateNameFormatFM2#INSTANCE}, while the
     * recommended value for new projects is {@link DefaultTemplateNameFormat#INSTANCE}.
     * 
     * @since 2.3.22
     */
    public void setTemplateNameFormat(TemplateNameFormat templateNameFormat) {
        if (templateResolver.getTemplateNameFormat() != templateNameFormat) {
            recreateTemplateResolverWith(templateResolver.getTemplateLoader(), templateResolver.getCacheStorage(),
                    templateResolver.getTemplateLookupStrategy(), templateNameFormat,
                    templateResolver.getTemplateConfigurations());
        }
        templateNameFormatExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isTemplateNameFormatExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetTemplateNameFormat() {
        if (templateNameFormatExplicitlySet) {
            setTemplateNameFormat(getDefaultTemplateNameFormat());
            templateNameFormatExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setTemplateNameFormat(TemplateNameFormat)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.22
     */
    public boolean isTemplateNameFormatExplicitlySet() {
        return templateNameFormatExplicitlySet;
    }

    /**
     * The getter pair of {@link #setTemplateNameFormat(TemplateNameFormat)}.
     */
    public TemplateNameFormat getTemplateNameFormat() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateNameFormat();
    }
    
    /**
     * Sets a {@link TemplateConfigurationFactory} that will configure individual templates where their settings differ
     * from those coming from the common {@link Configuration} object. A typical use case for that is specifying the
     * {@link TemplateConfiguration#setOutputFormat(OutputFormat) outputFormat} for templates based on their file
     * extension or parent directory.
     * 
     * <p>
     * Note that the settings suggested by standard file extensions are stronger than that you set here. See
     * {@link #setRecognizeStandardFileExtensions(boolean)} for more information about standard file extensions.
     * 
     * <p>See "Template configurations" in the FreeMarker Manual for examples.
     * 
     * @since 2.3.24
     */
    public void setTemplateConfigurations(TemplateConfigurationFactory templateConfigurations) {
        if (templateResolver.getTemplateConfigurations() != templateConfigurations) {
            if (templateConfigurations != null) {
                templateConfigurations.setConfiguration(this);
            }
            recreateTemplateResolverWith(templateResolver.getTemplateLoader(), templateResolver.getCacheStorage(),
                    templateResolver.getTemplateLookupStrategy(), templateResolver.getTemplateNameFormat(),
                    templateConfigurations);
        }
    }
    
    /**
     * The getter pair of {@link #setTemplateConfigurations(TemplateConfigurationFactory)}.
     */
    public TemplateConfigurationFactory getTemplateConfigurations() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateConfigurations();
    }

    /**
     * Sets the {@link CacheStorage} used for caching {@link Template}-s;
     * the earlier content of the template templateResolver will be dropt.
     * 
     * The default is a {@link SoftCacheStorage}. If the total size of the {@link Template}
     * objects is significant but most templates are used rarely, using a
     * {@link MruCacheStorage} instead might be advisable. If you don't want caching at
     * all, use {@link org.apache.freemarker.core.templateresolver.impl.NullCacheStorage} (you can't use {@code null}).
     * 
     * <p>Note that setting the templateResolver storage will re-create the template templateResolver, so
     * all its content will be lost.
     */
    public void setCacheStorage(CacheStorage cacheStorage) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (getCacheStorage() != cacheStorage) {
                recreateTemplateResolverWith(templateResolver.getTemplateLoader(), cacheStorage,
                        templateResolver.getTemplateLookupStrategy(), templateResolver.getTemplateNameFormat(),
                        templateResolver.getTemplateConfigurations());
            }
            cacheStorageExplicitlySet = true;
        }
    }
    
    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isCacheStorageExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetCacheStorage() {
        if (cacheStorageExplicitlySet) {
            setCacheStorage(getDefaultCacheStorage());
            cacheStorageExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setCacheStorage(CacheStorage)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.22
     */
    public boolean isCacheStorageExplicitlySet() {
        return cacheStorageExplicitlySet;
    }
    
    /**
     * The getter pair of {@link #setCacheStorage(CacheStorage)}.
     * 
     * @since 2.3.20
     */
    public CacheStorage getCacheStorage() {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (templateResolver == null) {
                return null;
            }
            return templateResolver.getCacheStorage();
        }
    }

    /**
     * Sets the file system directory from which to load templates. This is equivalent to
     * {@code setTemplateLoader(new FileTemplateLoader(dir))}, so see
     * {@link FileTemplateLoader#FileTemplateLoader(File)} for more details.
     * 
     * <p>
     * Note that FreeMarker can load templates from non-file-system sources too. See
     * {@link #setTemplateLoader(TemplateLoader)} from more details.
     * 
     * <p>
     * Note that this shouldn't be used for loading templates that are coming from a WAR; use
     * {@link #setServletContextForTemplateLoading(Object, String)} then. Servlet containers might not unpack the WAR
     * file, in which case you clearly can't access the contained files via {@link File}. Even if the WAR is unpacked,
     * the servlet container might not expose the location as a {@link File}.
     * {@link #setServletContextForTemplateLoading(Object, String)} on the other hand will work in all these cases.
     */
    public void setDirectoryForTemplateLoading(File dir) throws IOException {
        TemplateLoader tl = getTemplateLoader();
        if (tl instanceof FileTemplateLoader) {
            String path = ((FileTemplateLoader) tl).baseDir.getCanonicalPath();
            if (path.equals(dir.getCanonicalPath()))
                return;
        }
        setTemplateLoader(new FileTemplateLoader(dir));
    }

    /**
     * Sets the servlet context from which to load templates.
     * This is equivalent to {@code setTemplateLoader(new WebAppTemplateLoader(sctxt, path))}
     * or {@code setTemplateLoader(new WebAppTemplateLoader(sctxt))} if {@code path} was
     * {@code null}, so see {@code org.apache.freemarker.servlet.WebAppTemplateLoader} for more details.
     * 
     * @param servletContext the {@code javax.servlet.ServletContext} object. (The declared type is {@link Object}
     *        to prevent class loading error when using FreeMarker in an environment where
     *        there's no servlet classes available.)
     * @param path the path relative to the ServletContext.
     *
     * @see #setTemplateLoader(TemplateLoader)
     */
    public void setServletContextForTemplateLoading(Object servletContext, String path) {
        try {
            // Don't introduce linking-time dependency on servlets
            final Class webappTemplateLoaderClass = _ClassUtil.forName(
                    "org.apache.freemarker.servlet.WebAppTemplateLoader");
            
            // Don't introduce linking-time dependency on servlets
            final Class servletContextClass = _ClassUtil.forName("javax.servlet.ServletContext");
            
            final Class[] constructorParamTypes;
            final Object[] constructorParams;
            if (path == null) {
                constructorParamTypes = new Class[] { servletContextClass };
                constructorParams = new Object[] { servletContext };
            } else {
                constructorParamTypes = new Class[] { servletContextClass, String.class };
                constructorParams = new Object[] { servletContext, path };
            }
            
            setTemplateLoader( (TemplateLoader)
                    webappTemplateLoaderClass
                            .getConstructor(constructorParamTypes)
                                    .newInstance(constructorParams));
        } catch (Exception e) {
            throw new BugException(e);
        }
    }

    /**
     * Sets the class whose {@link Class#getResource(String)} method will be used to load templates, from the inside the
     * package specified. See {@link ClassTemplateLoader#ClassTemplateLoader(Class, String)} for more details.
     * 
     * @param basePackagePath
     *            Separate steps with {@code "/"}, not {@code "."}, and note that it matters if this starts with
     *            {@code /} or not. See {@link ClassTemplateLoader#ClassTemplateLoader(Class, String)} for more details.
     * 
     * @see #setClassLoaderForTemplateLoading(ClassLoader, String)
     * @see #setTemplateLoader(TemplateLoader)
     */
    public void setClassForTemplateLoading(Class resourceLoaderClass, String basePackagePath) {
        setTemplateLoader(new ClassTemplateLoader(resourceLoaderClass, basePackagePath));
    }
    
    /**
     * Sets the {@link ClassLoader} whose {@link ClassLoader#getResource(String)} method will be used to load templates,
     * from the inside the package specified. See {@link ClassTemplateLoader#ClassTemplateLoader(Class, String)} for
     * more details.
     * 
     * @param basePackagePath
     *            Separate steps with {@code "/"}, not {@code "."}. See
     *            {@link ClassTemplateLoader#ClassTemplateLoader(Class, String)} for more details.
     * 
     * @see #setClassForTemplateLoading(Class, String)
     * @see #setTemplateLoader(TemplateLoader)
     * 
     * @since 2.3.22
     */
    public void setClassLoaderForTemplateLoading(ClassLoader classLoader, String basePackagePath) {
        setTemplateLoader(new ClassTemplateLoader(classLoader, basePackagePath));
    }

    /**
     * Sets the time in milliseconds that must elapse before checking whether there is a newer version of a template
     * "file" exists than the cached one. Defaults to 5000 ms.
     * 
     * <p>
     * When you get a template via {@link #getTemplate(String)} (or some of its overloads). FreeMarker will try to get
     * the template from the template templateResolver. If the template is found, and at least this amount of time was elapsed
     * since the template last modification date was checked, FreeMarker will re-check the last modification date (this
     * could mean I/O), possibly reloading the template and updating the templateResolver as a consequence (can mean even more
     * I/O). The {@link #getTemplate(String)} (or some of its overloads) call will only return after this all is
     * done, so it will return the fresh template.
     * 
     * @since 2.3.23
     */
    public void setTemplateUpdateDelayMilliseconds(long millis) {
        templateResolver.setTemplateUpdateDelayMilliseconds(millis);
    }
    
    /**
     * The getter pair of {@link #setTemplateUpdateDelayMilliseconds(long)}.
     * 
     * @since 2.3.23
     */
    public long getTemplateUpdateDelayMilliseconds() {
        return templateResolver.getTemplateUpdateDelayMilliseconds();
    }
    
    @Override
    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        ObjectWrapper prevObjectWrapper = getObjectWrapper();
        super.setObjectWrapper(objectWrapper);
        objectWrapperExplicitlySet = true;
        if (objectWrapper != prevObjectWrapper) {
            try {
                setSharedVariablesFromRewrappableSharedVariables();
            } catch (TemplateModelException e) {
                throw new RuntimeException(
                        "Failed to re-wrap earliearly set shared variables with the newly set object wrapper",
                        e);
            }
        }
    }
    
    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isObjectWrapperExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetObjectWrapper() {
        if (objectWrapperExplicitlySet) {
            setObjectWrapper(getDefaultObjectWrapper());
            objectWrapperExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setObjectWrapper(ObjectWrapper)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.22
     */
    public boolean isObjectWrapperExplicitlySet() {
        return objectWrapperExplicitlySet;
    }
    
    @Override
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        super.setTemplateExceptionHandler(templateExceptionHandler);
        templateExceptionHandlerExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isTemplateExceptionHandlerExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetTemplateExceptionHandler() {
        if (templateExceptionHandlerExplicitlySet) {
            setTemplateExceptionHandler(getDefaultTemplateExceptionHandler());
            templateExceptionHandlerExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setTemplateExceptionHandler(TemplateExceptionHandler)} (or equivalent) was already called on
     * this instance.
     * 
     * @since 2.3.22
     */
    public boolean isTemplateExceptionHandlerExplicitlySet() {
        return templateExceptionHandlerExplicitlySet;
    }    
    
    /**
     * @since 2.3.22
     */
    @Override
    public void setLogTemplateExceptions(boolean value) {
        super.setLogTemplateExceptions(value);
        logTemplateExceptionsExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isTemplateExceptionHandlerExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.22
     */
    public void unsetLogTemplateExceptions() {
        if (logTemplateExceptionsExplicitlySet) {
            setLogTemplateExceptions(false);
            logTemplateExceptionsExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setLogTemplateExceptions(boolean)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.22
     */
    public boolean isLogTemplateExceptionsExplicitlySet() {
        return logTemplateExceptionsExplicitlySet;
    }

    /**
     * Use {@link #Configuration(Version)} instead if possible; see the meaning of the parameter there.
     * If the default value of a setting depends on the {@code incompatibleImprovements} and the value of that setting
     * was never set in this {@link Configuration} object through the public API, its value will be set to the default
     * value appropriate for the new {@code incompatibleImprovements}. (This adjustment of a setting value doesn't
     * count as setting that setting, so setting {@code incompatibleImprovements} for multiple times also works as
     * expected.) Note that if the {@code template_loader} have to be changed because of this, the template templateResolver will
     * be emptied.
     * 
     * @throws IllegalArgumentException
     *             If {@code incompatibleImmprovements} refers to a version that wasn't released yet when the currently
     *             used FreeMarker version was released, or is less than 2.3.0, or is {@code null}.
     * 
     * @since 2.3.20
     */
    public void setIncompatibleImprovements(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        
        if (!this.incompatibleImprovements.equals(incompatibleImprovements)) {
            this.incompatibleImprovements = incompatibleImprovements;
            
            if (!templateLoaderExplicitlySet) {
                templateLoaderExplicitlySet = true; 
                unsetTemplateLoader();
            }

            if (!templateLookupStrategyExplicitlySet) {
                templateLookupStrategyExplicitlySet = true;
                unsetTemplateLookupStrategy();
            }
            
            if (!templateNameFormatExplicitlySet) {
                templateNameFormatExplicitlySet = true;
                unsetTemplateNameFormat();
            }
            
            if (!cacheStorageExplicitlySet) {
                cacheStorageExplicitlySet = true;
                unsetCacheStorage();
            }
            
            if (!templateExceptionHandlerExplicitlySet) {
                templateExceptionHandlerExplicitlySet = true;
                unsetTemplateExceptionHandler();
            }
            
            if (!logTemplateExceptionsExplicitlySet) {
                logTemplateExceptionsExplicitlySet = true;
                unsetLogTemplateExceptions();
            }
            
            if (!objectWrapperExplicitlySet) {
                objectWrapperExplicitlySet = true;
                unsetObjectWrapper();
            }
            
            recreateTemplateResolver();
        }
    }

    /**
     * @see #setIncompatibleImprovements(Version)
     * @return Never {@code null}. 
     * @since 2.3.20
     */
    @Override
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }
    
    /**
     * Sets whether the FTL parser will try to remove
     * superfluous white-space around certain FTL tags.
     */
    public void setWhitespaceStripping(boolean b) {
        whitespaceStripping = b;
    }

    /**
     * Gets whether the FTL parser will try to remove
     * superfluous white-space around certain FTL tags.
     *
     * @see #setWhitespaceStripping
     */
    @Override
    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }

    /**
     * Sets when auto-escaping should be enabled depending on the current {@linkplain OutputFormat output format};
     * default is {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}. Note that the default output format,
     * {@link UndefinedOutputFormat}, is a non-escaping format, so there auto-escaping will be off.
     * Note that the templates can turn auto-escaping on/off locally with directives like {@code <#ftl auto_esc=...>},
     * which will ignore the policy.
     * 
     * <p><b>About auto-escaping</b></p>
     * 
     * <p>
     * Auto-escaping has significance when a value is printed with <code>${...}</code> (or <code>#{...}</code>). If
     * auto-escaping is on, FreeMarker will assume that the value is plain text (as opposed to markup or some kind of
     * rich text), so it will escape it according the current output format (see {@link #setOutputFormat(OutputFormat)}
     * and {@link TemplateConfiguration#setOutputFormat(OutputFormat)}). If auto-escaping is off, FreeMarker will assume
     * that the string value is already in the output format, so it prints it as is to the output.
     *
     * <p>Further notes on auto-escaping:
     * <ul>
     *   <li>When printing numbers, dates, and other kind of non-string values with <code>${...}</code>, they will be
     *       first converted to string (according the formatting settings and locale), then they are escaped just like
     *       string values.
     *   <li>When printing {@link TemplateMarkupOutputModel}-s, they aren't escaped again (they are already escaped).
     *   <li>Auto-escaping doesn't do anything if the current output format isn't an {@link MarkupOutputFormat}.
     *       That's the case for the default output format, {@link UndefinedOutputFormat}, and also for
     *       {@link PlainTextOutputFormat}.
     *   <li>The output format inside a string literal expression is always {@link PlainTextOutputFormat}
     *       (regardless of the output format of the containing template), which is a non-escaping format. Thus for
     *       example, with <code>&lt;#assign s = "foo${bar}"&gt;</code>, {@code bar} will always get into {@code s}
     *       without escaping, but with <code>&lt;#assign s&gt;foo${bar}&lt;#assign&gt;</code> it may will be escaped.
     * </ul>
     * 
     * <p>Note that what you set here is just a default, which can be overridden for individual templates via
     * {@link #setTemplateConfigurations(TemplateConfigurationFactory)}. This setting is also overridden by the standard file
     * extensions; see them at {@link #setRecognizeStandardFileExtensions(boolean)}.
     * 
     * @param autoEscapingPolicy
     *          One of the {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY},
     *          {@link #ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY}, and {@link #DISABLE_AUTO_ESCAPING_POLICY} constants.  
     * 
     * @see TemplateConfiguration#setAutoEscapingPolicy(int)
     * @see Configuration#setOutputFormat(OutputFormat)
     * @see TemplateConfiguration#setOutputFormat(OutputFormat)
     * 
     * @since 2.3.24
     */
    public void setAutoEscapingPolicy(int autoEscapingPolicy) {
        _TemplateAPI.validateAutoEscapingPolicyValue(autoEscapingPolicy);
        
        int prevAutoEscaping = getAutoEscapingPolicy();
        this.autoEscapingPolicy = autoEscapingPolicy;
        if (prevAutoEscaping != autoEscapingPolicy) {
            clearTemplateCache();
        }
    }

    /**
     * Getter pair of {@link #setAutoEscapingPolicy(int)}
     * 
     * @since 2.3.24
     */
    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy;
    }
    
    /**
     * Sets the default output format. Usually, you should leave this on its default, which is
     * {@link UndefinedOutputFormat#INSTANCE}, and then use standard file extensions like "ftlh" (for HTML) or "ftlx"
     * (for XML) and ensure that {@link #setRecognizeStandardFileExtensions(boolean)} is {@code true} (see more there).
     * Where you can't use the standard extensions, templates still can be associated to output formats with
     * patterns matching their name (their path) using {@link #setTemplateConfigurations(TemplateConfigurationFactory)}.
     * But if all templates will have the same output format, you may use {@link #setOutputFormat(OutputFormat)} after
     * all, to set a value like {@link HTMLOutputFormat#INSTANCE}, {@link XMLOutputFormat#INSTANCE}, etc. Also note
     * that templates can specify their own output format like {@code 
     * <#ftl output_format="HTML">}, which overrides any configuration settings.
     * 
     * <p>
     * The output format is mostly important because of auto-escaping (see {@link #setAutoEscapingPolicy(int)}), but
     * maybe also used by the embedding application to set the HTTP response MIME type, etc.
     * 
     * @see #setRegisteredCustomOutputFormats(Collection)
     * @see #setTemplateConfigurations(TemplateConfigurationFactory)
     * @see #setRecognizeStandardFileExtensions(boolean)
     * @see #setAutoEscapingPolicy(int)
     * 
     * @since 2.3.24
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        if (outputFormat == null) {
            throw new _NullArgumentException(
                    "outputFormat",
                    "You may meant: " + UndefinedOutputFormat.class.getSimpleName() + ".INSTANCE");
        }
        OutputFormat prevOutputFormat = getOutputFormat();
        this.outputFormat = outputFormat;
        outputFormatExplicitlySet = true;
        if (prevOutputFormat != outputFormat) {
            clearTemplateCache();
        }
    }

    /**
     * Getter pair of {@link #setOutputFormat(OutputFormat)}
     * 
     * @since 2.3.24
     */
    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }
    
    /**
     * Tells if {@link #setOutputFormat(OutputFormat)} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.24
     */
    public boolean isOutputFormatExplicitlySet() {
        return outputFormatExplicitlySet;
    }
    
    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also
     * {@link #isOutputFormatExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.24
     */
    public void unsetOutputFormat() {
        outputFormat = UndefinedOutputFormat.INSTANCE;
        outputFormatExplicitlySet = false;
    }
    
    /**
     * Returns the output format for a name.
     * 
     * @param name
     *            Either the name of the output format as it was registered with
     *            {@link Configuration#setRegisteredCustomOutputFormats(Collection)}, or a combined output format name.
     *            A output combined format is created ad-hoc from the registered formats. For example, if you need RTF
     *            embedded into HTML, the name will be <code>HTML{RTF}</code>, where "HTML" and "RTF" refer to the
     *            existing formats. This logic can be used recursively, so for example <code>XML{HTML{RTF}}</code> is
     *            also valid.
     * 
     * @return Not {@code null}.
     * 
     * @throws UnregisteredOutputFormatException
     *             If there's no output format registered with the given name.
     * @throws IllegalArgumentException
     *             If the usage of <code>{</code> and <code>}</code> in the name is syntactically wrong, or if not all
     *             {@link OutputFormat}-s are {@link MarkupOutputFormat}-s in the <code>...{...}</code> expression.
     * 
     * @since 2.3.24
     */
    public OutputFormat getOutputFormat(String name) throws UnregisteredOutputFormatException {
        if (name.length() == 0) {
            throw new IllegalArgumentException("0-length format name");
        }
        if (name.charAt(name.length() - 1) == '}') {
            // Combined markup
            int openBrcIdx = name.indexOf('{');
            if (openBrcIdx == -1) {
                throw new IllegalArgumentException("Missing opening '{' in: " + name);
            }
            
            MarkupOutputFormat outerOF = getMarkupOutputFormatForCombined(name.substring(0, openBrcIdx));
            MarkupOutputFormat innerOF = getMarkupOutputFormatForCombined(
                    name.substring(openBrcIdx + 1, name.length() - 1));
            
            return new CombinedMarkupOutputFormat(name, outerOF, innerOF);
        } else {
            OutputFormat custOF = registeredCustomOutputFormats.get(name);
            if (custOF != null) {
                return custOF;
            }
            
            OutputFormat stdOF = STANDARD_OUTPUT_FORMATS.get(name);
            if (stdOF == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unregistered output format name, ");
                sb.append(_StringUtil.jQuote(name));
                sb.append(". The output formats registered in the Configuration are: ");
                
                Set<String> registeredNames = new TreeSet<>();
                registeredNames.addAll(STANDARD_OUTPUT_FORMATS.keySet());
                registeredNames.addAll(registeredCustomOutputFormats.keySet());
                
                boolean first = true;
                for (String registeredName : registeredNames) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(_StringUtil.jQuote(registeredName));
                }
                
                throw new UnregisteredOutputFormatException(sb.toString());
            }
            return stdOF;
        }
    }

    private MarkupOutputFormat getMarkupOutputFormatForCombined(String outerName)
            throws UnregisteredOutputFormatException {
        OutputFormat of = getOutputFormat(outerName);
        if (!(of instanceof MarkupOutputFormat)) {
            throw new IllegalArgumentException("The \"" + outerName + "\" output format can't be used in "
                    + "...{...} expression, because it's not a markup format.");
        }
        return (MarkupOutputFormat) of;
    }
    
    /**
     * Sets the custom output formats that can be referred by their unique name ({@link OutputFormat#getName()}) from
     * templates. Names are also used to look up the {@link OutputFormat} for standard file extensions; see them at
     * {@link #setRecognizeStandardFileExtensions(boolean)}.
     * 
     * <p>
     * When there's a clash between a custom output format name and a standard output format name, the custom format
     * will win, thus you can override the meaning of standard output format names. Except, it's not allowed to override
     * {@link UndefinedOutputFormat} and {@link PlainTextOutputFormat}.
     * 
     * <p>
     * The default value is an empty collection.
     * 
     * @param registeredCustomOutputFormats
     *            The collection of the {@link OutputFormat}-s, each must be different and has a unique name (
     *            {@link OutputFormat#getName()}) within this collection.
     * 
     * @throws IllegalArgumentException
     *             When multiple different {@link OutputFormat}-s have the same name in the parameter collection. When
     *             the same {@link OutputFormat} object occurs for multiple times in the collection. If an
     *             {@link OutputFormat} name is 0 long. If an {@link OutputFormat} name doesn't start with letter or
     *             digit. If an {@link OutputFormat} name contains {@code '+'} or <code>'{'</code> or <code>'}'</code>.
     *             If an {@link OutputFormat} name equals to {@link UndefinedOutputFormat#getName()} or
     *             {@link PlainTextOutputFormat#getName()}.
     * 
     * @since 2.3.24
     */
    public void setRegisteredCustomOutputFormats(Collection<? extends OutputFormat> registeredCustomOutputFormats) {
        _NullArgumentException.check(registeredCustomOutputFormats);
        Map<String, OutputFormat> m = new LinkedHashMap<>(
                registeredCustomOutputFormats.size() * 4 / 3, 1f);
        for (OutputFormat outputFormat : registeredCustomOutputFormats) {
            String name = outputFormat.getName();
            if (name.equals(UndefinedOutputFormat.INSTANCE.getName())) {
                throw new IllegalArgumentException(
                        "The \"" + name + "\" output format can't be redefined");
            }
            if (name.equals(PlainTextOutputFormat.INSTANCE.getName())) {
                throw new IllegalArgumentException(
                        "The \"" + name + "\" output format can't be redefined");
            }
            if (name.length() == 0) {
                throw new IllegalArgumentException("The output format name can't be 0 long");
            }
            if (!Character.isLetterOrDigit(name.charAt(0))) {
                throw new IllegalArgumentException("The output format name must start with letter or digit: "
                        + name);
            }
            if (name.indexOf('+') != -1) {
                throw new IllegalArgumentException("The output format name can't contain \"+\" character: "
                        + name);
            }
            if (name.indexOf('{') != -1) {
                throw new IllegalArgumentException("The output format name can't contain \"{\" character: "
                        + name);
            }
            if (name.indexOf('}') != -1) {
                throw new IllegalArgumentException("The output format name can't contain \"}\" character: "
                        + name);
            }
            
            OutputFormat replaced = m.put(outputFormat.getName(), outputFormat);
            if (replaced != null) {
                if (replaced == outputFormat) {
                    throw new IllegalArgumentException(
                            "Duplicate output format in the collection: " + outputFormat);
                }
                throw new IllegalArgumentException(
                        "Clashing output format names between " + replaced + " and " + outputFormat + ".");
            }
        }
        this.registeredCustomOutputFormats = Collections.unmodifiableMap(m);
        
        clearTemplateCache();
    }
    
    /**
     * Getter pair of {@link #setRegisteredCustomOutputFormats(Collection)}.
     * 
     * @since 2.3.24
     */
    public Collection<? extends OutputFormat> getRegisteredCustomOutputFormats() {
        return registeredCustomOutputFormats.values();
    }

    /**
     * Sets if the "file" extension part of the source name ({@link Template#getSourceName()}) will influence certain
     * parsing settings. For backward compatibility, it defaults to {@code false} if
     * {@link #getIncompatibleImprovements()} is less than 2.3.24. Starting from {@code incompatibleImprovements}
     * 2.3.24, it defaults to {@code true}, so the following standard file extensions take their effect:
     * 
     * <ul>
     *   <li>{@code ftlh}: Sets {@link TemplateConfiguration#setOutputFormat(OutputFormat) outputFormat} to
     *       {@code "HTML"} (i.e., {@link HTMLOutputFormat#INSTANCE}, unless the {@code "HTML"} name is overridden by
     *       {@link #setRegisteredCustomOutputFormats(Collection)}) and
     *       {@link TemplateConfiguration#setAutoEscapingPolicy(int) autoEscapingPolicy} to
     *       {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}.
     *   <li>{@code ftlx}: Sets {@link TemplateConfiguration#setOutputFormat(OutputFormat) outputFormat} to
     *       {@code "XML"} (i.e., {@link XMLOutputFormat#INSTANCE}, unless the {@code "XML"} name is overridden by
     *       {@link #setRegisteredCustomOutputFormats(Collection)}) and
     *       {@link TemplateConfiguration#setAutoEscapingPolicy(int) autoEscapingPolicy} to
     *       {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}.
     * </ul>
     * 
     * <p>These file extensions are not case sensitive. The file extension is the part after the last dot in the source
     * name. If the source name contains no dot, then it has no file extension.
     * 
     * <p>The settings activated by these file extensions override the setting values dictated by
     * {@link #setTemplateConfigurations(TemplateConfigurationFactory)}.
     */
    public void setRecognizeStandardFileExtensions(boolean recognizeStandardFileExtensions) {
        boolean prevEffectiveValue = getRecognizeStandardFileExtensions();
        this.recognizeStandardFileExtensions = Boolean.valueOf(recognizeStandardFileExtensions);
        if (prevEffectiveValue != recognizeStandardFileExtensions) {
            clearTemplateCache();
        }
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isRecognizeStandardFileExtensionsExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.24
     */
    public void unsetRecognizeStandardFileExtensions() {
        if (recognizeStandardFileExtensions != null) {
            recognizeStandardFileExtensions = null;
        }
    }
    
    /**
     * Tells if {@link #setRecognizeStandardFileExtensions(boolean)} (or equivalent) was already called on this
     * instance.
     * 
     * @since 2.3.24
     */
    public boolean isRecognizeStandardFileExtensionsExplicitlySet() {
        return recognizeStandardFileExtensions != null;
    }
    
    /**
     * Getter pair of {@link #setRecognizeStandardFileExtensions(boolean)}.
     * 
     * @since 2.3.24
     */
    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return recognizeStandardFileExtensions == null
                ? true
                : recognizeStandardFileExtensions.booleanValue();
    }

    /**
     * Determines the syntax of the template files (angle bracket VS square bracket)
     * that has no {@code #ftl} in it. The {@code tagSyntax}
     * parameter must be one of:
     * <ul>
     *   <li>{@link Configuration#AUTO_DETECT_TAG_SYNTAX}:
     *     use the syntax of the first FreeMarker tag (can be anything, like <tt>#list</tt>,
     *     <tt>#include</tt>, user defined, etc.)
     *   <li>{@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}:
     *     use the angle bracket syntax (the normal syntax)
     *   <li>{@link Configuration#SQUARE_BRACKET_TAG_SYNTAX}:
     *     use the square bracket syntax
     * </ul>
     *
     * <p>In FreeMarker 2.3.x {@link Configuration#ANGLE_BRACKET_TAG_SYNTAX} is the
     * default for better backward compatibility. Starting from 2.4.x {@link
     * Configuration#AUTO_DETECT_TAG_SYNTAX} is the default, so it's recommended to use
     * that even for 2.3.x.
     * 
     * <p>This setting is ignored for the templates that have {@code ftl} directive in
     * it. For those templates the syntax used for the {@code ftl} directive determines
     * the syntax.
     */
    public void setTagSyntax(int tagSyntax) {
        _TemplateAPI.valideTagSyntaxValue(tagSyntax);
        this.tagSyntax = tagSyntax;
    }

    /**
     * The getter pair of {@link #setTagSyntax(int)}.
     */
    @Override
    public int getTagSyntax() {
        return tagSyntax;
    }

    /**
     * Sets the naming convention used for the identifiers that are part of the template language. The available naming
     * conventions are legacy (directive (tag) names are all-lower-case {@code likethis}, others are snake case
     * {@code like_this}), and camel case ({@code likeThis}). The default is auto-detect, which detects the naming
     * convention used and enforces that same naming convention for the whole template.
     * 
     * <p>
     * This setting doesn't influence what naming convention is used for the setting names outside templates. Also, it
     * won't ever convert the names of user-defined things, like of data-model members, or the names of user defined
     * macros/functions. It only influences the names of the built-in directives ({@code #elseIf} VS {@code elseif}),
     * built-ins ({@code ?upper_case} VS {@code ?upperCase} ), special variables ({@code .data_model} VS
     * {@code .dataModel}).
     * 
     * <p>
     * Which convention to use: FreeMarker prior to 2.3.23 has only supported
     * {@link Configuration#LEGACY_NAMING_CONVENTION}, so that's how most templates and examples out there are written
     * as of 2015. But as templates today are mostly written by programmers and often access Java API-s which already
     * use camel case, {@link Configuration#CAMEL_CASE_NAMING_CONVENTION} is the recommended option for most projects.
     * However, it's no necessary to make a application-wide decision; see auto-detection below.
     * 
     * <p>
     * FreeMarker will decide the naming convention automatically for each template individually when this setting is
     * set to {@link #AUTO_DETECT_NAMING_CONVENTION} (which is the default). The naming convention of a template is
     * decided when the first core (non-user-defined) identifier is met during parsing (not during processing) where the
     * naming convention is relevant (like for {@code s?upperCase} or {@code s?upper_case} it's relevant, but for
     * {@code s?length} it isn't). At that point, the naming convention of the template is decided, and any later core
     * identifier that uses a different convention will be a parsing error. As the naming convention is decided per
     * template, it's not a problem if a template and the other template it {@code #include}-s/{@code #import} uses a
     * different convention.
     * 
     * <p>
     * FreeMarker always enforces the same naming convention to be used consistently within the same template "file".
     * Additionally, when this setting is set to non-{@link #AUTO_DETECT_NAMING_CONVENTION}, the selected naming
     * convention is enforced on all templates. Thus such a setup can be used to enforce an application-wide naming
     * convention.
     * 
     * @param namingConvention
     *            One of the {@link #AUTO_DETECT_NAMING_CONVENTION} or {@link #LEGACY_NAMING_CONVENTION}
     *            {@link #CAMEL_CASE_NAMING_CONVENTION}.
     * 
     * @throws IllegalArgumentException
     *             If the parameter isn't one of the valid constants.
     * 
     * @since 2.3.23
     */
    public void setNamingConvention(int namingConvention) {
        _TemplateAPI.validateNamingConventionValue(namingConvention);
        this.namingConvention = namingConvention;
    }

    /**
     * The getter pair of {@link #setNamingConvention(int)}.
     * 
     * @since 2.3.23
     */
    @Override
    public int getNamingConvention() {
        return namingConvention;
    }
    
    /**
     * Sets the assumed display width of the tab character (ASCII 9), which influences the column number shown in error
     * messages (or the column number you get through other API-s). So for example if the users edit templates in an
     * editor where the tab width is set to 4, you should set this to 4 so that the column numbers printed by FreeMarker
     * will match the column number shown in the editor. This setting doesn't affect the output of templates, as a tab
     * in the template will remain a tab in the output too.
     * 
     * @param tabSize
     *            At least 1, at most 256.
     * 
     * @since 2.3.25
     */
    public void setTabSize(int tabSize) {
        if (tabSize < 1) {
           throw new IllegalArgumentException("\"tabSize\" must be at least 1, but was " + tabSize);
        }
        // To avoid integer overflows:
        if (tabSize > 256) {
            throw new IllegalArgumentException("\"tabSize\" can be more than 256, but was " + tabSize);
        }
        this.tabSize = tabSize;
    }

    /**
     * The getter pair of {@link #setTabSize(int)}.
     * 
     * @since 2.3.25
     */
    @Override
    public int getTabSize() {
        return tabSize;
    }
    
    /**
     * Retrieves the template with the given name from the template templateResolver, loading it into the templateResolver first if it's
     * missing/staled.
     * 
     * <p>
     * This is a shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, null, null, null, true, false)}; see more details there.
     * 
     * <p>
     * See {@link Configuration} for an example of basic usage.
     */
    public Template getTemplate(String name)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, null, null, null, true, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, locale, null, null, true, false)}.
     */
    public Template getTemplate(String name, Locale locale)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, null, null, true, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, null, null, encoding, true, false)}.
     */
    public Template getTemplate(String name, String encoding)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, null, null, encoding, true, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, locale, null, encoding, true, false)}.
     */
    public Template getTemplate(String name, Locale locale, String encoding)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, null, encoding, true, false);
    }
    
    /**
     * Shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, locale, null, encoding, parseAsFTL, false)}.
     */
    public Template getTemplate(String name, Locale locale, String encoding, boolean parseAsFTL)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, null, encoding, parseAsFTL, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Object, String, boolean, boolean)
     * getTemplate(name, locale, null, encoding, parseAsFTL, ignoreMissing)}.
     * 
     * @since 2.3.21
     */
    public Template getTemplate(String name, Locale locale, String encoding, boolean parseAsFTL, boolean ignoreMissing)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, null, encoding, parseAsFTL, ignoreMissing);
    }
    
    /**
     * Retrieves the template with the given name (and according the specified further parameters) from the template
     * templateResolver, loading it into the templateResolver first if it's missing/staled.
     * 
     * <p>
     * This method is thread-safe.
     * 
     * <p>
     * See {@link Configuration} for an example of basic usage.
     *
     * @param name
     *            The name or path of the template, which is not a real path, but interpreted inside the current
     *            {@link TemplateLoader}. Can't be {@code null}. The exact syntax of the name depends on the underlying
     *            {@link TemplateLoader}, but the templateResolver makes some assumptions. First, the name is expected to be a
     *            hierarchical path, with path components separated by a slash character (not with backslash!). The path
     *            (the name) given here must <em>not</em> begin with slash; it's always interpreted relative to the
     *            "template root directory". Then, the {@code ..} and {@code .} path meta-elements will be resolved. For
     *            example, if the name is {@code a/../b/./c.ftl}, then it will be simplified to {@code b/c.ftl}. The
     *            rules regarding this are the same as with conventional UN*X paths. The path must not reach outside the
     *            template root directory, that is, it can't be something like {@code "../templates/my.ftl"} (not even
     *            if this path happens to be equivalent with {@code "/my.ftl"}). Furthermore, the path is allowed to
     *            contain at most one path element whose name is {@code *} (asterisk). This path meta-element triggers
     *            the <i>acquisition mechanism</i>. If the template is not found in the location described by the
     *            concatenation of the path left to the asterisk (called base path) and the part to the right of the
     *            asterisk (called resource path), the templateResolver will attempt to remove the rightmost path component from
     *            the base path ("go up one directory") and concatenate that with the resource path. The process is
     *            repeated until either a template is found, or the base path is completely exhausted.
     *
     * @param locale
     *            The requested locale of the template. This is what {@link Template#getLocale()} on the resulting
     *            {@link Template} will return (unless it's overridden via {@link #getTemplateConfigurations()}). This
     *            parameter can be {@code null} since 2.3.22, in which case it defaults to
     *            {@link Configuration#getLocale()} (note that {@link Template#getLocale()} will give the default value,
     *            not {@code null}). This parameter also drives localized template lookup. Assuming that you have
     *            specified {@code en_US} as the locale and {@code myTemplate.ftl} as the name of the template, and the
     *            default {@link TemplateLookupStrategy} is used and
     *            {@code #setLocalizedLookup(boolean) localized_lookup} is {@code true}, FreeMarker will first try to
     *            retrieve {@code myTemplate_en_US.html}, then {@code myTemplate.en.ftl}, and finally
     *            {@code myTemplate.ftl}. Note that that the template's locale will be {@code en_US} even if it only
     *            finds {@code myTemplate.ftl}. Note that when the {@code locale} setting is overridden with a
     *            {@link TemplateConfiguration} provided by {@link #getTemplateConfigurations()}, that overrides the
     *            value specified here, but only after the localized lookup, that is, it modifies the template
     *            found by the localized lookup.
     * 
     * @param customLookupCondition
     *            This value can be used by a custom {@link TemplateLookupStrategy}; has no effect with the default one.
     *            Can be {@code null} (though it's up to the custom {@link TemplateLookupStrategy} if it allows that).
     *            This object will be used as part of the templateResolver key, so it must to have a proper
     *            {@link Object#equals(Object)} and {@link Object#hashCode()} method. It also should have reasonable
     *            {@link Object#toString()}, as it's possibly quoted in error messages. The expected type is up to the
     *            custom {@link TemplateLookupStrategy}. See also:
     *            {@link TemplateLookupContext#getCustomLookupCondition()}.
     *
     * @param encoding
     *            Deprecated mechanism, {@code null} is the recommended; the charset used to interpret the template
     *            source code bytes (if it's read from a binary source). Can be {@code null} since 2.3.22, in which case
     *            it will default to {@link Configuration#getEncoding(Locale)} where {@code Locale} is the
     *            {@code locale} parameter (when {@code locale} was {@code null} too, the its default value is used
     *            instead). Why is this deprecated: It doesn't make sense to get the <em>same</em> template with
     *            different encodings, hence, it's error prone to specify the encoding where you get the template.
     *            Instead, if you have template "files" with different charsets, you should use
     *            {@link #setTemplateConfigurations(TemplateConfigurationFactory)}, where you can associate encodings to
     *            individual templates based on their names (like which "directory" are they in, what's their file
     *            extension, etc.). The encoding associated with the templates that way overrides the encoding that you
     *            specify here.
     *
     * @param parseAsFTL
     *            If {@code true}, the loaded template is parsed and interpreted normally, as a regular FreeMarker
     *            template. If {@code false}, the loaded template is treated as a static text, so <code>${...}</code>,
     *            {@code <#...>} etc. will not have special meaning in it.
     * 
     * @param ignoreMissing
     *            If {@code true}, the method won't throw {@link TemplateNotFoundException} if the template doesn't
     *            exist, instead it returns {@code null}. Other kind of exceptions won't be suppressed.
     * 
     * @return the requested template; maybe {@code null} when the {@code ignoreMissing} parameter is {@code true}.
     * 
     * @throws TemplateNotFoundException
     *             If the template could not be found. Note that this exception extends {@link IOException}.
     * @throws MalformedTemplateNameException
     *             If the template name given was in violation with the {@link TemplateNameFormat} in use. Note that
     *             this exception extends {@link IOException}.
     * @throws ParseException
     *             (extends <code>IOException</code>) if the template is syntactically bad. Note that this exception
     *             extends {@link IOException}.
     * @throws IOException
     *             If there was some other problem with reading the template "file". Note that the other exceptions
     *             extend {@link IOException}, so this should be catched the last.
     * 
     * @since 2.3.22
     */
    public Template getTemplate(String name, Locale locale, Object customLookupCondition,
            String encoding, boolean parseAsFTL, boolean ignoreMissing)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        if (locale == null) {
            locale = getLocale();
        }
        if (encoding == null) {
            encoding = getEncoding(locale);
        }
        
        final GetTemplateResult maybeTemp = templateResolver.getTemplate(name, locale, customLookupCondition, encoding, parseAsFTL);
        final Template temp = maybeTemp.getTemplate();
        if (temp == null) {
            if (ignoreMissing) {
                return null;
            }
            
            TemplateLoader tl = getTemplateLoader();  
            String msg; 
            if (tl == null) {
                msg = "Don't know where to load template " + _StringUtil.jQuote(name)
                      + " from because the \"template_loader\" FreeMarker "
                      + "setting wasn't set (Configuration.setTemplateLoader), so it's null.";
            } else {
                final String missingTempNormName = maybeTemp.getMissingTemplateNormalizedName();
                final String missingTempReason = maybeTemp.getMissingTemplateReason();
                final TemplateLookupStrategy templateLookupStrategy = getTemplateLookupStrategy();
                msg = "Template not found for name " + _StringUtil.jQuote(name)
                        + (missingTempNormName != null && name != null
                                && !removeInitialSlash(name).equals(missingTempNormName)
                                ? " (normalized: " + _StringUtil.jQuote(missingTempNormName) + ")"
                                : "")
                        + (customLookupCondition != null ? " and custom lookup condition "
                        + _StringUtil.jQuote(customLookupCondition) : "")
                        + "."
                        + (missingTempReason != null
                                ? "\nReason given: " + ensureSentenceIsClosed(missingTempReason)
                                : "")
                        + "\nThe name was interpreted by this TemplateLoader: "
                        + _StringUtil.tryToString(tl) + "."
                        + (!isKnownNonConfusingLookupStrategy(templateLookupStrategy)
                                ? "\n(Before that, the name was possibly changed by this lookup strategy: "
                                  + _StringUtil.tryToString(templateLookupStrategy) + ".)"
                                : "")
                        // Suspected reasons or warning:
                        + (!templateLoaderExplicitlySet
                                ? "\nWarning: The \"template_loader\" FreeMarker setting "
                                  + "wasn't set (Configuration.setTemplateLoader), and using the default value "
                                  + "is most certainly not intended and dangerous, and can be the cause of this error."
                                : "")
                        + (missingTempReason == null && name.indexOf('\\') != -1
                                ? "\nWarning: The name contains backslash (\"\\\") instead of slash (\"/\"); "
                                    + "template names should use slash only."
                                : "");
            }
            
            String normName = maybeTemp.getMissingTemplateNormalizedName();
            throw new TemplateNotFoundException(
                    normName != null ? normName : name,
                    customLookupCondition,
                    msg);
        }
        return temp;
    }
    
    private boolean isKnownNonConfusingLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
        return templateLookupStrategy == DefaultTemplateLookupStrategy.INSTANCE;
    }

    private String removeInitialSlash(String name) {
        return name.startsWith("/") ? name.substring(1) : name;
    }

    private String ensureSentenceIsClosed(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        
        final char lastChar = s.charAt(s.length() - 1);
        return lastChar == '.' || lastChar == '!' || lastChar == '?' ? s : s + ".";
    }

    /**
     * Sets the charset used for decoding byte sequences to character sequences when
     * reading template files in a locale for which no explicit encoding
     * was specified via {@link #setEncoding(Locale, String)}. Note that by default there is no locale specified for
     * any locale, so the default encoding is always in effect.
     * 
     * <p>Defaults to the default system encoding, which can change from one server to
     * another, so <b>you should always set this setting</b>. If you don't know what charset your should chose,
     * {@code "UTF-8"} is usually a good choice.
     * 
     * <p>Note that individual templates may specify their own charset by starting with
     * <tt>&lt;#ftl encoding="..."&gt;</tt>
     * 
     * @param encoding The name of the charset, such as {@code "UTF-8"} or {@code "ISO-8859-1"}
     */
    public void setDefaultEncoding(String encoding) {
        defaultEncoding = encoding;
    }

    /**
     * Gets the default encoding for converting bytes to characters when
     * reading template files in a locale for which no explicit encoding
     * was specified. Defaults to the default system encoding.
     */
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Gets the preferred character encoding for the given locale, or the 
     * default encoding if no encoding is set explicitly for the specified
     * locale. You can associate encodings with locales using 
     * {@link #setEncoding(Locale, String)} or {@link #loadBuiltInEncodingMap()}.
     */
    public String getEncoding(Locale locale) {
        if (localeToCharsetMap.isEmpty()) {
            return defaultEncoding;
        } else {
            // Try for a full name match (may include country and variant)
            String charset = (String) localeToCharsetMap.get(locale.toString());
            if (charset == null) {
                if (locale.getVariant().length() > 0) {
                    Locale l = new Locale(locale.getLanguage(), locale.getCountry());
                    charset = (String) localeToCharsetMap.get(l.toString());
                    if (charset != null) {
                        localeToCharsetMap.put(locale.toString(), charset);
                    }
                } 
                charset = (String) localeToCharsetMap.get(locale.getLanguage());
                if (charset != null) {
                    localeToCharsetMap.put(locale.toString(), charset);
                }
            }
            return charset != null ? charset : defaultEncoding;
        }
    }

    /**
     * Sets the character set encoding to use for templates of
     * a given locale. If there is no explicit encoding set for some
     * locale, then the default encoding will be used, what you can
     * set with {@link #setDefaultEncoding}.
     *
     * @see #clearEncodingMap
     * @see #loadBuiltInEncodingMap
     */
    public void setEncoding(Locale locale, String encoding) {
        localeToCharsetMap.put(locale.toString(), encoding);
    }

    /**
     * Adds a shared variable to the configuration.
     * Shared sharedVariables are sharedVariables that are visible
     * as top-level sharedVariables for all templates which use this
     * configuration, if the data model does not contain a
     * variable with the same name.
     *
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared sharedVariables,
     * if the configuration is used by multiple threads! It is the typical situation for Servlet based Web sites.
     * 
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values. 
     *
     * @param name the name used to access the data object from your template.
     *     If a shared variable with this name already exists, it will replace
     *     that.
     *     
     * @see #setAllSharedVariables
     * @see #setSharedVariable(String,Object)
     */
    public void setSharedVariable(String name, TemplateModel tm) {
        Object replaced = sharedVariables.put(name, tm);
        if (replaced != null && rewrappableSharedVariables != null) {
            rewrappableSharedVariables.remove(name);
        }
    }

    /**
     * Returns the set containing the names of all defined shared sharedVariables.
     * The method returns a new Set object on each call that is completely
     * disconnected from the Configuration. That is, modifying the set will have
     * no effect on the Configuration object.
     */
    public Set getSharedVariableNames() {
        return new HashSet(sharedVariables.keySet());
    }
    
    /**
     * Adds shared variable to the configuration; It uses {@link Configurable#getObjectWrapper()} to wrap the 
     * {@code value}, so it's important that the object wrapper is set before this.
     * 
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values.
     * 
     * <p>The added value should be thread safe, if you are running templates from multiple threads with this
     * configuration.
     *
     * @throws TemplateModelException If some of the variables couldn't be wrapped via {@link #getObjectWrapper()}.
     *
     * @see #setSharedVaribles(Map)
     * @see #setSharedVariable(String,TemplateModel)
     * @see #setAllSharedVariables(TemplateHashModelEx)
     */
    public void setSharedVariable(String name, Object value) throws TemplateModelException {
        setSharedVariable(name, getObjectWrapper().wrap(value));
    }
    
    /**
     * Replaces all shared variables (removes all previously added ones).
     *
     * <p>The values in the map can be {@link TemplateModel}-s or plain Java objects which will be immediately converted
     * to {@link TemplateModel} with the {@link ObjectWrapper} returned by {@link #getObjectWrapper()}. If
     * {@link #setObjectWrapper(ObjectWrapper)} is called later, this conversion will be re-applied. Thus, ignoring some
     * extra resource usage, it doesn't mater if in what order are {@link #setObjectWrapper(ObjectWrapper)} and
     * {@link #setSharedVaribles(Map)} called. This is essential when you don't have control over the order in which
     * the setters are called. 
     *
     * <p>The values in the map must be thread safe, if you are running templates from multiple threads with
     * this configuration. This means that both the plain Java object and the {@link TemplateModel}-s created from them
     * by the {@link ObjectWrapper} must be thread safe. (The standard {@link ObjectWrapper}-s of FreeMarker create
     * thread safe {@link TemplateModel}-s.) The {@link Map} itself need not be thread-safe.
     * 
     * <p>This setter method has no getter pair because of the tricky relation ship with
     * {@link #setSharedVariable(String, Object)}.
     * 
     * @throws TemplateModelException If some of the variables couldn't be wrapped via {@link #getObjectWrapper()}.
     *  
     * @since 2.3.21
     */
    public void setSharedVaribles(Map/*<String, Object>*/ map) throws TemplateModelException {
        rewrappableSharedVariables = new HashMap(map);
        sharedVariables.clear();
        setSharedVariablesFromRewrappableSharedVariables();
    }

    private void setSharedVariablesFromRewrappableSharedVariables() throws TemplateModelException {
        if (rewrappableSharedVariables == null) return;
        for (Entry<String, Object> ent : rewrappableSharedVariables.entrySet()) {
            String name = ent.getKey();
            Object value = ent.getValue();

            TemplateModel valueAsTM;
            if (value instanceof TemplateModel) {
                valueAsTM = (TemplateModel) value;
            } else {
                valueAsTM = getObjectWrapper().wrap(value);
            }
            sharedVariables.put(name, valueAsTM);
        }
    }

    /**
     * Adds all object in the hash as shared variable to the configuration; it's like doing several
     * {@link #setSharedVariable(String, Object)} calls, one for each hash entry. It doesn't remove the already added
     * shared variable before doing this.
     *
     * <p>Never use <tt>TemplateModel</tt> implementation that is not thread-safe for shared shared variable values,
     * if the configuration is used by multiple threads! It is the typical situation for Servlet based Web sites.
     *
     * <p>This method is <b>not</b> thread safe; use it with the same restrictions as those that modify setting values. 
     *
     * @param hash a hash model whose objects will be copied to the
     * configuration with same names as they are given in the hash.
     * If a shared variable with these names already exist, it will be replaced
     * with those from the map.
     *
     * @see #setSharedVaribles(Map)
     * @see #setSharedVariable(String,Object)
     * @see #setSharedVariable(String,TemplateModel)
     */
    public void setAllSharedVariables(TemplateHashModelEx hash) throws TemplateModelException {
        TemplateModelIterator keys = hash.keys().iterator();
        TemplateModelIterator values = hash.values().iterator();
        while (keys.hasNext()) {
            setSharedVariable(((TemplateScalarModel) keys.next()).getAsString(), values.next());
        }
    }
    
    /**
     * Gets a shared variable. Shared sharedVariables are sharedVariables that are 
     * available to all templates. When a template is processed, and an identifier
     * is undefined in the data model, a shared variable object with the same identifier
     * is then looked up in the configuration. There are several predefined sharedVariables
     * that are always available through this method, see the FreeMarker manual
     * for a comprehensive list of them.
     *
     * @see #setSharedVariable(String,Object)
     * @see #setSharedVariable(String,TemplateModel)
     * @see #setAllSharedVariables
     */
    public TemplateModel getSharedVariable(String name) {
        return (TemplateModel) sharedVariables.get(name);
    }
    
    /**
     * Removes all shared sharedVariables, except the predefined ones (compress, html_escape, etc.).
     */
    public void clearSharedVariables() {
        sharedVariables.clear();
        loadBuiltInSharedVariables();
    }
    
    /**
     * Removes all entries from the template templateResolver, thus forcing reloading of templates
     * on subsequent <code>getTemplate</code> calls.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     */
    public void clearTemplateCache() {
        templateResolver.clearTemplateCache();
    }
    
    /**
     * Equivalent to <tt>removeTemplateFromCache(name, thisCfg.getLocale(), thisCfg.getEncoding(thisCfg.getLocale()), true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name) throws IOException {
        Locale loc = getLocale();
        removeTemplateFromCache(name, loc, getEncoding(loc), true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, locale, thisCfg.getEncoding(locale), true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale) throws IOException {
        removeTemplateFromCache(name, locale, getEncoding(locale), true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, thisCfg.getLocale(), encoding, true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, String encoding) throws IOException {
        removeTemplateFromCache(name, getLocale(), encoding, true);
    }

    /**
     * Equivalent to <tt>removeTemplateFromCache(name, locale, encoding, true)</tt>.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale, String encoding) throws IOException {
        removeTemplateFromCache(name, locale, encoding, true);
    }
    
    /**
     * Removes a template from the template templateResolver, hence forcing the re-loading
     * of it when it's next time requested. This is to give the application
     * finer control over templateResolver updating than {@link #setTemplateUpdateDelayMilliseconds(long)}
     * alone does.
     * 
     * <p>For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, String, boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     * 
     * @since 2.3.19
     */
    public void removeTemplateFromCache(
            String name, Locale locale, String encoding, boolean parse)
    throws IOException {
        templateResolver.removeTemplateFromCache(name, locale, encoding, parse);
    }    
    
    /**
     * The getter pair of {@link #setLocalizedLookup(boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine works.
     */
    public boolean getLocalizedLookup() {
        return templateResolver.getLocalizedLookup();
    }
    
    /**
     * Enables/disables localized template lookup. Enabled by default.
     * 
     * <p>
     * With the default {@link TemplateLookupStrategy}, localized lookup works like this: Let's say your locale setting
     * is {@code Locale("en", "AU")}, and you call {@link Configuration#getTemplate(String) cfg.getTemplate("foo.ftl")}.
     * Then FreeMarker will look for the template under these names, stopping at the first that exists:
     * {@code "foo_en_AU.ftl"}, {@code "foo_en.ftl"}, {@code "foo.ftl"}. See the description of the default value at
     * {@link #setTemplateLookupStrategy(TemplateLookupStrategy)} for a more details. If you need to generate different
     * template names, use {@link #setTemplateLookupStrategy(TemplateLookupStrategy)} with your custom
     * {@link TemplateLookupStrategy}.
     * 
     * <p>Note that changing the value of this setting causes the template templateResolver to be emptied so that old lookup
     * results won't be reused (since 2.3.22). 
     * 
     * <p>
     * Historical note: Despite what the API documentation said earlier, this method is <em>not</em> thread-safe. While
     * setting it can't cause any serious problems, and in fact it works well on most hardware, it's not guaranteed that
     * FreeMarker will see the update in all threads.
     */
    public void setLocalizedLookup(boolean localizedLookup) {
        this.localizedLookup = localizedLookup;
        templateResolver.setLocalizedLookup(localizedLookup);
    }
    
    @Override
    public void setSetting(String name, String value) throws ConfigurationException {
        boolean unknown = false;
        try {
            if ("TemplateUpdateInterval".equalsIgnoreCase(name)) {
                name = TEMPLATE_UPDATE_DELAY_KEY;
            } else if ("DefaultEncoding".equalsIgnoreCase(name)) {
                name = DEFAULT_ENCODING_KEY;
            }
            
            if (DEFAULT_ENCODING_KEY_SNAKE_CASE.equals(name) || DEFAULT_ENCODING_KEY_CAMEL_CASE.equals(name)) {
                setDefaultEncoding(value);
            } else if (LOCALIZED_LOOKUP_KEY_SNAKE_CASE.equals(name) || LOCALIZED_LOOKUP_KEY_CAMEL_CASE.equals(name)) {
                setLocalizedLookup(_StringUtil.getYesNo(value));
            } else if (WHITESPACE_STRIPPING_KEY_SNAKE_CASE.equals(name)
                    || WHITESPACE_STRIPPING_KEY_CAMEL_CASE.equals(name)) {
                setWhitespaceStripping(_StringUtil.getYesNo(value));
            } else if (AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE.equals(name) || AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE.equals(name)) {
                if ("enable_if_default".equals(value) || "enableIfDefault".equals(value)) {
                    setAutoEscapingPolicy(ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
                } else if ("enable_if_supported".equals(value) || "enableIfSupported".equals(value)) {
                    setAutoEscapingPolicy(ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
                } else if ("disable".equals(value)) {
                    setAutoEscapingPolicy(DISABLE_AUTO_ESCAPING_POLICY);
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (OUTPUT_FORMAT_KEY_SNAKE_CASE.equals(name) || OUTPUT_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetOutputFormat();
                } else {
                    setOutputFormat((OutputFormat) _ObjectBuilderSettingEvaluator.eval(
                            value, OutputFormat.class, true, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE.equals(name)
                    || REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE.equals(name)) {
                List list = (List) _ObjectBuilderSettingEvaluator.eval(
                        value, List.class, true, _SettingEvaluationEnvironment.getCurrent());
                for (Object item : list) {
                    if (!(item instanceof OutputFormat)) {
                        throw new _MiscTemplateException(getEnvironment(),
                                "Invalid value for setting ", new _DelayedJQuote(name), ": List items must be "
                                + OutputFormat.class.getName() + " intances, in: ", value);
                    }
                }
                setRegisteredCustomOutputFormats(list);
            } else if (RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE.equals(name)
                    || RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetRecognizeStandardFileExtensions();
                } else {
                    setRecognizeStandardFileExtensions(_StringUtil.getYesNo(value));
                }
            } else if (CACHE_STORAGE_KEY_SNAKE_CASE.equals(name) || CACHE_STORAGE_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetCacheStorage();
                } if (value.indexOf('.') == -1) {
                    int strongSize = 0;
                    int softSize = 0;
                    Map map = _StringUtil.parseNameValuePairList(
                            value, String.valueOf(Integer.MAX_VALUE));
                    Iterator it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry ent = (Map.Entry) it.next();
                        String pname = (String) ent.getKey();
                        int pvalue;
                        try {
                            pvalue = Integer.parseInt((String) ent.getValue());
                        } catch (NumberFormatException e) {
                            throw invalidSettingValueException(name, value);
                        }
                        if ("soft".equalsIgnoreCase(pname)) {
                            softSize = pvalue;
                        } else if ("strong".equalsIgnoreCase(pname)) {
                            strongSize = pvalue;
                        } else {
                            throw invalidSettingValueException(name, value);
                        }
                    }
                    if (softSize == 0 && strongSize == 0) {
                        throw invalidSettingValueException(name, value);
                    }
                    setCacheStorage(new MruCacheStorage(strongSize, softSize));
                } else {
                    setCacheStorage((CacheStorage) _ObjectBuilderSettingEvaluator.eval(
                            value, CacheStorage.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_UPDATE_DELAY_KEY_CAMEL_CASE.equals(name)) {
                final String valueWithoutUnit;
                final String unit;
                int numberEnd = 0;
                while (numberEnd < value.length() && !Character.isAlphabetic(value.charAt(numberEnd))) {
                    numberEnd++;
                }
                valueWithoutUnit = value.substring(0, numberEnd).trim();
                unit = value.substring(numberEnd).trim();
                
                final long multipier;
                if (unit.equals("ms")) {
                    multipier = 1;
                } else if (unit.equals("s")) {
                    multipier = 1000;
                } else if (unit.equals("m")) {
                    multipier = 1000 * 60;
                } else if (unit.equals("h")) {
                    multipier = 1000 * 60 * 60;
                } else if (!unit.isEmpty()) {
                    throw invalidSettingValueException(name, value,
                            "Unrecognized time unit " + _StringUtil.jQuote(unit) + ". Valid units are: ms, s, m, h");
                } else {
                    multipier = 0;
                }
                
                int parsedValue = Integer.parseInt(valueWithoutUnit);
                if (multipier == 0 && parsedValue != 0) {
                    throw invalidSettingValueException(name, value,
                            "Time unit must be specified for a non-0 value (examples: 500 ms, 3 s, 2 m, 1 h).");
                }
                
                setTemplateUpdateDelayMilliseconds(parsedValue * multipier);
            } else if (TAG_SYNTAX_KEY_SNAKE_CASE.equals(name) || TAG_SYNTAX_KEY_CAMEL_CASE.equals(name)) {
                if ("auto_detect".equals(value) || "autoDetect".equals(value)) {
                    setTagSyntax(AUTO_DETECT_TAG_SYNTAX);
                } else if ("angle_bracket".equals(value) || "angleBracket".equals(value)) {
                    setTagSyntax(ANGLE_BRACKET_TAG_SYNTAX);
                } else if ("square_bracket".equals(value) || "squareBracket".equals(value)) {
                    setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (NAMING_CONVENTION_KEY_SNAKE_CASE.equals(name) || NAMING_CONVENTION_KEY_CAMEL_CASE.equals(name)) {
                if ("auto_detect".equals(value) || "autoDetect".equals(value)) {
                    setNamingConvention(AUTO_DETECT_NAMING_CONVENTION);
                } else if ("legacy".equals(value)) {
                    setNamingConvention(LEGACY_NAMING_CONVENTION);
                } else if ("camel_case".equals(value) || "camelCase".equals(value)) {
                    setNamingConvention(CAMEL_CASE_NAMING_CONVENTION);
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (TAB_SIZE_KEY_SNAKE_CASE.equals(name) || TAB_SIZE_KEY_CAMEL_CASE.equals(name)) {
                setTabSize(Integer.parseInt(value));
            } else if (INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE.equals(name)
                    || INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE.equals(name)) {
                setIncompatibleImprovements(new Version(value));
            } else if (TEMPLATE_LOADER_KEY_SNAKE_CASE.equals(name) || TEMPLATE_LOADER_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetTemplateLoader();
                } else {
                    setTemplateLoader((TemplateLoader) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateLoader.class, true, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetTemplateLookupStrategy();
                } else {
                    setTemplateLookupStrategy((TemplateLookupStrategy) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateLookupStrategy.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetTemplateNameFormat();
                } else if (value.equalsIgnoreCase("default_2_3_0")) {
                    setTemplateNameFormat(DefaultTemplateNameFormatFM2.INSTANCE);
                } else if (value.equalsIgnoreCase("default_2_4_0")) {
                    setTemplateNameFormat(DefaultTemplateNameFormat.INSTANCE);
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE.equals(name)) {
                if (value.equals(NULL)) {
                    setTemplateConfigurations(null);
                } else {
                    setTemplateConfigurations((TemplateConfigurationFactory) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateConfigurationFactory.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else {
                unknown = true;
            }
        } catch (Exception e) {
            throw settingValueAssignmentException(name, value, e);
        }
        if (unknown) {
            super.setSetting(name, value);
        }
    }

    private String rightTrim(String s) {
        int ln = s.length();
        while (ln > 0 && Character.isWhitespace(s.charAt(ln - 1))) {
            ln--;
        }
        return s.substring(0, ln);
    }

    /**
     * Returns the valid {@link Configuration} setting names. Naturally, this includes the {@link Configurable} setting
     * names too.
     * 
     * @param camelCase
     *            If we want the setting names with camel case naming convention, or with snake case (legacy) naming
     *            convention.
     * 
     * @see Configurable#getSettingNames(boolean)
     * 
     * @since 2.3.24
     */
    @Override
    public Set<String> getSettingNames(boolean camelCase) {
        return new _UnmodifiableCompositeSet<>(
                super.getSettingNames(camelCase),
                new _SortedArraySet<>(camelCase ? SETTING_NAMES_CAMEL_CASE : SETTING_NAMES_SNAKE_CASE));
    }
    
    @Override
    protected Version getRemovalVersionForUnknownSetting(String name) {
        if (name.equals("strictSyntax") || name.equals("strict_syntax")) {
            return Configuration.VERSION_3_0_0;
        }
        return super.getRemovalVersionForUnknownSetting(name);
    }

    @Override
    protected String getCorrectedNameForUnknownSetting(String name) {
        if ("encoding".equals(name) || "charset".equals(name) || "default_charset".equals(name)) {
            // [2.4] Default might changes to camel-case
            return DEFAULT_ENCODING_KEY;
        }
        if ("defaultCharset".equals(name)) {
            return DEFAULT_ENCODING_KEY_CAMEL_CASE;
        }
        if (name.equals("incompatible_enhancements")) {
            return INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE;
        }
        if (name.equals("incompatibleEnhancements")) {
            return INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE;
        }
        
        return super.getCorrectedNameForUnknownSetting(name);
    }
    
    @Override
    protected void doAutoImportsAndIncludes(Environment env) throws TemplateException, IOException {
        Template t = env.getMainTemplate();
        doAutoImports(env, t);
        doAutoIncludes(env, t);
    }

    private void doAutoImports(Environment env, Template t) throws IOException, TemplateException {
        Map<String, String> envAutoImports = env.getAutoImportsWithoutFallback();
        Map<String, String> tAutoImports = t.getAutoImportsWithoutFallback();
        
        boolean lazyAutoImports = env.getLazyAutoImports() != null ? env.getLazyAutoImports().booleanValue()
                : env.getLazyImports();
        
        for (Map.Entry<String, String> autoImport : getAutoImportsWithoutFallback().entrySet()) {
            String nsVarName = autoImport.getKey();
            if ((tAutoImports == null || !tAutoImports.containsKey(nsVarName))
                    && (envAutoImports == null || !envAutoImports.containsKey(nsVarName))) {
                env.importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
            }
        }
        if (tAutoImports != null) {
            for (Map.Entry<String, String> autoImport : tAutoImports.entrySet()) {
                String nsVarName = autoImport.getKey();
                if (envAutoImports == null || !envAutoImports.containsKey(nsVarName)) {
                    env.importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
                }
            }
        }
        if (envAutoImports != null) {
            for (Map.Entry<String, String> autoImport : envAutoImports.entrySet()) {
                String nsVarName = autoImport.getKey();
                env.importLib(autoImport.getValue(), nsVarName, lazyAutoImports);
            }
        }
    }
    
    private void doAutoIncludes(Environment env, Template t) throws TemplateException, IOException {
        // We can't store autoIncludes in LinkedHashSet-s because setAutoIncludes(List) allows duplicates,
        // unfortunately. Yet we have to prevent duplicates among Configuration levels, with the lowest levels having
        // priority. So we build some Set-s to do that, but we avoid the most common cases where they aren't needed.
        
        List<String> tAutoIncludes = t.getAutoIncludesWithoutFallback();
        List<String> envAutoIncludes = env.getAutoIncludesWithoutFallback();
        
        for (String templateName : getAutoIncludesWithoutFallback()) {
            if ((tAutoIncludes == null || !tAutoIncludes.contains(templateName))
                    && (envAutoIncludes == null || !envAutoIncludes.contains(templateName))) {
                env.include(getTemplate(templateName, env.getLocale()));
            }
        }
        
        if (tAutoIncludes != null) {
            for (String templateName : tAutoIncludes) {
                if (envAutoIncludes == null || !envAutoIncludes.contains(templateName)) {
                    env.include(getTemplate(templateName, env.getLocale()));
                }
            }
        }
        
        if (envAutoIncludes != null) {
            for (String templateName : envAutoIncludes) {
                env.include(getTemplate(templateName, env.getLocale()));
            }
        }
    }
    
    /**
     * Returns the FreeMarker version information, most importantly the major.minor.micro version numbers.
     * 
     * On FreeMarker version numbering rules:
     * <ul>
     *   <li>For final/stable releases the version number is like major.minor.micro, like 2.3.19. (Historically,
     *       when micro was 0 the version strings was like major.minor instead of the proper major.minor.0, but that's
     *       not like that anymore.)
     *   <li>When only the micro version is increased, compatibility with previous versions with the same
     *       major.minor is kept. Thus <tt>freemarker.jar</tt> can be replaced in an existing application without
     *       breaking it.</li>
     *   <li>For non-final/unstable versions (that almost nobody uses), the format is:
     *       <ul>
     *         <li>Starting from 2.3.20: major.minor.micro-extraInfo, like
     *             2.3.20-nightly_20130506T123456Z, 2.4.0-RC01. The major.minor.micro
     *             always indicates the target we move towards, so 2.3.20-nightly or 2.3.20-M01 is
     *             after 2.3.19 and will eventually become to 2.3.20. "PRE", "M" and "RC" (uppercase!) means
     *             "preview", "milestone" and "release candidate" respectively, and is always followed by a 2 digit
     *             0-padded counter, like M03 is the 3rd milestone release of a given major.minor.micro.</li> 
     *         <li>Before 2.3.20: The extraInfo wasn't preceded by a "-".
     *             Instead of "nightly" there was "mod", where the major.minor.micro part has indicated where
     *             are we coming from, so 2.3.19mod (read as: 2.3.19 modified) was after 2.3.19 but before 2.3.20.
     *             Also, "pre" and "rc" was lowercase, and was followd by a number without 0-padding.</li>
     *       </ul>
     * </ul>
     * 
     * @since 2.3.20
     */ 
    public static Version getVersion() {
        return VERSION;
    }
    
    /**
     * Returns the default object wrapper for a given "incompatible_improvements" version.
     * 
     * @see #setIncompatibleImprovements(Version)
     * 
     * @since 2.3.21
     */
    public static ObjectWrapper getDefaultObjectWrapper(Version incompatibleImprovements) {
        return new DefaultObjectWrapperBuilder(incompatibleImprovements).build();
    }

    /**
     * Same as {@link #getSupportedBuiltInNames(int)} with argument {@link #getNamingConvention()}.
     * 
     * @since 2.3.20
     */
    public Set getSupportedBuiltInNames() {
        return getSupportedBuiltInNames(getNamingConvention());
    }

    /**
     * Returns the names of the supported "built-ins". These are the ({@code expr?builtin_name}-like things). As of this
     * writing, this information doesn't depend on the configuration options, so it could be a static method, but
     * to be future-proof, it's an instance method. 
     * 
     * @param namingConvention
     *            One of {@link #AUTO_DETECT_NAMING_CONVENTION}, {@link #LEGACY_NAMING_CONVENTION}, and
     *            {@link #CAMEL_CASE_NAMING_CONVENTION}. If it's {@link #AUTO_DETECT_NAMING_CONVENTION} then the union
     *            of the names in all the naming conventions is returned. 
     * 
     * @since 2.3.24
     */
    public Set<String> getSupportedBuiltInNames(int namingConvention) {
        return _CoreAPI.getSupportedBuiltInNames(namingConvention);
    }
    
    /**
     * Same as {@link #getSupportedBuiltInDirectiveNames(int)} with argument {@link #getNamingConvention()}.
     * 
     * @since 2.3.21
     */
    public Set getSupportedBuiltInDirectiveNames() {
        return getSupportedBuiltInDirectiveNames(getNamingConvention());
    }

    /**
     * Returns the names of the directives that are predefined by FreeMarker. These are the things that you call like
     * <tt>&lt;#directiveName ...&gt;</tt>.
     * 
     * @param namingConvention
     *            One of {@link #AUTO_DETECT_NAMING_CONVENTION}, {@link #LEGACY_NAMING_CONVENTION}, and
     *            {@link #CAMEL_CASE_NAMING_CONVENTION}. If it's {@link #AUTO_DETECT_NAMING_CONVENTION} then the union
     *            of the names in all the naming conventions is returned. 
     * 
     * @since 2.3.24
     */
    public Set<String> getSupportedBuiltInDirectiveNames(int namingConvention) {
        if (namingConvention == AUTO_DETECT_NAMING_CONVENTION) {
            return _CoreAPI.ALL_BUILT_IN_DIRECTIVE_NAMES;
        } else if (namingConvention == LEGACY_NAMING_CONVENTION) {
            return _CoreAPI.LEGACY_BUILT_IN_DIRECTIVE_NAMES;
        } else if (namingConvention == CAMEL_CASE_NAMING_CONVENTION) {
            return _CoreAPI.CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES;
        } else {
            throw new IllegalArgumentException("Unsupported naming convention constant: " + namingConvention);
        }
    }
    
    private static String getRequiredVersionProperty(Properties vp, String properyName) {
        String s = vp.getProperty(properyName);
        if (s == null) {
            throw new RuntimeException(
                    "Version file is corrupt: \"" + properyName + "\" property is missing.");
        }
        return s;
    }

}

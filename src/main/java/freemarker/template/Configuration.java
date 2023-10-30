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

package freemarker.template;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URLConnection;
import java.text.DecimalFormat;
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
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import freemarker.cache.CacheStorage;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MruCacheStorage;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.SoftCacheStorage;
import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateCache.MaybeMissingTemplate;
import freemarker.cache.TemplateConfigurationFactory;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.cache.TemplateNameFormat;
import freemarker.cache.URLTemplateLoader;
import freemarker.core.BugException;
import freemarker.core.CFormat;
import freemarker.core.CSSOutputFormat;
import freemarker.core.CombinedMarkupOutputFormat;
import freemarker.core.Configurable;
import freemarker.core.Environment;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.JSONOutputFormat;
import freemarker.core.JavaScriptOrJSONCFormat;
import freemarker.core.JavaScriptOutputFormat;
import freemarker.core.LegacyCFormat;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.OutputFormat;
import freemarker.core.ParseException;
import freemarker.core.ParserConfiguration;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.RTFOutputFormat;
import freemarker.core.TemplateConfiguration;
import freemarker.core.TemplateMarkupOutputModel;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.UnregisteredOutputFormatException;
import freemarker.core.XHTMLOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.core.XSCFormat;
import freemarker.core._CoreAPI;
import freemarker.core._DelayedJQuote;
import freemarker.core._MiscTemplateException;
import freemarker.core._ObjectBuilderSettingEvaluator;
import freemarker.core._SettingEvaluationEnvironment;
import freemarker.core._SortedArraySet;
import freemarker.core._UnmodifiableCompositeSet;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.log.Logger;
import freemarker.template.utility.CaptureOutput;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.Constants;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.NormalizeNewlines;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.SecurityUtilities;
import freemarker.template.utility.StandardCompress;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility.XmlEscape;

/**
 * <b>The main entry point into the FreeMarker API</b>; encapsulates the configuration settings of FreeMarker,
 * also serves as a central template-loading and caching service.
 *
 * <p>This class is meant to be used in a singleton pattern. That is, you create an instance of this at the beginning of
 * the application life-cycle, set its {@link #setSetting(String, String) configuration settings} there (either with the
 * setter methods like {@link #setTemplateLoader(TemplateLoader)} or by loading a {@code .properties} file), and then
 * use that single instance everywhere in your application. Frequently re-creating {@link Configuration} is a typical
 * and grave mistake from performance standpoint, as the {@link Configuration} holds the template cache, and often also
 * the class introspection cache, which then will be lost. (Note that, naturally, having multiple long-lived instances,
 * like one per component that internally uses FreeMarker is fine.)  
 * 
 * <p>The basic usage pattern is like:
 * 
 * <pre>
 *  // Where the application is initialized; in general you do this ONLY ONCE in the application life-cycle!
 *  Configuration cfg = new Configuration(VERSION_<i>X</i>_<i>Y</i>_<i>Z</i>));
 *  // Where VERSION_<i>X</i>_<i>Y</i>_<i>Z</i> enables the not-100%-backward-compatible fixes introduced in
 *  // FreeMarker version X.Y.Z  and earlier (see {@link #Configuration(Version)}).
 *  cfg.set<i>SomeSetting</i>(...);
 *  cfg.set<i>OtherSetting</i>(...);
 *  ...
 *  
 *  // Later, whenever the application needs a template (so you may do this a lot, and from multiple threads):
 *  {@link Template Template} myTemplate = cfg.{@link #getTemplate(String) getTemplate}("myTemplate.ftlh");
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
    
    private static final Logger CACHE_LOG = Logger.getLogger("freemarker.cache");
    
    private static final String VERSION_PROPERTIES_PATH = "/freemarker/version.properties";
    
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
    public static final String STRICT_SYNTAX_KEY_SNAKE_CASE = "strict_syntax";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String STRICT_SYNTAX_KEY_CAMEL_CASE = "strictSyntax";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String STRICT_SYNTAX_KEY = STRICT_SYNTAX_KEY_SNAKE_CASE;
    
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
    
    /**
     * Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23
     * @deprecated Use {@link Configurable#AUTO_IMPORT_KEY_SNAKE_CASE} instead.
     */
    public static final String AUTO_IMPORT_KEY_SNAKE_CASE = "auto_import";
    /**
     * Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23
     * @deprecated Use {@link Configurable#AUTO_IMPORT_KEY_CAMEL_CASE} instead.
     */
    public static final String AUTO_IMPORT_KEY_CAMEL_CASE = "autoImport";
    /**
     * Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints.
     * @deprecated Use {@link Configurable#AUTO_IMPORT_KEY_SNAKE_CASE} instead.
     */
    public static final String AUTO_IMPORT_KEY = AUTO_IMPORT_KEY_SNAKE_CASE;
    
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

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.28 */
    public static final String INTERPOLATION_SYNTAX_KEY_SNAKE_CASE = "interpolation_syntax";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.28 */
    public static final String INTERPOLATION_SYNTAX_KEY_CAMEL_CASE = "interpolationSyntax";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String INTERPOLATION_SYNTAX_KEY = INTERPOLATION_SYNTAX_KEY_SNAKE_CASE;
    
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
    
    /** @deprecated Use {@link #INCOMPATIBLE_IMPROVEMENTS_KEY} instead. */
    @Deprecated
    public static final String INCOMPATIBLE_IMPROVEMENTS = INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE;
    /** @deprecated Use {@link #INCOMPATIBLE_IMPROVEMENTS_KEY} instead. */
    @Deprecated
    public static final String INCOMPATIBLE_ENHANCEMENTS = "incompatible_enhancements";

    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.29 */
    public static final String FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_SNAKE_CASE = "fallback_on_null_loop_variable";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.29 */
    public static final String FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_CAMEL_CASE = "fallbackOnNullLoopVariable";
    /** Alias to the {@code ..._SNAKE_CASE} variation. @since 2.3.25 */
    public static final String FALLBACK_ON_NULL_LOOP_VARIABLE_KEY = FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_SNAKE_CASE;

    private static final String[] SETTING_NAMES_SNAKE_CASE = new String[] {
        // Must be sorted alphabetically!
        AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE,
        CACHE_STORAGE_KEY_SNAKE_CASE,
        DEFAULT_ENCODING_KEY_SNAKE_CASE,
        FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_SNAKE_CASE,
        INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE,
        INTERPOLATION_SYNTAX_KEY_SNAKE_CASE,
        LOCALIZED_LOOKUP_KEY_SNAKE_CASE,
        NAMING_CONVENTION_KEY_SNAKE_CASE,
        OUTPUT_FORMAT_KEY_SNAKE_CASE,
        RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE,
        REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE,
        STRICT_SYNTAX_KEY_SNAKE_CASE,
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
        FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_CAMEL_CASE,
        INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE,
        INTERPOLATION_SYNTAX_KEY_CAMEL_CASE,
        LOCALIZED_LOOKUP_KEY_CAMEL_CASE,
        NAMING_CONVENTION_KEY_CAMEL_CASE,
        OUTPUT_FORMAT_KEY_CAMEL_CASE,
        RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE,
        REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
        STRICT_SYNTAX_KEY_CAMEL_CASE,
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

    /**
     * The parser decides between {@link #ANGLE_BRACKET_TAG_SYNTAX} and {@link #SQUARE_BRACKET_TAG_SYNTAX} based on the
     * first tag (like {@code [#if x]} or {@code <#if x>}) it mets. Note that {@code [=...]} is <em>not</em> a tag, but
     * an interpolation, so it's not used for tag syntax auto-detection.
     */
    public static final int AUTO_DETECT_TAG_SYNTAX = 0;
    
    /** For example {@code <#if x><@foo /></#if>} */
    public static final int ANGLE_BRACKET_TAG_SYNTAX = 1;
    
    /**
     * For example {@code [#if x][@foo /][/#if]}.
     * It does <em>not</em> change <code>${x}</code> to {@code [=x]}; that's square bracket <em>interpolation</em>
     * syntax ({@link #SQUARE_BRACKET_INTERPOLATION_SYNTAX}).
     */
    public static final int SQUARE_BRACKET_TAG_SYNTAX = 2;

    /** <code>${expression}</code> and the deprecated <code>#{expression; numFormat}</code> @since 2.3.28 */
    public static final int LEGACY_INTERPOLATION_SYNTAX = 20;
    
    /** <code>${expression}</code> only (not <code>#{expression; numFormat}</code>) @since 2.3.28 */
    public static final int DOLLAR_INTERPOLATION_SYNTAX = 21;
    
    /**
     * <code>[=expression]</code> instead of <code>${expression}</code>.
     * It does <em>not</em> change {@code <#if x>} to {@code [#if x]}; that's square bracket <em>tag</em> syntax
     * ({@link #SQUARE_BRACKET_TAG_SYNTAX}).
     * @since 2.3.28
     */
    public static final int SQUARE_BRACKET_INTERPOLATION_SYNTAX = 22;
    
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
    
    /** FreeMarker version 2.3.0 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_0 = new Version(2, 3, 0);
    
    /** FreeMarker version 2.3.19 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_19 = new Version(2, 3, 19);
    
    /** FreeMarker version 2.3.20 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_20 = new Version(2, 3, 20);
    
    /** FreeMarker version 2.3.21 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_21 = new Version(2, 3, 21);

    /** FreeMarker version 2.3.22 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_22 = new Version(2, 3, 22);

    /** FreeMarker version 2.3.23 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_23 = new Version(2, 3, 23);

    /** FreeMarker version 2.3.24 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_24 = new Version(2, 3, 24);

    /** FreeMarker version 2.3.25 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_25 = new Version(2, 3, 25);

    /** FreeMarker version 2.3.26 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_26 = new Version(2, 3, 26);

    /** FreeMarker version 2.3.27 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_27 = new Version(2, 3, 27);

    /** FreeMarker version 2.3.28 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_28 = new Version(2, 3, 28);

    /** FreeMarker version 2.3.29 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_29 = new Version(2, 3, 29);

    /** FreeMarker version 2.3.30 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_30 = new Version(2, 3, 30);

    /** FreeMarker version 2.3.31 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_31 = new Version(2, 3, 31);

    /** FreeMarker version 2.3.32 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_32 = new Version(2, 3, 32);

    /** FreeMarker version 2.3.33 (an {@link #Configuration(Version) incompatible improvements break-point}) */
    public static final Version VERSION_2_3_33 = new Version(2, 3, 33);

    /** The default of {@link #getIncompatibleImprovements()}, currently {@link #VERSION_2_3_0}. */
    public static final Version DEFAULT_INCOMPATIBLE_IMPROVEMENTS = Configuration.VERSION_2_3_0;
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    @Deprecated
    public static final String DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.toString();
    /** @deprecated Use {@link #DEFAULT_INCOMPATIBLE_IMPROVEMENTS} instead. */
    @Deprecated
    public static final int PARSED_DEFAULT_INCOMPATIBLE_ENHANCEMENTS = DEFAULT_INCOMPATIBLE_IMPROVEMENTS.intValue(); 
    
    private static final String NULL = "null";
    private static final String DEFAULT = "default";
    private static final String JVM_DEFAULT = "JVM default";
    
    private static final Version VERSION;
    static {
        try {
            Properties props = ClassUtil.loadProperties(Configuration.class, VERSION_PROPERTIES_PATH);
            
            String versionString  = getRequiredVersionProperty(props, "version");
            
            Date buildDate;
            {
                String buildDateStr = getRequiredVersionProperty(props, "buildTimestamp");
                if (buildDateStr.endsWith("Z")) {
                    buildDateStr = buildDateStr.substring(0, buildDateStr.length() - 1) + "+0000";
                }
                try {
                    buildDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(buildDateStr);
                } catch (java.text.ParseException e) {
                    buildDate = null;
                }
            }
            
            final Boolean gaeCompliant = Boolean.valueOf(getRequiredVersionProperty(props, "isGAECompliant"));
            
            VERSION = new Version(versionString, gaeCompliant, buildDate);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load and parse " + VERSION_PROPERTIES_PATH, e);
        }
    }
    
    private static final String FM_24_DETECTION_CLASS_NAME = "freemarker.core._2_4_OrLaterMarker";
    private static final boolean FM_24_DETECTED;
    static {
        boolean fm24detected;
        try {
            Class.forName(FM_24_DETECTION_CLASS_NAME);
            fm24detected = true;
        } catch (ClassNotFoundException e) {
            fm24detected = false;
        } catch (LinkageError e) {
            fm24detected = true;
        } catch (Throwable e) {
            // Unexpected. We assume that there's no clash.
            fm24detected = false;
        }
        FM_24_DETECTED = fm24detected;
    }
    
    private final static Object defaultConfigLock = new Object();

    private static volatile Configuration defaultConfig;

    private boolean strictSyntax = true;
    private volatile boolean localizedLookup = true;
    private boolean whitespaceStripping = true;
    private int autoEscapingPolicy = ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY;
    private OutputFormat outputFormat = UndefinedOutputFormat.INSTANCE;
    private boolean outputFormatExplicitlySet;
    private Boolean recognizeStandardFileExtensions;
    private Map<String, ? extends OutputFormat> registeredCustomOutputFormats = Collections.emptyMap(); 
    private Version incompatibleImprovements;
    private int tagSyntax = ANGLE_BRACKET_TAG_SYNTAX;
    private int interpolationSyntax = LEGACY_INTERPOLATION_SYNTAX;
    private int namingConvention = AUTO_DETECT_NAMING_CONVENTION;
    private int tabSize = 8;  // Default from JavaCC 3.x
    private boolean fallbackOnNullLoopVariable = true;  // Default for backward compatibility
    private boolean preventStrippings;

    private TemplateCache cache;
    
    private boolean templateLoaderExplicitlySet;
    private boolean templateLookupStrategyExplicitlySet;
    private boolean templateNameFormatExplicitlySet;
    private boolean cacheStorageExplicitlySet;
    
    private boolean objectWrapperExplicitlySet;
    private boolean templateExceptionHandlerExplicitlySet;
    private boolean attemptExceptionReporterExplicitlySet;
    private boolean logTemplateExceptionsExplicitlySet;
    private boolean wrapUncheckedExceptionsExplicitlySet;
    private boolean localeExplicitlySet;
    private boolean defaultEncodingExplicitlySet;
    private boolean timeZoneExplicitlySet;
    private boolean cFormatExplicitlySet;

    
    private HashMap/*<String, TemplateModel>*/ sharedVariables = new HashMap();

    /**
     * Needed so that it doesn't mater in what order do you call {@link #setSharedVaribles(Map)}
     * and {@link #setObjectWrapper(ObjectWrapper)}. When the user configures FreeMarker from Spring XML, he has no
     * control over the order, so it has to work on both ways.
     */
    private HashMap/*<String, Object>*/ rewrappableSharedVariables = null;
    
    private String defaultEncoding = getDefaultDefaultEncoding();
    private ConcurrentMap localeToCharsetMap = new ConcurrentHashMap();
    
    /**
     * Same as {@link #Configuration(Version) Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)}.
     * 
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
     * improvements that you want to enable were already implemented. In new projects you should set this to the fixed
     * FreeMarker version that you start the development with. In older projects it's also usually better to keep
     * this high, however you should check the changes activated (find them below), especially if not only the 3rd
     * version number (the micro version) of {@code incompatibleImprovements} is increased. Generally, as far as you
     * only increase the last version number of this setting, the changes are low risk. The default value is 2.3.0 to
     * maximize backward compatibility, but that value isn't recommended.
     * 
     * <p>Bugfixes and improvements that are fully backward compatible, also those that are important security fixes,
     * are enabled regardless of the incompatible improvements setting.
     *
     * <p>Do NOT ever use {@link #getVersion()} to set the "incompatible improvements". Always use a fixed value, like
     * {@link #VERSION_2_3_30}. Otherwise your application can break as you upgrade FreeMarker. (As of 2.3.30, doing
     * this will be logged as an error. As of 2.4.0, it will be probably disallowed, by throwing exception.)
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
     *     2.3.0: This is the lowest supported value, the version used in very old projects. This is the default in the
     *     FreeMarker 2.3.x series (the one used by the deprecated {@link #Configuration()} constructor) for maximum
     *     backward compatibility.
     *   </li>
     *   <li><p>
     *     2.3.19 (or higher): Bug fix: Wrong {@code #} tags were printed as static text instead of
     *     causing parsing error when there was no correct {@code #} or {@code @} tag earlier in the
     *     same template.
     *   </li>
     *   <li><p>
     *     2.3.20 (or higher): {@code ?html} will escape apostrophe-quotes just like {@code ?xhtml} does. Utilizing
     *     this is highly recommended, because otherwise if interpolations are used inside attribute values that use
     *     apostrophe-quotation (<tt>&lt;foo bar='${val}'&gt;</tt>) instead of plain quotation mark
     *     (<tt>&lt;foo bar="${val}"&gt;</tt>), they might produce HTML/XML that's not well-formed. Note that
     *     {@code ?html} didn't do this because long ago there was no cross-browser way of doing this, but it's not a
     *     concern anymore.
     *   </li>
     *   <li><p>
     *     2.3.21 (or higher):
     *     <ul>
     *       <li><p>
     *         The <em>default</em> of the {@code object_wrapper} setting ({@link #getObjectWrapper()}) changes from
     *         {@link ObjectWrapper#DEFAULT_WRAPPER} to another almost identical {@link DefaultObjectWrapper} singleton,
     *         returned by {@link DefaultObjectWrapperBuilder#build()}. The new default object wrapper's
     *         "incompatible improvements" version is set to the same as of the {@link Configuration}.
     *         See {@link BeansWrapper#BeansWrapper(Version)} for further details. Furthermore, the new default
     *         object wrapper doesn't allow changing its settings; setter methods throw {@link IllegalStateException}).
     *         (If anything tries to call setters on the old default in your application, that's a dangerous bug that
     *         won't remain hidden now. As the old default is a singleton too, potentially shared by independently
     *         developed components, most of them expects the out-of-the-box behavior from it (and the others are
     *         necessarily buggy). Also, then concurrency glitches can occur (and even pollute the class introspection
     *         cache) because the singleton is modified after publishing to other threads.)
     *         Furthermore the new default object wrapper shares class introspection cache with other
     *         {@link BeansWrapper}-s created with {@link BeansWrapperBuilder}, which has an impact as
     *         {@link BeansWrapper#clearClassIntrospectionCache()} will be disallowed; see more about it there.
     *       </li>
     *       <li><p>
     *          The {@code ?iso_...} built-ins won't show the time zone offset for {@link java.sql.Time} values anymore,
     *          because most databases store time values that aren't in any time zone, but just store hour, minute,
     *          second, and decimal second field values. If you still want to show the offset (like for PostgreSQL
     *          "time with time zone" columns you should), you can force showing the time zone offset by using
     *          {@code myTime?string.iso_fz} (and its other variants).
     *       </li>
     *       <li><p>{@code ?is_enumerable} correctly returns {@code false} for Java methods get from Java objects that
     *         are wrapped with {@link BeansWrapper} and its subclasses, like {@link DefaultObjectWrapper}. Although
     *         method values implement {@link TemplateSequenceModel} (because of a historical design quirk in
     *         {@link BeansWrapper}), trying to {@code #list} them will cause error, hence they aren't enumerable.
     *       </li>
     *       <li><p>
     *          {@code ?c} will return {@code "INF"}, {@code "-INF"} and {@code "NaN"} for positive/negative infinity
     *          and IEEE floating point Not-a-Number, respectively. These are the XML Schema compatible representations
     *          of these special values. Earlier it has returned what {@link DecimalFormat} did with US locale, none of
     *          which was understood by any (common) computer language.
     *       </li>
     *       <li><p>
     *          FTL hash literals that repeat keys now only have the key once with {@code ?keys}, and only has the last
     *          value associated to that key with {@code ?values}. This is consistent with the behavior of
     *          {@code hash[key]} and how maps work in Java.       
     *       </li>
     *       <li><p>In most cases (where FreeMarker is able to do that), for {@link TemplateLoader}-s that use
     *         {@link URLConnection}, {@code URLConnection#setUseCaches(boolean)} will called with {@code false},
     *         so that only FreeMarker will do caching, not the URL scheme's handler.
     *         See {@link URLTemplateLoader#setURLConnectionUsesCaches(Boolean)} for more details.
     *       </li>
     *       <li><p>
     *         The default of the {@code template_loader} setting ({@link Configuration#getTemplateLoader()}) changes
     *         to {@code null}, which means that FreeMarker will not find any templates. Earlier
     *         the default was a {@link FileTemplateLoader} that used the current directory as the root. This was
     *         dangerous and fragile as you usually don't have good control over what the current directory will be.
     *         Luckily, the old default almost never looked for the templates at the right place
     *         anyway, so pretty much all applications had to set the {@code template_loader} setting, so it's unlikely
     *         that changing the default breaks your application.
     *       </li>
     *       <li><p>
     *          Right-unlimited ranges become readable (like listable), so {@code <#list 1.. as i>...</#list>} works.
     *          Earlier they were only usable for slicing (like {@code hits[10..]}).
     *       </li>
     *       <li><p>
     *          Empty ranges return {@link Constants#EMPTY_SEQUENCE} instead of an empty {@link SimpleSequence}. This
     *          is in theory backward compatible, as the API only promises to give something that implements
     *          {@link TemplateSequenceModel}.
     *       </li>
     *       <li><p>
     *          Unclosed comments ({@code <#-- ...}) and {@code #noparse}-s won't be silently closed at the end of
     *          template anymore, but cause a parsing error instead.
     *       </li>
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.22 (or higher):
     *     <ul>
     *       <li><p>
     *          {@link DefaultObjectWrapper} has some substantial changes with {@code incompatibleImprovements} 2.3.22;
     *          check them out at {@link DefaultObjectWrapper#DefaultObjectWrapper(Version)}. It's important to know
     *          that if you set the {@code object_wrapper} setting (to an other value than {@code "default"}), rather
     *          than leaving it on its default value, the {@code object_wrapper} won't inherit the
     *          {@code incompatibleImprovements} of the {@link Configuration}. In that case, if you want the 2.3.22
     *          improvements of {@link DefaultObjectWrapper}, you have to set it in the {@link DefaultObjectWrapper}
     *          object itself too! (Note that it's OK to use a {@link DefaultObjectWrapper} with a different
     *          {@code incompatibleImprovements} version number than that of the {@link Configuration}, if that's
     *          really what you want.)
     *       </li>
     *       <li><p>
     *          In templates, {@code .template_name} will <em>always</em> return the main (top level) template's name.
     *          It won't be affected by {@code #include} and {@code #nested} anymore. This is unintended, a bug with
     *          {@code incompatible_improvement} 2.3.22 (a consequence of the lower level fixing described in the next
     *          point). The old behavior of {@code .template_name} is restored if you set
     *          {@code incompatible_improvement} to 2.3.23 (while {@link Configurable#getParent()} of
     *          {@link Environment} keeps the changed behavior shown in the next point). 
     *       </li>
     *       <li><p>
     *          {@code #include} and {@code #nested} doesn't change the parent {@link Template} (see
     *          {@link Configurable#getParent()}) of the {@link Environment} anymore to the {@link Template} that's
     *          included or whose namespace {@code #nested} "returns" to. Thus, the parent of {@link Environment} will
     *          be now always the main {@link Template}. (The main {@link Template} is the {@link Template} whose
     *          {@code process} or {@code createProcessingEnvironment} method was called to initiate the output
     *          generation.) Note that apart from the effect on FTL's {@code .template_name} (see
     *          previous point), this should only matter if you have set settings directly on {@link Template} objects,
     *          and almost nobody does that. Also note that macro calls have never changed the {@link Environment}
     *          parent to the {@link Template} that contains the macro definition, so this mechanism was always broken.
     *          As now we consistently never change the parent, the behavior when calling macros didn't change.
     *       </li>
     *       <li><p>
     *          When using {@code freemarker.ext.servlet.FreemarkerServlet}:
     *          <ul>
     *             <li>
     *               <p>When using custom JSP tag libraries: Fixes bug where some kind of
     *               values, when put into the JSP <em>page</em> scope (via {@code #global} or via the JSP
     *               {@code PageContext} API) and later read back with the JSP {@code PageContext} API (typically in a
     *               custom JSP tag), might come back as FreeMarker {@link TemplateModel} objects instead of as objects
     *               with a standard Java type. Other Servlet scopes aren't affected. It's highly unlikely that
     *               something expects the presence of this bug. The affected values are of the FTL types listed below,
     *               and to trigger the bug, they either had to be created directly in the template (like as an FTL
     *               literal or with {@code ?date}/{@code time}/{@code datetime}), or you had to use
     *               {@link DefaultObjectWrapper} or {@link SimpleObjectWrapper} (or a subclass of them):
     *               
     *               <ul>
     *                 <li>FTL date/time/date-time values may came back as {@link SimpleDate}-s, now they come back as
     *                 {@link java.util.Date java.util.Date}-s instead.</li>
     *             
     *                 <li>FTL sequence values may came back as {@link SimpleSequence}-s, now they come back as
     *                 {@link java.util.List}-s as expected. This at least stands assuming that the
     *                 {@link Configuration#setSetting(String, String) object_wrapper} configuration setting is a
     *                 subclass of {@link BeansWrapper} (such as {@link DefaultObjectWrapper}, which is the default),
     *                 but that's practically always the case in applications that use FreeMarker's JSP extension
     *                 (otherwise it can still work, but it depends on the quality and capabilities of the
     *                 {@link ObjectWrapper} implementation).</li>
     *             
     *                 <li>FTL hash values may came back as {@link SimpleHash}-es, now they come back as
     *                 {@link java.util.Map}-s as expected (again, assuming that the object wrapper is a subclass of
     *                 {@link BeansWrapper}, like preferably {@link DefaultObjectWrapper}, which is also the default).
     *                 </li>
     *             
     *                 <li>FTL collection values may came back as {@link SimpleCollection}-s, now they come back as
     *                 {@link java.util.Collection}-s as expected (again, assuming that the object wrapper is a subclass
     *                 of {@link BeansWrapper}, like preferably {@link DefaultObjectWrapper}).</li>
     *               </ul>
     *             </li>
     *             <li><p>
     *               Initial {@code "["} in the {@code TemplatePath} init-param
     *               has special meaning; it's used for specifying multiple comma separated locations, like in
     *               {@code <param-value>[ WEB-INF/templates, classpath:com/example/myapp/templates ]</param-value>}
     *             </li>
     *             <li><p>
     *               Initial <tt>"{"</tt> in the {@code TemplatePath} init-param is reserved for future purposes, and
     *               thus will throw exception.
     *             </li>
     *          </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.23 (or higher):
     *     <ul>
     *       <li><p>
     *          Fixed a loophole in the implementation of the long existing parse-time rule that says that
     *          {@code #break}, in the FTL source code itself, must occur nested inside a breakable directive, such as
     *          {@code #list} or {@code #switch}. This check could be circumvented with {@code #macro} or
     *          {@code #function}, like this:
     *          {@code <#list 1..1 as x><#macro callMeLater><#break></#macro></#list><@callMeLater />}.
     *          After activating this fix, this will be a parse time error.
     *       </li>
     *       <li><p>
     *          If you have used {@code incompatible_improvements} 2.3.22 earlier, know that there the behavior of the
     *          {@code .template_name} special variable used in templates was accidentally altered, but now it's
     *          restored to be backward compatible with 2.3.0. (Ironically, the restored legacy behavior itself is
     *          broken when it comes to macro invocations, we just keep it for backward compatibility. If you need fixed
     *          behavior, use {@code .current_template_name} or {@code .main_template_name} instead.)
     *       </li>
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.24 (or higher):
     *     <ul>
     *       <li><p>
     *          The default of the
     *          {@link #setRecognizeStandardFileExtensions(boolean) recognize_standard_file_extensions}
     *          setting changes to {@code true}, which means that templates whose name ends with {@code ".ftlh"} or
     *          {@code ".ftlx"} will automatically get {@link HTMLOutputFormat#INSTANCE} or
     *          {@link XMLOutputFormat#INSTANCE} output format respectively, in both cases with
     *          {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY} {@link #setAutoEscapingPolicy(int) auto_escaping_policy}.
     *          These "file" extensions aren't case sensitive.
     *       </li>
     *       <li><p>
     *          In number format and date format strings (like in the {@code number_format} setting, or in templates in
     *          {@code n?string("0.##")}), an initial {@code '@'} has special meaning; they refer to a custom format
     *          with the name given after the {@code @} (see: {@link #setCustomNumberFormats(Map)},
     *          {@link #setCustomDateFormats(Map)}, {@link #setNumberFormat(String)}, and {@link #setDateTimeFormat}).
     *          If the custom format doesn't exist, that will be an error. To have a literal {@code @} as the first
     *          character in the output, it has to be written as {@code @@}. Again, all this only applies to the very
     *          first character of the format string, so {@code @} characters elsewhere must not be doubled. Also, if
     *          there are any custom formats defined, initial {@code '@'} will have the new meaning regardless of
     *          the value of the {@code incompatible_improvements} setting. So you don't need to set the
     *          {@code incompatible_improvements} only to use custom formats. 
     *       </li>
     *       <li><p>
     *          Expressions inside interpolations that were inside <em>string literal expressions</em>
     *          (not <code>${...}</code>-s in general), like in <code>&lt;#assign s="Hello ${name}!"&gt;</code>, has
     *          always used {@code incompatbileImprovement}-s 0 (2.3.0 in effect). Now it's fixed.
     *       </li>
     *       <li><p>
     *          {@link DefaultObjectWrapper} has some minor changes with {@code incompatibleImprovements} 2.3.24;
     *          check them out at {@link DefaultObjectWrapper#DefaultObjectWrapper(Version)}. It's important to know
     *          that if you set the {@code object_wrapper} setting (to an other value than {@code "default"}), rather
     *          than leaving it on its default value, the {@code object_wrapper} won't inherit the
     *          {@code incompatibleImprovements} of the {@link Configuration}. In that case, if you want the 2.3.24
     *          improvements of {@link DefaultObjectWrapper}, you have to set it in the {@link DefaultObjectWrapper}
     *          object itself too! (Note that it's OK to use a {@link DefaultObjectWrapper} with a different
     *          {@code incompatibleImprovements} version number than that of the {@link Configuration}, if that's
     *          really what you want.)
     *       </li>
     *       <li><p>
     *          Fixed bug: The {@code #import} directive meant to copy the library variable into a global variable if
     *          it's executed in the main namespace, but that haven't happened when the imported template was already
     *          imported earlier in another namespace. 
     *       </li>
     *       <li><p>
     *          {@code ?is_sequence} doesn't return {@code true} for Java methods wrapped by {@link BeansWrapper} and
     *          its subclasses (most notably {@link DefaultObjectWrapper}) anymore, as they only implement the
     *          {@code [index]} operator, but not {@code ?size}, which causes {@code <#list ...>} to fail among others.
     *          (They shouldn't implement either, but this is historical heritage.)
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.25 (or higher):
     *     <ul>
     *       <li><p>
     *          When calling {@link Configurable#setAutoIncludes(List)} on a {@link Configuration}, it filters out
     *          duplicates from the list, similarly as repeatedly calling {@link Configurable#addAutoInclude(String)}
     *          would, hence avoiding repeated inclusions. Calling {@link Configurable#setAutoIncludes(List)} on other
     *          {@link Configurable}-s always do this filtering regardless of the incompatible improvements setting. 
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.26 (or higher):
     *     <ul>
     *       <li><p>
     *          {@link BeansWrapper} and {@link DefaultObjectWrapper} now exposes Java 8 default methods (and the bean
     *          properties they define); see {@link BeansWrapper#BeansWrapper(Version)}. 
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.27 (or higher):
     *     <ul>
     *       <li><p>
     *          {@link BeansWrapper} and {@link DefaultObjectWrapper} now prefers the non-indexed JavaBean property
     *          read method over the indexed read method when Java 8 exposes both;
     *          see {@link BeansWrapper#BeansWrapper(Version)}.
     *       <li><p>
     *          The following unchecked exceptions (but not their subclasses) will be wrapped into
     *          {@link TemplateException}-s when thrown during evaluating expressions or calling directives:
     *          {@link NullPointerException}, {@link ClassCastException}, {@link IndexOutOfBoundsException}, and
     *          {@link InvocationTargetException}. The goal of this is the same as of setting
     *          {@link #setWrapUncheckedExceptions(boolean) wrap_unchecked_exceptions} to {@code true} (see more there),
     *          but this is more backward compatible, as it avoids wrapping unchecked exceptions that the calling
     *          application is likely to catch specifically (like application-specific unchecked exceptions).
     *       <li><p>
     *          When the {@link Writer} returned by {@link TemplateTransformModel#getWriter(Writer, Map)} implements
     *          {@link TransformControl}, exceptions that are used internally by FreeMarker for flow control (for
     *          {@code <#return>}, {@code <#break>}, etc.) won't be passed to
     *          {@link TransformControl#onError(Throwable)} anymore. Earlier, if {@code onError} didn't rethrow the
     *          exception (though almost all implementation does), you couldn't use said directives inside the
     *          transformed block. It's very unlikely that user code is affected by this, partially because these aren't
     *          commonly implemented interfaces (especially not {@link TransformControl}), and because it's unlikely
     *          that templates utilize the the bug that's not fixed.
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.28 (or higher):
     *     <ul>
     *       <li><p>When calling a macro or function (things defined in a template, not directly in Java) and the
     *           argument list contains {@code .current_template_name}, now it will correctly evaluate to the template
     *           that contains the call, rather than to the template that contains the macro or function definition.
     *           (Of course, the parameter default value expression is still evaluated in the context of the called
     *           macro or function.) Similarly, {@code .macro_caller_template_name} (which itself was added in 2.3.28),
     *           when used in a macro call argument, won't be incorrectly evaluated in the context of the called macro.
     *       <li><p>Fixed legacy parser glitch where a tag can be closed with an illegal {@code ]} (when it's not part
     *           of an expression) despite that the tag syntax is set to angle brackets. For example {@code <#if x]}
     *           worked just like {@code <#if x>}. Note that it doesn't affect the legal usage of {@code ]}, like
     *           {@code <#if x[0]>} works correctly without this fix as well. 
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.31 (or higher):
     *     <ul>
     *       <li><p>When you set the {@code number_format} setting to {@code "computer"} (or you call
     *       {@link Environment#getCNumberFormat()}), the format now matches the behavior of {@code ?c}, when formatting
     *       infinite (positive and negative), and NaN. Matching the behavior of {@code ?c} was always the intent,
     *       but before this incompatible improvement, the {@code "computer"} format always behaved like {@code ?c}
     *       before Incompatible Improvements 2.3.21, where instead of INF, and NaN, the results used unicode characters
     *       U+221E, and U+FFFD.
     *     </ul>
     *   </li>
     *   <li><p>
     *     2.3.32 (or higher):
     *     <ul>
     *       <li><p>
     *         The number formatting of {@code ?c}, {@code ?cn} (and thus also of the {@code "c"}, and
     *         {@code "computer"} {@link Configurable#setNumberFormat(String) number_format}) changes, if the
     *         {@link #setCFormat(CFormat) c_format} setting was left on its default. The default of
     *         {@link #setCFormat(CFormat) c_format} changes to {@link JavaScriptOrJSONCFormat#INSTANCE}, from
     *         {@link LegacyCFormat#INSTANCE}, and that's what contains the changes:</p>
     *         <ul>
     *           <li><p>Changes affecting non-whole numbers, and whole numbers with over 100 digits:
     *             Formatting is now lossless, so it potentially shows much more decimals.
     *             It now uses exponential format (like 1.2E-7 instead of 0.00000012) for numbers whose absolute value
     *             is less than 1E-6 (0.000001), and for whole numbers whose absolute value is at least 1E101 (so over
     *             100 digits). It also uses exponential format for whole floating point
     *             ({@code double}/{@code Double}}, or {@code float}/{@code Float}) numbers, when their absolute value
     *             is too big for the floating point type to store them precisely (so if the intent was to store some
     *             ID-s, they are likely corrupted anyway, as the type skips some whole numbers).</p></li>
     *           <li><p>Changes affecting floating point infinity: Output changes from {@code INF} to {@code Infinity},
     *             which is the JavaScript and JSON syntax. If you generate XML with XSD-style number syntax (which uses
     *             {@code INF}), but you want the other number formatting changes (recommended), then set
     *             {@link #setCFormat(CFormat) c_format} to {@link XSCFormat#INSTANCE}/{@code "XS"}.</p></li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * @throws IllegalArgumentException
     *             If {@code incompatibleImmprovements} refers to a version that wasn't released yet when the currently
     *             used FreeMarker version was released, or is less than 2.3.0, or is {@code null}.
     * 
     * @since 2.3.21
     */
    public Configuration(Version incompatibleImprovements) {
        super(incompatibleImprovements);
        
        // We postpone this until here (rather that doing this in static initializer) for two reason:
        // - Class initialization errors are often not reported very well
        // - This way we avoid the error if FM isn't actually used
        checkFreeMarkerVersionClash();
        
        NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
        checkCurrentVersionNotRecycled(incompatibleImprovements);
        this.incompatibleImprovements = incompatibleImprovements;
        
        createTemplateCache();
        loadBuiltInSharedVariables();
    }

    private static void checkFreeMarkerVersionClash() {
        if (FM_24_DETECTED) {
            throw new RuntimeException("Clashing FreeMarker versions (" + VERSION + " and some post-2.3.x) detected: "
                    + "found post-2.3.x class " + FM_24_DETECTION_CLASS_NAME + ". You probably have two different "
                    + "freemarker.jar-s in the classpath.");
        }
    }
    
    private void createTemplateCache() {
        cache = new TemplateCache(
                getDefaultTemplateLoader(),
                getDefaultCacheStorage(),
                getDefaultTemplateLookupStrategy(),
                getDefaultTemplateNameFormat(),
                null,
                this);
        cache.clear(); // for fully BC behavior
        cache.setDelay(5000);
    }
    
    private void recreateTemplateCacheWith(
            TemplateLoader loader, CacheStorage storage,
            TemplateLookupStrategy templateLookupStrategy, TemplateNameFormat templateNameFormat,
            TemplateConfigurationFactory templateConfigurations) {
        TemplateCache oldCache = cache;
        cache = new TemplateCache(
                loader, storage, templateLookupStrategy, templateNameFormat, templateConfigurations, this);
        cache.clear(); // for fully BC behavior
        cache.setDelay(oldCache.getDelay());
        cache.setLocalizedLookup(localizedLookup);
    }
    
    private void recreateTemplateCache() {
        recreateTemplateCacheWith(cache.getTemplateLoader(), cache.getCacheStorage(),
                cache.getTemplateLookupStrategy(), cache.getTemplateNameFormat(),
                getTemplateConfigurations());
    }
    
    private TemplateLoader getDefaultTemplateLoader() {
        return createDefaultTemplateLoader(getIncompatibleImprovements(), getTemplateLoader());
    }

    static TemplateLoader createDefaultTemplateLoader(Version incompatibleImprovements) {
        return createDefaultTemplateLoader(incompatibleImprovements, null);
    }
    
    private static TemplateLoader createDefaultTemplateLoader(
            Version incompatibleImprovements, TemplateLoader existingTemplateLoader) {
        if (incompatibleImprovements.intValue() < _VersionInts.V_2_3_21) {
            if (existingTemplateLoader instanceof LegacyDefaultFileTemplateLoader) {
                return existingTemplateLoader;
            }
            try {
                return new LegacyDefaultFileTemplateLoader();
            } catch (Exception e) {
                CACHE_LOG.warn("Couldn't create legacy default TemplateLoader which accesses the current directory. "
                        + "(Use new Configuration(Configuration.VERSION_2_3_21) or higher to avoid this.)", e);
                return null;
            }
        } else {
            return null;
        }
    }
    
    private static class LegacyDefaultFileTemplateLoader extends FileTemplateLoader {

        public LegacyDefaultFileTemplateLoader() throws IOException {
            super();
        }
        
    }
    
    private TemplateLookupStrategy getDefaultTemplateLookupStrategy() {
        return getDefaultTemplateLookupStrategy(getIncompatibleImprovements());
    }
    
    static TemplateLookupStrategy getDefaultTemplateLookupStrategy(Version incompatibleImprovements) {
        return TemplateLookupStrategy.DEFAULT_2_3_0;
    }
    
    private TemplateNameFormat getDefaultTemplateNameFormat() {
        return getDefaultTemplateNameFormat(getIncompatibleImprovements());
    }
    
    static TemplateNameFormat getDefaultTemplateNameFormat(Version incompatibleImprovements) {
        return TemplateNameFormat.DEFAULT_2_3_0;
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

    private AttemptExceptionReporter getDefaultAttemptExceptionReporter() {
        return getDefaultAttemptExceptionReporter(getIncompatibleImprovements());
    }
    
    private boolean getDefaultLogTemplateExceptions() {
        return getDefaultLogTemplateExceptions(getIncompatibleImprovements());
    }

    private boolean getDefaultWrapUncheckedExceptions() {
        return getDefaultWrapUncheckedExceptions(getIncompatibleImprovements());
    }
    
    private ObjectWrapper getDefaultObjectWrapper() {
        return getDefaultObjectWrapper(getIncompatibleImprovements());
    }
    
    // Package visible as Configurable needs this to initialize the field defaults.
    static TemplateExceptionHandler getDefaultTemplateExceptionHandler(Version incompatibleImprovements) {
        return TemplateExceptionHandler.DEBUG_HANDLER;
    }

    // Package visible as Configurable needs this to initialize the field defaults.
    static AttemptExceptionReporter getDefaultAttemptExceptionReporter(Version incompatibleImprovements) {
        return AttemptExceptionReporter.LOG_ERROR_REPORTER;
    }
    
    // Package visible as Configurable needs this to initialize the field defaults.
    static boolean getDefaultLogTemplateExceptions(Version incompatibleImprovements) {
        return true;
    }

    // Package visible as Configurable needs this to initialize the field defaults.
    static boolean getDefaultWrapUncheckedExceptions(Version incompatibleImprovements) {
        return false;
    }
    
    @Override
    public Object clone() {
        try {
            Configuration copy = (Configuration) super.clone();
            copy.sharedVariables = new HashMap(sharedVariables);
            copy.localeToCharsetMap = new ConcurrentHashMap(localeToCharsetMap);
            copy.recreateTemplateCacheWith(
                    cache.getTemplateLoader(), cache.getCacheStorage(),
                    cache.getTemplateLookupStrategy(), cache.getTemplateNameFormat(),
                    cache.getTemplateConfigurations());
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
     * Returns the default (singleton) Configuration object. Note that you can
     * create as many separate configurations as you wish; this global instance
     * is provided for convenience, or when you have no reason to use a separate
     * instance.
     * 
     * @deprecated The usage of the static singleton (the "default")
     * {@link Configuration} instance can easily cause erroneous, unpredictable
     * behavior. This is because multiple independent software components may use
     * FreeMarker internally inside the same application, so they will interfere
     * because of the common {@link Configuration} instance. Each such component
     * should use its own private {@link Configuration} object instead, that it
     * typically creates with <code>new Configuration()</code> when the component
     * is initialized.
     */
    @Deprecated
    static public Configuration getDefaultConfiguration() {
        Configuration defaultConfig = Configuration.defaultConfig;
        if (defaultConfig == null) {
            synchronized (defaultConfigLock) {
                defaultConfig = Configuration.defaultConfig;
                if (defaultConfig == null) {
                    defaultConfig = new Configuration();
                    Configuration.defaultConfig = defaultConfig; 
                }
            }
        }
        return defaultConfig;
    }

    /**
     * Sets the Configuration object that will be retrieved from future calls
     * to {@link #getDefaultConfiguration()}.
     * 
     * @deprecated Using the "default" {@link Configuration} instance can
     * easily lead to erroneous, unpredictable behaviour.
     * See more {@link Configuration#getDefaultConfiguration() here...}.
     */
    @Deprecated
    static public void setDefaultConfiguration(Configuration config) {
        synchronized (defaultConfigLock) {
            defaultConfig = config;
        }
    }
    
    /**
     * Sets a {@link TemplateLoader} that is used to look up and load templates;
     * as a side effect the template cache will be emptied (unless the new and the old values are the same).
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
            if (cache.getTemplateLoader() != templateLoader) {
                recreateTemplateCacheWith(templateLoader, cache.getCacheStorage(),
                        cache.getTemplateLookupStrategy(), cache.getTemplateNameFormat(),
                        cache.getTemplateConfigurations());
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
            setTemplateLoader(getDefaultTemplateLoader());
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
        if (cache == null) {
            return null;
        }
        return cache.getTemplateLoader();
    }
    
    /**
     * Sets the {@link TemplateLookupStrategy} that is used to look up templates based on the requested name; as a side
     * effect the template cache will be emptied. The default value is {@link TemplateLookupStrategy#DEFAULT_2_3_0}.
     * 
     * @since 2.3.22
     */
    public void setTemplateLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
        if (cache.getTemplateLookupStrategy() != templateLookupStrategy) {
            recreateTemplateCacheWith(cache.getTemplateLoader(), cache.getCacheStorage(),
                    templateLookupStrategy, cache.getTemplateNameFormat(),
                    cache.getTemplateConfigurations());
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
        if (cache == null) {
            return null;
        }
        return cache.getTemplateLookupStrategy();
    }
    
    /**
     * Sets the template name format used. The default is {@link TemplateNameFormat#DEFAULT_2_3_0}, while the
     * recommended value for new projects is {@link TemplateNameFormat#DEFAULT_2_4_0}.
     * 
     * @since 2.3.22
     */
    public void setTemplateNameFormat(TemplateNameFormat templateNameFormat) {
        if (cache.getTemplateNameFormat() != templateNameFormat) {
            recreateTemplateCacheWith(cache.getTemplateLoader(), cache.getCacheStorage(),
                    cache.getTemplateLookupStrategy(), templateNameFormat,
                    cache.getTemplateConfigurations());
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
        if (cache == null) {
            return null;
        }
        return cache.getTemplateNameFormat();
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
        if (cache.getTemplateConfigurations() != templateConfigurations) {
            if (templateConfigurations != null) {
                templateConfigurations.setConfiguration(this);
            }
            recreateTemplateCacheWith(cache.getTemplateLoader(), cache.getCacheStorage(),
                    cache.getTemplateLookupStrategy(), cache.getTemplateNameFormat(),
                    templateConfigurations);
        }
    }
    
    /**
     * The getter pair of {@link #setTemplateConfigurations(TemplateConfigurationFactory)}.
     */
    public TemplateConfigurationFactory getTemplateConfigurations() {
        if (cache == null) {
            return null;
        }
        return cache.getTemplateConfigurations();
    }

    /**
     * Sets the {@link CacheStorage} used for caching {@link Template}-s;
     * the earlier content of the template cache will be dropt.
     * 
     * The default is a {@link SoftCacheStorage}. If the total size of the {@link Template}
     * objects is significant but most templates are used rarely, using a
     * {@link MruCacheStorage} instead might be advisable. If you don't want caching at
     * all, use {@link freemarker.cache.NullCacheStorage} (you can't use {@code null}).
     * 
     * <p>Note that setting the cache storage will re-create the template cache, so
     * all its content will be lost.
     */
    public void setCacheStorage(CacheStorage cacheStorage) {
        // "synchronized" is removed from the API as it's not safe to set anything after publishing the Configuration
        synchronized (this) {
            if (getCacheStorage() != cacheStorage) {
                recreateTemplateCacheWith(cache.getTemplateLoader(), cacheStorage,
                        cache.getTemplateLookupStrategy(), cache.getTemplateNameFormat(),
                        cache.getTemplateConfigurations());
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
            if (cache == null) {
                return null;
            }
            return cache.getCacheStorage();
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
     * This is equivalent to {@code setTemplateLoader(new WebappTemplateLoader(sctxt, path))}
     * or {@code setTemplateLoader(new WebappTemplateLoader(sctxt))} if {@code path} was
     * {@code null}, so see {@code freemarker.cache.WebappTemplateLoader} for more details.
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
            final Class webappTemplateLoaderClass = ClassUtil.forName("freemarker.cache.WebappTemplateLoader");
            
            // Don't introduce linking-time dependency on servlets
            final Class servletContextClass = ClassUtil.forName("javax.servlet.ServletContext");
            
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
     * Sets the time in seconds that must elapse before checking whether there is a newer version of a template "file"
     * than the cached one.
     * 
     * <p>
     * Historical note: Despite what the API documentation said earlier, this method is <em>not</em> thread-safe. While
     * it works well on most hardware, it's not guaranteed that FreeMarker will see the update in all threads, and
     * theoretically it's also possible that it will see a value that's a binary mixture of the new and the old one.
     * 
     * @deprecated Use {@link #setTemplateUpdateDelayMilliseconds(long)} instead, because the time granularity of this method
     *             is often misunderstood to be milliseconds.
     */
    @Deprecated
    public void setTemplateUpdateDelay(int seconds) {
        cache.setDelay(1000L * seconds);
    }

    /**
     * Sets the time in milliseconds that must elapse before checking whether there is a newer version of a template
     * "file" than the cached one. Defaults to 5000 ms.
     * 
     * <p>
     * When you get a template via {@link #getTemplate(String)} (or some of its overloads). FreeMarker will try to get
     * the template from the template cache. If the template is found, and at least this amount of time was elapsed
     * since the template last modification date was checked, FreeMarker will re-check the last modification date (this
     * could mean I/O), possibly reloading the template and updating the cache as a consequence (can mean even more
     * I/O). The {@link #getTemplate(String)} (or some of its overloads) call will only return after this all is
     * done, so it will return the fresh template.
     * 
     * @since 2.3.23
     */
    public void setTemplateUpdateDelayMilliseconds(long millis) {
        cache.setDelay(millis);
    }
    
    /**
     * The getter pair of {@link #setTemplateUpdateDelayMilliseconds(long)}.
     * 
     * @since 2.3.23
     */
    public long getTemplateUpdateDelayMilliseconds() {
        return cache.getDelay();
    }
    
    /**
     * Sets whether directives such as {@code if}, {@code else}, etc must be written as {@code #if}, {@code #else}, etc.
     * Defaults to {@code true}.
     * 
     * <p>When this is {@code true},
     * any tag not starting with &lt;# or &lt;/# or &lt;@ or &lt;/@ is considered as plain text
     * and will go to the output as is. Tag starting with &lt;# or &lt;/# must
     * be valid FTL tag, or else the template is invalid (i.e. &lt;#noSuchDirective&gt;
     * is an error).
     * 
     * @deprecated Only {@code true} (the default) value will be supported sometimes in the future.
     */
    @Deprecated
    public void setStrictSyntaxMode(boolean b) {
        strictSyntax = b;
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
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        localeExplicitlySet = true;
    }
    
    /**
     * Resets the setting to its default, as if it was never set.
     * 
     * @since 2.3.26
     */
    public void unsetLocale() {
        if (localeExplicitlySet) {
            setLocale(getDefaultLocale());
            localeExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setLocale(Locale)} (or equivalent) was already called on this instance, or it just holds the
     * default value.
     * 
     * @since 2.3.26
     */
    public boolean isLocaleExplicitlySet() {
        return localeExplicitlySet;
    }
    
    static Locale getDefaultLocale() {
        return Locale.getDefault();
    }
    
    @Override
    public void setTimeZone(TimeZone timeZone) {
        super.setTimeZone(timeZone);
        timeZoneExplicitlySet = true;
    }
    
    /**
     * Resets the setting to its default, as if it was never set.
     * 
     * @since 2.3.26
     */
    public void unsetTimeZone() {
        if (timeZoneExplicitlySet) {
            setTimeZone(getDefaultTimeZone());
            timeZoneExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setTimeZone(TimeZone)} (or equivalent) was already called on this instance, or it just holds the
     * default value.
     * 
     * @since 2.3.26
     */
    public boolean isTimeZoneExplicitlySet() {
        return timeZoneExplicitlySet;
    }
    
    static TimeZone getDefaultTimeZone() {
        return TimeZone.getDefault();
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
    
    @Override
    public void setAttemptExceptionReporter(AttemptExceptionReporter attemptExceptionReporter) {
        super.setAttemptExceptionReporter(attemptExceptionReporter);
        attemptExceptionReporterExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also 
     * {@link #isAttemptExceptionReporterExplicitlySet()} will return {@code false}.
     * 
     * @since 2.3.27
     */
    public void unsetAttemptExceptionReporter() {
        if (attemptExceptionReporterExplicitlySet) {
            setAttemptExceptionReporter(getDefaultAttemptExceptionReporter());
            attemptExceptionReporterExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setAttemptExceptionReporter(AttemptExceptionReporter)} (or equivalent) was already called on
     * this instance.
     * 
     * @since 2.3.27
     */
    public boolean isAttemptExceptionReporterExplicitlySet() {
        return attemptExceptionReporterExplicitlySet;
    }    
    
    /**
     * {@inheritDoc}
     * 
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
            setLogTemplateExceptions(getDefaultLogTemplateExceptions());
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
     * {@inheritDoc}
     * 
     * @since 2.3.27
     */
    @Override
    public void setWrapUncheckedExceptions(boolean value) {
        super.setWrapUncheckedExceptions(value);
        wrapUncheckedExceptionsExplicitlySet = true;
    }
    
    /**
     * @since 2.3.27
     */
    public void unsetWrapUncheckedExceptions() {
        if (wrapUncheckedExceptionsExplicitlySet) {
            setWrapUncheckedExceptions(getDefaultWrapUncheckedExceptions());
            wrapUncheckedExceptionsExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setWrapUncheckedExceptions} (or equivalent) was already called on this instance.
     * 
     * @since 2.3.27
     */
    public boolean isWrapUncheckedExceptionsExplicitlySet() {
        return wrapUncheckedExceptionsExplicitlySet;
    }
    
    /**
     * The getter pair of {@link #setStrictSyntaxMode}.
     */
    @Override
    public boolean getStrictSyntaxMode() {
        return strictSyntax;
    }

    /**
     * Use {@link #Configuration(Version)} instead if possible; see the meaning of the parameter there.
     * 
     * <p>Do NOT ever use {@link #getVersion()} to set the "incompatible improvements". Always use a fixed value, like
     * {@link #VERSION_2_3_30}. Otherwise your application can break as you upgrade FreeMarker. (As of 2.3.30, doing
     * this will be logged as an error. As of 2.4.0, it will be probably disallowed, by throwing exception.)
     * 
     * <p>If the default value of a setting depends on the {@code incompatibleImprovements} and the value of that setting
     * was never set in this {@link Configuration} object through the public API, its value will be set to the default
     * value appropriate for the new {@code incompatibleImprovements}. (This adjustment of a setting value doesn't
     * count as setting that setting, so setting {@code incompatibleImprovements} for multiple times also works as
     * expected.) Note that if the {@code template_loader} have to be changed because of this, the template cache will
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
            checkCurrentVersionNotRecycled(incompatibleImprovements);

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

            if (!attemptExceptionReporterExplicitlySet) {
                attemptExceptionReporterExplicitlySet = true;
                unsetAttemptExceptionReporter();
            }
            
            if (!logTemplateExceptionsExplicitlySet) {
                logTemplateExceptionsExplicitlySet = true;
                unsetLogTemplateExceptions();
            }

            if (!cFormatExplicitlySet) {
                cFormatExplicitlySet = true;
                unsetCFormat();
            }

            if (!wrapUncheckedExceptionsExplicitlySet) {
                wrapUncheckedExceptionsExplicitlySet = true;
                unsetWrapUncheckedExceptions();
            }
            
            if (!objectWrapperExplicitlySet) {
                objectWrapperExplicitlySet = true;
                unsetObjectWrapper();
            }
            
            recreateTemplateCache();
        }
    }

    private static void checkCurrentVersionNotRecycled(Version incompatibleImprovements) {
        _TemplateAPI.checkCurrentVersionNotRecycled(
                incompatibleImprovements,
                "freemarker.configuration", "Configuration");
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
     * @deprecated Use {@link #Configuration(Version)}, or
     *    as last chance, {@link #setIncompatibleImprovements(Version)} instead.
     */
    @Deprecated
    public void setIncompatibleEnhancements(String version) {
        setIncompatibleImprovements(new Version(version));
    }
    
    /**
     * @deprecated Use {@link #getIncompatibleImprovements()} instead.
     */
    @Deprecated
    public String getIncompatibleEnhancements() {
        return incompatibleImprovements.toString();
    }
    
    /**
     * @deprecated Use {@link #getIncompatibleImprovements()} instead.
     */
    @Deprecated
    public int getParsedIncompatibleEnhancements() {
        return getIncompatibleImprovements().intValue();
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
     * default is {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}.
     * 
     * <p>Note that the default output format, {@link UndefinedOutputFormat}, is a non-escaping format, so there
     * auto-escaping will be off.
     * 
     * <p>Note that the templates can turn auto-escaping on/off locally with directives like
     * {@code <#ftl auto_esc=...>}, {@code <#autoesc>...</#autoesc>}, and {@code <#noautoesc>...</#noautoesc>}, which
     * are ignoring the auto-escaping policy.
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
     * all, to a value like {@link HTMLOutputFormat#INSTANCE}, {@link XMLOutputFormat#INSTANCE}, etc. Also note
     * that templates can specify their own output format like {@code 
     * <#ftl output_format="HTML">}, which overrides any configuration settings.
     * 
     * <p>
     * The output format is mostly important because of auto-escaping (see {@link #setAutoEscapingPolicy(int)}), but
     * maybe also used by the embedding application to set the HTTP response MIME type, etc.
     * 
     * @param outputFormat
     *            Not {@code null}; use {@link UndefinedOutputFormat#INSTANCE} instead.
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
            throw new NullArgumentException(
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
     *            A combined output format is created ad-hoc from the registered formats. For example, if you need RTF
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
                sb.append(StringUtil.jQuote(name));
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
                    sb.append(StringUtil.jQuote(registeredName));
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
        MarkupOutputFormat outerOF = (MarkupOutputFormat) of;
        return outerOF;
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
        NullArgumentException.check(registeredCustomOutputFormats);
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
                ? incompatibleImprovements.intValue() >= _VersionInts.V_2_3_24
                : recognizeStandardFileExtensions.booleanValue();
    }

    @Override
    public void setCFormat(CFormat cFormat) {
        super.setCFormat(cFormat);
        cFormatExplicitlySet = true;
    }

    /**
     * Resets the setting to its default, as if it was never set. This means that when you change the
     * {@code incompatibe_improvements} setting later, the default will also change as appropriate. Also
     * {@link #isCFormatExplicitlySet()} will return {@code false}.
     *
     * @since 2.3.32
     */
    public void unsetCFormat() {
        if (cFormatExplicitlySet) {
            setCFormat(getDefaultCFormat(incompatibleImprovements));
            cFormatExplicitlySet = false;
        }
    }

    static CFormat getDefaultCFormat(Version incompatibleImprovements) {
        return incompatibleImprovements.intValue() >= _VersionInts.V_2_3_32
                ? JavaScriptOrJSONCFormat.INSTANCE
                : LegacyCFormat.INSTANCE;
    }

    /**
     * Tells if {@link #setCFormat(CFormat)} (or equivalent) was already called on this instance.
     *
     * @since 2.3.32
     */
    public boolean isCFormatExplicitlySet() {
        return cFormatExplicitlySet;
    }

    /**
     * Determines the tag syntax (like {@code <#if x>} VS {@code [#if x]}) of the template files 
     * that has no {@code #ftl} header to decide that. Don't confuse this with the interpolation syntax
     * ({@link #setInterpolationSyntax(int)}); they are independent.
     * 
     * <p>The {@code tagSyntax} parameter must be one of:
     * <ul>
     *   <li>{@link Configuration#AUTO_DETECT_TAG_SYNTAX}:
     *     Use the syntax of the first FreeMarker tag (can be anything, like <tt>#list</tt>,
     *     <tt>#include</tt>, user defined, etc.)
     *   <li>{@link Configuration#ANGLE_BRACKET_TAG_SYNTAX}:
     *     Use the angle bracket tag syntax (the normal syntax), like {@code <#include ...>}
     *   <li>{@link Configuration#SQUARE_BRACKET_TAG_SYNTAX}:
     *     Use the square bracket tag syntax, like {@code [#include ...]}. Note that this does <em>not</em> change
     *     <code>${x}</code> to {@code [=...]}; that's <em>interpolation</em> syntax, so use
     *     {@link #setInterpolationSyntax(int)} for that.
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
     * 
     * @see #setInterpolationSyntax(int)
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
     * Determines the interpolation syntax (like <code>${x}</code> VS <code>[=x]</code>) of the template files. Don't
     * confuse this with the tag syntax ({@link #setTagSyntax(int)}); they are independent.
     * 
     * <p>
     * The {@code interpolationSyntax} parameter must be one of {@link Configuration#LEGACY_INTERPOLATION_SYNTAX},
     * {@link Configuration#DOLLAR_INTERPOLATION_SYNTAX}, and {@link Configuration#SQUARE_BRACKET_INTERPOLATION_SYNTAX}.
     * Note that {@link Configuration#SQUARE_BRACKET_INTERPOLATION_SYNTAX} does <em>not</em> change {@code <#if x>} to
     * {@code [#if x]}; that's <em>tag</em> syntax, so use {@link #setTagSyntax(int)} for that.
     * 
     * @see #setTagSyntax(int)
     * 
     * @since 2.3.28
     */
    public void setInterpolationSyntax(int interpolationSyntax) {
        _TemplateAPI.valideInterpolationSyntaxValue(interpolationSyntax);
        this.interpolationSyntax = interpolationSyntax;
    }
    
    /**
     * The getter pair of {@link #setInterpolationSyntax(int)}.
     * 
     * @since 2.3.28
     */
    @Override
    public int getInterpolationSyntax() {
        return interpolationSyntax;
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
     * <p>
     * Non-strict tags (a long deprecated syntax from FreeMarker 1, activated via {@link #setStrictSyntaxMode(boolean)})
     * are only recognized as FTL tags when they are using the {@link Configuration#LEGACY_NAMING_CONVENTION} syntax,
     * regardless of this setting. As they aren't exempt from the naming convention consistency enforcement, generally,
     * you can't use strict {@link Configuration#CAMEL_CASE_NAMING_CONVENTION} tags mixed with non-strict tags.
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
     * in the template will remain a tab in the output too. If you set this setting to 1, then tab characters will be
     * kept in the return value of {@link Template#getSource(int, int, int, int)}, otherwise they will be replaced with
     * the appropriate number of spaces.
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
            throw new IllegalArgumentException("\"tabSize\" can't be more than 256, but was " + tabSize);
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
     * The getter pair of {@link #setFallbackOnNullLoopVariable(boolean)}.
     *
     * @since 2.3.29
     */
    public boolean getFallbackOnNullLoopVariable() {
        return fallbackOnNullLoopVariable;
    }

    /**
     * Specifies the behavior when reading a loop variable (like {@code i} in {@code <#list items as i>}, or in
     * {@code <@myMacro items; i>}) that's {@code null} (missing); if {@code true}, FreeMarker will look for a variable
     * with the same name in higher variable scopes, or if {@code false} the variable will be simply {@code null}
     * (missing). For backward compatibility the default is {@code true}. The recommended value for new projects is
     * {@code false}, as otherwise adding new variables to higher scopes (typically to the data-model) can
     * unintentionally change the behavior of templates. You have to be quite unlucky for that to happen though:
     * The newly added variable has to have the same name as the loop variable, and there must be some null (missing)
     * values in what you loop through.
     *
     * <p>This setting doesn't influence the behavior of lambdas, like {@code items?filter(i -> i?hasContent)}, as they
     * never had this problem. Reading a lambda argument never falls back to higher scopes.
     *
     * @since 2.3.29
     */
    public void setFallbackOnNullLoopVariable(boolean fallbackOnNullLoopVariable) {
        this.fallbackOnNullLoopVariable = fallbackOnNullLoopVariable;
    }

    /**
     * Getter pair of {@link #setPreventStrippings(boolean)}.
     * 
     * @since 2.3.27
     */
    boolean getPreventStrippings() {
        return preventStrippings;
    }

    /**
     * Used internally; added for the FreeMarker 2 to FreeMarker 3 converter, prevents the stripping/removal of AST
     * nodes so that the source code can be fully reproduced from the AST.
     * 
     * @since 2.3.27
     */
    void setPreventStrippings(boolean preventStrippings) {
        this.preventStrippings = preventStrippings;
    }

    /**
     * Retrieves the template with the given name from the template cache, loading it into the cache first if it's
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
     * cache, loading it into the cache first if it's missing/staled.
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
     *            {@link TemplateLoader}, but the cache makes some assumptions. First, the name is expected to be a
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
     *            asterisk (called resource path), the cache will attempt to remove the rightmost path component from
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
     *            This object will be used as part of the cache key, so it must to have a proper
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
        
        final MaybeMissingTemplate maybeTemp = cache.getTemplate(name, locale, customLookupCondition, encoding, parseAsFTL);
        final Template temp = maybeTemp.getTemplate();
        if (temp == null) {
            if (ignoreMissing) {
                return null;
            }
            
            TemplateLoader tl = getTemplateLoader();  
            String msg; 
            if (tl == null) {
                msg = "Don't know where to load template " + StringUtil.jQuote(name)
                      + " from because the \"template_loader\" FreeMarker "
                      + "setting wasn't set (Configuration.setTemplateLoader), so it's null.";
            } else {
                final String missingTempNormName = maybeTemp.getMissingTemplateNormalizedName();
                final String missingTempReason = maybeTemp.getMissingTemplateReason();
                final TemplateLookupStrategy templateLookupStrategy = getTemplateLookupStrategy();
                msg = "Template not found for name " + StringUtil.jQuote(name)
                        + (missingTempNormName != null && name != null
                                && !removeInitialSlash(name).equals(missingTempNormName)
                                ? " (normalized: " + StringUtil.jQuote(missingTempNormName) + ")"
                                : "")
                        + (customLookupCondition != null ? " and custom lookup condition "
                        + StringUtil.jQuote(customLookupCondition) : "")
                        + "."
                        + (missingTempReason != null
                                ? "\nReason given: " + ensureSentenceIsClosed(missingTempReason)
                                : "")
                        + "\nThe name was interpreted by this TemplateLoader: "
                        + StringUtil.tryToString(tl) + "."
                        + (!isKnownNonConfusingLookupStrategy(templateLookupStrategy)
                                ? "\n(Before that, the name was possibly changed by this lookup strategy: "
                                  + StringUtil.tryToString(templateLookupStrategy) + ".)"
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
        return templateLookupStrategy == TemplateLookupStrategy.DEFAULT_2_3_0;
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
        defaultEncodingExplicitlySet = true;
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
     * Resets the setting to its default, as if it was never set.
     * 
     * @since 2.3.26
     */
    public void unsetDefaultEncoding() {
        if (defaultEncodingExplicitlySet) {
            setDefaultEncoding(getDefaultDefaultEncoding());
            defaultEncodingExplicitlySet = false;
        }
    }
    
    /**
     * Tells if {@link #setDefaultEncoding(String)} (or equivalent) was already called on this instance, or it just holds the
     * default value.
     * 
     * @since 2.3.26
     */
    public boolean isDefaultEncodingExplicitlySet() {
        return defaultEncodingExplicitlySet;
    }
    
    static private String getDefaultDefaultEncoding() {
        return getJVMDefaultEncoding();
    }

    static private String getJVMDefaultEncoding() {
        return SecurityUtilities.getSystemProperty("file.encoding", "utf-8");
    }
    
    /**
     * Gets the preferred character encoding for the given locale, or the 
     * default encoding if no encoding is set explicitly for the specified
     * locale. You can associate encodings with locales using 
     * {@link #setEncoding(Locale, String)} or {@link #loadBuiltInEncodingMap()}.
     * 
     * @param locale Shouldn't be {@code null}, though for backward compatibility it's accepted when the locale to
     *               encoding {@link Map} (see earlier) is empty.
     */
    public String getEncoding(Locale locale) {
        if (localeToCharsetMap.isEmpty()) {
            return defaultEncoding;
        } else {
            // Try for a full name match (may include country and variant)
            NullArgumentException.check("locale", locale);
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
     * @since 2.3.29
     */
    public void setSharedVariables(Map<String, ?> map) throws TemplateModelException {
        rewrappableSharedVariables = new HashMap(map);
        sharedVariables.clear();
        setSharedVariablesFromRewrappableSharedVariables();
    }

    /**
     * Same as {@link #setSharedVariables(Map)}, but with typo in the name.
     * @since 2.3.21
     * @deprecated Use {@link #setSharedVariables(Map)} instead. 
     */
    public void setSharedVaribles(Map/*<String, Object>*/ map) throws TemplateModelException {
        setSharedVariables(map);
    }
    
    private void setSharedVariablesFromRewrappableSharedVariables() throws TemplateModelException {
        if (rewrappableSharedVariables == null) return;
        for (Iterator it = rewrappableSharedVariables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry/*<String, Object>*/ ent = (Entry) it.next();
            String name = (String) ent.getKey();
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
     * Gets a shared variable. Shared shared variables are variables that are
     * available to all templates. When a template is processed, and an identifier
     * is undefined in the data model, a shared variable object with the same identifier
     * is then looked up in the configuration. There are several predefined shared variables
     * that are always available through this method; see the FreeMarker manual
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
     * Removes all shared variables, except the predefined ones (compress, html_escape, etc.).
     */
    public void clearSharedVariables() {
        sharedVariables.clear();
        loadBuiltInSharedVariables();
    }
    
    /**
     * Removes all entries from the template cache, thus forcing reloading of templates
     * on subsequent <code>getTemplate</code> calls.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     */
    public void clearTemplateCache() {
        cache.clear();
    }
    
    /**
     * Equivalent to {@link
     * #removeTemplateFromCache(String, Locale, Object, String, boolean)
     * removeTemplateFromCache(name, thisCfg.getLocale(), null, thisCfg.getEncoding(thisCfg.getLocale()), true)}.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name) throws IOException {
        Locale loc = getLocale();
        removeTemplateFromCache(name, loc, null, getEncoding(loc), true);
    }

    /**
     * Equivalent to {@link
     * #removeTemplateFromCache(String, Locale, Object, String, boolean)
     * removeTemplateFromCache(name, locale, null, thisCfg.getEncoding(locale), true)}.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale) throws IOException {
        removeTemplateFromCache(name, locale, null, getEncoding(locale), true);
    }

    /**
     * Equivalent to {@link
     * #removeTemplateFromCache(String, Locale, Object, String, boolean)
     * removeTemplateFromCache(name, thisCfg.getLocale(), null, encoding, true)}.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, String encoding) throws IOException {
        removeTemplateFromCache(name, getLocale(), null, encoding, true);
    }

    /**
     * Equivalent to {@link
     * #removeTemplateFromCache(String, Locale, Object, String, boolean)
     * removeTemplateFromCache(name, locale, null, encoding, true)}.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(String name, Locale locale, String encoding) throws IOException {
        removeTemplateFromCache(name, locale, null, encoding, true);
    }
    
    /**
     * Equivalent to {@link
     * #removeTemplateFromCache(String, Locale, Object, String, boolean)
     * removeTemplateFromCache(name, locale, null, encoding, parse)}.
     * @since 2.3.19
     */
    public void removeTemplateFromCache(
            String name, Locale locale, String encoding, boolean parse)
    throws IOException {
        removeTemplateFromCache(name, locale, null, encoding, parse);
    }    

    /**
     * Removes a template from the template cache, hence forcing the re-loading
     * of it when it's next time requested. This is to give the application
     * finer control over cache updating than {@link #setTemplateUpdateDelay(int)}
     * alone does.
     * 
     * <p>For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, Object, String, boolean, boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     * 
     * @since 2.3.28
     */
    public void removeTemplateFromCache(
            String name, Locale locale, Object customLookupCondition, String encoding, boolean parse)
    throws IOException {
        cache.removeTemplate(name, locale, customLookupCondition, encoding, parse);
    }    
    
    /**
     * The getter pair of {@link #setLocalizedLookup(boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine works.
     */
    public boolean getLocalizedLookup() {
        return cache.getLocalizedLookup();
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
     * <p>Note that changing the value of this setting causes the template cache to be emptied so that old lookup
     * results won't be reused (since 2.3.22). 
     * 
     * <p>
     * Historical note: Despite what the API documentation said earlier, this method is <em>not</em> thread-safe. While
     * setting it can't cause any serious problems, and in fact it works well on most hardware, it's not guaranteed that
     * FreeMarker will see the update in all threads.
     */
    public void setLocalizedLookup(boolean localizedLookup) {
        this.localizedLookup = localizedLookup;
        cache.setLocalizedLookup(localizedLookup);
    }
    
    @Override
    public void setSetting(String name, String value) throws TemplateException {
        boolean unknown = false;
        try {
            if ("TemplateUpdateInterval".equalsIgnoreCase(name)) {
                name = TEMPLATE_UPDATE_DELAY_KEY;
            } else if ("DefaultEncoding".equalsIgnoreCase(name)) {
                name = DEFAULT_ENCODING_KEY;
            }
            
            if (DEFAULT_ENCODING_KEY_SNAKE_CASE.equals(name) || DEFAULT_ENCODING_KEY_CAMEL_CASE.equals(name)) {
                if (JVM_DEFAULT.equalsIgnoreCase(value)) {
                    setDefaultEncoding(getJVMDefaultEncoding()); 
                } else {
                    setDefaultEncoding(value);
                }
            } else if (LOCALIZED_LOOKUP_KEY_SNAKE_CASE.equals(name) || LOCALIZED_LOOKUP_KEY_CAMEL_CASE.equals(name)) {
                setLocalizedLookup(StringUtil.getYesNo(value));
            } else if (STRICT_SYNTAX_KEY_SNAKE_CASE.equals(name) || STRICT_SYNTAX_KEY_CAMEL_CASE.equals(name)) {
                setStrictSyntaxMode(StringUtil.getYesNo(value));
            } else if (WHITESPACE_STRIPPING_KEY_SNAKE_CASE.equals(name)
                    || WHITESPACE_STRIPPING_KEY_CAMEL_CASE.equals(name)) {
                setWhitespaceStripping(StringUtil.getYesNo(value));
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
                    OutputFormat stdOF = STANDARD_OUTPUT_FORMATS.get(value);
                    setOutputFormat(
                            stdOF != null ? stdOF
                            : (OutputFormat) _ObjectBuilderSettingEvaluator.eval(
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
                                + OutputFormat.class.getName() + " instances, in: ", value);
                    }
                }
                setRegisteredCustomOutputFormats(list);
            } else if (RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE.equals(name)
                    || RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetRecognizeStandardFileExtensions();
                } else {
                    setRecognizeStandardFileExtensions(StringUtil.getYesNo(value));
                }
            } else if (CACHE_STORAGE_KEY_SNAKE_CASE.equals(name) || CACHE_STORAGE_KEY_CAMEL_CASE.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT)) {
                    unsetCacheStorage();
                } if (value.indexOf('.') == -1) {
                    int strongSize = 0;
                    int softSize = 0;
                    Map map = StringUtil.parseNameValuePairList(
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
                long multiplier;
                String valueWithoutUnit;
                if (value.endsWith("ms")) {
                    multiplier = 1;
                    valueWithoutUnit = rightTrim(value.substring(0, value.length() - 2));
                } else if (value.endsWith("s")) {
                    multiplier = 1000;
                    valueWithoutUnit = rightTrim(value.substring(0, value.length() - 1));
                } else if (value.endsWith("m")) {
                    multiplier = 1000 * 60;
                    valueWithoutUnit = rightTrim(value.substring(0, value.length() - 1));
                } else if (value.endsWith("h")) {
                    multiplier = 1000 * 60 * 60;
                    valueWithoutUnit = rightTrim(value.substring(0, value.length() - 1));
                } else {
                    multiplier = 1000;  // Default is seconds for backward compatibility
                    valueWithoutUnit = value;
                }
                setTemplateUpdateDelayMilliseconds(Integer.parseInt(valueWithoutUnit) * multiplier);
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
            } else if (INTERPOLATION_SYNTAX_KEY_SNAKE_CASE.equals(name)
                    || INTERPOLATION_SYNTAX_KEY_CAMEL_CASE.equals(name)) {
                if ("legacy".equals(value)) {
                    setInterpolationSyntax(LEGACY_INTERPOLATION_SYNTAX);
                } else if ("dollar".equals(value)) {
                    setInterpolationSyntax(DOLLAR_INTERPOLATION_SYNTAX);
                } else if ("square_bracket".equals(value) || "squareBracket".equals(value)) {
                    setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
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
            } else if (INCOMPATIBLE_ENHANCEMENTS.equals(name)) {
                setIncompatibleEnhancements(value);
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
                    setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_3_0);
                } else if (value.equalsIgnoreCase("default_2_4_0")) {
                    setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
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
            } else if (FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_SNAKE_CASE.equals(name)
                    || FALLBACK_ON_NULL_LOOP_VARIABLE_KEY_CAMEL_CASE.equals(name)) {
                setFallbackOnNullLoopVariable(StringUtil.getYesNo(value));
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
    protected String getCorrectedNameForUnknownSetting(String name) {
        if ("encoding".equals(name) || "charset".equals(name) || "default_charset".equals(name)) {
            // [2.4] Default might change to camel-case
            return DEFAULT_ENCODING_KEY;
        }
        if ("defaultCharset".equals(name)) {
            return DEFAULT_ENCODING_KEY_CAMEL_CASE;
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
    
    private void doAutoIncludes(Environment env, Template t) throws TemplateException, IOException,
            TemplateNotFoundException, MalformedTemplateNameException, ParseException {
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
     * Returns FreeMarker version number string. 
     * 
     * @deprecated Use {@link #getVersion()} instead.
     */
    @Deprecated
    public static String getVersionNumber() {
        return VERSION.toString();
    }
    
    /**
     * Returns FreeMarker version information, most importantly the major.minor.micro version numbers;
     * do NOT use this as the value of the {@code incompatible_improvements} setting (as the parameter to
     * {@link Configuration#Configuration(Version)}), as then your application can break when you upgrade FreeMarker!
     * Use a constant value, like {@link #VERSION_2_3_28}, to protect your application from fixes/changes that aren't
     * entirely backward compatible. Fixes and features that are backward compatible are always enabled. 
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
        if (incompatibleImprovements.intValue() < _VersionInts.V_2_3_21) {
            return ObjectWrapper.DEFAULT_WRAPPER;
        } else {
            return new DefaultObjectWrapperBuilder(incompatibleImprovements).build();
        }
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

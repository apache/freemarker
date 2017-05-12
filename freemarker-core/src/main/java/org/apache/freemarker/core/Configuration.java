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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.UnregisteredOutputFormatException;
import org.apache.freemarker.core.outputformat.impl.CSSOutputFormat;
import org.apache.freemarker.core.outputformat.impl.CombinedMarkupOutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.JSONOutputFormat;
import org.apache.freemarker.core.outputformat.impl.JavaScriptOutputFormat;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XHTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.GetTemplateResult;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.MruCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;
import org.apache.freemarker.core.util.CaptureOutput;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.util.HtmlEscape;
import org.apache.freemarker.core.util.NormalizeNewlines;
import org.apache.freemarker.core.util.StandardCompress;
import org.apache.freemarker.core.util.XmlEscape;
import org.apache.freemarker.core.util._ClassUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.util._UnmodifiableCompositeSet;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * <b>The main entry point into the FreeMarker API</b>; encapsulates the configuration settings of FreeMarker,
 * also serves as a central template-loading and caching service.
 *
 * <p>This class is meant to be used in a singleton pattern. That is, you create an instance of this at the beginning of
 * the application life-cycle with {@link Configuration.Builder}, set its settings
 * (either with the setter methods like {@link Configuration.Builder#setTemplateLoader(TemplateLoader)} or by loading a
 * {@code .properties} file and use that with {@link Configuration.Builder#setSettings(Properties)}}), and then
 * use that single instance everywhere in your application. Frequently re-creating {@link Configuration} is a typical
 * and grave mistake from performance standpoint, as the {@link Configuration} holds the template cache, and often also
 * the class introspection cache, which then will be lost. (Note that, naturally, having multiple long-lived instances,
 * like one per component that internally uses FreeMarker is fine.)  
 * 
 * <p>The basic usage pattern is like:
 * 
 * <pre>
 *  // Where the application is initialized; in general you do this ONLY ONCE in the application life-cycle!
 *  Configuration cfg = new Configuration.Builder(VERSION_<i>X</i>_<i>Y</i>_<i>Z</i>));
 *          .<i>someSetting</i>(...)
 *          .<i>otherSetting</i>(...)
 *          .build()
 *  // VERSION_<i>X</i>_<i>Y</i>_<i>Z</i> enables the not-100%-backward-compatible fixes introduced in
 *  // FreeMarker version X.Y.Z and earlier (see {@link Configuration#getIncompatibleImprovements()}).
 *  ...
 *  
 *  // Later, whenever the application needs a template (so you may do this a lot, and from multiple threads):
 *  {@link Template Template} myTemplate = cfg.{@link #getTemplate(String) getTemplate}("myTemplate.html");
 *  myTemplate.{@link Template#process(Object, java.io.Writer) process}(dataModel, out);</pre>
 * 
 * <p>A couple of settings that you should not leave on its default value are:
 * <ul>
 *   <li>{@link #getTemplateLoader templateLoader}: The default value is {@code null}, so you won't be able to load
 *       anything.
 *   <li>{@link #getSourceEncoding sourceEncoding}: The default value is system dependent, which makes it
 *       fragile on servers, so it should be set explicitly, like to "UTF-8" nowadays. 
 *   <li>{@link #getTemplateExceptionHandler() templateExceptionHandler}: For developing
 *       HTML pages, the most convenient value is {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}. For production,
 *       {@link TemplateExceptionHandler#RETHROW_HANDLER} is safer to use.
 * </ul>
 * 
 * <p>{@link Configuration} is thread-safe and (as of 3.0.0) immutable (apart from internal caches).
 */
public final class Configuration
        implements TopLevelConfiguration, CustomStateScope {
    
    private static final String VERSION_PROPERTIES_PATH = "org/apache/freemarker/core/version.properties";

    private static final String[] SETTING_NAMES_SNAKE_CASE = new String[] {
        // Must be sorted alphabetically!
        ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE,
        ExtendableBuilder.CACHE_STORAGE_KEY_SNAKE_CASE,
        ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE,
        ExtendableBuilder.LOCALIZED_LOOKUP_KEY_SNAKE_CASE,
        ExtendableBuilder.NAMING_CONVENTION_KEY_SNAKE_CASE,
        ExtendableBuilder.OUTPUT_FORMAT_KEY_SNAKE_CASE,
        ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE,
        ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE,
        ExtendableBuilder.SHARED_VARIABLES_KEY_SNAKE_CASE,
        ExtendableBuilder.SOURCE_ENCODING_KEY_SNAKE_CASE,
        ExtendableBuilder.TAB_SIZE_KEY_SNAKE_CASE,
        ExtendableBuilder.TAG_SYNTAX_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_LANGUAGE_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_LOADER_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE,
        ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE,
        ExtendableBuilder.WHITESPACE_STRIPPING_KEY_SNAKE_CASE,
    };

    private static final String[] SETTING_NAMES_CAMEL_CASE = new String[] {
        // Must be sorted alphabetically!
        ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE,
        ExtendableBuilder.CACHE_STORAGE_KEY_CAMEL_CASE,
        ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE,
        ExtendableBuilder.LOCALIZED_LOOKUP_KEY_CAMEL_CASE,
        ExtendableBuilder.NAMING_CONVENTION_KEY_CAMEL_CASE,
        ExtendableBuilder.OUTPUT_FORMAT_KEY_CAMEL_CASE,
        ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE,
        ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
        ExtendableBuilder.SHARED_VARIABLES_KEY_CAMEL_CASE,
        ExtendableBuilder.SOURCE_ENCODING_KEY_CAMEL_CASE,
        ExtendableBuilder.TAB_SIZE_KEY_CAMEL_CASE,
        ExtendableBuilder.TAG_SYNTAX_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_LANGUAGE_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_LOADER_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE,
        ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY_CAMEL_CASE,
        ExtendableBuilder.WHITESPACE_STRIPPING_KEY_CAMEL_CASE
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

    /** FreeMarker version 3.0.0 */
    public static final Version VERSION_3_0_0 = new Version(3, 0, 0);
    
    /** The default of {@link #getIncompatibleImprovements()}, currently {@link #VERSION_3_0_0}. */
    public static final Version DEFAULT_INCOMPATIBLE_IMPROVEMENTS = Configuration.VERSION_3_0_0;
    
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
                
                final Boolean gaeCompliant = Boolean.valueOf(getRequiredVersionProperty(vp, "isGAECompliant"));
                
                VERSION = new Version(versionString, gaeCompliant, null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load and parse " + VERSION_PROPERTIES_PATH, e);
        }
    }

    // Configuration-specific settings:

    private final Version incompatibleImprovements;
    private final DefaultTemplateResolver templateResolver;
    private final boolean localizedLookup;
    private final List<OutputFormat> registeredCustomOutputFormats;
    private final Map<String, OutputFormat> registeredCustomOutputFormatsByName;
    private final Map<String, Object> sharedVariables;
    private final Map<String, TemplateModel> wrappedSharedVariables;

    // ParsingConfiguration settings:

    private final TemplateLanguage templateLanguage;
    private final int tagSyntax;
    private final int namingConvention;
    private final boolean whitespaceStripping;
    private final int autoEscapingPolicy;
    private final OutputFormat outputFormat;
    private final Boolean recognizeStandardFileExtensions;
    private final int tabSize;
    private final Charset sourceEncoding;

    // ProcessingConfiguration settings:

    private final Locale locale;
    private final String numberFormat;
    private final String timeFormat;
    private final String dateFormat;
    private final String dateTimeFormat;
    private final TimeZone timeZone;
    private final TimeZone sqlDateAndTimeTimeZone;
    private final String booleanFormat;
    private final TemplateExceptionHandler templateExceptionHandler;
    private final ArithmeticEngine arithmeticEngine;
    private final ObjectWrapper objectWrapper;
    private final Charset outputEncoding;
    private final Charset urlEscapingCharset;
    private final Boolean autoFlush;
    private final TemplateClassResolver newBuiltinClassResolver;
    private final Boolean showErrorTips;
    private final Boolean apiBuiltinEnabled;
    private final Boolean logTemplateExceptions;
    private final Map<String, TemplateDateFormatFactory> customDateFormats;
    private final Map<String, TemplateNumberFormatFactory> customNumberFormats;
    private final Map<String, String> autoImports;
    private final List<String> autoIncludes;
    private final Boolean lazyImports;
    private final Boolean lazyAutoImports;
    private final Map<Object, Object> customAttributes;

    // CustomStateScope:

    private final ConcurrentHashMap<CustomStateKey, Object> customStateMap = new ConcurrentHashMap<>(0);
    private final Object customStateMapLock = new Object();

    private <SelfT extends ExtendableBuilder<SelfT>> Configuration(ExtendableBuilder<SelfT> builder)
            throws ConfigurationException {
        // Configuration-specific settings:

        incompatibleImprovements = builder.getIncompatibleImprovements();

        templateResolver = new DefaultTemplateResolver(
                builder.getTemplateLoader(),
                builder.getCacheStorage(), builder.getTemplateUpdateDelayMilliseconds(),
                builder.getTemplateLookupStrategy(), builder.getLocalizedLookup(),
                builder.getTemplateNameFormat(),
                builder.getTemplateConfigurations(),
                this);

        localizedLookup = builder.getLocalizedLookup();

        {
            Collection<OutputFormat> registeredCustomOutputFormats = builder.getRegisteredCustomOutputFormats();

            _NullArgumentException.check(registeredCustomOutputFormats);
            Map<String, OutputFormat> registeredCustomOutputFormatsByName = new LinkedHashMap<>(
                    registeredCustomOutputFormats.size() * 4 / 3, 1f);
            for (OutputFormat outputFormat : registeredCustomOutputFormats) {
                String name = outputFormat.getName();
                if (name.equals(UndefinedOutputFormat.INSTANCE.getName())) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The \"" + name + "\" output format can't be redefined",
                            null);
                }
                if (name.equals(PlainTextOutputFormat.INSTANCE.getName())) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The \"" + name + "\" output format can't be redefined",
                            null);
                }
                if (name.length() == 0) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't be 0 long",
                            null);
                }
                if (!Character.isLetterOrDigit(name.charAt(0))) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name must start with letter or digit: " + name,
                            null);
                }
                if (name.indexOf('+') != -1) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't contain \"+\" character: " + name,
                            null);
                }
                if (name.indexOf('{') != -1) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't contain \"{\" character: " + name,
                            null);
                }
                if (name.indexOf('}') != -1) {
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't contain \"}\" character: " + name,
                            null);
                }

                OutputFormat replaced = registeredCustomOutputFormatsByName.put(outputFormat.getName(), outputFormat);
                if (replaced != null) {
                    if (replaced == outputFormat) {
                        throw new IllegalArgumentException(
                                "Duplicate output format in the collection: " + outputFormat);
                    }
                    throw new ConfigurationSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "Clashing output format names between " + replaced + " and " + outputFormat + ".",
                            null);
                }
            }

            this.registeredCustomOutputFormatsByName = registeredCustomOutputFormatsByName;
            this.registeredCustomOutputFormats = Collections.unmodifiableList(new
                    ArrayList<OutputFormat>(registeredCustomOutputFormats));
        }

        ObjectWrapper objectWrapper = builder.getObjectWrapper();

        {
            Map<String, Object> sharedVariables = builder.getSharedVariables();

            HashMap<String, TemplateModel> wrappedSharedVariables = new HashMap<>(
                    (sharedVariables.size() + 5 /* [FM3] 5 legacy vars */) * 4 / 3 + 1, 0.75f);

            // TODO [FM3] Get rid of this
            wrappedSharedVariables.put("capture_output", new CaptureOutput());
            wrappedSharedVariables.put("compress", StandardCompress.INSTANCE);
            wrappedSharedVariables.put("html_escape", new HtmlEscape());
            wrappedSharedVariables.put("normalize_newlines", new NormalizeNewlines());
            wrappedSharedVariables.put("xml_escape", new XmlEscape());

            // In case the inherited sharedVariables aren't empty, we want to merge the two maps:
            wrapAndPutSharedVariables(wrappedSharedVariables, builder.getDefaultSharedVariables(),
                    objectWrapper);
            if (builder.isSharedVariablesSet()) {
                wrapAndPutSharedVariables(wrappedSharedVariables, sharedVariables, objectWrapper);
            }
            this.wrappedSharedVariables = wrappedSharedVariables;
            this.sharedVariables = Collections.unmodifiableMap(new LinkedHashMap<>(sharedVariables));
        }

        // ParsingConfiguration settings:

        templateLanguage = builder.getTemplateLanguage();
        tagSyntax = builder.getTagSyntax();
        namingConvention = builder.getNamingConvention();
        whitespaceStripping = builder.getWhitespaceStripping();
        autoEscapingPolicy = builder.getAutoEscapingPolicy();
        outputFormat = builder.getOutputFormat();
        recognizeStandardFileExtensions = builder.getRecognizeStandardFileExtensions();
        tabSize = builder.getTabSize();
        sourceEncoding = builder.getSourceEncoding();

        // ProcessingConfiguration settings:

        locale = builder.getLocale();
        numberFormat = builder.getNumberFormat();
        timeFormat = builder.getTimeFormat();
        dateFormat = builder.getDateFormat();
        dateTimeFormat = builder.getDateTimeFormat();
        timeZone = builder.getTimeZone();
        sqlDateAndTimeTimeZone = builder.getSQLDateAndTimeTimeZone();
        booleanFormat = builder.getBooleanFormat();
        templateExceptionHandler = builder.getTemplateExceptionHandler();
        arithmeticEngine = builder.getArithmeticEngine();
        this.objectWrapper = objectWrapper;
        outputEncoding = builder.getOutputEncoding();
        urlEscapingCharset = builder.getURLEscapingCharset();
        autoFlush = builder.getAutoFlush();
        newBuiltinClassResolver = builder.getNewBuiltinClassResolver();
        showErrorTips = builder.getShowErrorTips();
        apiBuiltinEnabled = builder.getAPIBuiltinEnabled();
        logTemplateExceptions = builder.getLogTemplateExceptions();
        customDateFormats = Collections.unmodifiableMap(builder.getCustomDateFormats());
        customNumberFormats = Collections.unmodifiableMap(builder.getCustomNumberFormats());
        autoImports = Collections.unmodifiableMap(builder.getAutoImports());
        autoIncludes = Collections.unmodifiableList(builder.getAutoIncludes());
        lazyImports = builder.getLazyImports();
        lazyAutoImports = builder.getLazyAutoImports();
        customAttributes = Collections.unmodifiableMap(builder.getCustomAttributes());
    }

    private <SelfT extends ExtendableBuilder<SelfT>> void wrapAndPutSharedVariables(
            HashMap<String, TemplateModel> wrappedSharedVariables, Map<String, Object> rawSharedVariables,
            ObjectWrapper objectWrapper) throws ConfigurationSettingValueException {
        if (rawSharedVariables.isEmpty()) {
            return;
        }

        for (Entry<String, Object> ent : rawSharedVariables.entrySet()) {
            try {
                wrappedSharedVariables.put(ent.getKey(), objectWrapper.wrap(ent.getValue()));
            } catch (TemplateModelException e) {
                throw new ConfigurationSettingValueException(
                        ExtendableBuilder.SHARED_VARIABLES_KEY, null, false,
                        "Failed to wrap shared variable " + _StringUtil.jQuote(ent.getKey()),
                        e);
            }
        }
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return templateExceptionHandler;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return true;
    }

    private static class DefaultSoftCacheStorage extends SoftCacheStorage {
        // Nothing to override
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateLoader();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateLoaderSet() {
        return true;
    }

    @Override
    public TemplateLookupStrategy getTemplateLookupStrategy() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateLookupStrategy();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateLookupStrategySet() {
        return true;
    }
    
    @Override
    public TemplateNameFormat getTemplateNameFormat() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateNameFormat();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateNameFormatSet() {
        return true;
    }

    @Override
    public TemplateConfigurationFactory getTemplateConfigurations() {
        if (templateResolver == null) {
            return null;
        }
        return templateResolver.getTemplateConfigurations();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateConfigurationsSet() {
        return true;
    }

    @Override
    public CacheStorage getCacheStorage() {
        return templateResolver.getCacheStorage();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isCacheStorageSet() {
        return true;
    }

    @Override
    public long getTemplateUpdateDelayMilliseconds() {
        return templateResolver.getTemplateUpdateDelayMilliseconds();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateUpdateDelayMillisecondsSet() {
        return true;
    }

    @Override
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isWhitespaceStrippingSet() {
        return true;
    }

    /**
     * When auto-escaping should be enabled depending on the current {@linkplain OutputFormat output format};
     * default is {@link ParsingConfiguration#ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}. Note that the default output
     * format, {@link UndefinedOutputFormat}, is a non-escaping format, so there auto-escaping will be off.
     * Note that the templates can turn auto-escaping on/off locally with directives like {@code <#ftl auto_esc=...>},
     * which will ignore the policy.
     *
     * <p><b>About auto-escaping</b></p>
     *
     * <p>
     * Auto-escaping has significance when a value is printed with <code>${...}</code> (or <code>#{...}</code>). If
     * auto-escaping is on, FreeMarker will assume that the value is plain text (as opposed to markup or some kind of
     * rich text), so it will escape it according the current output format (see {@link #getOutputFormat()}
     * and {@link TemplateConfiguration.Builder#setOutputFormat(OutputFormat)}). If auto-escaping is off, FreeMarker
     * will assume that the string value is already in the output format, so it prints it as is to the output.
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
     * <p>Note that what you set here is just a default, which can be overridden for individual templates with the
     * {@linkplain #getTemplateConfigurations() template configurations setting}. This setting is also overridden by
     * the standard file extensions; see them at {@link #getRecognizeStandardFileExtensions()}.
     *
     * @see Configuration.Builder#setAutoEscapingPolicy(int)
     * @see TemplateConfiguration.Builder#setAutoEscapingPolicy(int)
     * @see Configuration.Builder#setOutputFormat(OutputFormat)
     * @see TemplateConfiguration.Builder#setOutputFormat(OutputFormat)
     */
    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isAutoEscapingPolicySet() {
        return true;
    }

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isOutputFormatSet() {
        return true;
    }

    /**
     * Returns the output format for a name.
     * 
     * @param name
     *            Either the name of the output format as it was registered with the
     *            {@link Configuration#getRegisteredCustomOutputFormats registeredCustomOutputFormats} setting,
     *            or a combined output format name.
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
            OutputFormat custOF = registeredCustomOutputFormatsByName.get(name);
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
                registeredNames.addAll(registeredCustomOutputFormatsByName.keySet());
                
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
     * The custom output formats that can be referred by their unique name ({@link OutputFormat#getName()}) from
     * templates. Names are also used to look up the {@link OutputFormat} for standard file extensions; see them at
     * {@link #getRecognizeStandardFileExtensions()}. Each must be different and has a unique name
     * ({@link OutputFormat#getName()}) within this collection.
     *
     * <p>
     * When there's a clash between a custom output format name and a standard output format name, the custom format
     * will win, thus you can override the meaning of standard output format names. Except, it's not allowed to override
     * {@link UndefinedOutputFormat} and {@link PlainTextOutputFormat}.
     *
     * <p>
     * The default value is an empty collection.
     *
     * @throws IllegalArgumentException
     *             When multiple different {@link OutputFormat}-s have the same name in the parameter collection. When
     *             the same {@link OutputFormat} object occurs for multiple times in the collection. If an
     *             {@link OutputFormat} name is 0 long. If an {@link OutputFormat} name doesn't start with letter or
     *             digit. If an {@link OutputFormat} name contains {@code '+'} or <code>'{'</code> or <code>'}'</code>.
     *             If an {@link OutputFormat} name equals to {@link UndefinedOutputFormat#getName()} or
     *             {@link PlainTextOutputFormat#getName()}.
     */
    public Collection<OutputFormat> getRegisteredCustomOutputFormats() {
        return registeredCustomOutputFormats;
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return recognizeStandardFileExtensions == null
                ? true
                : recognizeStandardFileExtensions.booleanValue();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return true;
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return templateLanguage;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTemplateLanguageSet() {
        return true;
    }

    @Override
    public int getTagSyntax() {
        return tagSyntax;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTagSyntaxSet() {
        return true;
    }

    // [FM3] Use enum; won't be needed
    static void validateNamingConventionValue(int namingConvention) {
        if (namingConvention != ParsingConfiguration.AUTO_DETECT_NAMING_CONVENTION
                && namingConvention != ParsingConfiguration.LEGACY_NAMING_CONVENTION
                && namingConvention != ParsingConfiguration.CAMEL_CASE_NAMING_CONVENTION) {
            throw new IllegalArgumentException("\"naming_convention\" can only be set to one of these: "
                    + "Configuration.AUTO_DETECT_NAMING_CONVENTION, "
                    + "or Configuration.LEGACY_NAMING_CONVENTION"
                    + "or Configuration.CAMEL_CASE_NAMING_CONVENTION");
        }
    }

    @Override
    public int getNamingConvention() {
        return namingConvention;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isNamingConventionSet() {
        return true;
    }

    @Override
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTabSizeSet() {
        return true;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isLocaleSet() {
        return true;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTimeZoneSet() {
        return true;
    }

    @Override
    public TimeZone getSQLDateAndTimeTimeZone() {
        return sqlDateAndTimeTimeZone;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return true;
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return arithmeticEngine;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isArithmeticEngineSet() {
        return true;
    }

    @Override
    public String getNumberFormat() {
        return numberFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isNumberFormatSet() {
        return true;
    }

    @Override
    public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
        return customNumberFormats;
    }

    @Override
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        return customNumberFormats.get(name);
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isCustomNumberFormatsSet() {
        return true;
    }

    @Override
    public String getBooleanFormat() {
        return booleanFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isBooleanFormatSet() {
        return true;
    }

    @Override
    public String getTimeFormat() {
        return timeFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isTimeFormatSet() {
        return true;
    }

    @Override
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isDateFormatSet() {
        return true;
    }

    @Override
    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isDateTimeFormatSet() {
        return true;
    }

    @Override
    public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
        return customDateFormats;
    }

    @Override
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        return customDateFormats.get(name);
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isCustomDateFormatsSet() {
        return true;
    }

    @Override
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isObjectWrapperSet() {
        return true;
    }

    @Override
    public Charset getOutputEncoding() {
        return outputEncoding;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isOutputEncodingSet() {
        return true;
    }

    @Override
    public Charset getURLEscapingCharset() {
        return urlEscapingCharset;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isURLEscapingCharsetSet() {
        return true;
    }

    @Override
    public TemplateClassResolver getNewBuiltinClassResolver() {
        return newBuiltinClassResolver;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isNewBuiltinClassResolverSet() {
        return true;
    }

    @Override
    public boolean getAPIBuiltinEnabled() {
        return apiBuiltinEnabled;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isAPIBuiltinEnabledSet() {
        return true;
    }

    @Override
    public boolean getAutoFlush() {
        return autoFlush;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isAutoFlushSet() {
        return true;
    }

    @Override
    public boolean getShowErrorTips() {
        return showErrorTips;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isShowErrorTipsSet() {
        return true;
    }

    @Override
    public boolean getLogTemplateExceptions() {
        return logTemplateExceptions;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isLogTemplateExceptionsSet() {
        return true;
    }

    @Override
    public boolean getLazyImports() {
        return lazyImports;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isLazyImportsSet() {
        return true;
    }

    @Override
    public Boolean getLazyAutoImports() {
        return lazyAutoImports;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isLazyAutoImportsSet() {
        return true;
    }

    @Override
    public Map<String, String> getAutoImports() {
        return autoImports;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isAutoImportsSet() {
        return true;
    }

    @Override
    public List<String> getAutoIncludes() {
        return autoIncludes;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isAutoIncludesSet() {
        return true;
    }

    @Override
    public Map<Object, Object> getCustomAttributes() {
        return customAttributes;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isCustomAttributesSet() {
        return true;
    }

    @Override
    public Object getCustomAttribute(Object key) {
        return customAttributes.get(key);
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
    
    /**
     * Retrieves the template with the given name from the template cache, loading it into the cache first
     * if it's missing/staled.
     * 
     * <p>
     * This is a shorthand for {@link #getTemplate(String, Locale, Serializable, boolean)
     * getTemplate(name, null, null, false)}; see more details there.
     * 
     * <p>
     * See {@link Configuration} for an example of basic usage.
     */
    public Template getTemplate(String name)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, null, null, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Serializable, boolean)
     * getTemplate(name, locale, null, null, false)}.
     */
    public Template getTemplate(String name, Locale locale)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, null, false);
    }

    /**
     * Shorthand for {@link #getTemplate(String, Locale, Serializable, boolean)
     * getTemplate(name, locale, customLookupCondition, false)}.
     */
    public Template getTemplate(String name, Locale locale, Serializable customLookupCondition)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return getTemplate(name, locale, customLookupCondition, false);
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
     *            {@link TemplateLoader} (the {@link TemplateResolver} more generally), but the default
     *            {@link TemplateResolver} has some assumptions. First, the name is expected to be a
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
     *            asterisk (called resource path), the {@link TemplateResolver} (at least the default one) will attempt
     *            to remove the rightmost path component from the base path ("go up one directory") and concatenate
     *            that with the resource path. The process is repeated until either a template is found, or the base
     *            path is completely exhausted.
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
    public Template getTemplate(String name, Locale locale, Serializable customLookupCondition,
            boolean ignoreMissing)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        if (locale == null) {
            locale = getLocale();
        }
        final GetTemplateResult maybeTemp = templateResolver.getTemplate(name, locale, customLookupCondition);
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

    @Override
    public Charset getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isSourceEncodingSet() {
        return true;
    }

    @Override
    public Map<String, Object> getSharedVariables() {
        return sharedVariables;
    }

    @Override
    public boolean isSharedVariablesSet() {
        return true;
    }

    /**
     * Returns the shared variable as a {@link TemplateModel}, or {@code null} if it doesn't exist.
     */
    // TODO [FM3] How the caller can tell if a shared variable exists but null or it's missing?
    public TemplateModel getWrappedSharedVariable(String key) {
        return wrappedSharedVariables.get(key);
    }

    /**
     * Removes all entries from the template cache, thus forcing reloading of templates
     * on subsequent <code>getTemplate</code> calls.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     */
    public void clearTemplateCache() {
        templateResolver.clearTemplateCache();
    }
    
    /**
     * Removes a template from the template cache, hence forcing the re-loading
     * of it when it's next time requested. This is to give the application
     * finer control over cache updating than the
     * {@link #getTemplateUpdateDelayMilliseconds() templateUpdateDelayMilliseconds} setting
     * alone does.
     * 
     * <p>For the meaning of the parameters, see
     * {@link #getTemplate(String, Locale, Serializable, boolean)}.
     * 
     * <p>This method is thread-safe and can be called while the engine processes templates.
     */
    public void removeTemplateFromCache(String name, Locale locale, Serializable customLookupCondition)
            throws IOException {
        templateResolver.removeTemplateFromCache(name, locale, customLookupCondition);
    }

    @Override
    public boolean getLocalizedLookup() {
        return templateResolver.getLocalizedLookup();
    }

    /**
     * Always {@code true} in {@link Configuration}-s, so calling the corresponding getter is always safe.
     */
    @Override
    public boolean isLocalizedLookupSet() {
        return true;
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
     *            One of {@link ParsingConfiguration#AUTO_DETECT_NAMING_CONVENTION},
     *            {@link ParsingConfiguration#LEGACY_NAMING_CONVENTION}, and
     *            {@link ParsingConfiguration#CAMEL_CASE_NAMING_CONVENTION}. If it's
     *            {@link ParsingConfiguration#AUTO_DETECT_NAMING_CONVENTION} then the union
     *            of the names in all the naming conventions is returned.
     * 
     * @since 2.3.24
     */
    public Set<String> getSupportedBuiltInNames(int namingConvention) {
        Set<String> names;
        if (namingConvention == ParsingConfiguration.AUTO_DETECT_NAMING_CONVENTION) {
            names = ASTExpBuiltIn.BUILT_INS_BY_NAME.keySet();
        } else if (namingConvention == ParsingConfiguration.LEGACY_NAMING_CONVENTION) {
            names = ASTExpBuiltIn.SNAKE_CASE_NAMES;
        } else if (namingConvention == ParsingConfiguration.CAMEL_CASE_NAMING_CONVENTION) {
            names = ASTExpBuiltIn.CAMEL_CASE_NAMES;
        } else {
            throw new IllegalArgumentException("Unsupported naming convention constant: " + namingConvention);
        }
        return Collections.unmodifiableSet(names);
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
     *            One of {@link ParsingConfiguration#AUTO_DETECT_NAMING_CONVENTION},
     *            {@link ParsingConfiguration#LEGACY_NAMING_CONVENTION}, and
     *            {@link ParsingConfiguration#CAMEL_CASE_NAMING_CONVENTION}. If it's
     *            {@link ParsingConfiguration#AUTO_DETECT_NAMING_CONVENTION} then the union
     *            of the names in all the naming conventions is returned. 
     * 
     * @since 2.3.24
     */
    public Set<String> getSupportedBuiltInDirectiveNames(int namingConvention) {
        if (namingConvention == AUTO_DETECT_NAMING_CONVENTION) {
            return ASTDirective.ALL_BUILT_IN_DIRECTIVE_NAMES;
        } else if (namingConvention == LEGACY_NAMING_CONVENTION) {
            return ASTDirective.LEGACY_BUILT_IN_DIRECTIVE_NAMES;
        } else if (namingConvention == CAMEL_CASE_NAMING_CONVENTION) {
            return ASTDirective.CAMEL_CASE_BUILT_IN_DIRECTIVE_NAMES;
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

    /**
     * Usually you use {@link Builder} instead of this abstract class, except where you declare the type of a method
     * parameter or field, where the more generic {@link ExtendableBuilder} should be used. {@link ExtendableBuilder}
     * might have other subclasses than {@link Builder}, because some applications needs different setting defaults
     * or other changes.
     */
    public abstract static class ExtendableBuilder<SelfT extends ExtendableBuilder<SelfT>>
            extends MutableParsingAndProcessingConfiguration<SelfT>
            implements TopLevelConfiguration, CommonBuilder<Configuration> {

        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String SOURCE_ENCODING_KEY_SNAKE_CASE = "source_encoding";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String SOURCE_ENCODING_KEY = SOURCE_ENCODING_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String SOURCE_ENCODING_KEY_CAMEL_CASE = "sourceEncoding";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String LOCALIZED_LOOKUP_KEY_SNAKE_CASE = "localized_lookup";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String LOCALIZED_LOOKUP_KEY = LOCALIZED_LOOKUP_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String LOCALIZED_LOOKUP_KEY_CAMEL_CASE = "localizedLookup";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String WHITESPACE_STRIPPING_KEY_SNAKE_CASE = "whitespace_stripping";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String WHITESPACE_STRIPPING_KEY = WHITESPACE_STRIPPING_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String WHITESPACE_STRIPPING_KEY_CAMEL_CASE = "whitespaceStripping";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
        public static final String OUTPUT_FORMAT_KEY_SNAKE_CASE = "output_format";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String OUTPUT_FORMAT_KEY = OUTPUT_FORMAT_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
        public static final String OUTPUT_FORMAT_KEY_CAMEL_CASE = "outputFormat";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
        public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE = "recognize_standard_file_extensions";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY
                = RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
        public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE = "recognizeStandardFileExtensions";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
        public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE = "registered_custom_output_formats";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY = REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
        public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE = "registeredCustomOutputFormats";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
        public static final String AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE = "auto_escaping_policy";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String AUTO_ESCAPING_POLICY_KEY = AUTO_ESCAPING_POLICY_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
        public static final String AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE = "autoEscapingPolicy";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String CACHE_STORAGE_KEY_SNAKE_CASE = "cache_storage";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String CACHE_STORAGE_KEY = CACHE_STORAGE_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String CACHE_STORAGE_KEY_CAMEL_CASE = "cacheStorage";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE = "template_update_delay";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TEMPLATE_UPDATE_DELAY_KEY = TEMPLATE_UPDATE_DELAY_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_UPDATE_DELAY_KEY_CAMEL_CASE = "templateUpdateDelay";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String AUTO_INCLUDE_KEY_SNAKE_CASE = "auto_include";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String AUTO_INCLUDE_KEY = AUTO_INCLUDE_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String AUTO_INCLUDE_KEY_CAMEL_CASE = "autoInclude";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LANGUAGE_KEY_SNAKE_CASE = "template_language";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TEMPLATE_LANGUAGE_KEY = TEMPLATE_LANGUAGE_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LANGUAGE_KEY_CAMEL_CASE = "templateLanguage";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TAG_SYNTAX_KEY_SNAKE_CASE = "tag_syntax";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TAG_SYNTAX_KEY = TAG_SYNTAX_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TAG_SYNTAX_KEY_CAMEL_CASE = "tagSyntax";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String NAMING_CONVENTION_KEY_SNAKE_CASE = "naming_convention";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String NAMING_CONVENTION_KEY = NAMING_CONVENTION_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String NAMING_CONVENTION_KEY_CAMEL_CASE = "namingConvention";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.25 */
        public static final String TAB_SIZE_KEY_SNAKE_CASE = "tab_size";
        /** Alias to the {@code ..._SNAKE_CASE} variation. @since 2.3.25 */
        public static final String TAB_SIZE_KEY = TAB_SIZE_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.25 */
        public static final String TAB_SIZE_KEY_CAMEL_CASE = "tabSize";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LOADER_KEY_SNAKE_CASE = "template_loader";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TEMPLATE_LOADER_KEY = TEMPLATE_LOADER_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LOADER_KEY_CAMEL_CASE = "templateLoader";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE = "template_lookup_strategy";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TEMPLATE_LOOKUP_STRATEGY_KEY = TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE = "templateLookupStrategy";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE = "template_name_format";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String TEMPLATE_NAME_FORMAT_KEY = TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE = "templateNameFormat";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. */
        public static final String SHARED_VARIABLES_KEY_SNAKE_CASE = "shared_variables";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String SHARED_VARIABLES_KEY = SHARED_VARIABLES_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. */
        public static final String SHARED_VARIABLES_KEY_CAMEL_CASE = "sharedVariables";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.24 */
        public static final String TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE = "template_configurations";
        /** Alias to the {@code ..._SNAKE_CASE} variation. @since 2.3.24 */
        public static final String TEMPLATE_CONFIGURATIONS_KEY = TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.24 */
        public static final String TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE = "templateConfigurations";
        /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
        public static final String INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE = "incompatible_improvements";
        /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
        public static final String INCOMPATIBLE_IMPROVEMENTS_KEY = INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE;
        /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
        public static final String INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE = "incompatibleImprovements";
        // Set early in the constructor to non-null
        private Version incompatibleImprovements = Configuration.VERSION_3_0_0;

        private TemplateLoader templateLoader;
        private boolean templateLoaderSet;
        private CacheStorage cacheStorage;
        private CacheStorage cachedDefaultCacheStorage;
        private TemplateLookupStrategy templateLookupStrategy;
        private TemplateNameFormat templateNameFormat;
        private TemplateConfigurationFactory templateConfigurations;
        private boolean templateConfigurationsSet;
        private Long templateUpdateDelayMilliseconds;
        private Boolean localizedLookup;

        private Collection<OutputFormat> registeredCustomOutputFormats;
        private Map<String, Object> sharedVariables;

        /**
         * @param incompatibleImprovements
         *         The inital value of the {@link Configuration#getIncompatibleImprovements() incompatibleImprovements};
         *         can't {@code null}. This can be later changed via {@link #setIncompatibleImprovements(Version)}. The
         *         point here is just to ensure that it's never {@code null}.
         */
        protected ExtendableBuilder(Version incompatibleImprovements) {
            _NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
            this.incompatibleImprovements = incompatibleImprovements;
        }

        @Override
        public Configuration build() throws ConfigurationException {
            return new Configuration(this);
        }

        @Override
        public void setSetting(String name, String value) throws ConfigurationException {
            boolean unknown = false;
            try {
                if ("TemplateUpdateInterval".equalsIgnoreCase(name)) {
                    name = TEMPLATE_UPDATE_DELAY_KEY;
                } else if ("DefaultEncoding".equalsIgnoreCase(name)) {
                    name = SOURCE_ENCODING_KEY;
                }

                if (SOURCE_ENCODING_KEY_SNAKE_CASE.equals(name) || SOURCE_ENCODING_KEY_CAMEL_CASE.equals(name)) {
                    if (JVM_DEFAULT_VALUE.equalsIgnoreCase(value)) {
                        setSourceEncoding(Charset.defaultCharset());
                    } else {
                        setSourceEncoding(Charset.forName(value));
                    }
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
                        throw new ConfigurationSettingValueException( name, value,
                                "No such predefined auto escaping policy name");
                    }
                } else if (OUTPUT_FORMAT_KEY_SNAKE_CASE.equals(name) || OUTPUT_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
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
                            throw new ConfigurationSettingValueException(name, value,
                                    "List items must be " + OutputFormat.class.getName() + " instances.");
                        }
                    }
                    setRegisteredCustomOutputFormats(list);
                } else if (RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE.equals(name)
                        || RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetRecognizeStandardFileExtensions();
                    } else {
                        setRecognizeStandardFileExtensions(_StringUtil.getYesNo(value));
                    }
                } else if (CACHE_STORAGE_KEY_SNAKE_CASE.equals(name) || CACHE_STORAGE_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetCacheStorage();
                    } if (value.indexOf('.') == -1) {
                        int strongSize = 0;
                        int softSize = 0;
                        Map map = _StringUtil.parseNameValuePairList(
                                value, String.valueOf(Integer.MAX_VALUE));
                        Iterator it = map.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry ent = (Map.Entry) it.next();
                            String pName = (String) ent.getKey();
                            int pValue;
                            try {
                                pValue = Integer.parseInt((String) ent.getValue());
                            } catch (NumberFormatException e) {
                                throw new ConfigurationSettingValueException(name, value,
                                        "Malformed integer number (shown quoted): " + _StringUtil.jQuote(ent.getValue()));
                            }
                            if ("soft".equalsIgnoreCase(pName)) {
                                softSize = pValue;
                            } else if ("strong".equalsIgnoreCase(pName)) {
                                strongSize = pValue;
                            } else {
                                throw new ConfigurationSettingValueException(name, value,
                                        "Unsupported cache parameter name (shown quoted): "
                                                + _StringUtil.jQuote(ent.getValue()));
                            }
                        }
                        if (softSize == 0 && strongSize == 0) {
                            throw new ConfigurationSettingValueException(name, value,
                                    "Either cache soft- or strong size must be set and non-0.");
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
                        throw new ConfigurationSettingValueException(name, value,
                                "Unrecognized time unit " + _StringUtil.jQuote(unit) + ". Valid units are: ms, s, m, h");
                    } else {
                        multipier = 0;
                    }

                    int parsedValue = Integer.parseInt(valueWithoutUnit);
                    if (multipier == 0 && parsedValue != 0) {
                        throw new ConfigurationSettingValueException(name, value,
                                "Time unit must be specified for a non-0 value (examples: 500 ms, 3 s, 2 m, 1 h).");
                    }

                    setTemplateUpdateDelayMilliseconds(parsedValue * multipier);
                } else if (SHARED_VARIABLES_KEY_SNAKE_CASE.equals(name)
                        || SHARED_VARIABLES_KEY_CAMEL_CASE.equals(name)) {
                    Map<?, ?> sharedVariables = (Map<?, ?>) _ObjectBuilderSettingEvaluator.eval(
                            value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                    for (Object key : sharedVariables.keySet()) {
                        if (!(key instanceof String)) {
                            throw new ConfigurationSettingValueException(name, null, false,
                                    "All keys in this Map must be strings, but one of them is an instance of "
                                    + "this class: " + _ClassUtil.getShortClassNameOfObject(key), null);
                        }
                    }
                    setSharedVariables((Map) sharedVariables);
                } else if (TEMPLATE_LANGUAGE_KEY_SNAKE_CASE.equals(name) || TEMPLATE_LANGUAGE_KEY_CAMEL_CASE.equals(name)) {
                    if ("FTL".equals(value)) {
                        setTemplateLanguage(TemplateLanguage.FTL);
                    } else if ("static_text".equals(value) || "staticText".equals(value)) {
                        setTemplateLanguage(TemplateLanguage.STATIC_TEXT);
                    } else {
                        throw new ConfigurationSettingValueException(name, value, "Unsupported template language name");
                    }
                } else if (TAG_SYNTAX_KEY_SNAKE_CASE.equals(name) || TAG_SYNTAX_KEY_CAMEL_CASE.equals(name)) {
                    if ("auto_detect".equals(value) || "autoDetect".equals(value)) {
                        setTagSyntax(AUTO_DETECT_TAG_SYNTAX);
                    } else if ("angle_bracket".equals(value) || "angleBracket".equals(value)) {
                        setTagSyntax(ANGLE_BRACKET_TAG_SYNTAX);
                    } else if ("square_bracket".equals(value) || "squareBracket".equals(value)) {
                        setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
                    } else {
                        throw new ConfigurationSettingValueException(name, value, "No such predefined tag syntax name");
                    }
                } else if (NAMING_CONVENTION_KEY_SNAKE_CASE.equals(name) || NAMING_CONVENTION_KEY_CAMEL_CASE.equals(name)) {
                    if ("auto_detect".equals(value) || "autoDetect".equals(value)) {
                        setNamingConvention(AUTO_DETECT_NAMING_CONVENTION);
                    } else if ("legacy".equals(value)) {
                        setNamingConvention(LEGACY_NAMING_CONVENTION);
                    } else if ("camel_case".equals(value) || "camelCase".equals(value)) {
                        setNamingConvention(CAMEL_CASE_NAMING_CONVENTION);
                    } else {
                        throw new ConfigurationSettingValueException(name, value,
                                "No such predefined naming convention name.");
                    }
                } else if (TAB_SIZE_KEY_SNAKE_CASE.equals(name) || TAB_SIZE_KEY_CAMEL_CASE.equals(name)) {
                    setTabSize(Integer.parseInt(value));
                } else if (INCOMPATIBLE_IMPROVEMENTS_KEY_SNAKE_CASE.equals(name)
                        || INCOMPATIBLE_IMPROVEMENTS_KEY_CAMEL_CASE.equals(name)) {
                    setIncompatibleImprovements(new Version(value));
                } else if (TEMPLATE_LOADER_KEY_SNAKE_CASE.equals(name) || TEMPLATE_LOADER_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateLoader();
                    } else {
                        setTemplateLoader((TemplateLoader) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateLoader.class, true, _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_LOOKUP_STRATEGY_KEY_SNAKE_CASE.equals(name)
                        || TEMPLATE_LOOKUP_STRATEGY_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateLookupStrategy();
                    } else {
                        setTemplateLookupStrategy((TemplateLookupStrategy) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateLookupStrategy.class, false, _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_NAME_FORMAT_KEY_SNAKE_CASE.equals(name)
                        || TEMPLATE_NAME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateNameFormat();
                    } else if (value.equalsIgnoreCase("default_2_3_0")) {
                        setTemplateNameFormat(DefaultTemplateNameFormatFM2.INSTANCE);
                    } else if (value.equalsIgnoreCase("default_2_4_0")) {
                        setTemplateNameFormat(DefaultTemplateNameFormat.INSTANCE);
                    } else {
                        throw new ConfigurationSettingValueException(name, value,
                                "No such predefined template name format");
                    }
                } else if (TEMPLATE_CONFIGURATIONS_KEY_SNAKE_CASE.equals(name)
                        || TEMPLATE_CONFIGURATIONS_KEY_CAMEL_CASE.equals(name)) {
                    if (value.equals(NULL_VALUE)) {
                        setTemplateConfigurations(null);
                    } else {
                        setTemplateConfigurations((TemplateConfigurationFactory) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateConfigurationFactory.class, false, _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else {
                    unknown = true;
                }
            } catch (ConfigurationSettingValueException e) {
                throw e;
            } catch (Exception e) {
                throw new ConfigurationSettingValueException(name, value, e);
            }
            if (unknown) {
                super.setSetting(name, value);
            }
        }

        /**
         * Returns the valid {@link Configuration} setting names. Naturally, this includes the {@link MutableProcessingConfiguration} setting
         * names too.
         *
         * @param camelCase
         *            If we want the setting names with camel case naming convention, or with snake case (legacy) naming
         *            convention.
         *
         * @see MutableProcessingConfiguration#getSettingNames(boolean)
         */
        public static Set<String> getSettingNames(boolean camelCase) {
            return new _UnmodifiableCompositeSet<>(
                    MutableProcessingConfiguration.getSettingNames(camelCase),
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
            if ("encoding".equals(name) || "default_encoding".equals(name) || "charset".equals(name) || "default_charset"
                    .equals(name)) {
                // [2.4] Default might changes to camel-case
                return SOURCE_ENCODING_KEY;
            }
            if ("defaultEncoding".equals(name) || "defaultCharset".equals(name)) {
                return SOURCE_ENCODING_KEY_CAMEL_CASE;
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
        public TemplateLoader getTemplateLoader() {
            return isTemplateLoaderSet() ? templateLoader : getDefaultTemplateLoader();
        }

        @Override
        public boolean isTemplateLoaderSet() {
            return templateLoaderSet;
        }

        protected TemplateLoader getDefaultTemplateLoader() {
            return null;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateLoader()}.
         */
        public void setTemplateLoader(TemplateLoader templateLoader) {
            this.templateLoader = templateLoader;
            templateLoaderSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateLoader(TemplateLoader)}
         */
        public SelfT templateLoader(TemplateLoader templateLoader) {
            setTemplateLoader(templateLoader);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateLoader() {
            templateLoader = null;
            templateLoaderSet = false;
        }

        @Override
        public CacheStorage getCacheStorage() {
            return isCacheStorageSet() ? cacheStorage : getDefaultCacheStorage();
        }

        @Override
        public boolean isCacheStorageSet() {
            return cacheStorage != null;
        }

        protected CacheStorage getDefaultCacheStorage() {
            if (cachedDefaultCacheStorage == null) {
                // If this will depend on incompatibleImprovements, null it out in onIncompatibleImprovementsChanged()!
                cachedDefaultCacheStorage = new DefaultSoftCacheStorage();
            }
            return cachedDefaultCacheStorage;
        }

        /**
         * Setter pair of {@link Configuration#getCacheStorage()}
         */
        public void setCacheStorage(CacheStorage cacheStorage) {
            this.cacheStorage = cacheStorage;
            cachedDefaultCacheStorage = null;
        }

        /**
         * Fluent API equivalent of {@link #setCacheStorage(CacheStorage)}
         */
        public SelfT cacheStorage(CacheStorage cacheStorage) {
            setCacheStorage(cacheStorage);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetCacheStorage() {
            cacheStorage = null;
        }

        @Override
        public TemplateLookupStrategy getTemplateLookupStrategy() {
            return isTemplateLookupStrategySet() ? templateLookupStrategy : getDefaultTemplateLookupStrategySet();
        }

        @Override
        public boolean isTemplateLookupStrategySet() {
            return templateLookupStrategy != null;
        }

        protected TemplateLookupStrategy getDefaultTemplateLookupStrategySet() {
            return DefaultTemplateLookupStrategy.INSTANCE;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateLookupStrategy()}.
         */
        public void setTemplateLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
            _NullArgumentException.check("templateLookupStrategy", templateLookupStrategy);
            this.templateLookupStrategy = templateLookupStrategy;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateLookupStrategy(TemplateLookupStrategy)}
         */
        public SelfT templateLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
            setTemplateLookupStrategy(templateLookupStrategy);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateLookupStrategy() {
            templateLookupStrategy = null;
        }

        @Override
        public TemplateNameFormat getTemplateNameFormat() {
            return isTemplateNameFormatSet() ? templateNameFormat : getDefaultTemplateNameFormat();
        }

        /**
         * Tells if this setting was explicitly set (if not, the default value of the setting will be used).
         */
        @Override
        public boolean isTemplateNameFormatSet() {
            return  templateNameFormat != null;
        }

        protected TemplateNameFormat getDefaultTemplateNameFormat() {
            return DefaultTemplateNameFormatFM2.INSTANCE;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateNameFormat()}.
         */
        public void setTemplateNameFormat(TemplateNameFormat templateNameFormat) {
            _NullArgumentException.check("templateNameFormat", templateNameFormat);
            this.templateNameFormat = templateNameFormat;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateNameFormat(TemplateNameFormat)}
         */
        public SelfT templateNameFormat(TemplateNameFormat templateNameFormat) {
            setTemplateNameFormat(templateNameFormat);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateNameFormat() {
            this.templateNameFormat = null;
        }

        @Override
        public TemplateConfigurationFactory getTemplateConfigurations() {
            return isTemplateConfigurationsSet() ? templateConfigurations : getDefaultTemplateConfigurations();
        }

        @Override
        public boolean isTemplateConfigurationsSet() {
            return templateConfigurationsSet;
        }

        protected TemplateConfigurationFactory getDefaultTemplateConfigurations() {
            return null;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateConfigurations()}.
         */
        public void setTemplateConfigurations(TemplateConfigurationFactory templateConfigurations) {
            this.templateConfigurations = templateConfigurations;
            templateConfigurationsSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateConfigurations(TemplateConfigurationFactory)}
         */
        public SelfT templateConfigurations(TemplateConfigurationFactory templateConfigurations) {
            setTemplateConfigurations(templateConfigurations);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateConfigurations() {
            this.templateConfigurations = null;
            templateConfigurationsSet = false;
        }

        @Override
        public long getTemplateUpdateDelayMilliseconds() {
            return isTemplateUpdateDelayMillisecondsSet() ? templateUpdateDelayMilliseconds
                    : getDefaultTemplateUpdateDelayMilliseconds();
        }

        @Override
        public boolean isTemplateUpdateDelayMillisecondsSet() {
            return templateUpdateDelayMilliseconds != null;
        }

        protected long getDefaultTemplateUpdateDelayMilliseconds() {
            return 5000;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateUpdateDelayMilliseconds()}.
         */
        public void setTemplateUpdateDelayMilliseconds(long templateUpdateDelayMilliseconds) {
            this.templateUpdateDelayMilliseconds = templateUpdateDelayMilliseconds;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateUpdateDelayMilliseconds(long)}
         */
        public SelfT templateUpdateDelayMilliseconds(long templateUpdateDelayMilliseconds) {
            setTemplateUpdateDelayMilliseconds(templateUpdateDelayMilliseconds);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateUpdateDelayMilliseconds() {
            templateUpdateDelayMilliseconds = null;
        }

        @Override
        public boolean getLocalizedLookup() {
            return isLocalizedLookupSet() ? localizedLookup : getDefaultLocalizedLookup();
        }

        @Override
        public boolean isLocalizedLookupSet() {
            return localizedLookup != null;
        }

        protected boolean getDefaultLocalizedLookup() {
            return true;
        }

        /**
         * Setter pair of {@link Configuration#getLocalizedLookup()}.
         */
        public void setLocalizedLookup(Boolean localizedLookup) {
            this.localizedLookup = localizedLookup;
        }

        /**
         * Fluent API equivalent of {@link #setLocalizedLookup(Boolean)}
         */
        public SelfT localizedLookup(Boolean localizedLookup) {
            setLocalizedLookup(localizedLookup);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetLocalizedLookup() {
            this.localizedLookup = null;
        }

        public Collection<OutputFormat> getRegisteredCustomOutputFormats() {
            return isRegisteredCustomOutputFormatsSet() ? registeredCustomOutputFormats
                    : getDefaultRegisteredCustomOutputFormats();
        }

        /**
         * Tells if this setting was explicitly set (if not, the default value of the setting will be used).
         */
        public boolean isRegisteredCustomOutputFormatsSet() {
            return registeredCustomOutputFormats != null;
        }

        protected Collection<OutputFormat> getDefaultRegisteredCustomOutputFormats() {
            return Collections.emptyList();
        }

        /**
         * Setter pair of {@link Configuration#getRegisteredCustomOutputFormats()}.
         */
        public void setRegisteredCustomOutputFormats(Collection<OutputFormat> registeredCustomOutputFormats) {
            _NullArgumentException.check("registeredCustomOutputFormats", registeredCustomOutputFormats);
            this.registeredCustomOutputFormats = registeredCustomOutputFormats;
        }

        /**
         * Fluent API equivalent of {@link #setRegisteredCustomOutputFormats(Collection)}
         */
        public SelfT registeredCustomOutputFormats(Collection<OutputFormat> registeredCustomOutputFormats) {
            setRegisteredCustomOutputFormats(registeredCustomOutputFormats);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetRegisteredCustomOutputFormats() {
            this.registeredCustomOutputFormats = null;
        }

        @Override
        public Map<String, Object> getSharedVariables() {
            return isSharedVariablesSet() ? sharedVariables : getDefaultSharedVariables();
        }

        /**
         * Returns the shared variable, or {@code null} if it doesn't exist. If the shared variables weren't set,
         * it will also try to find it in the default shared variables (which is an empty map in the standard
         * implementation).
         */
        // TODO [FM3] How the caller can tell if a shared variable exists but null or it's missing?
        public Object getSharedVariable(String key) {
            return isSharedVariablesSet() ? sharedVariables.get(key) : getDefaultSharedVariables().get(key);
        }

        @Override
        public boolean isSharedVariablesSet() {
            return sharedVariables != null;
        }

        protected Map<String, Object> getDefaultSharedVariables() {
            return Collections.emptyMap();
        }

        /**
         * Setter pair of {@link Configuration#getSharedVariables()}.
         */
        public void setSharedVariables(Map<String, Object> sharedVariables) {
            _NullArgumentException.check("sharedVariables", sharedVariables);
            this.sharedVariables = sharedVariables;
        }

        /**
         * Convenience method for {@link #getSharedVariables()} and then {@link Map#put(Object, Object)}, with the
         * extra that it also creates an empty {@link HashMap} of shared variables if the shared variables {@link Map}
         * wasn't set yet.
         */
        public void setSharedVariable(String name, Object value) {
            if (!isSharedVariablesSet()) {
                setSharedVariables(new HashMap<String, Object>());
            }
            getSharedVariables().put(name, value);
        }

        /**
         * Fluent API equivalent of {@link #setSharedVariable(String, Object)}
         */
        public SelfT sharedVariable(String name, Object value) {
            setSharedVariable(name, value);
            return self();
        }

        public void unsetSharedVariables() {
            this.sharedVariables = null;
        }

        @Override
        protected int getDefaultTagSyntax() {
            return ANGLE_BRACKET_TAG_SYNTAX;
        }

        @Override
        protected TemplateLanguage getDefaultTemplateLanguage() {
            return TemplateLanguage.FTL;
        }

        @Override
        protected int getDefaultNamingConvention() {
            return AUTO_DETECT_NAMING_CONVENTION;
        }

        @Override
        public Version getIncompatibleImprovements() {
            return incompatibleImprovements;
        }

        /**
         * Setter pair of {@link Configuration#getIncompatibleImprovements()}.
         *
         * <p>This is not called from the {@link ExtendableBuilder#ExtendableBuilder(Version)}; the initial value is set
         * without this.
         *
         * @param incompatibleImprovements
         *         Not {@code null}. Must be a supported version (for example not a future version).
         *
         * @throws IllegalArgumentException
         *             If {@code incompatibleImmprovements} refers to a version that wasn't released yet when the currently
         *             used FreeMarker version was released, or is less than 3.0.0, or is {@code null}.
         */
        public void setIncompatibleImprovements(Version incompatibleImprovements) {
            _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
            Version previousIncompatibleImprovements = this.incompatibleImprovements;
            this.incompatibleImprovements = incompatibleImprovements;
            if (!incompatibleImprovements.equals(previousIncompatibleImprovements)) {
                onIncompatibleImprovementsChanged(previousIncompatibleImprovements);
            }
        }

        /**
         * Fluent API equivalent of {@link #setIncompatibleImprovements(Version)}
         */
        public SelfT incompatibleImprovements(Version incompatibleImprovements) {
            setIncompatibleImprovements(incompatibleImprovements);
            return self();
        }

        /**
         * Invoked by {@link #setIncompatibleImprovements(Version)} when the value is changed by it. This is not invoked
         * when {@linkplain #getIncompatibleImprovements()} incompatibleImprovements} is initialized (i.e., when the
         * previous value was {@code null}), nor if {@link #setIncompatibleImprovements(Version)} was called with a
         * value that's equivalent with the previous value. When this method is called, {@link
         * #getIncompatibleImprovements()} will already return the new value.
         * <p>
         * This method is typically used to drop cached default values that are {@linkplain
         * #getIncompatibleImprovements()} incompatibleImprovements} dependent.
         * <p>
         * If you override this method, don't forget to call the super method.
         *
         * @param previousIncompatibleImprovements
         *         The value of incompatibleImprovements before it was changed. Not {@code null}.
         */
        protected void onIncompatibleImprovementsChanged(Version previousIncompatibleImprovements) {
            cachedDefaultObjectWrapper = null;
        }

        @Override
        protected boolean getDefaultWhitespaceStripping() {
            return true;
        }

        @Override
        protected int getDefaultAutoEscapingPolicy() {
            return ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY;
        }

        @Override
        protected OutputFormat getDefaultOutputFormat() {
            return UndefinedOutputFormat.INSTANCE;
        }

        @Override
        protected boolean getDefaultRecognizeStandardFileExtensions() {
            return true;
        }

        @Override
        protected Charset getDefaultSourceEncoding() {
            return Charset.defaultCharset();
        }

        @Override
        protected int getDefaultTabSize() {
            return 8;
        }

        @Override
        protected Locale getDefaultLocale() {
            return Locale.getDefault();
        }

        @Override
        protected TimeZone getDefaultTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
        protected TimeZone getDefaultSQLDateAndTimeTimeZone() {
            return null;
        }

        @Override
        protected String getDefaultNumberFormat() {
            return "number";
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats() {
            return Collections.emptyMap();
        }

        @Override
        protected TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name) {
            return null;
        }

        @Override
        protected String getDefaultBooleanFormat() {
            return TemplateBooleanFormat.C_TRUE_FALSE;
        }

        @Override
        protected String getDefaultTimeFormat() {
            return "";
        }

        @Override
        protected String getDefaultDateFormat() {
            return "";
        }

        @Override
        protected String getDefaultDateTimeFormat() {
            return "";
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats() {
            return Collections.emptyMap();
        }

        @Override
        protected TemplateDateFormatFactory getDefaultCustomDateFormat(String name) {
            return null;
        }

        @Override
        protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
            return TemplateExceptionHandler.DEBUG_HANDLER; // [FM3] RETHROW;
        }

        @Override
        protected ArithmeticEngine getDefaultArithmeticEngine() {
            return BigDecimalArithmeticEngine.INSTANCE;
        }

        private DefaultObjectWrapper cachedDefaultObjectWrapper;

        @Override
        protected ObjectWrapper getDefaultObjectWrapper() {
            if (cachedDefaultObjectWrapper == null) {
                // Note: This field is cleared by onIncompatibleImprovementsChanged
                Version incompatibleImprovements = getIncompatibleImprovements();
                cachedDefaultObjectWrapper = new DefaultObjectWrapper.Builder(incompatibleImprovements).build();
            }
            return cachedDefaultObjectWrapper;
        }

        @Override
        public void setObjectWrapper(ObjectWrapper objectWrapper) {
            super.setObjectWrapper(objectWrapper);
            if (objectWrapper != cachedDefaultObjectWrapper) {
                // Just to make it GC-able
                cachedDefaultObjectWrapper = null;
            }
        }

        @Override
        protected Charset getDefaultOutputEncoding() {
            return null;
        }

        @Override
        protected Charset getDefaultURLEscapingCharset() {
            return null;
        }

        @Override
        protected TemplateClassResolver getDefaultNewBuiltinClassResolver() {
            return TemplateClassResolver.UNRESTRICTED_RESOLVER;
        }

        @Override
        protected boolean getDefaultAutoFlush() {
            return true;
        }

        @Override
        protected boolean getDefaultShowErrorTips() {
            return true;
        }

        @Override
        protected boolean getDefaultAPIBuiltinEnabled() {
            return false;
        }

        @Override
        protected boolean getDefaultLogTemplateExceptions() {
            return false;
        }

        @Override
        protected boolean getDefaultLazyImports() {
            return false;
        }

        @Override
        @SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
        protected Boolean getDefaultLazyAutoImports() {
            return null;
        }

        @Override
        protected Map<String, String> getDefaultAutoImports() {
            return Collections.emptyMap();
        }

        @Override
        protected List<String> getDefaultAutoIncludes() {
            return Collections.emptyList();
        }

        @Override
        protected Object getDefaultCustomAttribute(Object name) {
            return Collections.emptyMap();
        }

        @Override
        protected Map<Object, Object> getDefaultCustomAttributes() {
            return Collections.emptyMap();
        }

    }

    /**
     * Creates a new {@link Configuration}, with the setting specified in this object. Note that {@link Configuration}-s
     * are immutable (since FreeMarker 3.0.0), that's why the builder class is needed.
     */
    public static final class Builder extends ExtendableBuilder<Builder> {

        /**
         * @param incompatibleImprovements
         *         Specifies the value of the {@linkplain Configuration#getIncompatibleImprovements()} incompatible
         *         improvements setting}. This setting can't be changed later.
         */
        public Builder(Version incompatibleImprovements) {
            super(incompatibleImprovements);
        }

    }

}

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

import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
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
import org.apache.freemarker.core.templateresolver.MergingTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.MruCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;
import org.apache.freemarker.core.util._ClassUtils;
import org.apache.freemarker.core.util._CollectionUtils;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtils;
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
 *  {@link Template Template} myTemplate = cfg.{@link #getTemplate(String) getTemplate}("myTemplate.ftlh");
 *  myTemplate.{@link Template#process(Object, java.io.Writer) process}(dataModel, out);</pre>
 * 
 * <p>Note that you certainly want to set the {@link #getTemplateLoader templateLoader} setting, as its default
 * value is {@code null}, so you won't be able to load any templates (as FreeMarker doesn't know where from should it
 * load them).
 *
 * <p>{@link Configuration} is thread-safe and (as of 3.0.0) immutable (apart from internal caches).
 *
 * <p>The setting reader methods of this class don't throw {@link CoreSettingValueNotSetException}, because all settings
 * are set on the {@link Configuration} level (even if they were just initialized to a default value).
 */
public final class Configuration implements TopLevelConfiguration, CustomStateScope {
    
    private static final String VERSION_PROPERTIES_PATH = "/org/apache/freemarker/core/version.properties";

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
            Properties props = _ClassUtils.loadProperties(Configuration.class, VERSION_PROPERTIES_PATH);
            String versionString  = getRequiredVersionProperty(props, "version");
            final Boolean gaeCompliant = Boolean.valueOf(getRequiredVersionProperty(props, "isGAECompliant"));
            VERSION = new Version(versionString, gaeCompliant, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load and parse " + VERSION_PROPERTIES_PATH, e);
        }
    }

    // Configuration-specific settings:

    private final Version incompatibleImprovements;
    private final TemplateResolver templateResolver;
    private final TemplateLoader templateLoader;
    private final CacheStorage templateCacheStorage;
    private final TemplateLookupStrategy templateLookupStrategy;
    private final TemplateNameFormat templateNameFormat;
    private final TemplateConfigurationFactory templateConfigurations;
    private final Long templateUpdateDelayMilliseconds;
    private final Boolean localizedTemplateLookup;
    private final List<OutputFormat> registeredCustomOutputFormats;
    private final Map<String, OutputFormat> registeredCustomOutputFormatsByName;
    private final Map<String, Object> sharedVariables;
    private final Map<String, TemplateModel> wrappedSharedVariables;

    // ParsingConfiguration settings:

    private final TemplateLanguage templateLanguage;
    private final TagSyntax tagSyntax;
    private final boolean whitespaceStripping;
    private final AutoEscapingPolicy autoEscapingPolicy;
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
    private final AttemptExceptionReporter attemptExceptionReporter;
    private final ArithmeticEngine arithmeticEngine;
    private final ObjectWrapper objectWrapper;
    private final Charset outputEncoding;
    private final Charset urlEscapingCharset;
    private final Boolean autoFlush;
    private final TemplateClassResolver newBuiltinClassResolver;
    private final Boolean showErrorTips;
    private final Boolean apiBuiltinEnabled;
    private final Map<String, TemplateDateFormatFactory> customDateFormats;
    private final Map<String, TemplateNumberFormatFactory> customNumberFormats;
    private final Map<String, String> autoImports;
    private final List<String> autoIncludes;
    private final Boolean lazyImports;
    private final Boolean lazyAutoImports;
    private final Map<Serializable, Object> customSettings;

    // CustomStateScope:

    private final ConcurrentHashMap<CustomStateKey<?>, Object> customStateMap = new ConcurrentHashMap<>(0);
    private final Object customStateMapLock = new Object();

    private <SelfT extends ExtendableBuilder<SelfT>> Configuration(ExtendableBuilder<SelfT> builder)
            throws ConfigurationException {
        // Configuration-specific settings (except templateResolver):
        incompatibleImprovements = builder.getIncompatibleImprovements();

        {
            final Collection<OutputFormat> regCustOutputFormats;
            {
                Collection<OutputFormat> directRegCustOutputFormats = builder.getRegisteredCustomOutputFormats();
                Collection<OutputFormat> impliedRegCustOutputFormats =
                        builder.getImpliedRegisteredCustomOutputFormats();
                if (impliedRegCustOutputFormats.isEmpty()) {
                    regCustOutputFormats = directRegCustOutputFormats;
                } else if (directRegCustOutputFormats.isEmpty()) {
                    regCustOutputFormats = impliedRegCustOutputFormats;
                } else {
                    List<OutputFormat> mergedOutputFormats = new ArrayList<>(
                            impliedRegCustOutputFormats.size() + directRegCustOutputFormats.size());
                    HashSet<String> directNames = new HashSet<>(directRegCustOutputFormats.size() * 4 / 3 + 1, .75f);
                    for (OutputFormat directRegCustOutputFormat : directRegCustOutputFormats) {
                        directNames.add(directRegCustOutputFormat.getName());
                    }
                    for (OutputFormat impliedRegCustOutputFormat : impliedRegCustOutputFormats) {
                        if (!directNames.contains(impliedRegCustOutputFormat.getName())) {
                            mergedOutputFormats.add(impliedRegCustOutputFormat);
                        }
                    }
                    mergedOutputFormats.addAll(directRegCustOutputFormats);
                    regCustOutputFormats = Collections.unmodifiableList(mergedOutputFormats);
                }
            }

            _NullArgumentException.check(regCustOutputFormats);
            Map<String, OutputFormat> registeredCustomOutputFormatsByName = new LinkedHashMap<>(
                    regCustOutputFormats.size() * 4 / 3, 1f);
            for (OutputFormat outputFormat : regCustOutputFormats) {
                String name = outputFormat.getName();
                if (name.equals(UndefinedOutputFormat.INSTANCE.getName())) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The \"" + name + "\" output format can't be redefined",
                            null);
                }
                if (name.equals(PlainTextOutputFormat.INSTANCE.getName())) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The \"" + name + "\" output format can't be redefined",
                            null);
                }
                if (name.length() == 0) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't be 0 long",
                            null);
                }
                if (!Character.isLetterOrDigit(name.charAt(0))) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name must start with letter or digit: " + name,
                            null);
                }
                if (name.indexOf('+') != -1) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't contain \"+\" character: " + name,
                            null);
                }
                if (name.indexOf('{') != -1) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "The output format name can't contain \"{\" character: " + name,
                            null);
                }
                if (name.indexOf('}') != -1) {
                    throw new InvalidSettingValueException(
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
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, null, false,
                            "Clashing output format names between " + replaced + " and " + outputFormat + ".",
                            null);
                }
            }

            this.registeredCustomOutputFormatsByName = registeredCustomOutputFormatsByName;
            this.registeredCustomOutputFormats = Collections.unmodifiableList(
                    new ArrayList<> (regCustOutputFormats));
        }

        this.objectWrapper = builder.getObjectWrapper();

        {
            Map<String, Object> sharedVariables = _CollectionUtils.mergeImmutableMaps(builder
                    .getImpliedSharedVariables(), builder.getSharedVariables(), false);

            HashMap<String, TemplateModel> wrappedSharedVariables = new HashMap<>(
                    sharedVariables.size() * 4 / 3 + 1, 0.75f);
            for (Entry<String, Object> ent : sharedVariables.entrySet()) {
                try {
                    wrappedSharedVariables.put(ent.getKey(), objectWrapper.wrap(ent.getValue()));
                } catch (ObjectWrappingException e) {
                    throw new InvalidSettingValueException(
                            ExtendableBuilder.SHARED_VARIABLES_KEY, null, false,
                            "Failed to wrap shared variable " + _StringUtils.jQuote(ent.getKey()),
                            e);
                }
            }

            this.wrappedSharedVariables = wrappedSharedVariables;
            this.sharedVariables = Collections.unmodifiableMap(sharedVariables);
        }

        // ParsingConfiguration settings:

        templateLanguage = builder.getTemplateLanguage();
        tagSyntax = builder.getTagSyntax();
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
        attemptExceptionReporter = builder.getAttemptExceptionReporter();
        arithmeticEngine = builder.getArithmeticEngine();
        outputEncoding = builder.getOutputEncoding();
        urlEscapingCharset = builder.getURLEscapingCharset();
        autoFlush = builder.getAutoFlush();
        newBuiltinClassResolver = builder.getNewBuiltinClassResolver();
        showErrorTips = builder.getShowErrorTips();
        apiBuiltinEnabled = builder.getAPIBuiltinEnabled();
        customDateFormats = _CollectionUtils.mergeImmutableMaps(
                builder.getImpliedCustomDateFormats(), builder.getCustomDateFormats(), false);
        customNumberFormats = _CollectionUtils.mergeImmutableMaps(
                builder.getImpliedCustomNumberFormats(), builder.getCustomNumberFormats(), false);
        autoImports = _CollectionUtils.mergeImmutableMaps(
                builder.getImpliedAutoImports(), builder.getAutoImports(), true);
        autoIncludes = _CollectionUtils.mergeImmutableLists(
                builder.getImpliedAutoIncludes(), builder.getAutoIncludes(), true);
        lazyImports = builder.getLazyImports();
        lazyAutoImports = builder.getLazyAutoImports();
        customSettings = builder.getCustomSettings(false);

        // Configuration-specific settings continued... templateResolver):

        templateResolver = builder.getTemplateResolver();

        templateLoader = builder.getTemplateLoader();
        if (!templateResolver.supportsTemplateLoaderSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_LOADER_KEY, templateLoader);
        }

        templateCacheStorage = builder.getTemplateCacheStorage();
        if (!templateResolver.supportsTemplateCacheStorageSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_CACHE_STORAGE_KEY, templateCacheStorage);
        }

        templateUpdateDelayMilliseconds = builder.getTemplateUpdateDelayMilliseconds();
        if (!templateResolver.supportsTemplateUpdateDelayMillisecondsSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_UPDATE_DELAY_KEY, templateUpdateDelayMilliseconds);
        }

        templateLookupStrategy = builder.getTemplateLookupStrategy();
        if (!templateResolver.supportsTemplateLookupStrategySetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_LOOKUP_STRATEGY_KEY, templateLookupStrategy);
        }

        localizedTemplateLookup = builder.getLocalizedTemplateLookup();
        if (!templateResolver.supportsLocalizedTemplateLookupSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, LOCALIZED_TEMPLATE_LOOKUP_KEY, localizedTemplateLookup);
        }

        templateNameFormat = builder.getTemplateNameFormat();
        if (!templateResolver.supportsTemplateNameFormatSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_NAME_FORMAT_KEY, templateNameFormat);
        }

        TemplateConfigurationFactory templateConfigurations = builder.getTemplateConfigurations();
        if (!templateResolver.supportsTemplateConfigurationsSetting()) {
            checkSettingIsNullForThisTemplateResolver(
                    templateResolver, TEMPLATE_CONFIGURATIONS_KEY, templateConfigurations);
        }
        TemplateConfigurationFactory impliedTemplateConfigurations = builder.getImpliedTemplateConfigurations();
        if (impliedTemplateConfigurations != null) {
            if (templateConfigurations != null) {
                templateConfigurations = new MergingTemplateConfigurationFactory(
                        impliedTemplateConfigurations, templateConfigurations);
            } else {
                templateConfigurations = impliedTemplateConfigurations;
            }
        }
        this.templateConfigurations = templateConfigurations;

        templateResolver.setDependencies(new TemplateResolverDependenciesImpl(this, templateResolver));
    }

    private void checkSettingIsNullForThisTemplateResolver(
            TemplateResolver templateResolver,
            String settingName, Object value) {
        if (value != null) {
            throw new InvalidSettingValueException(
                    settingName, null, false,
                    "The templateResolver is a "
                    + templateResolver.getClass().getName() + ", which doesn't support this setting, hence it "
                    + "mustn't be set or must be set to null.",
                    null);
        }
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return templateExceptionHandler;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return true;
    }

    @Override
    public AttemptExceptionReporter getAttemptExceptionReporter() {
        return attemptExceptionReporter;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isAttemptExceptionReporterSet() {
        return true;
    }

    private static class DefaultSoftCacheStorage extends SoftCacheStorage {
        // Nothing to override
    }

    @Override
    public TemplateResolver getTemplateResolver() {
        return templateResolver;
    }



    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateResolverSet() {
        return true;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateLoaderSet() {
        return true;
    }

    @Override
    public TemplateLookupStrategy getTemplateLookupStrategy() {
        return templateLookupStrategy;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateLookupStrategySet() {
        return true;
    }
    
    @Override
    public TemplateNameFormat getTemplateNameFormat() {
        return templateNameFormat;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateNameFormatSet() {
        return true;
    }

    @Override
    public TemplateConfigurationFactory getTemplateConfigurations() {
        return templateConfigurations;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateConfigurationsSet() {
        return true;
    }

    @Override
    public CacheStorage getTemplateCacheStorage() {
        return templateCacheStorage;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateCacheStorageSet() {
        return true;
    }

    @Override
    public Long getTemplateUpdateDelayMilliseconds() {
        return templateUpdateDelayMilliseconds;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
    public boolean isIncompatibleImprovementsSet() {
        return true;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isWhitespaceStrippingSet() {
        return true;
    }

    /**
     * When auto-escaping should be enabled depending on the current {@linkplain OutputFormat output format};
     * default is {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}. Note that the default output
     * format, {@link UndefinedOutputFormat}, is a non-escaping format, so there auto-escaping will be off.
     * Note that the templates can turn auto-escaping on/off locally with directives like {@code <#ftl auto_esc=...>},
     * which will ignore the policy.
     *
     * <p><b>About auto-escaping</b></p>
     *
     * <p>
     * Auto-escaping has significance when a value is printed with <code>${...}</code>. If
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
     * @see Configuration.Builder#setAutoEscapingPolicy(AutoEscapingPolicy)
     * @see TemplateConfiguration.Builder#setAutoEscapingPolicy(AutoEscapingPolicy)
     * @see Configuration.Builder#setOutputFormat(OutputFormat)
     * @see TemplateConfiguration.Builder#setOutputFormat(OutputFormat)
     */
    @Override
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        return autoEscapingPolicy;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
                sb.append(_StringUtils.jQuote(name));
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
                    sb.append(_StringUtils.jQuote(registeredName));
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
    
    @Override
    public Collection<OutputFormat> getRegisteredCustomOutputFormats() {
        return registeredCustomOutputFormats;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isRegisteredCustomOutputFormatsSet() {
        return true;
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return recognizeStandardFileExtensions == null
                ? true
                : recognizeStandardFileExtensions.booleanValue();
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTemplateLanguageSet() {
        return true;
    }

    @Override
    public TagSyntax getTagSyntax() {
        return tagSyntax;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isTagSyntaxSet() {
        return true;
    }

    @Override
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isShowErrorTipsSet() {
        return true;
    }

    @Override
    public boolean getLazyImports() {
        return lazyImports;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isAutoIncludesSet() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because {@link Configuration} has on parent, the {@code includeInherited} parameter is ignored.
     */
    @Override
    public Map<Serializable, Object> getCustomSettings(boolean includeInherited) {
        return customSettings;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Unlike the other isXxxSet methods of {@link Configuration}, this can return {@code false}, as at least the
     * builders in FreeMarker Core can't provide defaults for custom settings. Note that since
     * {@link #getCustomSetting(Serializable)} just returns {@code null} for unset custom settings, it's usually not a
     * problem.
     */
    @Override
    public boolean isCustomSettingSet(Serializable key) {
        return customSettings.containsKey(key);
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
        Object value = customSettings.get(key);
        if (value != null || customSettings.containsKey(key)) {
            return value;
        }
        if (useDefaultValue) {
            return defaultValue;
        }
        throw new CustomSettingValueNotSetException(key);
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
     * <p>
     * This is a shorthand for {@link #getTemplate(String, Locale, Serializable, boolean)
     * getTemplate(name, null, null, false)}; see more details there.
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
     *            {@link TemplateLoader}. Can't be {@code null}. The exact syntax of the name depends on the
     *            underlying {@link TemplateNameFormat} and to an extent on the {@link TemplateLoader} (or on the
     *            {@link TemplateResolver} more generally), but the default configuration has some assumptions.
     *            First, the name is expected to be a hierarchical path, with path components separated by a slash
     *            character (not with backslash!). The path (the name) given here is always interpreted relative to
     *            the "template root directory" and must <em>not</em> begin with slash. Then, the {@code ..} and
     *            {@code .} path meta-elements will be resolved. For example, if the name is {@code a/../b/./c.ftl},
     *            then it will be simplified to {@code b/c.ftl}. The rules regarding this are the same as with
     *            conventional UN*X paths. The path must not reach outside the template root directory, that is, it
     *            can't be something like {@code "../templates/my.ftl"} (not even if this path happens to be
     *            equivalent with {@code "/my.ftl"}). Furthermore, the path is allowed to contain at most one path
     *            element whose name is {@code *} (asterisk). This path meta-element triggers the <i>acquisition
     *            mechanism</i>. If the template is not found in the location described by the concatenation of the
     *            path left to the asterisk (called base path) and the part to the right of the asterisk (called
     *            resource path), then the {@link TemplateResolver} (at least the default one) will attempt to remove
     *            the rightmost path component from the base path (go up one directory) and concatenate that with
     *            the resource path. The process is repeated until either a template is found, or the base path is
     *            completely exhausted.
     *
     * @param locale
     *            The requested locale of the template. This is what {@link Template#getLocale()} on the resulting
     *            {@link Template} will return (unless it's overridden via {@link #getTemplateConfigurations()}). This
     *            parameter can be {@code null} since 2.3.22, in which case it defaults to
     *            {@link Configuration#getLocale()} (note that {@link Template#getLocale()} will give the default value,
     *            not {@code null}). This parameter also drives localized template lookup. Assuming that you have
     *            specified {@code en_US} as the locale and {@code myTemplate.ftl} as the name of the template, and the
     *            default {@link TemplateLookupStrategy} is used and
     *            {@code #setLocalizedTemplateLookup(boolean) localizedTemplateLookup} is {@code true}, FreeMarker will first try to
     *            retrieve {@code myTemplate_en_US.html}, then {@code myTemplate.en.ftl}, and finally
     *            {@code myTemplate.ftl}. Note that that the template's locale will be {@code en_US} even if it only
     *            finds {@code myTemplate.ftl}. Note that when the {@code locale} setting is overridden with a
     *            {@link TemplateConfiguration} provided by {@link #getTemplateConfigurations()}, that overrides the
     *            value specified here, but only after the localized template lookup, that is, it modifies the template
     *            found by the localized template lookup.
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
     */
    public Template getTemplate(String name, Locale locale, Serializable customLookupCondition,
            boolean ignoreMissing)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        if (locale == null) {
            locale = getLocale();
        }
        final GetTemplateResult maybeTemp = getTemplateResolver().getTemplate(name, locale, customLookupCondition);
        final Template temp = maybeTemp.getTemplate();
        if (temp == null) {
            if (ignoreMissing) {
                return null;
            }
            
            TemplateLoader tl = getTemplateLoader();  
            String msg; 
            if (tl == null) {
                msg = "Don't know where to load template " + _StringUtils.jQuote(name)
                      + " from because the \"templateLoader\" FreeMarker "
                      + "setting wasn't set (Configuration.setTemplateLoader), so it's null.";
            } else {
                final String missingTempNormName = maybeTemp.getMissingTemplateNormalizedName();
                final String missingTempReason = maybeTemp.getMissingTemplateReason();
                final TemplateLookupStrategy templateLookupStrategy = getTemplateLookupStrategy();
                msg = "Template not found for name " + _StringUtils.jQuote(name)
                        + (missingTempNormName != null && name != null
                                && !removeInitialSlash(name).equals(missingTempNormName)
                                ? " (normalized: " + _StringUtils.jQuote(missingTempNormName) + ")"
                                : "")
                        + (customLookupCondition != null ? " and custom lookup condition "
                        + _StringUtils.jQuote(customLookupCondition) : "")
                        + "."
                        + (missingTempReason != null
                                ? "\nReason given: " + ensureSentenceIsClosed(missingTempReason)
                                : "")
                        + "\nThe name was interpreted by this TemplateLoader: "
                        + _StringUtils.tryToString(tl) + "."
                        + (!isKnownNonConfusingLookupStrategy(templateLookupStrategy)
                                ? "\n(Before that, the name was possibly changed by this lookup strategy: "
                                  + _StringUtils.tryToString(templateLookupStrategy) + ".)"
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
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
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
        getTemplateResolver().clearTemplateCache();
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
        getTemplateResolver().removeTemplateFromCache(name, locale, customLookupCondition);
    }

    @Override
    public Boolean getLocalizedTemplateLookup() {
        return localizedTemplateLookup;
    }

    /**
     * Always {@code true} in {@link Configuration}-s; even if this setting wasn't set in the builder, it gets a default
     * value in the {@link Configuration}.
     */
    @Override
    public boolean isLocalizedTemplateLookupSet() {
        return true;
    }

    /**
     * Returns the FreeMarker version information, most importantly the major.minor.micro version numbers; do not use
     * this for {@link #getIncompatibleImprovements() #incompatibleImprovements} value, use constants like
     * {@link Configuration#VERSION_3_0_0} for that.
     */
    public static Version getVersion() {
        return VERSION;
    }
    
    /**
     * Returns the names of the supported "built-ins". These are the ({@code expr?builtin_name}-like things). As of this
     * writing, this information doesn't depend on the configuration options, so it could be a static method, but
     * to be future-proof, it's an instance method. 
     */
    public Set<String> getSupportedBuiltInNames() {
        return Collections.unmodifiableSet(ASTExpBuiltIn.BUILT_INS_BY_NAME.keySet());
    }
    
    /**
     * Returns the names of the directives that are predefined by FreeMarker. These are the things that you call like
     * <tt>&lt;#directiveName ...&gt;</tt>.
     */
    public Set<String> getSupportedBuiltInDirectiveNames() {
        return ASTDirective.BUILT_IN_DIRECTIVE_NAMES;
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
            implements TopLevelConfiguration, org.apache.freemarker.core.util.CommonBuilder<Configuration> {

        public static final String LOCALIZED_TEMPLATE_LOOKUP_KEY = "localizedTemplateLookup";
        public static final String REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY = "registeredCustomOutputFormats";
        public static final String TEMPLATE_RESOLVER_KEY = "templateResolver";
        public static final String TEMPLATE_CACHE_STORAGE_KEY = "templateCacheStorage";
        public static final String TEMPLATE_UPDATE_DELAY_KEY = "templateUpdateDelay";
        public static final String TEMPLATE_LOADER_KEY = "templateLoader";
        public static final String TEMPLATE_LOOKUP_STRATEGY_KEY = "templateLookupStrategy";
        public static final String TEMPLATE_NAME_FORMAT_KEY = "templateNameFormat";
        public static final String SHARED_VARIABLES_KEY = "sharedVariables";
        public static final String TEMPLATE_CONFIGURATIONS_KEY = "templateConfigurations";
        public static final String OBJECT_WRAPPER_KEY = "objectWrapper";

        private static final _UnmodifiableCompositeSet<String> SETTING_NAMES = new _UnmodifiableCompositeSet<>(
                MutableParsingAndProcessingConfiguration.getSettingNames(),
                new _SortedArraySet<>(
                        // Must be sorted alphabetically!
                        ExtendableBuilder.LOCALIZED_TEMPLATE_LOOKUP_KEY,
                        ExtendableBuilder.OBJECT_WRAPPER_KEY,
                        ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY,
                        ExtendableBuilder.SHARED_VARIABLES_KEY,
                        ExtendableBuilder.TEMPLATE_CACHE_STORAGE_KEY,
                        ExtendableBuilder.TEMPLATE_CONFIGURATIONS_KEY,
                        ExtendableBuilder.TEMPLATE_LOADER_KEY,
                        ExtendableBuilder.TEMPLATE_LOOKUP_STRATEGY_KEY,
                        ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY,
                        ExtendableBuilder.TEMPLATE_RESOLVER_KEY,
                        ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY
                ));

        private Version incompatibleImprovements = Configuration.VERSION_3_0_0;

        private TemplateResolver templateResolver;
        private TemplateResolver cachedDefaultTemplateResolver;
        private TemplateLoader templateLoader;
        private boolean templateLoaderSet;
        private CacheStorage templateCacheStorage;
        private boolean templateCacheStorageSet;
        private CacheStorage cachedDefaultTemplateCacheStorage;
        private TemplateLookupStrategy templateLookupStrategy;
        private boolean templateLookupStrategySet;
        private TemplateNameFormat templateNameFormat;
        private boolean templateNameFormatSet;
        private TemplateConfigurationFactory templateConfigurations;
        private boolean templateConfigurationsSet;
        private Long templateUpdateDelayMilliseconds;
        private boolean templateUpdateDelayMillisecondsSet;
        private Boolean localizedTemplateLookup;
        private boolean localizedTemplateLookupSet;
        private Collection<OutputFormat> registeredCustomOutputFormats;
        private Map<String, Object> sharedVariables;
        private ObjectWrapper objectWrapper;

        private boolean alreadyBuilt;

        /**
         * @param incompatibleImprovements
         *            The initial value of the {@link Configuration#getIncompatibleImprovements()
         *            incompatibleImprovements}; can't {@code null}. This can be later changed via
         *            {@link #setIncompatibleImprovements(Version)}. The point here is just to ensure that it's never
         *            {@code null}. Do NOT ever use {@link Configuration#getVersion()} to set this. Always use a fixed
         *            value, like {@link #VERSION_3_0_0}, otherwise your application can break as you upgrade
         *            FreeMarker.
         */
        protected ExtendableBuilder(Version incompatibleImprovements) {
            _NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
            this.incompatibleImprovements = incompatibleImprovements;
        }

        @Override
        public Configuration build() throws ConfigurationException {
            if (alreadyBuilt) {
                throw new IllegalStateException("build() can only be executed once.");
            }
            Configuration configuration = new Configuration(this);
            alreadyBuilt = true;
            return configuration;
        }

        @Override
        public void setSetting(String name, String value) throws ConfigurationException {
            boolean nameUnhandled = false;
            try {
                if (LOCALIZED_TEMPLATE_LOOKUP_KEY.equals(name)) {
                    setLocalizedTemplateLookup(_StringUtils.getYesNo(value));
                } else if (REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY.equals(name)) {
                    List list = (List) _ObjectBuilderSettingEvaluator.eval(
                            value, List.class, true, _SettingEvaluationEnvironment.getCurrent());
                    for (Object item : list) {
                        if (!(item instanceof OutputFormat)) {
                            throw new InvalidSettingValueException(name, value,
                                    "List items must be " + OutputFormat.class.getName() + " instances.");
                        }
                    }
                    setRegisteredCustomOutputFormats(list);
                } else if (TEMPLATE_CACHE_STORAGE_KEY.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateCacheStorage();
                    } if (value.indexOf('.') == -1) {
                        int strongSize = 0;
                        int softSize = 0;
                        Map map = _StringUtils.parseNameValuePairList(
                                value, String.valueOf(Integer.MAX_VALUE));
                        Iterator it = map.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry ent = (Map.Entry) it.next();
                            String pName = (String) ent.getKey();
                            int pValue;
                            try {
                                pValue = Integer.parseInt((String) ent.getValue());
                            } catch (NumberFormatException e) {
                                throw new InvalidSettingValueException(name, value,
                                        "Malformed integer number (shown quoted): " + _StringUtils.jQuote(ent.getValue()));
                            }
                            if ("soft".equalsIgnoreCase(pName)) {
                                softSize = pValue;
                            } else if ("strong".equalsIgnoreCase(pName)) {
                                strongSize = pValue;
                            } else {
                                throw new InvalidSettingValueException(name, value,
                                        "Unsupported cache parameter name (shown quoted): "
                                                + _StringUtils.jQuote(ent.getValue()));
                            }
                        }
                        if (softSize == 0 && strongSize == 0) {
                            throw new InvalidSettingValueException(name, value,
                                    "Either cache soft- or strong size must be set and non-0.");
                        }
                        setTemplateCacheStorage(new MruCacheStorage(strongSize, softSize));
                    } else {
                        setTemplateCacheStorage((CacheStorage) _ObjectBuilderSettingEvaluator.eval(
                                value, CacheStorage.class, false, _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_UPDATE_DELAY_KEY.equals(name)) {
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
                        throw new InvalidSettingValueException(name, value,
                                "Unrecognized time unit " + _StringUtils.jQuote(unit) + ". Valid units are: ms, s, m, h");
                    } else {
                        multipier = 0;
                    }

                    int parsedValue = Integer.parseInt(valueWithoutUnit);
                    if (multipier == 0 && parsedValue != 0) {
                        throw new InvalidSettingValueException(name, value,
                                "Time unit must be specified for a non-0 value (examples: 500 ms, 3 s, 2 m, 1 h).");
                    }

                    setTemplateUpdateDelayMilliseconds(parsedValue * multipier);
                } else if (SHARED_VARIABLES_KEY.equals(name)) {
                    Map<?, ?> sharedVariables = (Map<?, ?>) _ObjectBuilderSettingEvaluator.eval(
                            value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                    for (Object key : sharedVariables.keySet()) {
                        if (!(key instanceof String)) {
                            throw new InvalidSettingValueException(name, null, false,
                                    "All keys in this Map must be strings, but one of them is an instance of "
                                    + "this class: " + _ClassUtils.getShortClassNameOfObject(key), null);
                        }
                    }
                    setSharedVariables((Map) sharedVariables);
                } else if (INCOMPATIBLE_IMPROVEMENTS_KEY.equals(name)) {
                    setIncompatibleImprovements(new Version(value));
                } else if (TEMPLATE_RESOLVER_KEY.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateResolver();
                    } else {
                        setTemplateResolver((TemplateResolver) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateResolver.class, false,
                                _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_LOADER_KEY.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateLoader();
                    } else {
                        setTemplateLoader((TemplateLoader) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateLoader.class, true,
                                _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_LOOKUP_STRATEGY_KEY.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateLookupStrategy();
                    } else {
                        setTemplateLookupStrategy((TemplateLookupStrategy) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateLookupStrategy.class, false,
                                _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (TEMPLATE_NAME_FORMAT_KEY.equals(name)) {
                    if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                        unsetTemplateNameFormat();
                    } else {
                        throw new InvalidSettingValueException(name, value,
                                "No such predefined template name format");
                    }
                } else if (TEMPLATE_CONFIGURATIONS_KEY.equals(name)) {
                    if (value.equals(NULL_VALUE)) {
                        setTemplateConfigurations(null);
                    } else {
                        setTemplateConfigurations((TemplateConfigurationFactory) _ObjectBuilderSettingEvaluator.eval(
                                value, TemplateConfigurationFactory.class, false,
                                _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else if (OBJECT_WRAPPER_KEY.equals(name)) {
                    if (DEFAULT_VALUE.equalsIgnoreCase(value)) {
                        this.unsetObjectWrapper();
                    } else if ("restricted".equalsIgnoreCase(value)) {
                        // FM3 TODO should depend on IcI, but maybe the simplest is to remove this convenience value
                        setObjectWrapper(new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
                    } else {
                        setObjectWrapper((ObjectWrapper) _ObjectBuilderSettingEvaluator.eval(
                                value, ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent()));
                    }
                } else {
                    nameUnhandled = true;
                }
            } catch (InvalidSettingValueException e) {
                throw e;
            } catch (Exception e) {
                throw new InvalidSettingValueException(name, value, e);
            }
            if (nameUnhandled) {
                super.setSetting(name, value);
            }
        }

        /**
         * Returns the valid {@link Configuration} setting names. Naturally, this includes the
         * {@link MutableProcessingConfiguration} setting names too.
         *
         * @see MutableProcessingConfiguration#getSettingNames()
         */
        public static Set<String> getSettingNames() {
            return SETTING_NAMES;
        }

        @Override
        protected Version getRemovalVersionForUnknownSetting(String name) {
            if (name.equals("strictSyntax") || name.equalsIgnoreCase("strict_syntax")) {
                return Configuration.VERSION_3_0_0;
            }
            return super.getRemovalVersionForUnknownSetting(name);
        }

        @Override
        protected String getCorrectedNameForUnknownSetting(String name) {
            switch(name.toLowerCase()) {
                case "encoding":
                case "default_encoding":
                case "charset":
                case "default_charset":
                case "defaultencoding":
                case "defaultcharset":
                case "sourceencoding":
                case "source_encoding":
                    return SOURCE_ENCODING_KEY;
                case "incompatible_enhancements":
                case "incompatibleenhancements":
                case "incompatibleimprovements":
                case "incompatibleImprovements":
                    return INCOMPATIBLE_IMPROVEMENTS_KEY;
                case "cachestorage":
                case "cache_storage":
                case "templatecachestorage":
                case "template_cache_storage":
                    return TEMPLATE_CACHE_STORAGE_KEY;
                case "localizedlookup":
                case "localized_lookup":
                case "localizedtemplatelookup":
                case "localized_template_lookup":
                    return LOCALIZED_TEMPLATE_LOOKUP_KEY;
                case "templateupdateinterval":
                case "templateupdatedelay":
                case "template_update_delay":
                    return TEMPLATE_UPDATE_DELAY_KEY;
                default:
                    return super.getCorrectedNameForUnknownSetting(name);
            }

        }

        @Override
        public TemplateResolver getTemplateResolver() {
            return isTemplateResolverSet() ? templateResolver : getDefaultTemplateResolver();
        }

        @Override
        public boolean isTemplateResolverSet() {
            return templateResolver != null;
        }

        protected TemplateResolver getDefaultTemplateResolver() {
            if (cachedDefaultTemplateResolver == null) {
                cachedDefaultTemplateResolver = new DefaultTemplateResolver();
            }
            return cachedDefaultTemplateResolver;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateResolver()}; note {@code null}.
         */
        public void setTemplateResolver(TemplateResolver templateResolver) {
            _NullArgumentException.check("templateResolver", templateResolver);
            this.templateResolver = templateResolver;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateResolver(TemplateResolver)}
         */
        public SelfT templateResolver(TemplateResolver templateResolver) {
            setTemplateResolver(templateResolver);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateResolver() {
            templateResolver = null;
        }

        @Override
        public TemplateLoader getTemplateLoader() {
            return isTemplateLoaderSet() ? templateLoader : getDefaultTemplateLoaderTRAware();
        }

        @Override
        public boolean isTemplateLoaderSet() {
            return templateLoaderSet;
        }

        private TemplateLoader getDefaultTemplateLoaderTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateLoaderSetting() ?
                    null : getDefaultTemplateLoader();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected TemplateLoader getDefaultTemplateLoader() {
            return null;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateLoader()}. Note that {@code null} is a valid value.
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
        public CacheStorage getTemplateCacheStorage() {
            return isTemplateCacheStorageSet() ? templateCacheStorage : getDefaultTemplateCacheStorageTRAware();
        }

        @Override
        public boolean isTemplateCacheStorageSet() {
            return templateCacheStorageSet;
        }

        private CacheStorage getDefaultTemplateCacheStorageTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateCacheStorageSetting() ? null
                    : getDefaultTemplateCacheStorage();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected CacheStorage getDefaultTemplateCacheStorage() {
            if (cachedDefaultTemplateCacheStorage == null) {
                // If this will depend on incompatibleImprovements, null it out in onIncompatibleImprovementsChanged()!
                cachedDefaultTemplateCacheStorage = new DefaultSoftCacheStorage();
            }
            return cachedDefaultTemplateCacheStorage;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateCacheStorage()}
         */
        public void setTemplateCacheStorage(CacheStorage templateCacheStorage) {
            this.templateCacheStorage = templateCacheStorage;
            this.templateCacheStorageSet = true;
            cachedDefaultTemplateCacheStorage = null;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateCacheStorage(CacheStorage)}
         */
        public SelfT templateCacheStorage(CacheStorage templateCacheStorage) {
            setTemplateCacheStorage(templateCacheStorage);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateCacheStorage() {
            templateCacheStorage = null;
            templateCacheStorageSet = false;
        }

        @Override
        public TemplateLookupStrategy getTemplateLookupStrategy() {
            return isTemplateLookupStrategySet() ? templateLookupStrategy : getDefaultTemplateLookupStrategyTRAware();
        }

        @Override
        public boolean isTemplateLookupStrategySet() {
            return templateLookupStrategySet;
        }

        private TemplateLookupStrategy getDefaultTemplateLookupStrategyTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateLookupStrategySetting() ? null :
                    getDefaultTemplateLookupStrategy();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected TemplateLookupStrategy getDefaultTemplateLookupStrategy() {
            return DefaultTemplateLookupStrategy.INSTANCE;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateLookupStrategy()}.
         */
        public void setTemplateLookupStrategy(TemplateLookupStrategy templateLookupStrategy) {
            this.templateLookupStrategy = templateLookupStrategy;
            templateLookupStrategySet = true;
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
            templateLookupStrategySet = false;
        }

        @Override
        public TemplateNameFormat getTemplateNameFormat() {
            return isTemplateNameFormatSet() ? templateNameFormat : getDefaultTemplateNameFormatTRAware();
        }

        /**
         * Tells if this setting was explicitly set (if not, the default value of the setting will be used).
         */
        @Override
        public boolean isTemplateNameFormatSet() {
            return templateNameFormatSet;
        }

        private TemplateNameFormat getDefaultTemplateNameFormatTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateNameFormatSetting() ? null
                    : getDefaultTemplateNameFormat();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected TemplateNameFormat getDefaultTemplateNameFormat() {
            return DefaultTemplateNameFormat.INSTANCE;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateNameFormat()}.
         */
        public void setTemplateNameFormat(TemplateNameFormat templateNameFormat) {
            this.templateNameFormat = templateNameFormat;
            templateNameFormatSet = true;
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
            templateNameFormatSet = false;
        }

        @Override
        public TemplateConfigurationFactory getTemplateConfigurations() {
            return isTemplateConfigurationsSet() ? templateConfigurations : getDefaultTemplateConfigurationsTRAware();
        }

        @Override
        public boolean isTemplateConfigurationsSet() {
            return templateConfigurationsSet;
        }

        private TemplateConfigurationFactory getDefaultTemplateConfigurationsTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateConfigurationsSetting() ? null
                    : getDefaultTemplateConfigurations();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected TemplateConfigurationFactory getDefaultTemplateConfigurations() {
            return null;
        }

        /**
         * The template configurations that will be added to the built {@link Configuration} before the ones
         * coming from {@link #setCustomNumberFormats(Map)}}, where addition happens with
         * {@link MergingTemplateConfigurationFactory}. When overriding this method, always
         * consider adding to the return value of the super method, rather than replacing it.
         *
         * @return Maybe {@code null}.
         */
        protected TemplateConfigurationFactory getImpliedTemplateConfigurations() {
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
        public Long getTemplateUpdateDelayMilliseconds() {
            return isTemplateUpdateDelayMillisecondsSet() ? templateUpdateDelayMilliseconds
                    : getDefaultTemplateUpdateDelayMillisecondsTRAware();
        }

        @Override
        public boolean isTemplateUpdateDelayMillisecondsSet() {
            return templateUpdateDelayMillisecondsSet;
        }

        private Long getDefaultTemplateUpdateDelayMillisecondsTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsTemplateUpdateDelayMillisecondsSetting()
                    ? null : getDefaultTemplateUpdateDelayMilliseconds();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected Long getDefaultTemplateUpdateDelayMilliseconds() {
            return 5000L;
        }

        /**
         * Setter pair of {@link Configuration#getTemplateUpdateDelayMilliseconds()}.
         */
        public void setTemplateUpdateDelayMilliseconds(Long templateUpdateDelayMilliseconds) {
            this.templateUpdateDelayMilliseconds = templateUpdateDelayMilliseconds;
            templateUpdateDelayMillisecondsSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setTemplateUpdateDelayMilliseconds(Long)}
         */
        public SelfT templateUpdateDelayMilliseconds(Long templateUpdateDelayMilliseconds) {
            setTemplateUpdateDelayMilliseconds(templateUpdateDelayMilliseconds);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetTemplateUpdateDelayMilliseconds() {
            templateUpdateDelayMilliseconds = null;
            templateUpdateDelayMillisecondsSet = false;
        }

        @Override
        public Boolean getLocalizedTemplateLookup() {
            return isLocalizedTemplateLookupSet() ? localizedTemplateLookup : getDefaultLocalizedTemplateLookupTRAware();
        }

        @Override
        public boolean isLocalizedTemplateLookupSet() {
            return localizedTemplateLookupSet;
        }

        private Boolean getDefaultLocalizedTemplateLookupTRAware() {
            return isTemplateResolverSet() && !getTemplateResolver().supportsLocalizedTemplateLookupSetting() ? null
                    : getDefaultLocalizedTemplateLookup();
        }

        /**
         * The default value when the {@link #getTemplateResolver() templateResolver} supports this setting (otherwise
         * the default is hardwired to be {@code null} and this method isn't called).
         */
        protected Boolean getDefaultLocalizedTemplateLookup() {
            return true;
        }

        /**
         * Setter pair of {@link Configuration#getLocalizedTemplateLookup()}.
         */
        public void setLocalizedTemplateLookup(Boolean localizedTemplateLookup) {
            this.localizedTemplateLookup = localizedTemplateLookup;
            localizedTemplateLookupSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setLocalizedTemplateLookup(Boolean)}
         */
        public SelfT localizedTemplateLookup(Boolean localizedTemplateLookup) {
            setLocalizedTemplateLookup(localizedTemplateLookup);
            return self();
        }

        /**
         * Resets this setting to its initial state, as if it was never set.
         */
        public void unsetLocalizedTemplateLookup() {
            this.localizedTemplateLookup = null;
            localizedTemplateLookupSet = false;
        }

        @Override
        public Collection<OutputFormat> getRegisteredCustomOutputFormats() {
            return isRegisteredCustomOutputFormatsSet() ? registeredCustomOutputFormats
                    : getDefaultRegisteredCustomOutputFormats();
        }

        @Override
        public boolean isRegisteredCustomOutputFormatsSet() {
            return registeredCustomOutputFormats != null;
        }

        protected Collection<OutputFormat> getDefaultRegisteredCustomOutputFormats() {
            return Collections.emptyList();
        }

        /**
         * The imports that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getRegisteredCustomOutputFormats()}. When overriding this method, always consider adding to the
         * return value of the super method, rather than replacing it.
         *
         * @return Immutable {@link Collection}; not {@code null}
         */
        protected Collection<OutputFormat> getImpliedRegisteredCustomOutputFormats() {
            return Collections.emptyList();
        }

        /**
         * Setter pair of {@link Configuration#getRegisteredCustomOutputFormats()}.
         */
        public void setRegisteredCustomOutputFormats(Collection<OutputFormat> registeredCustomOutputFormats) {
            _NullArgumentException.check("registeredCustomOutputFormats", registeredCustomOutputFormats);
            this.registeredCustomOutputFormats = Collections.unmodifiableCollection(
                    new ArrayList<>(registeredCustomOutputFormats));
        }

        /**
         * Fluent API equivalent of {@link #setRegisteredCustomOutputFormats(Collection)}
         */
        public SelfT registeredCustomOutputFormats(Collection<OutputFormat> registeredCustomOutputFormats) {
            setRegisteredCustomOutputFormats(registeredCustomOutputFormats);
            return self();
        }

        /**
         * Varargs overload if {@link #registeredCustomOutputFormats(Collection)}.
         */
        public SelfT registeredCustomOutputFormats(OutputFormat... registeredCustomOutputFormats) {
            return registeredCustomOutputFormats(Arrays.asList(registeredCustomOutputFormats));
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

        /**
         * The {@link Map} to use as shared variables if {@link #isSharedVariablesSet()} is {@code false}.
         *
         * @see #getImpliedSharedVariables()
         */
        protected Map<String, Object> getDefaultSharedVariables() {
            return Collections.emptyMap();
        }

        /**
         * The shared variables that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getSharedVariables()}. When overriding this method, always consider adding to the return value
         * of the super method, rather than replacing it.
         *
         * @return Immutable {@link Map}; not {@code null}
         */
        protected Map<String, Object> getImpliedSharedVariables() {
            return Collections.emptyMap();
        }

        /**
         * Setter pair of {@link Configuration#getSharedVariables()}.
         *
         * @param sharedVariables
         *         Will be copied (to prevent aliasing effect); can't be {@code null}; the {@link Map} can't contain
         *         {@code null} key, but can contain {@code null} value.
         */
        public void setSharedVariables(Map<String, ?> sharedVariables) {
            _NullArgumentException.check("sharedVariables", sharedVariables);
            _CollectionUtils.safeCastMap(
                    "sharedVariables", sharedVariables, String.class, false, Object.class,true);
            this.sharedVariables = Collections.unmodifiableMap(new HashMap<>(sharedVariables));
        }

        /**
         * Fluent API equivalent of {@link #setSharedVariables(Map)}
         */
        public SelfT sharedVariables(Map<String, ?> sharedVariables) {
            setSharedVariables(sharedVariables);
            return self();
        }

        public void unsetSharedVariables() {
            this.sharedVariables = null;
        }

        @Override
        protected TagSyntax getDefaultTagSyntax() {
            return TagSyntax.ANGLE_BRACKET;
        }

        @Override
        protected TemplateLanguage getDefaultTemplateLanguage() {
            return TemplateLanguage.FTL;
        }

        @Override
        public Version getIncompatibleImprovements() {
            return incompatibleImprovements;
        }

        @Override
        public boolean isIncompatibleImprovementsSet() {
            return true;
        }

        /**
         * Setter pair of {@link Configuration#getIncompatibleImprovements()}.
         * 
         * <p>Do NOT ever use {@link Configuration#getVersion()} to set the "incompatible improvements". Always use
         * a fixed value, like {@link #VERSION_3_0_0}. Otherwise your application can break as you upgrade FreeMarker. 
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
        protected AutoEscapingPolicy getDefaultAutoEscapingPolicy() {
            return AutoEscapingPolicy.ENABLE_IF_DEFAULT;
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
            return StandardCharsets.UTF_8;
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
            return TimeZone.getDefault();
        }

        @Override
        protected String getDefaultNumberFormat() {
            return "number";
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats() {
            return Collections.emptyMap();
        }

        /**
         * {@inheritDoc}
         *
         * @see #getImpliedCustomNumberFormats()
         */
        @Override
        protected TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name) {
            return null;
        }

        /**
         * The custom number formats that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getCustomNumberFormats()}. When overriding this method, always consider adding to the return
         * value of the super method, rather than replacing it.
         *
         * @return Immutable {@link Map}; not {@code null}
         */
        protected Map<String, TemplateNumberFormatFactory> getImpliedCustomNumberFormats() {
            return Collections.emptyMap();
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

        /**
         * {@inheritDoc}
         *
         * @see #getImpliedCustomDateFormats()
         */
        @Override
        protected Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats() {
            return Collections.emptyMap();
        }

        /**
         * The custom date formats that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getCustomDateFormats()}. When overriding this method, always consider adding to the return value
         * of the super method, rather than replacing it.
         *
         * @return Immutable {@link Map}; not {@code null}
         */
        protected Map<String, TemplateDateFormatFactory> getImpliedCustomDateFormats() {
            return Collections.emptyMap();
        }

        @Override
        protected TemplateDateFormatFactory getDefaultCustomDateFormat(String name) {
            return null;
        }

        @Override
        protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
            return TemplateExceptionHandler.RETHROW;
        }

        @Override
        protected AttemptExceptionReporter getDefaultAttemptExceptionReporter() {
            return AttemptExceptionReporter.LOG_ERROR;
        }
        
        @Override
        protected ArithmeticEngine getDefaultArithmeticEngine() {
            return BigDecimalArithmeticEngine.INSTANCE;
        }

        private DefaultObjectWrapper cachedDefaultObjectWrapper;

        /**
         * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
         * {@link ProcessingConfiguration}).
         */
        public void unsetObjectWrapper() {
            objectWrapper = null;
        }

        /**
         * Fluent API equivalent of {@link #setObjectWrapper(ObjectWrapper)}
         */
        public SelfT objectWrapper(ObjectWrapper value) {
            setObjectWrapper(value);
            return self();
        }

        @Override
        public ObjectWrapper getObjectWrapper() {
            return isObjectWrapperSet() ? objectWrapper : getDefaultObjectWrapper();
        }

        @Override
        public boolean isObjectWrapperSet() {
            return objectWrapper != null;
        }

        /**
         * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
         * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
         */
        protected ObjectWrapper getDefaultObjectWrapper() {
            if (cachedDefaultObjectWrapper == null) {
                // Note: This field is cleared by onIncompatibleImprovementsChanged
                Version incompatibleImprovements = getIncompatibleImprovements();
                cachedDefaultObjectWrapper = new DefaultObjectWrapper.Builder(incompatibleImprovements).build();
            }
            return cachedDefaultObjectWrapper;
        }

        public void setObjectWrapper(ObjectWrapper objectWrapper) {
            _NullArgumentException.check("objectWrapper", objectWrapper);
            this.objectWrapper = objectWrapper;
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
            return TemplateClassResolver.UNRESTRICTED;
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
        protected boolean getDefaultLazyImports() {
            return false;
        }

        @Override
        @SuppressFBWarnings("NP_BOOLEAN_RETURN_NULL")
        protected Boolean getDefaultLazyAutoImports() {
            return null;
        }

        /**
         * {@inheritDoc}
         *
         * @see #getImpliedAutoImports()
         */
        @Override
        protected Map<String, String> getDefaultAutoImports() {
            return Collections.emptyMap();
        }

        /**
         * The auto-imports that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getSharedVariables()}. When overriding this method, always consider adding to the return value
         * of the super method, rather than replacing it.
         */
        protected Map<String, String> getImpliedAutoImports() {
            return Collections.emptyMap();
        }

        /**
         * {@inheritDoc}
         *
         * @see #getImpliedAutoIncludes()
         */
        @Override
        protected List<String> getDefaultAutoIncludes() {
            return Collections.emptyList();
        }

        /**
         * The imports that will be added to the built {@link Configuration} before the ones coming from
         * {@link #getAutoIncludes()}. When overriding this method, always consider adding to the return
         * value of the super method, rather than replacing it.
         *
         * @return Immutable {@link List}; not {@code null}
         */
        protected List<String> getImpliedAutoIncludes() {
            return Collections.emptyList();
        }

        @Override
        protected Object getDefaultCustomSetting(Serializable key, Object defaultValue, boolean useDefaultValue) {
            if (useDefaultValue) {
                return defaultValue;
            }
            throw new CustomSettingValueNotSetException(key);
        }

        @Override
        protected void collectDefaultCustomSettingsSnapshot(Map<Serializable, Object> target) {
            // Doesn't inherit anything
        }
    }

    /**
     * Creates a new {@link Configuration}, with the setting specified in this object. Note that {@link Configuration}-s
     * are immutable (since FreeMarker 3.0.0), that's why the builder class is needed.
     */
    public static final class Builder extends ExtendableBuilder<Builder> {

        /**
         * @param incompatibleImprovements
         *         Specifies the value of the {@link Configuration#getIncompatibleImprovements()}
         *         incompatibleImprovements} setting, such as {@link Configuration#VERSION_3_0_0}. This setting can't be
         *         changed later.
         */
        public Builder(Version incompatibleImprovements) {
            super(incompatibleImprovements);
        }

    }

}

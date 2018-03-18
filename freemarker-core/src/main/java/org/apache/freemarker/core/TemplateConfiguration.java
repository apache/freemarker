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

import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.util._CollectionUtils;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * A partial set of configuration settings used for customizing the {@link Configuration}-level settings for individual
 * {@link Template}-s (or rather, for a group of templates). That it's partial means that you should call the
 * corresponding {@code isXxxSet()} before getting a settings, or else you may cause
 * {@link CoreSettingValueNotSetException}. (There's no fallback to the {@link Configuration}-level settings to keep the
 * dependency graph of configuration related beans non-cyclic. As user code seldom reads settings directly from
 * {@link TemplateConfiguration}-s anyway, this compromise was chosen.)
 * <p>
 * Note on the {@code locale} setting: When used with the standard template loading/caching mechanism ({@link
 * Configuration#getTemplate(String)} and its overloads), localized template lookup happens before the {@code locale}
 * specified here could have effect. The {@code locale} will be only set in the template that the localized
 * template lookup has already found.
 * <p>
 * This class is immutable. Use {@link TemplateConfiguration.Builder} to create a new instance.
 *
 * @see Template#Template(String, String, Reader, Configuration, TemplateConfiguration, Charset)
 */
public final class TemplateConfiguration implements ParsingAndProcessingConfiguration {

    private final Locale locale;
    private final String numberFormat;
    private final String timeFormat;
    private final String dateFormat;
    private final String dateTimeFormat;
    private final TimeZone timeZone;
    private final TimeZone sqlDateAndTimeTimeZone;
    private final boolean sqlDateAndTimeTimeZoneSet;
    private final String booleanFormat;
    private final TemplateExceptionHandler templateExceptionHandler;
    private final AttemptExceptionReporter attemptExceptionReporter;
    private final ArithmeticEngine arithmeticEngine;
    private final Charset outputEncoding;
    private final boolean outputEncodingSet;
    private final Charset urlEscapingCharset;
    private final boolean urlEscapingCharsetSet;
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
    private final boolean lazyAutoImportsSet;
    private final Map<Serializable, Object> customSettings;
    
    private final TemplateLanguage templateLanguage;
    private final TagSyntax tagSyntax;
    private final InterpolationSyntax interpolationSyntax;
    private final Boolean whitespaceStripping;
    private final AutoEscapingPolicy autoEscapingPolicy;
    private final Boolean recognizeStandardFileExtensions;
    private final OutputFormat outputFormat;
    private final Charset sourceEncoding;
    private final Integer tabSize;

    private TemplateConfiguration(Builder builder) {
        locale = builder.isLocaleSet() ? builder.getLocale() : null;
        numberFormat = builder.isNumberFormatSet() ? builder.getNumberFormat() : null;
        timeFormat = builder.isTimeFormatSet() ? builder.getTimeFormat() : null;
        dateFormat = builder.isDateFormatSet() ? builder.getDateFormat() : null;
        dateTimeFormat = builder.isDateTimeFormatSet() ? builder.getDateTimeFormat() : null;
        timeZone = builder.isTimeZoneSet() ? builder.getTimeZone() : null;
        sqlDateAndTimeTimeZoneSet = builder.isSQLDateAndTimeTimeZoneSet();
        sqlDateAndTimeTimeZone = sqlDateAndTimeTimeZoneSet ? builder.getSQLDateAndTimeTimeZone() : null;
        booleanFormat = builder.isBooleanFormatSet() ? builder.getBooleanFormat() : null;
        templateExceptionHandler = builder.isTemplateExceptionHandlerSet() ? builder.getTemplateExceptionHandler()
                : null;
        attemptExceptionReporter = builder.isAttemptExceptionReporterSet() ? builder.getAttemptExceptionReporter()
                : null;
        arithmeticEngine = builder.isArithmeticEngineSet() ? builder.getArithmeticEngine() : null;
        outputEncodingSet = builder.isOutputEncodingSet();
        outputEncoding = outputEncodingSet ? builder.getOutputEncoding() : null;
        urlEscapingCharsetSet = builder.isURLEscapingCharsetSet();
        urlEscapingCharset = urlEscapingCharsetSet ? builder.getURLEscapingCharset() : null;
        autoFlush = builder.isAutoFlushSet() ? builder.getAutoFlush() : null;
        newBuiltinClassResolver = builder.isNewBuiltinClassResolverSet() ? builder.getNewBuiltinClassResolver() : null;
        showErrorTips = builder.isShowErrorTipsSet() ? builder.getShowErrorTips() : null;
        apiBuiltinEnabled = builder.isAPIBuiltinEnabledSet() ? builder.getAPIBuiltinEnabled() : null;
        customDateFormats = builder.isCustomDateFormatsSet() ? builder.getCustomDateFormats() : null;
        customNumberFormats = builder.isCustomNumberFormatsSet() ? builder.getCustomNumberFormats() : null;
        autoImports = builder.isAutoImportsSet() ? builder.getAutoImports() : null;
        autoIncludes = builder.isAutoIncludesSet() ? builder.getAutoIncludes() : null;
        lazyImports = builder.isLazyImportsSet() ? builder.getLazyImports() : null;
        lazyAutoImportsSet = builder.isLazyAutoImportsSet();
        lazyAutoImports = lazyAutoImportsSet ? builder.getLazyAutoImports() : null;
        customSettings = builder.getCustomSettings(false);

        templateLanguage = builder.isTemplateLanguageSet() ? builder.getTemplateLanguage() : null;
        tagSyntax = builder.isTagSyntaxSet() ? builder.getTagSyntax() : null;
        interpolationSyntax = builder.isInterpolationSyntaxSet() ? builder.getInterpolationSyntax() : null;
        whitespaceStripping = builder.isWhitespaceStrippingSet() ? builder.getWhitespaceStripping() : null;
        autoEscapingPolicy = builder.isAutoEscapingPolicySet() ? builder.getAutoEscapingPolicy() : null;
        recognizeStandardFileExtensions = builder.isRecognizeStandardFileExtensionsSet() ? builder.getRecognizeStandardFileExtensions() : null;
        outputFormat = builder.isOutputFormatSet() ? builder.getOutputFormat() : null;
        sourceEncoding = builder.isSourceEncodingSet() ? builder.getSourceEncoding() : null;
        tabSize = builder.isTabSizeSet() ? builder.getTabSize() : null;
    }

    @Override
    public TagSyntax getTagSyntax() {
        if (!isTagSyntaxSet()) {
            throw new CoreSettingValueNotSetException("tagSyntax");
        }
        return tagSyntax;
    }
    
    @Override
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    @Override
    public InterpolationSyntax getInterpolationSyntax() {
        if (!isInterpolationSyntaxSet()) {
            throw new CoreSettingValueNotSetException("interpolationSyntax");
        }
        return interpolationSyntax;
    }

    @Override
    public boolean isInterpolationSyntaxSet() {
        return interpolationSyntax != null;
    }
    
    @Override
    public TemplateLanguage getTemplateLanguage() {
        if (!isTemplateLanguageSet()) {
            throw new CoreSettingValueNotSetException("templateLanguage");
        }
        return templateLanguage;
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return templateLanguage != null;
    }

    @Override
    public boolean getWhitespaceStripping() {
        if (!isWhitespaceStrippingSet()) {
            throw new CoreSettingValueNotSetException("whitespaceStripping");
        }
        return whitespaceStripping;
    }

    @Override
    public boolean isWhitespaceStrippingSet() {
        return whitespaceStripping != null;
    }

    @Override
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        if (!isAutoEscapingPolicySet()) {
            throw new CoreSettingValueNotSetException("autoEscapingPolicy");
        }
        return autoEscapingPolicy;
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    @Override
    public OutputFormat getOutputFormat() {
        if (!isOutputFormatSet()) {
            throw new CoreSettingValueNotSetException("outputFormat");
        }
        return outputFormat;
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        if (!isArithmeticEngineSet()) {
            throw new CoreSettingValueNotSetException("arithmeticEngine");
        }
        return arithmeticEngine;
    }

    @Override
    public boolean isArithmeticEngineSet() {
        return arithmeticEngine != null;
    }

    @Override
    public boolean isOutputFormatSet() {
        return outputFormat != null;
    }
    
    @Override
    public boolean getRecognizeStandardFileExtensions() {
        if (!isRecognizeStandardFileExtensionsSet()) {
            throw new CoreSettingValueNotSetException("recognizeStandardFileExtensions");
        }
        return recognizeStandardFileExtensions;
    }
    
    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return recognizeStandardFileExtensions != null;
    }

    @Override
    public Charset getSourceEncoding() {
        if (!isSourceEncodingSet()) {
            throw new CoreSettingValueNotSetException("sourceEncoding");
        }
        return sourceEncoding;
    }

    @Override
    public boolean isSourceEncodingSet() {
        return sourceEncoding != null;
    }
    
    @Override
    public int getTabSize() {
        if (!isTabSizeSet()) {
            throw new CoreSettingValueNotSetException("tabSize");
        }
        return tabSize;
    }
    
    @Override
    public boolean isTabSizeSet() {
        return tabSize != null;
    }
    
    /**
     * Always throws {@link CoreSettingValueNotSetException}, as this can't be set on the {@link TemplateConfiguration}
     * level.
     */
    @Override
    public Version getIncompatibleImprovements() {
        throw new CoreSettingValueNotSetException("incompatibleImprovements");
    }

    @Override
    public boolean isIncompatibleImprovementsSet() {
        return false;
    }

    @Override
    public Locale getLocale() {
        if (!isLocaleSet()) {
            throw new CoreSettingValueNotSetException("locale");
        }
        return locale;
    }

    @Override
    public boolean isLocaleSet() {
        return locale != null;
    }

    @Override
    public TimeZone getTimeZone() {
        if (!isTimeZoneSet()) {
            throw new CoreSettingValueNotSetException("timeZone");
        }
        return timeZone;
    }

    @Override
    public boolean isTimeZoneSet() {
        return timeZone != null;
    }

    @Override
    public TimeZone getSQLDateAndTimeTimeZone() {
        if (!isSQLDateAndTimeTimeZoneSet()) {
            throw new CoreSettingValueNotSetException("sqlDateAndTimeTimeZone");
        }
        return sqlDateAndTimeTimeZone;
    }

    @Override
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return sqlDateAndTimeTimeZoneSet;
    }

    @Override
    public String getNumberFormat() {
        if (!isNumberFormatSet()) {
            throw new CoreSettingValueNotSetException("numberFormat");
        }
        return numberFormat;
    }

    @Override
    public boolean isNumberFormatSet() {
        return numberFormat != null;
    }

    @Override
    public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
        if (!isCustomNumberFormatsSet()) {
            throw new CoreSettingValueNotSetException("customNumberFormats");
        }
        return customNumberFormats;
    }

    @Override
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        return getCustomNumberFormats().get(name);
    }

    @Override
    public boolean isCustomNumberFormatsSet() {
        return customNumberFormats != null;
    }

    @Override
    public String getBooleanFormat() {
        if (!isBooleanFormatSet()) {
            throw new CoreSettingValueNotSetException("booleanFormat");
        }
        return booleanFormat;
    }

    @Override
    public boolean isBooleanFormatSet() {
        return booleanFormat != null;
    }

    @Override
    public String getTimeFormat() {
        if (!isTimeFormatSet()) {
            throw new CoreSettingValueNotSetException("timeFormat");
        }
        return timeFormat;
    }

    @Override
    public boolean isTimeFormatSet() {
        return timeFormat != null;
    }

    @Override
    public String getDateFormat() {
        if (!isDateFormatSet()) {
            throw new CoreSettingValueNotSetException("dateFormat");
        }
        return dateFormat;
    }

    @Override
    public boolean isDateFormatSet() {
        return dateFormat != null;
    }

    @Override
    public String getDateTimeFormat() {
        if (!isDateTimeFormatSet()) {
            throw new CoreSettingValueNotSetException("dateTimeFormat");
        }
        return dateTimeFormat;
    }

    @Override
    public boolean isDateTimeFormatSet() {
        return dateTimeFormat != null;
    }

    @Override
    public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
        if (!isCustomDateFormatsSet()) {
            throw new CoreSettingValueNotSetException("customDateFormats");
        }
        return customDateFormats;
    }

    @Override
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        if (isCustomDateFormatsSet()) {
            TemplateDateFormatFactory format = customDateFormats.get(name);
            if (format != null) {
                return  format;
            }
        }
        return null;
    }

    @Override
    public boolean isCustomDateFormatsSet() {
        return customDateFormats != null;
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        if (!isTemplateExceptionHandlerSet()) {
            throw new CoreSettingValueNotSetException("templateExceptionHandler");
        }
        return templateExceptionHandler;
    }

    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return templateExceptionHandler != null;
    }

    @Override
    public AttemptExceptionReporter getAttemptExceptionReporter() {
        if (!isAttemptExceptionReporterSet()) {
            throw new CoreSettingValueNotSetException("attemptExceptionReporter");
        }
        return attemptExceptionReporter;
    }

    @Override
    public boolean isAttemptExceptionReporterSet() {
        return attemptExceptionReporter != null;
    }
    
    @Override
    public Charset getOutputEncoding() {
        if (!isOutputEncodingSet()) {
            throw new CoreSettingValueNotSetException("");
        }
        return outputEncoding;
    }

    @Override
    public boolean isOutputEncodingSet() {
        return outputEncodingSet;
    }

    @Override
    public Charset getURLEscapingCharset() {
        if (!isURLEscapingCharsetSet()) {
            throw new CoreSettingValueNotSetException("urlEscapingCharset");
        }
        return urlEscapingCharset;
    }

    @Override
    public boolean isURLEscapingCharsetSet() {
        return urlEscapingCharsetSet;
    }

    @Override
    public TemplateClassResolver getNewBuiltinClassResolver() {
        if (!isNewBuiltinClassResolverSet()) {
            throw new CoreSettingValueNotSetException("newBuiltinClassResolver");
        }
        return newBuiltinClassResolver;
    }

    @Override
    public boolean isNewBuiltinClassResolverSet() {
        return newBuiltinClassResolver != null;
    }

    @Override
    public boolean getAPIBuiltinEnabled() {
        if (!isAPIBuiltinEnabledSet()) {
            throw new CoreSettingValueNotSetException("apiBuiltinEnabled");
        }
        return apiBuiltinEnabled;
    }

    @Override
    public boolean isAPIBuiltinEnabledSet() {
        return apiBuiltinEnabled != null;
    }

    @Override
    public boolean getAutoFlush() {
        if (!isAutoFlushSet()) {
            throw new CoreSettingValueNotSetException("autoFlush");
        }
        return autoFlush;
    }

    @Override
    public boolean isAutoFlushSet() {
        return autoFlush != null;
    }

    @Override
    public boolean getShowErrorTips() {
        if (!isShowErrorTipsSet()) {
            throw new CoreSettingValueNotSetException("showErrorTips");
        }
        return showErrorTips;
    }

    @Override
    public boolean isShowErrorTipsSet() {
        return showErrorTips != null;
    }

    @Override
    public boolean getLazyImports() {
        if (!isLazyImportsSet()) {
            throw new CoreSettingValueNotSetException("lazyImports");
        }
        return lazyImports;
    }

    @Override
    public boolean isLazyImportsSet() {
        return lazyImports != null;
    }

    @Override
    public Boolean getLazyAutoImports() {
        if (!isLazyAutoImportsSet()) {
            throw new CoreSettingValueNotSetException("lazyAutoImports");
        }
        return lazyAutoImports;
    }

    @Override
    public boolean isLazyAutoImportsSet() {
        return lazyAutoImportsSet;
    }

    @Override
    public Map<String, String> getAutoImports() {
        if (!isAutoImportsSet()) {
            throw new CoreSettingValueNotSetException("");
        }
        return autoImports;
    }

    @Override
    public boolean isAutoImportsSet() {
        return autoImports != null;
    }

    @Override
    public List<String> getAutoIncludes() {
        if (!isAutoIncludesSet()) {
            throw new CoreSettingValueNotSetException("autoIncludes");
        }
        return autoIncludes;
    }

    @Override
    public boolean isAutoIncludesSet() {
        return autoIncludes != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the {@code includeInherited} has no effect here, as {@link TemplateConfiguration}-s has no parent.
     */
    @Override
    public Map<Serializable, Object> getCustomSettings(boolean includeInherited) {
        return customSettings;
    }

    @Override
    public boolean isCustomSettingSet(Serializable key) {
        return customSettings.containsKey(key);
    }

    @Override
    public Object getCustomSetting(Serializable key) {
        Object result = getCustomSetting(key, MISSING_VALUE_MARKER);
        if (result == MISSING_VALUE_MARKER) {
            throw new CustomSettingValueNotSetException(key);
        }
        return result;
    }

    @Override
    public Object getCustomSetting(Serializable key, Object defaultValue) {
        Object attValue = customSettings.get(key);
        if (attValue != null || customSettings.containsKey(key)) {
            return attValue;
        }
        return defaultValue;
    }

    public static final class Builder extends MutableParsingAndProcessingConfiguration<Builder>
            implements CommonBuilder<TemplateConfiguration> {

        private boolean alreadyBuilt;

        public Builder() {
            super();
        }

        @Override
        public TemplateConfiguration build() {
            if (alreadyBuilt) {
                throw new IllegalStateException("build() can only be executed once.");
            }
            TemplateConfiguration templateConfiguration = new TemplateConfiguration(this);
            alreadyBuilt = true;
            return templateConfiguration;
        }

        @Override
        protected Locale getDefaultLocale() {
            throw new CoreSettingValueNotSetException("locale");
        }

        @Override
        protected TimeZone getDefaultTimeZone() {
            throw new CoreSettingValueNotSetException("timeZone");
        }

        @Override
        protected TimeZone getDefaultSQLDateAndTimeTimeZone() {
            throw new CoreSettingValueNotSetException("SQLDateAndTimeTimeZone");
        }

        @Override
        protected String getDefaultNumberFormat() {
            throw new CoreSettingValueNotSetException("numberFormat");
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats() {
            throw new CoreSettingValueNotSetException("customNumberFormats");
        }

        @Override
        protected TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name) {
            return null;
        }

        @Override
        protected String getDefaultBooleanFormat() {
            throw new CoreSettingValueNotSetException("booleanFormat");
        }

        @Override
        protected String getDefaultTimeFormat() {
            throw new CoreSettingValueNotSetException("timeFormat");
        }

        @Override
        protected String getDefaultDateFormat() {
            throw new CoreSettingValueNotSetException("dateFormat");
        }

        @Override
        protected String getDefaultDateTimeFormat() {
            throw new CoreSettingValueNotSetException("dateTimeFormat");
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats() {
            throw new CoreSettingValueNotSetException("customDateFormats");
        }

        @Override
        protected TemplateDateFormatFactory getDefaultCustomDateFormat(String name) {
            throw new CoreSettingValueNotSetException("customDateFormat");
        }

        @Override
        protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
            throw new CoreSettingValueNotSetException("templateExceptionHandler");
        }

        @Override
        protected AttemptExceptionReporter getDefaultAttemptExceptionReporter() {
            throw new CoreSettingValueNotSetException("attemptExceptionReporter");
        }

        @Override
        protected ArithmeticEngine getDefaultArithmeticEngine() {
            throw new CoreSettingValueNotSetException("arithmeticEngine");
        }

        @Override
        protected Charset getDefaultOutputEncoding() {
            throw new CoreSettingValueNotSetException("outputEncoding");
        }

        @Override
        protected Charset getDefaultURLEscapingCharset() {
            throw new CoreSettingValueNotSetException("URLEscapingCharset");
        }

        @Override
        protected TemplateClassResolver getDefaultNewBuiltinClassResolver() {
            throw new CoreSettingValueNotSetException("newBuiltinClassResolver");
        }

        @Override
        protected boolean getDefaultAutoFlush() {
            throw new CoreSettingValueNotSetException("autoFlush");
        }

        @Override
        protected boolean getDefaultShowErrorTips() {
            throw new CoreSettingValueNotSetException("showErrorTips");
        }

        @Override
        protected boolean getDefaultAPIBuiltinEnabled() {
            throw new CoreSettingValueNotSetException("APIBuiltinEnabled");
        }

        @Override
        protected boolean getDefaultLazyImports() {
            throw new CoreSettingValueNotSetException("lazyImports");
        }

        @Override
        protected Boolean getDefaultLazyAutoImports() {
            throw new CoreSettingValueNotSetException("lazyAutoImports");
        }

        @Override
        protected Map<String, String> getDefaultAutoImports() {
            throw new CoreSettingValueNotSetException("autoImports");
        }

        @Override
        protected List<String> getDefaultAutoIncludes() {
            throw new CoreSettingValueNotSetException("autoIncludes");
        }

        @Override
        protected Object getDefaultCustomSetting(Serializable key, Object defaultValue, boolean useDefaultValue) {
            // We don't inherit from anything.
            if (useDefaultValue) {
                return defaultValue;
            }
            throw new CustomSettingValueNotSetException(key);
        }

        @Override
        protected void collectDefaultCustomSettingsSnapshot(Map<Serializable, Object> target) {
            // We don't inherit from anything.
        }

        /**
         * Set all settings in this {@link Builder} that were set in the parameter {@link TemplateConfiguration} (or
         * other {@link ParsingAndProcessingConfiguration}), possibly overwriting the earlier value in this object.
         * (A setting is said to be set in a {@link ParsingAndProcessingConfiguration} if it was explicitly set via a
         * setter method, as opposed to be inherited.)
         */
        public void merge(TemplateConfiguration tc) {
            if (tc.isAPIBuiltinEnabledSet()) {
                setAPIBuiltinEnabled(tc.getAPIBuiltinEnabled());
            }
            if (tc.isArithmeticEngineSet()) {
                setArithmeticEngine(tc.getArithmeticEngine());
            }
            if (tc.isAutoEscapingPolicySet()) {
                setAutoEscapingPolicy(tc.getAutoEscapingPolicy());
            }
            if (tc.isAutoFlushSet()) {
                setAutoFlush(tc.getAutoFlush());
            }
            if (tc.isBooleanFormatSet()) {
                setBooleanFormat(tc.getBooleanFormat());
            }
            if (tc.isCustomDateFormatsSet()) {
                setCustomDateFormats(_CollectionUtils.mergeImmutableMaps(
                        isCustomDateFormatsSet() ? getCustomDateFormats() : null, tc.getCustomDateFormats(), false),
                        true
                );
            }
            if (tc.isCustomNumberFormatsSet()) {
                setCustomNumberFormats(_CollectionUtils.mergeImmutableMaps(
                        isCustomNumberFormatsSet() ? getCustomNumberFormats() : null, tc.getCustomNumberFormats(), false),
                        true);
            }
            if (tc.isDateFormatSet()) {
                setDateFormat(tc.getDateFormat());
            }
            if (tc.isDateTimeFormatSet()) {
                setDateTimeFormat(tc.getDateTimeFormat());
            }
            if (tc.isSourceEncodingSet()) {
                setSourceEncoding(tc.getSourceEncoding());
            }
            if (tc.isLocaleSet()) {
                setLocale(tc.getLocale());
            }
            if (tc.isNewBuiltinClassResolverSet()) {
                setNewBuiltinClassResolver(tc.getNewBuiltinClassResolver());
            }
            if (tc.isNumberFormatSet()) {
                setNumberFormat(tc.getNumberFormat());
            }
            if (tc.isOutputEncodingSet()) {
                setOutputEncoding(tc.getOutputEncoding());
            }
            if (tc.isOutputFormatSet()) {
                setOutputFormat(tc.getOutputFormat());
            }
            if (tc.isRecognizeStandardFileExtensionsSet()) {
                setRecognizeStandardFileExtensions(tc.getRecognizeStandardFileExtensions());
            }
            if (tc.isShowErrorTipsSet()) {
                setShowErrorTips(tc.getShowErrorTips());
            }
            if (tc.isSQLDateAndTimeTimeZoneSet()) {
                setSQLDateAndTimeTimeZone(tc.getSQLDateAndTimeTimeZone());
            }
            if (tc.isTagSyntaxSet()) {
                setTagSyntax(tc.getTagSyntax());
            }
            if (tc.isInterpolationSyntaxSet()) {
                setInterpolationSyntax(tc.getInterpolationSyntax());
            }
            if (tc.isTemplateLanguageSet()) {
                setTemplateLanguage(tc.getTemplateLanguage());
            }
            if (tc.isTemplateExceptionHandlerSet()) {
                setTemplateExceptionHandler(tc.getTemplateExceptionHandler());
            }
            if (tc.isAttemptExceptionReporterSet()) {
                setAttemptExceptionReporter(tc.getAttemptExceptionReporter());
            }
            if (tc.isTimeFormatSet()) {
                setTimeFormat(tc.getTimeFormat());
            }
            if (tc.isTimeZoneSet()) {
                setTimeZone(tc.getTimeZone());
            }
            if (tc.isURLEscapingCharsetSet()) {
                setURLEscapingCharset(tc.getURLEscapingCharset());
            }
            if (tc.isWhitespaceStrippingSet()) {
                setWhitespaceStripping(tc.getWhitespaceStripping());
            }
            if (tc.isTabSizeSet()) {
                setTabSize(tc.getTabSize());
            }
            if (tc.isLazyImportsSet()) {
                setLazyImports(tc.getLazyImports());
            }
            if (tc.isLazyAutoImportsSet()) {
                setLazyAutoImports(tc.getLazyAutoImports());
            }
            if (tc.isAutoImportsSet()) {
                setAutoImports(_CollectionUtils.mergeImmutableMaps(
                        isAutoImportsSet() ? getAutoImports() : null,
                        tc.isAutoImportsSet() ? tc.getAutoImports() : null,
                        true),
                        true);
            }
            if (tc.isAutoIncludesSet()) {
                setAutoIncludes(_CollectionUtils.mergeImmutableLists(
                        isAutoIncludesSet() ? getAutoIncludes() : null,
                        tc.isAutoIncludesSet() ? tc.getAutoIncludes() : null,
                        true),
                        true);
            }

            setCustomSettingsMap(_CollectionUtils.mergeImmutableMaps(
                    getCustomSettings(false),
                    tc.getCustomSettings(false),
                    false));
        }

        @Override
        public Version getIncompatibleImprovements() {
            throw new CoreSettingValueNotSetException("incompatibleImprovements");
        }

        @Override
        public boolean isIncompatibleImprovementsSet() {
            return false;
        }

        @Override
        protected TagSyntax getDefaultTagSyntax() {
            throw new CoreSettingValueNotSetException("tagSyntax");
        }
        
        @Override
        protected InterpolationSyntax getDefaultInterpolationSyntax() {
            throw new CoreSettingValueNotSetException("interpolationSyntax");
        }

        @Override
        protected TemplateLanguage getDefaultTemplateLanguage() {
            throw new CoreSettingValueNotSetException("templateLanguage");
        }

        @Override
        protected boolean getDefaultWhitespaceStripping() {
            throw new CoreSettingValueNotSetException("whitespaceStripping");
        }

        @Override
        protected AutoEscapingPolicy getDefaultAutoEscapingPolicy() {
            throw new CoreSettingValueNotSetException("autoEscapingPolicy");
        }

        @Override
        protected OutputFormat getDefaultOutputFormat() {
            throw new CoreSettingValueNotSetException("outputFormat");
        }

        @Override
        protected boolean getDefaultRecognizeStandardFileExtensions() {
            throw new CoreSettingValueNotSetException("recognizeStandardFileExtensions");
        }

        @Override
        protected Charset getDefaultSourceEncoding() {
            throw new CoreSettingValueNotSetException("sourceEncoding");
        }

        @Override
        protected int getDefaultTabSize() {
            throw new CoreSettingValueNotSetException("tabSize");
        }

    }

}

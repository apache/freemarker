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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * A partial set of configuration settings used for customizing the {@link Configuration}-level settings for individual
 * {@link Template}-s (or rather, for a group of templates). That it's partial means that you should call the
 * corresponding {@code isXxxSet()} before getting a settings, or else you may cause
 * {@link SettingValueNotSetException}. (The fallback to the {@link Configuration} setting isn't automatic to keep
 * the dependency graph of configuration related beans non-cyclic. As user code seldom reads settings from here anyway,
 * this compromise was chosen.)
 * <p>
 * Note on the {@code locale} setting: When used with the standard template loading/caching mechanism ({@link
 * Configuration#getTemplate(String)} and its overloads), localized lookup happens before the {@code locale} specified
 * here could have effect. The {@code locale} will be only set in the template that the localized lookup has already
 * found.
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
    private final ArithmeticEngine arithmeticEngine;
    private final ObjectWrapper objectWrapper;
    private final Charset outputEncoding;
    private final boolean outputEncodingSet;
    private final Charset urlEscapingCharset;
    private final boolean urlEscapingCharsetSet;
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
    private final boolean lazyAutoImportsSet;
    private final Map<Object, Object> customAttributes;
    
    private final TemplateLanguage templateLanguage;
    private final TagSyntax tagSyntax;
    private final NamingConvention namingConvention;
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
        templateExceptionHandler = builder.isTemplateExceptionHandlerSet() ? builder.getTemplateExceptionHandler() : null;
        arithmeticEngine = builder.isArithmeticEngineSet() ? builder.getArithmeticEngine() : null;
        objectWrapper = builder.isObjectWrapperSet() ? builder.getObjectWrapper() : null;
        outputEncodingSet = builder.isOutputEncodingSet();
        outputEncoding = outputEncodingSet ? builder.getOutputEncoding() : null;
        urlEscapingCharsetSet = builder.isURLEscapingCharsetSet();
        urlEscapingCharset = urlEscapingCharsetSet ? builder.getURLEscapingCharset() : null;
        autoFlush = builder.isAutoFlushSet() ? builder.getAutoFlush() : null;
        newBuiltinClassResolver = builder.isNewBuiltinClassResolverSet() ? builder.getNewBuiltinClassResolver() : null;
        showErrorTips = builder.isShowErrorTipsSet() ? builder.getShowErrorTips() : null;
        apiBuiltinEnabled = builder.isAPIBuiltinEnabledSet() ? builder.getAPIBuiltinEnabled() : null;
        logTemplateExceptions = builder.isLogTemplateExceptionsSet() ? builder.getLogTemplateExceptions() : null;
        customDateFormats = builder.isCustomDateFormatsSet() ? builder.getCustomDateFormats() : null;
        customNumberFormats = builder.isCustomNumberFormatsSet() ? builder.getCustomNumberFormats() : null;
        autoImports = builder.isAutoImportsSet() ? builder.getAutoImports() : null;
        autoIncludes = builder.isAutoIncludesSet() ? builder.getAutoIncludes() : null;
        lazyImports = builder.isLazyImportsSet() ? builder.getLazyImports() : null;
        lazyAutoImportsSet = builder.isLazyAutoImportsSet();
        lazyAutoImports = lazyAutoImportsSet ? builder.getLazyAutoImports() : null;
        customAttributes = builder.isCustomAttributesSet() ? builder.getCustomAttributes() : null;

        templateLanguage = builder.isTemplateLanguageSet() ? builder.getTemplateLanguage() : null;
        tagSyntax = builder.isTagSyntaxSet() ? builder.getTagSyntax() : null;
        namingConvention = builder.isNamingConventionSet() ? builder.getNamingConvention() : null;
        whitespaceStripping = builder.isWhitespaceStrippingSet() ? builder.getWhitespaceStripping() : null;
        autoEscapingPolicy = builder.isAutoEscapingPolicySet() ? builder.getAutoEscapingPolicy() : null;
        recognizeStandardFileExtensions = builder.isRecognizeStandardFileExtensionsSet() ? builder.getRecognizeStandardFileExtensions() : null;
        outputFormat = builder.isOutputFormatSet() ? builder.getOutputFormat() : null;
        sourceEncoding = builder.isSourceEncodingSet() ? builder.getSourceEncoding() : null;
        tabSize = builder.isTabSizeSet() ? builder.getTabSize() : null;
    }

    /**
     * Adds two {@link Map}-s (keeping the iteration order); assuming the inputs are already unmodifiable and
     * unchanging, it returns an unmodifiable and unchanging {@link Map} itself.
     */
    private static <K,V> Map<K,V> mergeMaps(Map<K,V> m1, Map<K,V> m2, boolean overwriteUpdatesOrder) {
        if (m1 == null) return m2;
        if (m2 == null) return m1;
        if (m1.isEmpty()) return m2;
        if (m2.isEmpty()) return m1;

        LinkedHashMap<K, V> mergedM = new LinkedHashMap<>((m1.size() + m2.size()) * 4 / 3 + 1, 0.75f);
        mergedM.putAll(m1);
        if (overwriteUpdatesOrder) {
            for (K m2Key : m2.keySet()) {
                mergedM.remove(m2Key); // So that duplicate keys are moved after m1 keys
            }
        }
        mergedM.putAll(m2);
        return Collections.unmodifiableMap(mergedM);
    }

    /**
     * Adds two {@link List}-s; assuming the inputs are already unmodifiable and unchanging, it returns an
     * unmodifiable and unchanging {@link List} itself.
     */
    private static List<String> mergeLists(List<String> list1, List<String> list2) {
        if (list1 == null) return list2;
        if (list2 == null) return list1;
        if (list1.isEmpty()) return list2;
        if (list2.isEmpty()) return list1;

        ArrayList<String> mergedList = new ArrayList<>(list1.size() + list2.size());
        mergedList.addAll(list1);
        mergedList.addAll(list2);
        return Collections.unmodifiableList(mergedList);
    }

    /**
     * For internal usage only, copies the custom attributes set directly on this objects into another
     * {@link MutableProcessingConfiguration}. The target {@link MutableProcessingConfiguration} is assumed to be not seen be other thread than the current
     * one yet. (That is, the operation is not synchronized on the target {@link MutableProcessingConfiguration}, only on the source
     * {@link MutableProcessingConfiguration})
     */
    private void copyDirectCustomAttributes(MutableProcessingConfiguration<?> target, boolean overwriteExisting) {
        if (customAttributes == null) {
            return;
        }
        for (Map.Entry<?, ?> custAttrEnt : customAttributes.entrySet()) {
            Object custAttrKey = custAttrEnt.getKey();
            if (overwriteExisting || !target.isCustomAttributeSet(custAttrKey)) {
                target.setCustomAttribute(custAttrKey, custAttrEnt.getValue());
            }
        }
    }

    @Override
    public TagSyntax getTagSyntax() {
        if (!isTagSyntaxSet()) {
            throw new SettingValueNotSetException("tagSyntax");
        }
        return tagSyntax;
    }

    @Override
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        if (!isTemplateLanguageSet()) {
            throw new SettingValueNotSetException("templateLanguage");
        }
        return templateLanguage;
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return templateLanguage != null;
    }

    @Override
    public NamingConvention getNamingConvention() {
        if (!isNamingConventionSet()) {
            throw new SettingValueNotSetException("namingConvention");
        }
        return namingConvention;
    }

    @Override
    public boolean isNamingConventionSet() {
        return namingConvention != null;
    }

    @Override
    public boolean getWhitespaceStripping() {
        if (!isWhitespaceStrippingSet()) {
            throw new SettingValueNotSetException("whitespaceStripping");
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
            throw new SettingValueNotSetException("autoEscapingPolicy");
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
            throw new SettingValueNotSetException("outputFormat");
        }
        return outputFormat;
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        if (!isArithmeticEngineSet()) {
            throw new SettingValueNotSetException("arithmeticEngine");
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
            throw new SettingValueNotSetException("recognizeStandardFileExtensions");
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
            throw new SettingValueNotSetException("sourceEncoding");
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
            throw new SettingValueNotSetException("tabSize");
        }
        return tabSize;
    }
    
    @Override
    public boolean isTabSizeSet() {
        return tabSize != null;
    }
    
    /**
     * Always throws {@link SettingValueNotSetException}, as this can't be set on the {@link TemplateConfiguration}
     * level.
     */
    @Override
    public Version getIncompatibleImprovements() {
        throw new SettingValueNotSetException("incompatibleImprovements");
    }

    @Override
    public Locale getLocale() {
        if (!isLocaleSet()) {
            throw new SettingValueNotSetException("locale");
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
            throw new SettingValueNotSetException("timeZone");
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
            throw new SettingValueNotSetException("sqlDateAndTimeTimeZone");
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
            throw new SettingValueNotSetException("numberFormat");
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
            throw new SettingValueNotSetException("customNumberFormats");
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
            throw new SettingValueNotSetException("booleanFormat");
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
            throw new SettingValueNotSetException("timeFormat");
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
            throw new SettingValueNotSetException("dateFormat");
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
            throw new SettingValueNotSetException("dateTimeFormat");
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
            throw new SettingValueNotSetException("customDateFormats");
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
            throw new SettingValueNotSetException("templateExceptionHandler");
        }
        return templateExceptionHandler;
    }

    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return templateExceptionHandler != null;
    }

    @Override
    public ObjectWrapper getObjectWrapper() {
        if (!isObjectWrapperSet()) {
            throw new SettingValueNotSetException("objectWrapper");
        }
        return objectWrapper;
    }

    @Override
    public boolean isObjectWrapperSet() {
        return objectWrapper != null;
    }

    @Override
    public Charset getOutputEncoding() {
        if (!isOutputEncodingSet()) {
            throw new SettingValueNotSetException("");
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
            throw new SettingValueNotSetException("urlEscapingCharset");
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
            throw new SettingValueNotSetException("newBuiltinClassResolver");
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
            throw new SettingValueNotSetException("apiBuiltinEnabled");
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
            throw new SettingValueNotSetException("autoFlush");
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
            throw new SettingValueNotSetException("showErrorTips");
        }
        return showErrorTips;
    }

    @Override
    public boolean isShowErrorTipsSet() {
        return showErrorTips != null;
    }

    @Override
    public boolean getLogTemplateExceptions() {
        if (!isLogTemplateExceptionsSet()) {
            throw new SettingValueNotSetException("logTemplateExceptions");
        }
        return logTemplateExceptions;
    }

    @Override
    public boolean isLogTemplateExceptionsSet() {
        return logTemplateExceptions != null;
    }

    @Override
    public boolean getLazyImports() {
        if (!isLazyImportsSet()) {
            throw new SettingValueNotSetException("lazyImports");
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
            throw new SettingValueNotSetException("lazyAutoImports");
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
            throw new SettingValueNotSetException("");
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
            throw new SettingValueNotSetException("autoIncludes");
        }
        return autoIncludes;
    }

    @Override
    public boolean isAutoIncludesSet() {
        return autoIncludes != null;
    }

    @Override
    public Map<Object, Object> getCustomAttributes() {
        if (!isCustomAttributesSet()) {
            throw new SettingValueNotSetException("customAttributes");
        }
        return customAttributes;
    }

    @Override
    public boolean isCustomAttributesSet() {
        return customAttributes != null;
    }

    @Override
    public Object getCustomAttribute(Object name) {
        Object attValue;
        if (isCustomAttributesSet()) {
            attValue = customAttributes.get(name);
            if (attValue != null || customAttributes.containsKey(name)) {
                return attValue;
            }
        }
        return null;
    }

    public static final class Builder extends MutableParsingAndProcessingConfiguration<Builder>
            implements CommonBuilder<TemplateConfiguration> {

        public Builder() {
            super();
        }

        @Override
        public TemplateConfiguration build() {
            return new TemplateConfiguration(this);
        }

        @Override
        protected Locale getDefaultLocale() {
            throw new SettingValueNotSetException("locale");
        }

        @Override
        protected TimeZone getDefaultTimeZone() {
            throw new SettingValueNotSetException("timeZone");
        }

        @Override
        protected TimeZone getDefaultSQLDateAndTimeTimeZone() {
            throw new SettingValueNotSetException("SQLDateAndTimeTimeZone");
        }

        @Override
        protected String getDefaultNumberFormat() {
            throw new SettingValueNotSetException("numberFormat");
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats() {
            throw new SettingValueNotSetException("customNumberFormats");
        }

        @Override
        protected TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name) {
            return null;
        }

        @Override
        protected String getDefaultBooleanFormat() {
            throw new SettingValueNotSetException("booleanFormat");
        }

        @Override
        protected String getDefaultTimeFormat() {
            throw new SettingValueNotSetException("timeFormat");
        }

        @Override
        protected String getDefaultDateFormat() {
            throw new SettingValueNotSetException("dateFormat");
        }

        @Override
        protected String getDefaultDateTimeFormat() {
            throw new SettingValueNotSetException("dateTimeFormat");
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats() {
            throw new SettingValueNotSetException("customDateFormats");
        }

        @Override
        protected TemplateDateFormatFactory getDefaultCustomDateFormat(String name) {
            throw new SettingValueNotSetException("customDateFormat");
        }

        @Override
        protected TemplateExceptionHandler getDefaultTemplateExceptionHandler() {
            throw new SettingValueNotSetException("templateExceptionHandler");
        }

        @Override
        protected ArithmeticEngine getDefaultArithmeticEngine() {
            throw new SettingValueNotSetException("arithmeticEngine");
        }

        @Override
        protected ObjectWrapper getDefaultObjectWrapper() {
            throw new SettingValueNotSetException("objectWrapper");
        }

        @Override
        protected Charset getDefaultOutputEncoding() {
            throw new SettingValueNotSetException("outputEncoding");
        }

        @Override
        protected Charset getDefaultURLEscapingCharset() {
            throw new SettingValueNotSetException("URLEscapingCharset");
        }

        @Override
        protected TemplateClassResolver getDefaultNewBuiltinClassResolver() {
            throw new SettingValueNotSetException("newBuiltinClassResolver");
        }

        @Override
        protected boolean getDefaultAutoFlush() {
            throw new SettingValueNotSetException("autoFlush");
        }

        @Override
        protected boolean getDefaultShowErrorTips() {
            throw new SettingValueNotSetException("showErrorTips");
        }

        @Override
        protected boolean getDefaultAPIBuiltinEnabled() {
            throw new SettingValueNotSetException("APIBuiltinEnabled");
        }

        @Override
        protected boolean getDefaultLogTemplateExceptions() {
            throw new SettingValueNotSetException("logTemplateExceptions");
        }

        @Override
        protected boolean getDefaultLazyImports() {
            throw new SettingValueNotSetException("lazyImports");
        }

        @Override
        protected Boolean getDefaultLazyAutoImports() {
            throw new SettingValueNotSetException("lazyAutoImports");
        }

        @Override
        protected Map<String, String> getDefaultAutoImports() {
            throw new SettingValueNotSetException("autoImports");
        }

        @Override
        protected List<String> getDefaultAutoIncludes() {
            throw new SettingValueNotSetException("autoIncludes");
        }

        @Override
        protected Object getDefaultCustomAttribute(Object name) {
            return null;
        }

        @Override
        protected Map<Object, Object> getDefaultCustomAttributes() {
            throw new SettingValueNotSetException("customAttributes");
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
                setCustomDateFormats(mergeMaps(
                        isCustomDateFormatsSet() ? getCustomDateFormats() : null, tc.getCustomDateFormats(), false),
                        true
                );
            }
            if (tc.isCustomNumberFormatsSet()) {
                setCustomNumberFormats(mergeMaps(
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
            if (tc.isLogTemplateExceptionsSet()) {
                setLogTemplateExceptions(tc.getLogTemplateExceptions());
            }
            if (tc.isNamingConventionSet()) {
                setNamingConvention(tc.getNamingConvention());
            }
            if (tc.isNewBuiltinClassResolverSet()) {
                setNewBuiltinClassResolver(tc.getNewBuiltinClassResolver());
            }
            if (tc.isNumberFormatSet()) {
                setNumberFormat(tc.getNumberFormat());
            }
            if (tc.isObjectWrapperSet()) {
                setObjectWrapper(tc.getObjectWrapper());
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
            if (tc.isTemplateLanguageSet()) {
                setTemplateLanguage(tc.getTemplateLanguage());
            }
            if (tc.isTemplateExceptionHandlerSet()) {
                setTemplateExceptionHandler(tc.getTemplateExceptionHandler());
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
                setAutoImports(mergeMaps(
                        isAutoImportsSet() ? getAutoImports() : null,
                        tc.isAutoImportsSet() ? tc.getAutoImports() : null,
                        true));
            }
            if (tc.isAutoIncludesSet()) {
                setAutoIncludes(mergeLists(
                        isAutoIncludesSet() ? getAutoIncludes() : null,
                        tc.isAutoIncludesSet() ? tc.getAutoIncludes() : null));
            }

            if (tc.isCustomAttributesSet()) {
                setCustomAttributes(mergeMaps(
                        isCustomAttributesSet() ? getCustomAttributes() : null,
                        tc.isCustomAttributesSet() ? tc.getCustomAttributes() : null,
                        true),
                        true);
            }
        }

        @Override
        public Version getIncompatibleImprovements() {
            throw new SettingValueNotSetException("incompatibleImprovements");
        }

        @Override
        protected TagSyntax getDefaultTagSyntax() {
            throw new SettingValueNotSetException("tagSyntax");
        }

        @Override
        protected TemplateLanguage getDefaultTemplateLanguage() {
            throw new SettingValueNotSetException("templateLanguage");
        }

        @Override
        protected NamingConvention getDefaultNamingConvention() {
            throw new SettingValueNotSetException("namingConvention");
        }

        @Override
        protected boolean getDefaultWhitespaceStripping() {
            throw new SettingValueNotSetException("whitespaceStripping");
        }

        @Override
        protected AutoEscapingPolicy getDefaultAutoEscapingPolicy() {
            throw new SettingValueNotSetException("autoEscapingPolicy");
        }

        @Override
        protected OutputFormat getDefaultOutputFormat() {
            throw new SettingValueNotSetException("outputFormat");
        }

        @Override
        protected boolean getDefaultRecognizeStandardFileExtensions() {
            throw new SettingValueNotSetException("recognizeStandardFileExtensions");
        }

        @Override
        protected Charset getDefaultSourceEncoding() {
            throw new SettingValueNotSetException("sourceEncoding");
        }

        @Override
        protected int getDefaultTabSize() {
            throw new SettingValueNotSetException("tabSize");
        }

    }

}

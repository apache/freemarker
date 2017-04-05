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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

/**
 * Used for customizing the configuration settings for individual {@link Template}-s (or rather groups of templates),
 * relatively to the common setting values coming from the {@link Configuration}. This was designed with the standard
 * template loading mechanism of FreeMarker in mind ({@link Configuration#getTemplate(String)}
 * and {@link DefaultTemplateResolver}), though can also be reused for custom template loading and caching solutions.
 * 
 * <p>
 * Note on the {@code locale} setting: When used with the standard template loading/caching mechanism (
 * {@link Configuration#getTemplate(String)} and its overloads), localized lookup happens before the {@code locale}
 * specified here could have effect. The {@code locale} will be only set in the template that the localized lookup has
 * already found.
 * 
 * <p>
 * Note on the sourceEncoding setting {@code sourceEncoding}: See {@link Builder#setSourceEncoding(Charset)}.
 * 
 * <p>
 * Note that the result value of the reader methods (getter and "is" methods) is usually not useful unless the value of
 * that setting was already set on this object. Otherwise you will get the value from the parent {@link Configuration},
 * or an {@link IllegalStateException} before this object is associated to a {@link Configuration}.
 * 
 * <p>
 * If you are using this class for your own template loading and caching solution, rather than with the standard one,
 * you should be aware of a few more details:
 * 
 * <ul>
 * <li>This class implements both {@link MutableProcessingConfiguration} and {@link ParserConfiguration}. This means that it can influence
 * both the template parsing phase and the runtime settings. For both aspects (i.e., {@link ParserConfiguration} and
 * {@link MutableProcessingConfiguration}) to take effect, you have first pass this object to the {@link Template} constructor
 * (this is where the {@link ParserConfiguration} interface is used), and then you have to call {@link #apply(Template)}
 * on the resulting {@link Template} object (this is where the {@link MutableProcessingConfiguration} aspect is used).
 * 
 * <li>{@link #apply(Template)} only change the settings that weren't yet set on the {@link Template} (but are inherited
 * from the {@link Configuration}). This is primarily because if the template configures itself via the {@code #ftl}
 * header, those values should have precedence. A consequence of this is that if you want to configure the same
 * {@link Template} with multiple {@link TemplateConfiguration}-s, you either should merge them to a single one before
 * that (with {@link Builder#merge(ParserAndProcessingConfiguration)}), or you have to apply them in reverse order of
 * their intended precedence.
 * </ul>
 * 
 * @see Template#Template(String, String, Reader, Configuration, ParserConfiguration, Charset)
 * 
 * @since 2.3.24
 */
public final class TemplateConfiguration implements ParserAndProcessingConfiguration {
    private Configuration configuration;

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
    private final Integer tagSyntax;
    private final Integer namingConvention;
    private final Boolean whitespaceStripping;
    private final Integer autoEscapingPolicy;
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
     * Associates this instance with a {@link Configuration}; usually you don't call this, as it's called internally
     * when this instance is added to a {@link Configuration}. This method can be called only once (except with the same
     * {@link Configuration} parameter again, as that changes nothing anyway).
     *
     * @throws IllegalArgumentException
     *             if the argument is {@code null} or not a {@link Configuration}
     * @throws IllegalStateException
     *             if this object is already associated to a different {@link Configuration} object,
     *             or if the {@code Configuration} has {@code #getIncompatibleImprovements()} less than 2.3.22 and
     *             this object tries to change any non-parser settings
     */
    public void setParentConfiguration(Configuration configuration) {
        _NullArgumentException.check(configuration);
        synchronized (this) {
            if (this.configuration != null && this.configuration != configuration) {
                throw new IllegalStateException(
                        "This TemplateConfiguration was already associated to another Configuration");
            }
            this.configuration = configuration;
        }
    }

    /**
     * Returns the parent {@link Configuration}, or {@code null} if none was associated yet.
     */
    public Configuration getParentConfiguration() {
        return configuration;
    }

    private Configuration getNonNullParentConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("The TemplateConfiguration wasn't associated with a Configuration yet.");
        }
        return configuration;
    }

    /**
     * Sets those settings of the {@link Template} which aren't yet set in the {@link Template} and are set in this
     * {@link TemplateConfiguration}, leaves the other settings as is. A setting is said to be set in a
     * {@link TemplateConfiguration} or {@link Template} if it was explicitly set via a setter method on that object, as
     * opposed to be inherited from the {@link Configuration}.
     * 
     * <p>
     * Note that this method doesn't deal with settings that influence the parser, as those are already baked in at this
     * point via the {@link ParserConfiguration}. 
     * 
     * <p>
     * Note that the {@code sourceEncoding} setting of the {@link Template} counts as unset if it's {@code null},
     * even if {@code null} was set via {@link Template#setSourceEncoding(Charset)}.
     *
     * @throws IllegalStateException
     *             If the parent configuration wasn't yet set.
     */
    public void apply(Template template) {
        Configuration cfg = getNonNullParentConfiguration();
        if (template.getConfiguration() != cfg) {
            // This is actually not a problem right now, but for future BC we enforce this.
            throw new IllegalArgumentException(
                    "The argument Template doesn't belong to the same Configuration as the TemplateConfiguration");
        }

        if (isAPIBuiltinEnabledSet() && !template.isAPIBuiltinEnabledSet()) {
            template.setAPIBuiltinEnabled(getAPIBuiltinEnabled());
        }
        if (isArithmeticEngineSet() && !template.isArithmeticEngineSet()) {
            template.setArithmeticEngine(getArithmeticEngine());
        }
        if (isAutoFlushSet() && !template.isAutoFlushSet()) {
            template.setAutoFlush(getAutoFlush());
        }
        if (isBooleanFormatSet() && !template.isBooleanFormatSet()) {
            template.setBooleanFormat(getBooleanFormat());
        }
        if (isCustomDateFormatsSet()) {
            template.setCustomDateFormats(
                    mergeMaps(
                            getCustomDateFormats(),
                            template.isCustomDateFormatsSet() ? template.getCustomDateFormats() : null,
                            false));
        }
        if (isCustomNumberFormatsSet()) {
            template.setCustomNumberFormats(
                    mergeMaps(
                            getCustomNumberFormats(),
                            template.isCustomNumberFormatsSet() ? template.getCustomNumberFormats() : null,
                            false));
        }
        if (isDateFormatSet() && !template.isDateFormatSet()) {
            template.setDateFormat(getDateFormat());
        }
        if (isDateTimeFormatSet() && !template.isDateTimeFormatSet()) {
            template.setDateTimeFormat(getDateTimeFormat());
        }
        if (isSourceEncodingSet() && template.getSourceEncoding() == null) {
            template.setSourceEncoding(getSourceEncoding());
        }
        if (isLocaleSet() && !template.isLocaleSet()) {
            template.setLocale(getLocale());
        }
        if (isLogTemplateExceptionsSet() && !template.isLogTemplateExceptionsSet()) {
            template.setLogTemplateExceptions(getLogTemplateExceptions());
        }
        if (isNewBuiltinClassResolverSet() && !template.isNewBuiltinClassResolverSet()) {
            template.setNewBuiltinClassResolver(getNewBuiltinClassResolver());
        }
        if (isNumberFormatSet() && !template.isNumberFormatSet()) {
            template.setNumberFormat(getNumberFormat());
        }
        if (isObjectWrapperSet() && !template.isObjectWrapperSet()) {
            template.setObjectWrapper(getObjectWrapper());
        }
        if (isOutputEncodingSet() && !template.isOutputEncodingSet()) {
            template.setOutputEncoding(getOutputEncoding());
        }
        if (isShowErrorTipsSet() && !template.isShowErrorTipsSet()) {
            template.setShowErrorTips(getShowErrorTips());
        }
        if (isSQLDateAndTimeTimeZoneSet() && !template.isSQLDateAndTimeTimeZoneSet()) {
            template.setSQLDateAndTimeTimeZone(getSQLDateAndTimeTimeZone());
        }
        if (isTemplateExceptionHandlerSet() && !template.isTemplateExceptionHandlerSet()) {
            template.setTemplateExceptionHandler(getTemplateExceptionHandler());
        }
        if (isTimeFormatSet() && !template.isTimeFormatSet()) {
            template.setTimeFormat(getTimeFormat());
        }
        if (isTimeZoneSet() && !template.isTimeZoneSet()) {
            template.setTimeZone(getTimeZone());
        }
        if (isURLEscapingCharsetSet() && !template.isURLEscapingCharsetSet()) {
            template.setURLEscapingCharset(getURLEscapingCharset());
        }
        if (isLazyImportsSet() && !template.isLazyImportsSet()) {
            template.setLazyImports(getLazyImports());
        }
        if (isLazyAutoImportsSet() && !template.isLazyAutoImportsSet()) {
            template.setLazyAutoImports(getLazyAutoImports());
        }
        if (isAutoImportsSet()) {
            // Regarding the order of the maps in the merge:
            // - Existing template-level imports have precedence over those coming from the TC (just as with the others
            //   apply()-ed settings), thus for clashing import prefixes they must win.
            // - Template-level imports count as more specific, and so come after the more generic ones from TC.
            template.setAutoImports(mergeMaps(
                    getAutoImports(),
                    template.isAutoImportsSet() ? template.getAutoImports() : null,
                    true));
        }
        if (isAutoIncludesSet()) {
            template.setAutoIncludes(mergeLists(
                    getAutoIncludes(),
                    template.isAutoIncludesSet() ? template.getAutoIncludes() : null));
        }
        
        copyDirectCustomAttributes(template, false);

    }

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
        return mergedM;
    }

    private static List<String> mergeLists(List<String> list1, List<String> list2) {
        if (list1 == null) return list2;
        if (list2 == null) return list1;
        if (list1.isEmpty()) return list2;
        if (list2.isEmpty()) return list1;

        ArrayList<String> mergedList = new ArrayList<>(list1.size() + list2.size());
        mergedList.addAll(list1);
        mergedList.addAll(list2);
        return mergedList;
    }

    /**
     * For internal usage only, copies the custom attributes set directly on this objects into another
     * {@link MutableProcessingConfiguration}. The target {@link MutableProcessingConfiguration} is assumed to be not seen be other thread than the current
     * one yet. (That is, the operation is not synchronized on the target {@link MutableProcessingConfiguration}, only on the source
     * {@link MutableProcessingConfiguration})
     *
     * @since 2.3.24
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
    public int getTagSyntax() {
        return tagSyntax != null ? tagSyntax : getNonNullParentConfiguration().getTagSyntax();
    }

    @Override
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return templateLanguage != null ? templateLanguage : getNonNullParentConfiguration().getTemplateLanguage();
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return templateLanguage != null;
    }

    @Override
    public int getNamingConvention() {
        return namingConvention != null ? namingConvention
                : getNonNullParentConfiguration().getNamingConvention();
    }

    @Override
    public boolean isNamingConventionSet() {
        return namingConvention != null;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return whitespaceStripping != null ? whitespaceStripping
                : getNonNullParentConfiguration().getWhitespaceStripping();
    }

    @Override
    public boolean isWhitespaceStrippingSet() {
        return whitespaceStripping != null;
    }

    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy
                : getNonNullParentConfiguration().getAutoEscapingPolicy();
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : getNonNullParentConfiguration().getOutputFormat();
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return isArithmeticEngineSet() ? arithmeticEngine : getNonNullParentConfiguration().getArithmeticEngine();
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
        return isRecognizeStandardFileExtensionsSet() ? recognizeStandardFileExtensions
                : getNonNullParentConfiguration().getRecognizeStandardFileExtensions();
    }
    
    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return recognizeStandardFileExtensions != null;
    }

    @Override
    public Charset getSourceEncoding() {
        return isSourceEncodingSet() ? sourceEncoding : getNonNullParentConfiguration().getSourceEncoding();
    }

    @Override
    public boolean isSourceEncodingSet() {
        return sourceEncoding != null;
    }
    
    @Override
    public int getTabSize() {
        return isTabSizeSet() ? tabSize
                : getNonNullParentConfiguration().getTabSize();
    }
    
    @Override
    public boolean isTabSizeSet() {
        return tabSize != null;
    }
    
    /**
     * Returns {@link Configuration#getIncompatibleImprovements()} from the parent {@link Configuration}. This mostly
     * just exist to satisfy the {@link ParserConfiguration} interface.
     * 
     * @throws IllegalStateException
     *             If the parent configuration wasn't yet set.
     */
    @Override
    public Version getIncompatibleImprovements() {
        return getNonNullParentConfiguration().getIncompatibleImprovements();
    }

    @Override
    public Locale getLocale() {
        return isLocaleSet() ? locale : getNonNullParentConfiguration().getLocale();
    }

    @Override
    public boolean isLocaleSet() {
        return locale != null;
    }

    @Override
    public TimeZone getTimeZone() {
        return isTimeZoneSet() ? timeZone : getNonNullParentConfiguration().getTimeZone();
    }

    @Override
    public boolean isTimeZoneSet() {
        return timeZone != null;
    }

    @Override
    public TimeZone getSQLDateAndTimeTimeZone() {
        return isSQLDateAndTimeTimeZoneSet() ? sqlDateAndTimeTimeZone : getNonNullParentConfiguration()
                .getSQLDateAndTimeTimeZone();
    }

    @Override
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return sqlDateAndTimeTimeZoneSet;
    }

    @Override
    public String getNumberFormat() {
        return isNumberFormatSet() ? numberFormat : getNonNullParentConfiguration().getNumberFormat();
    }

    @Override
    public boolean isNumberFormatSet() {
        return numberFormat != null;
    }

    @Override
    public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
        return isCustomNumberFormatsSet() ? customNumberFormats : getNonNullParentConfiguration().getCustomNumberFormats();
    }

    @Override
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        if (isCustomNumberFormatsSet()) {
            TemplateNumberFormatFactory format = customNumberFormats.get(name);
            if (format != null) {
                return  format;
            }
        }
        return getNonNullParentConfiguration().getCustomNumberFormat(name);
    }

    @Override
    public boolean isCustomNumberFormatsSet() {
        return customNumberFormats != null;
    }

    @Override
    public boolean hasCustomFormats() {
        return isCustomNumberFormatsSet() ? !customNumberFormats.isEmpty()
                : getNonNullParentConfiguration().hasCustomFormats();
    }

    @Override
    public String getBooleanFormat() {
        return isBooleanFormatSet() ? booleanFormat : getNonNullParentConfiguration().getBooleanFormat();
    }

    @Override
    public boolean isBooleanFormatSet() {
        return booleanFormat != null;
    }

    @Override
    public String getTimeFormat() {
        return isTimeFormatSet() ? timeFormat : getNonNullParentConfiguration().getTimeFormat();
    }

    @Override
    public boolean isTimeFormatSet() {
        return timeFormat != null;
    }

    @Override
    public String getDateFormat() {
        return isDateFormatSet() ? dateFormat : getNonNullParentConfiguration().getDateFormat();
    }

    @Override
    public boolean isDateFormatSet() {
        return dateFormat != null;
    }

    @Override
    public String getDateTimeFormat() {
        return isDateTimeFormatSet() ? dateTimeFormat : getNonNullParentConfiguration().getDateTimeFormat();
    }

    @Override
    public boolean isDateTimeFormatSet() {
        return dateTimeFormat != null;
    }

    @Override
    public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
        return isCustomDateFormatsSet() ? customDateFormats : getNonNullParentConfiguration().getCustomDateFormats();
    }

    @Override
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        if (isCustomDateFormatsSet()) {
            TemplateDateFormatFactory format = customDateFormats.get(name);
            if (format != null) {
                return  format;
            }
        }
        return getNonNullParentConfiguration().getCustomDateFormat(name);
    }

    @Override
    public boolean isCustomDateFormatsSet() {
        return customDateFormats != null;
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return isTemplateExceptionHandlerSet() ? templateExceptionHandler : getNonNullParentConfiguration().getTemplateExceptionHandler();
    }

    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return templateExceptionHandler != null;
    }

    @Override
    public ObjectWrapper getObjectWrapper() {
        return isObjectWrapperSet() ? objectWrapper : getNonNullParentConfiguration().getObjectWrapper();
    }

    @Override
    public boolean isObjectWrapperSet() {
        return objectWrapper != null;
    }

    @Override
    public Charset getOutputEncoding() {
        return isOutputEncodingSet() ? outputEncoding : getNonNullParentConfiguration().getOutputEncoding();
    }

    @Override
    public boolean isOutputEncodingSet() {
        return outputEncodingSet;
    }

    @Override
    public Charset getURLEscapingCharset() {
        return isURLEscapingCharsetSet() ? urlEscapingCharset : getNonNullParentConfiguration().getURLEscapingCharset();
    }

    @Override
    public boolean isURLEscapingCharsetSet() {
        return urlEscapingCharsetSet;
    }

    @Override
    public TemplateClassResolver getNewBuiltinClassResolver() {
        return isNewBuiltinClassResolverSet() ? newBuiltinClassResolver : getNonNullParentConfiguration().getNewBuiltinClassResolver();
    }

    @Override
    public boolean isNewBuiltinClassResolverSet() {
        return newBuiltinClassResolver != null;
    }

    @Override
    public boolean getAPIBuiltinEnabled() {
        return isAPIBuiltinEnabledSet() ? apiBuiltinEnabled : getNonNullParentConfiguration().getAPIBuiltinEnabled();
    }

    @Override
    public boolean isAPIBuiltinEnabledSet() {
        return apiBuiltinEnabled != null;
    }

    @Override
    public boolean getAutoFlush() {
        return isAutoFlushSet() ? autoFlush : getNonNullParentConfiguration().getAutoFlush();
    }

    @Override
    public boolean isAutoFlushSet() {
        return autoFlush != null;
    }

    @Override
    public boolean getShowErrorTips() {
        return isShowErrorTipsSet() ? showErrorTips : getNonNullParentConfiguration().getShowErrorTips();
    }

    @Override
    public boolean isShowErrorTipsSet() {
        return showErrorTips != null;
    }

    @Override
    public boolean getLogTemplateExceptions() {
        return isLogTemplateExceptionsSet() ? logTemplateExceptions : getNonNullParentConfiguration().getLogTemplateExceptions();
    }

    @Override
    public boolean isLogTemplateExceptionsSet() {
        return logTemplateExceptions != null;
    }

    @Override
    public boolean getLazyImports() {
        return isLazyImportsSet() ? lazyImports : getNonNullParentConfiguration().getLazyImports();
    }

    @Override
    public boolean isLazyImportsSet() {
        return lazyImports != null;
    }

    @Override
    public Boolean getLazyAutoImports() {
        return isLazyAutoImportsSet() ? lazyAutoImports : getNonNullParentConfiguration().getLazyAutoImports();
    }

    @Override
    public boolean isLazyAutoImportsSet() {
        return lazyAutoImportsSet;
    }

    @Override
    public Map<String, String> getAutoImports() {
        return isAutoImportsSet() ? autoImports : getNonNullParentConfiguration().getAutoImports();
    }

    @Override
    public boolean isAutoImportsSet() {
        return autoImports != null;
    }

    @Override
    public List<String> getAutoIncludes() {
        return isAutoIncludesSet() ? autoIncludes : getNonNullParentConfiguration().getAutoIncludes();
    }

    @Override
    public boolean isAutoIncludesSet() {
        return autoIncludes != null;
    }

    @Override
    public Map<Object, Object> getCustomAttributes() {
        return isCustomAttributesSet() ? customAttributes : getNonNullParentConfiguration().getCustomAttributes();
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
        return getNonNullParentConfiguration().getCustomAttribute(name);
    }

    public static final class Builder extends MutableProcessingAndParseConfiguration<Builder>
            implements CommonBuilder<TemplateConfiguration> {

        public Builder() {
            super((Configuration) null);
        }

        @Override
        public TemplateConfiguration build() {
            return new TemplateConfiguration(this);
        }

        // TODO This will be removed
        @Override
        void setParent(ProcessingConfiguration cfg) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Locale getInheritedLocale() {
            throw new SettingValueNotSetException("locale");
        }

        @Override
        protected TimeZone getInheritedTimeZone() {
            throw new SettingValueNotSetException("timeZone");
        }

        @Override
        protected TimeZone getInheritedSQLDateAndTimeTimeZone() {
            throw new SettingValueNotSetException("SQLDateAndTimeTimeZone");
        }

        @Override
        protected String getInheritedNumberFormat() {
            throw new SettingValueNotSetException("numberFormat");
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getInheritedCustomNumberFormats() {
            throw new SettingValueNotSetException("customNumberFormats");
        }

        @Override
        protected TemplateNumberFormatFactory getInheritedCustomNumberFormat(String name) {
            return null;
        }

        @Override
        protected boolean getInheritedHasCustomFormats() {
            return false;
        }

        @Override
        protected String getInheritedBooleanFormat() {
            throw new SettingValueNotSetException("booleanFormat");
        }

        @Override
        protected String getInheritedTimeFormat() {
            throw new SettingValueNotSetException("timeFormat");
        }

        @Override
        protected String getInheritedDateFormat() {
            throw new SettingValueNotSetException("dateFormat");
        }

        @Override
        protected String getInheritedDateTimeFormat() {
            throw new SettingValueNotSetException("dateTimeFormat");
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getInheritedCustomDateFormats() {
            throw new SettingValueNotSetException("customDateFormats");
        }

        @Override
        protected TemplateDateFormatFactory getInheritedCustomDateFormat(String name) {
            throw new SettingValueNotSetException("customDateFormat");
        }

        @Override
        protected TemplateExceptionHandler getInheritedTemplateExceptionHandler() {
            throw new SettingValueNotSetException("templateExceptionHandler");
        }

        @Override
        protected ArithmeticEngine getInheritedArithmeticEngine() {
            throw new SettingValueNotSetException("arithmeticEngine");
        }

        @Override
        protected ObjectWrapper getInheritedObjectWrapper() {
            throw new SettingValueNotSetException("objectWrapper");
        }

        @Override
        protected Charset getInheritedOutputEncoding() {
            throw new SettingValueNotSetException("outputEncoding");
        }

        @Override
        protected Charset getInheritedURLEscapingCharset() {
            throw new SettingValueNotSetException("URLEscapingCharset");
        }

        @Override
        protected TemplateClassResolver getInheritedNewBuiltinClassResolver() {
            throw new SettingValueNotSetException("newBuiltinClassResolver");
        }

        @Override
        protected boolean getInheritedAutoFlush() {
            throw new SettingValueNotSetException("autoFlush");
        }

        @Override
        protected boolean getInheritedShowErrorTips() {
            throw new SettingValueNotSetException("showErrorTips");
        }

        @Override
        protected boolean getInheritedAPIBuiltinEnabled() {
            throw new SettingValueNotSetException("APIBuiltinEnabled");
        }

        @Override
        protected boolean getInheritedLogTemplateExceptions() {
            throw new SettingValueNotSetException("logTemplateExceptions");
        }

        @Override
        protected boolean getInheritedLazyImports() {
            throw new SettingValueNotSetException("lazyImports");
        }

        @Override
        protected Boolean getInheritedLazyAutoImports() {
            throw new SettingValueNotSetException("lazyAutoImports");
        }

        @Override
        protected Map<String, String> getInheritedAutoImports() {
            throw new SettingValueNotSetException("autoImports");
        }

        @Override
        protected List<String> getInheritedAutoIncludes() {
            throw new SettingValueNotSetException("autoIncludes");
        }

        @Override
        protected Object getInheritedCustomAttribute(Object name) {
            return null;
        }

        /**
         * Set all settings in this {@link Builder} that were set in the parameter
         * {@link TemplateConfiguration}, possibly overwriting the earlier value in this object. (A setting is said to be
         * set in a {@link TemplateConfiguration} if it was explicitly set via a setter method, as opposed to be inherited.)
         */
        public void merge(ParserAndProcessingConfiguration tc) {
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
                        isCustomDateFormatsSet() ? getCustomDateFormats() : null, tc.getCustomDateFormats(), false));
            }
            if (tc.isCustomNumberFormatsSet()) {
                setCustomNumberFormats(mergeMaps(
                        isCustomNumberFormatsSet() ? getCustomNumberFormats() : null, tc.getCustomNumberFormats(), false));
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
                        true));
            }
        }

        @Override
        public Version getIncompatibleImprovements() {
            throw new SettingValueNotSetException("incompatibleImprovements");
        }

        @Override
        protected int getInheritedTagSyntax() {
            throw new SettingValueNotSetException("tagSyntax");
        }

        @Override
        protected TemplateLanguage getInheritedTemplateLanguage() {
            throw new SettingValueNotSetException("templateLanguage");
        }

        @Override
        protected int getInheritedNamingConvention() {
            throw new SettingValueNotSetException("namingConvention");
        }

        @Override
        protected boolean getInheritedWhitespaceStripping() {
            throw new SettingValueNotSetException("whitespaceStripping");
        }

        @Override
        protected int getInheritedAutoEscapingPolicy() {
            throw new SettingValueNotSetException("autoEscapingPolicy");
        }

        @Override
        protected OutputFormat getInheritedOutputFormat() {
            throw new SettingValueNotSetException("outputFormat");
        }

        @Override
        protected boolean getInheritedRecognizeStandardFileExtensions() {
            throw new SettingValueNotSetException("recognizeStandardFileExtensions");
        }

        @Override
        protected Charset getInheritedSourceEncoding() {
            throw new SettingValueNotSetException("sourceEncoding");
        }

        @Override
        protected int getInheritedTabSize() {
            throw new SettingValueNotSetException("tabSize");
        }

    }

}

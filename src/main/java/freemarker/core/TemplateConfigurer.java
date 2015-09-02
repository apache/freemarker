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
package freemarker.core;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import freemarker.cache.TemplateCache;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.NullArgumentException;

/**
 * Used for customizing the configuration settings for individual {@link Template}-s, relatively to the common setting
 * values coming from the {@link Configuration}. This was designed with the standard template loading mechanism of
 * FreeMarker in mind ({@link Configuration#getTemplate(String)} and {@link TemplateCache}), though can also be reused
 * for custom template loading and caching solutions.
 * 
 * <p>
 * Note on the {@code locale} setting: When used with the standard template loading/caching mechanism
 * ({@link Configuration#getTemplate(String)} and its overloads), localized lookup happens before the {@code locale}
 * specified here could have effect. The {@code locale} will be only set in the template that the localized looks has
 * already found.
 * 
 * <p>
 * Note on encoding setting {@code encoding}: See {@link #setEncoding(String)}.
 * 
 * <p>
 * Note that the result value of the reader methods (getter and "is" methods) is usually not useful unless the value of
 * that setting was already set on this object. Otherwise you will get the value from the parent {@link Configuration},
 * which is {@link Configuration#getDefaultConfiguration()} before this object is associated to a {@link Configuration}.
 * 
 * <p>
 * If you are using this class for your own template loading and caching solution, rather than with the standard one,
 * you should be aware of a few more details:
 * 
 * <ul>
 * <li>This class implements both {@link Configurable} and
 * {@link ParserConfiguration}. This means that it can influence both the template parsing phase and the runtime
 * settings. For both aspects (i.e., {@link Configurable} and {@link ParserConfiguration}) to take effect, you have
 * first pass this object to the {@link Template} constructor (this is where the {@link ParserConfiguration} interface
 * is used), and then you have to call {@link #configure(Template)} on the resulting {@link Template} object (this is
 * where the {@link Configurable} aspect is used).
 * 
 * <li>{@link #configure(Template)} only change the settings that weren't yet set on the {@link Template} (but are
 * inherited from the {@link Configuration}). This is primarily because if the template configures itself via the
 * {@code #ftl} header, those values should have precedence. A consequence of this is that if you want to configure
 * the same {@link Template} with multiple {@link TemplateConfigurer}-s, you either should merge them to a single one
 * before that (with {@link #merge(TemplateConfigurer)}), or you have to apply them in reverse order of their intended
 * precedence. 
 * </ul>
 * 
 * @see Template#Template(String, String, Reader, Configuration, ParserConfiguration, String)
 * 
 * @since 2.3.24
 */
public final class TemplateConfigurer extends Configurable implements ParserConfiguration {

    private boolean parentConfigurationSet;
    private Integer tagSyntax;
    private Integer namingConvention;
    private Boolean whitespaceStripping;
    private Boolean strictSyntaxMode;
    private Integer autoEscapingPolicy;
    private Boolean recognizeStandardFileExtensions;
    private OutputFormat outputFormat;
    private String encoding;

    /**
     * Creates a new instance. The parent will be {@link Configuration#getDefaultConfiguration()} initially, but it will
     * be changed to the real parent {@link Configuration} when this object is added to the {@link Configuration}. (It's
     * not allowed to add the same instance to multiple {@link Configuration}-s).
     */
    public TemplateConfigurer() {
        super(Configuration.getDefaultConfiguration());
    }

    /**
     * Same as {@link #setParentConfiguration(Configuration)}.
     * 
     * @throws IllegalArgumentException
     *             if the argument is {@code null} or not a {@link Configuration}.
     */
    @Override
    void setParent(Configurable cfg) {
        NullArgumentException.check("cfg", cfg);
        if (!(cfg instanceof Configuration)) {
            throw new IllegalArgumentException("The parent of a TemplateConfigurer can only be a Configuration");
        }
        
        if (parentConfigurationSet) {
            if (getParent() != cfg) {
                throw new IllegalStateException(
                        "This TemplateConfigurer is already associated with a different Configuration instance.");
            }
            return;
        }
        
        if (((Configuration) cfg).getIncompatibleImprovements().intValue() < _TemplateAPI.VERSION_INT_2_3_22
                && hasAnyConfigurableSet()) {
            throw new IllegalStateException(
                    "This TemplateConfigurer can't be associated to a Configuration that has incompatibleImprovements "
                    + "less than 2.3.22, because it changes non-parser settings.");
        }
        
        super.setParent(cfg);
        parentConfigurationSet = true;
    }

    /**
     * Associates this instance with a {@link Configuration}; usually you don't call this, as it's called internally
     * when this instance is added to a {@link Configuration}. This method can be called only once (except with the same
     * {@link Configuration} parameter again, as that changes nothing anyway).
     * 
     * @throws IllegalStateException
     *             If the parent configuration was already set to a different {@link Configuration} instance.
     * @throws IllegalArgumentException
     *             if the argument is {@code null}.
     */
    public void setParentConfiguration(Configuration cfg) {
        setParent(cfg);
    }

    /**
     * Returns the parent {@link Configuration}, or {@code null} if none was associated yet.
     */
    public Configuration getParentConfiguration() {
        return parentConfigurationSet ? (Configuration) getParent() : null;
    }
    
    /**
     * Set all settings in this {@link TemplateConfigurer} that that were set in the parameter
     * {@link TemplateConfigurer}, possibly overwriting the earlier value in this object. (A setting is said to be set
     * in a {@link TemplateConfigurer} if it was explicitly set via a setter method, as opposed to be inherited.)
     */
    public void merge(TemplateConfigurer tc) {
        if (tc.isAPIBuiltinEnabledSet()) {
            setAPIBuiltinEnabled(tc.isAPIBuiltinEnabled());
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
        if (tc.isClassicCompatibleSet()) {
            setClassicCompatibleAsInt(tc.getClassicCompatibleAsInt());
        }
        if (tc.isCustomDateFormatsSet()) {
            setCustomDateFormats(mergeMaps(getCustomDateFormats(), tc.getCustomDateFormats()));
        }
        if (tc.isCustomNumberFormatsSet()) {
            setCustomNumberFormats(mergeMaps(getCustomNumberFormats(), tc.getCustomNumberFormats()));
        }
        if (tc.isDateFormatSet()) {
            setDateFormat(tc.getDateFormat());
        }
        if (tc.isDateTimeFormatSet()) {
            setDateTimeFormat(tc.getDateTimeFormat());
        }
        if (tc.isEncodingSet()) {
            setEncoding(tc.getEncoding());
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
        if (tc.isStrictSyntaxModeSet()) {
            setStrictSyntaxMode(tc.getStrictSyntaxMode());
        }
        if (tc.isTagSyntaxSet()) {
            setTagSyntax(tc.getTagSyntax());
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
        
        tc.copyDirectCustomAttributes(this, true);
    }

    /**
     * Sets the settings of the {@link Template} which are not yet set in the {@link Template} and are set in this
     * {@link TemplateConfigurer}, leaves the other settings as is. A setting is said to be set in a
     * {@link TemplateConfigurer} or {@link Template} if it was explicitly set via a setter method on that object, as
     * opposed to be inherited from the {@link Configuration}.
     * 
     * <p>
     * Note that the {@code encoding} setting of the {@link Template} counts as unset if it's {@code null},
     * even if {@code null} was set via {@link Template#setEncoding(String)}.
     *
     * @throws IllegalStateException
     *             If the parent configuration wasn't yet set.
     */
    public void configure(Template template) {
        checkParentConfigurationSet();
        Configuration cfg = getParentConfiguration();
        if (template.getConfiguration() != cfg) {
            // This is actually not a problem right now, but for future BC we enforce this.
            throw new IllegalArgumentException(
                    "The argument Template doesn't belong to the same Configuration as the TemplateConfigurer");
        }

        if (isAPIBuiltinEnabledSet() && !template.isAPIBuiltinEnabledSet()) {
            template.setAPIBuiltinEnabled(isAPIBuiltinEnabled());
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
        if (isClassicCompatibleSet() && !template.isClassicCompatibleSet()) {
            template.setClassicCompatibleAsInt(getClassicCompatibleAsInt());
        }
        if (isCustomDateFormatsSet() && !template.isCustomDateFormatsSet()) {
            template.setCustomDateFormats(getCustomDateFormats());
        }
        if (isCustomNumberFormatsSet() && !template.isCustomNumberFormatsSet()) {
            template.setCustomNumberFormats(getCustomNumberFormats());
        }
        if (isDateFormatSet() && !template.isDateFormatSet()) {
            template.setDateFormat(getDateFormat());
        }
        if (isDateTimeFormatSet() && !template.isDateTimeFormatSet()) {
            template.setDateTimeFormat(getDateTimeFormat());
        }
        if (isEncodingSet() && template.getEncoding() == null) {
            template.setEncoding(getEncoding());
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
        
        copyDirectCustomAttributes(template, false);
    }

    /**
     * See {@link Configuration#setTagSyntax(int)}.
     */
    public void setTagSyntax(int tagSyntax) {
        _TemplateAPI.valideTagSyntaxValue(tagSyntax);
        this.tagSyntax = Integer.valueOf(tagSyntax);
    }

    /**
     * The getter pair of {@link #setTagSyntax(int)}.
     */
    public int getTagSyntax() {
        return tagSyntax != null ? tagSyntax.intValue() : getParentConfiguration().getTagSyntax();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    /**
     * See {@link Configuration#setNamingConvention(int)}.
     */
    public void setNamingConvention(int namingConvention) {
        _TemplateAPI.validateNamingConventionValue(namingConvention);
        this.namingConvention = Integer.valueOf(namingConvention);
    }

    /**
     * The getter pair of {@link #setNamingConvention(int)}.
     */
    public int getNamingConvention() {
        return namingConvention != null ? namingConvention.intValue() : getParentConfiguration().getNamingConvention();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isNamingConventionSet() {
        return namingConvention != null;
    }

    /**
     * See {@link Configuration#setWhitespaceStripping(boolean)}.
     */
    public void setWhitespaceStripping(boolean whitespaceStripping) {
        this.whitespaceStripping = Boolean.valueOf(whitespaceStripping);
    }

    /**
     * The getter pair of {@link #getWhitespaceStripping()}.
     */
    public boolean getWhitespaceStripping() {
        return whitespaceStripping != null ? whitespaceStripping.booleanValue()
                : getParentConfiguration().getWhitespaceStripping();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isWhitespaceStrippingSet() {
        return whitespaceStripping != null;
    }

    /**
     * Sets the output format of the template; see {@link Configuration#setAutoEscapingPolicy(int)} for more.
     */
    public void setAutoEscapingPolicy(int autoEscapingPolicy) {
        _TemplateAPI.validateAutoEscapingPolicyValue(autoEscapingPolicy);
        this.autoEscapingPolicy = Integer.valueOf(autoEscapingPolicy);
    }

    /**
     * The getter pair of {@link #setAutoEscapingPolicy(int)}.
     */
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy.intValue()
                : getParentConfiguration().getAutoEscapingPolicy();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    /**
     * Sets the output format of the template; see {@link Configuration#setOutputFormat(OutputFormat)} for more.
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        NullArgumentException.check("outputFormat", outputFormat);
        this.outputFormat = outputFormat;
    }

    /**
     * The getter pair of {@link #setOutputFormat(OutputFormat)}.
     */
    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : getParentConfiguration().getOutputFormat();
    }

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isOutputFormatSet() {
        return outputFormat != null;
    }
    
    /**
     * See {@link Configuration#setRecognizeStandardFileExtensions(boolean)}. 
     */
    public void setRecognizeStandardFileExtensions(boolean recognizeStandardFileExtensions) {
        this.recognizeStandardFileExtensions = Boolean.valueOf(recognizeStandardFileExtensions);
    }

    /**
     * Getter pair of {@link #setRecognizeStandardFileExtensions(boolean)}.
     */
    public boolean getRecognizeStandardFileExtensions() {
        return recognizeStandardFileExtensions != null ? recognizeStandardFileExtensions.booleanValue()
                : getParentConfiguration().getRecognizeStandardFileExtensions();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isRecognizeStandardFileExtensionsSet() {
        return recognizeStandardFileExtensions != null;
    }
    
    /**
     * See {@link Configuration#setStrictSyntaxMode(boolean)}.
     */
    public void setStrictSyntaxMode(boolean strictSyntaxMode) {
        this.strictSyntaxMode = Boolean.valueOf(strictSyntaxMode);
    }

    /**
     * The getter pair of {@link #setStrictSyntaxMode(boolean)}.
     */
    public boolean getStrictSyntaxMode() {
        return strictSyntaxMode != null ? strictSyntaxMode.booleanValue()
                : getParentConfiguration().getStrictSyntaxMode();
    }
    
    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    public boolean isStrictSyntaxModeSet() {
        return strictSyntaxMode != null;
    }

    @Override
    public void setStrictBeanModels(boolean strict) {
        throw new UnsupportedOperationException(
                "Setting strictBeanModels on " + TemplateConfigurer.class.getSimpleName() + " level isn't supported.");
    }

    public String getEncoding() {
        return encoding != null ? encoding : getParentConfiguration().getDefaultEncoding();
    }

    /**
     * When the standard template loading/caching mechanism is used, this forces the charset used for reading the
     * template "file", overriding everything but the encoding coming from the {@code #ftl} header. This setting
     * overrides the locale-specific encodings set via {@link Configuration#setEncoding(java.util.Locale, String)}. It
     * also overrides the {@code encoding} parameter of {@link Configuration#getTemplate(String, String)} (and of its
     * overloads) and the {@code encoding} parameter of the {@code #include} directive. This works like that because
     * specifying the encoding where you are requesting the template is error prone and deprecated.
     * 
     * <p>
     * If you are developing your own template loading/caching mechanism instead of the standard one, note that the
     * above behavior is not guaranteed by this class alone; you have to ensure it. Also, read the note on
     * {@code encoding} in the documentation of {@link #configure(Template)}.
     */
    public void setEncoding(String encoding) {
        NullArgumentException.check("encoding", encoding);
        this.encoding = encoding;
    }

    public boolean isEncodingSet() {
        return encoding != null;
    }
    
    /**
     * Returns {@link Configuration#getIncompatibleImprovements()} from the parent {@link Configuration}. This mostly
     * just exist to satisfy the {@link ParserConfiguration} interface.
     * 
     * @throws IllegalStateException
     *             If the parent configuration wasn't yet set.
     */
    public Version getIncompatibleImprovements() {
        checkParentConfigurationSet();
        return getParentConfiguration().getIncompatibleImprovements();
    }

    private void checkParentConfigurationSet() {
        if (!parentConfigurationSet) {
            throw new IllegalStateException("The TemplateConfigurer wasn't associated with a Configuration yet.");
        }
    }

    private boolean hasAnyConfigurableSet() {
        return
                isAPIBuiltinEnabledSet()
                || isArithmeticEngineSet()
                || isAutoFlushSet()
                || isBooleanFormatSet()
                || isClassicCompatibleSet()
                || isCustomDateFormatsSet()
                || isCustomNumberFormatsSet()
                || isDateFormatSet()
                || isDateTimeFormatSet()
                || isLocaleSet()
                || isLogTemplateExceptionsSet()
                || isNewBuiltinClassResolverSet()
                || isNumberFormatSet()
                || isObjectWrapperSet()
                || isOutputEncodingSet()
                || isShowErrorTipsSet()
                || isSQLDateAndTimeTimeZoneSet()
                || isTemplateExceptionHandlerSet()
                || isTimeFormatSet()
                || isTimeZoneSet()
                || isURLEscapingCharsetSet();
    }
    
    private Map mergeMaps(Map m1, Map m2) {
        if (m1 == null) return m2;
        if (m2 == null) return m1;
        if (m1.isEmpty()) return m2;
        if (m2.isEmpty()) return m1;
        
        LinkedHashMap mergedM = new LinkedHashMap(m1);
        mergedM.putAll(m2);
        return mergedM;
    }
    
}

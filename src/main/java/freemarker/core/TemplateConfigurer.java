package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.NullArgumentException;

/**
 * Used with the standard template loader mechanism to customize the configuration settings of the individual
 * {@link Template}-s, relatively to the setting values coming from the {@link Configuration}.
 * 
 * <p>
 * Note that the result value of the reader methods (getter and "is" methods) is usually not useful unless the value of
 * that setting was already set on this object. Otherwise you will get the value from the parent {@link Configuration},
 * which is {@link Configuration#getDefaultConfiguration()} before this object is added to a {@link Configuration}.
 * 
 * @since 2.3.24
 */
public final class TemplateConfigurer extends Configurable implements ParserConfiguration {

    private boolean parentConfigurationSet;
    private Integer tagSyntax;
    private Integer namingConvention;
    private Boolean whitespaceStripping;
    private String encoding;

    /**
     * Creates a new instance. The parent will be {@link Configuration#getDefaultConfiguration()} initially, but it will
     * be changed to the real parent {@link Configuration} when this object is added to the {@link Configuration}. (It's
     * not allowed to add tge same instance to multiple {@link Configuration}-s).
     */
    public TemplateConfigurer() {
        super(Configuration.getDefaultConfiguration());
    }

    /**
     * Associates this instance with a {@link Configuration}; usually you don't call this, as it's called internally
     * when this instance is added to a {@link Configuration}. This method can be called only once (except with the same
     * {@link Configuration} parameter again, as that changes nothing anyway).
     * 
     * @throws IllegalStateException
     *             If the parent configuration was already set to a different {@link Configuration} instance.
     */
    public void setParentConfiguration(Configuration cfg) {
        NullArgumentException.check("cfg", cfg);
        if (parentConfigurationSet && getParent() != cfg) {
            throw new IllegalStateException(
                    "This TemplateConfigurer is already associated with a different Configuration instance.");
        }
        setParent(cfg);
        parentConfigurationSet = true;
    }

    /**
     * Creates a {@link TemplateConfigurer} that contains an union of the setting that were set in the parameter
     * {@link TemplateConfigurer}-s. If a setting is set in multiple parameter {@link TemplateConfigurer}-s, the one
     * that occurs later in the parameter list wins. If a setting is not set in any of the {@link TemplateConfigurer}
     * -s, it will remain unset in the result. The result won't have {@link #setParentConfiguration(Configuration)}
     * called yet.
     */
    public static TemplateConfigurer merge(TemplateConfigurer... templateConfigurers) {
        TemplateConfigurer mergedTC = new TemplateConfigurer();
        for (int i = 0; i < templateConfigurers.length; i++) {
            TemplateConfigurer tc = templateConfigurers[i];

            if (tc.isAPIBuiltinEnabledSet()) {
                mergedTC.setAPIBuiltinEnabled(tc.isAPIBuiltinEnabled());
            }
            if (tc.isSQLDateAndTimeTimeZoneSet()) {
                mergedTC.setSQLDateAndTimeTimeZone(tc.getSQLDateAndTimeTimeZone());
            }
            if (tc.isURLEscapingCharsetSet()) {
                mergedTC.setURLEscapingCharset(tc.getURLEscapingCharset());
            }
            if (tc.isArithmeticEngineSet()) {
                mergedTC.setArithmeticEngine(tc.getArithmeticEngine());
            }
            if (tc.isAutoFlushSet()) {
                mergedTC.setAutoFlush(tc.getAutoFlush());
            }
            if (tc.isBooleanFormatSet()) {
                mergedTC.setBooleanFormat(tc.getBooleanFormat());
            }
            if (tc.isClassicCompatibleSet()) {
                mergedTC.setClassicCompatibleAsInt(tc.getClassicCompatibleAsInt());
            }
            if (tc.isDateFormatSet()) {
                mergedTC.setDateFormat(tc.getDateFormat());
            }
            if (tc.isDateTimeFormatSet()) {
                mergedTC.setDateTimeFormat(tc.getDateTimeFormat());
            }
            if (tc.isEncodingSet()) {
                mergedTC.setEncoding(tc.getEncoding());
            }
            if (tc.isLocaleSet()) {
                mergedTC.setLocale(tc.getLocale());
            }
            if (tc.isLogTemplateExceptionsSet()) {
                mergedTC.setLogTemplateExceptions(tc.getLogTemplateExceptions());
            }
            if (tc.isNamingConventionSet()) {
                mergedTC.setNamingConvention(tc.getNamingConvention());
            }
            if (tc.isNewBuiltinClassResolverSet()) {
                mergedTC.setNewBuiltinClassResolver(tc.getNewBuiltinClassResolver());
            }
            if (tc.isNumberFormatSet()) {
                mergedTC.setNumberFormat(tc.getNumberFormat());
            }
            if (tc.isObjectWrapperSet()) {
                mergedTC.setObjectWrapper(tc.getObjectWrapper());
            }
            if (tc.isOutputEncodingSet()) {
                mergedTC.setOutputEncoding(tc.getOutputEncoding());
            }
            if (tc.isShowErrorTipsSet()) {
                mergedTC.setShowErrorTips(tc.getShowErrorTips());
            }
            if (tc.isTagSyntaxSet()) {
                mergedTC.setTagSyntax(tc.getTagSyntax());
            }
            if (tc.isTemplateExceptionHandlerSet()) {
                mergedTC.setTemplateExceptionHandler(tc.getTemplateExceptionHandler());
            }
            if (tc.isTimeFormatSet()) {
                mergedTC.setTimeFormat(tc.getTimeFormat());
            }
            if (tc.isTimeZoneSet()) {
                mergedTC.setTimeZone(tc.getTimeZone());
            }
            if (tc.isWhitespaceStrippingSet()) {
                mergedTC.setWhitespaceStripping(tc.getWhitespaceStripping());
            }
        }
        return mergedTC;
    }

    public void configure(Template template) {
        if (!parentConfigurationSet) {
            throw new IllegalStateException("The TemplateConfigurer wasn't associated with a Configuration yet.");
        }
        Configuration cfg = getParentConfiguration();
        if (template.getConfiguration() != cfg) {
            // This is actually not a problem right now, but for future BC we enforce this.
            throw new IllegalArgumentException(
                    "The argument Template doesn't belong to the same Configuration as the TemplateConfigurer");
        }

        if (isAPIBuiltinEnabledSet()) {
            template.setAPIBuiltinEnabled(isAPIBuiltinEnabled());
        }
        if (isSQLDateAndTimeTimeZoneSet()) {
            template.setSQLDateAndTimeTimeZone(getSQLDateAndTimeTimeZone());
        }
        if (isURLEscapingCharsetSet()) {
            template.setURLEscapingCharset(getURLEscapingCharset());
        }
        if (isArithmeticEngineSet()) {
            template.setArithmeticEngine(getArithmeticEngine());
        }
        if (isAutoFlushSet()) {
            template.setAutoFlush(getAutoFlush());
        }
        if (isBooleanFormatSet()) {
            template.setBooleanFormat(getBooleanFormat());
        }
        if (isClassicCompatibleSet()) {
            template.setClassicCompatibleAsInt(getClassicCompatibleAsInt());
        }
        if (isDateFormatSet()) {
            template.setDateFormat(getDateFormat());
        }
        if (isDateTimeFormatSet()) {
            template.setDateTimeFormat(getDateTimeFormat());
        }
        if (isLocaleSet()) {
            template.setLocale(getLocale());
        }
        if (isLogTemplateExceptionsSet()) {
            template.setLogTemplateExceptions(getLogTemplateExceptions());
        }
        if (isNewBuiltinClassResolverSet()) {
            template.setNewBuiltinClassResolver(getNewBuiltinClassResolver());
        }
        if (isNumberFormatSet()) {
            template.setNumberFormat(getNumberFormat());
        }
        if (isObjectWrapperSet()) {
            template.setObjectWrapper(getObjectWrapper());
        }
        if (isOutputEncodingSet()) {
            template.setOutputEncoding(getOutputEncoding());
        }
        if (isShowErrorTipsSet()) {
            template.setShowErrorTips(getShowErrorTips());
        }
        if (isTemplateExceptionHandlerSet()) {
            template.setTemplateExceptionHandler(getTemplateExceptionHandler());
        }
        if (isTimeFormatSet()) {
            template.setTimeFormat(getTimeFormat());
        }
        if (isTimeZoneSet()) {
            template.setTimeZone(getTimeZone());
        }
    }

    /**
     * See {@link Configuration#setTagSyntax(int)}.
     */
    public void setTagSyntax(int tagSyntax) {
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
     * See {@link Configuration#setTagSyntax(int)}.
     */
    public void setNamingConvention(int namingConvention) {
        this.namingConvention = Integer.valueOf(namingConvention);
    }

    /**
     * The getter pair of {@link #setTagSyntax(int)}.
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
     * See {@link Configuration#setTagSyntax(int)}.
     */
    public void setWhitespaceStripping(boolean whitespaceStripping) {
        this.whitespaceStripping = Boolean.valueOf(whitespaceStripping);
    }

    /**
     * The getter pair of {@link #setTagSyntax(int)}.
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

    public Configuration getParentConfiguration() {
        return (Configuration) getParent();
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
     * Specifies the charset used for reading template "file". This is very similar to
     * {@link Configuration#setDefaultEncoding(String)}, but this setting also overrides the locale-specific encodings
     * set via {@link Configuration#setEncoding(java.util.Locale, String)}. This setting can only be overridden in the
     * {@code #ftl} header of the template (like {@code <#ftl encoding="ISO-8859-1">}).
     */
    public void setEncoding(String encoding) {
        NullArgumentException.check("encoding", encoding);
        this.encoding = encoding;
    }

    public boolean isEncodingSet() {
        return encoding != null;
    }
    
}

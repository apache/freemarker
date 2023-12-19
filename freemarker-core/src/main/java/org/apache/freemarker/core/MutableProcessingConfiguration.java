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

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.ConservativeArithmeticEngine;
import org.apache.freemarker.core.cformat.CFormat;
import org.apache.freemarker.core.cformat.impl.StandardCFormats;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.*;
import org.apache.freemarker.core.pluggablebuiltin.TruncateBuiltinAlgorithm;
import org.apache.freemarker.core.pluggablebuiltin.impl.DefaultTruncateBuiltinAlgorithm;
import org.apache.freemarker.core.templateresolver.*;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.util.*;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Extended by FreeMarker core classes (not by you) that support specifying {@link ProcessingConfiguration} setting
 * values. <b>New abstract methods may be added any time in future FreeMarker versions, so don't try to implement this
 * interface yourself!</b>
 */
public abstract class MutableProcessingConfiguration<SelfT extends MutableProcessingConfiguration<SelfT>>
        implements ProcessingConfiguration {
    public static final String NULL_VALUE = "null";
    public static final String DEFAULT_VALUE = "default";
    public static final String JVM_DEFAULT_VALUE = "JVM default";
    
    public static final String LOCALE_KEY = "locale";
    public static final String NUMBER_FORMAT_KEY = "numberFormat";
    public static final String CUSTOM_NUMBER_FORMATS_KEY = "customNumberFormats";
    public static final String TIME_FORMAT_KEY = "timeFormat";
    public static final String DATE_FORMAT_KEY = "dateFormat";
    public static final String DATE_TIME_FORMAT_KEY = "dateTimeFormat";
    public static final String CUSTOM_DATE_FORMATS_KEY = "customDateFormats";
    public static final String TIME_ZONE_KEY = "timeZone";
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY = "sqlDateAndTimeTimeZone";
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY = "templateExceptionHandler";
    public static final String ATTEMPT_EXCEPTION_REPORTER_KEY = "attemptExceptionReporter";
    public static final String ARITHMETIC_ENGINE_KEY = "arithmeticEngine";
    public static final String BOOLEAN_FORMAT_KEY = "booleanFormat";
    public static final String C_FORMAT_KEY = "cFormat";
    public static final String OUTPUT_ENCODING_KEY = "outputEncoding";
    public static final String URL_ESCAPING_CHARSET_KEY = "urlEscapingCharset";
    public static final String AUTO_FLUSH_KEY = "autoFlush";
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY = "newBuiltinClassResolver";
    public static final String SHOW_ERROR_TIPS_KEY = "showErrorTips";
    public static final String API_BUILTIN_ENABLED_KEY = "apiBuiltinEnabled";
    public static final String TRUNCATE_BUILTIN_ALGORITHM_KEY = "truncateBuiltinAlgorithm";
    public static final String LAZY_IMPORTS_KEY = "lazyImports";
    public static final String LAZY_AUTO_IMPORTS_KEY = "lazyAutoImports";
    public static final String AUTO_IMPORTS_KEY = "autoImports";
    public static final String AUTO_INCLUDES_KEY = "autoIncludes";

    private static final Set<String> SETTING_NAMES = new _SortedArraySet<>(
        // Must be sorted alphabetically!
        API_BUILTIN_ENABLED_KEY,
        ARITHMETIC_ENGINE_KEY,
        ATTEMPT_EXCEPTION_REPORTER_KEY,
        AUTO_FLUSH_KEY,
        AUTO_IMPORTS_KEY,
        AUTO_INCLUDES_KEY,
        BOOLEAN_FORMAT_KEY,
        C_FORMAT_KEY,
        CUSTOM_DATE_FORMATS_KEY,
        CUSTOM_NUMBER_FORMATS_KEY,
        DATE_FORMAT_KEY,
        DATE_TIME_FORMAT_KEY,
        LAZY_AUTO_IMPORTS_KEY,
        LAZY_IMPORTS_KEY,
        LOCALE_KEY,
        NEW_BUILTIN_CLASS_RESOLVER_KEY,
        NUMBER_FORMAT_KEY,
        OUTPUT_ENCODING_KEY,
        SHOW_ERROR_TIPS_KEY,
        SQL_DATE_AND_TIME_TIME_ZONE_KEY,
        TEMPLATE_EXCEPTION_HANDLER_KEY,
        TIME_FORMAT_KEY,
        TIME_ZONE_KEY,
        TRUNCATE_BUILTIN_ALGORITHM_KEY,
        URL_ESCAPING_CHARSET_KEY
    );
    
    private Locale locale;
    private String numberFormat;
    private String timeFormat;
    private String dateFormat;
    private String dateTimeFormat;
    private TimeZone timeZone;
    private TimeZone sqlDateAndTimeTimeZone;
    private boolean sqlDateAndTimeTimeZoneSet;
    private String booleanFormat;
    private CFormat cFormat;
    private TemplateExceptionHandler templateExceptionHandler;
    private AttemptExceptionReporter attemptExceptionReporter;
    private ArithmeticEngine arithmeticEngine;
    private Charset outputEncoding;
    private boolean outputEncodingSet;
    private Charset urlEscapingCharset;
    private boolean urlEscapingCharsetSet;
    private Boolean autoFlush;
    private TemplateClassResolver newBuiltinClassResolver;
    private Boolean showErrorTips;
    private Boolean apiBuiltinEnabled;
    private TruncateBuiltinAlgorithm truncateBuiltinAlgorithm;
    private Map<String, TemplateDateFormatFactory> customDateFormats;
    private Map<String, TemplateNumberFormatFactory> customNumberFormats;
    private Map<String, String> autoImports;
    private List<String> autoIncludes;
    private Boolean lazyImports;
    private Boolean lazyAutoImports;
    private boolean lazyAutoImportsSet;
    private Map<Serializable, Object> customSettings = Collections.emptyMap();
    /** If {@code false}, we must use copy-on-write behavior for {@link #customSettings}. */
    private boolean customSettingsModifiable;

    /**
     * Creates a new instance. Normally you do not need to use this constructor,
     * as you don't use <code>MutableProcessingConfiguration</code> directly, but its subclasses.
     */
    protected MutableProcessingConfiguration() {
        // Empty
    }

    @Override
    public Locale getLocale() {
         return isLocaleSet() ? locale : getDefaultLocale();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract Locale getDefaultLocale();

    @Override
    public boolean isLocaleSet() {
        return locale != null;
    }

    /**
     * Setter pair of {@link ProcessingConfiguration#getLocale()}.
     */
    public void setLocale(Locale locale) {
        _NullArgumentException.check("locale", locale);
        this.locale = locale;
    }

    /**
     * Fluent API equivalent of {@link #setLocale(Locale)}
     */
    public SelfT locale(Locale value) {
        setLocale(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetLocale() {
        locale = null;
    }

    /**
     * Setter pair of {@link ProcessingConfiguration#getTimeZone()}.
     */
    public void setTimeZone(TimeZone timeZone) {
        _NullArgumentException.check("timeZone", timeZone);
        this.timeZone = timeZone;
    }

    /**
     * Fluent API equivalent of {@link #setTimeZone(TimeZone)}
     */
    public SelfT timeZone(TimeZone value) {
        setTimeZone(value);
        return self();
    }
    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetTimeZone() {
        this.timeZone = null;
    }

    @Override
    public TimeZone getTimeZone() {
         return isTimeZoneSet() ? timeZone : getDefaultTimeZone();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract TimeZone getDefaultTimeZone();

    @Override
    public boolean isTimeZoneSet() {
        return timeZone != null;
    }
    
    /**
     * Setter pair of {@link ProcessingConfiguration#getSQLDateAndTimeTimeZone()}.
     */
    public void setSQLDateAndTimeTimeZone(TimeZone tz) {
        sqlDateAndTimeTimeZone = tz;
        sqlDateAndTimeTimeZoneSet = true;
    }

    /**
     * Fluent API equivalent of {@link #setSQLDateAndTimeTimeZone(TimeZone)}
     */
    public SelfT sqlDateAndTimeTimeZone(TimeZone value) {
        setSQLDateAndTimeTimeZone(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetSQLDateAndTimeTimeZone() {
        sqlDateAndTimeTimeZone = null;
        sqlDateAndTimeTimeZoneSet = false;
    }

    @Override
    public TimeZone getSQLDateAndTimeTimeZone() {
        return sqlDateAndTimeTimeZoneSet
                ? sqlDateAndTimeTimeZone
                : getDefaultSQLDateAndTimeTimeZone();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract TimeZone getDefaultSQLDateAndTimeTimeZone();

    @Override
    public boolean isSQLDateAndTimeTimeZoneSet() {
        return sqlDateAndTimeTimeZoneSet;
    }

    /**
     * Setter pair of {@link #getNumberFormat()}
     */
    public void setNumberFormat(String numberFormat) {
        _NullArgumentException.check("numberFormat", numberFormat);
        this.numberFormat = numberFormat;
    }

    /**
     * Fluent API equivalent of {@link #setNumberFormat(String)}
     */
    public SelfT numberFormat(String value) {
        setNumberFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetNumberFormat() {
        numberFormat = null;
    }

    @Override
    public String getNumberFormat() {
         return isNumberFormatSet() ? numberFormat : getDefaultNumberFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract String getDefaultNumberFormat();

    @Override
    public boolean isNumberFormatSet() {
        return numberFormat != null;
    }

    /**
     * Setter pair of {@link #getCFormat()}
     */
    public void setCFormat(CFormat cFormat) {
        _NullArgumentException.check("cFormat", cFormat);
        this.cFormat = cFormat;
    }

    /**
     * Fluent API equivalent of {@link #setCFormat(CFormat)}
     */
    public SelfT cFormat(CFormat value) {
        setCFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetCFormat() {
        cFormat = null;
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract CFormat getDefaultCFormat();

    @Override
    public CFormat getCFormat() {
        return isCFormatSet() ? cFormat : getDefaultCFormat();
    }

    @Override
    public boolean isCFormatSet() {
        return cFormat != null;
    }

    @Override
    public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
         return isCustomNumberFormatsSet() ? customNumberFormats : getDefaultCustomNumberFormats();
    }

    protected abstract Map<String, TemplateNumberFormatFactory> getDefaultCustomNumberFormats();

    /**
     * Setter pair of {@link #getCustomNumberFormats()}. Note that custom number formats are get through
     * {@link #getCustomNumberFormat(String)}, not directly though this {@link Map}, so number formats from
     * {@link ProcessingConfiguration}-s on less specific levels are inherited without being present in this
     * {@link Map}.
     *
     * @param customNumberFormats
     *         Not {@code null}; will be copied (to prevent aliasing effect); keys must conform to format name
     *         syntactical restrictions  (see in {@link #getCustomNumberFormats()})
     */
    public void setCustomNumberFormats(Map<String, ? extends TemplateNumberFormatFactory> customNumberFormats) {
        setCustomNumberFormats(customNumberFormats, false);
    }

    /**
     * @param validatedImmutableUnchanging
     *         {@code true} if we know that the 1st argument is already validated, immutable, and unchanging (means,
     *         won't change later because of aliasing).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void setCustomNumberFormats(Map<String, ? extends TemplateNumberFormatFactory> customNumberFormats,
            boolean validatedImmutableUnchanging) {
        _NullArgumentException.check("customNumberFormats", customNumberFormats);
        if (!validatedImmutableUnchanging) {
            if (customNumberFormats == this.customNumberFormats) {
                return;
            }
            _CollectionUtils.safeCastMap("customNumberFormats", customNumberFormats,
                    String.class, false,
                    TemplateNumberFormatFactory.class, false);
            validateFormatNames(customNumberFormats.keySet());
            this.customNumberFormats = Collections.unmodifiableMap(new HashMap<>(customNumberFormats));
        } else {
            this.customNumberFormats = (Map) customNumberFormats;
        }
    }

    /**
     * Fluent API equivalent of {@link #setCustomNumberFormats(Map)}
     */
    public SelfT customNumberFormats(Map<String, ? extends TemplateNumberFormatFactory> value) {
        setCustomNumberFormats(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetCustomNumberFormats() {
        customNumberFormats = null;
    }

    private void validateFormatNames(Set<String> keySet) {
        for (String name : keySet) {
            if (name.length() == 0) {
                throw new IllegalArgumentException("Format names can't be 0 length");
            }
            char firstChar = name.charAt(0);
            if (firstChar == '@') {
                throw new IllegalArgumentException(
                        "Format names can't start with '@'. '@' is only used when referring to them from format "
                        + "strings. In: " + name);
            }
            if (!Character.isLetter(firstChar)) {
                throw new IllegalArgumentException("Format name must start with letter: " + name);
            }
            for (int i = 1; i < name.length(); i++) {
                // Note that we deliberately don't allow "_" here.
                if (!Character.isLetterOrDigit(name.charAt(i))) {
                    throw new IllegalArgumentException("Format name can only contain letters and digits: " + name);
                }
            }
        }
    }

    @Override
    public boolean isCustomNumberFormatsSet() {
        return customNumberFormats != null;
    }

    @Override
    public TemplateNumberFormatFactory getCustomNumberFormat(String name) {
        TemplateNumberFormatFactory r;
        if (customNumberFormats != null) {
            r = customNumberFormats.get(name);
            if (r != null) {
                return r;
            }
        }
        return getDefaultCustomNumberFormat(name);
    }

    protected abstract TemplateNumberFormatFactory getDefaultCustomNumberFormat(String name);

    /**
     * Setter pair of {@link #getBooleanFormat()}.
     */
    public void setBooleanFormat(String booleanFormat) {
        _NullArgumentException.check("booleanFormat", booleanFormat);
        TemplateBooleanFormat.validateFormatString(booleanFormat);
        this.booleanFormat = booleanFormat;
    }

    /**
     * Fluent API equivalent of {@link #setBooleanFormat(String)}
     */
    public SelfT booleanFormat(String value) {
        setBooleanFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetBooleanFormat() {
        booleanFormat = null;
    }
    
    @Override
    public String getBooleanFormat() {
         return isBooleanFormatSet() ? booleanFormat : getDefaultBooleanFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract String getDefaultBooleanFormat();

    @Override
    public boolean isBooleanFormatSet() {
        return booleanFormat != null;
    }

    /**
     * Setter pair of {@link #getTimeFormat()}
     */
    public void setTimeFormat(String timeFormat) {
        _NullArgumentException.check("timeFormat", timeFormat);
        this.timeFormat = timeFormat;
    }

    /**
     * Fluent API equivalent of {@link #setTimeFormat(String)}
     */
    public SelfT timeFormat(String value) {
        setTimeFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetTimeFormat() {
        timeFormat = null;
    }

    @Override
    public String getTimeFormat() {
         return isTimeFormatSet() ? timeFormat : getDefaultTimeFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract String getDefaultTimeFormat();

    @Override
    public boolean isTimeFormatSet() {
        return timeFormat != null;
    }

    /**
     * Setter pair of {@link #getDateFormat()}.
     */
    public void setDateFormat(String dateFormat) {
        _NullArgumentException.check("dateFormat", dateFormat);
        this.dateFormat = dateFormat;
    }

    /**
     * Fluent API equivalent of {@link #setDateFormat(String)}
     */
    public SelfT dateFormat(String value) {
        setDateFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetDateFormat() {
        dateFormat = null;
    }

    @Override
    public String getDateFormat() {
         return isDateFormatSet() ? dateFormat : getDefaultDateFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract String getDefaultDateFormat();

    @Override
    public boolean isDateFormatSet() {
        return dateFormat != null;
    }

    /**
     * Setter pair of {@link #getDateTimeFormat()}
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        _NullArgumentException.check("dateTimeFormat", dateTimeFormat);
        this.dateTimeFormat = dateTimeFormat;
    }

    /**
     * Fluent API equivalent of {@link #setDateTimeFormat(String)}
     */
    public SelfT dateTimeFormat(String value) {
        setDateTimeFormat(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetDateTimeFormat() {
        this.dateTimeFormat = null;
    }

    @Override
    public String getDateTimeFormat() {
         return isDateTimeFormatSet() ? dateTimeFormat : getDefaultDateTimeFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract String getDefaultDateTimeFormat();

    @Override
    public boolean isDateTimeFormatSet() {
        return dateTimeFormat != null;
    }

    @Override
    public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
         return isCustomDateFormatsSet() ? customDateFormats : getDefaultCustomDateFormats();
    }

    protected abstract Map<String, TemplateDateFormatFactory> getDefaultCustomDateFormats();

    /**
     * Setter pair of {@link #getCustomDateFormat(String)}. Note that custom date formats are get through
     * {@link #getCustomNumberFormat(String)}, not directly though this {@link Map}, so date formats from
     * {@link ProcessingConfiguration}-s on less specific levels are inherited without being present in this
     * {@link Map}.
     *
     * @param customDateFormats
     *         Not {@code null}; will be copied (to prevent aliasing effect); keys must conform to format name
     *         syntactical restrictions (see in {@link #getCustomDateFormats()})
     */
    public void setCustomDateFormats(Map<String, ? extends TemplateDateFormatFactory> customDateFormats) {
        setCustomDateFormats(customDateFormats, false);
    }

    /**
     * @param validatedImmutableUnchanging
     *         {@code true} if we know that the 1st argument is already validated, immutable, and unchanging (means,
     *         won't change later because of aliasing).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void setCustomDateFormats(
            Map<String, ? extends TemplateDateFormatFactory> customDateFormats,
            boolean validatedImmutableUnchanging) {
        _NullArgumentException.check("customDateFormats", customDateFormats);
        if (!validatedImmutableUnchanging) {
            if (customDateFormats == this.customDateFormats) {
                return;
            }
            _CollectionUtils.safeCastMap("customDateFormats", customDateFormats,
                    String.class, false,
                    TemplateDateFormatFactory.class, false);
            validateFormatNames(customDateFormats.keySet());
            this.customDateFormats = Collections.unmodifiableMap(new HashMap<>(customDateFormats));
        } else {
            this.customDateFormats = (Map) customDateFormats;
        }
    }

    /**
     * Fluent API equivalent of {@link #setCustomDateFormats(Map)}
     */
    public SelfT customDateFormats(Map<String, ? extends TemplateDateFormatFactory> value) {
        setCustomDateFormats(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetCustomDateFormats() {
        this.customDateFormats = null;
    }

    @Override
    public boolean isCustomDateFormatsSet() {
        return customDateFormats != null;
    }

    @Override
    public TemplateDateFormatFactory getCustomDateFormat(String name) {
        TemplateDateFormatFactory r;
        if (customDateFormats != null) {
            r = customDateFormats.get(name);
            if (r != null) {
                return r;
            }
        }
        return getDefaultCustomDateFormat(name);
    }

    protected abstract TemplateDateFormatFactory getDefaultCustomDateFormat(String name);

    /**
     * Setter pair of {@link #getTemplateExceptionHandler()}
     */
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        _NullArgumentException.check("templateExceptionHandler", templateExceptionHandler);
        this.templateExceptionHandler = templateExceptionHandler;
    }

    /**
     * Fluent API equivalent of {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}
     */
    public SelfT templateExceptionHandler(TemplateExceptionHandler value) {
        setTemplateExceptionHandler(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetTemplateExceptionHandler() {
        templateExceptionHandler = null;
    }

    @Override
    public TemplateExceptionHandler getTemplateExceptionHandler() {
         return isTemplateExceptionHandlerSet()
                ? templateExceptionHandler : getDefaultTemplateExceptionHandler();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract TemplateExceptionHandler getDefaultTemplateExceptionHandler();

    @Override
    public boolean isTemplateExceptionHandlerSet() {
        return templateExceptionHandler != null;
    }

    /**
     * Setter pair of {@link #getAttemptExceptionReporter()}
     */
    public void setAttemptExceptionReporter(AttemptExceptionReporter attemptExceptionReporter) {
        _NullArgumentException.check("attemptExceptionReporter", attemptExceptionReporter);
        this.attemptExceptionReporter = attemptExceptionReporter;
    }

    /**
     * Fluent API equivalent of {@link #setAttemptExceptionReporter(AttemptExceptionReporter)}
     */
    public SelfT attemptExceptionReporter(AttemptExceptionReporter value) {
        setAttemptExceptionReporter(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetAttemptExceptionReporter() {
        attemptExceptionReporter = null;
    }

    @Override
    public AttemptExceptionReporter getAttemptExceptionReporter() {
        return isAttemptExceptionReporterSet()
                ? attemptExceptionReporter : getDefaultAttemptExceptionReporter();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract AttemptExceptionReporter getDefaultAttemptExceptionReporter();

    @Override
    public boolean isAttemptExceptionReporterSet() {
        return attemptExceptionReporter != null;
    }

    /**
     * Setter pair of {@link #getArithmeticEngine()}
     */
    public void setArithmeticEngine(ArithmeticEngine arithmeticEngine) {
        _NullArgumentException.check("arithmeticEngine", arithmeticEngine);
        this.arithmeticEngine = arithmeticEngine;
    }

    /**
     * Fluent API equivalent of {@link #setArithmeticEngine(ArithmeticEngine)}
     */
    public SelfT arithmeticEngine(ArithmeticEngine value) {
        setArithmeticEngine(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetArithmeticEngine() {
        this.arithmeticEngine = null;
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
         return isArithmeticEngineSet() ? arithmeticEngine : getDefaultArithmeticEngine();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract ArithmeticEngine getDefaultArithmeticEngine();

    @Override
    public boolean isArithmeticEngineSet() {
        return arithmeticEngine != null;
    }

    /**
     * The setter pair of {@link #getOutputEncoding()}
     */
    public void setOutputEncoding(Charset outputEncoding) {
        this.outputEncoding = outputEncoding;
        outputEncodingSet = true;
    }

    /**
     * Fluent API equivalent of {@link #setOutputEncoding(Charset)}
     */
    public SelfT outputEncoding(Charset value) {
        setOutputEncoding(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetOutputEncoding() {
        this.outputEncoding = null;
        outputEncodingSet = false;
    }

    @Override
    public Charset getOutputEncoding() {
        return isOutputEncodingSet()
                ? outputEncoding
                : getDefaultOutputEncoding();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract Charset getDefaultOutputEncoding();

    @Override
    public boolean isOutputEncodingSet() {
        return outputEncodingSet;
    }

    /**
     * The setter pair of {@link #getURLEscapingCharset()}.
     */
    public void setURLEscapingCharset(Charset urlEscapingCharset) {
        this.urlEscapingCharset = urlEscapingCharset;
        urlEscapingCharsetSet = true;
    }

    /**
     * Fluent API equivalent of {@link #setURLEscapingCharset(Charset)}
     */
    public SelfT urlEscapingCharset(Charset value) {
        setURLEscapingCharset(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetURLEscapingCharset() {
        this.urlEscapingCharset = null;
        urlEscapingCharsetSet = false;
    }

    @Override
    public Charset getURLEscapingCharset() {
        return isURLEscapingCharsetSet() ? urlEscapingCharset : getDefaultURLEscapingCharset();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract Charset getDefaultURLEscapingCharset();

    @Override
    public boolean isURLEscapingCharsetSet() {
        return urlEscapingCharsetSet;
    }

    /**
     * Setter pair of {@link #getNewBuiltinClassResolver()}
     */
    public void setNewBuiltinClassResolver(TemplateClassResolver newBuiltinClassResolver) {
        _NullArgumentException.check("newBuiltinClassResolver", newBuiltinClassResolver);
        this.newBuiltinClassResolver = newBuiltinClassResolver;
    }

    /**
     * Fluent API equivalent of {@link #setNewBuiltinClassResolver(TemplateClassResolver)}
     */
    public SelfT newBuiltinClassResolver(TemplateClassResolver value) {
        setNewBuiltinClassResolver(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetNewBuiltinClassResolver() {
        this.newBuiltinClassResolver = null;
    }

    @Override
    public TemplateClassResolver getNewBuiltinClassResolver() {
         return isNewBuiltinClassResolverSet()
                ? newBuiltinClassResolver : getDefaultNewBuiltinClassResolver();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract TemplateClassResolver getDefaultNewBuiltinClassResolver();

    @Override
    public boolean isNewBuiltinClassResolverSet() {
        return newBuiltinClassResolver != null;
    }

    /**
     * Setter pair of {@link #getAutoFlush()}
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }

    /**
     * Fluent API equivalent of {@link #setAutoFlush(boolean)}
     */
    public SelfT autoFlush(boolean value) {
        setAutoFlush(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetAutoFlush() {
        this.autoFlush = null;
    }

    @Override
    public boolean getAutoFlush() {
         return isAutoFlushSet() ? autoFlush : getDefaultAutoFlush();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract boolean getDefaultAutoFlush();

    @Override
    public boolean isAutoFlushSet() {
        return autoFlush != null;
    }

    /**
     * Setter pair of {@link #getShowErrorTips()}
     */
    public void setShowErrorTips(boolean showTips) {
        showErrorTips = showTips;
    }

    /**
     * Fluent API equivalent of {@link #setShowErrorTips(boolean)}
     */
    public SelfT showErrorTips(boolean value) {
        setShowErrorTips(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetShowErrorTips() {
        showErrorTips = null;
    }

    @Override
    public boolean getShowErrorTips() {
         return isShowErrorTipsSet() ? showErrorTips : getDefaultShowErrorTips();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract boolean getDefaultShowErrorTips();

    @Override
    public boolean isShowErrorTipsSet() {
        return showErrorTips != null;
    }

    /**
     * Setter pair of {@link #getAPIBuiltinEnabled()}
     */
    public void setAPIBuiltinEnabled(boolean value) {
        apiBuiltinEnabled = Boolean.valueOf(value);
    }

    /**
     * Fluent API equivalent of {@link #setAPIBuiltinEnabled(boolean)}
     */
    public SelfT apiBuiltinEnabled(boolean value) {
        setAPIBuiltinEnabled(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetAPIBuiltinEnabled() {
        apiBuiltinEnabled = null;
    }

    @Override
    public boolean getAPIBuiltinEnabled() {
         return isAPIBuiltinEnabledSet() ? apiBuiltinEnabled : getDefaultAPIBuiltinEnabled();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract boolean getDefaultAPIBuiltinEnabled();

    @Override
    public boolean isAPIBuiltinEnabledSet() {
        return apiBuiltinEnabled != null;
    }

    /**
     * Setter pair of {@link #getTruncateBuiltinAlgorithm()}
     */
    public void setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm value) {
        _NullArgumentException.check("value", value);
        truncateBuiltinAlgorithm = value;
    }

    /**
     * Fluent API equivalent of {@link #setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm)}
     */
    public SelfT truncateBuiltinAlgorithm(TruncateBuiltinAlgorithm value) {
        setTruncateBuiltinAlgorithm(value);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetTruncateBuiltinAlgorithm() {
        truncateBuiltinAlgorithm = null;
    }

    @Override
    public TruncateBuiltinAlgorithm getTruncateBuiltinAlgorithm() {
        return isTruncateBuiltinAlgorithmSet() ? truncateBuiltinAlgorithm : getDefaultTruncateBuiltinAlgorithm();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract TruncateBuiltinAlgorithm getDefaultTruncateBuiltinAlgorithm();

    @Override
    public boolean isTruncateBuiltinAlgorithmSet() {
        return truncateBuiltinAlgorithm != null;
    }

    @Override
    public boolean getLazyImports() {
         return isLazyImportsSet() ? lazyImports : getDefaultLazyImports();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract boolean getDefaultLazyImports();

    /**
     * Setter pair of {@link #getLazyImports()}
     */
    public void setLazyImports(boolean lazyImports) {
        this.lazyImports = lazyImports;
    }

    /**
     * Fluent API equivalent of {@link #setLazyImports(boolean)}
     */
    public SelfT lazyImports(boolean lazyImports) {
        setLazyImports(lazyImports);
        return  self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetLazyImports() {
        this.lazyImports = null;
    }

    @Override
    public boolean isLazyImportsSet() {
        return lazyImports != null;
    }

    @Override
    public Boolean getLazyAutoImports() {
        return isLazyAutoImportsSet() ? lazyAutoImports : getDefaultLazyAutoImports();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract Boolean getDefaultLazyAutoImports();

    /**
     * Setter pair of {@link #getLazyAutoImports()}
     */
    public void setLazyAutoImports(Boolean lazyAutoImports) {
        this.lazyAutoImports = lazyAutoImports;
        lazyAutoImportsSet = true;
    }

    /**
     * Fluent API equivalent of {@link #setLazyAutoImports(Boolean)}
     */
    public SelfT lazyAutoImports(Boolean lazyAutoImports) {
        setLazyAutoImports(lazyAutoImports);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetLazyAutoImports() {
        lazyAutoImports = null;
        lazyAutoImportsSet = false;
    }

    @Override
    public boolean isLazyAutoImportsSet() {
        return lazyAutoImportsSet;
    }
    
    /**
     * Setter pair of {@link #getAutoImports()}.
     * 
     * @param autoImports
     *            Maps the namespace variable names to the template names; not {@code null}, and can't contain {@code
     *            null} keys of values. The content of the {@link Map} is copied into another {@link Map}, to avoid
     *            aliasing problems. The iteration order of the original {@link Map} entries is kept.
     */
    public void setAutoImports(Map<String, String> autoImports) {
        setAutoImports(autoImports, false);
    }

    /**
     * @param validatedImmutableUnchanging
     *         {@code true} if we know that the 1st argument is already validated, immutable, and unchanging (means,
     *         won't change later because of aliasing).
     */
    void setAutoImports(Map<String, String> autoImports, boolean validatedImmutableUnchanging) {
        _NullArgumentException.check("autoImports", autoImports);
        if (!validatedImmutableUnchanging) {
            if (autoImports == this.autoImports) {
                return;
            }
            _CollectionUtils.safeCastMap("autoImports", autoImports, String.class, false, String.class, false);
            this.autoImports = Collections.unmodifiableMap(new LinkedHashMap<>(autoImports));
        } else {
            this.autoImports = autoImports;
        }
    }

    /**
     * Fluent API equivalent of {@link #setAutoImports(Map)}
     */
    public SelfT autoImports(Map<String, String> map) {
        setAutoImports(map);
        return self();
    }

     /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ProcessingConfiguration}).
     */
    public void unsetAutoImports() {
        autoImports = null;
    }

    @Override
    public Map<String, String> getAutoImports() {
         return isAutoImportsSet() ? autoImports : getDefaultAutoImports();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract Map<String,String> getDefaultAutoImports();

    @Override
    public boolean isAutoImportsSet() {
        return autoImports != null;
    }

    /**
     * Setter pair of {@link #getAutoIncludes()}
     *
     * @param autoIncludes Not {@code null}. The {@link List} will be copied to avoid aliasing problems.
     */
    public void setAutoIncludes(List<String> autoIncludes) {
        setAutoIncludes(autoIncludes, false);
    }

    /**
     * @param validatedImmutableUnchanging
     *         {@code true} if we know that the 1st argument is already validated, immutable, and unchanging (means,
     *         won't change later because of aliasing).
     */
    void setAutoIncludes(List<String> autoIncludes, boolean validatedImmutableUnchanging) {
        _NullArgumentException.check("autoIncludes", autoIncludes);
        if (!validatedImmutableUnchanging) {
            if (autoIncludes == this.autoIncludes) {
                return;
            }
            _CollectionUtils.safeCastList("autoIncludes", autoIncludes, String.class, false);
            Set<String> uniqueItems = new LinkedHashSet<>(autoIncludes.size() * 4 / 3, 1f);
            for (String templateName : autoIncludes) {
                if (!uniqueItems.add(templateName)) {
                    // Move clashing item at the end of the collection
                    uniqueItems.remove(templateName);
                    uniqueItems.add(templateName);
                }
            }
            this.autoIncludes = Collections.unmodifiableList(new ArrayList<>(uniqueItems));
        } else {
            this.autoIncludes = autoIncludes;
        }
    }

    /**
     * Fluent API equivalent of {@link #setAutoIncludes(List)}
     */
    public SelfT autoIncludes(List<String> templateNames) {
        setAutoIncludes(templateNames);
        return self();
    }

    /**
     * Varargs overload of {@link #autoIncludes(List)}.
     */
    public SelfT autoIncludes(String... templateNames) {
        setAutoIncludes(Arrays.asList(templateNames));
        return self();
    }

    @Override
    public List<String> getAutoIncludes() {
         return isAutoIncludesSet() ? autoIncludes : getDefaultAutoIncludes();
    }

    /**
     * Returns the value the getter method returns when the setting is not set (possibly by inheriting the setting value
     * from another {@link ProcessingConfiguration}), or throws {@link CoreSettingValueNotSetException}.
     */
    protected abstract List<String> getDefaultAutoIncludes();

    @Override
    public boolean isAutoIncludesSet() {
        return autoIncludes != null;
    }
    
    private static final String ALLOWED_CLASSES = "allowedClasses";
    private static final String TRUSTED_TEMPLATES = "trustedTemplates";
    
    /**
     * Sets a FreeMarker setting by a name and string value. If you can configure FreeMarker directly with Java (or
     * other programming language), you should use the dedicated setter methods instead (like
     * {@link #setTimeZone(TimeZone)}. This meant to be used only when you get settings from somewhere
     * as {@link String}-{@link String} name-value pairs (typically, as a {@link Properties} object). Below you find an
     * overview of the settings available.
     * 
     * <p>Note: As of FreeMarker 2.3.23, setting names can be written in camel case too. For example, instead of
     * {@code dateFormat} you can also use {@code dateFormat}. It's likely that camel case will become to the
     * recommended convention in the future.
     * 
     * <p>The list of settings commonly supported in all {@link MutableProcessingConfiguration} subclasses:
     * <ul>
     *   <li><p>{@code "locale"}:
     *       See {@link #setLocale(Locale)}.
     *       <br>String value: local codes with the usual format in Java, such as {@code "en_US"}, or
     *       "JVM default" (ignoring case) to use the default locale of the Java environment.
     *
     *   <li><p>{@code "customNumberFormat"}: See {@link #setCustomNumberFormats(Map)}.
     *   <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>.
     *   <br>Example: <code>{ "hex": com.example.HexTemplateNumberFormatFactory,
     *   "gps": com.example.GPSTemplateNumberFormatFactory }</code>
     *
     *   <li><p>{@code "customDateFormat"}: See {@link #setCustomDateFormats(Map)}.
     *   <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>.
     *   <br>Example: <code>{ "trade": com.example.TradeTemplateDateFormatFactory,
     *   "log": com.example.LogTemplateDateFormatFactory }</code>
     *
     *   <li><p>{@code "cFormat"}:
     *       See {@link #setCFormat(CFormat)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default (on {@link Configuration} only),
     *       or one of the predefined values {@code "JavaScript or JSON"}, {@code "JSON"},
     *       {@code "JavaScript"}, {@code "Java"}, {@code "XS"},
     *       or an <a href="#fm_obe">object builder expression</a> that gives a {@link CFormat} object.
     *
     *   <li><p>{@code "templateExceptionHandler"}:
     *       See {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot, then it must be one of these predefined values (case insensitive):
     *       {@code "rethrow"} (means {@link TemplateExceptionHandler#RETHROW}),
     *       {@code "debug"} (means {@link TemplateExceptionHandler#DEBUG}),
     *       {@code "htmlDebug"} (means {@link TemplateExceptionHandler#HTML_DEBUG}),
     *       {@code "ignore"} (means {@link TemplateExceptionHandler#IGNORE}), or
     *       {@code "default"} (only allowed for {@link Configuration} instances) for the default.
     *
     *   <li><p>{@code "attemptExceptionReporter"}:
     *       See {@link #setAttemptExceptionReporter(AttemptExceptionReporter)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot, then it must be one of these predefined values (case insensitive):
     *       {@code "logError"} (means {@link AttemptExceptionReporter#LOG_ERROR}),
     *       {@code "logWarn"} (means {@link AttemptExceptionReporter#LOG_WARN}), or
     *       {@code "default"} (only allowed for {@link Configuration} instances) for the default value.
     *       
     *   <li><p>{@code "arithmeticEngine"}:
     *       See {@link #setArithmeticEngine(ArithmeticEngine)}.  
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then it must be one of these special values (case insensitive):
     *       {@code "bigdecimal"}, {@code "conservative"}.
     *       
     *   <li><p>{@code "objectWrapper"}:
     *       See {@link Configuration.Builder#setObjectWrapper(ObjectWrapper)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>, with the addition that {@link DefaultObjectWrapper}, {@link DefaultObjectWrapper} and
     *       {@link RestrictedObjectWrapper} can be referred without package name. For example, these strings are valid
     *       values: {@code "DefaultObjectWrapper(3.0.0)"}, {@code "RestrictedObjectWrapper(3.0.0)"}.
     *       {@code "com.example.MyObjectWrapper(1, 2, someProperty=true, otherProperty=false)"}.
     *       <br>It also accepts the special value (case insensitive) {@code "default"}.
     *       <br>It also accepts the special value (case insensitive) {@code "default"}.
     *
     *   <li><p>{@code "numberFormat"}: See {@link #setNumberFormat(String)}.
     *   
     *   <li><p>{@code "booleanFormat"}: See {@link #setBooleanFormat(String)} .
     *   
     *   <li><p>{@code "dateFormat", "dateFormat", "dateTimeFormat"}:
     *       See {@link #setDateFormat(String)}, {@link #setTimeFormat(String)}, {@link #setDateTimeFormat(String)}. 
     *        
     *   <li><p>{@code "timeZone"}:
     *       See {@link #setTimeZone(TimeZone)}.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone(String)} defines it.
     *       Also, {@code "JVM default"} can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called.
     *       For example {@code "GMT-8:00"} or {@code "America/Los_Angeles"}
     *       <br>If you set this setting, consider setting {@code sqlDateAndTimeTimeZone}
     *       too (see below)! 
     *       
     *   <li><p>{@code sqlDateAndTimeTimeZone}:
     *       See {@link #setSQLDateAndTimeTimeZone(TimeZone)}.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone(String)} defines it.
     *       Also, {@code "JVM default"} can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called. Also {@code "null"} can be used, which has the same effect
     *       as {@link #setSQLDateAndTimeTimeZone(TimeZone) setSQLDateAndTimeTimeZone(null)}.
     *       
     *   <li><p>{@code "outputEncoding"}:
     *       See {@link #setOutputEncoding(Charset)}.
     *       
     *   <li><p>{@code "urlEscapingCharset"}:
     *       See {@link #setURLEscapingCharset(Charset)}.
     *       
     *   <li><p>{@code "autoFlush"}:
     *       See {@link #setAutoFlush(boolean)}.
     *       Since 2.3.17.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code "autoImports"}:
     *       See {@link Configuration#getAutoImports()}
     *       <br>String value is something like:
     *       <br>{@code /lib/form.f3ah as f, /lib/widget as w, "/lib/odd name.f3ah" as odd}
     *       
     *   <li><p>{@code "autoInclude"}: Sets the list of auto-includes.
     *       See {@link Configuration#getAutoIncludes()}
     *       <br>String value is something like:
     *       <br>{@code /include/common.f3ah, "/include/evil name.f3ah"}
     *       
     *   <li><p>{@code "lazyAutoImports"}:
     *       See {@link Configuration#getLazyAutoImports()}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}), case insensitive. Also can be {@code "null"}.

     *   <li><p>{@code "lazyImports"}:
     *       See {@link Configuration#getLazyImports()}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}), case insensitive.
     *       
     *   <li><p>{@code "newBuiltinClassResolver"}:
     *       See {@link #setNewBuiltinClassResolver(TemplateClassResolver)}.
     *       Since 2.3.17.
     *       The value must be one of these (ignore the quotation marks):
     *       <ol>
     *         <li><p>{@code "unrestricted"}:
     *             Use {@link TemplateClassResolver#UNRESTRICTED}
     *         <li><p>{@code "allowNothing"}:
     *             Use {@link TemplateClassResolver#ALLOW_NOTHING}
     *         <li><p>Something that contains colon will use
     *             {@link OptInTemplateClassResolver} and is expected to
     *             store comma separated values (possibly quoted) segmented
     *             with {@code "allowedClasses:"} and/or
     *             {@code "trustedTemplates:"}. Examples of valid values:
     *             
     *             <table style="width: auto; border-collapse: collapse" border="1">
     *               <caption style="display: none">trustedTemplates value examples</caption>
     *               <tr>
     *                 <th>Setting value
     *                 <th>Meaning
     *               <tr>
     *                 <td>
     *                   {@code allowedClasses: com.example.C1, com.example.C2,
     *                   trustedTemplates: lib/*, safe.f3ah}
     *                 <td>
     *                   Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. But, allow templates
     *                   within the {@code lib/} directory (like
     *                   {@code lib/foo/bar.f3ah}) and template {@code safe.f3ah}
     *                   (that does not match {@code foo/safe.f3ah}, only
     *                   exactly {@code safe.f3ah}) to instantiate anything
     *                   that {@link TemplateClassResolver#UNRESTRICTED} allows.
     *               <tr>
     *                 <td>
     *                   {@code allowedClasses: com.example.C1, com.example.C2}
     *                 <td>Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. There are no
     *                   trusted templates.
     *               <tr>
     *                 <td>
                         {@code trustedTemplates: lib/*, safe.f3ah}
     *                 <td>
     *                   Do not allow instantiating any classes, except in
     *                   templates inside {@code lib/} or in template 
     *                   {@code safe.f3ah}.
     *             </table>
     *             
     *             <p>For more details see {@link OptInTemplateClassResolver}.
     *             
     *         <li><p>Otherwise if the value contains dot, it's interpreted as an <a href="#fm_obe">object builder
     *             expression</a>.
     *       </ol>
     *       Note that the {@code safer} option was removed in FreeMarker 3.0.0, as it has become equivalent with
     *       {@code "unrestricted"}, as the classes it has blocked were removed from FreeMarker.
     *   <li><p>{@code "showErrorTips"}:
     *       See {@link #setShowErrorTips(boolean)}.
     *       Since 2.3.21.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code apiBuiltinEnabled}:
     *       See {@link #setAPIBuiltinEnabled(boolean)}.
     *       Since 2.3.22.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *
     *   <li><p>{@code "truncateBuiltinAlgorithm"}:
     *       See {@link #setTruncateBuiltinAlgorithm(TruncateBuiltinAlgorithm)}.
     *       <br>String value: An
     *       <a href="#fm_obe">object builder expression</a>, or one of the predefined values (case insensitive),
     *       {@code ascii} (for {@link DefaultTruncateBuiltinAlgorithm#ASCII_INSTANCE}) and
     *       {@code unicode} (for {@link DefaultTruncateBuiltinAlgorithm#UNICODE_INSTANCE}).
     *       <br>Example object builder expressions:
     *       <br>Use {@code "..."} as terminator (and same as markup terminator), and add space if the
     *       truncation happened on word boundary:
     *       <br>{@code DefaultTruncateBuiltinAlgorithm("...", true)}
     *       <br>Use {@code "..."} as terminator, and also a custom HTML for markup terminator, and add space if the
     *       truncation happened on word boundary:
     *       <br>{@code DefaultTruncateBuiltinAlgorithm("...",
     *       markup(HTMLOutputFormat(), "<span class=trunc>...</span>"), true)}
     *       <br>Recreate default truncate algorithm, but with not preferring truncation at word boundaries (i.e.,
     *       with {@code wordBoundaryMinLength} 1.0):
     *       <br><code>DefaultTruncateBuiltinAlgorithm(<br>
     *       DefaultTruncateBuiltinAlgorithm.STANDARD_ASCII_TERMINATOR, null, null,<br>
     *       DefaultTruncateBuiltinAlgorithm.STANDARD_M_TERMINATOR, null, null,<br>
     *       true, 1.0)</code>
     * </ul>
     * 
     * <p>{@link Configuration.Builder} (which implements {@link MutableParsingAndProcessingConfiguration} and
     * {@link TopLevelConfiguration}) also understands these:</p>
     * <ul>
     *   <li><p>{@code "auto_escaping"}:
     *       See {@link ParsingConfiguration#getAutoEscapingPolicy()}
     *       <br>String value: {@code "enableIfDefault"} or {@code "enableIfDefault"} for
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT},
     *       {@code "enableIfDefault"} or {@code "enableIfSupported"} for
     *       {@link AutoEscapingPolicy#ENABLE_IF_SUPPORTED},
     *       {@code "enableIfDefault"} or {@code "force"} for
     *       {@link AutoEscapingPolicy#FORCE}
     *       {@code "disable"} for {@link AutoEscapingPolicy#DISABLE}.
     *       
     *   <li><p>{@code "sourceEncoding"}:
     *       See {@link ParsingConfiguration#getSourceEncoding()}; since 2.3.26 also accepts value "JVM default"
     *       (not case sensitive) to set the Java environment default value.
     *       <br>As the default value is the system default, which can change
     *       from one server to another, <b>you should always set this!</b>
     *       
     *   <li><p>{@code "localizedTemplateLookup"}:
     *       See {@link TopLevelConfiguration#getLocalizedTemplateLookup()}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}).
     *       ASTDirCase insensitive.
     *       
     *   <li><p>{@code "outputFormat"}:
     *       See {@link ParsingConfiguration#getOutputFormat()}.
     *       <br>String value: {@code "default"} (case insensitive) for the default,
     *       one of {@code undefined}, {@code HTML}, {@code XHTML}, {@code XML}, {@code RTF}, {@code plainText},
     *       {@code CSS}, {@code JavaScript}, {@code JSON},
     *       or an <a href="#fm_obe">object builder expression</a> that gives an {@link OutputFormat}, for example
     *       {@code HTMLOutputFormat}, or {@code com.example.MyOutputFormat()}.
     *       
     *   <li><p>{@code "registeredCustomOutputFormats"}:
     *       See {@link TopLevelConfiguration#getRegisteredCustomOutputFormats()}.
     *       <br>String value: an <a href="#fm_obe">object builder expression</a> that gives a {@link List} of
     *       {@link OutputFormat}-s.
     *       Example: {@code [com.example.MyOutputFormat(), com.example.MyOtherOutputFormat()]}
     *       
     *   <li><p>{@code "whitespaceStripping"}:
     *       See {@link ParsingConfiguration#getWhitespaceStripping()}.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code yes}, etc.
     *       
     *   <li><p>{@code "templateCacheStorage"}:
     *       See {@link TopLevelConfiguration#getTemplateCacheStorage()}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then a {@link org.apache.freemarker.core.templateresolver.impl.MruCacheStorage} will be used with the
     *       maximum strong and soft sizes specified with the setting value. Examples
     *       of valid setting values:
     *       
     *       <table style="width: auto; border-collapse: collapse" border="1">
     *         <caption style="display: none">templateCacheStorage value examples</caption>
     *         <tr><th>Setting value<th>max. strong size<th>max. soft size
     *         <tr><td>{@code "strong:50, soft:500"}<td>50<td>500
     *         <tr><td>{@code "strong:100, soft"}<td>100<td>{@code Integer.MAX_VALUE}
     *         <tr><td>{@code "strong:100"}<td>100<td>0
     *         <tr><td>{@code "soft:100"}<td>0<td>100
     *         <tr><td>{@code "strong"}<td>{@code Integer.MAX_VALUE}<td>0
     *         <tr><td>{@code "soft"}<td>0<td>{@code Integer.MAX_VALUE}
     *       </table>
     *       
     *       <p>The value is not case sensitive. The order of {@code soft} and {@code strong}
     *       entries is not significant.
     *       
     *   <li><p>{@code "templateUpdateDelay"}:
     *       Template update delay in <b>seconds</b> (not in milliseconds) if no unit is specified; see
     *       {@link TopLevelConfiguration#getTemplateUpdateDelayMilliseconds()} for more.
     *       <br>String value: Valid positive integer, optionally followed by a time unit (recommended). The default
     *       unit is seconds. It's strongly recommended to specify the unit for clarity, like in "500 ms" or "30 s".
     *       Supported units are: "s" (seconds), "ms" (milliseconds), "m" (minutes), "h" (hours). The whitespace between
     *       the unit and the number is optional. Units are only supported since 2.3.23.
     *       
     *   <li><p>{@code "incompatibleImprovements"}:
     *       See {@link ParsingConfiguration#getIncompatibleImprovements()}.
     *       <br>String value: version number like {@code 2.3.20}.
     *
     *   <li><p>{@code "recognizeStandardFileExtensions"}:
     *       See {@link TopLevelConfiguration#getRecognizeStandardFileExtensions()}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or {@code "true"}, {@code "false"},
     *       {@code yes}, etc.
     *       
     *   <li><p>{@code "templateConfigurations"}:
     *       See: {@link TopLevelConfiguration#getTemplateConfigurations()}.
     *       <br>String value: Interpreted as an <a href="#fm_obe">object builder expression</a>,
     *       can be {@code null}.
     *       
     *   <li><p>{@code "templateLoader"}:
     *       See: {@link TopLevelConfiguration#getTemplateLoader()}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>. {@code "null"} is also allowed.
     *       
     *   <li><p>{@code "templateLookupStrategy"}:
     *       See: {@link TopLevelConfiguration#getTemplateLookupStrategy()}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>.
     *       
     *   <li><p>{@code "templateNameFormat"}:
     *       See: {@link TopLevelConfiguration#getTemplateNameFormat()}.
     *       <br>String value: {@code "default"} (case insensitive) for the default,
     *       {@link DefaultTemplateNameFormat#INSTANCE}.
     * </ul>
     * 
     * <p id="fm_obe">Regarding <em>object builder expressions</em> (used by the setting values where it was
     * indicated):
     * <ul>
     *   <li><p>Before FreeMarker 2.3.21 it had to be a fully qualified class name, and nothing else.</li>
     *   <li><p>Since 2.3.21, the generic syntax is:
     *       <code><i>className</i>(<i>constrArg1</i>, <i>constrArg2</i>, ... <i>constrArgN</i>,
     *       <i>propName1</i>=<i>propValue1</i>, <i>propName2</i>=<i>propValue2</i>, ...
     *       <i>propNameN</i>=<i>propValueN</i>)</code>,
     *       where
     *       <code><i>className</i></code> is the fully qualified class name of the instance to invoke (except if we have
     *       builder class or {@code INSTANCE} field around, but see that later),
     *       <code><i>constrArg</i></code>-s are the values of constructor arguments,
     *       and <code><i>propName</i>=<i>propValue</i></code>-s set JavaBean properties (like {@code x=1} means
     *       {@code setX(1)}) on the created instance. You can have any number of constructor arguments and property
     *       setters, including 0. Constructor arguments must precede any property setters.   
     *   </li>
     *   <li>
     *     Example: {@code com.example.MyObjectWrapper(1, 2, exposeFields=true, cacheSize=5000)} is nearly
     *     equivalent with this Java code:
     *     {@code obj = new com.example.MyObjectWrapper(1, 2); obj.setExposeFields(true); obj.setCacheSize(5000);}
     *   </li>
     *   <li>
     *      <p>If you have no constructor arguments and property setters, and the <code><i>className</i></code> class has
     *      a public static {@code INSTANCE} field, the value of that filed will be the value of the expression, and
     *      the constructor won't be called.
     *   </li>
     *   <li>
     *      <p>If there exists a class named <code><i>className</i>Builder</code>, then that class will be instantiated
     *      instead with the given constructor arguments, and the JavaBean properties of that builder instance will be
     *      set. After that, the public {@code build()} method of the instance will be called, whose return value
     *      will be the value of the whole expression. (The builder class and the {@code build()} method is simply
     *      found by name, there's no special interface to implement.)Note that if you have a builder class, you don't
     *      actually need a <code><i>className</i></code> class (since 2.3.24); after all,
     *      <code><i>className</i>Builder.build()</code> can return any kind of object. 
     *   </li>
     *   <li>
     *      <p>Currently, the values of arguments and properties can only be one of these:
     *      <ul>
     *        <li>A numerical literal, like {@code 123} or {@code -1.5}. The value will be automatically converted to
     *        the type of the target (just like in FTL). However, a target type is only available if the number will
     *        be a parameter to a method or constructor, not when it's a value (or key) in a {@code List} or
     *        {@code Map} literal. Thus in the last case the type of number will be like in Java language, like
     *        {@code 1} is an {@code int}, and {@code 1.0} is a {@code double}, and {@code 1.0f} is a {@code float},
     *        etc. In all cases, the standard Java type postfixes can be used ("f", "d", "l"), plus "bd" for
     *        {@code BigDecimal} and "bi" for {@code BigInteger}.</li>
     *        <li>A boolean literal: {@code true} or {@code false}
     *        <li>The null literal: {@code null}
     *        <li>A string literal with FTL syntax, except that  it can't contain <code>${...}</code>-s.
     *            Examples: {@code "Line 1\nLine 2"} or {@code r"C:\temp"}.
     *        <li>A list literal (since 2.3.24) with FTL-like syntax, for example {@code [ 'foo', 2, true ]}.
     *            If the parameter is expected to be array, the list will be automatically converted to array.
     *            The list items can be any kind of expression, like even object builder expressions.
     *        <li>A map literal (since 2.3.24) with FTL-like syntax, for example <code>{ 'foo': 2, 'bar': true }</code>.
     *            The keys and values can be any kind of expression, like even object builder expressions.
     *            The resulting Java object will be a {@link Map} that keeps the item order ({@link LinkedHashMap} as
     *            of this writing).
     *        <li>A reference to a public static filed, like {@code Configuration.AUTO_DETECT} or
     *            {@code com.example.MyClass.MY_CONSTANT}.
     *        <li>An object builder expression. That is, object builder expressions can be nested into each other. 
     *      </ul>
     *   </li>
     *   <li>
     *     The same kind of expression as for parameters can also be used as top-level expressions (though it's
     *     rarely useful, apart from using {@code null}).
     *   </li>
     *   <li>
     *     <p>The top-level object builder expressions may omit {@code ()}.
     *   </li>
     *   <li>
     *     <p>The following classes can be referred to with simple (unqualified) name instead of fully qualified name:
     *     {@link DefaultObjectWrapper}, {@link DefaultObjectWrapper}, {@link RestrictedObjectWrapper}, {@link Locale},
     *     {@link TemplateConfiguration}, {@link PathGlobMatcher}, {@link FileNameGlobMatcher}, {@link PathRegexMatcher},
     *     {@link AndMatcher}, {@link OrMatcher}, {@link NotMatcher}, {@link ConditionalTemplateConfigurationFactory},
     *     {@link MergingTemplateConfigurationFactory}, {@link FirstMatchTemplateConfigurationFactory},
     *     {@link HTMLOutputFormat}, {@link XMLOutputFormat}, {@link RTFOutputFormat}, {@link PlainTextOutputFormat},
     *     {@link UndefinedOutputFormat}, {@link Configuration}, {@link TemplateLanguage}, {@link TagSyntax},
     *     {@link DefaultTruncateBuiltinAlgorithm}.
     *   </li>
     *   <li>
     *     <p>{@link TimeZone} objects can be created like {@code TimeZone("UTC")}, despite that there's no a such
     *     constructor.
     *   </li>
     *   <li>
     *     <p>{@link TemplateMarkupOutputModel} objects can be created like
     *     {@code markup(HTMLOutputFormat(), "<h1>Example</h1>")}. Of course the 1st argument can be any other
     *     {@link MarkupOutputFormat} too.
     *   </li>
     *   <li>
     *     <p>{@link Charset} objects can be created like {@code Charset("ISO-8859-5")}, despite that there's no a such
     *     constructor.
     *   </li>
     *   <li>
     *     <p>The classes and methods that the expression meant to access must be all public.
     *   </li>
     * </ul>
     * 
     * @param name the name of the setting.
     * @param value the string that describes the new value of the setting.
     * 
     * @throws InvalidSettingNameException if the name is wrong.
     * @throws InvalidSettingValueException if the new value of the setting can't be set for any other reasons.
     */
    public void setSetting(String name, String value) throws ConfigurationException {
        boolean unknown = false;
        try {
            if (LOCALE_KEY.equals(name)) {
                if (JVM_DEFAULT_VALUE.equalsIgnoreCase(value)) {
                    setLocale(Locale.getDefault());
                } else {
                    setLocale(_StringUtils.deduceLocale(value));
                }
            } else if (NUMBER_FORMAT_KEY.equals(name)) {
                setNumberFormat(value);
            } else if (CUSTOM_NUMBER_FORMATS_KEY.equals(name)) {
                Map map = (Map) _ObjectBuilderSettingEvaluator.eval(
                                value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                checkSettingValueItemsType("Map keys", String.class, map.keySet());
                checkSettingValueItemsType("Map values", TemplateNumberFormatFactory.class, map.values());
                setCustomNumberFormats(map);
            } else if (TIME_FORMAT_KEY.equals(name)) {
                setTimeFormat(value);
            } else if (DATE_FORMAT_KEY.equals(name)) {
                setDateFormat(value);
            } else if (DATE_TIME_FORMAT_KEY.equals(name)) {
                setDateTimeFormat(value);
            } else if (CUSTOM_DATE_FORMATS_KEY.equals(name)) {
                Map map = (Map) _ObjectBuilderSettingEvaluator.eval(
                                value, Map.class, false, _SettingEvaluationEnvironment.getCurrent());
                checkSettingValueItemsType("Map keys", String.class, map.keySet());
                checkSettingValueItemsType("Map values", TemplateDateFormatFactory.class, map.values());
                setCustomDateFormats(map);
            } else if (TIME_ZONE_KEY.equals(name)) {
                setTimeZone(parseTimeZoneSettingValue(value));
            } else if (SQL_DATE_AND_TIME_TIME_ZONE_KEY.equals(name)) {
                setSQLDateAndTimeTimeZone(value.equals("null") ? null : parseTimeZoneSettingValue(value));
            } else if (TEMPLATE_EXCEPTION_HANDLER_KEY.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("debug".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.DEBUG);
                    } else if ("htmlDebug".equals(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.HTML_DEBUG);
                    } else if ("ignore".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.IGNORE);
                    } else if ("rethrow".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.RETHROW);
                    } else if (DEFAULT_VALUE.equalsIgnoreCase(value)
                            && this instanceof Configuration.ExtendableBuilder) {
                        unsetTemplateExceptionHandler();
                    } else {
                        throw new InvalidSettingValueException(
                                name, value,
                                value.equalsIgnoreCase("html_debug")
                                        ? "The correct value would be: htmlDebug"
                                        : "No such predefined template exception handler name");
                    }
                } else {
                    setTemplateExceptionHandler((TemplateExceptionHandler) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateExceptionHandler.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (ATTEMPT_EXCEPTION_REPORTER_KEY.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("logError".equals(value)) {
                        setAttemptExceptionReporter(
                                AttemptExceptionReporter.LOG_ERROR);
                    } else if ("logWarn".equals(value)) {
                        setAttemptExceptionReporter(
                                AttemptExceptionReporter.LOG_WARN);
                    } else if (DEFAULT_VALUE.equalsIgnoreCase(value)
                            && this instanceof Configuration.ExtendableBuilder) {
                        unsetAttemptExceptionReporter();
                    } else {
                        throw new InvalidSettingValueException(
                                name, value,
                                value.equalsIgnoreCase("log_error") ? "The correct value would be: "
                                        + "logError"
                                : value.equalsIgnoreCase("log_wran") ? "The correct value would be: "
                                        + "logWarn"
                                : "No such predefined template exception handler name");
                    }
                } else {
                    setTemplateExceptionHandler((TemplateExceptionHandler) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateExceptionHandler.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (ARITHMETIC_ENGINE_KEY.equals(name)) {
                if (value.indexOf('.') == -1) { 
                    if ("bigdecimal".equalsIgnoreCase(value)) {
                        setArithmeticEngine(BigDecimalArithmeticEngine.INSTANCE);
                    } else if ("conservative".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ConservativeArithmeticEngine.INSTANCE);
                    } else {
                        throw new InvalidSettingValueException(
                                name, value, "No such predefined arithmetical engine name");
                    }
                } else {
                    setArithmeticEngine((ArithmeticEngine) _ObjectBuilderSettingEvaluator.eval(
                            value, ArithmeticEngine.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (BOOLEAN_FORMAT_KEY.equals(name)) {
                setBooleanFormat(value);
            } else if (C_FORMAT_KEY.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                    unsetCFormat();
                } else {
                    CFormat cFormat = StandardCFormats.STANDARD_C_FORMATS.get(value);
                    setCFormat(
                            cFormat != null ? cFormat
                                    : (CFormat) _ObjectBuilderSettingEvaluator.eval(
                                    value, CFormat.class, false, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (OUTPUT_ENCODING_KEY.equals(name)) {
                setOutputEncoding(Charset.forName(value));
            } else if (URL_ESCAPING_CHARSET_KEY.equals(name)) {
                setURLEscapingCharset(Charset.forName(value));
            } else if (AUTO_FLUSH_KEY.equals(name)) {
                setAutoFlush(_StringUtils.getYesNo(value));
            } else if (SHOW_ERROR_TIPS_KEY.equals(name)) {
                setShowErrorTips(_StringUtils.getYesNo(value));
            } else if (API_BUILTIN_ENABLED_KEY.equals(name)) {
                setAPIBuiltinEnabled(_StringUtils.getYesNo(value));
            } else if (TRUNCATE_BUILTIN_ALGORITHM_KEY.equals(name)) {
                if ("ascii".equalsIgnoreCase(value)) {
                    setTruncateBuiltinAlgorithm(DefaultTruncateBuiltinAlgorithm.ASCII_INSTANCE);
                } else if ("unicode".equalsIgnoreCase(value)) {
                    setTruncateBuiltinAlgorithm(DefaultTruncateBuiltinAlgorithm.UNICODE_INSTANCE);
                } else {
                    setTruncateBuiltinAlgorithm((TruncateBuiltinAlgorithm) _ObjectBuilderSettingEvaluator.eval(
                            value, TruncateBuiltinAlgorithm.class, false,
                            _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (NEW_BUILTIN_CLASS_RESOLVER_KEY.equals(name)) {
                if ("unrestricted".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.UNRESTRICTED);
                } else if ("allowNothing".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.ALLOW_NOTHING);
                } else if (value.indexOf(":") != -1) {
                    List<_KeyValuePair<String, List<String>>> segments = parseAsSegmentedList(value);
                    Set allowedClasses = null;
                    List<String> trustedTemplates = null;
                    for (_KeyValuePair<String, List<String>> segment : segments) {
                        String segmentKey = segment.getKey();
                        List<String> segmentValue = segment.getValue();
                        if (segmentKey.equals(ALLOWED_CLASSES)) {
                            allowedClasses = new HashSet(segmentValue);
                        } else if (segmentKey.equals(TRUSTED_TEMPLATES)) {
                            trustedTemplates = segmentValue;
                        } else {
                            throw new InvalidSettingValueException(name, value,
                                    "Unrecognized list segment key: " + _StringUtils.jQuote(segmentKey) +
                                            ". Supported keys are: \"" + ALLOWED_CLASSES + "\", \"" +
                                            TRUSTED_TEMPLATES + "\"");
                        }
                    }
                    setNewBuiltinClassResolver(
                            new OptInTemplateClassResolver(allowedClasses, trustedTemplates));
                } else if ("allowsNothing".equals(value) || "allows_nothing".equals(value)) {
                    throw new InvalidSettingValueException(
                            name, value, "The correct value would be: allowNothing");
                } else if (value.indexOf('.') != -1) {
                    setNewBuiltinClassResolver((TemplateClassResolver) _ObjectBuilderSettingEvaluator.eval(
                                    value, TemplateClassResolver.class, false,
                                    _SettingEvaluationEnvironment.getCurrent()));
                } else {
                    throw new InvalidSettingValueException(
                            name, value,
                            "Not predefined class resolved name, nor follows class resolver definition syntax, nor "
                            + "looks like class name");
                }
            } else if (LAZY_AUTO_IMPORTS_KEY.equals(name)) {
                setLazyAutoImports(value.equals(NULL_VALUE) ? null : Boolean.valueOf(_StringUtils.getYesNo(value)));
            } else if (LAZY_IMPORTS_KEY.equals(name)) {
                setLazyImports(_StringUtils.getYesNo(value));
            } else if (AUTO_INCLUDES_KEY.equals(name)) {
                setAutoIncludes(parseAsList(value));
            } else if (AUTO_IMPORTS_KEY.equals(name)) {
                setAutoImports(parseAsImportList(value));
            } else {
                unknown = true;
            }
        } catch (InvalidSettingValueException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSettingValueException(name, value, e);
        }
        if (unknown) {
            throw unknownSettingException(name);
        }
    }

    /**
     * Fluent API equivalent of {@link #setSetting(String, String)}.
     */
    public SelfT setting(String name, String value) throws ConfigurationException {
        setSetting(name, value);
        return self();
    }

    /**
     * @throws IllegalArgumentException
     *             if the type of the some of the values isn't as expected
     */
    private void checkSettingValueItemsType(String somethingsSentenceStart, Class<?> expectedClass,
                                                  Collection<?> values) {
        if (values == null) return;
        for (Object value : values) {
            if (!expectedClass.isInstance(value)) {
                throw new IllegalArgumentException(somethingsSentenceStart + " must be instances of "
                        + _ClassUtils.getShortClassName(expectedClass) + ", but one of them was a(n) "
                        + _ClassUtils.getShortClassNameOfObject(value) + ".");
            }
        }
    }

    /**
     * Returns the valid setting names for a {@link ProcessingConfiguration}.
     *
     * @see Configuration.ExtendableBuilder#getSettingNames()
     */
    public static Set<String> getSettingNames() {
        return SETTING_NAMES;
    }

    private TimeZone parseTimeZoneSettingValue(String value) {
        TimeZone tz;
        if (JVM_DEFAULT_VALUE.equalsIgnoreCase(value)) {
            tz = TimeZone.getDefault();
        } else {
            tz = TimeZone.getTimeZone(value);
        }
        return tz;
    }
    
    /**
     * Creates the exception that should be thrown when a setting name isn't recognized.
     */
    protected final InvalidSettingNameException unknownSettingException(String name) {
         Version removalVersion = getRemovalVersionForUnknownSetting(name);
         return removalVersion != null
                ? new InvalidSettingNameException(name, removalVersion)
                : new InvalidSettingNameException(name, getCorrectedNameForUnknownSetting(name));
    }

    /**
     * If a setting name is unknown because it was removed over time (not just renamed), then returns the version where
     * it was removed, otherwise returns {@code null}.
     */
    protected Version getRemovalVersionForUnknownSetting(String name) {
        if (name.equals("classic_compatible") || name.equals("classicCompatible")
                || name.equals("tag_syntax") || name.equals("tagSyntax")
                || name.equals("interpolation_syntax") || name.equals("interpolationSyntax")) {
            return Configuration.VERSION_3_0_0;
        }
        return null;
    }
    
    /**
     * @param name The wrong name
     * @return The corrected name, or {@code null} if there's no known correction
     */
    protected String getCorrectedNameForUnknownSetting(String name) {
        switch(name.toLowerCase()) {
            case "autoinclude":
            case "auto_include":
            case "auto_includes":
                return AUTO_INCLUDES_KEY;
            case "autoimport":
            case "auto_import":
            case "auto_imports":
                return AUTO_IMPORTS_KEY;
            case "datetimeformat":
            case "datetime_format":
            case "date_time_format":
                return DATE_TIME_FORMAT_KEY;
            default:
                return null;
        }
    }
    
    /**
     * Set the settings stored in a <code>Properties</code> object.
     * 
     * @throws ConfigurationException if the <code>Properties</code> object contains
     *     invalid keys, or invalid setting values, or any other error occurs
     *     while changing the settings.
     */    
    public void setSettings(Properties props) throws ConfigurationException {
        final _SettingEvaluationEnvironment prevEnv = _SettingEvaluationEnvironment.startScope();
        try {
            for (String key : props.stringPropertyNames()) {
                setSetting(key, props.getProperty(key).trim());
            }
        } finally {
            _SettingEvaluationEnvironment.endScope(prevEnv);
        }
    }

    /**
     * Fluent API equivalent of {@link #setSettings(Properties)}.
     */
    public SelfT settings(Properties props) {
        setSettings(props);
        return self();
    }

    /**
     * Setter pair of {@link #getCustomSetting(Serializable)}.
     *
     * @param key
     *         The identifier of the the custom setting; not {@code null}. Usually an enum or a {@link String}. Must
     *         be usable as {@link HashMap} key.
     * @param value
     *         The value of the custom setting. {@code null} is a legal attribute value. Thus, setting the value to
     *         {@code null} doesn't unset (remove) the attribute; use {@link #unsetCustomSetting(Serializable)} for
     *         that. Also, {@link #MISSING_VALUE_MARKER} is not an allowed value.
     *         The content of the object shouldn't be changed after it was added as an attribute (ideally, it should
     *         be a true immutable object); if you need to change the content, certainly you should use the
     *         {@link CustomStateScope} API.
     */
    public void setCustomSetting(Serializable key, Object value) {
        _NullArgumentException.check("key", key);
        if (value == MISSING_VALUE_MARKER) {
            throw new IllegalArgumentException("MISSING_VALUE_MARKER can't be used as custom setting value");
        }
        ensureCustomSettingsModifiable();
        customSettings.put(key, value);
    }

    /**
     * Fluent API equivalent of {@link #setCustomSetting(Serializable, Object)}
     */
    public SelfT customSetting(Serializable key, Object value) {
        setCustomSetting(key, value);
        return self();
    }

    @Override
    public boolean isCustomSettingSet(Serializable key) {
        return customSettings.containsKey(key);
    }

    /**
     * Unset the custom setting for this {@link ProcessingConfiguration} (but not from the parent
     * {@link ProcessingConfiguration}, from where it will be possibly inherited after this), as if
     * {@link #setCustomSetting(Serializable, Object)} was never called for it on this
     * {@link ProcessingConfiguration}. Note that this is different than setting the custom setting value to {@code
     * null}, as then {@link #getCustomSetting(Serializable)} will just return that {@code null}, and won't look for the
     * attribute in the parent {@link ProcessingConfiguration}.
     *
     * @param key As in {@link #getCustomSetting(Serializable)}
     */
    public void unsetCustomSetting(Serializable key) {
        if (customSettingsModifiable) {
            customSettings.remove(key);
        } else if (customSettings.containsKey(key)) {
            ensureCustomSettingsModifiable();
            customSettings.remove(key);
        }
    }

    @Override
    public Object getCustomSetting(Serializable key) throws CustomSettingValueNotSetException {
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
        return getDefaultCustomSetting(key, defaultValue, useDefaultValue);
    }

    @Override
    public Map<Serializable, Object> getCustomSettings(boolean includeInherited) {
        if (includeInherited) {
            LinkedHashMap<Serializable, Object> result = new LinkedHashMap<>();
            collectDefaultCustomSettingsSnapshot(result);
            if (!result.isEmpty()) {
                if (customSettings != null) {
                    result.putAll(customSettings);
                }
                return Collections.unmodifiableMap(result);
            }
        }

        // When there's no need for inheritance:
        customSettingsModifiable = false; // Copy-on-write on next modification
        return _CollectionUtils.unmodifiableMap(customSettings);
    }

    /**
     * Called from {@link #getCustomSettings(boolean)}, adds the default (such as inherited) custom settings
     * to the argument {@link Map}.
     */
    protected abstract void collectDefaultCustomSettingsSnapshot(Map<Serializable, Object> target);

    private void ensureCustomSettingsModifiable() {
        if (!customSettingsModifiable) {
            customSettings = new LinkedHashMap<>(customSettings);
            customSettingsModifiable = true;
        }
    }

    /**
     * Called be {@link #getCustomSetting(Serializable)} and {@link #getCustomSetting(Serializable, Object)} if the
     * attribute wasn't set in the current {@link ProcessingConfiguration}.
     *
     * @param useDefaultValue
     *         If {@code true}, and the attribute is missing, then return {@code defaultValue}, otherwise throw {@link
     *         CustomSettingValueNotSetException}.
     *
     * @throws CustomSettingValueNotSetException
     *         if the attribute wasn't set in the parents, or has no default otherwise, and {@code useDefaultValue} was
     *         {@code false}.
     */
    protected abstract Object getDefaultCustomSetting(
            Serializable key, Object defaultValue, boolean useDefaultValue) throws CustomSettingValueNotSetException;

    /**
     * Convenience method for calling {@link #setCustomSetting(Serializable, Object)} for each {@link Map} entry.
     * Note that it won't remove the already existing custom settings.
     */
    public void setCustomSettings(Map<? extends Serializable, ?> customSettings) {
        _NullArgumentException.check("customSettings", customSettings);
        for (Object value : customSettings.values()) {
            if (value == MISSING_VALUE_MARKER) {
                throw new IllegalArgumentException("MISSING_VALUE_MARKER can't be used as attribute value");
            }
        }

        ensureCustomSettingsModifiable();
        this.customSettings.putAll(customSettings);
        customSettingsModifiable = true;
    }

    /**
     * Fluent API equivalent of {@link #setCustomSettings(Map)}
     */
    public SelfT customSettings(Map<Serializable, Object> customSettings) {
        setCustomSettings(customSettings);
        return self();
    }

    /**
     * Used internally to avoid copying the {@link Map} when we know that its content won't change anymore.
     */
    void setCustomSettingsMap(Map<Serializable, Object> customSettings) {
        _NullArgumentException.check("customSettings", customSettings);
        this.customSettings = customSettings;
        this.customSettingsModifiable = false;
    }

    /**
     * Unsets all custom settings which were set in this {@link ProcessingConfiguration} (but doesn't unset
     * those inherited from a parent {@link ProcessingConfiguration}).
     */
    public void unsetAllCustomSettings() {
        customSettings = Collections.emptyMap();
        customSettingsModifiable = false;
    }

    protected final List<String> parseAsList(String text) throws GenericParseException {
        return new SettingStringParser(text).parseAsList();
    }

    protected final List<_KeyValuePair<String, List<String>>> parseAsSegmentedList(String text)
    throws GenericParseException {
        return new SettingStringParser(text).parseAsSegmentedList();
    }
    
    private final HashMap parseAsImportList(String text) throws GenericParseException {
        return new SettingStringParser(text).parseAsImportList();
    }

    @SuppressWarnings("unchecked")
    protected SelfT self() {
        return (SelfT) this;
    }
    
    /**
     * Helper class for parsing setting values given with string.
     */
    private static class SettingStringParser {
        private String text;
        private int p;
        private int ln;

        private SettingStringParser(String text) {
            this.text = text;
            p = 0;
            ln = text.length();
        }

        List<_KeyValuePair<String, List<String>>> parseAsSegmentedList() throws GenericParseException {
            List<_KeyValuePair<String, List<String>>> segments = new ArrayList();
            List<String> currentSegment = null;
            
            char c;
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String item = fetchStringValue();
                c = skipWS();
                
                if (c == ':') {
                    currentSegment = new ArrayList();
                    segments.add(new _KeyValuePair<>(item, currentSegment));
                } else {
                    if (currentSegment == null) {
                        throw new GenericParseException(
                                "The very first list item must be followed by \":\" so " +
                                "it will be the key for the following sub-list.");
                    }
                    currentSegment.add(item);
                }
                
                if (c == ' ') break;
                if (c != ',' && c != ':') throw new GenericParseException(
                        "Expected \",\" or \":\" or the end of text but " +
                        "found \"" + c + "\"");
                p++;
            }
            return segments;
        }

        ArrayList parseAsList() throws GenericParseException {
            char c;
            ArrayList seq = new ArrayList();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                seq.add(fetchStringValue());
                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new GenericParseException(
                        "Expected \",\" or the end of text but " +
                        "found \"" + c + "\"");
                p++;
            }
            return seq;
        }

        HashMap parseAsImportList() throws GenericParseException {
            char c;
            HashMap map = new HashMap();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String lib = fetchStringValue();

                c = skipWS();
                if (c == ' ') throw new GenericParseException(
                        "Unexpected end of text: expected \"as\"");
                String s = fetchKeyword();
                if (!s.equalsIgnoreCase("as")) throw new GenericParseException(
                        "Expected \"as\", but found " + _StringUtils.jQuote(s));

                c = skipWS();
                if (c == ' ') throw new GenericParseException(
                        "Unexpected end of text: expected gate hash name");
                String ns = fetchStringValue();
                
                map.put(ns, lib);

                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new GenericParseException(
                        "Expected \",\" or the end of text but "
                        + "found \"" + c + "\"");
                p++;
            }
            return map;
        }

        String fetchStringValue() throws GenericParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                w = w.substring(1, w.length() - 1);
            }
            return TemplateLanguageUtils.unescapeStringLiteralPart(w);
        }

        String fetchKeyword() throws GenericParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                throw new GenericParseException(
                    "Keyword expected, but a string value found: " + w);
            }
            return w;
        }

        char skipWS() {
            char c;
            while (p < ln) {
                c = text.charAt(p);
                if (!Character.isWhitespace(c)) return c;
                p++;
            }
            return ' ';
        }

        private String fetchWord() throws GenericParseException {
            if (p == ln) throw new GenericParseException(
                    "Unexpeced end of text");

            char c = text.charAt(p);
            int b = p;
            if (c == '\'' || c == '"') {
                boolean escaped = false;
                char q = c;
                p++;
                while (p < ln) {
                    c = text.charAt(p);
                    if (!escaped) {
                        if (c == '\\') {
                            escaped = true;
                        } else if (c == q) {
                            break;
                        }
                    } else {
                        escaped = false;
                    }
                    p++;
                }
                if (p == ln) {
                    throw new GenericParseException("Missing " + q);
                }
                p++;
                return text.substring(b, p);
            } else {
                do {
                    c = text.charAt(p);
                    if (!(Character.isLetterOrDigit(c)
                            || c == '/' || c == '\\' || c == '_'
                            || c == '.' || c == '-' || c == '!'
                            || c == '*' || c == '?')) break;
                    p++;
                } while (p < ln);
                if (b == p) {
                    throw new GenericParseException("Unexpected character: " + c);
                } else {
                    return text.substring(b, p);
                }
            }
        }
    }
    
}

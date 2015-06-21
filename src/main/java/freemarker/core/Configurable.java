/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.Version;
import freemarker.template._TemplateAPI;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.StringUtil;

/**
 * This is a common superclass of {@link freemarker.template.Configuration},
 * {@link freemarker.template.Template}, and {@link Environment} classes.
 * It provides settings that are common to each of them. FreeMarker
 * uses a three-level setting hierarchy - the return value of every setting
 * getter method on <code>Configurable</code> objects inherits its value from its parent 
 * <code>Configurable</code> object, unless explicitly overridden by a call to a 
 * corresponding setter method on the object itself. The parent of an 
 * <code>Environment</code> object is a <code>Template</code> object, the
 * parent of a <code>Template</code> object is a <code>Configuration</code>
 * object.
 */
public class Configurable
{
    static final String C_TRUE_FALSE = "true,false";
    
    private static final String DEFAULT = "default";
    private static final String DEFAULT_2_3_0 = "default_2_3_0";
    private static final String JVM_DEFAULT = "JVM default";
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALE_KEY_SNAKE_CASE = "locale";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String LOCALE_KEY_CAMEL_CASE = "locale";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String LOCALE_KEY = LOCALE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String NUMBER_FORMAT_KEY_SNAKE_CASE = "number_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String NUMBER_FORMAT_KEY_CAMEL_CASE = "numberFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String NUMBER_FORMAT_KEY = NUMBER_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_FORMAT_KEY_SNAKE_CASE = "time_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_FORMAT_KEY_CAMEL_CASE = "timeFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TIME_FORMAT_KEY = TIME_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String DATE_FORMAT_KEY_SNAKE_CASE = "date_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String DATE_FORMAT_KEY_CAMEL_CASE = "dateFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String DATE_FORMAT_KEY = DATE_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String DATETIME_FORMAT_KEY_SNAKE_CASE = "datetime_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String DATETIME_FORMAT_KEY_CAMEL_CASE = "datetimeFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String DATETIME_FORMAT_KEY = DATETIME_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_ZONE_KEY_SNAKE_CASE = "time_zone";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TIME_ZONE_KEY_CAMEL_CASE = "timeZone";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TIME_ZONE_KEY = TIME_ZONE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE = "sql_date_and_time_time_zone";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE = "sqlDateAndTimeTimeZone";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String SQL_DATE_AND_TIME_TIME_ZONE_KEY = SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String CLASSIC_COMPATIBLE_KEY_SNAKE_CASE = "classic_compatible";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String CLASSIC_COMPATIBLE_KEY_CAMEL_CASE = "classicCompatible";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String CLASSIC_COMPATIBLE_KEY = CLASSIC_COMPATIBLE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE = "template_exception_handler";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE = "templateExceptionHandler";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY = TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String ARITHMETIC_ENGINE_KEY_SNAKE_CASE = "arithmetic_engine";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String ARITHMETIC_ENGINE_KEY_CAMEL_CASE = "arithmeticEngine";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String ARITHMETIC_ENGINE_KEY = ARITHMETIC_ENGINE_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String OBJECT_WRAPPER_KEY_SNAKE_CASE = "object_wrapper";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String OBJECT_WRAPPER_KEY_CAMEL_CASE = "objectWrapper";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String OBJECT_WRAPPER_KEY = OBJECT_WRAPPER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String BOOLEAN_FORMAT_KEY_SNAKE_CASE = "boolean_format";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String BOOLEAN_FORMAT_KEY_CAMEL_CASE = "booleanFormat";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String BOOLEAN_FORMAT_KEY = BOOLEAN_FORMAT_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String OUTPUT_ENCODING_KEY_SNAKE_CASE = "output_encoding";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String OUTPUT_ENCODING_KEY_CAMEL_CASE = "outputEncoding";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String OUTPUT_ENCODING_KEY = OUTPUT_ENCODING_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String URL_ESCAPING_CHARSET_KEY_SNAKE_CASE = "url_escaping_charset";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String URL_ESCAPING_CHARSET_KEY_CAMEL_CASE = "urlEscapingCharset";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. */
    public static final String URL_ESCAPING_CHARSET_KEY = URL_ESCAPING_CHARSET_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String STRICT_BEAN_MODELS_KEY_SNAKE_CASE = "strict_bean_models";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String STRICT_BEAN_MODELS_KEY_CAMEL_CASE = "strictBeanModels";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.22 */
    public static final String STRICT_BEAN_MODELS_KEY = STRICT_BEAN_MODELS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_FLUSH_KEY_SNAKE_CASE = "auto_flush";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String AUTO_FLUSH_KEY_CAMEL_CASE = "autoFlush";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.17 */
    public static final String AUTO_FLUSH_KEY = AUTO_FLUSH_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE = "new_builtin_class_resolver";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE = "newBuiltinClassResolver";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.17 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY = NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String SHOW_ERROR_TIPS_KEY_SNAKE_CASE = "show_error_tips";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String SHOW_ERROR_TIPS_KEY_CAMEL_CASE = "showErrorTips";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.21 */
    public static final String SHOW_ERROR_TIPS_KEY = SHOW_ERROR_TIPS_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String API_BUILTIN_ENABLED_KEY_SNAKE_CASE = "api_builtin_enabled";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String API_BUILTIN_ENABLED_KEY_CAMEL_CASE = "apiBuiltinEnabled";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.22 */
    public static final String API_BUILTIN_ENABLED_KEY = API_BUILTIN_ENABLED_KEY_SNAKE_CASE;
    
    /** Legacy, snake case ({@code like_this}) variation of the setting name. @since 2.3.23 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE = "log_template_exceptions";
    /** Modern, camel case ({@code likeThis}) variation of the setting name. @since 2.3.23 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE = "logTemplateExceptions";
    /** Alias to the {@code ..._SNAKE_CASE} variation due to backward compatibility constraints. @since 2.3.22 */
    public static final String LOG_TEMPLATE_EXCEPTIONS_KEY = LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE;
    
    /** @deprecated Use {@link #STRICT_BEAN_MODELS_KEY} instead. */
    public static final String STRICT_BEAN_MODELS = STRICT_BEAN_MODELS_KEY;
    
    private static final String[] SETTING_NAMES_SNAKE_CASE = new String[] {
        // Must be sorted alphabetically!
        API_BUILTIN_ENABLED_KEY_SNAKE_CASE,
        ARITHMETIC_ENGINE_KEY_SNAKE_CASE,
        AUTO_FLUSH_KEY_SNAKE_CASE,
        BOOLEAN_FORMAT_KEY_SNAKE_CASE,
        CLASSIC_COMPATIBLE_KEY_SNAKE_CASE,
        DATE_FORMAT_KEY_SNAKE_CASE,
        DATETIME_FORMAT_KEY_SNAKE_CASE,
        LOCALE_KEY_SNAKE_CASE,
        LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE,
        NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE,
        NUMBER_FORMAT_KEY_SNAKE_CASE,
        OBJECT_WRAPPER_KEY_SNAKE_CASE,
        OUTPUT_ENCODING_KEY_SNAKE_CASE,
        SHOW_ERROR_TIPS_KEY_SNAKE_CASE,
        SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE,
        STRICT_BEAN_MODELS_KEY,
        TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE,
        TIME_FORMAT_KEY_SNAKE_CASE,
        TIME_ZONE_KEY_SNAKE_CASE,
        URL_ESCAPING_CHARSET_KEY_SNAKE_CASE
    };
    
    private static final String[] SETTING_NAMES_CAMEL_CASE = new String[] {
        // Must be sorted alphabetically!
        API_BUILTIN_ENABLED_KEY_CAMEL_CASE,
        ARITHMETIC_ENGINE_KEY_CAMEL_CASE,
        AUTO_FLUSH_KEY_CAMEL_CASE,
        BOOLEAN_FORMAT_KEY_CAMEL_CASE,
        CLASSIC_COMPATIBLE_KEY_CAMEL_CASE,
        DATE_FORMAT_KEY_CAMEL_CASE,
        DATETIME_FORMAT_KEY_CAMEL_CASE,
        LOCALE_KEY_CAMEL_CASE,
        LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE,
        NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE,
        NUMBER_FORMAT_KEY_CAMEL_CASE,
        OBJECT_WRAPPER_KEY_CAMEL_CASE,
        OUTPUT_ENCODING_KEY_CAMEL_CASE,
        SHOW_ERROR_TIPS_KEY_CAMEL_CASE,
        SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE,
        STRICT_BEAN_MODELS_KEY_CAMEL_CASE,
        TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE,
        TIME_FORMAT_KEY_CAMEL_CASE,
        TIME_ZONE_KEY_CAMEL_CASE,
        URL_ESCAPING_CHARSET_KEY_CAMEL_CASE
    };

    private Configurable parent;
    private Properties properties;
    private HashMap customAttributes;
    
    private Locale locale;
    private String numberFormat;
    private String timeFormat;
    private String dateFormat;
    private String dateTimeFormat;
    private TimeZone timeZone;
    private TimeZone sqlDataAndTimeTimeZone;
    private boolean sqlDataAndTimeTimeZoneSet;
    private String booleanFormat;
    private String trueStringValue;  // deduced from booleanFormat
    private String falseStringValue;  // deduced from booleanFormat
    private Integer classicCompatible;
    private TemplateExceptionHandler templateExceptionHandler;
    private ArithmeticEngine arithmeticEngine;
    private ObjectWrapper objectWrapper;
    private String outputEncoding;
    private boolean outputEncodingSet;
    private String urlEscapingCharset;
    private boolean urlEscapingCharsetSet;
    private Boolean autoFlush;
    private TemplateClassResolver newBuiltinClassResolver;
    private Boolean showErrorTips;
    private Boolean apiBuiltinEnabled;
    private Boolean logTemplateExceptions;
    
    /**
     * Creates a top-level configurable, one that doesn't inherit from a parent, and thus stores the default values.
     * 
     * @deprecated This shouldn't even be public; don't use it.
     */
    public Configurable() {
        this(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    }

    /**
     * Intended to be called from inside FreeMarker only.
     * Creates a top-level configurable, one that doesn't inherit from a parent, and thus stores the default values.
     * Called by the {@link Configuration} constructor.
     */
    protected Configurable(Version incompatibleImprovements) {
        _TemplateAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        
        parent = null;
        properties = new Properties();
        
        locale = Locale.getDefault();
        properties.setProperty(LOCALE_KEY, locale.toString());
        
        timeZone = TimeZone.getDefault();
        properties.setProperty(TIME_ZONE_KEY, timeZone.getID());
        
        sqlDataAndTimeTimeZone = null;
        properties.setProperty(SQL_DATE_AND_TIME_TIME_ZONE_KEY, String.valueOf(sqlDataAndTimeTimeZone));
        
        numberFormat = "number";
        properties.setProperty(NUMBER_FORMAT_KEY, numberFormat);
        
        timeFormat = "";
        properties.setProperty(TIME_FORMAT_KEY, timeFormat);
        
        dateFormat = "";
        properties.setProperty(DATE_FORMAT_KEY, dateFormat);
        
        dateTimeFormat = "";
        properties.setProperty(DATETIME_FORMAT_KEY, dateTimeFormat);
        
        classicCompatible = new Integer(0);
        properties.setProperty(CLASSIC_COMPATIBLE_KEY, classicCompatible.toString());
        
        templateExceptionHandler = _TemplateAPI.getDefaultTemplateExceptionHandler(
                incompatibleImprovements);
        properties.setProperty(TEMPLATE_EXCEPTION_HANDLER_KEY, templateExceptionHandler.getClass().getName());
        
        arithmeticEngine = ArithmeticEngine.BIGDECIMAL_ENGINE;
        properties.setProperty(ARITHMETIC_ENGINE_KEY, arithmeticEngine.getClass().getName());
        
        objectWrapper = Configuration.getDefaultObjectWrapper(incompatibleImprovements);
        // bug: setProperty missing
        
        autoFlush = Boolean.TRUE;
        properties.setProperty(AUTO_FLUSH_KEY, autoFlush.toString());
        
        newBuiltinClassResolver = TemplateClassResolver.UNRESTRICTED_RESOLVER;
        properties.setProperty(NEW_BUILTIN_CLASS_RESOLVER_KEY, newBuiltinClassResolver.getClass().getName());
        
        showErrorTips = Boolean.TRUE;
        properties.setProperty(SHOW_ERROR_TIPS_KEY, showErrorTips.toString());
        
        apiBuiltinEnabled = Boolean.FALSE;
        properties.setProperty(API_BUILTIN_ENABLED_KEY, apiBuiltinEnabled.toString());
        
        logTemplateExceptions = Boolean.valueOf(
                _TemplateAPI.getDefaultLogTemplateExceptions(incompatibleImprovements));
        properties.setProperty(LOG_TEMPLATE_EXCEPTIONS_KEY, logTemplateExceptions.toString());
        
        // outputEncoding and urlEscapingCharset defaults to null,
        // which means "not specified"

        setBooleanFormat(C_TRUE_FALSE);
        
        customAttributes = new HashMap();
    }

    /**
     * Creates a new instance. Normally you do not need to use this constructor,
     * as you don't use <code>Configurable</code> directly, but its subclasses.
     */
    public Configurable(Configurable parent) {
        this.parent = parent;
        locale = null;
        numberFormat = null;
        classicCompatible = null;
        templateExceptionHandler = null;
        properties = new Properties(parent.properties);
        customAttributes = new HashMap();
    }
    
    protected Object clone() throws CloneNotSupportedException {
        Configurable copy = (Configurable)super.clone();
        copy.properties = new Properties(properties);
        copy.customAttributes = (HashMap)customAttributes.clone();
        return copy;
    }
    
    /**
     * Returns the parent {@link Configurable} object of this object. The parent stores the default setting values for
     * this {@link Configurable}. For example, the parent of a {@link freemarker.template.Template} object is a
     * {@link Configuration} object, so values not specified on {@link Template}-level are get from the
     * {@link Configuration} object.
     * 
     * <p>
     * Note on the parent of {@link Environment}: If you set {@link Configuration#setIncompatibleImprovements(Version)
     * incompatible_improvements} to at least 2.3.22, it will be always the "main" {@link Template}, that is, the
     * template for whose processing the {@link Environment} was created. With lower {@code incompatible_improvements},
     * the current parent can temporary change <em>during template execution</em>, for example when your are inside an
     * {@code #include}-d template (among others). Thus, don't build on which {@link Template} the parent of
     * {@link Environment} is during template execution, unless you set {@code incompatible_improvements} to 2.3.22 or
     * higher.
     *
     * @return The parent {@link Configurable} object, or {@code null} if this is the root {@link Configurable} object
     *         (i.e, if it's the {@link Configuration} object).
     */
    public final Configurable getParent() {
        return parent;
    }
    
    /**
     * Reparenting support. This is used by Environment when it includes a
     * template - the included template becomes the parent configurable during
     * its evaluation.
     */
    final void setParent(Configurable parent) {
        this.parent = parent;
    }
    
    /**
     * Toggles the "Classic Compatible" mode. For a comprehensive description
     * of this mode, see {@link #isClassicCompatible()}.
     */
    public void setClassicCompatible(boolean classicCompatibility) {
        this.classicCompatible = new Integer(classicCompatibility ? 1 : 0);
        properties.setProperty(CLASSIC_COMPATIBLE_KEY, classicCompatibilityIntToString(classicCompatible));
    }

    /**
     * Same as {@link #setClassicCompatible(boolean)}, but allows some extra values. 
     * 
     * @param classicCompatibility {@code 0} means {@code false}, {@code 1} means {@code true},
     *     {@code 2} means {@code true} but with emulating bugs in early 2.x classic-compatibility mode. Currently
     *     {@code 2} affects how booleans are converted to string; with {@code 1} it's always {@code "true"}/{@code ""},
     *     but with {@code 2} it's {@code "true"}/{@code "false"} for values wrapped by {@link BeansWrapper} as then
     *     {@link Boolean#toString()} prevails. Note that {@code someBoolean?string} will always consistently format the
     *     boolean according the {@code boolean_format} setting, just like in FreeMarker 2.3 and later.
     */
    public void setClassicCompatibleAsInt(int classicCompatibility) {
        if (classicCompatibility < 0 || classicCompatibility > 2) {
            throw new IllegalArgumentException("Unsupported \"classicCompatibility\": " + classicCompatibility);
        }
        this.classicCompatible = new Integer(classicCompatibility);
    }
    
    private String classicCompatibilityIntToString(Integer i) {
        if (i == null) return null;
        else if (i.intValue() == 0) return MiscUtil.C_FALSE;
        else if (i.intValue() == 1) return MiscUtil.C_TRUE;
        else return i.toString();
    }
    
    /**
     * Returns whether the engine runs in the "Classic Compatibile" mode.
     * When this mode is active, the engine behavior is altered in following
     * way: (these resemble the behavior of the 1.7.x line of FreeMarker engine,
     * now named "FreeMarker Classic", hence the name).
     * <ul>
     * <li>handle undefined expressions gracefully. Namely when an expression
     *   "expr" evaluates to null:
     *   <ul>
     *     <li>
     *       in <tt>&lt;assign varname=expr&gt;</tt> directive, 
     *       or in <tt>${expr}</tt> directive,
     *       or in <tt>otherexpr == expr</tt>,
     *       or in <tt>otherexpr != expr</tt>, 
     *       or in <tt>hash[expr]</tt>,
     *       or in <tt>expr[keyOrIndex]</tt> (since 2.3.20),
     *       or in <tt>expr.key</tt> (since 2.3.20),
     *       then it's treated as empty string.
     *     </li>
     *     <li>as argument of <tt>&lt;list expr as item&gt;</tt> or 
     *       <tt>&lt;foreach item in expr&gt;</tt>, the loop body is not executed
     *       (as if it were a 0-length list)
     *     </li>
     *     <li>as argument of <tt>&lt;if&gt;</tt> directive, or on other places where a
     *       boolean expression is expected, it's treated as false
     *     </li>
     *   </ul>
     * </li>
     * <li>Non-boolean models are accepted in <tt>&lt;if&gt;</tt> directive,
     *   or as operands of logical operators. "Empty" models (zero-length string,
     * empty sequence or hash) are evaluated as false, all others are evaluated as
     * true.</li>
     * <li>When boolean value is treated as a string (i.e. output in 
     *   <tt>${...}</tt> directive, or concatenated with other string), true 
     * values are converted to string "true", false values are converted to 
     * empty string. Except, if the value of the setting is <tt>2</tt>, it will be
     * formatted according the <tt>boolean_format</tt> setting, just like in
     * 2.3.20 and later.
     * </li>
     * <li>Scalar models supplied to <tt>&lt;list&gt;</tt> and 
     *   <tt>&lt;foreach&gt;</tt> are treated as a one-element list consisting
     *   of the passed model.
     * </li>
     * <li>Paths parameter of <tt>&lt;include&gt;</tt> will be interpreted as
     * absolute path.
     * </li>
     * </ul>
     * In all other aspects, the engine is a 2.1 engine even in compatibility
     * mode - you don't lose any of the new functionality by enabling it.
     */
    public boolean isClassicCompatible() {
        return classicCompatible != null ? classicCompatible.intValue() != 0 : parent.isClassicCompatible();
    }

    public int getClassicCompatibleAsInt() {
        return classicCompatible != null ? classicCompatible.intValue() : parent.getClassicCompatibleAsInt();
    }
    
    /**
     * Sets the default locale used for number and date formatting (among others), also the locale used for searching
     * localized template variations when no locale was explicitly requested.
     * 
     * @see Configuration#getTemplate(String, Locale)
     */
    public void setLocale(Locale locale) {
        NullArgumentException.check("locale", locale);
        this.locale = locale;
        properties.setProperty(LOCALE_KEY, locale.toString());
    }

    /**
     * The getter pair of {@link #setTimeZone(TimeZone)}. 
     */
    public TimeZone getTimeZone() {
        return timeZone != null ? timeZone : parent.getTimeZone();
    }

    /**
     * Sets the time zone to use when formatting date/time values.
     * Defaults to the system time zone ({@link TimeZone#getDefault()}), regardless of the "locale" FreeMarker setting,
     * so in a server application you probably want to set it explicitly in the {@link Environment} to match the
     * preferred time zone of target audience (like the Web page visitor).
     * 
     * <p>If you or the templates set the time zone, you should probably also set
     * {@link #setSQLDateAndTimeTimeZone(TimeZone)}!
     * 
     * @see #setSQLDateAndTimeTimeZone(TimeZone)
     */
    public void setTimeZone(TimeZone timeZone) {
        NullArgumentException.check("timeZone", timeZone);
        this.timeZone = timeZone;
        properties.setProperty(TIME_ZONE_KEY, timeZone.getID());
    }

    /**
     * Sets the time zone used when dealing with {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. It defaults to {@code null} for backward compatibility, but in most
     * application this should be set to the JVM default time zone (server default time zone), because that's what
     * most JDBC drivers will use when constructing the {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. If this setting is {@code null}, FreeMarker will use the value of
     * ({@link #getTimeZone()}) for {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values,
     * which often gives bad results.
     * 
     * <p>This setting doesn't influence the formatting of other kind of values (like of
     * {@link java.sql.Timestamp java.sql.Timestamp} or plain {@link java.util.Date java.util.Date} values).
     * 
     * <p>To decide what value you need, a few things has to be understood:
     * <ul>
     *   <li>Date-only and time-only values in SQL-oriented databases are usually store calendar and clock field
     *   values directly (year, month, day, or hour, minute, seconds (with decimals)), as opposed to a set of points
     *   on the physical time line. Thus, unlike SQL timestamps, these values usually aren't meant to be shown
     *   differently depending on the time zone of the audience.
     *   
     *   <li>When a JDBC query has to return a date-only or time-only value, it has to convert it to a point on the
     *   physical time line, because that's what {@link java.util.Date} and its subclasses store (milliseconds since
     *   the epoch). Obviously, this is impossible to do. So JDBC just chooses a physical time which, when rendered
     *   <em>with the JVM default time zone</em>, will give the same field values as those stored
     *   in the database. (Actually, you can give JDBC a calendar, and so it can use other time zones too, but most
     *   application won't care using those overloads.) For example, assume that the system time zone is GMT+02:00.
     *   Then, 2014-07-12 in the database will be translated to physical time 2014-07-11 22:00:00 UTC, because that
     *   rendered in GMT+02:00 gives 2014-07-12 00:00:00. Similarly, 11:57:00 in the database will be translated to
     *   physical time 1970-01-01 09:57:00 UTC. Thus, the physical time stored in the returned value depends on the
     *   default system time zone of the JDBC client, not just on the content in the database. (This used to be the
     *   default behavior of ORM-s, like Hibernate, too.)
     *   
     *   <li>The value of the {@code time_zone} FreeMarker configuration setting sets the time zone used for the
     *   template output. For example, when a web page visitor has a preferred time zone, the web application framework
     *   may calls {@link Environment#setTimeZone(TimeZone)} with that time zone. Thus, the visitor will
     *   see {@link java.sql.Timestamp java.sql.Timestamp} and plain {@link java.util.Date java.util.Date} values as
     *   they look in his own time zone. While
     *   this is desirable for those types, as they meant to represent physical points on the time line, this is not
     *   necessarily desirable for date-only and time-only values. When {@code sql_date_and_time_time_zone} is
     *   {@code null}, {@code time_zone} is used for rendering all kind of date/time/dateTime values, including
     *   {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time}, and then if, for example,
     *   {@code time_zone} is GMT+00:00, the
     *   values from the earlier examples will be shown as 2014-07-11 (one day off) and 09:57:00 (2 hours off). While
     *   those are the time zone correct renderings, those values probably was meant to shown "as is".
     *   
     *   <li>You may wonder why this setting isn't simply "SQL time zone", since the time zone related behavior of JDBC
     *   applies to {@link java.sql.Timestamp java.sql.Timestamp} too. FreeMarker assumes that you have set up your
     *   application so that time stamps coming from the database go through the necessary conversion to store the
     *   correct distance from the epoch (1970-01-01 00:00:00 UTC), as requested by {@link java.util.Date}. In that case
     *   the time stamp can be safely rendered in different time zones, and thus it needs no special treatment.
     * </ul>
     * 
     * @param tz Maybe {@code null}, in which case {@link java.sql.Date java.sql.Date} and
     *          {@link java.sql.Time java.sql.Time} values will be formatted in the time zone returned by
     *          {@link #getTimeZone()}.
     *          (Note that since {@code null} is an allowed value for this setting, it will not cause
     *          {@link #getSQLDateAndTimeTimeZone()} to fall back to the parent configuration.)
     * 
     * @see #setTimeZone(TimeZone)
     * 
     * @since 2.3.21
     */
    public void setSQLDateAndTimeTimeZone(TimeZone tz) {
        sqlDataAndTimeTimeZone = tz;
        sqlDataAndTimeTimeZoneSet = true;
        properties.setProperty(SQL_DATE_AND_TIME_TIME_ZONE_KEY, tz != null ? tz.getID() : "null");
    }
    
    /**
     * The getter pair of {@link #setSQLDateAndTimeTimeZone(TimeZone)}.
     * 
     * @return {@code null} if the value of {@link #getTimeZone()} should be used for formatting
     *     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values, otherwise the time zone
     *     that should be used to format the values of those two types.  
     * 
     * @since 2.3.21
     */
    public TimeZone getSQLDateAndTimeTimeZone() {
        return sqlDataAndTimeTimeZoneSet
                ? sqlDataAndTimeTimeZone
                : (parent != null ? parent.getSQLDateAndTimeTimeZone() : null);
    }

    /**
     * Returns the assumed locale when searching for template files with no
     * explicit requested locale. Defaults to system locale.
     */
    public Locale getLocale() {
        return locale != null ? locale : parent.getLocale();
    }

    /**
     * Sets the default number format used to convert numbers to strings. Currently, this is either a
     * {@link java.text.DecimalFormat} pattern (like {@code "0.##"}), or one of the following special values:
     * <ul>
     *   <li>{@code "number"}: The number format returned by {@link NumberFormat#getNumberInstance(Locale)}</li>
     *   <li>{@code "currency"}: The number format returned by {@link NumberFormat#getCurrencyInstance(Locale)}</li>
     *   <li>{@code "percent"}: The number format returned by {@link NumberFormat#getPercentInstance(Locale)}</li>
     *   <li>{@code "computer"}: The number format used by FTL's {@code c} built-in (like in {@code someNumber?c}).</li>
     * </ul>
     * <p>Defaults to <tt>"number"</tt>.
     */
    public void setNumberFormat(String numberFormat) {
        NullArgumentException.check("numberFormat", numberFormat);
        this.numberFormat = numberFormat;
        properties.setProperty(NUMBER_FORMAT_KEY, numberFormat);
    }

    /**
     * Getter pair of {@link #setNumberFormat(String)}. 
     */
    public String getNumberFormat() {
        return numberFormat != null ? numberFormat : parent.getNumberFormat();
    }

    /**
     * The string value for the boolean {@code true} and {@code false} values, intended for human audience (not for a
     * computer language), separated with comma. For example, {@code "yes,no"}. Note that white-space is significant,
     * so {@code "yes, no"} is WRONG (unless you want that leading space before "no").
     * 
     * <p>For backward compatibility the default is {@code "true,false"}, but using that value is denied for automatic
     * boolean-to-string conversion (like <code>${myBoolean}</code> will fail with it), only {@code myBool?string} will
     * allow it, which is deprecated since FreeMarker 2.3.20.
     * 
     * <p>Note that automatic boolean-to-string conversion only exists since FreeMarker 2.3.20. Earlier this setting
     * only influenced the result of {@code myBool?string}. 
     */
    public void setBooleanFormat(String booleanFormat) {
        NullArgumentException.check("booleanFormat", booleanFormat);
        
        int commaIdx = booleanFormat.indexOf(',');
        if(commaIdx == -1) {
            throw new IllegalArgumentException(
                    "Setting value must be string that contains two comma-separated values for true and false, " +
                    "respectively.");
        }
        
        this.booleanFormat = booleanFormat; 
        properties.setProperty(BOOLEAN_FORMAT_KEY, booleanFormat);
        
        if (booleanFormat.equals(C_TRUE_FALSE)) {
            // C_TRUE_FALSE is the default for BC, but it's not a good default for human audience formatting, so we
            // pretend that it wasn't set.
            trueStringValue = null; 
            falseStringValue = null;
        } else {
            trueStringValue = booleanFormat.substring(0, commaIdx); 
            falseStringValue = booleanFormat.substring(commaIdx + 1);
        }
    }
    
    /**
     * The getter pair of {@link #setBooleanFormat(String)}.
     */
    public String getBooleanFormat() {
        return booleanFormat != null ? booleanFormat : parent.getBooleanFormat(); 
    }
    
    String formatBoolean(boolean value, boolean fallbackToTrueFalse) throws TemplateException {
        if (value) {
            String s = getTrueStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_TRUE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        } else {
            String s = getFalseStringValue();
            if (s == null) {
                if (fallbackToTrueFalse) {
                    return MiscUtil.C_FALSE;
                } else {
                    throw new _MiscTemplateException(getNullBooleanFormatErrorDescription());
                }
            } else {
                return s;
            }
        }
    }

    private _ErrorDescriptionBuilder getNullBooleanFormatErrorDescription() {
        return new _ErrorDescriptionBuilder(new Object[] {
                "Can't convert boolean to string automatically, because the \"", BOOLEAN_FORMAT_KEY ,"\" setting was ",
                new _DelayedJQuote(getBooleanFormat()), 
                (getBooleanFormat().equals(C_TRUE_FALSE)
                    ? ", which is the legacy default computer-language format, and hence isn't accepted."
                    : ".") }).tips(new Object[] {
                 "If you just want \"true\"/\"false\" result as you are generting computer-language output, "
                 + "use \"?c\", like ${myBool?c}.",
                 "You can write myBool?string('yes', 'no') and like to specify boolean formatting in place.",
                 new Object[] {
                     "If you need the same two values on most places, the programmers should set the \"",
                     BOOLEAN_FORMAT_KEY ,"\" setting to something like \"yes,no\"." }
                 });
    }

    /**
     * Returns the string to which {@code true} is converted to for human audience, or {@code null} if automatic
     * coercion to string is not allowed. The default value is {@code null}.
     * 
     * <p>This value is deduced from the {@code "boolean_format"} setting.
     * Confusingly, for backward compatibility (at least until 2.4) that defaults to {@code "true,false"}, yet this
     * defaults to {@code null}. That's so because {@code "true,false"} is treated exceptionally, as that default is a
     * historical mistake in FreeMarker, since it targets computer language output, not human writing. Thus it's
     * ignored.
     * 
     * @since 2.3.20
     */
    String getTrueStringValue() {
        // The first step deliberately tests booleanFormat instead of trueStringValue! 
        return booleanFormat != null ? trueStringValue : (parent != null ? parent.getTrueStringValue() : null); 
    }

    /**
     * Same as {@link #getTrueStringValue()} but with {@code false}. 
     * @since 2.3.20
     */
    String getFalseStringValue() {
        // The first step deliberately tests booleanFormat instead of falseStringValue! 
        return booleanFormat != null ? falseStringValue : (parent != null ? parent.getFalseStringValue() : null); 
    }

    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are time (no date part) values,
     * also the format that {@code someString?time} will use to parse strings.
     * 
     * <p>For the possible values see {@link #setDateTimeFormat(String)}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "medium"}.
     */
    public void setTimeFormat(String timeFormat) {
        NullArgumentException.check("timeFormat", timeFormat);
        this.timeFormat = timeFormat;
        properties.setProperty(TIME_FORMAT_KEY, timeFormat);
    }

    /**
     * The getter pair of {@link #setTimeFormat(String)}.
     */
    public String getTimeFormat() {
        return timeFormat != null ? timeFormat : parent.getTimeFormat();
    }

    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date (no time part) values,
     * also the format that {@code someString?date} will use to parse strings.
     * 
     * <p>For the possible values see {@link #setDateTimeFormat(String)}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "code"}.
     */
    public void setDateFormat(String dateFormat) {
        NullArgumentException.check("dateFormat", dateFormat);
        this.dateFormat = dateFormat;
        properties.setProperty(DATE_FORMAT_KEY, dateFormat);
    }

    /**
     * The getter pair of {@link #setDateFormat(String)}.
     */
    public String getDateFormat() {
        return dateFormat != null ? dateFormat : parent.getDateFormat();
    }

    /**
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date-time (timestamp) values,
     * also the format that {@code someString?datetime} will use to parse strings.
     * 
     * <p>The possible setting values are (the quotation marks aren't part of the value itself):
     * 
     * <ul>
     *   <li><p>Patterns accepted by Java's {@link SimpleDateFormat}, for example {@code "dd.MM.yyyy HH:mm:ss"} (where
     *       {@code HH} means 24 hours format) or {@code "MM/dd/yyyy hh:mm:ss a"} (where {@code a} prints AM or PM, if
     *       the current language is English).
     *   
     *   <li><p>{@code "xs"} for XML Schema format, or {@code "iso"} for ISO 8601:2004 format.
     *       These formats allow various additional options, separated with space, like in
     *       {@code "iso m nz"} (or with {@code _}, like in {@code "iso_m_nz"}; this is useful in a case like
     *       {@code lastModified?string.iso_m_nz}). The options and their meanings are:
     *       
     *       <ul>
     *         <li><p>Accuracy options:<br>
     *             {@code ms} = Milliseconds, always shown with all 3 digits, even if it's all 0-s.
     *                     Example: {@code 13:45:05.800}<br>
     *             {@code s} = Seconds (fraction seconds are dropped even if non-0), like {@code 13:45:05}<br>
     *             {@code m} = Minutes, like {@code 13:45}. This isn't allowed for "xs".<br>
     *             {@code h} = Hours, like {@code 13}. This isn't allowed for "xs".<br>
     *             Neither = Up to millisecond accuracy, but trailing millisecond 0-s are removed, also the whole
     *                     milliseconds part if it would be 0 otherwise. Example: {@code 13:45:05.8}
     *                     
     *         <li><p>Time zone offset visibility options:<br>
     *             {@code fz} = "Force Zone", always show time zone offset (even for for
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values).
     *                     But, because ISO 8601 doesn't allow for dates (means date without time of the day) to
     *                     show the zone offset, this option will have no effect in the case of {@code "iso"} with
     *                     dates.<br>
     *             {@code nz} = "No Zone", never show time zone offset<br>
     *             Neither = always show time zone offset, except for {@link java.sql.Date java.sql.Date}
     *                     and {@link java.sql.Time java.sql.Time}, and for {@code "iso"} date values.
     *                     
     *         <li><p>Time zone options:<br>
     *             {@code u} = Use UTC instead of what the {@code time_zone} setting suggests. However,
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} aren't affected
     *                     by this (see {@link #setSQLDateAndTimeTimeZone(TimeZone)} to understand why)<br>
     *             {@code fu} = "Force UTC", that is, use UTC instead of what the {@code time_zone} or the
     *                     {@code sql_date_and_time_time_zone} setting suggests. This also effects
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values<br>
     *             Neither = Use the time zone suggested by the {@code time_zone} or the
     *                     {@code sql_date_and_time_time_zone} configuration setting ({@link #setTimeZone(TimeZone)} and
     *                     {@link #setSQLDateAndTimeTimeZone(TimeZone)}).
     *       </ul>
     *       
     *       <p>The options can be specified in any order.</p>
     *       
     *       <p>Options from the same category are mutually exclusive, like using {@code m} and {@code s}
     *       together is an error.
     *       
     *       <p>The accuracy and time zone offset visibility options don't influence parsing, only formatting.
     *       For example, even if you use "iso m nz", "2012-01-01T15:30:05.125+01" will be parsed successfully and with
     *       milliseconds accuracy.
     *       The time zone options (like "u") influence what time zone is chosen only when parsing a string that doesn't
     *       contain time zone offset.
     *       
     *       <p>Parsing with {@code "iso"} understands both extend format and basic format, like
     *       {@code 20141225T235018}. It doesn't, however, support the parsing of all kind of ISO 8601 strings: if
     *       there's a date part, it must use year, month and day of the month values (not week of the year), and the
     *       day can't be omitted.
     *       
     *       <p>The output of {@code "iso"} is deliberately so that it's also a good representation of the value with
     *       XML Schema format, except for 0 and negative years, where it's impossible. Also note that the time zone
     *       offset is omitted for date values in the {@code "iso"} format, while it's preserved for the {@code "xs"}
     *       format.
     *       
     *   <li><p>{@code "short"}, {@code "medium"}, {@code "long"}, or {@code "full"}, which that has locale-dependent
     *       meaning defined by the Java platform (see in the documentation of {@link java.text.DateFormat}).
     *       For date-time values, you can specify the length of the date and time part independently, be separating
     *       them with {@code _}, like {@code "short_medium"}. ({@code "medium"} means
     *       {@code "medium_medium"} for date-time values.)
     * </ul> 
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@code "code"}.
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        NullArgumentException.check("dateTimeFormat", dateTimeFormat);
        this.dateTimeFormat = dateTimeFormat;
        properties.setProperty(DATETIME_FORMAT_KEY, dateTimeFormat);
    }

    /**
     * The getter pair of {@link #setDateTimeFormat(String)}.
     */
    public String getDateTimeFormat() {
        return dateTimeFormat != null ? dateTimeFormat : parent.getDateTimeFormat();
    }

    /**
     * Sets the exception handler used to handle exceptions occurring inside templates.
     * The default is {@link TemplateExceptionHandler#DEBUG_HANDLER}. The recommended values are:
     * 
     * <ul>
     *   <li>In production systems: {@link TemplateExceptionHandler#RETHROW_HANDLER}
     *   <li>During development of HTML templates: {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}
     *   <li>During development of non-HTML templates: {@link TemplateExceptionHandler#DEBUG_HANDLER}
     * </ul>
     * 
     * <p>All of these will let the exception propagate further, so that you can catch it around
     * {@link Template#process(Object, Writer)} for example. The difference is in what they print on the output before
     * they do that.
     * 
     * <p>Note that the {@link TemplateExceptionHandler} is not meant to be used for generating HTTP error pages.
     * Neither is it meant to be used to roll back the printed output. These should be solved outside template
     * processing when the exception raises from {@link Template#process(Object, Writer) Template.process}.
     * {@link TemplateExceptionHandler} meant to be used if you want to include special content <em>in</em> the template
     * output, or if you want to suppress certain exceptions. 
     */
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        NullArgumentException.check("templateExceptionHandler", templateExceptionHandler);
        this.templateExceptionHandler = templateExceptionHandler;
        properties.setProperty(TEMPLATE_EXCEPTION_HANDLER_KEY, templateExceptionHandler.getClass().getName());
    }

    /**
     * The getter pair of {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}.
     */
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return templateExceptionHandler != null
                ? templateExceptionHandler : parent.getTemplateExceptionHandler();
    }

    /**
     * Sets the arithmetic engine used to perform arithmetic operations.
     * The default is {@link ArithmeticEngine#BIGDECIMAL_ENGINE}.
     */
    public void setArithmeticEngine(ArithmeticEngine arithmeticEngine) {
        NullArgumentException.check("arithmeticEngine", arithmeticEngine);
        this.arithmeticEngine = arithmeticEngine;
        properties.setProperty(ARITHMETIC_ENGINE_KEY, arithmeticEngine.getClass().getName());
    }

    /**
     * The getter pair of {@link #setArithmeticEngine(ArithmeticEngine)}.
     */
    public ArithmeticEngine getArithmeticEngine() {
        return arithmeticEngine != null
                ? arithmeticEngine : parent.getArithmeticEngine();
    }

    /**
     * Sets the object wrapper used to wrap objects to {@link TemplateModel}-s.
     * The default is {@link ObjectWrapper#DEFAULT_WRAPPER}.
     */
    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        NullArgumentException.check("objectWrapper", objectWrapper);
        this.objectWrapper = objectWrapper;
        properties.setProperty(OBJECT_WRAPPER_KEY, objectWrapper.getClass().getName());
    }

    /**
     * The getter pair of {@link #setObjectWrapper(ObjectWrapper)}.
     */
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper != null
                ? objectWrapper : parent.getObjectWrapper();
    }
    
    /**
     * Informs FreeMarker about the charset used for the output. As FreeMarker outputs character stream (not
     * byte stream), it's not aware of the output charset unless the software that encloses it tells it
     * with this setting. Some templates may use FreeMarker features that require this information.
     * Setting this to {@code null} means that the output encoding is not known.
     * 
     * <p>Defaults to {@code null} (unknown).
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
        // java.util.Properties doesn't allow null value!
        if (outputEncoding != null) {
            properties.setProperty(OUTPUT_ENCODING_KEY, outputEncoding);
        } else {
            properties.remove(OUTPUT_ENCODING_KEY);
        }
        outputEncodingSet = true;
    }
    
    public String getOutputEncoding() {
        return outputEncodingSet
                ? outputEncoding
                : (parent != null ? parent.getOutputEncoding() : null);
    }
    
    /**
     * Sets the URL escaping charset. If not set ({@code null}), the output encoding
     * ({@link #setOutputEncoding(String)}) will be used for URL escaping.
     * 
     * Defaults to {@code null}.
     */
    public void setURLEscapingCharset(String urlEscapingCharset) {
        this.urlEscapingCharset = urlEscapingCharset;
        // java.util.Properties doesn't allow null value!
        if (urlEscapingCharset != null) {
            properties.setProperty(URL_ESCAPING_CHARSET_KEY, urlEscapingCharset);
        } else {
            properties.remove(URL_ESCAPING_CHARSET_KEY);
        }
        urlEscapingCharsetSet = true;
    }
    
    public String getURLEscapingCharset() {
        return urlEscapingCharsetSet
                ? urlEscapingCharset
                : (parent != null ? parent.getURLEscapingCharset() : null);
    }
    
    /**
     * Sets the {@link TemplateClassResolver} that is used when the
     * <code>new</code> built-in is called in a template. That is, when
     * a template contains the <code>"com.example.SomeClassName"?new</code>
     * expression, this object will be called to resolve the
     * <code>"com.example.SomeClassName"</code> string to a class. The default
     * value is {@link TemplateClassResolver#UNRESTRICTED_RESOLVER} in
     * FreeMarker 2.3.x, and {@link TemplateClassResolver#SAFER_RESOLVER}
     * starting from FreeMarker 2.4.0. If you allow users to upload templates,
     * it's important to use a custom restrictive {@link TemplateClassResolver}.
     * 
     * @since 2.3.17
     */
    public void setNewBuiltinClassResolver(TemplateClassResolver newBuiltinClassResolver) {
        NullArgumentException.check("newBuiltinClassResolver", newBuiltinClassResolver);
        this.newBuiltinClassResolver = newBuiltinClassResolver;
        properties.setProperty(NEW_BUILTIN_CLASS_RESOLVER_KEY,
                newBuiltinClassResolver.getClass().getName());
    }

    /**
     * Retrieves the {@link TemplateClassResolver} used
     * to resolve classes when "SomeClassName"?new is called in a template.
     * 
     * @since 2.3.17
     */
    public TemplateClassResolver getNewBuiltinClassResolver() {
        return newBuiltinClassResolver != null
                ? newBuiltinClassResolver : parent.getNewBuiltinClassResolver();
    }
    
    /**
     * Sets whether the output {@link Writer} is automatically flushed at
     * the end of {@link Template#process(Object, Writer)} (and its
     * overloads). The default is {@code true}.
     * 
     * <p>Using {@code false} is needed for example when a Web page is composed
     * from several boxes (like portlets, GUI panels, etc.) that aren't inserted
     * with <tt>#include</tt> (or with similar directives) into a master
     * FreeMarker template, rather they are all processed with a separate
     * {@link Template#process(Object, Writer)} call. In a such scenario the
     * automatic flushes would commit the HTTP response after each box, hence
     * interfering with full-page buffering, and also possibly decreasing
     * performance with too frequent and too early response buffer flushes.
     * 
     * @since 2.3.17
     */
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = Boolean.valueOf(autoFlush);
        properties.setProperty(AUTO_FLUSH_KEY, String.valueOf(autoFlush));
    }
    
    /**
     * See {@link #setAutoFlush(boolean)}
     * 
     * @since 2.3.17
     */
    public boolean getAutoFlush() {
        return autoFlush != null 
            ? autoFlush.booleanValue()
            : (parent != null ? parent.getAutoFlush() : true);
    }
    
    /**
     * Sets if tips should be shown in error messages of errors arising during template processing.
     * The default is {@code true}. 
     * 
     * @since 2.3.21
     */
    public void setShowErrorTips(boolean showTips) {
        this.showErrorTips = Boolean.valueOf(showTips);
        properties.setProperty(SHOW_ERROR_TIPS_KEY, String.valueOf(showTips));
    }
    
    /**
     * See {@link #setShowErrorTips(boolean)}
     * 
     * @since 2.3.21
     */
    public boolean getShowErrorTips() {
        return showErrorTips != null 
            ? showErrorTips.booleanValue()
            : (parent != null ? parent.getShowErrorTips() : true);
    }
    
    /**
     * Specifies if {@code ?api} can be used in templates. Defaults to {@code false} so that updating FreeMarker won't
     * decrease the security of existing applications.
     * 
     * @since 2.3.22
     */
    public void setAPIBuiltinEnabled(boolean value) {
        apiBuiltinEnabled = Boolean.valueOf(value);
        properties.setProperty(API_BUILTIN_ENABLED_KEY, String.valueOf(value));
    }

    /**
     * See {@link #setAPIBuiltinEnabled(boolean)}
     * 
     * @since 2.3.22
     */
    public boolean isAPIBuiltinEnabled() {
        return apiBuiltinEnabled != null 
                ? apiBuiltinEnabled.booleanValue()
                : (parent != null ? parent.isAPIBuiltinEnabled() : false);
    }
    
    /**
     * Specifies if {@link TemplateException}-s thrown by template processing are logged by FreeMarker or not. The
     * default is {@code true} for backward compatibility, but that results in logging the exception twice in properly
     * written applications, because there the {@link TemplateException} thrown by the public FreeMarker API is also
     * logged by the caller (even if only as the cause exception of a higher level exception). Hence, in modern
     * applications it should be set to {@code false}. Note that this setting has no effect on the logging of exceptions
     * caught by {@code #attempt}; those are always logged, no mater what (because those exceptions won't bubble up
     * until the API caller).
     * 
     * @since 2.3.22
     */
    public void setLogTemplateExceptions(boolean value) {
        logTemplateExceptions = Boolean.valueOf(value);
        properties.setProperty(LOG_TEMPLATE_EXCEPTIONS_KEY, String.valueOf(value));
    }

    /**
     * See {@link #setLogTemplateExceptions(boolean)}
     * 
     * @since 2.3.22
     */
    public boolean getLogTemplateExceptions() {
        return logTemplateExceptions != null 
                ? logTemplateExceptions.booleanValue()
                : (parent != null ? parent.getLogTemplateExceptions() : true);
    }
    
    private static final String ALLOWED_CLASSES = "allowed_classes";
    private static final String TRUSTED_TEMPLATES = "trusted_templates";
    
    /**
     * Sets a FreeMarker setting by a name and string value. If you can configure FreeMarker directly with Java (or
     * other programming language), you should use the dedicated setter methods instead (like
     * {@link #setObjectWrapper(ObjectWrapper)}. This meant to be used if you get the settings from somewhere
     * as text. Regardless, below you will find an overview of the settings available no matter how you set them. 
     * 
     * <p>Note: As of FreeMarker 2.3.23, setting names can be written in camel case too. For example, instead of
     * {@code date_format} you can also use {@code dateFormat}. It's likely that camel case will become to the
     * recommended convention in the future.
     * 
     * <p>The list of settings commonly supported in all {@link Configurable} subclasses:
     * <ul>
     *   <li><p>{@code "locale"}:
     *       See {@link #setLocale(Locale)}.
     *       <br>String value: local codes with the usual format in Java, such as {@code "en_US"}.
     *       
     *   <li><p>{@code "classic_compatible"}:
     *       See {@link #setClassicCompatible(boolean)} and {@link Configurable#setClassicCompatibleAsInt(int)}.
     *       <br>String value: {@code "true"}, {@code "false"}, also since 2.3.20 {@code 0} or {@code 1} or {@code 2}.
     *       (Also accepts {@code "yes"}, {@code "no"}, {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}.)
     *       Case insensitive.
     *       
     *   <li><p>{@code "template_exception_handler"}:
     *       See {@link #setTemplateExceptionHandler(TemplateExceptionHandler)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot, then it must be one of these predefined values (case insensitive):
     *       {@code "rethrow"} (means {@link TemplateExceptionHandler#RETHROW_HANDLER}),
     *       {@code "debug"} (means {@link TemplateExceptionHandler#DEBUG_HANDLER}),
     *       {@code "html_debug"} (means {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}),
     *       {@code "ignore"} (means {@link TemplateExceptionHandler#IGNORE_HANDLER}),
     *       {@code "default"} (only allowed for {@link Configuration} instances) for the default.
     *       
     *   <li><p>{@code "arithmetic_engine"}:
     *       See {@link #setArithmeticEngine(ArithmeticEngine)}.  
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then it must be one of these special values (case insensitive):
     *       {@code "bigdecimal"}, {@code "conservative"}.
     *       
     *   <li><p>{@code "object_wrapper"}:
     *       See {@link #setObjectWrapper(ObjectWrapper)}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>, with the addition that {@link BeansWrapper}, {@link DefaultObjectWrapper} and
     *       {@link SimpleObjectWrapper} can be referred without package name. For example, these strings are valid
     *       values: {@code "DefaultObjectWrapper(2.3.21)"},
     *       {@code "BeansWrapper(2.3.21, simpleMapWrapper=true)"}.
     *       <br>If the value does not contain dot, then it must be one of these special values (case insensitive):
     *       {@code "default"} means the default of {@link Configuration} (the default depends on the
     *       {@code Configuration#Configuration(Version) incompatible_improvements}, but a bug existed in 2.3.21 where
     *       that was ignored),
     *       {@code "default_2_3_0"} (means the deprecated {@link ObjectWrapper#DEFAULT_WRAPPER})
     *       {@code "simple"} (means the deprecated {@link ObjectWrapper#SIMPLE_WRAPPER}),
     *       {@code "beans"} (means the deprecated {@link BeansWrapper#BEANS_WRAPPER}
     *       or {@link BeansWrapperBuilder#build()}),
     *       {@code "jython"} (means {@link freemarker.ext.jython.JythonWrapper#DEFAULT_WRAPPER})
     *       
     *   <li><p>{@code "number_format"}: See {@link #setNumberFormat(String)}.
     *   
     *   <li><p>{@code "boolean_format"}: See {@link #setBooleanFormat(String)} .
     *   
     *   <li><p>{@code "date_format", "time_format", "datetime_format"}:
     *       See {@link #setDateFormat(String)}, {@link #setTimeFormat(String)}, {@link #setDateTimeFormat(String)}. 
     *        
     *   <li><p>{@code "time_zone"}:
     *       See {@link #setTimeZone(TimeZone)}.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone} defines it. Also, since 2.3.21
     *       {@code "JVM default"} can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called.
     *       For example {@code "GMT-8:00"} or {@code "America/Los_Angeles"}
     *       <br>If you set this setting, consider setting {@code sql_date_and_time_time_zone}
     *       too (see below)! 
     *       
     *   <li><p>{@code sql_date_and_time_time_zone}:
     *       See {@link #setSQLDateAndTimeTimeZone(TimeZone)}.
     *       Since 2.3.21.
     *       <br>String value: With the format as {@link TimeZone#getTimeZone} defines it. Also, {@code "JVM default"}
     *       can be used that will be replaced with the actual JVM default time zone when
     *       {@link #setSetting(String, String)} is called. Also {@code "null"} can be used, which has the same effect
     *       as {@link #setSQLDateAndTimeTimeZone(TimeZone) setSQLDateAndTimeTimeZone(null)}.
     *       
     *   <li><p>{@code "output_encoding"}:
     *       See {@link #setOutputEncoding(String)}.
     *       
     *   <li><p>{@code "url_escaping_charset"}:
     *       See {@link #setURLEscapingCharset(String)}.
     *       
     *   <li><p>{@code "auto_flush"}:
     *       See {@link #setAutoFlush(boolean)}.
     *       Since 2.3.17.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code "new_builtin_class_resolver"}:
     *       See {@link #setNewBuiltinClassResolver(TemplateClassResolver)}.
     *       Since 2.3.17.
     *       The value must be one of these (ignore the quotation marks):
     *       <ol>
     *         <li><p>{@code "unrestricted"}:
     *             Use {@link TemplateClassResolver#UNRESTRICTED_RESOLVER}
     *         <li><p>{@code "safer"}:
     *             Use {@link TemplateClassResolver#SAFER_RESOLVER}
     *         <li><p>{@code "allows_nothing"}:
     *             Use {@link TemplateClassResolver#ALLOWS_NOTHING_RESOLVER}
     *         <li><p>Something that contains colon will use
     *             {@link OptInTemplateClassResolver} and is expected to
     *             store comma separated values (possibly quoted) segmented
     *             with {@code "allowed_classes:"} and/or
     *             {@code "trusted_templates:"}. Examples of valid values:
     *             
     *             <table style="width: auto; border-collapse: collapse" border="1"
     *                  summary="trusted_template value examples">
     *               <tr>
     *                 <th>Setting value
     *                 <th>Meaning
     *               <tr>
     *                 <td>
     *                   {@code allowed_classes: com.example.C1, com.example.C2,
     *                   trusted_templates: lib/*, safe.ftl}                 
     *                 <td>
     *                   Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. But, allow templates
     *                   within the {@code lib/} directory (like
     *                   {@code lib/foo/bar.ftl}) and template {@code safe.ftl}
     *                   (that does not match {@code foo/safe.ftl}, only
     *                   exactly {@code safe.ftl}) to instantiate anything
     *                   that {@link TemplateClassResolver#SAFER_RESOLVER} allows.
     *               <tr>
     *                 <td>
     *                   {@code allowed_classes: com.example.C1, com.example.C2}
     *                 <td>Only allow instantiating the {@code com.example.C1} and
     *                   {@code com.example.C2} classes. There are no
     *                   trusted templates.
     *               <tr>
     *                 <td>
                         {@code trusted_templates: lib/*, safe.ftl}                 
     *                 <td>
     *                   Do not allow instantiating any classes, except in
     *                   templates inside {@code lib/} or in template 
     *                   {@code safe.ftl}.
     *             </table>
     *             
     *             <p>For more details see {@link OptInTemplateClassResolver}.
     *             
     *         <li><p>Otherwise if the value contains dot, it's interpreted as an <a href="#fm_obe">object builder
     *             expression</a>.
     *       </ol>
     *       
     *   <li><p>{@code "show_error_tips"}:
     *       See {@link #setShowErrorTips(boolean)}.
     *       Since 2.3.21.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     *   <li><p>{@code api_builtin_enabled}:
     *       See {@link #setAPIBuiltinEnabled(boolean)}.
     *       Since 2.3.22.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code "y"},  etc.
     *       
     * </ul>
     * 
     * <p>{@link Configuration} (a subclass of {@link Configurable}) also understands these:</p>
     * <ul>
     *   <li><p>{@code "auto_import"}:
     *       See {@link Configuration#setAutoImports(Map)}
     *       <br>String value is something like:
     *       <br>{@code /lib/form.ftl as f, /lib/widget as w, "/lib/odd name.ftl" as odd}
     *       
     *   <li><p>{@code "auto_include"}: Sets the list of auto-includes.
     *       See {@link Configuration#setAutoIncludes(List)}
     *       <br>String value is something like:
     *       <br>{@code /include/common.ftl, "/include/evil name.ftl"}
     *       
     *   <li><p>{@code "default_encoding"}:
     *       See {@link Configuration#setDefaultEncoding(String)}.
     *       <br>As the default value is the system default, which can change
     *       from one server to another, <b>you should always set this!</b>
     *       
     *   <li><p>{@code "localized_lookup"}:
     *       See {@link Configuration#setLocalizedLookup}.
     *       <br>String value: {@code "true"}, {@code "false"} (also the equivalents: {@code "yes"}, {@code "no"},
     *       {@code "t"}, {@code "f"}, {@code "y"}, {@code "n"}).
     *       Case insensitive.
     *       
     *   <li><p>{@code "strict_syntax"}:
     *       See {@link Configuration#setStrictSyntaxMode}. Deprecated.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code yes}, etc.
     *       
     *   <li><p>{@code "whitespace_stripping"}:
     *       See {@link Configuration#setWhitespaceStripping}.
     *       <br>String value: {@code "true"}, {@code "false"}, {@code yes}, etc.
     *       
     *   <li><p>{@code "cache_storage"}:
     *       See {@link Configuration#setCacheStorage}.
     *       <br>String value: If the value contains dot, then it's interpreted as an <a href="#fm_obe">object builder
     *       expression</a>.
     *       If the value does not contain dot,
     *       then a {@link freemarker.cache.MruCacheStorage} will be used with the
     *       maximum strong and soft sizes specified with the setting value. Examples
     *       of valid setting values:
     *       
     *       <table style="width: auto; border-collapse: collapse" border="1" summary="cache_storage value examples">
     *         <tr><th>Setting value<th>max. strong size<th>max. soft size
     *         <tr><td>{@code "strong:50, soft:500"}<td>50<td>500
     *         <tr><td>{@code "strong:100, soft"}<td>100<td>{@code Integer.MAX_VALUE}
     *         <tr><td>{@code "strong:100"}<td>100<td>0
     *         <tr><td>{@code "soft:100"}<td>0<td>100
     *         <tr><td>{@code "strong"}<td>{@code Integer.MAX_VALUE}<td>0
     *         <tr><td>{@code "soft"}<td>0<td>{@code Integer.MAX_VALUE}
     *       </table>
     *       
     *       <p>The value is not case sensitive. The order of <tt>soft</tt> and <tt>strong</tt>
     *       entries is not significant.
     *       
     *   <li><p>{@code "template_update_delay"}:
     *       Template update delay in <b>seconds</b> (not in milliseconds) if no unit is specified; see
     *       {@link Configuration#setTemplateUpdateDelayMilliseconds(long)} for more.
     *       <br>String value: Valid positive integer, optionally followed by a time unit (recommended). The default
     *       unit is seconds. It's strongly recommended to specify the unit for clarity, like in "500 ms" or "30 s".
     *       Supported units are: "s" (seconds), "ms" (milliseconds), "m" (minutes), "h" (hours). The whitespace between
     *       the unit and the number is optional. Units are only supported since 2.3.23.
     *       
     *   <li><p>{@code "tag_syntax"}:
     *       See {@link Configuration#setTagSyntax(int)}.
     *       <br>String value: Must be one of
     *       {@code "auto_detect"}, {@code "angle_bracket"}, and {@code "square_bracket"}. 
     *       
     *   <li><p>{@code "naming_convention"}:
     *       See {@link Configuration#setNamingConvention(int)}.
     *       <br>String value: Must be one of
     *       {@code "auto_detect"}, {@code "legacy"}, and {@code "camel_case"}. 
     *       
     *   <li><p>{@code "incompatible_improvements"}:
     *       See {@link Configuration#setIncompatibleImprovements(Version)}.
     *       <br>String value: version number like {@code 2.3.20}.
     *       
     *   <li><p>{@code "incompatible_enhancements"}:
     *       See: {@link Configuration#setIncompatibleEnhancements(String)}.
     *       This setting name is deprecated, use {@code "incompatible_improvements"} instead.
     *       
     *   <li><p>{@code "template_loader"}:
     *       See: {@link Configuration#setTemplateLoader(TemplateLoader)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>.
     *       
     *   <li><p>{@code "template_lookup_strategy"}:
     *       See: {@link Configuration#setTemplateLookupStrategy(freemarker.cache.TemplateLookupStrategy)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, or else interpreted as an
     *       <a href="#fm_obe">object builder expression</a>.
     *       
     *   <li><p>{@code "template_name_format"}:
     *       See: {@link Configuration#setTemplateNameFormat(freemarker.cache.TemplateNameFormat)}.
     *       <br>String value: {@code "default"} (case insensitive) for the default, {@code "default_2_3_0"}
     *       for {@link freemarker.cache.TemplateNameFormat#DEFAULT_2_3_0}, {@code "default_2_4_0"} for
     *       {@link freemarker.cache.TemplateNameFormat#DEFAULT_2_4_0}.
     * </ul>
     * 
     * <p><a name="fm_obe"></a>Regarding <em>object builder expressions</em> (used by the setting values where it was
     * indicated):
     * <ul>
     *   <li><p>Before FreeMarker 2.3.21 it had to be a fully qualified class name, and nothing else.</li>
     *   <li><p>Since 2.3.21, the generic syntax is:
     *       <tt><i>className</i>(<i>constrArg1</i>, <i>constrArg2</i>, ... <i>constrArgN</i>,
     *       <i>propName1</i>=<i>propValue1</i>, <i>propName2</i>=<i>propValue2</i>, ...
     *       <i>propNameN</i>=<i>propValueN</i>)</tt>,
     *       where
     *       <tt><i>className</i></tt> is the fully qualified class name of the instance to create (except if we have
     *       builder class or <tt>INSTANCE</tt> field around, but see that later),
     *       <tt><i>constrArg</i></tt>-s are the values of constructor arguments,
     *       and <tt><i>propName</i>=<i>propValue</i></tt>-s set JavaBean properties (like <tt>x=1</tt> means
     *       <tt>setX(1)</tt>) on the created instance. You can have any number of constructor arguments and property
     *       setters, including 0. Constructor arguments must precede any property setters.   
     *   </li>
     *   <li>
     *     Example: <tt>com.example.MyObjectWrapper(1, 2, exposeFields=true, cacheSize=5000)</tt> is nearly
     *     equivalent with this Java code:
     *     <tt>obj = new com.example.MyObjectWrapper(1, 2); obj.setExposeFields(true); obj.setCacheSize(5000);</tt>
     *   </li>
     *   <li>
     *      <p>If you have no constructor arguments and property setters, and the <tt><i>className</i></tt> class has
     *      a public static {@code INSTANCE} field, the value of that filed will be the value of the expression, and
     *      the constructor won't be called. Note that if you use the backward compatible
     *      syntax, where these's no parenthesis after the class name, then it will not look for {@code INSTANCE}.
     *   </li>
     *   <li>
     *      <p>If there exists a class named <tt><i>className</i>Builder</tt>, then that class will be instantiated
     *      instead with the given constructor arguments, and the JavaBean properties of that builder instance will be
     *      set. After that, the public <tt>build()</tt> method of the instance will be called, whose return value
     *      will be the value of the whole expression. (The builder class and the <tt>build()</tt> method is simply
     *      found by name, there's no special interface to implement.) Note that if you use the backward compatible
     *      syntax, where these's no parenthesis after the class name, then it will not look for builder class.
     *   </li>
     *   <li>
     *      <p>Currently, the values of arguments and properties can only be one of these:
     *      <ul>
     *        <li>A numerical literal, like {@code 123} or {@code -1.5}. Like in FTL, there are no numerical types,
     *            the value will be automatically converted to the type of the target.</li>
     *        <li>A boolean literal: {@code true} or {@code false}
     *        <li>The null literal: {@code null}
     *        <li>A string literal with FTL syntax, except that  it can't contain <tt>${...}</tt>-s and
     *            <tt>#{...}</tt>-s. Examples: {@code "Line 1\nLine 2"} or {@code r"C:\temp"}.
     *        <li>An object builder expression. That is, object builder expressions can be nested into each other. 
     *      </ul>
     *   </li>
     *   <li>
     *     <p>The top-level object builder expressions may omit {@code ()}. In that case, for backward compatibility,
     *     the {@code INSTANCE} field and the builder class is not searched, so the instance will be always
     *     created with its parameterless constructor. (This behavior will possibly change in 2.4.) The {@code ()}
     *     can't be omitted for nested expressions.
     *   </li>
     *   <li>
     *     <p>The classes and methods that the expression meant to access must be all public.
     *   </li>
     * </ul>
     * 
     * @param name the name of the setting.
     * @param value the string that describes the new value of the setting.
     * 
     * @throws UnknownSettingException if the name is wrong.
     * @throws TemplateException if the new value of the setting can't be set for any other reasons.
     */
    public void setSetting(String name, String value) throws TemplateException {
        boolean unknown = false;
        try {
            if (LOCALE_KEY.equals(name)) {
                setLocale(StringUtil.deduceLocale(value));
            } else if (NUMBER_FORMAT_KEY_SNAKE_CASE.equals(name) || NUMBER_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setNumberFormat(value);
            } else if (TIME_FORMAT_KEY_SNAKE_CASE.equals(name) || TIME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setTimeFormat(value);
            } else if (DATE_FORMAT_KEY_SNAKE_CASE.equals(name) || DATE_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setDateFormat(value);
            } else if (DATETIME_FORMAT_KEY_SNAKE_CASE.equals(name) || DATETIME_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setDateTimeFormat(value);
            } else if (TIME_ZONE_KEY_SNAKE_CASE.equals(name) || TIME_ZONE_KEY_CAMEL_CASE.equals(name)) {
                setTimeZone(parseTimeZoneSettingValue(value));
            } else if (SQL_DATE_AND_TIME_TIME_ZONE_KEY_SNAKE_CASE.equals(name)
                    || SQL_DATE_AND_TIME_TIME_ZONE_KEY_CAMEL_CASE.equals(name)) {
                setSQLDateAndTimeTimeZone(value.equals("null") ? null : parseTimeZoneSettingValue(value));
            } else if (CLASSIC_COMPATIBLE_KEY_SNAKE_CASE.equals(name)
                    || CLASSIC_COMPATIBLE_KEY_CAMEL_CASE.equals(name)) {
                char firstChar;
                if (value != null && value.length() > 0) {
                    firstChar =  value.charAt(0);
                } else {
                    firstChar = 0;
                }
                if (Character.isDigit(firstChar) || firstChar == '+' || firstChar == '-') {
                    setClassicCompatibleAsInt(Integer.parseInt(value));
                } else {
                    setClassicCompatible(value != null ? StringUtil.getYesNo(value) : false);
                }
            } else if (TEMPLATE_EXCEPTION_HANDLER_KEY_SNAKE_CASE.equals(name)
                    || TEMPLATE_EXCEPTION_HANDLER_KEY_CAMEL_CASE.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("debug".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.DEBUG_HANDLER);
                    } else if ("html_debug".equalsIgnoreCase(value) || "htmlDebug".equals(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    } else if ("ignore".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.IGNORE_HANDLER);
                    } else if ("rethrow".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.RETHROW_HANDLER);
                    } else if (DEFAULT.equalsIgnoreCase(value) && this instanceof Configuration) {
                        ((Configuration) this).unsetTemplateExceptionHandler();
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setTemplateExceptionHandler((TemplateExceptionHandler) _ObjectBuilderSettingEvaluator.eval(
                            value, TemplateExceptionHandler.class, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (ARITHMETIC_ENGINE_KEY_SNAKE_CASE.equals(name) || ARITHMETIC_ENGINE_KEY_CAMEL_CASE.equals(name)) {
                if (value.indexOf('.') == -1) { 
                    if ("bigdecimal".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.BIGDECIMAL_ENGINE);
                    } else if ("conservative".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.CONSERVATIVE_ENGINE);
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setArithmeticEngine((ArithmeticEngine) _ObjectBuilderSettingEvaluator.eval(
                            value, ArithmeticEngine.class, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (OBJECT_WRAPPER_KEY_SNAKE_CASE.equals(name) || OBJECT_WRAPPER_KEY_CAMEL_CASE.equals(name)) {
                if (DEFAULT.equalsIgnoreCase(value)) {
                    if (this instanceof Configuration) {
                        ((Configuration) this).unsetObjectWrapper();
                    } else {
                        setObjectWrapper(Configuration.getDefaultObjectWrapper(Configuration.VERSION_2_3_0));
                    }
                } else if (DEFAULT_2_3_0.equalsIgnoreCase(value)) {
                    setObjectWrapper(Configuration.getDefaultObjectWrapper(Configuration.VERSION_2_3_0));
                } else if ("simple".equalsIgnoreCase(value)) {
                    setObjectWrapper(ObjectWrapper.SIMPLE_WRAPPER);
                } else if ("beans".equalsIgnoreCase(value)) {
                    setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
                } else if ("jython".equalsIgnoreCase(value)) {
                    Class clazz = Class.forName(
                            "freemarker.ext.jython.JythonWrapper");
                    setObjectWrapper(
                            (ObjectWrapper) clazz.getField("INSTANCE").get(null));        
                } else {
                    setObjectWrapper((ObjectWrapper) _ObjectBuilderSettingEvaluator.eval(
                                    value, ObjectWrapper.class, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (BOOLEAN_FORMAT_KEY_SNAKE_CASE.equals(name) || BOOLEAN_FORMAT_KEY_CAMEL_CASE.equals(name)) {
                setBooleanFormat(value);
            } else if (OUTPUT_ENCODING_KEY_SNAKE_CASE.equals(name) || OUTPUT_ENCODING_KEY_CAMEL_CASE.equals(name)) {
                setOutputEncoding(value);
            } else if (URL_ESCAPING_CHARSET_KEY_SNAKE_CASE.equals(name)
                    || URL_ESCAPING_CHARSET_KEY_CAMEL_CASE.equals(name)) {
                setURLEscapingCharset(value);
            } else if (STRICT_BEAN_MODELS_KEY_SNAKE_CASE.equals(name)
                    || STRICT_BEAN_MODELS_KEY_CAMEL_CASE.equals(name)) {
                setStrictBeanModels(StringUtil.getYesNo(value));
            } else if (AUTO_FLUSH_KEY_SNAKE_CASE.equals(name) || AUTO_FLUSH_KEY_CAMEL_CASE.equals(name)) {
                setAutoFlush(StringUtil.getYesNo(value));
            } else if (SHOW_ERROR_TIPS_KEY_SNAKE_CASE.equals(name) || SHOW_ERROR_TIPS_KEY_CAMEL_CASE.equals(name)) {
                setShowErrorTips(StringUtil.getYesNo(value));
            } else if (API_BUILTIN_ENABLED_KEY_SNAKE_CASE.equals(name)
                    || API_BUILTIN_ENABLED_KEY_CAMEL_CASE.equals(name)) {
                setAPIBuiltinEnabled(StringUtil.getYesNo(value));
            } else if (NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE.equals(name)
                    || NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE.equals(name)) {
                if ("unrestricted".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.UNRESTRICTED_RESOLVER);
                } else if ("safer".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
                } else if ("allows_nothing".equals(value) || "allowsNothing".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
                } else if (value.indexOf(":") != -1) {
                    List segments = parseAsSegmentedList(value);
                    Set allowedClasses = null;
                    List trustedTemplates = null;
                    for (int i = 0; i < segments.size(); i++) {
                        KeyValuePair kv = (KeyValuePair) segments.get(i);
                        String segmentKey = (String) kv.getKey();
                        List segmentValue = (List) kv.getValue();
                        if (segmentKey.equals(ALLOWED_CLASSES)) {
                            allowedClasses = new HashSet(segmentValue); 
                        } else if (segmentKey.equals(TRUSTED_TEMPLATES)) {
                            trustedTemplates = segmentValue;
                        } else {
                            throw new ParseException(
                                    "Unrecognized list segment key: " + StringUtil.jQuote(segmentKey) +
                                    ". Supported keys are: \"" + ALLOWED_CLASSES + "\", \"" +
                                    TRUSTED_TEMPLATES + "\"", 0, 0);
                        }
                    }
                    setNewBuiltinClassResolver(
                            new OptInTemplateClassResolver(allowedClasses, trustedTemplates));
                } else if (value.indexOf('.') != -1) {
                    setNewBuiltinClassResolver((TemplateClassResolver) _ObjectBuilderSettingEvaluator.eval(
                                    value, TemplateClassResolver.class, _SettingEvaluationEnvironment.getCurrent()));
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else if (LOG_TEMPLATE_EXCEPTIONS_KEY_SNAKE_CASE.equals(name)
                    || LOG_TEMPLATE_EXCEPTIONS_KEY_CAMEL_CASE.equals(name)) {
                setLogTemplateExceptions(StringUtil.getYesNo(value));
            } else {
                unknown = true;
            }
        } catch(Exception e) {
            throw settingValueAssignmentException(name, value, e);
        }
        if (unknown) {
            throw unknownSettingException(name);
        }
    }
    
    /** Returns the possible setting names. */
    // [Java 5] Add type param. [FM 2.4] Add public parameterless version the returns the camelCase names.
    Set/*<String>*/ getSettingNames(boolean camelCase) {
        return new _SortedArraySet(camelCase ? SETTING_NAMES_CAMEL_CASE : SETTING_NAMES_SNAKE_CASE); 
    }

    private TimeZone parseTimeZoneSettingValue(String value) {
        TimeZone tz;
        if (JVM_DEFAULT.equalsIgnoreCase(value)) {
            tz = TimeZone.getDefault();
        } else {
            tz = TimeZone.getTimeZone(value);
        }
        return tz;
    }

    public void setStrictBeanModels(boolean strict) {
	if (!(objectWrapper instanceof BeansWrapper)) {
	    throw new IllegalStateException("The value of the " + OBJECT_WRAPPER_KEY +
	            " setting isn't a " + BeansWrapper.class.getName() + ".");
	}
	((BeansWrapper) objectWrapper).setStrict(strict);
    }
    
    /**
     * Returns the textual representation of a setting.
     * @param key the setting key. Can be any of standard <tt>XXX_KEY</tt>
     * constants, or a custom key.
     *
     * @deprecated It's not possible in general to convert setting values to string,
     *     and thus it's impossible to ensure that {@link #setSetting(String, String)} will work with
     *     the returned value correctly.
     */
    public String getSetting(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * This meant to return the String-to-String <code>Map</code> of the
     * settings. So it actually should return a <code>Properties</code> object,
     * but it doesn't by mistake. The returned <code>Map</code> is read-only,
     * but it will reflect the further configuration changes (aliasing effect).
     *
     * @deprecated This method was always defective, and certainly it always
     *     will be. Don't use it. (Simply, it's hardly possible in general to
     *     convert setting values to text in a way that ensures that
     *     {@link #setSettings(Properties)} will work with them correctly.)
     */
    public Map getSettings() {
        return Collections.unmodifiableMap(properties);
    }
    
    protected Environment getEnvironment() {
        return this instanceof Environment
            ? (Environment) this
            : Environment.getCurrentEnvironment();
    }
    
    /**
     * Creates the exception that should be thrown when a setting name isn't recognized.
     */
    protected TemplateException unknownSettingException(String name) {
        return new UnknownSettingException(
                getEnvironment(), name, getCorrectedNameForUnknownSetting(name));
    }

    /**
     * @param name The wrong name
     * @return The corrected name, or {@code null} if there's no known correction
     * @since 2.3.21
     */
    protected String getCorrectedNameForUnknownSetting(String name) {
        return null;
    }
    
    /**
     * @since 2.3.21
     */
    protected TemplateException settingValueAssignmentException(String name, String value, Throwable cause) {
        return new SettingValueAssignmentException(getEnvironment(), name, value, cause);
    }
    
    protected TemplateException invalidSettingValueException(String name, String value) {
        return new _MiscTemplateException(getEnvironment(), new Object[] {
                "Invalid value for setting ", new _DelayedJQuote(name), ": ",
                new _DelayedJQuote(value) });
    }
    
    /**
     * The setting name was not recognized. 
     */
    public static class UnknownSettingException extends _MiscTemplateException {

        private UnknownSettingException(Environment env, String name, String correctedName) {
            super(env, new Object[] {
                    "Unknown FreeMarker configuration setting: ", new _DelayedJQuote(name),
                    correctedName == null
                            ? (Object) "" : new Object[] { ". You may meant: ", new _DelayedJQuote(correctedName) } });
        }
        
    }

    /**
     * The setting name was recognized, but its value couldn't be parsed or the setting couldn't be set for some 
     * other reason. This exception always has a cause exception.
     *  
     * @since 2.3.21
     */
    public static class SettingValueAssignmentException extends _MiscTemplateException {
        
        private SettingValueAssignmentException(Environment env, String name, String value, Throwable cause) {
            super(cause, env, new Object[] {
                    "Failed to set FreeMarker configuration setting ", new _DelayedJQuote(name),
                    " to value ", new _DelayedJQuote(value), "; see cause exception." });
        }
        
    }
    
    /**
     * Set the settings stored in a <code>Properties</code> object.
     * 
     * @throws TemplateException if the <code>Properties</code> object contains
     *     invalid keys, or invalid setting values, or any other error occurs
     *     while changing the settings.
     */    
    public void setSettings(Properties props) throws TemplateException {
        final _SettingEvaluationEnvironment prevEnv = _SettingEvaluationEnvironment.startScope();
        try {
            for (Iterator it = props.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                setSetting(key, props.getProperty(key).trim()); 
            }
        } finally {
            _SettingEvaluationEnvironment.endScope(prevEnv);
        }
    }
    
    /**
     * Reads a setting list (key and element pairs) from the input stream.
     * The stream has to follow the usual <code>.properties</code> format.
     *
     * @throws TemplateException if the stream contains
     *     invalid keys, or invalid setting values, or any other error occurs
     *     while changing the settings.
     * @throws IOException if an error occurred when reading from the input stream.
     */
    public void setSettings(InputStream propsIn) throws TemplateException, IOException {
        Properties p = new Properties();
        p.load(propsIn);
        setSettings(p);
    }

    /**
     * Internal entry point for setting unnamed custom attributes
     */
    void setCustomAttribute(Object key, Object value) {
        synchronized(customAttributes) {
            customAttributes.put(key, value);
        }
    }

    /**
     * Internal entry point for getting unnamed custom attributes
     */
    Object getCustomAttribute(Object key, CustomAttribute attr) {
        synchronized(customAttributes) {
            Object o = customAttributes.get(key);
            if(o == null && !customAttributes.containsKey(key)) {
                o = attr.create();
                customAttributes.put(key, o);
            }
            return o;
        }
    }
    
    /**
     * Sets a named custom attribute for this configurable.
     *
     * @param name the name of the custom attribute
     * @param value the value of the custom attribute. You can set the value to
     * null, however note that there is a semantic difference between an
     * attribute set to null and an attribute that is not present, see
     * {@link #removeCustomAttribute(String)}.
     */
    public void setCustomAttribute(String name, Object value) {
        synchronized(customAttributes) {
            customAttributes.put(name, value);
        }
    }
    
    /**
     * Returns an array with names of all custom attributes defined directly 
     * on this configurable. (That is, it doesn't contain the names of custom attributes
     * defined indirectly on its parent configurables.) The returned array is never null,
     * but can be zero-length.
     * The order of elements in the returned array is not defined and can change
     * between invocations.  
     */
    public String[] getCustomAttributeNames() {
        synchronized(customAttributes) {
            Collection names = new LinkedList(customAttributes.keySet());
            for (Iterator iter = names.iterator(); iter.hasNext();) {
                if(!(iter.next() instanceof String)) {
                    iter.remove();
                }
            }
            return (String[])names.toArray(new String[names.size()]);
        }
    }
    
    /**
     * Removes a named custom attribute for this configurable. Note that this
     * is different than setting the custom attribute value to null. If you
     * set the value to null, {@link #getCustomAttribute(String)} will return
     * null, while if you remove the attribute, it will return the value of
     * the attribute in the parent configurable (if there is a parent 
     * configurable, that is). 
     *
     * @param name the name of the custom attribute
     */
    public void removeCustomAttribute(String name) {
        synchronized(customAttributes) {
            customAttributes.remove(name);
        }
    }

    /**
     * Retrieves a named custom attribute for this configurable. If the 
     * attribute is not present in the configurable, and the configurable has
     * a parent, then the parent is looked up as well.
     *
     * @param name the name of the custom attribute
     *
     * @return the value of the custom attribute. Note that if the custom attribute
     * was created with <tt>&lt;#ftl&nbsp;attributes={...}&gt;</tt>, then this value is already
     * unwrapped (i.e. it's a <code>String</code>, or a <code>List</code>, or a
     * <code>Map</code>, ...etc., not a FreeMarker specific class).
     */
    public Object getCustomAttribute(String name) {
        Object retval;
        synchronized(customAttributes) {
            retval = customAttributes.get(name);
            if(retval == null && customAttributes.containsKey(name)) {
                return null;
            }
        }
        if(retval == null && parent != null) {
            return parent.getCustomAttribute(name);
        }
        return retval;
    }
    
    protected void doAutoImportsAndIncludes(Environment env)
    throws TemplateException, IOException
    {
        if(parent != null) parent.doAutoImportsAndIncludes(env);
    }

    protected ArrayList parseAsList(String text) throws ParseException {
        return new SettingStringParser(text).parseAsList();
    }

    protected ArrayList parseAsSegmentedList(String text)
    throws ParseException {
        return new SettingStringParser(text).parseAsSegmentedList();
    }
    
    protected HashMap parseAsImportList(String text) throws ParseException {
        return new SettingStringParser(text).parseAsImportList();
    }
    
    private static class KeyValuePair {
        private final Object key;
        private final Object value;
        
        KeyValuePair(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
        
        Object getKey() {
            return key;
        }
        
        Object getValue() {
            return value;
        }
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
            this.p = 0;
            this.ln = text.length();
        }

        ArrayList parseAsSegmentedList() throws ParseException {
            ArrayList segments = new ArrayList();
            ArrayList currentSegment = null;
            
            char c;
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String item = fetchStringValue();
                c = skipWS();
                
                if (c == ':') {
                    currentSegment = new ArrayList();
                    segments.add(new KeyValuePair(item, currentSegment));
                } else {
                    if (currentSegment == null) {
                        throw new ParseException(
                                "The very first list item must be followed by \":\" so " +
                                "it will be the key for the following sub-list.",
                                0, 0);
                    }
                    currentSegment.add(item);
                }
                
                if (c == ' ') break;
                if (c != ',' && c != ':') throw new ParseException(
                        "Expected \",\" or \":\" or the end of text but " +
                        "found \"" + c + "\"", 0, 0);
                p++;
            }
            return segments;
        }

        ArrayList parseAsList() throws ParseException {
            char c;
            ArrayList seq = new ArrayList();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                seq.add(fetchStringValue());
                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new ParseException(
                        "Expected \",\" or the end of text but " +
                        "found \"" + c + "\"", 0, 0);
                p++;
            }
            return seq;
        }

        HashMap parseAsImportList() throws ParseException {
            char c;
            HashMap map = new HashMap();
            while (true) {
                c = skipWS();
                if (c == ' ') break;
                String lib = fetchStringValue();

                c = skipWS();
                if (c == ' ') throw new ParseException(
                        "Unexpected end of text: expected \"as\"", 0, 0);
                String s = fetchKeyword();
                if (!s.equalsIgnoreCase("as")) throw new ParseException(
                        "Expected \"as\", but found " + StringUtil.jQuote(s), 0, 0);

                c = skipWS();
                if (c == ' ') throw new ParseException(
                        "Unexpected end of text: expected gate hash name", 0, 0);
                String ns = fetchStringValue();
                
                map.put(ns, lib);

                c = skipWS();
                if (c == ' ') break;
                if (c != ',') throw new ParseException(
                        "Expected \",\" or the end of text but "
                        + "found \"" + c + "\"", 0, 0);
                p++;
            }
            return map;
        }

        String fetchStringValue() throws ParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                w = w.substring(1, w.length() - 1);
            }
            return StringUtil.FTLStringLiteralDec(w);
        }

        String fetchKeyword() throws ParseException {
            String w = fetchWord();
            if (w.startsWith("'") || w.startsWith("\"")) {
                throw new ParseException(
                    "Keyword expected, but a string value found: " + w, 0, 0);
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

        private String fetchWord() throws ParseException {
            if (p == ln) throw new ParseException(
                    "Unexpeced end of text", 0, 0);

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
                    throw new ParseException("Missing " + q, 0, 0);
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
                    throw new ParseException("Unexpected character: " + c, 0, 0);
                } else {
                    return text.substring(b, p);
                }
            }
        }
    }
    
}

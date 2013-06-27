/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
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

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;
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
 *
 * @author Attila Szegedi
 */
public class Configurable
{
    static final String C_TRUE_FALSE = "true,false";
    
    public static final String LOCALE_KEY = "locale";
    public static final String NUMBER_FORMAT_KEY = "number_format";
    public static final String TIME_FORMAT_KEY = "time_format";
    public static final String DATE_FORMAT_KEY = "date_format";
    public static final String DATETIME_FORMAT_KEY = "datetime_format";
    public static final String TIME_ZONE_KEY = "time_zone";
    public static final String CLASSIC_COMPATIBLE_KEY = "classic_compatible";
    public static final String TEMPLATE_EXCEPTION_HANDLER_KEY = "template_exception_handler";
    public static final String ARITHMETIC_ENGINE_KEY = "arithmetic_engine";
    public static final String OBJECT_WRAPPER_KEY = "object_wrapper";
    public static final String BOOLEAN_FORMAT_KEY = "boolean_format";
    public static final String OUTPUT_ENCODING_KEY = "output_encoding";
    public static final String URL_ESCAPING_CHARSET_KEY = "url_escaping_charset";
    public static final String STRICT_BEAN_MODELS = "strict_bean_models";
    /** @since 2.3.17 */
    public static final String AUTO_FLUSH_KEY = "auto_flush";
    /** @since 2.3.17 */
    public static final String NEW_BUILTIN_CLASS_RESOLVER_KEY = "new_builtin_class_resolver";

    private Configurable parent;
    private Properties properties;
    private HashMap customAttributes;
    
    private Locale locale;
    private String numberFormat;
    private String timeFormat;
    private String dateFormat;
    private String dateTimeFormat;
    private TimeZone timeZone;
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
    
    /**
     * Creates a top-level configurable, one that doesn't ingerit from a parent, and thus stores the default values.
     * The only class that should use this is {@link Configuration}.
     */
    public Configurable() {
        parent = null;
        locale = Locale.getDefault();
        timeZone = TimeZone.getDefault();
        numberFormat = "number";
        timeFormat = "";
        dateFormat = "";
        dateTimeFormat = "";
        classicCompatible = new Integer(0);
        templateExceptionHandler = TemplateExceptionHandler.DEBUG_HANDLER;
        arithmeticEngine = ArithmeticEngine.BIGDECIMAL_ENGINE;
        objectWrapper = ObjectWrapper.DEFAULT_WRAPPER;
        autoFlush = Boolean.TRUE;
        newBuiltinClassResolver = TemplateClassResolver.UNRESTRICTED_RESOLVER;
        // outputEncoding and urlEscapingCharset defaults to null,
        // which means "not specified"
        
        properties = new Properties();
        properties.setProperty(LOCALE_KEY, locale.toString());
        properties.setProperty(TIME_FORMAT_KEY, timeFormat);
        properties.setProperty(DATE_FORMAT_KEY, dateFormat);
        properties.setProperty(DATETIME_FORMAT_KEY, dateTimeFormat);
        properties.setProperty(TIME_ZONE_KEY, timeZone.getID());
        properties.setProperty(NUMBER_FORMAT_KEY, numberFormat);
        properties.setProperty(CLASSIC_COMPATIBLE_KEY, classicCompatible.toString());
        properties.setProperty(TEMPLATE_EXCEPTION_HANDLER_KEY, templateExceptionHandler.getClass().getName());
        properties.setProperty(ARITHMETIC_ENGINE_KEY, arithmeticEngine.getClass().getName());
        properties.setProperty(AUTO_FLUSH_KEY, autoFlush.toString());
        properties.setProperty(NEW_BUILTIN_CLASS_RESOLVER_KEY, newBuiltinClassResolver.getClass().getName());
        // as outputEncoding and urlEscapingCharset defaults to null, 
        // they are not set

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
     * Returns the parent <tt>Configurable</tt> object of this object.
     * The parent stores the default values for this configurable. For example,
     * the parent of the {@link freemarker.template.Template} object is the
     * {@link freemarker.template.Configuration} object, so setting values not
     * specfied on template level are specified by the confuration object.
     *
     * @return the parent <tt>Configurable</tt> object, or null, if this is
     *    the root <tt>Configurable</tt> object.
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
     * Toggles the "Classic Compatibile" mode. For a comprehensive description
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
     *       in <tt>&lt;assign varname=expr></tt> directive, 
     *       or in <tt>${expr}</tt> directive,
     *       or in <tt>otherexpr == expr</tt>,
     *       or in <tt>otherexpr != expr</tt>, 
     *       or in <tt>hash[expr]</tt>,
     *       or in <tt>expr[keyOrIndex]</tt> (since 2.3.20),
     *       or in <tt>expr.key</tt> (since 2.3.20),
     *       then it's treated as empty string.
     *     </li>
     *     <li>as argument of <tt>&lt;list expr as item></tt> or 
     *       <tt>&lt;foreach item in expr></tt>, the loop body is not executed
     *       (as if it were a 0-length list)
     *     </li>
     *     <li>as argument of <tt>&lt;if></tt> directive, or on other places where a
     *       boolean expression is expected, it's treated as false
     *     </li>
     *   </ul>
     * </li>
     * <li>Non-boolean models are accepted in <tt>&lt;if></tt> directive,
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
     * <li>Scalar models supplied to <tt>&lt;list></tt> and 
     *   <tt>&lt;foreach></tt> are treated as a one-element list consisting
     *   of the passed model.
     * </li>
     * <li>Paths parameter of <tt>&lt;include></tt> will be interpreted as
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
     * Defaults to the system time zone (regardless of the "locale" FreeMarker setting), so in a server application you
     * probably want to set it explicitly in the {@link Environment} to match the preferred time zone of the visitor.
     */
    public void setTimeZone(TimeZone timeZone) {
        NullArgumentException.check("timeZone", timeZone);
        this.timeZone = timeZone;
        properties.setProperty(TIME_ZONE_KEY, timeZone.getID());
    }

    /**
     * Returns the assumed locale when searching for template files with no
     * explicit requested locale. Defaults to system locale.
     */
    public Locale getLocale() {
        return locale != null ? locale : parent.getLocale();
    }

    /**
     * Sets the number format used to convert numbers to strings.
     */
    public void setNumberFormat(String numberFormat) {
        NullArgumentException.check("numberFormat", numberFormat);
        this.numberFormat = numberFormat;
        properties.setProperty(NUMBER_FORMAT_KEY, numberFormat);
    }

    /**
     * Returns the default number format used to convert numbers to strings.
     * Defaults to <tt>"number"</tt>
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
                    "Setting \"boolean_format\" must consist of two comma-separated values for true and false," +
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
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are time-only (not date part) values.
     * Possible values are patterns accepted by Java's {@link DateFormat}, also {@code "short"},
     * {@code "medium"}, {@code "long"} and {@code "full"} that has locale-dependent meaning also defined by
     * {@link DateFormat}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@link "medium"}.
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
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date-only (no time part) values.
     * Possible values are patterns accepted by Java's {@link DateFormat}, also {@code "short"},
     * {@code "medium"}, {@code "long"} and {@code "full"} that has locale-dependent meaning also defined by
     * {@link DateFormat}.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@link "medium"}.
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
     * Sets the format used to convert {@link java.util.Date}-s to string-s that are date+time values.
     * Possible values are patterns accepted by Java's {@link DateFormat}, also {@code "short"},
     * {@code "medium"}, {@code "long"} and {@code "full"} that has locale-dependent meaning also defined by
     * {@link DateFormat}.
     * It's also possible to give values like {@code "short_long"} (in any combinations), which will
     * use {@code "short"} for the date part, and {@code "long"} for the time part.
     *   
     * <p>Defaults to {@code ""}, which means "use the FreeMarker default", which is currently {@link "medium"}.
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
     *   <li>In productions systems: {@link TemplateExceptionHandler#RETHROW_HANDLER}
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
        this.autoFlush = autoFlush ? Boolean.TRUE : Boolean.FALSE;
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
    
    private static final String ALLOWED_CLASSES = "allowed_classes";
    private static final String TRUSTED_TEMPLATES = "trusted_templates";
    
    /**
     * Sets a FreeMarker setting by a name and string value. If you can configure FreeMarker directly with Java (or
     * other programming language), you should use the dedicated setter methods instead (like
     * {@link #setObjectWrapper(ObjectWrapper)}. This meant to be used if you get the settings from somewhere
     * as text. Regardless, below you will find an overview of the settings available no matter how you set them. 
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
     *       <br>String value: If the value contains dot, then it's
     *       interpreted as a class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these predefined values (case insensitive):
     *       {@code "rethrow"} (means {@link TemplateExceptionHandler#RETHROW_HANDLER}),
     *       {@code "debug"} (means {@link TemplateExceptionHandler#DEBUG_HANDLER}),
     *       {@code "html_debug"} (means {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER}),
     *       {@code "ignore"}  (means {@link TemplateExceptionHandler#IGNORE_HANDLER}).
     *       
     *   <li><p>{@code "arithmetic_engine"}:
     *       See {@link #setArithmeticEngine(ArithmeticEngine)}.  
     *       <br>String value: If the value contains dot, then it's
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these special values (case insensitive):
     *       {@code "bigdecimal"}, {@code "conservative"}.
     *       
     *   <li><p>{@code "object_wrapper"}:
     *       See {@link #setObjectWrapper(ObjectWrapper)}.
     *       <br>String value: If the value contains dot, then it's
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these special values (case insensitive):
     *       {@code "default"} (means {@link ObjectWrapper#DEFAULT_WRAPPER}),
     *       {@code "simple"} (means {@link ObjectWrapper#SIMPLE_WRAPPER}),
     *       {@code "beans"} (means {@link BeansWrapper#DEFAULT_WRAPPER}),
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
     *       <br>String value: With the format as {@link TimeZone#getTimeZone} defines it.
     *       For example {@code "GMT-8:00"} or {@code "America/Los_Angeles"}
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
     *       <br>String value: The value must be one of these (ignore the quotation marks):
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
     *             <table style="width: auto; border-collapse: collapse" border="1">
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
     *         <li><p>Otherwise if the value contains dot, it's interpreted as
     *             a full-qualified class name, and the object will be created
     *             with its parameterless constructor.
     *       </ol>
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
     *       <br>String value: If the value contains dot, then it's
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then a {@link freemarker.cache.MruCacheStorage} will be used with the
     *       maximum strong and soft sizes specified with the setting value. Examples
     *       of valid setting values:
     *       
     *       <table style="width: auto; border-collapse: collapse" border="1">
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
     *       See {@link Configuration#setTemplateUpdateDelay}.
     *       <br>String value: Valid positive integer, the update delay measured in seconds.
     *       
     *   <li><p>{@code "tag_syntax"}:
     *       See {@link Configuration#setTagSyntax(int)}.
     *       <br>String value: Must be one of
     *       {@code "auto_detect"}, {@code "angle_bracket"}, and {@code "square_bracket"}. 
     *       
     *   <li><p>{@code "incompatible_improvements"}:
     *       See {@link Configuration#setIncompatibleImprovements(Version)}.
     *       <br>String value: version number like {@code 2.3.20}.
     *       
     *   <li><p>{@code "incompatible_enhancements"}:
     *       See: {@link Configuration#setIncompatibleEnhancements(String)}.
     *       This setting name is deprecated, use {@code "incompatible_improvements"} instead.
     * </ul>
     * 
     * @param name the name of the setting.
     * @param value the string that describes the new value of the setting.
     * 
     * @throws UnknownSettingException if the name is wrong.
     * @throws TemplateException if the new value of the setting can't be set for any other reasons.
     */
    public void setSetting(String name, String value) throws TemplateException {
        try {
            if (LOCALE_KEY.equals(name)) {
                setLocale(StringUtil.deduceLocale(value));
            } else if (NUMBER_FORMAT_KEY.equals(name)) {
                setNumberFormat(value);
            } else if (TIME_FORMAT_KEY.equals(name)) {
                setTimeFormat(value);
            } else if (DATE_FORMAT_KEY.equals(name)) {
                setDateFormat(value);
            } else if (DATETIME_FORMAT_KEY.equals(name)) {
                setDateTimeFormat(value);
            } else if (TIME_ZONE_KEY.equals(name)) {
                setTimeZone(TimeZone.getTimeZone(value));
            } else if (CLASSIC_COMPATIBLE_KEY.equals(name)) {
                char firstChar;
                if (value != null && value.length() > 0) {
                    firstChar =  value.charAt(0);
                } else {
                    firstChar = 0;
                }
                if (Character.isDigit(firstChar) || firstChar == '+' || firstChar == '-') {
                    setClassicCompatibleAsInt(Integer.parseInt(value));
                } else {
                    setClassicCompatible(StringUtil.getYesNo(value));
                }
            } else if (TEMPLATE_EXCEPTION_HANDLER_KEY.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("debug".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.DEBUG_HANDLER);
                    } else if ("html_debug".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.HTML_DEBUG_HANDLER);
                    } else if ("ignore".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.IGNORE_HANDLER);
                    } else if ("rethrow".equalsIgnoreCase(value)) {
                        setTemplateExceptionHandler(
                                TemplateExceptionHandler.RETHROW_HANDLER);
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setTemplateExceptionHandler(
                            (TemplateExceptionHandler) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (ARITHMETIC_ENGINE_KEY.equals(name)) {
                if (value.indexOf('.') == -1) { 
                    if ("bigdecimal".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.BIGDECIMAL_ENGINE);
                    } else if ("conservative".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.CONSERVATIVE_ENGINE);
                    } else {
                        throw invalidSettingValueException(name, value);
                    }
                } else {
                    setArithmeticEngine(
                            (ArithmeticEngine) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (OBJECT_WRAPPER_KEY.equals(name)) {
                if (value.indexOf('.') == -1) {
                    if ("default".equalsIgnoreCase(value)) {
                        setObjectWrapper(ObjectWrapper.DEFAULT_WRAPPER);
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
                        throw invalidSettingValueException(name, value);
                    }
                    
                } else {
                    setObjectWrapper((ObjectWrapper) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (BOOLEAN_FORMAT_KEY.equals(name)) {
                setBooleanFormat(value);
            } else if (OUTPUT_ENCODING_KEY.equals(name)) {
                setOutputEncoding(value);
            } else if (URL_ESCAPING_CHARSET_KEY.equals(name)) {
                setURLEscapingCharset(value);
            } else if (STRICT_BEAN_MODELS.equals(name)) {
                setStrictBeanModels(StringUtil.getYesNo(value));
            } else if (AUTO_FLUSH_KEY.equals(name)) {
                setAutoFlush(StringUtil.getYesNo(value));
            } else if (NEW_BUILTIN_CLASS_RESOLVER_KEY.equals(name)) {
                if ("unrestricted".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.UNRESTRICTED_RESOLVER);
                } else if ("safer".equals(value)) {
                    setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
                } else if ("allows_nothing".equals(value)) {
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
                } else if (value.indexOf('.') == -1) {
                    setNewBuiltinClassResolver((TemplateClassResolver) ClassUtil.forName(value)
                            .newInstance());
                } else {
                    throw invalidSettingValueException(name, value);
                }
            } else {
                throw unknownSettingException(name);
            }
        } catch(Exception e) {
            throw new _MiscTemplateException(e, getEnvironment(), new Object[] {
                    "Failed to set setting ", new _DelayedJQuote(name),
                    " to value ", new _DelayedJQuote(value), "; see cause exception." });
        }
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
     * @deprecated This method was always defective, and certainly it always
     *     will be. Don't use it. (Simply, it's hardly possible in general to
     *     convert setting values to text in a way that ensures that
     *     {@link #setSetting(String, String)} will work with them correctly.)
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
    
    protected TemplateException unknownSettingException(String name) {
        return new UnknownSettingException(name, getEnvironment());
    }

    protected TemplateException invalidSettingValueException(String name, String value) {
        return new _MiscTemplateException(getEnvironment(), new Object[] {
                "Invalid value for setting ", new _DelayedJQuote(name), ": ",
                new _DelayedJQuote(value) });
    }
    
    public static class UnknownSettingException extends _MiscTemplateException {
        private UnknownSettingException(String name, Environment env) {
            super(env, new Object[] { "Unknown setting: ", new _DelayedJQuote(name) });
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
        Iterator it = props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            setSetting(key, props.getProperty(key).trim()); 
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
     * was created with <tt>&lt;#ftl&nbsp;attributes={...}></tt>, then this value is already
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

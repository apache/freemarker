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
import java.util.*;

import freemarker.template.*;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;
import freemarker.ext.beans.BeansWrapper;

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
 * @version $Id: Configurable.java,v 1.23.2.2 2007/04/02 13:46:43 szegedia Exp $
 * @author Attila Szegedi
 */
public class Configurable
{
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

    private static final char COMMA = ',';
    
    private Configurable parent;
    private Properties properties;
    private HashMap customAttributes;
    
    private Locale locale;
    private String numberFormat;
    private String timeFormat;
    private String dateFormat;
    private String dateTimeFormat;
    private TimeZone timeZone;
    private String trueFormat;
    private String falseFormat;
    private Boolean classicCompatible;
    private TemplateExceptionHandler templateExceptionHandler;
    private ArithmeticEngine arithmeticEngine;
    private ObjectWrapper objectWrapper;
    private String outputEncoding;
    private boolean outputEncodingSet;
    private String urlEscapingCharset;
    private boolean urlEscapingCharsetSet;
    private Boolean autoFlush;
    private TemplateClassResolver newBuiltinClassResolver;
    
    public Configurable() {
        parent = null;
        locale = Locale.getDefault();
        timeZone = TimeZone.getDefault();
        numberFormat = "number";
        timeFormat = "";
        dateFormat = "";
        dateTimeFormat = "";
        trueFormat = "true";
        falseFormat = "false";
        classicCompatible = Boolean.FALSE;
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
        properties.setProperty(BOOLEAN_FORMAT_KEY, "true,false");
        properties.setProperty(AUTO_FLUSH_KEY, autoFlush.toString());
        properties.setProperty(NEW_BUILTIN_CLASS_RESOLVER_KEY, newBuiltinClassResolver.getClass().getName());
        // as outputEncoding and urlEscapingCharset defaults to null, 
        // they are not set
        
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
        trueFormat = null;
        falseFormat = null;
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
        this.classicCompatible = classicCompatibility ? Boolean.TRUE : Boolean.FALSE;
        properties.setProperty(CLASSIC_COMPATIBLE_KEY, classicCompatible.toString());
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
     *     <li>as argument of the <tt>&lt;assign varname=expr></tt> directive, 
     *       <tt>${expr}</tt> directive, <tt>otherexpr == expr</tt> or 
     *       <tt>otherexpr != expr</tt> conditional expressions, or 
     *       <tt>hash[expr]</tt> expression, then it is treated as empty string.
     *     </li>
     *     <li>as argument of <tt>&lt;list expr as item></tt> or 
     *       <tt>&lt;foreach item in expr></tt>, the loop body is not executed
     *       (as if it were a 0-length list)
     *     </li>
     *     <li>as argument of <tt>&lt;if></tt> directive, or otherwise where a
     *       boolean expression is expected, it is treated as false
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
     * empty string.
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
        return classicCompatible != null ? classicCompatible.booleanValue() : parent.isClassicCompatible();
    }

    /**
     * Sets the locale to assume when searching for template files with no 
     * explicit requested locale.
     */
    public void setLocale(Locale locale) {
        if (locale == null) throw new IllegalArgumentException("Setting \"locale\" can't be null");
        this.locale = locale;
        properties.setProperty(LOCALE_KEY, locale.toString());
    }

    /**
     * Returns the time zone to use when formatting time values. Defaults to 
     * system time zone.
     */
    public TimeZone getTimeZone() {
        return timeZone != null ? timeZone : parent.getTimeZone();
    }

    /**
     * Sets the time zone to use when formatting time values. 
     */
    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null) throw new IllegalArgumentException("Setting \"time_zone\" can't be null");
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
        if (numberFormat == null) throw new IllegalArgumentException("Setting \"number_format\" can't be null");
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

    public void setBooleanFormat(String booleanFormat) {
        if (booleanFormat == null) {
            throw new IllegalArgumentException("Setting \"boolean_format\" can't be null");
        } 
        int comma = booleanFormat.indexOf(COMMA);
        if(comma == -1) {
            throw new IllegalArgumentException("Setting \"boolean_format\" must consist of two comma-separated values for true and false respectively");
        }
        trueFormat = booleanFormat.substring(0, comma);
        falseFormat = booleanFormat.substring(comma + 1);
        properties.setProperty(BOOLEAN_FORMAT_KEY, booleanFormat);
    }
    
    public String getBooleanFormat() {
        if(trueFormat == null) {
            return parent.getBooleanFormat(); 
        }
        return trueFormat + COMMA + falseFormat;
    }
    
    String getBooleanFormat(boolean value) {
        return value ? getTrueFormat() : getFalseFormat(); 
    }
    
    private String getTrueFormat() {
        return trueFormat != null ? trueFormat : parent.getTrueFormat(); 
    }
    
    private String getFalseFormat() {
        return falseFormat != null ? falseFormat : parent.getFalseFormat(); 
    }

    /**
     * Sets the date format used to convert date models representing time-only
     * values to strings.
     */
    public void setTimeFormat(String timeFormat) {
        if (timeFormat == null) throw new IllegalArgumentException("Setting \"time_format\" can't be null");
        this.timeFormat = timeFormat;
        properties.setProperty(TIME_FORMAT_KEY, timeFormat);
    }

    /**
     * Returns the date format used to convert date models representing
     * time-only dates to strings.
     * Defaults to <tt>"time"</tt>
     */
    public String getTimeFormat() {
        return timeFormat != null ? timeFormat : parent.getTimeFormat();
    }

    /**
     * Sets the date format used to convert date models representing date-only
     * dates to strings.
     */
    public void setDateFormat(String dateFormat) {
        if (dateFormat == null) throw new IllegalArgumentException("Setting \"date_format\" can't be null");
        this.dateFormat = dateFormat;
        properties.setProperty(DATE_FORMAT_KEY, dateFormat);
    }

    /**
     * Returns the date format used to convert date models representing 
     * date-only dates to strings.
     * Defaults to <tt>"date"</tt>
     */
    public String getDateFormat() {
        return dateFormat != null ? dateFormat : parent.getDateFormat();
    }

    /**
     * Sets the date format used to convert date models representing datetime
     * dates to strings.
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        if (dateTimeFormat == null) throw new IllegalArgumentException("Setting \"datetime_format\" can't be null");
        this.dateTimeFormat = dateTimeFormat;
        properties.setProperty(DATETIME_FORMAT_KEY, dateTimeFormat);
    }

    /**
     * Returns the date format used to convert date models representing datetime
     * dates to strings.
     * Defaults to <tt>"datetime"</tt>
     */
    public String getDateTimeFormat() {
        return dateTimeFormat != null ? dateTimeFormat : parent.getDateTimeFormat();
    }

    /**
     * Sets the exception handler used to handle template exceptions. 
     *
     * @param templateExceptionHandler the template exception handler to use for 
     * handling {@link TemplateException}s. By default, 
     * {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER} is used.
     */
    public void setTemplateExceptionHandler(TemplateExceptionHandler templateExceptionHandler) {
        if (templateExceptionHandler == null) throw new IllegalArgumentException("Setting \"template_exception_handler\" can't be null");
        this.templateExceptionHandler = templateExceptionHandler;
        properties.setProperty(TEMPLATE_EXCEPTION_HANDLER_KEY, templateExceptionHandler.getClass().getName());
    }

    /**
     * Retrieves the exception handler used to handle template exceptions. 
     */
    public TemplateExceptionHandler getTemplateExceptionHandler() {
        return templateExceptionHandler != null
                ? templateExceptionHandler : parent.getTemplateExceptionHandler();
    }

    /**
     * Sets the arithmetic engine used to perform arithmetic operations.
     *
     * @param arithmeticEngine the arithmetic engine used to perform arithmetic
     * operations.By default, {@link ArithmeticEngine#BIGDECIMAL_ENGINE} is 
     * used.
     */
    public void setArithmeticEngine(ArithmeticEngine arithmeticEngine) {
        if (arithmeticEngine == null) throw new IllegalArgumentException("Setting \"arithmetic_engine\" can't be null");
        this.arithmeticEngine = arithmeticEngine;
        properties.setProperty(ARITHMETIC_ENGINE_KEY, arithmeticEngine.getClass().getName());
    }

    /**
     * Retrieves the arithmetic engine used to perform arithmetic operations.
     */
    public ArithmeticEngine getArithmeticEngine() {
        return arithmeticEngine != null
                ? arithmeticEngine : parent.getArithmeticEngine();
    }

    /**
     * Sets the object wrapper used to wrap objects to template models.
     *
     * @param objectWrapper the object wrapper used to wrap objects to template
     * models.By default, {@link ObjectWrapper#DEFAULT_WRAPPER} is used.
     */
    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        if (objectWrapper == null) throw new IllegalArgumentException("Setting \"object_wrapper\" can't be null");
        this.objectWrapper = objectWrapper;
        properties.setProperty(OBJECT_WRAPPER_KEY, objectWrapper.getClass().getName());
    }

    /**
     * Retrieves the object wrapper used to wrap objects to template models.
     */
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper != null
                ? objectWrapper : parent.getObjectWrapper();
    }
    
    /**
     * Sets the output encoding. Allows <code>null</code>, which means that the
     * output encoding is not known.
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
     * Sets the URL escaping charset. Allows <code>null</code>, which means that the
     * output encoding will be used for URL escaping.
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
        if (newBuiltinClassResolver == null) throw new IllegalArgumentException(
                "Setting \"" + NEW_BUILTIN_CLASS_RESOLVER_KEY + "\" can't be null.");
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
     * Sets a setting by a name and string value.
     * 
     * <p>List of supported names and their valid values:
     * <ul>
     *   <li><code>"locale"</code>: local codes with the usual format, such as <code>"en_US"</code>.
     *   <li><code>"classic_compatible"</code>:
     *       <code>"true"</code>, <code>"false"</code>, <code>"yes"</code>, <code>"no"</code>,
     *       <code>"t"</code>, <code>"f"</code>, <code>"y"</code>, <code>"n"</code>.
     *       Case insensitive.
     *   <li><code>"template_exception_handler"</code>:  If the value contains dot, then it is
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these special values:
     *       <code>"rethrow"</code>, <code>"debug"</code>,
     *       <code>"html_debug"</code>, <code>"ignore"</code> (case insensitive).
     *   <li><code>"arithmetic_engine"</code>: If the value contains dot, then it is
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these special values:
     *       <code>"bigdecimal"</code>, <code>"conservative"</code> (case insensitive).  
     *   <li><code>"object_wrapper"</code>: If the value contains dot, then it is
     *       interpreted as class name, and the object will be created with
     *       its parameterless constructor. If the value does not contain dot,
     *       then it must be one of these special values:
     *       <code>"simple"</code>, <code>"beans"</code>, <code>"jython"</code> (case insensitive).
     *   <li><code>"number_format"</code>: pattern as <code>java.text.DecimalFormat</code> defines.
     *   <li><code>"boolean_format"</code>: the textual value for boolean true and false,
     *       separated with comma. For example <code>"yes,no"</code>.
     *   <li><code>"date_format", "time_format", "datetime_format"</code>: patterns as
     *       <code>java.text.SimpleDateFormat</code> defines.
     *   <li><code>"time_zone"</code>: time zone, with the format as
     *       <code>java.util.TimeZone.getTimeZone</code> defines. For example <code>"GMT-8:00"</code> or
     *       <code>"America/Los_Angeles"</code>
     *   <li><code>"output_encoding"</code>: Informs FreeMarker about the charset
     *       used for the output. As FreeMarker outputs character stream (not
     *       byte stream), it is not aware of the output charset unless the
     *       software that encloses it tells it explicitly with this setting.
     *       Some templates may use FreeMarker features that require this.</code>
     *   <li><code>"url_escaping_charset"</code>: If this setting is set, then it
     *       overrides the value of the <code>"output_encoding"</code> setting when
     *       FreeMarker does URL encoding.
     *   <li><code>"auto_flush"</code>: see {@link #setAutoFlush(boolean)}.
     *       Since 2.3.17.
     *   <li><code>"new_builtin_class_resolver"</code>:
     *       see {@link #setNewBuiltinClassResolver(TemplateClassResolver)}.
     *       Since 2.3.17. The value must be one of these (ignore the
     *       quotation marks):
     *       <ol>
     *         <li><code>"unrestricted"</code>:
     *             Use {@link TemplateClassResolver#UNRESTRICTED_RESOLVER}
     *         <li><code>"safer"</code>:
     *             Use {@link TemplateClassResolver#SAFER_RESOLVER}
     *         <li><code>"allows_nothing"</code>:
     *             Use {@link TemplateClassResolver#ALLOWS_NOTHING_RESOLVER}
     *         <li>Something that contains colon will use
     *             {@link OptInTemplateClassResolver} and is expected to
     *             store comma separated values (possibly quoted) segmented
     *             with <code>"allowed_classes:"</code> and/or
     *             <code>"trusted_templates:"</code>. Examples of valid values:
     *             
     *             <table border="1">
     *               <tr>
     *                 <th>Setting value
     *                 <th>Meaning
     *               <tr>
     *                 <td>
                         <code>allowed_classes: com.example.C1, com.example.C2,
     *                   trusted_templates: lib/*, safe.ftl</code>                 
     *                 <td>
     *                   Only allow instantiating the <code>com.example.C1</code> and
     *                   <code>com.example.C2</code> classes. But, allow templates
     *                   within the <code>lib/</code> directory (like
     *                   <code>lib/foo/bar.ftl</code>) and template <code>safe.ftl</code>
     *                   (that does not match <code>foo/safe.ftl</code>, only
     *                   exactly <code>safe.ftl</code>) to instantiate anything
     *                   that {@link TemplateClassResolver#SAFER_RESOLVER} allows.
     *               <tr>
     *                 <td>
     *                   <code>allowed_classes: com.example.C1, com.example.C2</code>
     *                 <td>Only allow instantiating the <code>com.example.C1</code> and
     *                   <code>com.example.C2</code> classes. There are no
     *                   trusted templates.
     *               <tr>
     *                 <td>
                         <code>trusted_templates: lib/*, safe.ftl</code>                 
     *                 <td>
     *                   Do not allow instantiating any classes, except in
     *                   templates inside <code>lib/</code> or in template 
     *                   <code>safe.ftl</code>.
     *             </table>
     *             For more details see {@link OptInTemplateClassResolver}.
     *         <li>Otherwise if the value contains dot, it's interpreted as
     *             a full-qualified class name, and the object will be created
     *             with its parameterless constructor.
     *       </ol>
     * </ul>
     * 
     * @param key the name of the setting.
     * @param value the string that describes the new value of the setting.
     * 
     * @throws UnknownSettingException if the key is wrong.
     * @throws TemplateException if the new value of the setting can't be set
     *     for any other reasons.
     */
    public void setSetting(String key, String value) throws TemplateException {
        try {
            if (LOCALE_KEY.equals(key)) {
                setLocale(StringUtil.deduceLocale(value));
            } else if (NUMBER_FORMAT_KEY.equals(key)) {
                setNumberFormat(value);
            } else if (TIME_FORMAT_KEY.equals(key)) {
                setTimeFormat(value);
            } else if (DATE_FORMAT_KEY.equals(key)) {
                setDateFormat(value);
            } else if (DATETIME_FORMAT_KEY.equals(key)) {
                setDateTimeFormat(value);
            } else if (TIME_ZONE_KEY.equals(key)) {
                setTimeZone(TimeZone.getTimeZone(value));
            } else if (CLASSIC_COMPATIBLE_KEY.equals(key)) {
                setClassicCompatible(StringUtil.getYesNo(value));
            } else if (TEMPLATE_EXCEPTION_HANDLER_KEY.equals(key)) {
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
                        throw invalidSettingValueException(key, value);
                    }
                } else {
                    setTemplateExceptionHandler(
                            (TemplateExceptionHandler) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (ARITHMETIC_ENGINE_KEY.equals(key)) {
                if (value.indexOf('.') == -1) { 
                    if ("bigdecimal".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.BIGDECIMAL_ENGINE);
                    } else if ("conservative".equalsIgnoreCase(value)) {
                        setArithmeticEngine(ArithmeticEngine.CONSERVATIVE_ENGINE);
                    } else {
                        throw invalidSettingValueException(key, value);
                    }
                } else {
                    setArithmeticEngine(
                            (ArithmeticEngine) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (OBJECT_WRAPPER_KEY.equals(key)) {
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
                        throw invalidSettingValueException(key, value);
                    }
                    
                } else {
                    setObjectWrapper((ObjectWrapper) ClassUtil.forName(value)
                            .newInstance());
                }
            } else if (BOOLEAN_FORMAT_KEY.equals(key)) {
                setBooleanFormat(value);
            } else if (OUTPUT_ENCODING_KEY.equals(key)) {
                setOutputEncoding(value);
            } else if (URL_ESCAPING_CHARSET_KEY.equals(key)) {
                setURLEscapingCharset(value);
            } else if (STRICT_BEAN_MODELS.equals(key)) {
                setStrictBeanModels(StringUtil.getYesNo(value));
            } else if (AUTO_FLUSH_KEY.equals(key)) {
                setAutoFlush(StringUtil.getYesNo(value));
            } else if (NEW_BUILTIN_CLASS_RESOLVER_KEY.equals(key)) {
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
                    throw invalidSettingValueException(key, value);
                }
            } else {
                throw unknownSettingException(key);
            }
        } catch(Exception e) {
            throw new TemplateException(
                    "Failed to set setting " + 
                    StringUtil.jQuote(key) + " to value " + StringUtil.jQuote(value) +
                    "; see cause exception.",
                    e, getEnvironment());
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
        return new TemplateException("Invalid value for setting " + name + ": " + value, getEnvironment());
    }
    
    public static class UnknownSettingException extends TemplateException {
        private UnknownSettingException(String name, Environment env) {
            super("Unknown setting: " + name, env);
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

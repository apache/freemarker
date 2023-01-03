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
import org.apache.freemarker.core.cformat.CFormat;
import org.apache.freemarker.core.cformat.impl.JavaScriptOrJSONCFormat;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.impl.MemberAccessPolicy;
import org.apache.freemarker.core.pluggablebuiltin.TruncateBuiltinAlgorithm;
import org.apache.freemarker.core.pluggablebuiltin.impl.DefaultTruncateBuiltinAlgorithm;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;

import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implemented by FreeMarker core classes (not by you) that provide configuration settings that affect {@linkplain
 * Template#process(Object, Writer) template processing} (as opposed to template parsing). <b>New methods may be added
 * any time in future FreeMarker versions, so don't try to implement this interface yourself!</b>
 *
 * @see ParsingConfiguration
 * @see ParsingAndProcessingConfiguration
 */
public interface ProcessingConfiguration {

    /**
     * Useful as the default value parameter to {#getCustomSetting(Serializable, Object)}, because this value is not
     * allowed for custom settings.
     */
    Object MISSING_VALUE_MARKER = new Object();

    /**
     * The locale used for number and date formatting (among others), also the locale used for searching localized
     * template variations when no locale was explicitly specified where the template is requested.
     *
     * @see Configuration#getTemplate(String, Locale)
     */
    Locale getLocale();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isLocaleSet();

    /**
     * The time zone to use when formatting date/time values. It {@link Configuration}-level default
     * is the system time zone ({@link TimeZone#getDefault()}), regardless of the "locale" FreeMarker setting,
     * so in a server application you probably want to set it explicitly in the {@link Environment} to match the
     * preferred time zone of the target audience (like the Web page visitor).
     *
     * <p>If you or the templates set the time zone, you should probably also set
     * {@link #getSQLDateAndTimeTimeZone()}!
     *
     * @see #getSQLDateAndTimeTimeZone()
     */
    TimeZone getTimeZone();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isTimeZoneSet();

    /**
     * The time zone used when dealing with {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. Its {@link Configuration}-level defaults is
     * {@link TimeZone#getDefault()}, that is, the JVM default time zone (server default time zone), because that's
     * what most JDBC drivers will use when constructing the {@link java.sql.Date java.sql.Date} and
     * {@link java.sql.Time java.sql.Time} values. If this setting is {@code null} FreeMarker will use the value of
     * ({@link #getTimeZone()}) for {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time}
     * values, which often gives bad results due to how most JDBC drivers work.
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
     *   default system time zone of the JDBC client, not just on the content of the database. (This used to be the
     *   default behavior of ORM-s, like Hibernate, too.)
     *
     *   <li>The value of the {@code timeZone} FreeMarker configuration setting sets the time zone used for the
     *   template output. For example, when a web page visitor has a preferred time zone, the web application framework
     *   may calls {@link Environment#setTimeZone(TimeZone)} with that time zone. Thus, the visitor will
     *   see {@link java.sql.Timestamp java.sql.Timestamp} and plain {@link java.util.Date java.util.Date} values as
     *   they look in his own time zone. While
     *   this is desirable for those types, as they meant to represent physical points on the time line, this is not
     *   necessarily desirable for date-only and time-only values. When {@code sqlDateAndTimeTimeZone} is
     *   {@code null}, {@code timeZone} is used for rendering all kind of date/time/dateTime values, including
     *   {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time}, and then if, for example,
     *   {@code timeZone} is GMT+00:00, the
     *   values from the earlier examples will be shown as 2014-07-11 (one day off) and 09:57:00 (2 hours off). While
     *   those are the time zone correct renderings, those values are probably meant to be shown "as is".
     *
     *   <li>You may wonder why this setting isn't simply "SQL time zone", that is, why's this time zone not applied to
     *   {@link java.sql.Timestamp java.sql.Timestamp} values as well. Timestamps in databases refer to a point on
     *   the physical time line, and thus doesn't have the inherent problem of date-only and time-only values.
     *   FreeMarker assumes that the JDBC driver converts time stamps coming from the database so that they store
     *   the distance from the epoch (1970-01-01 00:00:00 UTC), as requested by the {@link java.util.Date} API.
     *   Then time stamps can be safely rendered in different time zones, and thus need no special treatment.
     * </ul>
     *
     * @see #getTimeZone()
     */
    TimeZone getSQLDateAndTimeTimeZone();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isSQLDateAndTimeTimeZoneSet();

    /**
     * The number format used to convert numbers to strings (where no number format is explicitly given). Its
     * {@link Configuration}-level default is {@code "number"}. The possible values are:
     * <ul>
     *   <li>{@code "number"}: The number format returned by {@link NumberFormat#getNumberInstance(Locale)}</li>
     *   <li>{@code "c"}: The number format used by FTL's {@code c} built-in (like in
     *       {@code someNumber?c}). So with this <code>${someNumber}</code> will output the same as
     *       <code>${someNumber?c}</code>. This should only be used if the template solely generates source code,
     *       configuration file, or other content that's not read by normal users. If the template contains parts that's
     *       read by normal users (like typical a web page), you are not supposed to use this.</li>
     *   <li>{@code "currency"}: The number format returned by {@link NumberFormat#getCurrencyInstance(Locale)}</li>
     *   <li>{@code "percent"}: The number format returned by {@link NumberFormat#getPercentInstance(Locale)}</li>
     *   <li>A {@link java.text.DecimalFormat} pattern (like {@code "0.##"}). This syntax is extended by FreeMarker
     *       so that you can specify options like the rounding mode and the symbols used after a 2nd semicolon. For
     *       example, {@code ",000;; roundingMode=halfUp groupingSeparator=_"} will format numbers like {@code ",000"}
     *       would, but with half-up rounding mode, and {@code _} as the group separator. See more about "extended Java
     *       decimal format" in the FreeMarker Manual.
     *       </li>
     *   <li>If the string starts with {@code @} character followed by a letter then it's interpreted as a custom number
     *       format. The format of a such string is <code>"@<i>name</i>"</code> or <code>"@<i>name</i>
     *       <i>parameters</i>"</code>, where <code><i>name</i></code> is the key in the {@link Map} set by
     *       {@link MutableProcessingConfiguration#setCustomNumberFormats(Map)}, and <code><i>parameters</i></code> is
     *       parsed by the custom {@link TemplateNumberFormat}.
     *   </li>
     * </ul>
     */
    String getNumberFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isNumberFormatSet();

    /**
     * A {@link Map} that associates {@link TemplateNumberFormatFactory}-es to names, which then can be referred by the
     * {@link #getNumberFormat() numberFormat} setting with values starting with <code>@<i>name</i></code>. The keys in
     * the {@link Map} should start with an UNICODE letter, and should only contain UNICODE letters and digits (not
     * {@code _}), otherwise accessing the custom format from templates can be difficult or impossible. The
     * {@link Configuration}-level default of this setting is an empty  {@link Map}.
     * <p>
     * When the {@link ProcessingConfiguration} is part of a setting inheritance chain ({@link Environment} inherits
     * settings from the main {@link Template}, which inherits from the {@link Configuration}), you still only get the
     * {@link Map} from the closest {@link ProcessingConfiguration} where it was set, not a {@link Map} that respects
     * inheritance. Thus, to get a custom format you shouldn't use this {@link Map} directly, but
     * {@link #getCustomNumberFormat(String)}, which will search the format in the inheritance chain.
     *
     * @return Never {@code null}; unmodifiable {@link Map}.
     */
    Map<String, TemplateNumberFormatFactory> getCustomNumberFormats();

    /**
     * Gets the custom number format registered for the name. This differs from calling {@link #getCustomNumberFormats()
     * getCustomNumberFormats().get(name)}, because if there's {@link ProcessingConfiguration} from which setting values
     * are inherited then this method will search the custom format there as well if it isn't found here. For example,
     * {@link Environment#getCustomNumberFormat(String)} will check if the {@link Environment} contains the custom
     * format with the name, and if not, it will try {@link Template#getCustomNumberFormat(String)} on the main
     * template, which in turn might falls back to calling {@link Configuration#getCustomNumberFormat(String)}.
     */
    TemplateNumberFormatFactory getCustomNumberFormat(String name);

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isCustomNumberFormatsSet();

    /**
     * The string value for the boolean {@code true} and {@code false} values, usually intended for human consumption
     * (not for a computer language), separated with comma. For example, {@code "yes,no"}. Note that white-space is
     * significant, so {@code "yes, no"} is WRONG (unless you want that leading space before "no"). Because the proper
     * way of formatting booleans depends on the context too much, it's probably the best to leave this setting on its
     * default, {@code ""}, which will enforce explicit formatting, like <code>${aBoolean?string('on', 'off')}</code>.
     *
     * <p>If you generate the piece of output for "computer audience" as opposed to "human audience", then you should
     * write <code>${myBoolean?c}</code>, which will print {@code true} or {@code false}. If you really want to always
     * format for computer audience, then it's might be reasonable to set this setting to {@code c}.
     */
    String getBooleanFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isBooleanFormatSet();

    /**
     * The format used to convert {@link java.util.Date}-s that are time (no date part) values to string-s, also the
     * format that {@code someString?time} will use to parse strings.
     *
     * <p>For the possible values see {@link #getDateTimeFormat()}.
     *
     * <p>Its {@link Configuration}-level default is {@code ""}, which is equivalent to {@code "medium"}.
     */
    String getTimeFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isTimeFormatSet();

    /**
     * The format used to convert {@link java.util.Date}-s that are date-only (no time part) values to string-s,
     * also the format that {@code someString?date} will use to parse strings.
     *
     * <p>For the possible values see {@link #getDateTimeFormat()}.
     *
     * <p>Its {@link Configuration}-level default is {@code ""} which is equivalent to {@code "medium"}.
     */
    String getDateFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isDateFormatSet();

    /**
     * The format used to convert {@link java.util.Date}-s that are date-time (timestamp) values to string-s,
     * also the format that {@code someString?dateTime} will use to parse strings.
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
     *             {@code u} = Use UTC instead of what the {@code timeZone} setting suggests. However,
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} aren't affected
     *                     by this (see {@link #getSQLDateAndTimeTimeZone()} to understand why)<br>
     *             {@code fu} = "Force UTC", that is, use UTC instead of what the {@code timeZone} or the
     *                     {@code sqlDateAndTimeTimeZone} setting suggests. This also effects
     *                     {@link java.sql.Date java.sql.Date} and {@link java.sql.Time java.sql.Time} values<br>
     *             Neither = Use the time zone suggested by the {@code timeZone} or the
     *                     {@code sqlDateAndTimeTimeZone} configuration setting ({@link #getTimeZone()} and
     *                     {@link #getSQLDateAndTimeTimeZone()}).
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
     *
     *   <li><p>Anything that starts with {@code "@"} followed by a letter is interpreted as a custom
     *       date/time/dateTime format, but only if either {@link Configuration#getIncompatibleImprovements()}
     *       is at least 2.3.24, or there's any custom formats defined (even if custom number format). The format of
     *       such string is <code>"@<i>name</i>"</code> or <code>"@<i>name</i> <i>parameters</i>"</code>, where
     *       <code><i>name</i></code> is the name parameter to {@link #getCustomDateFormat(String)}, and
     *       <code><i>parameters</i></code> is parsed by the custom number format.
     *
     * </ul>
     *
     * <p>Its {@link Configuration}-level default is {@code ""}, which is equivalent to {@code "medium_medium"}.
     */
    String getDateTimeFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isDateTimeFormatSet();

    /**
     * A {@link Map} that associates {@link TemplateDateFormatFactory}-es to names, which then can be referred by the
     * {@link #getDateFormat() dateFormat}/{@link #getDateFormat() dateFormat }/{@link #getDateTimeFormat()
     * dateTimeFormat} settings with values starting with <code>@<i>name</i></code>. The keys in the {@link Map} should
     * start with an UNICODE letter, and should only contain UNICODE letters and digits (not {@code _}), otherwise
     * accessing the custom format from templates can be difficult or impossible. The {@link Configuration}-level
     * default of this setting is an empty {@link Map}.
     * <p>
     * When the {@link ProcessingConfiguration} is part of a setting inheritance chain ({@link Environment} inherits
     * settings from the main {@link Template}, which inherits from the {@link Configuration}), you still only get the
     * {@link Map} from the closest {@link ProcessingConfiguration} where it was set, not a {@link Map} that respects
     * inheritance. Thus, to get a custom format you shouldn't use this {@link Map} directly, but {@link
     * #getCustomDateFormat(String)}, which will search the format in the inheritance chain.
     *
     * @return Never {@code null}; unmodifiable {@link Map}.
     */
    Map<String, TemplateDateFormatFactory> getCustomDateFormats();

    /**
     * Gets the custom date or time or date-time format registered for the name. This differs from calling {@link
     * #getCustomDateFormats() getCustomDateFormats.get(name)}, because if there's {@link ProcessingConfiguration} from
     * which setting values are inherited then this method will search the custom format there as well if it isn't found
     * here. For example, {@link Environment#getCustomNumberFormat(String)} will check if the {@link Environment}
     * contains the custom format with the name, and if not, it will try {@link Template#getCustomDateFormat(String)} on
     * the main template, which in turn might falls back to calling {@link Configuration#getCustomDateFormat(String)}.
     */
    TemplateDateFormatFactory getCustomDateFormat(String name);

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isCustomDateFormatsSet();


    /**
     * The format (usually a computer language) used {@code ?c}, {@code ?cn}, and for the
     * {@code "c"} {@link #getNumberFormat() numberFormat}, and the
     * {@code "c"} {@link #getBooleanFormat() booleanFormat}.
     * Its {@link Configuration}-level default is {@link JavaScriptOrJSONCFormat#INSTANCE "JavaScript or JSON"}.
     */
    CFormat getCFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isCFormatSet();

    /**
     * The exception handler used to handle exceptions occurring inside templates.
     * Its {@link Configuration}-level default is {@link TemplateExceptionHandler#RETHROW}. The recommended
     * values are:
     *
     * <ul>
     *   <li>In production systems: {@link TemplateExceptionHandler#RETHROW}
     *   <li>During development of HTML templates, if you want to the error directly in the browser:
     *   {@link TemplateExceptionHandler#HTML_DEBUG}
     *   <li>During development of non-HTML templates, if you want to the error directly in the output:
     *   {@link TemplateExceptionHandler#DEBUG}
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
     * output, or if you want to suppress certain exceptions. If you suppress an exception then it's the responsibility
     * of the {@link TemplateExceptionHandler} to log the exception (if you want it to be logged).
     *
     * @see #getAttemptExceptionReporter()
     */
    TemplateExceptionHandler getTemplateExceptionHandler();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isTemplateExceptionHandlerSet();

    /**
     * Specifies how exceptions handled (and hence suppressed) by an {@code #attempt} blocks will be logged or otherwise
     * reported. The default value is {@link AttemptExceptionReporter#LOG_ERROR}.
     *
     * <p>Note that {@code #attempt} is not supposed to be a general purpose error handler mechanism, like {@code try}
     * is in Java. It's for decreasing the impact of unexpected errors, by making it possible that only part of the
     * page is going down, instead of the whole page. But it's still an error, something that someone should fix. So the
     * error should be reported, not just ignored in a custom {@link AttemptExceptionReporter}-s.
     *
     * <p>The {@link AttemptExceptionReporter} is not invoked if the {@link TemplateExceptionHandler} has
     * suppressed the exception.
     */
    AttemptExceptionReporter getAttemptExceptionReporter();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * a {@link CoreSettingValueNotSetException}.
     */
    boolean isAttemptExceptionReporterSet();

    /**
     * The arithmetic engine used to perform arithmetic operations.
     * Its {@link Configuration}-level default is {@link BigDecimalArithmeticEngine#INSTANCE}.
     * Note that this setting overlaps with {@link ParsingConfiguration#getArithmeticEngine()}.
     */
    ArithmeticEngine getArithmeticEngine();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * Informs FreeMarker about the charset used for the output. As FreeMarker outputs character stream (not
     * byte stream), it's not aware of the output charset unless the software that encloses it tells it
     * with this setting. Some templates may use FreeMarker features that require this information.
     * Setting this to {@code null} means that the output encoding is not known.
     *
     * <p>Its {@link Configuration}-level default is {@code null}.
     */
    Charset getOutputEncoding();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isOutputEncodingSet();

    /**
     * The URL escaping (URL encoding, percentage encoding) charset. If ({@code null}), the output encoding
     * ({@link #getOutputEncoding()}) will be used. Its {@link Configuration}-level default is {@code null}.
     */
    Charset getURLEscapingCharset();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isURLEscapingCharsetSet();

    /**
     * The {@link TemplateClassResolver} that is used when the <code>new</code> built-in is called in a template. That
     * is, when a template contains the <code>"com.example.SomeClassName"?new</code> expression, this object will be
     * called to resolve the <code>"com.example.SomeClassName"</code> string to a class. The default value is {@link
     * TemplateClassResolver#UNRESTRICTED}. If you allow users to upload templates, it's important to use a
     * custom restrictive {@link TemplateClassResolver} or {@link TemplateClassResolver#ALLOW_NOTHING}.
     *
     * <p>Note that the {@link MemberAccessPolicy} used by the {@link ObjectWrapper} also influences what constructors
     * are available. Allowing the resolution of the class here is not enough in itself, as the
     * {@link MemberAccessPolicy} has to allow exposing the particular constructor you try to call as well.
     */
    TemplateClassResolver getNewBuiltinClassResolver();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isNewBuiltinClassResolverSet();

    /**
     * Specifies if {@code ?api} can be used in templates. Its {@link Configuration}-level is {@code false} (which
     * is the safest option).
     */
    boolean getAPIBuiltinEnabled();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isAPIBuiltinEnabledSet();

    /**
     * Whether the output {@link Writer} is automatically flushed at the end of {@link Template#process(Object, Writer)}
     * (and its overloads). Its {@link Configuration}-level default is {@code true}.
     * <p>
     * Using {@code false} is needed for example when a Web page is composed from several boxes (like portlets, GUI
     * panels, etc.) that aren't inserted with <tt>#include</tt> (or with similar directives) into a master FreeMarker
     * template, rather they are all processed with a separate {@link Template#process(Object, Writer)} call. In a such
     * scenario the automatic flushes would commit the HTTP response after each box, hence interfering with full-page
     * buffering, and also possibly decreasing performance with too frequent and too early response buffer flushes.
     */
    boolean getAutoFlush();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isAutoFlushSet();

    /**
     * Whether tips should be shown in error messages of errors arising during template processing.
     * Its {@link Configuration}-level default is {@code true}.
     */
    boolean getShowErrorTips();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isShowErrorTipsSet();

    /**
     * Specifies if {@code <#import ...>} (and {@link Environment#importLib(String, String)}) should delay the loading
     * and processing of the imported templates until the content of the imported namespace is actually accessed. This
     * makes the overhead of <em>unused</em> imports negligible. Note that turning on lazy importing isn't entirely
     * transparent, as accessing global variables (usually created with {@code <#global ...=...>}) that should be
     * created by the imported template won't trigger the loading and processing of the lazily imported template
     * (because globals aren't accessed through the namespace variable), so the global variable will just be missing.
     * In general, you lose the strict control over when the namespace initializing code in the imported template will
     * be executed, though it shouldn't mater for most well designed imported templates.
     * Another drawback is that importing a missing or otherwise broken template will be successful, and the problem
     * will remain hidden until (and if) the namespace content is actually used. Note that the namespace initializing
     * code will run with the same {@linkplain ProcessingConfiguration#getLocale() locale} as it was at the point of the
     * {@code <#import ...>} call (other settings won't be handled specially like that).
     * <p>
     * The default is {@code false} (and thus imports are eager) for backward compatibility, which can cause
     * perceivable overhead if you have many imports and only a few of them is actually used.
     * <p>
     * This setting also affects {@linkplain #getAutoImports() auto-imports}, unless you have set a non-{@code null}
     * value with {@link #getLazyAutoImports()}.
     *
     * @see #getLazyAutoImports()
     */
    boolean getLazyImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isLazyImportsSet();

    /**
     * Specifies if {@linkplain #getAutoImports() auto-imports} will be
     * {@link #getLazyImports() lazy imports}. This is useful to make the overhead of <em>unused</em>
     * auto-imports negligible. If this is set to {@code null}, {@link #getLazyImports()} specifies the behavior of
     * auto-imports too. The default value is {@code null}.
     */
    Boolean getLazyAutoImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isLazyAutoImportsSet();

    /**
     * Adds invisible <code>#import <i>templateName</i> as <i>namespaceVarName</i></code> statements at the beginning of
     * the main template (that's the top-level template that wasn't included/imported from another template). While
     * it only affects the main template directly, as the imports will create a global variable there, the imports
     * will be visible from the further imported templates too.
     * <p>
     * It's recommended to set the {@link Configuration#getLazyAutoImports() lazyAutoImports} setting to {@code true}
     * when using this, so that auto-imports that are unused in a template won't degrade performance by unnecessary
     * loading and initializing the imported library.
     * <p>
     * If the imports aren't lazy, the order of the imports will be the same as the order in which the {@link Map}
     * iterates through its entries.
     * <p>
     * When the {@link ProcessingConfiguration} is part of a setting inheritance chain ({@link Environment} inherits
     * settings from the main {@link Template}, which inherits from the {@link Configuration}), you still only get the
     * {@link Map} from the closest {@link ProcessingConfiguration} where it was set, not a {@link Map} that respects
     * inheritance. But FreeMarker will walk the whole inheritance chain, executing all auto-imports starting
     * from the ancestors. If, however, the same auto-import <code><i>namespaceVarName</i></code> occurs in multiple
     * {@link ProcessingConfiguration}-s of the chain, only the one in the last (child)
     * {@link ProcessingConfiguration} will be executed.
     * <p>
     * If there are also auto-includes (see {@link #getAutoIncludes()}), those will be executed after the auto-imports.
     * <p>
     * The {@link Configuration}-level default of this setting is an empty {@link Map}.
     *
     * @return Never {@code null}
     */
    Map<String, String> getAutoImports();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isAutoImportsSet();

    /**
     * Adds an invisible <code>#include <i>templateName</i></code> at the beginning of the main template (that's the
     * top-level template that wasn't included/imported from another template).
     * <p>
     * The order of the inclusions will be the same as the order in this {@link List}.
     * <p>
     * When the {@link ProcessingConfiguration} is part of a setting inheritance chain ({@link Environment} inherits
     * settings from the main {@link Template}, which inherits from the {@link Configuration}), you still only get the
     * {@link List} from the closest {@link ProcessingConfiguration} where it was set, not a {@link List} that respects
     * inheritance. But FreeMarker will walk the whole inheritance chain, executing all auto-imports starting
     * from the ancestors. If, however, the same auto-included template name occurs in multiple
     * {@link ProcessingConfiguration}-s of the chain, only the one in the last (child)
     * {@link ProcessingConfiguration} will be executed.
     * <p>
     * If there are also auto-imports ({@link #getAutoImports()}), those imports will be executed before
     * the auto-includes, hence the namespace variables are alrady accessible for the auto-included templates.
     *
     * @return An unmodifiable {@link List}; not {@code null}
     */
    List<String> getAutoIncludes();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    boolean isAutoIncludesSet();

    /**
     * Retrieves the value of a custom setting. Custom settings are key-value pairs associated to a {@link
     * ProcessingConfiguration} object, that the FreeMarker core doesn't try to interpret. They are like configuration
     * settings added dynamically (as opposed to in compilation time), where each custom setting is treated as an
     * individual setting. So where predefined configuration settings used to have {@code isXxxSet}, {@code
     * unsetXxx}, and {@code setXxx} methods, custom settings have these too, with a key (the identifier of the
     * custom setting) as an extra argument (see {@link #isCustomSettingSet(Serializable)},
     * {@link MutableProcessingConfiguration#setCustomSetting(Serializable, Object)},
     * {@link MutableProcessingConfiguration#unsetCustomSetting(Serializable)}).
     * <p>
     * When the {@link ProcessingConfiguration} is part of a setting inheritance chain ({@link Environment} inherits
     * settings from the main {@link Template}, which inherits from the {@link Configuration}), this method will search
     * the custom setting in the whole inheritance chain, until it finds it.
     * <p>
     * To prevent key clashes (and for better performance), it's often a good idea to use enums as keys, rather than
     * {@link String}-s. If {@link String}-s are used for keys (names) by components that will be reused on several
     * places, then to avoid accidental name clashes, the names should use a prefix similar to a package name, like
     * like "com.example.myframework.".
     * <p>
     * The values of custom settings should be immutable, or at least not changed after they were added as a
     * custom setting value. To store custom state information (such as application or framework specific caches)
     * you should use the methods provided by {@link CustomStateScope} instead.
     * <p>
     * The FreeMarker core doesn't provide any means for accessing custom settings from the templates. If a framework
     * or application needs such functionality, it has to add its own custom directives/methods for that. But its
     * more typical that custom settings just influence the behavior of custom directives/methods without the normal
     * templates directly accessing them, or that they are just used by the framework code that invokes templates.
     *
     * @param key
     *         The identifier (usually an enum or a {@link String}) of the custom setting; not {@code null}; must be
     *         usable as {@link HashMap} key
     *
     * @return The value of the custom setting; possibly {@code null}, as that's a legal attribute value. The content
     * of the value object shouldn't be changed after it was added as an attribute (ideally, it should be an
     * immutable object); if you need to change the content, certainly you should use the {@link CustomStateScope}
     * API. Note that if the custom setting was created with <tt>&lt;#ftl&nbsp;customSettings={...}&gt;</tt>, then
     * this value is already unwrapped (i.e. it's a <code>String</code>, or a <code>List</code>, or a <code>Map</code>,
     * ...etc., not a FreeMarker specific class).
     *
     * @throws CustomSettingValueNotSetException if the custom setting was not set (not even to {@code null}), nor in
     * this {@link ProcessingConfiguration}, nor in another where we inherit settings from. Use
     * {@link #getCustomSetting(Serializable, Object)} to avoid this exception.
     */
    Object getCustomSetting(Serializable key) throws CustomSettingValueNotSetException;

    /**
     * Same as {@link #getCustomSetting(Serializable)}, but instead of throwing {@link CustomSettingValueNotSetException}
     * it returns the default value specified as the 2nd argument.
     *
     * @param defaultValue
     *         The value to return if the attribute is not set. Note that an attribute that was explicitly set to
     *         {@code null}, then {@code null} will be returned for it, not the default value specified here, since
     *         the attribute was set. If you want to know if the value was set, {@link #MISSING_VALUE_MARKER} can
     *         be used, as it's guaranteed that an attribute never has that value.
     */
    Object getCustomSetting(Serializable key, Object defaultValue);

    /**
     * Tells if this custom setting is set directly in this object (not in its parent
     * {@link ProcessingConfiguration}). If not, then depending on the implementing class, reading the custom
     * attribute might return the value of the setting from a parent object, or returns {@code null}, or throws a
     * {@link CoreSettingValueNotSetException}. Note that if an attribute was set to {@code
     * null} (as opposed to not set at all) then this method will return {@code true}.
     */
    boolean isCustomSettingSet(Serializable key);

    /**
     * Collects all {@linkplain #getCustomSetting(Serializable)} custom settings} into a {@link Map} (which is a
     * snapshot); mostly useful for debugging and tooling, and is possibly too slow to call very frequently.
     *
     * @param includeInherited
     *         If {@code false}, only the custom settings set in this {@link ProcessingConfiguration} will be
     *         collected, otherwise the custom settings inherited from the parent {@link ProcessingConfiguration}-s
     *         will be too. Note that it's the last that matches the behavior of {@link
     *         #getCustomSetting(Serializable)}.
     *
     * @return An unmodifiable and unchanging {@link Map}; not {@code null}. The object identity of keys and values of
     * this {@link Map} will not change when custom settings are set/unset later (it's a snapshot). But, if
     * a key or value objects are themselves mutable objects, FreeMarker can't prevent their content from changing.
     * You shouldn't change the content of those objects.
     */
    Map<Serializable, Object> getCustomSettings(boolean includeInherited);

    /**
     * The algorithm used for {@code ?truncate}. Defaults to {@link DefaultTruncateBuiltinAlgorithm#ASCII_INSTANCE}.
     * Most customization needs can be addressed by creating a new {@link DefaultTruncateBuiltinAlgorithm} with the
     * proper constructor parameters. Otherwise users my use their own {@link TruncateBuiltinAlgorithm} implementation.
     *
     * <p>In case you need to set this with {@link Properties}, or a similar configuration approach that doesn't let you
     * create the value in Java, see examples at {@link MutableProcessingConfiguration#setSetting(String, String)}.
     */
    public TruncateBuiltinAlgorithm getTruncateBuiltinAlgorithm();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link CoreSettingValueNotSetException}.
     */
    public boolean isTruncateBuiltinAlgorithmSet();

}

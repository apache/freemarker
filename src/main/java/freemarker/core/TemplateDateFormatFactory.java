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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.Configuration;
import freemarker.template.TemplateDateModel;

/**
 * Factory for a certain kind of date/time/dateTime formatting ({@link TemplateDateFormat}). Usually a singleton
 * (one-per-VM or one-per-{@link Configuration}), and so must be thread-safe.
 * 
 * @see Configurable#setCustomDateFormats(java.util.Map)
 * 
 * @since 2.3.24
 */
public abstract class TemplateDateFormatFactory extends TemplateValueFormatFactory {
    
    /**
     * Returns a formatter for the given parameters.
     * 
     * <p>
     * The returned formatter can be a new instance or a reused (cached) instance. Note that {@link Environment} itself
     * caches the returned instances, though that cache is lost with the {@link Environment} (i.e., when the top-level
     * template execution ends), also it might flushes lot of entries if the locale or time zone is changed during
     * template execution. So caching on the factory level is still useful, unless creating the formatters is
     * sufficiently cheap.
     * 
     * @param params
     *            The string that further describes how the format should look. For example, when the
     *            {@link Configurable#getDateFormat() dateFormat} is {@code "@fooBar 1, 2"}, then it will be
     *            {@code "1, 2"} (and {@code "@fooBar"} selects the factory). The format of this string is up to the
     *            {@link TemplateDateFormatFactory} implementation. Not {@code null}, often an empty string.
     * @param dateType
     *            {@link TemplateDateModel#DATE}, {@link TemplateDateModel#TIME}, {@link TemplateDateModel#DATETIME} or
     *            {@link TemplateDateModel#UNKNOWN}. Supporting {@link TemplateDateModel#UNKNOWN} is not necessary, in
     *            which case the method should throw an {@link UnknownDateTypeFormattingUnsupportedException} exception.
     * @param locale
     *            The locale to format for. Not {@code null}. The resulting format should be bound to this locale
     *            forever (i.e. locale changes in the {@link Environment} must not be followed).
     * @param timeZone
     *            The time zone to format for. Not {@code null}. The resulting format must be bound to this time zone
     *            forever (i.e. time zone changes in the {@link Environment} must not be followed).
     * @param zonelessInput
     *            Indicates that the input Java {@link Date} is not from a time zone aware source. When this is
     *            {@code true}, the formatters shouldn't override the time zone provided to its constructor (most
     *            formatters don't do that anyway), and it shouldn't show the time zone, if it can hide it (like a
     *            {@link SimpleDateFormat} pattern-based formatter may can't do that, as the pattern prescribes what to
     *            show).
     *            <p>
     *            As of FreeMarker 2.3.21, this is {@code true} exactly when the date is an SQL "date without time of
     *            the day" (i.e., a {@link java.sql.Date java.sql.Date}) or an SQL "time of the day" value (i.e., a
     *            {@link java.sql.Time java.sql.Time}, although this rule can change in future, depending on
     *            configuration settings and such, so you shouldn't rely on this rule, just accept what this parameter
     *            says.
     * @param env
     *            The runtime environment from which the formatting was called. This is mostly meant to be used for
     *            {@link Environment#setCustomState(Object, Object)}/{@link Environment#getCustomState(Object)}.
     * 
     * @throws TemplateValueFormatException
     *             If any problem occurs while parsing/getting the format. Notable subclasses:
     *             {@link InvalidFormatParametersException} if {@code params} is malformed;
     *             {@link UnknownDateTypeFormattingUnsupportedException} if {@code dateType} is
     *             {@link TemplateDateModel#UNKNOWN} and that's unsupported by this factory.
     */
    public abstract TemplateDateFormat get(
            String params,
            int dateType, Locale locale, TimeZone timeZone, boolean zonelessInput,
            Environment env)
                    throws TemplateValueFormatException;

}

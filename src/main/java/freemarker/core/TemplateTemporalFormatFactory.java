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

import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.TimeZone;

import freemarker.template.Configuration;

/**
 * Factory for a certain kind of {@link Temporal} formatting ({@link TemplateTemporalFormat}). Usually a singleton
 * (one-per-VM, or one-per-{@link Configuration}), and so must be thread-safe.
 *
 * @see Configurable#setCustomTemporalFormats(java.util.Map)
 * 
 * @since 2.3.32
 */
public abstract class TemplateTemporalFormatFactory extends TemplateValueFormatFactory {
    
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
     *            {@link Configurable#getDateTimeFormat() dateTimeFormat} is {@code "@fooBar 1, 2"}, then it will be
     *            {@code "1, 2"} (and {@code "@fooBar"} selects the factory). The format of this string is up to the
     *            {@link TemplateTemporalFormatFactory} implementation. Not {@code null}, but often an empty string.
     * @param temporalClass
     *            The type of the temporal. If this type is not supported, the method should throw an
     *            {@link UnformattableTemporalTypeException} exception.
     * @param locale
     *            The locale to format for. Not {@code null}. The resulting format should be bound to this locale
     *            forever. That is, the result of {@link Environment#getLocale()} must not be taken into account.
     * @param timeZone
     *            The time zone to format for. Not {@code null}. The resulting format must be bound to this time zone
     *            forever. That is, the result of {@link Environment#getTimeZone()} must not be taken into account.
     * @param env
     *            The runtime environment from which the formatting was called. This is mostly meant to be used for
     *            {@link Environment#setCustomState(Object, Object)}/{@link Environment#getCustomState(Object)}. The
     *            result shouldn't depend on setting values in the {@link Environment}, because changing settings
     *            will not necessarily invalidate the result.
     * 
     * @throws TemplateValueFormatException
     *             If any problem occurs while parsing/getting the format. Notable subclasses:
     *             {@link InvalidFormatParametersException} if {@code params} is malformed;
     *             {@link UnformattableTemporalTypeException} if the {@code temporalClass} subclass is
     *             not supported by this factory.
     */
    public abstract TemplateTemporalFormat get(
            String params,
            Class<? extends Temporal> temporalClass, Locale locale, TimeZone timeZone, Environment env)
                    throws TemplateValueFormatException;

}

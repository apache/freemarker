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

import java.util.Locale;

import freemarker.template.Configuration;

/**
 * Factory for a certain kind of number formatting ({@link TemplateNumberFormat}). Usually a singleton (one-per-VM or
 * one-per-{@link Configuration}), and so must be thread-safe.
 * 
 * @see Configurable#setCustomNumberFormats(java.util.Map)
 * 
 * @since 2.3.24
 */
public abstract class TemplateNumberFormatFactory extends TemplateValueFormatFactory {

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
     *            {@link Configurable#getNumberFormat() numberFormat} is {@code "@fooBar 1, 2"}, then it will be
     *            {@code "1, 2"} (and {@code "@fooBar"} selects the factory). The format of this string is up to the
     *            {@link TemplateNumberFormatFactory} implementation. Not {@code null}, often an empty string.
     * @param locale
     *            The locale to format for. Not {@code null}. The resulting format must be bound to this locale
     *            forever (i.e. locale changes in the {@link Environment} must not be followed).
     * @param env
     *            The runtime environment from which the formatting was called. This is mostly meant to be used for
     *            {@link Environment#setCustomState(Object, Object)}/{@link Environment#getCustomState(Object)}.
     *            
     * @throws TemplateValueFormatException
     *             if any problem occurs while parsing/getting the format. Notable subclasses:
     *             {@link InvalidFormatParametersException} if the {@code params} is malformed.
     */
    public abstract TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws TemplateValueFormatException;

}

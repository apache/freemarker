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

import java.util.Locale;

import freemarker.template.Configuration;

/**
 * Factory for a certain type of number formatting ({@link TemplateNumberFormat}). Usually a singleton (one-per-VM or
 * one-per-{@link Configuration}), and so must be thread-safe.
 * 
 * @since 2.3.24
 */
public abstract class TemplateNumberFormatFactory {

    /**
     * Returns a formatter for the given parameter. The returned formatter can be a new instance or a reused (cached)
     * instance.
     * 
     * @param params
     *            The string that further describes how the format should look. For example, when the
     *            {@link Configurable#getNumberFormat() numberFormat} is {@code "@fooBar 1, 2"}, then it will be
     *            {@code "1, 2"} (and {@code "@fooBar"} selects the factory). The format of this string is up to the
     *            {@link TemplateDateFormatFactory} implementation. Not {@code null}, often an empty string.
     * @param locale
     *            The locale to format for.
     * @param env
     *            The runtime environment from which the formatting was called. This is mostly meant to be used for
     *            {@link Environment#setCustomState(Object, Object)}/{@link Environment#getCustomState(Object)}.
     */
    public abstract TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws InvalidFormatParametersException;

}

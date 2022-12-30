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

import java.text.NumberFormat;

import freemarker.template.TemplateException;

/**
 * Defines a format (usually a computer language) that's used by the {@code c}, {@code cn} built-ins, and for the
 * {@code "c"} and {@code "computer"} {@link Configurable#setNumberFormat(String) number_format}, and
 * the {@code "c"} {@link Configurable#setBooleanFormat(String) boolean_format}.
 * A {@link CFormat} currently defines how numbers, booleans, and strings are converted to text that defines a similar
 * value in a certain computer language (or other computer-parsed syntax).
 *
 * <p><b>Experimental class!</b> This class is too new, and might will change over time. Therefore, for now
 * constructor and most methods are not exposed outside FreeMarker, and so you can't create a custom implementation.
 * The class itself and some members are exposed as they are needed for configuring FreeMarker.
 *
 * @since 2.3.32
 */
public abstract class CFormat {

    // Visibility is not "protected" to avoid external implementations while this class is experimental.
    CFormat() {
    }

    /**
     * Gets/creates the number format to use; this is not always a cheap operation, so in case it will be called
     * for many cases, the result should be cached or otherwise reused.
     *
     * <p>The returned object is typically not thread-safe. The implementation must ensure that if there's a singleton,
     * which is mutable, or not thread-safe, then it's not returned, but a clone or copy of it. The caller of this
     * method is not responsible for do any such cloning or copying.
     */
    abstract TemplateNumberFormat getTemplateNumberFormat(Environment env);

    /**
     * Similar to {@link #getTemplateNumberFormat(Environment)}, but only exist to serve the deprecated
     * {@link Environment#getCNumberFormat()} method. We don't expect the result of the formatting to be the same as
     * with the {@link TemplateNumberFormat}, but this method should make some effort to be similar.
     *
     * @deprecated Use {@link #getTemplateNumberFormat(Environment)} instead, except in
     * {@link Environment#getCNumberFormat()}.
     */
    @Deprecated
    abstract NumberFormat getLegacyNumberFormat(Environment env);

    /**
     * Format a {@link String} to a string literal.
     *
     * @param env
     *      Not {@code null}; is here mostly to be used to figure out escaping preferences (like based on
     *      {@link Environment#getOutputEncoding()}).
     */
    abstract String formatString(String s, Environment env) throws TemplateException;

    abstract String getTrueString();

    abstract String getFalseString();

    abstract String getNullString();

    public abstract String getName();
}

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

package org.apache.freemarker.core.cformat;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.ProcessingConfiguration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;

/**
 * Defines a format (usually a computer language) that's used by the {@code c}, {@code cn} built-ins, and for the
 * {@code "c"} and {@code "computer"} {@link ProcessingConfiguration#getNumberFormat() numberFormat}, and
 * the {@code "c"} {@link ProcessingConfiguration#getBooleanFormat() booleanFormat}.
 * A {@link CFormat} currently defines how numbers, booleans, and strings are converted to text that defines a similar
 * value in a certain computer language (or other computer-parsed syntax).
 *
 *
 */
// TODO FM3: Must be finished, as it's not marked as experimental here. Printing identifiers, escaping for US-ASCOO output. etc.
public abstract class CFormat {

    protected CFormat() {
    }

    /**
     * Gets/creates the number format to use; this is not always a cheap operation, so in case it will be called
     * for many cases, the result should be cached or otherwise reused.
     *
     * <p>The returned object is typically not thread-safe. The implementation must ensure that if there's a singleton,
     * which is mutable, or not thread-safe, then it's not returned, but a clone or copy of it. The caller of this
     * method is not responsible for do any such cloning or copying.
     */
    public abstract TemplateNumberFormat getTemplateNumberFormat(Environment env);

    /**
     * Format a {@link String} to a string literal.
     *
     * @param env
     *      Not {@code null}; is here mostly to be used to figure out escaping preferences (like based on
     *      {@link Environment#getOutputEncoding()}).
     */
    public abstract String formatString(String s, Environment env) throws TemplateException;

    public abstract String getTrueString();

    public abstract String getFalseString();

    public abstract String getNullString();

    public abstract String getName();
}

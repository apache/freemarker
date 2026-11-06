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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Factory for a locale-sensitive number format that, unlike the standard {@code number} format, keeps the
 * {@linkplain BigDecimal#scale() scale} of {@link BigDecimal}-s, and so shows exactly as many fraction digits as the
 * scale of the {@link BigDecimal} specifies, including trailing 0-s. For example, a {@link BigDecimal} created as
 * {@code new BigDecimal("1.500")} (which has a scale of 3) is shown as {@code 1.500} (with {@code en_US} locale),
 * while the standard {@code number} format would show it as {@code 1.5}. This is useful when the data-model is
 * created by a system that knows the desired number of fraction digits, and hence sets the scale-s accordingly.
 *
 * <p>Numbers that aren't {@link BigDecimal}-s are formatted with all their fraction digits shown (but without adding
 * trailing 0-s), as those types don't carry scale information.
 *
 * <p>Apart from the number of fraction digits shown, formatting is done as with the standard (locale-sensitive)
 * {@code number} format, so for example, the decimal separator depends on the locale, and grouping separators are
 * shown.
 *
 * <p>Note that the result of arithmetic operations done inside a template is also a {@link BigDecimal}, whose scale
 * follows the rules of {@link BigDecimal} arithmetic, and so with this format the number of fraction digits shown
 * may differ from that of the operands. For example, as the scale of {@code 1.50 * 1.50} is 4, it's shown as
 * {@code 2.2500}.
 *
 * <p>This format is available under the built-in format name {@code "scalePreserving"}, so no registration is needed;
 * either use it in templates like {@code ${x?string.scalePreserving}}, or set it as the default number format with
 * {@link Configurable#setNumberFormat(String) cfg.setNumberFormat("scalePreserving")}.
 *
 * <p>You can also register this factory as a
 * {@linkplain Configurable#setCustomNumberFormats(java.util.Map) custom number format}, which is only useful if you
 * want to refer to it with an application-specific name (like {@code ${x?string.@price}}):
 *
 * <pre>
 * cfg.setCustomNumberFormats(
 *         Collections.singletonMap("price", ScalePreservingTemplateNumberFormatFactory.INSTANCE));</pre>
 *
 * @since 2.3.36
 */
public final class ScalePreservingTemplateNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final ScalePreservingTemplateNumberFormatFactory INSTANCE
            = new ScalePreservingTemplateNumberFormatFactory();

    private ScalePreservingTemplateNumberFormatFactory() {
        // Not meant to be instantiated from outside
    }

    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws TemplateValueFormatException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new ScalePreservingTemplateNumberFormat(NumberFormat.getNumberInstance(locale));
    }

}

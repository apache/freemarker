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

import freemarker.template.utility.StringUtil;

/**
 * Factory for a locale-sensitive number format that, unlike the standard {@code number} format, keeps the
 * {@linkplain BigDecimal#scale() scale} of {@link BigDecimal}-s, and so shows exactly as many fraction digits as the
 * scale specifies, including trailing 0-s. For example, {@code new BigDecimal("1.500")} is shown as {@code 1.500} (with
 * {@code en_US} locale), while the {@code number} format would show {@code 1.5}. Otherwise {@link BigDecimal}-s are
 * formatted as with {@code number} (locale-specific decimal and grouping separators).
 *
 * <p>Other numbers (like {@link Double}-s and {@link Integer}-s) have no scale, so they are formatted with another
 * number format. That's {@code number} by default, and can be specified in the format string like this:
 *
 * <ul>
 *   <li>{@code "scalePreserving|<otherFormat>"}: {@code <otherFormat>} is any number format string (see
 *       {@link Configurable#setNumberFormat(String)}), like in {@code "scalePreserving|0.00##"}, or
 *       {@code "scalePreserving|@foo"}.</li>
 *   <li>{@code "scalePreserving_<max>"}, or {@code "scalePreserving_<min>_<max>"}: Like {@code number}, but with
 *       at least {@code <min>} (0 if omitted) and at most {@code <max>} fraction digits. Can be used like
 *       {@code ${x?string.scalePreserving_2_6}}.</li>
 * </ul>
 *
 * <p>Note that the results of arithmetic operations in templates are also {@link BigDecimal}-s (with the default
 * {@link ArithmeticEngine}), and their scale follows the rules of {@link BigDecimal} arithmetic. So {@code 1.50 * 1.50}
 * is shown as {@code 2.2500}, and, as division uses a scale of at least 12, {@code 6 / 2} is shown as
 * {@code 3.000000000000}. To avoid that, format such results with another format (like
 * {@code ${(a / b)?string.number}}), or use an {@link ArithmeticEngine} whose division doesn't return
 * {@link BigDecimal} unless the result is exact.
 *
 * <p>This format is available under the built-in format name {@code "scalePreserving"}, so no registration is needed;
 * use it like {@code ${x?string.scalePreserving}}, or
 * {@link Configurable#setNumberFormat(String) cfg.setNumberFormat("scalePreserving")}. Registering this factory as a
 * {@linkplain Configurable#setCustomNumberFormats(java.util.Map) custom number format} is only useful if you want to
 * refer to it with an application-specific name (like {@code ${x?string.@price}}); the parameters are then
 * given like {@code "@price |0.00##"}, or {@code "@price_2_6"}:
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

    /**
     * Precedes the format string of the non-{@link BigDecimal} format in the parameters.
     */
    static final char NON_BIG_DECIMAL_FORMAT_STRING_PREFIX = '|';

    private static final String DEFAULT_NON_BIG_DECIMAL_FORMAT_STRING = "number";

    private ScalePreservingTemplateNumberFormatFactory() {
        // Not meant to be instantiated from outside
    }

    /**
     * @param params
     *         Empty, or {@code "|<otherFormat>"}, or {@code "<max>"}, or {@code "<min>_<max>"}; see in the
     *         {@linkplain ScalePreservingTemplateNumberFormatFactory class documentation}.
     */
    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment env)
            throws TemplateValueFormatException {
        return new ScalePreservingTemplateNumberFormat(
                getJavaNumberFormat(locale), getNonBigDecimalNumberFormat(params, locale, env));
    }

    /**
     * Gets the locale-specific {@link NumberFormat} via {@link JavaTemplateNumberFormatFactory}, to utilize its cache.
     *
     * @return A new instance, so it can be freely modified
     */
    private static NumberFormat getJavaNumberFormat(Locale locale) throws TemplateValueFormatException {
        return ((JavaTemplateNumberFormat) JavaTemplateNumberFormatFactory.INSTANCE.get(
                DEFAULT_NON_BIG_DECIMAL_FORMAT_STRING, locale, null)).getJavaNumberFormat();
    }

    private static TemplateNumberFormat getNonBigDecimalNumberFormat(String params, Locale locale, Environment env)
            throws TemplateValueFormatException {
        if (params.length() == 0) {
            return getNumberFormatForFormatString(DEFAULT_NON_BIG_DECIMAL_FORMAT_STRING, locale, env);
        }
        if (params.charAt(0) == NON_BIG_DECIMAL_FORMAT_STRING_PREFIX) {
            String formatString = params.substring(1);
            if (formatString.length() == 0) {
                throw new InvalidFormatParametersException(
                        "The format string of the number format used for non-BigDecimal-s is missing after the "
                                + StringUtil.jQuote(String.valueOf(NON_BIG_DECIMAL_FORMAT_STRING_PREFIX)) + ".");
            }
            return getNumberFormatForFormatString(formatString, locale, env);
        }
        return getFractionDigitsLimitedNumberFormat(params, locale);
    }

    private static TemplateNumberFormat getNumberFormatForFormatString(
            String formatString, Locale locale, Environment env) throws TemplateValueFormatException {
        if (env != null) {
            return env.getTemplateNumberFormat(formatString, locale);
        }

        // Without an Environment we can only resolve the format strings of JavaTemplateNumberFormatFactory. We must
        // reject the others, as otherwise they would be silently misinterpreted as DecimalFormat patterns.
        if (formatString.charAt(0) == '@'
                || formatString.equals(Environment.C_FORMAT_STRING)
                || formatString.equals(Environment.COMPUTER_FORMAT_STRING)
                || Environment.getScalePreservingFormatParams(formatString) != null) {
            throw new InvalidFormatParametersException(
                    "The " + StringUtil.jQuote(formatString) + " number format can't be used for non-BigDecimal-s "
                            + "when the format is created without an Environment.");
        }
        return JavaTemplateNumberFormatFactory.INSTANCE.get(formatString, locale, null);
    }

    /**
     * Handles the {@code "<max>"} and {@code "<min>_<max>"} parameters.
     */
    private static TemplateNumberFormat getFractionDigitsLimitedNumberFormat(String params, Locale locale)
            throws TemplateValueFormatException {
        int separatorIdx = params.indexOf('_');
        int minFractionDigits = separatorIdx == -1 ? 0 : parseFractionDigits(params.substring(0, separatorIdx), params);
        int maxFractionDigits = parseFractionDigits(params.substring(separatorIdx + 1), params);
        if (minFractionDigits > maxFractionDigits) {
            throw new InvalidFormatParametersException(
                    "The minimum number of fraction digits (" + minFractionDigits + ") can't be greater than the "
                            + "maximum number of fraction digits (" + maxFractionDigits + ").");
        }

        NumberFormat javaNumberFormat = getJavaNumberFormat(locale);
        javaNumberFormat.setMinimumFractionDigits(minFractionDigits);
        javaNumberFormat.setMaximumFractionDigits(maxFractionDigits);
        return new JavaTemplateNumberFormat(
                javaNumberFormat,
                "number with " + minFractionDigits + " to " + maxFractionDigits + " fraction digits");
    }

    /**
     * @param params
     *         The whole parameter string, for the error message
     */
    private static int parseFractionDigits(String s, String params) throws InvalidFormatParametersException {
        if (isFractionDigits(s)) {
            return Integer.parseInt(s);
        }
        throw new InvalidFormatParametersException(
                "Malformed format parameters, " + StringUtil.jQuote(params) + ". The parameters must be either "
                        + "\"" + NON_BIG_DECIMAL_FORMAT_STRING_PREFIX + "\" followed by the format string of the "
                        + "number format used for non-BigDecimal-s (like \"|0.00##\"), or the maximum number of "
                        + "fraction digits shown for non-BigDecimal-s (like \"6\"), or the minimum and maximum "
                        + "number of fraction digits shown for non-BigDecimal-s separated by \"_\" (like \"2_6\").");
    }

    private static boolean isFractionDigits(String s) {
        // We are stricter than Integer.parseInt, which also accepts a sign, and non-ASCII digits. More than 9 digits
        // are rejected as well, as that could overflow an int.
        if (s.length() == 0 || s.length() > 9) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

}

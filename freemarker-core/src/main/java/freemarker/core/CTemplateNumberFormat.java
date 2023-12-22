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
import java.math.BigInteger;

import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * The number format used by most {@link CFormat}-s starting from Incompatible Improvements
 * {@link Configuration#VERSION_2_3_32}.
 *
 * <p>This {@link TemplateNumberFormat} implementation is thread-safe an immutable.
 *
 * <p>This is a lossless format, except that the number types are not kept. That is, the original number can always be
 * exactly restored from the string representation, but only if you know what the type of the number was.
 *
 * @since 2.3.32
 */
final class CTemplateNumberFormat extends TemplateNumberFormat {
    private static final float MAX_INCREMENT_1_FLOAT = 16777216f;
    private static final double MAX_INCREMENT_1_DOUBLE = 9007199254740992d;

    private final String doublePositiveInfinity;
    private final String doubleNegativeInfinity;
    private final String doubleNaN;
    private final String floatPositiveInfinity;
    private final String floatNegativeInfinity;
    private final String floatNaN;

    CTemplateNumberFormat(
            String doublePositiveInfinity, String doubleNegativeInfinity, String doubleNaN,
            String floatPositiveInfinity, String floatNegativeInfinity, String floatNaN) {
        this.doublePositiveInfinity = doublePositiveInfinity;
        this.doubleNegativeInfinity = doubleNegativeInfinity;
        this.doubleNaN = doubleNaN;
        this.floatPositiveInfinity = floatPositiveInfinity;
        this.floatNegativeInfinity = floatNegativeInfinity;
        this.floatNaN = floatNaN;
    }

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel) throws TemplateValueFormatException,
            TemplateModelException {
        Number num = TemplateFormatUtil.getNonNullNumber(numberModel);

        if (num instanceof Integer || num instanceof Long) {
            // Accelerate these fairly common cases
            return num.toString();
        } else if (num instanceof Double) {
            double n = num.doubleValue();
            if (n == Double.POSITIVE_INFINITY) {
                return doublePositiveInfinity;
            }
            if (n == Double.NEGATIVE_INFINITY) {
                return doubleNegativeInfinity;
            }
            if (Double.isNaN(n)) {
                return doubleNaN;
            }
            if (Math.floor(n) == n) {
                if (Math.abs(n) <= MAX_INCREMENT_1_DOUBLE) {
                    return Long.toString((long) n);
                }
                // We let this fall through to exponential form, despite that we certainly haven't reached the 100
                // digits limit, because ulp > 1, so if this was an ID, its value is potentially corrupted anyway.
            } else { // Non-whole number
                double absN = Math.abs(n);
                // The range where Double.toString already uses exponential form, but BigDecimal toString doesn't yet.
                if (absN < 1E-3 && absN > 1E-7) {
                    // Always use BigDecimal.valueOf(n), not new BigDecimal(n)! For example, BigDecimal.valueOf(1.9) is
                    // just the BigDecimal 1.9, but new BigDecimal(1.9) is a very long BigDecimal starting with 1.899.
                    return BigDecimal.valueOf(n).toString();
                }
                // Avoid exponential form where we don't use it with BigDecimal-s. We don't worry about the 100 digits
                // limit, as such big numbers are whole numbers.
                if (absN >= 1E7) {
                    // No new BigDecimal(n)! See earlier why.
                    return BigDecimal.valueOf(n).toPlainString();
                }
            }
            // Remove redundant ".0" so that result look the same as with BigDecimal
            return removeRedundantDot0(Double.toString(n));
        } else if (num instanceof Float) {
            float n = num.floatValue();

            if (n == Float.POSITIVE_INFINITY) {
                return floatPositiveInfinity;
            }
            if (n == Float.NEGATIVE_INFINITY) {
                return floatNegativeInfinity;
            }
            if (Float.isNaN(n)) {
                return floatNaN;
            }
            if (Math.floor(n) == n) {
                if (Math.abs(n) <= MAX_INCREMENT_1_FLOAT) {
                    return Long.toString((long) n);
                }
                // We let this fall through to exponential form, despite that we certainly haven't reached the 100
                // digits limit, because ulp > 1, so if this was an ID, its value is potentially corrupted anyway.
            } else { // Non-whole number
                float absN = Math.abs(n);
                // The range where Float.toString already uses exponential form, but BigDecimal toString doesn't yet.
                if (absN < 1E-3f && absN > 1E-7f) {
                    // No new BigDecimal(n); see earlier why. But there's no BigDecimal.valueOf(float), so we do this.
                    return new BigDecimal(num.toString()).toString();
                }
                // Unlike with double, we don't need an absN >= 1E7 branch, since those number are whole with float.
            }
            // Remove redundant ".0" so that result look the same as with BigDecimal
            return removeRedundantDot0(Float.toString(n));
        } else if (num instanceof BigInteger) {
            return num.toString();
        } else if (num instanceof BigDecimal) {
            BigDecimal bd = ((BigDecimal) num).stripTrailingZeros();
            int scale = bd.scale();
            if (scale <= 0) {
                // A whole number. Maybe a long ID in a database or other system, and for those exponential form is not
                // expected generally, so we avoid that. But then, it becomes too easy to write something like
                // 1e1000000000000 and kill the server with a terra byte long rendering of the number, so for lengths
                // that realistically aren't ID-s or such, we use exponential format after all:
                if (scale <= -100) {
                    return bd.toString(); // Will give exponential form for this scale
                }
                return bd.toPlainString(); // Never gives exponential form
            }
            // `db` is not a whole number. Note that `bd` is already normalized to not have trailing zeroes.
            return bd.toString(); // Gives exponential form of the absolute value of the number is less than 1E-7
        } else {
            // We don't know what this is, but as we want the format to be lossless, this is our best guess.
            return num.toString();
        }
    }

    /**
     * Like "1.0" to "1", and "1.0E-7" to "1E-7". Designed to work on Float/Double.toString() result only.
     */
    private static String removeRedundantDot0(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '.') {
                i++;
                if (s.charAt(i) == '0') {
                    i++;
                    if (i == len) {
                        return s.substring(0, len - 2);
                    }
                    if (s.charAt(i) == 'E') {
                        char[] result = new char[s.length() - 2];
                        int dst = 0;
                        for (int src = 0; src < i - 2; src++) {
                            result[dst++] = s.charAt(src);
                        }
                        for (int src = i; src < len; src++) {
                            result[dst++] = s.charAt(src);
                        }
                        return String.valueOf(result);
                    }
                }
                break;
            }
        }
        return s;
    }

    @Override
    public boolean isLocaleBound() {
        return false;
    }

    @Override
    public String getDescription() {
        return "c";
    }

}

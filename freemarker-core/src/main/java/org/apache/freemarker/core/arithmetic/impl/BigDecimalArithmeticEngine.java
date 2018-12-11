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
package org.apache.freemarker.core.arithmetic.impl;

import java.math.BigDecimal;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.util._NumberUtils;

/**
 * Arithmetic engine that converts all numbers to {@link BigDecimal} and then operates on them, and also keeps the
 * result as a {@link BigDecimal}. This is FreeMarker's default arithmetic engine.
 */
public class BigDecimalArithmeticEngine extends ArithmeticEngine {

    public static final BigDecimalArithmeticEngine INSTANCE = new BigDecimalArithmeticEngine();

    protected BigDecimalArithmeticEngine() {
        //
    }

    @Override
    public int compareNumbers(Number first, Number second) {
        // We try to find the result based on the sign (+/-/0) first, because:
        // - It's much faster than converting to BigDecial, and comparing to 0 is the most common comparison.
        // - It doesn't require any type conversions, and thus things like "Infinity > 0" won't fail.
        int firstSignum = _NumberUtils.getSignum(first); 
        int secondSignum = _NumberUtils.getSignum(second);
        if (firstSignum != secondSignum) {
            return firstSignum < secondSignum ? -1 : (firstSignum > secondSignum ? 1 : 0); 
        } else if (firstSignum == 0 && secondSignum == 0) {
            return 0;
        } else {
            // The most common case is comparing values of the same type. As BigDecimal can represent all of these
            // with loseless round-trip (i.e., converting to BigDecimal and then back the original type gives the
            // original value), we can avoid conversion to BigDecimal without changing the result.
            if (first.getClass() == second.getClass()) {
                // Bit of optimization for this is a very common case:
                if (first instanceof BigDecimal) {
                    return ((BigDecimal) first).compareTo((BigDecimal) second);
                }
                
                if (first instanceof Integer) {
                    return ((Integer) first).compareTo((Integer) second);
                }
                if (first instanceof Long) {
                    return ((Long) first).compareTo((Long) second);
                }
                if (first instanceof Double) {
                    return ((Double) first).compareTo((Double) second);
                }
                if (first instanceof Float) {
                    return ((Float) first).compareTo((Float) second);
                }
                if (first instanceof Byte) {
                    return ((Byte) first).compareTo((Byte) second);
                }
                if (first instanceof Short) {
                    return ((Short) first).compareTo((Short) second);
                }
            }
            // We are going to compare values of two different types.
            
            // Handle infinity before we try conversion to BigDecimal, as that BigDecimal can't represent that:
            if (first instanceof Double) {
                double firstD = first.doubleValue();
                if (Double.isInfinite(firstD)) {
                    if (_NumberUtils.hasTypeThatIsKnownToNotSupportInfiniteAndNaN(second)) {
                        return  firstD == Double.NEGATIVE_INFINITY ? -1 : 1;
                    }
                    if (second instanceof Float) {
                        return Double.compare(firstD, second.doubleValue());
                    }
                }
            }
            if (first instanceof Float) {
                float firstF = first.floatValue();
                if (Float.isInfinite(firstF)) {
                    if (_NumberUtils.hasTypeThatIsKnownToNotSupportInfiniteAndNaN(second)) {
                        return firstF == Float.NEGATIVE_INFINITY ? -1 : 1;
                    }
                    if (second instanceof Double) {
                        return Double.compare(firstF, second.doubleValue());
                    }
                }
            }
            if (second instanceof Double) {
                double secondD = second.doubleValue();
                if (Double.isInfinite(secondD)) {
                    if (_NumberUtils.hasTypeThatIsKnownToNotSupportInfiniteAndNaN(first)) {
                        return secondD == Double.NEGATIVE_INFINITY ? 1 : -1;
                    }
                    if (first instanceof Float) {
                        return Double.compare(first.doubleValue(), secondD);
                    }
                }
            }
            if (second instanceof Float) {
                float secondF = second.floatValue();
                if (Float.isInfinite(secondF)) {
                    if (_NumberUtils.hasTypeThatIsKnownToNotSupportInfiniteAndNaN(first)) {
                        return secondF == Float.NEGATIVE_INFINITY ? 1 : -1;
                    }
                    if (first instanceof Double) {
                        return Double.compare(first.doubleValue(), secondF);
                    }
                }
            }
            
            return _NumberUtils.toBigDecimal(first).compareTo(_NumberUtils.toBigDecimal(second));
        }
    }

    @Override
    public Number add(Number first, Number second) {
        BigDecimal left = _NumberUtils.toBigDecimal(first);
        BigDecimal right = _NumberUtils.toBigDecimal(second);
        return left.add(right);
    }

    @Override
    public Number subtract(Number first, Number second) {
        BigDecimal left = _NumberUtils.toBigDecimal(first);
        BigDecimal right = _NumberUtils.toBigDecimal(second);
        return left.subtract(right);
    }

    @Override
    public Number multiply(Number first, Number second) {
        BigDecimal left = _NumberUtils.toBigDecimal(first);
        BigDecimal right = _NumberUtils.toBigDecimal(second);
        BigDecimal result = left.multiply(right);
        if (result.scale() > maxScale) {
            result = result.setScale(maxScale, roundingPolicy);
        }
        return result;
    }

    @Override
    public Number divide(Number first, Number second) {
        BigDecimal left = _NumberUtils.toBigDecimal(first);
        BigDecimal right = _NumberUtils.toBigDecimal(second);
        return divide(left, right);
    }

    @Override
    public Number modulus(Number first, Number second) {
        long left = first.longValue();
        long right = second.longValue();
        return Long.valueOf(left % right);
    }

    @Override
    public Number toNumber(String s) {
        return _NumberUtils.toBigDecimalOrDouble(s);
    }

    private BigDecimal divide(BigDecimal left, BigDecimal right) {
        int scale1 = left.scale();
        int scale2 = right.scale();
        int scale = Math.max(scale1, scale2);
        scale = Math.max(minScale, scale);
        return left.divide(right, scale, roundingPolicy);
    }
}

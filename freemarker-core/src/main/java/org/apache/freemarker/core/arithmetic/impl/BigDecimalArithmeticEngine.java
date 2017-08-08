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
            BigDecimal left = _NumberUtils.toBigDecimal(first);
            BigDecimal right = _NumberUtils.toBigDecimal(second);
            return left.compareTo(right);
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

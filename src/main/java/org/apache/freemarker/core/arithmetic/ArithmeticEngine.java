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

package org.apache.freemarker.core.arithmetic;

import java.math.BigDecimal;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.util._StringUtil;

/**
 * Implements the arithmetic operations executed by the template language; see
 * {@link Configuration#setArithmeticEngine(ArithmeticEngine)}.
 */
public abstract class ArithmeticEngine {

    public abstract int compareNumbers(Number first, Number second) throws TemplateException;
    public abstract Number add(Number first, Number second) throws TemplateException;
    public abstract Number subtract(Number first, Number second) throws TemplateException;
    public abstract Number multiply(Number first, Number second) throws TemplateException;
    public abstract Number divide(Number first, Number second) throws TemplateException;
    public abstract Number modulus(Number first, Number second) throws TemplateException;
    // [FM3] Add negate (should keep the Number type even for BigDecimalArithmeticEngine, unlike multiply). Then fix
    // the negate operation in the template language.

    /**
     * Should be able to parse all FTL numerical literals, Java Double toString results, and XML Schema numbers.
     * This means these should be parsed successfully, except if the arithmetical engine
     * couldn't support the resulting value anyway (such as NaN, infinite, even non-integers):
     * {@code -123.45}, {@code 1.5e3}, {@code 1.5E3}, {@code 0005}, {@code +0}, {@code -0}, {@code NaN},
     * {@code INF}, {@code -INF}, {@code Infinity}, {@code -Infinity}. 
     */    
    public abstract Number toNumber(String s);

    protected int minScale = 12;
    protected int maxScale = 12;
    protected int roundingPolicy = BigDecimal.ROUND_HALF_UP;

    /**
     * Sets the minimal scale to use when dividing BigDecimal numbers. Default
     * value is 12.
     */
    public void setMinScale(int minScale) {
        if (minScale < 0) {
            throw new IllegalArgumentException("minScale < 0");
        }
        this.minScale = minScale;
    }
    
    /**
     * Sets the maximal scale to use when multiplying BigDecimal numbers. 
     * Default value is 100.
     */
    public void setMaxScale(int maxScale) {
        if (maxScale < minScale) {
            throw new IllegalArgumentException("maxScale < minScale");
        }
        this.maxScale = maxScale;
    }

    public void setRoundingPolicy(int roundingPolicy) {
        if (roundingPolicy != BigDecimal.ROUND_CEILING
            && roundingPolicy != BigDecimal.ROUND_DOWN
            && roundingPolicy != BigDecimal.ROUND_FLOOR
            && roundingPolicy != BigDecimal.ROUND_HALF_DOWN
            && roundingPolicy != BigDecimal.ROUND_HALF_EVEN
            && roundingPolicy != BigDecimal.ROUND_HALF_UP
            && roundingPolicy != BigDecimal.ROUND_UNNECESSARY
            && roundingPolicy != BigDecimal.ROUND_UP) {
            throw new IllegalArgumentException("invalid rounding policy");        
        }
        
        this.roundingPolicy = roundingPolicy;
    }

}

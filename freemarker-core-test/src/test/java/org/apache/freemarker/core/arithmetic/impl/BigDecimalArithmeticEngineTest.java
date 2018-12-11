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

import static org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

public class BigDecimalArithmeticEngineTest {

    @Test
    public void compareNumbersZeroTest() {
        assertEquals(0, INSTANCE.compareNumbers(0, new BigDecimal("-0")));
        assertEquals(0, INSTANCE.compareNumbers(0.0, new BigDecimal("-0")));
        assertEquals(0, INSTANCE.compareNumbers(-0.0, new BigDecimal("+0")));
        assertEquals(0, INSTANCE.compareNumbers(-0.0, 0.0));
    }

    @Test
    public void compareNumbersNoRoundingGlitchTest() {
        assertEquals(0, INSTANCE.compareNumbers(1.1, new BigDecimal("1.1")));
        assertEquals(-1, INSTANCE.compareNumbers(Double.MIN_VALUE, Math.nextUp(Double.MIN_VALUE)));
        assertEquals(-1, INSTANCE.compareNumbers(
                Double.MIN_VALUE, new BigDecimal("" + Math.nextUp(Double.MIN_VALUE))));
    }

    @Test
    public void compareNumbersSameTypeTest() {
        assertEquals(-1, INSTANCE.compareNumbers(1, 2));
        assertEquals(-1, INSTANCE.compareNumbers(1L, 2L));
        assertEquals(-1, INSTANCE.compareNumbers((short) 1, (short) 2));
        assertEquals(-1, INSTANCE.compareNumbers((byte) 1, (byte) 2));
        assertEquals(-1, INSTANCE.compareNumbers(1.0, 2.0));
        assertEquals(-1, INSTANCE.compareNumbers(1.0f, 2.0f));
        assertEquals(-1, INSTANCE.compareNumbers(BigDecimal.ONE, BigDecimal.TEN));
    }

    @Test
    public void compareNumbersScaleDoesNotMatterTest() {
        assertEquals(0, INSTANCE.compareNumbers(1.0, new BigDecimal("1")));
        assertEquals(0, INSTANCE.compareNumbers(1.0, new BigDecimal("1.00")));
        assertEquals(0, INSTANCE.compareNumbers(1.0f, new BigDecimal("0.1E1")));
        assertEquals(0, INSTANCE.compareNumbers(1, new BigDecimal("1.0")));
    }

    @Test
    public void compareNumbersInfinityTest() {
        for (boolean isFloat : new boolean[] { false, true }) {
            Number negInf = isFloat ? Float.NEGATIVE_INFINITY : Double.NEGATIVE_INFINITY;
            Number posInf = isFloat ? Float.POSITIVE_INFINITY : Double.POSITIVE_INFINITY;
            
            assertEquals(-1, INSTANCE.compareNumbers(negInf, posInf));
            assertEquals(1, INSTANCE.compareNumbers(posInf, negInf));
            assertEquals(0, INSTANCE.compareNumbers(posInf, posInf));
            assertEquals(0, INSTANCE.compareNumbers(negInf, negInf));
            
            Number otherNegInf = !isFloat ? Float.NEGATIVE_INFINITY : Double.NEGATIVE_INFINITY;
            Number otherPosInf = !isFloat ? Float.POSITIVE_INFINITY : Double.POSITIVE_INFINITY;
            assertEquals(-1, INSTANCE.compareNumbers(negInf, otherPosInf));
            assertEquals(1, INSTANCE.compareNumbers(posInf, otherNegInf));
            assertEquals(0, INSTANCE.compareNumbers(posInf, otherPosInf));
            assertEquals(0, INSTANCE.compareNumbers(negInf, otherNegInf));
            
            for (Number one : new Number[] {
                    BigDecimal.ONE, 1.0, 1f, 1, 1L, (byte) 1, (short) 1, BigInteger.ONE,
                    BigDecimal.ONE.negate(), -1.0, -1f, -1, -1L, (byte) -1, (short) -1, BigInteger.ONE.negate(),
                    BigDecimal.ZERO, 0.0, 0f, 0, 0L, (byte) 0, (short) 0, BigInteger.ZERO }) {
                assertEquals(-1, INSTANCE.compareNumbers(negInf, one));
                assertEquals(1, INSTANCE.compareNumbers(posInf, one));
                assertEquals(1, INSTANCE.compareNumbers(one, negInf));
                assertEquals(-1, INSTANCE.compareNumbers(one, posInf));
            }
        }
    }
    
}

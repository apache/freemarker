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
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._MiscTemplateException;
import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._NumberUtil;

/**
 * Arithmetic engine that uses (more-or-less) the widening conversions of
 * Java language to determine the type of result of operation, instead of
 * converting everything to BigDecimal up front.
 * <p>
 * Widening conversions occur in following situations:
 * <ul>
 * <li>byte and short are always widened to int (alike to Java language).</li>
 * <li>To preserve magnitude: when operands are of different types, the
 * result type is the type of the wider operand.</li>
 * <li>to avoid overflows: if add, subtract, or multiply would overflow on
 * integer types, the result is widened from int to long, or from long to
 * BigInteger.</li>
 * <li>to preserve fractional part: if a division of integer types would
 * have a fractional part, int and long are converted to double, and
 * BigInteger is converted to BigDecimal. An operation on a float and a
 * long results in a double. An operation on a float or double and a
 * BigInteger results in a BigDecimal.</li>
 * </ul>
 */
// [FM3] Review
public class ConservativeArithmeticEngine extends ArithmeticEngine {

    public static final ConservativeArithmeticEngine INSTANCE = new ConservativeArithmeticEngine();

    private static final int INTEGER = 0;
    private static final int LONG = 1;
    private static final int FLOAT = 2;
    private static final int DOUBLE = 3;
    private static final int BIG_INTEGER = 4;
    private static final int BIG_DECIMAL = 5;

    private static final Map classCodes = createClassCodesMap();

    protected ConservativeArithmeticEngine() {
        //
    }

    @Override
    public int compareNumbers(Number first, Number second) throws TemplateException {
        switch (getCommonClassCode(first, second)) {
            case INTEGER: {
                int n1 = first.intValue();
                int n2 = second.intValue();
                return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
            }
            case LONG: {
                long n1 = first.longValue();
                long n2 = second.longValue();
                return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
            }
            case FLOAT: {
                float n1 = first.floatValue();
                float n2 = second.floatValue();
                return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
            }
            case DOUBLE: {
                double n1 = first.doubleValue();
                double n2 = second.doubleValue();
                return  n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                return n1.compareTo(n2);
            }
            case BIG_DECIMAL: {
                BigDecimal n1 = _NumberUtil.toBigDecimal(first);
                BigDecimal n2 = _NumberUtil.toBigDecimal(second);
                return n1.compareTo(n2);
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new Error();
    }

    @Override
    public Number add(Number first, Number second) throws TemplateException {
        switch(getCommonClassCode(first, second)) {
            case INTEGER: {
                int n1 = first.intValue();
                int n2 = second.intValue();
                int n = n1 + n2;
                return
                    ((n ^ n1) < 0 && (n ^ n2) < 0) // overflow check
                    ? Long.valueOf(((long) n1) + n2)
                    : Integer.valueOf(n);
            }
            case LONG: {
                long n1 = first.longValue();
                long n2 = second.longValue();
                long n = n1 + n2;
                return
                    ((n ^ n1) < 0 && (n ^ n2) < 0) // overflow check
                    ? toBigInteger(first).add(toBigInteger(second))
                    : Long.valueOf(n);
            }
            case FLOAT: {
                return Float.valueOf(first.floatValue() + second.floatValue());
            }
            case DOUBLE: {
                return Double.valueOf(first.doubleValue() + second.doubleValue());
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                return n1.add(n2);
            }
            case BIG_DECIMAL: {
                BigDecimal n1 = _NumberUtil.toBigDecimal(first);
                BigDecimal n2 = _NumberUtil.toBigDecimal(second);
                return n1.add(n2);
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new Error();
    }

    @Override
    public Number subtract(Number first, Number second) throws TemplateException {
        switch(getCommonClassCode(first, second)) {
            case INTEGER: {
                int n1 = first.intValue();
                int n2 = second.intValue();
                int n = n1 - n2;
                return
                    ((n ^ n1) < 0 && (n ^ ~n2) < 0) // overflow check
                    ? Long.valueOf(((long) n1) - n2)
                    : Integer.valueOf(n);
            }
            case LONG: {
                long n1 = first.longValue();
                long n2 = second.longValue();
                long n = n1 - n2;
                return
                    ((n ^ n1) < 0 && (n ^ ~n2) < 0) // overflow check
                    ? toBigInteger(first).subtract(toBigInteger(second))
                    : Long.valueOf(n);
            }
            case FLOAT: {
                return Float.valueOf(first.floatValue() - second.floatValue());
            }
            case DOUBLE: {
                return Double.valueOf(first.doubleValue() - second.doubleValue());
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                return n1.subtract(n2);
            }
            case BIG_DECIMAL: {
                BigDecimal n1 = _NumberUtil.toBigDecimal(first);
                BigDecimal n2 = _NumberUtil.toBigDecimal(second);
                return n1.subtract(n2);
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new Error();
    }

    @Override
    public Number multiply(Number first, Number second) throws TemplateException {
        switch(getCommonClassCode(first, second)) {
            case INTEGER: {
                int n1 = first.intValue();
                int n2 = second.intValue();
                int n = n1 * n2;
                return
                    n1 == 0 || n / n1 == n2 // overflow check
                    ? Integer.valueOf(n)
                    : Long.valueOf(((long) n1) * n2);
            }
            case LONG: {
                long n1 = first.longValue();
                long n2 = second.longValue();
                long n = n1 * n2;
                return
                    n1 == 0L || n / n1 == n2 // overflow check
                    ? Long.valueOf(n)
                    : toBigInteger(first).multiply(toBigInteger(second));
            }
            case FLOAT: {
                return Float.valueOf(first.floatValue() * second.floatValue());
            }
            case DOUBLE: {
                return Double.valueOf(first.doubleValue() * second.doubleValue());
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                return n1.multiply(n2);
            }
            case BIG_DECIMAL: {
                BigDecimal n1 = _NumberUtil.toBigDecimal(first);
                BigDecimal n2 = _NumberUtil.toBigDecimal(second);
                BigDecimal r = n1.multiply(n2);
                return r.scale() > maxScale ? r.setScale(maxScale, roundingPolicy) : r;
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new Error();
    }

    @Override
    public Number divide(Number first, Number second) throws TemplateException {
        switch(getCommonClassCode(first, second)) {
            case INTEGER: {
                int n1 = first.intValue();
                int n2 = second.intValue();
                if (n1 % n2 == 0) {
                    return Integer.valueOf(n1 / n2);
                }
                return Double.valueOf(((double) n1) / n2);
            }
            case LONG: {
                long n1 = first.longValue();
                long n2 = second.longValue();
                if (n1 % n2 == 0) {
                    return Long.valueOf(n1 / n2);
                }
                return Double.valueOf(((double) n1) / n2);
            }
            case FLOAT: {
                return Float.valueOf(first.floatValue() / second.floatValue());
            }
            case DOUBLE: {
                return Double.valueOf(first.doubleValue() / second.doubleValue());
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                BigInteger[] divmod = n1.divideAndRemainder(n2);
                if (divmod[1].equals(BigInteger.ZERO)) {
                    return divmod[0];
                } else {
                    BigDecimal bd1 = new BigDecimal(n1);
                    BigDecimal bd2 = new BigDecimal(n2);
                    return bd1.divide(bd2, minScale, roundingPolicy);
                }
            }
            case BIG_DECIMAL: {
                BigDecimal n1 = _NumberUtil.toBigDecimal(first);
                BigDecimal n2 = _NumberUtil.toBigDecimal(second);
                int scale1 = n1.scale();
                int scale2 = n2.scale();
                int scale = Math.max(scale1, scale2);
                scale = Math.max(minScale, scale);
                return n1.divide(n2, scale, roundingPolicy);
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new Error();
    }

    @Override
    public Number modulus(Number first, Number second) throws TemplateException {
        switch(getCommonClassCode(first, second)) {
            case INTEGER: {
                return Integer.valueOf(first.intValue() % second.intValue());
            }
            case LONG: {
                return Long.valueOf(first.longValue() % second.longValue());
            }
            case FLOAT: {
                return Float.valueOf(first.floatValue() % second.floatValue());
            }
            case DOUBLE: {
                return Double.valueOf(first.doubleValue() % second.doubleValue());
            }
            case BIG_INTEGER: {
                BigInteger n1 = toBigInteger(first);
                BigInteger n2 = toBigInteger(second);
                return n1.mod(n2);
            }
            case BIG_DECIMAL: {
                throw new _MiscTemplateException("Can't calculate remainder on BigDecimals");
            }
        }
        // Make the compiler happy. getCommonClassCode() is guaranteed to
        // return only above codes, or throw an exception.
        throw new BugException();
    }

    @Override
    public Number toNumber(String s) {
        Number n = _NumberUtil.toBigDecimalOrDouble(s);
        return n instanceof BigDecimal ? _NumberUtil.optimizeNumberRepresentation(n) : n;
    }

    private static Map createClassCodesMap() {
        Map map = new HashMap(17);
        Integer intcode = Integer.valueOf(INTEGER);
        map.put(Byte.class, intcode);
        map.put(Short.class, intcode);
        map.put(Integer.class, intcode);
        map.put(Long.class, Integer.valueOf(LONG));
        map.put(Float.class, Integer.valueOf(FLOAT));
        map.put(Double.class, Integer.valueOf(DOUBLE));
        map.put(BigInteger.class, Integer.valueOf(BIG_INTEGER));
        map.put(BigDecimal.class, Integer.valueOf(BIG_DECIMAL));
        return map;
    }

    private static int getClassCode(Number num) throws TemplateException {
        try {
            return ((Integer) classCodes.get(num.getClass())).intValue();
        } catch (NullPointerException e) {
            if (num == null) {
                throw new _MiscTemplateException("The Number object was null.");
            } else {
                throw new _MiscTemplateException("Unknown number type ", num.getClass().getName());
            }
        }
    }

    private static int getCommonClassCode(Number num1, Number num2) throws TemplateException {
        int c1 = getClassCode(num1);
        int c2 = getClassCode(num2);
        int c = c1 > c2 ? c1 : c2;
        // If BigInteger is combined with a Float or Double, the result is a
        // BigDecimal instead of BigInteger in order not to lose the
        // fractional parts. If Float is combined with Long, the result is a
        // Double instead of Float to preserve the bigger bit width.
        switch (c) {
            case FLOAT: {
                if ((c1 < c2 ? c1 : c2) == LONG) {
                    return DOUBLE;
                }
                break;
            }
            case BIG_INTEGER: {
                int min = c1 < c2 ? c1 : c2;
                if (min == DOUBLE || min == FLOAT) {
                    return BIG_DECIMAL;
                }
                break;
            }
        }
        return c;
    }

    private static BigInteger toBigInteger(Number num) {
        return num instanceof BigInteger ? (BigInteger) num : new BigInteger(num.toString());
    }
}

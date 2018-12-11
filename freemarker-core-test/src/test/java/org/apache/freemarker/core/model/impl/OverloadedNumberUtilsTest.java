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

package org.apache.freemarker.core.model.impl;

import java.math.BigDecimal;
import java.math.BigInteger;

import junit.framework.TestCase;

@SuppressWarnings("boxing")
public class OverloadedNumberUtilsTest extends TestCase {

    public OverloadedNumberUtilsTest(String name) {
        super(name);
    }
    
    public void testIntegerCoercions() {
        cipEqu(Byte.valueOf(Byte.MAX_VALUE));
        cipEqu(Byte.valueOf((byte) 0));
        cipEqu(Byte.valueOf(Byte.MIN_VALUE));
        
        cipEqu(Short.valueOf(Byte.MAX_VALUE),
                new OverloadedNumberUtils.ShortOrByte((short) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(Short.valueOf((short) 0),
                new OverloadedNumberUtils.ShortOrByte((short) 0, (byte) 0));
        cipEqu(Short.valueOf(Byte.MIN_VALUE),
                new OverloadedNumberUtils.ShortOrByte((short) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(Short.valueOf((short) (Byte.MAX_VALUE + 1)));
        cipEqu(Short.valueOf((short) (Byte.MIN_VALUE - 1)));
        cipEqu(Short.valueOf(Short.MAX_VALUE));
        cipEqu(Short.valueOf(Short.MIN_VALUE));
        
        cipEqu(Integer.valueOf(Byte.MAX_VALUE),
                new OverloadedNumberUtils.IntegerOrByte((int) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(Integer.valueOf(0),
                new OverloadedNumberUtils.IntegerOrByte(0, (byte) 0));
        cipEqu(Integer.valueOf(Byte.MIN_VALUE),
                new OverloadedNumberUtils.IntegerOrByte((int) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(Integer.valueOf(Byte.MAX_VALUE + 1),
                new OverloadedNumberUtils.IntegerOrShort(Byte.MAX_VALUE + 1, (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Integer.valueOf(Byte.MIN_VALUE - 1),
                new OverloadedNumberUtils.IntegerOrShort(Byte.MIN_VALUE - 1, (short) (Byte.MIN_VALUE - 1)));
        cipEqu(Integer.valueOf(Short.MAX_VALUE),
                new OverloadedNumberUtils.IntegerOrShort((int) Short.MAX_VALUE, Short.MAX_VALUE));
        cipEqu(Integer.valueOf(Short.MIN_VALUE),
                new OverloadedNumberUtils.IntegerOrShort((int) Short.MIN_VALUE, Short.MIN_VALUE));
        
        cipEqu(Integer.valueOf(Short.MAX_VALUE + 1));
        cipEqu(Integer.valueOf(Short.MIN_VALUE - 1));
        cipEqu(Integer.valueOf(Integer.MAX_VALUE));
        cipEqu(Integer.valueOf(Integer.MIN_VALUE));
        
        cipEqu(Long.valueOf(Byte.MAX_VALUE),
                new OverloadedNumberUtils.LongOrByte((long) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(Long.valueOf(0),
                new OverloadedNumberUtils.LongOrByte((long) 0, (byte) 0));
        cipEqu(Long.valueOf(Byte.MIN_VALUE),
                new OverloadedNumberUtils.LongOrByte((long) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(Long.valueOf(Byte.MAX_VALUE + 1),
                new OverloadedNumberUtils.LongOrShort((long) (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Long.valueOf(Byte.MIN_VALUE - 1),
                new OverloadedNumberUtils.LongOrShort((long) (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        cipEqu(Long.valueOf(Short.MAX_VALUE),
                new OverloadedNumberUtils.LongOrShort((long) Short.MAX_VALUE, Short.MAX_VALUE));
        cipEqu(Long.valueOf(Short.MIN_VALUE),
                new OverloadedNumberUtils.LongOrShort((long) Short.MIN_VALUE, Short.MIN_VALUE));

        cipEqu(Long.valueOf(Short.MAX_VALUE + 1),
                new OverloadedNumberUtils.LongOrInteger((long) Short.MAX_VALUE + 1, Short.MAX_VALUE + 1));
        cipEqu(Long.valueOf(Short.MIN_VALUE - 1),
                new OverloadedNumberUtils.LongOrInteger((long) Short.MIN_VALUE - 1, Short.MIN_VALUE - 1));
        cipEqu(Long.valueOf(Integer.MAX_VALUE),
                new OverloadedNumberUtils.LongOrInteger((long) Integer.MAX_VALUE, Integer.MAX_VALUE));
        cipEqu(Long.valueOf(Integer.MIN_VALUE),
                new OverloadedNumberUtils.LongOrInteger((long) Integer.MIN_VALUE, Integer.MIN_VALUE));
        
        cipEqu(Long.valueOf(Integer.MAX_VALUE + 1L));
        cipEqu(Long.valueOf(Integer.MIN_VALUE - 1L));
        cipEqu(Long.valueOf(Long.MAX_VALUE));
        cipEqu(Long.valueOf(Long.MIN_VALUE));
    }
    
    public void testIntegerNoCoercions() {
        cipEqu(Integer.valueOf(Byte.MAX_VALUE), Integer.valueOf(Byte.MAX_VALUE), 0);
        cipEqu(Integer.valueOf(0), Integer.valueOf(0), 0);
        cipEqu(Integer.valueOf(Byte.MIN_VALUE), Integer.valueOf(Byte.MIN_VALUE), 0);
    }
    
    public void testIntegerLimitedCoercions() {
        cipEqu(Integer.valueOf(Byte.MAX_VALUE), Integer.valueOf(Byte.MAX_VALUE), TypeFlags.INTEGER);
        cipEqu(Integer.valueOf(0), Integer.valueOf(0), TypeFlags.INTEGER);
        cipEqu(Integer.valueOf(Byte.MIN_VALUE), Integer.valueOf(Byte.MIN_VALUE), TypeFlags.INTEGER);
        
        cipEqu(Long.valueOf(Integer.MAX_VALUE + 1L), Long.valueOf(Integer.MAX_VALUE + 1L), TypeFlags.INTEGER);
        
        for (int n = -1; n < 2; n++) {
            final Long longN = Long.valueOf(n);
            cipEqu(longN, new OverloadedNumberUtils.LongOrInteger(longN, n), TypeFlags.INTEGER);
            cipEqu(longN, new OverloadedNumberUtils.LongOrShort(longN, (short) n), TypeFlags.SHORT);
            cipEqu(longN, new OverloadedNumberUtils.LongOrByte(longN, (byte) n), TypeFlags.BYTE);
            cipEqu(longN, new OverloadedNumberUtils.LongOrShort(longN, (short) n),
                    TypeFlags.SHORT | TypeFlags.INTEGER);
        }
    }

    public void testBigDecimalCoercions() {
        cipEqu(new BigDecimal(123), new OverloadedNumberUtils.IntegerBigDecimal(new BigDecimal(123)));
        cipEqu(new BigDecimal(123), new OverloadedNumberUtils.IntegerBigDecimal(new BigDecimal(123)),
                TypeFlags.DOUBLE | TypeFlags.INTEGER);
        cipEqu(new BigDecimal(123), TypeFlags.INTEGER);
        cipEqu(new BigDecimal(123), TypeFlags.INTEGER | TypeFlags.LONG);
        cipEqu(new BigDecimal(123), TypeFlags.DOUBLE);
        cipEqu(new BigDecimal(123), TypeFlags.DOUBLE | TypeFlags.FLOAT);
        
        cipEqu(new BigDecimal(123.5));
        // Not wasting time with check if it's a whole number if we only have integer-only or non-integer-only targets:  
        cipEqu(new BigDecimal(123.5), TypeFlags.INTEGER | TypeFlags.LONG);
        cipEqu(new BigDecimal(123.5), TypeFlags.DOUBLE | TypeFlags.FLOAT);
        
        cipEqu(new BigDecimal("0.01"));
        cipEqu(new BigDecimal("-0.01"));
        cipEqu(BigDecimal.ZERO, new OverloadedNumberUtils.IntegerBigDecimal(BigDecimal.ZERO));
    }

    public void testUnknownNumberCoercion() {
        cipEqu(new RationalNumber(2, 3));
    }

    @SuppressWarnings("boxing")
    public void testDoubleCoercion() {
        cipEqu(Double.valueOf(1.5), new OverloadedNumberUtils.DoubleOrFloat(1.5));
        cipEqu(Double.valueOf(-0.125), new OverloadedNumberUtils.DoubleOrFloat(-0.125));
        cipEqu(Double.valueOf(Float.MAX_VALUE), new OverloadedNumberUtils.DoubleOrFloat((double) Float.MAX_VALUE));
        cipEqu(Double.valueOf(-Float.MAX_VALUE), new OverloadedNumberUtils.DoubleOrFloat((double) -Float.MAX_VALUE));
        cipEqu(Double.valueOf(Float.MAX_VALUE * 10.0));
        cipEqu(Double.valueOf(-Float.MAX_VALUE * 10.0));
        
        cipEqu(Double.valueOf(0), new OverloadedNumberUtils.DoubleOrByte(0.0, (byte) 0));
        cipEqu(Double.valueOf(Byte.MAX_VALUE), new OverloadedNumberUtils.DoubleOrByte((double) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(Double.valueOf(Byte.MIN_VALUE), new OverloadedNumberUtils.DoubleOrByte((double) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1), new OverloadedNumberUtils.DoubleOrShort((double)
                (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1), new OverloadedNumberUtils.DoubleOrShort((double)
                (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Double.valueOf(Short.MAX_VALUE + 1),
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat((double) Short.MAX_VALUE + 1, Short.MAX_VALUE + 1));
        cipEqu(Double.valueOf(Short.MIN_VALUE - 1),
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat((double) Short.MIN_VALUE - 1, Short.MIN_VALUE -  1));
        cipEqu(Double.valueOf(16777216), new OverloadedNumberUtils.DoubleOrIntegerOrFloat(16777216.0, 16777216));
        cipEqu(Double.valueOf(-16777216), new OverloadedNumberUtils.DoubleOrIntegerOrFloat(-16777216.0, -16777216));
        
        cipEqu(Double.valueOf(Integer.MAX_VALUE),
                new OverloadedNumberUtils.DoubleOrInteger((double) Integer.MAX_VALUE, Integer.MAX_VALUE));
        cipEqu(Double.valueOf(Integer.MIN_VALUE),
                new OverloadedNumberUtils.DoubleOrInteger((double) Integer.MIN_VALUE, Integer.MIN_VALUE));
        
        cipEqu(Double.valueOf(Integer.MAX_VALUE + 1L),
                new OverloadedNumberUtils.DoubleOrLong(Double.valueOf(Integer.MAX_VALUE + 1L), Integer.MAX_VALUE + 1L));
        cipEqu(Double.valueOf(Integer.MIN_VALUE - 1L),
                new OverloadedNumberUtils.DoubleOrLong(Double.valueOf(Integer.MIN_VALUE - 1L), Integer.MIN_VALUE - 1L));
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                new OverloadedNumberUtils.DoubleOrFloat((double) Long.MAX_VALUE));
        cipEqu(Double.valueOf(Long.MIN_VALUE),
                new OverloadedNumberUtils.DoubleOrFloat((double) Long.MIN_VALUE));

        // When only certain target types are present:
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrByte(5.0, (byte) 5), TypeFlags.BYTE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrByte(5.0, (byte) 5), TypeFlags.BYTE | TypeFlags.SHORT);
        cipEqu(Double.valueOf(-129), TypeFlags.BYTE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrShort(5.0, (short) 5), TypeFlags.SHORT);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrInteger(5.0, 5), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrLong(5.0, 5), TypeFlags.LONG);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrFloat(5.0), TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5), Double.valueOf(5), TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtils.DoubleOrFloat(5.0),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5.9), new OverloadedNumberUtils.DoubleOrFloat(5.9),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5.9),
                TypeFlags.DOUBLE | TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5.9), new OverloadedNumberUtils.DoubleOrFloat(5.9),
                TypeFlags.FLOAT | TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5.9), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                new OverloadedNumberUtils.DoubleOrFloat((double) Long.MAX_VALUE),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                TypeFlags.DOUBLE | TypeFlags.LONG);
        cipEqu(Double.valueOf(Long.MIN_VALUE),
                new OverloadedNumberUtils.DoubleOrFloat((double) Long.MIN_VALUE),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(Float.MAX_VALUE * 10),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(-Float.MAX_VALUE * 10),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        
        // Rounded values:
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrByte(0.0000009, (byte) 0));
        cipEqu(Double.valueOf(-0.0000009),
                new OverloadedNumberUtils.DoubleOrByte(-0.0000009, (byte) 0));
        cipEqu(Double.valueOf(0.9999991),
                new OverloadedNumberUtils.DoubleOrByte(0.9999991, (byte) 1));
        cipEqu(Double.valueOf(-0.9999991),
                new OverloadedNumberUtils.DoubleOrByte(-0.9999991, (byte) -1));
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrShort(0.0000009, (short) 0),
                TypeFlags.SHORT | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009), new OverloadedNumberUtils.DoubleOrInteger(0.0000009, 0),
                TypeFlags.INTEGER | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009), new OverloadedNumberUtils.DoubleOrLong(0.0000009, 0),
                TypeFlags.LONG | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrByte(0.0000009, (byte) 0), TypeFlags.BYTE);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrShort(0.0000009, (short) 0), TypeFlags.SHORT);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrInteger(0.0000009, 0), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtils.DoubleOrLong(0.0000009, 0L), TypeFlags.LONG);
        cipEqu(Double.valueOf(0.9999999),
                new OverloadedNumberUtils.DoubleOrInteger(0.9999999, 1), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 0.9e-6),
                new OverloadedNumberUtils.DoubleOrByte(Byte.MAX_VALUE + 0.9e-6, Byte.MAX_VALUE));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 0.9e-6),
                new OverloadedNumberUtils.DoubleOrByte(Byte.MIN_VALUE - 0.9e-6, Byte.MIN_VALUE));
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1.1e-6),
                new OverloadedNumberUtils.DoubleOrFloat(Byte.MAX_VALUE + 1.1e-6));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1.1e-6),
                new OverloadedNumberUtils.DoubleOrFloat(Byte.MIN_VALUE - 1.1e-6));
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 0.9999991),
                new OverloadedNumberUtils.DoubleOrShort(
                        Byte.MAX_VALUE + 0.9999991, (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 0.9999991),
                new OverloadedNumberUtils.DoubleOrShort(
                        Byte.MIN_VALUE - 0.9999991, (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1), new OverloadedNumberUtils.DoubleOrShort((double)
                (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1), new OverloadedNumberUtils.DoubleOrShort((double)
                (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Short.MAX_VALUE + 0.9999991,
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat(Short.MAX_VALUE + 0.9999991, Short.MAX_VALUE + 1));
        cipEqu(Short.MIN_VALUE - 0.9999991,
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat(Short.MIN_VALUE - 0.9999991, Short.MIN_VALUE - 1));
        cipEqu(16777216 + 0.9e-6,
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat(16777216 + 0.9e-6, 16777216));
        cipEqu(-16777216 - 0.9e-6,
                new OverloadedNumberUtils.DoubleOrIntegerOrFloat(-16777216 - 0.9e-6, -16777216));
        
        cipEqu(Integer.MAX_VALUE + 0.9e-6,
                new OverloadedNumberUtils.DoubleOrInteger(Integer.MAX_VALUE + 0.9e-6, Integer.MAX_VALUE));
        cipEqu(Integer.MIN_VALUE - 0.9e-6,
                new OverloadedNumberUtils.DoubleOrInteger(Integer.MIN_VALUE - 0.9e-6, Integer.MIN_VALUE));
        
        cipEqu(Integer.MAX_VALUE + 1L + 0.9e-6,
                new OverloadedNumberUtils.DoubleOrFloat(Integer.MAX_VALUE + 1L + 0.9e-6));
        cipEqu(Integer.MIN_VALUE - 1L - 0.9e-6,
                new OverloadedNumberUtils.DoubleOrFloat(Integer.MIN_VALUE - 1L - 0.9e-6));
        cipEqu(Long.MAX_VALUE + 0.9e-6,
                new OverloadedNumberUtils.DoubleOrFloat(Long.MAX_VALUE + 0.9e-6));
        cipEqu(Long.MIN_VALUE - 0.9e-6,
                new OverloadedNumberUtils.DoubleOrFloat(Long.MIN_VALUE - 0.9e-6));
    }

    @SuppressWarnings("boxing")
    public void testFloatCoercion() {
        cipEqu(1.00002f);
        cipEqu(-1.00002f);
        cipEqu(1.999989f);
        cipEqu(-1.999989f);
        cipEqu(16777218f);
        cipEqu(-16777218f);
        
        cipEqu(1f, new OverloadedNumberUtils.FloatOrByte(1f, (byte) 1));
        cipEqu(-1f, new OverloadedNumberUtils.FloatOrByte(-1f, (byte) -1));
        cipEqu(1.000009f, new OverloadedNumberUtils.FloatOrByte(1.000009f, (byte) 1));
        cipEqu(-1.000009f, new OverloadedNumberUtils.FloatOrByte(-1.000009f, (byte) -1));
        cipEqu(1.999991f, new OverloadedNumberUtils.FloatOrByte(1.999991f, (byte) 2));
        cipEqu(-1.999991f, new OverloadedNumberUtils.FloatOrByte(-1.999991f, (byte) -2));
        
        cipEqu(1000f, new OverloadedNumberUtils.FloatOrShort(1000f, (short) 1000));
        cipEqu(-1000f, new OverloadedNumberUtils.FloatOrShort(-1000f, (short) -1000));
        cipEqu(1000.00006f);

        cipEqu(60000f, new OverloadedNumberUtils.FloatOrInteger(60000f, 60000));
        cipEqu(-60000f, new OverloadedNumberUtils.FloatOrInteger(-60000f, -60000));
        cipEqu(60000.004f);

        cipEqu(100f, new OverloadedNumberUtils.FloatOrByte(100f, (byte) 100), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(1000f, new OverloadedNumberUtils.FloatOrShort(1000f, (short) 1000), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(60000f, new OverloadedNumberUtils.FloatOrInteger(60000f, 60000), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(60000f, new OverloadedNumberUtils.FloatOrInteger(60000f, 60000), TypeFlags.LONG);
        cipEqu((float) Integer.MAX_VALUE, (float) Integer.MAX_VALUE, TypeFlags.LONG);
        cipEqu((float) -Integer.MAX_VALUE, (float) -Integer.MAX_VALUE);
        
        cipEqu(0.5f, 0.5f, TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(0.5f, 0.5f, TypeFlags.DOUBLE);
    }
    
    public void testBigIntegerCoercion() {
        BigInteger bi;
        
        cipEqu(BigInteger.ZERO, new OverloadedNumberUtils.BigIntegerOrByte(BigInteger.ZERO));
        bi = new BigInteger(String.valueOf(Byte.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrByte(bi));
        bi = new BigInteger(String.valueOf(Byte.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrByte(bi));
        
        bi = new BigInteger(String.valueOf(Byte.MAX_VALUE + 1));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Byte.MIN_VALUE - 1));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Short.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Short.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrShort(bi));
        
        bi = new BigInteger(String.valueOf(Short.MAX_VALUE + 1));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Short.MIN_VALUE - 1));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Integer.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Integer.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrInteger(bi));
        
        bi = new BigInteger(String.valueOf(Integer.MAX_VALUE + 1L));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Integer.MIN_VALUE - 1L));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Long.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Long.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrLong(bi));
        
        cipEqu(new BigInteger(String.valueOf(Long.MAX_VALUE)).add(BigInteger.ONE));
        cipEqu(new BigInteger(String.valueOf(Long.MIN_VALUE)).subtract(BigInteger.ONE));
        
        bi = new BigInteger("0");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi),
                TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi),
                TypeFlags.FLOAT);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi),
                TypeFlags.DOUBLE);

        bi = new BigInteger("16777215");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        bi = new BigInteger("-16777215");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        
        bi = new BigInteger("16777216");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        bi = new BigInteger("-16777216");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        
        bi = new BigInteger("16777217");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.FLOAT);
        bi = new BigInteger("-16777217");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.FLOAT);
        
        bi = new BigInteger("9007199254740991");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        bi = new BigInteger("-9007199254740991");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        
        bi = new BigInteger("9007199254740992");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        bi = new BigInteger("-9007199254740992");
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtils.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        
        bi = new BigInteger("9007199254740993");
        cipEqu(bi, TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(bi, TypeFlags.FLOAT);
        cipEqu(bi, TypeFlags.DOUBLE);
        bi = new BigInteger("-9007199254740993");
        cipEqu(bi, TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(bi, TypeFlags.FLOAT);
        cipEqu(bi, TypeFlags.DOUBLE);
        
        bi = new BigInteger("9007199254740994");
        cipEqu(bi, TypeFlags.MASK_KNOWN_NONINTEGERS);
        bi = new BigInteger("-9007199254740994");
        cipEqu(bi, TypeFlags.MASK_KNOWN_NONINTEGERS);
    }
    
    private void cipEqu(Number actualAndExpected) {
        cipEqu(actualAndExpected, actualAndExpected, -1);
    }
    
    private void cipEqu(Number actual, Number expected) {
        cipEqu(actual, expected, -1);
    }

    private void cipEqu(Number actualAndExpected, int flags) {
        cipEqu(actualAndExpected, actualAndExpected, flags);
    }
    
    @SuppressWarnings("boxing")
    private void cipEqu(Number actual, Number expected, int flags) {
        Number res = OverloadedNumberUtils.addFallbackType(actual, flags);
        assertEquals(expected.getClass(), res.getClass());
        assertEquals(expected, res);
        
        // Some number types wrap the number with multiple types and equals() only compares one of them. So we try to
        // catch any inconsistency:
        assertEquals(expected.byteValue(), res.byteValue());
        assertEquals(expected.shortValue(), res.shortValue());
        assertEquals(expected.intValue(), res.intValue());
        assertEquals(expected.longValue(), res.longValue());
        assertEquals(expected.floatValue(), res.floatValue());
        assertEquals(expected.doubleValue(), res.doubleValue());
    }
    
    public void testGetArgumentConversionPrice() {
        // Unknown number types:
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                RationalNumber.class, Integer.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, RationalNumber.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                RationalNumber.class, Float.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                Float.class, RationalNumber.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                RationalNumber.class, RationalNumber.class));
        
        // Fully check some rows (not all of them; the code is generated anyways):

        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Short.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Integer.class));
        assertEquals(10004, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Long.class));
        assertEquals(10005, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, BigInteger.class));
        assertEquals(30006, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Float.class));
        assertEquals(20007, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, Double.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, BigDecimal.class));
        
        assertEquals(45001, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Byte.class));
        assertEquals(44002, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Short.class));
        assertEquals(41003, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Integer.class));
        assertEquals(41004, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Long.class));
        assertEquals(40005, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, BigInteger.class));
        assertEquals(33006, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Float.class));
        assertEquals(32007, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, Double.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, BigDecimal.class));
        
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Short.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Integer.class));
        assertEquals(21004, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Long.class));
        assertEquals(21005, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, BigInteger.class));
        assertEquals(40006, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Float.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, Double.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, BigDecimal.class));
        
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Short.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Integer.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Long.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, BigInteger.class));
        assertEquals(40006, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Float.class));
        assertEquals(20007, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, Double.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, BigDecimal.class));
        
        // Check if all fromC is present:
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Byte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Short.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Integer.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Long.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                BigInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Float.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                Double.class, BigDecimal.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                BigDecimal.class, BigDecimal.class));
        assertEquals(0, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.IntegerBigDecimal.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrFloat.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.FloatOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrShort.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.FloatOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.FloatOrShort.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.FloatOrInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrIntegerOrFloat.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.DoubleOrLong.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrByte.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrShort.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrInteger.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrLong.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrFloat.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtils.getArgumentConversionPrice(
                OverloadedNumberUtils.BigIntegerOrDouble.class, BigDecimal.class));
    }
    
}

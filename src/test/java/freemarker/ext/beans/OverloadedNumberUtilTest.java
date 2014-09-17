/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.beans;

import java.math.BigDecimal;
import java.math.BigInteger;

import junit.framework.TestCase;

@SuppressWarnings("boxing")
public class OverloadedNumberUtilTest extends TestCase {

    public OverloadedNumberUtilTest(String name) {
        super(name);
    }
    
    public void testIntegerCoercions() {
        cipEqu(new Byte(Byte.MAX_VALUE));
        cipEqu(new Byte((byte) 0));
        cipEqu(new Byte(Byte.MIN_VALUE));
        
        cipEqu(new Short(Byte.MAX_VALUE),
                new OverloadedNumberUtil.ShortOrByte((short) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(new Short((short) 0),
                new OverloadedNumberUtil.ShortOrByte((short) 0, (byte) 0));
        cipEqu(new Short(Byte.MIN_VALUE),
                new OverloadedNumberUtil.ShortOrByte((short) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(new Short((short) (Byte.MAX_VALUE + 1)));
        cipEqu(new Short((short) (Byte.MIN_VALUE - 1)));
        cipEqu(new Short(Short.MAX_VALUE));
        cipEqu(new Short(Short.MIN_VALUE));
        
        cipEqu(new Integer(Byte.MAX_VALUE),
                new OverloadedNumberUtil.IntegerOrByte((int) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(new Integer(0),
                new OverloadedNumberUtil.IntegerOrByte(0, (byte) 0));
        cipEqu(new Integer(Byte.MIN_VALUE),
                new OverloadedNumberUtil.IntegerOrByte((int) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(new Integer(Byte.MAX_VALUE + 1),
                new OverloadedNumberUtil.IntegerOrShort(Byte.MAX_VALUE + 1, (short) (Byte.MAX_VALUE + 1)));
        cipEqu(new Integer(Byte.MIN_VALUE - 1),
                new OverloadedNumberUtil.IntegerOrShort(Byte.MIN_VALUE - 1, (short) (Byte.MIN_VALUE - 1)));
        cipEqu(new Integer(Short.MAX_VALUE),
                new OverloadedNumberUtil.IntegerOrShort((int) Short.MAX_VALUE, Short.MAX_VALUE));
        cipEqu(new Integer(Short.MIN_VALUE),
                new OverloadedNumberUtil.IntegerOrShort((int) Short.MIN_VALUE, Short.MIN_VALUE));
        
        cipEqu(new Integer(Short.MAX_VALUE + 1));
        cipEqu(new Integer(Short.MIN_VALUE - 1));
        cipEqu(new Integer(Integer.MAX_VALUE));
        cipEqu(new Integer(Integer.MIN_VALUE));
        
        cipEqu(new Long(Byte.MAX_VALUE),
                new OverloadedNumberUtil.LongOrByte((long) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(new Long(0),
                new OverloadedNumberUtil.LongOrByte((long) 0, (byte) 0));
        cipEqu(new Long(Byte.MIN_VALUE),
                new OverloadedNumberUtil.LongOrByte((long) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(new Long(Byte.MAX_VALUE + 1),
                new OverloadedNumberUtil.LongOrShort((long) (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(new Long(Byte.MIN_VALUE - 1),
                new OverloadedNumberUtil.LongOrShort((long) (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        cipEqu(new Long(Short.MAX_VALUE),
                new OverloadedNumberUtil.LongOrShort((long) Short.MAX_VALUE, Short.MAX_VALUE));
        cipEqu(new Long(Short.MIN_VALUE),
                new OverloadedNumberUtil.LongOrShort((long) Short.MIN_VALUE, Short.MIN_VALUE));

        cipEqu(new Long(Short.MAX_VALUE + 1),
                new OverloadedNumberUtil.LongOrInteger((long) Short.MAX_VALUE + 1, Short.MAX_VALUE + 1));
        cipEqu(new Long(Short.MIN_VALUE - 1),
                new OverloadedNumberUtil.LongOrInteger((long) Short.MIN_VALUE - 1, Short.MIN_VALUE - 1));
        cipEqu(new Long(Integer.MAX_VALUE),
                new OverloadedNumberUtil.LongOrInteger((long) Integer.MAX_VALUE, Integer.MAX_VALUE));
        cipEqu(new Long(Integer.MIN_VALUE),
                new OverloadedNumberUtil.LongOrInteger((long) Integer.MIN_VALUE, Integer.MIN_VALUE));
        
        cipEqu(new Long(Integer.MAX_VALUE + 1L));
        cipEqu(new Long(Integer.MIN_VALUE - 1L));
        cipEqu(new Long(Long.MAX_VALUE));
        cipEqu(new Long(Long.MIN_VALUE));
    }
    
    public void testIntegerNoCoercions() {
        cipEqu(new Integer(Byte.MAX_VALUE), new Integer(Byte.MAX_VALUE), 0);
        cipEqu(new Integer(0), new Integer(0), 0);
        cipEqu(new Integer(Byte.MIN_VALUE), new Integer(Byte.MIN_VALUE), 0);
    }
    
    public void testIntegerLimitedCoercions() {
        cipEqu(new Integer(Byte.MAX_VALUE), new Integer(Byte.MAX_VALUE), TypeFlags.INTEGER);
        cipEqu(new Integer(0), new Integer(0), TypeFlags.INTEGER);
        cipEqu(new Integer(Byte.MIN_VALUE), new Integer(Byte.MIN_VALUE), TypeFlags.INTEGER);
        
        cipEqu(new Long(Integer.MAX_VALUE + 1L), new Long(Integer.MAX_VALUE + 1L), TypeFlags.INTEGER);
        
        for (int n = -1; n < 2; n++) {
            final Long longN = new Long(n);
            cipEqu(longN, new OverloadedNumberUtil.LongOrInteger(longN, n), TypeFlags.INTEGER);
            cipEqu(longN, new OverloadedNumberUtil.LongOrShort(longN, (short) n), TypeFlags.SHORT);
            cipEqu(longN, new OverloadedNumberUtil.LongOrByte(longN, (byte) n), TypeFlags.BYTE);
            cipEqu(longN, new OverloadedNumberUtil.LongOrShort(longN, (short) n),
                    TypeFlags.SHORT | TypeFlags.INTEGER);
        }
    }

    public void testBigDecimalCoercions() {
        cipEqu(new BigDecimal(123), new OverloadedNumberUtil.IntegerBigDecimal(new BigDecimal(123)));
        cipEqu(new BigDecimal(123), new OverloadedNumberUtil.IntegerBigDecimal(new BigDecimal(123)),
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
        cipEqu(BigDecimal.ZERO, new OverloadedNumberUtil.IntegerBigDecimal(BigDecimal.ZERO));
    }

    public void testUnknownNumberCoercion() {
        cipEqu(new RationalNumber(2, 3));
    }

    @SuppressWarnings("boxing")
    public void testDoubleCoercion() {
        cipEqu(Double.valueOf(1.5), new OverloadedNumberUtil.DoubleOrFloat(1.5));
        cipEqu(Double.valueOf(-0.125), new OverloadedNumberUtil.DoubleOrFloat(-0.125));
        cipEqu(Double.valueOf(Float.MAX_VALUE), new OverloadedNumberUtil.DoubleOrFloat((double) Float.MAX_VALUE));
        cipEqu(Double.valueOf(-Float.MAX_VALUE), new OverloadedNumberUtil.DoubleOrFloat((double) -Float.MAX_VALUE));
        cipEqu(Double.valueOf(Float.MAX_VALUE * 10.0));
        cipEqu(Double.valueOf(-Float.MAX_VALUE * 10.0));
        
        cipEqu(Double.valueOf(0), new OverloadedNumberUtil.DoubleOrByte(0.0, (byte) 0));
        cipEqu(Double.valueOf(Byte.MAX_VALUE), new OverloadedNumberUtil.DoubleOrByte((double) Byte.MAX_VALUE, Byte.MAX_VALUE));
        cipEqu(Double.valueOf(Byte.MIN_VALUE), new OverloadedNumberUtil.DoubleOrByte((double) Byte.MIN_VALUE, Byte.MIN_VALUE));
        
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1), new OverloadedNumberUtil.DoubleOrShort((double)
                (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1), new OverloadedNumberUtil.DoubleOrShort((double)
                (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Double.valueOf(Short.MAX_VALUE + 1),
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat((double) Short.MAX_VALUE + 1, Short.MAX_VALUE + 1));
        cipEqu(Double.valueOf(Short.MIN_VALUE - 1),
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat((double) Short.MIN_VALUE - 1, Short.MIN_VALUE-  1));
        cipEqu(Double.valueOf(16777216), new OverloadedNumberUtil.DoubleOrIntegerOrFloat(16777216.0, 16777216));
        cipEqu(Double.valueOf(-16777216), new OverloadedNumberUtil.DoubleOrIntegerOrFloat(-16777216.0, -16777216));
        
        cipEqu(Double.valueOf(Integer.MAX_VALUE),
                new OverloadedNumberUtil.DoubleOrInteger((double) Integer.MAX_VALUE, Integer.MAX_VALUE));
        cipEqu(Double.valueOf(Integer.MIN_VALUE),
                new OverloadedNumberUtil.DoubleOrInteger((double) Integer.MIN_VALUE, Integer.MIN_VALUE));
        
        cipEqu(Double.valueOf(Integer.MAX_VALUE + 1L),
                new OverloadedNumberUtil.DoubleOrLong(Double.valueOf(Integer.MAX_VALUE + 1L), Integer.MAX_VALUE + 1L));
        cipEqu(Double.valueOf(Integer.MIN_VALUE - 1L),
                new OverloadedNumberUtil.DoubleOrLong(Double.valueOf(Integer.MIN_VALUE - 1L), Integer.MIN_VALUE - 1L));
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                new OverloadedNumberUtil.DoubleOrFloat((double) Long.MAX_VALUE));
        cipEqu(Double.valueOf(Long.MIN_VALUE),
                new OverloadedNumberUtil.DoubleOrFloat((double) Long.MIN_VALUE));

        // When only certain target types are present:
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrByte(5.0, (byte) 5), TypeFlags.BYTE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrByte(5.0, (byte) 5), TypeFlags.BYTE | TypeFlags.SHORT);
        cipEqu(Double.valueOf(-129), TypeFlags.BYTE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrShort(5.0, (short) 5), TypeFlags.SHORT);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrInteger(5.0, 5), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrLong(5.0, 5), TypeFlags.LONG);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrFloat(5.0), TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5), Double.valueOf(5), TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(5), new OverloadedNumberUtil.DoubleOrFloat(5.0),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5.9), new OverloadedNumberUtil.DoubleOrFloat(5.9),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(5.9),
                TypeFlags.DOUBLE | TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5.9), new OverloadedNumberUtil.DoubleOrFloat(5.9),
                TypeFlags.FLOAT | TypeFlags.INTEGER);
        cipEqu(Double.valueOf(5.9), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                new OverloadedNumberUtil.DoubleOrFloat((double) Long.MAX_VALUE),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(Long.MAX_VALUE),
                TypeFlags.DOUBLE | TypeFlags.LONG);
        cipEqu(Double.valueOf(Long.MIN_VALUE),
                new OverloadedNumberUtil.DoubleOrFloat((double) Long.MIN_VALUE),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(Float.MAX_VALUE * 10),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(Double.valueOf(-Float.MAX_VALUE * 10),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        
        // Rounded values:
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrByte(0.0000009, (byte) 0));
        cipEqu(Double.valueOf(-0.0000009),
                new OverloadedNumberUtil.DoubleOrByte(-0.0000009, (byte) 0));
        cipEqu(Double.valueOf(0.9999991),
                new OverloadedNumberUtil.DoubleOrByte(0.9999991, (byte) 1));
        cipEqu(Double.valueOf(-0.9999991),
                new OverloadedNumberUtil.DoubleOrByte(-0.9999991, (byte) -1));
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrShort(0.0000009, (short) 0),
                TypeFlags.SHORT | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009), new OverloadedNumberUtil.DoubleOrInteger(0.0000009, 0),
                TypeFlags.INTEGER | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009), new OverloadedNumberUtil.DoubleOrLong(0.0000009, 0),
                TypeFlags.LONG | TypeFlags.DOUBLE);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrByte(0.0000009, (byte) 0), TypeFlags.BYTE);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrShort(0.0000009, (short) 0), TypeFlags.SHORT);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrInteger(0.0000009, 0), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(0.0000009),
                new OverloadedNumberUtil.DoubleOrLong(0.0000009, 0L), TypeFlags.LONG);
        cipEqu(Double.valueOf(0.9999999),
                new OverloadedNumberUtil.DoubleOrInteger(0.9999999, 1), TypeFlags.INTEGER);
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 0.9e-6),
                new OverloadedNumberUtil.DoubleOrByte(Byte.MAX_VALUE + 0.9e-6, Byte.MAX_VALUE));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 0.9e-6),
                new OverloadedNumberUtil.DoubleOrByte(Byte.MIN_VALUE - 0.9e-6, Byte.MIN_VALUE));
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1.1e-6),
                new OverloadedNumberUtil.DoubleOrFloat(Byte.MAX_VALUE + 1.1e-6));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1.1e-6),
                new OverloadedNumberUtil.DoubleOrFloat(Byte.MIN_VALUE - 1.1e-6));
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 0.9999991),
                new OverloadedNumberUtil.DoubleOrShort(
                        Byte.MAX_VALUE + 0.9999991, (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 0.9999991),
                new OverloadedNumberUtil.DoubleOrShort(
                        Byte.MIN_VALUE - 0.9999991, (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Double.valueOf(Byte.MAX_VALUE + 1), new OverloadedNumberUtil.DoubleOrShort((double)
                (Byte.MAX_VALUE + 1), (short) (Byte.MAX_VALUE + 1)));
        cipEqu(Double.valueOf(Byte.MIN_VALUE - 1), new OverloadedNumberUtil.DoubleOrShort((double)
                (Byte.MIN_VALUE - 1), (short) (Byte.MIN_VALUE - 1)));
        
        cipEqu(Short.MAX_VALUE + 0.9999991,
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat(Short.MAX_VALUE + 0.9999991, Short.MAX_VALUE + 1));
        cipEqu(Short.MIN_VALUE - 0.9999991,
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat(Short.MIN_VALUE - 0.9999991, Short.MIN_VALUE - 1));
        cipEqu(16777216 + 0.9e-6,
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat(16777216 + 0.9e-6, 16777216));
        cipEqu(-16777216 - 0.9e-6,
                new OverloadedNumberUtil.DoubleOrIntegerOrFloat(-16777216 - 0.9e-6, -16777216));
        
        cipEqu(Integer.MAX_VALUE + 0.9e-6,
                new OverloadedNumberUtil.DoubleOrInteger(Integer.MAX_VALUE + 0.9e-6, Integer.MAX_VALUE));
        cipEqu(Integer.MIN_VALUE - 0.9e-6,
                new OverloadedNumberUtil.DoubleOrInteger(Integer.MIN_VALUE - 0.9e-6, Integer.MIN_VALUE));
        
        cipEqu(Integer.MAX_VALUE + 1L + 0.9e-6,
                new OverloadedNumberUtil.DoubleOrFloat(Integer.MAX_VALUE + 1L + 0.9e-6));
        cipEqu(Integer.MIN_VALUE - 1L - 0.9e-6,
                new OverloadedNumberUtil.DoubleOrFloat(Integer.MIN_VALUE - 1L - 0.9e-6));
        cipEqu(Long.MAX_VALUE + 0.9e-6,
                new OverloadedNumberUtil.DoubleOrFloat(Long.MAX_VALUE + 0.9e-6));
        cipEqu(Long.MIN_VALUE - 0.9e-6,
                new OverloadedNumberUtil.DoubleOrFloat(Long.MIN_VALUE - 0.9e-6));
    }

    @SuppressWarnings("boxing")
    public void testFloatCoercion() {
        cipEqu(1.00002f);
        cipEqu(-1.00002f);
        cipEqu(1.999989f);
        cipEqu(-1.999989f);
        cipEqu(16777218f);
        cipEqu(-16777218f);
        
        cipEqu(1f, new OverloadedNumberUtil.FloatOrByte(1f, (byte) 1));
        cipEqu(-1f, new OverloadedNumberUtil.FloatOrByte(-1f, (byte) -1));
        cipEqu(1.000009f, new OverloadedNumberUtil.FloatOrByte(1.000009f, (byte) 1));
        cipEqu(-1.000009f, new OverloadedNumberUtil.FloatOrByte(-1.000009f, (byte) -1));
        cipEqu(1.999991f, new OverloadedNumberUtil.FloatOrByte(1.999991f, (byte) 2));
        cipEqu(-1.999991f, new OverloadedNumberUtil.FloatOrByte(-1.999991f, (byte) -2));
        
        cipEqu(1000f, new OverloadedNumberUtil.FloatOrShort(1000f, (short) 1000));
        cipEqu(-1000f, new OverloadedNumberUtil.FloatOrShort(-1000f, (short) -1000));
        cipEqu(1000.00006f);

        cipEqu(60000f, new OverloadedNumberUtil.FloatOrInteger(60000f, 60000));
        cipEqu(-60000f, new OverloadedNumberUtil.FloatOrInteger(-60000f, -60000));
        cipEqu(60000.004f);

        cipEqu(100f, new OverloadedNumberUtil.FloatOrByte(100f, (byte) 100), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(1000f, new OverloadedNumberUtil.FloatOrShort(1000f, (short) 1000), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(60000f, new OverloadedNumberUtil.FloatOrInteger(60000f, 60000), TypeFlags.MASK_KNOWN_INTEGERS);
        cipEqu(60000f, new OverloadedNumberUtil.FloatOrInteger(60000f, 60000), TypeFlags.LONG);
        cipEqu((float) Integer.MAX_VALUE, (float) Integer.MAX_VALUE, TypeFlags.LONG);
        cipEqu((float) -Integer.MAX_VALUE, (float) -Integer.MAX_VALUE);
        
        cipEqu(0.5f, 0.5f, TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(0.5f, 0.5f, TypeFlags.DOUBLE);
    }
    
    public void testBigIntegerCoercion() {
        BigInteger bi;
        
        cipEqu(BigInteger.ZERO, new OverloadedNumberUtil.BigIntegerOrByte(BigInteger.ZERO));
        bi = new BigInteger(String.valueOf(Byte.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrByte(bi));
        bi = new BigInteger(String.valueOf(Byte.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrByte(bi));
        
        bi = new BigInteger(String.valueOf(Byte.MAX_VALUE + 1));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Byte.MIN_VALUE - 1));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Short.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrShort(bi));
        bi = new BigInteger(String.valueOf(Short.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrShort(bi));
        
        bi = new BigInteger(String.valueOf(Short.MAX_VALUE + 1));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Short.MIN_VALUE - 1));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Integer.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrInteger(bi));
        bi = new BigInteger(String.valueOf(Integer.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrInteger(bi));
        
        bi = new BigInteger(String.valueOf(Integer.MAX_VALUE + 1L));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Integer.MIN_VALUE - 1L));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Long.MAX_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrLong(bi));
        bi = new BigInteger(String.valueOf(Long.MIN_VALUE));
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrLong(bi));
        
        cipEqu(new BigInteger(String.valueOf(Long.MAX_VALUE)).add(BigInteger.ONE));
        cipEqu(new BigInteger(String.valueOf(Long.MIN_VALUE)).subtract(BigInteger.ONE));
        
        bi = new BigInteger("0");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi),
                TypeFlags.DOUBLE | TypeFlags.FLOAT);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi),
                TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi),
                TypeFlags.FLOAT);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi),
                TypeFlags.DOUBLE);

        bi = new BigInteger("16777215");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        bi = new BigInteger("-16777215");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        
        bi = new BigInteger("16777216");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        bi = new BigInteger("-16777216");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrFloat(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        
        bi = new BigInteger("16777217");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.FLOAT);
        bi = new BigInteger("-16777217");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, TypeFlags.FLOAT);
        
        bi = new BigInteger("9007199254740991");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        bi = new BigInteger("-9007199254740991");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        
        bi = new BigInteger("9007199254740992");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        bi = new BigInteger("-9007199254740992");
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.MASK_KNOWN_NONINTEGERS);
        cipEqu(bi, new OverloadedNumberUtil.BigIntegerOrDouble(bi), TypeFlags.DOUBLE);
        
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
        Number res = OverloadedNumberUtil.addFallbackType(actual, flags);
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
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                RationalNumber.class, Integer.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, RationalNumber.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                RationalNumber.class, Float.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                Float.class, RationalNumber.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                RationalNumber.class, RationalNumber.class));
        
        // Fully check some rows (not all of them; the code is generated anyways):

        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Short.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Integer.class));
        assertEquals(10004, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Long.class));
        assertEquals(10005, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, BigInteger.class));
        assertEquals(30006, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Float.class));
        assertEquals(20007, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, Double.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, BigDecimal.class));
        
        assertEquals(45001, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Byte.class));
        assertEquals(44002, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Short.class));
        assertEquals(41003, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Integer.class));
        assertEquals(41004, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Long.class));
        assertEquals(40005, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, BigInteger.class));
        assertEquals(33006, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Float.class));
        assertEquals(32007, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, Double.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, BigDecimal.class));
        
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Short.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Integer.class));
        assertEquals(21004, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Long.class));
        assertEquals(21005, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, BigInteger.class));
        assertEquals(40006, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Float.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, Double.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, BigDecimal.class));
        
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Byte.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Short.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Integer.class));
        assertEquals(Integer.MAX_VALUE, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Long.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, BigInteger.class));
        assertEquals(40006, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Float.class));
        assertEquals(20007, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, Double.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, BigDecimal.class));
        
        // Check if all fromC is present:
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Byte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Short.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Integer.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Long.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                BigInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Float.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                Double.class, BigDecimal.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                BigDecimal.class, BigDecimal.class));
        assertEquals(0, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.IntegerBigDecimal.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrFloat.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.FloatOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrShort.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.FloatOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.FloatOrShort.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.FloatOrInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrByte.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrIntegerOrFloat.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrInteger.class, BigDecimal.class));
        assertEquals(20008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.DoubleOrLong.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrByte.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrShort.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrInteger.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrLong.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrFloat.class, BigDecimal.class));
        assertEquals(10008, OverloadedNumberUtil.getArgumentConversionPrice(
                OverloadedNumberUtil.BigIntegerOrDouble.class, BigDecimal.class));
    }
    
}

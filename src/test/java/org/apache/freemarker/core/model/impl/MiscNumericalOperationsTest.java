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

import org.junit.Assert;

import junit.framework.TestCase;

public class MiscNumericalOperationsTest extends TestCase {

    public MiscNumericalOperationsTest(String name) {
        super(name);
    }
    
    public void testForceUnwrappedNumberToType() {
        // Usual type to to all other types:
        Double n = Double.valueOf(123.75);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Short.class), Short.valueOf(n.shortValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Short.TYPE), Short.valueOf(n.shortValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Byte.class), Byte.valueOf(n.byteValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Byte.TYPE), Byte.valueOf(n.byteValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Integer.class), Integer.valueOf(n.intValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Integer.TYPE), Integer.valueOf(n.intValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Long.class), Long.valueOf(n.longValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Long.TYPE), Long.valueOf(n.longValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Float.class), Float.valueOf(n.floatValue()));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Float.TYPE), Float.valueOf(n.floatValue()));
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Double.class) == n);
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Double.TYPE) == n);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, BigInteger.class), new BigInteger("123"));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(n, BigDecimal.class), new BigDecimal("123.75"));
        
        // Cases of conversion to BigDecimal:
        BigDecimal bd = new BigDecimal("123");
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(new BigInteger("123"), BigDecimal.class), bd);
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(bd, BigDecimal.class) == bd);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(Long.valueOf(123), BigDecimal.class), bd);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(Double.valueOf(123), BigDecimal.class), bd);
        
        // Cases of conversion to BigInteger:
        BigInteger bi = new BigInteger("123");
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(new BigDecimal("123.6"), BigInteger.class), bi);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(
                new OverloadedNumberUtil.IntegerBigDecimal(new BigDecimal("123")), BigInteger.class), bi);
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(bi, BigInteger.class) == bi);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(Long.valueOf(123), BigInteger.class), bi);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(Double.valueOf(123.6), BigInteger.class), bi);

        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(n, Number.class) == n);
        assertNull(DefaultObjectWrapper.forceUnwrappedNumberToType(n, RationalNumber.class));
        RationalNumber r = new RationalNumber(1, 2);
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(r, RationalNumber.class) == r);
        assertTrue(DefaultObjectWrapper.forceUnwrappedNumberToType(r, Number.class) == r);
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(r, Double.class), Double.valueOf(0.5));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(r, BigDecimal.class), new BigDecimal("0.5"));
        assertEquals(DefaultObjectWrapper.forceUnwrappedNumberToType(r, BigInteger.class), BigInteger.ZERO);
    }
    
    @SuppressWarnings("boxing")
    public void testForceNumberArgumentsToParameterTypes() {
        OverloadedMethodsSubset oms = new OverloadedFixArgsMethods();
        Class[] paramTypes = new Class[] { Short.TYPE, Short.class, Double.TYPE, BigDecimal.class, BigInteger.class };
        Object[] args;
        
        args = newArgs();
        oms.forceNumberArgumentsToParameterTypes(args, paramTypes, new int[] { 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF });
        Assert.assertArrayEquals(
                args,
                new Object[] { (short) 123, (short) 123, 123.75, new BigDecimal("123.75"), BigInteger.valueOf(123) });
        
        args = newArgs();
        oms.forceNumberArgumentsToParameterTypes(args, paramTypes, new int[] { 0, 0, 0, 0, 0 });
        Assert.assertArrayEquals(args, newArgs());
        
        args = newArgs();
        oms.forceNumberArgumentsToParameterTypes(args, paramTypes, new int[] { 8, 8, 8, 8, 8 });
        Assert.assertArrayEquals(args, newArgs());
        
        args = newArgs();
        oms.forceNumberArgumentsToParameterTypes(args, paramTypes, new int[] { 0xFFFF, 0, 0xFFFF, 0, 0xFFFF });
        Assert.assertArrayEquals(
                args,
                new Object[] { (short) 123, 123.75, 123.75, 123.75, BigInteger.valueOf(123) });
    }

    @SuppressWarnings("boxing")
    private Object[] newArgs() {
        return new Object[] { 123.75, 123.75, 123.75, 123.75, 123.75 };
    }

}

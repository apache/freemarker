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

import org.junit.Assert;

import freemarker.template.Configuration;

public class MiscNumericalOperationsTest extends TestCase {

    public MiscNumericalOperationsTest(String name) {
        super(name);
    }
    
    public void testForceUnwrappedNumberToType() {
        // Usual type to to all other types:
        Double n = Double.valueOf(123.75);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Short.class, false), Short.valueOf(n.shortValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Short.TYPE, false), Short.valueOf(n.shortValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Byte.class, false), Byte.valueOf(n.byteValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Byte.TYPE, false), Byte.valueOf(n.byteValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Integer.class, false), Integer.valueOf(n.intValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Integer.TYPE, false), Integer.valueOf(n.intValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Long.class, false), Long.valueOf(n.longValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Long.TYPE, false), Long.valueOf(n.longValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Float.class, false), Float.valueOf(n.floatValue()));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, Float.TYPE, false), Float.valueOf(n.floatValue()));
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(n, Double.class, false) == n);
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(n, Double.TYPE, false) == n);
        try {
            assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, BigInteger.class, false), new BigInteger("123"));
            fail();
        } catch (NumberFormatException e) {
            // expected
        }
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, BigInteger.class, true), new BigInteger("123"));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(n, BigDecimal.class, false), new BigDecimal("123.75"));
        
        // Cases of conversion to BigDecimal:
        BigDecimal bd = new BigDecimal("123");
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(new BigInteger("123"), BigDecimal.class, false), bd);
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(bd, BigDecimal.class, false) == bd);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(Long.valueOf(123), BigDecimal.class, false), bd);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(Double.valueOf(123), BigDecimal.class, false), bd);
        
        // Cases of conversion to BigInteger:
        BigInteger bi = new BigInteger("123");
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(new BigDecimal("123.6"), BigInteger.class, true), bi);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(
                new OverloadedNumberUtil.IntegerBigDecimal(new BigDecimal("123")), BigInteger.class, true), bi);
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(bi, BigInteger.class, true) == bi);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(Long.valueOf(123), BigInteger.class, true), bi);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(Double.valueOf(123.6), BigInteger.class, true), bi);

        assertTrue(BeansWrapper.forceUnwrappedNumberToType(n, Number.class, true) == n);
        assertNull(BeansWrapper.forceUnwrappedNumberToType(n, RationalNumber.class, true));
        RationalNumber r = new RationalNumber(1, 2);
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(r, RationalNumber.class, true) == r);
        assertTrue(BeansWrapper.forceUnwrappedNumberToType(r, Number.class, true) == r);
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(r, Double.class, true), Double.valueOf(0.5));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(r, BigDecimal.class, true), new BigDecimal("0.5"));
        assertEquals(BeansWrapper.forceUnwrappedNumberToType(r, BigInteger.class, true), BigInteger.ZERO);
    }
    
    @SuppressWarnings("boxing")
    public void testForceNumberArgumentsToParameterTypes() {
        OverloadedMethodsSubset oms
                = new OverloadedFixArgsMethods(new BeansWrapper(Configuration.VERSION_2_3_21).is2321Bugfixed());
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

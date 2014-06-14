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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;
import freemarker.template.utility.NumberUtil;

@SuppressWarnings("boxing")
public class ParameterListPreferabilityTest extends TestCase {

    public ParameterListPreferabilityTest(String name) {
        super(name);
    }
    
    public void testNumberical() {
        // Note: the signature lists consists of the same elements, only their order changes depending on the type
        // of the argument value.
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { byte.class },
                    new Class[] { Byte.class },
                    new Class[] { short.class },
                    new Class[] { Short.class },
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { BigInteger.class },
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { BigDecimal.class },
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class }
                },
                new Object[] { (byte) 1 });
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { BigDecimal.class },
                    new Class[] { BigInteger.class },
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    new Class[] { short.class },
                    new Class[] { Short.class },
                    new Class[] { byte.class },
                    new Class[] { Byte.class },
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class },
                },
                new Object[] { new OverloadedNumberUtil.IntegerBigDecimal(new BigDecimal("1")) });

        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { BigDecimal.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    new Class[] { BigInteger.class },
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { short.class },
                    new Class[] { Short.class },
                    new Class[] { byte.class },
                    new Class[] { Byte.class },
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class },
                },
                new Object[] { new BigDecimal("1") /* possibly non-integer */ });
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { BigDecimal.class },
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { short.class },
                    new Class[] { Short.class },
                    new Class[] { byte.class },
                    new Class[] { Byte.class },
                    new Class[] { BigInteger.class },
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class },
                },
                new Object[] { new OverloadedNumberUtil.FloatOrByte(1f, (byte) 1) });
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { BigInteger.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { BigDecimal.class },
                    new Class[] { short.class },
                    new Class[] { Short.class },
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    
                    // Two incompatibles removed, would be removed in reality:
                    new Class[] { byte.class },
                    new Class[] { Byte.class },
                    
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class }
                },
                new Object[] { new OverloadedNumberUtil.IntegerOrShort(1, (short) 1) });
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { long.class },
                    new Class[] { Long.class },
                    new Class[] { BigInteger.class },
                    new Class[] { BigDecimal.class },
                    new Class[] { double.class },
                    new Class[] { Double.class },
                    new Class[] { float.class },
                    new Class[] { Float.class },
                    // skip byte and short  as the would be equal with int (all invalid target types)
                    new Class[] { int.class },  // In reality, this and Integer are removed as not-applicable overloads
                    new Class[] { Integer.class },
                    new Class[] { Number.class },
                    new Class[] { Serializable.class },
                    new Class[] { Object.class }
                },
                new Object[] { 1L });
        
        // Undecidable comparisons:
        
        testAllCmpPermutationsEqu(
                new Class[][] {
                        new Class[] { Byte.class },
                        new Class[] { Short.class },
                        new Class[] { Integer.class },
                        new Class[] { Long.class },
                        new Class[] { BigInteger.class },
                        new Class[] { Float.class },
                    },
                    new Object[] { 1.0 });
        
        testAllCmpPermutationsEqu(
                new Class[][] {
                        new Class[] { byte.class },
                        new Class[] { short.class },
                        new Class[] { int.class },
                        new Class[] { long.class },
                        new Class[] { float.class },
                    },
                    new Object[] { 1.0 });
    }
    
    public void testPrimitiveIsMoreSpecific() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { boolean.class },
                    new Class[] { Boolean.class }
                },
                new Object[] { true });
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { char.class },
                    new Class[] { Character.class }
                },
                new Object[] { 'x' });
    }
    
    public void testCharIsMoreSpecificThanString() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { char.class },
                    new Class[] { Character.class },
                    new Class[] { String.class },
                    new Class[] { CharSequence.class }
                },
                new Object[] { "s" });
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { char.class },
                    new Class[] { Character.class },
                    new Class[] { String.class }
                },
                new Object[] { 'c' });
    }
    
    public void testClassHierarchy() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { LinkedHashMap.class },
                    new Class[] { HashMap.class },
                    new Class[] { Map.class },
                    new Class[] { Object.class }
                },
                new Object[] { new LinkedHashMap() });
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { LinkedHashMap.class },
                    new Class[] { Cloneable.class },
                    new Class[] { Object.class }
                },
                new Object[] { new LinkedHashMap() });
    }

    public void testNumericalWithNonNumerical() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { Comparable.class },
                    new Class[] { Object.class },
                },
                new Object[] { 1 });
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { int.class },
                    new Class[] { Integer.class },
                    new Class[] { char.class },
                    new Class[] { Character.class },
                },
                new Object[] { 1 });
    }
    
    public void testUnrelated() {
        testAllCmpPermutationsEqu(
                new Class[][] {
                    new Class[] { Serializable.class },
                    new Class[] { CharSequence.class },
                    new Class[] { Comparable.class }
                },
                new Object[] { "s" });
        
        testAllCmpPermutationsEqu(
                new Class[][] {
                    new Class[] { HashMap.class },
                    new Class[] { TreeMap.class }
                },
                new Object[] { null });
    }

    public void testMultiParameter() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, String.class, String.class },
                        
                    new Class[] { String.class, String.class, Object.class },
                    new Class[] { String.class, Object.class, String.class },
                    new Class[] { Object.class, String.class, String.class },
                    
                    new Class[] { String.class, Object.class, Object.class },
                    new Class[] { Object.class, String.class, Object.class },
                    new Class[] { Object.class, Object.class, String.class },
                    
                    new Class[] { Object.class, Object.class, Object.class },
                },
                new Object[] { "a", "b", "c" });
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, String.class },
                    new Class[] { String.class, Object.class },
                    new Class[] { CharSequence.class, CharSequence.class },
                    new Class[] { CharSequence.class, Object.class },
                    new Class[] { Object.class, String.class },
                    new Class[] { Object.class, CharSequence.class },
                    new Class[] { Object.class, Object.class },
                },
                new Object[] { "a", "b" });
        
        /** Subclassing is more important than primitive-VS-boxed: */
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { boolean.class, boolean.class, boolean.class, String.class },
                    new Class[] { Boolean.class, boolean.class, boolean.class, String.class },
                    new Class[] { boolean.class, Boolean.class, Boolean.class, String.class },
                    new Class[] { Boolean.class, boolean.class, Boolean.class, String.class },
                    new Class[] { Boolean.class, Boolean.class, boolean.class, String.class },
                    new Class[] { Boolean.class, Boolean.class, Boolean.class, String.class },
                    new Class[] { Boolean.class, Boolean.class, Boolean.class, CharSequence.class },
                    new Class[] { boolean.class, boolean.class, boolean.class, Object.class },
                    new Class[] { Boolean.class, boolean.class, boolean.class, Object.class },
                    new Class[] { boolean.class, Boolean.class, Boolean.class, Object.class },
                    new Class[] { Boolean.class, boolean.class, Boolean.class, Object.class },
                    new Class[] { Boolean.class, Boolean.class, boolean.class, Object.class },
                    new Class[] { Boolean.class, Boolean.class, Boolean.class, Object.class },
                },
                new Object[] { true, false, true, "a" });
        
        /** Subclassing is more important than primitive-VS-boxed: */
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { int.class, int.class, int.class, String.class },
                    new Class[] { Integer.class, int.class, int.class, String.class },
                    new Class[] { int.class, Integer.class, Integer.class, String.class },
                    new Class[] { Integer.class, int.class, Integer.class, String.class },
                    new Class[] { Integer.class, Integer.class, int.class, String.class },
                    new Class[] { Integer.class, Integer.class, Integer.class, String.class },
                    new Class[] { Integer.class, Integer.class, Integer.class, CharSequence.class },
                    new Class[] { int.class, int.class, int.class, Object.class },
                    new Class[] { Integer.class, int.class, int.class, Object.class },
                    new Class[] { int.class, Integer.class, Integer.class, Object.class },
                    new Class[] { Integer.class, int.class, Integer.class, Object.class },
                    new Class[] { Integer.class, Integer.class, int.class, Object.class },
                    new Class[] { Integer.class, Integer.class, Integer.class, Object.class },
                },
                new Object[] { 1, 2, 3, "a" });
    }

    public void testVarargs() {
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, String[].class },
                    new Class[] { String.class, CharSequence[].class },
                    new Class[] { String.class, Object[].class },
                },
                new Object[] { "a", "b", "c" },
                true);
        
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, int[].class },
                    new Class[] { String.class, Integer[].class },
                    new Class[] { String.class, long[].class },
                    new Class[] { String.class, Long[].class },
                    new Class[] { String.class, double[].class },
                    new Class[] { String.class, Double[].class },
                    new Class[] { String.class, Serializable[].class },
                    new Class[] { String.class, Object[].class },
                },
                new Object[] { "a", 1, 2, 3 },
                true);
        
        // 0-long varargs list; in case of ambiguity, the varargs component type decides:
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, Object[].class },
                    new Class[] { CharSequence.class, int[].class },
                    new Class[] { CharSequence.class, Integer[].class },
                    new Class[] { CharSequence.class, long[].class },
                    new Class[] { CharSequence.class, Long[].class },
                    new Class[] { CharSequence.class, double[].class },
                    new Class[] { CharSequence.class, Double[].class },
                    new Class[] { CharSequence.class, Serializable[].class },
                    new Class[] { CharSequence.class, Object[].class },
                    new Class[] { Object.class, int[].class },
                },
                new Object[] { "a" },
                true);
        
        
        // Different fixed prefix length; in the case of ambiguity, the one with higher fixed param count wins.
        testAllCmpPermutationsInc(
                new Class[][] {
                    new Class[] { String.class, int.class, int.class, int[].class },
                    new Class[] { String.class, int.class, int[].class },
                    new Class[] { String.class, int[].class },
                },
                new Object[] { "a", 1, 2, 3 },
                true);
    }
    
    private void testAllCmpPermutationsInc(Class[][] sortedSignatures, Object[] args) {
        testAllCmpPermutationsInc(sortedSignatures, args, false);
    }

    /**
     * Compares all items with all other items in the provided descending sorted array of signatures, checking that
     * for all valid indexes i and j, where j > i, it stands that sortedSignatures[i] > sortedSignatures[j].
     * The comparisons are done with both operand orders, also each items is compared to itself too.
     * 
     * @param sortedSignatures method signatures sorted by decreasing specificity
     */
    private void testAllCmpPermutationsInc(Class[][] sortedSignatures, Object[] args, boolean varargs) {
        final ArgumentTypes argTs = new ArgumentTypes(args, true);
        for (int i = 0; i < sortedSignatures.length; i++) {
            for (int j = 0; j < sortedSignatures.length; j++) {
                assertEquals("sortedSignatures[" + i + "] <==> sortedSignatures [" + j + "]",
                        NumberUtil.getSignum(
                                Integer.valueOf(j).compareTo(i)),
                        NumberUtil.getSignum(
                                argTs.compareParameterListPreferability(
                                        sortedSignatures[i], sortedSignatures[j], varargs)));
            }
        }
    }

    private void testAllCmpPermutationsEqu(Class[][] signatures, Object[] args) {
        final ArgumentTypes argTs = new ArgumentTypes(args, true);
        for (int i = 0; i < signatures.length; i++) {
            for (int j = 0; j < signatures.length; j++) {
                assertEquals("sortedSignatures[" + i + "] <==> sortedSignatures [" + j + "]",
                        0,
                        argTs.compareParameterListPreferability(signatures[i], signatures[j], false));
            }
        }
    }
    
}

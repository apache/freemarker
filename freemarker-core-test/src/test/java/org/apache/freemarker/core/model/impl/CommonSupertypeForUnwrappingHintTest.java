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

import java.io.Serializable;
import java.util.List;

import org.apache.freemarker.core.model.TemplateModelException;

import junit.framework.TestCase;

public class CommonSupertypeForUnwrappingHintTest extends TestCase {
    
    final OverloadedMethodsSubset oms = new DummyOverloadedMethodsSubset();

    public CommonSupertypeForUnwrappingHintTest(String name) {
        super(name);
    }

    public void testInterfaces() {
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(String.class, Number.class));
        assertEquals(C1I1.class, oms.getCommonSupertypeForUnwrappingHint(C2ExtC1I1.class, C3ExtC1I1.class));
        assertEquals(Object.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, C4I1I2.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, C5I1.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, I1.class));
        assertEquals(I2.class, oms.getCommonSupertypeForUnwrappingHint(C3I1I2.class, I2.class));
        assertEquals(I1.class, oms.getCommonSupertypeForUnwrappingHint(I1I2.class, I1.class));
        assertEquals(I2.class, oms.getCommonSupertypeForUnwrappingHint(I1I2.class, I2.class));
        assertEquals(CharSequence.class, oms.getCommonSupertypeForUnwrappingHint(String.class, StringBuilder.class));
        assertEquals(C6.class, oms.getCommonSupertypeForUnwrappingHint(C7ExtC6I1.class, C8ExtC6I1.class));
    }

    public void testArrayAndOther() {
        testArrayAndOther(oms);
    }
    
    /** These will be the same with oms and buggy: */
    private void testArrayAndOther(OverloadedMethodsSubset oms) {
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(int[].class, String.class));
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(Object[].class, String.class));
        
        assertEquals(Object.class, oms.getCommonSupertypeForUnwrappingHint(int[].class, List.class));
        assertEquals(Object.class, oms.getCommonSupertypeForUnwrappingHint(Object[].class, List.class));
        
        assertEquals(int[].class, oms.getCommonSupertypeForUnwrappingHint(int[].class, int[].class));
        assertEquals(Object[].class, oms.getCommonSupertypeForUnwrappingHint(Object[].class, Object[].class));
    }
    
    public void testArrayAndDifferentArray() {
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(int[].class, Object[].class));
        assertEquals(Serializable.class, oms.getCommonSupertypeForUnwrappingHint(int[].class, long[].class));
    }
    
    public void testPrimitive() {
        assertEquals(Integer.class, oms.getCommonSupertypeForUnwrappingHint(int.class, Integer.class));
        assertEquals(Integer.class, oms.getCommonSupertypeForUnwrappingHint(Integer.class, int.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(int.class, Long.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(Long.class, int.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(Integer.class, long.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(long.class, Integer.class));
        assertEquals(Boolean.class, oms.getCommonSupertypeForUnwrappingHint(boolean.class, Boolean.class));
        assertEquals(Boolean.class, oms.getCommonSupertypeForUnwrappingHint(Boolean.class, boolean.class));
        assertEquals(Character.class, oms.getCommonSupertypeForUnwrappingHint(char.class, Character.class));
        assertEquals(Character.class, oms.getCommonSupertypeForUnwrappingHint(Character.class, char.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(int.class, short.class));
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(short.class, int.class));
    }

    public void testMisc() {
        assertEquals(Number.class, oms.getCommonSupertypeForUnwrappingHint(Long.class, Integer.class));
        assertEquals(char.class, oms.getCommonSupertypeForUnwrappingHint(char.class, char.class));
        assertEquals(Integer.class, oms.getCommonSupertypeForUnwrappingHint(Integer.class, Integer.class));
        assertEquals(String.class, oms.getCommonSupertypeForUnwrappingHint(String.class, String.class));
    }
    
    static interface I1 { };
    static class C1I1 implements I1 { };
    static class C2ExtC1I1 extends C1I1 { };
    static class C3ExtC1I1 extends C1I1 { };
    static interface I2 { };
    static class C3I1I2 implements I1, I2 { };
    static class C4I1I2 implements I1, I2 { };
    static class C5I1 implements I1 { };
    static interface I1I2 extends I1, I2 { };
    static class C6 { };
    static class C7ExtC6I1 extends C6 implements I1 { };
    static class C8ExtC6I1 extends C6 implements I1 { };
    
    private static class DummyOverloadedMethodsSubset extends OverloadedMethodsSubset {

        DummyOverloadedMethodsSubset() {
            super();
        }

        @Override
        Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc) {
            return memberDesc.getParamTypes();
        }

        @Override
        void afterWideningUnwrappingHints(Class[] paramTypes, int[] paramNumericalTypes) {
            // Do nothing
        }

        @Override
        MaybeEmptyMemberAndArguments getMemberAndArguments(List tmArgs, DefaultObjectWrapper w) throws TemplateModelException {
            throw new RuntimeException("Not implemented in this dummy.");
        }
        
    }

}

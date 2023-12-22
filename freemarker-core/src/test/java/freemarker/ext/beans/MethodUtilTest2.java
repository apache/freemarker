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

package freemarker.ext.beans;

import static freemarker.ext.beans._MethodUtil.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class MethodUtilTest2 {

    @Test
    public void testGetMethodWithClosestNonSubReturnType1() {
        List<Method> methods = getMethods(ObjectM.class, ListM.class, CollectionM.class);
        assertEquals(getMethod(ObjectM.class), getMethodWithClosestNonSubReturnType(Object.class, methods));
        assertEquals(getMethod(CollectionM.class), getMethodWithClosestNonSubReturnType(Collection.class, methods));
        assertEquals(getMethod(ListM.class), getMethodWithClosestNonSubReturnType(List.class, methods));
        assertEquals(getMethod(ListM.class), getMethodWithClosestNonSubReturnType(ArrayList.class, methods));
        assertEquals(getMethod(ObjectM.class), getMethodWithClosestNonSubReturnType(String.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(int.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(void.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType2() {
        List<Method> methods = getMethods(ListM.class, CollectionM.class);
        assertNull(getMethodWithClosestNonSubReturnType(Object.class, methods));
        assertEquals(getMethod(CollectionM.class), getMethodWithClosestNonSubReturnType(Collection.class, methods));
        assertEquals(getMethod(ListM.class), getMethodWithClosestNonSubReturnType(List.class, methods));
        assertEquals(getMethod(ListM.class), getMethodWithClosestNonSubReturnType(ArrayList.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(String.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(int.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(void.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType3() {
        List<Method> methods = getMethods(ObjectM.class, SerializableM.class);
        assertEquals(getMethod(SerializableM.class), getMethodWithClosestNonSubReturnType(String.class, methods));
        assertEquals(getMethod(SerializableM.class), getMethodWithClosestNonSubReturnType(Serializable.class, methods));
        assertEquals(getMethod(ObjectM.class), getMethodWithClosestNonSubReturnType(List.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(int.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(void.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType4() {
        List<Method> methods = getMethods(ReturnType1M.class, ReturnType2M.class, ObjectM.class);
        assertEquals(getMethod(ReturnType2M.class), getMethodWithClosestNonSubReturnType(ReturnType3.class, methods));
        assertEquals(getMethod(ReturnType2M.class), getMethodWithClosestNonSubReturnType(ReturnType2.class, methods));
        assertEquals(getMethod(ReturnType1M.class), getMethodWithClosestNonSubReturnType(ReturnType1.class, methods));
        assertEquals(getMethod(ObjectM.class), getMethodWithClosestNonSubReturnType(Serializable.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType5() {
        List<Method> methods = getMethods(SerializableM.class, ReturnType1M.class);
        assertEquals(getMethod(ReturnType1M.class), getMethodWithClosestNonSubReturnType(ReturnType3.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType6() {
        List<Method> methods = getMethods(SerializableM.class);
        assertEquals(getMethod(SerializableM.class), getMethodWithClosestNonSubReturnType(ReturnType3.class, methods));
    }

    @Test
    public void testGetMethodWithClosestNonSubReturnType7() {
        List<Method> methods = getMethods(IntM.class, VoidM.class, ObjectM.class, CollectionM.class);
        assertEquals(getMethod(IntM.class), getMethodWithClosestNonSubReturnType(int.class, methods));
        assertEquals(getMethod(VoidM.class), getMethodWithClosestNonSubReturnType(void.class, methods));
        assertNull(getMethodWithClosestNonSubReturnType(long.class, methods));
        assertEquals(getMethod(ObjectM.class), getMethodWithClosestNonSubReturnType(Long.class, methods));
        assertEquals(getMethod(CollectionM.class), getMethodWithClosestNonSubReturnType(List.class, methods));
    }

    private static List<Method> getMethods(Class<?>... methodHolders) {
        List<Method> result = new ArrayList<>();
        for (Class<?> methodHolder : methodHolders) {
            result.add(getMethod(methodHolder));
        }
        return result;
    }

    private static Method getMethod(Class<?> methodHolder) {
        try {
            return methodHolder.getMethod("m");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ObjectM {
        public Object m() { return null; }
    }

    public static class CollectionM {
        public Collection<?> m() { return null; }
    }

    public static class ListM {
        public List<?> m() { return null; }
    }

    public static class SerializableM {
        public Serializable m() { return null; }
    }

    public static class StringM {
        public String m() { return null; }
    }

    public static class ReturnType1M {
        public ReturnType1 m() { return null; }
    }

    public static class ReturnType2M {
        public ReturnType2 m() { return null; }
    }

    public static class ReturnType3M {
        public ReturnType3 m() { return null; }
    }

    public static class IntM {
        public int m() { return 0; }
    }

    public static class LongM {
        public long m() { return 0L; }
    }

    public static class VoidM {
        public void m() { }
    }

    public static class ReturnType1 { }
    public static class ReturnType2 extends ReturnType1 implements Serializable { }
    public static class ReturnType3 extends ReturnType2 { }

}
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

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

public class MethodMatcherTest {

    @Test
    public void testReturnTypeOverload() throws NoSuchMethodException {
        MethodMatcher matcher = new MethodMatcher();
        Method genericM = TestReturnTypeOverloadGeneric.class.getMethod("m");
        assertEquals(Object.class, genericM.getReturnType());
        matcher.addMatching(TestReturnTypeOverloadGeneric.class, genericM);

        Method stringM = TestReturnTypeOverloadString.class.getMethod("m");
        assertEquals(String.class, stringM.getReturnType());

        assertTrue(matcher.matches(TestReturnTypeOverloadGeneric.class, genericM));
        assertTrue(matcher.matches(TestReturnTypeOverloadString.class, genericM));
        assertTrue(matcher.matches(TestReturnTypeOverloadString.class, stringM));
    }

    public static class TestReturnTypeOverloadGeneric<T> {
        public T m() {
            return null;
        };
    }

    public static class TestReturnTypeOverloadString extends TestReturnTypeOverloadGeneric<String> {
        public String m() {
            return "";
        };
    }

    /** Mostly to test upper bound classes. */
    @Test
    public void testInheritance() throws NoSuchMethodException {
        {
            MethodMatcher matcher = new MethodMatcher();
            Method m = TestInheritanceC2.class.getMethod("m1");
            assertEquals(m, TestInheritanceC1.class.getMethod("m1"));
            matcher.addMatching(TestInheritanceC2.class, m);
            assertFalse(matcher.matches(TestInheritanceC1.class, m));
            assertTrue(matcher.matches(TestInheritanceC2.class, m));
            assertTrue(matcher.matches(TestInheritanceC3.class, m));
        }
        {
            MethodMatcher matcher = new MethodMatcher();
            Method m = TestInheritanceC2.class.getMethod("m2");
            assertNotEquals(m, TestInheritanceC1.class.getMethod("m2"));
            matcher.addMatching(TestInheritanceC2.class, m);
            assertFalse(matcher.matches(TestInheritanceC1.class, m));
            assertTrue(matcher.matches(TestInheritanceC2.class, m));
            assertTrue(matcher.matches(TestInheritanceC3.class, m));
        }
        {
            // m2 again, but with a non-same-instance but "equal" method.
            MethodMatcher matcher = new MethodMatcher();
            Method m = TestInheritanceC1.class.getMethod("m2");
            matcher.addMatching(TestInheritanceC2.class, m);
            assertFalse(matcher.matches(TestInheritanceC1.class, m));
            assertTrue(matcher.matches(TestInheritanceC2.class, m));
            assertTrue(matcher.matches(TestInheritanceC3.class, m));
        }
        {
            MethodMatcher matcher = new MethodMatcher();
            Method m = TestInheritanceC2.class.getMethod("m3");
            assertEquals(m, TestInheritanceC1.class.getMethod("m3"));
            assertNotEquals(m, TestInheritanceC3.class.getMethod("m3"));
            matcher.addMatching(TestInheritanceC2.class, m);
            assertFalse(matcher.matches(TestInheritanceC1.class, m));
            assertTrue(matcher.matches(TestInheritanceC2.class, m));
            assertTrue(matcher.matches(TestInheritanceC3.class, m));
        }
    }

    public static class TestInheritanceC1 {
        public void m1() {
        }

        public void m2() {
        }

        public void m3() {
        }
    }

    public static class TestInheritanceC2 extends TestInheritanceC1 {
        @Override
        public void m2() {
        }
    }

    public static class TestInheritanceC3 extends TestInheritanceC2 {
        @Override
        public void m3() {
        }
    }

    /** Mostly to test when same method associated to multiple unrelated classes. */
    @Test
    public void testInheritance2() throws NoSuchMethodException {
        MethodMatcher matcher = new MethodMatcher();
        Method m = Runnable.class.getMethod("run");
        matcher.addMatching(TestInheritance2SafeRunnable1.class, m);
        matcher.addMatching(TestInheritance2SafeRunnable2.class, m);

        assertTrue(matcher.matches(
                TestInheritance2SafeRunnable1.class, TestInheritance2SafeRunnable1.class.getMethod("run")));
        assertTrue(matcher.matches(
                TestInheritance2SafeRunnable2.class, TestInheritance2SafeRunnable2.class.getMethod("run")));
        assertFalse(matcher.matches(
                TestInheritance2UnsafeRunnable.class, TestInheritance2UnsafeRunnable.class.getMethod("run")));
    }

    public static class TestInheritance2SafeRunnable1 implements Runnable {
        public void run() {
        }
    }

    public static class TestInheritance2SafeRunnable2 implements Runnable {
        public void run() {
        }
    }

    public static class TestInheritance2UnsafeRunnable implements Runnable {
        public void run() {
        }
    }

    @Test
    public void testOverloads() throws NoSuchMethodException {
        Method mInt = TestOverloads.class.getMethod("m", int.class);
        Method mIntInt = TestOverloads.class.getMethod("m", int.class, int.class);
        {
            MethodMatcher matcher = new MethodMatcher();
            matcher.addMatching(TestOverloads.class, mInt);
            assertTrue(matcher.matches(TestOverloads.class, mInt));
            assertFalse(matcher.matches(TestOverloads.class, mIntInt));
        }
        {
            MethodMatcher matcher = new MethodMatcher();
            matcher.addMatching(TestOverloads.class, mIntInt);
            assertFalse(matcher.matches(TestOverloads.class, mInt));
            assertTrue(matcher.matches(TestOverloads.class, mIntInt));
        }
    }

    public static class TestOverloads {
        public void m(int x) {
        }

        public void m(int x, int y) {
        }
    }

}

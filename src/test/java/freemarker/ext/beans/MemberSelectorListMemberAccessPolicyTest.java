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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;

import org.junit.Test;

public class MemberSelectorListMemberAccessPolicyTest {

    @Test
    public void testEmpty() throws NoSuchMethodException, NoSuchFieldException {
        WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy();
        ClassMemberAccessPolicy classPolicy = policy.forClass(C1.class);
        assertFalse(classPolicy.isConstructorExposed(C1.class.getConstructor()));
        assertFalse(classPolicy.isMethodExposed(C1.class.getMethod("m1")));
        assertFalse(classPolicy.isFieldExposed(C1.class.getField("f1")));
    }

    @Test
    public void testBasics() throws NoSuchMethodException, NoSuchFieldException {
        WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                C1.class.getName() + "." + C1.class.getSimpleName() + "()",
                C1.class.getName() + ".m1()",
                C1.class.getName() + ".m2(int)",
                C1.class.getName() + ".f1");

        {
            ClassMemberAccessPolicy c1Policy = policy.forClass(C1.class);
            assertTrue(c1Policy.isConstructorExposed(C1.class.getConstructor()));
            assertTrue(c1Policy.isMethodExposed(C1.class.getMethod("m1")));
            assertTrue(c1Policy.isMethodExposed(C1.class.getMethod("m2", int.class)));
            assertTrue(c1Policy.isFieldExposed(C1.class.getField("f1")));
        }

        {
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            assertFalse(d1Policy.isMethodExposed(D1.class.getMethod("m1")));
            assertFalse(d1Policy.isFieldExposed(D1.class.getField("f1")));
        }
    }

    @Test
    public void testInheritanceAndMoreOverloads() throws NoSuchMethodException, NoSuchFieldException {
        WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                C1.class.getName() + ".m2(int)",
                C1.class.getName() + ".f1",
                C2.class.getName() + "." + C2.class.getSimpleName() + "(int)",
                C2.class.getName() + ".m1()",
                C2.class.getName() + ".m2(boolean)",
                C3.class.getName() + ".f2",
                C3.class.getName() + "." + C3.class.getSimpleName() + "()",
                C3.class.getName() + ".m4()",
                C3.class.getName() + ".f3"
        );
        ClassMemberAccessPolicy c1Policy = policy.forClass(C1.class);
        ClassMemberAccessPolicy c2Policy = policy.forClass(C2.class);
        ClassMemberAccessPolicy c3Policy = policy.forClass(C3.class);

        assertTrue(c1Policy.isMethodExposed(C1.class.getMethod("m2", int.class)));
        assertTrue(c2Policy.isMethodExposed(C2.class.getMethod("m2", int.class)));
        assertTrue(c3Policy.isMethodExposed(C3.class.getMethod("m2", int.class)));

        assertTrue(c1Policy.isFieldExposed(C1.class.getField("f1")));
        assertTrue(c2Policy.isFieldExposed(C2.class.getField("f1")));
        assertTrue(c3Policy.isFieldExposed(C3.class.getField("f1")));

        assertFalse(c1Policy.isConstructorExposed(C1.class.getConstructor(int.class)));
        assertTrue(c2Policy.isConstructorExposed(C2.class.getConstructor(int.class)));
        assertFalse(c3Policy.isConstructorExposed(C3.class.getConstructor(int.class))); // Not inherited

        assertFalse(c1Policy.isMethodExposed(C1.class.getMethod("m1")));
        assertTrue(c2Policy.isMethodExposed(C2.class.getMethod("m1")));
        assertTrue(c3Policy.isMethodExposed(C3.class.getMethod("m1")));

        assertFalse(c1Policy.isMethodExposed(C2.class.getMethod("m2", boolean.class))); // Doesn't exist in C1
        assertTrue(c2Policy.isMethodExposed(C2.class.getMethod("m2", boolean.class)));
        assertTrue(c3Policy.isMethodExposed(C3.class.getMethod("m2", boolean.class)));

        assertFalse(c1Policy.isFieldExposed(C1.class.getField("f2")));
        assertFalse(c2Policy.isFieldExposed(C2.class.getField("f2")));
        assertTrue(c3Policy.isFieldExposed(C3.class.getField("f2")));

        assertFalse(c1Policy.isConstructorExposed(C1.class.getConstructor()));
        assertFalse(c2Policy.isConstructorExposed(C1.class.getConstructor())); // Doesn't exist in C2
        assertTrue(c3Policy.isConstructorExposed(C3.class.getConstructor()));

        assertFalse(c1Policy.isMethodExposed(C2.class.getMethod("m4"))); // Doesn't exist in C1
        assertFalse(c2Policy.isMethodExposed(C2.class.getMethod("m4")));
        assertTrue(c3Policy.isMethodExposed(C3.class.getMethod("m4")));

        assertFalse(c1Policy.isFieldExposed(C2.class.getField("f3"))); // Doesn't exist in C1
        assertFalse(c2Policy.isFieldExposed(C2.class.getField("f3")));
        assertTrue(c3Policy.isFieldExposed(C3.class.getField("f3")));
    }

    @Test
    public void testInterfaces() throws NoSuchMethodException, NoSuchFieldException {
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    I1.class.getName() + ".m1()",
                    I1.class.getName() + ".f1"
            );
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            ClassMemberAccessPolicy d2Policy = policy.forClass(D2.class);
            ClassMemberAccessPolicy e1Policy = policy.forClass(E1.class);
            ClassMemberAccessPolicy e2Policy = policy.forClass(E2.class);
            assertTrue(d1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(d2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(d1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(d2Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e2Policy.isFieldExposed(I1.class.getField("f1")));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    I1Sub.class.getName() + ".m1()",
                    I1Sub.class.getName() + ".m2()",
                    I1Sub.class.getName() + ".f1"
            );
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            ClassMemberAccessPolicy d2Policy = policy.forClass(D2.class);
            ClassMemberAccessPolicy e1Policy = policy.forClass(E1.class);
            ClassMemberAccessPolicy e2Policy = policy.forClass(E2.class);
            assertFalse(d1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertFalse(d2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertFalse(d1Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertFalse(d2Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertTrue(e1Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertTrue(e2Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertFalse(d1Policy.isFieldExposed(I1.class.getField("f1")));
            assertFalse(d2Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e2Policy.isFieldExposed(I1.class.getField("f1")));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    I1.class.getName() + ".m1()",
                    I1.class.getName() + ".f1"
            );
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            ClassMemberAccessPolicy d2Policy = policy.forClass(D2.class);
            ClassMemberAccessPolicy e1Policy = policy.forClass(E1.class);
            ClassMemberAccessPolicy e2Policy = policy.forClass(E2.class);
            assertTrue(d1Policy.isMethodExposed(I1Sub.class.getMethod("m1")));
            assertTrue(d2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertFalse(d1Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertFalse(d2Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertFalse(e1Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertFalse(e2Policy.isMethodExposed(I1Sub.class.getMethod("m2")));
            assertTrue(d1Policy.isFieldExposed(I1Sub.class.getField("f1")));
            assertTrue(d2Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e2Policy.isFieldExposed(I1.class.getField("f1")));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    D2.class.getName() + ".m1()",
                    D2.class.getName() + ".f1"
            );
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            ClassMemberAccessPolicy d2Policy = policy.forClass(D2.class);
            assertFalse(d1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(d2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertFalse(d1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(d2Policy.isFieldExposed(I1.class.getField("f1")));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    I1Sub.class.getName() + ".m1()",
                    D2.class.getName() + ".m1()",
                    I1Sub.class.getName() + ".m2()",
                    J1.class.getName() + ".m2()",
                    I1.class.getName() + ".f1",
                    I1Sub.class.getName() + ".f1"
            );
            ClassMemberAccessPolicy d1Policy = policy.forClass(D1.class);
            ClassMemberAccessPolicy d2Policy = policy.forClass(D2.class);
            ClassMemberAccessPolicy e1Policy = policy.forClass(E1.class);
            ClassMemberAccessPolicy e2Policy = policy.forClass(E2.class);
            ClassMemberAccessPolicy f1Policy = policy.forClass(F1.class);
            assertFalse(d1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(d2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e1Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertTrue(e2Policy.isMethodExposed(I1.class.getMethod("m1")));
            assertFalse(d1Policy.isMethodExposed(J1.class.getMethod("m2")));
            assertFalse(d2Policy.isMethodExposed(J1.class.getMethod("m2")));
            assertTrue(e1Policy.isMethodExposed(J1.class.getMethod("m2")));
            assertTrue(e2Policy.isMethodExposed(J1.class.getMethod("m2")));
            assertTrue(f1Policy.isMethodExposed(J1.class.getMethod("m2")));
            assertTrue(d1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(d2Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e1Policy.isFieldExposed(I1.class.getField("f1")));
            assertTrue(e2Policy.isFieldExposed(I1.class.getField("f1")));
        }
    }

    @Test
    public void testArrayArgs() throws NoSuchMethodException {
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    CArrayArgs.class.getName() + ".m1(java.lang.String)",
                    CArrayArgs.class.getName() + ".m1(java.lang.String[])",
                    CArrayArgs.class.getName() + ".m1(java.lang.String[][])",
                    CArrayArgs.class.getName() + ".m2(" + C1.class.getName() + "[])",
                    CArrayArgs.class.getName() + ".m2("
                            + C1.class.getName() + "[], "
                            + C1.class.getName() + "[], "
                            + C1.class.getName() + ")"
            );
            ClassMemberAccessPolicy classPolicy = policy.forClass(CArrayArgs.class);
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String.class)));
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String[].class)));
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String[][].class)));
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m2", C1[].class)));
            assertTrue(classPolicy.isMethodExposed(
                    CArrayArgs.class.getMethod("m2", C1[].class, C1[].class, C1.class)));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    CArrayArgs.class.getName() + ".m1(java.lang.String)",
                    CArrayArgs.class.getName() + ".m1(java.lang.String[][])"
            );
            ClassMemberAccessPolicy classPolicy = policy.forClass(CArrayArgs.class);
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String.class)));
            assertFalse(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String[].class)));
            assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String[][].class)));
            assertFalse(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m2", C1[].class)));
            assertFalse(classPolicy.isMethodExposed(
                    CArrayArgs.class.getMethod("m2", C1[].class, C1[].class, C1.class)));
        }
    }

    @Test
    public void testBlacklist1() throws NoSuchMethodException, NoSuchFieldException {
        BlacklistMemberAccessPolicy policy = newBlacklistMemberAccessPolicy(
                C1.class.getName() + ".m1()",
                C1.class.getName() + ".f1",
                C1.class.getName() + "." + C1.class.getSimpleName() + "()"
        );

        for (Class<?> cl : new Class[] { C1.class, C2.class, C3.class }) {
            ClassMemberAccessPolicy classPolicy = policy.forClass(cl);
            assertFalse(classPolicy.isMethodExposed(cl.getMethod("m1")));
            assertTrue(classPolicy.isMethodExposed(cl.getMethod("m2", int.class)));
            assertTrue(classPolicy.isMethodExposed(cl.getMethod("m3")));
            assertFalse(classPolicy.isFieldExposed(cl.getField("f1")));
            assertTrue(classPolicy.isFieldExposed(cl.getField("f2")));
            if (cl != C2.class) {
                assertEquals(cl != C1.class, classPolicy.isConstructorExposed(cl.getConstructor()));
            }
            assertTrue(classPolicy.isConstructorExposed(cl.getConstructor(int.class)));
        }
    }

    @Test
    public void testBlacklist2() throws NoSuchMethodException, NoSuchFieldException {
        BlacklistMemberAccessPolicy policy = newBlacklistMemberAccessPolicy(
                C2.class.getName() + ".m1()",
                C2.class.getName() + ".f1",
                C2.class.getName() + "." + C2.class.getSimpleName() + "(int)"
        );

        {
            Class<C1> lc = C1.class;
            ClassMemberAccessPolicy classPolicy = policy.forClass(lc);
            assertTrue(classPolicy.isMethodExposed(lc.getMethod("m1")));
            assertTrue(classPolicy.isFieldExposed(lc.getField("f1")));
            assertTrue(classPolicy.isConstructorExposed(lc.getConstructor(int.class)));
        }

        {
            Class<C2> lc = C2.class;
            ClassMemberAccessPolicy classPolicy = policy.forClass(lc);
            assertFalse(classPolicy.isMethodExposed(lc.getMethod("m1")));
            assertFalse(classPolicy.isFieldExposed(lc.getField("f1")));
            assertFalse(classPolicy.isConstructorExposed(lc.getConstructor(int.class)));
        }

        {
            Class<C3> lc = C3.class;
            ClassMemberAccessPolicy classPolicy = policy.forClass(lc);
            assertFalse(classPolicy.isMethodExposed(lc.getMethod("m1")));
            assertFalse(classPolicy.isFieldExposed(lc.getField("f1")));
            assertTrue(classPolicy.isConstructorExposed(lc.getConstructor(int.class)));
        }
    }

    @Test
    public void testBlacklistIgnoresAnnotation() throws NoSuchMethodException, NoSuchFieldException {
        BlacklistMemberAccessPolicy policy = newBlacklistMemberAccessPolicy(
                CAnnotationsTest1.class.getName() + ".m5()",
                CAnnotationsTest1.class.getName() + ".f5",
                CAnnotationsTest1.class.getName() + "." + CAnnotationsTest1.class.getSimpleName() + "()"
        );

        ClassMemberAccessPolicy classPolicy = policy.forClass(CAnnotationsTest1.class);
        assertFalse(classPolicy.isMethodExposed(CAnnotationsTest1.class.getMethod("m5")));
        assertFalse(classPolicy.isFieldExposed(CAnnotationsTest1.class.getField("f5")));
        assertFalse(classPolicy.isConstructorExposed(CAnnotationsTest1.class.getConstructor()));
    }

    @Test
    public void testBlacklistAndToString() throws NoSuchMethodException {
        {
            BlacklistMemberAccessPolicy policy = newBlacklistMemberAccessPolicy(
                    C1.class.getName() + ".m1()",
                    C1.class.getName() + ".m2()"
            );
            assertTrue(policy.isToStringAlwaysExposed());
            assertTrue(policy.forClass(C1.class).isMethodExposed(Object.class.getMethod("toString")));
        }
        {
            BlacklistMemberAccessPolicy policy = newBlacklistMemberAccessPolicy(
                    C1.class.getName() + ".m1()",
                    C2.class.getName() + ".toString()",
                    C1.class.getName() + ".m2()"
            );
            assertFalse(policy.isToStringAlwaysExposed());
            assertTrue(policy.forClass(C1.class).isMethodExposed(Object.class.getMethod("toString")));
            assertFalse(policy.forClass(C2.class).isMethodExposed(Object.class.getMethod("toString")));
            assertFalse(policy.forClass(C3.class).isMethodExposed(Object.class.getMethod("toString")));
        }
    }

    @Test
    public void testWhitelistAndToString() throws NoSuchMethodException {
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    C2.class.getName() + ".toString()"
            );
            assertFalse(policy.isToStringAlwaysExposed());
            assertFalse(policy.forClass(C1.class).isMethodExposed(Object.class.getMethod("toString")));
            assertTrue(policy.forClass(C2.class).isMethodExposed(Object.class.getMethod("toString")));
            assertTrue(policy.forClass(C3.class).isMethodExposed(Object.class.getMethod("toString")));
        }
        {
            WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                    Object.class.getName() + ".toString()"
            );
            assertTrue(policy.isToStringAlwaysExposed());
            assertTrue(policy.forClass(C1.class).isMethodExposed(Object.class.getMethod("toString")));
        }
    }

    @Test
    public void memberSelectorParserIgnoresWhitespace() throws NoSuchMethodException {
        WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                (CArrayArgs.class.getName() + ".m1(java.lang.String)").replace(".", "\n\t. "),
                CArrayArgs.class.getName() + ".m2("
                        + C1.class.getName() + "  [  ]\t,"
                        + C1.class.getName() + "[]  ,\n "
                        + C1.class.getName() + " )"
        );
        ClassMemberAccessPolicy classPolicy = policy.forClass(CArrayArgs.class);
        assertTrue(classPolicy.isMethodExposed(CArrayArgs.class.getMethod("m1", String.class)));
        assertTrue(classPolicy.isMethodExposed(
                CArrayArgs.class.getMethod("m2", C1[].class, C1[].class, C1.class)));
    }

    @Test
    public void memberSelectorParsingErrorsTest() {
        try {
            newWhitelistMemberAccessPolicy("foo()");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("missing dot"));
        }
        try {
            newWhitelistMemberAccessPolicy("com.example.Foo-bar.m()");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("malformed upper bound class name"));
        }
        try {
            newWhitelistMemberAccessPolicy("java.util.Date.m-x()");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("malformed member name"));
        }
        try {
            newWhitelistMemberAccessPolicy("java.util.Date.to string()");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("malformed member name"));
        }
        try {
            newWhitelistMemberAccessPolicy("java.util.Date.toString(");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("should end with ')'"));
        }
        try {
            newWhitelistMemberAccessPolicy("java.util.Date.m(com.x-y)");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("malformed argument class name"));
        }
        try {
            newWhitelistMemberAccessPolicy("java.util.Date.m(int[)");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("malformed argument class name"));
        }
    }

    @Test
    public void testAnnotation() throws NoSuchFieldException, NoSuchMethodException {
        WhitelistMemberAccessPolicy policy = newWhitelistMemberAccessPolicy(
                CAnnotationsTest2.class.getName() + ".f2",
                CAnnotationsTest2.class.getName() + ".f3",
                CAnnotationsTest2.class.getName() + ".m2()",
                CAnnotationsTest2.class.getName() + ".m3()",
                CAnnotationsTest2.class.getName() + "." + CAnnotationsTest2.class.getSimpleName() + "(int)",
                CAnnotationsTest2.class.getName() + "." + CAnnotationsTest2.class.getSimpleName() + "(int, int)"
        );
        ClassMemberAccessPolicy classPolicy = policy.forClass(CAnnotationsTest2.class);

        assertFalse(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f1")));
        assertTrue(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f2")));
        assertTrue(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f3")));
        assertTrue(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f4")));
        assertTrue(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f5")));
        assertTrue(classPolicy.isFieldExposed(CAnnotationsTest2.class.getField("f6")));

        assertFalse(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m1")));
        assertTrue(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m2")));
        assertTrue(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m3")));
        assertTrue(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m4")));
        assertTrue(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m5")));
        assertTrue(classPolicy.isMethodExposed(CAnnotationsTest2.class.getMethod("m6")));

        assertTrue(classPolicy.isConstructorExposed(
                CAnnotationsTest2.class.getConstructor()));
        assertTrue(classPolicy.isConstructorExposed(
                CAnnotationsTest2.class.getConstructor(int.class)));
        assertTrue(classPolicy.isConstructorExposed(
                CAnnotationsTest2.class.getConstructor(int.class, int.class)));
        assertTrue(classPolicy.isConstructorExposed(
                CAnnotationsTest2.class.getConstructor(int.class, int.class, int.class)));
        assertFalse(classPolicy.isConstructorExposed(
                CAnnotationsTest2.class.getConstructor(int.class, int.class, int.class, int.class)));
    }

    private static WhitelistMemberAccessPolicy newWhitelistMemberAccessPolicy(String... memberSelectors) {
        try {
            return new WhitelistMemberAccessPolicy(
                    MemberSelectorListMemberAccessPolicy.MemberSelector.parse(
                            Arrays.asList(memberSelectors), false,
                            MemberSelectorListMemberAccessPolicyTest.class.getClassLoader()));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static BlacklistMemberAccessPolicy newBlacklistMemberAccessPolicy(String... memberSelectors) {
        try {
            return new BlacklistMemberAccessPolicy(
                    MemberSelectorListMemberAccessPolicy.MemberSelector.parse(
                            Arrays.asList(memberSelectors), false,
                            MemberSelectorListMemberAccessPolicyTest.class.getClassLoader()));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static class C1 {
        public int f1;
        public int f2;

        public C1() {
        }

        public C1(int x) {
        }

        public void m1() {
        }

        public void m2() {
        }

        public void m2(int x) {
        }

        public void m2(double x) {
        }

        public void m3() {
        }
    }

    public static class C2 extends C1 {
        public int f3;

        public C2(int x) {
            super(x);
        }

        @Override
        public void m2(int x) {
        }

        public void m2(boolean x) {
        }

        public void m4() {
        }
    }

    public static class C3 extends C2 {
        public C3() {
            super(0);
        }

        public C3(int x) {
            super(x);
        }
    }

    public static class D1 implements I1 {
        public int f1;
        public void m1() {
        }
    }

    public static class D2 extends D1 {
    }

    public static class E1 implements I1Sub {
        public void m1() {

        }

        public void m2() {
        }
    }

    public static class E2 extends E1 implements J1 {
    }

    public static class F1 implements J1 {
        public void m2() {
        }
    }

    interface I1 {
        int f1 = 1;
        void m1();
    }

    interface I1Sub extends Serializable, I1 {
        void m2();
    }

    interface J1 {
        void m2();
    }

    public class CArrayArgs {
        public void m1(String arg) {
        }

        public void m1(String[] arg) {
        }

        public void m1(String[][] arg) {
        }

        public void m2(C1[] arg) {
        }

        public void m2(C1[] arg1, C1[] arg2, C1 arg3) {
        }
    }

    public static class CAnnotationsTest1 {
        @TemplateAccessible
        public int f5;

        @TemplateAccessible
        public CAnnotationsTest1() {}

        @TemplateAccessible
        public void m5() {}
    }

    public interface IAnnotationTest {
        @TemplateAccessible
        int f6 = 0;

        @TemplateAccessible
        void m6();
    }

    public static class CAnnotationsTest2 extends CAnnotationsTest1 implements IAnnotationTest {
        public int f1;

        public int f2;

        @TemplateAccessible
        public int f3;

        @TemplateAccessible
        public int f4;

        public int f5;

        public int f6;

        public CAnnotationsTest2() {}

        public CAnnotationsTest2(int x) {}

        @TemplateAccessible
        public CAnnotationsTest2(int x, int y) {}

        @TemplateAccessible
        public CAnnotationsTest2(int x, int y, int z) {}

        public CAnnotationsTest2(int x, int y, int z, int a) {}

        public void m1() {}

        public void m2() {}

        @TemplateAccessible
        public void m3() {}

        @TemplateAccessible
        public void m4() {}

        public void m5() {}

        public void m6() {}
    }

}

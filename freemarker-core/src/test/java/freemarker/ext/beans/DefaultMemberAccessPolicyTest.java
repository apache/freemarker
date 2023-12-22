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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.junit.Test;

import freemarker.template.Configuration;

public class DefaultMemberAccessPolicyTest {

    private static final DefaultMemberAccessPolicy POLICY = DefaultMemberAccessPolicy.getInstance(Configuration.VERSION_2_3_30);

    @Test
    public void testWhitelistRuleWithNoMembers() throws NoSuchMethodException {
        ClassMemberAccessPolicy classPolicy = POLICY.forClass(ProtectionDomain.class);
        assertFalse(classPolicy.isMethodExposed(ProtectionDomain.class.getMethod("getClassLoader")));
    }

    @Test
    public void testWhitelistRuleWithSomeMembers() throws NoSuchMethodException {
        ClassMemberAccessPolicy classPolicy = POLICY.forClass(URL.class);
        assertFalse(classPolicy.isMethodExposed(URL.class.getMethod("openStream")));
        assertFalse(classPolicy.isMethodExposed(URL.class.getMethod("getContent", Class[].class)));
        assertTrue(classPolicy.isMethodExposed(URL.class.getMethod("getHost")));
        assertTrue(classPolicy.isMethodExposed(URL.class.getMethod("sameFile", URL.class)));
    }

    @Test
    public void testWhitelistRuleOnSubclass() throws NoSuchMethodException {
        ClassMemberAccessPolicy classPolicy = POLICY.forClass(CustomClassLoader.class);
        assertFalse(classPolicy.isMethodExposed(CustomClassLoader.class.getMethod("loadClass", String.class)));
        assertFalse(classPolicy.isMethodExposed(CustomClassLoader.class.getMethod("m1")));
    }

    @Test
    public void testBlacklistUnlistedRule() throws NoSuchMethodException {
        for (Class<?> testedClass : Arrays.asList(
                Object.class, Thread.class, ThreadSubclass.class, ProtectionDomain.class, CustomClassLoader.class,
                UserClass.class)) {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(testedClass);
            assertFalse(classPolicy.isMethodExposed(testedClass.getMethod("wait")));
            assertTrue(testedClass.getName(), classPolicy.isMethodExposed(testedClass.getMethod("toString")));
        }

        ClassMemberAccessPolicy classPolicy = POLICY.forClass(UserClass.class);
        assertTrue(classPolicy.isMethodExposed(UserClass.class.getMethod("foo")));
    }

    @Test
    public void testBlacklistUnlistedRuleOnSubclass() throws NoSuchMethodException {
        ClassMemberAccessPolicy classPolicy = POLICY.forClass(ThreadSubclass.class);
        assertFalse(classPolicy.isMethodExposed(ThreadSubclass.class.getMethod("run")));
        assertTrue(classPolicy.isMethodExposed(ThreadSubclass.class.getMethod("getName")));
        assertTrue(classPolicy.isMethodExposed(ThreadSubclass.class.getMethod("m1")));
    }

    @Test
    public void testToString() throws NoSuchMethodException {
        assertTrue(POLICY.isToStringAlwaysExposed());
        assertTrue(POLICY.forClass(UserClass.class).isMethodExposed(Object.class.getMethod("toString")));
    }

    @Test
    public void testWellKnownUnsafeMethodsAreBanned() throws NoSuchMethodException {
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(Class.class);
            assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("forName", String.class)));
            assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("newInstance")));
            assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("getClassLoader")));
            assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("getResourceAsStream", String.class)));
            assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("getResource", String.class)));
            assertTrue(classPolicy.isMethodExposed(Class.class.getMethod("getProtectionDomain"))); // Allowed
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(ProtectionDomain.class);
            assertFalse(classPolicy.isMethodExposed(ProtectionDomain.class.getMethod("getClassLoader")));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(ClassLoader.class);
            assertFalse(classPolicy.isMethodExposed(ClassLoader.class.getMethod("loadClass", String.class)));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(Method.class);
            assertFalse(classPolicy.isMethodExposed(Method.class.getMethod("invoke", Object.class, Object[].class)));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(Constructor.class);
            assertFalse(classPolicy.isMethodExposed(Constructor.class.getMethod("newInstance", Object[].class)));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(Field.class);
            assertFalse(classPolicy.isMethodExposed(Field.class.getMethod("get", Object.class)));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(Object.class);
            assertTrue(classPolicy.isMethodExposed(Field.class.getMethod("getClass"))); // Allowed by design
            assertFalse(classPolicy.isMethodExposed(Field.class.getMethod("wait")));
        }
        {
            ClassMemberAccessPolicy classPolicy = POLICY.forClass(URL.class);
            assertFalse(classPolicy.isMethodExposed(URL.class.getMethod("openConnection")));
        }
    }

    public static class ThreadSubclass extends Thread {
        @Override
        public void run() {
            super.run();
        }

        public void m1() {}
    }

    public static class UserClass {
        public void foo() {
        }
    }

    public static class CustomClassLoader extends ClassLoader {
        public void m1() {}
    }

}

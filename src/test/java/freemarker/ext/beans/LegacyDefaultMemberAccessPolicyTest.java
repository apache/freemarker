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

import org.junit.Test;

public class LegacyDefaultMemberAccessPolicyTest {

    @Test
    public void testBasic() throws NoSuchMethodException, NoSuchFieldException {
        ClassMemberAccessPolicy classPolicy = LegacyDefaultMemberAccessPolicy.INSTANCE.forClass(UserClass.class);
        assertFalse(classPolicy.isMethodExposed(UserClass.class.getMethod("wait")));
        assertTrue(classPolicy.isMethodExposed(UserClass.class.getMethod("foo")));
        assertTrue(classPolicy.isConstructorExposed(UserClass.class.getConstructor()));
        assertTrue(classPolicy.isFieldExposed(UserClass.class.getField("f1")));
    }

    @Test
    public void testThread() throws NoSuchMethodException, NoSuchFieldException {
        {
            ClassMemberAccessPolicy classPolicy = LegacyDefaultMemberAccessPolicy.INSTANCE.forClass(Thread.class);
            assertFalse(classPolicy.isMethodExposed(Thread.class.getMethod("run")));
        }
        {
            ClassMemberAccessPolicy classPolicy = LegacyDefaultMemberAccessPolicy.INSTANCE.forClass(ThreadSubclass.class);
            // Strange glitch that we reproduce for backward compatibility:
            assertTrue(classPolicy.isMethodExposed(ThreadSubclass.class.getMethod("run")));
        }
    }

    @Test
    public void testClass() throws NoSuchMethodException, NoSuchFieldException {
        ClassMemberAccessPolicy classPolicy = LegacyDefaultMemberAccessPolicy.INSTANCE.forClass(Class.class);
        assertFalse(classPolicy.isMethodExposed(Class.class.getMethod("getClassLoader")));
        assertTrue(classPolicy.isMethodExposed(Class.class.getMethod("getName")));
    }

    public static class ThreadSubclass extends Thread {
        @Override
        public void run() {
            super.run();
        }

        public void m1() {}
    }

    public static class UserClass {
        public String f1;

        public void foo() {
        }
    }

}

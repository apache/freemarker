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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;

public class MethodUtilTest {

    @Test
    public void testMethodBasic() throws NoSuchMethodException, NoSuchFieldException {
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getMethod("m1"), TemplateAccessible.class));
        assertNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getMethod("m2"), TemplateAccessible.class));

        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getConstructor(int.class), TemplateAccessible.class));
        assertNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getConstructor(int.class, int.class), TemplateAccessible.class));

        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getField("f1"), TemplateAccessible.class));
        assertNull(_MethodUtil.getInheritableAnnotation(
                C1.class, C1.class.getField("f3"), TemplateAccessible.class));
    }

    @Test
    public void testMethodInheritance() throws NoSuchMethodException, NoSuchFieldException {
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getMethod("m1"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getMethod("m2"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getMethod("m3"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getMethod("m4"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getMethod("m5"), TemplateAccessible.class));

        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getConstructor(int.class), TemplateAccessible.class));
        assertNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getConstructor(), TemplateAccessible.class));

        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getField("f1"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getField("f2"), TemplateAccessible.class));
        assertNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getField("f3"), TemplateAccessible.class));
        assertNotNull(_MethodUtil.getInheritableAnnotation(
                C2.class, C2.class.getField("f4"), TemplateAccessible.class));
    }

    @Test
    public void testMethodInheritanceWithSyntheticMethod() {
        for (Method method : D2.class.getMethods()) {
            if (method.getName().equals("m1")) {
                assertNotNull(_MethodUtil.getInheritableAnnotation(
                        C2.class, method, TemplateAccessible.class));
            }
        }
    }

    static public class C1 implements Serializable {
        @TemplateAccessible
        public int f1;

        @TemplateAccessible
        public int f2;

        public int f3;

        public int f4;

        @TemplateAccessible
        public C1(int x) {}

        public C1(int x, int y) {}

        @TemplateAccessible
        public void m1() {}

        public void m2() {}

        public void m3() {}

        @TemplateAccessible
        public void m4() {}

        @TemplateAccessible
        public void m5() {}
    }

    static public class C2 extends C1 implements I1 {
        public long f2;

        public C2() {
            super(0);
        }

        public C2(int x) {
            super(x);
        }

        @Override
        public void m1() {}

        @TemplateAccessible
        @Override
        public void m3() {}
    }

    public interface I1 {
        @TemplateAccessible
        int f4 = 0;

        @TemplateAccessible
        void m2();

        void m5();
    }

    public static class D1<T> {
        @TemplateAccessible
        public T m1() { return null; }
    }

    public static class D2 extends D1<String> {
        @Override
        public String m1() { return ""; }
    }

}
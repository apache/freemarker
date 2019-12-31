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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class DefaultObjectWrapperMemberAccessPolicyTest {

    @Test
    public void testMethodsWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper ow = createDefaultMemberAccessPolicyObjectWrapper();
        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());

        assertNotNull(objM.get("m1"));
        assertEquals("m2(true)", exec(ow, objM.get("m2"), true));
        assertEquals("staticM()", exec(ow, objM.get("staticM")));

        assertEquals("x", getHashValue(ow, objM, "x"));
        assertNotNull(objM.get("getX"));
        assertNotNull(objM.get("setX"));

        assertNull(objM.get("notPublic"));

        assertNull(objM.get("notify"));

        // Because it was overridden, we allow it historically.
        assertNotNull(objM.get("run"));

        assertEquals("safe wait(1)", exec(ow, objM.get("wait"), 1L));
        try {
            exec(ow, objM.get("wait")); // 0 arg overload is not visible, a it's "unsafe"
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("wait(int)"));
        }
    }

    @Test
    public void testFieldsWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper ow = createDefaultMemberAccessPolicyObjectWrapper();
        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());
        assertFieldsNotExposed(objM);
    }

    private void assertFieldsNotExposed(TemplateHashModel objM) throws TemplateException {
        assertNull(objM.get("publicField1"));
        assertNull(objM.get("publicField2"));
        assertNonPublicFieldsNotExposed(objM);
    }

    private void assertNonPublicFieldsNotExposed(TemplateHashModel objM) throws TemplateException {
        assertNull(objM.get("nonPublicField1"));
        assertNull(objM.get("nonPublicField2"));

        // Strangely, public static fields are banned historically, while static methods aren't.
        assertNull(objM.get("STATIC_FIELD"));
    }

    @Test
    public void testGenericGetWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper ow = createDefaultMemberAccessPolicyObjectWrapper();

        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new CWithGenericGet());

        assertEquals("get(x)", getHashValue(ow, objM, "x"));
    }

    @Test
    public void testConstructorsWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper ow = createDefaultMemberAccessPolicyObjectWrapper();
        assertNonPublicConstructorNotExposed(ow);

        assertEquals(CWithConstructor.class,
                ow.newInstance(CWithConstructor.class, new TemplateModel[0], null)
                        .getClass());

        assertEquals(CWithOverloadedConstructor.class,
                ow.newInstance(CWithOverloadedConstructor.class, new TemplateModel[0], null)
                        .getClass());

        assertEquals(CWithOverloadedConstructor.class,
                ow.newInstance(CWithOverloadedConstructor.class, new TemplateModel[] {new SimpleNumber(1)}, null)
                        .getClass());
    }

    private void assertNonPublicConstructorNotExposed(DefaultObjectWrapper ow) {
        try {
            ow.newInstance(C.class, new TemplateModel[0], null);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("constructor"));
        }
    }

    @Test
    public void testExposeAllWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setExposureLevel(DefaultObjectWrapper.EXPOSE_ALL);
        DefaultObjectWrapper ow = owb.build();
        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());
        // Because the MemberAccessPolicy is ignored:
        assertNotNull(objM.get("notify"));
        assertFieldsNotExposed(objM);
    }

    @Test
    public void testExposeFieldsWithDefaultMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setExposeFields(true);
        DefaultObjectWrapper ow = owb.build();
        {
            TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());
            assertNull(objM.get("notify"));
            assertEquals(1, getHashValue(ow, objM, "publicField1"));
            assertEquals(2, getHashValue(ow, objM, "publicField2"));
            assertNonPublicFieldsNotExposed(objM);
        }

        {
            TemplateHashModel objM = (TemplateHashModel) ow.wrap(new CExtended());
            assertNull(objM.get("notify"));
            assertEquals(1, getHashValue(ow, objM, "publicField1"));
            assertEquals(2, getHashValue(ow, objM, "publicField2"));
            assertEquals(3, getHashValue(ow, objM, "publicField3"));
            assertNonPublicFieldsNotExposed(objM);
        }
    }

    @Test
    public void testMethodsWithCustomMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setMemberAccessPolicy(new MemberAccessPolicy() {
            public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                return new ClassMemberAccessPolicy() {
                    public boolean isMethodExposed(Method method) {
                        String name = method.getName();
                        Class<?>[] paramTypes = method.getParameterTypes();
                        return name.equals("m3")
                                || (name.equals("m2")
                                && (paramTypes.length == 0 || paramTypes[0].equals(boolean.class)));
                    }

                    public boolean isConstructorExposed(Constructor<?> constructor) {
                        return true;
                    }

                    public boolean isFieldExposed(Field field) {
                        return true;
                    }
                };
            }
        });
        DefaultObjectWrapper ow = owb.build();

        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());
        assertNull(objM.get("m1"));
        assertEquals("m3()", exec(ow, objM.get("m3")));
        assertEquals("m2()", exec(ow, objM.get("m2")));
        assertEquals("m2(true)", exec(ow, objM.get("m2"), true));
        try {
            exec(ow, objM.get("m2"), 1);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("overload"));
        }

        assertNull(objM.get("notify"));
    }

    @Test
    public void testFieldsWithCustomMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setExposeFields(true);
        owb.setMemberAccessPolicy(new MemberAccessPolicy() {
            public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                return new ClassMemberAccessPolicy() {
                    public boolean isMethodExposed(Method method) {
                        return true;
                    }

                    public boolean isConstructorExposed(Constructor<?> constructor) {
                        return true;
                    }

                    public boolean isFieldExposed(Field field) {
                        return field.getName().equals("publicField1")
                                || field.getName().equals("nonPublicField1");
                    }
                };
            }
        });
        DefaultObjectWrapper ow = owb.build();

        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new C());

        assertNonPublicFieldsNotExposed(objM);
        assertEquals(1, getHashValue(ow, objM, "publicField1"));
        assertNull(getHashValue(ow, objM, "publicField2"));
    }

    @Test
    public void testGenericGetWithCustomMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setMemberAccessPolicy(new MemberAccessPolicy() {
            public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                return new ClassMemberAccessPolicy() {
                    public boolean isMethodExposed(Method method) {
                        return false;
                    }

                    public boolean isConstructorExposed(Constructor<?> constructor) {
                        return true;
                    }

                    public boolean isFieldExposed(Field field) {
                        return true;
                    }
                };
            }
        });
        DefaultObjectWrapper ow = owb.build();

        TemplateHashModel objM = (TemplateHashModel) ow.wrap(new CWithGenericGet());
        assertNull(getHashValue(ow, objM, "x"));
    }

    @Test
    public void testConstructorsWithCustomMemberAccessPolicy() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        owb.setMemberAccessPolicy(new MemberAccessPolicy() {
            public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                return new ClassMemberAccessPolicy() {
                    public boolean isMethodExposed(Method method) {
                        return true;
                    }

                    public boolean isConstructorExposed(Constructor<?> constructor) {
                        return constructor.getDeclaringClass() == CWithOverloadedConstructor.class
                                && constructor.getParameterTypes().length == 1;
                    }

                    public boolean isFieldExposed(Field field) {
                        return true;
                    }
                };
            }
        });
        DefaultObjectWrapper ow = owb.build();

        assertNonPublicConstructorNotExposed(ow);

        try {
            assertEquals(CWithConstructor.class,
                    ow.newInstance(CWithConstructor.class, new TemplateModel[0], null).getClass());
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("constructor"));
        }

        try {
            ow.newInstance(CWithOverloadedConstructor.class, new TemplateModel[0], null);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("constructor"));
        }

        assertEquals(CWithOverloadedConstructor.class,
                ow.newInstance(CWithOverloadedConstructor.class,
                        new TemplateModel[] {new SimpleNumber(1)}, null).getClass());
    }

    @Test
    public void testMemberAccessPolicyAndApiBI() throws IOException, TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .memberAccessPolicy(new MemberAccessPolicy() {
                    @Override
                    public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                        return new ClassMemberAccessPolicy() {
                            public boolean isMethodExposed(Method method) {
                                return method.getName().equals("size");
                            }

                            public boolean isConstructorExposed(Constructor<?> constructor) {
                                return true;
                            }

                            public boolean isFieldExposed(Field field) {
                                return true;
                            }
                        };
                    }
                })
                .build();

        Map<String, Object> dataModel = ImmutableMap.<String, Object>of("m", ImmutableMap.of("k", "v"));

        String templateSource = "size=${m?api.size()} get=${(m?api.get('k'))!'hidden'}";

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .objectWrapper(ow)
                    .apiBuiltinEnabled(true)
                    .build();
            Template template = new Template(null, templateSource, cfg);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            assertEquals("size=1 get=hidden", out.toString());
        }

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .objectWrapper(new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build())
                    .apiBuiltinEnabled(true)
                    .build();
            Template template = new Template(null, templateSource, cfg);
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            assertEquals("size=1 get=v", out.toString());
        }
    }

    @Test
    public void testMemberAccessPolicyAndNewBI() throws IOException, TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .memberAccessPolicy(new MemberAccessPolicy() {
                        public ClassMemberAccessPolicy forClass(Class<?> contextClass) {
                            return new ClassMemberAccessPolicy() {
                                public boolean isMethodExposed(Method method) {
                                    return true;
                                }

                                public boolean isConstructorExposed(Constructor<?> constructor) {
                                    return constructor.getDeclaringClass().equals(CustomModel.class);
                                }

                                public boolean isFieldExposed(Field field) {
                                    return true;
                                }
                            };
                        }
                    })
                .build();

        String templateSource = "${'" + CustomModel.class.getName() + "'?new()} "
                + "<#attempt>${'" + OtherCustomModel.class.getName() + "'?new()}<#recover>failed</#attempt>";
        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .objectWrapper(ow)
                    .apiBuiltinEnabled(true)
                    .build();
            Template template = new Template(null, templateSource, cfg);
            StringWriter out = new StringWriter();
            template.process(null, out);
            assertEquals("1 failed", out.toString());
        }

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
            Template template = new Template(null, templateSource, cfg);
            StringWriter out = new StringWriter();
            template.process(null, out);
            assertEquals("1 2", out.toString());
        }
    }

    private static DefaultObjectWrapper createDefaultMemberAccessPolicyObjectWrapper() {
        return new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
    }

    private static Object getHashValue(ObjectWrapperAndUnwrapper ow, TemplateHashModel objM, String key)
            throws TemplateException {
        return ow.unwrap(objM.get(key));
    }

    private static Object exec(ObjectWrapperAndUnwrapper ow, TemplateModel objM, Object... args) throws TemplateException {
        assertThat(objM, instanceOf(TemplateFunctionModel.class));
        TemplateModel[] argModels = new TemplateModel[args.length];
        for (int i = 0; i < args.length; i++) {
            argModels[i] = ow.wrap(args[i]);
        }
        Object returnValue = ((TemplateFunctionModel) objM).execute(argModels, null, null);
        return unwrap(ow, returnValue);
    }

    private static Object unwrap(ObjectWrapperAndUnwrapper ow, Object returnValue) throws TemplateException {
        return returnValue instanceof TemplateModel ? ow.unwrap((TemplateModel) returnValue) : returnValue;
    }

    public static class C extends Thread {
        public static final int STATIC_FIELD = 1;
        public int publicField1 = 1;
        public int publicField2 = 2;
        protected int nonPublicField1 = 1;
        private int nonPublicField2 = 2;

        // Non-public
        C() {

        }

        void notPublic() {
        }

        public void m1() {
        }

        public String m2() {
            return "m2()";
        }

        public String m2(int otherOverload) {
            return "m2(" + otherOverload + ")";
        }

        public String m2(boolean otherOverload) {
            return "m2(" + otherOverload + ")";
        }

        public String m3() {
            return "m3()";
        }

        public static String staticM() {
            return "staticM()";
        }

        public String getX() {
            return "x";
        }

        public void setX(String x) {
        }

        public String wait(int otherOverload) {
            return "safe wait(" + otherOverload + ")";
        }

        @Override
        public void run() {
            return;
        }
    }

    public static class CExtended extends C {
        public int publicField3 = 3;
    }

    public static class CWithGenericGet extends Thread {
        public String get(String key) {
            return "get(" + key + ")";
        }
    }

    public static class CWithConstructor implements TemplateModel {
        public CWithConstructor() {
        }
    }

    public static class CWithOverloadedConstructor implements TemplateModel {
        public CWithOverloadedConstructor() {
        }

        public CWithOverloadedConstructor(int x) {
        }
    }

    public static class CustomModel implements TemplateNumberModel {
        public Number getAsNumber() {
            return 1;
        }
    }

    public static class OtherCustomModel implements TemplateNumberModel {
        public Number getAsNumber() {
            return 2;
        }
    }
}

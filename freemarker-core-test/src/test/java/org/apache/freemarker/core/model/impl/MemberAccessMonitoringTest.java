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

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class MemberAccessMonitoringTest extends TemplateTest {

    private MonitoredDefaultObjectWrapper ow = new MonitoredDefaultObjectWrapper();

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.setObjectWrapper(ow);
    }

    @Test
    public void test() throws TemplateException, IOException {
        addToDataModel("C1", ow.getStaticModels().get(C1.class.getName()));
        addToDataModel("c2", new C2());

        assertOutput(
                "${C1.m1()} ${C1.F1} ${C1.F2} ${c2.m1()} ${c2.f1} ${c2.f2} ${c2['abc']}",
                "1 11 111 2 22 222 3");
        assertEquals(
                ImmutableSet.of("C1.m1()", "C1.F1", "C1.F2", "C2.m1()", "C2.f1", "C2.f2", "C2.get()"),
                ow.getAccessedMembers());
    }

    public static class C1 {
        public static final int F1 = 11;
        public static int F2 = 111;

        public static int m1() {
            return 1;
        }

        public static int get(String k) {
            return k.length();
        }
    }

    public static class C2 {
        public final int f1 = 22;
        public int f2 = 222;

        public int m1() {
            return 2;
        }

        public int get(String k) {
            return k.length();
        }
    }

    public static class MonitoredDefaultObjectWrapper extends DefaultObjectWrapper {
        private final Set<String> accessedMembers;

        public MonitoredDefaultObjectWrapper() {
            super(new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).exposeFields(true));
            this.accessedMembers = Collections.synchronizedSet(new HashSet<>());
        }

        @Override
        protected TemplateModel invokeMethod(Object object, Method method, Object[] args) throws
                InvocationTargetException, IllegalAccessException, TemplateException {
            accessedMembers.add(method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()");
            return super.invokeMethod(object, method, args);
        }

        @Override
        protected TemplateModel readField(Object object, Field field) throws IllegalAccessException,
                TemplateException {
            accessedMembers.add(field.getDeclaringClass().getSimpleName() + "." + field.getName());
            return super.readField(object, field);
        }

        public Set<String> getAccessedMembers() {
            return accessedMembers;
        }
    }

}

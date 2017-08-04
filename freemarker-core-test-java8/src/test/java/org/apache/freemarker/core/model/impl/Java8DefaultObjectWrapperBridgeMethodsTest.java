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

import java.util.Collections;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateMethodModel;
import org.junit.Test;

public class Java8DefaultObjectWrapperBridgeMethodsTest {
    
    @Test
    public void testWithoutDefaultMethod() throws TemplateException {
        test(BridgeMethodsBean.class);
    }

    @Test
    public void testWithDefaultMethod() throws TemplateException {
        test(Java8BridgeMethodsWithDefaultMethodBean.class);
    }

    @Test
    public void testWithDefaultMethod2() throws TemplateException {
        test(Java8BridgeMethodsWithDefaultMethodBean2.class);
    }

    private void test(Class<?> pClass) throws TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateHashModel wrapped;
        try {
            wrapped = (TemplateHashModel) ow.wrap(pClass.newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        TemplateMethodModel m1 = (TemplateMethodModel) wrapped.get("m1");
        assertEquals(BridgeMethodsBean.M1_RETURN_VALUE, "" + m1.execute(Collections.emptyList()));
        
        TemplateMethodModel m2 = (TemplateMethodModel) wrapped.get("m2");
        assertNull(m2.execute(Collections.emptyList()));
    }
    
}

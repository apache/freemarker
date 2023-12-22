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

import java.util.Collections;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Java8BeansWrapperBridgeMethodsTest {
    
    @Test
    public void testWithoutDefaultMethod() throws TemplateModelException {
        test(BridgeMethodsBean.class);
    }

    @Test
    public void testWithDefaultMethod() throws TemplateModelException {
        test(Java8BridgeMethodsWithDefaultMethodBean.class);
    }

    @Test
    public void testWithDefaultMethod2() throws TemplateModelException {
        test(Java8BridgeMethodsWithDefaultMethodBean2.class);
    }

    private void test(Class<?> pClass) throws TemplateModelException {
        BeansWrapper ow = new BeansWrapperBuilder(Configuration.VERSION_2_3_26).build();
        TemplateHashModel wrapped;
        try {
            wrapped = (TemplateHashModel) ow.wrap(pClass.newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        
        TemplateMethodModelEx m1 = (TemplateMethodModelEx) wrapped.get("m1");
        assertEquals(BridgeMethodsBean.M1_RETURN_VALUE, "" + m1.exec(Collections.emptyList()));
        
        TemplateMethodModelEx m2 = (TemplateMethodModelEx) wrapped.get("m2");
        assertNull(m2.exec(Collections.emptyList()));
    }
    
}

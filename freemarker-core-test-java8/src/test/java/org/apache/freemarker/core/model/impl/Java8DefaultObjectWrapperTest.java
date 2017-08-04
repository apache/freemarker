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
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.junit.Test;

public class Java8DefaultObjectWrapperTest {

    @Test
    public void testDefaultMethodRecognized() throws TemplateException {
        DefaultObjectWrapper.Builder owb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        DefaultObjectWrapper ow = owb.build();
        TemplateHashModel wrappedBean = (TemplateHashModel) ow.wrap(new Java8DefaultMethodsBean());
        
        {
            TemplateScalarModel prop = (TemplateScalarModel) wrappedBean.get(Java8DefaultMethodsBean.NORMAL_PROP);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.NORMAL_PROP_VALUE, prop.getAsString());
        }
        {
            // This is overridden in the subclass, so it's visible even without default method support: 
            TemplateScalarModel prop = (TemplateScalarModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_PROP_2);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.PROP_2_OVERRIDE_VALUE, prop.getAsString());
        }
        {
            TemplateScalarModel prop = (TemplateScalarModel) wrappedBean.get(
                    Java8DefaultMethodsBeanBase.DEFAULT_METHOD_PROP);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBeanBase.DEFAULT_METHOD_PROP_VALUE, prop.getAsString());
        }
        {
            // Has only indexed read method, so it's not exposed as a property
            assertNull(wrappedBean.get(Java8DefaultMethodsBeanBase.DEFAULT_METHOD_INDEXED_PROP));

            TemplateMethodModel indexedReadMethod = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBeanBase.DEFAULT_METHOD_INDEXED_PROP_GETTER);
            assertNotNull(indexedReadMethod);
            assertEquals(Java8DefaultMethodsBeanBase.DEFAULT_METHOD_INDEXED_PROP_VALUE,
                    ((TemplateScalarModel) indexedReadMethod.execute(Collections.singletonList(new SimpleNumber(0))))
                            .getAsString
                            ());
        }
        {
            // We see default method indexed read method, but it's invalidated by normal getter in the subclass
            TemplateNumberModel prop = (TemplateNumberModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_NOT_AN_INDEXED_PROP);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.NOT_AN_INDEXED_PROP_VALUE, prop.getAsNumber());
        }
        {
            // The default method read method invalidates the indexed read method in the subclass
            TemplateScalarModel prop = (TemplateScalarModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_NOT_AN_INDEXED_PROP_2);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.DEFAULT_METHOD_NOT_AN_INDEXED_PROP_2_VALUE, prop.getAsString());
        }
        {
            // The default method read method invalidates the indexed read method in the subclass
            TemplateSequenceModel prop = (TemplateSequenceModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_NOT_AN_INDEXED_PROP_3);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.DEFAULT_METHOD_NOT_AN_INDEXED_PROP_3_VALUE_0,
                    ((TemplateScalarModel) prop.get(0)).getAsString());
        }
        {
            // We see the default method indexed reader, which overrides the plain array reader in the subclass.
            TemplateSequenceModel prop = (TemplateSequenceModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_INDEXED_PROP_2);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBean.ARRAY_PROP_2_VALUE_0,
                    ((TemplateScalarModel) prop.get(0)).getAsString());
        }
        {
            // We do see the default method non-indexed reader, but the subclass has a matching indexed reader, so that
            // takes over.
            TemplateSequenceModel prop = (TemplateSequenceModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_INDEXED_PROP_3);
            assertNotNull(prop);
            assertEquals(Java8DefaultMethodsBeanBase.DEFAULT_METHOD_INDEXED_PROP_3_VALUE_0,
                    ((TemplateScalarModel) prop.get(0)).getAsString());
        }        
        {
            // Only present in the subclass.

            // Has only indexed read method, so it's not exposed as a property
            assertNull(wrappedBean.get(Java8DefaultMethodsBean.INDEXED_PROP_4));

            TemplateMethodModel indexedReadMethod = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBean.INDEXED_PROP_GETTER_4);
            assertNotNull(indexedReadMethod);
            assertEquals(Java8DefaultMethodsBean.INDEXED_PROP_4_VALUE,
                    ((TemplateScalarModel) indexedReadMethod.execute(Collections.singletonList(new SimpleNumber(0))))
                            .getAsString());
        }        
        {
            TemplateMethodModel action = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBean.NORMAL_ACTION);
            assertNotNull(action);
            assertEquals(
                    Java8DefaultMethodsBean.NORMAL_ACTION_RETURN_VALUE,
                    ((TemplateScalarModel) action.execute(Collections.emptyList())).getAsString());
        }
        
        {
            TemplateMethodModel action = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBean.NORMAL_ACTION);
            assertNotNull(action);
            assertEquals(
                    Java8DefaultMethodsBean.NORMAL_ACTION_RETURN_VALUE,
                    ((TemplateScalarModel) action.execute(Collections.emptyList())).getAsString());
        }
        {
            TemplateMethodModel action = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_ACTION);
            assertNotNull(action);
            assertEquals(
                    Java8DefaultMethodsBean.DEFAULT_METHOD_ACTION_RETURN_VALUE,
                    ((TemplateScalarModel) action.execute(Collections.emptyList())).getAsString());
        }
        {
            TemplateMethodModel action = (TemplateMethodModel) wrappedBean.get(
                    Java8DefaultMethodsBean.OVERRIDDEN_DEFAULT_METHOD_ACTION);
            assertNotNull(action);
            assertEquals(
                    Java8DefaultMethodsBean.OVERRIDDEN_DEFAULT_METHOD_ACTION_RETURN_VALUE,
                    ((TemplateScalarModel) action.execute(Collections.emptyList())).getAsString());
        }
    }

}

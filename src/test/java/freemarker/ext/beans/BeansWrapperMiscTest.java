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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.Configuration;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Constants;

@RunWith(JUnit4.class)
public class BeansWrapperMiscTest {

    @Test
    public void booleansTest() throws Exception {
        final BeansWrapper bw = new BeansWrapper();

        assertTrue(((TemplateBooleanModel) bw.wrap(Boolean.TRUE)).getAsBoolean());
        assertFalse(((TemplateBooleanModel) bw.wrap(Boolean.FALSE)).getAsBoolean());
        
        TemplateHashModel tm = (TemplateHashModel) bw.wrap(Boolean.TRUE);
        assertNotNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
        assertNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_NOTHING);
        assertNull(tm.get("hashCode"));
        assertNull(tm.get("class"));
        bw.setExposureLevel(BeansWrapper.EXPOSE_ALL);
        assertNotNull(tm.get("hashCode"));
        assertNotNull(tm.get("class"));
        
        assertSame(tm, bw.wrap(Boolean.TRUE));
    }
    
    @Test
    public void java8IndexedMethodIssueTest() throws TemplateModelException {
        {
            BeansWrapper bw = new BeansWrapper(Configuration.VERSION_2_3_26);
            TemplateHashModel beanTM = (TemplateHashModel) bw.wrap(new BeanWithBothIndexedAndArrayProperty());
            TemplateModel fooTM = beanTM.get("foo");
            assertThat(fooTM, instanceOf(TemplateMethodModelEx.class));
            assertThat(fooTM, instanceOf(TemplateSequenceModel.class));
            assertEquals("b",
                    ((TemplateScalarModel) ((TemplateMethodModelEx) fooTM).exec(
                            Collections.singletonList(Constants.ONE)))
                    .getAsString());
            try {
                ((TemplateSequenceModel) fooTM).size();
                fail();
            } catch (TemplateModelException e) {
                // Expected
            }
        }
        
        {
            BeansWrapper bw = new BeansWrapper(Configuration.VERSION_2_3_27);
            TemplateHashModel beanTM = (TemplateHashModel) bw.wrap(new BeanWithBothIndexedAndArrayProperty());
            TemplateModel fooTM = beanTM.get("foo");
            assertThat(fooTM, not(instanceOf(TemplateMethodModelEx.class)));
            assertThat(fooTM, instanceOf(TemplateSequenceModel.class));
            assertEquals("b",
                    ((TemplateScalarModel) ((TemplateSequenceModel) fooTM).get(1)).getAsString());
            assertEquals(2, ((TemplateSequenceModel) fooTM).size());
        }
    }
    
    public static class BeanWithBothIndexedAndArrayProperty {
        
        private final static String[] FOO = new String[] { "a", "b" };
        
        public String[] getFoo() {
            return FOO;
        }
        
        public String getFoo(int index) {
            return FOO[index];
        }
        
    }
    
}

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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FineTuneMethodAppearanceTest {

    @Test
    public void newWayOfConfiguring() throws TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .methodAppearanceFineTuner(GetlessMethodsAsPropertyGettersRule.INSTANCE)
                .exposeFields(true)
                .build();
        checkIfProperlyWrapped(ow.wrap(new C()));
    }
    
    private void checkIfProperlyWrapped(TemplateModel tm) throws TemplateException {
        TemplateHashModel thm = (TemplateHashModel) tm;
        assertEquals("v1", ((TemplateStringModel) thm.get("v1")).getAsString());
        assertEquals("v2()", ((TemplateStringModel) thm.get("v2")).getAsString());
        assertEquals("getV3()", ((TemplateStringModel) thm.get("v3")).getAsString());
        assertTrue(thm.get("getV3") instanceof JavaMethodModel);
    }
    
    @Test
    public void existingPropertyReplacement() throws TemplateException {
        for (Boolean replaceExistingProperty : new Boolean[] { null, false }) {
            // The "real" property wins, no mater what:
            assertSSubvariableValue(replaceExistingProperty, true, "from getS()");
            assertSSubvariableValue(replaceExistingProperty, false, "from getS()");
        }
        
        // replaceExistingProperty = true; the "real" property can be overridden:
        assertSSubvariableValue(true, true, "from getS()");
        assertSSubvariableValue(true, false, "from s()");
    }

    private void assertSSubvariableValue(Boolean replaceExistingProperty, boolean preferGetS, String expectedValue)
            throws TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .methodAppearanceFineTuner(
                        new PropertyReplacementMethodAppearanceFineTuner(replaceExistingProperty, preferGetS))
                .build();
        assertEquals(expectedValue,
                ((TemplateStringModel) ((TemplateHashModel) ow.wrap(new PropertyReplacementTestBean())).get("s"))
                .getAsString());
    }    
    
    static public class C {
        
        public String v1 = "v1";

        public String v2 = "v2";
        public String v2() { return "v2()"; }
        
        public String v3() { return "v3()"; }
        public String getV3() { return "getV3()"; }
    }
    
    static public class PropertyReplacementTestBean {
        
        public String getS() {
            return "from getS()";
        }
        
        public String s() {
            return "from s()";
        }
    }
    
    static class PropertyReplacementMethodAppearanceFineTuner implements MethodAppearanceFineTuner {
        private final Boolean replaceExistingProperty; 
        private final boolean preferGetS;
        
        PropertyReplacementMethodAppearanceFineTuner(Boolean replaceExistingProperty, boolean preferGetS) {
            this.replaceExistingProperty = replaceExistingProperty;
            this.preferGetS = preferGetS;
        }

        @Override
        public void process(MethodAppearanceFineTuner.DecisionInput in, MethodAppearanceFineTuner.Decision out) {
            if (replaceExistingProperty != null) {
                out.setReplaceExistingProperty(replaceExistingProperty);
            }
            if (preferGetS) {
                if (in.getMethod().getName().equals("getS")) {
                    try {
                        out.setExposeAsProperty(new PropertyDescriptor("s", in.getMethod(), null));
                    } catch (IntrospectionException e) {
                        throw new IllegalStateException(e);
                    }
                }
            } else {
                if (in.getMethod().getName().equals("s")) {
                    try {
                        out.setExposeAsProperty(new PropertyDescriptor("s", in.getMethod(), null));
                        out.setMethodShadowsProperty(false);
                    } catch (IntrospectionException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        
    }
        
}

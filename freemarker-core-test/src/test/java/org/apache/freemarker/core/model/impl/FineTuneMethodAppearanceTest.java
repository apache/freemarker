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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
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
        assertEquals("v1", ((TemplateScalarModel) thm.get("v1")).getAsString());
        assertEquals("v2()", ((TemplateScalarModel) thm.get("v2")).getAsString());
        assertEquals("getV3()", ((TemplateScalarModel) thm.get("v3")).getAsString());
        assertTrue(thm.get("getV3") instanceof JavaMethodModel);
    }
    
    static public class C {
        
        public String v1 = "v1";

        public String v2 = "v2";
        public String v2() { return "v2()"; }
        
        public String v3() { return "v3()"; }
        public String getV3() { return "getV3()"; }
    }
    
}

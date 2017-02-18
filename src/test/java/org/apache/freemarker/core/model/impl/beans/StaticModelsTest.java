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

package org.apache.freemarker.core.model.impl.beans;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateMethodModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StaticModelsTest {

    @Test
    public void modelCaching() throws Exception {
        BeansWrapper bw = new BeansWrapper(Configuration.VERSION_3_0_0);
        TemplateHashModel statics = bw.getStaticModels();
        TemplateHashModel s = (TemplateHashModel) statics.get(S.class.getName());
        assertNotNull(s);
        assertNotNull(s.get("F"));
        assertNotNull(s.get("m"));
        try {
            s.get("x");
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsString("No such key"));
        }
        
        try {
            statics.get("no.such.ClassExists");
            fail();
        } catch (TemplateModelException e) {
            assertTrue(e.getCause() instanceof ClassNotFoundException);
        }
        
        TemplateModel f = s.get("F");
        assertTrue(f instanceof TemplateScalarModel);
        assertEquals(((TemplateScalarModel) f).getAsString(), "F OK");
        
        TemplateModel m = s.get("m");
        assertTrue(m instanceof TemplateMethodModelEx);
        assertEquals(((TemplateScalarModel) ((TemplateMethodModelEx) m).exec(new ArrayList())).getAsString(), "m OK");
        
        assertSame(s, statics.get(S.class.getName()));
        
        bw.clearClassIntrospecitonCache();
        TemplateHashModel sAfterClean = (TemplateHashModel) statics.get(S.class.getName());
        assertNotSame(s, sAfterClean);
        assertSame(sAfterClean, statics.get(S.class.getName()));
        assertNotNull(sAfterClean.get("F"));
        assertNotNull(sAfterClean.get("m"));
    }
    
    public static class S {
        
        public static final String F = "F OK"; 
        
        public static String m() {
            return "m OK";
        }
        
    }
    
}

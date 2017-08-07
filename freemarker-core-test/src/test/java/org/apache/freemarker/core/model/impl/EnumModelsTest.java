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
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EnumModelsTest {
    
    @Test
    public void modelCaching() throws Exception {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).usePrivateCaches(true)
                .build();
        TemplateHashModel enums = ow.getEnumModels();
        TemplateHashModel e = (TemplateHashModel) enums.get(E.class.getName());
        assertNotNull(e);
        assertNotNull(e.get("A"));
        assertNotNull(e.get("B"));
        assertNull(e.get("C"));

        try {
            enums.get("no.such.ClassExists");
            fail();
        } catch (TemplateModelException ex) {
            assertTrue(ex.getCause() instanceof ClassNotFoundException);
        }
        
        TemplateModel a = e.get("A");
        assertTrue(a instanceof TemplateScalarModel);
        assertTrue(a instanceof TemplateHashModel);
        assertEquals(((TemplateScalarModel) a).getAsString(), "ts:A");
        JavaMethodModel nameMethod = (JavaMethodModel) ((TemplateHashModel) a).get("name");
        assertEquals(((TemplateScalarModel) nameMethod.execute(
                Constants.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE)).getAsString(),
                "A");
        
        assertSame(e, enums.get(E.class.getName()));
        
        ow.clearClassIntrospecitonCache();
        TemplateHashModel eAfterClean = (TemplateHashModel) enums.get(E.class.getName());
        assertNotSame(e, eAfterClean);
        assertSame(eAfterClean, enums.get(E.class.getName()));
        assertNotNull(eAfterClean.get("A"));
        assertNotNull(eAfterClean.get("B"));
        assertNull(eAfterClean.get("C"));
    }
    
    public enum E {
        A, B;

        @Override
        public String toString() {
            return "ts:" + super.toString();
        }
        
    }

}

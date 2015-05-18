/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.beans;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

@RunWith(JUnit4.class)
public class EnumModelsTest {
    
    @Test
    public void modelCaching() throws Exception {
        BeansWrapper bw = new BeansWrapper(Configuration.VERSION_2_3_21);
        TemplateHashModel enums = bw.getEnumModels();
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
        TemplateMethodModelEx nameMethod = (TemplateMethodModelEx) ((TemplateHashModel) a).get("name");
        assertEquals(((TemplateScalarModel) nameMethod.exec(new ArrayList())).getAsString(), "A");
        
        assertSame(e, enums.get(E.class.getName()));
        
        bw.clearClassIntrospecitonCache();
        TemplateHashModel eAfterClean = (TemplateHashModel) enums.get(E.class.getName());
        assertNotSame(e, eAfterClean);
        assertSame(eAfterClean, enums.get(E.class.getName()));
        assertNotNull(eAfterClean.get("A"));
        assertNotNull(eAfterClean.get("B"));
        assertNull(eAfterClean.get("C"));
    }
    
    public static enum E {
        A, B;

        @Override
        public String toString() {
            return "ts:" + super.toString();
        }
        
    }

}

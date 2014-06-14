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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModel;

@RunWith(JUnit4.class)
public class BeansWrapperMiscTest {

    @Test
    public void booleans() throws Exception {
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
    
}

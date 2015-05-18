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

package freemarker.template;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.junit.Test;

import freemarker.template.DefaultObjectWrapperTest.TestBean;

public class SimpleObjectWrapperTest {
    
    @Test
    public void testBasics() throws TemplateModelException {
        {
            SimpleObjectWrapper ow = new SimpleObjectWrapper(Configuration.VERSION_2_3_22);
            testCustomizationCommonPart(ow);
            assertTrue(ow.wrap(Collections.emptyMap()) instanceof DefaultMapAdapter);
            assertTrue(ow.wrap(Collections.emptyList()) instanceof DefaultListAdapter);
            assertTrue(ow.wrap(new boolean[] { }) instanceof DefaultArrayAdapter);
            assertTrue(ow.wrap(new HashSet()) instanceof SimpleSequence);  // at least until IcI 2.4
        }
        
        {
            SimpleObjectWrapper ow = new SimpleObjectWrapper(Configuration.VERSION_2_3_21);
            testCustomizationCommonPart(ow);
            assertTrue(ow.wrap(Collections.emptyMap()) instanceof SimpleHash);
            assertTrue(ow.wrap(Collections.emptyList()) instanceof SimpleSequence);
            assertTrue(ow.wrap(new boolean[] { }) instanceof SimpleSequence);
            assertTrue(ow.wrap(new HashSet()) instanceof SimpleSequence);
        }
    }

    @SuppressWarnings("boxing")
    private void testCustomizationCommonPart(SimpleObjectWrapper ow) throws TemplateModelException {
        assertFalse(ow.isWriteProtected());
        
        assertTrue(ow.wrap("x") instanceof SimpleScalar);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        try {
            ow.wrap(new TestBean());
            fail();
        } catch (TemplateModelException e) {
            assertTrue(e.getMessage().contains("type"));
        }
    }
    
}

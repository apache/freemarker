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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class ErrorMessagesTest {

    @Test
    public void getterMessage() throws TemplateModelException {
        BeansWrapper bw = new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build();
        TemplateHashModel thm = (TemplateHashModel) bw.wrap(new TestBean());
        try {
            thm.get("foo");
        } catch (TemplateModelException e) {
            e.printStackTrace();
            final String msg = e.getMessage();
            assertThat(msg, containsString("\"foo\""));
            assertThat(msg, containsString("existing sub-variable"));
        }
        assertNull(thm.get("bar"));
    }
    
    public class TestBean {
        
        public String getFoo() {
            throw new RuntimeException("Dummy");
        }
        
    }
    
}

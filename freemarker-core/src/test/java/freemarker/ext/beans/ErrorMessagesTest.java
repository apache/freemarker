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
import java.util.Date;

import org.junit.Test;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateHTMLOutputModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;

public class ErrorMessagesTest {

    @Test
    public void getterMessage() throws TemplateModelException {
        BeansWrapper bw = new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build();
        TemplateHashModel thm= (TemplateHashModel) bw.wrap(new TestBean());
        
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
    
    @Test
    public void markupOutputParameter() throws Exception {
        TemplateHTMLOutputModel html = HTMLOutputFormat.INSTANCE.fromMarkup("<p>a");

        for (Version ici : new Version[] { Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_24 }) {
            BeansWrapper bw = new BeansWrapperBuilder(ici).build();
            TemplateHashModel thm = (TemplateHashModel) bw.wrap(new TestBean());
            
            {
                TemplateMethodModelEx m = (TemplateMethodModelEx) thm.get("m1");
                try {
                    m.exec(Collections.singletonList(html));
                    fail();
                } catch (TemplateModelException e) {
                    assertThat(e.getMessage(), allOf(
                            containsString("String"), containsString("convert"), containsString("markup_output"),
                            containsString("Tip:"), containsString("?markup_string")));
                }
            }
            
            {
                TemplateMethodModelEx m = (TemplateMethodModelEx) thm.get("m2");
                try {
                    m.exec(Collections.singletonList(html));
                    fail();
                } catch (TemplateModelException e) {
                    assertThat(e.getMessage(), allOf(
                            containsString("Date"), containsString("convert"), containsString("markup_output"),
                            not(containsString("?markup_string"))));
                }
            }
            
            for (String methodName : new String[] { "mOverloaded", "mOverloaded3" }) {
                TemplateMethodModelEx m = (TemplateMethodModelEx) thm.get(methodName);
                try {
                    m.exec(Collections.singletonList(html));
                    fail();
                } catch (TemplateModelException e) {
                    assertThat(e.getMessage(), allOf(
                            containsString("No compatible overloaded"),
                            containsString("String"), containsString("markup_output"),
                            containsString("Tip:"), containsString("?markup_string")));
                }
            }
            
            {
                TemplateMethodModelEx m = (TemplateMethodModelEx) thm.get("mOverloaded2");
                try {
                    m.exec(Collections.singletonList(html));
                    fail();
                } catch (TemplateModelException e) {
                    assertThat(e.getMessage(), allOf(
                            containsString("No compatible overloaded"),
                            containsString("Integer"), containsString("markup_output"),
                            not(containsString("?markup_string"))));
                }
            }
            
            {
                TemplateMethodModelEx m = (TemplateMethodModelEx) thm.get("mOverloaded4");
                Object r = m.exec(Collections.singletonList(html));
                if (r instanceof TemplateScalarModel) {
                    r = ((TemplateScalarModel) r).getAsString();
                }
                assertEquals("<p>a", r);
            }
        }
    }
    
    public static class TestBean {
        
        public String getFoo() {
            throw new RuntimeException("Dummy");
        }
        
        public void m1(String s) {
            // nop
        }

        public void m2(Date s) {
            // nop
        }

        public void mOverloaded(String s) {
            // nop
        }

        public void mOverloaded(Date d) {
            // nop
        }

        public void mOverloaded2(Integer n) {
            // nop
        }

        public void mOverloaded2(Date d) {
            // nop
        }

        public void mOverloaded3(String... s) {
            // nop
        }

        public void mOverloaded3(Date d) {
            // nop
        }
        
        public String mOverloaded4(String s) {
            return s;
        }

        public String mOverloaded4(TemplateHTMLOutputModel s) throws TemplateModelException {
            return s.getOutputFormat().getMarkupString(s);
        }
        
    }
    
}

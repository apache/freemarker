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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.TemplateHTMLOutputModel;
import org.junit.Test;

public class ErrorMessagesTest {

    @Test
    public void getterMessage() throws TemplateException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateHashModel thm= (TemplateHashModel) ow.wrap(new TestBean());
        
        try {
            thm.get("foo");
        } catch (TemplateException e) {
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

        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateHashModel thm = (TemplateHashModel) ow.wrap(new TestBean());
        
        {
            JavaMethodModel m = (JavaMethodModel) thm.get("m1");
            try {
                m.execute(new TemplateModel[] { html }, NonTemplateCallPlace.INSTANCE);
                fail();
            } catch (TemplateException e) {
                assertThat(e.getMessage(), allOf(
                        containsString("String"), containsString("convert"), containsString("markupOutput"),
                        containsString("Tip:"), containsString("?markupString")));
            }
        }
        
        {
            JavaMethodModel m = (JavaMethodModel) thm.get("m2");
            try {
                m.execute(new TemplateModel[] { html }, NonTemplateCallPlace.INSTANCE);
                fail();
            } catch (TemplateException e) {
                assertThat(e.getMessage(), allOf(
                        containsString("Date"), containsString("convert"), containsString("markupOutput"),
                        not(containsString("?markupString"))));
            }
        }
        
        for (String methodName : new String[] { "mOverloaded", "mOverloaded3" }) {
            JavaMethodModel m = (JavaMethodModel)thm.get(methodName);
            try {
                m.execute(new TemplateModel[] { html }, NonTemplateCallPlace.INSTANCE);
                fail();
            } catch (TemplateException e) {
                assertThat(e.getMessage(), allOf(
                        containsString("No compatible overloaded"),
                        containsString("String"), containsString("markupOutput"),
                        containsString("Tip:"), containsString("?markupString")));
            }
        }
        
        {
            JavaMethodModel m = (JavaMethodModel)thm.get("mOverloaded2");
            try {
                m.execute(new TemplateModel[] { html }, NonTemplateCallPlace.INSTANCE);
                fail();
            } catch (TemplateException e) {
                assertThat(e.getMessage(), allOf(
                        containsString("No compatible overloaded"),
                        containsString("Integer"), containsString("markupOutput"),
                        not(containsString("?markupString"))));
            }
        }
        
        {
            JavaMethodModel m = (JavaMethodModel)thm.get("mOverloaded4");
            Object r = m.execute(new TemplateModel[] { html }, NonTemplateCallPlace.INSTANCE);
            if (r instanceof TemplateStringModel) {
                r = ((TemplateStringModel) r).getAsString();
            }
            assertEquals("<p>a", r);
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

        public String mOverloaded4(TemplateHTMLOutputModel s) throws TemplateException {
            return s.getOutputFormat().getMarkupString(s);
        }
        
    }
    
}

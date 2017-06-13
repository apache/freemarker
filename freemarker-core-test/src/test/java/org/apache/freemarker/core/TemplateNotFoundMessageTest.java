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

package org.apache.freemarker.core;

import static org.apache.freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupResult;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.FileTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.junit.Test;

public class TemplateNotFoundMessageTest {

    @Test
    public void testFileTemplateLoader() throws IOException {
        final File baseDir = new File(System.getProperty("user.home"));
        final String errMsg = failWith(new FileTemplateLoader(baseDir));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString(baseDir.toString()));
        assertThat(errMsg, containsString("FileTemplateLoader"));
    }

    @Test
    public void testClassTemplateLoader() throws IOException {
        final String errMsg = failWith(new ClassTemplateLoader(getClass(), "foo/bar"));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("ClassTemplateLoader"));
        assertThat(errMsg, containsString("foo/bar"));
    }

    @Test
    public void testStringTemplateLoader() throws IOException {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("aaa", "A");
        tl.putTemplate("bbb", "B");
        tl.putTemplate("ccc", "C");
        final String errMsg = failWith(tl);
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("StringTemplateLoader"));
        assertThat(errMsg, containsString("aaa"));
        assertThat(errMsg, containsString("bbb"));
        assertThat(errMsg, containsString("ccc"));
    }
    
    @Test
    public void testMultiTemplateLoader() throws IOException {
        final String errMsg = failWith(new MultiTemplateLoader(new TemplateLoader[] {
                new StringTemplateLoader(),
                new ClassTemplateLoader(getClass(), "foo/bar")
        }));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("MultiTemplateLoader"));
        assertThat(errMsg, containsString("StringTemplateLoader"));
        assertThat(errMsg, containsString("ClassTemplateLoader"));
        assertThat(errMsg, containsString("foo/bar"));
    }

    @Test
    public void testDefaultTemplateLoader() throws IOException {
        String errMsg = failWith(null);
        showErrorMessage(errMsg);
        assertThat(errMsg, allOf(containsString("setTemplateLoader"), containsString("null")));
    }
    
    @Test
    public void testOtherMessageDetails() throws IOException {
        // Non-null TemplateLoader:
        StringTemplateLoader emptyLoader = new StringTemplateLoader();
        {
            String errMsg = failWith(emptyLoader, "../x");
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(
                            containsStringIgnoringCase("Malformed template name"),
                            containsStringIgnoringCase("root directory")));
        }
        {
            String errMsg = failWith(emptyLoader, "x\u0000y");
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(
                            containsStringIgnoringCase("Malformed template name"),
                            containsStringIgnoringCase("null character")));
        }
        {
            String errMsg = failWith(emptyLoader, "x\\y");
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    containsStringIgnoringCase("backslash"));
        }
        {
            String errMsg = failWith(emptyLoader, "x/./y");
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(containsStringIgnoringCase("normalized"), containsStringIgnoringCase("x/y")));
        }
        {
            String errMsg = failWith(emptyLoader, "/x/y");
            showErrorMessage(errMsg);
            assertThat(errMsg, not(containsStringIgnoringCase("normalized")));
        }
        {
            String errMsg = failWith(emptyLoader, "x/y");
            showErrorMessage(errMsg);
            assertThat(errMsg, not(containsStringIgnoringCase("normalized")));
            assertThat(errMsg, not(containsStringIgnoringCase("lookup strategy")));
        }

        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(new StringTemplateLoader())
                .templateLookupStrategy(
                        new TemplateLookupStrategy() {
                            @Override
                            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                                return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
                            }
                        }
        );
        {
            String errMsg = failWith(emptyLoader, "x/y",
                    new TemplateLookupStrategy() {
                        @Override
                        public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                            return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
                        }
                    }
            );
            showErrorMessage(errMsg);
            assertThat(errMsg, containsStringIgnoringCase("lookup strategy"));
        }
        
        try {
            cfgB.build().getTemplate("./missing", null, new DomainLookupCondition());
            fail();
        } catch (TemplateNotFoundException e) {
            showErrorMessage(e.getMessage());
            assertThat(e.getMessage(), containsStringIgnoringCase("example.com"));
        }
    }

    private void showErrorMessage(String errMsg) {
        // System.out.println(errMsg);
    }

    private String failWith(TemplateLoader tl, String name, TemplateLookupStrategy templateLookupStrategy) {
        try {
            Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
            cfgB.setTemplateLoader(tl);
            if (templateLookupStrategy != null) {
                cfgB.setTemplateLookupStrategy(templateLookupStrategy);
            }
            cfgB.build().getTemplate(name);
            fail();
        } catch (TemplateNotFoundException | MalformedTemplateNameException e) {
            return e.getMessage();
        } catch (IOException e) {
            fail("Unexpected exception: " + e);
        }
        return null;
    }

    private String failWith(TemplateLoader tl, String name) {
        return failWith(tl, name, null);
    }

    private String failWith(TemplateLoader tl) {
        return failWith(tl, "missing.ftl", null);
    }

    @SuppressWarnings("serial")
    private static final class DomainLookupCondition implements Serializable {
        @Override
        public String toString() {
            return "example.com";
        }
    }

}

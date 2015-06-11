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

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupResult;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.cache.WebappTemplateLoader;

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
        final String errMsg = failWith(new ClassTemplateLoader(this.getClass(), "foo/bar"));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("ClassTemplateLoader"));
        assertThat(errMsg, containsString("foo/bar"));
    }

    @Test
    public void testWebappTemplateLoader() throws IOException {
        final String errMsg = failWith(new WebappTemplateLoader(new MockServletContext(), "WEB-INF/templates"));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("WebappTemplateLoader"));
        assertThat(errMsg, containsString("MyApp"));
        assertThat(errMsg, containsString("WEB-INF/templates"));
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
                new WebappTemplateLoader(new MockServletContext(), "WEB-INF/templates"),
                new ClassTemplateLoader(this.getClass(), "foo/bar")
        }));
        showErrorMessage(errMsg);
        assertThat(errMsg, containsString("MultiTemplateLoader"));
        assertThat(errMsg, containsString("WebappTemplateLoader"));
        assertThat(errMsg, containsString("MyApp"));
        assertThat(errMsg, containsString("WEB-INF/templates"));
        assertThat(errMsg, containsString("ClassTemplateLoader"));
        assertThat(errMsg, containsString("foo/bar"));
    }

    @Test
    public void testDefaultTemplateLoader() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        {
            String errMsg = failWith(cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg, allOf(containsString("setTemplateLoader"), containsString("dangerous")));
        }
        {
            cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_21);
            String errMsg = failWith(cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg, allOf(containsString("setTemplateLoader"), containsString("null")));
        }
    }
    
    @Test
    public void testOtherMessageDetails() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setTemplateLoader(new StringTemplateLoader());
        
        {
            String errMsg = failWith("../x", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(containsStringIgnoringCase("reason"), containsStringIgnoringCase("root directory")));
        }
        {
            String errMsg = failWith("x\u0000y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(containsStringIgnoringCase("reason"), containsStringIgnoringCase("null character")));
        }
        {
            String errMsg = failWith("x\\y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(containsStringIgnoringCase("warning"), containsStringIgnoringCase("backslash")));
        }
        {
            String errMsg = failWith("x/./y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg,
                    allOf(containsStringIgnoringCase("normalized"), containsStringIgnoringCase("x/y")));
        }
        {
            String errMsg = failWith("/x/y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg, not(containsStringIgnoringCase("normalized")));
        }
        {
            String errMsg = failWith("x/y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg, not(containsStringIgnoringCase("normalized")));
            assertThat(errMsg, not(containsStringIgnoringCase("lookup strategy")));
        }
        
        cfg.setTemplateLookupStrategy(new TemplateLookupStrategy() {
            @Override
            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
            }
        });
        {
            String errMsg = failWith("x/y", cfg);
            showErrorMessage(errMsg);
            assertThat(errMsg, containsStringIgnoringCase("lookup strategy"));
        }
        
        try {
            cfg.getTemplate("./missing", null, "example.com", null, true, false);
            fail();
        } catch (TemplateNotFoundException e) {
            showErrorMessage(e.getMessage());
            assertThat(e.getMessage(), containsStringIgnoringCase("example.com"));
        }
    }

    private void showErrorMessage(String errMsg) {
        // System.out.println(errMsg);
    }

    private String failWith(TemplateLoader tl, String name, Configuration cfg) {
        if (tl != null) {
            cfg.setTemplateLoader(tl);
        }
        try {
            cfg.getTemplate(name);
            fail();
        } catch (TemplateNotFoundException e) {
            return e.getMessage();
        } catch (IOException e) {
            fail();
        }
        return null;
    }
    
    private String failWith(TemplateLoader tl) {
        return failWith(tl, "missing.ftl", new Configuration(Configuration.VERSION_2_3_21));
    }

    private String failWith(Configuration cfg) {
        return failWith(null, "missing.ftl", cfg);
    }

    private String failWith(String name, Configuration cfg) {
        return failWith(null, name, cfg);
    }

}

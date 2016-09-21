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

package freemarker.test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;
import freemarker.test.templatesuite.TemplateTestSuite;

/**
 * Superclass of JUnit tests that process templates but aren't practical to implement via {@link TemplateTestSuite}. 
 */
@Ignore
public abstract class TemplateTest {
    
    private Configuration configuration;
    private boolean dataModelCreated;
    private Object dataModel;

    protected final Configuration getConfiguration() {
        if (configuration == null) {
            try {
                configuration = createConfiguration();
                addCommonTemplates();
            } catch (Exception e) {
                throw new RuntimeException("Failed to set up configuration for the test", e);
            }
        }
        return configuration;
    }

    protected final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected final void dropConfiguration() {
        configuration = null;
    }
    
    protected void assertOutput(String ftl, String expectedOut) throws IOException, TemplateException {
        assertOutput(createTemplate(ftl), expectedOut, false);
    }

    private Template createTemplate(String ftl) throws IOException {
        Template t = new Template(null, ftl, getConfiguration());
        return t;
    }

    protected void assertOutputForNamed(String name, String expectedOut) throws IOException, TemplateException {
        assertOutput(getConfiguration().getTemplate(name), expectedOut, false);
    }

    @SuppressFBWarnings(value="UI_INHERITANCE_UNSAFE_GETRESOURCE", justification="By design relative to subclass")
    protected void assertOutputForNamed(String name) throws IOException, TemplateException {
        String expectedOut;
        {
            String resName = name + ".out";
            InputStream in = this.getClass().getResourceAsStream(resName);
            if (in == null) {
                throw new IOException("Reference output resource not found: " + this.getClass() + ", " + resName);
            }
            try {
                expectedOut = TestUtil.removeTxtCopyrightComment(IOUtils.toString(in, "utf-8"));
            } finally {
                in.close();
            }
        }
        assertOutput(getConfiguration().getTemplate(name), expectedOut, true);
    }

    protected void assertOutput(Template t, String expectedOut) throws TemplateException, IOException {
        assertOutput(t, expectedOut, false);
    }
    
    protected void assertOutput(Template t, String expectedOut, boolean normalizeNewlines)
            throws TemplateException, IOException {
        String actualOut = getOutput(t);
        
        if (normalizeNewlines) {
            expectedOut = normalizeNewLines(expectedOut);
            actualOut = normalizeNewLines(actualOut);
        }
        assertEquals(expectedOut, actualOut);
    }

    protected String getOutput(String ftl) throws IOException, TemplateException {
        return getOutput(createTemplate(ftl));
    }
    
    protected String getOutput(Template t) throws TemplateException, IOException {
        StringWriter out = new StringWriter();
        t.process(getDataModel(), out);
        String actualOut = out.toString();
        return actualOut;
    }
    
    protected Configuration createConfiguration() throws Exception {
        Configuration c=new Configuration(Configuration.VERSION_2_3_0);
        c.setLocale(Locale.US);
        return c;
    }
    
    protected void addCommonTemplates() {
        //
    }

    protected Object getDataModel() {
        if (!dataModelCreated) {
            dataModel = createDataModel();
            dataModelCreated = true;
        }
        return dataModel;
    }
    
    protected Object createDataModel() {
        return null;
    }
    
    @SuppressWarnings("boxing")
    protected Map<String, Object> createCommonTestValuesDataModel() {
        Map<String, Object> dataModel = new HashMap<String, Object>();
        dataModel.put("map", Collections.singletonMap("key", "value"));
        dataModel.put("list", Collections.singletonList("item"));
        dataModel.put("s", "text");
        dataModel.put("n", 1);
        dataModel.put("b", true);
        dataModel.put("bean", new TestBean());
        return dataModel;
    }
    
    protected void addTemplate(String name, String content) {
        Configuration cfg = getConfiguration();
        TemplateLoader tl = cfg.getTemplateLoader();
        StringTemplateLoader stl;
        if (tl != null) {
            stl = extractStringTemplateLoader(tl);
        } else {
            stl = new StringTemplateLoader();
            cfg.setTemplateLoader(stl);
        }
        stl.putTemplate(name, content);
    }
    
    private StringTemplateLoader extractStringTemplateLoader(TemplateLoader tl) {
        if (tl instanceof MultiTemplateLoader) {
            MultiTemplateLoader mtl = (MultiTemplateLoader) tl;
            for (int i = 0; i < mtl.getTemplateLoaderCount(); i++) {
                TemplateLoader tli = mtl.getTemplateLoader(i);
                if (tli instanceof StringTemplateLoader) {
                    return (StringTemplateLoader) tli;
                }
            }
            throw new IllegalStateException(
                    "The template loader was a MultiTemplateLoader that didn't contain StringTemplateLoader: "
                            + tl);
        } else if (tl instanceof StringTemplateLoader) {
            return (StringTemplateLoader) tl;
        } else {
            throw new IllegalStateException(
                    "The template loader was already set to a non-StringTemplateLoader non-MultiTemplateLoader: "
                            + tl);
        }
    }

    protected void addToDataModel(String name, Object value) {
        Object dm = getDataModel();
        if (dm == null) {
            dm = new HashMap<String, Object>();
            dataModel = dm;
        }
        if (dm instanceof Map) {
            ((Map) dm).put(name, value);
        } else {
            throw new IllegalStateException("Can't add to non-Map data-model: " + dm);
        }
    }
    
    protected Throwable assertErrorContains(String ftl, String... expectedSubstrings) {
        return assertErrorContains(null, ftl, null, expectedSubstrings);
    }

    protected Throwable assertErrorContains(String ftl, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        return assertErrorContains(null, ftl, exceptionClass, expectedSubstrings);
    }

    protected void assertErrorContainsForNamed(String name, String... expectedSubstrings) {
        assertErrorContains(name, null, null, expectedSubstrings);
    }

    protected void assertErrorContainsForNamed(String name, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        assertErrorContains(name, null, exceptionClass, expectedSubstrings);
    }
    
    private Throwable assertErrorContains(String name, String ftl, Class<? extends Throwable> exceptionClass,
            String... expectedSubstrings) {
        try {
            Template t;
            if (ftl == null) {
                t = getConfiguration().getTemplate(name);
            } else {
                t = new Template("adhoc", ftl, getConfiguration());
            }
            t.process(getDataModel(), new StringWriter());
            fail("The tempalte had to fail");
            return null;
        } catch (TemplateException e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
            }
            assertContainsAll(e.getMessageWithoutStackTop(), expectedSubstrings);
            return e;
        } catch (ParseException e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
            }
            assertContainsAll(e.getEditorMessage(), expectedSubstrings);
            return e;
        } catch (Exception e) {
            if (exceptionClass != null) {
                assertThat(e, instanceOf(exceptionClass));
                return e;
            } else {
                throw new RuntimeException("Unexpected exception class: " + e.getClass().getName(), e);
            }
        }
    }
    
    private void assertContainsAll(String msg, String... expectedSubstrings) {
        for (String needle: expectedSubstrings) {
            if (needle.startsWith("\\!")) {
                String netNeedle = needle.substring(2); 
                if (msg.contains(netNeedle)) {
                    fail("The message shouldn't contain substring " + StringUtil.jQuote(netNeedle) + ":\n" + msg);
                }
            } else if (!msg.contains(needle)) {
                fail("The message didn't contain substring " + StringUtil.jQuote(needle) + ":\n" + msg);
            }
        }
    }
    
    private String normalizeNewLines(String s) {
        return StringUtil.replace(s, "\r\n", "\n").replace('\r', '\n');
    }

    public static class TestBean {
        private int x;
        private boolean b;
        
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public boolean isB() {
            return b;
        }
        public void setB(boolean b) {
            this.b = b;
        }

        public int intM() {
            return 1;
        }

        public int intMP(int x) {
            return x;
        }
        
        public void voidM() {
            
        }
        
    }
    
}

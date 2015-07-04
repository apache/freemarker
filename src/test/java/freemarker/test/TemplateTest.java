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

package freemarker.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;

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
    
    private Configuration configuration = new Configuration(Configuration.VERSION_2_3_0);

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    protected void assertOutput(String ftl, String expectedOut) throws IOException, TemplateException {
        Template t = new Template(null, ftl, configuration);
        assertOutput(t, expectedOut);
    }

    protected void assertOutputForNamed(String name, String expectedOut) throws IOException, TemplateException {
        assertOutput(configuration.getTemplate(name), expectedOut);
    }
    
    protected void assertOutput(Template t, String expectedOut) throws TemplateException, IOException {
        StringWriter out = new StringWriter();
        t.process(createDataModel(), out);
        assertEquals(expectedOut, out.toString());
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
    
    protected void assertErrorContains(String ftl, String... expectedSubstrings) {
        try {
            new Template("adhoc", ftl, configuration).process(createDataModel(), new StringWriter());
            fail("The tempalte had to fail");
        } catch (TemplateException e) {
            assertContainsAll(e.getMessageWithoutStackTop(), expectedSubstrings);
        } catch (ParseException e) {
            assertContainsAll(e.getEditorMessage(), expectedSubstrings);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception class: " + e.getClass().getName(), e);
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

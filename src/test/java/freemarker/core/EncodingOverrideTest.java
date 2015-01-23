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

package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class EncodingOverrideTest {

    @Test
    public void testExactMarchingCharset() throws Exception {
        Template t = createConfig("UTF-8").getTemplate("encodingOverride-UTF-8.ftl");
        assertEquals("UTF-8", t.getEncoding());
        checkTempateOutput(t);
    }

    @Test
    public void testCaseDiffCharset() throws Exception {
        Template t = createConfig("utf-8").getTemplate("encodingOverride-UTF-8.ftl");
        assertEquals("utf-8", t.getEncoding());
        checkTempateOutput(t);
    }

    @Test
    public void testReallyDiffCharset() throws Exception {
        Template t = createConfig("utf-8").getTemplate("encodingOverride-ISO-8859-1.ftl");
        assertEquals("ISO-8859-1", t.getEncoding());
        checkTempateOutput(t);
    }

    private void checkTempateOutput(Template t) throws TemplateException, IOException {
        StringWriter out = new StringWriter(); 
        t.process(Collections.emptyMap(), out);
        assertEquals("Béka", out.toString());
    }
    
    private Configuration createConfig(String charset) {
       Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
       cfg.setClassForTemplateLoading(EncodingOverrideTest.class, "");
       cfg.setDefaultEncoding(charset);
       return cfg;
    }

}

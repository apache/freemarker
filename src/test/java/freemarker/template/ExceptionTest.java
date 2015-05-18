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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;

import junit.framework.TestCase;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.utility.NullWriter;
public class ExceptionTest extends TestCase {
    
    public ExceptionTest(String name) {
        super(name);
    }

    public void testParseExceptionSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        try {
            new Template("<string>", new StringReader("<@>"), cfg);
            fail();
        } catch (ParseException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }

    public void testTemplateErrorSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        Template tmp = new Template("<string>", new StringReader("${noSuchVar}"), cfg);
        try {
            tmp.process(Collections.EMPTY_MAP, new StringWriter());
            fail();
        } catch (TemplateException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }
    
    @SuppressWarnings("boxing")
    public void testTemplateExceptionLocationInformation() throws IOException {
        Configuration cfg = new Configuration();
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("foo_en.ftl", "\n\nxxx${noSuchVariable}");
        cfg.setTemplateLoader(tl);
        
        Template t = cfg.getTemplate("foo.ftl", Locale.US);
        try {
            t.process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertEquals("foo.ftl", e.getTemplateName());
            assertEquals("foo_en.ftl", e.getTemplateSourceName());
            assertEquals(3, (int) e.getLineNumber());
            assertEquals(6, (int) e.getColumnNumber());
            assertEquals(3, (int) e.getEndLineNumber());
            assertEquals(19, (int) e.getEndColumnNumber());
            assertThat(e.getMessage(), containsString("foo_en.ftl"));
            assertThat(e.getMessage(), containsString("noSuchVariable"));
        }
    }

    @SuppressWarnings("cast")
    public void testParseExceptionLocationInformation() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("foo_en.ftl", "\n\nxxx<#noSuchDirective>");
        cfg.setTemplateLoader(tl);
        
        try {
            cfg.getTemplate("foo.ftl", Locale.US);
            fail();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            assertEquals("foo_en.ftl", e.getTemplateName());
            assertEquals(3, (int) e.getLineNumber());
            assertEquals(5, (int) e.getColumnNumber());
            assertEquals(3, (int) e.getEndLineNumber());
            assertEquals(20, (int) e.getEndColumnNumber());
            assertThat(e.getMessage(), containsString("foo_en.ftl"));
            assertThat(e.getMessage(), containsString("noSuchDirective"));
        }
    }
    
}

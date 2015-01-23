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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;
public class TemplateConstructorsTest {

    private static final String READER_CONTENT = "From a reader...";
    private static final String READER_CONTENT_FORCE_UTF8 = "<#ftl encoding='utf-8'>From a reader...";
    
    @Test
    public void test() throws IOException {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        //cfg.setDefaultEncoding("ISO-8859-1");
        
        final String name = "foo/bar.ftl";
        final String sourceName = "foo/bar_de.ftl";
        final String content = "From a String...";
        final String encoding = "UTF-16LE";
        {
            Template t = new Template(name, createReader());
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, createReader(), cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, content, cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(content, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, createReader(), cfg, encoding);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertEquals("UTF-16LE", t.getEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg);
            assertEquals(name, t.getName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertNull(t.getEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg, encoding);
            assertEquals(name, t.getName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(READER_CONTENT, t.toString());
            assertEquals("UTF-16LE", t.getEncoding());
        }
        {
            Template t = Template.getPlainTextTemplate(name, content, cfg);
            assertEquals(name, t.getName());
            assertEquals(name, t.getSourceName());
            assertEquals(content, t.toString());
            assertNull(t.getEncoding());
        }
        {
            try {
                new Template(name, sourceName, createReaderForceUTF8(), cfg, encoding);
                fail();
            } catch (Template.WrongEncodingException e) {
                assertThat(e.getMessage(), containsString("utf-8"));
                assertThat(e.getMessage(), containsString(encoding));
            }
        }
    }
    
    private final Reader createReader() {
        return new StringReader(READER_CONTENT);
    }

    private final Reader createReaderForceUTF8() {
        return new StringReader(READER_CONTENT_FORCE_UTF8);
    }
    
}

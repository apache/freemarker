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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;
public class TemplateConstructorsTest {

    private static final String CONTENT = "From a reader...";
    private static final String CONTENT_FORCE_UTF8 = "<#ftl encoding='utf-8'>From a reader...";
    
    @Test
    public void test() throws IOException {
        final Configuration cfg = new TestConfigurationBuilder().sourceEncoding(StandardCharsets.ISO_8859_1).build();
        
        final String name = "foo/bar.f3ah";
        final String sourceName = "foo/bar_de.f3ah";
        final Charset sourceEncoding = StandardCharsets.UTF_16LE;
        {
            Template t = new Template(name, createReader(), cfg);
            assertEquals(name, t.getLookupName());
            assertNull(t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertNull(t.getActualSourceEncoding());
        }
        {
            Template t = new Template(name, CONTENT, cfg);
            assertEquals(name, t.getLookupName());
            assertNull(t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertNull(t.getActualSourceEncoding());
        }
        {
            Template t = new Template(name, CONTENT_FORCE_UTF8, cfg);
            assertEquals(name, t.getLookupName());
            assertNull(t.getSourceName());
            // assertEquals(CONTENT_FORCE_UTF8, t.toString()); // FIXME the #ftl header is missing from the dump, why?
            assertNull(t.getActualSourceEncoding()); // Because it was created from a String
        }
        {
            Template t = new Template(name, createReader(), cfg, sourceEncoding);
            assertEquals(name, t.getLookupName());
            assertNull(t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertEquals(StandardCharsets.UTF_16LE, t.getActualSourceEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg);
            assertEquals(name, t.getLookupName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertNull(t.getActualSourceEncoding());
        }
        {
            Template t = new Template(name, sourceName, createReader(), cfg, sourceEncoding);
            assertEquals(name, t.getLookupName());
            assertEquals(sourceName, t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertEquals(StandardCharsets.UTF_16LE, t.getActualSourceEncoding());
        }
        {
            Template t = new Template(name, CONTENT, cfg,
                    new TemplateConfiguration.Builder()
                    .templateLanguage(UnparsedTemplateLanguage.F3UU)
                    .recognizeStandardFileExtensions(false)
                    .build());
            assertEquals(name, t.getLookupName());
            assertNull(t.getSourceName());
            assertEquals(CONTENT, t.toString());
            assertNull(t.getActualSourceEncoding());
        }
        {
            try {
                new Template(name, sourceName, createReaderForceUTF8(), cfg, sourceEncoding);
                fail();
            } catch (WrongTemplateCharsetException e) {
                assertThat(e.getMessage(), containsString(StandardCharsets.UTF_8.name()));
                assertThat(e.getMessage(), containsString(sourceEncoding.name()));
            }
        }
    }
    
    private Reader createReader() {
        return new StringReader(CONTENT);
    }

    private Reader createReaderForceUTF8() {
        return new StringReader(CONTENT_FORCE_UTF8);
    }
    
}

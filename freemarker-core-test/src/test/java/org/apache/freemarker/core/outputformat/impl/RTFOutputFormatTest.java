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
package org.apache.freemarker.core.outputformat.impl;

import static org.apache.freemarker.core.outputformat.impl.RTFOutputFormat.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.outputformat._OutputFormatTestAPI;
import org.junit.Test; 

public class RTFOutputFormatTest {
    
    @Test
    public void testOutputMO() throws TemplateException, IOException {
       StringWriter out = new StringWriter();
       
       INSTANCE.output(INSTANCE.fromMarkup("\\par Test "), out);
       INSTANCE.output(INSTANCE.fromPlainTextByEscaping("foo { bar } \\ "), out);
       INSTANCE.output(INSTANCE.fromPlainTextByEscaping("baaz "), out);
       INSTANCE.output(INSTANCE.fromPlainTextByEscaping("\\par qweqwe"), out);
       INSTANCE.output(INSTANCE.fromMarkup("\\par{0} End"), out);
       
       assertEquals(
               "\\par Test "
               + "foo \\{ bar \\} \\\\ "
               + "baaz "
               + "\\\\par qweqwe"
               + "\\par{0} End",
               out.toString());
    }
    
    @Test
    public void testOutputString() throws TemplateException, IOException {
        StringWriter out = new StringWriter();
        
        INSTANCE.output("a", out);
        INSTANCE.output("{", out);
        INSTANCE.output("b}c", out);
        
        assertEquals("a\\{b\\}c", out.toString());
    }
    
    @Test
    public void testFromPlainTextByEscaping() throws TemplateException {
        String plainText = "a\\b";
        TemplateRTFOutputModel mo = INSTANCE.fromPlainTextByEscaping(plainText);
        assertSame(plainText, _OutputFormatTestAPI.getPlainTextContent(mo));
        assertNull(_OutputFormatTestAPI.getMarkupContent(mo)); // Not the MO's duty to calculate it!
    }

    @Test
    public void testFromMarkup() throws TemplateException {
        String markup = "a \\par b";
        TemplateRTFOutputModel mo = INSTANCE.fromMarkup(markup);
        assertSame(markup, _OutputFormatTestAPI.getMarkupContent(mo));
        assertNull(_OutputFormatTestAPI.getPlainTextContent(mo)); // Not the MO's duty to calculate it!
    }
    
    @Test
    public void testGetMarkup() throws TemplateException {
        {
            String markup = "a \\par b";
            TemplateRTFOutputModel mo = INSTANCE.fromMarkup(markup);
            assertSame(markup, INSTANCE.getMarkupString(mo));
        }
        
        {
            String safe = "abc";
            TemplateRTFOutputModel mo = INSTANCE.fromPlainTextByEscaping(safe);
            assertSame(safe, INSTANCE.getMarkupString(mo));
        }
    }
    
    @Test
    public void testConcat() throws Exception {
        assertMO(
                "ab", null,
                INSTANCE.concat(new TemplateRTFOutputModel("a", null), new TemplateRTFOutputModel("b", null)));
        assertMO(
                null, "ab",
                INSTANCE.concat(new TemplateRTFOutputModel(null, "a"), new TemplateRTFOutputModel(null, "b")));
        assertMO(
                null, "{a}\\{b\\}",
                INSTANCE.concat(new TemplateRTFOutputModel(null, "{a}"), new TemplateRTFOutputModel("{b}", null)));
        assertMO(
                null, "\\{a\\}{b}",
                INSTANCE.concat(new TemplateRTFOutputModel("{a}", null), new TemplateRTFOutputModel(null, "{b}")));
    }
    
    @Test
    public void testEscaplePlainText() {
        assertEquals("", INSTANCE.escapePlainText(""));
        assertEquals("a", INSTANCE.escapePlainText("a"));
        assertEquals("\\{a\\\\b\\}", INSTANCE.escapePlainText("{a\\b}"));
        assertEquals("a\\\\b", INSTANCE.escapePlainText("a\\b"));
        assertEquals("\\{\\}", INSTANCE.escapePlainText("{}"));
    }
    
    private void assertMO(String pc, String mc, TemplateRTFOutputModel mo) {
        assertEquals(pc, _OutputFormatTestAPI.getPlainTextContent(mo));
        assertEquals(mc, _OutputFormatTestAPI.getMarkupContent(mo));
    }
    
    @Test
    public void testGetMimeType() {
        assertEquals("application/rtf", INSTANCE.getMimeType());
    }
    
}

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
package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import freemarker.template.TemplateModelException; 

public class CombinedMarkupOutputFormatTest {
    
    private static final CombinedMarkupOutputFormat HTML_RTF = new CombinedMarkupOutputFormat(
            HTMLOutputFormat.INSTANCE, RTFOutputFormat.INSTANCE);
    private static final CombinedMarkupOutputFormat XML_XML = new CombinedMarkupOutputFormat(
            XMLOutputFormat.INSTANCE, XMLOutputFormat.INSTANCE);

    @Test
    public void testName() {
        assertEquals("HTML{RTF}", HTML_RTF.getName());
        assertEquals("XML{XML}", XML_XML.getName());
    }
    
    @Test
    public void testOutputMO() throws TemplateModelException, IOException {
       StringWriter out = new StringWriter();
       
       HTML_RTF.output(HTML_RTF.fromMarkup("<pre>\\par Test "), out);
       HTML_RTF.output(HTML_RTF.fromPlainTextByEscaping("foo { bar } \\ "), out);
       HTML_RTF.output(HTML_RTF.fromPlainTextByEscaping("& baaz "), out);
       HTML_RTF.output(HTML_RTF.fromPlainTextByEscaping("\\par & qwe"), out);
       HTML_RTF.output(HTML_RTF.fromMarkup("\\par{0} End</pre>"), out);
       
       assertEquals(
               "<pre>\\par Test "
               + "foo \\{ bar \\} \\\\ "
               + "&amp; baaz "
               + "\\\\par &amp; qwe"
               + "\\par{0} End</pre>",
               out.toString());
    }

    @Test
    public void testOutputMO2() throws TemplateModelException, IOException {
       StringWriter out = new StringWriter();
       
       XML_XML.output(XML_XML.fromMarkup("<pre>&lt;p&gt; Test "), out);
       XML_XML.output(XML_XML.fromPlainTextByEscaping("a & b < c"), out);
       XML_XML.output(XML_XML.fromMarkup(" End</pre>"), out);
       
       assertEquals(
               "<pre>&lt;p&gt; Test "
               + "a &amp;amp; b &amp;lt; c"
               + " End</pre>",
               out.toString());
    }

    @Test
    public void testOutputMO3() throws TemplateModelException, IOException {
        MarkupOutputFormat outputFormat = new CombinedMarkupOutputFormat(
                RTFOutputFormat.INSTANCE,
                new CombinedMarkupOutputFormat(RTFOutputFormat.INSTANCE, RTFOutputFormat.INSTANCE));
        StringWriter out = new StringWriter();
        
        outputFormat.output(outputFormat.fromPlainTextByEscaping("b{}"), out);
        outputFormat.output(outputFormat.fromMarkup("a{}"), out);
        
        assertEquals(
                "b\\\\\\\\\\\\\\{\\\\\\\\\\\\\\}"
                + "a{}",
                out.toString());
    }
    
    @Test
    public void testOutputString() throws TemplateModelException, IOException {
        StringWriter out = new StringWriter();
        
        HTML_RTF.output("a", out);
        HTML_RTF.output("{", out);
        HTML_RTF.output("<b>}c", out);
        
        assertEquals("a\\{&lt;b&gt;\\}c", out.toString());
    }
    
    @Test
    public void testOutputString2() throws TemplateModelException, IOException {
        StringWriter out = new StringWriter();
        
        XML_XML.output("a", out);
        XML_XML.output("&", out);
        XML_XML.output("<b>", out);
        
        assertEquals("a&amp;amp;&amp;lt;b&amp;gt;", out.toString());
    }
    
    @Test
    public void testFromPlainTextByEscaping() throws TemplateModelException {
        String plainText = "a\\b&c";
        TemplateCombinedMarkupOutputModel mo = HTML_RTF.fromPlainTextByEscaping(plainText);
        assertSame(plainText, mo.getPlainTextContent());
        assertNull(mo.getMarkupContent()); // Not the MO's duty to calculate it!
    }

    @Test
    public void testFromMarkup() throws TemplateModelException {
        String markup = "a \\par <b>";
        TemplateCombinedMarkupOutputModel mo = HTML_RTF.fromMarkup(markup);
        assertSame(markup, mo.getMarkupContent());
        assertNull(mo.getPlainTextContent()); // Not the MO's duty to calculate it!
    }
    
    @Test
    public void testGetMarkup() throws TemplateModelException {
        {
            String markup = "a \\par <b>";
            TemplateCombinedMarkupOutputModel mo = HTML_RTF.fromMarkup(markup);
            assertSame(markup, HTML_RTF.getMarkupString(mo));
        }
        
        {
            String safe = "abc";
            TemplateCombinedMarkupOutputModel mo = HTML_RTF.fromPlainTextByEscaping(safe);
            assertSame(safe, HTML_RTF.getMarkupString(mo));
        }
    }
    
    @Test
    public void testConcat() throws Exception {
        assertMO(
                "ab", null,
                HTML_RTF.concat(
                        new TemplateCombinedMarkupOutputModel("a", null, HTML_RTF),
                        new TemplateCombinedMarkupOutputModel("b", null, HTML_RTF)));
        assertMO(
                null, "ab",
                HTML_RTF.concat(
                        new TemplateCombinedMarkupOutputModel(null, "a", HTML_RTF),
                        new TemplateCombinedMarkupOutputModel(null, "b", HTML_RTF)));
        assertMO(
                null, "{<a>}\\{&lt;b&gt;\\}",
                HTML_RTF.concat(
                        new TemplateCombinedMarkupOutputModel(null, "{<a>}", HTML_RTF),
                        new TemplateCombinedMarkupOutputModel("{<b>}", null, HTML_RTF)));
        assertMO(
                null, "\\{&lt;a&gt;\\}{<b>}",
                HTML_RTF.concat(
                        new TemplateCombinedMarkupOutputModel("{<a>}", null, HTML_RTF),
                        new TemplateCombinedMarkupOutputModel(null, "{<b>}", HTML_RTF)));
    }
    
    @Test
    public void testEscaplePlainText() throws TemplateModelException {
        assertEquals("", HTML_RTF.escapePlainText(""));
        assertEquals("a", HTML_RTF.escapePlainText("a"));
        assertEquals("\\{a\\\\b&amp;\\}", HTML_RTF.escapePlainText("{a\\b&}"));
        assertEquals("a\\\\b&amp;", HTML_RTF.escapePlainText("a\\b&"));
        assertEquals("\\{\\}&amp;", HTML_RTF.escapePlainText("{}&"));
        
        assertEquals("a", XML_XML.escapePlainText("a"));
        assertEquals("a&amp;apos;b", XML_XML.escapePlainText("a'b"));
    }
    
    private void assertMO(String pc, String mc, TemplateCombinedMarkupOutputModel mo) {
        assertEquals(pc, mo.getPlainTextContent());
        assertEquals(mc, mo.getMarkupContent());
    }
    
    @Test
    public void testGetMimeType() {
        assertEquals("text/html", HTML_RTF.getMimeType());
        assertEquals("application/xml", XML_XML.getMimeType());
    }
    
}

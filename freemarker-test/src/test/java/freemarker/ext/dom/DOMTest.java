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
package freemarker.ext.dom;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class DOMTest extends TemplateTest {

    @Test
    public void xpathDetectionBugfix() throws Exception {
        addDocToDataModel("<root><a>A</a><b>B</b><c>C</c></root>");
        assertOutput("${doc.root.b['following-sibling::c']}", "C");
        assertOutput("${doc.root.b['following-sibling::*']}", "C");
    }

    @Test
    public void xmlnsPrefixes() throws Exception {
        addDocToDataModel("<root xmlns='http://example.com/ns1' xmlns:ns2='http://example.com/ns2'>"
                + "<a>A</a><ns2:b>B</ns2:b><c a1='1' ns2:a2='2'/></root>");

        String ftlHeader = "<#ftl ns_prefixes={'D':'http://example.com/ns1', 'n2':'http://example.com/ns2'}>";
        
        // @@markup:
        assertOutput("${doc.@@markup}",
                "<a:root xmlns:a=\"http://example.com/ns1\" xmlns:b=\"http://example.com/ns2\">"
                + "<a:a>A</a:a><b:b>B</b:b><a:c a1=\"1\" b:a2=\"2\" />"
                + "</a:root>");
        assertOutput(ftlHeader
                + "${doc.@@markup}",
                "<root xmlns=\"http://example.com/ns1\" xmlns:n2=\"http://example.com/ns2\">"
                + "<a>A</a><n2:b>B</n2:b><c a1=\"1\" n2:a2=\"2\" /></root>");
        assertOutput("<#ftl ns_prefixes={'D':'http://example.com/ns1'}>"
                + "${doc.@@markup}",
                "<root xmlns=\"http://example.com/ns1\" xmlns:a=\"http://example.com/ns2\">"
                + "<a>A</a><a:b>B</a:b><c a1=\"1\" a:a2=\"2\" /></root>");
        
        // When there's no matching prefix declared via the #ftl header, return null for qname:
        assertOutput("${doc?children[0].@@qname!'null'}", "null");
        assertOutput("${doc?children[0]?children[1].@@qname!'null'}", "null");
        assertOutput("${doc?children[0]?children[2]['@*'][1].@@qname!'null'}", "null");
        
        // When we have prefix declared in the #ftl header:
        assertOutput(ftlHeader + "${doc?children[0].@@qname}", "root");
        assertOutput(ftlHeader + "${doc?children[0]?children[1].@@qname}", "n2:b");
        assertOutput(ftlHeader + "${doc?children[0]?children[2].@@qname}", "c");
        assertOutput(ftlHeader + "${doc?children[0]?children[2]['@*'][0].@@qname}", "a1");
        assertOutput(ftlHeader + "${doc?children[0]?children[2]['@*'][1].@@qname}", "n2:a2");
        // Unfortunately these include the xmlns attributes, but that would be non-BC to fix now:
        assertThat(getOutput(ftlHeader + "${doc?children[0].@@start_tag}"), startsWith("<root"));
        assertThat(getOutput(ftlHeader + "${doc?children[0]?children[1].@@start_tag}"), startsWith("<n2:b"));
    }
    
    @Test
    public void namespaceUnaware() throws Exception {
        addNSUnawareDocToDataModel("<root><x:a>A</x:a><:>B</:><xyz::c>C</xyz::c></root>");
        assertOutput("${doc.root['x:a']}", "A");
        assertOutput("${doc.root[':']}", "B");
        try {
            assertOutput("${doc.root['xyz::c']}", "C");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("xyz"));
        }
    }
    
    private void addDocToDataModel(String xml) throws SAXException, IOException, ParserConfigurationException {
        addToDataModel("doc", NodeModel.parse(new InputSource(new StringReader(xml))));
    }

    private void addDocToDataModelNoSimplification(String xml) throws SAXException, IOException, ParserConfigurationException {
        addToDataModel("doc", NodeModel.parse(new InputSource(new StringReader(xml)), false, false));
    }
    
    private void addNSUnawareDocToDataModel(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
        newFactory.setNamespaceAware(false);
        DocumentBuilder builder = newFactory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        addToDataModel("doc", doc);
    }

    @Test
    public void testInvalidAtAtKeyErrors() throws Exception {
        addDocToDataModel("<r><multipleMatches /><multipleMatches /></r>");
        assertErrorContains("${doc.r.@@invalid_key}", "Unsupported @@ key", "@invalid_key");
        assertErrorContains("${doc.@@start_tag}", "@@start_tag", "not supported", "document");
        assertErrorContains("${doc.@@}", "\"@@\"", "not supported", "document");
        assertErrorContains("${doc.r.noMatch.@@invalid_key}", "Unsupported @@ key", "@invalid_key");
        assertErrorContains("${doc.r.multipleMatches.@@invalid_key}", "Unsupported @@ key", "@invalid_key");
        assertErrorContains("${doc.r.noMatch.@@attributes_markup}", "single XML node", "@@attributes_markup");
        assertErrorContains("${doc.r.multipleMatches.@@attributes_markup}", "single XML node", "@@attributes_markup");
    }
    
    @Test
    public void testAtAtSiblingElement() throws Exception {
        addDocToDataModel("<r><a/><b/></r>");
        assertOutput("${doc.r.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.@@next_sibling_element?size}", "0");
        assertOutput("${doc.r.a.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.a.@@next_sibling_element.@@qname}", "b");
        assertOutput("${doc.r.b.@@previous_sibling_element.@@qname}", "a");
        assertOutput("${doc.r.b.@@next_sibling_element?size}", "0");
        
        addDocToDataModel("<r>\r\n\t <a/>\r\n\t <b/>\r\n\t </r>");
        assertOutput("${doc.r.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.@@next_sibling_element?size}", "0");
        assertOutput("${doc.r.a.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.a.@@next_sibling_element.@@qname}", "b");
        assertOutput("${doc.r.b.@@previous_sibling_element.@@qname}", "a");
        assertOutput("${doc.r.b.@@next_sibling_element?size}", "0");
        
        addDocToDataModel("<r>t<a/>t<b/>t</r>");
        assertOutput("${doc.r.a.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.a.@@next_sibling_element?size}", "0");
        assertOutput("${doc.r.b.@@previous_sibling_element?size}", "0");
        assertOutput("${doc.r.b.@@next_sibling_element?size}", "0");
        
        addDocToDataModelNoSimplification("<r><a/> <!-- --><?pi?>&#x20;<b/></r>");
        assertOutput("${doc.r.a.@@next_sibling_element.@@qname}", "b");
        assertOutput("${doc.r.b.@@previous_sibling_element.@@qname}", "a");
        
        addDocToDataModelNoSimplification("<r><a/> <!-- -->t<!-- --> <b/></r>");
        assertOutput("${doc.r.a.@@next_sibling_element?size}", "0");
        assertOutput("${doc.r.b.@@previous_sibling_element?size}", "0");
    }
    
}

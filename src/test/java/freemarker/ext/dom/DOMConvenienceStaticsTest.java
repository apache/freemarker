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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DOMConvenienceStaticsTest {

    private static final String COMMON_TEST_XML
            = "<!DOCTYPE a []><?p?><a>x<![CDATA[y]]><!--c--><?p?>z<?p?><b><!--c--></b><c></c>"
              + "<d>a<e>c</e>b<!--c--><!--c--><!--c--><?p?><?p?><?p?></d>"
              + "<f><![CDATA[1]]>2</f></a><!--c-->";

    private static final String TEXT_MERGE_CONTENT =
            "<a>"
            + "a<!--c--><s/>"
            + "<!--c-->a<s/>"
            + "a<!--c-->b<s/>"
            + "<!--c-->a<!--c-->b<!--c--><s/>"
            + "a<b>b</b>c<s/>"
            + "a<b>b</b><!--c-->c<s/>"
            + "a<!--c-->1<b>b<!--c--></b>c<!--c-->1<s/>"
            + "a<!--c-->1<b>b<!--c-->c</b>d<!--c-->1<s/>"
            + "a<!--c-->1<b>b<!--c-->c</b>d<!--c-->1<s/>"
            + "a<!--c-->1<b>b<!--c-->1<e>c<!--c-->1</e>d<!--c-->1</b>e<!--c-->1<s/>"
            + "</a>";
    private static final String TEXT_MERGE_EXPECTED =
            "<a>"
            + "%a<s/>"
            + "%a<s/>"
            + "%ab<s/>"
            + "%ab<s/>"
            + "%a<b>%b</b>%c<s/>"
            + "%a<b>%b</b>%c<s/>"
            + "%a1<b>%b</b>%c1<s/>"
            + "%a1<b>%bc</b>%d1<s/>"
            + "%a1<b>%bc</b>%d1<s/>"
            + "%a1<b>%b1<e>%c1</e>%d1</b>%e1<s/>"
            + "</a>";
    
    @Test
    public void testTest() throws Exception {
        String expected = "<!DOCTYPE ...><?p?><a>%x<![CDATA[y]]><!--c--><?p?>%z<?p?><b><!--c--></b><c/>"
                   + "<d>%a<e>%c</e>%b<!--c--><!--c--><!--c--><?p?><?p?><?p?></d>"
                   + "<f><![CDATA[1]]>%2</f></a><!--c-->";
        assertEquals(expected, toString(toDOM(COMMON_TEST_XML)));
    }

    @Test
    public void testMergeAdjacentText() throws Exception {
        Document dom = toDOM(COMMON_TEST_XML);
        NodeModel.mergeAdjacentText(dom);
        assertEquals(
                "<!DOCTYPE ...><?p?><a>%xy<!--c--><?p?>%z<?p?><b><!--c--></b><c/>"
                + "<d>%a<e>%c</e>%b<!--c--><!--c--><!--c--><?p?><?p?><?p?></d>"
                + "<f><![CDATA[12]]></f></a><!--c-->",
                toString(dom));
    }

    @Test
    public void testRemoveComments() throws Exception {
        Document dom = toDOM(COMMON_TEST_XML);
        NodeModel.removeComments(dom);
        assertEquals(
                "<!DOCTYPE ...><?p?><a>%x<![CDATA[y]]><?p?>%z<?p?><b/><c/>"
                + "<d>%a<e>%c</e>%b<?p?><?p?><?p?></d>"
                + "<f><![CDATA[1]]>%2</f></a>",
                toString(dom));
    }

    @Test
    public void testRemovePIs() throws Exception {
        Document dom = toDOM(COMMON_TEST_XML);
        NodeModel.removePIs(dom);
        assertEquals(
                "<!DOCTYPE ...><a>%x<![CDATA[y]]><!--c-->%z<b><!--c--></b><c/>"
                + "<d>%a<e>%c</e>%b<!--c--><!--c--><!--c--></d>"
                + "<f><![CDATA[1]]>%2</f></a><!--c-->",
                toString(dom));
    }
    
    @Test
    public void testSimplify() throws Exception {
        testSimplify(
                "<!DOCTYPE ...><a>%xyz<b/><c/>"
                + "<d>%a<e>%c</e>%b</d><f><![CDATA[12]]></f></a>",
                COMMON_TEST_XML);
    }

    @Test
    public void testSimplify2() throws Exception {
        testSimplify(TEXT_MERGE_EXPECTED, TEXT_MERGE_CONTENT);
    }

    @Test
    public void testSimplify3() throws Exception {
        testSimplify("<a/>", "<a/>");
    }
    
    private void testSimplify(String expected, String content)
            throws SAXException, IOException, ParserConfigurationException {
        {
            Document dom = toDOM(content);
            NodeModel.simplify(dom);
            assertEquals(expected, toString(dom));
        }
        
        // Must be equivalent:
        {
            Document dom = toDOM(content);
            NodeModel.removeComments(dom);
            NodeModel.removePIs(dom);
            NodeModel.mergeAdjacentText(dom);
            assertEquals(expected, toString(dom));
        }
        
        // Must be equivalent:
        {
            Document dom = toDOM(content);
            NodeModel.removeComments(dom);
            NodeModel.removePIs(dom);
            NodeModel.simplify(dom);
            assertEquals(expected, toString(dom));
        }
    }

    private Document toDOM(String content) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder builder =  NodeModel.getDocumentBuilderFactory().newDocumentBuilder();
        ErrorHandler errorHandler =  NodeModel.getErrorHandler();
        if (errorHandler != null) builder.setErrorHandler(errorHandler);
        return builder.parse(toInputSource(content));
    }

    private InputSource toInputSource(String content) {
        return new InputSource(new StringReader(content));
    }

    private String toString(Document doc) {
        StringBuilder sb = new StringBuilder();
        toString(doc, sb);
        return sb.toString();
    }

    private void toString(Node node, StringBuilder sb) {
        if (node instanceof Document) {
            childrenToString(node, sb);
        } else if (node instanceof Element) {
            if (node.hasChildNodes()) {
                sb.append("<").append(node.getNodeName()).append(">");
                childrenToString(node, sb);
                sb.append("</").append(node.getNodeName()).append(">");
            } else {
                sb.append("<").append(node.getNodeName()).append("/>");
            }
        } else if (node instanceof Text) {
            if (node instanceof CDATASection) {
                sb.append("<![CDATA[").append(node.getNodeValue()).append("]]>");
            } else {
                sb.append("%").append(node.getNodeValue());
            }
        } else if (node instanceof Comment) {
            sb.append("<!--").append(node.getNodeValue()).append("-->");
        } else if (node instanceof ProcessingInstruction) {
            sb.append("<?").append(node.getNodeName()).append("?>");
        } else if (node instanceof DocumentType) {
            sb.append("<!DOCTYPE ...>");
        } else {
            throw new IllegalStateException("Unhandled node type: " + node.getClass().getName());
        }
    }

    private void childrenToString(Node node, StringBuilder sb) {
        Node child = node.getFirstChild();
        while (child != null) {
            toString(child, sb);
            child = child.getNextSibling();
        }
    }
    
}

/*
 * Copyright (c) 2005 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.test.templatesuite;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import freemarker.ext.dom.NodeModel;
import freemarker.template.utility.StringUtil;

/**
 * Test suite where the test cases are defined in testcases.xml, and usually process
 * templates and compare their output with the expected output.
 * 
 * If you only want to run certain tests, you can specify a regular expression for
 * the test name in the {@link #TEST_FILTER_PROPERTY_NAME} system property.
 */
public class TemplateTestSuite extends TestSuite {
    
    public static final String CONFIGURATION_XML_FILE_NAME = "testcases.xml";

    /**
     * When setting this system property, only the tests whose name matches the
     * given regular expression will be executed.
     */
    public static final String TEST_FILTER_PROPERTY_NAME = "freemareker.templateTestSuite.testFilter";
    
    private Map configParams = new LinkedHashMap();
    
    public static TestSuite suite() throws Exception {
        return new TemplateTestSuite();
    }
    
    public TemplateTestSuite() throws Exception {
        NodeModel.useJaxenXPathSupport();
        readConfig();
    }
    
    void readConfig() throws Exception {
        java.net.URL url = TemplateTestSuite.class.getResource(CONFIGURATION_XML_FILE_NAME);
        File f = new File(url.getFile());
        readConfig(f);
    }
    
    /**
     * Read the test case configurations file and build up the test suite.
     */
    public void readConfig(File f) throws Exception {
        String filterStr = System.getProperty(TEST_FILTER_PROPERTY_NAME);
        Pattern filter = filterStr != null ? Pattern.compile(filterStr) : null;
        if (filter != null) {
            System.out.println("Note: " + TEST_FILTER_PROPERTY_NAME + " is " + StringUtil.jQuote(filter));
        }
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(f);
        Element root = d.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals("config")) {
                    NamedNodeMap atts = n.getAttributes();
                    for (int j=0; j<atts.getLength(); j++) {
                        Attr att = (Attr) atts.item(j);
                        configParams.put(att.getName(), att.getValue());
                    }
                }
                if (n.getNodeName().equals("testcase")) {
                    TestCase tc = createTestCaseFromNode((Element) n, filter);
                    if (tc != null) addTest(tc);
                }
            }
        }
    }
    
    String getTextInElement(Element e) {
        StringBuilder buf = new StringBuilder();
        NodeList children = e.getChildNodes();
        for(int i=0; i<children.getLength(); i++) {
            Node n = children.item(i);
            short type = n.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                buf.append(n.getNodeValue());
            }
        }
        return buf.toString();
    }
    
    /**
     * Takes as input the DOM node that specifies the test case
     * and instantiates a {@link TestCase} or {@code null} if the test is
     * filtered out. If the class is not specified by the DOM node,
     * it defaults to {@link TemplateTestCase} class. If the class is specified,
     * it must extend {@link TestCase} and have a constructor with the same parameters as of
     * {@link TemplateTestCase#TemplateTestCase(String, String, String, boolean)}.
     */
    private TestCase createTestCaseFromNode(Element e, Pattern filter) throws Exception {
        String name = StringUtil.emptyToNull(e.getAttribute("name"));
        if (name == null) throw new Exception("Invalid XML: the \"name\" attribute is mandatory.");
        if (filter != null && !filter.matcher(name).matches()) return null;
        
        String templateName = StringUtil.emptyToNull(e.getAttribute("template"));
        if (templateName == null) templateName = name + ".ftl";

        String expectedFileName = StringUtil.emptyToNull(e.getAttribute("expected"));
        if (expectedFileName == null) expectedFileName = name + ".txt";
        
        String noOutputStr = StringUtil.emptyToNull(e.getAttribute("nooutput"));
        boolean noOutput = noOutputStr == null ? false : StringUtil.getYesNo(noOutputStr);
        
        String classname = StringUtil.emptyToNull(e.getAttribute("class"));
        
        if (classname != null) {
            Class cl = Class.forName(classname);
            Constructor cons = cl.getConstructor(new Class[] {
                    String.class, String.class, String.class, boolean.class});
            return (TestCase) cons.newInstance(new Object [] {
                    name, templateName, expectedFileName, Boolean.valueOf(noOutput)});
        } else { 
	        TemplateTestCase result = new TemplateTestCase(name, templateName, expectedFileName, noOutput);
	        for (Iterator it=configParams.entrySet().iterator(); it.hasNext();) {
	            Map.Entry entry = (Map.Entry) it.next();
	            result.setConfigParam(entry.getKey().toString(), entry.getValue().toString());
	        }
	        NodeList configs = e.getElementsByTagName("config");
	        for (int i=0; i<configs.getLength(); i++)  {
	            NamedNodeMap atts = configs.item(i).getAttributes();
	            for (int j=0; j<atts.getLength(); j++) {
	                Attr att = (Attr) atts.item(j);
	                result.setConfigParam(att.getName(), att.getValue());
	            }
	        }
	        return result;
        }
    }
    
    public static void main (String[] args) throws Exception {
        junit.textui.TestRunner.run(new TemplateTestSuite());
//       junit.swingui.TestRunner.run (TemplateTestSuite.class);
//        junit.awtui.TestRunner.run (TemplateTestSuite.class);
    }
}

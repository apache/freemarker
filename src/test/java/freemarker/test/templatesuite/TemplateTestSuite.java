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

package freemarker.test.templatesuite;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;

/**
 * Test suite where the test cases are defined in testcases.xml, and usually process
 * templates and compare their output with the expected output.
 * 
 * If you only want to run certain tests, you can specify a regular expression for
 * the test name in the {@link #TEST_FILTER_PROPERTY_NAME} system property.
 */
public class TemplateTestSuite extends TestSuite {
    
    private static final String ELEM_TEST_CASE = "testCase";

    private static final String ELEM_SETTING = "setting";

    private static final String ATTR_NO_OUTPUT = "noOutput";

    private static final String ATTR_EXPECTED = "expected";

    private static final String ATTR_TEMPLATE = "template";

    private static final String BASE_NAME_SEPARATOR = "|";

    public static final String CONFIGURATION_XML_FILE_NAME = "testcases.xml";

    /**
     * When setting this system property, only the tests whose name matches the
     * given regular expression will be executed.
     */
    public static final String TEST_FILTER_PROPERTY_NAME = "freemareker.templateTestSuite.testFilter";
    
    /**
     * Comma separated list of "incompatible improvements" versions to run the test cases with.
     */
    public static final String INCOMPATIBLE_IMPROVEMENTS_PROPERTY_NAME
            = "freemareker.templateTestSuite.incompatibleImprovements";
    
    private final Map<String, String> testSuiteSettings = new LinkedHashMap();
    
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
        
        List<Version> testedIcIs = new ArrayList<Version>();
        String testedIcIsStr = System.getProperty(INCOMPATIBLE_IMPROVEMENTS_PROPERTY_NAME);
        if (testedIcIsStr != null) {
            for (String iciStr : testedIcIsStr.split(",")) {
                iciStr = iciStr.trim();
                if (iciStr.length() != 0) {
                    testedIcIs.add(new Version(iciStr));
                }
            }
        }
        if (testedIcIs.isEmpty()) {
            testedIcIs.add(Configuration.VERSION_2_3_0);
            testedIcIs.add(Configuration.getVersion());
        }
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(f);
        Element root = d.getDocumentElement();
        NodeList children = root.getChildNodes();
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Node n = children.item(childIdx);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                final String nodeName = n.getNodeName();
                if (nodeName.equals(ELEM_SETTING)) {
                    NamedNodeMap attrs = n.getAttributes();
                    for (int attrIdx = 0; attrIdx < attrs.getLength(); attrIdx++) {
                        Attr attr = (Attr) attrs.item(attrIdx);
                        testSuiteSettings.put(attr.getName(), attr.getValue());
                    }
                } else if (nodeName.equals(ELEM_TEST_CASE)) {
                    boolean firstTestedIcI = true;
                    for (Version testedIcI : testedIcIs) {
                        TemplateTestCase tc = createTestCaseFromElement((Element) n, filter, testedIcI, firstTestedIcI);
                        if (tc != null) {
                            addTest(tc);
                            firstTestedIcI = false;
                        }
                    }
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
     * {@link TemplateTestCase#TemplateTestCase(String, String, String, boolean, Version)}.
     */
    private TemplateTestCase createTestCaseFromElement(Element testCaseElem, Pattern nameFilter,
            Version testedIcI, boolean firstTestedIcI) throws Exception {
        final String caseName = StringUtil.emptyToNull(testCaseElem.getAttribute("name"));
        if (caseName == null) throw new Exception("Invalid XML: the \"name\" attribute is mandatory.");
        if (nameFilter != null && !nameFilter.matcher(caseName).matches()) return null;
        
        final String templateName;
        final String expectedFileName;
        {
            final String baseName;
            final String namePostfix;
            {
                int nameBaseSep = caseName.indexOf(BASE_NAME_SEPARATOR);
                baseName = nameBaseSep == -1 ? caseName : caseName.substring(0, nameBaseSep);
                namePostfix = nameBaseSep == -1 ? "" : caseName.substring(nameBaseSep + 1);
            }
            
            {
                String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_TEMPLATE));
                templateName = s != null ? s : baseName + ".ftl";
            }
    
            {
                String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_EXPECTED));
                expectedFileName = s != null ? s : baseName + namePostfix + ".txt";
            }
        }
        
        final boolean noOutput;
        {
            String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_NO_OUTPUT));
            noOutput = s == null ? false : StringUtil.getYesNo(s);
        }

        final Map<String, String> testCaseSettings = getCaseFMSettings(testCaseElem);
        final boolean caseSpecifiesIcI = testCaseSettings.containsKey(Configuration.INCOMPATIBLE_IMPROVEMENTS)
                || testCaseSettings.containsKey(Configuration.INCOMPATIBLE_ENHANCEMENTS);
        
        if (caseSpecifiesIcI && !firstTestedIcI) {
            // We would just run the test case with the same IcI as before for the 2nd time.
            return null;
        }
        
        TemplateTestCase result = new TemplateTestCase(
                caseSpecifiesIcI ? caseName : caseName + "(ici=" + testedIcI + ")",
                templateName, expectedFileName, noOutput, testedIcI);
        
        for (Map.Entry<String, String> setting : testSuiteSettings.entrySet()) {
            result.setSetting(setting.getKey(), setting.getValue());
        }
        for (Map.Entry<String, String> setting : testCaseSettings.entrySet()) {
            result.setSetting(setting.getKey(), setting.getValue());
        }
        
        return result;
    }

    private Map<String, String> getCaseFMSettings(Element e) {
        final Map<String, String> caseFMSettings;
        caseFMSettings = new LinkedHashMap<String, String>();
        NodeList settingElems = e.getElementsByTagName(ELEM_SETTING);
        for (int elemIdx = 0; elemIdx < settingElems.getLength(); elemIdx++) {
            NamedNodeMap attrs = settingElems.item(elemIdx).getAttributes();
            for (int attrIdx = 0; attrIdx < attrs.getLength(); attrIdx++) {
                Attr attr = (Attr) attrs.item(attrIdx);

                final String settingName = attr.getName();
                caseFMSettings.put(settingName, attr.getValue());
            }
        }
        return caseFMSettings;
    }
    
    public static void main (String[] args) throws Exception {
        junit.textui.TestRunner.run(new TemplateTestSuite());
    }
    
}

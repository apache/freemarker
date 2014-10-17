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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestSuite;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

    private static final String END_TEMPLATE_NAME_MARK = "[#endTN]";

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

    private final ArrayList<Version> testSuiteIcis;

    private final Pattern testCaseNameFilter;
    
    public static TestSuite suite() throws Exception {
        return new TemplateTestSuite();
    }
    
    public TemplateTestSuite() throws Exception {
        NodeModel.useJaxenXPathSupport();
        
        String filterStr = System.getProperty(TEST_FILTER_PROPERTY_NAME);
        testCaseNameFilter = filterStr != null ? Pattern.compile(filterStr) : null;
        if (testCaseNameFilter != null) {
            System.out.println("Note: " + TEST_FILTER_PROPERTY_NAME + " is " + StringUtil.jQuote(testCaseNameFilter));
        }
        
        testSuiteIcis = new ArrayList<Version>();
        String testedIcIsStr = System.getProperty(INCOMPATIBLE_IMPROVEMENTS_PROPERTY_NAME);
        if (testedIcIsStr != null) {
            for (String iciStr : testedIcIsStr.split(",")) {
                iciStr = iciStr.trim();
                if (iciStr.length() != 0) {
                    testSuiteIcis.add(new Version(iciStr));
                }
            }
        }
        if (testSuiteIcis.isEmpty()) {
            testSuiteIcis.add(getMinIcIVersion());
            testSuiteIcis.add(getMaxIcIVersion());
        }
        
        java.net.URL url = TemplateTestSuite.class.getResource(CONFIGURATION_XML_FILE_NAME);
        if (url == null) {
            throw new IOException("Resource not found: "
                    + TemplateTestSuite.class.getName() + ", " + CONFIGURATION_XML_FILE_NAME);
        }
        processConfigXML(url.toURI());
    }
    
    /**
     * Read the test case configurations file and build up the test suite.
     */
    public void processConfigXML(URI uri) throws Exception {
        Element testCasesElem = loadXMLFromURL(uri);
        
        NodeList children = testCasesElem.getChildNodes();
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
                    for (TemplateTestCase testCase : createTestCasesFromElement((Element) n)) {
                        addTest(testCase);
                    }
                }
            }
        }
    }

    private Element loadXMLFromURL(URI uri) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(uri.toString());
        return d.getDocumentElement();
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
     * Returns the list of test cases generated from the {@link #ELEM_TEST_CASE} element.
     * There can be multiple generated test cases because of "incompatible improvements" variations, or none because
     * of the {@code nameFilter}.
     */
    private List<TemplateTestCase> createTestCasesFromElement(Element testCaseElem)
            throws Exception {
        final String caseName = StringUtil.emptyToNull(testCaseElem.getAttribute("name"));
        if (caseName == null) throw new Exception("Invalid XML: the \"name\" attribute is mandatory.");
        
        if (testCaseNameFilter != null
                && !testCaseNameFilter.matcher(caseName).matches()) {
            return Collections.emptyList();
        }
        
        final String templateName;
        final String expectedFileName;
        {
            final String beforeEndTN;
            final String afterEndTN;
            {
                int tBNameSep = caseName.indexOf(END_TEMPLATE_NAME_MARK);
                beforeEndTN = tBNameSep == -1 ? caseName : caseName.substring(0, tBNameSep);
                afterEndTN = tBNameSep == -1
                        ? "" : caseName.substring(tBNameSep + END_TEMPLATE_NAME_MARK.length());
            }
            
            {
                String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_TEMPLATE));
                templateName = s != null ? s : beforeEndTN + ".ftl";
            }
    
            {
                String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_EXPECTED));
                expectedFileName = s != null ? s : beforeEndTN + afterEndTN + ".txt";
            }
        }
        
        final boolean noOutput;
        {
            String s = StringUtil.emptyToNull(testCaseElem.getAttribute(ATTR_NO_OUTPUT));
            noOutput = s == null ? false : StringUtil.getYesNo(s);
        }

        final Map<String, String> testCaseSettings = getCaseFMSettings(testCaseElem);
        
        final List<Version> icisToTest;
        {
            final String testCaseIcis = testCaseSettings.get(Configuration.INCOMPATIBLE_IMPROVEMENTS) != null
                    ? testCaseSettings.get(Configuration.INCOMPATIBLE_IMPROVEMENTS)
                    : testCaseSettings.get(Configuration.INCOMPATIBLE_ENHANCEMENTS);
                    
            icisToTest = testCaseIcis != null ? parseVersionList(testCaseIcis) : testSuiteIcis;
            if (icisToTest.isEmpty()) {
                throw new Exception("The incompatible_improvement list was empty");
            }
        }

        List<TemplateTestCase> result = new ArrayList<TemplateTestCase>(); 
        for (Version iciToTest : icisToTest) {
            TemplateTestCase testCase = new TemplateTestCase(
                    caseName + "(ici=" + iciToTest + ")", caseName,
                    templateName, expectedFileName, noOutput, iciToTest);
            for (Map.Entry<String, String> setting : testSuiteSettings.entrySet()) {
                testCase.setSetting(setting.getKey(), setting.getValue());
            }
            for (Map.Entry<String, String> setting : testCaseSettings.entrySet()) {
                testCase.setSetting(setting.getKey(), setting.getValue());
            }
            
            result.add(testCase);
        }
        
        return result;
    }

    private List<Version> parseVersionList(String versionsStr) {
        List<Version> versions = new ArrayList<Version>();
        for (String versionStr : versionsStr.split(",")) {
            versionStr = versionStr.trim();
            if (versionStr.length() != 0) {
                final Version v;
                if ("min".equals(versionStr)) {
                    v = getMinIcIVersion();
                } else if ("max".equals(versionStr)) {
                    v = getMaxIcIVersion();
                } else {
                    v = new Version(versionStr);
                }
                if (!versions.contains(v)) {
                    versions.add(v);
                }
            }
        }
        return versions;
    }

    private Version getMaxIcIVersion() {
        Version v = Configuration.getVersion();
        // Remove nightly, RC and such:
        return new Version(v.getMajor(), v.getMinor(), v.getMicro());
    }

    private Version getMinIcIVersion() {
        return Configuration.VERSION_2_3_0;
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

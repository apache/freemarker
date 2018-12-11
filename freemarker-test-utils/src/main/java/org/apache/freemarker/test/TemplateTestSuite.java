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

package org.apache.freemarker.test;

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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.util._StringUtils;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import junit.framework.TestSuite;

/**
 * Abstract superclass for JUnit test suites where the test cases are defined in
 * {@code <suiteClassPackage>/testcases.xml}, and process templates and compare their output with the expected output.
 * The concrete subclass must have a static method like <code>public static TestSuite suite() { return new
 * SomeTemplateTestSuite(); }</code>!
 * <p>
 * If you only want to run certain tests, you can specify a regular expression for the test name in the
 * {@link #TEST_FILTER_PROPERTY_NAME} system property.
 * <p>
 * To add a test-case, go to the resource directory that corresponds to the package of the {@link TemplateTestSuite}
 * subclass and inside that directory:</p>
 * <ol>
 * <li>Add a new <tt>testcase</tt> element to <tt>testcases.xml</tt></li>
 * <li>Add a template to <tt>templates/</tt> with fits the <tt>testcase</tt> added to the XML (by default it's the test
 * case name + ".f3ac")</li>
 * <li>Add the expected output to <tt>references/</tt> with fits the <tt>testcase</tt> added to the XML (by default
 * it's the test name + ".txt")</li>
 * <li>If you want to add items to the data-model or change the {@link Configuration}, modify the
 * {@link #setUpTestCase(String, Map, Configuration.ExtendableBuilder)}} method in the {@link TemplateTestSuite}
 * subclass.</li>
 * </ol>
 */
@RunWith(AllTests.class)
public abstract class TemplateTestSuite extends TestSuite {
    
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
    
    private final Map<String, String> testSuiteSettings = new LinkedHashMap<>();

    private final ArrayList<Version> testSuiteIcis;

    private final Pattern testCaseNameFilter;

    public TemplateTestSuite() {
        try {
            String filterStr = System.getProperty(TEST_FILTER_PROPERTY_NAME);
            testCaseNameFilter = filterStr != null ? Pattern.compile(filterStr) : null;
            if (testCaseNameFilter != null) {
                System.out.println(
                        "Note: " + TEST_FILTER_PROPERTY_NAME + " is " + _StringUtils.jQuote(testCaseNameFilter));
            }

            testSuiteIcis = new ArrayList<>();
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

            java.net.URL url = getClass().getResource(CONFIGURATION_XML_FILE_NAME);
            if (url == null) {
                throw new IOException("Resource not found: "
                        + getClass().getName() + ", " + CONFIGURATION_XML_FILE_NAME);
            }
            processConfigXML(url.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize test suite", e);
        }
    }

    protected abstract void setUpTestCase(String simpleTestName, Map<String, Object> dataModel,
            Configuration.ExtendableBuilder<?> confB) throws Exception;

    /**
     * Read the test case configurations file and build up the test suite.
     */
    private void processConfigXML(URI uri) throws Exception {
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
        for (int i = 0; i < children.getLength(); i++) {
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
        final String caseName = _StringUtils.emptyToNull(testCaseElem.getAttribute("name"));
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
                String s = _StringUtils.emptyToNull(testCaseElem.getAttribute(ATTR_TEMPLATE));
                templateName = s != null ? s : beforeEndTN + ".f3ac";
            }
    
            {
                String s = _StringUtils.emptyToNull(testCaseElem.getAttribute(ATTR_EXPECTED));
                expectedFileName = s != null ? s : beforeEndTN + afterEndTN + ".txt";
            }
        }
        
        final boolean noOutput;
        {
            String s = _StringUtils.emptyToNull(testCaseElem.getAttribute(ATTR_NO_OUTPUT));
            noOutput = s != null && _StringUtils.getYesNo(s);
        }

        final Map<String, String> testCaseSettings = getCaseFMSettings(testCaseElem);
        
        final List<Version> icisToTest;
        {
            final String testCaseIcis = testCaseSettings.get(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY);
                    
            icisToTest = testCaseIcis != null ? parseVersionList(testCaseIcis) : testSuiteIcis;
            if (icisToTest.isEmpty()) {
                throw new Exception("The incompatible_improvement list was empty");
            }
        }

        List<TemplateTestCase> result = new ArrayList<>();
        for (Version iciToTest : icisToTest) {
            TemplateTestCase testCase = new TemplateTestCase(
                    this,
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
        List<Version> versions = new ArrayList<>();
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
        return Configuration.VERSION_3_0_0;
    }

    private Map<String, String> getCaseFMSettings(Element e) {
        final Map<String, String> caseFMSettings;
        caseFMSettings = new LinkedHashMap<>();
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

    /**
     * Override this if you want to check something in the main {@link Template} before running the test.
     */
    protected void validateTemplate(Template template) {
        // Does nothing by default
    }
}

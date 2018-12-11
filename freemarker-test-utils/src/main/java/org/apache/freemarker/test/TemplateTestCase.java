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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ConfigurationException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.test.templateutil.AssertDirective;
import org.apache.freemarker.test.templateutil.AssertEqualsDirective;
import org.apache.freemarker.test.templateutil.AssertFailsDirective;
import org.apache.freemarker.test.templateutil.NoOutputDirective;
import org.junit.Ignore;

import com.google.common.collect.ImmutableMap;

import junit.framework.AssertionFailedError;

/**
 * Instances of this are created and called by {@link TemplateTestSuite}. (It's on "Ignore" so that Eclipse doesn't try
 * to run this alone.) 
 */
@Ignore
class TemplateTestCase extends FileTestCase {
    
    // Name of variables exposed to all test FTL-s:
    private static final String ICI_INT_VALUE_VAR_NAME = "iciIntValue";
    private static final String TEST_NAME_VAR_NAME = "testName";
    private static final String NO_OUTPUT_VAR_NAME = "noOutput";
    private static final String ASSERT_FAILS_VAR_NAME = "assertFails";
    private static final String ASSERT_EQUALS_VAR_NAME = "assertEquals";
    private static final String ASSERT_VAR_NAME = "assert";

    private final TemplateTestSuite testSuite;

    private final String simpleTestName;
    private final String templateName;
    private final String expectedFileName;
    private final boolean noOutput;
    
    private final Configuration.ExtendableBuilder<?> confB;
    private final HashMap<String, Object> dataModel = new HashMap<>();
    
    TemplateTestCase(
            TemplateTestSuite testSuite,
            String testName, String simpleTestName, String templateName, String expectedFileName, boolean noOutput,
            Version incompatibleImprovements) {
        super(testName);
        _NullArgumentException.check("testName", testName);

        _NullArgumentException.check("testSuite", testSuite);
        this.testSuite = testSuite;
        
        _NullArgumentException.check("simpleTestName", simpleTestName);
        this.simpleTestName = simpleTestName;
        
        _NullArgumentException.check("templateName", templateName);
        this.templateName = templateName;
        
        _NullArgumentException.check("expectedFileName", expectedFileName);
        this.expectedFileName = expectedFileName;
        
        this.noOutput = noOutput;

        confB = new TestConfigurationBuilder(incompatibleImprovements);
    }
    
    void setSetting(String param, String value) throws IOException {
        if ("autoImports".equals(param)) {
            StringTokenizer st = new StringTokenizer(value);
            if (!st.hasMoreTokens()) fail("Expecting libname");
            String libname = st.nextToken();
            if (!st.hasMoreTokens()) fail("Expecting 'as <alias>' in autoimport");
            String as = st.nextToken();
            if (!as.equals("as")) fail("Expecting 'as <alias>' in autoimport");
            if (!st.hasMoreTokens()) fail("Expecting alias after 'as' in autoimport");
            String alias = st.nextToken();
            confB.setAutoImports(ImmutableMap.of(alias, libname));
        } else if ("sourceEncoding".equals(param)) {
            confB.setSourceEncoding(Charset.forName(value));
        // INCOMPATIBLE_IMPROVEMENTS is a list here, and was already set in the constructor.
        } else if (!Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY.equals(param)) {
            try {
                confB.setSetting(param, value);
            } catch (ConfigurationException e) {
                throw new RuntimeException(
                        "Failed to set setting " +
                        _StringUtils.jQuote(param) + " to " +
                        _StringUtils.jQuote(value) + "; see cause exception.",
                        e);
            }
        }
    }
    
    /*
     * This method just contains all the code to seed the data model 
     * ported over from the individual classes. This seems ugly and unnecessary.
     * We really might as well just expose pretty much 
     * the same tree to all our tests. (JR)
     */
    @Override
    @SuppressWarnings("boxing")
    protected void setUp() throws Exception {
        confB.setTemplateLoader(
                new CopyrightCommentRemoverTemplateLoader(
                        new ClassTemplateLoader(testSuite.getClass(), "templates")));
        
        dataModel.put(ASSERT_VAR_NAME, AssertDirective.INSTANCE);
        dataModel.put(ASSERT_EQUALS_VAR_NAME, AssertEqualsDirective.INSTANCE);
        dataModel.put(ASSERT_FAILS_VAR_NAME, AssertFailsDirective.INSTANCE);
        dataModel.put(NO_OUTPUT_VAR_NAME, NoOutputDirective.INSTANCE);

        dataModel.put(TEST_NAME_VAR_NAME, simpleTestName);
        dataModel.put(ICI_INT_VALUE_VAR_NAME, confB.getIncompatibleImprovements().intValue());

        testSuite.setUpTestCase(simpleTestName, dataModel, confB);
    }
    
    @Override
    protected void runTest() throws IOException, ConfigurationException {
        Template template;
        try {
            template = confB.build().getTemplate(templateName);
        } catch (IOException e) {
            throw new AssertionFailedError(
                    "Could not load template " + _StringUtils.jQuote(templateName) + ":\n" + getStackTrace(e));
        }

        testSuite.validateTemplate(template);

        StringWriter out = noOutput ? null : new StringWriter();
        try {
            template.process(dataModel, out != null ? out : _NullWriter.INSTANCE);
        } catch (TemplateException e) {
            throw new AssertionFailedError("Template " + _StringUtils.jQuote(templateName) + " has stopped with error:\n"
                        + getStackTrace(e));
        }
        
        if (out != null) {
            assertExpectedFileEqualsString(expectedFileName, out.toString());
        }
    }

    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    protected String getExpectedContentFileDirectoryResourcePath() throws IOException {
        return joinResourcePaths(super.getExpectedContentFileDirectoryResourcePath(), "expected");
    }

    @Override
    protected Charset getTestResourceDefaultCharset() {
        return confB.getOutputEncoding() != null ? confB.getOutputEncoding() : StandardCharsets.UTF_8;
    }

    @Override
    protected Class getTestResourcesBaseClass() {
        return testSuite.getClass();
    }


}

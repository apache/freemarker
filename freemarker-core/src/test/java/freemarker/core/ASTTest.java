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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

import freemarker.core.ASTPrinter.Options;
import freemarker.template.utility.StringUtil;
import freemarker.test.utility.FileTestCase;
import freemarker.test.utility.TestUtil;

@RunWith(JUnit38ClassRunner.class)
public class ASTTest extends FileTestCase {

    public ASTTest(String name) {
        super(name);
    }
    
    public void test1() throws Exception {
        testAST("ast-1");
    }

    public void testRange() throws Exception {
        testAST("ast-range");
    }
    
    public void testAssignments() throws Exception {
        testAST("ast-assignments");
    }
    
    public void testBuiltins() throws Exception {
        testAST("ast-builtins");
    }
    
    public void testStringLiteralInterpolation() throws Exception {
        testAST("ast-strlitinterpolation");
    }
    
    public void testWhitespaceStripping() throws Exception {
        testAST("ast-whitespacestripping");
    }

    public void testMixedContentSimplifications() throws Exception {
        testAST("ast-mixedcontentsimplifications");
    }

    public void testMultipleIgnoredChildren() throws Exception {
        testAST("ast-multipleignoredchildren");
    }
    
    public void testNestedIgnoredChildren() throws Exception {
        testAST("ast-nestedignoredchildren");
    }

    public void testLambda() throws Exception {
        testAST("ast-lambda");
    }

    public void testLocations() throws Exception {
        testASTWithLocations("ast-locations");
    }
    
    private void testAST(String testName) throws FileNotFoundException, IOException {
        testAST(testName, null);
    }

    private void testASTWithLocations(String testName) throws FileNotFoundException, IOException {
        Options options = new Options();
        options.setShowLocation(true);
        testAST(testName, options);
    }

    private void testAST(String testName, Options ops) throws FileNotFoundException, IOException {
        final String templateName = testName + ".ftl";
        assertExpectedFileEqualsString(
                testName + ".ast",
                ASTPrinter.getASTAsString(templateName,
                        TestUtil.removeFTLCopyrightComment(normalizeLineBreaks(loadResource(templateName))), ops));
    }
    
    private String normalizeLineBreaks(final String s) throws FileNotFoundException, IOException {
        return StringUtil.replace(s, "\r\n", "\n").replace('\r', '\n');
    }
    
}

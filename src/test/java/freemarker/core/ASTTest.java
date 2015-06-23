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

package freemarker.core;

import java.io.FileNotFoundException;
import java.io.IOException;

import freemarker.test.utility.FileTestCase;

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
        testAST("ast-whitesoacestripping");
    }

    public void testMixedContentSimplifications() throws Exception {
        testAST("ast-mixedcontentsimplifications");
    }
    
    private void testAST(String testName) throws FileNotFoundException, IOException {
        final String templateName = testName + ".ftl";
        assertExpectedFileEqualsString(
                testName + ".ast",
                ASTPrinter.getASTAsString(templateName, loadResource(templateName)));
    }
    
}

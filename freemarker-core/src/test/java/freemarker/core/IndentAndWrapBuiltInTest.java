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

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

/**
 * Checks indent/dedend/wrap built-ns; for the thorough testing of the text transformations see the
 * {@link _CoreStringUtilsTest}!
 */
public class IndentAndWrapBuiltInTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        return new Configuration(Configuration.VERSION_2_3_35);
    }

    @Test
    public void testIndentBasic() throws Exception {
        assertExpOutput("'line1\\nline2'?indent(' * ')", " * line1\n * line2");
    }

    @Test
    public void testIndent2Arg() throws Exception {
        assertExpOutput("'a\\n\\nb'?indent('# ', true)", "# a\n#\n# b");
        assertExpOutput("'a\\n\\nb'?indent('# ', false)", "# a\n# \n# b");
    }

    @Test
    public void testIndentRightTrimDefaultsToTrue() throws Exception {
        assertExpOutput("'a\\n\\nb'?indent('# ')", "# a\n#\n# b");
    }

    @Test
    public void testIndentBadNumberOfArgs() {
        assertErrorContains("${''?indent()}",  "?indent", "expects 1 or 2 arguments");
        assertErrorContains("${''?indent(1, 2, 3)}",  "?indent", "expects 1 or 2 arguments");
    }

    @Test
    public void testIndentArgTypeCoercion() {
        assertErrorContains("${''?indent(1)}", "string as argument #1");
        assertErrorContains("${''?indent(' ', 'yes')}", "boolean as argument #2");
    }

    @Test
    public void testWrap1Arg() throws Exception {
        assertExpOutput("'Hello world'?wrap(4)", "Hello\nworld\n");
        assertExpOutput("'Hello world'?wrap(40)", "Hello world\n");
    }

    @Test
    public void testWrap2Arg() throws Exception {
        assertExpOutput("'Hello world'?wrap(4, '* ')", "* Hello\n* world\n");
    }

    @Test
    public void testWrap3Args() throws Exception {
        assertExpOutput("'Hello world'?wrap(4, '* ', '  ')", "* Hello\n  world\n");
    }

    @Test
    public void testWrapNoArgTypeCoercion() throws Exception {
        assertErrorContains("${''?wrap(4, 1)}", "string as argument #2");
    }

    @Test
    public void testWrapBadNumberOfArgs() {
        assertErrorContains("${''?wrap()}",  "?wrap", "expects 1 to 3 arguments");
        assertErrorContains("${''?wrap(4, '*', '**', '***')}",  "?wrap", "expects 1 to 3 arguments");
    }

    @Test
    public void testWrapArg1AtLeast1() throws TemplateException, IOException {
        assertErrorContains("${''?wrap(0, '* ')}", "width", "at least 1");
        assertErrorContains("${''?wrap(-1, '* ')}", "width", "at least 1");
    }

    @Test
    public void testWrapBadArgTypeError() {
        assertErrorContains("${''?wrap('4', '*')}",  "number as argument #1");
    }

    @Test
    public void testDedent1Arg() throws Exception {
        assertExpOutput("'    int x;\\n    int y;\\n'?dedent('    ')", "int x;\nint y;\n");
        assertExpOutput("'  hello'?dedent('')", "  hello");
    }

    @Test
    public void testDedent0Arg() throws Exception {
        assertExpOutput("'  a\\n   b\\n  c'?dedent", "a\n b\nc");
    }

    @Test
    public void testDedent2Arg() throws Exception {
        assertExpOutput("'    a\\n     \\n    b'?dedent('    ', true)", "a\n\nb");
        assertExpOutput("'    a\\n     \\n    b'?dedent('    ', false)", "a\n \nb");
    }

    @Test
    public void testDedentRightTrimDefaultsToTrue() throws Exception {
        assertExpOutput("'    a\\n     \\n    b'?dedent('    ')", "a\n\nb");
    }

    @Test
    public void testDedentBadNumberOfArgs() {
        assertErrorContains("${''?dedent()}",  "?dedent", "expects 1 or 2 arguments");
        assertErrorContains("${''?dedent('  ', true, 3)}",  "?dedent", "expects 1 or 2 arguments");
    }

    @Test
    public void testDedentArgTypeCoercion() {
        assertErrorContains("${''?dedent(1)}", "string as argument #1");
        assertErrorContains("${''?dedent('  ', 2)}", "boolean as argument #2");
    }

    @Test
    public void testDedentNoArgTypeCoercion() throws Exception {
        assertErrorContains("${''?dedent(1)}", "string as argument #1");
    }

}

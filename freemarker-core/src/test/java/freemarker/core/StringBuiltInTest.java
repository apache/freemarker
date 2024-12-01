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
 * Note that there are much more string built-in tests in {@code string-builtins1.ftl} and such, as part of the
 * {@code TemplateTestSuite}, but now we prefer doing new tests like this.
 */
public class StringBuiltInTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setNumberFormat(",##0.###");
        return cfg;
    }

    @Test
    public void testBlankToNull() throws IOException, TemplateException {
    	assertOutput("${nonExisting?blank_to_null!'-'}", "-");
    	assertOutput("${nonExisting!?blank_to_null!'-'}", "-");
        assertOutput("${''?blank_to_null!'-'}", "-");
        assertOutput("${' '?blank_to_null!'-'}", "-");
        assertOutput("${'  a  '?blank_to_null!'-'}", "  a  ");
        assertOutput("${'a '?blank_to_null!'-'}", "a ");
        assertOutput("${' a'?blank_to_null!'-'}", " a");
        assertOutput("${'a'?blank_to_null!'-'}", "a");
        assertOutput("${'a b'?blank_to_null!'-'}", "a b");

        assertOutput("${(nonExisting + '.')?blank_to_null!'-'}", "-");

        assertOutput("${1234?blank_to_null!'-'}", "1,234");

        // No consistent with ?trim (and String.trim()), as all UNICODE whitespace count as whitespace:
        assertOutput("${' \u2003  '?blank_to_null!'-'}", "-");
        assertOutput("${' \u00A0  '?blank_to_null!'-'}", "-"); // Even if it's non-breaking whitespace

        // Camel case:
        assertOutput("${nonExisting?blankToNull!'-'}", "-");
    }

    @Test
    public void blankToNullTypeError() {
        assertErrorContains("${[]?blank_to_null!'-'}",
                "For \"?blank_to_null\" left-hand operand: Expected a string", "sequence");
        assertErrorContains("<#assign html></#assign>${html?blank_to_null!'-'}",
                "For \"?blank_to_null\" left-hand operand: Expected a string", "TemplateHTMLOutputModel");
    }

    @Test
    public void testTrimToNull() throws IOException, TemplateException {
    	assertOutput("${nonExisting?trim_to_null!'-'}", "-");
    	assertOutput("${nonExisting!?trim_to_null!'-'}", "-");
        assertOutput("${''?trim_to_null!'-'}", "-");
        assertOutput("${' '?trim_to_null!'-'}", "-");
        assertOutput("${'    '?trim_to_null!'-'}", "-");
        assertOutput("${'  a  '?trim_to_null!'-'}", "a");
        assertOutput("${'a '?trim_to_null!'-'}", "a");
        assertOutput("${' a'?trim_to_null!'-'}", "a");
        assertOutput("${'a'?trim_to_null!'-'}", "a");
        assertOutput("${'a b'?trim_to_null!'-'}", "a b");

        assertOutput("${(nonExisting + '.')?trim_to_null!'-'}", "-");

        assertOutput("${1234?trim_to_null!'-'}", "1,234");

        // To be consistent with ?trim (and String.trim()), only char <= 32 is whitespace, not all UNICODE whitespace:
        assertOutput("${'  \u2003  '?trim_to_null!'-'}", "\u2003");

        // Camel case:
        assertOutput("${nonExisting?trimToNull!'-'}", "-");
    }

    @Test
    public void trimToNullTypeError() {
        assertErrorContains("${[]?trim_to_null!'-'}",
                "For \"?trim_to_null\" left-hand operand: Expected a string", "sequence");
        assertErrorContains("<#assign html></#assign>${html?trim_to_null!'-'}",
                "For \"?trim_to_null\" left-hand operand: Expected a string", "TemplateHTMLOutputModel");
    }

    @Test
    public void emptyToNull() throws IOException, TemplateException {
    	assertOutput("${nonExisting?empty_to_null!'-'}", "-");
    	assertOutput("${nonExisting!?empty_to_null!'-'}", "-");
        assertOutput("${''?empty_to_null!'-'}", "-");
        assertOutput("${' '?empty_to_null!'-'}", " ");
        assertOutput("${'    '?empty_to_null!'-'}", "    ");
        assertOutput("${'  a  '?empty_to_null!'-'}", "  a  ");
        assertOutput("${'a '?empty_to_null!'-'}", "a ");
        assertOutput("${' a'?empty_to_null!'-'}", " a");
        assertOutput("${'a'?empty_to_null!'-'}", "a");
        assertOutput("${'a b'?empty_to_null!'-'}", "a b");

        assertOutput("${(nonExisting + '.')?empty_to_null!'-'}", "-");

        assertOutput("${1234?empty_to_null!'-'}", "1,234");

        // Camel case:
        assertOutput("${nonExisting?emptyToNull!'-'}", "-");
    }

    @Test
    public void emptyToNullTypeError() {
        assertErrorContains("${[]?empty_to_null!'-'}",
                "For \"?empty_to_null\" left-hand operand: Expected a string", "sequence");
        assertErrorContains("<#assign html></#assign>${html?empty_to_null!'-'}",
                "For \"?empty_to_null\" left-hand operand: Expected a string", "TemplateHTMLOutputModel");
    }

}
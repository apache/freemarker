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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

@RunWith(Parameterized.class)
public class LinesBuiltInTest extends TemplateTest {

    private final String lineBreak;

    public LinesBuiltInTest(String lineBreakName, String lineBreak) {
        this.lineBreak = lineBreak;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[]{"LF", "\n"},
                new Object[]{"CRLF", "\r\n"},
                new Object[]{"CR", "\r"});
    }

    @Override
    protected Configuration createConfiguration() throws Exception {
        return new Configuration(Configuration.VERSION_2_3_35);
    }

    private String lb(String s) {
        return s.replace("\n", lineBreak);
    }

    private void assertLinesOutput(String input, String expectedOutput) throws TemplateException, IOException {
        addToDataModel("s", lb(input));
        assertOutput(
                "[<#list s?lines as line>'${line}'<#sep>, </#list>]",
                expectedOutput);
    }

    @Test
    public void testBasics() throws Exception {
        assertLinesOutput("", "[]");

        assertLinesOutput("\n", "['']");

        assertLinesOutput("L1", "['L1']");
        assertLinesOutput("L1\n", "['L1']");

        assertLinesOutput("L1\nL2", "['L1', 'L2']");
        assertLinesOutput("L1\nL2\n", "['L1', 'L2']");

        assertLinesOutput("1\n2\n3", "['1', '2', '3']");
        assertLinesOutput("1\n2\n3\n", "['1', '2', '3']");
    }

    @Test
    public void testEmptyLines() throws Exception {
        assertLinesOutput("L1", "['L1']");
        assertLinesOutput("L1\n", "['L1']");
        assertLinesOutput("L1\n\n", "['L1', '']");
        assertLinesOutput("L1\n\n\n", "['L1', '', '']");

        assertLinesOutput("L1\nL2", "['L1', 'L2']");
        assertLinesOutput("L1\n\nL2", "['L1', '', 'L2']");
        assertLinesOutput("L1\n\n\nL2", "['L1', '', '', 'L2']");

        assertLinesOutput("\nL1\nL2", "['', 'L1', 'L2']");
        assertLinesOutput("\n\nL1\nL2", "['', '', 'L1', 'L2']");
        assertLinesOutput("\n\n\nL1\nL2", "['', '', '', 'L1', 'L2']");
    }

    @Test
    public void testNoTrimming() throws Exception {
        assertLinesOutput(" a \n\tb\nc \n \n", "[' a ', '\tb', 'c ', ' ']");
    }

    @Test
    public void testExampleUseCase() throws Exception {
        addToDataModel("s", "a\n\nbb\nccc\n");
        assertOutput(
                "<#list s?lines as line>${line?right_pad(3)}|\n</#list>",
                """
                        a  |
                           |
                        bb |
                        ccc|
                        """);
    }
}

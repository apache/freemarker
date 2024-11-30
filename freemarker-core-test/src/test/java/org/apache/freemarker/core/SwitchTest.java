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

package org.apache.freemarker.core;

import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

import java.io.IOException;

/**
 * There are more (older) test in "switch-builtin.ftl", but we prefer creating new ones in Java.
 */
public class SwitchTest extends TemplateTest {

    @Test
    public void testCaseBasics() throws TemplateException, IOException {
        testCaseBasics(true);
        testCaseBasics(false);
    }

    private void testCaseBasics(boolean hasDefault) throws TemplateException, IOException {
        for (int i = 1 ; i <= 6; i++) {
            assertOutput(
                    "[<#switch " + i + ">\n"
                            + "<#case 3>Case 3<#break>"
                            + "<#case 1>Case 1<#break>"
                            + "<#case 4><#case 5>Case 4 or 5<#break>"
                            + "<#case 2>Case 2<#break>"
                            + (hasDefault ? "<#default>D" : "")
                            + "</#switch>]",
                    i < 6
                            ? "[Case " + (i < 4 ? i : "4 or 5") + "]"
                            : (hasDefault ? "[D]" : "[]"));
        }
    }

    @Test
    public void testDefaultOnly() throws TemplateException, IOException {
        assertOutput("<#switch 1><#default>D</#switch>", "D");
        assertOutput("<#list 1..2 as i><#switch 1><#default>D<#break>unreachable</#switch></#list>", "DD");
    }

    @Test
    public void testCaseWhitespace() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "<#list 1..3 as i>\n"
                        + "[\n"  // <#----> is to avoid unrelated old white-space stripping bug
                        + "  <#switch i>\n"
                        + "    <#case 1>C1\n"
                        + "    <#case 2>C2<#break>\n"
                        + "    <#default>D\n"
                        + "  </#switch>\n"
                        + "]\n"
                        + "</#list>",
                "[\nC1\n    C2]\n[\nC2]\n[\nD\n]\n");
    }

    @Test
    public void testOn() throws TemplateException, IOException {
        testOnBasics(true);
        testOnBasics(false);
    }

    private void testOnBasics(boolean hasDefault) throws TemplateException, IOException {
        for (int i = 1 ; i <= 6; i++) {
            assertOutput(
                    "[<#switch " + i + ">\n"
                            + "<#on 3>On 3"
                            + "<#on 1>On 1"
                            + "<#on 4, 5>On 4 or 5"
                            + "<#on 2>On 2"
                            + (hasDefault ? "<#default>D" : "")
                            + "</#switch>]",
                    i < 6
                            ? "[On " + (i < 4 ? i : "4 or 5") + "]"
                            : (hasDefault ? "[D]" : "[]"));
        }
    }

    @Test
    public void testParsingErrors() {
        assertErrorContains(
                "<#list 1..3 as i><#switch i><#case 1>1<#default>D<#case 3>3</#switch>;</#list>",
                ParseException.class,
                "#case directive after the #default");
    }

    @Test
    public void testOnParsingErrors() {
        assertErrorContains(
                "<#switch x><#on 1>On 1<#default>D<#on 2>On 2</#switch>",
                ParseException.class,
                "#on after #default");
        assertErrorContains(
                "<#switch x><#on 1>On 1<#case 2>On 2</#switch>",
                ParseException.class,
                "can't use both #on, and #case", "already had an #on");
        assertErrorContains(
                "<#switch x><#case 1>On 1<#on 2>On 2</#switch>",
                ParseException.class,
                "can't use both #on, and #case", "already had a #case");
        assertErrorContains(
                "<#switch x><#on 1>On 1<#default>D<#case 2>On 2</#switch>",
                ParseException.class,
                "can't use both #on, and #case", "already had an #on");
        assertErrorContains(
                "<#switch x><#on 1>On 1<#default>D1<#default>D2</#switch>",
                ParseException.class,
                "already had a #default");
        assertErrorContains(
                "<#switch x><#case 1>On 1<#default>D1<#default>D2</#switch>",
                ParseException.class,
                "already had a #default");
        assertErrorContains(
                "<#switch x><#on 1>On 1<#default>D<#on 2>On 2</#switch>",
                ParseException.class,
                "#on after #default");
        assertErrorContains(
                "<#switch x><#default>D<#on 2>On 2</#switch>",
                ParseException.class,
                "#on after #default");
    }

    @Test
    public void testOnWhitespace() throws TemplateException, IOException {
        assertOutput(
                ""
                        + "<#list 1..3 as i>\n"
                        + "[\n"  // <#----> is to avoid unrelated old white-space stripping bug
                        + "  <#switch i>\n"
                        + "    <#on 1>C1\n"
                        + "    <#on 2>C2\n"
                        + "    <#default>D\n"
                        + "  </#switch>\n"
                        + "]\n"
                        + "</#list>",
                "[\nC1\n    ]\n[\nC2\n    ]\n[\nD\n]\n");
        assertOutput(
                ""
                        + "<#list 1..3 as i>\n"
                        + "[\n"  // <#----> is to avoid unrelated old white-space stripping bug
                        + "  <#switch i>\n"
                        + "    <#on 1>C1<#t>\n"
                        + "    <#on 2>C2<#t>\n"
                        + "    <#default>D<#t>\n"
                        + "  </#switch>\n"
                        + "]\n"
                        + "</#list>",
                "[\nC1]\n[\nC2]\n[\nD]\n");
        assertOutput(
                ""
                        + "<#list 1..3 as i>\n"
                        + "[\n"  // <#----> is to avoid unrelated old white-space stripping bug
                        + "  <#switch i>\n"
                        + "    <#on 1>\n"
                        + "      C1\n"
                        + "    <#on 2>\n"
                        + "      C2\n"
                        + "    <#default>\n"
                        + "      D\n"
                        + "  </#switch>\n"
                        + "]\n"
                        + "</#list>",
                "[\n      C1\n]\n[\n      C2\n]\n[\n      D\n]\n");
    }

}

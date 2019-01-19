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

import java.io.IOException;
import java.util.LinkedList;

import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class ParsingErrorMessagesTest extends TemplateTest {

    @Test
    public void testNeedlessInterpolation() {
        assertErrorContainsAS("<#if ${x} == 3></#if>", "instead of ${");
        assertErrorContainsAS("<#if ${x == 3}></#if>", "instead of ${");
        assertErrorContainsAS("<@foo ${x == 3} />", "instead of ${");
        
        setConfiguration(new TestConfigurationBuilder()
                .templateLanguage(DefaultTemplateLanguage.F3SU));
        assertErrorContains("[@foo [= x == 3] /]", "instead of [=");
    }

    @Test
    public void testWrongDirectiveNames() {
        assertErrorContainsAS("<#foo />", "nknown directive", "#foo");
        assertErrorContainsAS("<#set x = 1 />", "nknown directive", "#set", "#assign");
        assertErrorContainsAS("<#iterator></#iterator>", "nknown directive", "#iterator", "#list");
        assertErrorContainsAS("<#if x><#elseif y></#if>", "nknown directive", "#elseIf");
        assertErrorContainsAS("<#outputformat 'HTML'></#outputformat>", "nknown directive", "#outputFormat");
        assertErrorContainsAS("<#autoesc></#autoesc>", "nknown directive", "#autoEsc");
        assertErrorContainsAS("<#noautoesc></#noautoesc>", "nknown directive", "#noAutoEsc");
    }

    @Test
    public void testBug402() {
        assertErrorContainsAS("<#list 1..i as k>${k}<#list>", "existing directive", "malformed", "#list");
        assertErrorContainsAS("<#assign>", "existing directive", "malformed", "#assign");
        assertErrorContainsAS("</#if x>", "existing directive", "malformed", "#if");
        assertErrorContainsAS("<#compress x>", "existing directive", "malformed", "#compress");
    }

    @Test
    public void testUnclosedDirectives() {
        assertErrorContainsAS("<#macro x>", "#macro", "unclosed");
        assertErrorContainsAS("<#macro x></#function>", "macro end tag");
        assertErrorContainsAS("<#function x()>", "#macro", "unclosed");
        assertErrorContainsAS("<#function x()></#macro>", "function end tag");
        assertErrorContainsAS("<#assign x>", "#assign", "unclosed");
        assertErrorContainsAS("<#macro m><#local x>", "#local", "unclosed");
        assertErrorContainsAS("<#global x>", "#global", "unclosed");
        assertErrorContainsAS("<@foo>", "@...", "unclosed");
        assertErrorContainsAS("<#list xs as x>", "#list", "unclosed");
        assertErrorContainsAS("<#list xs as x><#if x>", "#if", "unclosed");
        assertErrorContainsAS("<#list xs as x><#if x><#if q><#else>", "#if", "unclosed");
        assertErrorContainsAS("<#list xs as x><#if x><#if q><#else><#macro x>qwe", "#macro", "unclosed");
        assertErrorContainsAS("${(blah", "\"(\"", "unclosed");
        assertErrorContainsAS("${blah", "\"{\"", "unclosed");
    }

    @Test
    public void testBuiltInWrongNames() {
        assertErrorContainsAS("${x?lower_case}", "camel case", "The correct name is: lowerCase");
        assertErrorContainsAS("${x?iso_utc_nz}", "camel case", "The correct name is: isoUtcNZ");
        assertErrorContainsAS("${x?no_such_name}", "camel case", "\\!The correct name is:", "alphabetical list");
        assertErrorContainsAS("${x?nosuchname}", "\\!camel case", "\\!The correct name is:", "alphabetical list");
        assertErrorContainsAS("${x?datetime}", "The correct name is: dateTime");
        assertErrorContainsAS("${x?datetimeIfUnknown}", "The correct name is: dateTimeIfUnknown");
        assertErrorContainsAS("${x?datetime_if_unknown}", "The correct name is: dateTimeIfUnknown");
        assertErrorContainsAS("${x?exists}", "someExpression??");
        assertErrorContainsAS("${x?if_exists}", "someExpression!");
        assertErrorContainsAS("${x?default(1)}", "someExpression!defaultExpression");
    }

    @Test
    public void testSettingWrongNames() {
        assertErrorContainsAS("<#setting time_format='HHmm'>", "camel case", "The correct name is: timeFormat",
                "\\!setting names are:");
        assertErrorContainsAS("<#setting no_such_name=1>", "camel case", "\\!The correct name is:",
                "setting names are:");
        assertErrorContainsAS("<#setting nosuchname=1>", "\\!The correct name is:", "\\!camel case",
                "setting names are:");

        assertErrorContainsAS("<#setting datetime_format='HHmm'>", "The correct name is: dateTimeFormat");
        assertErrorContainsAS("<#setting datetimeFormat='HHmm'>", "The correct name is: dateTimeFormat");
    }

    @Test
    public void testSpecialVariableWrongNames() {
        assertErrorContainsAS("${.data_model}", "camel case", "The correct name is: dataModel",
                "\\!variable names are:");
        assertErrorContainsAS("${.no_such_name}", "camel case", "\\!The correct name is:",
                "variable names are:");
        assertErrorContainsAS("${.nosuchname}", "\\!camel case", "\\!The correct name is:",
                "variable names are:");
    }

    @Test
    public void testFtlParameterWrongNames() {
        assertErrorContainsAS("<#ftl strip_whitespace=false>", "camel case", "The correct name is: stripWhitespace");
        assertErrorContainsAS("<#ftl no_such_name=1>", "camel case", "\\!The correct name is:");
        assertErrorContainsAS("<#ftl nosuchname=1>", "\\!camel case", "\\!The correct name is:");
    }

    @Test
    public void testInterpolatingClosingsErrors() throws Exception {
        assertErrorContainsAS("<#ftl>${x", "unclosed");        
        assertErrorContainsAS("<#assign x = x}>", "\"}\"", "open");
        assertErrorContains("${'x']", "\"]\"", "open");
        assertErrorContains("${'x'>", "end of file");
        
        assertOutput("<#assign x = '${x'>", ""); // TODO [FM3] Legacy glitch... should fail in theory.
    }

    @Test
    public void testUnknownHeaderParameter() {
        assertErrorContainsAS("<#ftl foo=1>", "Unknown", "foo");
        assertErrorContainsAS("<#ftl attributes={}>", "Unknown", "attributes", "customSettings");
    }

    @Test
    public void testDynamicTopCalls() throws IOException, TemplateException {
        assertErrorContainsAS("<@a, n1=1 />", "Remove comma", "between", "by position");
        assertErrorContainsAS("<@a n1=1, n2=1 />", "Remove comma", "between", "by position");
        assertErrorContainsAS("<@a n1=1, 2 />", "Remove comma", "between", "by position");
        assertErrorContainsAS("<@a, 1 />", "Remove comma", "between", "by position");
        assertErrorContainsAS("<@a 1, , 2 />", "Two commas");
        assertErrorContainsAS("<@a 1 2 />", "Missing comma");
        assertErrorContainsAS("<@a n1=1 2 />", "must be earlier than arguments passed by name");
    }

    @Test
    public void testMacroAndFunctionDefinitions() {
        assertErrorContainsAS("<#macro m><#macro n></#macro></#macro>", "nested into each other");
        assertErrorContainsAS("<#macro m(a)></#macro>", "can't use \"(\"");
        assertErrorContainsAS("<#function f a></#function>", "must use \"(\"");
        assertErrorContainsAS("<#macro m a{badOption})></#macro>", "\"badOption\"",
                "\"" + ASTDirMacroOrFunction.POSITIONAL_PARAMETER_OPTION_NAME + "\"",
                "\"" + ASTDirMacroOrFunction.NAMED_PARAMETER_OPTION_NAME + "\"");
        assertErrorContainsAS("<#function f(a{named}, b)></#function>", "Positional", "must precede named");
        assertErrorContainsAS("<#function f(a..., b)></#function>", "another", "after", "positional varargs");
        assertErrorContainsAS("<#function f(a..., b...)></#function>", "another", "after", "positional varargs");
        assertErrorContainsAS("<#macro m a... b></#macro>", "another", "after", "named varargs");
        assertErrorContainsAS("<#macro m a... b...></#macro>", "another", "after", "named varargs");
        assertErrorContainsAS("<#function f(a b)></#function>", "Function", "must have comma");
        assertErrorContainsAS("<#function f(a{named} b{named})></#function>", "Function", "must have comma");
        assertErrorContainsAS("<#macro m a, b></#macro>", "Named param", "macro", "need no comma");
        assertErrorContainsAS("<#macro m a{positional} b{positional}></#macro>",
                "Positional param", "must have comma");
        assertErrorContainsAS("<#macro m a...=[]></#macro>", "Varargs", "default");
        assertErrorContainsAS("<#function f(a=0, b)></#function>", "with default", "without a default");
        assertErrorContainsAS("<#function f(a,)></#function>", "Comma without");
        assertErrorContainsAS("<#macro m a{positional}, b{positional},></#macro>", "Comma without");
        assertErrorContainsAS("<#function f(a, b></#function>",
                    new String[] { ">" }, new String[] { "Missing closing \")\"" });
        assertErrorContainsAS("<#function f(></#function>",
                    new String[] { ">" }, new String[] { "Missing closing \")\"" });
        assertErrorContainsAS("<#macro m a b)></#macro>", "\")\" without", "opening");
        assertErrorContainsAS("<#macro m a b a></#macro>", "\"a\"", "multiple");
    }

    /**
     * "assertErrorContains" with both angle bracket and square bracket tag syntax, by converting the input tag syntax.
     * Beware, it uses primitive search-and-replace.
     */
    protected Throwable assertErrorContainsAS(String angleBracketsFtl, String... expectedSubstrings) {
        return assertErrorContainsAS(angleBracketsFtl, expectedSubstrings, expectedSubstrings);
    }
    
    protected Throwable assertErrorContainsAS(String f3aSrc,
            String[] expectedSubstringsA, String[] expectedSubstringsS) {
        pushNamelessTemplateConfiguraitonSettings(new TemplateConfiguration.Builder()
                .templateLanguage(DefaultTemplateLanguage.F3AU)
                .build());
        try {
            assertErrorContains(f3aSrc, expectedSubstringsA);
        } finally {
            popNamelessTemplateConfiguraitonSettings();
        }
        
        pushNamelessTemplateConfiguraitonSettings(new TemplateConfiguration.Builder()
                .templateLanguage(DefaultTemplateLanguage.F3SU)
                .build());
        try {
            return assertErrorContains(f3aToF3s(f3aSrc));
        } finally {
            popNamelessTemplateConfiguraitonSettings();
        }
    }

    /**
     * Very naive F3A to F3S conversion (incorrect, but good enough for this test).
     */
    private String f3aToF3s(String f3aSrc) {
        StringBuilder sb = new StringBuilder();
        LinkedList<Character> openingStack = new LinkedList<>();
        for (int i = 0; i < f3aSrc.length(); i++) {
            char c = f3aSrc.charAt(i);
            if (c == '<') {
                char cNext = i + 1 < f3aSrc.length() ? f3aSrc.charAt(i + 1) : 0; 
                if (cNext == '/' || cNext == '#' || cNext == '@') {
                    sb.append("[");
                    openingStack.push('A');
                } else {
                    sb.append(c);
                    openingStack.push(c);
                }
            } else if (c == '{') {
                if (i > 0 && f3aSrc.charAt(i - 1) == '$') {
                    sb.deleteCharAt(i - 1);
                    sb.append("[=");
                    openingStack.push('D');
                } else {
                    sb.append(c);
                    openingStack.push(c);
                }
            } else if (c == '>') {
                Character top = openingStack.peek();
                if (top != null && top == '<' || top == 'A') {
                    openingStack.pop();
                    sb.append(top == 'A' ? ']' : c);
                } else {
                    sb.append(c);
                }
            } else if (c == '}') {
                Character top = openingStack.peek();
                if (top != null && top == '{' || top == 'D') {
                    openingStack.pop();
                    sb.append(top == 'D' ? ']' : c);
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}

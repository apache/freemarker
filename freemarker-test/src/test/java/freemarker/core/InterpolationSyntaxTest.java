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

import static freemarker.template.Configuration.*;
import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.test.TemplateTest;

public class InterpolationSyntaxTest extends TemplateTest {

    @Test
    public void legacyInterpolationSyntaxTest() throws Exception {
        // The default is: getConfiguration().setInterpolationSyntax(Configuration.LEGACY_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "1 1 [=1]");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "1 1 [={'x': 1}['x']]");
        
        assertOutput("${'a[=1]b'}", "a[=1]b");
        assertOutput("${'a${1}#{2}b'}", "a12b");
        assertOutput("${'a${1}#{2}b[=3]'}", "a12b[=3]");
        
        assertOutput("<@r'${1} #{1} [=1]'?interpret />", "1 1 [=1]");
        assertOutput("${'\"${1} #{1} [=1]\"'?eval}", "1 1 [=1]");
        
        assertOutput("<#setting booleanFormat='y,n'>${2>1}", "y"); // Not an error since 2.3.28
        assertOutput("[#ftl][#setting booleanFormat='y,n']${2>1}", "y"); // Not an error since 2.3.28
    }

    @Test
    public void dollarInterpolationSyntaxTest() throws Exception {
        getConfiguration().setInterpolationSyntax(DOLLAR_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "1 #{1} [=1]");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "1 #{{'x': 1}['x']} [={'x': 1}['x']]");
        
        assertOutput("${'a[=1]b'}", "a[=1]b");
        assertOutput("${'a${1}#{2}b'}", "a1#{2}b");
        assertOutput("${'a${1}#{2}b[=3]'}", "a1#{2}b[=3]");
        
        assertOutput("<@r'${1} #{1} [=1]'?interpret />", "1 #{1} [=1]");
        assertOutput("${'\"${1} #{1} [=1]\"'?eval}", "1 #{1} [=1]");
    }

    @Test
    public void squareBracketInterpolationSyntaxTest() throws Exception {
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        
        assertOutput("${1} #{1} [=1]", "${1} #{1} 1");
        assertOutput(
                "${{'x': 1}['x']} #{{'x': 1}['x']} [={'x': 1}['x']]",
                "${{'x': 1}['x']} #{{'x': 1}['x']} 1");

        assertOutput("[=1]][=2]]", "1]2]");
        assertOutput("[= 1 ][= <#-- c --> 2 <#-- c --> ]", "12");
        assertOutput("[ =1]", "[ =1]");
        
        // Legacy tag closing glitch is not emulated with this:
        assertErrorContains("<#if [true][0]]></#if>", "\"]\"", "nothing open");
        
        getConfiguration().setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
        assertOutput("[#if [true][0]]>[/#if]", ">");
        assertOutput("[=1][=2]${3}", "12${3}");
        getConfiguration().setTagSyntax(ANGLE_BRACKET_TAG_SYNTAX);
        assertOutput("[#ftl][#if [true][0]]>[/#if]", ">");
        assertOutput("[#ftl][=1][=2]${3}", "12${3}");
        
        assertOutput("[='a[=1]b']", "a1b");
        assertOutput("[='a${1}#{2}b']", "a${1}#{2}b");
        assertOutput("[='a${1}#{2}b[=3]']", "a${1}#{2}b3");
        
        assertOutput("<@r'${1} #{1} [=1]'?interpret />", "${1} #{1} 1");
        assertOutput("[='\"${1} #{1} [=1]\"'?eval]", "${1} #{1} 1");
        
        assertErrorContains("[=", "end of file");
        assertErrorContains("[=1", "unclosed \"[\"");

        assertOutput("<#setting booleanFormat='y,n'>[=2>1]", "y");
        assertOutput("[#ftl][#setting booleanFormat='y,n'][=2>1]", "y");
        
        assertOutput("[='[\\=1]']", "[=1]");
        assertOutput("[='[\\=1][=2]']", "12"); // Usual legacy interpolation escaping glitch...
        assertOutput("[=r'[=1]']", "[=1]");
        
        StringWriter sw = new StringWriter();
        new Template(null, "[= 1 + '[= 2 ]' ]", getConfiguration()).dump(sw);
        assertEquals("[=1 + \"[=2]\"]", sw.toString());
    }

    @Test
    public void squareBracketTagSyntaxStillWorks() throws Exception {
        getConfiguration().setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
        for (int syntax : new int[] {
                LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX, SQUARE_BRACKET_INTERPOLATION_SYNTAX }) {
            assertOutput("[#if [true][0]]t[#else]f[/#if]", "t");
            assertOutput("[@r'[#if [true][0]]t[#else]f[/#if]'?interpret /]", "t");
        }
    }
    
    @Test
    public void legacyTagSyntaxGlitchStillWorksTest() throws Exception {
        String badFtl1 = "<#if [true][0]]OK</#if>";
        String badFtl2 = "<#if true>OK</#if]";
        String badFtl3 = "<#assign x = 'OK'/]${x}";
        String badFtl4 = " <#t/]OK\n";
        
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_27);
        for (int syntax : new int[] { LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX }) {
            getConfiguration().setInterpolationSyntax(syntax);
            assertOutput(badFtl1, "OK");
            assertOutput(badFtl2, "OK");
            assertOutput(badFtl3, "OK");
            assertOutput(badFtl4, "OK");
        }
        
        // Legacy tag closing glitch is not emulated with this:
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        assertErrorContains(badFtl1, "\"]\"");
        assertErrorContains(badFtl2, "\"]\"");
        assertErrorContains(badFtl3, "\"]\"");
        assertErrorContains(badFtl4, "\"]\"");
        
        getConfiguration().setInterpolationSyntax(LEGACY_INTERPOLATION_SYNTAX);
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_28);
        assertErrorContains(badFtl1, "\"]\"");
        assertErrorContains(badFtl2, "\"]\"");
        assertErrorContains(badFtl3, "\"]\"");
        assertErrorContains(badFtl4, "\"]\"");
    }
    
    @Test
    public void errorMessagesAreSquareBracketInterpolationSyntaxAwareTest() throws Exception {
        assertErrorContains("<#if ${x}></#if>", "${...}", "${myExpression}");
        assertErrorContains("<#if #{x}></#if>", "#{...}", "#{myExpression}");
        assertErrorContains("<#if [=x]></#if>", "[=...]", "[=myExpression]");
    }

    @Test
    public void unclosedSyntaxErrorTest() throws Exception {
        assertErrorContains("${1", "unclosed \"{\"");
        
        getConfiguration().setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        assertErrorContains("[=1", "unclosed \"[\"");
    }
    
}

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

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class ParsingErrorMessagesTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_21);
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        return cfg;
    }

    @Test
    public void testNeedlessInterpolation() {
        assertErrorContainsAS("<#if ${x} == 3></#if>", "instead of ${");
        assertErrorContainsAS("<#if ${x == 3}></#if>", "instead of ${");
        assertErrorContainsAS("<@foo ${x == 3} />", "instead of ${");
        getConfiguration().setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        assertErrorContainsAS("<@foo [= x == 3] />", "instead of [=");
    }

    @Test
    public void testWrongDirectiveNames() {
        assertErrorContainsAS("<#foo />", "nknown directive", "#foo");
        assertErrorContainsAS("<#set x = 1 />", "nknown directive", "#set", "#assign");
        assertErrorContainsAS("<#iterator></#iterator>", "nknown directive", "#iterator", "#list");
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
        assertErrorContainsAS("<#function x>", "#macro", "unclosed");
        assertErrorContainsAS("<#function x></#macro>", "function end tag");
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
    public void testInterpolatingClosingsErrors() throws Exception {
        assertErrorContainsAS("<#ftl>${x", "unclosed");
        assertErrorContainsAS("<#assign x = x}>", "\"}\"", "open");
        assertOutput("<#assign x = '${x'>", ""); // Legacy glitch... should fail in theory.
        
        for (int syntax : new int[] { LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX }) {
            getConfiguration().setInterpolationSyntax(syntax);
            assertErrorContainsAS("<#ftl>${'x']", "\"]\"", "open");
            assertErrorContains("<#ftl>${'x'>", "end of file");
            assertErrorContains("[#ftl]${'x'>", "end of file");
        }
    }

    @Test
    public void testNestingErrors() throws Exception {
        assertErrorContains(
                "<#if true><#list xs as x></list></#if>",
                "</#if>", "#list", "end-tag");
        assertErrorContains(
                "<#if true><#assign x><#else></#assign></#if>",
                "<#else>", "#if", "#list", "#assign");
        assertErrorContains(
                "<#list xs><#items as x></#list>",
                "</#list>", "#items", "end-tag");
        assertErrorContains(
                "<#list xs as x><#sep></#if></#list>",
                "</#if>", "#list", "#sep", "end-tag");
        assertErrorContains(
                "<#list xs as x>",
                "end of file", "#list", "end-tag");
        assertErrorContains(
                "<#if true>text<#list xs as x></#list>",
                "end of file", "#if", "end-tag");
    }

    /**
     * "assertErrorContains" with both angle bracket and square bracket tag syntax, by converting the input tag syntax.
     * Beware, it uses primitive search-and-replace.
     */
    protected Throwable assertErrorContainsAS(String angleBracketsFtl, String... expectedSubstrings) {
        assertErrorContains(angleBracketsFtl, expectedSubstrings);
        angleBracketsFtl = angleBracketsFtl.replace('<', '[').replace('>', ']');
        return assertErrorContains(angleBracketsFtl, expectedSubstrings);
    }

}

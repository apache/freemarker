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
        assertErrorContains("<#if ${x} == 3></#if>", "instead of ${");
        assertErrorContains("<#if ${x == 3}></#if>", "instead of ${");
        assertErrorContains("<@foo ${x == 3} />", "instead of ${");
    }

    @Test
    public void testWrongDirectiveNames() {
        assertErrorContains("<#foo />", "nknown directive", "#foo");
        assertErrorContains("<#set x = 1 />", "nknown directive", "#set", "#assign");
        assertErrorContains("<#iterator></#iterator>", "nknown directive", "#iterator", "#list");
    }

    @Test
    public void testBug402() {
        assertErrorContains("<#list 1..i as k>${k}<#list>", "existing directive", "malformed", "#list");
        assertErrorContains("<#assign>", "existing directive", "malformed", "#assign");
        assertErrorContains("</#if x>", "existing directive", "malformed", "#if");
        assertErrorContains("<#compress x>", "existing directive", "malformed", "#compress");
    }

    @Test
    public void testUnclosedDirectives() {
        assertErrorContains("<#macro x>", "#macro", "unclosed");
        assertErrorContains("<#macro x></#function>", "macro end tag");
        assertErrorContains("<#function x>", "#macro", "unclosed");
        assertErrorContains("<#function x></#macro>", "function end tag");
        assertErrorContains("<#assign x>", "#assign", "unclosed");
        assertErrorContains("<#macro m><#local x>", "#local", "unclosed");
        assertErrorContains("<#global x>", "#global", "unclosed");
        assertErrorContains("<@foo>", "@...", "unclosed");
        assertErrorContains("<#list xs as x>", "#list", "unclosed");
        assertErrorContains("<#list xs as x><#if x>", "#if", "unclosed");
        assertErrorContains("<#list xs as x><#if x><#if q><#else>", "#if", "unclosed");
        assertErrorContains("<#list xs as x><#if x><#if q><#else><#macro x>qwe", "#macro", "unclosed");
        assertErrorContains("${(blah", "\"(\"", "unclosed");
        assertErrorContains("${blah", "\"{\"", "unclosed");
    }
    
    @Test
    public void testInterpolatingClosingsErrors() throws Exception {
        assertErrorContains("<#ftl>${x", "unclosed");
        assertErrorContains("<#assign x = x}>", "\"}\"", "open");
        assertOutput("<#assign x = '${x'>", ""); // Legacy glitch... should fail in theory.
        
        for (int syntax : new int[] { LEGACY_INTERPOLATION_SYNTAX, DOLLAR_INTERPOLATION_SYNTAX }) {
            getConfiguration().setInterpolationSyntax(syntax);
            assertErrorContains("<#ftl>${'x']", "\"]\"", "open");
            super.assertErrorContains("<#ftl>${'x'>", "end of file");
            super.assertErrorContains("[#ftl]${'x'>", "end of file");
        }
    }
    
    protected Throwable assertErrorContains(String ftl, String... expectedSubstrings) {
        super.assertErrorContains(ftl, expectedSubstrings);
        ftl = ftl.replace('<', '[').replace('>', ']');
        return super.assertErrorContains(ftl, expectedSubstrings);
    }

}

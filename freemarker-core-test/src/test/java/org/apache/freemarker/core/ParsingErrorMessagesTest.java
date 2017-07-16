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

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class ParsingErrorMessagesTest {

    private Configuration cfg = new TestConfigurationBuilder()
            .tagSyntax(TagSyntax.AUTO_DETECT)
            .build();

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
        assertErrorContains("<#if x><#elseif y></#if>", "nknown directive", "#elseIf");
        assertErrorContains("<#outputformat 'HTML'></#outputformat>", "nknown directive", "#outputFormat");
        assertErrorContains("<#autoesc></#autoesc>", "nknown directive", "#autoEsc");
        assertErrorContains("<#noautoesc></#noautoesc>", "nknown directive", "#noAutoEsc");
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
        assertErrorContains("<#function x>", "#macro", "unclosed");
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
    public void testBuiltInWrongNames() {
        assertErrorContains("${x?lower_case}", "camel case", "The correct name is: lowerCase");
        assertErrorContains("${x?iso_utc_nz}", "camel case", "The correct name is: isoUtcNZ");
        assertErrorContains("${x?no_such_name}", "camel case", "\\!The correct name is:", "alphabetical list");
        assertErrorContains("${x?nosuchname}", "\\!camel case", "\\!The correct name is:", "alphabetical list");
    }

    @Test
    public void testSettingWrongNames() {
        assertErrorContains("<#setting time_format='HHmm'>", "camel case", "The correct name is: timeFormat",
                "\\!setting names are:");
        assertErrorContains("<#setting no_such_name=1>", "camel case", "\\!The correct name is:",
                "setting names are:");
        assertErrorContains("<#setting nosuchname=1>", "\\!The correct name is:", "\\!camel case",
                "setting names are:");
    }

    @Test
    public void testSpecialVariableWrongNames() {
        assertErrorContains("${.data_model}", "camel case", "The correct name is: dataModel",
                "\\!variable names are:");
        assertErrorContains("${.no_such_name}", "camel case", "\\!The correct name is:",
                "variable names are:");
        assertErrorContains("${.nosuchname}", "\\!camel case", "\\!The correct name is:",
                "variable names are:");
    }

    @Test
    public void testFtlParameterWrongNames() {
        assertErrorContains("<#ftl strip_whitespace=false>", "camel case", "The correct name is: stripWhitespace");
        assertErrorContains("<#ftl no_such_name=1>", "camel case", "\\!The correct name is:");
        assertErrorContains("<#ftl nosuchname=1>", "\\!camel case", "\\!The correct name is:");
    }

    @Test
    public void testInterpolatingClosingsErrors() {
        assertErrorContains("${x", "unclosed");
        assertErrorContains("<#assign x = x}>", "\"}\"", "open");
        // TODO assertErrorContains("<#assign x = '${x'>", "unclosed");
    }
    
    private void assertErrorContains(String ftl, String... expectedSubstrings) {
        assertErrorContains(false, ftl, expectedSubstrings);
        assertErrorContains(true, ftl, expectedSubstrings);
    }

    private void assertErrorContains(boolean squareTags, String ftl, String... expectedSubstrings) {
        try {
            if (squareTags) {
                ftl = ftl.replace('<', '[').replace('>', ']');
            }
            new Template("adhoc", ftl, cfg);
            fail("The template had to fail");
        } catch (ParseException e) {
            String msg = e.getMessage();
            for (String needle: expectedSubstrings) {
                if (needle.startsWith("\\!")) {
                    String netNeedle = needle.substring(2); 
                    if (msg.contains(netNeedle)) {
                        fail("The message shouldn't contain substring " + _StringUtil.jQuote(netNeedle) + ":\n" + msg);
                    }
                } else if (!msg.contains(needle)) {
                    fail("The message didn't contain substring " + _StringUtil.jQuote(needle) + ":\n" + msg);
                }
            }
            showError(e);
        } catch (IOException e) {
            // Won't happen
            throw new RuntimeException(e);
        }
    }
    
    private void showError(Throwable e) {
        //System.out.println(e);
    }

    @Test
    public void testUnknownHeaderParameter() {
        assertErrorContains("<#ftl foo=1>", "Unknown", "foo");
        assertErrorContains("<#ftl attributes={}>", "Unknown", "attributes", "customSettings");
    }

}

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

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.utility.StringUtil;

public class ParsingErrorMessagesTest {

    private Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
    {
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
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
            fail("The tempalte had to fail");
        } catch (ParseException e) {
            String msg = e.getMessage();
            for (String needle: expectedSubstrings) {
                if (needle.startsWith("\\!")) {
                    String netNeedle = needle.substring(2); 
                    if (msg.contains(netNeedle)) {
                        fail("The message shouldn't contain substring " + StringUtil.jQuote(netNeedle) + ":\n" + msg);
                    }
                } else if (!msg.contains(needle)) {
                    fail("The message didn't contain substring " + StringUtil.jQuote(needle) + ":\n" + msg);
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

}

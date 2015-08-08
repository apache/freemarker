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

package freemarker.template.utility;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Test;

public class StringUtilTest {
    
    @Test
    public void testJQuote() {
        assertEquals("null", StringUtil.jQuote(null));
        assertEquals("\"foo\"", StringUtil.jQuote("foo"));
        assertEquals("\"123\"", StringUtil.jQuote(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                StringUtil.jQuote("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                StringUtil.jQuote("\n\r\t\u0001"));
        assertEquals("\"<\\nb\\rc\\td\\u0001>\"",
                StringUtil.jQuote("<\nb\rc\td\u0001>"));
    }

    @Test
    public void testJQuoteNoXSS() {
        assertEquals("null", StringUtil.jQuoteNoXSS(null));
        assertEquals("\"foo\"", StringUtil.jQuoteNoXSS("foo"));
        assertEquals("\"123\"", StringUtil.jQuoteNoXSS(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                StringUtil.jQuoteNoXSS("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                StringUtil.jQuoteNoXSS("\n\r\t\u0001"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                StringUtil.jQuoteNoXSS("<\nb\rc\td\u0001>"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                StringUtil.jQuoteNoXSS((Object) "<\nb\rc\td\u0001>"));
    }
    
    @Test
    public void testFTLStringLiteralEnc() {
        assertEquals("", StringUtil.FTLStringLiteralEnc(""));
        assertEquals("abc", StringUtil.FTLStringLiteralEnc("abc"));
        assertEquals("{", StringUtil.FTLStringLiteralEnc("{"));
        assertEquals("a{b}c", StringUtil.FTLStringLiteralEnc("a{b}c"));
        assertEquals("a#b", StringUtil.FTLStringLiteralEnc("a#b"));
        assertEquals("a$b", StringUtil.FTLStringLiteralEnc("a$b"));
        assertEquals("a#\\{b}c", StringUtil.FTLStringLiteralEnc("a#{b}c"));
        assertEquals("a$\\{b}c", StringUtil.FTLStringLiteralEnc("a${b}c"));
        assertEquals("a'c\\\"d", StringUtil.FTLStringLiteralEnc("a'c\"d", '"'));
        assertEquals("a\\'c\"d", StringUtil.FTLStringLiteralEnc("a'c\"d", '\''));
        assertEquals("a\\'c\"d", StringUtil.FTLStringLiteralEnc("a'c\"d", '\''));
        assertEquals("\\n\\r\\t\\f\\x0002\\\\", StringUtil.FTLStringLiteralEnc("\n\r\t\f\u0002\\"));
        assertEquals("\\l\\g\\a", StringUtil.FTLStringLiteralEnc("<>&"));
    }
    
    @Test
    public void testGlobToRegularExpression() {
        assertGlobMatches("a/b/c.ftl", "a/b/c.ftl");
        assertGlobDoesNotMatch("/a/b/cxftl", "/a/b/c.ftl", "a/b/C.ftl");
        
        assertGlobMatches("a/b/*.ftl", "a/b/.ftl", "a/b/x.ftl", "a/b/xx.ftl");
        assertGlobDoesNotMatch("a/b/*.ftl", "a/c/x.ftl", "a/b/c/x.ftl", "/a/b/x.ftl", "a/b/xxftl");
        
        assertGlobMatches("a/b/?.ftl", "a/b/x.ftl");
        assertGlobDoesNotMatch("a/b/?.ftl", "a/c/x.ftl", "a/b/.ftl", "a/b/xx.ftl", "a/b/xxftl");
        
        assertGlobMatches("a/**/c.ftl", "a/b/c.ftl", "a/c.ftl", "a/b/b2/b3/c.ftl", "a//c.ftl");
        assertGlobDoesNotMatch("a/**/c.ftl", "x/b/c.ftl", "a/b/x.ftl");
        
        assertGlobMatches("**/c.ftl", "a/b/c.ftl", "c.ftl", "/c.ftl", "///c.ftl");
        assertGlobDoesNotMatch("**/c.ftl", "a/b/x.ftl");

        assertGlobMatches("a/b/**", "a/b/c.ftl", "a/b/c2/c.ftl", "a/b/", "a/b/c/");
        assertGlobDoesNotMatch("a/b.ftl");

        assertGlobMatches("**", "a/b/c.ftl", "");

        assertGlobMatches("\\[\\{\\*\\?\\}\\]\\\\", "[{*?}]\\");
        assertGlobDoesNotMatch("\\[\\{\\*\\?\\}\\]\\\\", "[{xx}]\\");

        assertGlobMatches("a/b/\\?.ftl", "a/b/?.ftl");
        assertGlobDoesNotMatch("a/b/\\?.ftl", "a/b/x.ftl");

        assertGlobMatches("\\?\\?.ftl", "??.ftl");
        assertGlobMatches("\\\\\\\\", "\\\\");
        assertGlobMatches("\\\\\\\\?", "\\\\x");
        assertGlobMatches("x\\", "x");

        assertGlobMatches("???*", "123", "1234", "12345");
        assertGlobDoesNotMatch("???*", "12", "1", "");

        assertGlobMatches("**/a??/b*.ftl", "a11/b1.ftl", "x/a11/b123.ftl", "x/y/a11/b.ftl");
        assertGlobDoesNotMatch("**/a??/b*.ftl", "a1/b1.ftl", "x/a11/c123.ftl");
        
        try {
            StringUtil.globToRegularExpression("x**/y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            StringUtil.globToRegularExpression("**y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            StringUtil.globToRegularExpression("[ab]c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
        
        try {
            StringUtil.globToRegularExpression("{aa,bb}c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
    }
    
    private void assertGlobMatches(String glob, String... ss) {
        Pattern pattern = StringUtil.globToRegularExpression(glob);
        for (String s : ss) {
            if (!pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") doesn't match " + s);
            }
        }
    }

    private void assertGlobDoesNotMatch(String glob, String... ss) {
        Pattern pattern = StringUtil.globToRegularExpression(glob);
        for (String s : ss) {
            if (pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") matches " + s);
            }
        }
    }
    
}

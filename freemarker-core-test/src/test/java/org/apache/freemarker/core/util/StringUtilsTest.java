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

package org.apache.freemarker.core.util;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testV2319() {
        assertEquals("\\n\\r\\f\\b\\t\\x00\\x19", _StringUtils.javaScriptStringEnc("\n\r\f\b\t\u0000\u0019"));
    }

    @Test
    public void testControlChars() {
        assertEsc(
                "\n\r\f\b\t \u0000\u0019\u001F \u007F\u0080\u009F \u2028\u2029",
                "\\n\\r\\f\\b\\t \\x00\\x19\\x1F \\x7F\\x80\\x9F \\u2028\\u2029",
                "\\n\\r\\f\\b\\t \\u0000\\u0019\\u001F \\u007F\\u0080\\u009F \\u2028\\u2029");
    }

    @Test
    public void testHtmlChars() {
        assertEsc(
                "<safe>/>->]> </foo> <!-- --> <![CDATA[ ]]> <?php?>",
                "<safe>/>->]> <\\/foo> \\x3C!-- --\\> \\x3C![CDATA[ ]]\\> \\x3C?php?>",
                "<safe>/>->]> <\\/foo> \\u003C!-- --\\u003E \\u003C![CDATA[ ]]\\u003E \\u003C?php?>");
        assertEsc("<!c", "\\x3C!c", "\\u003C!c");
        assertEsc("c<!", "c\\x3C!", "c\\u003C!");
        assertEsc("c<", "c\\x3C", "c\\u003C");
        assertEsc("c<c", "c<c", "c<c");
        assertEsc("<c", "<c", "<c");
        assertEsc(">", "\\>", "\\u003E");
        assertEsc("->", "-\\>", "-\\u003E");
        assertEsc("-->", "--\\>", "--\\u003E");
        assertEsc("c-->", "c--\\>", "c--\\u003E");
        assertEsc("-->c", "--\\>c", "--\\u003Ec");
        assertEsc("]>", "]\\>", "]\\u003E");
        assertEsc("]]>", "]]\\>", "]]\\u003E");
        assertEsc("c]]>", "c]]\\>", "c]]\\u003E");
        assertEsc("]]>c", "]]\\>c", "]]\\u003Ec");
        assertEsc("c->", "c->", "c->");
        assertEsc("c>", "c>", "c>");
        assertEsc("-->", "--\\>", "--\\u003E");
        assertEsc("/", "\\/", "\\/");
        assertEsc("/c", "\\/c", "\\/c");
        assertEsc("</", "<\\/", "<\\/");
        assertEsc("</c", "<\\/c", "<\\/c");
        assertEsc("c/", "c/", "c/");
    }

    @Test
    public void testJSChars() {
        assertEsc("\"", "\\\"", "\\\"");
        assertEsc("'", "\\'", "'");
        assertEsc("\\", "\\\\", "\\\\");
    }

    @Test
    public void testSameStringsReturned() {
        String s = "==> I/m <safe>!";
        assertTrue(s == _StringUtils.jsStringEnc(s, false));  // "==" because is must return the same object
        assertTrue(s == _StringUtils.jsStringEnc(s, true));

        s = "";
        assertTrue(s == _StringUtils.jsStringEnc(s, false));
        assertTrue(s == _StringUtils.jsStringEnc(s, true));

        s = "\u00E1rv\u00EDzt\u0171r\u0151 \u3020";
        assertEquals(s, _StringUtils.jsStringEnc(s, false));
        assertTrue(s == _StringUtils.jsStringEnc(s, false));
        assertTrue(s == _StringUtils.jsStringEnc(s, true));
    }

    @Test
    public void testOneOffs() {
        assertEsc("c\"c\"cc\"\"c", "c\\\"c\\\"cc\\\"\\\"c", "c\\\"c\\\"cc\\\"\\\"c");
        assertEsc("\"c\"cc\"", "\\\"c\\\"cc\\\"", "\\\"c\\\"cc\\\"");
        assertEsc("c/c/cc//c", "c/c/cc//c", "c/c/cc//c");
        assertEsc("c<c<cc<<c", "c<c<cc<<c", "c<c<cc<<c");
        assertEsc("/<", "\\/\\x3C", "\\/\\u003C");
        assertEsc(">", "\\>", "\\u003E");
        assertEsc("]>", "]\\>", "]\\u003E");
        assertEsc("->", "-\\>", "-\\u003E");
    }

    private void assertEsc(String s, String javaScript, String json) {
        assertEquals(javaScript, _StringUtils.jsStringEnc(s, false));
        assertEquals(json, _StringUtils.jsStringEnc(s, true));
    }

    @Test
    public void testTrim() {
        assertSame(_CollectionUtils.EMPTY_CHAR_ARRAY, _StringUtils.trim(_CollectionUtils.EMPTY_CHAR_ARRAY));
        assertSame(_CollectionUtils.EMPTY_CHAR_ARRAY, _StringUtils.trim(" \t\u0001 ".toCharArray()));
        {
            char[] cs = "foo".toCharArray();
            assertSame(cs, cs);
        }
        assertArrayEquals("foo".toCharArray(), _StringUtils.trim("foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtils.trim(" foo".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtils.trim(" foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtils.trim("\t\tfoo \r\n".toCharArray()));
        assertArrayEquals("x".toCharArray(), _StringUtils.trim(" x ".toCharArray()));
        assertArrayEquals("x y z".toCharArray(), _StringUtils.trim(" x y z ".toCharArray()));
    }

    @Test
    public void testIsTrimmedToEmpty() {
        assertTrue(_StringUtils.isTrimmableToEmpty("".toCharArray()));
        assertTrue(_StringUtils.isTrimmableToEmpty("\r\r\n\u0001".toCharArray()));
        assertFalse(_StringUtils.isTrimmableToEmpty("x".toCharArray()));
        assertFalse(_StringUtils.isTrimmableToEmpty("  x  ".toCharArray()));
    }
    
    @Test
    public void testJQuote() {
        assertEquals("null", _StringUtils.jQuote(null));
        assertEquals("\"foo\"", _StringUtils.jQuote("foo"));
        assertEquals("\"123\"", _StringUtils.jQuote(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                _StringUtils.jQuote("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                _StringUtils.jQuote("\n\r\t\u0001"));
        assertEquals("\"<\\nb\\rc\\td\\u0001>\"",
                _StringUtils.jQuote("<\nb\rc\td\u0001>"));
    }

    @Test
    public void testJQuoteNoXSS() {
        assertEquals("null", _StringUtils.jQuoteNoXSS(null));
        assertEquals("\"foo\"", _StringUtils.jQuoteNoXSS("foo"));
        assertEquals("\"123\"", _StringUtils.jQuoteNoXSS(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                _StringUtils.jQuoteNoXSS("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                _StringUtils.jQuoteNoXSS("\n\r\t\u0001"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                _StringUtils.jQuoteNoXSS("<\nb\rc\td\u0001>"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                _StringUtils.jQuoteNoXSS((Object) "<\nb\rc\td\u0001>"));
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
        
        assertFalse(_StringUtils.globToRegularExpression("ab*").matcher("aBc").matches());
        assertTrue(_StringUtils.globToRegularExpression("ab*", true).matcher("aBc").matches());
        assertTrue(_StringUtils.globToRegularExpression("ab", true).matcher("aB").matches());
        assertTrue(_StringUtils.globToRegularExpression("\u00E1b*", true).matcher("\u00C1bc").matches());
        
        try {
            _StringUtils.globToRegularExpression("x**/y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            _StringUtils.globToRegularExpression("**y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            _StringUtils.globToRegularExpression("[ab]c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
        
        try {
            _StringUtils.globToRegularExpression("{aa,bb}c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
    }
    
    private void assertGlobMatches(String glob, String... ss) {
        Pattern pattern = _StringUtils.globToRegularExpression(glob);
        for (String s : ss) {
            if (!pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") doesn't match " + s);
            }
        }
    }

    private void assertGlobDoesNotMatch(String glob, String... ss) {
        Pattern pattern = _StringUtils.globToRegularExpression(glob);
        for (String s : ss) {
            if (pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") matches " + s);
            }
        }
    }
    
    @Test
    public void testHTMLEnc() {
        String s = "";
        assertSame(s, _StringUtils.XMLEncNA(s));
        
        s = "asd";
        assertSame(s, _StringUtils.XMLEncNA(s));
        
        assertEquals("a&amp;b&lt;c&gt;d&quot;e'f", _StringUtils.XMLEncNA("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtils.XMLEncNA("<"));
        assertEquals("&lt;a", _StringUtils.XMLEncNA("<a"));
        assertEquals("&lt;a&gt;", _StringUtils.XMLEncNA("<a>"));
        assertEquals("a&gt;", _StringUtils.XMLEncNA("a>"));
        assertEquals("&lt;&gt;", _StringUtils.XMLEncNA("<>"));
        assertEquals("a&lt;&gt;b", _StringUtils.XMLEncNA("a<>b"));
    }

    @Test
    public void testXHTMLEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtils.XHTMLEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtils.XHTMLEnc(s));
        
        testXHTMLEnc("a&amp;b&lt;c&gt;d&quot;e&#39;f", "a&b<c>d\"e'f");
        testXHTMLEnc("&lt;", "<");
        testXHTMLEnc("&lt;a", "<a");
        testXHTMLEnc("&lt;a&gt;", "<a>");
        testXHTMLEnc("a&gt;", "a>");
        testXHTMLEnc("&lt;&gt;", "<>");
        testXHTMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXHTMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtils.XHTMLEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtils.XHTMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtils.XMLEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtils.XMLEnc(s));
        
        testXMLEnc("a&amp;b&lt;c&gt;d&quot;e&apos;f", "a&b<c>d\"e'f");
        testXMLEnc("&lt;", "<");
        testXMLEnc("&lt;a", "<a");
        testXMLEnc("&lt;a&gt;", "<a>");
        testXMLEnc("a&gt;", "a>");
        testXMLEnc("&lt;&gt;", "<>");
        testXMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtils.XMLEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtils.XMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEncQAttr() throws IOException {
        String s = "";
        assertSame(s, _StringUtils.XMLEncQAttr(s));
        
        s = "asd";
        assertSame(s, _StringUtils.XMLEncQAttr(s));
        
        assertEquals("a&amp;b&lt;c>d&quot;e'f", _StringUtils.XMLEncQAttr("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtils.XMLEncQAttr("<"));
        assertEquals("&lt;a", _StringUtils.XMLEncQAttr("<a"));
        assertEquals("&lt;a>", _StringUtils.XMLEncQAttr("<a>"));
        assertEquals("a>", _StringUtils.XMLEncQAttr("a>"));
        assertEquals("&lt;>", _StringUtils.XMLEncQAttr("<>"));
        assertEquals("a&lt;>b", _StringUtils.XMLEncQAttr("a<>b"));
    }
    
    @Test
    public void testXMLEncNQG() throws IOException {
        String s = "";
        assertSame(s, _StringUtils.XMLEncNQG(s));
        
        s = "asd";
        assertSame(s, _StringUtils.XMLEncNQG(s));
        
        assertEquals("a&amp;b&lt;c>d\"e'f", _StringUtils.XMLEncNQG("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtils.XMLEncNQG("<"));
        assertEquals("&lt;a", _StringUtils.XMLEncNQG("<a"));
        assertEquals("&lt;a>", _StringUtils.XMLEncNQG("<a>"));
        assertEquals("a>", _StringUtils.XMLEncNQG("a>"));
        assertEquals("&lt;>", _StringUtils.XMLEncNQG("<>"));
        assertEquals("a&lt;>b", _StringUtils.XMLEncNQG("a<>b"));
        
        assertEquals("&gt;", _StringUtils.XMLEncNQG(">"));
        assertEquals("]&gt;", _StringUtils.XMLEncNQG("]>"));
        assertEquals("]]&gt;", _StringUtils.XMLEncNQG("]]>"));
        assertEquals("x]]&gt;", _StringUtils.XMLEncNQG("x]]>"));
        assertEquals("x]>", _StringUtils.XMLEncNQG("x]>"));
        assertEquals("]x>", _StringUtils.XMLEncNQG("]x>"));
    }

    @Test
    public void testRTFEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtils.RTFEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtils.RTFEnc(s));
        
        testRTFEnc("a\\{b\\}c\\\\d", "a{b}c\\d");
        testRTFEnc("\\{", "{");
        testRTFEnc("\\{a", "{a");
        testRTFEnc("\\{a\\}", "{a}");
        testRTFEnc("a\\}", "a}");
        testRTFEnc("\\{\\}", "{}");
        testRTFEnc("a\\{\\}b", "a{}b");
    }

    private void testRTFEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtils.RTFEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtils.RTFEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testNormalizeEOLs() {
        assertNull(_StringUtils.normalizeEOLs(null));
        assertEquals("", _StringUtils.normalizeEOLs(""));
        assertEquals("x", _StringUtils.normalizeEOLs("x"));
        assertEquals("x\ny", _StringUtils.normalizeEOLs("x\ny"));
        assertEquals("x\ny", _StringUtils.normalizeEOLs("x\r\ny"));
        assertEquals("x\ny", _StringUtils.normalizeEOLs("x\ry"));
        assertEquals("\n\n\n\n\n\n", _StringUtils.normalizeEOLs("\n\r\r\r\n\r\n\r"));
    }

    @Test
    public void snakeCaseToCamelCase() {
        assertNull(_StringUtils.snakeCaseToCamelCase(null));
        assertEquals("", _StringUtils.snakeCaseToCamelCase(""));
        assertEquals("x", _StringUtils.snakeCaseToCamelCase("x"));
        assertEquals("xxx", _StringUtils.snakeCaseToCamelCase("xXx"));
        assertEquals("fooBar", _StringUtils.snakeCaseToCamelCase("foo_bar"));
        assertEquals("fooBar", _StringUtils.snakeCaseToCamelCase("FOO_BAR"));
        assertEquals("fooBar", _StringUtils.snakeCaseToCamelCase("_foo__bar_"));
        assertEquals("aBC", _StringUtils.snakeCaseToCamelCase("a_b_c"));
    }

}

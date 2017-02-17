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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Test;

public class StringUtilTest {

    @Test
    public void testV2319() {
        assertEquals("\\n\\r\\f\\b\\t\\x00\\x19", _StringUtil.javaScriptStringEnc("\n\r\f\b\t\u0000\u0019"));
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
        assertTrue(s == _StringUtil.jsStringEnc(s, false));  // "==" because is must return the same object
        assertTrue(s == _StringUtil.jsStringEnc(s, true));

        s = "";
        assertTrue(s == _StringUtil.jsStringEnc(s, false));
        assertTrue(s == _StringUtil.jsStringEnc(s, true));

        s = "\u00E1rv\u00EDzt\u0171r\u0151 \u3020";
        assertEquals(s, _StringUtil.jsStringEnc(s, false));
        assertTrue(s == _StringUtil.jsStringEnc(s, false));
        assertTrue(s == _StringUtil.jsStringEnc(s, true));
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

    @Test
    public void testFTLEscaping() {
        assertFTLEsc("", "", "", "", "\"\"");
        assertFTLEsc("\'", "\\'", "'", "\\'", "\"'\"");
        assertFTLEsc("\"", "\\\"", "\\\"", "\"", "'\"'");
        assertFTLEsc("\"", "\\\"", "\\\"", "\"", "'\"'");
        assertFTLEsc("foo", "foo", "foo", "foo", "\"foo\"");
        assertFTLEsc("foo's", "foo\\'s", "foo's", "foo\\'s", "\"foo's\"");
        assertFTLEsc("foo \"", "foo \\\"", "foo \\\"", "foo \"", "'foo \"'");
        assertFTLEsc("foo's \"", "foo\\'s \\\"", "foo's \\\"", "foo\\'s \"", "\"foo's \\\"\"");
        assertFTLEsc("foo\nb\u0000c", "foo\\nb\\x0000c", "foo\\nb\\x0000c", "foo\\nb\\x0000c", "\"foo\\nb\\x0000c\"");
    }
    
    private void assertEsc(String s, String javaScript, String json) {
        assertEquals(javaScript, _StringUtil.jsStringEnc(s, false));
        assertEquals(json, _StringUtil.jsStringEnc(s, true));
    }

    private void assertFTLEsc(String s, String partAny, String partQuot, String partApos, String quoted) {
        assertEquals(partAny, _StringUtil.FTLStringLiteralEnc(s));
        assertEquals(partQuot, _StringUtil.FTLStringLiteralEnc(s, '\"'));
        assertEquals(partApos, _StringUtil.FTLStringLiteralEnc(s, '\''));
        assertEquals(quoted, _StringUtil.ftlQuote(s));
    }
    
    @Test
    public void testTrim() {
        assertSame(_CollectionUtil.EMPTY_CHAR_ARRAY, _StringUtil.trim(_CollectionUtil.EMPTY_CHAR_ARRAY));
        assertSame(_CollectionUtil.EMPTY_CHAR_ARRAY, _StringUtil.trim(" \t\u0001 ".toCharArray()));
        {
            char[] cs = "foo".toCharArray();
            assertSame(cs, cs);
        }
        assertArrayEquals("foo".toCharArray(), _StringUtil.trim("foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtil.trim(" foo".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtil.trim(" foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), _StringUtil.trim("\t\tfoo \r\n".toCharArray()));
        assertArrayEquals("x".toCharArray(), _StringUtil.trim(" x ".toCharArray()));
        assertArrayEquals("x y z".toCharArray(), _StringUtil.trim(" x y z ".toCharArray()));
    }

    @Test
    public void testIsTrimmedToEmpty() {
        assertTrue(_StringUtil.isTrimmableToEmpty("".toCharArray()));
        assertTrue(_StringUtil.isTrimmableToEmpty("\r\r\n\u0001".toCharArray()));
        assertFalse(_StringUtil.isTrimmableToEmpty("x".toCharArray()));
        assertFalse(_StringUtil.isTrimmableToEmpty("  x  ".toCharArray()));
    }
    
    @Test
    public void testJQuote() {
        assertEquals("null", _StringUtil.jQuote(null));
        assertEquals("\"foo\"", _StringUtil.jQuote("foo"));
        assertEquals("\"123\"", _StringUtil.jQuote(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                _StringUtil.jQuote("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                _StringUtil.jQuote("\n\r\t\u0001"));
        assertEquals("\"<\\nb\\rc\\td\\u0001>\"",
                _StringUtil.jQuote("<\nb\rc\td\u0001>"));
    }

    @Test
    public void testJQuoteNoXSS() {
        assertEquals("null", _StringUtil.jQuoteNoXSS(null));
        assertEquals("\"foo\"", _StringUtil.jQuoteNoXSS("foo"));
        assertEquals("\"123\"", _StringUtil.jQuoteNoXSS(Integer.valueOf(123)));
        assertEquals("\"foo's \\\"bar\\\"\"",
                _StringUtil.jQuoteNoXSS("foo's \"bar\""));
        assertEquals("\"\\n\\r\\t\\u0001\"",
                _StringUtil.jQuoteNoXSS("\n\r\t\u0001"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                _StringUtil.jQuoteNoXSS("<\nb\rc\td\u0001>"));
        assertEquals("\"\\u003C\\nb\\rc\\td\\u0001>\"",
                _StringUtil.jQuoteNoXSS((Object) "<\nb\rc\td\u0001>"));
    }
    
    @Test
    public void testFTLStringLiteralEnc() {
        assertEquals("", _StringUtil.FTLStringLiteralEnc(""));
        assertEquals("abc", _StringUtil.FTLStringLiteralEnc("abc"));
        assertEquals("{", _StringUtil.FTLStringLiteralEnc("{"));
        assertEquals("a{b}c", _StringUtil.FTLStringLiteralEnc("a{b}c"));
        assertEquals("a#b", _StringUtil.FTLStringLiteralEnc("a#b"));
        assertEquals("a$b", _StringUtil.FTLStringLiteralEnc("a$b"));
        assertEquals("a#\\{b}c", _StringUtil.FTLStringLiteralEnc("a#{b}c"));
        assertEquals("a$\\{b}c", _StringUtil.FTLStringLiteralEnc("a${b}c"));
        assertEquals("a'c\\\"d", _StringUtil.FTLStringLiteralEnc("a'c\"d", '"'));
        assertEquals("a\\'c\"d", _StringUtil.FTLStringLiteralEnc("a'c\"d", '\''));
        assertEquals("a\\'c\"d", _StringUtil.FTLStringLiteralEnc("a'c\"d", '\''));
        assertEquals("\\n\\r\\t\\f\\x0002\\\\", _StringUtil.FTLStringLiteralEnc("\n\r\t\f\u0002\\"));
        assertEquals("\\l\\g\\a", _StringUtil.FTLStringLiteralEnc("<>&"));
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
        
        assertFalse(_StringUtil.globToRegularExpression("ab*").matcher("aBc").matches());
        assertTrue(_StringUtil.globToRegularExpression("ab*", true).matcher("aBc").matches());
        assertTrue(_StringUtil.globToRegularExpression("ab", true).matcher("aB").matches());
        assertTrue(_StringUtil.globToRegularExpression("\u00E1b*", true).matcher("\u00C1bc").matches());
        
        try {
            _StringUtil.globToRegularExpression("x**/y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            _StringUtil.globToRegularExpression("**y");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("**"));
        }
        
        try {
            _StringUtil.globToRegularExpression("[ab]c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
        
        try {
            _StringUtil.globToRegularExpression("{aa,bb}c");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), Matchers.containsString("unsupported"));
        }
    }
    
    private void assertGlobMatches(String glob, String... ss) {
        Pattern pattern = _StringUtil.globToRegularExpression(glob);
        for (String s : ss) {
            if (!pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") doesn't match " + s);
            }
        }
    }

    private void assertGlobDoesNotMatch(String glob, String... ss) {
        Pattern pattern = _StringUtil.globToRegularExpression(glob);
        for (String s : ss) {
            if (pattern.matcher(s).matches()) {
                fail("Glob " + glob + " (regexp: " + pattern + ") matches " + s);
            }
        }
    }
    
    @Test
    public void testHTMLEnc() {
        String s = "";
        assertSame(s, _StringUtil.XMLEncNA(s));
        
        s = "asd";
        assertSame(s, _StringUtil.XMLEncNA(s));
        
        assertEquals("a&amp;b&lt;c&gt;d&quot;e'f", _StringUtil.XMLEncNA("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtil.XMLEncNA("<"));
        assertEquals("&lt;a", _StringUtil.XMLEncNA("<a"));
        assertEquals("&lt;a&gt;", _StringUtil.XMLEncNA("<a>"));
        assertEquals("a&gt;", _StringUtil.XMLEncNA("a>"));
        assertEquals("&lt;&gt;", _StringUtil.XMLEncNA("<>"));
        assertEquals("a&lt;&gt;b", _StringUtil.XMLEncNA("a<>b"));
    }

    @Test
    public void testXHTMLEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtil.XHTMLEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtil.XHTMLEnc(s));
        
        testXHTMLEnc("a&amp;b&lt;c&gt;d&quot;e&#39;f", "a&b<c>d\"e'f");
        testXHTMLEnc("&lt;", "<");
        testXHTMLEnc("&lt;a", "<a");
        testXHTMLEnc("&lt;a&gt;", "<a>");
        testXHTMLEnc("a&gt;", "a>");
        testXHTMLEnc("&lt;&gt;", "<>");
        testXHTMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXHTMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtil.XHTMLEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtil.XHTMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtil.XMLEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtil.XMLEnc(s));
        
        testXMLEnc("a&amp;b&lt;c&gt;d&quot;e&apos;f", "a&b<c>d\"e'f");
        testXMLEnc("&lt;", "<");
        testXMLEnc("&lt;a", "<a");
        testXMLEnc("&lt;a&gt;", "<a>");
        testXMLEnc("a&gt;", "a>");
        testXMLEnc("&lt;&gt;", "<>");
        testXMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtil.XMLEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtil.XMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEncQAttr() throws IOException {
        String s = "";
        assertSame(s, _StringUtil.XMLEncQAttr(s));
        
        s = "asd";
        assertSame(s, _StringUtil.XMLEncQAttr(s));
        
        assertEquals("a&amp;b&lt;c>d&quot;e'f", _StringUtil.XMLEncQAttr("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtil.XMLEncQAttr("<"));
        assertEquals("&lt;a", _StringUtil.XMLEncQAttr("<a"));
        assertEquals("&lt;a>", _StringUtil.XMLEncQAttr("<a>"));
        assertEquals("a>", _StringUtil.XMLEncQAttr("a>"));
        assertEquals("&lt;>", _StringUtil.XMLEncQAttr("<>"));
        assertEquals("a&lt;>b", _StringUtil.XMLEncQAttr("a<>b"));
    }
    
    @Test
    public void testXMLEncNQG() throws IOException {
        String s = "";
        assertSame(s, _StringUtil.XMLEncNQG(s));
        
        s = "asd";
        assertSame(s, _StringUtil.XMLEncNQG(s));
        
        assertEquals("a&amp;b&lt;c>d\"e'f", _StringUtil.XMLEncNQG("a&b<c>d\"e'f"));
        assertEquals("&lt;", _StringUtil.XMLEncNQG("<"));
        assertEquals("&lt;a", _StringUtil.XMLEncNQG("<a"));
        assertEquals("&lt;a>", _StringUtil.XMLEncNQG("<a>"));
        assertEquals("a>", _StringUtil.XMLEncNQG("a>"));
        assertEquals("&lt;>", _StringUtil.XMLEncNQG("<>"));
        assertEquals("a&lt;>b", _StringUtil.XMLEncNQG("a<>b"));
        
        assertEquals("&gt;", _StringUtil.XMLEncNQG(">"));
        assertEquals("]&gt;", _StringUtil.XMLEncNQG("]>"));
        assertEquals("]]&gt;", _StringUtil.XMLEncNQG("]]>"));
        assertEquals("x]]&gt;", _StringUtil.XMLEncNQG("x]]>"));
        assertEquals("x]>", _StringUtil.XMLEncNQG("x]>"));
        assertEquals("]x>", _StringUtil.XMLEncNQG("]x>"));
    }

    @Test
    public void testRTFEnc() throws IOException {
        String s = "";
        assertSame(s, _StringUtil.RTFEnc(s));
        
        s = "asd";
        assertSame(s, _StringUtil.RTFEnc(s));
        
        testRTFEnc("a\\{b\\}c\\\\d", "a{b}c\\d");
        testRTFEnc("\\{", "{");
        testRTFEnc("\\{a", "{a");
        testRTFEnc("\\{a\\}", "{a}");
        testRTFEnc("a\\}", "a}");
        testRTFEnc("\\{\\}", "{}");
        testRTFEnc("a\\{\\}b", "a{}b");
    }

    private void testRTFEnc(String expected, String in) throws IOException {
        assertEquals(expected, _StringUtil.RTFEnc(in));
        
        StringWriter sw = new StringWriter();
        _StringUtil.RTFEnc(in, sw);
        assertEquals(expected, sw.toString());
    }
    
}

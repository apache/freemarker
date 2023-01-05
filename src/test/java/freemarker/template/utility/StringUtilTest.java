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

package freemarker.template.utility;

import static freemarker.template.utility.StringUtil.JsStringEncCompatibility.*;
import static freemarker.template.utility.StringUtil.JsStringEncQuotation.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Test;

import freemarker.core.ParseException;
import freemarker.template.utility.StringUtil.JsStringEncCompatibility;

public class StringUtilTest {

    @Test
    public void testJavaScriptStringEncV2319() {
        assertEquals("\\n\\r\\f\\b\\t\\x00\\x19", StringUtil.javaScriptStringEnc("\n\r\f\b\t\u0000\u0019"));
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
        assertTrue(s == StringUtil.jsStringEnc(s, false));  // "==" because is must return the same object
        assertTrue(s == StringUtil.jsStringEnc(s, true));

        s = "";
        assertTrue(s == StringUtil.jsStringEnc(s, false));
        assertTrue(s == StringUtil.jsStringEnc(s, true));

        s = "\u00E1rv\u00EDzt\u0171r\u0151 \u3020";
        assertEquals(s, StringUtil.jsStringEnc(s, false));
        assertTrue(s == StringUtil.jsStringEnc(s, false));
        assertTrue(s == StringUtil.jsStringEnc(s, true));
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
        assertEquals(javaScript, StringUtil.jsStringEnc(s, false));
        assertEquals(json, StringUtil.jsStringEnc(s, true));
    }

    private void assertFTLEsc(String s, String partAny, String partQuot, String partApos, String quoted) {
        assertEquals(partAny, StringUtil.FTLStringLiteralEnc(s));
        assertEquals(partQuot, StringUtil.FTLStringLiteralEnc(s, '\"'));
        assertEquals(partApos, StringUtil.FTLStringLiteralEnc(s, '\''));
        assertEquals(quoted, StringUtil.ftlQuote(s));
    }
    
    @Test
    public void testTrim() {
        assertSame(CollectionUtils.EMPTY_CHAR_ARRAY, StringUtil.trim(CollectionUtils.EMPTY_CHAR_ARRAY));
        assertSame(CollectionUtils.EMPTY_CHAR_ARRAY, StringUtil.trim(" \t\u0001 ".toCharArray()));
        {
            char[] cs = "foo".toCharArray();
            assertSame(cs, cs);
        }
        assertArrayEquals("foo".toCharArray(), StringUtil.trim("foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), StringUtil.trim(" foo".toCharArray()));
        assertArrayEquals("foo".toCharArray(), StringUtil.trim(" foo ".toCharArray()));
        assertArrayEquals("foo".toCharArray(), StringUtil.trim("\t\tfoo \r\n".toCharArray()));
        assertArrayEquals("x".toCharArray(), StringUtil.trim(" x ".toCharArray()));
        assertArrayEquals("x y z".toCharArray(), StringUtil.trim(" x y z ".toCharArray()));
    }

    @Test
    public void testIsTrimmedToEmpty() {
        assertTrue(StringUtil.isTrimmableToEmpty("".toCharArray()));
        assertTrue(StringUtil.isTrimmableToEmpty("\r\r\n\u0001".toCharArray()));
        assertFalse(StringUtil.isTrimmableToEmpty("x".toCharArray()));
        assertFalse(StringUtil.isTrimmableToEmpty("  x  ".toCharArray()));
    }
    
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
        assertEquals("=[\\=]=", StringUtil.FTLStringLiteralEnc("=[=]="));
        assertEquals("[\\=", StringUtil.FTLStringLiteralEnc("[="));
    }

    @Test
    public void testFTLStringLiteralDec() throws ParseException {
        assertEquals("", StringUtil.FTLStringLiteralDec(""));
        assertEquals("x", StringUtil.FTLStringLiteralDec("x"));
        assertEquals("\nq", StringUtil.FTLStringLiteralDec("\\x0Aq"));
        assertEquals("\n\r1", StringUtil.FTLStringLiteralDec("\\x0A\\x000D1"));
        assertEquals("\n\r\t", StringUtil.FTLStringLiteralDec("\\n\\r\\t"));
        assertEquals("${1}#{2}[=3]", StringUtil.FTLStringLiteralDec("$\\{1}#\\{2}[\\=3]"));
        assertEquals("{=", StringUtil.FTLStringLiteralDec("\\{\\="));
        assertEquals("\\=", StringUtil.FTLStringLiteralDec("\\\\="));
           
        try {
            StringUtil.FTLStringLiteralDec("\\[");
            fail();
        } catch (ParseException e) {
            assertThat(e.getMessage(), containsString("\\["));
        }
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
        
        assertFalse(StringUtil.globToRegularExpression("ab*").matcher("aBc").matches());
        assertTrue(StringUtil.globToRegularExpression("ab*", true).matcher("aBc").matches());
        assertTrue(StringUtil.globToRegularExpression("ab", true).matcher("aB").matches());
        assertTrue(StringUtil.globToRegularExpression("\u00E1b*", true).matcher("\u00C1bc").matches());
        
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
    
    @Test
    public void testHTMLEnc() {
        String s = "";
        assertSame(s, StringUtil.HTMLEnc(s));
        
        s = "asd";
        assertSame(s, StringUtil.HTMLEnc(s));
        
        assertEquals("a&amp;b&lt;c&gt;d&quot;e'f", StringUtil.HTMLEnc("a&b<c>d\"e'f"));
        assertEquals("&lt;", StringUtil.HTMLEnc("<"));
        assertEquals("&lt;a", StringUtil.HTMLEnc("<a"));
        assertEquals("&lt;a&gt;", StringUtil.HTMLEnc("<a>"));
        assertEquals("a&gt;", StringUtil.HTMLEnc("a>"));
        assertEquals("&lt;&gt;", StringUtil.HTMLEnc("<>"));
        assertEquals("a&lt;&gt;b", StringUtil.HTMLEnc("a<>b"));
    }

    @Test
    public void testXHTMLEnc() throws IOException {
        String s = "";
        assertSame(s, StringUtil.XHTMLEnc(s));
        
        s = "asd";
        assertSame(s, StringUtil.XHTMLEnc(s));
        
        testXHTMLEnc("a&amp;b&lt;c&gt;d&quot;e&#39;f", "a&b<c>d\"e'f");
        testXHTMLEnc("&lt;", "<");
        testXHTMLEnc("&lt;a", "<a");
        testXHTMLEnc("&lt;a&gt;", "<a>");
        testXHTMLEnc("a&gt;", "a>");
        testXHTMLEnc("&lt;&gt;", "<>");
        testXHTMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXHTMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, StringUtil.XHTMLEnc(in));
        
        StringWriter sw = new StringWriter();
        StringUtil.XHTMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEnc() throws IOException {
        String s = "";
        assertSame(s, StringUtil.XMLEnc(s));
        
        s = "asd";
        assertSame(s, StringUtil.XMLEnc(s));
        
        testXMLEnc("a&amp;b&lt;c&gt;d&quot;e&apos;f", "a&b<c>d\"e'f");
        testXMLEnc("&lt;", "<");
        testXMLEnc("&lt;a", "<a");
        testXMLEnc("&lt;a&gt;", "<a>");
        testXMLEnc("a&gt;", "a>");
        testXMLEnc("&lt;&gt;", "<>");
        testXMLEnc("a&lt;&gt;b", "a<>b");
    }
    
    private void testXMLEnc(String expected, String in) throws IOException {
        assertEquals(expected, StringUtil.XMLEnc(in));
        
        StringWriter sw = new StringWriter();
        StringUtil.XMLEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void testXMLEncQAttr() throws IOException {
        String s = "";
        assertSame(s, StringUtil.XMLEncQAttr(s));
        
        s = "asd";
        assertSame(s, StringUtil.XMLEncQAttr(s));
        
        assertEquals("a&amp;b&lt;c>d&quot;e'f", StringUtil.XMLEncQAttr("a&b<c>d\"e'f"));
        assertEquals("&lt;", StringUtil.XMLEncQAttr("<"));
        assertEquals("&lt;a", StringUtil.XMLEncQAttr("<a"));
        assertEquals("&lt;a>", StringUtil.XMLEncQAttr("<a>"));
        assertEquals("a>", StringUtil.XMLEncQAttr("a>"));
        assertEquals("&lt;>", StringUtil.XMLEncQAttr("<>"));
        assertEquals("a&lt;>b", StringUtil.XMLEncQAttr("a<>b"));
    }
    
    @Test
    public void testXMLEncNQG() throws IOException {
        String s = "";
        assertSame(s, StringUtil.XMLEncNQG(s));
        
        s = "asd";
        assertSame(s, StringUtil.XMLEncNQG(s));
        
        assertEquals("a&amp;b&lt;c>d\"e'f", StringUtil.XMLEncNQG("a&b<c>d\"e'f"));
        assertEquals("&lt;", StringUtil.XMLEncNQG("<"));
        assertEquals("&lt;a", StringUtil.XMLEncNQG("<a"));
        assertEquals("&lt;a>", StringUtil.XMLEncNQG("<a>"));
        assertEquals("a>", StringUtil.XMLEncNQG("a>"));
        assertEquals("&lt;>", StringUtil.XMLEncNQG("<>"));
        assertEquals("a&lt;>b", StringUtil.XMLEncNQG("a<>b"));
        
        assertEquals("&gt;", StringUtil.XMLEncNQG(">"));
        assertEquals("]&gt;", StringUtil.XMLEncNQG("]>"));
        assertEquals("]]&gt;", StringUtil.XMLEncNQG("]]>"));
        assertEquals("x]]&gt;", StringUtil.XMLEncNQG("x]]>"));
        assertEquals("x]>", StringUtil.XMLEncNQG("x]>"));
        assertEquals("]x>", StringUtil.XMLEncNQG("]x>"));
    }

    @Test
    public void testRTFEnc() throws IOException {
        String s = "";
        assertSame(s, StringUtil.RTFEnc(s));
        
        s = "asd";
        assertSame(s, StringUtil.RTFEnc(s));
        
        testRTFEnc("a\\{b\\}c\\\\d", "a{b}c\\d");
        testRTFEnc("\\{", "{");
        testRTFEnc("\\{a", "{a");
        testRTFEnc("\\{a\\}", "{a}");
        testRTFEnc("a\\}", "a}");
        testRTFEnc("\\{\\}", "{}");
        testRTFEnc("a\\{\\}b", "a{}b");
    }

    private void testRTFEnc(String expected, String in) throws IOException {
        assertEquals(expected, StringUtil.RTFEnc(in));
        
        StringWriter sw = new StringWriter();
        StringUtil.RTFEnc(in, sw);
        assertEquals(expected, sw.toString());
    }

    @Test
    public void jsStringEncQuotationTests() {
        for (JsStringEncCompatibility anyCompatibility : JsStringEncCompatibility.values()) {
            JsStringEncCompatibility anyJsonCompatible = anyCompatibility.isJSONCompatible() ? anyCompatibility : JSON;

            assertEquals("", StringUtil.jsStringEnc("", anyCompatibility, null));
            assertEquals("''", StringUtil.jsStringEnc("", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"\"", StringUtil.jsStringEnc("", anyCompatibility, QUOTATION_MARK));

            assertEquals("a", StringUtil.jsStringEnc("a", anyCompatibility, null));
            assertEquals("a", StringUtil.jsStringEnc("a", anyCompatibility, null));
            assertEquals("'a'", StringUtil.jsStringEnc("a", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"a\"", StringUtil.jsStringEnc("a", anyCompatibility, QUOTATION_MARK));

            try {
                StringUtil.jsStringEnc("", anyJsonCompatible, APOSTROPHE);
                fail();
            } catch (IllegalArgumentException e) {
                // expected
            }

            assertEquals("a\\'b", StringUtil.jsStringEnc("a'b", JAVA_SCRIPT, null));
            assertEquals("a'b", StringUtil.jsStringEnc("a'b", JSON, null));
            assertEquals("a\\u0027b", StringUtil.jsStringEnc("a'b", JAVA_SCRIPT_OR_JSON, null));
            assertEquals("'a\\'b'", StringUtil.jsStringEnc("a'b", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"a'b\"", StringUtil.jsStringEnc("a'b", anyCompatibility, QUOTATION_MARK));

            assertEquals("<\\/e>", StringUtil.jsStringEnc("</e>", anyCompatibility, null));
            assertEquals("'<\\/e>'", StringUtil.jsStringEnc("</e>", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"<\\/e>\"", StringUtil.jsStringEnc("</e>", anyCompatibility, QUOTATION_MARK));

            assertEquals("\\/e>", StringUtil.jsStringEnc("/e>", anyCompatibility, null));
            assertEquals("'/e>'", StringUtil.jsStringEnc("/e>", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"/e>\"", StringUtil.jsStringEnc("/e>", anyCompatibility, QUOTATION_MARK));

            assertEquals("\\>", StringUtil.jsStringEnc(">", JAVA_SCRIPT, null));
            assertEquals("\\u003E", StringUtil.jsStringEnc(">", JSON, null));
            assertEquals("'>'", StringUtil.jsStringEnc(">", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\">\"", StringUtil.jsStringEnc(">", anyCompatibility, QUOTATION_MARK));

            assertEquals("-\\>", StringUtil.jsStringEnc("->", JAVA_SCRIPT, null));
            assertEquals("-\\u003E", StringUtil.jsStringEnc("->", JSON, null));
            assertEquals("'->'", StringUtil.jsStringEnc("->", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"->\"", StringUtil.jsStringEnc("->", JAVA_SCRIPT, QUOTATION_MARK));

            assertEquals("--\\>", StringUtil.jsStringEnc("-->", JAVA_SCRIPT, null));
            assertEquals("--\\u003E", StringUtil.jsStringEnc("-->", anyJsonCompatible, null));
            assertEquals("'--\\>'", StringUtil.jsStringEnc("-->", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"--\\>\"", StringUtil.jsStringEnc("-->", JAVA_SCRIPT, QUOTATION_MARK));
            assertEquals("\"--\\u003E\"", StringUtil.jsStringEnc("-->", anyJsonCompatible, QUOTATION_MARK));

            assertEquals("x->", StringUtil.jsStringEnc("x->", anyCompatibility, null));
            assertEquals("'x->'", StringUtil.jsStringEnc("x->", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"x->\"", StringUtil.jsStringEnc("x->", anyCompatibility, QUOTATION_MARK));

            assertEquals("\\x3C", StringUtil.jsStringEnc("<", JAVA_SCRIPT, null));
            assertEquals("\\u003C", StringUtil.jsStringEnc("<", JSON, null));
            assertEquals("'<'", StringUtil.jsStringEnc("<", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"<\"", StringUtil.jsStringEnc("<", anyCompatibility, QUOTATION_MARK));

            assertEquals("\\x3C!", StringUtil.jsStringEnc("<!", JAVA_SCRIPT, null));
            assertEquals("\\u003C!", StringUtil.jsStringEnc("<!", JSON, null));
            assertEquals("'\\x3C!'", StringUtil.jsStringEnc("<!", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"\\x3C!\"", StringUtil.jsStringEnc("<!", JAVA_SCRIPT, QUOTATION_MARK));
            assertEquals("\"\\u003C!\"", StringUtil.jsStringEnc("<!", anyJsonCompatible, QUOTATION_MARK));

            assertEquals("<x", StringUtil.jsStringEnc("<x", anyCompatibility, null));
            assertEquals("'<x'", StringUtil.jsStringEnc("<x", JAVA_SCRIPT, APOSTROPHE));
            assertEquals("\"<x\"", StringUtil.jsStringEnc("<x", anyCompatibility, QUOTATION_MARK));
        }
    }

}

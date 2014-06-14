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

package freemarker.template;

import junit.framework.TestCase;
import freemarker.template.utility.StringUtil;

public class StringUtilTest extends TestCase {

    public StringUtilTest(String name) {
        super(name);
    }
    
    public void testV2319() {
        assertEquals("\\n\\r\\f\\b\\t\\x00\\x19", StringUtil.javaScriptStringEnc("\n\r\f\b\t\u0000\u0019"));
    }

    public void testControlChars() {
        assertEsc(
                "\n\r\f\b\t \u0000\u0019\u001F \u007F\u0080\u009F \u2028\u2029",
                "\\n\\r\\f\\b\\t \\x00\\x19\\x1F \\x7F\\x80\\x9F \\u2028\\u2029",
                "\\n\\r\\f\\b\\t \\u0000\\u0019\\u001F \\u007F\\u0080\\u009F \\u2028\\u2029");
    }

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

    public void testJSChars() {
        assertEsc("\"", "\\\"", "\\\"");
        assertEsc("'", "\\'", "'");
        assertEsc("\\", "\\\\", "\\\\");
    }

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
        assertEquals(javaScript, StringUtil.jsStringEnc(s, false));
        assertEquals(json, StringUtil.jsStringEnc(s, true));
    }
    
}

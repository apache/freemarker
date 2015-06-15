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

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {
    
    public StringUtilTest(String name) {
        super(name);
    }
    
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
    
}

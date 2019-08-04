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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.*;

import org.junit.Test;

public class TemplateLanguageUtilsTest {

    @Test
    public void testEscapeStringLiteralPart() {
        assertEquals("", TemplateLanguageUtils.escapeStringLiteralPart(""));
        assertEquals("abc", TemplateLanguageUtils.escapeStringLiteralPart("abc"));
        assertEquals("{", TemplateLanguageUtils.escapeStringLiteralPart("{"));
        assertEquals("a{b}c", TemplateLanguageUtils.escapeStringLiteralPart("a{b}c"));
        assertEquals("a#b", TemplateLanguageUtils.escapeStringLiteralPart("a#b"));
        assertEquals("a$b", TemplateLanguageUtils.escapeStringLiteralPart("a$b"));
        // Find related: [interpolation prefixes]
        assertEquals("a#{b}c", TemplateLanguageUtils.escapeStringLiteralPart("a#{b}c"));
        assertEquals("a$\\{b}c", TemplateLanguageUtils.escapeStringLiteralPart("a${b}c"));
        assertEquals("a'c\\\"d", TemplateLanguageUtils.escapeStringLiteralPart("a'c\"d", '"'));
        assertEquals("a\\'c\"d", TemplateLanguageUtils.escapeStringLiteralPart("a'c\"d", '\''));
        assertEquals("a\\'c\"d", TemplateLanguageUtils.escapeStringLiteralPart("a'c\"d", '\''));
        assertEquals("\\n\\r\\t\\f\\x0002\\\\", TemplateLanguageUtils.escapeStringLiteralPart("\n\r\t\f\u0002\\"));
        assertEquals("\\l\\g\\a", TemplateLanguageUtils.escapeStringLiteralPart("<>&"));
        assertEquals("=[\\=]=", TemplateLanguageUtils.escapeStringLiteralPart("=[=]="));
        assertEquals("[\\=", TemplateLanguageUtils.escapeStringLiteralPart("[="));
    }
    
    @Test
    public void testEscapeStringLiteralAll() {
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

    private void assertFTLEsc(String s, String partAny, String partQuot, String partApos, String quoted) {
        assertEquals(partAny, TemplateLanguageUtils.escapeStringLiteralPart(s));
        assertEquals(partQuot, TemplateLanguageUtils.escapeStringLiteralPart(s, '\"'));
        assertEquals(partApos, TemplateLanguageUtils.escapeStringLiteralPart(s, '\''));
        assertEquals(quoted, TemplateLanguageUtils.toStringLiteral(s));
    }

    @Test
    public void testUnescapeStringLiteralPart() throws Exception {
        assertEquals("", TemplateLanguageUtils.unescapeStringLiteralPart(""));
        assertEquals("1", TemplateLanguageUtils.unescapeStringLiteralPart("1"));
        assertEquals("123", TemplateLanguageUtils.unescapeStringLiteralPart("123"));
        assertEquals("1&2&3", TemplateLanguageUtils.unescapeStringLiteralPart("1\\a2\\a3"));
        assertEquals("&", TemplateLanguageUtils.unescapeStringLiteralPart("\\a"));
        assertEquals("&&&", TemplateLanguageUtils.unescapeStringLiteralPart("\\a\\a\\a"));
        assertEquals(
                "\u0000\u0000&\u0000\u0000\u0000\u0000",
                TemplateLanguageUtils.unescapeStringLiteralPart("\\x0000\\x0000\\a\\x0000\\x000\\x00\\x0"));
        assertEquals(
                "'\"\n\b\u0000c><&{\\",
                TemplateLanguageUtils.unescapeStringLiteralPart("\\'\\\"\\n\\b\\x0000c\\g\\l\\a\\{\\\\"));
        
        assertEquals("\nq", TemplateLanguageUtils.unescapeStringLiteralPart("\\x0Aq"));
        assertEquals("\n\r1", TemplateLanguageUtils.unescapeStringLiteralPart("\\x0A\\x000D1"));
        assertEquals("\n\r\t", TemplateLanguageUtils.unescapeStringLiteralPart("\\n\\r\\t"));
        assertEquals("${1}#{2}[=3]", TemplateLanguageUtils.unescapeStringLiteralPart("$\\{1}#\\{2}[\\=3]"));
        assertEquals("{=", TemplateLanguageUtils.unescapeStringLiteralPart("\\{\\="));
        assertEquals("\\=", TemplateLanguageUtils.unescapeStringLiteralPart("\\\\="));
           
        try {
            TemplateLanguageUtils.unescapeStringLiteralPart("\\[");
            fail();
        } catch (GenericParseException e) {
            assertThat(e.getMessage(), containsString("\\["));
        }
    }

    @Test
    public void testEscapeIdentifier() {
        assertNull(TemplateLanguageUtils.escapeIdentifier(null));
        assertEquals("", TemplateLanguageUtils.escapeIdentifier(""));
        assertEquals("a", TemplateLanguageUtils.escapeIdentifier("a"));
        assertEquals("ab", TemplateLanguageUtils.escapeIdentifier("ab"));
        assertEquals("\\.", TemplateLanguageUtils.escapeIdentifier("."));
        assertEquals("\\.\\:\\-", TemplateLanguageUtils.escapeIdentifier(".:-"));
        assertEquals("a\\.b", TemplateLanguageUtils.escapeIdentifier("a.b"));
        assertEquals("a\\.b\\:c\\-d", TemplateLanguageUtils.escapeIdentifier("a.b:c-d"));
    }

    @Test
    public void testIsNonEscapedIdentifierStart() {
        assertTrue(TemplateLanguageUtils.isNonEscapedIdentifierPart('a'));
        assertTrue(TemplateLanguageUtils.isNonEscapedIdentifierPart('á'));
        assertTrue(TemplateLanguageUtils.isNonEscapedIdentifierPart('1'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierPart('-'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierPart(' '));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierPart('\u0000'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierPart('\\'));
    }

    @Test
    public void testisNonEscapedIdentifierStart() {
        assertTrue(TemplateLanguageUtils.isNonEscapedIdentifierStart('a'));
        assertTrue(TemplateLanguageUtils.isNonEscapedIdentifierStart('á'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierStart('1'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierStart('-'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierStart(' '));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierStart('\u0000'));
        assertFalse(TemplateLanguageUtils.isNonEscapedIdentifierStart('\\'));
    }

    @Test
    public void testToStringLiteral() {
        assertNull(TemplateLanguageUtils.toStringLiteral(null));
        assertEquals("\"\"", TemplateLanguageUtils.toStringLiteral(""));
        assertEquals("'foo\"bar\"baaz\\''", TemplateLanguageUtils.toStringLiteral("foo\"bar\"baaz'"));
        assertEquals("\"foo'bar'baaz\\\"\"", TemplateLanguageUtils.toStringLiteral("foo'bar'baaz\""));
        assertEquals("r\"\\d\"", TemplateLanguageUtils.toStringLiteral("\\d"));
        assertEquals("r'\\d\"'", TemplateLanguageUtils.toStringLiteral("\\d\""));
    }

}

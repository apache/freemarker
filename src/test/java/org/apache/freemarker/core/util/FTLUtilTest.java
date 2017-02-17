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

import org.apache.freemarker.core.ast.ParseException;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FTLUtilTest {

    @Test
    public void testEscapeStringLiteralPart() {
        assertEquals("", FTLUtil.escapeStringLiteralPart(""));
        assertEquals("abc", FTLUtil.escapeStringLiteralPart("abc"));
        assertEquals("{", FTLUtil.escapeStringLiteralPart("{"));
        assertEquals("a{b}c", FTLUtil.escapeStringLiteralPart("a{b}c"));
        assertEquals("a#b", FTLUtil.escapeStringLiteralPart("a#b"));
        assertEquals("a$b", FTLUtil.escapeStringLiteralPart("a$b"));
        assertEquals("a#\\{b}c", FTLUtil.escapeStringLiteralPart("a#{b}c"));
        assertEquals("a$\\{b}c", FTLUtil.escapeStringLiteralPart("a${b}c"));
        assertEquals("a'c\\\"d", FTLUtil.escapeStringLiteralPart("a'c\"d", '"'));
        assertEquals("a\\'c\"d", FTLUtil.escapeStringLiteralPart("a'c\"d", '\''));
        assertEquals("a\\'c\"d", FTLUtil.escapeStringLiteralPart("a'c\"d", '\''));
        assertEquals("\\n\\r\\t\\f\\x0002\\\\", FTLUtil.escapeStringLiteralPart("\n\r\t\f\u0002\\"));
        assertEquals("\\l\\g\\a", FTLUtil.escapeStringLiteralPart("<>&"));
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
        assertEquals(partAny, FTLUtil.escapeStringLiteralPart(s));
        assertEquals(partQuot, FTLUtil.escapeStringLiteralPart(s, '\"'));
        assertEquals(partApos, FTLUtil.escapeStringLiteralPart(s, '\''));
        assertEquals(quoted, FTLUtil.toStringLiteral(s));
    }

    @Test
    public void testUnescapeStringLiteralPart() throws ParseException {
        assertEquals("", FTLUtil.unescapeStringLiteralPart(""));
        assertEquals("1", FTLUtil.unescapeStringLiteralPart("1"));
        assertEquals("123", FTLUtil.unescapeStringLiteralPart("123"));
        assertEquals("1&2&3", FTLUtil.unescapeStringLiteralPart("1\\a2\\a3"));
        assertEquals("&", FTLUtil.unescapeStringLiteralPart("\\a"));
        assertEquals("&&&", FTLUtil.unescapeStringLiteralPart("\\a\\a\\a"));
        assertEquals(
                "\u0000\u0000&\u0000\u0000\u0000\u0000",
                FTLUtil.unescapeStringLiteralPart("\\x0000\\x0000\\a\\x0000\\x000\\x00\\x0"));
        assertEquals(
                "'\"\n\b\u0000c><&{\\",
                FTLUtil.unescapeStringLiteralPart("\\'\\\"\\n\\b\\x0000c\\g\\l\\a\\{\\\\"));
    }

    @Test
    public void testEscapeIdentifier() {
        assertNull(FTLUtil.escapeIdentifier(null));
        assertEquals("", FTLUtil.escapeIdentifier(""));
        assertEquals("a", FTLUtil.escapeIdentifier("a"));
        assertEquals("ab", FTLUtil.escapeIdentifier("ab"));
        assertEquals("\\.", FTLUtil.escapeIdentifier("."));
        assertEquals("\\.\\:\\-", FTLUtil.escapeIdentifier(".:-"));
        assertEquals("a\\.b", FTLUtil.escapeIdentifier("a.b"));
        assertEquals("a\\.b\\:c\\-d", FTLUtil.escapeIdentifier("a.b:c-d"));
    }

    @Test
    public void testIsNonEscapedIdentifierStart() {
        assertTrue(FTLUtil.isNonEscapedIdentifierPart('a'));
        assertTrue(FTLUtil.isNonEscapedIdentifierPart('á'));
        assertTrue(FTLUtil.isNonEscapedIdentifierPart('1'));
        assertFalse(FTLUtil.isNonEscapedIdentifierPart('-'));
        assertFalse(FTLUtil.isNonEscapedIdentifierPart(' '));
        assertFalse(FTLUtil.isNonEscapedIdentifierPart('\u0000'));
        assertFalse(FTLUtil.isNonEscapedIdentifierPart('\\'));
    }

    @Test
    public void testisNonEscapedIdentifierStart() {
        assertTrue(FTLUtil.isNonEscapedIdentifierStart('a'));
        assertTrue(FTLUtil.isNonEscapedIdentifierStart('á'));
        assertFalse(FTLUtil.isNonEscapedIdentifierStart('1'));
        assertFalse(FTLUtil.isNonEscapedIdentifierStart('-'));
        assertFalse(FTLUtil.isNonEscapedIdentifierStart(' '));
        assertFalse(FTLUtil.isNonEscapedIdentifierStart('\u0000'));
        assertFalse(FTLUtil.isNonEscapedIdentifierStart('\\'));
    }

}

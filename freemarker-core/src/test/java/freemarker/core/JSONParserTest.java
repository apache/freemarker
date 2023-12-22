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

package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;

public class JSONParserTest {

    @Test
    public void testObjects() throws JSONParser.JSONParseException {
        assertEquals(ImmutableMap.of("a", 1, "b", 2), JSONParser.parse("{\"a\": 1, \"b\": 2}"));
        assertEquals(Collections.emptyMap(), JSONParser.parse("{}"));
        try {
            JSONParser.parse("{1: 1}");
            fail();
        } catch (JSONParser.JSONParseException e) {
            assertThat(e.getMessage(), containsString("string key"));
        }
    }

    @Test
    public void testLists() throws JSONParser.JSONParseException {
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("[1, 2]"));
        assertEquals(Collections.emptyList(), JSONParser.parse("[]"));
    }

    @Test
    public void testStrings() throws JSONParser.JSONParseException {
        assertEquals("", JSONParser.parse("\"\""));
        assertEquals(" ", JSONParser.parse("\" \""));
        assertEquals("'", JSONParser.parse("\"'\""));
        assertEquals("foo", JSONParser.parse("\"foo\""));
        assertEquals("\" \\ / \b \f \n \r \t \ufeff",
                JSONParser.parse(
                        "\"" +
                        "\\\" \\\\ \\/ \\b \\f \\n \\r \\t \\uFEFF" +
                        "\""));
    }

    @Test
    public void testNumbers() throws JSONParser.JSONParseException {
        assertEquals(0, JSONParser.parse("0"));
        assertEquals(123, JSONParser.parse("123"));
        assertEquals(-123, JSONParser.parse("-123"));
        assertNotEquals(123L, JSONParser.parse("123"));
        assertEquals(2147483647, JSONParser.parse("2147483647"));
        assertEquals(2147483648L, JSONParser.parse("2147483648"));
        assertEquals(-2147483648, JSONParser.parse("-2147483648"));
        assertEquals(-2147483649L, JSONParser.parse("-2147483649"));
        assertEquals(-123, JSONParser.parse("-1.23E2"));
        assertEquals(new BigDecimal("1.23"), JSONParser.parse("1.23"));
        assertEquals(new BigDecimal("-1.23"), JSONParser.parse("-1.23"));
        assertEquals(new BigDecimal("12.3"), JSONParser.parse("1.23E1"));
        assertEquals(new BigDecimal("0.123"), JSONParser.parse("123E-3"));
    }

    @Test
    public void testKeywords() throws JSONParser.JSONParseException {
        assertNull(JSONParser.parse("null"));
        assertEquals(true, JSONParser.parse("true"));
        assertEquals(false, JSONParser.parse("false"));
        try {
            JSONParser.parse("NULL");
            fail();
        } catch (JSONParser.JSONParseException e) {
            assertThat(e.getMessage(), containsString("quoted"));
        }
    }

    @Test
    public void testBlockComments() throws JSONParser.JSONParseException {
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("/**/[/**/1/**/, /**/2/**/]/**/"));
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("/*x*/[/*x*/1/*x*/, /*x*/2/*x*/]/*x*/"));
        assertEquals(ImmutableList.of(1), JSONParser.parse(" /*x*/ /**//**/ [ /*x*/ /*\n*//***/ 1 ]"));
        try {
            JSONParser.parse("/*");
            fail();
        } catch (JSONParser.JSONParseException e) {
            assertThat(e.getMessage(), containsString("Unclosed comment"));
        }
        try {
            JSONParser.parse("[/*]");
            fail();
        } catch (JSONParser.JSONParseException e) {
            assertThat(e.getMessage(), containsString("Unclosed comment"));
        }
    }

    @Test
    public void testLineComments() throws JSONParser.JSONParseException {
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("//c1\n[ //c2\n1, //c3\n 2//c5\n] //c4"));
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("// c1\n//\r// c2\r\n// c3\r\n[ 1, 2 ]//"));
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("[1, 2]\n//\n"));
    }

    @Test
    public void testWhitespace() throws JSONParser.JSONParseException {
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("  [  1  ,\n2  ]  "));
        assertEquals(ImmutableList.of(1, 2), JSONParser.parse("\uFEFF[\u00A01\u00A0,2]"));
    }

    @Test
    public void testMixed() throws JSONParser.JSONParseException {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("x", 1);
        m.put("y", null);
        assertEquals(
                ImmutableList.of(
                        ImmutableMap.of("a", Collections.emptyMap()),
                        ImmutableMap.of("b",
                                Arrays.asList(
                                        m,
                                        true,
                                        null
                                ))
                ),
                JSONParser.parse("" +
                        "[\n" +
                            "{\"a\":{}},\n" +
                            "{\"b\":\n" +
                                    "[" +
                                        "{\"x\":1, \"y\": null}," +
                                        "true," +
                                        "null" +
                                    "] // comment\n" +
                            "}\n" +
                        "]"));
    }

    private static void assertEquals(Object expected, TemplateModel actual) {
        try {
            Assert.assertEquals(expected, DeepUnwrap.unwrap(actual));
        } catch (TemplateModelException e) {
            throw new BugException(e);
        }
    }

}
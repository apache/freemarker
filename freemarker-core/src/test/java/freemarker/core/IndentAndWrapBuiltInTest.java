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

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class IndentAndWrapBuiltInTest {

    private String eval(String expr) throws Exception {
        return eval(expr, new HashMap<String, Object>());
    }

    private String eval(String expr, Map<String, Object> model) throws Exception {
        String templateContent = "${" + expr + "}";
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        Template t = new Template("test.ftl", new StringReader(templateContent), cfg);
        StringWriter sw = new StringWriter();
        t.process(model, sw);
        return sw.toString();
    }

    // ---- ?indent tests ----

    @Test
    public void testIndentSingleLine() throws Exception {
        assertEquals("    hello", eval("'hello'?indent('    ')"));
    }

    @Test
    public void testIndentMultiLine() throws Exception {
        assertEquals("  line1\n  line2\n  line3",
                eval("'line1\\nline2\\nline3'?indent('  ')"));
    }

    @Test
    public void testIndentWithPrefix() throws Exception {
        assertEquals(" * line1\n * line2",
                eval("'line1\\nline2'?indent(' * ')"));
    }

    @Test
    public void testIndentEmptyString() throws Exception {
        assertEquals("", eval("''?indent('  ')"));
    }

    @Test
    public void testIndentPreservesBlankLines() throws Exception {
        assertEquals("  a\n\n  b",
                eval("'a\\n\\nb'?indent('  ')"));
    }

    @Test
    public void testIndentTrailingNewline() throws Exception {
        assertEquals("  a\n  b\n",
                eval("'a\\nb\\n'?indent('  ')"));
    }

    // ---- ?wrap tests ----

    @Test
    public void testWrapBasic() throws Exception {
        assertEquals(" * @brief Hello world.\n",
                eval("'Hello world.'?wrap(40, ' * @brief ')"));
    }

    @Test
    public void testWrapLongText() throws Exception {
        String text = "This is a long description that should be wrapped at the specified width";
        Map<String, Object> model = new HashMap<>();
        model.put("text", text);
        String result = eval("text?wrap(40, ' * ', ' * ')", model);
        // Every line should end with \n and be <= 40 chars (excluding \n)
        String[] lines = result.split("\n", -1);
        // Last element is empty after trailing \n
        for (int i = 0; i < lines.length - 1; i++) {
            assertTrue("Line " + i + " too long: [" + lines[i] + "] (" + lines[i].length() + " chars)",
                    lines[i].length() <= 40);
        }
        assertTrue(result.startsWith(" * This"));
    }

    @Test
    public void testWrapWithDifferentPrefixes() throws Exception {
        String text = "This is a description that needs wrapping to fit within bounds";
        Map<String, Object> model = new HashMap<>();
        model.put("text", text);
        String result = eval("text?wrap(40, ' * @brief ', ' *          ')", model);
        assertTrue(result.startsWith(" * @brief "));
        // Second line should start with rest prefix
        String[] lines = result.split("\n");
        if (lines.length > 1) {
            assertTrue("Second line should start with rest prefix",
                    lines[1].startsWith(" *          "));
        }
    }

    @Test
    public void testWrapSamePrefix() throws Exception {
        // Two-arg form: same prefix for all lines
        assertEquals("// hello world\n",
                eval("'hello world'?wrap(40, '// ')"));
    }

    @Test
    public void testWrapSingleLongWord() throws Exception {
        // A single word longer than width — can't break, just emit it
        String result = eval("'superlongword'?wrap(5, '')");
        assertEquals("superlongword\n", result);
    }

    @Test(expected = TemplateException.class)
    public void testWrapZeroWidthThrows() throws Exception {
        eval("'hello'?wrap(0, '')");
    }

    // ---- ?dedent tests ----

    @Test
    public void testDedentBasic() throws Exception {
        assertEquals("int x;\nint y;\n",
                eval("'    int x;\\n    int y;\\n'?dedent('    ')"));
    }

    @Test
    public void testDedentNoMatch() throws Exception {
        // Line doesn't start with prefix — left unchanged
        // "  short" has only 2 spaces, doesn't match 4-space prefix → unchanged
        // "    full" has 4 spaces, matches prefix → stripped
        assertEquals("  short\nfull\n",
                eval("'  short\\n    full\\n'?dedent('    ')"));
    }

    @Test
    public void testDedentMixed() throws Exception {
        // Some lines match, some don't
        assertEquals("a\n  b\nc\n",
                eval("'  a\\n    b\\n  c\\n'?dedent('  ')"));
    }

    @Test
    public void testDedentEmptyString() throws Exception {
        assertEquals("", eval("''?dedent('  ')"));
    }

    @Test
    public void testDedentEmptyPrefix() throws Exception {
        assertEquals("  hello", eval("'  hello'?dedent('')"));
    }

    @Test
    public void testDedentNoTrailingNewline() throws Exception {
        assertEquals("hello",
                eval("'    hello'?dedent('    ')"));
    }

    @Test
    public void testDedentSymmetryWithIndent() throws Exception {
        // indent then dedent should round-trip
        Map<String, Object> model = new HashMap<>();
        model.put("text", "line1\nline2\nline3");
        assertEquals("line1\nline2\nline3",
                eval("text?indent('  ')?dedent('  ')", model));
    }

    // ---- ?pad_lines tests ----

    @Test
    public void testPadLinesBasic() throws Exception {
        assertEquals("a         \nbb        \nccc       \n",
                eval("'a\\nbb\\nccc\\n'?pad_lines(10)"));
    }

    @Test
    public void testPadLinesWithFillChar() throws Exception {
        assertEquals("a.........\nbb........\n",
                eval("'a\\nbb\\n'?pad_lines(10, '.')"));
    }

    @Test
    public void testPadLinesLinePastColumn() throws Exception {
        // "long line" (9 chars) past column 5 — no padding
        // "ab" (2 chars) shorter than column 5 — padded
        assertEquals("long line\nab   \n",
                eval("'long line\\nab\\n'?pad_lines(5)"));
    }

    @Test
    public void testPadLinesNoTrailingNewline() throws Exception {
        assertEquals("a         ",
                eval("'a'?pad_lines(10)"));
    }

    @Test
    public void testPadLinesEmpty() throws Exception {
        assertEquals("", eval("''?pad_lines(10)"));
    }

    @Test
    public void testPadLinesCamelCase() throws Exception {
        assertEquals("a    \nbb   \n",
                eval("'a\\nbb\\n'?padLines(5)"));
    }

    @Test
    public void testPadLinesCodeAlignment() throws Exception {
        // Practical use: align code for trailing comments
        Map<String, Object> model = new HashMap<>();
        model.put("code", "int x;\nString name;\nboolean active;\n");
        String result = eval("code?pad_lines(20)", model);
        String[] lines = result.split("\n", -1);
        assertEquals("int x;              ", lines[0]);
        assertEquals("String name;        ", lines[1]);
        assertEquals("boolean active;     ", lines[2]);
    }
}

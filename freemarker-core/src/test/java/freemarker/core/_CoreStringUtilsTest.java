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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import freemarker.template.utility.StringUtil;
import freemarker.test.hamcerst.Matchers;

public class _CoreStringUtilsTest {

    // ---- indent tests ----

    @Test
    public void testIndentSingleLine() {
        assertEquals(
                "    hello",
                _CoreStringUtils.indent("hello", "    "));
    }

    @Test
    public void testIndentMultiLine() {
        assertEquals(
                "  line1\n  line2\n  line3",
                _CoreStringUtils.indent("line1\nline2\nline3", "  "));
    }

    @Test
    public void testIndentWithPrefix() {
        assertEquals(
                " * line1\n * line2",
                _CoreStringUtils.indent("line1\nline2", " * "));
    }

    @Test
    public void testIndentEmptyString() {
        assertEquals(
                "",
                _CoreStringUtils.indent("", "  "));
    }

    @Test
    public void testIndentPreservesBlankLines() {
        assertEquals(
                "  a\n\n  b",
                _CoreStringUtils.indent("a\n\nb", "  "));
    }

    @Test
    public void testIndentTrailingNewline() {
        assertEquals(
                "  a\n  b\n",
                _CoreStringUtils.indent("a\nb\n", "  "));
    }

    @Test
    public void testIndentNonWhitespacePrefixOnBlankLine() {
        // The prefix is added to the blank line too, then right-trimmed, so it becomes "#" rather
        // than "# " — the space in "# " is a separator, only wanted when there's content after it.
        assertEquals(
                "# a\n#\n# b",
                _CoreStringUtils.indent("a\n\nb", "# "));
    }

    @Test
    public void testIndentWhitespaceOnlyLineTreatedAsBlank() {
        // A line of accidental spaces behaves the same as a truly empty one.
        assertEquals(
                "# a\n#\n# b",
                _CoreStringUtils.indent("a\n   \nb", "# "));
    }

    @Test
    public void testIndentRightTrimOff() {
        assertEquals(
                "# a\n# \n# b",
                _CoreStringUtils.indent("a\n\nb", "# ", false));
    }

    @Test
    public void testIndentRemovesTrailingWhitespaceFromContentLines() {
        assertEquals(
                "  a\n  b",
                _CoreStringUtils.indent("a   \nb\t", "  "));
    }

    @Test
    public void testIndentKeepsNonBreakingSpace() {
        // U+00A0 isn't whitespace for trimming purposes — that's the point of a non-breaking space.
        assertEquals(
                "  a\u00A0",
                _CoreStringUtils.indent("a\u00A0", "  "));
    }

    @Test
    public void testIndentDedentRoundTrip() {
        String original = "int x;\n\nint y;\n";
        assertEquals(
                original,
                _CoreStringUtils.dedent(_CoreStringUtils.indent(original, "    "), "    "));
    }

    // ---- wrap tests ----

    @Test
    public void testWrapBasic() {
        assertEquals(
                " * @brief Hello world.\n",
                _CoreStringUtils.wrap("Hello world.", 40, " * @brief "));
    }

    @Test
    public void testWrapLongTextNoPrefix() {
        testWrapLongText(null, null);
    }

    @Test
    public void testWrapLongTextFirstPrefixOnly() {
        testWrapLongText(" * ", null);
    }

    @Test
    public void testWrapLongTextWithDifferentPrefixes() {
        testWrapLongText(" * @brief ", " *          ");
    }

    private void testWrapLongText(String firstPrefix, String restPrefix) {
        for (int width = 30; width <= 100; width += 10) {
            testWrapLongText(width, firstPrefix, restPrefix);
        }
    }

    private void testWrapLongText(int width, String firstPrefix, String restPrefix) {
        String text = "This is a description that needs wrapping to fit within bounds. Also it's a very long text.";

        String result =
                firstPrefix == null ? _CoreStringUtils.wrap(text, width)
                        : restPrefix == null ? _CoreStringUtils.wrap(text, width, firstPrefix)
                                : _CoreStringUtils.wrap(text, width, firstPrefix, restPrefix);

        String effFirstPrefix = firstPrefix != null ? firstPrefix : "";
        String effRestPrefix = restPrefix != null ? restPrefix : effFirstPrefix;

        assertTrue(result.startsWith(effFirstPrefix));

        // Second line should start with rest prefix
        String[] lines = result.split("\n", -1);
        for (int i = 1; i < lines.length - 1; i++) {
            String line = lines[i];
            if (line.length() > width) {
                fail("Line " + i + " is too long: " + StringUtil.jQuote(line));
            }
            if (!line.startsWith(effRestPrefix)) {
                fail("Line " + i + " doesn't start as expected: " + StringUtil.jQuote(line));
            }
        }

        assertEquals("", lines[lines.length - 1]);
        assertTrue(result.endsWith("\n"));
    }

    @Test
    public void testWrapSamePrefix() {
        assertEquals(
                "// hello world\n",
                _CoreStringUtils.wrap("hello world", 40, "// "));
    }

    @Test
    public void testWrapSingleLongWord() {
        // A single word longer than width — can't break, just emit it
        assertEquals(
                "superlongword\n",
                _CoreStringUtils.wrap("superlongword", 4, ""));
        // Not even after the prefix
        assertEquals(
                "  *  superlongword\n",
                _CoreStringUtils.wrap("superlongword", 4, "  *  "));
    }

    @Test
    public void testWrapCollapsesWhitespaces() {
        assertEquals(
                "a b c d e\n",
                _CoreStringUtils.wrap("  a  \n b \n c\t\td   e  ", 40, ""));
    }

    @Test
    public void testWrapWithNbsp() {
        // No NBSP:
        assertEquals(
                "word1\nword2\nword3\nword4\n",
                _CoreStringUtils.wrap("word1 word2 word3 word4", 4));
        // With NBSP:
        assertEquals(
                "word1\u00A0word2\u00A0word3\u00A0word4\n",
                _CoreStringUtils.wrap("word1\u00A0word2\u00A0word3\u00A0word4", 4));
        assertEquals(
                "word1\u00A0word2\nword3\u00A0word4\n",
                _CoreStringUtils.wrap("word1\u00A0word2 word3\u00A0word4", 4));
        assertEquals(
                "word1\u00A0\nword2\n\u00A0word3\n",
                _CoreStringUtils.wrap("word1\u00A0 word2 \u00A0word3", 4));
        assertEquals(
                "\u00A0 a \u00A0\u00A0 b \u00A0\n",
                _CoreStringUtils.wrap(" \u00A0  a  \u00A0\u00A0 b \u00A0  ", 40, ""));
    }

    @Test
    public void testWrapWithInputLeadingTrailingEmptyLinesDoesntMatter() {
        assertEquals(
                "word1\nword2\n",
                _CoreStringUtils.wrap("word1 word2", 4));
        assertEquals(
                "word1\nword2\n",
                _CoreStringUtils.wrap("word1 word2\n", 4));
        assertEquals(
                "word1\nword2\n",
                _CoreStringUtils.wrap("\n\nword1 word2\n\n", 4));
    }

    @Test
    public void testWrapZeroWidthThrows() {
        try {
            _CoreStringUtils.wrap("hello", 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(
                    e.getMessage(),
                    Matchers.containsStringIgnoringCase("must be at least 1"));
        }
    }

    // ---- dedent tests ----

    @Test
    public void testDedentBasic() {
        assertEquals(
                "int x;\nint y;\n",
                _CoreStringUtils.dedent("    int x;\n    int y;\n", "    ")
        );
    }

    @Test
    public void testDedentPartialPrefixIsShortened() {
        // A line carrying only part of the prefix loses that part, rather than being left alone.
        // "  short" shares 2 characters with the 4-space prefix → those 2 are removed.
        // "    full" carries the whole prefix → all 4 are removed.
        assertEquals(
                "short\nfull\n",
                _CoreStringUtils.dedent("  short\n    full\n", "    ")
        );
    }

    @Test
    public void testDedentPartialPrefixNonWhitespace() {
        // Every one of these loses whatever it shares with "---", so all end up as "a".
        assertEquals(
                "a\na\na\na\n",
                _CoreStringUtils.dedent("a\n-a\n--a\n---a\n", "---")
        );
    }

    @Test
    public void testDedentUnrelatedPrefixLeftAlone() {
        // Shares nothing with the prefix → untouched.
        assertEquals(
                "xa\nyb\n",
                _CoreStringUtils.dedent("xa\nyb\n", "---")
        );
    }

    @Test
    public void testDedentWhitespaceOnlyLineBecomesEmpty() {
        // The 2 spaces are all this line shares with the 4-space prefix, so it's left empty
        // instead of keeping accidental trailing whitespace.
        assertEquals(
                "a\n\nb\n",
                _CoreStringUtils.dedent("    a\n  \n    b\n", "    ")
        );
    }

    @Test
    public void testDedentMixed() {
        // Some lines match, some don't
        assertEquals(
                "a\n  b\nc\n",
                _CoreStringUtils.dedent("  a\n    b\n  c\n", "  ")
        );
    }

    @Test
    public void testDedentEmptyString() {
        assertEquals(
                "",
                _CoreStringUtils.dedent("", "  ")
        );
    }

    @Test
    public void testDedentEmptyPrefix() {
        assertEquals(
                "  hello",
                _CoreStringUtils.dedent("  hello", "")
        );
    }

    @Test
    public void testDedentNoTrailingNewline() {
        assertEquals(
                "hello",
                _CoreStringUtils.dedent("    hello", "    ")
        );
    }

    @Test
    public void testDedentSymmetryWithIndent() {
        // indent then dedent should round-trip
        String text = "line1\n line2\nline3";
        String prefix = "  ";
        assertEquals(
                text,
                _CoreStringUtils.dedent(_CoreStringUtils.indent(text, prefix), prefix)
        );
    }

    // ---- dedent no-args (Python textwrap.dedent-style) tests ----

    @Test
    public void testDedentNoArgsUniformIndent() {
        assertEquals(
                "a\nb\nc",
                _CoreStringUtils.dedent("    a\n    b\n    c")
        );
    }

    @Test
    public void testDedentNoArgsMixedIndent() {
        // The longest common leading whitespace across non-empty lines is 2 spaces.
        assertEquals(
                "a\n  b\n    c",
                _CoreStringUtils.dedent("  a\n    b\n      c")
        );
    }

    @Test
    public void testDedentNoArgsRespectsEmptyLines() {
        // Empty/whitespace-only lines are ignored when computing the common prefix.
        assertEquals(
                "a\n\nb",
                _CoreStringUtils.dedent("    a\n\n    b")
        );
    }

    @Test
    public void testDedentNoArgsNormalizesWhitespaceOnlyLines() {
        // A whitespace-only line doesn't constrain the common prefix, and comes out empty rather
        // than keeping whitespace that was accidental to begin with. Same as textwrap.dedent.
        assertEquals(
                "a\n\nb",
                _CoreStringUtils.dedent("    a\n  \n    b")
        );
        // Including when it's longer than the common prefix.
        assertEquals(
                "a\n\nb",
                _CoreStringUtils.dedent("    a\n        \n    b")
        );
    }

    @Test
    public void testDedentNoArgsNoCommonPrefix() {
        // If lines have no common leading whitespace, nothing is stripped.
        assertEquals(
                "a\n    b",
                _CoreStringUtils.dedent("a\n    b")
        );
    }

    @Test
    public void testDedentNoArgsTabAndSpaceDistinct() {
        // A leading tab and a leading space have no common prefix.
        // (Same behaviour as Python textwrap.dedent.)
        assertEquals(
                "\ta\n    b",
                _CoreStringUtils.dedent("\ta\n    b")
        );
    }

    @Test
    public void testDedentNoArgsTabsOnly() {
        assertEquals(
                "a\nb",
                _CoreStringUtils.dedent("\t\ta\n\t\tb")
        );
    }

    @Test
    public void testDedentNoArgsEmptyString() {
        assertEquals(
                "",
                _CoreStringUtils.dedent("")
        );
    }

    @Test
    public void testDedentNoArgsAlreadyDedented() {
        // No common leading whitespace => no change.
        assertEquals(
                "a\nb\nc",
                _CoreStringUtils.dedent("a\nb\nc")
        );
    }

}

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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import freemarker.template.utility.StringUtil;
import freemarker.test.hamcerst.Matchers;

@RunWith(Enclosed.class)
public class _CoreStringUtilsTest {

    /**
     * Tests of {@code indent} and {@code dedent}, repeated for each line-break type, and with and without a
     * line-break at the end of the input.
     *
     * <p>The test data is written with {@code "\n"} as the line-break and with no line-break at the end;
     * {@link #lb(String)} adapts both the input and the expected value to the parameters of the actual run. So
     * {@code assertIndent("  a\n  b", "a\nb", "  ")} also checks
     * {@code indent("a\r\nb\r\n", "  ") == "  a\r\n  b\r\n"}, and so on.
     */
    @RunWith(Parameterized.class)
    public static class IndentAndDedentTest {

        @Parameters(name = "{0}, trailingLineBreak={2}")
        public static Collection<Object[]> parameters() {
            Collection<Object[]> result = new ArrayList<>();
            for (String[] lineBreak : new String[][] { { "LF", "\n" }, { "CRLF", "\r\n" }, { "CR", "\r" } }) {
                for (boolean trailingLineBreak : new boolean[] { false, true }) {
                    result.add(new Object[] { lineBreak[0], lineBreak[1], trailingLineBreak });
                }
            }
            return result;
        }

        private final String lineBreak;
        private final boolean trailingLineBreak;

        public IndentAndDedentTest(String lineBreakName, String lineBreak, boolean trailingLineBreak) {
            this.lineBreak = lineBreak;
            this.trailingLineBreak = trailingLineBreak;
        }

        /**
         * Adapts test data written with {@code "\n"} and without a trailing line-break to the parameters of this run.
         */
        private String lb(String s) {
            return s.replace("\n", lineBreak) + (trailingLineBreak ? lineBreak : "");
        }

        private void assertIndent(String expected, String s, String prefix) {
            assertEquals(lb(expected), _CoreStringUtils.indent(lb(s), prefix));
        }

        private void assertIndent(String expected, String s, String prefix, boolean rightTrim) {
            assertEquals(lb(expected), _CoreStringUtils.indent(lb(s), prefix, rightTrim));
        }

        private void assertDedent(String expected, String s, String prefixToRemove) {
            assertEquals(lb(expected), _CoreStringUtils.dedent(lb(s), prefixToRemove));
        }

        private void assertDedent(String expected, String s, String prefixToRemove, boolean rightTrim) {
            assertEquals(lb(expected), _CoreStringUtils.dedent(lb(s), prefixToRemove, rightTrim));
        }

        private void assertDedent(String expected, String s) {
            assertEquals(lb(expected), _CoreStringUtils.dedent(lb(s)));
        }

        // ---- indent ----

        @Test
        public void testIndentSingleLine() {
            assertIndent("    hello", "hello", "    ");
        }

        @Test
        public void testIndentMultiLine() {
            assertIndent("  line1\n  line2\n  line3", "line1\nline2\nline3", "  ");
        }

        @Test
        public void testIndentNonWhitespacePrefix() {
            assertIndent(" * line1\n * line2", "line1\nline2", " * ");
        }

        @Test
        public void testIndentEmptyString() {
            assertIndent("", "", "  ");
        }

        @Test
        public void testIndentWhitespacePrefixLeavesBlankLinesEmpty() {
            // The prefix is added to the blank line too, but then trimmed away again.
            assertIndent("  a\n\n  b", "a\n\nb", "  ");
        }

        @Test
        public void testIndentNonWhitespacePrefixOnBlankLine() {
            // The blank line becomes "#" rather than "# ": the space in "# " is only meant to separate
            // the prefix from the content of a line, and there's no content here.
            assertIndent("# a\n#\n# b", "a\n\nb", "# ");
        }

        @Test
        public void testIndentWhitespaceOnlyLineTreatedAsBlank() {
            // A line of accidental spaces gives the same result as a truly empty one.
            assertIndent("# a\n#\n# b", "a\n   \nb", "# ");
        }

        @Test
        public void testIndentRightTrimOff() {
            assertIndent("# a\n# \n# b", "a\n\nb", "# ", false);
            assertIndent("#    \n# a", "   \na", "# ", false);
        }

        @Test
        public void testIndentRemovesTrailingWhitespaceOfContentLines() {
            assertIndent("  a\n  b", "a   \nb\t", "  ");
        }

        @Test
        public void testIndentKeepsNonBreakingSpace() {
            // U+00A0 isn't whitespace as far as trimming is concerned; that's the point of it.
            assertIndent("  a\u00A0", "a\u00A0", "  ");
        }

        @Test
        public void testIndentEmptyPrefixDoesNothing() {
            // Nothing to add, so nothing happens; not even the trailing whitespace is trimmed.
            assertIndent("a  \n  \nb", "a  \n  \nb", "");
        }

        // ---- dedent with an explicit prefix ----

        @Test
        public void testDedentBasic() {
            assertDedent("int x;\nint y;", "    int x;\n    int y;", "    ");
        }

        @Test
        public void testDedentPartialPrefixIsShortened() {
            // "  short" shares 2 characters with the 4 character long prefix, so it loses those 2.
            // "    full" starts with the whole prefix, so it loses all 4.
            assertDedent("short\nfull", "  short\n    full", "    ");
        }

        @Test
        public void testDedentPartialPrefixNonWhitespace() {
            assertDedent("a\na\na\na\nx123a\n23a", "a\n1a\n12a\n123a\nx123a\n23a", "123");
        }

        @Test
        public void testDedentUnrelatedPrefixLeftAlone() {
            assertDedent("xa\nyb", "xa\nyb", "---");
        }

        @Test
        public void testDedentWhitespaceOnlyLineIsTrimmed() {
            // The 3rd line only shares 4 of its 5 spaces with the prefix; the leftover space is trimmed
            // away, so the line ends up empty, as with indent.
            assertDedent("a\n\nb\n", "    a\n  \n    b\n     ", "    ");
        }

        @Test
        public void testDedentWhitespaceOnlyLineKeptWithTrimOff() {
            assertDedent("a\n\nb\n ", "    a\n  \n    b\n     ", "    ", false);
        }

        @Test
        public void testDedentRemovesTrailingWhitespaceOfContentLines() {
            assertDedent("a\nb", "    a   \n    b\t", "    ");
            assertDedent("a   \nb\t", "    a   \n    b\t", "    ", false);
        }

        @Test
        public void testDedentMixed() {
            assertDedent("a\n  b\nc", "  a\n    b\n  c", "  ");
        }

        @Test
        public void testDedentEmptyString() {
            assertDedent("", "", "  ");
        }

        @Test
        public void testDedentEmptyPrefixDoesNothing() {
            // Nothing to remove, so nothing happens; not even the trailing whitespace is trimmed.
            assertDedent("  hello", "  hello", "");
            assertDedent("  hello   ", "  hello   ", "");
        }

        @Test
        public void testIndentDedentRoundTrip() {
            String prefix = "  ";
            // Note that the blank line must have no trailing whitespace for this to round-trip, which is
            // what the trimming of both built-ins ensures.
            String s = lb("line1\n line2\n\nline3");
            assertEquals(s, _CoreStringUtils.dedent(_CoreStringUtils.indent(s, prefix), prefix));
        }

        // ---- dedent with no prefix (Python textwrap.dedent-style) ----

        @Test
        public void testDedentNoArgsUniformIndent() {
            assertDedent("a\nb\nc", "    a\n    b\n    c");
        }

        @Test
        public void testDedentNoArgsMixedIndent() {
            // The longest common leading whitespace across non-empty lines is 2 spaces.
            assertDedent("a\n  b\n    c", "  a\n    b\n      c");
        }

        @Test
        public void testDedentNoArgsRespectsEmptyLines() {
            // Empty lines are ignored when computing the common prefix.
            assertDedent("a\n\nb", "    a\n\n    b");
        }

        @Test
        public void testDedentNoArgsNormalizesWhitespaceOnlyLines() {
            // A whitespace-only line doesn't constrain the common prefix, and comes out empty rather than
            // keeping whitespace that was accidental to begin with. Same as textwrap.dedent.
            assertDedent("a\n\nb", "    a\n  \n    b");
            // Including when it has more whitespace than the common prefix:
            assertDedent("a\n\nb", "    a\n        \n    b");
        }

        @Test
        public void testDedentNoArgsNoCommonPrefix() {
            assertDedent("a\n    b", "a\n    b");
        }

        @Test
        public void testDedentNoArgsTabAndSpaceDistinct() {
            // A leading tab and a leading space have no common prefix. (Same as textwrap.dedent.)
            assertDedent("\ta\n    b", "\ta\n    b");
        }

        @Test
        public void testDedentNoArgsTabsOnly() {
            assertDedent("a\nb", "\t\ta\n\t\tb");
        }

        @Test
        public void testDedentNoArgsEmptyString() {
            assertDedent("", "");
        }

        @Test
        public void testDedentNoArgsAlreadyDedented() {
            assertDedent("a\nb\nc", "a\nb\nc");
        }
    }

    /**
     * Tests of {@code wrap}, which doesn't preserve the line structure of its input (it collapses all whitespace,
     * including line-breaks), and so isn't affected by the line-break type of the input.
     */
    public static class WrapTest {

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
    }

}

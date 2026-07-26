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
    public void testDedentNoMatch() {
        // Line doesn't start with prefix — left unchanged
        // "  short" has only 2 spaces, doesn't match 4-space prefix → unchanged
        // "    full" has 4 spaces, matches prefix → stripped
        assertEquals(
                "  short\nfull\n",
                _CoreStringUtils.dedent("  short\n    full\n", "    ")
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
        // Empty/whitespace-only lines are ignored when computing the common prefix
        // and pass through unchanged.
        assertEquals(
                "a\n\nb",
                _CoreStringUtils.dedent("    a\n\n    b")
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

    // ---- rightPadLines tests ----

    @Test
    public void testRightPadLinesBasic() {
        assertEquals(
                "a         \nbb        \nccc       \n",
                _CoreStringUtils.rightPadLines("a\nbb\nccc\n", 10)
        );
    }

    @Test
    public void testRightPadLinesWithFillChar() {
        assertEquals(
                "a.........\nbb........\n",
                _CoreStringUtils.rightPadLines("a\nbb\n", 10, '.')
        );
    }

    @Test
    public void testRightPadLinesLinePastColumn() {
        // "long line" (9 chars) past column 5 — no padding
        // "ab" (2 chars) shorter than column 5 — padded
        assertEquals(
                "long line\nab   \n",
                _CoreStringUtils.rightPadLines("long line\nab\n", 5)
        );
    }

    @Test
    public void testRightPadLinesNoTrailingNewline() {
        assertEquals(
                "a         ",
                _CoreStringUtils.rightPadLines("a", 10)
        );
    }

    @Test
    public void testRightPadLinesEmpty() {
        assertEquals(
                "",
                _CoreStringUtils.rightPadLines("", 10)
        );
    }

    @Test
    public void testRightPadLinesCamelCase() {
        assertEquals(
                "a    \nbb   \n",
                _CoreStringUtils.rightPadLines("a\nbb\n", 5)
        );
    }

    @Test
    public void testRightPadLinesCodeAlignment() {
        // Practical use: align code for trailing comments
        String code = "int x;\nString name;\nboolean active;\n";
        String result = _CoreStringUtils.rightPadLines(code, 20);
        String[] lines = result.split("\n", -1);
        assertEquals("int x;              ", lines[0]);
        assertEquals("String name;        ", lines[1]);
        assertEquals("boolean active;     ", lines[2]);
        assertEquals("", lines[3]);
    }

}

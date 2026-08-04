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

import java.util.Collection;

import freemarker.template.Configuration;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.StringUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all! This class is to
 * work around the lack of module system in Java, i.e., so that other FreeMarker packages can access things inside this
 * package that users shouldn't.
 */
public final class _CoreStringUtils {

    private _CoreStringUtils() {
        // No meant to be instantiated
    }

    public static String toFTLIdentifierReferenceAfterDot(String name) {
        return backslashEscapeIdentifier(name);
    }

    public static String toFTLTopLevelIdentifierReference(String name) {
        return backslashEscapeIdentifier(name);
    }

    public static String toFTLTopLevelTragetIdentifier(final String name) {
        char quotationType = 0;
        scanForQuotationType:
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(i == 0 ? StringUtil.isFTLIdentifierStart(c) : StringUtil.isFTLIdentifierPart(c)) && c != '@') {
                if ((quotationType == 0 || quotationType == '\\')
                        && StringUtil.isBackslashEscapedFTLIdentifierCharacter(c)) {
                    quotationType = '\\';
                } else {
                    quotationType = '"';
                    break scanForQuotationType;
                }
            }
        }
        switch (quotationType) {
            case 0:
                return name;
            case '"':
                return StringUtil.ftlQuote(name);
            case '\\':
                return backslashEscapeIdentifier(name);
            default:
                throw new BugException();
        }
    }

    /*
     * Escapes an identifier. This assumes that the identifier was once accepted by the parser, thus it is properly
     * escapeable. Invalid characters that can't be escaped will be left as is. (This is actually feature because of
     * historically weirdness, like that a sole {@code *} is a valid subvariable name, which must not be escaped.)
     */
    public static String backslashEscapeIdentifier(String name) {
        StringBuilder sb = null;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (StringUtil.isBackslashEscapedFTLIdentifierCharacter(c)) {
                if (sb == null) {
                    sb = new StringBuilder(name.length() + 8);
                    sb.append(name, 0, i);
                }
                sb.append('\\');
            }
            if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? name : sb.toString();
    }

    /**
     * @return {@link Configuration#CAMEL_CASE_NAMING_CONVENTION}, or {@link Configuration#LEGACY_NAMING_CONVENTION} or,
     * {@link Configuration#AUTO_DETECT_NAMING_CONVENTION} when undecidable.
     */
    public static int getIdentifierNamingConvention(String name) {
        final int ln = name.length();
        for (int i = 0; i < ln; i++) {
            final char c = name.charAt(i);
            if (c == '_') {
                return Configuration.LEGACY_NAMING_CONVENTION;
            }
            if (isUpperUSASCII(c)) {
                return Configuration.CAMEL_CASE_NAMING_CONVENTION;
            }
        }
        return Configuration.AUTO_DETECT_NAMING_CONVENTION;
    }

    // [2.4] Won't be needed anymore

    /**
     * A deliberately very inflexible camel case to underscored converter; it must not convert improper camel case names
     * to a proper underscored name.
     */
    public static String camelCaseToUnderscored(String camelCaseName) {
        int i = 0;
        while (i < camelCaseName.length() && Character.isLowerCase(camelCaseName.charAt(i))) {
            i++;
        }
        if (i == camelCaseName.length()) {
            // No conversion needed
            return camelCaseName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(camelCaseName.substring(0, i));
        while (i < camelCaseName.length()) {
            final char c = camelCaseName.charAt(i);
            if (isUpperUSASCII(c)) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    public static boolean isUpperUSASCII(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static String commaSeparatedJQuotedItems(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(StringUtil.jQuote(item));
        }
        return sb.toString();
    }

    /**
     * Same as {@link #indent(String, String, boolean)} with {@code rightTrim} set to {@code true}.
     */
    public static String indent(String s, String prefix) {
        return indent(s, prefix, true);
    }

    /**
     * Prepends {@code prefix} to each line, then, if {@code rightTrim} is {@code true}, removes the trailing whitespace
     * of each resulting line.
     *
     * <p>The prefix is added unconditionally, including to lines that are empty or contain whitespace only. The
     * right-trimming is what keeps that from leaving superfluous whitespace behind: with a prefix like {@code "# "} an
     * empty line becomes {@code "#"} rather than a line with a trailing space, and with a whitespace-only prefix it
     * becomes empty. That's also why empty and whitespace-only lines end up treated alike, without either being a
     * special case in the code.
     *
     * <p>Note that a non-breaking space (U+00A0) isn't whitespace as far as trimming is concerned, so it's kept.
     */
    public static String indent(String s, String prefix, boolean rightTrim) {
        // An empty prefix adds nothing, so this does nothing at all then, not even trimming; that's the least
        // surprising behavior for something called "indent".
        if (s == null || s.isEmpty() || prefix.isEmpty()) {
            return s;
        }

        int len = s.length();
        StringBuilder sb = new StringBuilder(len + prefix.length() * (4 + len / 20));
        int lineStartPos = 0;
        while (lineStartPos < len) {
            int lineBreakPos = findLineBreakFrom(s, lineStartPos);

            int lineStartPosSb = sb.length();
            sb.append(prefix);
            sb.append(s, lineStartPos, lineBreakPos);
            if (rightTrim) {
                int end = sb.length();
                while (end > lineStartPosSb && isTrimmableInlineWhitespace(sb.charAt(end - 1))) {
                    end--;
                }
                sb.setLength(end);
            }

            lineStartPos = appendSameTypeLineBreak(sb, s, lineBreakPos);
        }
        return sb.toString();
    }

    /**
     * Removes from each line the longest prefix of {@code prefixToRemove} that the line starts with. Lines that start
     * with the whole prefix lose all of it; lines that only start with some head part of the prefix lose that part;
     * lines that share nothing with it are left alone.
     *
     * <p>This is consistent with how code editors behave when you repeatedly dedent a block of code, and some lines
     * reach 0 indentation earlier than others. In that case, the dedent is not blocked, instead the code hierarchy will
     * start to flatten (so you lose information, be it's tolerated as  least wrong outcome).
     *
     * <p>Also, an all-or-nothing dedent could leave a line that was originally the least indented as the most
     * indented one, which is much more confusing that a (partially) flattened hierarchy.
     */
    public static String dedent(String s, String prefixToRemove) {
        return dedent(s, prefixToRemove, true);
    }

    /**
     * Same as {@link #dedent(String, String)}, but you can also specify if the trailing whitespace of each resulting
     * line should be removed.
     *
     * <p>The trimming matters because removing a prefix can leave whitespace behind that only looks like indentation:
     * a line that contains whitespace only loses just as much of it as the prefix is long, so with a 4 character long
     * prefix a line of 5 spaces would keep 1 space. Trimming removes such remains, and so a line that contains
     * whitespace only becomes empty. It also keeps this symmetrical with
     * {@link #indent(String, String, boolean)}, which trims too.
     */
    public static String dedent(String s, String prefixToRemove, boolean rightTrim) {
        // An empty prefix removes nothing, so this does nothing at all then, not even trimming; same as with
        // indent.
        if (s == null || s.isEmpty() || prefixToRemove.isEmpty()) {
            return s;
        }

        int prefixToRemoveLen = prefixToRemove.length();
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        int lineStartPos = 0;
        while (lineStartPos < len) {
            int lineBreakPos = findLineBreakFrom(s, lineStartPos);

            int matchedLen = 0;
            while (matchedLen < prefixToRemoveLen && lineStartPos + matchedLen < lineBreakPos
                    && s.charAt(lineStartPos + matchedLen) == prefixToRemove.charAt(matchedLen)) {
                matchedLen++;
            }
            int lineStartPosSb = sb.length();
            sb.append(s, lineStartPos + matchedLen, lineBreakPos);
            if (rightTrim) {
                int end = sb.length();
                while (end > lineStartPosSb && isTrimmableInlineWhitespace(sb.charAt(end - 1))) {
                    end--;
                }
                sb.setLength(end);
            }

            lineStartPos = appendSameTypeLineBreak(sb, s, lineBreakPos);
        }
        return sb.toString();
    }

    /**
     * Returns the index of the first line-break character at or after {@code from}, or the length of {@code s} if
     * there's none.
     */
    private static int findLineBreakFrom(String s, int from) {
        int len = s.length();
        int i = from;
        while (i < len && !isLineBreakChar(s.charAt(i))) {
            i++;
        }
        return i;
    }

    /**
     * Appends the same type of line-break (LF, or CRLF, or CR) to {@code sb}, which is found at {@code lineBreakPos} in
     * {@code s}; if the position is after the last char of {@code s}, then this appends nothing.
     *
     * @return the position at which the next line starts in {@code s}
     */
    private static int appendSameTypeLineBreak(StringBuilder sb, String s, int lineBreakPos) {
        int len = s.length();
        if (lineBreakPos >= len) {
            return len;
        }
        char c = s.charAt(lineBreakPos);
        sb.append(c);
        if (c == '\r' && lineBreakPos + 1 < len && s.charAt(lineBreakPos + 1) == '\n') {
            sb.append('\n');
            return lineBreakPos + 2;
        }
        return lineBreakPos + 1;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isLineBreakChar(char c) {
        return c == '\r' || c == '\n';
    }

    /**
     * Whether the character counts as removable whitespace inside a line. Anything that satisfies
     * {@link Character#isWhitespace(char)} and is not a line terminator. Note the non-breaking space is not considered
     * as trimmable.
     */
    private static boolean isTrimmableInlineWhitespace(char c) {
        return !isLineBreakChar(c) && Character.isWhitespace(c);
    }

    /**
     * Strip the longest leading indentation-white-space string (contains spaces and tabs only) that is a common amongst
     * of all non-blank line (blank meaning only containing indentation-white-space). Lines that are blank are ignored
     * and are 0-length in the output. Mirrors Python's {@code textwrap.dedent} semantics.
     *
     * <p>Note: this methods can't resolve tabs to spaces, nor convert spaces to tabs, so a tab in line only matches
     * another tab in the other lines, not some number spaces.
     */
    public static String dedent(String s) {
        if (s.isEmpty()) {
            return s;
        }

        String commonPrefix = getLongestCommonIndentationWhitespace(s);
        if (commonPrefix == null || commonPrefix.isEmpty()) {
            return s;
        }

        return removeCommonIndentationWhitespacePrefixFromLines(s, commonPrefix);
    }

    /**
     * Finds the longest common leading white-space indentation of the lines, but ignoring lines that only contains
     * indentation-white-space.
     */
    private static String getLongestCommonIndentationWhitespace(String s) {
        int len = s.length();

        String longestCommonIndent = null;
        int lineStartPos = 0;
        int contentEndPos;
        while (lineStartPos < len) {
            char c = s.charAt(lineStartPos);

            int identEndPos = lineStartPos;
            while (isIndentationWhitespace(c)) {
                identEndPos++;
                if (identEndPos == len) {
                    break;
                }
                c = s.charAt(identEndPos);
            }

            contentEndPos = identEndPos;
            while (!isLineBreakChar(c)) {
                contentEndPos++;
                if (contentEndPos == len) {
                    break;
                }
                c = s.charAt(contentEndPos);
            }

            if (identEndPos < contentEndPos) {
                if (longestCommonIndent == null) {
                    longestCommonIndent = s.substring(lineStartPos, identEndPos);
                } else {
                    int maxLen = Math.min(longestCommonIndent.length(), identEndPos - lineStartPos);
                    int matchingLen = 0;
                    while (matchingLen < maxLen
                            && longestCommonIndent.charAt(matchingLen) == s.charAt(lineStartPos + matchingLen)) {
                        matchingLen++;
                    }
                    if (matchingLen < longestCommonIndent.length()) {
                        longestCommonIndent = longestCommonIndent.substring(0, matchingLen);
                    }
                    if (longestCommonIndent.isEmpty()) {
                        return longestCommonIndent; // can't shrink further; finish quickly
                    }
                }
            }

            if (contentEndPos < len) {
                if (c == '\r' && contentEndPos + 1 < len && s.charAt(contentEndPos + 1) == '\n') {
                    // Skip CRLF
                    contentEndPos += 2;
                } else {
                    contentEndPos++;
                }
            }

            lineStartPos = contentEndPos;
        }
        return longestCommonIndent;
    }

    /**
     * Removes common prefix from each line, but replace indentation-white-space-only lines with empty line. We assume
     * that each non-blank line starts with the given common prefix, and that it only contains indentation white-space,
     * otherwise behavior is undefined.
     */
    private static String removeCommonIndentationWhitespacePrefixFromLines(String s, String commonPrefix) {
        int len = s.length();
        int prefixLen = commonPrefix.length();
        StringBuilder sb = new StringBuilder(len);
        int lineStartPos = 0;
        while (lineStartPos < len) {
            int lineBreakPos = lineStartPos;
            boolean blankLine = true;
            while (lineBreakPos < len) {
                char c = s.charAt(lineBreakPos);
                if (isLineBreakChar(c)) {
                    break;
                }
                if (blankLine && !isIndentationWhitespace(c)) {
                    blankLine = false;
                    // Don't break from the loop! It's faster and more logical to get to the line-break in this loop.
                }
                lineBreakPos++;
            }

            // An identation-whitespace-only line is normalized to empty rather than kept as is. Its whitespace is
            // assumed to be accidental. This also matches Python textwrap.dedent behavior.
            if (!blankLine) {
                sb.append(s, lineStartPos + prefixLen, lineBreakPos);
            }
            if (lineBreakPos < len) {
                lineBreakPos = appendSameTypeLineBreak(sb, s, lineBreakPos);
            }
            lineStartPos = lineBreakPos;
        }
        return sb.toString();
    }

    private static boolean isIndentationWhitespace(char c) {
        return c == ' ' || c == '\t';
    }

    public static String wrap(String s, int width) {
        return wrap(s, width, "");
    }

    public static String wrap(String s, int width, String firstPrefix) {
        return wrap(s, width, firstPrefix, firstPrefix);
    }

    /**
     * In effect trims the input, breaks it to item at whitespace (except unbreakable space), and the reflow the items
     * into space-separated lines no longer than the specified width, or a single item if that's longer. Inside a line,
     * all interleaving whitespace is replaced with a single space. Unbreakable space is not considered to be
     * white-space here.
     *
     * @param width
     *         Maximum line width, when possible. If a single unbreakable section is longer than this, then that will be
     *         in its own line.
     * @param firstPrefix
     *         Prefix of the first line
     * @param restPrefix
     *         Prefix of all lines after the first
     */
    public static String wrap(String s, int width, String firstPrefix, String restPrefix) {
        NullArgumentException.check(firstPrefix, "firstPrefix");
        NullArgumentException.check(restPrefix, "restPrefix");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be at least 1");
        }

        String[] words = s.split("\\s+");
        if (words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
            return firstPrefix + "\n";
        }

        StringBuilder sb = new StringBuilder();
        String currentPrefix = firstPrefix;
        int lineLen = currentPrefix.length();
        sb.append(currentPrefix);
        boolean firstWord = true;

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (firstWord) {
                sb.append(word);
                lineLen += word.length();
                firstWord = false;
            } else {
                if (lineLen + 1 + word.length() > width) {
                    sb.append('\n');
                    currentPrefix = restPrefix;
                    sb.append(currentPrefix);
                    sb.append(word);
                    lineLen = currentPrefix.length() + word.length();
                } else {
                    sb.append(' ');
                    sb.append(word);
                    lineLen += 1 + word.length();
                }
            }
        }
        sb.append('\n');
        return sb.toString();
    }

}

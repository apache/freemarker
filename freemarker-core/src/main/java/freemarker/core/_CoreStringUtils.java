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
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
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
        scanForQuotationType: for (int i = 0; i < name.length(); i++) {
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
     * @return {@link Configuration#CAMEL_CASE_NAMING_CONVENTION}, or {@link Configuration#LEGACY_NAMING_CONVENTION}
     *         or, {@link Configuration#AUTO_DETECT_NAMING_CONVENTION} when undecidable.
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
     * A deliberately very inflexible camel case to underscored converter; it must not convert improper camel case
     * names to a proper underscored name.
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
     * Prepends {@code prefix} to each line, then, if {@code rightTrim} is {@code true}, removes the trailing
     * whitespace of each resulting line.
     *
     * <p>The prefix is added unconditionally, including to lines that are empty or contain whitespace only. The
     * right-trimming is what keeps that from leaving junk behind: with a prefix like {@code "# "} an empty line
     * becomes {@code "#"} rather than a line with a trailing space, and with a whitespace-only prefix it becomes
     * empty. That's also why empty and whitespace-only lines end up treated alike, without either being a special
     * case in the code.
     *
     * <p>Note that a non-breaking space (U+00A0) isn't whitespace as far as trimming is concerned, so it's kept;
     * that's the point of a non-breaking space.
     */
    public static String indent(String s, String prefix, boolean rightTrim) {
        if (s == null || s.isEmpty() || (prefix.isEmpty() && !rightTrim)) {
            return s;
        }

        int len = s.length();
        StringBuilder sb = new StringBuilder(len + prefix.length() * 8);
        int i = 0;
        while (i < len) {
            int lineEnd = findLineEnd(s, i);

            int lineStartInSb = sb.length();
            sb.append(prefix);
            sb.append(s, i, lineEnd);
            if (rightTrim) {
                int end = sb.length();
                while (end > lineStartInSb && isTrimmableSpace(sb.charAt(end - 1))) {
                    end--;
                }
                sb.setLength(end);
            }

            i = appendEol(s, lineEnd, sb);
        }
        return sb.toString();
    }

    /**
     * Removes from each line the longest prefix of {@code prefix} that the line starts with. Lines that carry the
     * whole prefix lose all of it; lines that only carry part of it lose that part; lines that share nothing with it
     * are left alone.
     *
     * <p>This deliberately doesn't require an exact match. Since {@link #indent(String, String, boolean)} adds the
     * prefix unconditionally, an all-or-nothing dedent could leave a line that was originally the least indented as
     * the most indented one — so partial matches are shortened rather than ignored. Whitespace-only lines lose their
     * whitespace up to the length of the prefix, which is what makes them behave like empty lines here.
     */
    public static String dedent(String s, String prefix) {
        if (s == null || s.isEmpty() || prefix.isEmpty()) {
            return s;
        }

        int prefixLen = prefix.length();
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            int lineEnd = findLineEnd(s, i);

            int matched = 0;
            while (matched < prefixLen && i + matched < lineEnd
                    && s.charAt(i + matched) == prefix.charAt(matched)) {
                matched++;
            }
            sb.append(s, i + matched, lineEnd);

            i = appendEol(s, lineEnd, sb);
        }
        return sb.toString();
    }

    /**
     * Returns the index of the first line-terminator character at or after {@code from}, or the length of {@code s}
     * if there's none.
     */
    private static int findLineEnd(String s, int from) {
        int len = s.length();
        int i = from;
        while (i < len && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
            i++;
        }
        return i;
    }

    /**
     * Appends the line terminator found at {@code lineEnd} (if any, treating {@code "\r\n"} as one) to {@code sb},
     * and returns the index at which the next line starts.
     */
    private static int appendEol(String s, int lineEnd, StringBuilder sb) {
        int len = s.length();
        if (lineEnd >= len) {
            return len;
        }
        char c = s.charAt(lineEnd);
        sb.append(c);
        if (c == '\r' && lineEnd + 1 < len && s.charAt(lineEnd + 1) == '\n') {
            sb.append('\n');
            return lineEnd + 2;
        }
        return lineEnd + 1;
    }

    /**
     * Whether the character counts as trailing whitespace for right-trimming purposes. Line terminators are
     * excluded, as they're handled separately, and so is anything that {@link Character#isWhitespace(char)} rejects
     * (notably the non-breaking space).
     */
    private static boolean isTrimmableSpace(char c) {
        return c != '\n' && c != '\r' && Character.isWhitespace(c);
    }

    /**
     * Strip the longest leading-whitespace string (spaces and tabs only) that
     * is a common prefix of every non-empty line. Lines that are empty or
     * contain whitespace only are ignored when computing the prefix, and are
     * empty in the output. Mirrors Python's textwrap.dedent semantics. Note: a
     * leading tab and a leading space do not collapse — they're distinct
     * characters with no common prefix.
     */
    public static String dedent(String s) {
        if (s.isEmpty()) {
            return s;
        }
        int len = s.length();

        // First pass: walk lines, find the leading-whitespace run of each,
        // and compute the common prefix among non-empty lines.
        String commonPrefix = null;
        int lineStart = 0;
        for (int i = 0; i <= len; i++) {
            boolean atEnd = (i == len);
            char c = atEnd ? '\n' : s.charAt(i);
            if (atEnd || c == '\n' || c == '\r') {
                int contentStart = lineStart;
                while (contentStart < i) {
                    char cc = s.charAt(contentStart);
                    if (cc != ' ' && cc != '\t') break;
                    contentStart++;
                }
                boolean nonEmpty = contentStart < i;
                if (nonEmpty) {
                    if (commonPrefix == null) {
                        commonPrefix = s.substring(lineStart, contentStart);
                    } else {
                        int maxLen = Math.min(commonPrefix.length(), contentStart - lineStart);
                        int matched = 0;
                        while (matched < maxLen
                                && commonPrefix.charAt(matched) == s.charAt(lineStart + matched)) {
                            matched++;
                        }
                        if (matched < commonPrefix.length()) {
                            commonPrefix = commonPrefix.substring(0, matched);
                        }
                        if (commonPrefix.isEmpty()) break; // can't shrink further; finish quickly
                    }
                }
                if (!atEnd) {
                    // Step past \r\n if applicable
                    if (c == '\r' && i + 1 < len && s.charAt(i + 1) == '\n') i++;
                    lineStart = i + 1;
                }
            }
        }

        if (commonPrefix == null || commonPrefix.isEmpty()) {
            return s;
        }

        // Second pass: emit each line with the common prefix stripped (from
        // non-empty lines only).
        int prefixLen = commonPrefix.length();
        StringBuilder sb = new StringBuilder(len);
        lineStart = 0;
        for (int i = 0; i <= len; i++) {
            boolean atEnd = (i == len);
            if (atEnd || s.charAt(i) == '\n' || s.charAt(i) == '\r') {
                int contentStart = lineStart;
                while (contentStart < i) {
                    char cc = s.charAt(contentStart);
                    if (cc != ' ' && cc != '\t') break;
                    contentStart++;
                }
                boolean nonEmpty = contentStart < i;
                if (nonEmpty) {
                    // Non-empty line: by construction it has the common prefix.
                    sb.append(s, lineStart + prefixLen, i);
                }
                // Else: a whitespace-only line, which is normalized to empty rather than kept as is. Its
                // whitespace is accidental (whatever the emitting loop happened to produce), and keeping it would
                // mean leaving trailing whitespace behind. This also matches textwrap.dedent.
                if (!atEnd) {
                    sb.append(s.charAt(i));
                    if (s.charAt(i) == '\r' && i + 1 < len && s.charAt(i + 1) == '\n') {
                        i++;
                        sb.append('\n');
                    }
                    lineStart = i + 1;
                }
            }
        }
        return sb.toString();
    }

    public static String wrap(String s, int width) {
        return wrap(s, width, "");
    }

    public static String wrap(String s, int width, String firstPrefix) {
        return wrap(s, width, firstPrefix, firstPrefix);
    }

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

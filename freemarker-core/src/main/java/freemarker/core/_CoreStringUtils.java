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

    public static String indent(String s, String prefix) {
        if (s == null || s.isEmpty() || prefix.isEmpty()) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s.length() + prefix.length() * 10);
        int len = s.length();
        boolean atLineStart = true;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (atLineStart && c != '\n' && c != '\r') {
                sb.append(prefix);
            }
            sb.append(c);
            atLineStart = (c == '\n' || (c == '\r' && (i + 1 >= len || s.charAt(i + 1) != '\n')));
        }
        return sb.toString();
    }

    /**
     * Remove the given prefix from each line that starts with it; leave other lines unchanged.
     */
    public static String dedent(String s, String prefix) {
        if (s == null || s.isEmpty() || prefix.isEmpty()) {
            return s;
        }

        int prefixLen = prefix.length();
        StringBuilder sb = new StringBuilder(s.length());
        int len = s.length();
        boolean atLineStart = true;
        int matchPos = 0;
        boolean stripping = true;

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (atLineStart && stripping) {
                if (matchPos < prefixLen && c == prefix.charAt(matchPos)) {
                    matchPos++;
                    if (matchPos == prefixLen) {
                        stripping = false;
                    }
                    continue; // consume prefix char
                } else {
                    // Prefix didn't match — emit what we skipped
                    sb.append(prefix, 0, matchPos);
                    stripping = false;
                }
            }
            sb.append(c);
            if (c == '\n') {
                atLineStart = true;
                matchPos = 0;
                stripping = true;
            } else if (c == '\r') {
                atLineStart = true;
                matchPos = 0;
                stripping = true;
            } else {
                atLineStart = false;
            }
        }
        // Handle trailing partial match (line without newline)
        if (stripping && matchPos > 0 && matchPos < prefixLen) {
            sb.append(prefix, 0, matchPos);
        }
        return sb.toString();
    }

    /**
     * Strip the longest leading-whitespace string (spaces and tabs only) that
     * is a common prefix of every non-empty line. Empty lines are ignored when
     * computing the prefix but remain empty in the output. Mirrors Python's
     * textwrap.dedent semantics. Note: a leading tab and a leading space do
     * not collapse — they're distinct characters with no common prefix.
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
                } else {
                    // Whitespace-only or empty line — keep as is.
                    sb.append(s, lineStart, i);
                }
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

    public static String rightPadLines(String s, int width) {
        return rightPadLines(s, width, ' ');
    }

    public static String rightPadLines(String s, int width, char fillChar) {
        if (s.isEmpty()) {
            return s;
        }

        if (width < 0) {
            throw new IllegalArgumentException("width must be non-negative");
        }

        StringBuilder sb = new StringBuilder(s.length() + width);
        int lineStart = 0;
        int len = s.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || s.charAt(i) == '\n' || s.charAt(i) == '\r') {
                int lineLen = i - lineStart;
                sb.append(s, lineStart, i);
                // Pad to column (skip empty lines)
                if (lineLen > 0) {
                    for (int p = lineLen; p < width; p++) {
                        sb.append(fillChar);
                    }
                }
                // Append the line ending
                if (i < len) {
                    sb.append(s.charAt(i));
                    if (s.charAt(i) == '\r' && i + 1 < len && s.charAt(i + 1) == '\n') {
                        i++;
                        sb.append('\n');
                    }
                }
                lineStart = i + 1;
            }
        }
        return sb.toString();
    }

}

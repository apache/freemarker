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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template._ObjectWrappers;
import freemarker.template.utility.Constants;
import freemarker.template.utility.NumberUtil;
import freemarker.template.utility.StringUtil;

/**
 * JSON parser that returns a {@link TameplatModel}, similar to what FTL literals product (and so, what
 * @code ?eval} would return). A notable difference compared to the result FTL literals is that this doesn't use the
 * {@link ParserConfiguration#getArithmeticEngine()} to parse numbers, as JSON has its own fixed number syntax. For
 * numbers this parser returns {@link SimpleNumberModel}-s, where the wrapped numbers will be {@link Integer}-s when
 * they fit into that, otherwise they will be {@link Long}-s if they fit into that, otherwise they will be
 * {@link BigDecimal}-s. Another difference to the result of FTL literals is that instead of
 * {@code HashLiteral.SequenceHash} it uses {@link SimpleHash} with {@link LinkedHashMap} as backing store, for
 * efficiency.
 *
 * <p>This parser allows certain things that are errors in pure JSON:
 * <ul>
 *     <li>JavaScript comments are supported</li>
 *     <li>Non-breaking space (nbsp) and BOM are treated as whitespace</li>
 * </ul>
 */
class JSONParser {

    private static final String UNCLOSED_OBJECT_MESSAGE
            = "This {...} was still unclosed when the end of the file was reached. (Look for a missing \"}\")";

    private static final String UNCLOSED_ARRAY_MESSAGE
            = "This [...] was still unclosed when the end of the file was reached. (Look for a missing \"]\")";

    private static final BigDecimal MIN_INT_AS_BIGDECIMAL = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal MAX_INT_AS_BIGDECIMAL = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal MIN_LONG_AS_BIGDECIMAL = BigDecimal.valueOf(Long.MIN_VALUE);
    private static final BigDecimal MAX_LONG_AS_BIGDECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);

    private final String src;
    private final int ln;

    private int p;

    public static TemplateModel parse(String src) throws JSONParseException {
        return new JSONParser(src).parse();
    }

    /**
     * @param sourceLocation Only used in error messages, maybe {@code null}.
     */
    private JSONParser(String src) {
        this.src = src;
        this.ln = src.length();
    }

    private TemplateModel parse() throws JSONParseException {
        skipWS();
        TemplateModel result = consumeValue("Empty JSON (contains no value)", p);

        skipWS();
        if (p != ln) {
            throw newParseException("End-of-file was expected but found further non-whitespace characters.");
        }

        return result;
    }

    private TemplateModel consumeValue(String eofErrorMessage, int eofBlamePosition) throws JSONParseException {
        if (p == ln) {
            throw newParseException(
                    eofErrorMessage == null
                            ? "A value was expected here, but end-of-file was reached." : eofErrorMessage,
                    eofBlamePosition == -1 ? p : eofBlamePosition);
        }

        TemplateModel result;

        result = tryConsumeString();
        if (result != null) return result;

        result = tryConsumeNumber();
        if (result != null) return result;

        result = tryConsumeObject();
        if (result != null) return result;

        result = tryConsumeArray();
        if (result != null) return result;

        result = tryConsumeTrueFalseNull();
        if (result != null) return result != TemplateNullModel.INSTANCE ? result : null;

        // Better error message for a frequent mistake:
        if (p < ln && src.charAt(p) == '\'') {
            throw newParseException("Unexpected apostrophe-quote character. "
                    + "JSON strings must be quoted with quotation mark.");
        }

        throw newParseException(
                "Expected either the beginning of a (negative) number or the beginning of one of these: "
                        + "{...}, [...], \"...\", true, false, null. Found character " + StringUtil.jQuote(src.charAt(p))
                        + " instead.");
    }

    private TemplateModel tryConsumeTrueFalseNull() throws JSONParseException {
        int startP = p;
        if (p < ln && isIdentifierStart(src.charAt(p))) {
            p++;
            while (p < ln && isIdentifierPart(src.charAt(p))) {
                p++;
            }
        }

        if (startP == p) return null;

        String keyword = src.substring(startP, p);
        if (keyword.equals("true")) {
            return TemplateBooleanModel.TRUE;
        } else if (keyword.equals("false")) {
            return TemplateBooleanModel.FALSE;
        } else if (keyword.equals("null")) {
            return TemplateNullModel.INSTANCE;
        }

        throw newParseException(
                "Invalid JSON keyword: " + StringUtil.jQuote(keyword)
                        + ". Should be one of: true, false, null. "
                        + "If it meant to be a string then it must be quoted.", startP);
    }

    private TemplateNumberModel tryConsumeNumber() throws JSONParseException {
        if (p >= ln) {
            return null;
        }
        char c = src.charAt(p);
        boolean negative = c == '-';
        if (!(negative || isDigit(c) || c == '.')) {
            return null;
        }

        int startP = p;

        if (negative) {
            if (p + 1 >= ln) {
                throw newParseException("Expected a digit after \"-\", but reached end-of-file.");
            }
            char lookAheadC = src.charAt(p + 1);
            if (!(isDigit(lookAheadC) || lookAheadC == '.')) {
                return null;
            }
            p++; // Consume "-" only, not the digit
        }

        long longSum = 0;
        boolean firstDigit = true;
        consumeLongFittingHead: do {
            c = src.charAt(p);

            if (!isDigit(c)) {
                if (c == '.' && firstDigit) {
                    throw newParseException("JSON doesn't allow numbers starting with \".\".");
                }
                break consumeLongFittingHead;
            }

            int digit = c - '0';
            if (longSum == 0) {
                if (!firstDigit) {
                    throw newParseException("JSON doesn't allow superfluous leading 0-s.", p - 1);
                }

                longSum = !negative ? digit : -digit;
                p++;
            } else {
                long prevLongSum = longSum;
                longSum = longSum * 10 + (!negative ? digit : -digit);
                if (!negative && prevLongSum > longSum || negative && prevLongSum < longSum) {
                    // We had an overflow => Can't consume this digit as long-fitting
                    break consumeLongFittingHead;
                }
                p++;
            }
            firstDigit = false;
        } while (p < ln);

        if (p < ln && isBigDecimalFittingTailCharacter(c)) {
            char lastC = c;
            p++;

            consumeBigDecimalFittingTail: while (p < ln) {
                c = src.charAt(p);
                if (isBigDecimalFittingTailCharacter(c)) {
                    p++;
                } else if ((c == '+' || c == '-') && isE(lastC)) {
                    p++;
                } else {
                    break consumeBigDecimalFittingTail;
                }
                lastC = c;
            }

            String numStr = src.substring(startP, p);
            BigDecimal bd;
            try {
                bd = new BigDecimal(numStr);
            } catch (NumberFormatException e) {
                throw new JSONParseException("Malformed number: " + numStr, src, startP, e);
            }

            if (bd.compareTo(MIN_INT_AS_BIGDECIMAL) >= 0 && bd.compareTo(MAX_INT_AS_BIGDECIMAL) <= 0) {
                if (NumberUtil.isIntegerBigDecimal(bd)) {
                    return new SimpleNumber(bd.intValue());
                }
            } else if (bd.compareTo(MIN_LONG_AS_BIGDECIMAL) >= 0 && bd.compareTo(MAX_LONG_AS_BIGDECIMAL) <= 0) {
                if (NumberUtil.isIntegerBigDecimal(bd)) {
                    return new SimpleNumber(bd.longValue());
                }
            }
            return new SimpleNumber(bd);
        } else {
            return new SimpleNumber(
                    longSum <= Integer.MAX_VALUE && longSum >= Integer.MIN_VALUE
                            ? (Number) (int) longSum
                            : longSum);
        }
    }

    private TemplateScalarModel tryConsumeString() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('"')) return null;

        StringBuilder sb = new StringBuilder();
        char c = 0;
        while (p < ln) {
            c = src.charAt(p);

            if (c == '"') {
                p++;
                return new SimpleScalar(sb.toString());  // Call normally returns here!
            } else if (c == '\\') {
                p++;
                sb.append(consumeAfterBackslash());
            } else if (c <= 0x1F) {
                throw newParseException("JSON doesn't allow unescaped control characters in string literals, "
                        + "but found character with code (decimal): " + (int) c);
            } else {
                p++;
                sb.append(c);
            }
        }

        throw newParseException("String literal was still unclosed when the end of the file was reached. "
                + "(Look for missing or accidentally escaped closing quotation mark.)", startP);
    }

    private TemplateSequenceModel tryConsumeArray() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('[')) return null;

        skipWS();
        if (tryConsumeChar(']')) return Constants.EMPTY_SEQUENCE;

        boolean afterComma = false;
        SimpleSequence elements = new SimpleSequence(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
        do {
            skipWS();
            elements.add(consumeValue(afterComma ? null : UNCLOSED_ARRAY_MESSAGE, afterComma ? -1 : startP));

            skipWS();
            afterComma = true;
        } while (consumeChar(',', ']', UNCLOSED_ARRAY_MESSAGE, startP) == ',');
        return elements;
    }

    private TemplateHashModelEx2 tryConsumeObject() throws JSONParseException {
        int startP = p;
        if (!tryConsumeChar('{')) return null;

        skipWS();
        if (tryConsumeChar('}')) return Constants.EMPTY_HASH_EX2;

        boolean afterComma = false;
        Map<String, Object> map = new LinkedHashMap<>();  // Must keeps original order!
        do {
            skipWS();
            int keyStartP = p;
            Object key = consumeValue(afterComma ? null : UNCLOSED_OBJECT_MESSAGE, afterComma ? -1 : startP);
            if (!(key instanceof TemplateScalarModel)) {
                throw newParseException("Wrong key type. JSON only allows string keys inside {...}.", keyStartP);
            }
            String strKey = null;
            try {
                strKey = ((TemplateScalarModel) key).getAsString();
            } catch (TemplateModelException e) {
                throw new BugException(e);
            }

            skipWS();
            consumeChar(':');

            skipWS();
            map.put(strKey, consumeValue(null, -1));

            skipWS();
            afterComma = true;
        } while (consumeChar(',', '}', UNCLOSED_OBJECT_MESSAGE, startP) == ',');
        return new SimpleHash(map, _ObjectWrappers.SAFE_OBJECT_WRAPPER, 0);
    }

    private boolean isE(char c) {
        return c == 'e' || c == 'E';
    }

    private boolean isBigDecimalFittingTailCharacter(char c) {
        return c == '.' || isE(c) || isDigit(c);
    }

    private char consumeAfterBackslash() throws JSONParseException {
        if (p == ln) {
            throw newParseException("Reached the end of the file, but the escape is unclosed.");
        }

        final char c = src.charAt(p);
        switch (c) {
            case '"':
            case '\\':
            case '/':
                p++;
                return c;
            case 'b':
                p++;
                return '\b';
            case 'f':
                p++;
                return '\f';
            case 'n':
                p++;
                return '\n';
            case 'r':
                p++;
                return '\r';
            case 't':
                p++;
                return '\t';
            case 'u':
                p++;
                return consumeAfterBackslashU();
        }
        throw newParseException("Unsupported escape: \\" + c);
    }

    private char consumeAfterBackslashU() throws JSONParseException {
        if (p + 3 >= ln) {
            throw newParseException("\\u must be followed by exactly 4 hexadecimal digits");
        }
        final String hex = src.substring(p, p + 4);
        try {
            char r = (char) Integer.parseInt(hex, 16);
            p += 4;
            return r;
        } catch (NumberFormatException e) {
            throw newParseException("\\u must be followed by exactly 4 hexadecimal digits, but was followed by "
                    + StringUtil.jQuote(hex) + ".");
        }
    }

    private boolean tryConsumeChar(char c) {
        if (p < ln && src.charAt(p) == c) {
            p++;
            return true;
        } else {
            return false;
        }
    }

    private void consumeChar(char expected) throws JSONParseException {
        consumeChar(expected, (char) 0, null, -1);
    }

    private char consumeChar(char expected1, char expected2, String eofErrorHint, int eofErrorP) throws JSONParseException {
        if (p >= ln) {
            throw newParseException(eofErrorHint == null
                            ? "Expected " + StringUtil.jQuote(expected1)
                            + ( expected2 != 0 ? " or " + StringUtil.jQuote(expected2) : "")
                            + " character, but reached end-of-file. "
                            : eofErrorHint,
                    eofErrorP == -1 ? p : eofErrorP);
        }
        char c = src.charAt(p);
        if (c == expected1 || (expected2 != 0 && c == expected2)) {
            p++;
            return c;
        }
        throw newParseException("Expected " + StringUtil.jQuote(expected1)
                + ( expected2 != 0 ? " or " + StringUtil.jQuote(expected2) : "")
                + " character, but found " + StringUtil.jQuote(c) + " instead.");
    }

    private void skipWS() throws JSONParseException {
        do {
            while (p < ln && isWS(src.charAt(p))) {
                p++;
            }
        } while (skipComment());
    }

    private boolean skipComment() throws JSONParseException {
        if (p + 1 < ln) {
            if (src.charAt(p) == '/') {
                char c2 = src.charAt(p + 1);
                if (c2 == '/') {
                    int eolP = p + 2;
                    while (eolP < ln && !isLineBreak(src.charAt(eolP))) {
                        eolP++;
                    }
                    p = eolP;
                    return true;
                } else if (c2 == '*') {
                    int closerP = p + 3;
                    while (closerP < ln && !(src.charAt(closerP - 1) == '*' && src.charAt(closerP) == '/')) {
                        closerP++;
                    }
                    if (closerP >= ln) {
                        throw newParseException("Unclosed comment");
                    }
                    p = closerP + 1;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whitespace as specified by JSON, plus non-breaking space (nbsp), and BOM.
     */
    private static boolean isWS(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == 0xA0 || c == '\uFEFF';
    }

    private static boolean isLineBreak(char c) {
        return c == '\r' || c == '\n';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    private JSONParseException newParseException(String message) {
        return newParseException(message, p);
    }

    private JSONParseException newParseException(String message, int p) {
        return new JSONParseException(message, src, p);
    }

    static class JSONParseException extends Exception {
        public JSONParseException(String message, String src, int position) {
            super(createSourceCodeErrorMessage(message, src, position));
        }

        public JSONParseException(String message, String src, int position,
                Throwable cause) {
            super(createSourceCodeErrorMessage(message, src, position), cause);
        }

    }

    private static int MAX_QUOTATION_LENGTH = 50;

    private static String createSourceCodeErrorMessage(String message, String srcCode, int position) {
        int ln = srcCode.length();
        if (position < 0) {
            position = 0;
        }
        if (position >= ln) {
            return message + "\n"
                    + "Error location: At the end of text.";
        }

        int i;
        char c;
        int rowBegin = 0;
        int rowEnd;
        int row = 1;
        char lastChar = 0;
        for (i = 0; i <= position; i++) {
            c = srcCode.charAt(i);
            if (lastChar == 0xA) {
                rowBegin = i;
                row++;
            } else if (lastChar == 0xD && c != 0xA) {
                rowBegin = i;
                row++;
            }
            lastChar = c;
        }
        for (i = position; i < ln; i++) {
            c = srcCode.charAt(i);
            if (c == 0xA || c == 0xD) {
                if (c == 0xA && i > 0 && srcCode.charAt(i - 1) == 0xD) {
                    i--;
                }
                break;
            }
        }
        rowEnd = i - 1;
        if (position > rowEnd + 1) {
            position = rowEnd + 1;
        }
        int col = position - rowBegin + 1;
        if (rowBegin > rowEnd) {
            return message + "\n"
                    + "Error location: line "
                    + row + ", column " + col + ":\n"
                    + "(Can't show the line because it is empty.)";
        }
        String s1 = srcCode.substring(rowBegin, position);
        String s2 = srcCode.substring(position, rowEnd + 1);
        s1 = expandTabs(s1, 8);
        int ln1 = s1.length();
        s2 = expandTabs(s2, 8, ln1);
        int ln2 = s2.length();
        if (ln1 + ln2 > MAX_QUOTATION_LENGTH) {
            int newLn2 = ln2 - ((ln1 + ln2) - MAX_QUOTATION_LENGTH);
            if (newLn2 < 6) {
                newLn2 = 6;
            }
            if (newLn2 < ln2) {
                s2 = s2.substring(0, newLn2 - 3) + "...";
                ln2 = newLn2;
            }
            if (ln1 + ln2 > MAX_QUOTATION_LENGTH) {
                s1 = "..." + s1.substring((ln1 + ln2) - MAX_QUOTATION_LENGTH + 3);
            }
        }
        StringBuilder res = new StringBuilder(message.length() + 80);
        res.append(message);
        res.append("\nError location: line ").append(row).append(", column ").append(col).append(":\n");
        res.append(s1).append(s2).append("\n");
        int x = s1.length();
        while (x != 0) {
            res.append(' ');
            x--;
        }
        res.append('^');

        return res.toString();
    }

    private static String expandTabs(String s, int tabWidth) {
        return expandTabs(s, tabWidth, 0);
    }

    /**
     * Replaces all tab-s with spaces in a single line.
     */
    private static String expandTabs(String s, int tabWidth, int startCol) {
        int e = s.indexOf('\t');
        if (e == -1) {
            return s;
        }
        int b = 0;
        StringBuilder buf = new StringBuilder(s.length() + Math.max(16, tabWidth * 2));
        do {
            buf.append(s, b, e);
            int col = buf.length() + startCol;
            for (int i = tabWidth * (1 + col / tabWidth) - col; i > 0; i--) {
                buf.append(' ');
            }
            b = e + 1;
            e = s.indexOf('\t', b);
        } while (e != -1);
        buf.append(s, b, s.length());
        return buf.toString();
    }

}

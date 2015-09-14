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

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import freemarker.template.utility.StringUtil;

class ExtendedDecimalFormatParser {

    private static final HashMap<String, ? extends ParameterHandler> PARAM_HANDLERS;

    static {
        HashMap<String, ParameterHandler> m = new HashMap<String, ParameterHandler>();
        m.put("ro", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                RoundingMode parsedValue;
                if (value.equals("u")) {
                    parsedValue = RoundingMode.UP;
                } else if (value.equals("d")) {
                    parsedValue = RoundingMode.DOWN;
                } else if (value.equals("c")) {
                    parsedValue = RoundingMode.CEILING;
                } else if (value.equals("f")) {
                    parsedValue = RoundingMode.FLOOR;
                } else if (value.equals("hd")) {
                    parsedValue = RoundingMode.HALF_DOWN;
                } else if (value.equals("he")) {
                    parsedValue = RoundingMode.HALF_EVEN;
                } else if (value.equals("hu")) {
                    parsedValue = RoundingMode.HALF_UP;
                } else if (value.equals("un")) {
                    parsedValue = RoundingMode.UNNECESSARY;
                } else {
                    throw new InvalidParameterValueException("Should be one of: u, d, c, f, hd, he, hu, un");
                }

                if (_JavaVersions.JAVA_6 == null) {
                    throw new InvalidParameterValueException("For setting the rounding mode you need Java 6 or later.");
                }

                parser.roundingMode = parsedValue;
            }
        });
        m.put("mul", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                try {
                    parser.multipier = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new InvalidParameterValueException("Malformed integer.");
                }
            }
        });
        m.put("dec", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setDecimalSeparator(value.charAt(0));
            }
        });
        m.put("grp", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setGroupingSeparator(value.charAt(0));
            }
        });
        m.put("exp", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (_JavaVersions.JAVA_6 == null) {
                    throw new InvalidParameterValueException(
                            "For setting the exponent separator you need Java 6 or later.");
                }
                _JavaVersions.JAVA_6.setExponentSeparator(parser.symbols, value);
            }
        });
        m.put("min", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setMinusSign(value.charAt(0));
            }
        });
        m.put("inf", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                parser.symbols.setInfinity(value);
            }
        });
        m.put("nan", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                parser.symbols.setNaN(value);
            }
        });
        m.put("prc", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setPercent(value.charAt(0));
            }
        });
        m.put("prm", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setPerMill(value.charAt(0));
            }
        });
        m.put("zero", new ParameterHandler() {

            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setZeroDigit(value.charAt(0));
            }
        });
        PARAM_HANDLERS = m;
    }

    private static final String SNIP_MARK = "[...]";
    private static final int MAX_QUOTATION_LENGTH = 10; // Must be more than SNIP_MARK.length!

    private final String src;
    private int pos = 0;

    private final DecimalFormatSymbols symbols;
    private RoundingMode roundingMode;
    private Integer multipier;

    static DecimalFormat parse(String formatString, Locale locale) throws ParseException {
        return new ExtendedDecimalFormatParser(formatString, locale).parse();
    }

    private DecimalFormat parse() throws ParseException {
        String stdPattern = fetchStandardPattern();
        skipWS();
        parseFormatStringExtension();

        DecimalFormat decimalFormat = new DecimalFormat(stdPattern, symbols);

        if (roundingMode != null) {
            if (_JavaVersions.JAVA_6 == null) {
                throw new ParseException("Setting rounding mode needs Java 6 or later", 0);
            }
            _JavaVersions.JAVA_6.setRoundingMode(decimalFormat, roundingMode);
        }

        if (multipier != null) {
            decimalFormat.setMultiplier(multipier.intValue());
        }

        return decimalFormat;
    }

    private void parseFormatStringExtension() throws ParseException {
        int ln = src.length();

        if (pos == ln) {
            return;
        }

        do {
            int namePos = pos;
            String name = fetchName();
            if (name == null) {
                throw newExpectedSgParseException("name");
            }

            skipWS();

            if (!fetchChar('=')) {
                throw newExpectedSgParseException("\"=\"");
            }

            skipWS();

            int valuePos = pos;
            String value = fetchValue();
            if (value == null) {
                throw newExpectedSgParseException("value");
            }
            int paramEndPos = pos;

            applyFormatStringExtensionParameter(name, namePos, value, valuePos);

            skipWS();

            // Optional comma
            if (fetchChar(',')) {
                skipWS();
            } else {
                if (pos == ln) {
                    return;
                }
                if (pos == paramEndPos) {
                    throw newExpectedSgParseException("parameter separator whitespace or comma");
                }
            }
        } while (true);
    }

    private void applyFormatStringExtensionParameter(
            String name, int namePos, String value, int valuePos) throws ParseException {
        ParameterHandler handler = PARAM_HANDLERS.get(name);
        if (handler == null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Unsupported parameter name, ").append(StringUtil.jQuote(name));
            sb.append(". The supported names are: ");
            Set<String> legalNames = PARAM_HANDLERS.keySet();
            String[] legalNameArr = legalNames.toArray(new String[legalNames.size()]);
            Arrays.sort(legalNameArr);
            for (int i = 0; i < legalNameArr.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(legalNameArr[i]);
            }
            throw new java.text.ParseException(sb.toString(), namePos);
        }

        try {
            handler.handle(this, value);
        } catch (InvalidParameterValueException e) {
            throw new java.text.ParseException(
                    StringUtil.jQuote(value) + " is an invalid value for the \"" + name + "\" parameter: " + e.message,
                    valuePos);
        }
    }

    private void skipWS() {
        int ln = src.length();
        while (pos < ln && isWS(src.charAt(pos))) {
            pos++;
        }
    }

    private boolean fetchChar(char fetchedChar) {
        if (pos < src.length() && src.charAt(pos) == fetchedChar) {
            pos++;
            return true;
        } else {
            return false;
        }
    }

    private boolean isWS(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u00A0';
    }

    private String fetchName() throws ParseException {
        int ln = src.length();
        int startPos = pos;
        boolean firstChar = true;
        scanUntilEnd: while (pos < ln) {
            char c = src.charAt(pos);
            if (firstChar) {
                if (!Character.isJavaIdentifierStart(c)) {
                    break scanUntilEnd;
                }
                firstChar = false;
            } else if (!Character.isJavaIdentifierPart(c)) {
                break scanUntilEnd;
            }
            pos++;
        }
        return !firstChar ? src.substring(startPos, pos) : null;
    }

    private String fetchValue() throws ParseException {
        int ln = src.length();
        int startPos = pos;
        boolean quotedMode = false;
        boolean needsUnescaping = false;
        scanUntilEnd: while (pos < ln) {
            char c = src.charAt(pos);
            if (c == '\'') {
                if (!quotedMode) {
                    if (startPos != pos) {
                        throw new java.text.ParseException(
                                "The \"'\" character can only be used for quoting values, "
                                        + "but it was in the middle of an non-quoted value.",
                                pos);
                    }
                    quotedMode = true;
                } else {
                    if (pos + 1 < ln && src.charAt(pos + 1) == '\'') {
                        pos++; // skip "''" (escaped "'")
                        needsUnescaping = true;
                    } else {
                        String str = src.substring(startPos + 1, pos);
                        pos++;
                        return needsUnescaping ? unescape(str) : str;
                    }
                }
            } else {
                if (!quotedMode && !Character.isJavaIdentifierPart(c)) {
                    break scanUntilEnd;
                }
            }
            pos++;
        } // while
        if (quotedMode) {
            throw new java.text.ParseException(
                    "The \"'\" quotation wasn't closed when the end of the source was reached.",
                    pos);
        }
        return startPos == pos ? null : src.substring(startPos, pos);
    }

    private String unescape(String s) {
        return StringUtil.replace(s, "\'\'", "\'");
    }

    private String fetchStandardPattern() {
        int pos = this.pos;
        int ln = src.length();
        int semicolonCnt = 0;
        boolean quotedMode = false;
        findStdPartEnd: while (pos < ln) {
            char c = src.charAt(pos);
            if (c == ';' && !quotedMode) {
                semicolonCnt++;
                if (semicolonCnt == 2) {
                    break findStdPartEnd;
                }
            } else if (c == '\'') {
                if (quotedMode) {
                    if (pos + 1 < ln && src.charAt(pos + 1) == '\'') {
                        // Skips "''" used for escaping "'"
                        pos++;
                    } else {
                        quotedMode = false;
                    }
                } else {
                    quotedMode = true;
                }
            }
            pos++;
        }

        String stdFormatStr;
        if (semicolonCnt < 2) { // We have a standard DecimalFormat string
            // Note that "0.0;" and "0.0" gives the same result with DecimalFormat, so we leave a ';' there
            stdFormatStr = src;
        } else { // `pos` points to the 2nd ';'
            int stdEndPos = pos;
            if (src.charAt(pos - 1) == ';') { // we have a ";;"
                // Note that ";;" is illegal in DecimalFormat, so this is backward compatible.
                stdEndPos--;
            }
            stdFormatStr = src.substring(0, stdEndPos);
        }

        if (pos < ln) {
            pos++; // Skips closing ';'
        }
        this.pos = pos;

        return stdFormatStr;
    }

    private ExtendedDecimalFormatParser(String formatString, Locale locale) {
        src = formatString;
        this.symbols = new DecimalFormatSymbols(locale);
    }

    private ParseException newExpectedSgParseException(String expectedThing) {
        String quotation;

        // Ignore trailing WS when calculating the length:
        int i = src.length() - 1;
        while (i >= 0 && Character.isWhitespace(src.charAt(i))) {
            i--;
        }
        int ln = i + 1;

        if (pos < ln) {
            int qEndPos = pos + MAX_QUOTATION_LENGTH;
            if (qEndPos >= ln) {
                quotation = src.substring(pos, ln);
            } else {
                quotation = src.substring(pos, qEndPos - SNIP_MARK.length()) + SNIP_MARK;
            }
        } else {
            quotation = null;
        }

        return new ParseException(
                "Expected a(n) " + expectedThing + " at position " + pos + " (0-based), but "
                        + (quotation == null ? "reached the end of the input." : "found: " + quotation),
                pos);
    }

    private interface ParameterHandler {

        void handle(ExtendedDecimalFormatParser parser, String value)
                throws InvalidParameterValueException;

    }

    private static class InvalidParameterValueException extends Exception {

        private final String message;

        public InvalidParameterValueException(String message) {
            this.message = message;
        }

    }

}

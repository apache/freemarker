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
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import freemarker.template.utility.StringUtil;

class ExtendedDecimalFormatParser {
    
    private static final String PARAM_ROUNDING_MODE = "roundingMode";
    private static final String PARAM_MULTIPIER = "multipier";
    private static final String PARAM_MULTIPLIER = "multiplier";
    private static final String PARAM_DECIMAL_SEPARATOR = "decimalSeparator";
    private static final String PARAM_MONETARY_DECIMAL_SEPARATOR = "monetaryDecimalSeparator";
    private static final String PARAM_GROUP_SEPARATOR = "groupingSeparator";
    private static final String PARAM_EXPONENT_SEPARATOR = "exponentSeparator";
    private static final String PARAM_MINUS_SIGN = "minusSign";
    private static final String PARAM_INFINITY = "infinity";
    private static final String PARAM_NAN = "nan";
    private static final String PARAM_PERCENT = "percent";
    private static final String PARAM_PER_MILL = "perMill";
    private static final String PARAM_ZERO_DIGIT = "zeroDigit";
    private static final String PARAM_CURRENCY_CODE = "currencyCode";
    private static final String PARAM_CURRENCY_SYMBOL = "currencySymbol";

    private static final String PARAM_VALUE_RND_UP = "up";
    private static final String PARAM_VALUE_RND_DOWN = "down";
    private static final String PARAM_VALUE_RND_CEILING = "ceiling";
    private static final String PARAM_VALUE_RND_FLOOR = "floor";
    private static final String PARAM_VALUE_RND_HALF_DOWN = "halfDown";
    private static final String PARAM_VALUE_RND_HALF_EVEN = "halfEven";
    private static final String PARAM_VALUE_RND_HALF_UP = "halfUp";
    private static final String PARAM_VALUE_RND_UNNECESSARY = "unnecessary";
    
    private static final HashMap<String, ? extends ParameterHandler> PARAM_HANDLERS;
    static {
        HashMap<String, ParameterHandler> m = new HashMap<>();
        m.put(PARAM_ROUNDING_MODE, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                RoundingMode parsedValue;
                if (value.equals(PARAM_VALUE_RND_UP)) {
                    parsedValue = RoundingMode.UP;
                } else if (value.equals(PARAM_VALUE_RND_DOWN)) {
                    parsedValue = RoundingMode.DOWN;
                } else if (value.equals(PARAM_VALUE_RND_CEILING)) {
                    parsedValue = RoundingMode.CEILING;
                } else if (value.equals(PARAM_VALUE_RND_FLOOR)) {
                    parsedValue = RoundingMode.FLOOR;
                } else if (value.equals(PARAM_VALUE_RND_HALF_DOWN)) {
                    parsedValue = RoundingMode.HALF_DOWN;
                } else if (value.equals(PARAM_VALUE_RND_HALF_EVEN)) {
                    parsedValue = RoundingMode.HALF_EVEN;
                } else if (value.equals(PARAM_VALUE_RND_HALF_UP)) {
                    parsedValue = RoundingMode.HALF_UP;
                } else if (value.equals(PARAM_VALUE_RND_UNNECESSARY)) {
                    parsedValue = RoundingMode.UNNECESSARY;
                } else {
                    throw new InvalidParameterValueException("Should be one of: "
                            + PARAM_VALUE_RND_UP + ", " + PARAM_VALUE_RND_DOWN + ", " + PARAM_VALUE_RND_CEILING + ", "
                            + PARAM_VALUE_RND_FLOOR + ", " + PARAM_VALUE_RND_HALF_DOWN + ", "
                            + PARAM_VALUE_RND_HALF_EVEN + ", " + PARAM_VALUE_RND_UNNECESSARY);
                }

                parser.roundingMode = parsedValue;
            }
        });
        ParameterHandler multiplierParamHandler = new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                try {
                    parser.multiplier = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new InvalidParameterValueException("Malformed integer.");
                }
            }
        };
        m.put(PARAM_MULTIPLIER, multiplierParamHandler);
        m.put(PARAM_MULTIPIER, multiplierParamHandler);
        m.put(PARAM_DECIMAL_SEPARATOR, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setDecimalSeparator(value.charAt(0));
            }
        });
        m.put(PARAM_MONETARY_DECIMAL_SEPARATOR, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setMonetaryDecimalSeparator(value.charAt(0));
            }
        });
        m.put(PARAM_GROUP_SEPARATOR, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setGroupingSeparator(value.charAt(0));
            }
        });
        m.put(PARAM_EXPONENT_SEPARATOR, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                parser.symbols.setExponentSeparator(value);
            }
        });
        m.put(PARAM_MINUS_SIGN, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setMinusSign(value.charAt(0));
            }
        });
        m.put(PARAM_INFINITY, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                parser.symbols.setInfinity(value);
            }
        });
        m.put(PARAM_NAN, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                parser.symbols.setNaN(value);
            }
        });
        m.put(PARAM_PERCENT, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setPercent(value.charAt(0));
            }
        });
        m.put(PARAM_PER_MILL, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setPerMill(value.charAt(0));
            }
        });
        m.put(PARAM_ZERO_DIGIT, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                if (value.length() != 1) {
                    throw new InvalidParameterValueException("Must contain exactly 1 character.");
                }
                parser.symbols.setZeroDigit(value.charAt(0));
            }
        });
        m.put(PARAM_CURRENCY_CODE, new ParameterHandler() {
            @Override
            public void handle(ExtendedDecimalFormatParser parser, String value)
                    throws InvalidParameterValueException {
                Currency currency;
                try {
                    currency = Currency.getInstance(value);
                } catch (IllegalArgumentException e) {
                    throw new InvalidParameterValueException("Not a known ISO 4217 code.");
                }
                parser.symbols.setCurrency(currency);
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
    private Integer multiplier;

    static DecimalFormat parse(String formatString, Locale locale) throws ParseException {
        return new ExtendedDecimalFormatParser(formatString, locale).parse();
    }

    private DecimalFormat parse() throws ParseException {
        String stdPattern = fetchStandardPattern();
        skipWS();
        parseFormatStringExtension();

        DecimalFormat decimalFormat;
        try {
            decimalFormat = new DecimalFormat(stdPattern, symbols);
        } catch (IllegalArgumentException e) {
            ParseException pe = new ParseException(e.getMessage(), 0);
            if (e.getCause() != null) {
                try {
                    e.initCause(e.getCause());
                } catch (Exception e2) {
                    // Supress
                }
            }
            throw pe;
        }

        if (roundingMode != null) {
            decimalFormat.setRoundingMode(roundingMode);
        }

        if (multiplier != null) {
            decimalFormat.setMultiplier(multiplier.intValue());
        }

        return decimalFormat;
    }

    private void parseFormatStringExtension() throws ParseException {
        int ln = src.length();

        if (pos == ln) {
            return;
        }

        String currencySymbol = null;  // Exceptional, as must be applied after "currency code"
        fetchParameters: do {
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

            ParameterHandler handler = PARAM_HANDLERS.get(name);
            if (handler == null) {
                if (name.equals(PARAM_CURRENCY_SYMBOL)) {
                    currencySymbol = value;
                } else {
                    throw newUnknownParameterException(name, namePos);
                }
            } else {
                try {
                    handler.handle(this, value);
                } catch (InvalidParameterValueException e) {
                    throw newInvalidParameterValueException(name, value, valuePos, e);
                }
            }

            skipWS();

            // Optional comma
            if (fetchChar(',')) {
                skipWS();
            } else {
                if (pos == ln) {
                    break fetchParameters;
                }
                if (pos == paramEndPos) {
                    throw newExpectedSgParseException("parameter separator whitespace or comma");
                }
            }
        } while (true);
        
        // This is brought out to here to ensure that it's applied after "currency code":
        if (currencySymbol != null) {
            symbols.setCurrencySymbol(currencySymbol);
        }
    }

    private ParseException newInvalidParameterValueException(String name, String value, int valuePos,
            InvalidParameterValueException e) {
        return new java.text.ParseException(
                StringUtil.jQuote(value) + " is an invalid value for the \"" + name + "\" parameter: "
                + e.message,
                valuePos);
    }

    private ParseException newUnknownParameterException(String name, int namePos) throws ParseException {
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
        return new java.text.ParseException(sb.toString(), namePos);
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
        char openedQuot = 0;
        boolean needsUnescaping = false;
        scanUntilEnd: while (pos < ln) {
            char c = src.charAt(pos);
            if (c == '\'' || c == '"') {
                if (openedQuot == 0) {
                    if (startPos != pos) {
                        throw new java.text.ParseException(
                                "The " + c + " character can only be used for quoting values, "
                                        + "but it was in the middle of an non-quoted value.",
                                pos);
                    }
                    openedQuot = c;
                } else if (c == openedQuot) {
                    if (pos + 1 < ln && src.charAt(pos + 1) == openedQuot) {
                        pos++; // skip doubled quote (escaping)
                        needsUnescaping = true;
                    } else {
                        String str = src.substring(startPos + 1, pos);
                        pos++;
                        return needsUnescaping ? unescape(str, openedQuot) : str;
                    }
                }
            } else {
                if (openedQuot == 0 && !Character.isJavaIdentifierPart(c)) {
                    break scanUntilEnd;
                }
            }
            pos++;
        } // while
        if (openedQuot != 0) {
            throw new java.text.ParseException(
                    "The " + openedQuot 
                    + " quotation wasn't closed when the end of the source was reached.",
                    pos);
        }
        return startPos == pos ? null : src.substring(startPos, pos);
    }

    private String unescape(String s, char openedQuot) {
        return openedQuot == '\'' ? StringUtil.replace(s, "\'\'", "\'") : StringUtil.replace(s, "\"\"", "\"");
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
        this.symbols = DecimalFormatSymbols.getInstance(locale);
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

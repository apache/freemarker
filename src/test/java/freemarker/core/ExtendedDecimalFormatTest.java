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

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class ExtendedDecimalFormatTest extends TemplateTest {
    
    private static final Locale LOC = Locale.US;
    private static final DecimalFormatSymbols SYMS = DecimalFormatSymbols.getInstance(LOC);
    
    @Test
    public void testNonExtended() throws ParseException {
        for (String fStr : new String[] { "0.00", "0.###", "#,#0.###", "#0.####", "0.0;m", "0.0;",
                "0'x'", "0'x';'m'", "0';'", "0';';m", "0';';'#'m';'", "0';;'", "" }) {
            assertFormatsEquivalent(new DecimalFormat(fStr, SYMS), ExtendedDecimalFormatParser.parse(fStr, LOC));
        }
        
        try {
            new DecimalFormat(";");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            ExtendedDecimalFormatParser.parse(";", LOC);
        } catch (ParseException e) {
            // Expected
        }
    }

    @Test
    public void testNonExtended2() throws ParseException {
        assertFormatsEquivalent(new DecimalFormat("0.0", SYMS), ExtendedDecimalFormatParser.parse("0.0;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0.0", SYMS), ExtendedDecimalFormatParser.parse("0.0;;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0.0;m", SYMS), ExtendedDecimalFormatParser.parse("0.0;m;", LOC));
        assertFormatsEquivalent(new DecimalFormat("", SYMS), ExtendedDecimalFormatParser.parse(";;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0'x'", SYMS), ExtendedDecimalFormatParser.parse("0'x';;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0'x';'m'", SYMS),
                ExtendedDecimalFormatParser.parse("0'x';'m';", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';'", SYMS), ExtendedDecimalFormatParser.parse("0';';;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';';m", SYMS), ExtendedDecimalFormatParser.parse("0';';m;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';';'#'m';'", SYMS),
                ExtendedDecimalFormatParser.parse("0';';'#'m';';", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';;'", SYMS), ExtendedDecimalFormatParser.parse("0';;';;", LOC));
        
        try {
            new DecimalFormat(";m");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            new DecimalFormat("; ;");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            ExtendedDecimalFormatParser.parse("; ;", LOC);
            fail();
        } catch (ParseException e) {
            // Expected
        }
        try {
            ExtendedDecimalFormatParser.parse(";m", LOC);
            fail();
        } catch (ParseException e) {
            // Expected
        }
        try {
            ExtendedDecimalFormatParser.parse(";m;", LOC);
            fail();
        } catch (ParseException e) {
            // Expected
        }
    }
    
    @SuppressWarnings("boxing")
    @Test
    public void testExtendedParamsParsing() throws ParseException {
        for (String fs : new String[] {
                "00.##;; decimalSeparator='D'",
                "00.##;;decimalSeparator=D",
                "00.##;;  decimalSeparator  =  D ", "00.##;; decimalSeparator = 'D' " }) {
            assertFormatted(fs, 1.125, "01D12");
        }
        for (String fs : new String[] {
                ",#0.0;; decimalSeparator=D, groupingSeparator=_",
                ",#0.0;;decimalSeparator=D,groupingSeparator=_",
                ",#0.0;; decimalSeparator = D , groupingSeparator = _ ",
                ",#0.0;; decimalSeparator='D', groupingSeparator='_'"
                }) {
            assertFormatted(fs, 12345, "1_23_45D0");
        }
        
        assertFormatted("0.0;;infinity=infinity", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;infinity='infinity'", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;infinity=\"infinity\"", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;infinity=''", Double.POSITIVE_INFINITY, "");
        assertFormatted("0.0;;infinity=\"\"", Double.POSITIVE_INFINITY, "");
        assertFormatted("0.0;;infinity='x''y'", Double.POSITIVE_INFINITY, "x'y");
        assertFormatted("0.0;;infinity=\"x'y\"", Double.POSITIVE_INFINITY, "x'y");
        assertFormatted("0.0;;infinity='x\"\"y'", Double.POSITIVE_INFINITY, "x\"\"y");
        assertFormatted("0.0;;infinity=\"x''y\"", Double.POSITIVE_INFINITY, "x''y");
        assertFormatted("0.0;;decimalSeparator=''''", 1, "1'0");
        assertFormatted("0.0;;decimalSeparator=\"'\"", 1, "1'0");
        assertFormatted("0.0;;decimalSeparator='\"'", 1, "1\"0");
        assertFormatted("0.0;;decimalSeparator=\"\"\"\"", 1, "1\"0");
        
        try {
            ExtendedDecimalFormatParser.parse(";;decimalSeparator=D,", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsStringIgnoringCase("expected a(n) name"), containsString(" end of ")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;foo=D,", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("\"foo\""), containsString("name")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;decimalSeparator='D", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("quotation"), containsString("closed")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;decimalSeparator=\"D", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("quotation"), containsString("closed")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;decimalSeparator='D'groupingSeparator=G", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("separator"), containsString("whitespace"), containsString("comma")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;decimalSeparator=., groupingSeparator=G", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsStringIgnoringCase("expected a(n) value"), containsString("., gr[...]")));
        }
        try {
            ExtendedDecimalFormatParser.parse("0.0;;decimalSeparator=''", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsStringIgnoringCase("\"decimalSeparator\""), containsString("exactly 1 char")));
        }
        try {
            ExtendedDecimalFormatParser.parse("0.0;;multipier=ten", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("\"multipier\""), containsString("\"ten\""), containsString("integer")));
        }
    }
    
    @SuppressWarnings("boxing")
    @Test
    public void testExtendedParamsEffect() throws ParseException {
        assertFormatted("0",
                1.5, "2", 2.5, "2", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; roundingMode=halfEven",
                1.5, "2", 2.5, "2", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; roundingMode=halfUp",
                1.5, "2", 2.5, "3", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-3", -1.6, "-2");
        assertFormatted("0;; roundingMode=halfDown",
                1.5, "1", 2.5, "2", 3.5, "3", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-1", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; roundingMode=floor",
                1.5, "1", 2.5, "2", 3.5, "3", 1.4, "1", 1.6, "1", -1.4, "-2", -1.5, "-2", -2.5, "-3", -1.6, "-2");
        assertFormatted("0;; roundingMode=ceiling",
                1.5, "2", 2.5, "3", 3.5, "4", 1.4, "2", 1.6, "2", -1.4, "-1", -1.5, "-1", -2.5, "-2", -1.6, "-1");
        assertFormatted("0;; roundingMode=up",
                1.5, "2", 2.5, "3", 3.5, "4", 1.4, "2", 1.6, "2", -1.4, "-2", -1.5, "-2", -2.5, "-3", -1.6, "-2");
        assertFormatted("0;; roundingMode=down",
                1.5, "1", 2.5, "2", 3.5, "3", 1.4, "1", 1.6, "1", -1.4, "-1", -1.5, "-1", -2.5, "-2", -1.6, "-1");
        assertFormatted("0;; roundingMode=unnecessary", 2, "2");
        try {
            assertFormatted("0;; roundingMode=unnecessary", 2.5, "2");
            fail();
        } catch (ArithmeticException e) {
            // Expected
        }

        assertFormatted("0.##;; multipier=100", 12.345, "1234.5");
        assertFormatted("0.##;; multipier=1000", 12.345, "12345");
        assertFormatted("0.##;; multiplier=100", 12.345, "1234.5");
        assertFormatted("0.##;; multiplier=1000", 12.345, "12345");

        assertFormatted(",##0.##;; groupingSeparator=_ decimalSeparator=D", 12345.1, "12_345D1", 1, "1");
        
        assertFormatted("0.##E0;; exponentSeparator='*10^'", 12345.1, "1.23*10^4");
        
        assertFormatted("0.##;; minusSign=m", -1, "m1", 1, "1");
        
        assertFormatted("0.##;; infinity=foo", Double.POSITIVE_INFINITY, "foo", Double.NEGATIVE_INFINITY, "-foo");
        
        assertFormatted("0.##;; nan=foo", Double.NaN, "foo");
        
        assertFormatted("0%;; percent='c'", 0.75, "75c");
        
        assertFormatted("0\u2030;; perMill='m'", 0.75, "750m");
        
        assertFormatted("0.00;; zeroDigit='@'", 10.5, "A@.E@");
        
        assertFormatted("0;; currencyCode=USD", 10, "10");
        assertFormatted("0 \u00A4;; currencyCode=USD", 10, "10 $");
        assertFormatted("0 \u00A4\u00A4;; currencyCode=USD", 10, "10 USD");
        assertFormatted(Locale.GERMANY, "0 \u00A4;; currencyCode=EUR", 10, "10 \u20AC");
        assertFormatted(Locale.GERMANY, "0 \u00A4\u00A4;; currencyCode=EUR", 10, "10 EUR");
        try {
            assertFormatted("0;; currencyCode=USDX", 10, "10");
        } catch (ParseException e) {
            assertThat(e.getMessage(), containsString("ISO 4217"));
        }
        assertFormatted("0 \u00A4;; currencyCode=USD currencySymbol=bucks", 10, "10 bucks");
     // Order doesn't mater:
        assertFormatted("0 \u00A4;; currencySymbol=bucks currencyCode=USD", 10, "10 bucks");
        // International symbol isn't affected:
        assertFormatted("0 \u00A4\u00A4;; currencyCode=USD currencySymbol=bucks", 10, "10 USD");
        
        assertFormatted("0.0 \u00A4;; monetaryDecimalSeparator=m", 10.5, "10m5 $");
        assertFormatted("0.0 kg;; monetaryDecimalSeparator=m", 10.5, "10.5 kg");
        assertFormatted("0.0 \u00A4;; decimalSeparator=d", 10.5, "10.5 $");
        assertFormatted("0.0 kg;; decimalSeparator=d", 10.5, "10d5 kg");
        assertFormatted("0.0 \u00A4;; monetaryDecimalSeparator=m decimalSeparator=d", 10.5, "10m5 $");
        assertFormatted("0.0 kg;; monetaryDecimalSeparator=m decimalSeparator=d", 10.5, "10d5 kg");
    }
    
    @Test
    public void testLocale() throws ParseException {
        assertEquals("1000.0", ExtendedDecimalFormatParser.parse("0.0", Locale.US).format(1000));
        assertEquals("1000,0", ExtendedDecimalFormatParser.parse("0.0", Locale.FRANCE).format(1000));
        assertEquals("1_000.0", ExtendedDecimalFormatParser.parse(",000.0;;groupingSeparator=_", Locale.US).format(1000));
        assertEquals("1_000,0", ExtendedDecimalFormatParser.parse(",000.0;;groupingSeparator=_", Locale.FRANCE).format(1000));
    }
    
    @Test
    public void testTemplates() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setLocale(Locale.US);
        
        cfg.setNumberFormat(",000.#");
        assertOutput("${1000.15} ${1000.25}", "1,000.2 1,000.2");
        cfg.setNumberFormat(",000.#;; roundingMode=halfUp groupingSeparator=_");
        assertOutput("${1000.15} ${1000.25}", "1_000.2 1_000.3");
        cfg.setLocale(Locale.GERMANY);
        assertOutput("${1000.15} ${1000.25}", "1_000,2 1_000,3");
        cfg.setLocale(Locale.US);
        assertOutput(
                "${1000.15}; "
                + "${1000.15?string(',##.#;;groupingSeparator=\" \"')}; "
                + "<#setting locale='de_DE'>${1000.15}; "
                + "<#setting numberFormat='0.0;;roundingMode=down'>${1000.15}",
                "1_000.2; 10 00.2; 1_000,2; 1000,1");
        assertErrorContains("${1?string('#E')}",
                TemplateException.class, "\"#E\"", "format string", "exponential");
        assertErrorContains("<#setting numberFormat='#E'>${1}",
                TemplateException.class, "\"#E\"", "format string", "exponential");
        assertErrorContains("<#setting numberFormat=';;foo=bar'>${1}",
                TemplateException.class, "\"foo\"", "supported");
        assertErrorContains("<#setting numberFormat='0;;roundingMode=unnecessary'>${1.5}",
                TemplateException.class, "can't format", "1.5", "UNNECESSARY");
    }

    private void assertFormatted(String formatString, Object... numberAndExpectedOutput) throws ParseException {
        assertFormatted(LOC, formatString, numberAndExpectedOutput);
    }
    
    private void assertFormatted(Locale loc, String formatString, Object... numberAndExpectedOutput) throws ParseException {
        if (numberAndExpectedOutput.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        
        DecimalFormat df = ExtendedDecimalFormatParser.parse(formatString, loc);
        Number num = null;
        for (int i = 0; i < numberAndExpectedOutput.length; i++) {
            if (i % 2 == 0) {
                num = (Number) numberAndExpectedOutput[i];
            } else {
                assertEquals(numberAndExpectedOutput[i], df.format(num));
            }
        }
    }
    
    private void assertFormatsEquivalent(DecimalFormat dfExpected, DecimalFormat dfActual) {
        for (int signum : new int[] { 1, -1 }) {
            assertFormatsEquivalent(dfExpected, dfActual, signum * 0);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 0.5);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 0.25);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 0.125);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 1);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 10);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 100);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 1000);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 10000);
            assertFormatsEquivalent(dfExpected, dfActual, signum * 100000);
        }
    }

    private void assertFormatsEquivalent(DecimalFormat dfExpected, DecimalFormat dfActual, double n) {
        assertEquals(dfExpected.format(n), dfActual.format(n));
    }
    
}

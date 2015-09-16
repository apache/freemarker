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
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class ExtendedDecimalFormatTest extends TemplateTest {
    
    private static final Locale LOC = Locale.US;
    
    @Test
    public void testNonExtended() throws ParseException {
        for (String fStr : new String[] { "0.00", "0.###", "#,#0.###", "#0.####", "0.0;m", "0.0;",
                "0'x'", "0'x';'m'", "0';'", "0';';m", "0';';'#'m';'", "0';;'", "" }) {
            assertFormatsEquivalent(new DecimalFormat(fStr), ExtendedDecimalFormatParser.parse(fStr, LOC));
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
        assertFormatsEquivalent(new DecimalFormat("0.0"), ExtendedDecimalFormatParser.parse("0.0;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0.0"), ExtendedDecimalFormatParser.parse("0.0;;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0.0;m"), ExtendedDecimalFormatParser.parse("0.0;m;", LOC));
        assertFormatsEquivalent(new DecimalFormat(""), ExtendedDecimalFormatParser.parse(";;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0'x'"), ExtendedDecimalFormatParser.parse("0'x';;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0'x';'m'"), ExtendedDecimalFormatParser.parse("0'x';'m';", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';'"), ExtendedDecimalFormatParser.parse("0';';;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';';m"), ExtendedDecimalFormatParser.parse("0';';m;", LOC));
        assertFormatsEquivalent(new DecimalFormat("0';';'#'m';'"), ExtendedDecimalFormatParser.parse("0';';'#'m';';",
                LOC));
        assertFormatsEquivalent(new DecimalFormat("0';;'"), ExtendedDecimalFormatParser.parse("0';;';;", LOC));
        
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
                "00.##;; dec='D'", "00.##;;dec=D", "00.##;;  dec  =  D ", "00.##;; dec = 'D' " }) {
            assertFormatted(fs, 1.125, "01D12");
        }
        for (String fs : new String[] {
                ",#0.0;; dec=D, grp=_", ",#0.0;;dec=D,grp=_", ",#0.0;; dec = D , grp = _ ", ",#0.0;; dec='D', grp='_'"
                }) {
            assertFormatted(fs, 12345, "1_23_45D0");
        }
        
        assertFormatted("0.0;;inf=infinity", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;inf='infinity'", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;inf=\"infinity\"", Double.POSITIVE_INFINITY, "infinity");
        assertFormatted("0.0;;inf=''", Double.POSITIVE_INFINITY, "");
        assertFormatted("0.0;;inf=\"\"", Double.POSITIVE_INFINITY, "");
        assertFormatted("0.0;;inf='x''y'", Double.POSITIVE_INFINITY, "x'y");
        assertFormatted("0.0;;inf=\"x'y\"", Double.POSITIVE_INFINITY, "x'y");
        assertFormatted("0.0;;inf='x\"\"y'", Double.POSITIVE_INFINITY, "x\"\"y");
        assertFormatted("0.0;;inf=\"x''y\"", Double.POSITIVE_INFINITY, "x''y");
        assertFormatted("0.0;;dec=''''", 1, "1'0");
        assertFormatted("0.0;;dec=\"'\"", 1, "1'0");
        assertFormatted("0.0;;dec='\"'", 1, "1\"0");
        assertFormatted("0.0;;dec=\"\"\"\"", 1, "1\"0");
        
        try {
            ExtendedDecimalFormatParser.parse(";;dec=D,", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsStringIgnoringCase("expected a(n) name"), containsString(" end of ")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;xdec=D,", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("\"xdec\""), containsString("name")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;dec='D", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("quotation"), containsString("closed")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;dec=\"D", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("quotation"), containsString("closed")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;dec='D'grp=G", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("separator"), containsString("whitespace"), containsString("comma")));
        }
        try {
            ExtendedDecimalFormatParser.parse(";;dec=., grp=G", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsStringIgnoringCase("expected a(n) value"), containsString("., grp")));
        }
        try {
            ExtendedDecimalFormatParser.parse("0.0;;dec=''", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsStringIgnoringCase("\"dec\""), containsString("exactly 1 char")));
        }
        try {
            ExtendedDecimalFormatParser.parse("0.0;;mul=ten", LOC);
            fail();
        } catch (java.text.ParseException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("\"mul\""), containsString("\"ten\""), containsString("integer")));
        }
    }
    
    @SuppressWarnings("boxing")
    @Test
    public void testExtendedParamsEffect() throws ParseException {
        assertFormatted("0",
                1.5, "2", 2.5, "2", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; rnd=he",
                1.5, "2", 2.5, "2", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; rnd=hu",
                1.5, "2", 2.5, "3", 3.5, "4", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-2", -2.5, "-3", -1.6, "-2");
        assertFormatted("0;; rnd=hd",
                1.5, "1", 2.5, "2", 3.5, "3", 1.4, "1", 1.6, "2", -1.4, "-1", -1.5, "-1", -2.5, "-2", -1.6, "-2");
        assertFormatted("0;; rnd=f",
                1.5, "1", 2.5, "2", 3.5, "3", 1.4, "1", 1.6, "1", -1.4, "-2", -1.5, "-2", -2.5, "-3", -1.6, "-2");
        assertFormatted("0;; rnd=c",
                1.5, "2", 2.5, "3", 3.5, "4", 1.4, "2", 1.6, "2", -1.4, "-1", -1.5, "-1", -2.5, "-2", -1.6, "-1");
        assertFormatted("0;; rnd=un", 2, "2");
        try {
            assertFormatted("0;; rnd=un", 2.5, "2");
            fail();
        } catch (ArithmeticException e) {
            // Expected
        }

        assertFormatted("0.##;; mul=100", 12.345, "1234.5");
        assertFormatted("0.##;; mul=1000", 12.345, "12345");
        
        assertFormatted(",##0.##;; grp=_ dec=D", 12345.1, "12_345D1", 1, "1");
        
        assertFormatted("0.##E0;; exp='*10^'", 12345.1, "1.23*10^4");
        
        assertFormatted("0.##;; min=m", -1, "m1", 1, "1");
        
        assertFormatted("0.##;; inf=foo", Double.POSITIVE_INFINITY, "foo", Double.NEGATIVE_INFINITY, "-foo");
        
        assertFormatted("0.##;; nan=foo", Double.NaN, "foo");
        
        assertFormatted("0%;; prc='c'", 0.75, "75c");
        
        assertFormatted("0\u2030;; prm='m'", 0.75, "750m");
        
        assertFormatted("0.00;; zero='@'", 10.5, "A@.E@");
        
        assertFormatted("0;; curc=USD", 10, "10");
        assertFormatted("0 \u00A4;; curc=USD", 10, "10 $");
        assertFormatted("0 \u00A4\u00A4;; curc=USD", 10, "10 USD");
        assertFormatted(Locale.GERMANY, "0 \u00A4;; curc=EUR", 10, "10 \u20AC");
        assertFormatted(Locale.GERMANY, "0 \u00A4\u00A4;; curc=EUR", 10, "10 EUR");
        try {
            assertFormatted("0;; curc=USDX", 10, "10");
        } catch (ParseException e) {
            assertThat(e.getMessage(), containsString("ISO 4217"));
        }
        assertFormatted("0 \u00A4;; curc=USD curs=bucks", 10, "10 bucks");
        assertFormatted("0 \u00A4;; curs=bucks curc=USD", 10, "10 bucks"); // Order doesn't mater
        assertFormatted("0 \u00A4\u00A4;; curc=USD curs=bucks", 10, "10 USD"); // International symbol isn't affected
        
        assertFormatted("0.0 \u00A4;; mdec=m", 10.5, "10m5 $");
        assertFormatted("0.0 kg;; mdec=m", 10.5, "10.5 kg");
        assertFormatted("0.0 \u00A4;; dec=d", 10.5, "10.5 $");
        assertFormatted("0.0 kg;; dec=d", 10.5, "10d5 kg");
        assertFormatted("0.0 \u00A4;; mdec=m dec=d", 10.5, "10m5 $");
        assertFormatted("0.0 kg;; mdec=m dec=d", 10.5, "10d5 kg");
    }
    
    @Test
    public void testLocale() throws ParseException {
        assertEquals("1000.0", ExtendedDecimalFormatParser.parse("0.0", Locale.US).format(1000));
        assertEquals("1000,0", ExtendedDecimalFormatParser.parse("0.0", Locale.FRANCE).format(1000));
        assertEquals("1_000.0", ExtendedDecimalFormatParser.parse(",000.0;;grp=_", Locale.US).format(1000));
        assertEquals("1_000,0", ExtendedDecimalFormatParser.parse(",000.0;;grp=_", Locale.FRANCE).format(1000));
    }
    
    @Test
    public void testTemplates() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        cfg.setLocale(Locale.US);
        
        cfg.setNumberFormat(",000.#");
        assertOutput("${1000.15} ${1000.25}", "1,000.2 1,000.2");
        cfg.setNumberFormat(",000.#;; rnd=hu grp=_");
        assertOutput("${1000.15} ${1000.25}", "1_000.2 1_000.3");
        cfg.setLocale(Locale.GERMANY);
        assertOutput("${1000.15} ${1000.25}", "1_000,2 1_000,3");
        cfg.setLocale(Locale.US);
        assertOutput(
                "${1000.15}; "
                + "${1000.15?string(',##.#;;grp=\" \"')}; "
                + "<#setting locale='de_DE'>${1000.15}; "
                + "<#setting numberFormat='0.0;;rnd=d'>${1000.15}",
                "1_000.2; 10 00.2; 1_000,2; 1000,1");
        assertErrorContains("${1?string('#E')}", "\"#E\"", "format string", "exponential");
        assertErrorContains("<#setting numberFormat='#E'>${1}", "\"#E\"", "format string", "exponential");
        assertErrorContains("<#setting numberFormat=';;foo=bar'>${1}", "\"foo\"", "supported");
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

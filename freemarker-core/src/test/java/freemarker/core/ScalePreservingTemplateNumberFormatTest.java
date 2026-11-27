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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.test.TemplateTest;

/**
 * Tests the {@code "scalePreserving"} number format ({@link ScalePreservingTemplateNumberFormatFactory}).
 */
public class ScalePreservingTemplateNumberFormatTest extends TemplateTest {

    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setLocale(Locale.US);
        cfg.setNumberFormat("scalePreserving");
    }

    @Test
    public void testBigDecimalScaleIsPreserved() throws Exception {
        addToDataModel("n", new BigDecimal("123.45000"));
        assertOutput("${n}", "123.45000");

        addToDataModel("n", new BigDecimal("1.500"));
        assertOutput("${n}", "1.500");

        addToDataModel("n", new BigDecimal("0.0"));
        assertOutput("${n}", "0.0");
    }

    @Test
    public void testBigDecimalWithoutFractionDigits() throws Exception {
        addToDataModel("n", new BigDecimal("123"));
        assertOutput("${n}", "123");
    }

    /**
     * A negative scale means there are no fraction digits to preserve, and must not break the format (a naive
     * implementation could show a negative number of fraction digits, or throw).
     */
    @Test
    public void testBigDecimalWithNegativeScale() throws Exception {
        addToDataModel("n", new BigDecimal("12E+3"));
        assertOutput("${n}", "12,000");
    }

    @Test
    public void testGroupingSeparatorsAreShown() throws Exception {
        addToDataModel("n", new BigDecimal("12300000000000000.45000"));
        assertOutput("${n}", "12,300,000,000,000,000.45000");
    }

    @Test
    public void testLocaleIsFollowed() throws Exception {
        addToDataModel("n", new BigDecimal("1234.500"));
        assertOutput("${n}", "1,234.500");
        getConfiguration().setLocale(Locale.GERMANY);
        assertOutput("${n}", "1.234,500");
    }

    /**
     * The result of template arithmetic is a {@link BigDecimal} again, with the scale that follows from
     * {@link BigDecimal} arithmetic.
     */
    @Test
    public void testArithmeticResultScaleIsPreserved() throws Exception {
        addToDataModel("a", new BigDecimal("123.45000"));
        addToDataModel("b", new BigDecimal("17.5"));
        assertOutput("${a + 1}", "124.45000");
        assertOutput("${b + 2}", "19.5");
    }

    @Test
    public void testNonBigDecimalNumbers() throws Exception {
        addToDataModel("d", Double.valueOf(1.5));
        addToDataModel("d2", Double.valueOf(0.0625));
        addToDataModel("f", Float.valueOf(2.25f));
        addToDataModel("i", Integer.valueOf(1000));
        addToDataModel("l", Long.valueOf(1234567890123L));
        addToDataModel("bi", new BigInteger("12345678901234567890"));
        // As there are no format parameters, these are formatted like with the standard "number" format, so at most 3
        // fraction digits are shown.
        String template = "${d} ${d2} ${(1 / 3)?double} ${f} ${i} ${l} ${bi}";
        assertOutput(template, "1.5 0.062 0.333 2.25 1,000 1,234,567,890,123 12,345,678,901,234,567,890");

        getConfiguration().setLocale(Locale.GERMANY);
        assertOutput(template, "1,5 0,062 0,333 2,25 1.000 1.234.567.890.123 12.345.678.901.234.567.890");
    }

    /**
     * The format after the {@code |} must not affect {@link BigDecimal}-s.
     */
    @Test
    public void testOtherFormatIsUsedForNonBigDecimalsOnly() throws Exception {
        getConfiguration().setNumberFormat("scalePreserving|0.00##");
        addToDataModel("bd", new BigDecimal("1234.500000"));
        addToDataModel("bd2", new BigDecimal("1"));
        addToDataModel("d", Double.valueOf(1234.5));
        addToDataModel("d2", Double.valueOf(0.123456));
        addToDataModel("i", Integer.valueOf(7));
        assertOutput("${bd} ${bd2} ${d} ${d2} ${i}", "1,234.500000 1 1234.50 0.1235 7.00");
    }

    @Test
    public void testOtherFormatCanBeAnyFormatString() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setCustomNumberFormats(Collections.singletonMap("hex", HexTemplateNumberFormatFactory.INSTANCE));
        addToDataModel("bd", new BigDecimal("0.50"));
        addToDataModel("d", Double.valueOf(0.5));
        addToDataModel("i", Integer.valueOf(255));

        cfg.setNumberFormat("scalePreserving|percent");
        assertOutput("${bd} ${d}", "0.50 50%");

        cfg.setNumberFormat("scalePreserving|c");
        assertOutput("${bd} ${1234567.125?double}", "0.50 1234567.125");

        cfg.setNumberFormat("scalePreserving|@hex");
        assertOutput("${bd} ${i}", "0.50 ff");

        cfg.setNumberFormat("scalePreserving|,##0.0;; roundingMode=halfUp groupingSeparator=_");
        assertOutput("${bd} ${1234.25?double}", "0.50 1_234.3");

        cfg.setNumberFormat("scalePreserving|number");
        assertOutput("${bd} ${0.0625?double}", "0.50 0.062");
    }

    /**
     * Changing the {@code c_format} setting must affect the already created (cached) formats that wrap {@code c}.
     */
    @Test
    public void testOtherFormatFollowsCFormatChange() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_32);
        cfg.setCustomNumberFormats(Collections.singletonMap(
                "price", ScalePreservingTemplateNumberFormatFactory.INSTANCE));
        addToDataModel("inf", Double.valueOf(Double.POSITIVE_INFINITY));

        cfg.setNumberFormat("scalePreserving|c");
        assertOutput("${inf}<#setting c_format='legacy'> ${inf}", "Infinity INF");

        cfg.setNumberFormat("number");
        assertOutput(
                "${inf?string('scalePreserving|c')} ${inf?string('@price |c')}"
                        + "<#setting c_format='legacy'>"
                        + " ${inf?string('scalePreserving|c')} ${inf?string('@price |c')}",
                "Infinity Infinity INF INF");
    }

    @Test
    public void testOtherFormatCanBeScalePreserving() throws Exception {
        // Pointless, but must not cause problems (like infinite recursion)
        getConfiguration().setNumberFormat("scalePreserving|scalePreserving|0.00");
        addToDataModel("bd", new BigDecimal("1.500"));
        assertOutput("${bd} ${1.5?double}", "1.500 1.50");
    }

    @Test
    public void testOtherFormatFollowsLocale() throws Exception {
        getConfiguration().setNumberFormat("scalePreserving|,##0.00");
        addToDataModel("bd", new BigDecimal("1234.500"));
        addToDataModel("d", Double.valueOf(1234.5));
        assertOutput("${bd} ${d}", "1,234.500 1,234.50");
        getConfiguration().setLocale(Locale.GERMANY);
        assertOutput("${bd} ${d}", "1.234,500 1.234,50");
        assertOutput("<#setting locale='fr_FR'>${d?string('scalePreserving|0.00')}", "1234,50");
    }

    /**
     * The markup output of the non-{@link BigDecimal} format must not be escaped.
     */
    @Test
    public void testOtherFormatCanProduceMarkup() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setCustomNumberFormats(Collections.singletonMap(
                "printfG", PrintfGTemplateNumberFormatFactory.INSTANCE));
        cfg.setNumberFormat("scalePreserving|@printfG_3");
        addToDataModel("bd", new BigDecimal("1.500"));
        addToDataModel("i", Integer.valueOf(1234567));
        assertOutput("${bd} ${i}", "1.500 1.23*10<sup>6</sup>");
    }

    @Test
    public void testMaxFractionDigitsShorthand() throws Exception {
        getConfiguration().setNumberFormat("scalePreserving_6");
        addToDataModel("bd", new BigDecimal("1234.50000000"));
        addToDataModel("d", Double.valueOf(1234.5));
        addToDataModel("i", Integer.valueOf(1000));
        assertOutput("${bd} ${d} ${0.12345678?double} ${i}", "1,234.50000000 1,234.5 0.123457 1,000");

        getConfiguration().setNumberFormat("scalePreserving_0");
        assertOutput("${bd} ${d} ${i}", "1,234.50000000 1,234 1,000");
    }

    @Test
    public void testMinAndMaxFractionDigitsShorthand() throws Exception {
        getConfiguration().setNumberFormat("scalePreserving_2_6");
        addToDataModel("bd", new BigDecimal("1234.5"));
        addToDataModel("d", Double.valueOf(1234.5));
        addToDataModel("i", Integer.valueOf(1000));
        assertOutput("${bd} ${d} ${0.12345678?double} ${i}", "1,234.5 1,234.50 0.123457 1,000.00");

        getConfiguration().setLocale(Locale.GERMANY);
        assertOutput("${bd} ${d}", "1.234,5 1.234,50");

        getConfiguration().setNumberFormat("scalePreserving_3_3");
        assertOutput("${d}", "1.234,500");
    }

    /**
     * Division results have a scale of at least 12 with the default {@link ArithmeticEngine}, which is shown as is.
     */
    @Test
    public void testDivisionResult() throws Exception {
        assertOutput("${6 / 2}", "3.000000000000");
        assertOutput("${(6 / 2)?string.number}", "3");
        assertOutput("${(1 / 3)?double}", "0.333");
    }

    @Test
    public void testWorksWithStringBuiltIn() throws Exception {
        getConfiguration().setNumberFormat("number");
        addToDataModel("n", new BigDecimal("1.500"));
        addToDataModel("d", Double.valueOf(1.5));
        assertOutput("${n} ${n?string.scalePreserving}", "1.5 1.500");
        assertOutput("${n?string.scalePreserving_2_6} ${d?string.scalePreserving_2_6}", "1.500 1.50");
        assertOutput("${n?string.scalePreserving_2} ${0.125?double?string.scalePreserving_2}", "1.500 0.12");
        assertOutput("${n?string('scalePreserving|0.00')} ${d?string('scalePreserving|0.00')}", "1.500 1.50");
    }

    @Test
    public void testWorksWithSettingDirective() throws Exception {
        getConfiguration().setNumberFormat("number");
        addToDataModel("n", new BigDecimal("1.500"));
        addToDataModel("d", Double.valueOf(1.5));
        assertOutput("${n}<#setting number_format='scalePreserving'>${n}", "1.51.500");
        assertOutput("<#setting number_format='scalePreserving|0.00'>${n} ${d}", "1.500 1.50");
        assertOutput("<#setting number_format='scalePreserving_2_6'>${n} ${d}", "1.500 1.50");
    }

    @Test
    public void testSimilarFormatStringsAreNotAffected() throws Exception {
        addToDataModel("n", new BigDecimal("1.500"));
        // These must remain DecimalFormat patterns
        assertOutput("${n?string('scalePreservingX0.0')}", "scalePreservingX1.5");
        assertOutput("${n?string('scalePreserving 0.0')}", "scalePreserving 1.5");
    }

    /**
     * Besides being available as the {@code "scalePreserving"} built-in format name, the factory is public, so it can
     * also be registered as a custom format under an application-specific name.
     */
    @Test
    public void testWorksAsCustomFormat() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setCustomNumberFormats(Collections.singletonMap(
                "price", ScalePreservingTemplateNumberFormatFactory.INSTANCE));
        cfg.setNumberFormat("number");
        addToDataModel("n", new BigDecimal("1.500"));
        addToDataModel("d", Double.valueOf(1.5));
        assertOutput("${n} ${n?string.@price} ${0.0625?double?string.@price}", "1.5 1.500 0.062");
        assertOutput("${n?string.@price_2_6} ${d?string.@price_2_6}", "1.500 1.50");
        assertOutput("${n?string('@price |0.00')} ${d?string('@price |0.00')}", "1.500 1.50");
        assertOutput("${n?string('@price_|0.00')} ${d?string('@price_|0.00')}", "1.500 1.50");
    }

    @Test
    public void testMalformedShorthandParametersAreRejected() throws Exception {
        addToDataModel("n", new BigDecimal("1.500"));
        for (String params : new String[] { "x", "2_x", "x_2", "_2", "2_", "2_3_4", "-2", "+2", "2.5", "2 ",
                "99999999999", "\u0662" }) {
            assertErrorContains("${n?string('scalePreserving_" + params + "')}",
                    "Malformed format parameters", "\"|0.00##\"", "\"2_6\"");
        }
    }

    @Test
    public void testEmptyShorthandParametersMeanNoParameters() throws Exception {
        addToDataModel("n", new BigDecimal("1.500"));
        // Consistent with custom formats, where "@price_" means no parameters either
        assertOutput("${n?string.scalePreserving_} ${0.0625?double?string.scalePreserving_}", "1.500 0.062");
    }

    @Test
    public void testMinGreaterThanMaxFractionDigitsIsRejected() throws Exception {
        addToDataModel("n", new BigDecimal("1.500"));
        assertErrorContains("${n?string.scalePreserving_6_2}", "minimum", "(6)", "greater", "maximum", "(2)");
    }

    @Test
    public void testMissingOrInvalidOtherFormatIsRejected() throws Exception {
        // We need to have a custom format, as otherwise "@noSuchFormat" can be a legacy DecimalFormat pattern
        getConfiguration().setCustomNumberFormats(Collections.singletonMap(
                "hex", HexTemplateNumberFormatFactory.INSTANCE));
        addToDataModel("n", new BigDecimal("1.500"));
        assertErrorContains("${n?string('scalePreserving|')}", "non-BigDecimal", "missing");
        assertErrorContains("${n?string('scalePreserving|@noSuchFormat')}", "noSuchFormat");
        assertErrorContains("${n?string('scalePreserving|0.0;; noSuchOption=1')}", "noSuchOption");
    }

    @Test
    public void testCustomFormatParametersAreValidated() throws Exception {
        getConfiguration().setCustomNumberFormats(Collections.singletonMap(
                "price", ScalePreservingTemplateNumberFormatFactory.INSTANCE));
        addToDataModel("n", new BigDecimal("1.500"));
        assertErrorContains("${n?string.@price_x}", "Malformed format parameters");
        assertErrorContains("${n?string('@price |')}", "missing");
    }

    /**
     * The factory is public, so it can be called without an {@link Environment}.
     */
    @Test
    public void testFactoryWorksWithoutEnvironment() throws Exception {
        ScalePreservingTemplateNumberFormatFactory factory = ScalePreservingTemplateNumberFormatFactory.INSTANCE;
        SimpleNumber bigDecimal = new SimpleNumber(new BigDecimal("1.500"));
        SimpleNumber nonBigDecimal = new SimpleNumber(Double.valueOf(0.0625));

        TemplateNumberFormat format = factory.get("", Locale.US, null);
        assertEquals("1.500", format.formatToPlainText(bigDecimal));
        assertEquals("0.062", format.formatToPlainText(nonBigDecimal));
        assertTrue(format.isLocaleBound());

        format = factory.get("|0.00000", Locale.GERMANY, null);
        assertEquals("1,500", format.formatToPlainText(bigDecimal));
        assertEquals("0,06250", format.formatToPlainText(nonBigDecimal));
        assertEquals("0,06250", format.format(nonBigDecimal));

        format = factory.get("1_2", Locale.US, null);
        assertEquals("1.500", format.formatToPlainText(bigDecimal));
        assertEquals("0.06", format.formatToPlainText(nonBigDecimal));
        assertEquals("1.0", format.formatToPlainText(new SimpleNumber(Integer.valueOf(1))));
    }

    /**
     * Without an {@link Environment} these can't be resolved, and must not be treated as
     * {@link java.text.DecimalFormat} patterns instead.
     */
    @Test
    public void testOtherFormatsRejectedWithoutEnvironment() throws Exception {
        for (String otherFormat : new String[] { "@hex", "c", "computer", "scalePreserving", "scalePreserving|0.00",
                "scalePreserving_2" }) {
            try {
                ScalePreservingTemplateNumberFormatFactory.INSTANCE.get("|" + otherFormat, Locale.US, null);
                fail("Should have failed for: " + otherFormat);
            } catch (InvalidFormatParametersException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("\"" + otherFormat + "\""));
                assertTrue(e.getMessage(), e.getMessage().contains("without an Environment"));
            }
        }
    }
}

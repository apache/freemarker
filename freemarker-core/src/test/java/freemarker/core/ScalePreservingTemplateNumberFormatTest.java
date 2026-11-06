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
import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

/**
 * Tests {@link ScalePreservingTemplateNumberFormatFactory}, available under the {@code "scalePreserving"} built-in
 * format name, whose main promise is that the {@linkplain BigDecimal#scale() scale} of a {@link BigDecimal},
 * including trailing 0-s within that scale, survives formatting (FREEMARKER-232), while non-{@link BigDecimal}
 * numbers are still formatted with all their fraction digits.
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
        addToDataModel("i", Integer.valueOf(1000));
        addToDataModel("bi", new BigInteger("12345678901234567890"));
        // Unlike the standard "number" format, this shows all fraction digits (not at most 3), but as these types
        // carry no scale, no trailing 0-s are added either.
        assertOutput("${d} ${0.0625?double} ${i} ${bi}", "1.5 0.0625 1,000 12,345,678,901,234,567,890");
    }

    @Test
    public void testWorksWithStringBuiltIn() throws Exception {
        getConfiguration().setNumberFormat("number");
        addToDataModel("n", new BigDecimal("1.500"));
        assertOutput("${n} ${n?string.scalePreserving}", "1.5 1.500");
    }

    @Test
    public void testWorksWithSettingDirective() throws Exception {
        getConfiguration().setNumberFormat("number");
        addToDataModel("n", new BigDecimal("1.500"));
        assertOutput("${n}<#setting number_format='scalePreserving'>${n}", "1.51.500");
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
        assertOutput("${n} ${n?string.@price}", "1.5 1.500");
    }

    @Test
    public void testParametersAreRejected() throws Exception {
        getConfiguration().setCustomNumberFormats(Collections.singletonMap(
                "scalePreserving", ScalePreservingTemplateNumberFormatFactory.INSTANCE));
        addToDataModel("n", new BigDecimal("1.500"));
        assertErrorContains("${n?string.@scalePreserving_x}", "doesn't support any parameters");
    }

}

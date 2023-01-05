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

import org.junit.Test;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.UnsupportedNumberClassException;

public class CTemplateNumberFormatTest {
    @Test
    public void testFormatDouble() throws TemplateModelException, TemplateValueFormatException {
        testFormat(1.0, "1");
        testFormat(1.2, "1.2");
        testFormat(9007199254740991d, "9007199254740991");
        testFormat(9007199254740992d, "9007199254740992");
        testFormat(9007199254740994d, "9.007199254740994E15");
        testFormat(10000000000000000d, "1E16");
        testFormat(12300000000000000d, "1.23E16");
        testFormat(Double.NaN, "NaN");
        testFormat(Double.POSITIVE_INFINITY, "Infinity");
        testFormat(Double.NEGATIVE_INFINITY, "-Infinity");
        testFormat(1.9E-6, "0.0000019");
        testFormat(9.5E-7, "9.5E-7");
        testFormat(9999999.5, "9999999.5");
        testFormat(10000000.5, "10000000.5");
    }

    @Test
    public void testFormatFloat() throws TemplateModelException, TemplateValueFormatException {
        testFormat(1.0f, "1");
        testFormat(1.2f, "1.2");
        testFormat(16777215f, "16777215");
        testFormat(16777216f, "16777216");
        testFormat(16777218f, "1.6777218E7");
        testFormat(100000000f, "1E8");
        testFormat(123000000f, "1.23E8");
        testFormat(Float.NaN, "NaN");
        testFormat(Float.POSITIVE_INFINITY, "Infinity");
        testFormat(Float.NEGATIVE_INFINITY, "-Infinity");
        testFormat(1.9E-6f, "0.0000019");
        testFormat(9.5E-7f, "9.5E-7");
        testFormat(1000000.5f, "1000000.5");
        // For float, values >= 1E7 has ulp >= 1, so we don't have to deal with non-wholes in that range.
    }

    @Test
    public void testFormatBigInteger() throws TemplateModelException, TemplateValueFormatException {
        testFormat(new BigInteger("-0"), "0");
        testFormat(new BigInteger("1"), "1");
        testFormat(new BigInteger("9000000000000000000000"), "9000000000000000000000");
    }

    @Test
    public void testFormatBigDecimalWholeNumbers() throws TemplateModelException, TemplateValueFormatException {
        testFormat(new BigDecimal("-0"), "0");
        testFormat(new BigDecimal("1.0"), "1");
        testFormat(new BigDecimal("10E-1"), "1");
        testFormat(new BigDecimal("0.01E2"), "1");
        testFormat(new BigDecimal("9000000000000000000000"), "9000000000000000000000");
        testFormat(new BigDecimal("9e21"), "9000000000000000000000");
        testFormat(new BigDecimal("9e100"), "9E+100");
    }

    @Test
    public void testFormatBigDecimalNonWholeNumbers() throws TemplateModelException, TemplateValueFormatException {
        testFormat(new BigDecimal("0.1"), "0.1");
        testFormat(new BigDecimal("1E-1"), "0.1");
        testFormat(new BigDecimal("0.0000001"), "1E-7");
        testFormat(new BigDecimal("0.00000010"), "1E-7");
        testFormat(new BigDecimal("0.000000999"), "9.99E-7");
        testFormat(new BigDecimal("-0.0000001"), "-1E-7");
        testFormat(new BigDecimal("0.000000123"), "1.23E-7");
        testFormat(new BigDecimal("1E-6"), "0.000001");
        testFormat(new BigDecimal("0.0000010"), "0.000001");
        testFormat(new BigDecimal("1.0000000001"), "1.0000000001");
        testFormat(new BigDecimal("1000000000.5"), "1000000000.5");
    }

    private void testFormat(Number n, String expectedResult) throws TemplateModelException,
        TemplateValueFormatException {
        TemplateNumberFormat cTemplateNumberFormat = JSONCFormat.INSTANCE.getTemplateNumberFormat(null);
        String actualResult = (String) cTemplateNumberFormat.format(new SimpleNumber(n));
        assertFormatResult(n, actualResult, expectedResult);
        if (!actualResult.equals("NaN") && !actualResult.equals("0") && !actualResult.startsWith("-")) {
            Number negativeN = negate(n);
            actualResult = (String) cTemplateNumberFormat.format(new SimpleNumber(negativeN));
            assertFormatResult(negativeN, actualResult, "-" + expectedResult);
        }
    }

    private static Number negate(Number n) {
        if (n instanceof Integer) {
            return -n.intValue();
        } else if (n instanceof BigDecimal) {
            return ((BigDecimal) n).negate();
        } else if (n instanceof Double) {
            return -n.doubleValue();
        } else if (n instanceof Float) {
            return -n.floatValue();
        } else if (n instanceof Long) {
            return -n.longValue();
        } else if (n instanceof Short) {
            return -n.shortValue();
        } else if (n instanceof Byte) {
            return -n.byteValue();
        } else if (n instanceof BigInteger) {
            return ((BigInteger) n).negate();
        } else {
            throw new UnsupportedNumberClassException(n.getClass());
        }
    }

    private void assertFormatResult(Number n, String actualResult, String expectedResult) {
        if (!actualResult.equals(expectedResult)) {
            fail("When formatting " + n + ", expected \"" + expectedResult + "\", but got "
                    + "\"" + actualResult + "\".");
        }
    }

}
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

package org.apache.freemarker.core;

import org.apache.freemarker.core.cformat.impl.JSONCFormat;
import org.apache.freemarker.core.cformat.impl.JavaScriptCFormat;
import org.apache.freemarker.core.cformat.impl.JavaScriptOrJSONCFormat;
import org.apache.freemarker.core.cformat.impl.XSCFormat;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;

public class CAndCnBuiltInTest extends TemplateTest {

    @Before
    public void addModelVariables() {
        addToDataModel("double1", 1.0);
        addToDataModel("double2", 1.000000000000001);
        addToDataModel("double3", 0.0000000000000001);
        addToDataModel("double4", -0.0000000000000001);
        addToDataModel("bigDecimal1", new BigDecimal("1"));
        addToDataModel("bigDecimal2", new BigDecimal("0.0000000000000001"));
        addToDataModel("doubleInf", Double.POSITIVE_INFINITY);
        addToDataModel("doubleNegativeInf", Double.NEGATIVE_INFINITY);
        addToDataModel("doubleNaN", Double.NaN);
        addToDataModel("floatInf", Float.POSITIVE_INFINITY);
        addToDataModel("floatNegativeInf", Float.NEGATIVE_INFINITY);
        addToDataModel("floatNaN", Float.NaN);
        addToDataModel("string", "a\nb\u0000c");
        addToDataModel("long", Long.MAX_VALUE);
        addToDataModel("int", Integer.MAX_VALUE);
        addToDataModel("bigInteger", new BigInteger("123456789123456789123456789123456789"));
        addToDataModel("dateTime", new Timestamp(1671641049876L));
        addToDataModel("booleanTrue", true);
        addToDataModel("booleanFalse", false);
    }

    @Test
    public void testCWithNumber() throws TemplateException, IOException {
        testWithNumber("c");
    }

    @Test
    public void testCnWithNumber() throws TemplateException, IOException {
        testWithNumber("cn");
    }

    void testWithNumber(String builtInName) throws TemplateException, IOException {
        // Always the same
        assertOutput("${double1? " + builtInName + "}", "1");
        assertOutput("${double2?" + builtInName + "}", "1.000000000000001");
        assertOutput("${bigDecimal1? " + builtInName + "}", "1");
        assertOutput("${int? " + builtInName + "}", String.valueOf(Integer.MAX_VALUE));
        assertOutput("${long? " + builtInName + "}", String.valueOf(Long.MAX_VALUE));
        assertOutput("${bigInteger? " + builtInName + "}", "123456789123456789123456789123456789");
        assertOutput("${double3?" + builtInName + "}", "1E-16");
        assertOutput("${double4?" + builtInName + "}", "-1E-16");
        assertOutput("${bigDecimal2?" + builtInName + "}", "1E-16");

        for (String type : new String[] {"float", "double"}) {
            String expectedInf = "Infinity";
            String expectedNaN = "NaN";
            assertOutput("${" + type + "Inf?" + builtInName + "}", expectedInf);
            assertOutput("${" + type + "NegativeInf?" + builtInName + "}", "-" + expectedInf);
            assertOutput("${" + type + "NaN?" + builtInName + "}", expectedNaN);
        }
    }

    @Test
    public void testCNWithNonNumber() throws TemplateException, IOException {
        testWithNonNumber("c");
    }

    @Test
    public void testCWithNonNumber() throws TemplateException, IOException {
        testWithNonNumber("cn");
    }

    private void testWithNonNumber(String builtInName) throws TemplateException, IOException {
        assertOutput("${string?" + builtInName + "}", "\"a\\nb\\u0000c\"");
        assertOutput("${booleanTrue?" + builtInName + "}", "true");
        assertOutput("${booleanFalse?" + builtInName + "}", "false");
        assertErrorContains("${dateTime?" + builtInName + "}",
                "Expected a number, boolean, or string");
    }

    @Test
    public void testCFormatsWithString() throws TemplateException, IOException {
        assertOutput("${string?c}", "\"a\\nb\\u0000c\"");
        setConfiguration(newConfigurationBuilder().cFormat(JavaScriptCFormat.INSTANCE));
        assertOutput("${string?c}", "\"a\\nb\\x00c\"");
        setConfiguration(newConfigurationBuilder().cFormat(JSONCFormat.INSTANCE));
        assertOutput("${string?c}", "\"a\\nb\\u0000c\"");
        setConfiguration(newConfigurationBuilder().cFormat(JavaScriptOrJSONCFormat.INSTANCE));
        assertOutput("${string?c}", "\"a\\nb\\u0000c\"");
        setConfiguration(newConfigurationBuilder().cFormat(XSCFormat.INSTANCE));
        assertOutput("${string?c}", "a\nb\u0000c");
    }

    @Test
    public void testWithNull() throws TemplateException, IOException {
        assertOutput("${noSuchVar?cn}", "null");
        assertErrorContains("${noSuchVar?c}", "null or missing");
    }
    
}

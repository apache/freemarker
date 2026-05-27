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

import java.io.IOException;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class HexLiteralTest extends TemplateTest {

    @Test
    public void testBasicHex() throws IOException, TemplateException {
        assertOutput("${0xFF?c}", "255");
        assertOutput("${0xff?c}", "255");
        assertOutput("${0x0?c}", "0");
        assertOutput("${0x1?c}", "1");
        assertOutput("${0x10?c}", "16");
        assertOutput("${0xA?c}", "10");
        assertOutput("${0xDEAD?c}", "57005");
    }

    @Test
    public void testHexInExpressions() throws IOException, TemplateException {
        assertOutput("${(0xFF + 1)?c}", "256");
        assertOutput("${(0x10 * 2)?c}", "32");
        assertOutput("${(0xFF - 0x0F)?c}", "240");
    }

    @Test
    public void testHexAssignment() throws IOException, TemplateException {
        assertOutput("<#assign x = 0xFF>${x?c}", "255");
        assertOutput("<#assign x = 0x00FF00>${x?c}", "65280");
    }

    @Test
    public void testHexComparison() throws IOException, TemplateException {
        assertOutput("<#if 0xFF == 255>yes</#if>", "yes");
        assertOutput("<#if (0xFF > 0xFE)>yes</#if>", "yes");
    }

    @Test
    public void testHexLargeValues() throws IOException, TemplateException {
        // Values that fit in int
        assertOutput("${0x7FFFFFFF?c}", "2147483647");
        // Values that require long
        assertOutput("${0x80000000?c}", "2147483648");
        assertOutput("${0xFFFFFFFF?c}", "4294967295");
        assertOutput("${0x100000000?c}", "4294967296");
        // Highest value that still fits in a signed long
        assertOutput("${0x7FFFFFFFFFFFFFFF?c}", "9223372036854775807");
    }

    @Test
    public void testHexBeyondLongRange() throws IOException, TemplateException {
        // Values too large for a signed long stay exact (BigInteger), rather than
        // overflowing or failing to parse.
        assertOutput("${0x8000000000000000?c}", "9223372036854775808");
        assertOutput("${0xFFFFFFFFFFFFFFFF?c}", "18446744073709551615");
        // Arbitrarily many digits: 0x1 followed by 32 zeros is 16^32.
        assertOutput("${0x100000000000000000000000000000000?c}",
                "340282366920938463463374607431768211456");
    }

    @Test
    public void testHexLeadingZerosDoNotChangeValue() throws IOException, TemplateException {
        // Leading zeros must not push a small value into a wider type.
        assertOutput("${0x00000000000000000000FF?c}", "255");
    }

    @Test
    public void testHexUppercaseX() throws IOException, TemplateException {
        assertOutput("${0XFF?c}", "255");
        assertOutput("${0Xff?c}", "255");
    }
}

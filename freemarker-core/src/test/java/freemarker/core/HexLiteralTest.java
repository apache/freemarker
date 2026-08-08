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
import java.util.List;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.ClassUtil;
import freemarker.test.TemplateTest;

public class HexLiteralTest extends TemplateTest {

    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration conf = super.createConfiguration();
        conf.setNumberFormat("c");
        return conf;
    }

    @Test
    public void testBasicValues() throws IOException, TemplateException {
        assertExpOutput("0xFF", "255");
        assertExpOutput("0x0", "0");
        assertExpOutput("0x1", "1");
        assertExpOutput("0x10", "16");
        assertExpOutput("0xA", "10");
        assertExpOutput("0xDEAD", "57005");
    }

    @Test
    public void testCaseInsensitive() throws IOException, TemplateException {
        for (String hex : List.of("0xCAFE", "0XCAFE", "0Xcafe", "0xCafE")) {
            assertExpOutput(hex, "51966");
        }
    }

    @Test
    public void testHexInExpressions() throws IOException, TemplateException {
        assertExpOutput("0xFF + 1", "256");
        assertExpOutput("0x10 * 2", "32");
        assertExpOutput("0xFF - 0x0F", "240");
    }

    @Test
    public void testHexAssignment() throws IOException, TemplateException {
        assertOutput("<#assign x = 0xFF>${x}", "255");
        assertOutput("<#assign x = 0x00FF00>${x}", "65280");
    }

    @Test
    public void testHexComparison() throws IOException, TemplateException {
        assertOutput("<#if 0xFF == 255>yes</#if>", "yes");
        assertOutput("<#if (0xFF > 0xFE)>yes</#if>", "yes");
    }

    @Test
    public void testHexValueTypes() throws IOException, TemplateException {
        addDumpMethod();

        // Values that fit in int
        assertExpOutput("dump(0x0)", "Integer 0");
        assertExpOutput("dump(0x7FFFFFFF)", "Integer 2147483647");

        // Values that require long
        assertExpOutput("dump(0x80000000)", "Long 2147483648");
        assertExpOutput("dump(0xFFFFFFFF)", "Long 4294967295");
        assertExpOutput("dump(0x100000000)", "Long 4294967296");
        // Highest value that still fits in a signed long
        assertExpOutput("dump(0x7FFFFFFFFFFFFFFF)", "Long 9223372036854775807");

        // Values too large for a signed long become BigInteger:
        assertExpOutput(
                "dump(0x8000000000000000)", "java.math.BigInteger 9223372036854775808");
        assertExpOutput(
                "dump(0xFFFFFFFFFFFFFFFF)", "java.math.BigInteger 18446744073709551615");
        assertExpOutput(
                "dump(0x100000000000000000000000000000000)",
                "java.math.BigInteger 340282366920938463463374607431768211456");
    }

    @Test
    public void testHexLeadingZerosDoNotChangeValueOrType() throws IOException, TemplateException {
        addDumpMethod();

        // Leading zeros must not push a small value into a wider type.
        assertExpOutput("dump(0x000000000000007FFFFFFF)", "Integer 2147483647");
        assertExpOutput("dump(0x0000007FFFFFFFFFFFFFFF)", "Long 9223372036854775807");
        assertExpOutput(
                "dump(0x0000008000000000000000)", "java.math.BigInteger 9223372036854775808");
    }

    @Test
    public void testNegativeValues() throws IOException, TemplateException {
        assertExpOutput("-0xFF", "-255");
        assertExpOutput("-0x80000000", "-2147483648");
    }

    private void addDumpMethod() {
        addToDataModel("dump", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                Number arg = ((TemplateNumberModel) arguments.get(0)).getAsNumber();
                return ClassUtil.getShortClassNameOfObject(arg) + " " + arg;
            }
        });
    }
}

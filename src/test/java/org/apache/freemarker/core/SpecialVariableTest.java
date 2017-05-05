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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Test;

public class SpecialVariableTest extends TemplateTest {

    @Test
    public void testNamesSorted() throws Exception {
        String prevName = null;
        for (String name : ASTExpBuiltInVariable.SPEC_VAR_NAMES) {
            if (prevName != null) {
                assertThat(name, greaterThan(prevName));
            }
            prevName = name;
        }
    }
    
    @Test
    public void testVersion() throws Exception {
        String versionStr = Configuration.getVersion().toString();
        assertOutput("${.version}", versionStr);
    }

    @Test
    public void testIncompationImprovements() throws Exception {
        setConfiguration(new Configuration.Builder(Configuration.VERSION_3_0_0).build());
        assertOutput(
                "${.incompatibleImprovements}",
                getConfiguration().getIncompatibleImprovements().toString());
        
        setConfiguration(new Configuration.Builder(Configuration.getVersion()).build());
        assertOutput(
                "${.incompatible_improvements}",
                getConfiguration().getIncompatibleImprovements().toString());
    }

    @Test
    public void testAutoEsc() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        for (int autoEscaping : new int[] {
                ParsingConfiguration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY, ParsingConfiguration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY }) {
            cfgB.setAutoEscapingPolicy(autoEscaping);
            cfgB.setOutputFormat(HTMLOutputFormat.INSTANCE);
            setConfiguration(cfgB.build());
            assertOutput("${.autoEsc?c}", "true");
            assertOutput("<#ftl autoEsc=false>${.autoEsc?c}", "false");

            cfgB.setOutputFormat(PlainTextOutputFormat.INSTANCE);
            setConfiguration(cfgB.build());
            assertOutput("${.autoEsc?c}", "false");

            cfgB.setOutputFormat(UndefinedOutputFormat.INSTANCE);
            setConfiguration(cfgB.build());
            assertOutput("${.autoEsc?c}", "false");
        }
        
        cfgB.setAutoEscapingPolicy(ParsingConfiguration.DISABLE_AUTO_ESCAPING_POLICY);
        cfgB.setOutputFormat(HTMLOutputFormat.INSTANCE);
        setConfiguration(cfgB.build());
        assertOutput("${.autoEsc?c}", "false");
        assertOutput("<#ftl autoEsc=true>${.autoEsc?c}", "true");

        cfgB.setOutputFormat(PlainTextOutputFormat.INSTANCE);
        setConfiguration(cfgB.build());
        assertOutput("${.autoEsc?c}", "false");

        cfgB.setOutputFormat(UndefinedOutputFormat.INSTANCE);
        setConfiguration(cfgB.build());
        assertOutput("${.autoEsc?c}", "false");

        cfgB.setAutoEscapingPolicy(ParsingConfiguration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
        setConfiguration(cfgB.build());
        assertOutput(
                "${.autoEsc?c} "
                + "<#outputFormat 'HTML'>${.autoEsc?c}</#outputFormat> "
                + "<#outputFormat 'undefined'>${.autoEsc?c}</#outputFormat> "
                + "<#outputFormat 'HTML'>"
                + "${.autoEsc?c} <#noAutoEsc>${.autoEsc?c} "
                + "<#autoEsc>${.autoEsc?c}</#autoEsc> ${.autoEsc?c}</#noAutoEsc> ${.autoEsc?c}"
                + "</#outputFormat>",
                "false true false "
                + "true false true false true");
        
        assertErrorContains("${.autoEscaping}", "You may meant: \"autoEsc\"");
    }
    
}

/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.test.TemplateTest;

public class SpecialVariableTest extends TemplateTest {

    @Test
    public void testNamesSorted() throws Exception {
        String prevName = null;
        for (String name : BuiltinVariable.SPEC_VAR_NAMES) {
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
        assertOutput(
                "${.incompatibleImprovements}",
                getConfiguration().getIncompatibleImprovements().toString());
        
        getConfiguration().setIncompatibleImprovements(new Version(2, 3, 23));
        assertOutput(
                "${.incompatible_improvements}",
                getConfiguration().getIncompatibleImprovements().toString());
    }

    @Test
    public void testAutoEsc() throws Exception {
        Configuration cfg = getConfiguration();
        
        for (int autoEscaping : new int[] {
                Configuration.ENABLE_AUTO_ESCAPING_IF_DEFAULT, Configuration.ENABLE_AUTO_ESCAPING_IF_SUPPORTED }) {
            cfg.setAutoEscaping(autoEscaping);
            cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
            assertOutput("${.autoEsc?c}", "true");
            assertOutput("<#ftl autoEsc=false>${.autoEsc?c}", "false");
            cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
            assertOutput("${.autoEsc?c}", "false");
            cfg.setOutputFormat(UndefinedOutputFormat.INSTANCE);
            assertOutput("${.autoEsc?c}", "false");
        }
        
        cfg.setAutoEscaping(Configuration.DISABLE_AUTO_ESCAPING);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        assertOutput("${.autoEsc?c}", "false");
        assertOutput("<#ftl autoEsc=true>${.autoEsc?c}", "true");
        cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
        assertOutput("${.autoEsc?c}", "false");
        cfg.setOutputFormat(UndefinedOutputFormat.INSTANCE);
        assertOutput("${.autoEsc?c}", "false");

        cfg.setAutoEscaping(Configuration.ENABLE_AUTO_ESCAPING_IF_DEFAULT);
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

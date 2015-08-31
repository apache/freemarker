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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.test.TemplateTest;

@SuppressWarnings("boxing")
public class NumberFormatTest extends TemplateTest {
    
    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setLocale(Locale.US);
        
        cfg.setCustomNumberFormats(ImmutableMap.of(
                "hex", HexTemplateNumberFormatFactory.INSTANCE,
                "loc", LocaleSensitiveTemplateNumberFormatFactory.INSTANCE,
                "base", BaseNTemplateNumberFormatFactory.INSTANCE));
    }

    @Test
    public void testUnknownNumberFormat() throws Exception {
        {
            getConfiguration().setNumberFormat("@noSuchFormat");
            Throwable exc = assertErrorContains("${1}", "\"@noSuchFormat\"", "\"noSuchFormat\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }

        {
            getConfiguration().setNumberFormat("number");
            Throwable exc = assertErrorContains("${1?string('@noSuchFormat2')}",
                    "\"@noSuchFormat2\"", "\"noSuchFormat2\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }
    }
    
    @Test
    public void testStringBI() throws Exception {
        assertOutput("${11} ${11?string.@hex} ${12} ${12?string.@hex}", "11 b 12 c");
    }

    @Test
    public void testSetting() throws Exception {
        getConfiguration().setNumberFormat("@hex");
        assertOutput("${11?string.number} ${11} ${12?string.number} ${12}", "11 b 12 c");
    }

    @Test
    public void testSetting2() throws Exception {
        assertOutput(
                "<#setting numberFormat='@hex'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}"
                + "<#setting numberFormat='@loc'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}",
                "11 b 12 c d"
                + "11 11_en_US 12 12_en_US 13_en_US");
    }
    
    @Test
    public void testUnformattableNumber() throws Exception {
        getConfiguration().setNumberFormat("@hex");
        assertErrorContains("${1.1}", "hexadecimal int", "doesn't fit into an int");
    }

    @Test
    public void testLocaleSensitive() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@loc");
        assertOutput("${1.1}", "1.1_en_US");
        cfg.setLocale(Locale.GERMANY);
        assertOutput("${1.1}", "1.1_de_DE");
    }

    @Test
    public void testLocaleSensitive2() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@loc");
        assertOutput("${1.1} <#setting locale='de_DE'>${1.1}", "1.1_en_US 1.1_de_DE");
    }

    @Test
    public void testParameterized() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@base 2");
        assertOutput("${11}", "1011");
        assertOutput("${11?string}", "1011");
        assertOutput("${11?string.@base_3}", "102");
        
        assertErrorContains("${11?string.@base_xyz}", "\"@base_xyz\"", "\"xyz\"");
        cfg.setNumberFormat("@base");
        assertErrorContains("${11}", "\"@base\"", "format parameter is required");
    }
    
    /**
     * ?string formats lazily (at least in 2.3.x), so it must make a snapshot of the format inputs when it's called.
     */
    @Test
    public void testStringBIDoesSnapshot() throws Exception {
        // TemplateNumberModel-s shouldn't change, but we have to keep BC when that still happens.
        final MutableTemplateNumberModel nm = new MutableTemplateNumberModel();
        nm.setNumber(123);
        addToDataModel("n", nm);
        addToDataModel("incN", new TemplateDirectiveModel() {
            
            public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
                    throws TemplateException, IOException {
                nm.setNumber(nm.getAsNumber().intValue() + 1);
            }
        });
        assertOutput(
                "<#assign s1 = n?string>"
                + "<#setting numberFormat='@loc'>"
                + "<#assign s2 = n?string>"
                + "<#setting numberFormat='@hex'>"
                + "<#assign s3 = n?string>"
                + "${s1} ${s2} ${s3}",
                "123 123_en_US 7b");
        assertOutput(
                "<#assign s1 = n?string>"
                + "<@incN />"
                + "<#assign s2 = n?string>"
                + "${s1} ${s2}",
                "123 124");
    }

    @Test
    public void testNullInNumberModel() throws Exception {
        addToDataModel("n", new MutableTemplateNumberModel());
        assertErrorContains("${n}", "nothing inside it");
        assertErrorContains("${n?string}", "nothing inside it");
    }
    
    private static class MutableTemplateNumberModel implements TemplateNumberModel {
        
        private Number number;

        public void setNumber(Number number) {
            this.number = number;
        }

        public Number getAsNumber() throws TemplateModelException {
            return number;
        }
        
    }
    
}

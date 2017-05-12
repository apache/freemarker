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
package org.apache.freemarker.core.valueformat;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateDirectiveBody;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.userpkg.BaseNTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.HexTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.LocaleSensitiveTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.PrintfGTemplateNumberFormatFactory;
import org.apache.freemarker.core.valueformat.impl.AliasTemplateNumberFormatFactory;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("boxing")
public class NumberFormatTest extends TemplateTest {
    
    @Test
    public void testUnknownCustomFormat() throws Exception {
        {
            setConfigurationWithNumberFormat("@noSuchFormat");
            Throwable exc = assertErrorContains("${1}", "\"@noSuchFormat\"", "\"noSuchFormat\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }

        {
            setConfigurationWithNumberFormat("number");
            Throwable exc = assertErrorContains("${1?string('@noSuchFormat2')}",
                    "\"@noSuchFormat2\"", "\"noSuchFormat2\"");
            assertThat(exc.getCause(), instanceOf(UndefinedCustomFormatException.class));
        }
    }
    
    @Test
    public void testStringBI() throws Exception {
        setConfigurationWithNumberFormat(null);
        assertOutput("${11} ${11?string.@hex} ${12} ${12?string.@hex}", "11 b 12 c");
    }

    @Test
    public void testSetting() throws Exception {
        setConfigurationWithNumberFormat("@hex");
        assertOutput("${11?string.number} ${11} ${12?string.number} ${12}", "11 b 12 c");
    }

    @Test
    public void testSetting2() throws Exception {
        setConfigurationWithNumberFormat(null);
        assertOutput(
                "<#setting numberFormat='@hex'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}"
                + "<#setting numberFormat='@loc'>${11?string.number} ${11} ${12?string.number} ${12} ${13?string}",
                "11 b 12 c d"
                + "11 11_en_US 12 12_en_US 13_en_US");
    }
    
    @Test
    public void testUnformattableNumber() throws Exception {
        setConfigurationWithNumberFormat("@hex");
        assertErrorContains("${1.1}", "hexadecimal int", "doesn't fit into an int");
    }

    @Test
    public void testLocaleSensitive() throws Exception {
        setConfigurationWithNumberFormat("@loc");
        assertOutput("${1.1}", "1.1_en_US");
        setConfigurationWithNumberFormat("@loc", null, null, Locale.GERMANY);
        assertOutput("${1.1}", "1.1_de_DE");
    }

    @Test
    public void testLocaleSensitive2() throws Exception {
        setConfigurationWithNumberFormat("@loc");
        assertOutput("${1.1} <#setting locale='de_DE'>${1.1}", "1.1_en_US 1.1_de_DE");
    }

    @Test
    public void testCustomParameterized() throws Exception {
        setConfigurationWithNumberFormat("@base 2");
        assertOutput("${11}", "1011");
        assertOutput("${11?string}", "1011");
        assertOutput("${11?string.@base_3}", "102");
        
        assertErrorContains("${11?string.@base_xyz}", "\"@base_xyz\"", "\"xyz\"");
        setConfigurationWithNumberFormat("@base");
        assertErrorContains("${11}", "\"@base\"", "format parameter is required");
    }

    @Test
    public void testCustomWithFallback() throws Exception {
        Configuration cfg = getConfiguration();
        setConfigurationWithNumberFormat("@base 2|0.0#");
        assertOutput("${11}", "1011");
        assertOutput("${11.34}", "11.34");
        assertOutput("${11?string('@base 3|0.00')}", "102");
        assertOutput("${11.2?string('@base 3|0.00')}", "11.20");
    }

    @Test
    public void testEnvironmentGetters() throws Exception {
        setConfigurationWithNumberFormat(null);

        Template t = new Template(null, "", getConfiguration());
        Environment env = t.createProcessingEnvironment(null, null);
        
        TemplateNumberFormat defF = env.getTemplateNumberFormat();
        //
        TemplateNumberFormat explF = env.getTemplateNumberFormat("0.00");
        assertEquals("1.25", explF.formatToPlainText(new SimpleNumber(1.25)));
        //
        TemplateNumberFormat expl2F = env.getTemplateNumberFormat("@loc");
        assertEquals("1.25_en_US", expl2F.formatToPlainText(new SimpleNumber(1.25)));
        
        TemplateNumberFormat explFFr = env.getTemplateNumberFormat("0.00", Locale.FRANCE);
        assertNotSame(explF, explFFr);
        assertEquals("1,25", explFFr.formatToPlainText(new SimpleNumber(1.25)));
        //
        TemplateNumberFormat expl2FFr = env.getTemplateNumberFormat("@loc", Locale.FRANCE);
        assertEquals("1.25_fr_FR", expl2FFr.formatToPlainText(new SimpleNumber(1.25)));
        
        assertSame(env.getTemplateNumberFormat(), defF);
        //
        assertSame(env.getTemplateNumberFormat("0.00"), explF);
        //
        assertSame(env.getTemplateNumberFormat("@loc"), expl2F);
    }
    
    /**
     * ?string formats lazily (at least in 2.3.x), so it must make a snapshot of the format inputs when it's called.
     */
    @Test
    @Ignore // [FM3] We want to rework BI-s so that lazy evaluation won't be needed. Then this will go away too.
    public void testStringBIDoesSnapshot() throws Exception {
        // TemplateNumberModel-s shouldn't change, but we have to keep BC when that still happens.
        final MutableTemplateNumberModel nm = new MutableTemplateNumberModel();
        nm.setNumber(123);
        addToDataModel("n", nm);
        addToDataModel("incN", new TemplateDirectiveModel() {
            
            @Override
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
    public void testNullInModel() throws Exception {
        addToDataModel("n", new MutableTemplateNumberModel());
        assertErrorContains("${n}", "nothing inside it");
        assertErrorContains("${n?string}", "nothing inside it");
    }
    
    @Test
    public void testAtPrefix() throws Exception {
        Configuration cfg = getConfiguration();

        setConfigurationWithNumberFormat("@hex");
        assertOutput("${10}", "a");
        setConfigurationWithNumberFormat("'@'0");
        assertOutput("${10}", "@10");
        setConfigurationWithNumberFormat("@@0");
        assertOutput("${10}", "@@10");

        setConfigurationWithNumberFormat(
                "@hex", Collections.<String, TemplateNumberFormatFactory>emptyMap());
        assertErrorContains("${10}", "custom", "\"hex\"");

        setConfigurationWithNumberFormat(
                "'@'0", Collections.<String, TemplateNumberFormatFactory>emptyMap());
        assertOutput("${10}", "@10");

        setConfigurationWithNumberFormat(
                "@@0", Collections.<String, TemplateNumberFormatFactory>emptyMap());
        assertOutput("${10}", "@@10");
    }

    @Test
    public void testAlieses() throws Exception {
        setConfigurationWithNumberFormat(
                "'@'0",
                ImmutableMap.of(
                        "f", new AliasTemplateNumberFormatFactory("0.#'f'"),
                        "d", new AliasTemplateNumberFormatFactory("0.0#"),
                        "hex", HexTemplateNumberFormatFactory.INSTANCE),
                new ConditionalTemplateConfigurationFactory(
                        new FileNameGlobMatcher("*2*"),
                        new TemplateConfiguration.Builder()
                                .customNumberFormats(ImmutableMap.<String, TemplateNumberFormatFactory>of(
                                        "d", new AliasTemplateNumberFormatFactory("0.#'d'"),
                                        "i", new AliasTemplateNumberFormatFactory("@hex")))
                                .build()));

        String commonFtl = "${1?string.@f} ${1?string.@d} "
                + "<#setting locale='fr_FR'>${1.5?string.@d} "
                + "<#attempt>${10?string.@i}<#recover>E</#attempt>";
        addTemplate("t1.ftl", commonFtl);
        addTemplate("t2.ftl", commonFtl);
        
        assertOutputForNamed("t1.ftl", "1f 1.0 1,5 E");
        assertOutputForNamed("t2.ftl", "1f 1d 1,5d a");
    }

    @Test
    public void testAlieses2() throws Exception {
        setConfigurationWithNumberFormat(
                "@n",
                ImmutableMap.<String, TemplateNumberFormatFactory>of(
                        "n", new AliasTemplateNumberFormatFactory("0.0",
                                ImmutableMap.of(
                                        new Locale("en"), "0.0'_en'",
                                        Locale.UK, "0.0'_en_GB'",
                                        Locale.FRANCE, "0.0'_fr_FR'"))));
        assertOutput(
                "<#setting locale='en_US'>${1} "
                + "<#setting locale='en_GB'>${1} "
                + "<#setting locale='en_GB_Win'>${1} "
                + "<#setting locale='fr_FR'>${1} "
                + "<#setting locale='hu_HU'>${1}",
                "1.0_en 1.0_en_GB 1.0_en_GB 1,0_fr_FR 1,0");
    }
    
    @Test
    public void testMarkupFormat() throws IOException, TemplateException {
        setConfigurationWithNumberFormat("@printfG_3");

        String commonFTL = "${1234567} ${'cat:' + 1234567} ${0.0000123}";
        String commonOutput = "1.23*10<sup>6</sup> cat:1.23*10<sup>6</sup> 1.23*10<sup>-5</sup>";
        assertOutput(commonFTL, commonOutput);
        assertOutput("<#ftl outputFormat='HTML'>" + commonFTL, commonOutput);
        assertOutput("<#escape x as x?html>" + commonFTL + "</#escape>", commonOutput);
        assertOutput("<#escape x as x?xhtml>" + commonFTL + "</#escape>", commonOutput);
        assertOutput("<#escape x as x?xml>" + commonFTL + "</#escape>", commonOutput);
        assertOutput("${\"" + commonFTL + "\"}", "1.23*10<sup>6</sup> cat:1.23*10<sup>6</sup> 1.23*10<sup>-5</sup>");
        assertErrorContains("<#ftl outputFormat='plainText'>" + commonFTL, "HTML", "plainText", "conversion");
    }

    @Test
    public void testPrintG() throws IOException, TemplateException {
        setConfigurationWithNumberFormat(null);
        for (Number n : new Number[] {
                1234567, 1234567L, 1234567d, 1234567f, BigInteger.valueOf(1234567), BigDecimal.valueOf(1234567) }) {
            addToDataModel("n", n);
            
            assertOutput("${n?string.@printfG}", "1.23457E+06");
            assertOutput("${n?string.@printfG_3}", "1.23E+06");
            assertOutput("${n?string.@printfG_7}", "1234567");
            assertOutput("${0.0000123?string.@printfG}", "1.23000E-05");
        }
    }

    private void setConfigurationWithNumberFormat(
            String numberFormat,
            Map<String, TemplateNumberFormatFactory> customNumberFormats,
            TemplateConfigurationFactory templateConfigurationFactory,
            Locale locale) {
        TestConfigurationBuilder cfgB = new TestConfigurationBuilder(Configuration.VERSION_3_0_0);

        if (numberFormat != null) {
            cfgB.setNumberFormat(numberFormat);
        }
        cfgB.setCustomNumberFormats(
                customNumberFormats != null ? customNumberFormats
                        : ImmutableMap.of(
                                "hex", HexTemplateNumberFormatFactory.INSTANCE,
                                "loc", LocaleSensitiveTemplateNumberFormatFactory.INSTANCE,
                                "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                                "printfG", PrintfGTemplateNumberFormatFactory.INSTANCE));
        if (locale != null) {
            cfgB.setLocale(locale);
        }
        if (templateConfigurationFactory != null) {
            cfgB.setTemplateConfigurations(templateConfigurationFactory);
        }

        setConfiguration(cfgB.build());
    }

    private void setConfigurationWithNumberFormat(String numberFormat) {
        setConfigurationWithNumberFormat(numberFormat, null, null, null);
    }

    private void setConfigurationWithNumberFormat(
            String numberFormat, Map<String, TemplateNumberFormatFactory> customNumberFormats) {
        setConfigurationWithNumberFormat(numberFormat, customNumberFormats, null, null);
    }

    private void setConfigurationWithNumberFormat(
            String numberFormat, Map<String, TemplateNumberFormatFactory> customNumberFormats,
            TemplateConfigurationFactory templateConfigurationFactory) {
        setConfigurationWithNumberFormat(numberFormat, customNumberFormats, templateConfigurationFactory, null);
    }

    private static class MutableTemplateNumberModel implements TemplateNumberModel {
        
        private Number number;

        public void setNumber(Number number) {
            this.number = number;
        }

        @Override
        public Number getAsNumber() throws TemplateModelException {
            return number;
        }
        
    }
    
}

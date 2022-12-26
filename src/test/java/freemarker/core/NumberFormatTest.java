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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.template.Configuration;
import freemarker.template.SimpleNumber;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.Version;
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
                "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                "printfG", PrintfGTemplateNumberFormatFactory.INSTANCE));
    }

    @Test
    public void testUnknownCustomFormat() throws Exception {
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
    public void testCustomParameterized() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@base 2");
        assertOutput("${11}", "1011");
        assertOutput("${11?string}", "1011");
        assertOutput("${11?string.@base_3}", "102");
        
        assertErrorContains("${11?string.@base_xyz}", "\"@base_xyz\"", "\"xyz\"");
        cfg.setNumberFormat("@base");
        assertErrorContains("${11}", "\"@base\"", "format parameter is required");
    }

    @Test
    public void testCustomWithFallback() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setNumberFormat("@base 2|0.0#");
        assertOutput("${11}", "1011");
        assertOutput("${11.34}", "11.34");
        assertOutput("${11?string('@base 3|0.00')}", "102");
        assertOutput("${11.2?string('@base 3|0.00')}", "11.20");
    }

    @Test
    public void testEnvironmentGetters() throws Exception {
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
    public void testNullInModel() throws Exception {
        addToDataModel("n", new MutableTemplateNumberModel());
        assertErrorContains("${n}", "nothing inside it");
        assertErrorContains("${n?string}", "nothing inside it");
    }
    
    @Test
    public void testIcIAndEscaping() throws Exception {
        Configuration cfg = getConfiguration();
        testIcIAndEscapingWhenCustFormsAccepted(cfg);
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        testIcIAndEscapingWhenCustFormsAccepted(cfg);
        
        cfg.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>emptyMap());
        cfg.setNumberFormat("@hex");
        assertOutput("${10}", "@hex10");
        cfg.setNumberFormat("'@'0");
        assertOutput("${10}", "@10");
        cfg.setNumberFormat("@@0");
        assertOutput("${10}", "@@10");
        
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        cfg.setNumberFormat("@hex");
        assertErrorContains("${10}", "custom", "\"hex\"");
        cfg.setNumberFormat("'@'0");
        assertOutput("${10}", "@10");
        cfg.setNumberFormat("@@0");
        assertOutput("${10}", "@@10");
    }

    protected void testIcIAndEscapingWhenCustFormsAccepted(Configuration cfg) throws IOException, TemplateException {
        cfg.setNumberFormat("@hex");
        assertOutput("${10}", "a");
        cfg.setNumberFormat("'@'0");
        assertOutput("${10}", "@10");
        cfg.setNumberFormat("@@0");
        assertOutput("${10}", "@@10");
    }

    @Test
    public void testAlieses() throws Exception {
        Configuration cfg = getConfiguration();
        cfg.setCustomNumberFormats(ImmutableMap.of(
                "f", new AliasTemplateNumberFormatFactory("0.#'f'"),
                "d", new AliasTemplateNumberFormatFactory("0.0#"),
                "hex", HexTemplateNumberFormatFactory.INSTANCE));
        
        TemplateConfiguration tc = new TemplateConfiguration();
        tc.setCustomNumberFormats(ImmutableMap.of(
                "d", new AliasTemplateNumberFormatFactory("0.#'d'"),
                "i", new AliasTemplateNumberFormatFactory("@hex")));
        cfg.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*2*"), tc));
        
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
        Configuration cfg = getConfiguration();
        cfg.setCustomNumberFormats(ImmutableMap.of(
                "n", new AliasTemplateNumberFormatFactory("0.0",
                        ImmutableMap.of(
                                new Locale("en"), "0.0'_en'",
                                Locale.UK, "0.0'_en_GB'",
                                Locale.FRANCE, "0.0'_fr_FR'"))));
        cfg.setNumberFormat("@n");
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
        getConfiguration().setNumberFormat("@printfG_3");

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
        for (Number n : new Number[] {
                1234567, 1234567L, 1234567d, 1234567f, BigInteger.valueOf(1234567), BigDecimal.valueOf(1234567) }) {
            addToDataModel("n", n);
            
            assertOutput("${n?string.@printfG}", "1.23457E+06");
            assertOutput("${n?string.@printfG_3}", "1.23E+06");
            assertOutput("${n?string.@printfG_7}", "1234567");
            assertOutput("${0.0000123?string.@printfG}", "1.23000E-05");
        }
    }

    @Test
    public void testCFormatOfSpecialNumbers() throws IOException, TemplateException {
        addToDataModel("pInf", Double.POSITIVE_INFINITY);
        addToDataModel("nInf", Double.NEGATIVE_INFINITY);
        addToDataModel("nan", Double.NaN);

        Configuration cfg = getConfiguration();
        for (Version ici : new Version[] {
                Configuration.VERSION_2_3_20,
                Configuration.VERSION_2_3_21, Configuration.VERSION_2_3_30,
                Configuration.VERSION_2_3_31,
                Configuration.VERSION_2_3_32 } ) {
            cfg.setIncompatibleImprovements(ici);

            boolean cBuiltInBroken = ici.intValue() < Configuration.VERSION_2_3_21.intValue();
            boolean cNumberFormatBroken = ici.intValue() < Configuration.VERSION_2_3_31.intValue();

            String humanAudienceOutput = "\u221e -\u221e \ufffd";
            String computerAudienceOutput = ici.intValue() < Configuration.VERSION_2_3_32.intValue()
                    ? "INF -INF NaN" : "Infinity -Infinity NaN";

            assertOutput(
                    "${pInf?c} ${nInf?c} ${nan?c}",
                    cBuiltInBroken ? humanAudienceOutput : computerAudienceOutput);

            assertOutput(
                    "<#setting numberFormat='computer'>${pInf} ${nInf} ${nan}",
                    cNumberFormatBroken ? humanAudienceOutput : computerAudienceOutput);

            assertOutput(
                    "${pInf} ${nInf} ${nan}",
                    humanAudienceOutput);

            Environment env = new Template(null, "", cfg)
                    .createProcessingEnvironment(null, null);
            assertEquals(
                    cNumberFormatBroken ? humanAudienceOutput : computerAudienceOutput,
                    env.getCNumberFormat().format(Double.POSITIVE_INFINITY)
                            + " " + env.getCNumberFormat().format(Double.NEGATIVE_INFINITY)
                            + " " + env.getCNumberFormat().format(Double.NaN));
        }
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

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

import static org.apache.freemarker.core.ProcessingConfiguration.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.FirstMatchTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.MergingTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

public class TemplateConfigurationWithDefaultTemplateResolverTest {

    private static final String TEXT_WITH_ACCENTS = "pr\u00F3ba";

    private static final Serializable CUST_SETTING_1 = Integer.valueOf(111);
    private static final Serializable CUST_SETTING_2 = Integer.valueOf(222);

    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

    @Test
    public void testEncoding() throws Exception {
        Configuration cfg = createCommonEncodingTesterConfig();
        
        {
            Template t = cfg.getTemplate("utf8.f3ah");
            assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf16.f3ah");
            assertEquals(StandardCharsets.UTF_16LE, t.getActualSourceEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.f3ah");
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf8-latin2.f3ah");
            assertEquals(ISO_8859_2, t.getActualSourceEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default-latin2.f3ah");
            assertEquals(ISO_8859_2, t.getActualSourceEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
    }
    
    @Test
    public void testIncludeAndEncoding() throws Exception {
        Configuration cfg = createCommonEncodingTesterConfig();
        ByteArrayTemplateLoader tl = (ByteArrayTemplateLoader) cfg.getTemplateLoader();
        tl.putTemplate("main.f3ah", (
                        "<#include 'utf8.f3ah'>"
                        + "<#include 'utf16.f3ah'>"
                        + "<#include 'default.f3ah'>"
                        + "<#include 'utf8-latin2.f3ah'>"
                ).getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(
                TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS,
                getTemplateOutput(cfg.getTemplate("main.f3ah")));
    }

    @Test
    public void testLocale() throws Exception {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("(de).f3ah", "${.locale}");
        loader.putTemplate("default.f3ah", "${.locale}");
        loader.putTemplate("(de)-fr.f3ah",
                ("<#ftl locale='fr_FR'>${.locale}"));
        loader.putTemplate("default-fr.f3ah",
                ("<#ftl locale='fr_FR'>${.locale}"));

        Configuration cfg = new TestConfigurationBuilder()
                .templateLoader(loader)
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*(de)*"),
                                new TemplateConfiguration.Builder()
                                        .locale(Locale.GERMANY)
                                        .build()))
                .build();

        {
            Template t = cfg.getTemplate("(de).f3ah");
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("(de).f3ah", Locale.ITALY);
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.f3ah");
            assertEquals(Locale.US, t.getLocale());
            assertEquals("en_US", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.f3ah", Locale.ITALY);
            assertEquals(Locale.ITALY, t.getLocale());
            assertEquals("it_IT", getTemplateOutput(t));
        }
    }

    @Test
    public void testConfigurableSettings() throws Exception {
        String commonFTL = "${.locale} ${true?string} ${1.2}";
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("default", commonFTL);
        loader.putTemplate("(fr)", commonFTL);
        loader.putTemplate("(yn)(00)", commonFTL);
        loader.putTemplate("(00)(fr)", commonFTL);

        Configuration cfg = new TestConfigurationBuilder()
                .templateConfigurations(
                        new MergingTemplateConfigurationFactory(
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*(fr)*"),
                                        new TemplateConfiguration.Builder().locale(Locale.FRANCE).build()),
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*(yn)*"),
                                        new TemplateConfiguration.Builder().booleanFormat("Y,N").build()),
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*(00)*"),
                                        new TemplateConfiguration.Builder().numberFormat("0.00").build())))
                .templateLoader(loader)
                .build();

        assertEquals("en_US true 1.2", getTemplateOutput(cfg.getTemplate("default")));
        assertEquals("fr_FR true 1,2", getTemplateOutput(cfg.getTemplate("(fr)")));
        assertEquals("en_US Y 1.20", getTemplateOutput(cfg.getTemplate("(yn)(00)")));
        assertEquals("fr_FR true 1,20", getTemplateOutput(cfg.getTemplate("(00)(fr)")));
    }
    
    @Test
    public void testCustomSettings() throws Exception {
        String commonFTL = "<#ftl customSettings={ 'a3': 'a3temp' }>";
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("(tc1)", commonFTL);
        tl.putTemplate("(tc1)noHeader", "");
        tl.putTemplate("(tc2)", commonFTL);
        tl.putTemplate("(tc1)(tc2)", commonFTL);

        Configuration cfg = new TestConfigurationBuilder()
                .templateConfigurations(
                        new MergingTemplateConfigurationFactory(
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*(tc1)*"),
                                        new TemplateConfiguration.Builder()
                                                .customSetting("a1", "a1tc1")
                                                .customSetting("a2", "a2tc1")
                                                .customSetting("a3", "a3tc1")
                                                .customSetting(CUST_SETTING_1, "ca1tc1")
                                                .customSetting(CUST_SETTING_2, "ca2tc1")
                                                .build()),
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*(tc2)*"),
                                        new TemplateConfiguration.Builder()
                                                .customSetting("a1", "a1tc2")
                                                .customSetting(CUST_SETTING_1, "ca1tc2")
                                                .build())))
                .templateLoader(tl)
                .build();

        {
            Template t = cfg.getTemplate("(tc1)");
            assertEquals("a1tc1", t.getCustomSetting("a1"));
            assertEquals("a2tc1", t.getCustomSetting("a2"));
            assertEquals("a3temp", t.getCustomSetting("a3"));
            assertEquals("ca1tc1", t.getCustomSetting(CUST_SETTING_1));
            assertEquals("ca2tc1", t.getCustomSetting(CUST_SETTING_2));
        }
        {
            Template t = cfg.getTemplate("(tc1)noHeader");
            assertEquals("a1tc1", t.getCustomSetting("a1"));
            assertEquals("a2tc1", t.getCustomSetting("a2"));
            assertEquals("a3tc1", t.getCustomSetting("a3"));
            assertEquals("ca1tc1", t.getCustomSetting(CUST_SETTING_1));
            assertEquals("ca2tc1", t.getCustomSetting(CUST_SETTING_2));
        }
        {
            Template t = cfg.getTemplate("(tc2)");
            assertEquals("a1tc2", t.getCustomSetting("a1"));
            assertEquals(MISSING_VALUE_MARKER, t.getCustomSetting("a2", MISSING_VALUE_MARKER));
            assertEquals("a3temp", t.getCustomSetting("a3"));
            assertEquals("ca1tc2", t.getCustomSetting(CUST_SETTING_1));
            assertEquals(MISSING_VALUE_MARKER, t.getCustomSetting(CUST_SETTING_2, MISSING_VALUE_MARKER));
        }
        {
            Template t = cfg.getTemplate("(tc1)(tc2)");
            assertEquals("a1tc2", t.getCustomSetting("a1"));
            assertEquals("a2tc1", t.getCustomSetting("a2"));
            assertEquals("a3temp", t.getCustomSetting("a3"));
            assertEquals("ca1tc2", t.getCustomSetting(CUST_SETTING_1));
            assertEquals("ca2tc1", t.getCustomSetting(CUST_SETTING_2));
        }
    }
    
    private String getTemplateOutput(Template t) throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        return sw.toString();
    }

    private Configuration createCommonEncodingTesterConfig() throws UnsupportedEncodingException {
        ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
        tl.putTemplate("utf8.f3ah", TEXT_WITH_ACCENTS.getBytes(StandardCharsets.UTF_8));
        tl.putTemplate("utf16.f3ah", TEXT_WITH_ACCENTS.getBytes(StandardCharsets.UTF_16LE));
        tl.putTemplate("default.f3ah", TEXT_WITH_ACCENTS.getBytes(ISO_8859_2));
        tl.putTemplate("utf8-latin2.f3ah",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes(ISO_8859_2));
        tl.putTemplate("default-latin2.f3ah",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes(ISO_8859_2));

        return new TestConfigurationBuilder()
                .sourceEncoding(StandardCharsets.ISO_8859_1)
                .locale(Locale.US)
                .templateLoader(tl)
                .templateConfigurations(
                        new FirstMatchTemplateConfigurationFactory(
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*utf8*"),
                                        new TemplateConfiguration.Builder()
                                                .sourceEncoding(StandardCharsets.UTF_8)
                                                .build()),
                                new ConditionalTemplateConfigurationFactory(
                                        new FileNameGlobMatcher("*utf16*"),
                                        new TemplateConfiguration.Builder()
                                                .sourceEncoding(StandardCharsets.UTF_16LE)
                                                .build())
                        )
                        .allowNoMatch(true))
                .build();
    }

}

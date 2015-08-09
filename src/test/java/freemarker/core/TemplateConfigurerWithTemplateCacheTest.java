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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.junit.Test;

import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.cache.ConditionalTemplateConfigurerFactory;
import freemarker.cache.FileNameGlobMatcher;
import freemarker.cache.FirstMatchTemplateConfigurerFactory;
import freemarker.cache.MergingTemplateConfigurerFactory;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateConfigurerWithTemplateCacheTest {

    private static final String TEXT_WITH_ACCENTS = "pr\u00F3ba";

    private static final CustomAttribute CUST_ATT_1 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE);
    private static final CustomAttribute CUST_ATT_2 = new CustomAttribute(CustomAttribute.SCOPE_TEMPLATE);

    @Test
    public void testEncoding() throws Exception {
        Configuration cfg = createCommonEncodingTesterConfig();
        
        {
            Template t = cfg.getTemplate("utf8.ftl");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf8.ftl", "iso-8859-1");
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf16.ftl");
            assertEquals("utf-16", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl");
            assertEquals("iso-8859-1", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl", "iso-8859-5");
            assertEquals("iso-8859-5", t.getEncoding());
            assertEquals(new String(TEXT_WITH_ACCENTS.getBytes("iso-8859-1"), "iso-8859-5"),
                    getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf8-latin2.ftl");
            assertEquals("iso-8859-2", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default-latin2.ftl");
            assertEquals("iso-8859-2", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
    }
    
    @Test
    public void testIncludeAndEncoding() throws Exception {
        Configuration cfg = createCommonEncodingTesterConfig();
        ByteArrayTemplateLoader tl = (ByteArrayTemplateLoader) cfg.getTemplateLoader();
        tl.putTemplate("main.ftl", (
                        "<#include 'utf8.ftl'>"
                        + "<#include 'utf16.ftl'>"
                        + "<#include 'default.ftl'>"
                        + "<#include 'utf8-latin2.ftl'>"
                        // With mostly ignored encoding params:
                        + "<#include 'utf8.ftl' encoding='utf-16'>"
                        + "<#include 'utf16.ftl' encoding='iso-8859-5'>"
                        + "<#include 'default.ftl' encoding='iso-8859-5'>"
                        + "<#include 'utf8-latin2.ftl' encoding='iso-8859-5'>"
                ).getBytes("iso-8859-1"));
        assertEquals(
                TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS
                + TEXT_WITH_ACCENTS + TEXT_WITH_ACCENTS
                + new String(TEXT_WITH_ACCENTS.getBytes("iso-8859-1"), "iso-8859-5")
                + TEXT_WITH_ACCENTS,
                getTemplateOutput(cfg.getTemplate("main.ftl")));
    }

    @Test
    public void testLocale() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setLocale(Locale.US);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("(de).ftl", "${.locale}");
        tl.putTemplate("default.ftl", "${.locale}");
        tl.putTemplate("(de)-fr.ftl",
                ("<#ftl locale='fr_FR'>${.locale}"));
        tl.putTemplate("default-fr.ftl",
                ("<#ftl locale='fr_FR'>${.locale}"));
        cfg.setTemplateLoader(tl);

        TemplateConfigurer tcDe = new TemplateConfigurer();
        tcDe.setLocale(Locale.GERMANY);
        cfg.setTemplateConfigurers(new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(de)*"), tcDe));
        
        {
            Template t = cfg.getTemplate("(de).ftl");
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("(de).ftl", Locale.ITALY);
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("de_DE", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl");
            assertEquals(Locale.US, t.getLocale());
            assertEquals("en_US", getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("default.ftl", Locale.ITALY);
            assertEquals(Locale.ITALY, t.getLocale());
            assertEquals("it_IT", getTemplateOutput(t));
        }
    }

    @Test
    public void testPlainText() throws Exception {
        Configuration cfg = createCommonEncodingTesterConfig();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_22);
        
        TemplateConfigurer tcDE = new TemplateConfigurer();
        tcDE.setLocale(Locale.GERMANY);
        TemplateConfigurer tcYN = new TemplateConfigurer();
        tcYN.setBooleanFormat("Y,N");
        cfg.setTemplateConfigurers(
                    new MergingTemplateConfigurerFactory(
                            cfg.getTemplateConfigurers(),
                            new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("utf16.ftl"), tcDE),
                            new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("utf16.ftl"), tcYN)
                    )
                );
        
        {
            Template t = cfg.getTemplate("utf8.ftl", null, null, false);
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
            assertEquals(Locale.US, t.getLocale());
            assertEquals("true,false", t.getBooleanFormat());
        }
        {
            Template t = cfg.getTemplate("utf8.ftl", null, "iso-8859-1", false);
            assertEquals("utf-8", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
        {
            Template t = cfg.getTemplate("utf16.ftl", null, null, false);
            assertEquals("utf-16", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
            assertEquals(Locale.GERMANY, t.getLocale());
            assertEquals("Y,N", t.getBooleanFormat());
        }
        {
            Template t = cfg.getTemplate("default.ftl", null, null, false);
            assertEquals("iso-8859-1", t.getEncoding());
            assertEquals(TEXT_WITH_ACCENTS, getTemplateOutput(t));
        }
    }

    @Test
    public void testConfigurableSettings() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setLocale(Locale.US);
        
        TemplateConfigurer tcFR = new TemplateConfigurer();
        tcFR.setLocale(Locale.FRANCE);
        TemplateConfigurer tcYN = new TemplateConfigurer();
        tcYN.setBooleanFormat("Y,N");
        TemplateConfigurer tc00 = new TemplateConfigurer();
        tc00.setNumberFormat("0.00");
        cfg.setTemplateConfigurers(
                new MergingTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(fr)*"), tcFR),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(yn)*"), tcYN),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(00)*"), tc00)
                )
        );
        
        String commonFTL = "${.locale} ${true?string} ${1.2}";
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("default", commonFTL);
        tl.putTemplate("(fr)", commonFTL);
        tl.putTemplate("(yn)(00)", commonFTL);
        tl.putTemplate("(00)(fr)", commonFTL);
        cfg.setTemplateLoader(tl);
        
        assertEquals("en_US true 1.2", getTemplateOutput(cfg.getTemplate("default")));
        assertEquals("fr_FR true 1,2", getTemplateOutput(cfg.getTemplate("(fr)")));
        assertEquals("en_US Y 1.20", getTemplateOutput(cfg.getTemplate("(yn)(00)")));
        assertEquals("fr_FR true 1,20", getTemplateOutput(cfg.getTemplate("(00)(fr)")));
    }
    
    @Test
    public void testCustomAttributes() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        TemplateConfigurer tc1 = new TemplateConfigurer();
        tc1.setCustomAttribute("a1", "a1tc1");
        tc1.setCustomAttribute("a2", "a2tc1");
        tc1.setCustomAttribute("a3", "a3tc1");
        CUST_ATT_1.set("ca1tc1", tc1);
        CUST_ATT_2.set("ca2tc1", tc1);
        
        TemplateConfigurer tc2 = new TemplateConfigurer();
        tc2.setCustomAttribute("a1", "a1tc2");
        CUST_ATT_1.set("ca1tc2", tc2);
        
        cfg.setTemplateConfigurers(
                new MergingTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(tc1)*"), tc1),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*(tc2)*"), tc2)
                )
        );
        
        String commonFTL = "<#ftl attributes={ 'a3': 'a3temp' }>";
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("(tc1)", commonFTL);
        tl.putTemplate("(tc1)noHeader", "");
        tl.putTemplate("(tc2)", commonFTL);
        tl.putTemplate("(tc1)(tc2)", commonFTL);
        cfg.setTemplateLoader(tl);

        {
            Template t = cfg.getTemplate("(tc1)");
            assertEquals("a1tc1", t.getCustomAttribute("a1"));
            assertEquals("a2tc1", t.getCustomAttribute("a2"));
            assertEquals("a3temp", t.getCustomAttribute("a3"));
            assertEquals("ca1tc1", CUST_ATT_1.get(t));
            assertEquals("ca2tc1", CUST_ATT_2.get(t));
        }
        {
            Template t = cfg.getTemplate("(tc1)noHeader");
            assertEquals("a1tc1", t.getCustomAttribute("a1"));
            assertEquals("a2tc1", t.getCustomAttribute("a2"));
            assertEquals("a3tc1", t.getCustomAttribute("a3"));
            assertEquals("ca1tc1", CUST_ATT_1.get(t));
            assertEquals("ca2tc1", CUST_ATT_2.get(t));
        }
        {
            Template t = cfg.getTemplate("(tc2)");
            assertEquals("a1tc2", t.getCustomAttribute("a1"));
            assertNull(t.getCustomAttribute("a2"));
            assertEquals("a3temp", t.getCustomAttribute("a3"));
            assertEquals("ca1tc2", CUST_ATT_1.get(t));
            assertNull(CUST_ATT_2.get(t));
        }
        {
            Template t = cfg.getTemplate("(tc1)(tc2)");
            assertEquals("a1tc2", t.getCustomAttribute("a1"));
            assertEquals("a2tc1", t.getCustomAttribute("a2"));
            assertEquals("a3temp", t.getCustomAttribute("a3"));
            assertEquals("ca1tc2", CUST_ATT_1.get(t));
            assertEquals("ca2tc1", CUST_ATT_2.get(t));
        }
    }
    
    private String getTemplateOutput(Template t) throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        return sw.toString();
    }

    private Configuration createCommonEncodingTesterConfig() throws UnsupportedEncodingException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setDefaultEncoding("iso-8859-1");
        cfg.setLocale(Locale.US);
        
        ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
        tl.putTemplate("utf8.ftl", TEXT_WITH_ACCENTS.getBytes("utf-8"));
        tl.putTemplate("utf16.ftl", TEXT_WITH_ACCENTS.getBytes("utf-16"));
        tl.putTemplate("default.ftl", TEXT_WITH_ACCENTS.getBytes("iso-8859-2"));
        tl.putTemplate("utf8-latin2.ftl",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes("iso-8859-2"));
        tl.putTemplate("default-latin2.ftl",
                ("<#ftl encoding='iso-8859-2'>" + TEXT_WITH_ACCENTS).getBytes("iso-8859-2"));
        cfg.setTemplateLoader(tl);
        
        TemplateConfigurer tcUtf8 = new TemplateConfigurer();
        tcUtf8.setEncoding("utf-8");
        TemplateConfigurer tcUtf16 = new TemplateConfigurer();
        tcUtf16.setEncoding("utf-16");
        cfg.setTemplateConfigurers(
                new FirstMatchTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*utf8*"), tcUtf8),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*utf16*"), tcUtf16)
                ).allowNoMatch(true));
        return cfg;
    }

}

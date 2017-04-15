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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupResult;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.test.MonitoredTemplateLoader;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TemplateLookupStrategyTest {

    @Test
    public void testSetSetting() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        assertSame(DefaultTemplateLookupStrategy.INSTANCE, cfg.getTemplateLookupStrategy());

        cfg.setSetting(Configuration.TEMPLATE_LOOKUP_STRATEGY_KEY, MyTemplateLookupStrategy.class.getName() + "()");
        assertTrue(cfg.getTemplateLookupStrategy() instanceof MyTemplateLookupStrategy);
        
        cfg.setSetting(Configuration.TEMPLATE_LOOKUP_STRATEGY_KEY, "dEfault");
        assertSame(DefaultTemplateLookupStrategy.INSTANCE, cfg.getTemplateLookupStrategy());
    }
    
    @Test
    public void testCustomStrategy() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.ftl", "");
        tl.putTextTemplate("aa/test.ftl", "");
        cfg.setTemplateLoader(tl);
        
        cfg.setTemplateLookupStrategy(MyTemplateLookupStrategy.INSTANCE);
        
        final Locale locale = new Locale("aa", "BB", "CC_DD");
        
        try {
            cfg.getTemplate("missing.ftl", locale);
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(ImmutableList.of("aa/missing.ftl", "missing.ftl"), tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Template t = cfg.getTemplate("test.ftl", locale);
            assertEquals("test.ftl", t.getLookupName());
            assertEquals("aa/test.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(ImmutableList.of("aa/test.ftl"), tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
    }
    
    @Test
    public void testDefaultStrategy() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.ftl", "");
        tl.putTextTemplate("test_aa.ftl", "");
        tl.putTextTemplate("test_aa_BB.ftl", "");
        tl.putTextTemplate("test_aa_BB_CC.ftl", "");
        tl.putTextTemplate("test_aa_BB_CC_DD.ftl", "");
        cfg.setTemplateLoader(tl);
        
        try {
            cfg.getTemplate("missing.ftl", new Locale("aa", "BB", "CC_DD"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of(
                            "missing_aa_BB_CC_DD.ftl",
                            "missing_aa_BB_CC.ftl",
                            "missing_aa_BB.ftl",
                            "missing_aa.ftl",
                            "missing.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        cfg.setLocale(new Locale("xx"));
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing_xx.ftl", "missing.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        cfg.setLocalizedLookup(false);
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        cfg.setLocalizedLookup(true);
        
        try {
            cfg.getTemplate("_a_b_.ftl", new Locale("xx", "yy"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("_a_b_.ftl", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("_a_b__xx_YY.ftl", "_a_b__xx.ftl", "_a_b_.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        for (String templateName : new String[] { "test.ftl", "./test.ftl", "/test.ftl", "x/foo/../../test.ftl" }) {
            {
                final Locale locale = new Locale("aa", "BB", "CC_DD");
                final Template t = cfg.getTemplate("test.ftl", locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test_aa_BB_CC_DD.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(ImmutableList.of("test_aa_BB_CC_DD.ftl"), tl.getLoadNames());
                assertNull(t.getCustomLookupCondition());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
            
            {
                final Locale locale = new Locale("aa", "BB", "CC_XX");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test_aa_BB_CC.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(ImmutableList.of("test_aa_BB_CC_XX.ftl", "test_aa_BB_CC.ftl"), tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
            
            {
                final Locale locale = new Locale("aa", "BB", "XX_XX");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test_aa_BB.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test_aa_BB_XX_XX.ftl", "test_aa_BB_XX.ftl", "test_aa_BB.ftl"),
                        tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
    
            {
                cfg.setLocalizedLookup(false);
                final Locale locale = new Locale("aa", "BB", "XX_XX");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test.ftl"),
                        tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
                cfg.setLocalizedLookup(true);
            }
    
            {
                final Locale locale = new Locale("aa", "XX", "XX_XX");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test_aa.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test_aa_XX_XX_XX.ftl", "test_aa_XX_XX.ftl", "test_aa_XX.ftl", "test_aa.ftl"),
                        tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
            
            {
                final Locale locale = new Locale("xx", "XX", "XX_XX");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of(
                                "test_xx_XX_XX_XX.ftl", "test_xx_XX_XX.ftl", "test_xx_XX.ftl", "test_xx.ftl", "test.ftl"),
                        tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
            
            {
                final Locale locale = new Locale("xx", "BB", "CC_DD");
                final Template t = cfg.getTemplate(templateName, locale);
                assertEquals("test.ftl", t.getLookupName());
                assertEquals("test.ftl", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of(
                            "test_xx_BB_CC_DD.ftl", "test_xx_BB_CC.ftl", "test_xx_BB.ftl", "test_xx.ftl", "test.ftl"),
                        tl.getLoadNames());
                tl.clearEvents();
                cfg.clearTemplateCache();
            }
        }
    }
    
    @Test
    public void testAcquisition() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("t.ftl", "");
        tl.putTextTemplate("sub/i.ftl", "");
        tl.putTextTemplate("x/sub/i.ftl", "");
        cfg.setTemplateLoader(tl);

        final Locale locale = new Locale("xx");
        
        {
            final Template t = cfg.getTemplate("/./moo/../x/y/*/sub/i.ftl", locale);
            assertEquals("x/y/*/sub/i.ftl", t.getLookupName());
            assertEquals("x/sub/i.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(
                    ImmutableList.of(
                        "x/y/sub/i_xx.ftl", "x/sub/i_xx.ftl", "sub/i_xx.ftl",
                        "x/y/sub/i.ftl", "x/sub/i.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            final Template t = cfg.getTemplate("a/b/*/./sub/i.ftl", locale);
            assertEquals("a/b/*/sub/i.ftl", t.getLookupName());
            assertEquals("sub/i.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(
                    ImmutableList.of(
                        "a/b/sub/i_xx.ftl", "a/sub/i_xx.ftl", "sub/i_xx.ftl",
                        "a/b/sub/i.ftl", "a/sub/i.ftl", "sub/i.ftl"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
    }

    @Test
    public void testCustomLookupCondition() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        final String iAtDefaultContent = "i at default";
        final String iXxAtDefaultContent = "i_xx at default";
        final String iAtBaazComContent = "i at baaz.com";
        final String iAtFooComContent = "i at foo.com";
        final String tAtDefaultWithoutIncludeContent = "t at default ";
        final String tAtDefaultContent = toCanonicalFTL(tAtDefaultWithoutIncludeContent + "<#include 'i.ftl'>", cfg);
        final String tAtBarComWithoutIncludeContent = "t at bar.com ";
        final String tAtBarComContent = toCanonicalFTL(tAtBarComWithoutIncludeContent + "<#include 'i.ftl'>", cfg);
        final String tAtFooComWithoutIncludeContent = "t at foo.com ";
        final String tAtFooComContent = toCanonicalFTL(tAtFooComWithoutIncludeContent + "<#include 'i.ftl'>", cfg);
        final String t2XxLocaleExpectedOutput = "i3_xx at foo.com";
        final String t2OtherLocaleExpectedOutput = "i3 at foo.com";
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("@foo.com/t.ftl", tAtFooComContent);
        tl.putTextTemplate("@bar.com/t.ftl", tAtBarComContent);
        tl.putTextTemplate("@default/t.ftl", tAtDefaultContent);
        tl.putTextTemplate("@foo.com/i.ftl", iAtFooComContent);
        tl.putTextTemplate("@baaz.com/i.ftl", iAtBaazComContent);
        tl.putTextTemplate("@default/i_xx.ftl", iXxAtDefaultContent);
        tl.putTextTemplate("@default/i.ftl", iAtDefaultContent);
        tl.putTextTemplate("@foo.com/t2.ftl", "<#import 'i2.ftl' as i2 />${proof}");
        tl.putTextTemplate("@default/i2.ftl", "<#import 'i3.ftl' as i3 />");
        tl.putTextTemplate("@foo.com/i3.ftl", "<#global proof = '" + t2OtherLocaleExpectedOutput + "'>");
        tl.putTextTemplate("@foo.com/i3_xx.ftl", "<#global proof = '" + t2XxLocaleExpectedOutput + "'>");
        cfg.setTemplateLoader(tl);
        
        cfg.setTemplateLookupStrategy(new DomainTemplateLookupStrategy());
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t.ftl", locale, domain);
            assertEquals("t.ftl", t.getLookupName());
            assertEquals("@foo.com/t.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtFooComContent, t.toString());
            assertEquals(
                    ImmutableList.of("@foo.com/t_xx.ftl", "@foo.com/t.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtFooComWithoutIncludeContent + iAtFooComContent, t);
            assertEquals(
                    ImmutableList.of("@foo.com/i_xx.ftl", "@foo.com/i.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("bar.com");
            final Template t = cfg.getTemplate("t.ftl", locale, domain);
            assertEquals("t.ftl", t.getLookupName());
            assertEquals("@bar.com/t.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtBarComContent, t.toString());
            assertEquals(
                    ImmutableList.of("@bar.com/t_xx.ftl", "@bar.com/t.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtBarComWithoutIncludeContent + iXxAtDefaultContent, t);
            assertEquals(
                    ImmutableList.of(
                            "@bar.com/i_xx.ftl", "@bar.com/i.ftl",
                            "@default/i_xx.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("baaz.com");
            final Template t = cfg.getTemplate("t.ftl", locale, domain);
            assertEquals("t.ftl", t.getLookupName());
            assertEquals("@default/t.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of(
                            "@baaz.com/t_xx_YY.ftl", "@baaz.com/t_xx.ftl", "@baaz.com/t.ftl",
                            "@default/t_xx_YY.ftl", "@default/t_xx.ftl", "@default/t.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtDefaultWithoutIncludeContent + iAtBaazComContent, t);
            assertEquals(
                    ImmutableList.of("@baaz.com/i_xx_YY.ftl", "@baaz.com/i_xx.ftl", "@baaz.com/i.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("nosuch.com");
            final Template t = cfg.getTemplate("i.ftl", locale, domain);
            assertEquals("i.ftl", t.getLookupName());
            assertEquals("@default/i_xx.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(iXxAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of(
                            "@nosuch.com/i_xx_YY.ftl", "@nosuch.com/i_xx.ftl", "@nosuch.com/i.ftl",
                            "@default/i_xx_YY.ftl", "@default/i_xx.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            cfg.setLocalizedLookup(false);
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("nosuch.com");
            final Template t = cfg.getTemplate("i.ftl", locale, domain);
            assertEquals("i.ftl", t.getLookupName());
            assertEquals("@default/i.ftl", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(iAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of("@nosuch.com/i.ftl", "@default/i.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.setLocalizedLookup(true);
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t2.ftl", locale, domain);
            assertOutputEquals(t2XxLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2_xx.ftl", "@foo.com/t2.ftl",
                            "@foo.com/i2_xx.ftl", "@foo.com/i2.ftl", "@default/i2_xx.ftl", "@default/i2.ftl",
                            "@foo.com/i3_xx.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("yy");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t2.ftl", locale, domain);
            assertOutputEquals(t2OtherLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2_yy.ftl", "@foo.com/t2.ftl",
                            "@foo.com/i2_yy.ftl", "@foo.com/i2.ftl", "@default/i2_yy.ftl", "@default/i2.ftl",
                            "@foo.com/i3_yy.ftl", "@foo.com/i3.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            cfg.setLocalizedLookup(false);
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t2.ftl", locale, domain);
            assertOutputEquals(t2OtherLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2.ftl",
                            "@foo.com/i2.ftl", "@default/i2.ftl",
                            "@foo.com/i3.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.setLocalizedLookup(true);
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            cfg.getTemplate("i3.ftl", locale, domain);
            assertEquals(
                    ImmutableList.of("@foo.com/i3_xx.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("bar.com");
            try {
                cfg.getTemplate("i3.ftl", locale, domain);
            } catch (TemplateNotFoundException e) {
                assertEquals("i3.ftl", e.getTemplateName());
                assertEquals(domain, e.getCustomLookupCondition());
            }
            assertEquals(
                    ImmutableList.of(
                            "@bar.com/i3_xx.ftl", "@bar.com/i3.ftl",
                            "@default/i3_xx.ftl", "@default/i3.ftl"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
    }

    public static class Domain implements Serializable {
        private final String name;

        public Domain(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Domain domain = (Domain) o;

            return name != null ? name.equals(domain.name) : domain.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
    
    @Test
    public void testNonparsed() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.txt", "");
        tl.putTextTemplate("test_aa.txt", "");
        cfg.setTemplateLoader(tl);
        
        try {
            cfg.getTemplate("missing.txt", new Locale("aa", "BB"), null, false);
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.txt", e.getTemplateName());
            assertEquals(
                    ImmutableList.of(
                            "missing_aa_BB.txt",
                            "missing_aa.txt",
                            "missing.txt"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            Template t = cfg.getTemplate("test.txt", new Locale("aa", "BB"), null, false);
            assertEquals("test.txt", t.getLookupName());
            assertEquals("test_aa.txt", t.getSourceName());
            assertEquals(
                    ImmutableList.of(
                            "test_aa_BB.txt",
                            "test_aa.txt"),
                    tl.getLoadNames());
        }
    }

    @Test
    public void testParseError() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_3_0_0);
        
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.ftl", "");
        tl.putTextTemplate("test_aa.ftl", "<#wrong>");
        cfg.setTemplateLoader(tl);
        
        try {
            cfg.getTemplate("test.ftl", new Locale("aa", "BB"));
            fail();
        } catch (ParseException e) {
            assertEquals("test_aa.ftl", e.getTemplateSourceName());
            assertEquals("test.ftl", e.getTemplateLookupName());
        }
    }
    
    private String toCanonicalFTL(String ftl, Configuration cfg) throws IOException {
        return new Template(null, ftl, cfg).toString();        
    }

    private void assertOutputEquals(final String expectedContent, final Template t) throws TemplateException,
            IOException {
        StringWriter sw = new StringWriter(); 
        t.process(null, sw);
        assertEquals(expectedContent, sw.toString());
    }
    
    public static class MyTemplateLookupStrategy extends TemplateLookupStrategy {
        
        public static final MyTemplateLookupStrategy INSTANCE = new MyTemplateLookupStrategy();
        
        private MyTemplateLookupStrategy() { }

        @Override
        public <R extends TemplateLookupResult> R lookup(TemplateLookupContext<R> ctx) throws IOException {
            String lang = ctx.getTemplateLocale().getLanguage().toLowerCase();
            R lookupResult = ctx.lookupWithAcquisitionStrategy(lang + "/" + ctx.getTemplateName());
            if (lookupResult.isPositive()) {
                return lookupResult;
            }
            
            return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
        }
        
    }
    
    public static class DomainTemplateLookupStrategy extends TemplateLookupStrategy {
        
        public static final DomainTemplateLookupStrategy INSTANCE = new DomainTemplateLookupStrategy();

        @Override
        public <R extends TemplateLookupResult> R lookup(TemplateLookupContext<R> ctx) throws IOException {
            Domain domain = (Domain) ctx.getCustomLookupCondition();
            if (domain == null) {
                throw new NullPointerException("The domain wasn't specified");
            }
            
            final String templateName = ctx.getTemplateName();
            
            // Disallow addressing the domain roots directly:
            if (templateName.startsWith("@")) {
                return ctx.createNegativeLookupResult();
            }
            
            R lookupResult = ctx.lookupWithLocalizedThenAcquisitionStrategy(
                    "@" + domain.name + "/" + templateName,
                    ctx.getTemplateLocale());
            if (lookupResult.isPositive()) {
                return lookupResult;
            }
            
            return ctx.lookupWithLocalizedThenAcquisitionStrategy("@default/" + templateName, ctx.getTemplateLocale());
        }
        
    }

}

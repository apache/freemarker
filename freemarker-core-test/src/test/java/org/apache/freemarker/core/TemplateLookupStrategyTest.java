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

import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;
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
        assertSame(
                DefaultTemplateLookupStrategy.INSTANCE,
                new Configuration.Builder(Configuration.VERSION_3_0_0).build()
                        .getTemplateLookupStrategy());

        assertTrue(
                new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .setting(TEMPLATE_LOOKUP_STRATEGY_KEY, MyTemplateLookupStrategy.class.getName() + "()")
                        .build()
                        .getTemplateLookupStrategy() instanceof MyTemplateLookupStrategy);
        
        assertSame(
                DefaultTemplateLookupStrategy.INSTANCE,
                new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .setting(TEMPLATE_LOOKUP_STRATEGY_KEY, "dEfault")
                        .build()
                        .getTemplateLookupStrategy());
    }
    
    @Test
    public void testCustomStrategy() throws IOException {
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.f3ah", "");
        tl.putTextTemplate("aa/test.f3ah", "");

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(tl)
                .templateLookupStrategy(MyTemplateLookupStrategy.INSTANCE)
                .build();
        
        final Locale locale = new Locale("aa", "BB", "CC_DD");
        
        try {
            cfg.getTemplate("missing.f3ah", locale);
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.f3ah", e.getTemplateName());
            assertEquals(ImmutableList.of("aa/missing.f3ah", "missing.f3ah"), tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Template t = cfg.getTemplate("test.f3ah", locale);
            assertEquals("test.f3ah", t.getLookupName());
            assertEquals("aa/test.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(ImmutableList.of("aa/test.f3ah"), tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
    }
    
    @Test
    public void testDefaultStrategy() throws IOException {
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.f3ah", "");
        tl.putTextTemplate("test_aa.f3ah", "");
        tl.putTextTemplate("test_aa_BB.f3ah", "");
        tl.putTextTemplate("test_aa_BB_CC.f3ah", "");
        tl.putTextTemplate("test_aa_BB_CC_DD.f3ah", "");

        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                    .getTemplate("missing.f3ah", new Locale("aa", "BB", "CC_DD"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.f3ah", e.getTemplateName());
            assertEquals(
                    ImmutableList.of(
                            "missing_aa_BB_CC_DD.f3ah",
                            "missing_aa_BB_CC.f3ah",
                            "missing_aa_BB.f3ah",
                            "missing_aa.f3ah",
                            "missing.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
        }

        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl)
                    .locale(new Locale("xx")).build()
                    .getTemplate("missing.f3ah");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.f3ah", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing_xx.f3ah", "missing.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
        }
        
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl)
                    .locale(new Locale("xx"))
                    .localizedTemplateLookup(false).build()
                    .getTemplate("missing.f3ah");
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("missing.f3ah", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("missing.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
        }

        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                    .getTemplate("_a_b_.f3ah", new Locale("xx", "yy"));
            fail();
        } catch (TemplateNotFoundException e) {
            assertEquals("_a_b_.f3ah", e.getTemplateName());
            assertEquals(
                    ImmutableList.of("_a_b__xx_YY.f3ah", "_a_b__xx.f3ah", "_a_b_.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
        }

        for (String templateName : new String[] { "test.f3ah", "./test.f3ah", "/test.f3ah", "x/foo/../../test.f3ah" }) {
            {
                final Locale locale = new Locale("aa", "BB", "CC_DD");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate("test.f3ah", locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test_aa_BB_CC_DD.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(ImmutableList.of("test_aa_BB_CC_DD.f3ah"), tl.getLoadNames());
                assertNull(t.getCustomLookupCondition());
                tl.clearEvents();
            }
            
            {
                final Locale locale = new Locale("aa", "BB", "CC_XX");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test_aa_BB_CC.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(ImmutableList.of("test_aa_BB_CC_XX.f3ah", "test_aa_BB_CC.f3ah"), tl.getLoadNames());
                tl.clearEvents();
            }
            
            {
                final Locale locale = new Locale("aa", "BB", "XX_XX");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test_aa_BB.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test_aa_BB_XX_XX.f3ah", "test_aa_BB_XX.f3ah", "test_aa_BB.f3ah"),
                        tl.getLoadNames());
                tl.clearEvents();
            }
    
            {
                final Locale locale = new Locale("aa", "BB", "XX_XX");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl)
                        .localizedTemplateLookup(false).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test.f3ah"),
                        tl.getLoadNames());
                tl.clearEvents();
            }
    
            {
                final Locale locale = new Locale("aa", "XX", "XX_XX");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test_aa.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of("test_aa_XX_XX_XX.f3ah", "test_aa_XX_XX.f3ah", "test_aa_XX.f3ah", "test_aa.f3ah"),
                        tl.getLoadNames());
                tl.clearEvents();
            }
            
            {
                final Locale locale = new Locale("xx", "XX", "XX_XX");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of(
                                "test_xx_XX_XX_XX.f3ah", "test_xx_XX_XX.f3ah", "test_xx_XX.f3ah", "test_xx.f3ah", "test.f3ah"),
                        tl.getLoadNames());
                tl.clearEvents();
            }
            
            {
                final Locale locale = new Locale("xx", "BB", "CC_DD");
                final Template t = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build()
                        .getTemplate(templateName, locale);
                assertEquals("test.f3ah", t.getLookupName());
                assertEquals("test.f3ah", t.getSourceName());
                assertEquals(locale, t.getLocale());
                assertNull(t.getCustomLookupCondition());
                assertEquals(
                        ImmutableList.of(
                            "test_xx_BB_CC_DD.f3ah", "test_xx_BB_CC.f3ah", "test_xx_BB.f3ah", "test_xx.f3ah", "test.f3ah"),
                        tl.getLoadNames());
                tl.clearEvents();
            }
        }
    }
    
    @Test
    public void testAcquisition() throws IOException {
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("t.f3ah", "");
        tl.putTextTemplate("sub/i.f3ah", "");
        tl.putTextTemplate("x/sub/i.f3ah", "");

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build();

        final Locale locale = new Locale("xx");
        
        {
            final Template t = cfg.getTemplate("/./moo/../x/y/*/sub/i.f3ah", locale);
            assertEquals("x/y/*/sub/i.f3ah", t.getLookupName());
            assertEquals("x/sub/i.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(
                    ImmutableList.of(
                        "x/y/sub/i_xx.f3ah", "x/sub/i_xx.f3ah", "sub/i_xx.f3ah",
                        "x/y/sub/i.f3ah", "x/sub/i.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            final Template t = cfg.getTemplate("a/b/*/./sub/i.f3ah", locale);
            assertEquals("a/b/*/sub/i.f3ah", t.getLookupName());
            assertEquals("sub/i.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(
                    ImmutableList.of(
                        "a/b/sub/i_xx.f3ah", "a/sub/i_xx.f3ah", "sub/i_xx.f3ah",
                        "a/b/sub/i.f3ah", "a/sub/i.f3ah", "sub/i.f3ah"),
                    tl.getLoadNames());
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
    }

    @Test
    public void testCustomLookupCondition() throws IOException, TemplateException {
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();

        final Configuration cfg;
        final Configuration cfgNoLocLU;
        {
            cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateLookupStrategy(new DomainTemplateLookupStrategy())
                    .build();
            cfgNoLocLU = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateLookupStrategy(new DomainTemplateLookupStrategy())
                    .localizedTemplateLookup(false)
                    .build();
        }

        final String iAtDefaultContent = "i at default";
        final String iXxAtDefaultContent = "i_xx at default";
        final String iAtBaazComContent = "i at baaz.com";
        final String iAtFooComContent = "i at foo.com";
        final String tAtDefaultWithoutIncludeContent = "t at default ";
        final String tAtDefaultContent = toCanonicalFTL(tAtDefaultWithoutIncludeContent + "<#include 'i.f3ah'>", cfg);
        final String tAtBarComWithoutIncludeContent = "t at bar.com ";
        final String tAtBarComContent = toCanonicalFTL(tAtBarComWithoutIncludeContent + "<#include 'i.f3ah'>", cfg);
        final String tAtFooComWithoutIncludeContent = "t at foo.com ";
        final String tAtFooComContent = toCanonicalFTL(tAtFooComWithoutIncludeContent + "<#include 'i.f3ah'>", cfg);
        final String t2XxLocaleExpectedOutput = "i3_xx at foo.com";
        final String t2OtherLocaleExpectedOutput = "i3 at foo.com";
        
        tl.putTextTemplate("@foo.com/t.f3ah", tAtFooComContent);
        tl.putTextTemplate("@bar.com/t.f3ah", tAtBarComContent);
        tl.putTextTemplate("@default/t.f3ah", tAtDefaultContent);
        tl.putTextTemplate("@foo.com/i.f3ah", iAtFooComContent);
        tl.putTextTemplate("@baaz.com/i.f3ah", iAtBaazComContent);
        tl.putTextTemplate("@default/i_xx.f3ah", iXxAtDefaultContent);
        tl.putTextTemplate("@default/i.f3ah", iAtDefaultContent);
        tl.putTextTemplate("@foo.com/t2.f3ah", "<#import 'i2.f3ah' as i2 />${proof}");
        tl.putTextTemplate("@default/i2.f3ah", "<#import 'i3.f3ah' as i3 />");
        tl.putTextTemplate("@foo.com/i3.f3ah", "<#global proof = '" + t2OtherLocaleExpectedOutput + "'>");
        tl.putTextTemplate("@foo.com/i3_xx.f3ah", "<#global proof = '" + t2XxLocaleExpectedOutput + "'>");

        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t.f3ah", locale, domain);
            assertEquals("t.f3ah", t.getLookupName());
            assertEquals("@foo.com/t.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtFooComContent, t.toString());
            assertEquals(
                    ImmutableList.of("@foo.com/t_xx.f3ah", "@foo.com/t.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtFooComWithoutIncludeContent + iAtFooComContent, t);
            assertEquals(
                    ImmutableList.of("@foo.com/i_xx.f3ah", "@foo.com/i.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("bar.com");
            final Template t = cfg.getTemplate("t.f3ah", locale, domain);
            assertEquals("t.f3ah", t.getLookupName());
            assertEquals("@bar.com/t.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtBarComContent, t.toString());
            assertEquals(
                    ImmutableList.of("@bar.com/t_xx.f3ah", "@bar.com/t.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtBarComWithoutIncludeContent + iXxAtDefaultContent, t);
            assertEquals(
                    ImmutableList.of(
                            "@bar.com/i_xx.f3ah", "@bar.com/i.f3ah",
                            "@default/i_xx.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("baaz.com");
            final Template t = cfg.getTemplate("t.f3ah", locale, domain);
            assertEquals("t.f3ah", t.getLookupName());
            assertEquals("@default/t.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(tAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of(
                            "@baaz.com/t_xx_YY.f3ah", "@baaz.com/t_xx.f3ah", "@baaz.com/t.f3ah",
                            "@default/t_xx_YY.f3ah", "@default/t_xx.f3ah", "@default/t.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            assertOutputEquals(tAtDefaultWithoutIncludeContent + iAtBaazComContent, t);
            assertEquals(
                    ImmutableList.of("@baaz.com/i_xx_YY.f3ah", "@baaz.com/i_xx.f3ah", "@baaz.com/i.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("nosuch.com");
            final Template t = cfg.getTemplate("i.f3ah", locale, domain);
            assertEquals("i.f3ah", t.getLookupName());
            assertEquals("@default/i_xx.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(iXxAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of(
                            "@nosuch.com/i_xx_YY.f3ah", "@nosuch.com/i_xx.f3ah", "@nosuch.com/i.f3ah",
                            "@default/i_xx_YY.f3ah", "@default/i_xx.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }

        {
            final Locale locale = new Locale("xx", "YY");
            final Domain domain = new Domain("nosuch.com");
            final Template t = cfgNoLocLU.getTemplate("i.f3ah", locale, domain);
            assertEquals("i.f3ah", t.getLookupName());
            assertEquals("@default/i.f3ah", t.getSourceName());
            assertEquals(locale, t.getLocale());
            assertEquals(domain, t.getCustomLookupCondition());
            assertEquals(iAtDefaultContent, t.toString());
            assertEquals(
                    ImmutableList.of("@nosuch.com/i.f3ah", "@default/i.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfgNoLocLU.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t2.f3ah", locale, domain);
            assertOutputEquals(t2XxLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2_xx.f3ah", "@foo.com/t2.f3ah",
                            "@foo.com/i2_xx.f3ah", "@foo.com/i2.f3ah", "@default/i2_xx.f3ah", "@default/i2.f3ah",
                            "@foo.com/i3_xx.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("yy");
            final Domain domain = new Domain("foo.com");
            final Template t = cfg.getTemplate("t2.f3ah", locale, domain);
            assertOutputEquals(t2OtherLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2_yy.f3ah", "@foo.com/t2.f3ah",
                            "@foo.com/i2_yy.f3ah", "@foo.com/i2.f3ah", "@default/i2_yy.f3ah", "@default/i2.f3ah",
                            "@foo.com/i3_yy.f3ah", "@foo.com/i3.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            final Template t = cfgNoLocLU.getTemplate("t2.f3ah", locale, domain);
            assertOutputEquals(t2OtherLocaleExpectedOutput, t);
            assertEquals(
                    ImmutableList.of(
                            "@foo.com/t2.f3ah",
                            "@foo.com/i2.f3ah", "@default/i2.f3ah",
                            "@foo.com/i3.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfgNoLocLU.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("foo.com");
            cfg.getTemplate("i3.f3ah", locale, domain);
            assertEquals(
                    ImmutableList.of("@foo.com/i3_xx.f3ah"),
                    tl.getLoadNames());
            
            tl.clearEvents();
            cfg.clearTemplateCache();
        }
        
        {
            final Locale locale = new Locale("xx");
            final Domain domain = new Domain("bar.com");
            try {
                cfg.getTemplate("i3.f3ah", locale, domain);
            } catch (TemplateNotFoundException e) {
                assertEquals("i3.f3ah", e.getTemplateName());
                assertEquals(domain, e.getCustomLookupCondition());
            }
            assertEquals(
                    ImmutableList.of(
                            "@bar.com/i3_xx.f3ah", "@bar.com/i3.f3ah",
                            "@default/i3_xx.f3ah", "@default/i3.f3ah"),
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
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.txt", "");
        tl.putTextTemplate("test_aa.txt", "");

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build();

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
        MonitoredTemplateLoader tl = new MonitoredTemplateLoader();
        tl.putTextTemplate("test.f3ah", "");
        tl.putTextTemplate("test_aa.f3ah", "<#wrong>");

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build();
        
        try {
            cfg.getTemplate("test.f3ah", new Locale("aa", "BB"));
            fail();
        } catch (ParseException e) {
            assertEquals("test_aa.f3ah", e.getTemplateSourceName());
            assertEquals("test.f3ah", e.getTemplateLookupName());
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

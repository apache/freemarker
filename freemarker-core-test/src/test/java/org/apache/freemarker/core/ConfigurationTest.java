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

import static org.apache.freemarker.core.Configuration.*;
import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;
import static org.apache.freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.UnregisteredOutputFormatException;
import org.apache.freemarker.core.outputformat.impl.CombinedMarkupOutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.CacheStorageWithGetSize;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactoryException;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;
import org.apache.freemarker.core.templateresolver.TemplateLookupResult;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormatFM2;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateResolver;
import org.apache.freemarker.core.templateresolver.impl.NullCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.StrongCacheStorage;
import org.apache.freemarker.core.userpkg.BaseNTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.CustomHTMLOutputFormat;
import org.apache.freemarker.core.userpkg.DummyOutputFormat;
import org.apache.freemarker.core.userpkg.EpochMillisDivTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.EpochMillisTemplateDateFormatFactory;
import org.apache.freemarker.core.userpkg.HexTemplateNumberFormatFactory;
import org.apache.freemarker.core.userpkg.NameClashingDummyOutputFormat;
import org.apache.freemarker.core.userpkg.SeldomEscapedOutputFormat;
import org.apache.freemarker.core.util._CollectionUtil;
import org.apache.freemarker.core.util._DateUtil;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ConfigurationTest {

    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

    @Test
    public void testUnsetAndIsSet() throws Exception {
        // TODO This should automatically test all setting via reflection...

        Configuration.ExtendableBuilder<?> cfgB = new Builder(VERSION_3_0_0);
        
        assertFalse(cfgB.isAutoEscapingPolicySet());
        assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());
        //
        cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.DISABLE);
        {
            assertTrue(cfgB.isAutoEscapingPolicySet());
            assertEquals(AutoEscapingPolicy.DISABLE, cfgB.getAutoEscapingPolicy());
        }
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetAutoEscapingPolicy();
            assertFalse(cfgB.isAutoEscapingPolicySet());
            assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());
        }

        DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(VERSION_3_0_0).build();
        assertFalse(cfgB.isObjectWrapperSet());
        assertSame(dow, cfgB.getObjectWrapper());
        //
        RestrictedObjectWrapper ow = new RestrictedObjectWrapper.Builder(VERSION_3_0_0).build();
        cfgB.setObjectWrapper(ow);
        assertTrue(cfgB.isObjectWrapperSet());
        assertSame(ow, cfgB.getObjectWrapper());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetObjectWrapper();
            assertFalse(cfgB.isObjectWrapperSet());
            assertSame(dow, cfgB.getObjectWrapper());
        }
        
        assertFalse(cfgB.isTemplateExceptionHandlerSet());
        assertSame(TemplateExceptionHandler.RETHROW_HANDLER, cfgB.getTemplateExceptionHandler());
        //
        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
        assertTrue(cfgB.isTemplateExceptionHandlerSet());
        assertSame(TemplateExceptionHandler.DEBUG_HANDLER, cfgB.getTemplateExceptionHandler());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateExceptionHandler();
            assertFalse(cfgB.isTemplateExceptionHandlerSet());
            assertSame(TemplateExceptionHandler.RETHROW_HANDLER, cfgB.getTemplateExceptionHandler());
        }
        
        assertFalse(cfgB.isTemplateLoaderSet());
        assertNull(cfgB.getTemplateLoader());
        //
        cfgB.setTemplateLoader(null);
        assertTrue(cfgB.isTemplateLoaderSet());
        assertNull(cfgB.getTemplateLoader());
        //
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                cfgB.setTemplateLoader(new StringTemplateLoader());
            }
            cfgB.unsetTemplateLoader();
            assertFalse(cfgB.isTemplateLoaderSet());
            assertNull(cfgB.getTemplateLoader());
        }
        
        assertFalse(cfgB.isTemplateLookupStrategySet());
        assertSame(DefaultTemplateLookupStrategy.INSTANCE, cfgB.getTemplateLookupStrategy());
        //
        cfgB.setTemplateLookupStrategy(DefaultTemplateLookupStrategy.INSTANCE);
        assertTrue(cfgB.isTemplateLookupStrategySet());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateLookupStrategy();
            assertFalse(cfgB.isTemplateLookupStrategySet());
        }
        
        assertFalse(cfgB.isTemplateNameFormatSet());
        assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        //
        cfgB.setTemplateNameFormat(DefaultTemplateNameFormat.INSTANCE);
        assertTrue(cfgB.isTemplateNameFormatSet());
        assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateNameFormat();
            assertFalse(cfgB.isTemplateNameFormatSet());
            assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        }
        
        assertFalse(cfgB.isTemplateCacheStorageSet());
        assertTrue(cfgB.getTemplateCacheStorage() instanceof SoftCacheStorage);
        //
        cfgB.setTemplateCacheStorage(NullCacheStorage.INSTANCE);
        assertTrue(cfgB.isTemplateCacheStorageSet());
        assertSame(NullCacheStorage.INSTANCE, cfgB.getTemplateCacheStorage());
        //
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                cfgB.setTemplateCacheStorage(cfgB.getTemplateCacheStorage());
            }
            cfgB.unsetTemplateCacheStorage();
            assertFalse(cfgB.isTemplateCacheStorageSet());
            assertTrue(cfgB.getTemplateCacheStorage() instanceof SoftCacheStorage);
        }
    }

    @Test
    public void testTemplateLoadingErrors() throws Exception {
        Configuration cfg = new Builder(VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(getClass(), "nosuchpackage"))
                .build();
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), not(containsString("wasn't set")));
        }
    }

    @Test
    public void testVersion() {
        Version v = getVersion();
        assertTrue(v.intValue() >= _CoreAPI.VERSION_INT_3_0_0);
        
        try {
            new Builder(new Version(999, 1, 2));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }
        
        try {
            new Builder(new Version(2, 3, 0));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("3.0.0"));
        }
    }

    @Test
    public void testShowErrorTips() throws Exception {
        try {
            Configuration cfg = new Builder(VERSION_3_0_0).build();
            new Template(null, "${x}", cfg).process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("Tip:"));
        }

        try {
            Configuration cfg = new Builder(VERSION_3_0_0).showErrorTips(false).build();
            new Template(null, "${x}", cfg).process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), not(containsString("Tip:")));
        }
    }
    
    @Test
    @SuppressWarnings("boxing")
    public void testGetTemplateOverloads() throws Exception {
        final Locale hu = new Locale("hu", "HU");
        final String tFtl = "t.ftl";
        final String tHuFtl = "t_hu.ftl";
        final String tEnFtl = "t_en.ftl";
        final String tUtf8Ftl = "utf8.ftl";
        final Serializable custLookupCond = 12345;

        ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
        tl.putTemplate(tFtl, "${1}".getBytes(StandardCharsets.UTF_8));
        tl.putTemplate(tEnFtl, "${1}".getBytes(StandardCharsets.UTF_8));
        tl.putTemplate(tHuFtl, "${1}".getBytes(StandardCharsets.UTF_8));
        tl.putTemplate(tUtf8Ftl, "<#ftl encoding='utf-8'>".getBytes(StandardCharsets.UTF_8));

        Configuration cfg = new Builder(VERSION_3_0_0)
                .locale(Locale.GERMAN)
                .sourceEncoding(StandardCharsets.ISO_8859_1)
                .templateLoader(tl)
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*_hu.*"),
                                new TemplateConfiguration.Builder().sourceEncoding(ISO_8859_2).build()))
                .build();

        // 1 args:
        {
            Template t = cfg.getTemplate(tFtl);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl);
            assertEquals(tUtf8Ftl, t.getLookupName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
        }
        
        // 2 args:
        {
            Template t = cfg.getTemplate(tFtl, Locale.GERMAN);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, (Locale) null);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, Locale.US);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tEnFtl, t.getSourceName());
            assertEquals(Locale.US, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl, Locale.US);
            assertEquals(tUtf8Ftl, t.getLookupName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(Locale.US, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, hu);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tHuFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(ISO_8859_2, t.getActualSourceEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl, hu);
            assertEquals(tUtf8Ftl, t.getLookupName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(StandardCharsets.UTF_8, t.getActualSourceEncoding());
        }

        // 4 args:
        try {
            cfg.getTemplate("missing.ftl", hu, custLookupCond, false);
            fail();
        } catch (TemplateNotFoundException e) {
            // Expected
        }
        assertNull(cfg.getTemplate("missing.ftl", hu, custLookupCond, true));
        {
            Template t = cfg.getTemplate(tFtl, hu, custLookupCond, false);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tHuFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(ISO_8859_2, t.getActualSourceEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, custLookupCond, false);
            assertEquals(tFtl, t.getLookupName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(StandardCharsets.ISO_8859_1, t.getActualSourceEncoding());
            assertOutputEquals("1", t);
        }
    }

    private void assertOutputEquals(final String expectedContent, final Template t) throws ConfigurationException,
            IOException, TemplateException {
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        assertEquals(expectedContent, sw.toString());
    }

    @Test
    public void testTemplateResolverCache() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfgB.getTemplateCacheStorage();
        assertEquals(0, cache.getSize());
        cfgB.setTemplateCacheStorage(new StrongCacheStorage());
        cache = (CacheStorageWithGetSize) cfgB.getTemplateCacheStorage();
        assertEquals(0, cache.getSize());
        cfgB.setTemplateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""));
        Configuration cfg = cfgB.build();
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.getTemplate("toCache2.ftl");
        assertEquals(2, cache.getSize());
        cfg.clearTemplateCache();
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfgB.setTemplateLoader(cfgB.getTemplateLoader());
        assertEquals(1, cache.getSize());
    }

    @Test
    public void testTemplateNameFormat() throws Exception {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("a/b.ftl", "In a/b.ftl");
        tl.putTemplate("b.ftl", "In b.ftl");

        {
            Configuration cfg = new Builder(VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateNameFormat(DefaultTemplateNameFormatFM2.INSTANCE)
                    .build();
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("a/b.ftl", template.getLookupName());
            assertEquals("a/b.ftl", template.getSourceName());
            assertEquals("In a/b.ftl", template.toString());
        }
        
        {
            Configuration cfg = new Builder(VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateNameFormat(DefaultTemplateNameFormat.INSTANCE)
                    .build();
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("b.ftl", template.getLookupName());
            assertEquals("b.ftl", template.getSourceName());
            assertEquals("In b.ftl", template.toString());
        }
    }

    @Test
    public void testTemplateNameFormatSetSetting() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        cfgB.setSetting(TEMPLATE_NAME_FORMAT_KEY, "defAult_2_4_0");
        assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
        cfgB.setSetting(TEMPLATE_NAME_FORMAT_KEY, "defaUlt_2_3_0");
        assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        assertTrue(cfgB.isTemplateNameFormatSet());
        cfgB.setSetting(TEMPLATE_NAME_FORMAT_KEY, "defauLt");
        assertFalse(cfgB.isTemplateNameFormatSet());
    }

    @Test
    public void testObjectWrapperSetSetting() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        {
            cfgB.setSetting(OBJECT_WRAPPER_KEY, "defAult");
            DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(VERSION_3_0_0).build();
            assertSame(dow, cfgB.getObjectWrapper());
            assertEquals(VERSION_3_0_0, dow.getIncompatibleImprovements());
        }
        
        {
            cfgB.setSetting(OBJECT_WRAPPER_KEY, "restricted");
            assertThat(cfgB.getObjectWrapper(), instanceOf(RestrictedObjectWrapper.class));
        }
    }

    @Test
    public void testTemplateLookupStrategyDefault() throws Exception {
        Configuration cfg = new Builder(VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""))
                .build();
        assertSame(DefaultTemplateLookupStrategy.INSTANCE, cfg.getTemplateLookupStrategy());
        assertEquals("toCache1.ftl", cfg.getTemplate("toCache1.ftl").getSourceName());
    }

    @Test
    public void testTemplateLookupStrategyCustom() throws Exception {
        final TemplateLookupStrategy myStrategy = new TemplateLookupStrategy() {
            @Override
            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                return ctx.lookupWithAcquisitionStrategy("toCache2.ftl");
            }
        };

        Configuration cfg = new Builder(VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""))
                .templateLookupStrategy(myStrategy)
                .build();
        assertSame(myStrategy, cfg.getTemplateLookupStrategy());
        assertEquals("toCache2.ftl", cfg.getTemplate("toCache1.ftl").getSourceName());
    }

    @Test
    public void testSetTemplateConfigurations() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        assertNull(cfgB.getTemplateConfigurations());

        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("t.de.ftlh", "");
        tl.putTemplate("t.fr.ftlx", "");
        tl.putTemplate("t.ftlx", "");
        tl.putTemplate("Stat/t.de.ftlx", "");
        cfgB.setTemplateLoader(tl);
        
        cfgB.setTimeZone(TimeZone.getTimeZone("GMT+09"));
        
        cfgB.setSetting(TEMPLATE_CONFIGURATIONS_KEY,
                "MergingTemplateConfigurationFactory("
                    + "FirstMatchTemplateConfigurationFactory("
                        + "ConditionalTemplateConfigurationFactory("
                            + "FileNameGlobMatcher('*.de.*'), TemplateConfiguration(timeZone=TimeZone('GMT+01'))), "
                        + "ConditionalTemplateConfigurationFactory("
                            + "FileNameGlobMatcher('*.fr.*'), TemplateConfiguration(timeZone=TimeZone('GMT'))), "
                        + "allowNoMatch=true"
                    + "), "
                    + "FirstMatchTemplateConfigurationFactory("
                        + "ConditionalTemplateConfigurationFactory("
                            + "FileExtensionMatcher('ftlh'), TemplateConfiguration(booleanFormat='TODO,HTML')), "
                        + "ConditionalTemplateConfigurationFactory("
                            + "FileExtensionMatcher('ftlx'), TemplateConfiguration(booleanFormat='TODO,XML')), "
                        + "noMatchErrorDetails='Unrecognized template file extension'"
                    + "), "
                    + "ConditionalTemplateConfigurationFactory("
                        + "PathGlobMatcher('stat/**', caseInsensitive=true), "
                        + "TemplateConfiguration(timeZone=TimeZone('UTC'))"
                    + ")"
                + ")");

        Configuration cfg = cfgB.build();
        {
            Template t = cfg.getTemplate("t.de.ftlh");
            assertEquals("TODO,HTML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT+01"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("t.fr.ftlx");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("t.ftlx");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT+09"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("Stat/t.de.ftlx");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(_DateUtil.UTC, t.getTimeZone());
        }
        
        assertNotNull(cfgB.getTemplateConfigurations());
        cfgB.setSetting(TEMPLATE_CONFIGURATIONS_KEY, "null");
        assertNull(cfgB.getTemplateConfigurations());
    }

    @Test
    public void testGetOutputFormatByName() throws Exception {
        Configuration cfg = new Builder(VERSION_3_0_0).build();
        
        assertSame(HTMLOutputFormat.INSTANCE, cfg.getOutputFormat(HTMLOutputFormat.INSTANCE.getName()));
        
        try {
            cfg.getOutputFormat("noSuchFormat");
            fail();
        } catch (UnregisteredOutputFormatException e) {
            assertThat(e.getMessage(), containsString("noSuchFormat"));
        }
        
        try {
            cfg.getOutputFormat("HTML}");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("'{'"));
        }
        
        {
            OutputFormat of = cfg.getOutputFormat("HTML{RTF}");
            assertThat(of, instanceOf(CombinedMarkupOutputFormat.class));
            CombinedMarkupOutputFormat combinedOF = (CombinedMarkupOutputFormat) of;
            assertSame(HTMLOutputFormat.INSTANCE, combinedOF.getOuterOutputFormat());
            assertSame(RTFOutputFormat.INSTANCE, combinedOF.getInnerOutputFormat());
        }

        {
            OutputFormat of = cfg.getOutputFormat("XML{HTML{RTF}}");
            assertThat(of, instanceOf(CombinedMarkupOutputFormat.class));
            CombinedMarkupOutputFormat combinedOF = (CombinedMarkupOutputFormat) of;
            assertSame(XMLOutputFormat.INSTANCE, combinedOF.getOuterOutputFormat());
            MarkupOutputFormat innerOF = combinedOF.getInnerOutputFormat();
            assertThat(innerOF, instanceOf(CombinedMarkupOutputFormat.class));
            CombinedMarkupOutputFormat innerCombinedOF = (CombinedMarkupOutputFormat) innerOF; 
            assertSame(HTMLOutputFormat.INSTANCE, innerCombinedOF.getOuterOutputFormat());
            assertSame(RTFOutputFormat.INSTANCE, innerCombinedOF.getInnerOutputFormat());
        }
        
        try {
            cfg.getOutputFormat("plainText{HTML}");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(containsString("plainText"), containsString("markup")));
        }
        try {
            cfg.getOutputFormat("HTML{plainText}");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(containsString("plainText"), containsString("markup")));
        }
    }

    @Test
    public void testSetRegisteredCustomOutputFormats() throws Exception {
        Builder cfg = new Builder(VERSION_3_0_0);
        
        assertTrue(cfg.getRegisteredCustomOutputFormats().isEmpty());
        
        cfg.setSetting(REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
                "[org.apache.freemarker.core.userpkg.CustomHTMLOutputFormat(), "
                + "org.apache.freemarker.core.userpkg.DummyOutputFormat()]");
        assertEquals(
                ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE),
                new ArrayList(cfg.getRegisteredCustomOutputFormats()));
        
        try {
            cfg.setSetting(REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE, "[TemplateConfiguration()]");
            fail();
        } catch (InvalidSettingValueException e) {
            assertThat(e.getMessage(), containsString(OutputFormat.class.getSimpleName()));
        }
    }

    @Test
    public void testSetICIViaSetSettingAPI() throws ConfigurationException {
        Builder cfg = new Builder(VERSION_3_0_0);
        assertEquals(DEFAULT_INCOMPATIBLE_IMPROVEMENTS, cfg.getIncompatibleImprovements());
        // This is the only valid value ATM:
        cfg.setSetting(INCOMPATIBLE_IMPROVEMENTS_KEY, "3.0.0");
        assertEquals(VERSION_3_0_0, cfg.getIncompatibleImprovements());
    }

    @Test
    public void testSharedVariables() throws TemplateException, IOException {
        Configuration cfg = new Builder(VERSION_3_0_0)
                .sharedVariables(ImmutableMap.of(
                        "a", "aa",
                        "b", "bb",
                        "c", new MyScalarModel()
                ))
                .build();

        assertNull(cfg.getSharedVariables().get("noSuchVar"));
        assertNull(cfg.getWrappedSharedVariable("noSuchVar"));

        TemplateScalarModel aVal = (TemplateScalarModel) cfg.getWrappedSharedVariable("a");
        assertEquals("aa", aVal.getAsString());
        assertEquals(SimpleScalar.class, aVal.getClass());

        TemplateScalarModel bVal = (TemplateScalarModel) cfg.getWrappedSharedVariable("b");
        assertEquals("bb", bVal.getAsString());
        assertEquals(SimpleScalar.class, bVal.getClass());

        TemplateScalarModel cVal = (TemplateScalarModel) cfg.getWrappedSharedVariable("c");
        assertEquals("my", cVal.getAsString());
        assertEquals(MyScalarModel.class, cfg.getWrappedSharedVariable("c").getClass());

        // See if it actually works in templates:
        StringWriter sw = new StringWriter();
        new Template(null, "${a} ${b}", cfg)
                .process(ImmutableMap.of("a", "aaDM"), sw);
        assertEquals("aaDM bb", sw.toString());
    }

    @Test
    public void testTemplateUpdateDelay() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);

        assertEquals(
                DefaultTemplateResolver.DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS,
                (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setTemplateUpdateDelayMilliseconds(4000L);
        assertEquals(4000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setTemplateUpdateDelayMilliseconds(100L);
        assertEquals(100L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        try {
            cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "5");
            assertEquals(5000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        } catch (InvalidSettingValueException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("unit must be specified"));
        }
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "0");
        assertEquals(0L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        try {
            cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "5 foo");
            assertEquals(5000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        } catch (InvalidSettingValueException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("\"foo\""));
        }
        
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "3 ms");
        assertEquals(3L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "4ms");
        assertEquals(4L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "3 s");
        assertEquals(3000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "4s");
        assertEquals(4000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "3 m");
        assertEquals(1000L * 60 * 3, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "4m");
        assertEquals(1000L * 60 * 4, (Object) cfgB.getTemplateUpdateDelayMilliseconds());

        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "1 h");
        assertEquals(1000L * 60 * 60, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(TEMPLATE_UPDATE_DELAY_KEY, "2h");
        assertEquals(1000L * 60 * 60 * 2, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
    }

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        for (boolean camelCase : new boolean[] { false, true }) {
            List<String> names = new ArrayList<>(Builder.getSettingNames(camelCase));
            List<String> inheritedNames = new ArrayList<>(
                    MutableParsingAndProcessingConfiguration.getSettingNames(camelCase));
            assertStartsWith(names, inheritedNames);
            
            String prevName = null;
            for (int i = inheritedNames.size(); i < names.size(); i++) {
                String name = names.get(i);
                if (prevName != null) {
                    assertThat(name, greaterThan(prevName));
                }
                prevName = name;
            }
        }
    }

    @Test
    public void testGetSettingNamesNameConventionsContainTheSame() throws Exception {
        MutableProcessingConfigurationTest.testGetSettingNamesNameConventionsContainTheSame(
                new ArrayList<>(Builder.getSettingNames(false)),
                new ArrayList<>(Builder.getSettingNames(true)));
    }

    @Test
    public void testStaticFieldKeysCoverAllGetSettingNames() throws Exception {
        List<String> names = new ArrayList<>(Builder.getSettingNames(false));
        for (String name :  names) {
            assertTrue("No field was found for " + name, keyFieldExists(name));
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllStaticKeyFields() throws Exception {
        Collection<String> names = Builder.getSettingNames(false);
        
        for (Class<?> cfgableClass : new Class[] {
                Configuration.class,
                MutableParsingAndProcessingConfiguration.class,
                MutableProcessingConfiguration.class }) {
            for (Field f : cfgableClass.getFields()) {
                if (f.getName().endsWith("_KEY")) {
                    final Object name = f.get(null);
                    assertTrue("Missing setting name: " + name, names.contains(name));
                }
            }
        }
    }
    
    @Test
    public void testKeyStaticFieldsHasAllVariationsAndCorrectFormat() throws IllegalArgumentException, IllegalAccessException {
        MutableProcessingConfigurationTest.testKeyStaticFieldsHasAllVariationsAndCorrectFormat(ExtendableBuilder.class);
    }

    @Test
    public void testSetSettingSupportsBothNamingConventions() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        
        cfgB.setSetting(SOURCE_ENCODING_KEY_CAMEL_CASE, StandardCharsets.UTF_16LE.name());
        assertEquals(StandardCharsets.UTF_16LE, cfgB.getSourceEncoding());
        cfgB.setSetting(SOURCE_ENCODING_KEY_SNAKE_CASE, StandardCharsets.UTF_8.name());
        assertEquals(StandardCharsets.UTF_8, cfgB.getSourceEncoding());
        
        for (String nameCC : cfgB.getSettingNames(true)) {
            for (String value : new String[] { "1", "default", "true" }) {
                Exception resultCC = null;
                try {
                    cfgB.setSetting(nameCC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(InvalidSettingNameException.class)));
                    resultCC = e;
                }
                
                String nameSC = _StringUtil.camelCaseToUnderscored(nameCC);
                Exception resultSC = null;
                try {
                    cfgB.setSetting(nameSC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(InvalidSettingNameException.class)));
                    resultSC = e;
                }
                
                if (resultCC == null) {
                    assertNull(resultSC);
                } else {
                    assertNotNull(resultSC);
                    assertEquals(resultCC.getClass(), resultSC.getClass());
                }
            }
        }
    }
    
    @Test
    public void testGetSupportedBuiltInDirectiveNames() {
        Configuration cfg = new Builder(VERSION_3_0_0).build();
        
        Set<String> allNames = cfg.getSupportedBuiltInDirectiveNames(NamingConvention.AUTO_DETECT);
        Set<String> lNames = cfg.getSupportedBuiltInDirectiveNames(NamingConvention.LEGACY);
        Set<String> cNames = cfg.getSupportedBuiltInDirectiveNames(NamingConvention.CAMEL_CASE);
        
        checkNamingConventionNameSets(allNames, lNames, cNames);
        
        for (String name : cNames) {
            assertThat(name.toLowerCase(), isIn(lNames));
        }
    }

    @Test
    public void testGetSupportedBuiltInNames() {
        Configuration cfg = new Builder(VERSION_3_0_0).build();
        
        Set<String> allNames = cfg.getSupportedBuiltInNames(NamingConvention.AUTO_DETECT);
        Set<String> lNames = cfg.getSupportedBuiltInNames(NamingConvention.LEGACY);
        Set<String> cNames = cfg.getSupportedBuiltInNames(NamingConvention.CAMEL_CASE);
        
        checkNamingConventionNameSets(allNames, lNames, cNames);
    }

    private void checkNamingConventionNameSets(Set<String> allNames, Set<String> lNames, Set<String> cNames) {
        for (String name : lNames) {
            assertThat(allNames, hasItem(name));
            assertTrue("Should be all-lowercase: " + name, name.equals(name.toLowerCase()));
        }
        for (String name : cNames) {
            assertThat(allNames, hasItem(name));
        }
        for (String name : allNames) {
            assertThat(name, anyOf(isIn(lNames), isIn(cNames)));
        }
        assertEquals(lNames.size(), cNames.size());
    }
    
    @Test
    public void testRemovedSettings() {
        Builder cfgB = new Builder(VERSION_3_0_0);
        try {
            cfgB.setSetting("classic_compatible", "true");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), allOf(containsString("removed"), containsString("3.0.0")));
        }
        try {
            cfgB.setSetting("strict_syntax", "true");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), allOf(containsString("removed"), containsString("3.0.0")));
        }
    }

    @Test
    public void testCanBeBuiltOnlyOnce() {
        Builder builder = new Builder(VERSION_3_0_0);
        builder.build();
        try {
            builder.build();
            fail();
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void testCollectionSettingMutability() throws IOException {
        Builder cb = new Builder(VERSION_3_0_0);

        assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(cb.getSharedVariables()));
        Map<String, Object> mutableValue = new HashMap<>();
        mutableValue.put("x", "v1");
        cb.setSharedVariables(mutableValue);
        Map<String, Object> immutableValue = cb.getSharedVariables();
        assertNotSame(mutableValue, immutableValue); // Must be a copy
        assertTrue(_CollectionUtil.isMapKnownToBeUnmodifiable(immutableValue));
        assertEquals(mutableValue, immutableValue);
        mutableValue.put("y", "v2");
        assertNotEquals(mutableValue, immutableValue); // No aliasing
    }

    @Test
    public void testImpliedSettingValues()
            throws IOException, TemplateConfigurationFactoryException, UnregisteredOutputFormatException {
        Configuration cfg = new ImpliedSettingValuesTestBuilder().build();

        assertEquals("Y,N", cfg.getTemplateConfigurations().get("t.yn", null).getBooleanFormat());
        assertNotNull(cfg.getCustomNumberFormat("hex"));
        assertNotNull(cfg.getCustomDateFormat("epoch"));
        assertEquals(ImmutableMap.of("lib", "lib.ftl"), cfg.getAutoImports());
        assertEquals(ImmutableList.of("inc.ftl"), cfg.getAutoIncludes());
        assertEquals(ImmutableMap.of("v", 1), cfg.getSharedVariables());
        assertEquals(ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE),
                cfg.getRegisteredCustomOutputFormats());
        assertSame(CustomHTMLOutputFormat.INSTANCE, cfg.getOutputFormat("HTML"));
        assertSame(DummyOutputFormat.INSTANCE, cfg.getOutputFormat("dummy"));
        assertSame(XMLOutputFormat.INSTANCE, cfg.getOutputFormat("XML"));
    }

    @Test
    public void testImpliedSettingValues2()
            throws IOException, TemplateConfigurationFactoryException, UnregisteredOutputFormatException {
        Configuration cfg = new ImpliedSettingValuesTestBuilder()
                .templateConfigurations(
                        new ConditionalTemplateConfigurationFactory(
                                new FileNameGlobMatcher("*.jn"),
                                new TemplateConfiguration.Builder().booleanFormat("J,N").build()
                        )
                )
                .customNumberFormats(ImmutableMap.of("baseN", BaseNTemplateNumberFormatFactory.INSTANCE))
                .customDateFormats(ImmutableMap.of("epochDiv", EpochMillisDivTemplateDateFormatFactory.INSTANCE))
                .autoImports(ImmutableMap.of("lib2", "lib2.ftl"))
                .autoIncludes(ImmutableList.of("inc2.ftl"))
                .sharedVariables(ImmutableMap.of("v2", 2))
                .registeredCustomOutputFormats(
                        SeldomEscapedOutputFormat.INSTANCE, NameClashingDummyOutputFormat.INSTANCE)
                .build();

        TemplateConfigurationFactory tcf = cfg.getTemplateConfigurations();
        assertEquals("Y,N", tcf.get("t.yn", null).getBooleanFormat());
        assertEquals("J,N", tcf.get("t.jn", null).getBooleanFormat());

        assertNotNull(cfg.getCustomNumberFormat("hex"));
        assertNotNull(cfg.getCustomNumberFormat("baseN"));

        assertNotNull(cfg.getCustomDateFormat("epoch"));
        assertNotNull(cfg.getCustomDateFormat("epochDiv"));

        assertEquals(ImmutableMap.of("lib", "lib.ftl", "lib2", "lib2.ftl"), cfg.getAutoImports());

        assertEquals(ImmutableList.of("inc.ftl", "inc2.ftl"), cfg.getAutoIncludes());

        assertEquals(ImmutableMap.of("v", 1, "v2", 2), cfg.getSharedVariables());

        assertEquals(
                ImmutableList.of(
                        CustomHTMLOutputFormat.INSTANCE,
                        SeldomEscapedOutputFormat.INSTANCE,
                        NameClashingDummyOutputFormat.INSTANCE),
                cfg.getRegisteredCustomOutputFormats());
        assertSame(CustomHTMLOutputFormat.INSTANCE, cfg.getOutputFormat("HTML"));
        assertSame(NameClashingDummyOutputFormat.INSTANCE, cfg.getOutputFormat("dummy"));
        assertSame(SeldomEscapedOutputFormat.INSTANCE, cfg.getOutputFormat("seldomEscaped"));
        assertSame(XMLOutputFormat.INSTANCE, cfg.getOutputFormat("XML"));
    }

    @SuppressWarnings("boxing")
    private void assertStartsWith(List<String> list, List<String> headList) {
        int index = 0;
        for (String name : headList) {
            assertThat(index, lessThan(list.size()));
            assertEquals(name, list.get(index));
            index++;
        }
    }

    private boolean keyFieldExists(String name) throws Exception {
        Field field;
        try {
            field = ExtendableBuilder.class.getField(name.toUpperCase() + "_KEY");
        } catch (NoSuchFieldException e) {
            return false;
        }
        assertEquals(name, field.get(null));
        return true;
    }
    
    private static class MyScalarModel implements TemplateScalarModel {

        @Override
        public String getAsString() throws TemplateModelException {
            return "my";
        }
        
    }

    private static class ImpliedSettingValuesTestBuilder
            extends Configuration.ExtendableBuilder<ImpliedSettingValuesTestBuilder> {

        ImpliedSettingValuesTestBuilder() {
            super(VERSION_3_0_0);
        }

        @Override
        protected TemplateConfigurationFactory getImpliedTemplateConfigurations() {
            return new ConditionalTemplateConfigurationFactory(
                    new FileNameGlobMatcher("*.yn"),
                    new TemplateConfiguration.Builder().booleanFormat("Y,N").build());
        }

        @Override
        protected Map<String, TemplateNumberFormatFactory> getImpliedCustomNumberFormats() {
            return ImmutableMap.<String, TemplateNumberFormatFactory>of(
                    "hex", HexTemplateNumberFormatFactory.INSTANCE);
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getImpliedCustomDateFormats() {
            return ImmutableMap.<String, TemplateDateFormatFactory>of(
                    "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE);
        }

        @Override
        protected Map<String, String> getImpliedAutoImports() {
            return ImmutableMap.of("lib", "lib.ftl");
        }

        @Override
        protected List<String> getImpliedAutoIncludes() {
            return ImmutableList.of("inc.ftl");
        }

        @Override
        protected Map<String, Object> getImpliedSharedVariables() {
            return ImmutableMap.<String, Object>of("v", 1);
        }

        @Override
        protected Collection<OutputFormat> getImpliedRegisteredCustomOutputFormats() {
            return ImmutableList.<OutputFormat>of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE);
        }
    }

}

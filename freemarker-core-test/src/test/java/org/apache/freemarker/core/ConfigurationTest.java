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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.CacheStorageWithGetSize;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
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
import org.apache.freemarker.core.util._DateUtil;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.util._StringUtil;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import junit.framework.TestCase;

public class ConfigurationTest extends TestCase {

    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

    public ConfigurationTest(String name) {
        super(name);
    }

    public void testUnsetAndIsSet() throws Exception {
        Configuration.ExtendableBuilder<?> cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        assertFalse(cfgB.isLogTemplateExceptionsSet());
        assertFalse(cfgB.getLogTemplateExceptions());
        //
        cfgB.setLogTemplateExceptions(true);
        {
            assertTrue(cfgB.isLogTemplateExceptionsSet());
            assertTrue(cfgB.getLogTemplateExceptions());
        }
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetLogTemplateExceptions();
            assertFalse(cfgB.isLogTemplateExceptionsSet());
            assertFalse(cfgB.getLogTemplateExceptions());
        }

        DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        assertFalse(cfgB.isObjectWrapperSet());
        assertSame(dow, cfgB.getObjectWrapper());
        //
        RestrictedObjectWrapper ow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
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
        assertSame(TemplateExceptionHandler.DEBUG_HANDLER, cfgB.getTemplateExceptionHandler());
        //
        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        assertTrue(cfgB.isTemplateExceptionHandlerSet());
        assertSame(TemplateExceptionHandler.RETHROW_HANDLER, cfgB.getTemplateExceptionHandler());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateExceptionHandler();
            assertFalse(cfgB.isTemplateExceptionHandlerSet());
            assertSame(TemplateExceptionHandler.DEBUG_HANDLER, cfgB.getTemplateExceptionHandler());
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
        
        assertFalse(cfgB.isCacheStorageSet());
        assertTrue(cfgB.getCacheStorage() instanceof SoftCacheStorage);
        //
        cfgB.setCacheStorage(NullCacheStorage.INSTANCE);
        assertTrue(cfgB.isCacheStorageSet());
        assertSame(NullCacheStorage.INSTANCE, cfgB.getCacheStorage());
        //
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                cfgB.setCacheStorage(cfgB.getCacheStorage());
            }
            cfgB.unsetCacheStorage();
            assertFalse(cfgB.isCacheStorageSet());
            assertTrue(cfgB.getCacheStorage() instanceof SoftCacheStorage);
        }
    }
    
    public void testTemplateLoadingErrors() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(getClass(), "nosuchpackage"))
                .build();
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), not(containsString("wasn't set")));
        }
    }
    
    public void testVersion() {
        Version v = Configuration.getVersion();
        assertTrue(v.intValue() >= _CoreAPI.VERSION_INT_3_0_0);
        
        try {
            new Configuration.Builder(new Version(999, 1, 2));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }
        
        try {
            new Configuration.Builder(new Version(2, 3, 0));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("3.0.0"));
        }
    }
    
    public void testShowErrorTips() throws Exception {
        try {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
            new Template(null, "${x}", cfg).process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("Tip:"));
        }

        try {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).showErrorTips(false).build();
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

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
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
    
    public void testTemplateResolverCache() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfgB.getCacheStorage();
        assertEquals(0, cache.getSize());
        cfgB.setCacheStorage(new StrongCacheStorage());
        cache = (CacheStorageWithGetSize) cfgB.getCacheStorage();
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

    public void testTemplateNameFormat() throws Exception {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("a/b.ftl", "In a/b.ftl");
        tl.putTemplate("b.ftl", "In b.ftl");

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateNameFormat(DefaultTemplateNameFormatFM2.INSTANCE)
                    .build();
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("a/b.ftl", template.getLookupName());
            assertEquals("a/b.ftl", template.getSourceName());
            assertEquals("In a/b.ftl", template.toString());
        }
        
        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateNameFormat(DefaultTemplateNameFormat.INSTANCE)
                    .build();
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("b.ftl", template.getLookupName());
            assertEquals("b.ftl", template.getSourceName());
            assertEquals("In b.ftl", template.toString());
        }
    }

    public void testTemplateNameFormatSetSetting() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY, "defAult_2_4_0");
        assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY, "defaUlt_2_3_0");
        assertSame(DefaultTemplateNameFormatFM2.INSTANCE, cfgB.getTemplateNameFormat());
        assertTrue(cfgB.isTemplateNameFormatSet());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_NAME_FORMAT_KEY, "defauLt");
        assertFalse(cfgB.isTemplateNameFormatSet());
    }

    public void testObjectWrapperSetSetting() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        {
            cfgB.setSetting(MutableProcessingConfiguration.OBJECT_WRAPPER_KEY, "defAult");
            DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
            assertSame(dow, cfgB.getObjectWrapper());
            assertEquals(Configuration.VERSION_3_0_0, dow.getIncompatibleImprovements());
        }
        
        {
            cfgB.setSetting(MutableProcessingConfiguration.OBJECT_WRAPPER_KEY, "restricted");
            assertThat(cfgB.getObjectWrapper(), instanceOf(RestrictedObjectWrapper.class));
        }
    }
    
    public void testTemplateLookupStrategyDefault() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""))
                .build();
        assertSame(DefaultTemplateLookupStrategy.INSTANCE, cfg.getTemplateLookupStrategy());
        assertEquals("toCache1.ftl", cfg.getTemplate("toCache1.ftl").getSourceName());
    }

    public void testTemplateLookupStrategyCustom() throws Exception {
        final TemplateLookupStrategy myStrategy = new TemplateLookupStrategy() {
            @Override
            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                return ctx.lookupWithAcquisitionStrategy("toCache2.ftl");
            }
        };

        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""))
                .templateLookupStrategy(myStrategy)
                .build();
        assertSame(myStrategy, cfg.getTemplateLookupStrategy());
        assertEquals("toCache2.ftl", cfg.getTemplate("toCache1.ftl").getSourceName());
    }
    
    public void testSetTemplateConfigurations() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertNull(cfgB.getTemplateConfigurations());

        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("t.de.ftlh", "");
        tl.putTemplate("t.fr.ftlx", "");
        tl.putTemplate("t.ftlx", "");
        tl.putTemplate("Stat/t.de.ftlx", "");
        cfgB.setTemplateLoader(tl);
        
        cfgB.setTimeZone(TimeZone.getTimeZone("GMT+09"));
        
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_CONFIGURATIONS_KEY,
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
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_CONFIGURATIONS_KEY, "null");
        assertNull(cfgB.getTemplateConfigurations());
    }

    public void testSetAutoEscaping() throws Exception {
       Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
    
       assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

       cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_SUPPORTED);
       assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, cfgB.getAutoEscapingPolicy());

       cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_DEFAULT);
       assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

       cfgB.setAutoEscapingPolicy(AutoEscapingPolicy.DISABLE);
       assertEquals(AutoEscapingPolicy.DISABLE, cfgB.getAutoEscapingPolicy());
       
       cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enableIfSupported");
       assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, cfgB.getAutoEscapingPolicy());

       cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enable_if_supported");
       assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, cfgB.getAutoEscapingPolicy());
       
       cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enableIfDefault");
       assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());

       cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enable_if_default");
       assertEquals(AutoEscapingPolicy.ENABLE_IF_DEFAULT, cfgB.getAutoEscapingPolicy());
       
       cfgB.setSetting(Configuration.ExtendableBuilder.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "disable");
       assertEquals(AutoEscapingPolicy.DISABLE, cfgB.getAutoEscapingPolicy());
    }

    public void testSetOutputFormat() throws Exception {
       Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
       
       assertEquals(UndefinedOutputFormat.INSTANCE, cfgB.getOutputFormat());
       assertFalse(cfgB.isOutputFormatSet());
       
       try {
           cfgB.setOutputFormat(null);
           fail();
       } catch (_NullArgumentException e) {
           // Expected
       }
       
       assertFalse(cfgB.isOutputFormatSet());
       
       cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY_CAMEL_CASE, XMLOutputFormat.class.getSimpleName());
       assertEquals(XMLOutputFormat.INSTANCE, cfgB.getOutputFormat());
       
       cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY_SNAKE_CASE, HTMLOutputFormat.class.getSimpleName());
       assertEquals(HTMLOutputFormat.INSTANCE, cfgB.getOutputFormat());
       
       cfgB.unsetOutputFormat();
       assertEquals(UndefinedOutputFormat.INSTANCE, cfgB.getOutputFormat());
       assertFalse(cfgB.isOutputFormatSet());
       
       cfgB.setOutputFormat(UndefinedOutputFormat.INSTANCE);
       assertTrue(cfgB.isOutputFormatSet());
       cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY_CAMEL_CASE, "default");
       assertFalse(cfgB.isOutputFormatSet());
       
       try {
           cfgB.setSetting(Configuration.ExtendableBuilder.OUTPUT_FORMAT_KEY, "null");
       } catch (ConfigurationSettingValueException e) {
           assertThat(e.getCause().getMessage(), containsString(UndefinedOutputFormat.class.getSimpleName()));
       }
    }
    
    @Test
    public void testGetOutputFormatByName() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
        
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

    public void testSetRegisteredCustomOutputFormats() throws Exception {
        Configuration.Builder cfg = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        assertTrue(cfg.getRegisteredCustomOutputFormats().isEmpty());
        
        cfg.setSetting(Configuration.ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
                "[org.apache.freemarker.core.userpkg.CustomHTMLOutputFormat(), "
                + "org.apache.freemarker.core.userpkg.DummyOutputFormat()]");
        assertEquals(
                ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE),
                new ArrayList(cfg.getRegisteredCustomOutputFormats()));
        
        try {
            cfg.setSetting(Configuration.ExtendableBuilder.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE, "[TemplateConfiguration()]");
            fail();
        } catch (ConfigurationSettingValueException e) {
            assertThat(e.getMessage(), containsString(OutputFormat.class.getSimpleName()));
        }
    }

    public void testSetRecognizeStandardFileExtensions() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
     
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());

        cfgB.setRecognizeStandardFileExtensions(false);
        assertFalse(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());
     
        cfgB.unsetRecognizeStandardFileExtensions();
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());
        
        cfgB.setRecognizeStandardFileExtensions(true);
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());
     
        cfgB.setSetting(Configuration.ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE, "false");
        assertFalse(cfgB.getRecognizeStandardFileExtensions());
        assertTrue(cfgB.isRecognizeStandardFileExtensionsSet());
        
        cfgB.setSetting(Configuration.ExtendableBuilder.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE, "default");
        assertTrue(cfgB.getRecognizeStandardFileExtensions());
        assertFalse(cfgB.isRecognizeStandardFileExtensionsSet());
     }
    
    public void testSetTimeZone() throws ConfigurationException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);
            
            Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
            assertEquals(sysDefTZ, cfgB.getTimeZone());
            cfgB.setSetting(MutableProcessingConfiguration.TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfgB.getTimeZone());
            
            TimeZone newSysDefTZ = TimeZone.getTimeZone("GMT+09");
            TimeZone.setDefault(newSysDefTZ);
            assertEquals(sysDefTZ, cfgB.getTimeZone());
            cfgB.setSetting(MutableProcessingConfiguration.TIME_ZONE_KEY, "JVM default");
            assertEquals(newSysDefTZ, cfgB.getTimeZone());
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }
    
    public void testSetSQLDateAndTimeTimeZone() throws ConfigurationException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);
            
            Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
            assertNull(cfgB.getSQLDateAndTimeTimeZone());
            
            cfgB.setSQLDateAndTimeTimeZone(null);
            assertNull(cfgB.getSQLDateAndTimeTimeZone());
            
            cfgB.setSetting(MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfgB.getSQLDateAndTimeTimeZone());
            
            cfgB.setSetting(MutableProcessingConfiguration.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "null");
            assertNull(cfgB.getSQLDateAndTimeTimeZone());
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }

    public void testTimeZoneLayers() throws Exception {
        TimeZone localTZ = TimeZone.getTimeZone("Europe/Brussels");

        {
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
            Template t = new Template(null, "", cfg);
            Environment env1 = t.createProcessingEnvironment(null, new StringWriter());
            Environment env2 = t.createProcessingEnvironment(null, new StringWriter());

            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(TimeZone.getDefault(), env1.getTimeZone());
            assertNull(env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(TimeZone.getDefault(), env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());

            env1.setSQLDateAndTimeTimeZone(_DateUtil.UTC);
            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(TimeZone.getDefault(), env1.getTimeZone());
            assertEquals(_DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());

            env1.setTimeZone(localTZ);
            // cfg:
            assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(localTZ, env1.getTimeZone());
            assertEquals(_DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(TimeZone.getDefault(), env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());
        }

        {
            TimeZone otherTZ1 = TimeZone.getTimeZone("GMT+05");
            TimeZone otherTZ2 = TimeZone.getTimeZone("GMT+06");
            Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                    .timeZone(otherTZ1)
                    .sqlDateAndTimeTimeZone(otherTZ2)
                    .build();

            Template t = new Template(null, "", cfg);
            Environment env1 = t.createProcessingEnvironment(null, new StringWriter());
            Environment env2 = t.createProcessingEnvironment(null, new StringWriter());

            env1.setTimeZone(localTZ);
            env1.setSQLDateAndTimeTimeZone(_DateUtil.UTC);

            // cfg:
            assertEquals(otherTZ1, cfg.getTimeZone());
            assertEquals(otherTZ2, cfg.getSQLDateAndTimeTimeZone());
            // env:
            assertEquals(localTZ, env1.getTimeZone());
            assertEquals(_DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());
            // env 2:
            assertEquals(otherTZ1, env2.getTimeZone());
            assertEquals(otherTZ2, env2.getSQLDateAndTimeTimeZone());

            try {
                setTimeZoneToNull(env2);
                fail();
            } catch (IllegalArgumentException e) {
                // expected
            }
            env2.setSQLDateAndTimeTimeZone(null);
            assertEquals(otherTZ1, env2.getTimeZone());
            assertNull(env2.getSQLDateAndTimeTimeZone());
        }
    }

    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="Expected to fail")
    private void setTimeZoneToNull(Environment env2) {
        env2.setTimeZone(null);
    }
    
    public void testSetICIViaSetSettingAPI() throws ConfigurationException {
        Configuration.Builder cfg = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertEquals(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS, cfg.getIncompatibleImprovements());
        // This is the only valid value ATM:
        cfg.setSetting(Configuration.ExtendableBuilder.INCOMPATIBLE_IMPROVEMENTS_KEY, "3.0.0");
        assertEquals(Configuration.VERSION_3_0_0, cfg.getIncompatibleImprovements());
    }

    public void testSetLogTemplateExceptionsViaSetSettingAPI() throws ConfigurationException {
        Configuration.Builder cfg = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertFalse(cfg.getLogTemplateExceptions());
        cfg.setSetting(MutableProcessingConfiguration.LOG_TEMPLATE_EXCEPTIONS_KEY, "true");
        assertTrue(cfg.getLogTemplateExceptions());
    }
    
    public void testSharedVariables() throws TemplateException, IOException {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
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
    public void testApiBuiltinEnabled() throws Exception {
        try {
            new Template(
                    null, "${1?api}",
                    new Configuration.Builder(Configuration.VERSION_3_0_0).build())
                    .process(null, _NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString(MutableProcessingConfiguration.API_BUILTIN_ENABLED_KEY));
        }
            
        new Template(
                null, "${m?api.hashCode()}",
                new Configuration.Builder(Configuration.VERSION_3_0_0).apiBuiltinEnabled(true).build())
                .process(Collections.singletonMap("m", new HashMap()), _NullWriter.INSTANCE);
    }

    @Test
    public void testTemplateUpdateDelay() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(
                DefaultTemplateResolver.DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS,
                (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setTemplateUpdateDelayMilliseconds(4000L);
        assertEquals(4000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setTemplateUpdateDelayMilliseconds(100L);
        assertEquals(100L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        try {
            cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "5");
            assertEquals(5000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        } catch (ConfigurationSettingValueException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("unit must be specified"));
        }
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "0");
        assertEquals(0L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        try {
            cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "5 foo");
            assertEquals(5000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        } catch (ConfigurationSettingValueException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("\"foo\""));
        }
        
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "3 ms");
        assertEquals(3L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "4ms");
        assertEquals(4L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "3 s");
        assertEquals(3000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "4s");
        assertEquals(4000L, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "3 m");
        assertEquals(1000L * 60 * 3, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "4m");
        assertEquals(1000L * 60 * 4, (Object) cfgB.getTemplateUpdateDelayMilliseconds());

        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "1 h");
        assertEquals(1000L * 60 * 60, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
        cfgB.setSetting(Configuration.ExtendableBuilder.TEMPLATE_UPDATE_DELAY_KEY, "2h");
        assertEquals(1000L * 60 * 60 * 2, (Object) cfgB.getTemplateUpdateDelayMilliseconds());
    }
    
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS ", justification = "Testing wrong args")
    public void testSetCustomNumberFormat() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        try {
            cfgB.setCustomNumberFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "a_b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfgB.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>singletonMap(
                    "a b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }
        
        try {
            cfgB.setCustomNumberFormats(ImmutableMap.<String, TemplateNumberFormatFactory>of(
                    "a", HexTemplateNumberFormatFactory.INSTANCE,
                    "@wrong", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE,
                "{ 'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("base", BaseNTemplateNumberFormatFactory.INSTANCE),
                cfgB.getCustomNumberFormats());
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE,
                "{ "
                + "'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "(), "
                + "'hex': " + HexTemplateNumberFormatFactory.class.getName() + "()"
                + " }");
        assertEquals(
                ImmutableMap.of(
                        "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                        "hex", HexTemplateNumberFormatFactory.INSTANCE),
                cfgB.getCustomNumberFormats());
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfgB.getCustomNumberFormats());
        
        try {
            cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE,
                    "{ 'x': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(EpochMillisTemplateDateFormatFactory.class.getName()),
                    containsString(TemplateNumberFormatFactory.class.getName())));
        }
    }

    @Test
    public void testSetTabSize() throws Exception {
        String ftl = "${\t}";
        
        try {
            new Template(null, ftl,
                    new Configuration.Builder(Configuration.VERSION_3_0_0).build());
            fail();
        } catch (ParseException e) {
            assertEquals(9, e.getColumnNumber());
        }
        
        try {
            new Template(null, ftl,
                    new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(1).build());
            fail();
        } catch (ParseException e) {
            assertEquals(4, e.getColumnNumber());
        }
        
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).tabSize(257);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testTabSizeSetting() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertEquals(8, cfgB.getTabSize());
        cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY_CAMEL_CASE, "4");
        assertEquals(4, cfgB.getTabSize());
        cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY_SNAKE_CASE, "1");
        assertEquals(1, cfgB.getTabSize());
        
        try {
            cfgB.setSetting(Configuration.ExtendableBuilder.TAB_SIZE_KEY_SNAKE_CASE, "x");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause(), instanceOf(NumberFormatException.class));
        }
    }
    
    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="We test failures")
    @Test
    public void testSetCustomDateFormat() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        try {
            cfgB.setCustomDateFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }
        
        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "a_b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfgB.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>singletonMap(
                    "a b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }
        
        try {
            cfgB.setCustomDateFormats(ImmutableMap.<String, TemplateDateFormatFactory>of(
                    "a", EpochMillisTemplateDateFormatFactory.INSTANCE,
                    "@wrong", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE,
                "{ 'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("epoch", EpochMillisTemplateDateFormatFactory.INSTANCE),
                cfgB.getCustomDateFormats());
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE,
                "{ "
                + "'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "(), "
                + "'epochDiv': " + EpochMillisDivTemplateDateFormatFactory.class.getName() + "()"
                + " }");
        assertEquals(
                ImmutableMap.of(
                        "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                        "epochDiv", EpochMillisDivTemplateDateFormatFactory.INSTANCE),
                cfgB.getCustomDateFormats());
        
        cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfgB.getCustomDateFormats());
        
        try {
            cfgB.setSetting(MutableProcessingConfiguration.CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE,
                    "{ 'x': " + HexTemplateNumberFormatFactory.class.getName() + "() }");
            fail();
        } catch (ConfigurationException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(HexTemplateNumberFormatFactory.class.getName()),
                    containsString(TemplateDateFormatFactory.class.getName())));
        }
    }

    public void testNamingConventionSetSetting() throws ConfigurationException {
        Configuration.Builder cfg = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(NamingConvention.AUTO_DETECT, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "legacy");
        assertEquals(NamingConvention.LEGACY, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "camel_case");
        assertEquals(NamingConvention.CAMEL_CASE, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "auto_detect");
        assertEquals(NamingConvention.AUTO_DETECT, cfg.getNamingConvention());
    }

    public void testLazyImportsSetSetting() throws ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertFalse(cfgB.getLazyImports());
        assertFalse(cfgB.isLazyImportsSet());
        cfgB.setSetting("lazy_imports", "true");
        assertTrue(cfgB.getLazyImports());
        cfgB.setSetting("lazyImports", "false");
        assertFalse(cfgB.getLazyImports());
        assertTrue(cfgB.isLazyImportsSet());
    }
    
    public void testLazyAutoImportsSetSetting() throws ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertNull(cfgB.getLazyAutoImports());
        assertFalse(cfgB.isLazyAutoImportsSet());
        cfgB.setSetting("lazy_auto_imports", "true");
        assertEquals(Boolean.TRUE, cfgB.getLazyAutoImports());
        assertTrue(cfgB.isLazyAutoImportsSet());
        cfgB.setSetting("lazyAutoImports", "false");
        assertEquals(Boolean.FALSE, cfgB.getLazyAutoImports());
        cfgB.setSetting("lazyAutoImports", "null");
        assertNull(cfgB.getLazyAutoImports());
        assertTrue(cfgB.isLazyAutoImportsSet());
        cfgB.unsetLazyAutoImports();
        assertNull(cfgB.getLazyAutoImports());
        assertFalse(cfgB.isLazyAutoImportsSet());
    }

    public void testLocaleSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertFalse(cfgB.isLocaleSet());

        Locale nonDefault = Locale.getDefault().equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
        cfgB.setLocale(nonDefault);
        assertTrue(cfgB.isLocaleSet());
        assertEquals(nonDefault, cfgB.getLocale());

        cfgB.unsetLocale();
        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertFalse(cfgB.isLocaleSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.LOCALE_KEY, "JVM default");
        assertEquals(Locale.getDefault(), cfgB.getLocale());
        assertTrue(cfgB.isLocaleSet());
    }

    public void testDefaultEncodingSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(Charset.defaultCharset(), cfgB.getSourceEncoding());
        assertFalse(cfgB.isSourceEncodingSet());

        Charset nonDefault = Charset.defaultCharset().equals(StandardCharsets.UTF_8) ? StandardCharsets.ISO_8859_1
                : StandardCharsets.UTF_8;
        cfgB.setSourceEncoding(nonDefault);
        assertTrue(cfgB.isSourceEncodingSet());
        assertEquals(nonDefault, cfgB.getSourceEncoding());

        cfgB.unsetSourceEncoding();
        assertEquals(Charset.defaultCharset(), cfgB.getSourceEncoding());
        assertFalse(cfgB.isSourceEncodingSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY, "JVM default");
        assertEquals(Charset.defaultCharset(), cfgB.getSourceEncoding());
        assertTrue(cfgB.isSourceEncodingSet());
    }

    public void testTimeZoneSetting() throws TemplateException, ConfigurationException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);

        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertFalse(cfgB.isTimeZoneSet());

        TimeZone nonDefault = TimeZone.getDefault().equals(_DateUtil.UTC) ? TimeZone.getTimeZone("PST") : _DateUtil.UTC;
        cfgB.setTimeZone(nonDefault);
        assertTrue(cfgB.isTimeZoneSet());
        assertEquals(nonDefault, cfgB.getTimeZone());

        cfgB.unsetTimeZone();
        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertFalse(cfgB.isTimeZoneSet());

        cfgB.setSetting(Configuration.ExtendableBuilder.TIME_ZONE_KEY, "JVM default");
        assertEquals(TimeZone.getDefault(), cfgB.getTimeZone());
        assertTrue(cfgB.isTimeZoneSet());
    }

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
        for (boolean camelCase : new boolean[] { false, true }) {
            List<String> names = new ArrayList<>(Configuration.Builder.getSettingNames(camelCase));
            List<String> procCfgNames = new ArrayList<>(new Template(null, "", cfg)
                    .createProcessingEnvironment(null, _NullWriter.INSTANCE)
                    .getSettingNames(camelCase));
            assertStartsWith(names, procCfgNames);
            
            String prevName = null;
            for (int i = procCfgNames.size(); i < names.size(); i++) {
                String name = names.get(i);
                if (prevName != null) {
                    assertThat(name, greaterThan(prevName));
                }
                prevName = name;
            }
        }
    }

    @Test
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void testGetSettingNamesNameConventionsContainTheSame() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        ConfigurableTest.testGetSettingNamesNameConventionsContainTheSame(
                new ArrayList<>(cfgB.getSettingNames(false)),
                new ArrayList<>(cfgB.getSettingNames(true)));
    }

    @Test
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void testStaticFieldKeysCoverAllGetSettingNames() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        List<String> names = new ArrayList<>(cfgB.getSettingNames(false));
        List<String> cfgableNames = new ArrayList<>(cfgB.getSettingNames(false));
        assertStartsWith(names, cfgableNames);
        
        for (int i = cfgableNames.size(); i < names.size(); i++) {
            String name = names.get(i);
            assertTrue("No field was found for " + name, keyFieldExists(name));
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllStaticKeyFields() throws Exception {
        Collection<String> names = new Configuration.Builder(Configuration.VERSION_3_0_0).getSettingNames(false);
        
        for (Class<? extends MutableProcessingConfiguration> cfgableClass : new Class[] { Configuration.class, MutableProcessingConfiguration.class }) {
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
        ConfigurableTest.testKeyStaticFieldsHasAllVariationsAndCorrectFormat(Configuration.ExtendableBuilder.class);
    }

    @Test
    public void testGetSettingNamesCoversAllSettingNames() throws Exception {
        Collection<String> names = new Configuration.Builder(Configuration.VERSION_3_0_0).getSettingNames(false);
        
        for (Field f : MutableProcessingConfiguration.class.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final Object name = f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
            }
        }
    }

    @Test
    public void testSetSettingSupportsBothNamingConventions() throws Exception {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        
        cfgB.setSetting(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY_CAMEL_CASE, StandardCharsets.UTF_16LE.name());
        assertEquals(StandardCharsets.UTF_16LE, cfgB.getSourceEncoding());
        cfgB.setSetting(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY_SNAKE_CASE, StandardCharsets.UTF_8.name());
        assertEquals(StandardCharsets.UTF_8, cfgB.getSourceEncoding());
        
        for (String nameCC : cfgB.getSettingNames(true)) {
            for (String value : new String[] { "1", "default", "true" }) {
                Exception resultCC = null;
                try {
                    cfgB.setSetting(nameCC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(UnknownConfigurationSettingException.class)));
                    resultCC = e;
                }
                
                String nameSC = _StringUtil.camelCaseToUnderscored(nameCC);
                Exception resultSC = null;
                try {
                    cfgB.setSetting(nameSC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(UnknownConfigurationSettingException.class)));
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
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
        
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
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();
        
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
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
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
            field = Configuration.class.getField(name.toUpperCase() + "_KEY");
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
    
}

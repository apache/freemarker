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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.*;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.UnregisteredOutputFormatException;
import org.apache.freemarker.core.outputformat.impl.CombinedMarkupOutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.RTFOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;
import org.apache.freemarker.core.templateresolver.*;
import org.apache.freemarker.core.templateresolver.impl.*;
import org.apache.freemarker.core.userpkg.*;
import org.apache.freemarker.core.util._CollectionUtils;
import org.apache.freemarker.core.util._DateUtils;
import org.apache.freemarker.core.util._NullWriter;
import org.apache.freemarker.core.valueformat.TemplateDateFormatFactory;
import org.apache.freemarker.core.valueformat.TemplateNumberFormatFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.freemarker.core.Configuration.*;
import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;
import static org.apache.freemarker.test.hamcerst.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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
        assertSame(TemplateExceptionHandler.RETHROW, cfgB.getTemplateExceptionHandler());
        //
        cfgB.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG);
        assertTrue(cfgB.isTemplateExceptionHandlerSet());
        assertSame(TemplateExceptionHandler.DEBUG, cfgB.getTemplateExceptionHandler());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateExceptionHandler();
            assertFalse(cfgB.isTemplateExceptionHandlerSet());
            assertSame(TemplateExceptionHandler.RETHROW, cfgB.getTemplateExceptionHandler());
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
        assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
        //
        cfgB.setTemplateNameFormat(null);
        assertTrue(cfgB.isTemplateNameFormatSet());
        assertNull(cfgB.getTemplateNameFormat());
        //
        for (int i = 0; i < 2; i++) {
            cfgB.unsetTemplateNameFormat();
            assertFalse(cfgB.isTemplateNameFormatSet());
            assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
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
            cfg.getTemplate("missing.f3ah");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), not(containsString("wasn't set")));
        }
    }

    @Test
    public void testVersion() {
        Version v = getVersion();
        assertTrue(v.intValue() >= _VersionInts.VERSION_INT_3_0_0);
        
        try {
            new Builder(new Version(999, 1, 2)).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }
        
        try {
            new Builder(new Version(2, 3, 0)).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("3.0.0"));
        }

        try {
            new Builder(Configuration.getVersion()).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("getVersion()"));
        }
        new Builder(new Version(Configuration.getVersion().toString()));
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
        final String tFtl = "t.f3ah";
        final String tHuFtl = "t_hu.f3ah";
        final String tEnFtl = "t_en.f3ah";
        final String tUtf8Ftl = "utf8.f3ah";
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
            Template t = cfg.getTemplate(tFtl, null);
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
            cfg.getTemplate("missing.f3ah", hu, custLookupCond, false);
            fail();
        } catch (TemplateNotFoundException e) {
            // Expected
        }
        assertNull(cfg.getTemplate("missing.f3ah", hu, custLookupCond, true));
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
        cfg.getTemplate("toCache1.f3ah");
        assertEquals(1, cache.getSize());
        cfg.getTemplate("toCache2.f3ah");
        assertEquals(2, cache.getSize());
        cfg.clearTemplateCache();
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.f3ah");
        assertEquals(1, cache.getSize());
        cfgB.setTemplateLoader(cfgB.getTemplateLoader());
        assertEquals(1, cache.getSize());
    }

    @Test
    public void testTemplateNameFormat() throws Exception {
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("a/b.f3ah", "In a/b.f3ah");
        tl.putTemplate("b.f3ah", "In b.f3ah");

        {
            Configuration cfg = new Builder(VERSION_3_0_0)
                    .templateLoader(tl)
                    .templateNameFormat(DefaultTemplateNameFormat.INSTANCE)
                    .build();
            final Template template = cfg.getTemplate("a/./../b.f3ah");
            assertEquals("b.f3ah", template.getLookupName());
            assertEquals("b.f3ah", template.getSourceName());
            assertEquals("In b.f3ah", template.toString());
        }
    }

    @Test
    public void testTemplateNameFormatSetSetting() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        assertSame(DefaultTemplateNameFormat.INSTANCE, cfgB.getTemplateNameFormat());
        cfgB.setTemplateNameFormat(null);
        assertNull(cfgB.getTemplateNameFormat());
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
        assertEquals("toCache1.f3ah", cfg.getTemplate("toCache1.f3ah").getSourceName());
    }

    @Test
    public void testTemplateLookupStrategyCustom() throws Exception {
        final TemplateLookupStrategy myStrategy = new TemplateLookupStrategy() {
            @Override
            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                return ctx.lookupWithAcquisitionStrategy("toCache2.f3ah");
            }
        };

        Configuration cfg = new Builder(VERSION_3_0_0)
                .templateLoader(new ClassTemplateLoader(ConfigurationTest.class, ""))
                .templateLookupStrategy(myStrategy)
                .build();
        assertSame(myStrategy, cfg.getTemplateLookupStrategy());
        assertEquals("toCache2.f3ah", cfg.getTemplate("toCache1.f3ah").getSourceName());
    }

    @Test
    public void testSetTemplateConfigurations() throws Exception {
        Builder cfgB = new Builder(VERSION_3_0_0);
        assertNull(cfgB.getTemplateConfigurations());

        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("t.de.f3ah", "");
        tl.putTemplate("t.fr.f3ax", "");
        tl.putTemplate("t.f3ax", "");
        tl.putTemplate("Stat/t.de.f3ax", "");
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
                            + "FileExtensionMatcher('f3ah'), TemplateConfiguration(booleanFormat='TODO,HTML')), "
                        + "ConditionalTemplateConfigurationFactory("
                            + "FileExtensionMatcher('f3ax'), TemplateConfiguration(booleanFormat='TODO,XML')), "
                        + "noMatchErrorDetails='Unrecognized template file extension'"
                    + "), "
                    + "ConditionalTemplateConfigurationFactory("
                        + "PathGlobMatcher('stat/**', caseInsensitive=true), "
                        + "TemplateConfiguration(timeZone=TimeZone('UTC'))"
                    + ")"
                + ")");

        Configuration cfg = cfgB.build();
        {
            Template t = cfg.getTemplate("t.de.f3ah");
            assertEquals("TODO,HTML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT+01"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("t.fr.f3ax");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("t.f3ax");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(TimeZone.getTimeZone("GMT+09"), t.getTimeZone());
        }
        {
            Template t = cfg.getTemplate("Stat/t.de.f3ax");
            assertEquals("TODO,XML", t.getBooleanFormat());
            assertEquals(_DateUtils.UTC, t.getTimeZone());
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
        
        cfg.setSetting(REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY,
                "[org.apache.freemarker.core.userpkg.CustomHTMLOutputFormat(), "
                + "org.apache.freemarker.core.userpkg.DummyOutputFormat()]");
        assertEquals(
                ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE),
                new ArrayList(cfg.getRegisteredCustomOutputFormats()));
        
        try {
            cfg.setSetting(REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY, "[TemplateConfiguration()]");
            fail();
        } catch (InvalidSettingValueException e) {
            assertThat(e.getMessage(), containsString(OutputFormat.class.getSimpleName()));
        }
    }

    @Test
    public void testSetICIViaSetSettingAPI() throws ConfigurationException {
        Builder cfgB = new Builder(VERSION_3_0_0);
        assertEquals(DEFAULT_INCOMPATIBLE_IMPROVEMENTS, cfgB.getIncompatibleImprovements());
        // This is the only valid value ATM:
        cfgB.setSetting(MutableParsingAndProcessingConfiguration.INCOMPATIBLE_IMPROVEMENTS_KEY, "3.0.0");
        assertEquals(VERSION_3_0_0, cfgB.getIncompatibleImprovements());
    }

    @Test
    public void testSetAttemptExceptionReporter() throws TemplateException {
        Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0);
        assertEquals(AttemptExceptionReporter.LOG_ERROR, cfgB.getAttemptExceptionReporter());
        assertFalse(cfgB.isAttemptExceptionReporterSet());
        cfgB.setSetting(MutableProcessingConfiguration.ATTEMPT_EXCEPTION_REPORTER_KEY, "logWarn");
        assertEquals(AttemptExceptionReporter.LOG_WARN, cfgB.getAttemptExceptionReporter());
        assertTrue(cfgB.isAttemptExceptionReporterSet());
        cfgB.setSetting(MutableProcessingConfiguration.ATTEMPT_EXCEPTION_REPORTER_KEY, "default");
        assertEquals(AttemptExceptionReporter.LOG_ERROR, cfgB.getAttemptExceptionReporter());
        assertFalse(cfgB.isAttemptExceptionReporterSet());

        assertEquals(AttemptExceptionReporter.LOG_ERROR,
                new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .build()
                .getAttemptExceptionReporter());

        assertEquals(AttemptExceptionReporter.LOG_WARN,
                new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .attemptExceptionReporter(AttemptExceptionReporter.LOG_WARN)
                        .build()
                        .getAttemptExceptionReporter());
    }

    @Test
    public void testSharedVariables() throws TemplateException, IOException {
        Configuration cfg = new Builder(VERSION_3_0_0)
                .sharedVariables(ImmutableMap.of(
                        "a", "aa",
                        "b", "bb",
                        "c", new MyStringModel()
                ))
                .build();

        assertNull(cfg.getSharedVariables().get("noSuchVar"));
        assertNull(cfg.getWrappedSharedVariable("noSuchVar"));

        TemplateStringModel aVal = (TemplateStringModel) cfg.getWrappedSharedVariable("a");
        assertEquals("aa", aVal.getAsString());
        assertEquals(SimpleString.class, aVal.getClass());

        TemplateStringModel bVal = (TemplateStringModel) cfg.getWrappedSharedVariable("b");
        assertEquals("bb", bVal.getAsString());
        assertEquals(SimpleString.class, bVal.getClass());

        TemplateStringModel cVal = (TemplateStringModel) cfg.getWrappedSharedVariable("c");
        assertEquals("my", cVal.getAsString());
        assertEquals(MyStringModel.class, cfg.getWrappedSharedVariable("c").getClass());

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

    public static final MemberAccessPolicy CONFIG_TEST_MEMBER_ACCESS_POLICY;
    static {
        try {
            CONFIG_TEST_MEMBER_ACCESS_POLICY = new WhitelistMemberAccessPolicy(MemberSelectorListMemberAccessPolicy.MemberSelector.parse(
                    ImmutableList.of(
                            File.class.getName() + ".getName()",
                            File.class.getName() + ".isFile()"),
                    false,
                    ConfigurationTest.class.getClassLoader()));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMemberAccessPolicySetting() throws TemplateException {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .setting(
                        "objectWrapper",
                        "DefaultObjectWrapper(3.0.0, "
                                + "memberAccessPolicy="
                                + ConfigurationTest.class.getName() + ".CONFIG_TEST_MEMBER_ACCESS_POLICY"
                                + ")")
                .build();
        TemplateHashModel m = (TemplateHashModel) cfg.getObjectWrapper().wrap(new File("x"));
        assertNotNull(m.get("getName"));
        assertNotNull(m.get("isFile"));
        assertNull(m.get("delete"));
    }

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        List<String> names = new ArrayList<>(Builder.getSettingNames());
        List<String> inheritedNames = new ArrayList<>(
                MutableParsingAndProcessingConfiguration.getSettingNames());
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

    @Test
    public void testAllSettingsAreCoveredByMutableSettingsObject() throws Exception {
        testAllSettingsAreCoveredByMutableSettingsObject(TopLevelConfiguration.class, Configuration.Builder.class);
    }

    @SuppressWarnings("rawtypes")
    public static void testAllSettingsAreCoveredByMutableSettingsObject(
            Class<?> settingsInterface, Class<?> settingsMutableObjectClass) throws Exception {
        Set<String> beanSettingNames = new TreeSet<>();
        Set<String> isSetSettingNames = new TreeSet<>();
        for (Method method : settingsInterface.getMethods()) {
            String name = method.getName();
            if (method.getParameterTypes().length == 0 && method.getReturnType() != void.class
                    && (method.getModifiers() & Modifier.PUBLIC) != 0) {
                if (name.startsWith("is") && name.endsWith("Set") && method.getReturnType() == boolean.class) {
                    isSetSettingNames.add(toGetterNameToSettingNameCase(name.substring(2, name.length() - 3)));
                } else if (name.startsWith("get")) {
                    String settingName = toGetterNameToSettingNameCase(name.substring(3));
                    beanSettingNames.add(settingName);
                }
            }
        }

        assertEquals("Not all getXxx have isXxxSet pair", beanSettingNames, isSetSettingNames);

        Method method = settingsMutableObjectClass.getMethod("getSettingNames");
        assertTrue((method.getModifiers() & Modifier.STATIC) != 0);
        assertEquals("Names deduced from getXxx methods and getSettingNames() result differs",
                beanSettingNames, new TreeSet<>((Set) method.invoke(null)));

        // TODO [FM3] Check if all has setXxx, unsetXxx, etc.
    }

    private static String toGetterNameToSettingNameCase(String name) {
        int idx = 0;
        while (idx < name.length() && Character.isUpperCase(name.charAt(idx))) {
            idx++;
        }
        String result =
                idx == 0 ? name
                : idx == 1 ? Character.toLowerCase(name.charAt(0)) + name.substring(1)
                : name.substring(0, idx - 1).toLowerCase() + name.substring(idx - 1); // "FOOBar" -> "fooBar"
        if (result.equals("templateUpdateDelayMilliseconds")) {
            result = "templateUpdateDelay";
        }

        return result;
    }

    @Test
    public void testGetSettingNamesCorrespondToStaticKeyFields() throws Exception {
        testGetSettingNamesCorrespondToStaticKeyFields(
                Configuration.Builder.getSettingNames(),
                Configuration.Builder.class);
    }

    public static void testGetSettingNamesCorrespondToStaticKeyFields(Set<String> names, Class<?> cfgClass) throws
            Exception {
        Set<String> uncoveredNames = new HashSet<>(names);
        for (Field f : cfgClass.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final String name = (String) f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
                uncoveredNames.remove(name);
            }
        }
        assertEquals("Some setting names aren't covered by the ..._KEY constants.",
                Collections.emptySet(), uncoveredNames);
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

        assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(cb.getSharedVariables()));
        Map<String, Object> mutableValue = new HashMap<>();
        mutableValue.put("x", "v1");
        cb.setSharedVariables(mutableValue);
        Map<String, Object> immutableValue = cb.getSharedVariables();
        assertNotSame(mutableValue, immutableValue); // Must be a copy
        assertTrue(_CollectionUtils.isMapKnownToBeUnmodifiable(immutableValue));
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
        assertEquals(ImmutableMap.of("lib", "lib.f3ah"), cfg.getAutoImports());
        assertEquals(ImmutableList.of("inc.f3ah"), cfg.getAutoIncludes());
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
                .autoImports(ImmutableMap.of("lib2", "lib2.f3ah"))
                .autoIncludes(ImmutableList.of("inc2.f3ah"))
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

        assertEquals(ImmutableMap.of("lib", "lib.f3ah", "lib2", "lib2.f3ah"), cfg.getAutoImports());

        assertEquals(ImmutableList.of("inc.f3ah", "inc2.f3ah"), cfg.getAutoIncludes());

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

    private static class MyStringModel implements TemplateStringModel {

        @Override
        public String getAsString() throws TemplateException {
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
            return ImmutableMap.of(
                    "hex", HexTemplateNumberFormatFactory.INSTANCE);
        }

        @Override
        protected Map<String, TemplateDateFormatFactory> getImpliedCustomDateFormats() {
            return ImmutableMap.of(
                    "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE);
        }

        @Override
        protected Map<String, String> getImpliedAutoImports() {
            return ImmutableMap.of("lib", "lib.f3ah");
        }

        @Override
        protected List<String> getImpliedAutoIncludes() {
            return ImmutableList.of("inc.f3ah");
        }

        @Override
        protected Map<String, Object> getImpliedSharedVariables() {
            return ImmutableMap.of("v", 1);
        }

        @Override
        protected Collection<OutputFormat> getImpliedRegisteredCustomOutputFormats() {
            return ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE);
        }
    }

}

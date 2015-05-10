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

package freemarker.template;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.junit.Test;

import freemarker.cache.CacheStorageWithGetSize;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.cache.SoftCacheStorage;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.StrongCacheStorage;
import freemarker.cache.TemplateCache;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupResult;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.cache.TemplateNameFormat;
import freemarker.core.Configurable;
import freemarker.core.Configurable.UnknownSettingException;
import freemarker.core.ConfigurableTest;
import freemarker.core.Environment;
import freemarker.core._CoreAPI;
import freemarker.core._CoreStringUtils;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.StringModel;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.NullWriter;

public class ConfigurationTest extends TestCase {

    public ConfigurationTest(String name) {
        super(name);
    }
    
    public void testIncompatibleImprovementsChangesDefaults() {
        Version newVersion = Configuration.VERSION_2_3_21;
        Version oldVersion = Configuration.VERSION_2_3_20;
        
        Configuration cfg = new Configuration();
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        assertEquals(cfg.getIncompatibleImprovements(), Configuration.VERSION_2_3_0);
        
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        cfg.setIncompatibleImprovements(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setObjectWrapper(new SimpleObjectWrapper());
        cfg.setIncompatibleImprovements(oldVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertUsesLegacyTemplateLoader(cfg);
        
        cfg.setTemplateLoader(new StringTemplateLoader());
        cfg.setIncompatibleImprovements(newVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertSame(SimpleObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());

        cfg.setObjectWrapper(ObjectWrapper.DEFAULT_WRAPPER);
        cfg.setIncompatibleImprovements(newVersion);
        assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        assertSame(StringTemplateLoader.class, cfg.getTemplateLoader().getClass());
        
        cfg.unsetObjectWrapper();
        assertUsesNewObjectWrapper(cfg);
        cfg.unsetTemplateLoader();
        assertUsesNewTemplateLoader(cfg);

        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);

        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_22);
        assertUses2322ObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        // ---
        
        cfg = new Configuration(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        
        // ---
        
        cfg = new Configuration(Configuration.VERSION_2_3_22);
        assertUses2322ObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
    }

    private void assertUses2322ObjectWrapper(Configuration cfg) {
        Object ow = cfg.getObjectWrapper();
        assertEquals(DefaultObjectWrapper.class, ow.getClass());
        assertEquals(Configuration.VERSION_2_3_22,
                ((DefaultObjectWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
    }
    
    public void testUnsetAndIsExplicitlySet() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        assertFalse(cfg.isLogTemplateExceptionsExplicitlySet());
        assertTrue(cfg.getLogTemplateExceptions());
        //
        cfg.setLogTemplateExceptions(false);
        assertTrue(cfg.isLogTemplateExceptionsExplicitlySet());
        assertFalse(cfg.getLogTemplateExceptions());
        //
        for (int i = 0; i < 2; i++) {
            cfg.unsetLogTemplateExceptions();
            assertFalse(cfg.isLogTemplateExceptionsExplicitlySet());
            assertTrue(cfg.getLogTemplateExceptions());
        }
        
        assertFalse(cfg.isObjectWrapperExplicitlySet());
        assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        //
        cfg.setObjectWrapper(ObjectWrapper.SIMPLE_WRAPPER);
        assertTrue(cfg.isObjectWrapperExplicitlySet());
        assertSame(ObjectWrapper.SIMPLE_WRAPPER, cfg.getObjectWrapper());
        //
        for (int i = 0; i < 2; i++) {
            cfg.unsetObjectWrapper();
            assertFalse(cfg.isObjectWrapperExplicitlySet());
            assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        }
        
        assertFalse(cfg.isTemplateExceptionHandlerExplicitlySet());
        assertSame(TemplateExceptionHandler.DEBUG_HANDLER, cfg.getTemplateExceptionHandler());
        //
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        assertTrue(cfg.isTemplateExceptionHandlerExplicitlySet());
        assertSame(TemplateExceptionHandler.RETHROW_HANDLER, cfg.getTemplateExceptionHandler());
        //
        for (int i = 0; i < 2; i++) {
            cfg.unsetTemplateExceptionHandler();
            assertFalse(cfg.isTemplateExceptionHandlerExplicitlySet());
            assertSame(TemplateExceptionHandler.DEBUG_HANDLER, cfg.getTemplateExceptionHandler());
        }
        
        assertFalse(cfg.isTemplateLoaderExplicitlySet());
        assertTrue(cfg.getTemplateLoader() instanceof FileTemplateLoader);
        //
        cfg.setTemplateLoader(null);
        assertTrue(cfg.isTemplateLoaderExplicitlySet());
        assertNull(cfg.getTemplateLoader());
        //
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                cfg.setTemplateLoader(cfg.getTemplateLoader());
            }
            cfg.unsetTemplateLoader();
            assertFalse(cfg.isTemplateLoaderExplicitlySet());
            assertTrue(cfg.getTemplateLoader() instanceof FileTemplateLoader);
        }
        
        assertFalse(cfg.isTemplateLookupStrategyExplicitlySet());
        assertSame(TemplateLookupStrategy.DEFAULT_2_3_0, cfg.getTemplateLookupStrategy());
        //
        cfg.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
        assertTrue(cfg.isTemplateLookupStrategyExplicitlySet());
        //
        for (int i = 0; i < 2; i++) {
            cfg.unsetTemplateLookupStrategy();
            assertFalse(cfg.isTemplateLookupStrategyExplicitlySet());
        }
        
        assertFalse(cfg.isTemplateNameFormatExplicitlySet());
        assertSame(TemplateNameFormat.DEFAULT_2_3_0, cfg.getTemplateNameFormat());
        //
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        assertTrue(cfg.isTemplateNameFormatExplicitlySet());
        assertSame(TemplateNameFormat.DEFAULT_2_4_0, cfg.getTemplateNameFormat());
        //
        for (int i = 0; i < 2; i++) {
            cfg.unsetTemplateNameFormat();
            assertFalse(cfg.isTemplateNameFormatExplicitlySet());
            assertSame(TemplateNameFormat.DEFAULT_2_3_0, cfg.getTemplateNameFormat());
        }
        
        assertFalse(cfg.isCacheStorageExplicitlySet());
        assertTrue(cfg.getCacheStorage() instanceof SoftCacheStorage);
        //
        cfg.setCacheStorage(NullCacheStorage.INSTANCE);
        assertTrue(cfg.isCacheStorageExplicitlySet());
        assertSame(NullCacheStorage.INSTANCE, cfg.getCacheStorage());
        //
        for (int i = 0; i < 3; i++) {
            if (i == 2) {
                cfg.setCacheStorage(cfg.getCacheStorage());
            }
            cfg.unsetCacheStorage();
            assertFalse(cfg.isCacheStorageExplicitlySet());
            assertTrue(cfg.getCacheStorage() instanceof SoftCacheStorage);
        }
    }
    
    public void testTemplateLoadingErrors() throws Exception {
        Configuration cfg = new Configuration();
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), allOf(containsString("wasn't set"), containsString("default")));
        }
        
        cfg = new Configuration(Configuration.VERSION_2_3_21);
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), allOf(containsString("wasn't set"), not(containsString("default"))));
        }
        
        cfg.setClassForTemplateLoading(this.getClass(), "nosuchpackage");
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            assertThat(e.getMessage(), not(containsString("wasn't set")));
        }
    }
    
    private void assertUsesLegacyObjectWrapper(Configuration cfg) {
        assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
    }

    private void assertUsesNewObjectWrapper(Configuration cfg) {
        assertEquals(
                Configuration.VERSION_2_3_21,
                ((DefaultObjectWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
    }
    
    private void assertUsesNewTemplateLoader(Configuration cfg) {
        assertNull(cfg.getTemplateLoader());
    }
    
    private void assertUsesLegacyTemplateLoader(Configuration cfg) {
        assertTrue(cfg.getTemplateLoader() instanceof FileTemplateLoader);
    }
    
    public void testVersion() {
        Version v = Configuration.getVersion();
        assertTrue(v.intValue() > _TemplateAPI.VERSION_INT_2_3_20);
        assertSame(v.toString(), Configuration.getVersionNumber());
        
        try {
            new Configuration(new Version(999, 1, 2));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }
        
        try {
            new Configuration(new Version(2, 2, 2));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("2.3.0"));
        }
    }
    
    public void testShowErrorTips() throws Exception {
        Configuration cfg = new Configuration();
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("Tip:"));
        }
        
        cfg.setShowErrorTips(false);
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), not(containsString("Tip:")));
        }
    }
    
    @Test
    @SuppressWarnings("boxing")
    public void testGetTemplateOverloads() throws IOException, TemplateException {
        final Locale hu = new Locale("hu", "HU");
        final String latin1 = "ISO-8859-1";
        final String latin2 = "ISO-8859-2";
        final String utf8 = "utf-8";
        final String tFtl = "t.ftl";
        final String tEnFtl = "t_en.ftl";
        final String tUtf8Ftl = "t-utf8.ftl";
        final Integer custLookupCond = 123;
        
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setLocale(Locale.GERMAN);
        cfg.setDefaultEncoding(latin1);
        cfg.setEncoding(hu, latin2);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate(tFtl, "${1}");
        tl.putTemplate(tEnFtl, "${1}");
        tl.putTemplate(tUtf8Ftl, "<#ftl encoding='utf-8'>");
        cfg.setTemplateLoader(tl);
        
        // 1 args:
        {
            Template t = cfg.getTemplate(tFtl);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl);
            assertEquals(tUtf8Ftl, t.getName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
        }
        
        // 2 args overload 1:
        {
            Template t = cfg.getTemplate(tFtl, Locale.GERMAN);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, (Locale) null);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, Locale.US);
            assertEquals(tFtl, t.getName());
            assertEquals(tEnFtl, t.getSourceName());
            assertEquals(Locale.US, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl, Locale.US);
            assertEquals(tUtf8Ftl, t.getName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(Locale.US, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, hu);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin2, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tUtf8Ftl, hu);
            assertEquals(tUtf8Ftl, t.getName());
            assertEquals(tUtf8Ftl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
        }
        
        // 2 args overload 2:
        {
            Template t = cfg.getTemplate(tFtl, utf8);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
        }
        {
            Template t = cfg.getTemplate(tFtl, (String) null);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
        }
        
        // 3 args:
        {
            Template t = cfg.getTemplate(tFtl, hu, utf8);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, null);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin2, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, utf8);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, null);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin1, t.getEncoding());
            assertOutputEquals("1", t);
        }
        
        // 4 args:
        {
            Template t = cfg.getTemplate(tFtl, hu, utf8, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("${1}", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, utf8, true);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, utf8, true);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, null, true);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin2, t.getEncoding());
            assertOutputEquals("1", t);
        }
        
        // 5 args:
        {
            Template t = cfg.getTemplate(tFtl, hu, utf8, false, true);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("${1}", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, utf8, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, utf8, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, null, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertNull(t.getCustomLookupCondition());
            assertEquals(latin2, t.getEncoding());
            assertOutputEquals("1", t);
        }
        try {
            cfg.getTemplate("missing.ftl", hu, utf8, true, false);
            fail();
        } catch (TemplateNotFoundException e) {
            // Expected
        }
        assertNull(cfg.getTemplate("missing.ftl", hu, utf8, true, true));
        
        // 6 args:
        {
            Template t = cfg.getTemplate(tFtl, hu, custLookupCond, utf8, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, custLookupCond, utf8, false, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("${1}", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, null, custLookupCond, utf8, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(Locale.GERMAN, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(utf8, t.getEncoding());
            assertOutputEquals("1", t);
        }
        {
            Template t = cfg.getTemplate(tFtl, hu, custLookupCond, null, true, false);
            assertEquals(tFtl, t.getName());
            assertEquals(tFtl, t.getSourceName());
            assertEquals(hu, t.getLocale());
            assertEquals(custLookupCond, t.getCustomLookupCondition());
            assertEquals(latin2, t.getEncoding());
            assertOutputEquals("1", t);
        }
        try {
            cfg.getTemplate("missing.ftl", hu, custLookupCond, utf8, true, false);
            fail();
        } catch (TemplateNotFoundException e) {
            // Expected
        }
        assertNull(cfg.getTemplate("missing.ftl", hu, custLookupCond, utf8, true, true));
    }

    private void assertOutputEquals(final String expectedContent, final Template t) throws TemplateException,
            IOException {
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        assertEquals(expectedContent, sw.toString());
    }
    
    public void testSetTemplateLoaderAndCache() throws Exception {
        Configuration cfg = new Configuration();
        
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        assertEquals(0, cache.getSize());
        cfg.setCacheStorage(new StrongCacheStorage());
        cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        assertEquals(0, cache.getSize());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.getTemplate("toCache2.ftl");
        assertEquals(2, cache.getSize());
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.setTemplateLoader(cfg.getTemplateLoader());
        assertEquals(1, cache.getSize());
    }

    public void testChangingLocalizedLookupClearsCache() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setCacheStorage(new StrongCacheStorage());
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        
        assertEquals(0, cache.getSize());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.setLocalizedLookup(true);
        assertEquals(1, cache.getSize());
        cfg.setLocalizedLookup(false);
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.setLocalizedLookup(false);
        assertEquals(1, cache.getSize());
        cfg.setLocalizedLookup(true);
        assertEquals(0, cache.getSize());
    }

    public void testChangingTemplateNameFormatClearsCache() throws Exception {
        Configuration cfg = new Configuration();
        cfg.setCacheStorage(new StrongCacheStorage());
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        
        assertEquals(0, cache.getSize());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_3_0);
        assertEquals(1, cache.getSize());
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        assertEquals(1, cache.getSize());
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_3_0);
        assertEquals(0, cache.getSize());
    }

    public void testChangingTemplateNameFormatHasEffect() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("a/b.ftl", "In a/b.ftl");
        tl.putTemplate("b.ftl", "In b.ftl");
        cfg.setTemplateLoader(tl);
        
        {
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("a/b.ftl", template.getName());
            assertEquals("a/b.ftl", template.getSourceName());
            assertEquals("In a/b.ftl", template.toString());
        }
        
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        
        {
            final Template template = cfg.getTemplate("a/./../b.ftl");
            assertEquals("b.ftl", template.getName());
            assertEquals("b.ftl", template.getSourceName());
            assertEquals("In b.ftl", template.toString());
        }
    }

    public void testTemplateNameFormatSetSetting() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        assertSame(TemplateNameFormat.DEFAULT_2_3_0, cfg.getTemplateNameFormat());
        cfg.setSetting(Configuration.TEMPLATE_NAME_FORMAT_KEY, "defAult_2_4_0");
        assertSame(TemplateNameFormat.DEFAULT_2_4_0, cfg.getTemplateNameFormat());
        cfg.setSetting(Configuration.TEMPLATE_NAME_FORMAT_KEY, "defaUlt_2_3_0");
        assertSame(TemplateNameFormat.DEFAULT_2_3_0, cfg.getTemplateNameFormat());
        assertTrue(cfg.isTemplateNameFormatExplicitlySet());
        cfg.setSetting(Configuration.TEMPLATE_NAME_FORMAT_KEY, "defauLt");
        assertFalse(cfg.isTemplateNameFormatExplicitlySet());
    }

    public void testObjectWrapperSetSetting() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_20);
        
        {
            cfg.setSetting(Configuration.OBJECT_WRAPPER_KEY, "defAult");
            assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        }
        
        {
            cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_22);
            assertNotSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
            DefaultObjectWrapper dow = (DefaultObjectWrapper) cfg.getObjectWrapper();
            assertEquals(Configuration.VERSION_2_3_22, dow.getIncompatibleImprovements());
            assertTrue(dow.getForceLegacyNonListCollections());
        }
        
        {
            cfg.setSetting(Configuration.OBJECT_WRAPPER_KEY, "defAult_2_3_0");
            assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        }
        
        {
            cfg.setSetting(Configuration.OBJECT_WRAPPER_KEY,
                    "DefaultObjectWrapper(2.3.21, useAdaptersForContainers=true, forceLegacyNonListCollections=false)");
            DefaultObjectWrapper dow = (DefaultObjectWrapper) cfg.getObjectWrapper();
            assertEquals(Configuration.VERSION_2_3_21, dow.getIncompatibleImprovements());
            assertFalse(dow.getForceLegacyNonListCollections());
        }
        
        {
            cfg.setSetting(Configuration.OBJECT_WRAPPER_KEY, "defAult");
            DefaultObjectWrapper dow = (DefaultObjectWrapper) cfg.getObjectWrapper();
            assertEquals(Configuration.VERSION_2_3_22, dow.getIncompatibleImprovements());
            assertTrue(dow.getForceLegacyNonListCollections());
        }
    }
    
    public void testTemplateLookupStrategyDefaultAndSet() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        assertSame(TemplateLookupStrategy.DEFAULT_2_3_0, cfg.getTemplateLookupStrategy());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertSame(TemplateLookupStrategy.DEFAULT_2_3_0, cfg.getTemplateLookupStrategy());
        
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        
        cfg.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
        assertEquals(1, cache.getSize());
        
        final TemplateLookupStrategy myStrategy = new TemplateLookupStrategy() {
            public TemplateLookupResult lookup(TemplateLookupContext ctx) throws IOException {
                return ctx.lookupWithAcquisitionStrategy(ctx.getTemplateName());
            }
        };
        cfg.setTemplateLookupStrategy(myStrategy);
        assertEquals(0, cache.getSize());
        assertSame(myStrategy, cfg.getTemplateLookupStrategy());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        
        cfg.setTemplateLookupStrategy(myStrategy);
        assertEquals(1, cache.getSize());
        
        cfg.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
        assertEquals(0, cache.getSize());
    }
    
    public void testSetTimeZone() throws TemplateException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);
            
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
            assertEquals(sysDefTZ, cfg.getTimeZone());
            assertEquals(sysDefTZ.getID(), cfg.getSetting(Configurable.TIME_ZONE_KEY));
            cfg.setSetting(Configurable.TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfg.getTimeZone());
            assertEquals(sysDefTZ.getID(), cfg.getSetting(Configurable.TIME_ZONE_KEY));
            
            TimeZone newSysDefTZ = TimeZone.getTimeZone("GMT+09");
            TimeZone.setDefault(newSysDefTZ);
            assertEquals(sysDefTZ, cfg.getTimeZone());
            assertEquals(sysDefTZ.getID(), cfg.getSetting(Configurable.TIME_ZONE_KEY));
            cfg.setSetting(Configurable.TIME_ZONE_KEY, "JVM default");
            assertEquals(newSysDefTZ, cfg.getTimeZone());
            assertEquals(newSysDefTZ.getID(), cfg.getSetting(Configurable.TIME_ZONE_KEY));
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }
    
    public void testSetSQLDateAndTimeTimeZone() throws TemplateException {
        TimeZone origSysDefTZ = TimeZone.getDefault();
        try {
            TimeZone sysDefTZ = TimeZone.getTimeZone("GMT-01");
            TimeZone.setDefault(sysDefTZ);
            
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
            
            cfg.setSQLDateAndTimeTimeZone(null);
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
            
            cfg.setSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "JVM default");
            assertEquals(sysDefTZ, cfg.getSQLDateAndTimeTimeZone());
            assertEquals(sysDefTZ.getID(), cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
            
            cfg.setSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY, "null");
            assertNull(cfg.getSQLDateAndTimeTimeZone());
            assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        } finally {
            TimeZone.setDefault(origSysDefTZ);
        }
    }

    public void testTimeZoneLayers() throws TemplateException, IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        Template t = new Template(null, "", cfg);
        Environment env1 = t.createProcessingEnvironment(null, new StringWriter());
        Environment env2 = t.createProcessingEnvironment(null, new StringWriter());
        
        // cfg:
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertNull(cfg.getSQLDateAndTimeTimeZone());
        assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env:
        assertEquals(TimeZone.getDefault(), env1.getTimeZone());
        assertNull(env1.getSQLDateAndTimeTimeZone());
        assertEquals("null", env1.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env 2:
        assertEquals(TimeZone.getDefault(), env2.getTimeZone());
        assertNull(env2.getSQLDateAndTimeTimeZone());
        assertEquals("null", env2.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        
        env1.setSQLDateAndTimeTimeZone(DateUtil.UTC);
        // cfg:
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertNull(cfg.getSQLDateAndTimeTimeZone());
        assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env:
        assertEquals(TimeZone.getDefault(), env1.getTimeZone());
        assertEquals(DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());
        assertEquals(DateUtil.UTC.getID(), env1.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        
        TimeZone localTZ = TimeZone.getTimeZone("Europe/Brussels");
        env1.setTimeZone(localTZ);
        // cfg:
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertNull(cfg.getSQLDateAndTimeTimeZone());
        assertEquals("null", cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env:
        assertEquals(localTZ, env1.getTimeZone());
        assertEquals(DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());
        assertEquals(DateUtil.UTC.getID(), env1.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env 2:
        assertEquals(TimeZone.getDefault(), env2.getTimeZone());
        assertNull(env2.getSQLDateAndTimeTimeZone());
        assertEquals("null", env2.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        
        TimeZone otherTZ1 = TimeZone.getTimeZone("GMT+05");
        TimeZone otherTZ2 = TimeZone.getTimeZone("GMT+06");
        cfg.setTimeZone(otherTZ1);
        cfg.setSQLDateAndTimeTimeZone(otherTZ2);
        // cfg:
        assertEquals(otherTZ1, cfg.getTimeZone());
        assertEquals(otherTZ2, cfg.getSQLDateAndTimeTimeZone());
        assertEquals(otherTZ1.getID(), cfg.getSetting(Configurable.TIME_ZONE_KEY));
        assertEquals(otherTZ2.getID(), cfg.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env:
        assertEquals(localTZ, env1.getTimeZone());
        assertEquals(DateUtil.UTC, env1.getSQLDateAndTimeTimeZone());
        assertEquals(DateUtil.UTC.getID(), env1.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        // env 2:
        assertEquals(otherTZ1, env2.getTimeZone());
        assertEquals(otherTZ2, env2.getSQLDateAndTimeTimeZone());
        assertEquals(otherTZ1.getID(), env2.getSetting(Configurable.TIME_ZONE_KEY));
        assertEquals(otherTZ2.getID(), env2.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
        
        try {
            setTimeZoneToNull(env2);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        env2.setSQLDateAndTimeTimeZone(null);
        assertEquals(otherTZ1, env2.getTimeZone());
        assertNull(env2.getSQLDateAndTimeTimeZone());
        assertEquals(otherTZ1.getID(), env2.getSetting(Configurable.TIME_ZONE_KEY));
        assertEquals("null", env2.getSetting(Configurable.SQL_DATE_AND_TIME_TIME_ZONE_KEY));
    }

    // Findbugs annotation, try to put back on Java 1.6:
    // @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="Meant to fail")
    private void setTimeZoneToNull(Environment env2) {
        env2.setTimeZone(null);
    }
    
    public void testSetICIViaSetSettingAPI() throws TemplateException {
        Configuration cfg = new Configuration();
        assertEquals(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS, cfg.getIncompatibleImprovements());
        cfg.setSetting(Configuration.INCOMPATIBLE_IMPROVEMENTS, "2.3.21");
        assertEquals(Configuration.VERSION_2_3_21, cfg.getIncompatibleImprovements());
    }

    public void testSetLogTemplateExceptionsViaSetSettingAPI() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertEquals(true, cfg.getLogTemplateExceptions());
        cfg.setSetting(Configuration.LOG_TEMPLATE_EXCEPTIONS_KEY, "false");
        assertEquals(false, cfg.getLogTemplateExceptions());
    }
    
    public void testSharedVariables() throws TemplateModelException {
        Configuration cfg = new Configuration();

        cfg.setSharedVariable("erased", "");
        assertNotNull(cfg.getSharedVariable("erased"));
        
        Map<String, Object> vars = new HashMap<String, Object>(); 
        vars.put("a", "aa");
        vars.put("b", "bb");
        vars.put("c", new MyScalarModel());
        cfg.setSharedVaribles(vars);

        assertNull(cfg.getSharedVariable("erased"));
        
        {
            TemplateScalarModel aVal = (TemplateScalarModel) cfg.getSharedVariable("a");
            assertEquals("aa", aVal.getAsString());
            assertEquals(SimpleScalar.class, aVal.getClass());
            
            TemplateScalarModel bVal = (TemplateScalarModel) cfg.getSharedVariable("b");
            assertEquals("bb", bVal.getAsString());
            assertEquals(SimpleScalar.class, bVal.getClass());
            
            TemplateScalarModel cVal = (TemplateScalarModel) cfg.getSharedVariable("c");
            assertEquals("my", cVal.getAsString());
            assertEquals(MyScalarModel.class, cfg.getSharedVariable("c").getClass());
        }
        
        // Legacy method: Keeps TemplateModel created on the time it was called. 
        cfg.setSharedVariable("b", "bbLegacy");
        
        // Cause re-wrapping of variables added via setSharedVaribles:
        cfg.setObjectWrapper(new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build());

        {
            TemplateScalarModel aVal = (TemplateScalarModel) cfg.getSharedVariable("a");
            assertEquals("aa", aVal.getAsString());
            assertEquals(StringModel.class, aVal.getClass());
            
            TemplateScalarModel bVal = (TemplateScalarModel) cfg.getSharedVariable("b");
            assertEquals("bbLegacy", bVal.getAsString());
            assertEquals(SimpleScalar.class, bVal.getClass());
            
            TemplateScalarModel cVal = (TemplateScalarModel) cfg.getSharedVariable("c");
            assertEquals("my", cVal.getAsString());
            assertEquals(MyScalarModel.class, cVal.getClass());
        }
    }

    @Test
    public void testApiBuiltinEnabled() throws IOException, TemplateException {
        for (Version v : new Version[] { Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_22 }) {
            Configuration cfg = new Configuration(v);
            try {
                new Template(null, "${1?api}", cfg).process(null, NullWriter.INSTANCE);
                fail();
            } catch (TemplateException e) {
                assertThat(e.getMessage(), containsString(Configurable.API_BUILTIN_ENABLED_KEY));
            }
        }
        
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setAPIBuiltinEnabled(true);
        new Template(null, "${m?api.hashCode()}", cfg)
                .process(Collections.singletonMap("m", new HashMap()), NullWriter.INSTANCE);
    }

    @Test
    public void testTemplateUpdateDelay() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

        assertEquals(TemplateCache.DEFAULT_TEMPLATE_UPDATE_DELAY_MILLIS, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setTemplateUpdateDelay(4);
        assertEquals(4000L, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setTemplateUpdateDelayMilliseconds(100);
        assertEquals(100L, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "5");
        assertEquals(5000L, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "3 ms");
        assertEquals(3L, cfg.getTemplateUpdateDelayMilliseconds());
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "4ms");
        assertEquals(4L, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "3 s");
        assertEquals(3000L, cfg.getTemplateUpdateDelayMilliseconds());
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "4s");
        assertEquals(4000L, cfg.getTemplateUpdateDelayMilliseconds());
        
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "3 m");
        assertEquals(1000L * 60 * 3, cfg.getTemplateUpdateDelayMilliseconds());
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "4m");
        assertEquals(1000L * 60 * 4, cfg.getTemplateUpdateDelayMilliseconds());

        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "1 h");
        assertEquals(1000L * 60 * 60, cfg.getTemplateUpdateDelayMilliseconds());
        cfg.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "2h");
        assertEquals(1000L * 60 * 60 * 2, cfg.getTemplateUpdateDelayMilliseconds());
    }
    
    public void testNamingConventionSetSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

        assertEquals(Configuration.AUTO_DETECT_NAMING_CONVENTION, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "legacy");
        assertEquals(Configuration.LEGACY_NAMING_CONVENTION, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "camel_case");
        assertEquals(Configuration.CAMEL_CASE_NAMING_CONVENTION, cfg.getNamingConvention());
        
        cfg.setSetting("naming_convention", "auto_detect");
        assertEquals(Configuration.AUTO_DETECT_NAMING_CONVENTION, cfg.getNamingConvention());
    }
    
    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        for (boolean camelCase : new boolean[] { false, true }) {
            List<String> names = new ArrayList<String>(cfg.getSettingNames(camelCase)); 
            List<String> cfgableNames = new ArrayList<String>(_CoreAPI.getConfigurableSettingNames(cfg, camelCase));
            assertStartsWith(names, cfgableNames);
            
            String prevName = null;
            for (int i = cfgableNames.size(); i < names.size(); i++) {
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
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        ConfigurableTest.testGetSettingNamesNameConventionsContainTheSame(
                new ArrayList<String>(cfg.getSettingNames(false)),
                new ArrayList<String>(cfg.getSettingNames(true)));
    }

    @Test
    public void testStaticFieldKeysCoverAllGetSettingNames() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        List<String> names = new ArrayList<String>(cfg.getSettingNames(false)); 
        List<String> cfgableNames = new ArrayList<String>(_CoreAPI.getConfigurableSettingNames(cfg, false));
        assertStartsWith(names, cfgableNames);
        
        for (int i = cfgableNames.size(); i < names.size(); i++) {
            String name = names.get(i);
            assertTrue("No field was found for " + name, keyFieldExists(name));
        }
    }
    
    @Test
    public void testGetSettingNamesCoversAllStaticKeyFields() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        Collection<String> names = cfg.getSettingNames(false);
        
        for (Class<? extends Configurable> cfgableClass : new Class[] { Configuration.class, Configurable.class }) {
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
        ConfigurableTest.testKeyStaticFieldsHasAllVariationsAndCorrectFormat(Configuration.class);
    }

    @Test
    public void testGetSettingNamesCoversAllSettingNames() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        Collection<String> names = cfg.getSettingNames(false);
        
        for (Field f : Configurable.class.getFields()) {
            if (f.getName().endsWith("_KEY")) {
                final Object name = f.get(null);
                assertTrue("Missing setting name: " + name, names.contains(name));
            }
        }
    }

    @Test
    public void testSetSettingSupportsBothNamingConventions() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        
        cfg.setSetting(Configuration.DEFAULT_ENCODING_KEY_CAMEL_CASE, "UTF-16LE");
        assertEquals("UTF-16LE", cfg.getDefaultEncoding());
        cfg.setSetting(Configuration.DEFAULT_ENCODING_KEY_SNAKE_CASE, "UTF-8");
        assertEquals("UTF-8", cfg.getDefaultEncoding());
        
        for (String nameCC : (Set<String>) cfg.getSettingNames(true)) {
            for (String value : new String[] { "1", "default", "true" }) {
                Exception resultCC = null;
                try {
                    cfg.setSetting(nameCC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(UnknownSettingException.class)));
                    resultCC = e;
                }
                
                String nameSC = _CoreStringUtils.camelCaseToUnderscored(nameCC);
                Exception resultSC = null;
                try {
                    cfg.setSetting(nameSC, value);
                } catch (Exception e) {
                    assertThat(e, not(instanceOf(UnknownSettingException.class)));
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

        public String getAsString() throws TemplateModelException {
            return "my";
        }
        
    }
    
}

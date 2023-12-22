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

package freemarker.template;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
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

import org.junit.Test;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import freemarker.core.BaseNTemplateNumberFormatFactory;
import freemarker.core.CFormat;
import freemarker.core.CombinedMarkupOutputFormat;
import freemarker.core.Configurable;
import freemarker.core.Configurable.SettingValueAssignmentException;
import freemarker.core.Configurable.UnknownSettingException;
import freemarker.core.ConfigurableTest;
import freemarker.core.CustomHTMLOutputFormat;
import freemarker.core.DefaultTruncateBuiltinAlgorithm;
import freemarker.core.DummyOutputFormat;
import freemarker.core.Environment;
import freemarker.core.EpochMillisDivTemplateDateFormatFactory;
import freemarker.core.EpochMillisTemplateDateFormatFactory;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.HexTemplateNumberFormatFactory;
import freemarker.core.JSONCFormat;
import freemarker.core.JavaCFormat;
import freemarker.core.JavaScriptCFormat;
import freemarker.core.JavaScriptOrJSONCFormat;
import freemarker.core.LegacyCFormat;
import freemarker.core.MarkupOutputFormat;
import freemarker.core.OptInTemplateClassResolver;
import freemarker.core.OutputFormat;
import freemarker.core.ParseException;
import freemarker.core.RTFOutputFormat;
import freemarker.core.TemplateClassResolver;
import freemarker.core.TemplateDateFormatFactory;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.UndefinedOutputFormat;
import freemarker.core.UnregisteredOutputFormatException;
import freemarker.core.XHTMLOutputFormat;
import freemarker.core.XMLOutputFormat;
import freemarker.core.XSCFormat;
import freemarker.core._CoreStringUtils;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.LegacyDefaultMemberAccessPolicy;
import freemarker.ext.beans.MemberAccessPolicy;
import freemarker.ext.beans.MemberSelectorListMemberAccessPolicy;
import freemarker.ext.beans.StringModel;
import freemarker.ext.beans.WhitelistMemberAccessPolicy;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.NullWriter;
import junit.framework.TestCase;

@RunWith(JUnit38ClassRunner.class)
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
        
        cfg = new Configuration(Configuration.VERSION_2_3_25);
        assertFalse(((DefaultObjectWrapper) cfg.getObjectWrapper()).getTreatDefaultMethodsAsBeanMembers());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_26);
        assertTrue(((DefaultObjectWrapper) cfg.getObjectWrapper()).getTreatDefaultMethodsAsBeanMembers());
        assertTrue(((DefaultObjectWrapper) cfg.getObjectWrapper()).getPreferIndexedReadMethod());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_27);
        assertTrue(((DefaultObjectWrapper) cfg.getObjectWrapper()).getTreatDefaultMethodsAsBeanMembers());
        assertFalse(((DefaultObjectWrapper) cfg.getObjectWrapper()).getPreferIndexedReadMethod());

        cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_20);
        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_21);
        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_31);
        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_32);
        assertSame(JavaScriptOrJSONCFormat.INSTANCE, cfg.getCFormat());
        cfg.setCFormat(JavaScriptOrJSONCFormat.INSTANCE); // Same as default, but explicitly set now
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_31);
        assertSame(JavaScriptOrJSONCFormat.INSTANCE, cfg.getCFormat());
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

        assertFalse(cfg.isCFormatExplicitlySet());
        //
        cfg.setCFormat(XSCFormat.INSTANCE);
        assertTrue(cfg.isCFormatExplicitlySet());
        //
        cfg.unsetCFormat();
        assertFalse(cfg.isCFormatExplicitlySet());
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
        assertTrue(v.intValue() > _VersionInts.V_2_3_20);
        assertSame(v.toString(), Configuration.getVersionNumber());
        
        try {
            new Configuration(new Version(999, 1, 2));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("upgrade"));
        }
        
        try {
            new Configuration(new Version(2, 2, 2));
            fail();
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
        
        CacheStorageWithGetSize cacheStorage = (CacheStorageWithGetSize) cfg.getCacheStorage();
        assertEquals(0, cacheStorage.getSize());
        cfg.setCacheStorage(new StrongCacheStorage());
        cacheStorage = (CacheStorageWithGetSize) cfg.getCacheStorage();
        assertEquals(0, cacheStorage.getSize());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cacheStorage.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cacheStorage.getSize());
        cfg.getTemplate("toCache2.ftl");
        assertEquals(2, cacheStorage.getSize());
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cacheStorage.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cacheStorage.getSize());
        cfg.setTemplateLoader(cfg.getTemplateLoader());
        assertEquals(1, cacheStorage.getSize());
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
            cfg.setSetting(Configurable.OBJECT_WRAPPER_KEY, "defAult");
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
            cfg.setSetting(Configurable.OBJECT_WRAPPER_KEY, "defAult_2_3_0");
            assertSame(ObjectWrapper.DEFAULT_WRAPPER, cfg.getObjectWrapper());
        }
        
        {
            cfg.setSetting(Configurable.OBJECT_WRAPPER_KEY,
                    "DefaultObjectWrapper(2.3.21, useAdaptersForContainers=true, forceLegacyNonListCollections=false)");
            DefaultObjectWrapper dow = (DefaultObjectWrapper) cfg.getObjectWrapper();
            assertEquals(Configuration.VERSION_2_3_21, dow.getIncompatibleImprovements());
            assertFalse(dow.getForceLegacyNonListCollections());
        }
        
        {
            cfg.setSetting(Configurable.OBJECT_WRAPPER_KEY, "defAult");
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
            @Override
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
    
    public void testSetTemplateConfigurations() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        assertNull(cfg.getTemplateConfigurations());

        StringTemplateLoader tl = new StringTemplateLoader();
        tl.putTemplate("t.de.ftlh", "");
        tl.putTemplate("t.fr.ftlx", "");
        tl.putTemplate("t.ftlx", "");
        tl.putTemplate("Stat/t.de.ftlx", "");
        cfg.setTemplateLoader(tl);
        
        cfg.setTimeZone(TimeZone.getTimeZone("GMT+09"));
        
        cfg.setSetting(Configuration.TEMPLATE_CONFIGURATIONS_KEY,
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
            assertEquals(DateUtil.UTC, t.getTimeZone());
        }
        
        assertNotNull(cfg.getTemplateConfigurations());
        cfg.setSetting(Configuration.TEMPLATE_CONFIGURATIONS_KEY, "null");
        assertNull(cfg.getTemplateConfigurations());
    }

    public void testSetAutoEscaping() throws Exception {
       Configuration cfg = new Configuration();
    
       assertEquals(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());

       cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
       assertEquals(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());

       cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
       assertEquals(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());

       cfg.setAutoEscapingPolicy(Configuration.DISABLE_AUTO_ESCAPING_POLICY);
       assertEquals(Configuration.DISABLE_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());
       
       cfg.setSetting(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enableIfSupported");
       assertEquals(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());

       cfg.setSetting(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enable_if_supported");
       assertEquals(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());
       
       cfg.setSetting(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enableIfDefault");
       assertEquals(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());

       cfg.setSetting(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "enable_if_default");
       assertEquals(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());
       
       cfg.setSetting(Configuration.AUTO_ESCAPING_POLICY_KEY_CAMEL_CASE, "disable");
       assertEquals(Configuration.DISABLE_AUTO_ESCAPING_POLICY, cfg.getAutoEscapingPolicy());
       
       try {
           cfg.setAutoEscapingPolicy(Configuration.CAMEL_CASE_NAMING_CONVENTION);
           fail();
       } catch (IllegalArgumentException e) {
           // Expected
       }
    }

    public void testSetOutputFormat() throws Exception {
       Configuration cfg = new Configuration();
       
       assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getOutputFormat());
       assertFalse(cfg.isOutputFormatExplicitlySet());
       
       try {
           cfg.setOutputFormat(null);
           fail();
       } catch (NullArgumentException e) {
           // Expected
       }
       
       assertFalse(cfg.isOutputFormatExplicitlySet());
       
       cfg.setSetting(Configuration.OUTPUT_FORMAT_KEY_CAMEL_CASE, XMLOutputFormat.class.getSimpleName());
       assertEquals(XMLOutputFormat.INSTANCE, cfg.getOutputFormat());
       
       cfg.setSetting(Configuration.OUTPUT_FORMAT_KEY_SNAKE_CASE, HTMLOutputFormat.class.getSimpleName());
       assertEquals(HTMLOutputFormat.INSTANCE, cfg.getOutputFormat());

       // Set standard format by name instead of class name:
       cfg.setSetting(Configuration.OUTPUT_FORMAT_KEY_CAMEL_CASE, XHTMLOutputFormat.INSTANCE.getName());
       assertEquals(XHTMLOutputFormat.INSTANCE, cfg.getOutputFormat());
       
       cfg.unsetOutputFormat();
       assertEquals(UndefinedOutputFormat.INSTANCE, cfg.getOutputFormat());
       assertFalse(cfg.isOutputFormatExplicitlySet());
       
       cfg.setOutputFormat(UndefinedOutputFormat.INSTANCE);
       assertTrue(cfg.isOutputFormatExplicitlySet());
       cfg.setSetting(Configuration.OUTPUT_FORMAT_KEY_CAMEL_CASE, "default");
       assertFalse(cfg.isOutputFormatExplicitlySet());
       
       try {
           cfg.setSetting(Configuration.OUTPUT_FORMAT_KEY, "null");
       } catch (SettingValueAssignmentException e) {
           assertThat(e.getCause().getMessage(), containsString(UndefinedOutputFormat.class.getSimpleName()));
       }
    }
    
    @Test
    public void testGetOutputFormatByName() throws Exception {
        Configuration cfg = new Configuration();
        
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
        Configuration cfg = new Configuration();
        
        assertTrue(cfg.getRegisteredCustomOutputFormats().isEmpty());
        
        cfg.setSetting(Configuration.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_CAMEL_CASE,
                "[freemarker.core.CustomHTMLOutputFormat(), freemarker.core.DummyOutputFormat()]");
        assertEquals(
                ImmutableList.of(CustomHTMLOutputFormat.INSTANCE, DummyOutputFormat.INSTANCE),
                new ArrayList(cfg.getRegisteredCustomOutputFormats()));
        
        try {
            cfg.setSetting(Configuration.REGISTERED_CUSTOM_OUTPUT_FORMATS_KEY_SNAKE_CASE, "[TemplateConfiguration()]");
            fail();
        } catch (Exception e) {
            assertThat(e.getCause().getMessage(), containsString(OutputFormat.class.getSimpleName()));
        }
    }

    public void testSetRecognizeStandardFileExtensions() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
     
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        assertFalse(cfg.isRecognizeStandardFileExtensionsExplicitlySet());

        cfg.setRecognizeStandardFileExtensions(true);
        assertTrue(cfg.getRecognizeStandardFileExtensions());
        assertTrue(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
     
        cfg.unsetRecognizeStandardFileExtensions();
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        assertFalse(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
     
        cfg.setSetting(Configuration.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_CAMEL_CASE, "false");
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        assertTrue(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
        
        cfg.setSetting(Configuration.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY_SNAKE_CASE, "default");
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        assertFalse(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
        
        cfg.unsetRecognizeStandardFileExtensions();
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        assertTrue(cfg.getRecognizeStandardFileExtensions());
        assertFalse(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        cfg.setRecognizeStandardFileExtensions(false);
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_24);
        assertFalse(cfg.getRecognizeStandardFileExtensions());
        assertTrue(cfg.isRecognizeStandardFileExtensionsExplicitlySet());
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

    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="Expected to fail")
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
        cfg.setSetting(Configurable.LOG_TEMPLATE_EXCEPTIONS_KEY, "false");
        assertEquals(false, cfg.getLogTemplateExceptions());
    }

    public void testSetWrapUncheckedExceptionsViaSetSettingAPI() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        assertEquals(false, cfg.getWrapUncheckedExceptions());
        cfg.setSetting(Configurable.WRAP_UNCHECKED_EXCEPTIONS_KEY_CAMEL_CASE, "true");
        assertEquals(true, cfg.getWrapUncheckedExceptions());
        cfg.setSetting(Configurable.WRAP_UNCHECKED_EXCEPTIONS_KEY_SNAKE_CASE, "false");
        assertEquals(false, cfg.getWrapUncheckedExceptions());
    }
    
    public void testSetAttemptExceptionReporter() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertEquals(AttemptExceptionReporter.LOG_ERROR_REPORTER, cfg.getAttemptExceptionReporter());
        assertFalse(cfg.isAttemptExceptionReporterExplicitlySet());
        cfg.setSetting(Configurable.ATTEMPT_EXCEPTION_REPORTER_KEY, "log_warn");
        assertEquals(AttemptExceptionReporter.LOG_WARN_REPORTER, cfg.getAttemptExceptionReporter());
        assertTrue(cfg.isAttemptExceptionReporterExplicitlySet());
        cfg.setSetting(Configurable.ATTEMPT_EXCEPTION_REPORTER_KEY, "default");
        assertEquals(AttemptExceptionReporter.LOG_ERROR_REPORTER, cfg.getAttemptExceptionReporter());
        assertFalse(cfg.isAttemptExceptionReporterExplicitlySet());
    }
    
    public void testSharedVariables() throws TemplateModelException {
        Configuration cfg = new Configuration();

        cfg.setSharedVariable("erased", "");
        assertNotNull(cfg.getSharedVariable("erased"));
        
        Map<String, Object> vars = new HashMap<>();
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
    
    @Test
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS ", justification = "Testing wrong args")
    public void testSetCustomNumberFormat() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        try {
            cfg.setCustomNumberFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }

        try {
            cfg.setCustomNumberFormats(Collections.singletonMap("", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfg.setCustomNumberFormats(Collections.singletonMap("a_b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfg.setCustomNumberFormats(Collections.singletonMap("a b", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }
        
        try {
            cfg.setCustomNumberFormats(ImmutableMap.of(
                    "a", HexTemplateNumberFormatFactory.INSTANCE,
                    "@wrong", HexTemplateNumberFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }
        
        cfg.setSetting(Configurable.CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE,
                "{ 'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("base", BaseNTemplateNumberFormatFactory.INSTANCE),
                cfg.getCustomNumberFormats());
        
        cfg.setSetting(Configurable.CUSTOM_NUMBER_FORMATS_KEY_SNAKE_CASE,
                "{ "
                + "'base': " + BaseNTemplateNumberFormatFactory.class.getName() + "(), "
                + "'hex': " + HexTemplateNumberFormatFactory.class.getName() + "()"
                + " }");
        assertEquals(
                ImmutableMap.of(
                        "base", BaseNTemplateNumberFormatFactory.INSTANCE,
                        "hex", HexTemplateNumberFormatFactory.INSTANCE),
                cfg.getCustomNumberFormats());
        
        cfg.setSetting(Configurable.CUSTOM_NUMBER_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfg.getCustomNumberFormats());
        
        try {
            cfg.setSetting(Configurable.CUSTOM_NUMBER_FORMATS_KEY_CAMEL_CASE,
                    "{ 'x': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(EpochMillisTemplateDateFormatFactory.class.getName()),
                    containsString(TemplateNumberFormatFactory.class.getName())));
        }
    }

    @Test
    public void testSetBooleanFormat() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

        cfg.setBooleanFormat("yes,no");
        assertOutputEquals("yes no", new Template(null, "${true} ${false}", cfg));

        cfg.setBooleanFormat("c");
        assertOutputEquals("true false", new Template(null, "${true} ${false}", cfg));

        try {
            cfg.setBooleanFormat("yes no");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("comma"));
        }
    }

    @Test
    public void testSetTabSize() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        String ftl = "${\t}";
        
        try {
            new Template(null, ftl, cfg);
            fail();
        } catch (ParseException e) {
            assertEquals(9, e.getColumnNumber());
        }
        
        cfg.setTabSize(1);
        try {
            new Template(null, ftl, cfg);
            fail();
        } catch (ParseException e) {
            assertEquals(4, e.getColumnNumber());
        }
        
        try {
            cfg.setTabSize(0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            cfg.setTabSize(257);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testTabSizeSetting() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertEquals(8, cfg.getTabSize());
        cfg.setSetting(Configuration.TAB_SIZE_KEY_CAMEL_CASE, "4");
        assertEquals(4, cfg.getTabSize());
        cfg.setSetting(Configuration.TAB_SIZE_KEY_SNAKE_CASE, "1");
        assertEquals(1, cfg.getTabSize());
        
        try {
            cfg.setSetting(Configuration.TAB_SIZE_KEY_SNAKE_CASE, "x");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getCause(), instanceOf(NumberFormatException.class));
        }
    }
    
    @SuppressFBWarnings(value="NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", justification="We test failures")
    @Test
    public void testSetCustomDateFormat() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        try {
            cfg.setCustomDateFormats(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }
        
        try {
            cfg.setCustomDateFormats(Collections.singletonMap("", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("0 length"));
        }

        try {
            cfg.setCustomDateFormats(Collections.singletonMap("a_b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a_b"));
        }

        try {
            cfg.setCustomDateFormats(Collections.singletonMap("a b", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("a b"));
        }
        
        try {
            cfg.setCustomDateFormats(ImmutableMap.of(
                    "a", EpochMillisTemplateDateFormatFactory.INSTANCE,
                    "@wrong", EpochMillisTemplateDateFormatFactory.INSTANCE));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("@wrong"));
        }
        
        cfg.setSetting(Configurable.CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE,
                "{ 'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "() }");
        assertEquals(
                Collections.singletonMap("epoch", EpochMillisTemplateDateFormatFactory.INSTANCE),
                cfg.getCustomDateFormats());
        
        cfg.setSetting(Configurable.CUSTOM_DATE_FORMATS_KEY_SNAKE_CASE,
                "{ "
                + "'epoch': " + EpochMillisTemplateDateFormatFactory.class.getName() + "(), "
                + "'epochDiv': " + EpochMillisDivTemplateDateFormatFactory.class.getName() + "()"
                + " }");
        assertEquals(
                ImmutableMap.of(
                        "epoch", EpochMillisTemplateDateFormatFactory.INSTANCE,
                        "epochDiv", EpochMillisDivTemplateDateFormatFactory.INSTANCE),
                cfg.getCustomDateFormats());
        
        cfg.setSetting(Configurable.CUSTOM_DATE_FORMATS_KEY, "{}");
        assertEquals(Collections.emptyMap(), cfg.getCustomDateFormats());
        
        try {
            cfg.setSetting(Configurable.CUSTOM_DATE_FORMATS_KEY_CAMEL_CASE,
                    "{ 'x': " + HexTemplateNumberFormatFactory.class.getName() + "() }");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getCause().getMessage(), allOf(
                    containsString(HexTemplateNumberFormatFactory.class.getName()),
                    containsString(TemplateDateFormatFactory.class.getName())));
        }
    }
    
    @Test
    public void testHasCustomFormats() throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        Template t = new Template(null, "", cfg);
        Environment env = t.createProcessingEnvironment(null, null);
        assertFalse(cfg.hasCustomFormats());
        assertFalse(t.hasCustomFormats());
        assertFalse(env.hasCustomFormats());
        
        env.setCustomDateFormats(Collections.singletonMap("f", EpochMillisTemplateDateFormatFactory.INSTANCE));
        assertFalse(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
        t.setCustomDateFormats(Collections.singletonMap("f", EpochMillisTemplateDateFormatFactory.INSTANCE));
        assertFalse(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        cfg.setCustomDateFormats(Collections.singletonMap("f", EpochMillisTemplateDateFormatFactory.INSTANCE));
        assertTrue(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
        
        cfg.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        t.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        env.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        assertFalse(cfg.hasCustomFormats());
        assertFalse(t.hasCustomFormats());
        assertFalse(env.hasCustomFormats());
        
        cfg.setCustomDateFormats(Collections.singletonMap("f", EpochMillisTemplateDateFormatFactory.INSTANCE));
        assertTrue(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
        
        cfg.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        t.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        env.setCustomDateFormats(Collections.<String, TemplateDateFormatFactory>emptyMap());
        assertFalse(cfg.hasCustomFormats());
        assertFalse(t.hasCustomFormats());
        assertFalse(env.hasCustomFormats());
        
        // Same with number formats:
        
        env.setCustomNumberFormats(Collections.singletonMap("f", HexTemplateNumberFormatFactory.INSTANCE));
        assertFalse(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
        t.setCustomNumberFormats(Collections.singletonMap("f", HexTemplateNumberFormatFactory.INSTANCE));
        assertFalse(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        cfg.setCustomNumberFormats(Collections.singletonMap("f", HexTemplateNumberFormatFactory.INSTANCE));
        assertTrue(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
        
        cfg.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>emptyMap());
        t.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>emptyMap());
        env.setCustomNumberFormats(Collections.<String, TemplateNumberFormatFactory>emptyMap());
        assertFalse(cfg.hasCustomFormats());
        assertFalse(t.hasCustomFormats());
        assertFalse(env.hasCustomFormats());
        
        cfg.setCustomNumberFormats(Collections.singletonMap("f", HexTemplateNumberFormatFactory.INSTANCE));
        assertTrue(cfg.hasCustomFormats());
        assertTrue(t.hasCustomFormats());
        assertTrue(env.hasCustomFormats());
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

    public void testTagSyntaxSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);

        // Default is "angle brackets":
        assertEquals(Configuration.ANGLE_BRACKET_TAG_SYNTAX, cfg.getTagSyntax());

        cfg.setSetting("tag_syntax", "angle_bracket");
        assertEquals(Configuration.ANGLE_BRACKET_TAG_SYNTAX, cfg.getTagSyntax());
        
        cfg.setSetting("tag_syntax", "square_bracket");
        assertEquals(Configuration.SQUARE_BRACKET_TAG_SYNTAX, cfg.getTagSyntax());
        
        cfg.setSetting("tag_syntax", "auto_detect");
        assertEquals(Configuration.AUTO_DETECT_TAG_SYNTAX, cfg.getTagSyntax());
        
        // Camel case:
        cfg.setSetting("tagSyntax", "squareBracket");
        assertEquals(Configuration.SQUARE_BRACKET_TAG_SYNTAX, cfg.getTagSyntax());
        
        try {
            cfg.setSetting("tag_syntax", "no_such_syntax");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("no_such_syntax"));
        }
        
        // Catch an oversight that's easy to do:
        try {
            cfg.setTagSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("SQUARE_BRACKET_TAG_SYNTAX"));
        }
    }
    
    public void testInterpolationSyntaxSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);

        // Default is "legacy":
        assertEquals(Configuration.LEGACY_INTERPOLATION_SYNTAX, cfg.getInterpolationSyntax());
        
        cfg.setSetting("interpolation_syntax", "dollar");
        assertEquals(Configuration.DOLLAR_INTERPOLATION_SYNTAX, cfg.getInterpolationSyntax());
        
        cfg.setSetting("interpolation_syntax", "square_bracket");
        assertEquals(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX, cfg.getInterpolationSyntax());
        
        cfg.setSetting("interpolation_syntax", "legacy");
        assertEquals(Configuration.LEGACY_INTERPOLATION_SYNTAX, cfg.getInterpolationSyntax());
        
        // Camel case:
        cfg.setSetting("interpolationSyntax", "squareBracket");
        assertEquals(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX, cfg.getInterpolationSyntax());
        
        try {
            cfg.setSetting("interpolation_syntax", "no_such_syntax");
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("no_such_syntax"));
        }

        // Catch an oversight that's easy to do:
        try {
            cfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("SQUARE_BRACKET_INTERPOLATION_SYNTAX"));
        }
    }
    
    public void testLazyImportsSetSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

        assertFalse(cfg.getLazyImports());
        assertTrue(cfg.isLazyImportsSet());
        cfg.setSetting("lazy_imports", "true");
        assertTrue(cfg.getLazyImports());
        cfg.setSetting("lazyImports", "false");
        assertFalse(cfg.getLazyImports());
    }
    
    public void testLazyAutoImportsSetSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

        assertNull(cfg.getLazyAutoImports());
        assertTrue(cfg.isLazyAutoImportsSet());
        cfg.setSetting("lazy_auto_imports", "true");
        assertEquals(Boolean.TRUE, cfg.getLazyAutoImports());
        assertTrue(cfg.isLazyAutoImportsSet());
        cfg.setSetting("lazyAutoImports", "false");
        assertEquals(Boolean.FALSE, cfg.getLazyAutoImports());
        cfg.setSetting("lazyAutoImports", "null");
        assertNull(cfg.getLazyAutoImports());
        assertTrue(cfg.isLazyAutoImportsSet());
    }
    
    public void testLocaleSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        assertEquals(Locale.getDefault(), cfg.getLocale());
        assertFalse(cfg.isLocaleExplicitlySet());
        
        Locale nonDefault = Locale.getDefault().equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
        cfg.setLocale(nonDefault);
        assertTrue(cfg.isLocaleExplicitlySet());
        assertEquals(nonDefault, cfg.getLocale());
        
        cfg.unsetLocale();
        assertEquals(Locale.getDefault(), cfg.getLocale());
        assertFalse(cfg.isLocaleExplicitlySet());
        
        cfg.setSetting(Configuration.LOCALE_KEY, "JVM default");
        assertEquals(Locale.getDefault(), cfg.getLocale());
        assertTrue(cfg.isLocaleExplicitlySet());
    }
    
    public void testDefaultEncodingSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        String defaultFileEncoding = System.getProperty("file.encoding");
        assertNotNull(defaultFileEncoding);
        
        assertEquals(defaultFileEncoding, cfg.getDefaultEncoding());
        assertFalse(cfg.isDefaultEncodingExplicitlySet());
        
        String nonDefault = defaultFileEncoding.equalsIgnoreCase("UTF-8") ? "ISO-8859-1" : "UTF-8";
        cfg.setDefaultEncoding(nonDefault);
        assertTrue(cfg.isDefaultEncodingExplicitlySet());
        assertEquals(nonDefault, cfg.getDefaultEncoding());
        
        cfg.unsetDefaultEncoding();
        assertEquals(defaultFileEncoding, cfg.getDefaultEncoding());
        assertFalse(cfg.isDefaultEncodingExplicitlySet());
        
        cfg.setSetting(Configuration.DEFAULT_ENCODING_KEY, "JVM default");
        assertEquals(defaultFileEncoding, cfg.getDefaultEncoding());
        assertTrue(cfg.isDefaultEncodingExplicitlySet());
    }
    
    public void testTimeZoneSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertFalse(cfg.isTimeZoneExplicitlySet());
        
        TimeZone nonDefault = TimeZone.getDefault().equals(DateUtil.UTC) ? TimeZone.getTimeZone("PST") : DateUtil.UTC;
        cfg.setTimeZone(nonDefault);
        assertTrue(cfg.isTimeZoneExplicitlySet());
        assertEquals(nonDefault, cfg.getTimeZone());
        
        cfg.unsetTimeZone();
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertFalse(cfg.isTimeZoneExplicitlySet());
        
        cfg.setSetting(Configuration.TIME_ZONE_KEY, "JVM default");
        assertEquals(TimeZone.getDefault(), cfg.getTimeZone());
        assertTrue(cfg.isTimeZoneExplicitlySet());
    }
    
    public void testNewBuiltinClassResolverSetting() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertSame(TemplateClassResolver.UNRESTRICTED_RESOLVER, cfg.getNewBuiltinClassResolver());
        
        cfg.setSetting(Configuration.NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE,
                "allowed_classes: com.example.C1, com.example.C2, trusted_templates: lib/*, safe.ftl");
        assertThat(cfg.getNewBuiltinClassResolver(), instanceOf(OptInTemplateClassResolver.class));
        
        cfg.setSetting(Configuration.NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE, "safer");
        assertSame(TemplateClassResolver.SAFER_RESOLVER, cfg.getNewBuiltinClassResolver());
        
        cfg.setSetting(Configuration.NEW_BUILTIN_CLASS_RESOLVER_KEY_CAMEL_CASE,
                "allowedClasses: com.example.C1, com.example.C2, trustedTemplates: lib/*, safe.ftl");
        assertThat(cfg.getNewBuiltinClassResolver(), instanceOf(OptInTemplateClassResolver.class));
        
        cfg.setSetting(Configuration.NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE, "allowsNothing");
        assertSame(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER, cfg.getNewBuiltinClassResolver());
        
        cfg.setSetting(Configuration.NEW_BUILTIN_CLASS_RESOLVER_KEY_SNAKE_CASE, "allows_nothing");
        assertSame(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER, cfg.getNewBuiltinClassResolver());
    }

    public void testTruncateBuiltinAlgorithm() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        assertSame(DefaultTruncateBuiltinAlgorithm.ASCII_INSTANCE, cfg.getTruncateBuiltinAlgorithm());

        cfg.setSetting("truncateBuiltinAlgorithm", "unicodE");
        assertSame(DefaultTruncateBuiltinAlgorithm.UNICODE_INSTANCE, cfg.getTruncateBuiltinAlgorithm());

        cfg.setSetting("truncate_builtin_algorithm", "ASCII");
        assertSame(DefaultTruncateBuiltinAlgorithm.ASCII_INSTANCE, cfg.getTruncateBuiltinAlgorithm());

        {
            cfg.setSetting("truncate_builtin_algorithm",
                    "DefaultTruncateBuiltinAlgorithm('...', false)");
            DefaultTruncateBuiltinAlgorithm alg =
                    (DefaultTruncateBuiltinAlgorithm) cfg.getTruncateBuiltinAlgorithm();
            assertEquals("...", alg.getDefaultTerminator());
            assertFalse(alg.getAddSpaceAtWordBoundary());
            assertEquals(3, alg.getDefaultTerminatorLength());
            assertNull(alg.getDefaultMTerminator());
            assertNull(alg.getDefaultMTerminatorLength());
            assertEquals(
                    DefaultTruncateBuiltinAlgorithm.DEFAULT_WORD_BOUNDARY_MIN_LENGTH,
                    alg.getWordBoundaryMinLength(),
                    0);
        }

        {
            cfg.setSetting("truncate_builtin_algorithm",
                    "DefaultTruncateBuiltinAlgorithm(" +
                            "'...', " +
                            "markup(HTMLOutputFormat(), '<span class=trunc>...</span>'), " +
                            "true)");
            DefaultTruncateBuiltinAlgorithm alg =
                    (DefaultTruncateBuiltinAlgorithm) cfg.getTruncateBuiltinAlgorithm();
            assertEquals("...", alg.getDefaultTerminator());
            assertTrue(alg.getAddSpaceAtWordBoundary());
            assertEquals(3, alg.getDefaultTerminatorLength());
            assertEquals("markupOutput(format=HTML, markup=<span class=trunc>...</span>)",
                    alg.getDefaultMTerminator().toString());
            assertEquals(Integer.valueOf(3), alg.getDefaultMTerminatorLength());
            assertEquals(
                    DefaultTruncateBuiltinAlgorithm.DEFAULT_WORD_BOUNDARY_MIN_LENGTH,
                    alg.getWordBoundaryMinLength(),
                    0);
        }

        {
            cfg.setSetting("truncate_builtin_algorithm",
                    "DefaultTruncateBuiltinAlgorithm(" +
                            "DefaultTruncateBuiltinAlgorithm.STANDARD_ASCII_TERMINATOR, null, null, " +
                            "DefaultTruncateBuiltinAlgorithm.STANDARD_M_TERMINATOR, null, null, " +
                            "true, 0.5)");
            DefaultTruncateBuiltinAlgorithm alg =
                    (DefaultTruncateBuiltinAlgorithm) cfg.getTruncateBuiltinAlgorithm();
            assertEquals(DefaultTruncateBuiltinAlgorithm.STANDARD_ASCII_TERMINATOR, alg.getDefaultTerminator());
            assertTrue(alg.getAddSpaceAtWordBoundary());
            assertEquals(5, alg.getDefaultTerminatorLength());
            assertEquals(DefaultTruncateBuiltinAlgorithm.STANDARD_M_TERMINATOR.toString(),
                    alg.getDefaultMTerminator().toString());
            assertEquals(Integer.valueOf(3), alg.getDefaultMTerminatorLength());
            assertEquals(0.5, alg.getWordBoundaryMinLength());
        }
    }

    public void testCFormat() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);

        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setSetting(Configuration.C_FORMAT_KEY_SNAKE_CASE, LegacyCFormat.NAME);
        assertSame(LegacyCFormat.INSTANCE, cfg.getCFormat());
        cfg.setSetting(Configuration.C_FORMAT_KEY_CAMEL_CASE, JSONCFormat.NAME);
        assertSame(JSONCFormat.INSTANCE, cfg.getCFormat());

        cfg.setSetting(Configuration.C_FORMAT_KEY_CAMEL_CASE, "default");
        cfg.setSetting(Configuration.C_FORMAT_KEY_SNAKE_CASE, LegacyCFormat.NAME);

        for (CFormat standardCFormat : new CFormat[] {
                        LegacyCFormat.INSTANCE,
                        JSONCFormat.INSTANCE, JavaScriptCFormat.INSTANCE, JavaScriptOrJSONCFormat.INSTANCE,
                        JavaCFormat.INSTANCE, XSCFormat.INSTANCE
                }) {
            cfg.setSetting(Configuration.C_FORMAT_KEY, standardCFormat.getName());
            assertSame(standardCFormat, cfg.getCFormat());
        }

        // Object Builder value:
        cfg.setSetting(Configuration.C_FORMAT_KEY, JSONCFormat.class.getName() + "()");
        assertSame(JSONCFormat.INSTANCE, cfg.getCFormat());
    }

    public void testFallbackOnNullLoopVariable() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        assertTrue(cfg.getFallbackOnNullLoopVariable());

        cfg.setSetting("fallback_on_null_loop_variable", "false");
        assertFalse(cfg.getFallbackOnNullLoopVariable());

        cfg.setSetting("fallback_on_null_loop_variable", "true");
        assertTrue(cfg.getFallbackOnNullLoopVariable());

        cfg.setSetting("fallbackOnNullLoopVariable", "NO");
        assertFalse(cfg.getFallbackOnNullLoopVariable());
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
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setSetting(
                "objectWrapper",
                "DefaultObjectWrapper(2.3.30, "
                        + "memberAccessPolicy="
                        + ConfigurationTest.class.getName() + ".CONFIG_TEST_MEMBER_ACCESS_POLICY"
                        + ")");
        TemplateHashModel m = (TemplateHashModel) cfg.getObjectWrapper().wrap(new File("x"));
        assertNotNull(m.get("getName"));
        assertNotNull(m.get("isFile"));
        assertNull(m.get("delete"));
    }

    @Test
    public void testMemberAccessPolicySetting2() throws TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setSetting(
                "objectWrapper",
                "DefaultObjectWrapper(2.3.30, "
                        + "memberAccessPolicy=" + LegacyDefaultMemberAccessPolicy.class.getName() + "())");
        assertSame(((DefaultObjectWrapper) cfg.getObjectWrapper()).getMemberAccessPolicy(),
                LegacyDefaultMemberAccessPolicy.INSTANCE);
    }

    @Test
    public void testGetSettingNamesAreSorted() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        for (boolean camelCase : new boolean[] { false, true }) {
            List<String> names = new ArrayList<>(cfg.getSettingNames(camelCase));
            List<String> cfgableNames = new ArrayList<>(new Template(null, "", cfg).getSettingNames(camelCase));
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
                new ArrayList<>(cfg.getSettingNames(false)),
                new ArrayList<>(cfg.getSettingNames(true)));
    }

    @Test
    public void testStaticFieldKeysCoverAllGetSettingNames() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        List<String> names = new ArrayList<>(cfg.getSettingNames(false));
        List<String> cfgableNames = new ArrayList<>(cfg.getSettingNames(false));
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
        
        for (String nameCC : cfg.getSettingNames(true)) {
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
    
    @Test
    public void testGetSupportedBuiltInDirectiveNames() {
        Configuration cfg = new Configuration();
        
        Set<String> allNames = cfg.getSupportedBuiltInDirectiveNames(Configuration.AUTO_DETECT_NAMING_CONVENTION);
        Set<String> lNames = cfg.getSupportedBuiltInDirectiveNames(Configuration.LEGACY_NAMING_CONVENTION);
        Set<String> cNames = cfg.getSupportedBuiltInDirectiveNames(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        
        checkNamingConventionNameSets(allNames, lNames, cNames);
        
        for (String name : cNames) {
            assertThat(name.toLowerCase(), isIn(lNames));
        }
    }

    @Test
    public void testGetSupportedBuiltInNames() {
        Configuration cfg = new Configuration();
        
        Set<String> allNames = cfg.getSupportedBuiltInNames(Configuration.AUTO_DETECT_NAMING_CONVENTION);
        Set<String> lNames = cfg.getSupportedBuiltInNames(Configuration.LEGACY_NAMING_CONVENTION);
        Set<String> cNames = cfg.getSupportedBuiltInNames(Configuration.CAMEL_CASE_NAMING_CONVENTION);
        
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

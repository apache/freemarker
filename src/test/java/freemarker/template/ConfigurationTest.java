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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.junit.Test;

import freemarker.cache.CacheStorageWithGetSize;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.StrongCacheStorage;
import freemarker.cache.TemplateLookupContext;
import freemarker.cache.TemplateLookupResult;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.core.Configurable;
import freemarker.core.Environment;
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
        
        // ---
        
        cfg = new Configuration(newVersion);
        assertUsesNewObjectWrapper(cfg);
        assertUsesNewTemplateLoader(cfg);
        
        cfg.setIncompatibleImprovements(oldVersion);
        assertUsesLegacyObjectWrapper(cfg);
        assertUsesLegacyTemplateLoader(cfg);
        
        // ---
        
        cfg = new Configuration(Configuration.VERSION_2_3_22);
        Object ow = cfg.getObjectWrapper();
        assertEquals(DefaultObjectWrapper.class, ow.getClass());
        assertEquals(Configuration.VERSION_2_3_22,
                ((DefaultObjectWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
    }
    
    public void testTemplateLoadingErrors() throws Exception {
        Configuration cfg = new Configuration();
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().contains("wasn't set") && e.getMessage().contains("default"));
        }
        
        cfg = new Configuration(Configuration.VERSION_2_3_21);
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().contains("wasn't set") && !e.getMessage().contains("default"));
        }
        
        cfg.setClassForTemplateLoading(this.getClass(), "nosuchpackage");
        try {
            cfg.getTemplate("missing.ftl");
            fail();
        } catch (IOException e) {
            assertTrue(!e.getMessage().contains("wasn't set"));
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
            assertTrue(e.getMessage().contains("upgrade"));
        }
        
        try {
            new Configuration(new Version(2, 2, 2));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("2.3.0"));
        }
    }
    
    public void testShowErrorTips() throws Exception {
        Configuration cfg = new Configuration();
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertTrue(e.getMessage().contains("Tip:"));
        }
        
        cfg.setShowErrorTips(false);
        try {
            new Template(null, "${x}", cfg).process(null, NullWriter.INSTANCE);
            fail();
        } catch (TemplateException e) {
            assertFalse(e.getMessage().contains("Tip:"));
        }
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
    
    public void testTemplateLookupStrategyDefaultAndSet() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        assertSame(Configuration.DEFAULT_LOOKUP_STRATEGY, cfg.getTemplateLookupStrategy());
        
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertSame(Configuration.DEFAULT_LOOKUP_STRATEGY, cfg.getTemplateLookupStrategy());
        
        CacheStorageWithGetSize cache = (CacheStorageWithGetSize) cfg.getCacheStorage();
        cfg.setClassForTemplateLoading(ConfigurationTest.class, "");
        assertEquals(0, cache.getSize());
        cfg.getTemplate("toCache1.ftl");
        assertEquals(1, cache.getSize());
        
        cfg.setTemplateLookupStrategy(Configuration.DEFAULT_LOOKUP_STRATEGY);
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
        
        cfg.setTemplateLookupStrategy(Configuration.DEFAULT_LOOKUP_STRATEGY);
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
                assertTrue(e.getMessage().contains(Configurable.API_BUILTIN_ENABLED_KEY));
            }
        }
        
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setAPIBuiltinEnabled(true);
        new Template(null, "${m?api.hashCode()}", cfg)
                .process(Collections.singletonMap("m", new HashMap()), NullWriter.INSTANCE);
    }
    
    private static class MyScalarModel implements TemplateScalarModel {

        public String getAsString() throws TemplateModelException {
            return "my";
        }
        
    }
    
}

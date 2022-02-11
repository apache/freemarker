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

package freemarker.core;

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.cache.CacheStorage;
import freemarker.cache.MruCacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.core.subpkg.PublicWithMixedConstructors;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.jython.JythonWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import freemarker.template.utility.WriteProtectable;

@SuppressWarnings("boxing")
public class ObjectBuilderSettingsTest {

    @Test
    public void newInstanceTest() throws Exception {
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(4f, res.f, 0);
            assertFalse(res.b);
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(4f, res.f, 0);
            assertFalse(res.b);
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(1.5, -20, 8589934592, true)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(1.5f, res.f, 0);
            assertEquals(Integer.valueOf(-20), res.i);
            assertEquals(8589934592l, res.l);
            assertTrue(res.b);
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(1, true)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(2f, res.f, 0);
            assertEquals(Integer.valueOf(1), res.i);
            assertEquals(2l, res.l);
            assertTrue(res.b);
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(11, 22)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(3f, res.f, 0);
            assertEquals(Integer.valueOf(11), res.i);
            assertEquals(22l, res.l);
            assertFalse(res.b);
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(p1 = 1, p2 = 2, p3 = true, p4 = 's')",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(4f, res.f, 0);
            assertFalse(res.b);
            assertEquals(1d, res.getP1(), 0);
            assertEquals(2, res.getP2());
            assertTrue(res.isP3());
            assertEquals("s", res.getP4());
        }

        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1("
                    + "null, 2, p1 = 1, p2 = 2, p3 = false, p4 = null)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertNull(res.i);
            assertEquals(2, res.l, 0);
            assertEquals(3f, res.f, 0);
            assertFalse(res.b);
            assertEquals(1d, res.getP1(), 0);
            assertEquals(2, res.getP2());
            assertFalse(res.isP3());
            assertNull(res.getP4());
        }
        
        {
            // Deliberately odd spacings
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "\t\tfreemarker . core.\n\tObjectBuilderSettingsTest$TestBean1(\n\r\tp1=1\n,p2=2,p3=true,p4='s'  )",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(4f, res.f, 0);
            assertFalse(res.b);
            assertEquals(1d, res.getP1(), 0);
            assertEquals(2, res.getP2());
            assertTrue(res.isP3());
            assertEquals("s", res.getP4());
        }
        
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(1, true, p2 = 2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(2f, res.f, 0);
            assertEquals(Integer.valueOf(1), res.i);
            assertEquals(2l, res.l);
            assertTrue(res.b);
            assertEquals(0d, res.getP1(), 0);
            assertEquals(2, res.getP2());
            assertFalse(res.isP3());
        }
    }
    
    @Test
    public void builderTest() throws Exception {
        {
            // Backward-compatible mode, no builder:
            TestBean2 res = (TestBean2) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean2",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertFalse(res.built);
            assertEquals(0, res.x);
        }
        
        {
            TestBean2 res = (TestBean2) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean2()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertTrue(res.built);
            assertEquals(0, res.x);
        }
        
        {
            TestBean2 res = (TestBean2) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean2(x = 1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertTrue(res.built);
            assertEquals(1, res.x);
        }
        
        {
            TestBean2 res = (TestBean2) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean2(1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertTrue(res.built);
            assertEquals(1, res.x);
        }
    }

    @Test
    public void staticInstanceTest() throws Exception {
        // Backward compatible mode:
        {
            TestBean5 res = (TestBean5) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean5",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(0, res.i);
            assertEquals(0, res.x);
            assertNotSame(TestBean5.INSTANCE, res);
        }
        
        {
            TestBean5 res = (TestBean5) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean5()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(0, res.i);
            assertEquals(0, res.x);
            assertSame(TestBean5.INSTANCE, res); //!
        }
        
        {
            TestBean5 res = (TestBean5) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean5(x = 1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(0, res.i);
            assertEquals(1, res.x);
            assertNotSame(TestBean5.INSTANCE, res);
        }

        {
            TestBean5 res = (TestBean5) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean5(1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(1, res.i);
            assertEquals(0, res.x);
            assertNotSame(TestBean5.INSTANCE, res);
        }
    }
    
    @Test
    public void writeProtectionTest() throws Exception {
        {
            TestBean3 res = (TestBean3) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean3(x = 1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(1, res.x);
            assertTrue(res.isWriteProtected());
            try {
                res.setX(2);
                fail();
            } catch (IllegalStateException e) {
                // expected
            }
        }
        
        {
            // Backward-compatible mode
            TestBean3 res = (TestBean3) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean3",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(0, res.x);
            assertFalse(res.isWriteProtected());
            res.setX(2);
        }
    }

    @Test
    public void stringLiteralsTest() throws Exception {
        {
            TestBean4 res = (TestBean4) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean4(\"\", '', s3 = r\"\", s4 = r'')",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("", res.getS1());
            assertEquals("", res.getS2());
            assertEquals("", res.getS3());
            assertEquals("", res.getS4());
        }
        
        {
            TestBean4 res = (TestBean4) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean4(\"a\", 'b', s3 = r\"c\", s4 = r'd')",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("a", res.getS1());
            assertEquals("b", res.getS2());
            assertEquals("c", res.getS3());
            assertEquals("d", res.getS4());
        }
        
        {
            TestBean4 res = (TestBean4) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean4(\"a'A\", 'b\"B', s3 = r\"c'C\", s4 = r'd\"D')",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("a'A", res.getS1());
            assertEquals("b\"B", res.getS2());
            assertEquals("c'C", res.getS3());
            assertEquals("d\"D", res.getS4());
        }
        
        {
            TestBean4 res = (TestBean4) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean4("
                    + "\"a\\nA\\\"a\\\\A\", 'a\\nA\\'a\\\\A', s3 = r\"a\\n\\A\", s4 = r'a\\n\\A')",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("a\nA\"a\\A", res.getS1());
            assertEquals("a\nA'a\\A", res.getS2());
            assertEquals("a\\n\\A", res.getS3());
            assertEquals("a\\n\\A", res.getS4());
        }
    }

    @Test
    public void nestedBuilderTest() throws Exception {
        {
            TestBean6 res = (TestBean6) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean6("
                    + "freemarker.core.ObjectBuilderSettingsTest$TestBean1(11, 22, p4 = 'foo'),"
                    + "1,"
                    + "freemarker.core.ObjectBuilderSettingsTest$TestBean2(11),"
                    + "y=2,"
                    + "b3=freemarker.core.ObjectBuilderSettingsTest$TestBean2(x = 22)"
                    + ")",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(Integer.valueOf(11), res.b1.i);
            assertEquals(22, res.b1.l);
            assertEquals("foo", res.b1.p4);
            assertEquals(1, res.x);
            assertEquals(11, res.b2.x);
            assertEquals(2, res.y);
            assertEquals(22, res.b3.x);
            assertNull(res.b4);
        }
        
        {
            TestBean6 res = (TestBean6) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean6("
                    + "null,"
                    + "-1,"
                    + "null,"
                    + "b4=freemarker.core.ObjectBuilderSettingsTest$TestBean6("
                    + "   freemarker.core.ObjectBuilderSettingsTest$TestBean1(11, 22, p4 = 'foo'),"
                    + "   1,"
                    + "   freemarker.core.ObjectBuilderSettingsTest$TestBean2(11),"
                    + "   y=2,"
                    + "   b3=freemarker.core.ObjectBuilderSettingsTest$TestBean2(x = 22)"
                    + "),"
                    + "y=2"
                    + ")",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertNull(res.b1);
            assertEquals(-1, res.x);
            assertNull(res.b2);
            assertEquals(2, res.y);
            assertEquals(Integer.valueOf(11), res.b4.b1.i);
            assertEquals(22, res.b4.b1.l);
            assertEquals("foo", res.b4.b1.p4);
            assertEquals(1, res.b4.x);
            assertEquals(11, res.b4.b2.x);
            assertEquals(2, res.b4.y);
            assertEquals(22, res.b4.b3.x);
            assertNull(res.b4.b4);
        }
    }
    
    @Test
    public void beansWrapperTest() throws Exception {
        BeansWrapper bw = (BeansWrapper) _ObjectBuilderSettingEvaluator.eval(
                "BeansWrapper(2.3.21, simpleMapWrapper=true, exposeFields=true)",
                ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent());
        assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
        assertTrue(bw.isSimpleMapWrapper());
        assertTrue(bw.isExposeFields());
    }

    @Test
    public void defaultObjectWrapperTest() throws Exception {
        DefaultObjectWrapper bw = (DefaultObjectWrapper) _ObjectBuilderSettingEvaluator.eval(
                "DefaultObjectWrapper(2.3.21)",
                ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent());
        assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
        assertFalse(bw.isExposeFields());
    }

    @Test
    public void jythonWrapperTest() throws Exception {
        JythonWrapper jw = (JythonWrapper) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.ext.jython.JythonWrapper()",
                ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent());
        assertSame(JythonWrapper.INSTANCE, jw);
    }

    @Test
    public void configurationPropertiesTest() throws TemplateException {
        final Configuration cfg = new Configuration();
        
        {
            Properties props = new Properties();
            props.setProperty(Configurable.OBJECT_WRAPPER_KEY, "freemarker.ext.beans.BeansWrapper(2.3.21)");
            props.setProperty(Configurable.ARITHMETIC_ENGINE_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyArithmeticEngine");
            props.setProperty(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyTemplateExceptionHandler");
            props.setProperty(Configuration.CACHE_STORAGE_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyCacheStorage()");
            props.setProperty(Configurable.NEW_BUILTIN_CLASS_RESOLVER_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyNewBuiltinClassResolver()");
            props.setProperty(Configuration.DEFAULT_ENCODING_KEY, "utf-8");
            props.setProperty(Configuration.TEMPLATE_LOADER_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyTemplateLoader()");
            cfg.setSettings(props);
            assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
            assertTrue(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
            assertEquals(Configuration.VERSION_2_3_21, ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
            assertEquals(DummyArithmeticEngine.class, cfg.getArithmeticEngine().getClass());
            assertEquals(DummyTemplateExceptionHandler.class, cfg.getTemplateExceptionHandler().getClass());
            assertEquals(DummyCacheStorage.class, cfg.getCacheStorage().getClass());
            assertEquals(DummyNewBuiltinClassResolver.class, cfg.getNewBuiltinClassResolver().getClass());
            assertEquals(DummyTemplateLoader.class, cfg.getTemplateLoader().getClass());
            assertEquals("utf-8", cfg.getDefaultEncoding());
        }
        
        {
            Properties props = new Properties();
            props.setProperty(Configurable.OBJECT_WRAPPER_KEY, "defAult");
            props.setProperty(Configurable.ARITHMETIC_ENGINE_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyArithmeticEngine(x = 1)");
            props.setProperty(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY,
                    "freemarker.core.ObjectBuilderSettingsTest$DummyTemplateExceptionHandler(x = 1)");
            props.setProperty(Configuration.CACHE_STORAGE_KEY,
                    "soft: 500, strong: 100");
            props.setProperty(Configurable.NEW_BUILTIN_CLASS_RESOLVER_KEY,
                    "safer");
            cfg.setSettings(props);
            assertEquals(DefaultObjectWrapper.class, cfg.getObjectWrapper().getClass());
            assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
            assertEquals(1, ((DummyArithmeticEngine) cfg.getArithmeticEngine()).getX());
            assertEquals(1, ((DummyTemplateExceptionHandler) cfg.getTemplateExceptionHandler()).getX());
            assertEquals(Configuration.VERSION_2_3_0, ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
            assertEquals(500, ((MruCacheStorage) cfg.getCacheStorage()).getSoftSizeLimit());
            assertEquals(TemplateClassResolver.SAFER_RESOLVER, cfg.getNewBuiltinClassResolver());
            assertEquals("utf-8", cfg.getDefaultEncoding());
        }

        {
            Properties props = new Properties();
            props.setProperty(Configurable.OBJECT_WRAPPER_KEY, "Beans");
            props.setProperty(Configurable.ARITHMETIC_ENGINE_KEY, "bigdecimal");
            props.setProperty(Configurable.TEMPLATE_EXCEPTION_HANDLER_KEY, "rethrow");
            cfg.setSettings(props);
            assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
            assertSame(ArithmeticEngine.BIGDECIMAL_ENGINE, cfg.getArithmeticEngine());
            assertSame(TemplateExceptionHandler.RETHROW_HANDLER, cfg.getTemplateExceptionHandler());
            assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
            assertEquals(Configuration.VERSION_2_3_0, ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        }

        {
            Properties props = new Properties();
            props.setProperty(Configurable.OBJECT_WRAPPER_KEY, "freemarker.ext.beans.BeansWrapper");
            cfg.setSettings(props);
            assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
            assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
            assertEquals(Configuration.VERSION_2_3_0, ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        }
        
        {
            Properties props = new Properties();
            props.setProperty(Configurable.OBJECT_WRAPPER_KEY, "DefaultObjectWrapper(2.3.19)");
            cfg.setSettings(props);
            assertEquals(DefaultObjectWrapper.class, cfg.getObjectWrapper().getClass());
            assertTrue(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
            assertEquals(Configuration.VERSION_2_3_0, ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        }
    }
    
    @Test
    public void timeZoneTest() throws _ObjectBuilderSettingEvaluationException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        for (String timeZoneId : new String[] { "GMT+01", "GMT", "UTC" }) {
            TestBean8 result = (TestBean8) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean8(timeZone=TimeZone('" + timeZoneId + "'))",
                    TestBean8.class, false, new _SettingEvaluationEnvironment());
            assertEquals(TimeZone.getTimeZone(timeZoneId), result.getTimeZone());
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean8(timeZone=TimeZone('foobar'))",
                    TestBean8.class, false, new _SettingEvaluationEnvironment());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getCause().getMessage(),
                    allOf(containsStringIgnoringCase("unrecognized"), containsString("foobar")));
        }
    }
    
    @Test
    public void configureBeanTest() throws Exception {
        final TestBean7 bean = new TestBean7();
        final String src = "a/b(s='foo', x=1, b=true), bar";
        int nextPos = _ObjectBuilderSettingEvaluator.configureBean(src, src.indexOf('(') + 1, bean,
                _SettingEvaluationEnvironment.getCurrent());
        assertEquals("foo", bean.getS());
        assertEquals(1, bean.getX());
        assertTrue(bean.isB());
        assertEquals(", bar", src.substring(nextPos));
    }
    
    @Test
    public void parsingErrors() throws Exception {
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(1,,2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("\",\""));
        }

        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(x=1,2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("must precede named"));
        }


        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(x=1;2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("\";\""));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(1,2))",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("\")\""));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "foo.Bar('s${x}s'))",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("${...}"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "foo.Bar('s#{x}s'))",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("#{...}"));
        }
    }

    @Test
    public void semanticErrors() throws Exception {
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$XTestBean1(1,2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("Failed to get class"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(true, 2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("constructor"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(x = 1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("no writeable JavaBeans property called \"x\""));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1(p1 = 1, p1 = 2)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("twice"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "java.util.HashMap()",
                    ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("is not a(n) " + ObjectWrapper.class.getName()));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "null",
                    ObjectWrapper.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("can't be null"));
        }
    }
    
    @Test
    public void testLiteralAsObjectBuilder() throws Exception {
        assertNull(_ObjectBuilderSettingEvaluator.eval(
                "null",
                ObjectWrapper.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals("foo", _ObjectBuilderSettingEvaluator.eval(
                "'foo'",
                CharSequence.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Boolean.TRUE, _ObjectBuilderSettingEvaluator.eval(
                "  true  ",
                Object.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Double.valueOf("1.23"), _ObjectBuilderSettingEvaluator.eval(
                "1.23 ",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(new Version(1, 2, 3), _ObjectBuilderSettingEvaluator.eval(
                " 1.2.3",
                Object.class, true, _SettingEvaluationEnvironment.getCurrent()));
    }

    @Test
    public void testNumberLiteralJavaTypes() throws Exception {
        assertEquals(Double.valueOf("1.0"), _ObjectBuilderSettingEvaluator.eval(
                "1.0",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));

        assertEquals(new BigInteger("-9223372036854775809"), _ObjectBuilderSettingEvaluator.eval(
                "-9223372036854775809",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(new BigInteger("9223372036854775808"), _ObjectBuilderSettingEvaluator.eval(
                "9223372036854775808",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        
        assertEquals(Long.valueOf(-9223372036854775808L), _ObjectBuilderSettingEvaluator.eval(
                "-9223372036854775808",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Long.valueOf(9223372036854775807L), _ObjectBuilderSettingEvaluator.eval(
                "9223372036854775807",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        
        assertEquals(Integer.valueOf(-2147483648), _ObjectBuilderSettingEvaluator.eval(
                "-2147483648",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Integer.valueOf(2147483647), _ObjectBuilderSettingEvaluator.eval(
                "2147483647",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        
        assertEquals(Integer.valueOf(-1), _ObjectBuilderSettingEvaluator.eval(
                "-1",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Integer.valueOf(1), _ObjectBuilderSettingEvaluator.eval(
                "1",
                Number.class, true, _SettingEvaluationEnvironment.getCurrent()));
    }
    
    @Test
    public void testListLiterals() throws Exception {
        {
            ArrayList<Object> expected = new ArrayList();
            expected.add("s");
            expected.add(null);
            expected.add(true);
            expected.add(new TestBean9(1));
            expected.add(ImmutableList.of(11, 22, 33));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    "['s', null, true, freemarker.core.ObjectBuilderSettingsTest$TestBean9(1), [11, 22, 33]]",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    "  [  's'  ,  null ,  true , freemarker.core.ObjectBuilderSettingsTest$TestBean9(1) ,"
                    + "  [ 11 , 22 , 33 ]  ]  ",
                    Collection.class, false, _SettingEvaluationEnvironment.getCurrent()));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    "['s',null,true,freemarker.core.ObjectBuilderSettingsTest$TestBean9(1),[11,22,33]]",
                    List.class, false, _SettingEvaluationEnvironment.getCurrent()));
        }
        
        assertEquals(Collections.emptyList(), _ObjectBuilderSettingEvaluator.eval(
                "[]",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Collections.emptyList(), _ObjectBuilderSettingEvaluator.eval(
                "[  ]",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));

        assertEquals(Collections.singletonList(123), _ObjectBuilderSettingEvaluator.eval(
                "[123]",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Collections.singletonList(123), _ObjectBuilderSettingEvaluator.eval(
                "[ 123 ]",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        
        assertEquals(new TestBean9(1, ImmutableList.of("a", "b")), _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.ObjectBuilderSettingsTest$TestBean9(1, ['a', 'b'])",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "[1,]",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \"]\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "[,1]",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \",\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "1]",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \"]\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "[1",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("end of"));
        }
    }

    @Test
    public void testMapLiterals() throws Exception {
        {
            HashMap<String, Object> expected = new HashMap();
            expected.put("k1", "s");
            expected.put("k2", null);
            expected.put("k3", true);
            expected.put("k4", new TestBean9(1));
            expected.put("k5", ImmutableList.of(11, 22, 33));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    "{'k1': 's', 'k2': null, 'k3': true, "
                    + "'k4': freemarker.core.ObjectBuilderSettingsTest$TestBean9(1), 'k5': [11, 22, 33]}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    " {  'k1'  :  's'  ,  'k2' :  null  , 'k3' : true , "
                    + "'k4' : freemarker.core.ObjectBuilderSettingsTest$TestBean9 ( 1 ) , 'k5' : [ 11 , 22 , 33 ] } ",
                    Map.class, false, _SettingEvaluationEnvironment.getCurrent()));
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    " {'k1':'s','k2':null,'k3':true,"
                    + "'k4':freemarker.core.ObjectBuilderSettingsTest$TestBean9(1),'k5':[11,22,33]}",
                    LinkedHashMap.class, false, _SettingEvaluationEnvironment.getCurrent()));
        }
        
        {
            HashMap<Object, String> expected = new HashMap();
            expected.put(true, "T");
            expected.put(1, "O");
            expected.put(new TestBean9(1), "B");
            expected.put(ImmutableList.of(11, 22, 33), "L");
            assertEquals(expected, _ObjectBuilderSettingEvaluator.eval(
                    "{ true: 'T', 1: 'O', freemarker.core.ObjectBuilderSettingsTest$TestBean9(1): 'B', "
                    + "[11, 22, 33]: 'L' }",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        }
        
        assertEquals(Collections.emptyMap(), _ObjectBuilderSettingEvaluator.eval(
                "{}",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Collections.emptyMap(), _ObjectBuilderSettingEvaluator.eval(
                "{  }",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));

        assertEquals(Collections.singletonMap("k1", 123), _ObjectBuilderSettingEvaluator.eval(
                "{'k1':123}",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        assertEquals(Collections.singletonMap("k1", 123), _ObjectBuilderSettingEvaluator.eval(
                "{ 'k1' : 123 }",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        
        assertEquals(new TestBean9(1, ImmutableMap.of(11, "a", 22, "b")), _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.ObjectBuilderSettingsTest$TestBean9(1, { 11: 'a', 22: 'b' })",
                Object.class, false, _SettingEvaluationEnvironment.getCurrent()));
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "{1:2,}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \"}\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "{,1:2}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \",\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "1:2}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \":\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "1}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("found character \"}\""));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "{1",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("end of"));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "{1:",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("end of"));
        }
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "{null:1}",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(), containsString("null as key"));
        }
    }

    @Test
    public void testMethodParameterNumberTypes() throws Exception {
        {
            TestBean8 result = (TestBean8) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean8(anyObject=1)",
                    TestBean8.class, false, new _SettingEvaluationEnvironment());
            assertEquals(result.getAnyObject(), 1);
        }
        {
            TestBean8 result = (TestBean8) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean8(anyObject=2147483649)",
                    TestBean8.class, false, new _SettingEvaluationEnvironment());
            assertEquals(result.getAnyObject(), 2147483649L);
        }
        {
            TestBean8 result = (TestBean8) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean8(anyObject=1.0)",
                    TestBean8.class, false, new _SettingEvaluationEnvironment());
            // Like in FTL, non-integer numbers are BigDecimal-s, that are later coerced to the actual parameter type.
            // However, here the type is Object, so it remains BigDecimal.
            assertEquals(new BigDecimal("1.0"), result.getAnyObject());
        }
    }
    
    @Test
    public void testNonMethodParameterNumberTypes() throws Exception {
        assertEqualsEvaled(Integer.valueOf(1), "1");
        assertEqualsEvaled(Double.valueOf(1), "1.0");
        assertEqualsEvaled(Long.valueOf(2147483649l), "2147483649");

        assertEqualsEvaled(Double.valueOf(1), "1d");
        assertEqualsEvaled(Double.valueOf(1), "1D");
        assertEqualsEvaled(Float.valueOf(1), "1f");
        assertEqualsEvaled(Float.valueOf(1), "1F");
        assertEqualsEvaled(Long.valueOf(1), "1l");
        assertEqualsEvaled(Long.valueOf(1), "1L");
        assertEqualsEvaled(BigDecimal.valueOf(1), "1bd");
        assertEqualsEvaled(BigDecimal.valueOf(1), "1Bd");
        assertEqualsEvaled(BigDecimal.valueOf(1), "1BD");
        assertEqualsEvaled(BigInteger.valueOf(1), "1bi");
        assertEqualsEvaled(BigInteger.valueOf(1), "1bI");
        
        assertEqualsEvaled(Float.valueOf(1.5f), "1.5f");
        assertEqualsEvaled(Double.valueOf(1.5), "1.5d");
        assertEqualsEvaled(BigDecimal.valueOf(1.5), "1.5bd");
        
        assertEqualsEvaled(
                ImmutableList.of(-1, -0.5, new BigDecimal("-0.1")),
                "[ -1, -0.5, -0.1bd ]");
        assertEqualsEvaled(
                ImmutableMap.of(-1, -11, -0.5, -0.55, new BigDecimal("-0.1"), new BigDecimal("-0.11")),
                "{ -1: -11, -0.5: -0.55, -0.1bd: -0.11bd }");
    }
    
    @Test
    public void testStaticFields() throws Exception {
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1("
                    + "freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST, true)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(TestStaticFields.CONST, (int) res.i);
        }
        {
            TestBean1 res = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1("
                    + "p2 = freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(TestStaticFields.CONST, res.getP2());
        }
        assertEqualsEvaled(123, "freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST");
        
        // With shorthand class name:
        assertEqualsEvaled(Configuration.AUTO_DETECT_TAG_SYNTAX, "Configuration.AUTO_DETECT_TAG_SYNTAX");
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.ObjectBuilderSettingsTest$TestBean1("
                    + "p2 = freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST())",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(),
                    containsString("freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST"));
        }
        try {
            assertEqualsEvaled(123, "freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST()");
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(),
                    containsString("freemarker.core.ObjectBuilderSettingsTest$TestStaticFields.CONST"));
        }
        try {
            assertEqualsEvaled(123, "java.lang.String(freemarker.core.ObjectBuilderSettingsTest$TestBean5.INSTANCE)");
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertThat(e.getMessage(),
                    containsString("freemarker.core.ObjectBuilderSettingsTest$TestBean5()"));
        }
    }
    
    private void assertEqualsEvaled(Object expectedValue, String s)
            throws _ObjectBuilderSettingEvaluationException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Object actualValue = _ObjectBuilderSettingEvaluator.eval(
                s, Object.class, true, _SettingEvaluationEnvironment.getCurrent());
        assertEquals(expectedValue, actualValue);
    }
    
    @Test
    public void visibilityTest() throws Exception {
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PackageVisibleAll()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertEquals(IllegalAccessException.class, e.getCause().getClass());
        }

        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PackageVisibleWithPublicConstructor()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertEquals(IllegalAccessException.class, e.getCause().getClass());
        }

        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PublicWithPackageVisibleConstructor()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertEquals(IllegalAccessException.class, e.getCause().getClass());
        }
        
        {
            Object o = _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PublicAll()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals(freemarker.core.subpkg.PublicAll.class, o.getClass());
        }
        
        {
            Object o = _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PublicWithMixedConstructors(1)",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("Integer", ((PublicWithMixedConstructors) o).getS());
        }
        
        
        {
            Object o = _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.subpkg.PackageVisibleAllWithBuilder()",
                    Object.class, false, _SettingEvaluationEnvironment.getCurrent());
            assertEquals("freemarker.core.subpkg.PackageVisibleAllWithBuilder", o.getClass().getName());
        }
    }

    public static class TestBean1 {
        float f;
        Integer i;
        long l;
        boolean b;
        
        double p1;
        int p2;
        boolean p3;
        String p4;
        
        public TestBean1(float f, Integer i, long l, boolean b) {
            this.f = f;
            this.i = i;
            this.l = l;
            this.b = b;
        }
        
        public TestBean1(Integer i, boolean b) {
            this.f = 2;
            this.i = i;
            this.l = 2;
            this.b = b;
        }
    
        public TestBean1(Integer i, long l) {
            this.f = 3;
            this.i = i;
            this.l = l;
            this.b = false;
        }
        
        public TestBean1() {
            this.f = 4;
        }
    
        public double getP1() {
            return p1;
        }
    
        public void setP1(double p1) {
            this.p1 = p1;
        }
    
        public int getP2() {
            return p2;
        }
    
        public void setP2(int p2) {
            this.p2 = p2;
        }
    
        public boolean isP3() {
            return p3;
        }
    
        public void setP3(boolean p3) {
            this.p3 = p3;
        }

        public String getP4() {
            return p4;
        }

        public void setP4(String p4) {
            this.p4 = p4;
        }
        
    }
    
    public static class TestBean2 {
        final boolean built;
        final int x;

        public TestBean2() {
            this.built = false;
            this.x = 0;
        }
        
        public TestBean2(int x) {
            this.built = false;
            this.x = x;
        }

        public TestBean2(TestBean2Builder builder) {
            this.built = true;
            this.x = builder.x;
        }
        
    }

    public static class TestBean2Builder {
        int x;
        
        public TestBean2Builder() { }

        public TestBean2Builder(int x) {
            this.x = x;
        }
        
        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
        
        public TestBean2 build() {
            return new TestBean2(this);
        }
        
    }

    public static class TestBean3 implements WriteProtectable {
        
        private boolean writeProtected;
        
        private int x;

        public void writeProtect() {
            writeProtected = true;
        }

        public boolean isWriteProtected() {
            return writeProtected;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            if (writeProtected) throw new IllegalStateException();
            this.x = x;
        }
        
    }
    
    public static class TestBean4 {
        private final String s1, s2;
        private String s3, s4;
        
        public TestBean4(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
        
        public String getS1() {
            return s1;
        }

        public String getS2() {
            return s2;
        }

        public String getS3() {
            return s3;
        }
        
        public void setS3(String s3) {
            this.s3 = s3;
        }
        
        public String getS4() {
            return s4;
        }
        
        public void setS4(String s4) {
            this.s4 = s4;
        }
        
    }
    
    public static class TestBean5 {
        
        public final static TestBean5 INSTANCE = new TestBean5();
        
        private final int i;
        private int x;
        
        public TestBean5() {
            i = 0;
        }

        public TestBean5(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
        
    }

    public static class TestBean6 {
        private final TestBean1 b1;
        private int x;
        private final TestBean2 b2;
        private int y;
        private TestBean2 b3;
        private TestBean6 b4;
        
        public TestBean6(TestBean1 b1, int x, TestBean2 b2) {
            this.b1 = b1;
            this.x = x;
            this.b2 = b2;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public TestBean2 getB3() {
            return b3;
        }

        public void setB3(TestBean2 b3) {
            this.b3 = b3;
        }

        public TestBean1 getB1() {
            return b1;
        }

        public TestBean2 getB2() {
            return b2;
        }

        public TestBean6 getB4() {
            return b4;
        }

        public void setB4(TestBean6 b4) {
            this.b4 = b4;
        }
        
    }
    
    public class TestBean7 {

        private String s;
        private int x;
        private boolean b;

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public boolean isB() {
            return b;
        }

        public void setB(boolean b) {
            this.b = b;
        }

        @Override
        public String toString() {
            return "TestBean [s=" + s + ", x=" + x + ", b=" + b + "]";
        }

    }
    
    public static class TestBean8 {
        private TimeZone timeZone;
        private Object anyObject;
        private List<?> list;
        
        public TimeZone getTimeZone() {
            return timeZone;
        }
        
        public void setTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
        
        public Object getAnyObject() {
            return anyObject;
        }
        
        public void setAnyObject(Object anyObject) {
            this.anyObject = anyObject;
        }

        public List<?> getList() {
            return list;
        }
        
        public void setList(List<?> list) {
            this.list = list;
        }
        
    }
    
    public static class TestBean9 {
        
        private final int n;
        private final List<?> list;
        private final Map<?, ?> map;

        public TestBean9(int n) {
            this(n, null, null);
        }

        public TestBean9(int n, List<?> list) {
            this(n, list, null);
        }

        public TestBean9(int n, Map<?, ?> map) {
            this(n, null, map);
        }
        
        public TestBean9(int n, List<?> list, Map<?, ?> map) {
            this.n = n;
            this.list = list;
            this.map = map;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            result = prime * result + n;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TestBean9 other = (TestBean9) obj;
            if (list == null) {
                if (other.list != null) return false;
            } else if (!list.equals(other.list)) return false;
            if (map == null) {
                if (other.map != null) return false;
            } else if (!map.equals(other.map)) return false;
            if (n != other.n) return false;
            return true;
        }
        
    }
    
    public static class TestStaticFields {
        public static final int CONST = 123;
    }
    
    public static class DummyArithmeticEngine extends ArithmeticEngine {
        
        private int x;

        @Override
        public int compareNumbers(Number first, Number second) throws TemplateException {
            return 0;
        }

        @Override
        public Number add(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number subtract(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number multiply(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number divide(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number modulus(Number first, Number second) throws TemplateException {
            return null;
        }

        @Override
        public Number toNumber(String s) {
            return null;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
        
    }
    
    public static class DummyTemplateExceptionHandler implements TemplateExceptionHandler {
        
        private int x;

        public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
        
    }
    
    public static class DummyCacheStorage implements CacheStorage {
        
        public Object get(Object key) {
            return null;
        }

        public void put(Object key, Object value) {
        }

        public void remove(Object key) {
        }

        public void clear() {
        }
        
    }
    
    public static class DummyNewBuiltinClassResolver implements TemplateClassResolver {

        public Class resolve(String className, Environment env, Template template) throws TemplateException {
            return null;
        }
        
    }
    
    public static class DummyTemplateLoader implements TemplateLoader {

        public Object findTemplateSource(String name) throws IOException {
            return null;
        }

        public long getLastModified(Object templateSource) {
            return 0;
        }

        public Reader getReader(Object templateSource, String encoding) throws IOException {
            return null;
        }

        public void closeTemplateSource(Object templateSource) throws IOException {
        }
        
    }
    
}

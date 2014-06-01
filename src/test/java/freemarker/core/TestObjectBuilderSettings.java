package freemarker.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.jython.JythonWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.WriteProtectable;

public class TestObjectBuilderSettings {

    @Test
    public void newInstanceTest() throws Exception {
        TestBean1 tb;
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(4f, tb.f, 0);
        assertFalse(tb.b);
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1()",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(4f, tb.f, 0);
        assertFalse(tb.b);
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1(1.5, -20, 8589934592, true)",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(1.5f, tb.f, 0);
        assertEquals(-20, tb.i);
        assertEquals(8589934592l, tb.l);
        assertTrue(tb.b);
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1(1, true)",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(2f, tb.f, 0);
        assertEquals(1, tb.i);
        assertEquals(2l, tb.l);
        assertTrue(tb.b);
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1(11, 22)",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(3f, tb.f, 0);
        assertEquals(11, tb.i);
        assertEquals(22l, tb.l);
        assertFalse(tb.b);
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1(p1 = 1, p2 = 2, p3 = true)",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(4f, tb.f, 0);
        assertFalse(tb.b);
        assertEquals(1d, tb.getP1(), 0);
        assertEquals(2, tb.getP2());
        assertTrue(tb.isP3());
        
        tb = (TestBean1) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.core.TestObjectBuilderSettings$TestBean1(1, true, p2 = 2)",
                Object.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(2f, tb.f, 0);
        assertEquals(1, tb.i);
        assertEquals(2l, tb.l);
        assertTrue(tb.b);
        assertEquals(0d, tb.getP1(), 0);
        assertEquals(2, tb.getP2());
        assertFalse(tb.isP3());
    }

    @Test
    public void beansWrapperTest() throws Exception {
        BeansWrapper bw = (BeansWrapper) _ObjectBuilderSettingEvaluator.eval(
                "BeansWrapper(2.3.21, simpleMapWrapper=true, exposeFields=true)",
                ObjectWrapper.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
        assertTrue(bw.isSimpleMapWrapper());
        assertTrue(bw.isExposeFields());
    }

    @Test
    public void defaultObjectWrapperTest() throws Exception {
        DefaultObjectWrapper bw = (DefaultObjectWrapper) _ObjectBuilderSettingEvaluator.eval(
                "DefaultObjectWrapper(2.3.21)",
                ObjectWrapper.class, _SettingEvaluationEnvironment.getInstance());
        assertEquals(new Version(2, 3, 21), bw.getIncompatibleImprovements());
        assertFalse(bw.isExposeFields());
    }

    @Test
    public void jythonWrapperTest() throws Exception {
        JythonWrapper jw = (JythonWrapper) _ObjectBuilderSettingEvaluator.eval(
                "freemarker.ext.jython.JythonWrapper()",
                ObjectWrapper.class, _SettingEvaluationEnvironment.getInstance());
        assertNotNull(jw);
    }

    public static class TestBean1 {
        float f;
        int i;
        long l;
        boolean b;
        
        double p1;
        int p2;
        boolean p3;
        
        public TestBean1(float f, int i, long l, boolean b) {
            this.f = f;
            this.i = i;
            this.l = l;
            this.b = b;
        }
        
        public TestBean1(int i, boolean b) {
            this.f = 2;
            this.i = i;
            this.l = 2;
            this.b = b;
        }

        public TestBean1(int i, long l) {
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
        
    }
    
    @Test
    public void configurationPropertiesTest() throws TemplateException {
        Properties props = new Properties();
        props.setProperty(Configuration.OBJECT_WRAPPER_KEY, "freemarker.ext.beans.BeansWrapper(2.3.21)");
        props.setProperty(Configuration.DEFAULT_ENCODING_KEY, "utf-8");
        Configuration cfg = new Configuration();
        cfg.setSettings(props);
        assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
        assertTrue(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
        assertEquals(new Version(2, 3, 21), ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        assertEquals("utf-8", cfg.getDefaultEncoding());
        
        props = new Properties();
        props.setProperty(Configuration.OBJECT_WRAPPER_KEY, "defAult");
        cfg.setSettings(props);
        assertEquals(DefaultObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
        assertEquals(new Version(2, 3, 0), ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        assertEquals("utf-8", cfg.getDefaultEncoding());

        props = new Properties();
        props.setProperty(Configuration.OBJECT_WRAPPER_KEY, "Beans");
        cfg.setSettings(props);
        assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
        assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
        assertEquals(new Version(2, 3, 0), ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());

        props = new Properties();
        props.setProperty(Configuration.OBJECT_WRAPPER_KEY, "freemarker.ext.beans.BeansWrapper");
        cfg.setSettings(props);
        assertEquals(BeansWrapper.class, cfg.getObjectWrapper().getClass());
        assertFalse(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
        assertEquals(new Version(2, 3, 0), ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
        
        props = new Properties();
        props.setProperty(Configuration.OBJECT_WRAPPER_KEY, "DefaultObjectWrapper(2.3.19)");
        cfg.setSettings(props);
        assertEquals(DefaultObjectWrapper.class, cfg.getObjectWrapper().getClass());
        assertTrue(((WriteProtectable) cfg.getObjectWrapper()).isWriteProtected());
        assertEquals(new Version(2, 3, 0), ((BeansWrapper) cfg.getObjectWrapper()).getIncompatibleImprovements());
    }
    
    @Test
    public void parsingErrors() throws Exception {
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(1,,2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("\",\""));
        }

        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(x=1,2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("must precede named"));
        }


        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(x=1;2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("\";\""));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(1,2))",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("\")\""));
        }
    }

    @Test
    public void semanticErrors() throws Exception {
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$XTestBean1(1,2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("Failed to get class"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(true, 2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("constructor"));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(x = 1)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("no writeable JavaBeans property called \"x\""));
        }
        
        try {
            _ObjectBuilderSettingEvaluator.eval(
                    "freemarker.core.TestObjectBuilderSettings$TestBean1(p1 = 1, p1 = 2)",
                    Object.class, _SettingEvaluationEnvironment.getInstance());
            fail();
        } catch (_ObjectBuilderSettingEvaluationException e) {
            assertTrue(e.getMessage().contains("twice"));
        }
    }
    
}

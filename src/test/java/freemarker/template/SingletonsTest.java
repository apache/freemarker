package freemarker.template;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Assert;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.utility.Lockable;

@SuppressWarnings("boxing")
public class SingletonsTest extends TestCase {

    public SingletonsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        ConfigurationSingletons.removeAllSingletons();
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigurationSingletons.removeAllSingletons();
    }

    public void testCreateOrGet() throws Exception {
        assertSingletonSet();
        
        Object c1s1 = Configuration.getSingleton(C1.class, null, null, true);
        assertEquals("C1()", c1s1.toString());
        assertSingletonSet("hard C1()");
        
        Object c1s2 = Configuration.getSingleton(C1.class, new Object[] {}, new HashMap(), false);
        assertSame(c1s1, c1s2);
        assertSingletonSet("hard C1()");
        
        Object c2s1 = Configuration.getSingleton(C2.class, new Object[] { "t", 1 }, null, true);
        assertEquals("C2(t, 1)", c2s1.toString());
        assertSingletonSet("hard C1()", "hard C2(t, 1)");
        
        Object c2s2 = Configuration.getSingleton(C2.class, new Object[] { "t", 1 }, null, true);
        assertSame(c2s1, c2s2);
        assertSingletonSet("hard C1()", "hard C2(t, 1)");
        
        Configuration.getSingleton(C2.class, new Object[] { "t", 2 }, null, true);
        assertSingletonSet("hard C1()", "hard C2(t, 1)", "hard C2(t, 2)");
        
        Object c3s1 = Configuration.getSingleton(C3.class, null, newMap("s", "t", "x", 2), true);
        assertEquals("C3() {s = t, x = 2}", c3s1.toString());
        assertSingletonSet("hard C1()", "hard C2(t, 1)", "hard C2(t, 2)", "hard C3() {s = t, x = 2}");
        
        Object c3s2 = Configuration.getSingleton(C3.class, null, newMap("x", 2, "s", "t"), true);
        assertSame(c3s1, c3s2);
        assertSingletonSet("hard C1()", "hard C2(t, 1)", "hard C2(t, 2)", "hard C3() {s = t, x = 2}");
        
        Object c4s1 = Configuration.getSingleton(C4.class, new Object[] { "t", 1 }, newMap("x", 2, "s", "t"), true);
        assertSingletonSet("hard C1()", "hard C2(t, 1)", "hard C2(t, 2)", "hard C3() {s = t, x = 2}",
                "hard C4(t, 1) {s = t, x = 2}");
        
        Object c4s2 = Configuration.getSingleton(C4.class, new Object[] { "t", 1 }, newMap("x", 2, "s", "t"), true);
        assertSame(c4s1, c4s2);
        assertSingletonSet("hard C1()", "hard C2(t, 1)", "hard C2(t, 2)", "hard C3() {s = t, x = 2}",
                "hard C4(t, 1) {s = t, x = 2}");
    }
    
    public void testWidenNumberIfPossible() {
        assertEquals((byte) 1, ConfigurationSingletons.widenNumberIfPossible((byte) 1, byte.class));        
        assertEquals((short) 1, ConfigurationSingletons.widenNumberIfPossible((byte) 1, short.class));        
        assertEquals(1, ConfigurationSingletons.widenNumberIfPossible((short) 1, int.class));        
        assertEquals(1, ConfigurationSingletons.widenNumberIfPossible(1, short.class));        
        assertEquals(1L, ConfigurationSingletons.widenNumberIfPossible(1, long.class));        
        assertEquals(1f, ConfigurationSingletons.widenNumberIfPossible((byte) 1, float.class));        
        assertEquals(1.0, ConfigurationSingletons.widenNumberIfPossible(1f, double.class));        
        assertEquals(1.0, ConfigurationSingletons.widenNumberIfPossible(1, Double.class));        
    }
    
    public void testWidenNumbersToParameterTypes() throws Exception {
        Method m1 = SingletonsTest.class.getMethod("m1", new Class[] { String.class, Integer.class, double.class });
        Object[] correctArgs = new Object[] { "s", 1, 2.0 };
        assertSame(correctArgs, ConfigurationSingletons.widenNumbersToParameterTypes(m1, correctArgs));
        
        Assert.assertArrayEquals(
                correctArgs,
                ConfigurationSingletons.widenNumbersToParameterTypes(m1, new Object[] { "s", (short) 1, 2 }));
        
        Assert.assertArrayEquals(
                new Object[] { "s", null, 2.0 },
                ConfigurationSingletons.widenNumbersToParameterTypes(m1, new Object[] { "s", null, 2 }));
        
        Method m2 = SingletonsTest.class.getMethod("m2", new Class[] { long.class, double[].class });
        
        Assert.assertArrayEquals(
                new Object[] { 1L },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1L }));
        Assert.assertArrayEquals(
                new Object[] { 1L },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1 }));
        Assert.assertArrayEquals(
                new Object[] { 1L, 2.0 },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1, 2 }));
        Assert.assertArrayEquals(
                new Object[] { 1L, 2.0, 3.0 },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1, 2, 3 }));
        Assert.assertArrayEquals(
                new Object[] { 1L, 2.0, 3.0 },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1L, 2.0, 3.0 }));
        Assert.assertArrayEquals(
                new Object[] { 1L, 2.0, 3.0 },
                ConfigurationSingletons.widenNumbersToParameterTypes(m2, new Object[] { 1L, 2.0, 3 }));
    }
    
    @SuppressWarnings("unused")
    public void m1(String s, Integer i, double d) { }
    
    @SuppressWarnings("unused")
    public void m2(long l, double... ds) { }

    public void testCreationErrors() throws Exception {
        assertSingletonSet();
        
        try {
            Configuration.getSingleton(C2.class, new Object[] { "t" }, null, true);
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }

        try {
            Configuration.getSingleton(C2.class, new Object[] { 1, 2 }, null, true);
            fail();
        } catch (NoSuchMethodException e) {
            // Expected
        }
        
        try {
            Configuration.getSingleton(C3.class, null, newMap("y", 2, "s", "t"), true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("getPropertyDefaults"));
        }
        
        try {
            Configuration.getSingleton(C3.class, null, newMap("x", 2L, "s", "t"), true);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testArgumentNormalization() throws Exception {
        Object c2s1 = Configuration.getSingleton(CNotOverloaded.class, new Object[] { "t", 1 }, null, true);
        Object c2s2 = Configuration.getSingleton(CNotOverloaded.class, new Object[] { "t", (short) 1 }, null, true);
        assertSame(c2s1, c2s2);
        assertEquals("CNotOverloaded(t, 1)", c2s1.toString());
        
        c2s1 = Configuration.getSingleton(CNotOverloaded.class, new Object[] { null, 2 }, null, true);
        c2s2 = Configuration.getSingleton(CNotOverloaded.class, new Object[] { null, (short) 2 }, null, true);
        assertSame(c2s1, c2s2);
        assertEquals("CNotOverloaded(null, 2)", c2s1.toString());
        
        Object cos1 = Configuration.getSingleton(COverloaded.class, new Object[] { "t", 1 }, null, true);
        Object cos2 = Configuration.getSingleton(COverloaded.class, new Object[] { "t", (short) 1 }, null, true);
        assertSame(cos1, cos2);
        assertEquals("COverloaded(t, 1)", cos1.toString());
        
        cos1 = Configuration.getSingleton(COverloaded.class, new Object[] { null, 2 }, null, true);
        cos2 = Configuration.getSingleton(COverloaded.class, new Object[] { null, (short) 2 }, null, true);
        assertSame(cos1, cos2);
        assertEquals("COverloaded(null, 2)", cos1.toString());
        
        cos1 = Configuration.getSingleton(COverloaded.class, new Object[] { null, 1 }, null, true);
        cos2 = Configuration.getSingleton(COverloaded.class, new Object[] { "s", (short) 1 }, null, true);
        assertNotSame(cos1, cos2);
        assertEquals("COverloaded(null, 1)", cos1.toString());
        assertEquals("COverloaded(s, 1)", cos2.toString());
        
        Object co2s1 = Configuration.getSingleton(COverloaded2.class, new Object[] { "t", false }, null, true);
        Object co2s2 = Configuration.getSingleton(COverloaded2.class, new Object[] { "t" }, null, true);
        assertSame(co2s1, co2s2);
        assertEquals("COverloaded2(t, false)", co2s1.toString());

        try {
            Configuration.getSingleton(COverloaded2Wrong.class, new Object[] { "t", false }, null, true);
            fail();
        } catch (NoSuchMethodException e) {
            assertTrue(e.getMessage().contains("normalizeConstructorArguments"));
        }
        
        try {
            Configuration.getSingleton(COverloadedWrong.class, new Object[] { "t", false }, null, true);
            fail();
        } catch (NoSuchMethodException e) {
            assertTrue(e.getMessage().contains("normalizeConstructorArguments"));
        }
        
        try {
            Configuration.getSingleton(COverloaded3.class, new Object[] { "t" }, null, true);
        } catch (NoSuchMethodException e) {
            assertTrue(e.getMessage().contains("normalizeConstructorArguments"));
        }
        
        assertEquals(
                Configuration.getSingleton(COverloaded3.class, null, null, true).toString(),
                "COverloaded3(null)");
    }
    
    public void testPropertyNormalization() throws Exception {
        // Unspecified defaults problem:
        {
            Object s1 = Configuration.getSingleton(CPropNorm.class, null, null, true);
            Object s2 = Configuration.getSingleton(CPropNorm.class, null, newMap("s", "s", "x", 6), true);
            Object s3 = Configuration.getSingleton(CPropNorm.class, null, newMap("s", "s"), true);
            Object s4 = Configuration.getSingleton(CPropNorm.class, null, newMap("x", 6), true);
            assertSame(s1, s2);
            assertSame(s2, s3);
            assertSame(s3, s4);
            assertEquals("CPropNorm() {s = s, x = 6}", s1.toString());
        }
        {
            Object s1 = Configuration.getSingleton(CPropNorm.class, null, newMap("x", 7), true);
            Object s2 = Configuration.getSingleton(CPropNorm.class, null, newMap("s", "s", "x", 7), true);
            assertSame(s1, s2);
            assertEquals("CPropNorm() {s = s, x = 7}", s1.toString());
        }
        
        // Insignificant number type differences:
        {
            Object s1 = Configuration.getSingleton(CPropNorm.class, null, newMap("s", "t", "x", 2), true);
            Object s2 = Configuration.getSingleton(CPropNorm.class, null, newMap("s", "t", "x", (short) 2), true);
            assertSame(s1, s2);
            assertEquals("CPropNorm() {s = t, x = 2}", s1.toString());
        }
        
        Configuration.getSingleton(CPropNormWrong.class, null, newMap("x", 8, "s", "t"), true);  // works
        Configuration.getSingleton(CPropNormWrong.class, null, newMap("x", 8), true);  // works
        try {
            Configuration.getSingleton(CPropNormWrong.class, null, newMap("s", "t"), true);  // x default will be wrong
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("getPropertyDefaults") && e.getMessage().contains("-6"));
        }
        
        try {
            Configuration.getSingleton(CPropNormWrong2.class, null, null, true);  // s default will be wrong
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("getPropertyDefaults") && e.getMessage().contains("\"s\""));
        }

        try {
            Configuration.getSingleton(CPropNormWrong3.class, null, null, true);  // s default will be wrong
            fail();
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("getPropertyDefaults") && e.getMessage().contains("\"foo\""));
        }
        
        Configuration.getSingleton(CPropNormUnsettable.class, null, null, true);  // works
        try {
            Configuration.getSingleton(CPropNormUnsettable.class, null, newMap("s", "t"), true);
            fail();
        } catch (NoSuchMethodException e) {
            assertTrue(e.getMessage().contains("getPropertyDefaults"));
        }
    }
    
    private static Map newMap(Object... args) {
        Map res = new LinkedHashMap();
        int ln = args.length;
        if (ln % 2 != 0) throw new IllegalArgumentException("args must have an even length"); 
        for (int i = 0; i < ln; i += 2) {
            res.put(args[i], args[i + 1]);
        }
        return Collections.unmodifiableMap(res);
    }

    public void testReferenceTypes() throws Exception {
        Object c1s1 = Configuration.getSingleton(C1.class, null, null, false);  // soft
        assertSingletonSet("soft C1()");
        
        Object c1s2 = Configuration.getSingleton(C1.class, null, null, false);  // still soft
        assertSame(c1s1, c1s2);
        assertSingletonSet("soft C1()");
        
        Object c1s3 = Configuration.getSingleton(C1.class, null, null, true);  // soft -> hard
        assertSame(c1s1, c1s3);
        assertSingletonSet("hard C1()");
        
        Object c1s4 = Configuration.getSingleton(C1.class, null, null, false);  // hard -> soft never happens
        assertSame(c1s1, c1s4);
        assertSingletonSet("hard C1()");
        
        Configuration.weakenSingletonReference(C1.class, null, null);  // * -> weak
        assertSingletonSet("weak C1()");
        
        Object c1s5 = Configuration.getSingleton(C1.class, null, null, false);  // weak -> soft never happens
        assertSame(c1s1, c1s5);
        assertSingletonSet("weak C1()");

        Object c1s6 = Configuration.getSingleton(C1.class, null, null, true);  // weak -> hard never happens
        assertSame(c1s1, c1s6);
        assertSingletonSet("weak C1()");
        
        assertTrue(c1s1 instanceof C1);  // so that the reference will be hold
        
        ConfigurationSingletons.removeAllSingletons();
        Configuration.getSingleton(C1.class, null, null, true);  // hard
        assertSingletonSet("hard C1()");
        Configuration.weakenSingletonReference(C1.class, null, null);  // * -> weak
        assertSingletonSet("weak C1()");
    }

    public void testVersionNormalization() throws Exception {
        Configuration.getSingleton(CVersionNormalization.class, new Object[] { new Version(1, 2, 0) }, null, true);
        assertSingletonSet("hard CVersionNormalization(1.0.0)");
        Configuration.getSingleton(CVersionNormalization.class, new Object[] { new Version(1, 1, 0) }, null, true);
        assertSingletonSet("hard CVersionNormalization(1.0.0)");
        Configuration.getSingleton(CVersionNormalization.class, new Object[] { new Version(0, 9, 0) }, null, true);
        assertSingletonSet("hard CVersionNormalization(1.0.0)", "hard CVersionNormalization(0.9.0)");
        Configuration.getSingleton(CVersionNormalization.class, new Object[] { new Version(1, 0, 0) }, null, true);
        assertSingletonSet("hard CVersionNormalization(1.0.0)", "hard CVersionNormalization(0.9.0)");
        Configuration.getSingleton(CVersionNormalization.class, new Object[] { new Version(0, 9, 0) }, null, true);
        assertSingletonSet("hard CVersionNormalization(1.0.0)", "hard CVersionNormalization(0.9.0)");
        
        try {
            Configuration.getSingleton(CVersionNormalization.class, new Object[] { null }, null, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Version"));
        }
        
        try {
            Configuration.getSingleton(
                    CVersionNormalizationWrong.class, new Object[] { new Version(1, 2, 0) }, null, true);
            fail();
        } catch (NoSuchMethodException e) {
            assertTrue(e.getMessage().contains("normalizeIncompatibleImprovementsVersion"));
        }
    }
    
    public void testBeansWrapperSingletons() throws Exception {
        String bw1Str = "soft freemarker.ext.beans.BeansWrapper(2.3.0) { "
                + "simpleMapWrapper = true, exposureLevel = 1, exposeFields = false, strict = false }";
        
        final BeansWrapper bw1 = Configuration.getSingletonBeansWrapper(new Version(2, 3, 19), true);
        assertSingletonSet(bw1Str);
        assertTrue(bw1.wrap(newMap("x", 1)) instanceof SimpleMapModel);
        assertSame(bw1.getClass(), BeansWrapper.class);
        
        final BeansWrapper bw2 = Configuration.getSingletonBeansWrapper(new Version(2, 3, 20), true);
        assertSingletonSet(bw1Str);
        assertSame(bw1, bw2);
        
        final String bwBf1Str = "soft freemarker.ext.beans.BeansWrapper(2.3.21) { "
                + "simpleMapWrapper = true, exposureLevel = 1, exposeFields = false, strict = false }";
        final ObjectWrapper bwBf1 = Configuration.getSingletonBeansWrapper(new Version(2, 3, 21), true);
        assertSingletonSet(bw1Str, bwBf1Str);
        assertNotSame(bwBf1, bw2);
        
        final ObjectWrapper bwBf2 = Configuration.getSingletonBeansWrapper(new Version(2, 3, 22), true);
        assertSingletonSet(bw1Str, bwBf1Str);
        assertNotSame(bwBf1, bw2);
        assertSame(bwBf1, bwBf2);
        
        String bwDefStr = "soft freemarker.ext.beans.BeansWrapper(2.3.0) { "
                + "simpleMapWrapper = false, exposureLevel = 1, exposeFields = false, strict = false }";
        final ObjectWrapper bwDef1 = Configuration.getSingletonBeansWrapper(new Version(2, 3, 10), false);
        assertTrue(bwDef1.wrap(newMap("x", 1)) instanceof MapModel);
        assertSingletonSet(bw1Str, bwBf1Str, bwDefStr);
        final ObjectWrapper bwDef2 = (BeansWrapper) Configuration.getSingleton(BeansWrapper.class, null, null, false);
        assertSingletonSet(bw1Str, bwBf1Str, bwDefStr);
        
        assertSame(bwDef1, bwDef2);
        assertSame(bwDef1, Configuration.getSingletonBeansWrapper(new Version(2, 3, 12)));
        assertSame(bwDef1.getClass(), BeansWrapper.class);
        
        assertSame(bw1, Configuration.getSingletonBeansWrapper(new Version(2, 3, 12), true));
        assertSame(bwBf1, Configuration.getSingletonBeansWrapper(new Version(2, 3, 21), true));
    }

    public void testDefaultObjectWrapperSingletons() throws Exception {
        DefaultObjectWrapper dw1 = Configuration.getSingletonDefaultObjectWrapper(new Version(2, 3, 20));
        assertSame(dw1.getClass(), DefaultObjectWrapper.class);
        DefaultObjectWrapper dw2 = (DefaultObjectWrapper) Configuration.getSingleton(DefaultObjectWrapper.class, null, null, false);
        assertSame(dw1, dw2);
        
        DefaultObjectWrapper dwBf1 = Configuration.getSingletonDefaultObjectWrapper(new Version(2, 3, 22));
        assertEquals(dwBf1.getIncompatibleImprovements(), new Version(2, 3, 21));
        DefaultObjectWrapper dwBf2 = Configuration.getSingletonDefaultObjectWrapper(new Version(2, 3, 21));
        assertSame(dwBf1, dwBf2);
    }
    
    private void assertSingletonSet(String... expected){
        HashSet expectedSet = new HashSet();
        for (String item : expected) {
            expectedSet.add(item);
        }
        assertEquals(expectedSet, ConfigurationSingletons.getSingletonDescriptions());
    }
    
    public static abstract class C0 implements Lockable {
        
        boolean readOnly = false;

        public void makeReadOnly() {
            readOnly = true;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public String toString() {
            String name = this.getClass().getName();
            return name.substring(name.lastIndexOf('$') + 1) + (readOnly ? "" : " RW! ") + getAttributes();
        }
        
        protected abstract String getAttributes();
        
    }
    
    public static class C1 extends C0 {
        
        public C1() { }

        @Override
        protected String getAttributes() {
            return "()";
        }
        
    }

    public static class C2 extends C0 {
        
        final String s;
        final int x;
        
        public C2(String s, int x) {
            this.s = s;
            this.x = x;
        }

        @Override
        protected String getAttributes() {
            return "(" + s + ", " + x + ")";
        }
        
    }
    
    public static class C3 extends C0 {
        
        String s;
        int x;
        
        public C3() {
        }

        @Override
        protected String getAttributes() {
            return "() {s = " + s + ", x = " + x + "}";
        }

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
        
        public static Map getPropertyDefaults(@SuppressWarnings("unused") Object[] args) {
            return newMap("s", null, "x", 0);
        }
        
    }

    public static class C4 extends C3 {
        
        final String as;
        final int ax;
        
        String s;
        int x;
        
        public C4(String as, int ax) {
            this.as = as;
            this.ax = ax;
        }

        @Override
        protected String getAttributes() {
            return "(" + as + ", " + ax + ") {s = " + s + ", x = " + x + "}";
        }

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
        
    }

    public static class CNotOverloaded extends C0 {
        
        final String s;
        final int x;
        
        public CNotOverloaded(String s, int x) {
            this.s = s;
            this.x = x;
        }

        @Override
        protected String getAttributes() {
            return "(" + s + ", " + x + ")";
        }
        
    }
    
    public static class COverloaded extends C0 {
        
        final String s;
        final int x;
        
        public COverloaded(String s, int x) {
            this.s = s;
            this.x = x;
        }

        public COverloaded(String s, short x) {
            this.s = s;
            this.x = x;
        }
        
        @Override
        protected String getAttributes() {
            return "(" + s + ", " + x + ")";
        }
        
        public static Object[] normalizeConstructorArguments(Object[] args) {
            return new Object[] { args[0], Integer.valueOf(((Number) args[1]).intValue()) };
        }
        
    }

    public static class COverloaded2 extends C0 {
        
        private final String s;
        private final boolean x;
        
        public COverloaded2(String s, boolean x) {
            this.s = s;
            this.x = x;
        }

        public COverloaded2(String s) {
            this(s, false);
        }
        
        @Override
        protected String getAttributes() {
            return "(" + s + ", " + x + ")";
        }
        
        public static Object[] normalizeConstructorArguments(Object[] args) {
            return new Object[] { args[0], args.length > 1 ? args[1] : false };
        }
        
    }
    
    public static class COverloaded2Wrong extends COverloaded2 {
        
        public COverloaded2Wrong(String s, boolean x) {
            super(s, x);
        }

        public COverloaded2Wrong(String s) {
            super(s);
        }
    }    

    public static class COverloaded3 extends C0 {
        private final String s;
        
        public COverloaded3(String s) {
            this.s = s;
        }

        public COverloaded3() {
            this(null);
        }
        
        @Override
        protected String getAttributes() {
            return "(" + s + ")";
        }
    }
    
    public static class COverloadedWrong extends C0 {
        
        private final String s;
        private final boolean x;
        
        public COverloadedWrong(String s, boolean x) {
            this.s = s;
            this.x = x;
        }

        public COverloadedWrong(String s) {
            this(s, false);
        }
        
        @Override
        protected String getAttributes() {
            return "(" + s + ", " + x + ")";
        }
        
    }
    
    public static class CPropNorm extends C0 {
        
        protected String s = "s";
        protected int x = 6;
        
        @Override
        protected String getAttributes() {
            return "() {s = " + s + ", x = " + x + "}";
        }

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
        
        public static Map getPropertyDefaults(@SuppressWarnings("unused") Object[] args) {
            return newMap("s", "s", "x", 6);
        }

    }
    
    public static class CPropNormWrong extends CPropNorm {
        
        public CPropNormWrong() {
            x = -6;  // to go out of sync with getPropertyDefaults
        }
        
    }

    public static class CPropNormWrong2 extends CPropNorm {
        
        public CPropNormWrong2() {
            s = null;  // to go out of sync with getPropertyDefaults
        }
        
    }

    public static class CPropNormWrong3 extends CPropNorm {
        
        public CPropNormWrong3() {
            s = "foo";  // to go out of sync with getPropertyDefaults
        }

        public static Map getPropertyDefaults(Object[] args) {
            return newMap("s", null, "x", 6);
        }
        
    }

    public static class CPropNormUnsettable extends C0 {
        
        protected String s = "s";
        protected int x = 6;
        
        @Override
        protected String getAttributes() {
            return "() {s = " + s + ", x = " + x + "}";
        }

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

    }

    public static class CVersionNormalizationWrong extends C0 {
        
        private final Version ici;

        public CVersionNormalizationWrong(Version ici) {
            this.ici = ici;
        }

        @Override
        protected String getAttributes() {
            return "(" + ici + ")";
        }
        
    }

    public static class CVersionNormalization extends CVersionNormalizationWrong {
        
        public CVersionNormalization(Version ici) {
            super(ici);
        }

        public static Version normalizeIncompatibleImprovementsVersion(Version version) {
            return version.intValue() > 1000000 ? new Version(1, 0, 0) : version;
        }
        
    }
    
}

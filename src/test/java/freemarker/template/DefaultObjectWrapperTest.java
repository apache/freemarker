package freemarker.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.HashAdapter;
import freemarker.ext.util.WrapperTemplateModel;

public class DefaultObjectWrapperTest {

    private final static DefaultObjectWrapper OW22 = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22).build();
    
    private final static DefaultObjectWrapper OW22NM = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
    static {
        OW22NM.setNullModel(NullModel.INSTANCE);
    }
    
    @Test
    public void testIncompatibleImprovementsVersionBreakPoints() throws Exception {
        List<Version> expected = new ArrayList<Version>();
        for (int u = 0; u < 21; u++) {
            expected.add(Configuration.VERSION_2_3_0);
        }
        expected.add(Configuration.VERSION_2_3_21);
        expected.add(Configuration.VERSION_2_3_22);

        List<Version> actual = new ArrayList<Version>();
        for (int i = _TemplateAPI.VERSION_INT_2_3_0; i <= Configuration.getVersion().intValue(); i++) {
            int major = i / 1000000;
            int minor = i % 1000000 / 1000;
            int micro = i % 1000;
            final Version version = new Version(major, minor, micro);
            final Version normalizedVersion = DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(version);
            actual.add(normalizedVersion);

            final DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(version);
            assertEquals(normalizedVersion, builder.getIncompatibleImprovements());
            assertEquals(normalizedVersion, builder.build().getIncompatibleImprovements());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testIncompatibleImprovementsVersionOutOfBounds() throws Exception {
        try {
            DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(new Version(2, 2, 0));
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        Version curVersion = Configuration.getVersion();
        final Version futureVersion = new Version(curVersion.getMajor(), curVersion.getMicro(),
                curVersion.getMicro() + 1);
        try {
            DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(futureVersion);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new DefaultObjectWrapperBuilder(futureVersion);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testDefaultObjectWrapperBuilder() throws Exception {
        {
            DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            DefaultObjectWrapper bw = factory.build();
            assertSame(bw, factory.build());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.isClassIntrospectionCacheRestricted());
        }

        for (boolean simpleMapWrapper : new boolean[] { true, false }) {
            {
                DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
                factory.setSimpleMapWrapper(simpleMapWrapper); // Shouldn't mater
                DefaultObjectWrapper bw = factory.build();
                assertSame(bw, factory.build());
                assertSame(bw.getClass(), DefaultObjectWrapper.class);
                assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
                assertTrue(bw.isWriteProtected());
                assertEquals(simpleMapWrapper, bw.isSimpleMapWrapper());
                assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
                assertTrue(bw.wrap(new ArrayList()) instanceof SimpleSequence);
                assertTrue(bw.wrap(new String[] {}) instanceof SimpleSequence);
            }

            {
                DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22);
                factory.setSimpleMapWrapper(simpleMapWrapper); // Shouldn't mater
                BeansWrapper bw = factory.build();
                assertSame(bw, factory.build());
                assertSame(bw.getClass(), DefaultObjectWrapper.class);
                assertEquals(Configuration.VERSION_2_3_22, bw.getIncompatibleImprovements());
                assertTrue(bw.isWriteProtected());
                assertEquals(simpleMapWrapper, bw.isSimpleMapWrapper());
                assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapAdapter);
                assertTrue(bw.wrap(new ArrayList()) instanceof SimpleListAdapter);
                assertTrue(bw.wrap(new String[] {}) instanceof SimpleArrayAdapter);
            }
        }

        {
            DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.getVersion());
            factory.setSimpleMapWrapper(true);
            BeansWrapper bw = factory.build();
            assertSame(bw, factory.build());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(
                    DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(Configuration.getVersion()),
                    bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleMapAdapter);
        }

        {
            DefaultObjectWrapper bw = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19).build();
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);

            assertSame(bw, new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_20).build());
            assertSame(bw, new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_0).build());
            assertSame(bw, new DefaultObjectWrapperBuilder(new Version(2, 3, 5)).build());
        }

        {
            DefaultObjectWrapper bw = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21).build();
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.isClassIntrospectionCacheRestricted());

            assertSame(bw, new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21).build());
        }

        {
            DefaultObjectWrapper bw = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19).build();
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
        }

        {
            DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            DefaultObjectWrapper bw = factory.build();
            DefaultObjectWrapper bw2 = factory.build();
            assertSame(bw, bw2); // not cached

            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
        }

        {
            DefaultObjectWrapperBuilder factory = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            factory.setExposeFields(true);
            BeansWrapper bw = factory.build();
            BeansWrapper bw2 = factory.build();
            assertSame(bw, bw2); // not cached

            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(true, bw.isExposeFields());
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testRoundtripping() throws TemplateModelException, ClassNotFoundException {
        DefaultObjectWrapper dow21 = new DefaultObjectWrapper(Configuration.VERSION_2_3_21);
        DefaultObjectWrapper dow22 = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);

        final Map hashMap = new HashMap();
        inintTestMap(hashMap);
        final Map treeMap = new TreeMap();
        inintTestMap(treeMap);
        final Map linkedHashMap = new LinkedHashMap();
        inintTestMap(linkedHashMap);
        final Map gMap = ImmutableMap.<String, Object> of("a", 1, "b", 2, "c", 3);
        final LinkedList linkedList = new LinkedList();
        linkedList.add("a");
        linkedList.add("b");
        linkedList.add("c");
        final int[] intArray = new int[] { 1, 2, 3 };
        final String[] stringArray = new String[] { "a", "b", "c" };

        assertRoundtrip(dow21, linkedHashMap, SimpleHash.class, HashAdapter.class, linkedHashMap.toString());
        assertRoundtrip(dow21, treeMap, SimpleHash.class, HashAdapter.class, treeMap.toString());
        assertRoundtrip(dow21, gMap, SimpleHash.class, HashAdapter.class, hashMap.toString());
        assertRoundtrip(dow21, linkedList, SimpleSequence.class, Class.forName("freemarker.ext.beans.SequenceAdapter"),
                linkedList.toString());
        assertRoundtrip(dow21, intArray, SimpleSequence.class, Class.forName("freemarker.ext.beans.SequenceAdapter"),
                "[1, 2, 3]");
        assertRoundtrip(dow21, stringArray, SimpleSequence.class,
                Class.forName("freemarker.ext.beans.SequenceAdapter"),
                "[a, b, c]");

        assertRoundtrip(dow22, linkedHashMap, SimpleMapAdapter.class, LinkedHashMap.class, linkedHashMap.toString());
        assertRoundtrip(dow22, treeMap, SimpleMapAdapter.class, TreeMap.class, treeMap.toString());
        assertRoundtrip(dow22, gMap, SimpleMapAdapter.class, ImmutableMap.class, gMap.toString());
        assertRoundtrip(dow22, linkedList, SimpleListAdapter.class, LinkedList.class, linkedList.toString());
        assertRoundtrip(dow22, intArray, SimpleArrayAdapter.class, int[].class, null);
        assertRoundtrip(dow22, stringArray, SimpleArrayAdapter.class, String[].class, null);
    }

    @SuppressWarnings("boxing")
    private void inintTestMap(Map map) {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testMapAdapter() throws TemplateModelException {
        HashMap<String, Object> testMap = new LinkedHashMap<String, Object>();
        testMap.put("a", 1);
        testMap.put("b", null);
        testMap.put("c", "C");
        testMap.put("d", Collections.singletonList("x"));

        {
            TemplateHashModelEx hash = (TemplateHashModelEx) OW22.wrap(testMap);
            assertEquals(4, hash.size());
            assertFalse(hash.isEmpty());
            assertNull(hash.get("e"));
            assertEquals(1, ((TemplateNumberModel) hash.get("a")).getAsNumber());
            assertNull(hash.get("b"));
            assertEquals("C", ((TemplateScalarModel) hash.get("c")).getAsString());
            assertTrue(hash.get("d") instanceof SimpleListAdapter);

            assertCollectionTMEquals(hash.keys(), "a", "b", "c", "d");
            assertCollectionTMEquals(hash.values(), 1, null, "C", Collections.singletonList("x"));
        }
        
        {
            assertTrue(((TemplateHashModel) OW22.wrap(Collections.emptyMap())).isEmpty());
        }

        {
            final TemplateHashModelEx hash = (TemplateHashModelEx) OW22NM.wrap(testMap);
            assertSame(NullModel.INSTANCE, hash.get("b"));
            assertNull(hash.get("e"));
            
            assertCollectionTMEquals(hash.keys(), "a", "b", "c", "d");
            assertCollectionTMEquals(hash.values(), 1, null, "C", Collections.singletonList("x"));
        }
    }

    private void assertCollectionTMEquals(TemplateCollectionModel keys, Object... expectedItems)
            throws TemplateModelException {
        for (int i = 0; i < 2; i++) {  // Run twice to check if we always get a new iterator
            int idx = 0;
            for (TemplateModelIterator it = keys.iterator(); it.hasNext();) {
                TemplateModel actualItem = it.next();
                if (idx >= expectedItems.length) {
                    fail("Number of items is more than the expected " + expectedItems.length);
                }
                assertEquals(expectedItems[idx], OW22.unwrap(actualItem));
                idx++;
            }
            if (expectedItems.length != idx) {
                fail("Number of items is " + idx + ", which is less than the expected " + expectedItems.length);
            }
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testListAdapter() throws TemplateModelException {
        List testList = new ArrayList<Object>();
        testList.add(1);
        testList.add(null);
        testList.add("c");
        testList.add(new String[] { "x" });

        {
            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testList);
            assertEquals(4, seq.size());
            assertNull(seq.get(-1));
            assertEquals(1, ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertNull(seq.get(1));
            assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
            assertTrue(seq.get(3) instanceof SimpleArrayAdapter);
            assertNull(seq.get(4));
        }

        {
            final TemplateSequenceModel seq = (TemplateSequenceModel) OW22NM.wrap(testList);
            assertSame(NullModel.INSTANCE, seq.get(1));
            assertNull(seq.get(4));
        }
    }

    @Test
    public void testArrayAdapterTypes() throws TemplateModelException {
        assertArrayAdapterClass("Object", OW22.wrap(new Object[] {}));
        assertArrayAdapterClass("Object", OW22.wrap(new String[] {}));
        assertArrayAdapterClass("byte", OW22.wrap(new byte[] {}));
        assertArrayAdapterClass("short", OW22.wrap(new short[] {}));
        assertArrayAdapterClass("int", OW22.wrap(new int[] {}));
        assertArrayAdapterClass("long", OW22.wrap(new long[] {}));
        assertArrayAdapterClass("float", OW22.wrap(new float[] {}));
        assertArrayAdapterClass("double", OW22.wrap(new double[] {}));
        assertArrayAdapterClass("boolean", OW22.wrap(new boolean[] {}));
        assertArrayAdapterClass("char", OW22.wrap(new char[] {}));
    }

    private void assertArrayAdapterClass(String adapterCompType, TemplateModel adaptedArray) {
        assertTrue(adaptedArray instanceof SimpleArrayAdapter);
        assertTrue(adaptedArray.getClass().getName()
                .contains("$" + adapterCompType.substring(0, 1).toUpperCase() + adapterCompType.substring(1)));
    }

    @SuppressWarnings("boxing")
    @Test
    public void testArrayAdapters() throws TemplateModelException {
        {
            final String[] testArray = new String[] { "a", null, "c" };

            {
                TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testArray);
                assertEquals(3, seq.size());
                assertNull(seq.get(-1));
                assertEquals("a", ((TemplateScalarModel) seq.get(0)).getAsString());
                assertNull(seq.get(1));
                assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
                assertNull(seq.get(3));
            }

            {
                TemplateSequenceModel seq = (TemplateSequenceModel) OW22NM.wrap(testArray);
                assertNull(seq.get(-1));
                assertEquals("a", ((TemplateScalarModel) seq.get(0)).getAsString());
                assertSame(NullModel.INSTANCE, seq.get(1));
                assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
                assertNull(seq.get(3));
            }
        }

        {
            final int[] testArray = new int[] { 11, 22 };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Integer.valueOf(11), ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertEqualsAndSameClass(Integer.valueOf(22), ((TemplateNumberModel) seq.get(1)).getAsNumber());
            assertNull(seq.get(2));
        }

        {
            final double[] testArray = new double[] { 11, 22 };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Double.valueOf(11), ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertEqualsAndSameClass(Double.valueOf(22), ((TemplateNumberModel) seq.get(1)).getAsNumber());
            assertNull(seq.get(2));
        }

        {
            final boolean[] testArray = new boolean[] { true, false };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Boolean.valueOf(true), ((TemplateBooleanModel) seq.get(0)).getAsBoolean());
            assertEqualsAndSameClass(Boolean.valueOf(false), ((TemplateBooleanModel) seq.get(1)).getAsBoolean());
            assertNull(seq.get(2));
        }

        {
            final char[] testArray = new char[] { 'a', 'b' };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEquals("a", ((TemplateScalarModel) seq.get(0)).getAsString());
            assertEquals("b", ((TemplateScalarModel) seq.get(1)).getAsString());
            assertNull(seq.get(2));
        }
    }

    private void assertEqualsAndSameClass(Object expected, Object actual) {
        assertEquals(expected, actual);
        if (expected != null) {
            assertEquals(expected.getClass(), actual.getClass());
        }
    }

    private void assertRoundtrip(DefaultObjectWrapper dow, Object obj, Class expectedTMClass,
            Class expectedPojoClass,
            String expectedPojoToString)
            throws TemplateModelException {
        final TemplateModel objTM = dow.wrap(obj);
        assertTrue(expectedTMClass.isAssignableFrom(objTM.getClass()));

        final TemplateHashModel testBeanTM = (TemplateHashModel) dow.wrap(new RoundtripTesterBean());

        {
            TemplateMethodModelEx getClassM = (TemplateMethodModelEx) testBeanTM.get("getClass");
            Object r = getClassM.exec(Collections.singletonList(objTM));
            final Class rClass = (Class) ((WrapperTemplateModel) r).getWrappedObject();
            assertTrue(expectedPojoClass.isAssignableFrom(rClass));
        }

        if (expectedPojoToString != null) {
            TemplateMethodModelEx getToStringM = (TemplateMethodModelEx) testBeanTM.get("toString");
            Object r = getToStringM.exec(Collections.singletonList(objTM));
            assertEquals(expectedPojoToString, ((TemplateScalarModel) r).getAsString());
        }
    }

    public static class RoundtripTesterBean {

        public Class getClass(Object o) {
            return o.getClass();
        }

        public String toString(Object o) {
            return o.toString();
        }

    }
    
    private static final class NullModel implements TemplateModel, AdapterTemplateModel {
        
        final static NullModel INSTANCE = new NullModel();

        public Object getAdaptedObject(Class hint) {
            return null;
        }
        
    }

}

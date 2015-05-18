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

import static freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.HashAdapter;
import freemarker.ext.util.WrapperTemplateModel;

public class DefaultObjectWrapperTest {

    private final static DefaultObjectWrapper OW0 = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_0)
            .build();

    private final static DefaultObjectWrapper OW22 = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22)
            .build();

    private final static DefaultObjectWrapper OW22NM = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
    static {
        OW22NM.setNullModel(NullModel.INSTANCE);
    }

    private final static DefaultObjectWrapper OW22_FUTURE = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
    static {
        OW22_FUTURE.setForceLegacyNonListCollections(false);
    }

    @Test
    public void testIncompatibleImprovementsVersionBreakPoints() throws Exception {
        List<Version> expected = new ArrayList<Version>();
        for (int u = 0; u < 21; u++) {
            expected.add(Configuration.VERSION_2_3_0);
        }
        expected.add(Configuration.VERSION_2_3_21);
        expected.add(Configuration.VERSION_2_3_22);
        expected.add(Configuration.VERSION_2_3_22); // no non-BC change here yet

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
    public void testBuilder() throws Exception {
        {
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            DefaultObjectWrapper bw = builder.build();
            assertSame(bw, builder.build());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.isClassIntrospectionCacheRestricted());

            assertFalse(bw.getUseAdaptersForContainers());
            assertTrue(bw.getForceLegacyNonListCollections());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.wrap(new ArrayList()) instanceof SimpleSequence);
            assertTrue(bw.wrap(new String[] {}) instanceof SimpleSequence);
            assertTrue(bw.wrap(new HashSet()) instanceof SimpleSequence);

        }

        for (boolean simpleMapWrapper : new boolean[] { true, false }) {
            {
                DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
                builder.setSimpleMapWrapper(simpleMapWrapper); // Shouldn't mater
                DefaultObjectWrapper bw = builder.build();
                assertSame(bw, builder.build());
                assertSame(bw.getClass(), DefaultObjectWrapper.class);
                assertEquals(Configuration.VERSION_2_3_21, bw.getIncompatibleImprovements());
                assertTrue(bw.isWriteProtected());
                assertEquals(simpleMapWrapper, bw.isSimpleMapWrapper());
                assertFalse(bw.getUseAdaptersForContainers());
                assertTrue(bw.getForceLegacyNonListCollections());
                assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
                assertTrue(bw.wrap(new ArrayList()) instanceof SimpleSequence);
                assertTrue(bw.wrap(new String[] {}) instanceof SimpleSequence);
                assertTrue(bw.wrap(new HashSet()) instanceof SimpleSequence);
                assertTrue(bw.wrap('c') instanceof TemplateScalarModel); // StringModel now, but should change later
            }

            {
                DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22);
                builder.setSimpleMapWrapper(simpleMapWrapper); // Shouldn't mater
                DefaultObjectWrapper bw = builder.build();
                assertSame(bw, builder.build());
                assertSame(bw.getClass(), DefaultObjectWrapper.class);
                assertEquals(Configuration.VERSION_2_3_22, bw.getIncompatibleImprovements());
                assertTrue(bw.isWriteProtected());
                assertEquals(simpleMapWrapper, bw.isSimpleMapWrapper());
                assertTrue(bw.getUseAdaptersForContainers());
                assertTrue(bw.getForceLegacyNonListCollections());
                assertTrue(bw.wrap(new HashMap()) instanceof DefaultMapAdapter);
                assertTrue(bw.wrap(new ArrayList()) instanceof DefaultListAdapter);
                assertTrue(bw.wrap(new String[] {}) instanceof DefaultArrayAdapter);
                assertTrue(bw.wrap(new HashSet()) instanceof SimpleSequence);
            }
        }

        {
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.getVersion());
            builder.setSimpleMapWrapper(true);
            BeansWrapper bw = builder.build();
            assertSame(bw, builder.build());
            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(
                    DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(Configuration.getVersion()),
                    bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertTrue(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof DefaultMapAdapter);
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
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            builder.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY);
            DefaultObjectWrapper bw = builder.build();
            DefaultObjectWrapper bw2 = builder.build();
            assertSame(bw, bw2); // not cached

            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(BeansWrapper.EXPOSE_PROPERTIES_ONLY, bw.getExposureLevel());
        }

        {
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_19);
            builder.setExposeFields(true);
            BeansWrapper bw = builder.build();
            BeansWrapper bw2 = builder.build();
            assertSame(bw, bw2); // not cached

            assertSame(bw.getClass(), DefaultObjectWrapper.class);
            assertEquals(Configuration.VERSION_2_3_0, bw.getIncompatibleImprovements());
            assertTrue(bw.isWriteProtected());
            assertFalse(bw.isSimpleMapWrapper());
            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertEquals(true, bw.isExposeFields());
        }

        {
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
            builder.setForceLegacyNonListCollections(false);
            DefaultObjectWrapper bw = builder.build();
            assertSame(bw, builder.build());
            assertFalse(bw.getForceLegacyNonListCollections());

            assertTrue(bw.wrap(new HashMap()) instanceof SimpleHash);
            assertTrue(bw.wrap(new ArrayList()) instanceof SimpleSequence);
            assertTrue(bw.wrap(new String[] {}) instanceof SimpleSequence);
            assertTrue(bw.wrap(new HashSet()) instanceof SimpleSequence);
        }

        {
            DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22);
            builder.setForceLegacyNonListCollections(false);
            DefaultObjectWrapper bw = builder.build();
            assertSame(bw, builder.build());
            assertFalse(bw.getForceLegacyNonListCollections());

            assertTrue(bw.wrap(new HashMap()) instanceof DefaultMapAdapter);
            assertTrue(bw.wrap(new ArrayList()) instanceof DefaultListAdapter);
            assertTrue(bw.wrap(new String[] {}) instanceof DefaultArrayAdapter);
            assertTrue(bw.wrap(new HashSet()) instanceof DefaultNonListCollectionAdapter);
        }
    }
    
    @Test
    public void testConstructors() throws Exception {
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper();
            assertEquals(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS, ow.getIncompatibleImprovements());
            assertFalse(ow.isWriteProtected());
            assertFalse(ow.getUseAdaptersForContainers());
            assertTrue(ow.getForceLegacyNonListCollections());
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_20);
            assertEquals(Configuration.VERSION_2_3_0, ow.getIncompatibleImprovements());
            assertFalse(ow.isWriteProtected());
            assertFalse(ow.getUseAdaptersForContainers());
            assertTrue(ow.getForceLegacyNonListCollections());
        }

        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_21);
            assertEquals(Configuration.VERSION_2_3_21, ow.getIncompatibleImprovements());
            assertFalse(ow.isWriteProtected());
            assertFalse(ow.getUseAdaptersForContainers());
            assertTrue(ow.getForceLegacyNonListCollections());
        }
        
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
            assertEquals(Configuration.VERSION_2_3_22, ow.getIncompatibleImprovements());
            assertFalse(ow.isWriteProtected());
            assertTrue(ow.getUseAdaptersForContainers());
            assertTrue(ow.getForceLegacyNonListCollections());
        }
        
        try {
            new DefaultObjectWrapper(new Version(99, 9, 9));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("version"));
        }
    }
    
    
    @Test
    public void testCustomization() throws TemplateModelException {
        {
            CustomizedDefaultObjectWrapper ow = new CustomizedDefaultObjectWrapper(Configuration.VERSION_2_3_22);
            assertEquals(Configuration.VERSION_2_3_22, ow.getIncompatibleImprovements());
            assertTrue(ow.getUseAdaptersForContainers());
            testCustomizationCommonPart(ow,
                    DefaultMapAdapter.class, DefaultListAdapter.class, DefaultArrayAdapter.class);
        }
        
        {
            CustomizedDefaultObjectWrapper ow = new CustomizedDefaultObjectWrapper(Configuration.VERSION_2_3_21);
            assertFalse(ow.getUseAdaptersForContainers());
            assertEquals(Configuration.VERSION_2_3_21, ow.getIncompatibleImprovements());
            testCustomizationCommonPart(ow,
                    SimpleHash.class, SimpleSequence.class, SimpleSequence.class);
        }
        
        {
            CustomizedDefaultObjectWrapper ow = new CustomizedDefaultObjectWrapper(Configuration.VERSION_2_3_20);
            assertFalse(ow.getUseAdaptersForContainers());
            assertEquals(Configuration.VERSION_2_3_0, ow.getIncompatibleImprovements());
            testCustomizationCommonPart(ow,
                    SimpleHash.class, SimpleSequence.class, SimpleSequence.class);
        }
    }

    @SuppressWarnings("boxing")
    private void testCustomizationCommonPart(CustomizedDefaultObjectWrapper ow,
            Class<? extends TemplateHashModel> mapTMClass,
            Class<? extends TemplateSequenceModel> listTMClass,
            Class<? extends TemplateSequenceModel> arrayTMClass)
            throws TemplateModelException {
        assertFalse(ow.isWriteProtected());
        
        TemplateSequenceModel seq = (TemplateSequenceModel) ow.wrap(new Tupple(11, 22));
        assertEquals(2, seq.size());
        assertEquals(11, ow.unwrap(seq.get(0)));
        assertEquals(22, ow.unwrap(seq.get(1)));
        
        assertTrue(ow.wrap("x") instanceof SimpleScalar);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        assertTrue(mapTMClass.isInstance(ow.wrap(Collections.emptyMap())));
        assertTrue(listTMClass.isInstance(ow.wrap(Collections.emptyList())));
        assertTrue(arrayTMClass.isInstance(ow.wrap(new boolean[] { })));
        assertTrue(ow.wrap(new HashSet()) instanceof SimpleSequence);  // at least until IcI 2.4
        assertTrue(ow.wrap('c') instanceof TemplateScalarModel); // StringModel right now, but should change later
        
        TemplateHashModel bean = (TemplateHashModel) ow.wrap(new TestBean());
        assertEquals(1, ow.unwrap(bean.get("x")));
        {
            // Check method calls, and also if the return value is wrapped with the overidden "wrap".
            final TemplateModel mr = (TemplateModel) ((TemplateMethodModelEx) bean.get("m")).exec(Collections.emptyList());
            assertEquals(
                    Collections.singletonList(1),
                    ow.unwrap(mr));
            assertTrue(listTMClass.isInstance(mr));
        }
        {
            // Check custom TM usage and round trip:
            final TemplateModel mr = (TemplateModel) ((TemplateMethodModelEx) bean.get("incTupple"))
                    .exec(Collections.singletonList(ow.wrap(new Tupple<Integer, Integer>(1, 2))));
            assertEquals(new Tupple<Integer, Integer>(2, 3), ow.unwrap(mr));
            assertTrue(TuppleAdapter.class.isInstance(mr));
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

        assertRoundtrip(dow22, linkedHashMap, DefaultMapAdapter.class, LinkedHashMap.class, linkedHashMap.toString());
        assertRoundtrip(dow22, treeMap, DefaultMapAdapter.class, TreeMap.class, treeMap.toString());
        assertRoundtrip(dow22, gMap, DefaultMapAdapter.class, ImmutableMap.class, gMap.toString());
        assertRoundtrip(dow22, linkedList, DefaultListAdapter.class, LinkedList.class, linkedList.toString());
        assertRoundtrip(dow22, intArray, DefaultArrayAdapter.class, int[].class, null);
        assertRoundtrip(dow22, stringArray, DefaultArrayAdapter.class, String[].class, null);
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
            assertTrue(hash.get("d") instanceof DefaultListAdapter);

            assertCollectionTMEquals(hash.keys(), "a", "b", "c", "d");
            assertCollectionTMEquals(hash.values(), 1, null, "C", Collections.singletonList("x"));
            
            assertSizeThroughAPIModel(4, hash);
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

    private void assertCollectionTMEquals(TemplateCollectionModel coll, Object... expectedItems)
            throws TemplateModelException {
        for (int i = 0; i < 2; i++) { // Run twice to check if we always get a new iterator
            int idx = 0;
            TemplateModelIterator it2 = null;
            for (TemplateModelIterator it = coll.iterator(); it.hasNext();) {
                TemplateModel actualItem = it.next();
                if (idx >= expectedItems.length) {
                    fail("Number of items is more than the expected " + expectedItems.length);
                }
                assertEquals(expectedItems[idx], OW22.unwrap(actualItem));
                if (i == 1) {
                    // In the 2nd round we also test with two iterators in parallel.
                    // This 2nd iterator is also special in that its hasNext() is never called.
                    if (it2 == null) {
                        it2 = coll.iterator();
                    }
                    assertEquals(expectedItems[idx], OW22.unwrap(it2.next()));
                }
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
        {
            List testList = new ArrayList<Object>();
            testList.add(1);
            testList.add(null);
            testList.add("c");
            testList.add(new String[] { "x" });

            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testList);
            assertTrue(seq instanceof DefaultListAdapter);
            assertFalse(seq instanceof TemplateCollectionModel); // Maybe changes at 2.4.0
            assertEquals(4, seq.size());
            assertNull(seq.get(-1));
            assertEquals(1, ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertNull(seq.get(1));
            assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
            assertTrue(seq.get(3) instanceof DefaultArrayAdapter);
            assertNull(seq.get(4));
            
            assertSizeThroughAPIModel(4, seq);
        }

        {
            List testList = new LinkedList<Object>();
            testList.add(1);
            testList.add(null);
            testList.add("c");

            TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(testList);
            assertTrue(seq instanceof DefaultListAdapter);
            assertTrue(seq instanceof TemplateCollectionModel); // Maybe changes at 2.4.0
            assertEquals(3, seq.size());
            assertNull(seq.get(-1));
            assertEquals(1, ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertNull(seq.get(1));
            assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
            assertNull(seq.get(3));

            assertCollectionTMEquals((TemplateCollectionModel) seq, 1, null, "c");

            TemplateModelIterator it = ((TemplateCollectionModel) seq).iterator();
            it.next();
            it.next();
            it.next();
            try {
                it.next();
                fail();
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsString("no more"));
            }
        }

        {
            List testList = new ArrayList<Object>();
            testList.add(null);

            final TemplateSequenceModel seq = (TemplateSequenceModel) OW22NM.wrap(testList);
            assertSame(NullModel.INSTANCE, seq.get(0));
            assertNull(seq.get(1));
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
        assertTrue(adaptedArray instanceof DefaultArrayAdapter);
        assertThat(adaptedArray.getClass().getName(),
                containsString("$" + adapterCompType.substring(0, 1).toUpperCase() + adapterCompType.substring(1)));
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
            assertTrue(rClass + " instanceof " + expectedPojoClass, expectedPojoClass.isAssignableFrom(rClass));
        }

        if (expectedPojoToString != null) {
            TemplateMethodModelEx getToStringM = (TemplateMethodModelEx) testBeanTM.get("toString");
            Object r = getToStringM.exec(Collections.singletonList(objTM));
            assertEquals(expectedPojoToString, ((TemplateScalarModel) r).getAsString());
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCollectionAdapterBasics() throws TemplateModelException {
        {
            Set set = new TreeSet();
            set.add("a");
            set.add("b");
            set.add("c");
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW22_FUTURE.wrap(set);
            assertTrue(coll instanceof DefaultNonListCollectionAdapter);
            assertEquals(3, coll.size());
            assertFalse(coll.isEmpty());
            assertCollectionTMEquals(coll, "a", "b", "c");

            assertTrue(coll.contains(OW22_FUTURE.wrap("a")));
            assertTrue(coll.contains(OW22_FUTURE.wrap("b")));
            assertTrue(coll.contains(OW22_FUTURE.wrap("c")));
            assertTrue(coll.contains(OW22_FUTURE.wrap("c")));
            assertFalse(coll.contains(OW22_FUTURE.wrap("d")));
            try {
                assertFalse(coll.contains(OW22_FUTURE.wrap(1)));
                fail();
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsString("Integer"));
            }

            assertRoundtrip(OW22_FUTURE, set, DefaultNonListCollectionAdapter.class, TreeSet.class, "[a, b, c]");
            
            assertSizeThroughAPIModel(3, coll);
        }

        {
            Set set = new HashSet();
            final List<String> list = Collections.singletonList("b");
            set.add(list);
            set.add(null);
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW22_FUTURE.wrap(set);
            TemplateModelIterator it = coll.iterator();
            final TemplateModel tm1 = it.next();
            Object obj1 = OW22_FUTURE.unwrap(tm1);
            final TemplateModel tm2 = it.next();
            Object obj2 = OW22_FUTURE.unwrap(tm2);
            assertTrue(obj1 == null || obj2 == null);
            assertTrue(obj1 != null && obj1.equals(list) || obj2 != null && obj2.equals(list));
            assertTrue(tm1 instanceof DefaultListAdapter || tm2 instanceof DefaultListAdapter);

            List similarList = new ArrayList();
            similarList.add("b");
            assertTrue(coll.contains(OW22_FUTURE.wrap(similarList)));
            assertTrue(coll.contains(OW22_FUTURE.wrap(null)));
            assertFalse(coll.contains(OW22_FUTURE.wrap("a")));
            assertFalse(coll.contains(OW22_FUTURE.wrap(1)));

            assertRoundtrip(OW22_FUTURE, set, DefaultNonListCollectionAdapter.class, HashSet.class, "[" + obj1 + ", "
                    + obj2 + "]");
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCollectionAdapterOutOfBounds() throws TemplateModelException {
        Set set = Collections.singleton(123);

        TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW22_FUTURE.wrap(set);
        TemplateModelIterator it = coll.iterator();

        for (int i = 0; i < 3; i++) {
            assertTrue(it.hasNext());
        }

        assertEquals(123, OW22_FUTURE.unwrap(it.next()));

        for (int i = 0; i < 3; i++) {
            assertFalse(it.hasNext());
            try {
                it.next();
                fail();
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsStringIgnoringCase("no more"));
            }
        }
    }

    @Test
    public void testCollectionAdapterAndNulls() throws TemplateModelException {
        Set set = new HashSet();
        set.add(null);

        {
            DefaultObjectWrapper dow = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
            dow.setForceLegacyNonListCollections(false);
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) dow.wrap(set);
            assertEquals(1, coll.size());
            assertFalse(coll.isEmpty());
            assertNull(coll.iterator().next());
        }

        {
            DefaultObjectWrapper dow = new DefaultObjectWrapper(Configuration.VERSION_2_3_22);
            dow.setForceLegacyNonListCollections(false);
            dow.setNullModel(NullModel.INSTANCE);
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) dow.wrap(set);
            assertEquals(1, coll.size());
            assertFalse(coll.isEmpty());
            assertEquals(NullModel.INSTANCE, coll.iterator().next());
        }
    }

    @Test
    public void testLegacyNonListCollectionWrapping() throws TemplateModelException {
        Set set = new TreeSet();
        set.add("a");
        set.add("b");
        set.add("c");
        TemplateSequenceModel seq = (TemplateSequenceModel) OW22.wrap(set);
        assertTrue(seq instanceof SimpleSequence);
        assertEquals(3, seq.size());
        assertEquals("a", OW22.unwrap(seq.get(0)));
        assertEquals("b", OW22.unwrap(seq.get(1)));
        assertEquals("c", OW22.unwrap(seq.get(2)));
    }

    @Test
    public void testIteratorWrapping() throws TemplateModelException, ClassNotFoundException {
        testIteratorWrapping(OW0, SimpleCollection.class, Class.forName("freemarker.ext.beans.SetAdapter"));
        testIteratorWrapping(OW22, DefaultIteratorAdapter.class, Iterator.class);
    }

    private void testIteratorWrapping(DefaultObjectWrapper ow, Class<?> expectedTMClass, Class<?> expectedPOJOClass)
            throws TemplateModelException {
        final List<String> list = ImmutableList.<String> of("a", "b", "c");
        Iterator<String> it = list.iterator();
        TemplateCollectionModel coll = (TemplateCollectionModel) ow.wrap(it);

        assertRoundtrip(ow, coll, expectedTMClass, expectedPOJOClass, null);

        TemplateModelIterator itIt = coll.iterator();
        TemplateModelIterator itIt2 = coll.iterator(); // used later
        assertTrue(itIt.hasNext());
        assertEquals("a", ow.unwrap(itIt.next()));
        assertTrue(itIt.hasNext());
        assertEquals("b", ow.unwrap(itIt.next()));
        assertTrue(itIt.hasNext());
        assertEquals("c", ow.unwrap(itIt.next()));
        assertFalse(itIt.hasNext());
        try {
            itIt.next();
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("no more"));
        }

        try {
            itIt2.hasNext();
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsString("can be listed only once"));
        }

        TemplateModelIterator itIt3 = coll.iterator();
        try {
            itIt3.hasNext();
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsString("can be listed only once"));
        }
    }
    
    @SuppressWarnings("boxing")
    @Test
    public void testCharKeyFallback() throws TemplateModelException {
        Map hashMapS = new HashMap<String, Integer>();
        hashMapS.put("a", 1);
        Map sortedMapS = new TreeMap<String, Integer>();
        sortedMapS.put("a", 1);
        Map hashMapC = new HashMap<Character, Integer>();
        hashMapC.put('a', 1);
        Map sortedMapC = new TreeMap<Character, Integer>();
        sortedMapC.put('a', 1);
        
        for (DefaultObjectWrapper ow : new DefaultObjectWrapper[] { OW0, OW22 } ) {
            assertEquals(1, ow.unwrap(((TemplateHashModel) ow.wrap(hashMapS)).get("a")));
            assertEquals(1, ow.unwrap(((TemplateHashModel) ow.wrap(hashMapC)).get("a")));
            assertEquals(1, ow.unwrap(((TemplateHashModel) ow.wrap(sortedMapS)).get("a")));
            try {
                ((TemplateHashModel) ow.wrap(sortedMapC)).get("a");
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsStringIgnoringCase("String key"));
            }
            
            assertNull(((TemplateHashModel) ow.wrap(hashMapS)).get("b"));
            assertNull(((TemplateHashModel) ow.wrap(hashMapC)).get("b"));
            assertNull(((TemplateHashModel) ow.wrap(sortedMapS)).get("b"));
            try {
                ((TemplateHashModel) ow.wrap(sortedMapC)).get("b");
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsStringIgnoringCase("String key"));
            }
        }
    }
    
    @Test
    public void assertCanWrapDOM() throws SAXException, IOException, ParserConfigurationException,
            TemplateModelException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader("<doc><sub a='1' /></doc>"));
        Document doc = db.parse(is);        
        assertTrue(OW22.wrap(doc) instanceof TemplateNodeModel);
    }
    
    private void assertSizeThroughAPIModel(int expectedSize, TemplateModel normalModel) throws TemplateModelException {
        if (!(normalModel instanceof TemplateModelWithAPISupport)) {
            fail(); 
        }
        TemplateHashModel apiModel = (TemplateHashModel) ((TemplateModelWithAPISupport) normalModel).getAPI();
        TemplateMethodModelEx sizeMethod = (TemplateMethodModelEx) apiModel.get("size");
        TemplateNumberModel r = (TemplateNumberModel) sizeMethod.exec(Collections.emptyList());
        assertEquals(expectedSize, r.getAsNumber().intValue());
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
    
    private static class Tupple<E1, E2> {
        
        private final E1 e1;
        private final E2 e2;

        public Tupple(E1 e1, E2 e2) {
            if (e1 == null || e2 == null) throw new NullPointerException();
            this.e1 = e1;
            this.e2 = e2;
        }

        public E1 getE1() {
            return e1;
        }

        public E2 getE2() {
            return e2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((e1 == null) ? 0 : e1.hashCode());
            result = prime * result + ((e2 == null) ? 0 : e2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Tupple other = (Tupple) obj;
            if (e1 == null) {
                if (other.e1 != null) return false;
            } else if (!e1.equals(other.e1)) return false;
            if (e2 == null) {
                if (other.e2 != null) return false;
            } else if (!e2.equals(other.e2)) return false;
            return true;
        }
        
    }
    
    @SuppressWarnings("boxing")
    public static class TestBean {
        
        public int getX() {
            return 1;
        }
        
        public List<Integer> m() {
            return Collections.singletonList(1);
        }

        public Tupple incTupple(Tupple<Integer, Integer> tupple) {
            return new Tupple(tupple.e1 + 1, tupple.e2 + 1);
        }
        
    }
    
    private static class CustomizedDefaultObjectWrapper extends DefaultObjectWrapper {

        private CustomizedDefaultObjectWrapper(Version incompatibleImprovements) {
            super(incompatibleImprovements);
        }
        
        @Override
        protected TemplateModel handleUnknownType(final Object obj) throws TemplateModelException {
            if (obj instanceof Tupple) {
                return new TuppleAdapter((Tupple<?, ?>) obj, this);
            }
            
            return super.handleUnknownType(obj);
        }
        
    }
    
    private static class TuppleAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
            AdapterTemplateModel {
        
        private final Tupple<?, ?> tupple;
        
        public TuppleAdapter(Tupple<?, ?> tupple, ObjectWrapper ow) {
            super(ow);
            this.tupple = tupple;
        }

        public int size() throws TemplateModelException {
            return 2;
        }
        
        public TemplateModel get(int index) throws TemplateModelException {
            switch (index) {
            case 0: return wrap(tupple.getE1());
            case 1: return wrap(tupple.getE2());
            default: return null;
            }
        }

        public Object getAdaptedObject(Class hint) {
            return tupple;
        }
        
    };
    
}

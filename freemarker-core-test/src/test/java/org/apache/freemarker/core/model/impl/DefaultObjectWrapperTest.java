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

package org.apache.freemarker.core.model.impl;

import static org.apache.freemarker.test.hamcerst.Matchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
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
import java.util.Vector;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core._CallableUtils;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateCollectionModelEx;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DefaultObjectWrapperTest {

    private final static DefaultObjectWrapper OW = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
            .build();

    // This will make sense if we will have multipe incompatibleImprovement versions.
    @Test
    public void testIncompatibleImprovementsVersionBreakPoints() throws Exception {
        List<Version> expected = new ArrayList<>();
        expected.add(Configuration.VERSION_3_0_0);

        List<Version> actual = new ArrayList<>();
        int i = _CoreAPI.VERSION_INT_3_0_0;
        while (i <= Configuration.getVersion().intValue()) {
            int major = i / 1000000;
            int minor = i % 1000000 / 1000;
            int micro = i % 1000;
            final Version version = new Version(major, minor, micro);
            
            final Version normalizedVersion = DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(version);
            actual.add(normalizedVersion);

            final DefaultObjectWrapper.Builder builder = new DefaultObjectWrapper.Builder(version);
            assertEquals(normalizedVersion, builder.getIncompatibleImprovements());
            assertEquals(normalizedVersion, builder.build().getIncompatibleImprovements());
            
            i++;
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
            new DefaultObjectWrapper.Builder(futureVersion);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testBuilder() throws Exception {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        assertSame(ow, new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build());
        assertSame(ow.getClass(), DefaultObjectWrapper.class);
        assertEquals(Configuration.VERSION_3_0_0, ow.getIncompatibleImprovements());
    }

    @Test
    public void testWrappedTypes() throws Exception {
        DefaultObjectWrapper.Builder builder = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        DefaultObjectWrapper ow = builder.build();

        assertThat(ow.wrap(new HashMap()), instanceOf(DefaultMapAdapter.class));
        assertThat(ow.wrap(new ArrayList()), instanceOf(DefaultListAdapter.class));
        assertThat(ow.wrap(new String[] {}), instanceOf(DefaultArrayAdapter.class));
        assertThat(ow.wrap(new HashSet()), instanceOf(DefaultNonListCollectionAdapter.class));
        assertThat(ow.wrap(new PureIterable()), instanceOf(DefaultIterableAdapter.class));
        assertThat(ow.wrap(new Vector<>().iterator()), instanceOf(DefaultIteratorAdapter.class));
        assertThat(ow.wrap(new Vector<>().elements()), instanceOf(DefaultEnumerationAdapter.class));
    }
    
    @Test
    public void testConstructors() throws Exception {
        {
            DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                    .usePrivateCaches(true).build();
            assertEquals(Configuration.VERSION_3_0_0, ow.getIncompatibleImprovements());
        }
        
        try {
            new DefaultObjectWrapper.Builder(new Version(99, 9, 9)).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("version"));
        }
    }
    
    
    @Test
    public void testCustomization() throws TemplateException {
        CustomizedDefaultObjectWrapper ow = new CustomizedDefaultObjectWrapper(Configuration.VERSION_3_0_0);
        assertEquals(Configuration.VERSION_3_0_0, ow.getIncompatibleImprovements());

        TemplateSequenceModel seq = (TemplateSequenceModel) ow.wrap(new Tupple(11, 22));
        assertEquals(2, seq.size());
        assertEquals(11, ow.unwrap(seq.get(0)));
        assertEquals(22, ow.unwrap(seq.get(1)));
        
        assertTrue(ow.wrap("x") instanceof SimpleScalar);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        assertTrue(ow.wrap(Collections.emptyMap()) instanceof DefaultMapAdapter);
        assertTrue(ow.wrap(Collections.emptyList()) instanceof DefaultListAdapter);
        assertTrue(ow.wrap(new boolean[] { }) instanceof DefaultArrayAdapter);
        assertTrue(ow.wrap(new HashSet()) instanceof DefaultNonListCollectionAdapter);
        assertTrue(ow.wrap('c') instanceof TemplateScalarModel); // BeanAndStringModel right now, but should change later
        
        TemplateHashModel bean = (TemplateHashModel) ow.wrap(new TestBean());
        assertEquals(1, ow.unwrap(bean.get("x")));
        {
            // Check method calls, and also if the return value is wrapped with the overidden "wrap".
            final TemplateModel mr = ((JavaMethodModel) bean.get("m")).execute(
                    _CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE);
            assertEquals(Collections.singletonList(1), ow.unwrap(mr));
            assertTrue(DefaultListAdapter.class.isInstance(mr));
        }
        {
            // Check custom TM usage and round trip:
            final TemplateModel mr = ((JavaMethodModel) bean.get("incTupple"))
                    .execute(new TemplateModel[] { ow.wrap(new Tupple<>(1, 2)) },
                            NonTemplateCallPlace.INSTANCE);
            assertEquals(new Tupple<>(2, 3), ow.unwrap(mr));
            assertTrue(TuppleAdapter.class.isInstance(mr));
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCompositeValueWrapping() throws TemplateException, ClassNotFoundException {
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();

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
        final PureIterable pureIterable = new PureIterable();
        final HashSet hashSet = new HashSet();

        assertRoundtrip(ow, linkedHashMap, DefaultMapAdapter.class, LinkedHashMap.class, linkedHashMap.toString());
        assertRoundtrip(ow, treeMap, DefaultMapAdapter.class, TreeMap.class, treeMap.toString());
        assertRoundtrip(ow, gMap, DefaultMapAdapter.class, ImmutableMap.class, gMap.toString());
        assertRoundtrip(ow, linkedList, DefaultListAdapter.class, LinkedList.class, linkedList.toString());
        assertRoundtrip(ow, intArray, DefaultArrayAdapter.class, int[].class, null);
        assertRoundtrip(ow, stringArray, DefaultArrayAdapter.class, String[].class, null);
        assertRoundtrip(ow, pureIterable, DefaultIterableAdapter.class, PureIterable.class, pureIterable.toString());
        assertRoundtrip(ow, hashSet, DefaultNonListCollectionAdapter.class, HashSet.class, hashSet.toString());
    }

    @SuppressWarnings("boxing")
    private void inintTestMap(Map map) {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testMapAdapter() throws TemplateException {
        HashMap<String, Object> testMap = new LinkedHashMap<>();
        testMap.put("a", 1);
        testMap.put("b", null);
        testMap.put("c", "C");
        testMap.put("d", Collections.singletonList("x"));

        {
            TemplateHashModelEx hash = (TemplateHashModelEx) OW.wrap(testMap);
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
            assertTrue(((TemplateHashModel) OW.wrap(Collections.emptyMap())).isEmpty());
        }
    }

    private void assertCollectionTMEquals(TemplateCollectionModel coll, Object... expectedItems)
            throws TemplateModelException {
        for (int i = 0; i < 2; i++) { // Run twice to check if we always get a new iterator
            int idx = 0;
            TemplateModelIterator it2 = null;
            for (TemplateModelIterator it = coll.iterator(); it.hasNext(); ) {
                TemplateModel actualItem = it.next();
                if (idx >= expectedItems.length) {
                    fail("Number of items is more than the expected " + expectedItems.length);
                }
                assertEquals(expectedItems[idx], OW.unwrap(actualItem));
                if (i == 1) {
                    // In the 2nd round we also test with two iterators in parallel.
                    // This 2nd iterator is also special in that its hasNext() is never called.
                    if (it2 == null) {
                        it2 = coll.iterator();
                    }
                    assertEquals(expectedItems[idx], OW.unwrap(it2.next()));
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
    public void testListAdapter() throws TemplateException {
        {
            List testList = new ArrayList<>();
            testList.add(1);
            testList.add(null);
            testList.add("c");
            testList.add(new String[] { "x" });

            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testList);
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
            List<Object> testList = new LinkedList<>();
            testList.add(1);
            testList.add(null);
            testList.add("c");

            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testList);
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
    }

    @Test
    public void testArrayAdapterTypes() throws TemplateModelException {
        assertArrayAdapterClass("Object", OW.wrap(new Object[] {}));
        assertArrayAdapterClass("Object", OW.wrap(new String[] {}));
        assertArrayAdapterClass("byte", OW.wrap(new byte[] {}));
        assertArrayAdapterClass("short", OW.wrap(new short[] {}));
        assertArrayAdapterClass("int", OW.wrap(new int[] {}));
        assertArrayAdapterClass("long", OW.wrap(new long[] {}));
        assertArrayAdapterClass("float", OW.wrap(new float[] {}));
        assertArrayAdapterClass("double", OW.wrap(new double[] {}));
        assertArrayAdapterClass("boolean", OW.wrap(new boolean[] {}));
        assertArrayAdapterClass("char", OW.wrap(new char[] {}));
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

            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testArray);
            assertEquals(3, seq.size());
            assertNull(seq.get(-1));
            assertEquals("a", ((TemplateScalarModel) seq.get(0)).getAsString());
            assertNull(seq.get(1));
            assertEquals("c", ((TemplateScalarModel) seq.get(2)).getAsString());
            assertNull(seq.get(3));
        }

        {
            final int[] testArray = new int[] { 11, 22 };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Integer.valueOf(11), ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertEqualsAndSameClass(Integer.valueOf(22), ((TemplateNumberModel) seq.get(1)).getAsNumber());
            assertNull(seq.get(2));
        }

        {
            final double[] testArray = new double[] { 11, 22 };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Double.valueOf(11), ((TemplateNumberModel) seq.get(0)).getAsNumber());
            assertEqualsAndSameClass(Double.valueOf(22), ((TemplateNumberModel) seq.get(1)).getAsNumber());
            assertNull(seq.get(2));
        }

        {
            final boolean[] testArray = new boolean[] { true, false };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testArray);
            assertEquals(2, seq.size());
            assertNull(seq.get(-1));
            assertEqualsAndSameClass(Boolean.valueOf(true), ((TemplateBooleanModel) seq.get(0)).getAsBoolean());
            assertEqualsAndSameClass(Boolean.valueOf(false), ((TemplateBooleanModel) seq.get(1)).getAsBoolean());
            assertNull(seq.get(2));
        }

        {
            final char[] testArray = new char[] { 'a', 'b' };
            TemplateSequenceModel seq = (TemplateSequenceModel) OW.wrap(testArray);
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
            throws TemplateException {
        final TemplateModel objTM = dow.wrap(obj);
        assertThat(objTM.getClass(), typeCompatibleWith(expectedTMClass));

        final TemplateHashModel testBeanTM = (TemplateHashModel) dow.wrap(new RoundtripTesterBean());

        {
            JavaMethodModel getClassM = (JavaMethodModel) testBeanTM.get("getClass");
            TemplateModel r = getClassM.execute(new TemplateModel[] { objTM }, NonTemplateCallPlace.INSTANCE);
            final Class rClass = (Class) ((WrapperTemplateModel) r).getWrappedObject();
            assertThat(rClass, typeCompatibleWith(expectedPojoClass));
        }

        if (expectedPojoToString != null) {
            JavaMethodModel getToStringM = (JavaMethodModel) testBeanTM.get("toString");
            TemplateModel r = getToStringM.execute(new TemplateModel[] { objTM }, NonTemplateCallPlace.INSTANCE);
            assertEquals(expectedPojoToString, ((TemplateScalarModel) r).getAsString());
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCollectionAdapterBasics() throws TemplateException {
        {
            Set set = new TreeSet();
            set.add("a");
            set.add("b");
            set.add("c");
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW.wrap(set);
            assertTrue(coll instanceof DefaultNonListCollectionAdapter);
            assertEquals(3, coll.size());
            assertFalse(coll.isEmpty());
            assertCollectionTMEquals(coll, "a", "b", "c");

            assertRoundtrip(OW, set, DefaultNonListCollectionAdapter.class, TreeSet.class, "[a, b, c]");
            
            assertSizeThroughAPIModel(3, coll);
        }

        {
            Set set = new HashSet();
            final List<String> list = Collections.singletonList("b");
            set.add(list);
            set.add(null);
            TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW.wrap(set);
            TemplateModelIterator it = coll.iterator();
            final TemplateModel tm1 = it.next();
            Object obj1 = OW.unwrap(tm1);
            final TemplateModel tm2 = it.next();
            Object obj2 = OW.unwrap(tm2);
            assertTrue(obj1 == null || obj2 == null);
            assertTrue(obj1 != null && obj1.equals(list) || obj2 != null && obj2.equals(list));
            assertTrue(tm1 instanceof DefaultListAdapter || tm2 instanceof DefaultListAdapter);

            assertRoundtrip(OW, set, DefaultNonListCollectionAdapter.class, HashSet.class, "[" + obj1 + ", "
                    + obj2 + "]");
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCollectionAdapterOutOfBounds() throws TemplateModelException {
        Set set = Collections.singleton(123);

        TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW.wrap(set);
        TemplateModelIterator it = coll.iterator();

        for (int i = 0; i < 3; i++) {
            assertTrue(it.hasNext());
        }

        assertEquals(123, OW.unwrap(it.next()));

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

        TemplateCollectionModelEx coll = (TemplateCollectionModelEx) OW.wrap(set);
        assertEquals(1, coll.size());
        assertFalse(coll.isEmpty());
        assertNull(coll.iterator().next());
    }

    @Test
    public void testIteratorWrapping() throws TemplateException, ClassNotFoundException {
        final List<String> list = ImmutableList.of("a", "b", "c");
        Iterator<String> it = list.iterator();
        TemplateCollectionModel coll = (TemplateCollectionModel) OW.wrap(it);

        assertRoundtrip(OW, coll, DefaultIteratorAdapter.class, Iterator.class, null);

        TemplateModelIterator itIt = coll.iterator();
        TemplateModelIterator itIt2 = coll.iterator(); // used later
        assertTrue(itIt.hasNext());
        assertEquals("a", OW.unwrap(itIt.next()));
        assertTrue(itIt.hasNext());
        assertEquals("b", OW.unwrap(itIt.next()));
        assertTrue(itIt.hasNext());
        assertEquals("c", OW.unwrap(itIt.next()));
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

    @Test
    public void testIteratorApiSupport() throws TemplateException {
        TemplateModel wrappedIterator = OW.wrap(Collections.emptyIterator());
        assertThat(wrappedIterator, instanceOf(DefaultIteratorAdapter.class));
        DefaultIteratorAdapter iteratorAdapter = (DefaultIteratorAdapter) wrappedIterator;

        TemplateHashModel api = (TemplateHashModel) iteratorAdapter.getAPI();
        assertFalse(((TemplateBooleanModel) ((JavaMethodModel)
                api.get("hasNext")).execute(_CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE))
                .getAsBoolean());
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCharKeyFallback() throws TemplateModelException {
        Map hashMapS = new HashMap<>();
        hashMapS.put("a", 1);
        Map sortedMapS = new TreeMap<>();
        sortedMapS.put("a", 1);
        Map hashMapC = new HashMap<>();
        hashMapC.put('a', 1);
        Map sortedMapC = new TreeMap<>();
        sortedMapC.put('a', 1);
        
        assertEquals(1, OW.unwrap(((TemplateHashModel) OW.wrap(hashMapS)).get("a")));
        assertEquals(1, OW.unwrap(((TemplateHashModel) OW.wrap(hashMapC)).get("a")));
        assertEquals(1, OW.unwrap(((TemplateHashModel) OW.wrap(sortedMapS)).get("a")));
        try {
            ((TemplateHashModel) OW.wrap(sortedMapC)).get("a");
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("String key"));
        }
        
        assertNull(((TemplateHashModel) OW.wrap(hashMapS)).get("b"));
        assertNull(((TemplateHashModel) OW.wrap(hashMapC)).get("b"));
        assertNull(((TemplateHashModel) OW.wrap(sortedMapS)).get("b"));
        try {
            ((TemplateHashModel) OW.wrap(sortedMapC)).get("b");
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("String key"));
        }
    }
    
    @Test
    public void testIterableSupport() throws TemplateException, IOException {
        Iterable<String> iterable = new PureIterable();
        
        String listingFTL = "<#list value as x>${x}<#sep>, </#list>";
        
        DefaultObjectWrapper ow = OW;
        TemplateModel tm = ow.wrap(iterable);
        assertThat(tm, instanceOf(TemplateCollectionModel.class));
        TemplateCollectionModel iterableTM = (TemplateCollectionModel) tm;

        for (int i = 0; i < 2; i++) {
            TemplateModelIterator iteratorTM = iterableTM.iterator();
            assertTrue(iteratorTM.hasNext());
            assertEquals("a", ow.unwrap(iteratorTM.next()));
            assertTrue(iteratorTM.hasNext());
            assertEquals("b", ow.unwrap(iteratorTM.next()));
            assertTrue(iteratorTM.hasNext());
            assertEquals("c", ow.unwrap(iteratorTM.next()));
            assertFalse(iteratorTM.hasNext());
            try {
                iteratorTM.next();
                fail();
            } catch (TemplateModelException e) {
                assertThat(e.getMessage(), containsStringIgnoringCase("no more"));
            }
        }

        assertTemplateOutput(OW, iterable, listingFTL, "a, b, c");
    }

    @Test
    public void testEnumerationAdapter() throws TemplateException {
        Vector<String> vector = new Vector<String>();
        vector.add("a");
        vector.add("b");

        TemplateModel wrappedEnumeration = OW.wrap(vector.elements());
        assertThat(wrappedEnumeration, instanceOf(DefaultEnumerationAdapter.class));
        DefaultEnumerationAdapter enumAdapter = (DefaultEnumerationAdapter) wrappedEnumeration;
        TemplateModelIterator iterator = enumAdapter.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", ((TemplateScalarModel) iterator.next()).getAsString());
        assertTrue(iterator.hasNext());
        assertEquals("b", ((TemplateScalarModel) iterator.next()).getAsString());
        assertFalse(iterator.hasNext());

        iterator = enumAdapter.iterator();
        try {
            iterator.hasNext();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("only once"));
        }

        TemplateHashModel api = (TemplateHashModel) enumAdapter.getAPI();
        assertFalse(((TemplateBooleanModel) ((JavaMethodModel) api.get("hasMoreElements"))
                .execute(_CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE)).getAsBoolean());
    }

    @Test
    public void testExposureLevel() throws Exception {
        TestBean bean = new TestBean();

        {
            TemplateHashModel tm = wrapWithExposureLevel(bean, DefaultObjectWrapper.EXPOSE_SAFE);
            assertNotNull(tm.get("hashCode"));
            assertNotNull(tm.get("class"));
        }

        {
            TemplateHashModel tm = wrapWithExposureLevel(bean, DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY);
            assertNull(tm.get("hashCode"));
            assertNotNull(tm.get("class"));
        }

        {
            TemplateHashModel tm = wrapWithExposureLevel(bean, DefaultObjectWrapper.EXPOSE_NOTHING);
            assertNull(tm.get("hashCode"));
            assertNull(tm.get("class"));
        }

        {
            TemplateHashModel tm = wrapWithExposureLevel(bean, DefaultObjectWrapper.EXPOSE_ALL);
            assertNotNull(tm.get("hashCode"));
            assertNotNull(tm.get("class"));
        }
    }

    @Test
    public void testCanBeBuiltOnlyOnce() {
        DefaultObjectWrapper.Builder tcb = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        tcb.build();
        try {
            tcb.build();
            fail();
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    private TemplateHashModel wrapWithExposureLevel(Object bean, int exposureLevel) throws TemplateModelException {
        return (TemplateHashModel) new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                .exposureLevel(exposureLevel).build()
                .wrap(bean);
    }

    private void assertSizeThroughAPIModel(int expectedSize, TemplateModel normalModel) throws TemplateException {
        if (!(normalModel instanceof TemplateModelWithAPISupport)) {
            fail(); 
        }
        TemplateHashModel apiModel = (TemplateHashModel) ((TemplateModelWithAPISupport) normalModel).getAPI();
        JavaMethodModel sizeMethod = (JavaMethodModel) apiModel.get("size");
        TemplateNumberModel r = (TemplateNumberModel) sizeMethod.execute(
                _CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE);
        assertEquals(expectedSize, r.getAsNumber().intValue());
    }

    private void assertTemplateOutput(ObjectWrapper objectWrapper, Object value, String ftl, String expectedOutput)
            throws TemplateException, IOException {
        assertEquals(expectedOutput, processTemplate(objectWrapper, value, ftl));
    }

    private void assertTemplateFails(ObjectWrapper objectWrapper, Object value, String ftl, String expectedMessagePart)
            throws TemplateException, IOException {
        try {
            processTemplate(objectWrapper, value, ftl);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString(expectedMessagePart));
        }
    }
    
    private String processTemplate(ObjectWrapper objectWrapper, Object value, String ftl)
            throws TemplateException, IOException {
        Configuration cfg = new TestConfigurationBuilder()
                .objectWrapper(objectWrapper)
                .build();
        StringWriter out = new StringWriter();
        new Template(null, ftl, cfg).process(ImmutableMap.of("value", value), out);
        return out.toString();
    }

    private static final class PureIterable implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return ImmutableList.of("a", "b", "c").iterator();
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
            super(new DefaultObjectWrapper.Builder(incompatibleImprovements), true);
        }
        
        @Override
        protected TemplateModel wrapGenericObject(final Object obj) throws TemplateModelException {
            if (obj instanceof Tupple) {
                return new TuppleAdapter((Tupple<?, ?>) obj, this);
            }
            
            return super.wrapGenericObject(obj);
        }
        
    }
    
    private static class TuppleAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
            AdapterTemplateModel {
        
        private final Tupple<?, ?> tupple;
        
        public TuppleAdapter(Tupple<?, ?> tupple, ObjectWrapper ow) {
            super(ow);
            this.tupple = tupple;
        }

        @Override
        public int size() throws TemplateModelException {
            return 2;
        }
        
        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            switch (index) {
            case 0: return wrap(tupple.getE1());
            case 1: return wrap(tupple.getE2());
            default: return null;
            }
        }

        @Override
        public Object getAdaptedObject(Class hint) {
            return tupple;
        }
        
    }

}

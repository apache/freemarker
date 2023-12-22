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

package freemarker.template.utility;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.DefaultNonListCollectionAdapter;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

public class TemplateModelUtilTest {

    private final DefaultObjectWrapper ow = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_28).build();
    
    @Test
    public void testGetKeyValuePairIterator() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        TemplateHashModelEx thme = new TemplateHashModelExOnly(map);
        
        assertetGetKeyValuePairIteratorContent("", thme);
        
        map.put("k1", 11);
        assertetGetKeyValuePairIteratorContent("str(k1): num(11)", thme);
        
        map.put("k2", "v2");
        assertetGetKeyValuePairIteratorContent("str(k1): num(11), str(k2): str(v2)", thme);

        map.put("k2", null);
        assertetGetKeyValuePairIteratorContent("str(k1): num(11), str(k2): null", thme);
        
        map.put(3, 33);
        try {
            assertetGetKeyValuePairIteratorContent("fails anyway...", thme);
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("keys must be"), containsString("string"), containsString("number")));
        }
        map.remove(3);
        
        map.put(null, 44);
        try {
            assertetGetKeyValuePairIteratorContent("fails anyway...", thme);
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(),
                    allOf(containsString("keys must be"), containsString("string"), containsString("Null")));
        }
    }

    @Test
    public void testGetKeyValuePairIteratorWithEx2() throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        TemplateHashModelEx thme = DefaultMapAdapter.adapt(map, ow);
        
        assertetGetKeyValuePairIteratorContent("", thme);
        
        map.put("k1", 11);
        map.put("k2", "v2");
        map.put("k2", null);
        map.put(3, 33);
        map.put(null, 44);
        assertetGetKeyValuePairIteratorContent(
                "str(k1): num(11), str(k2): null, num(3): num(33), null: num(44)", thme);
    }
    
    private void assertetGetKeyValuePairIteratorContent(String expected, TemplateHashModelEx thme)
            throws TemplateModelException {
         StringBuilder sb = new StringBuilder();
         KeyValuePairIterator kvpi = TemplateModelUtils.getKeyValuePairIterator(thme);
         while (kvpi.hasNext()) {
             KeyValuePair kvp = kvpi.next();
             if (sb.length() != 0) {
                 sb.append(", ");
             }
             sb.append(toValueAssertionString(kvp.getKey())).append(": ")
                     .append(toValueAssertionString(kvp.getValue()));
         }
    }
    
    private String toValueAssertionString(TemplateModel model) throws TemplateModelException {
        if (model instanceof TemplateNumberModel) {
            return "num(" + ((TemplateNumberModel) model).getAsNumber() + ")";
        } else if (model instanceof TemplateScalarModel) {
            return "str(" + ((TemplateScalarModel) model).getAsString() + ")";
        } else if (model == null) {
            return "null";
        }
        
        throw new IllegalArgumentException("Type unsupported by test: " + model.getClass().getName());
    }

    @Test
    public void wrapAsHashUnionBasics() throws TemplateModelException {
        TemplateHashModelEx thEx1 = new TemplateHashModelExOnly(ImmutableMap.of("a", 1, "b", 2));
        TemplateHashModelEx thEx2 = new TemplateHashModelExOnly(ImmutableMap.of("c", 3, "d", 4));
        TemplateHashModelEx thEx3 = new TemplateHashModelExOnly(ImmutableMap.of("b", 22, "c", 33));
        TemplateHashModelEx thEx4 = new TemplateHashModelExOnly(Collections.emptyMap());
        TemplateHashModel th1 = new TemplateHashModelOnly(ImmutableMap.of("a", 1, "b", 2));
        TemplateHashModel th2 = new TemplateHashModelOnly(ImmutableMap.of("c", 3, "d", 4));
        TemplateHashModel th3 = new TemplateHashModelOnly(ImmutableMap.of("b", 22, "c", 33));
        TemplateHashModel th4 = new TemplateHashModelOnly(Collections.emptyMap());
        
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx1, thEx2));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th1, th2));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4), false,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx1, th2));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th1, thEx2));
        
        assertUnionResult(ImmutableMap.of("a", 1, "b", 22, "c", 33), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx1, thEx3));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 22, "c", 33), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th1, th3));
        assertUnionResult(ImmutableMap.of("b", 2, "c", 33, "a", 1), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx3, thEx1));
        assertUnionResult(ImmutableMap.of("b", 2, "c", 33, "a", 1), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th3, th1));
        
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx1, thEx4));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx4, thEx1));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th1, th4));
        assertUnionResult(ImmutableMap.of("a", 1, "b", 2), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th4, th1));
        assertUnionResult(Collections.<String, Integer>emptyMap(), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx4, thEx4));
        assertUnionResult(Collections.<String, Integer>emptyMap(), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th4, th4));
    }

    @Test
    public void wrapAsHashUnionWrapping() throws TemplateModelException {
        TemplateHashModel h = TemplateModelUtils.wrapAsHashUnion(ow,
                ImmutableMap.of("a", 1), new MyBean(), null, ow.wrap(ImmutableMap.of("c", 3)));
        assertThat(h, instanceOf(TemplateHashModelEx.class));
        assertEquals(((TemplateNumberModel) h.get("a")).getAsNumber(), 1);
        assertEquals(((TemplateNumberModel) h.get("b")).getAsNumber(), 2);
        assertEquals(((TemplateNumberModel) h.get("c")).getAsNumber(), 3);
        assertNotNull(h.get("class"));
        assertNull(h.get("noSuchVariable"));
        
        try {
            TemplateModelUtils.wrapAsHashUnion(ow, "x");
            fail();
        } catch (TemplateModelException e) {
            // Expected
        }
    }

    @Test
    public void wrapAsHashUnionSizeEdgeCases() throws TemplateModelException {
        assertSame(Constants.EMPTY_HASH, TemplateModelUtils.wrapAsHashUnion(ow));
        assertSame(Constants.EMPTY_HASH, TemplateModelUtils.wrapAsHashUnion(ow, null, null));
        
        TemplateModel hash = ow.wrap(ImmutableMap.of("a", 1));
        assertSame(hash, TemplateModelUtils.wrapAsHashUnion(ow, hash));
        assertSame(hash, TemplateModelUtils.wrapAsHashUnion(ow, null, hash, null));
    }
    
    private void assertUnionResult(
            Map<String, Integer> expected, boolean expectHashEx,
            TemplateHashModel actual) throws TemplateModelException {
        assertTrue(expectHashEx == actual instanceof TemplateHashModelEx);
        
        for (Entry<String, Integer> kvp : expected.entrySet()) {
            TemplateModel tmValue = actual.get(kvp.getKey());
            assertNotNull(tmValue);
            assertEquals(kvp.getValue(), ((TemplateNumberModel) tmValue).getAsNumber());
        }
        
        assertEquals(expected.isEmpty(), actual.isEmpty());
        
        if (actual instanceof TemplateHashModelEx) {
            TemplateHashModelEx actualEx = (TemplateHashModelEx) actual;
            
            assertEquals(expected.size(), actualEx.size());
            
            List<String> expectedKeys = new ArrayList<>(expected.keySet());
            List<String> actualKeys = new ArrayList<>();
            for (TemplateModelIterator it = ((TemplateHashModelEx) actual).keys().iterator(); it.hasNext(); ) {
                actualKeys.add(((TemplateScalarModel) it.next()).getAsString());
            }
            assertEquals(expectedKeys, actualKeys);
            
            List<Integer> expectedValues = new ArrayList<>(expected.values());
            List<Integer> actualValues = new ArrayList<>();
            for (TemplateModelIterator it = ((TemplateHashModelEx) actual).values().iterator(); it.hasNext(); ) {
                actualValues.add((Integer) ((TemplateNumberModel) it.next()).getAsNumber());
            }
            assertEquals(expectedValues, actualValues);
        }
    }

    /**
     * Deliberately doesn't implement {@link TemplateHashModelEx2}, only {@link TemplateHashModelEx}. 
     */
    private class TemplateHashModelExOnly implements TemplateHashModelEx {
        
        private final Map<?, ?> map;
        
        public TemplateHashModelExOnly(Map<?, ?> map) {
            this.map = map;
        }

        public TemplateModel get(String key) throws TemplateModelException {
            return ow.wrap(map.get(key));
        }

        public boolean isEmpty() throws TemplateModelException {
            return map.isEmpty();
        }

        public int size() throws TemplateModelException {
            return map.size();
        }

        public TemplateCollectionModel keys() throws TemplateModelException {
            return DefaultNonListCollectionAdapter.adapt(map.keySet(), ow);
        }

        public TemplateCollectionModel values() throws TemplateModelException {
            return DefaultNonListCollectionAdapter.adapt(map.values(), ow);
        } 
        
    }
    
    private class TemplateHashModelOnly implements TemplateHashModel {

        private final Map<?, ?> map;
        
        public TemplateHashModelOnly(Map<?, ?> map) {
            this.map = map;
        }

        public TemplateModel get(String key) throws TemplateModelException {
            return ow.wrap(map.get(key));
        }

        public boolean isEmpty() throws TemplateModelException {
            return map.isEmpty();
        }
        
    }
    
    public static class MyBean {
        public int getB() {
            return 2;
        }
    }
    
}

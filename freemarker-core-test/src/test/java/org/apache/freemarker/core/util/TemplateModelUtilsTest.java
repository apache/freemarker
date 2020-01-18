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

package org.apache.freemarker.core.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TemplateModelUtilsTest {

    private final DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
    
    @Test
    public void wrapAsHashUnionBasics() throws TemplateException {
        TemplateHashModelEx thEx1 = (TemplateHashModelEx) ow.wrap(ImmutableMap.of("a", 1, "b", 2));
        TemplateHashModelEx thEx2 = (TemplateHashModelEx) ow.wrap(ImmutableMap.of("c", 3, "d", 4));
        TemplateHashModelEx thEx3 = (TemplateHashModelEx) ow.wrap(ImmutableMap.of("b", 22, "c", 33));
        TemplateHashModelEx thEx4 = (TemplateHashModelEx) ow.wrap(Collections.emptyMap());
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
        assertUnionResult(Collections.emptyMap(), true,
                TemplateModelUtils.wrapAsHashUnion(ow, thEx4, thEx4));
        assertUnionResult(Collections.emptyMap(), false,
                TemplateModelUtils.wrapAsHashUnion(ow, th4, th4));
    }

    @Test
    public void wrapAsHashUnionWrapping() throws TemplateException {
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
        } catch (TemplateException e) {
            // Expected
        }
    }

    @Test
    public void wrapAsHashUnionSizeEdgeCases() throws TemplateException {
        assertSame(TemplateHashModel.EMPTY_HASH, TemplateModelUtils.wrapAsHashUnion(ow));
        assertSame(TemplateHashModel.EMPTY_HASH, TemplateModelUtils.wrapAsHashUnion(ow, null, null));
        
        TemplateModel hash = ow.wrap(ImmutableMap.of("a", 1));
        assertSame(hash, TemplateModelUtils.wrapAsHashUnion(ow, hash));
        assertSame(hash, TemplateModelUtils.wrapAsHashUnion(ow, null, hash, null));
    }
    
    private void assertUnionResult(
            Map<String, Integer> expected, boolean expectHashEx,
            TemplateHashModel actual) throws TemplateException {
        assertTrue(expectHashEx == actual instanceof TemplateHashModelEx);
        
        for (Entry<String, Integer> kvp : expected.entrySet()) {
            TemplateModel tmValue = actual.get(kvp.getKey());
            assertNotNull(tmValue);
            assertEquals(kvp.getValue(), ((TemplateNumberModel) tmValue).getAsNumber());
        }
        
        if (actual instanceof TemplateHashModelEx) {
            TemplateHashModelEx actualEx = (TemplateHashModelEx) actual;

            assertEquals(expected.isEmpty(), actualEx.isEmptyHash());
            
            assertEquals(expected.size(), actualEx.getHashSize());
            
            List<String> expectedKeys = new ArrayList<>(expected.keySet());
            List<String> actualKeys = new ArrayList<>();
            for (TemplateModelIterator it = ((TemplateHashModelEx) actual).keys().iterator(); it.hasNext(); ) {
                actualKeys.add(((TemplateStringModel) it.next()).getAsString());
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

    private class TemplateHashModelOnly implements TemplateHashModel {
        private final Map<?, ?> map;
        
        public TemplateHashModelOnly(Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public TemplateModel get(String key) throws TemplateException {
            return ow.wrap(map.get(key));
        }
        
    }
    
    public static class MyBean {
        public int getB() {
            return 2;
        }
    }
    
}

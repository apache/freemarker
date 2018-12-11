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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.junit.Test;

public class TemplateHashModelAdapterTest {

    @Test
    public void testNonEmpty() throws ObjectWrappingException {
        Map<Object, Object> map = new LinkedHashMap<Object, Object>();
        map.put("k1", "v1");
        map.put("k2", 2);
        map.put("k3", null);
        map.put(4, "v4");
        map.put(null, "v5");
        map.put("k6", true);

        DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateHashModel model = (TemplateHashModel) dow.wrap(map);

        TemplateHashModelAdapter<Object, Object> adapted = new TemplateHashModelAdapter<Object, Object>(model, dow);

        assertEquals("v1", adapted.get("k1"));
        assertEquals(2, adapted.get("k2"));
        assertNull(adapted.get("k3"));
        assertNull(adapted.get(4)); // Because it's not a string key
        assertNull(adapted.get(null)); // Because it's not a string key
        assertEquals(true, adapted.get("k6"));

        assertArrayEquals(new Object[] { "k1", "k2", "k3", 4, null, "k6" }, adapted.keySet().toArray());
        assertArrayEquals(new Object[] { "v1", 2, null, "v4", "v5", true }, adapted.values().toArray());
        assertArrayEquals(
                new Object[] {
                        Pair.of("k1", "v1"),
                        Pair.of("k2", 2),
                        Pair.of("k3", null),
                        Pair.of(4, "v4"),
                        Pair.of(null, "v5"),
                        Pair.of("k6", true)
                },
                adapted.entrySet().toArray());
        
        assertEquals(map.size(), adapted.size());
        assertEquals(map.isEmpty(), adapted.isEmpty());
    }

    @Test
    public void testEmpty() throws ObjectWrappingException {
        Map<Object, Object> map = Collections.emptyMap();

        DefaultObjectWrapper dow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        TemplateHashModel model = (TemplateHashModel) dow.wrap(map);

        TemplateHashModelAdapter<Object, Object> adapted = new TemplateHashModelAdapter<Object, Object>(model, dow);

        assertNull(adapted.get("k1"));

        assertThat(adapted.keySet(), empty());
        assertThat(adapted.values(), empty());
        assertThat(adapted.entrySet(), empty());
        
        assertEquals(map.size(), adapted.size());
        assertEquals(map.isEmpty(), adapted.isEmpty());
    }
    
}

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

import org.apache.freemarker.core.util.StringToIndexMap.Entry;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class StringToIndexMapTest {

    @Test
    public void testEmpty() {
        StringToIndexMap m = StringToIndexMap.EMPTY;
        assertEquals(0, m.size());
        assertEquals(-1, m.get("a"));
        assertTrue(m.getKeys().isEmpty());

        assertSame(m, StringToIndexMap.of());
        assertSame(m, StringToIndexMap.of(new Entry[0]));
    }

    @Test
    public void testSize1() {
        StringToIndexMap m = StringToIndexMap.of("i", 0);
        assertEquals(1, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(ImmutableList.of("i"), m.getKeys());
        assertEquals("i", m.getKeyOfValue(0));
        assertNull(m.getKeyOfValue(1));
    }

    @Test
    public void testSize2() {
        StringToIndexMap m = StringToIndexMap.of("i", 0, "j", 1);
        assertEquals(2, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(ImmutableList.of("i", "j"), m.getKeys());
        assertEquals("i", m.getKeyOfValue(0));
        assertEquals("j", m.getKeyOfValue(1));
        assertNull(m.getKeyOfValue(2));
    }

    @Test
    public void testSize3() {
        StringToIndexMap m = StringToIndexMap.of("i", 0, "j", 1, "k", 2);
        assertEquals(3, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(2, m.get("k"));
        assertEquals(ImmutableList.of("i", "j", "k"), m.getKeys());
    }

    @Test
    public void testSize4() {
        StringToIndexMap m = StringToIndexMap.of("i", 0, "j", 1, "k", 2, "l", 3);
        assertEquals(4, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(2, m.get("k"));
        assertEquals(3, m.get("l"));
        assertEquals(ImmutableList.of("i", "j", "k", "l"), m.getKeys());
    }

    @Test
    public void testSize5() {
        StringToIndexMap m = StringToIndexMap.of(
                "i", 0, "j", 1, "k", 2, "l", 3, "m", 4);
        assertEquals(5, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(2, m.get("k"));
        assertEquals(3, m.get("l"));
        assertEquals(4, m.get("m"));
        assertEquals(ImmutableList.of("i", "j", "k", "l", "m"), m.getKeys());
    }

    @Test
    public void testSize6() {
        StringToIndexMap m = StringToIndexMap.of(
                "i", 0, "j", 1, "k", 2, "l", 3, "m", 4, "n", 5);
        assertEquals(6, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(2, m.get("k"));
        assertEquals(3, m.get("l"));
        assertEquals(4, m.get("m"));
        assertEquals(5, m.get("n"));
        assertEquals(ImmutableList.of("i", "j", "k", "l", "m", "n"), m.getKeys());
    }

    @Test
    public void testBufferWithExcessCapacity() {
        StringToIndexMap m = StringToIndexMap.of(
                new Entry[]{
                        new Entry("i", 0), new Entry("j", 1), new Entry("k", 2),
                        null, null, null, null
                }, 3);
        assertEquals(3, m.size());
        assertEquals(-1, m.get("a"));
        assertEquals(0, m.get("i"));
        assertEquals(1, m.get("j"));
        assertEquals(2, m.get("k"));
        assertEquals(ImmutableList.of("i", "j", "k"), m.getKeys());
    }

    @Test
    public void testSizeBig() {
        // We try several sizes here, to catch rarely occurring bugs (needs a bucket of size 3 and such):
        for (int size = 7; size <= 7 * 2 * 2 * 2 * 2 * 2 * 2 * 2; size *= 2) {
            Entry[] entries = new Entry[size];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = new Entry(
                        "a" + i + "_" + i,  // This will hopefully generate some clashes
                        i);
            }

            StringToIndexMap map = StringToIndexMap.of(entries);
            assertEquals(size, map.size());
            assertEquals(size, map.getKeys().size());
            for (Entry entry : entries) {
                assertEquals(entry.getValue(), map.get(entry.getKey()));

                // Hoping for some matching buckets here:
                for (int i = 0; i < 10; i++) {
                    assertEquals(-1, map.get(i + "-" + entry.getKey()));
                }
            }
        }
    }

    @Test
    public void testClashingKey() {
        try {
            StringToIndexMap.of("foo", 0, "foo", 1);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("\"foo\""));
        }
    }

}

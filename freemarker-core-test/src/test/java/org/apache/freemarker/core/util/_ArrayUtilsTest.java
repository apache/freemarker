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

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class _ArrayUtilsTest {

    @Test
    public void testAddAll() {
        Object [] arr = _ArrayUtils.addAll(null);
        assertTrue(arr.length == 0);

        arr = _ArrayUtils.addAll(null, null);
        assertNull(arr);

        Object[] arr1 = { "a", "b", "c" };
        Object[] arr2 = { "1", "2", "3" };
        Object[] arrAll = { "a", "b", "c", "1", "2", "3" };

        arr = _ArrayUtils.addAll(arr1, null);
        assertNotSame(arr1, arr);
        assertArrayEquals(arr1, arr);

        arr = _ArrayUtils.addAll(null, arr2);
        assertNotSame(arr2, arr);
        assertArrayEquals(arr2, arr);

        arr = _ArrayUtils.addAll(arr1, arr2);
        assertArrayEquals(arrAll, arr);
    }

    @Test
    public void testClone() {
        assertArrayEquals(null, _ArrayUtils.clone((Object[]) null));
        Object[] original1 = new Object[0];
        Object[] cloned1 = _ArrayUtils.clone(original1);
        assertTrue(Arrays.equals(original1, cloned1));
        assertTrue(original1 != cloned1);

        final StringBuilder builder = new StringBuilder("pick");
        original1 = new Object[]{builder, "a", new String[]{"stick"}};
        cloned1 = _ArrayUtils.clone(original1);
        assertTrue(Arrays.equals(original1, cloned1));
        assertTrue(original1 != cloned1);
        assertSame(original1[0], cloned1[0]);
        assertSame(original1[1], cloned1[1]);
        assertSame(original1[2], cloned1[2]);
    }

}
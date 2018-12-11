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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Don't use this; used internally by FreeMarker, might changes without notice.
 * Immutable list that wraps an array that's known to be non-changing.
 */
public class _ArrayAdapterList<E> extends AbstractList<E> {

    private final E[] array;

    public static <E> _ArrayAdapterList<E> adapt(E[] array) {
        return  array != null ? new _ArrayAdapterList<E>(array) : null;
    }

    private _ArrayAdapterList(E[] array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public E get(int index) {
        return array[index];
    }

    @Override
    public Iterator<E> iterator() {
        return new _ArrayIterator<E>(array);
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(array, array.length);
    }

}

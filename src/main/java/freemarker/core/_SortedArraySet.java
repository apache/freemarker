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

package freemarker.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
// [Java 5] Make this generic
public class _SortedArraySet extends _UnmodifiableSet {

    private final Object[] array;

    public _SortedArraySet(Object[] array) {
        this.array = array;
    }

    public int size() {
        return array.length;
    }

    public boolean contains(Object o) {
        return Arrays.binarySearch(array, o) >= 0;
    }

    public Iterator iterator() {
        return new _ArrayIterator(array);
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
    
}

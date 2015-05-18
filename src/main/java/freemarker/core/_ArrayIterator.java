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

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _ArrayIterator implements Iterator {

    private final Object[] array;
    private int nextIndex;

    public _ArrayIterator(Object[] array) {
        this.array = array;
        this.nextIndex = 0;
    }

    public boolean hasNext() {
        return nextIndex < array.length;
    }

    public Object next() {
        if (nextIndex >= array.length) {
            throw new NoSuchElementException();
        }
        return array[nextIndex++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}

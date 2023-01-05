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

package freemarker.core;

import java.util.Iterator;
import java.util.Set;

/** Don't use this; used internally by FreeMarker, might change without notice. */
public class _UnmodifiableCompositeSet<E> extends _UnmodifiableSet<E> {
    
    private final Set<E> set1, set2;
    
    public _UnmodifiableCompositeSet(Set<E> set1, Set<E> set2) {
        this.set1 = set1;
        this.set2 = set2;
    }

    @Override
    public Iterator<E> iterator() {
        return new CompositeIterator();
    }
    
    @Override
    public boolean contains(Object o) {
        return set1.contains(o) || set2.contains(o);
    }

    @Override
    public int size() {
        return set1.size() + set2.size();
    }
    
    private class CompositeIterator implements Iterator<E> {

        private Iterator<E> it1, it2;
        private boolean it1Deplected;
        
        @Override
        public boolean hasNext() {
            if (!it1Deplected) {
                if (it1 == null) {
                    it1 = set1.iterator();
                }
                if (it1.hasNext()) {
                    return true;
                }
                
                it2 = set2.iterator();
                it1 = null;
                it1Deplected = true;
                // Falls through
            }
            return it2.hasNext();
        }

        @Override
        public E next() {
            if (!it1Deplected) {
                if (it1 == null) {
                    it1 = set1.iterator();
                }
                if (it1.hasNext()) {
                    return it1.next();
                }
                
                it2 = set2.iterator();
                it1 = null;
                it1Deplected = true;
                // Falls through
            }
            return it2.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
    
}

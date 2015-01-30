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
import java.util.Set;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
//[Java 5] Make this generic
public class _UnmodifiableCompositeSet extends _UnmodifiableSet {
    
    private final Set set1, set2;
    
    public _UnmodifiableCompositeSet(Set set1, Set set2) {
        this.set1 = set1;
        this.set2 = set2;
    }

    public Iterator iterator() {
        return new CompositeIterator();
    }
    
    public boolean contains(Object o) {
        return set1.contains(o) || set2.contains(o);
    }

    public int size() {
        return set1.size() + set2.size();
    }
    
    private class CompositeIterator implements Iterator {

        private Iterator it1, it2;
        private boolean it1Deplected;
        
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

        public Object next() {
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

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
    
}

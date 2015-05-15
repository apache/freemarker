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
package freemarker.test.templatesuite.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ListableValueFactory {
    
    public List<Integer> getList() {
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(11);
        arrayList.add(22);
        arrayList.add(33);
        return arrayList;
    }
    
    public Set<Integer> getSet() {
        Set<Integer> set = new TreeSet<Integer>();
        set.add(11);
        set.add(22);
        set.add(33);
        return set;
    }

    public Iterator<Integer> getIterator() {
        Set<Integer> set = new TreeSet<Integer>();
        set.add(11);
        set.add(22);
        set.add(33);
        return set.iterator();
    }

    public List<Integer> getEmptyList() {
        return Collections.emptyList();
    }
    
    public Set<Integer> getEmptySet() {
        return Collections.emptySet();
    }

    public Iterator<Integer> getEmptyIterator() {
        return Collections.<Integer>emptySet().iterator();
    }
    
}

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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("boxing")
public class Listables {
    
    private static final List<Integer> LIST;
    static {
        List<Integer> list = new ArrayList<Integer>();
        list.add(11);
        list.add(22);
        list.add(33);
        LIST = list;
    }

    private static final List<Integer> LINKED_LIST;
    static {
        List<Integer> list = new LinkedList<Integer>();
        list.add(11);
        list.add(22);
        list.add(33);
        LINKED_LIST = list;
    }

    private static final List<Integer> EMPTY_LINKED_LIST;
    static {
        List<Integer> list = new LinkedList<Integer>();
        EMPTY_LINKED_LIST = list;
    }
    
    private static final Set<Integer> SET;
    static {
        Set<Integer> set = new TreeSet<Integer>();
        set.add(11);
        set.add(22);
        set.add(33);
        SET = set;
    }
    
    public List<Integer> getList() {
        return LIST;
    }
    
    public List<Integer> getLinkedList() {
        return LINKED_LIST;
    }
    
    public Set<Integer> getSet() {
        return SET;
    }

    public Iterator<Integer> getIterator() {
        return SET.iterator();
    }

    public List<Integer> getEmptyList() {
        return Collections.emptyList();
    }
    
    public List<Integer> getEmptyLinkedList() {
        return Collections.emptyList();
    }
    
    public Set<Integer> getEmptySet() {
        return Collections.emptySet();
    }

    public Iterator<Integer> getEmptyIterator() {
        return Collections.<Integer>emptySet().iterator();
    }
    
}

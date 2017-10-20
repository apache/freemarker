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
package org.apache.freemarker.core.templatesuite.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.impl.DefaultMapAdapter;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.model.impl.SimpleHash;

@SuppressWarnings("boxing")
public class Listables {
    
    private static final List<Integer> LIST;
    static {
        List<Integer> list = new ArrayList<>();
        list.add(11);
        list.add(22);
        list.add(33);
        LIST = list;
    }

    private static final List<Integer> LINKED_LIST;
    static {
        List<Integer> list = new LinkedList<>();
        list.add(11);
        list.add(22);
        list.add(33);
        LINKED_LIST = list;
    }

    private static final Set<Integer> SET;
    static {
        Set<Integer> set = new TreeSet<>();
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
    
    public List<TemplateHashModelEx> getHashExs() throws TemplateException {
        Map<Object, Object> map;
        map = new LinkedHashMap<>();
        map.put("k1", "v1");
        map.put(2, "v2");
        map.put("k3", "v3");
        map.put(null, "v4");
        map.put(true, "v5");
        map.put(false, null);
        
        return getMapsWrappedAsEx(map);
    }

    public List<? extends TemplateHashModelEx> getEmptyHashes() throws ObjectWrappingException {
        List<TemplateHashModelEx> emptyMaps = new ArrayList<>();
        emptyMaps.addAll(getMapsWrappedAsEx(Collections.emptyMap()));
        emptyMaps.add((TemplateHashModelEx) new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build()
                .wrap(Collections.emptyMap()));
        return emptyMaps;
    }
    
    /**
     * Returns the map wrapped on various ways.
     */
    private List<TemplateHashModelEx> getMapsWrappedAsEx(Map<?, ?> map) throws ObjectWrappingException {
        List<TemplateHashModelEx> maps = new ArrayList<>();
        
        DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        maps.add(new SimpleHash(map, ow));
        maps.add((DefaultMapAdapter) ow.wrap(map));

        return maps;
    }
    
}

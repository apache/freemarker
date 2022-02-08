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
package freemarker.test.templatesuite.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ImmutableMap;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.WrappingTemplateModel;

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

    private static final List<Integer> EMPTY_LINKED_LIST;
    static {
        List<Integer> list = new LinkedList<>();
        EMPTY_LINKED_LIST = list;
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
    
    public List<TemplateHashModelEx2> getHashEx2s() throws TemplateModelException {
        Map<Object, Object> map;
        map = new LinkedHashMap<>();
        map.put("k1", "v1");
        map.put(2, "v2");
        map.put("k3", "v3");
        map.put(null, "v4");
        map.put(true, "v5");
        map.put(false, null);
        
        return getMapsWrappedAsEx2(map);
    }

    public List<? extends TemplateHashModelEx> getEmptyHashes() throws TemplateModelException {
        List<TemplateHashModelEx> emptyMaps = new ArrayList<>();
        emptyMaps.addAll(getMapsWrappedAsEx2(Collections.emptyMap()));
        emptyMaps.add((TemplateHashModelEx) new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_24).build()
                .wrap(Collections.emptyMap()));
        return emptyMaps;
    }
    
    /**
     * Returns the map wrapped on various ways.
     */
    private List<TemplateHashModelEx2> getMapsWrappedAsEx2(Map<?, ?> map) throws TemplateModelException {
        List<TemplateHashModelEx2> maps = new ArrayList<>();
        
        maps.add((SimpleHash) new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_0).build().wrap(map));
        
        maps.add((DefaultMapAdapter) new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_24).build().wrap(map));
        
        BeansWrapperBuilder bwb = new BeansWrapperBuilder(Configuration.VERSION_2_3_24);
        bwb.setSimpleMapWrapper(true);
        maps.add((TemplateHashModelEx2) bwb.build().wrap(map));

        return maps;
    }
    
    public TemplateHashModelEx getHashNonEx2() {
        return new NonEx2MapAdapter(ImmutableMap.of("k1", 11, "k2", 22),
                new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_24).build());
    }
    
    public static class NonEx2MapAdapter extends WrappingTemplateModel implements TemplateHashModelEx {

        private final Map<?, ?> map;
        
        public NonEx2MapAdapter(Map<?, ?> map, ObjectWrapper wrapper) {
            super(wrapper);
            this.map = map;
        }
        
        public TemplateModel get(String key) throws TemplateModelException {
            return wrap(map.get(key));
        }
        
        public boolean isEmpty() {
            return map.isEmpty();
        }
        
        public int size() {
            return map.size();
        }
        
        public TemplateCollectionModel keys() {
            return new SimpleCollection(map.keySet(), getObjectWrapper());
        }
        
        public TemplateCollectionModel values() {
            return new SimpleCollection(map.values(), getObjectWrapper());
        }
        
    }
    
}

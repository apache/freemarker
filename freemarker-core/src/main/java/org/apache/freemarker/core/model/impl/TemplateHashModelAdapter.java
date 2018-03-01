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

package org.apache.freemarker.core.model.impl;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePairIterator;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.util.UndeclaredThrowableException;

/**
 * Adapts a {@link TemplateHashModel} to a {@link Map}.
 */
class TemplateHashModelAdapter<K, V> extends AbstractMap<K, V> implements TemplateModelAdapter {
    private final DefaultObjectWrapper wrapper;
    private final TemplateHashModel model;
    private Set<Map.Entry<K, V>> entrySet;
    
    TemplateHashModelAdapter(TemplateHashModel model, DefaultObjectWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    @Override
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    @Override
    public boolean isEmpty() {
        try {
            return getModelEx().isEmptyHash();
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        // TODO [FM3] This restriction must be removed when TemplateHashModel allows non-string keys.
        if (!(key instanceof String)) {
            return null; 
        }
        try {
            return (V) wrapper.unwrap(model.get((String) key));
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    @Override
    public int size() {
        try {
            return getModelEx().getHashSize();
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        // A quick check that doesn't require TemplateHashModelEx 
        if (get(key) != null) {
            return true;
        }
        return super.containsKey(key);
    }
    
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet != null) {
            return entrySet;
        }
        return entrySet = new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                final KeyValuePairIterator kvpIter;
                try {
                     kvpIter = getModelEx().keyValuePairIterator();
                } catch (TemplateException e) {
                    throw new UndeclaredThrowableException(e);
                }
                return new Iterator<Map.Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        try {
                            return kvpIter.hasNext();
                        } catch (TemplateException e) {
                            throw new UndeclaredThrowableException(e);
                        }
                    }
                    
                    @Override
                    public Map.Entry<K, V> next() {
                        final KeyValuePair kvp;
                        try {
                            if (!kvpIter.hasNext()) {
                                throw new NoSuchElementException();
                            }
                            kvp = kvpIter.next();
                        } catch (TemplateException e) {
                            throw new UndeclaredThrowableException(e);
                        }
                        return new Map.Entry<K, V>() {
                            private boolean keyCalculated;
                            private K key;
                            
                            private boolean valueCalculated;
                            private V value;
                            
                            @SuppressWarnings("unchecked")
                            @Override
                            public K getKey() {
                                    if (!keyCalculated) {
                                        try {
                                            key = (K) wrapper.unwrap(kvp.getKey());;
                                        } catch (TemplateException e) {
                                            throw new UndeclaredThrowableException(e);
                                        }
                                        keyCalculated = true;
                                    }
                                    return key;
                            }
                            
                            @SuppressWarnings("unchecked")
                            @Override
                            public V getValue() {
                                if (!valueCalculated) {
                                    try {
                                        value = (V) wrapper.unwrap(kvp.getValue());;
                                    } catch (TemplateException e) {
                                        throw new UndeclaredThrowableException(e);
                                    }
                                    valueCalculated = true;
                                }
                                return value;
                            }
                            
                            @Override
                            public V setValue(Object value) {
                                throw new UnsupportedOperationException();
                            }
                            
                            @SuppressWarnings("unchecked")
                            @Override
                            public boolean equals(Object o) {
                                if (!(o instanceof Map.Entry)) {
                                    return false;
                                }
                                Map.Entry<K, V> e = (Map.Entry <K, V>) o;
                                Object k1 = getKey();
                                Object k2 = e.getKey();
                                if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                                    Object v1 = getValue();
                                    Object v2 = e.getValue();
                                    if (v1 == v2 || (v1 != null && v1.equals(v2))) { 
                                        return true;
                                    }
                                }
                                return false;
                            }
                        
                            @Override
                            public int hashCode() {
                                K key = getKey();
                                V value = getValue();
                                return (key == null ? 0 : key.hashCode()) ^
                                       (value == null ? 0 : value.hashCode());
                            }
                        };
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            
            @Override
            public int size() {
                try {
                    return getModelEx().getHashSize();
                } catch (TemplateException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        };
    }
    
    private TemplateHashModelEx getModelEx() {
        if (model instanceof TemplateHashModelEx) {
            return ((TemplateHashModelEx) model);
        }
        throw new UnsupportedOperationException(
                "Operation supported only on TemplateHashModelEx. " + 
                model.getClass().getName() + " does not implement it though.");
    }
    
}

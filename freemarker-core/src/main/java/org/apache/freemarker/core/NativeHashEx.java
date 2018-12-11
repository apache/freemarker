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

package org.apache.freemarker.core;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * A hash where each value is already a {@link TemplateModel}, so no {@link ObjectWrapper} need to be specified.
 *
 * <p>While this class allows adding items, doing so is not thread-safe, and thus only meant to be done during the
 * initialization of the sequence.
 */
class NativeHashEx implements TemplateHashModelEx, Serializable {

    private final LinkedHashMap<String, TemplateModel> map;

    public NativeHashEx() {
        this.map = new LinkedHashMap<>();
    }

    @Override
    public int getHashSize() throws TemplateException {
        return map.size();
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return map.get(key);
    }

    @Override
    public boolean isEmptyHash() throws TemplateException {
        return map.isEmpty();
    }

    @Override
    public TemplateHashModelEx.KeyValuePairIterator keyValuePairIterator() throws TemplateException {
        return new TemplateHashModelEx.KeyValuePairIterator() {
            private final Iterator<Map.Entry<String, TemplateModel>> entrySetIterator = map.entrySet().iterator();

            @Override
            public boolean hasNext() throws TemplateException {
                return entrySetIterator.hasNext();
            }

            @Override
            public TemplateHashModelEx.KeyValuePair next() throws TemplateException {
                return new TemplateHashModelEx.KeyValuePair() {
                    private final Map.Entry<String, TemplateModel> entry = entrySetIterator.next();

                    @Override
                    public TemplateModel getKey() throws TemplateException {
                        return new SimpleString(entry.getKey());
                    }

                    @Override
                    public TemplateModel getValue() throws TemplateException {
                        return entry.getValue();
                    }
                };
            }
        };
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateException {
        return new NativeStringCollectionCollection(map.keySet());
    }

    @Override
    public TemplateCollectionModel values() throws TemplateException {
        return new NativeCollection(map.values());
    }

    public TemplateModel put(String key, TemplateModel value) {
        return map.put(key, value);
    }

}

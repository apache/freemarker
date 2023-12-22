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
package freemarker.template;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;

/**
 *  Implementation of {@link KeyValuePairIterator} for a {@link TemplateHashModelEx2} that wraps or otherwise uses a
 *  {@link Map} internally.
 *
 *  @since 2.3.25
 */
public class MapKeyValuePairIterator implements KeyValuePairIterator {

    private final Iterator<Entry<?, ?>> entrySetIterator;
    
    private final ObjectWrapper objectWrapper;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <K, V> MapKeyValuePairIterator(Map<?, ?> map, ObjectWrapper objectWrapper) {
        entrySetIterator = ((Map) map).entrySet().iterator();
        this.objectWrapper = objectWrapper;
    }

    @Override
    public boolean hasNext() {
        return entrySetIterator.hasNext();
    }

    @Override
    public KeyValuePair next() {
        final Entry<?, ?> entry = entrySetIterator.next();
        return new KeyValuePair() {

            @Override
            public TemplateModel getKey() throws TemplateModelException {
                return wrap(entry.getKey());
            }

            @Override
            public TemplateModel getValue() throws TemplateModelException {
                return wrap(entry.getValue());
            }
            
        };
    }
    
    private TemplateModel wrap(Object obj) throws TemplateModelException {
        return (obj instanceof TemplateModel) ? (TemplateModel) obj : objectWrapper.wrap(obj);
    }

}

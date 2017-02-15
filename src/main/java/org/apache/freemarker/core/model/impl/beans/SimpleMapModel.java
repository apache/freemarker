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

package org.apache.freemarker.core.model.impl.beans;

import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.ast.CollectionAndSequence;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.MapKeyValuePairIterator;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateMethodModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;
import org.apache.freemarker.core.model.impl.SimpleSequence;
import org.apache.freemarker.core.util.RichObjectWrapper;

/**
 * Model used by {@link BeansWrapper} when <tt>simpleMapWrapper</tt>
 * mode is enabled. Provides a simple hash model interface to the
 * underlying map (does not copy like {@link org.apache.freemarker.core.model.impl.SimpleHash}),
 * and a method interface to non-string keys.
 */
public class SimpleMapModel extends WrappingTemplateModel 
implements TemplateHashModelEx2, TemplateMethodModelEx, AdapterTemplateModel, 
WrapperTemplateModel, TemplateModelWithAPISupport {
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper) {
                return new SimpleMapModel((Map) object, (BeansWrapper) wrapper);
            }
        };

    private final Map map;
    
    public SimpleMapModel(Map map, BeansWrapper wrapper) {
        super(wrapper);
        this.map = map;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object val = map.get(key);
        if (val == null) {
            if (key.length() == 1) {
                // just check for Character key if this is a single-character string
                Character charKey = Character.valueOf(key.charAt(0));
                val = map.get(charKey);
                if (val == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                    return null;
                }
            } else if (!map.containsKey(key)) {
                return null;
            }
        }
        return wrap(val);
    }
    
    public Object exec(List args) throws TemplateModelException {
        Object key = ((BeansWrapper) getObjectWrapper()).unwrap((TemplateModel) args.get(0));
        Object value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            return null;
        }
        return wrap(value);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    public TemplateCollectionModel keys() {
        return new CollectionAndSequence(new SimpleSequence(map.keySet(), getObjectWrapper()));
    }

    public TemplateCollectionModel values() {
        return new CollectionAndSequence(new SimpleSequence(map.values(), getObjectWrapper()));
    }
    
    public KeyValuePairIterator keyValuePairIterator() {
        return new MapKeyValuePairIterator(map, getObjectWrapper());
    }

    public Object getAdaptedObject(Class hint) {
        return map;
    }
    
    public Object getWrappedObject() {
        return map;
    }

    public TemplateModel getAPI() throws TemplateModelException {
        return ((RichObjectWrapper) getObjectWrapper()).wrapAsAPI(map);
    }
}
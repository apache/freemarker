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

package freemarker.ext.beans;

import java.util.List;
import java.util.Map;

import freemarker.core.CollectionAndSequence;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.MapKeyValuePairIterator;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelWithAPISupport;
import freemarker.template.WrappingTemplateModel;
import freemarker.template.utility.RichObjectWrapper;

/**
 * Model used by {@link BeansWrapper} when <tt>simpleMapWrapper</tt>
 * mode is enabled. Provides a simple hash model interface to the
 * underlying map (does not copy like {@link freemarker.template.SimpleHash}),
 * and a method interface to non-string keys.
 */
public class SimpleMapModel extends WrappingTemplateModel 
implements TemplateHashModelEx2, TemplateMethodModelEx, AdapterTemplateModel, 
WrapperTemplateModel, TemplateModelWithAPISupport {
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            @Override
            public TemplateModel create(Object object, ObjectWrapper wrapper) {
                return new SimpleMapModel((Map) object, (BeansWrapper) wrapper);
            }
        };

    private final Map map;
    
    public SimpleMapModel(Map map, BeansWrapper wrapper) {
        super(wrapper);
        this.map = map;
    }

    @Override
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
    
    @Override
    public Object exec(List args) throws TemplateModelException {
        Object key = ((BeansWrapper) getObjectWrapper()).unwrap((TemplateModel) args.get(0));
        Object value = map.get(key);
        if (value == null && !map.containsKey(key)) {
            return null;
        }
        return wrap(value);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public TemplateCollectionModel keys() {
        return new CollectionAndSequence(new SimpleSequence(map.keySet(), getObjectWrapper()));
    }

    @Override
    public TemplateCollectionModel values() {
        return new CollectionAndSequence(new SimpleSequence(map.values(), getObjectWrapper()));
    }
    
    @Override
    public KeyValuePairIterator keyValuePairIterator() {
        return new MapKeyValuePairIterator(map, getObjectWrapper());
    }

    @Override
    public Object getAdaptedObject(Class hint) {
        return map;
    }
    
    @Override
    public Object getWrappedObject() {
        return map;
    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return ((RichObjectWrapper) getObjectWrapper()).wrapAsAPI(map);
    }
}
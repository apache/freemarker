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

import java.util.Map;
import java.util.SortedMap;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperWithAPISupport;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;

/**
 * Adapts a {@link Map} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@link Map}, but build a hash specifically to
 * be used from a template, also consider using {@link SimpleHash} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link DefaultMapAdapter} is as thread-safe as the {@link Map} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these hashes (though of
 * course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 */
public class DefaultMapAdapter extends WrappingTemplateModel
        implements TemplateHashModelEx2, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport {

    private final Map map;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param map
     *            The map to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array.
     */
    public static DefaultMapAdapter adapt(Map map, ObjectWrapperWithAPISupport wrapper) {
        return new DefaultMapAdapter(map, wrapper);
    }
    
    private DefaultMapAdapter(Map map, ObjectWrapperWithAPISupport wrapper) {
        super(wrapper);
        this.map = map;
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        Object val;
        try {
            val = map.get(key);
        } catch (ClassCastException e) {
            throw new TemplateException(e,
                    "ClassCastException while getting Map entry with String key ",
                    new _DelayedJQuote(key));
        } catch (NullPointerException e) {
            throw new TemplateException(e,
                    "NullPointerException while getting Map entry with String key ",
                    new _DelayedJQuote(key));
        }
            
        if (val == null) {
            // Check for Character key if this is a single-character string.
            // In SortedMap-s, however, we can't do that safely, as it can cause ClassCastException.
            if (key.length() == 1 && !(map instanceof SortedMap)) {
                Character charKey = Character.valueOf(key.charAt(0));
                try {
                    val = map.get(charKey);
                    if (val == null) {
                        TemplateModel wrappedNull = wrap(null);
                        if (wrappedNull == null || !(map.containsKey(key) || map.containsKey(charKey))) {
                            return null;
                        } else {
                            return wrappedNull;
                        }
                    } 
                } catch (ClassCastException e) {
                    throw new TemplateException(e,
                                    "Class casting exception while getting Map entry with Character key ",
                                    new _DelayedJQuote(charKey));
                } catch (NullPointerException e) {
                    throw new TemplateException(e,
                                    "NullPointerException while getting Map entry with Character key ",
                                    new _DelayedJQuote(charKey));
                }
            } else {  // No char key fallback was possible
                TemplateModel wrappedNull = wrap(null);
                if (wrappedNull == null || !map.containsKey(key)) {
                    return null;
                } else {
                    return wrappedNull;
                }
            }
        }
        
        return wrap(val);
    }

    @Override
    public boolean isEmptyHash() {
        return map.isEmpty();
    }

    @Override
    public int getHashSize() {
        return map.size();
    }

    @Override
    public TemplateCollectionModel keys() {
        return DefaultNonListCollectionAdapter.adapt(map.keySet(), (ObjectWrapperWithAPISupport) getObjectWrapper());
    }

    @Override
    public TemplateCollectionModel values() {
        return DefaultNonListCollectionAdapter.adapt(map.values(), (ObjectWrapperWithAPISupport) getObjectWrapper());
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
    public TemplateModel getAPI() throws TemplateException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(map);
    }

}

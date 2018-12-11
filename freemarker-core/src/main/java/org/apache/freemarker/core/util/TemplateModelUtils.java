package org.apache.freemarker.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._EvalUtils;
import org.apache.freemarker.core._KVPCollectionKVPIterator;
import org.apache.freemarker.core._KVPCollectionKeysModel;
import org.apache.freemarker.core._KVPCollectionValuesModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;

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
public class TemplateModelUtils {

    /**
     * Same as {@link #wrapAsHashUnion(ObjectWrapper, List)}, but uses a varargs parameter instead of a {@link List}. 
     */
    public static TemplateHashModel wrapAsHashUnion(ObjectWrapper objectWrapper, Object... hashLikeObjects)
            throws TemplateException {
        return wrapAsHashUnion(objectWrapper, Arrays.asList(hashLikeObjects));
    }
    
    /**
     * Creates a {@link TemplateHashModel} that is the union of the hash-like objects passed in as argument. Hash-like
     * here means that the argument {@link ObjectWrapper} will wrap it into an {@link TemplateModel} that implements
     * {@link TemplateHashModel}, or it's already a {@link TemplateHashModel}. (Typical hash-like objects are JavaBeans
     * and {@link Map}-s, though it depends on the {@link ObjectWrapper}.)
     * 
     * <p>
     * This method is typical used when you want to compose a data-model from multiple objects in a way so that their
     * entries ({@link Map} key-value pairs, bean properties, etc.) appear together on the top level of the data-model.
     * In such case, use the return value of this method as the combined data-model. Note that this functionality
     * somewhat overlaps with {@link Configuration#getSharedVariables()}; check if that fits your use case better.
     * 
     * @param objectWrapper
     *            {@link ObjectWrapper} used to wrap the elements of {@code hashLikeObjects}, except those that are
     *            already {@link TemplateModel}-s. Usually, you should pass in {@link Configuration#getObjectWrapper()}
     *            here.
     * @param hashLikeObjects
     *            Hash-like objects whose union the result hash will be. The content of these hash-like objects must not
     *            change, or else the behavior of the resulting hash can be erratic. If multiple hash-like object
     *            contains the same key, then the value from the last such hash-like object wins. The oder of keys is
     *            kept, with the keys of earlier hash-like object object coming first (even if their values were
     *            replaced by a later hash-like object). This argument can't be {@code null}, but the list can contain
     *            {@code null} elements, which will be silently ignored. The list can be empty, in which case the result
     *            is an empty hash.
     * 
     * @return The {@link TemplateHashModel} that's the union of the objects provided. This is a "view", that delegates
     *         to the underlying hashes, not a copy. The object is not thread safe. If all elements in
     *         {@code hashLikeObjects} are {@link TemplateHashModelEx} objects (or if there are 0 elements), then the
     *         result will implement {@link TemplateHashModelEx} as well.
     * 
     * @throws TemplateException
     *             If wrapping an element of {@code hashLikeObjects} fails with {@link TemplateException}, or if
     *             wrapping an element results in a {@link TemplateModel} that's not a {@link TemplateHashModel}, or if
     *             the element was already a {@link TemplateModel} that isn't a {@link TemplateHashModel}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static TemplateHashModel wrapAsHashUnion(ObjectWrapper objectWrapper, List<?> hashLikeObjects)
            throws TemplateException {
        _NullArgumentException.check("hashLikeObjects", hashLikeObjects);
        
        List<TemplateHashModel> hashes = new ArrayList<TemplateHashModel>(hashLikeObjects.size());
        
        boolean allTHMEx = true;
        for (Object hashLikeObject : hashLikeObjects) {
            if (hashLikeObject == null) {
                continue;
            }
            
            TemplateModel tm;
            if (hashLikeObject instanceof TemplateModel) {
                tm = (TemplateModel) hashLikeObject;
            } else {
                tm = objectWrapper.wrap(hashLikeObject);
            }
            
            if (!(tm instanceof TemplateHashModelEx)) {
                allTHMEx = false;
                if (!(tm instanceof TemplateHashModel)) {
                    throw new TemplateException(
                            "One of the objects of the hash union is not hash-like: "
                            + TemplateLanguageUtils.getTypeDescription(tm));
                }
            }
            
            hashes.add((TemplateHashModel) tm);
        }
        
        return  hashes.isEmpty() ? TemplateHashModel.EMPTY_HASH
                : hashes.size() == 1 ? hashes.get(0)
                : allTHMEx ? new HashExUnionModel((List) hashes)
                : new HashUnionModel(hashes);
    }

    private static abstract class AbstractHashUnionModel<T extends TemplateHashModel> implements TemplateHashModel {
        protected final List<? extends T> hashes;

        public AbstractHashUnionModel(List<? extends T> hashes) {
            this.hashes = hashes;
        }

        @Override
        public TemplateModel get(String key) throws TemplateException {
            for (int i = hashes.size() - 1; i >= 0; i--) {
                TemplateModel value = hashes.get(i).get(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        
    }
    
    private static class HashUnionModel extends AbstractHashUnionModel<TemplateHashModel> {
        HashUnionModel(List<? extends TemplateHashModel> hashes) {
            super(hashes);
        }
    }

    private static final class HashExUnionModel extends AbstractHashUnionModel<TemplateHashModelEx>
            implements TemplateHashModelEx {
        /** Lazily calculated list of key-value pairs; there's only one item per duplicate key. */
        private Collection<KeyValuePair> kvps;

        private HashExUnionModel(List<? extends TemplateHashModelEx> hashes) {
            super(hashes);
        }

        @Override
        public boolean isEmptyHash() throws TemplateException {
            for (TemplateHashModelEx hash : hashes) {
                if (!hash.isEmptyHash()) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public int getHashSize() throws TemplateException {
            initKvps();
            return kvps.size();
        }

        @Override
        public TemplateCollectionModel keys() throws TemplateException {
            initKvps();
            return new _KVPCollectionKeysModel(kvps);
        }

        @Override
        public TemplateCollectionModel values() throws TemplateException {
            initKvps();
            return new _KVPCollectionValuesModel(kvps);
        }
        
        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
            initKvps();
            return new _KVPCollectionKVPIterator(kvps);
        }

        /**
         * We must precreate the whole key-value pair list, as we have to deal with duplicate keys. 
         */
        private void initKvps() throws TemplateException {
            if (kvps != null) {
                return;
            }
            
            Map<Object, KeyValuePair> kvpsMap = new LinkedHashMap<>();
            for (TemplateHashModelEx hash : hashes) {
                putKVPs(kvpsMap, hash);
            } 
            this.kvps = kvpsMap.values();
        }

        private static void putKVPs(Map<Object, KeyValuePair> kvps, TemplateHashModelEx hash) throws TemplateException {
            for (KeyValuePairIterator iter = hash.keyValuePairIterator(); iter.hasNext(); ) {
                KeyValuePair kvp = iter.next();
                kvps.put(_EvalUtils.unwrapTemplateHashModelKey(kvp.getKey()), kvp);
            }
        }
        
    }
     
}

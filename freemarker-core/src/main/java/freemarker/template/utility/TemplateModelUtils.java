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
package freemarker.template.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.core.CollectionAndSequence;
import freemarker.core._MessageUtil;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template._ObjectWrappers;

/**
 * Static utility method related to {@link TemplateModel}-s that didn't fit elsewhere.
 * 
 * @since 2.3.28
 */
public final class TemplateModelUtils {

    // Private to prevent instantiation
    private TemplateModelUtils() {
        // no op.
    }

    /**
     * {@link TemplateHashModelExKeyValuePairIterator} that even works for a non-{@link TemplateHashModelEx2}
     * {@link TemplateHashModelEx}. This is used to simplify code that needs to iterate through the key-value pairs of
     * {@link TemplateHashModelEx}-s, as with this you don't have to handle non-{@link TemplateHashModelEx2}-s
     * separately. For non-{@link TemplateHashModelEx2} values the iteration will throw {@link TemplateModelException}
     * if it reaches a key that's not a string ({@link TemplateScalarModel}).
     * 
     * @since 2.3.28
     */
    public static final TemplateHashModelEx2.KeyValuePairIterator getKeyValuePairIterator(TemplateHashModelEx hash)
            throws TemplateModelException {
        return hash instanceof TemplateHashModelEx2 ? ((TemplateHashModelEx2) hash).keyValuePairIterator()
                : new TemplateHashModelExKeyValuePairIterator(hash);
    }

    private static class TemplateHashModelExKeyValuePairIterator implements TemplateHashModelEx2.KeyValuePairIterator {

        private final TemplateHashModelEx hash;
        private final TemplateModelIterator keyIter;

        private TemplateHashModelExKeyValuePairIterator(TemplateHashModelEx hash) throws TemplateModelException {
            this.hash = hash;
            keyIter = hash.keys().iterator();
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            return keyIter.hasNext();
        }

        @Override
        public KeyValuePair next() throws TemplateModelException {
            final TemplateModel key = keyIter.next();
            if (!(key instanceof TemplateScalarModel)) {
                throw _MessageUtil.newKeyValuePairListingNonStringKeyExceptionMessage(key, hash);
            }

            return new KeyValuePair() {

                @Override
                public TemplateModel getKey() throws TemplateModelException {
                    return key;
                }

                @Override
                public TemplateModel getValue() throws TemplateModelException {
                    return hash.get(((TemplateScalarModel) key).getAsString());
                }

            };
        }

    }

    /**
     * Same as {@link #wrapAsHashUnion(ObjectWrapper, List)}, but uses a varargs parameter instead of a {@link List}. 
     * 
     * @since 2.3.29
     */
    public static TemplateHashModel wrapAsHashUnion(ObjectWrapper objectWrapper, Object... hashLikeObjects)
            throws TemplateModelException {
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
     * somewhat overlaps with {@link Configuration#setSharedVariables(Map)}; check if that fits your use case better.
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
     * @throws TemplateModelException
     *             If wrapping an element of {@code hashLikeObjects} fails with {@link TemplateModelException}, or if
     *             wrapping an element results in a {@link TemplateModel} that's not a {@link TemplateHashModel}, or if
     *             the element was already a {@link TemplateModel} that isn't a {@link TemplateHashModel}.
     * 
     * @since 2.3.29
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static TemplateHashModel wrapAsHashUnion(ObjectWrapper objectWrapper, List<?> hashLikeObjects)
            throws TemplateModelException {
        NullArgumentException.check("hashLikeObjects", hashLikeObjects);
        
        List<TemplateHashModel> hashes = new ArrayList<>(hashLikeObjects.size());
        
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
                    throw new TemplateModelException(
                            "One of the objects of the hash union is not hash-like: "
                            + ClassUtil.getFTLTypeDescription(tm));
                }
            }
            
            hashes.add((TemplateHashModel) tm);
        }
        
        return  hashes.isEmpty() ? Constants.EMPTY_HASH
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
        public TemplateModel get(String key) throws TemplateModelException {
            for (int i = hashes.size() - 1; i >= 0; i--) {
                TemplateModel value = hashes.get(i).get(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            for (TemplateHashModel hash : hashes) {
                if (!hash.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        
    }
    
    private static class HashUnionModel extends AbstractHashUnionModel<TemplateHashModel> {
        HashUnionModel(List<? extends TemplateHashModel> hashes) {
            super(hashes);
        }
    }

    private static final class HashExUnionModel extends AbstractHashUnionModel<TemplateHashModelEx>
            implements TemplateHashModelEx {
        private CollectionAndSequence keys;
        private CollectionAndSequence values;

        private HashExUnionModel(List<? extends TemplateHashModelEx> hashes) {
            super(hashes);
        }
        
        @Override
        public int size() throws TemplateModelException {
            initKeys();
            return keys.size();
        }

        @Override
        public TemplateCollectionModel keys() throws TemplateModelException {
            initKeys();
            return keys;
        }

        @Override
        public TemplateCollectionModel values() throws TemplateModelException {
            initValues();
            return values;
        }

        private void initKeys() throws TemplateModelException {
            if (keys == null) {
                Set<String> keySet = new HashSet<>();
                SimpleSequence keySeq = new SimpleSequence(_ObjectWrappers.SAFE_OBJECT_WRAPPER);
                for (TemplateHashModelEx hash : hashes) {
                    addKeys(keySet, keySeq, hash);
                }
                keys = new CollectionAndSequence(keySeq);
            }
        }

        private static void addKeys(Set<String> keySet, SimpleSequence keySeq, TemplateHashModelEx hash)
                throws TemplateModelException {
            TemplateModelIterator it = hash.keys().iterator();
            while (it.hasNext()) {
                TemplateScalarModel tsm = (TemplateScalarModel) it.next();
                if (keySet.add(tsm.getAsString())) {
                    // The first occurrence of the key decides the index;
                    // this is consistent with the behavior of java.util.LinkedHashSet.
                    keySeq.add(tsm);
                }
            }
        }        

        private void initValues() throws TemplateModelException {
            if (values == null) {
                SimpleSequence seq = new SimpleSequence(size(), _ObjectWrappers.SAFE_OBJECT_WRAPPER);
                // Note: size() invokes initKeys() if needed.
            
                int ln = keys.size();
                for (int i  = 0; i < ln; i++) {
                    seq.add(get(((TemplateScalarModel) keys.get(i)).getAsString()));
                }
                values = new CollectionAndSequence(seq);
            }
        }
    }
    
}
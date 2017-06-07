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

package org.apache.freemarker.core.templateresolver.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.CacheStorageWithGetSize;

/**
 * Soft cache storage is a cache storage that uses {@link SoftReference} objects to hold the objects it was passed,
 * therefore allows the garbage collector to purge the cache when it determines that it wants to free up memory. This
 * class is thread-safe to the extent that its underlying map is. The parameterless constructor uses a thread-safe map
 * since 2.3.24 or Java 5.
 *
 * @see ExtendableBuilder#setTemplateCacheStorage(CacheStorage)
 */
public class SoftCacheStorage implements CacheStorage, CacheStorageWithGetSize {
    
    private final ReferenceQueue queue = new ReferenceQueue();
    private final ConcurrentMap map;
    
    /**
     * Creates an instance that uses a {@link ConcurrentMap} internally.
     */
    public SoftCacheStorage() {
        map = new ConcurrentHashMap();
    }
    
    @Override
    public Object get(Object key) {
        processQueue();
        Reference ref = (Reference) map.get(key);
        return ref == null ? null : ref.get();
    }

    @Override
    public void put(Object key, Object value) {
        processQueue();
        map.put(key, new SoftValueReference(key, value, queue));
    }

    @Override
    public void remove(Object key) {
        processQueue();
        map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
        processQueue();
    }
    
    /**
     * Returns a close approximation of the number of cache entries.
     */
    @Override
    public int getSize() {
        processQueue();
        return map.size();
    }

    private void processQueue() {
        for (; ; ) {
            SoftValueReference ref = (SoftValueReference) queue.poll();
            if (ref == null) {
                return;
            }
            Object key = ref.getKey();
            map.remove(key, ref);
        }
    }

    private static final class SoftValueReference extends SoftReference {
        private final Object key;

        SoftValueReference(Object key, Object value, ReferenceQueue queue) {
            super(value, queue);
            this.key = key;
        }

        Object getKey() {
            return key;
        }
    }
    
}
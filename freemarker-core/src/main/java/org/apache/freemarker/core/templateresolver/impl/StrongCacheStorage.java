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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.CacheStorageWithGetSize;

/**
 * Strong cache storage is a cache storage that simply wraps a {@link Map}. It holds a strong reference to all objects
 * it was passed, therefore prevents the cache from being purged during garbage collection. This class is always
 * thread-safe since 2.3.24, before that if we are running on Java 5 or later.
 *
 * @see Configuration#getTemplateCacheStorage()
 */
public class StrongCacheStorage implements CacheStorageWithGetSize {
    
    private final ConcurrentMap map = new ConcurrentHashMap();

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    @Override
    public void remove(Object key) {
        map.remove(key);
    }
    
    /**
     * Returns a close approximation of the number of cache entries.
     */
    @Override
    public int getSize() {
        return map.size();
    }
    
    @Override
    public void clear() {
        map.clear();
    }
}

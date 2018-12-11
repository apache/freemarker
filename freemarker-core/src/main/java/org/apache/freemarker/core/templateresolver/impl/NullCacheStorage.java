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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.CacheStorageWithGetSize;

/**
 * A cache storage that doesn't store anything. Use this if you
 * don't want caching.
 *
 * @see Configuration#getTemplateCacheStorage()
 */
public class NullCacheStorage implements CacheStorage, CacheStorageWithGetSize {
    
    /**
     */
    public static final NullCacheStorage INSTANCE = new NullCacheStorage();
    
    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        // do nothing
    }

    @Override
    public void remove(Object key) {
        // do nothing
    }
    
    @Override
    public void clear() {
        // do nothing
    }

    /**
     * Always returns 0.
     */
    @Override
    public int getSize() {
        return 0;
    }
    
}

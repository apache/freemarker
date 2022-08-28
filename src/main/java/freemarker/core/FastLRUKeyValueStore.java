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

package freemarker.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe {@link Map}-like object, with get/put-like operations, that implements a rough, but fast
 * least-recently-used (LRU) logic to remove old entries automatically, in order to keep the size under a specified
 * maximum. It will remember at least the last {@link #guaranteedRecentEntries} accessed entries. Removing entries is
 * only guaranteed when the size exceeds the double of {@link #guaranteedRecentEntries}. Actually, if the methods are
 * accessed from N threads concurrently, there's a chance to end up with N-1 more remembered entries, though in
 * practical applications this is very unlikely to happen. That's also precision given up for speed.
 *
 * @since 2.3.32
 */
class FastLRUKeyValueStore<K, V> {
    private final int guaranteedRecentEntries;

    private final ConcurrentHashMap<K, V> recentEntries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, V> olderEntries = new ConcurrentHashMap<>();

    /**
     * @param guaranteedRecentEntries
     *         The number of least recently accessed ("get", or "put") entries that we are guaranteed to remember. The
     *         actual size can grow bigger than that; see in class documentation.
     */
    public FastLRUKeyValueStore(int guaranteedRecentEntries) {
        this.guaranteedRecentEntries = guaranteedRecentEntries;
    }

    /**
     * @return If the value was already in the cache, then it's not replaced, and the old value is returned, otherwise
     * the new (argument) value is returned.
     */
    V putIfAbsentThenReturnStored(K key, V value) {
        if (recentEntries.size() >= guaranteedRecentEntries) {
            synchronized (this) {
                if (recentEntries.size() >= guaranteedRecentEntries) {
                    olderEntries.clear();
                    olderEntries.putAll(recentEntries);
                    recentEntries.clear();
                }
            }
        }

        V prevValue = recentEntries.putIfAbsent(key, value);
        return prevValue != null ? prevValue : value;
    }

    /**
     * Gets the entry with the given key, and ensures that it becomes a "recent" entry.
     *
     * @return {@code null} if there's no entry with the given key.
     */
    V get(K cacheKey) {
        V value = recentEntries.get(cacheKey);
        if (value != null) {
            return value;
        }

        value = olderEntries.remove(cacheKey);
        if (value == null) {
            return null;
        }

        return putIfAbsentThenReturnStored(cacheKey, value);
    }

    /**
     * Drops all entries.
     */
    void clear() {
        synchronized (this) {
            olderEntries.clear();
            recentEntries.clear();
        }
    }

    /**
     * Total number of entries stored at the moment. Can be inaccurate if the store concurrent modified, but the main
     * application is unit testing, where we can avoid that.
     */
    int size() {
        synchronized (this) {
            return recentEntries.size() + olderEntries.size();
        }
    }
}

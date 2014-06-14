/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.cache;

import java.util.Map;

import freemarker.core._ConcurrentMapFactory;

/**
 * Strong cache storage is a cache storage that simply wraps a {@link Map}.
 * It holds a strong reference to all objects it was passed, therefore prevents
 * the cache from being purged during garbage collection.
 * This class is thread-safe to the extent that its underlying map is. The 
 * default implementation uses a concurrent map on Java 5 and above, so it's
 * thread-safe in that case.
 *
 * @see freemarker.template.Configuration#setCacheStorage(CacheStorage)
 *
 * @author Attila Szegedi
 */
public class StrongCacheStorage implements ConcurrentCacheStorage
{
    private final Map map = _ConcurrentMapFactory.newMaybeConcurrentHashMap();

    /**
     * Returns true if the underlying Map is a {@code ConcurrentMap}.
     */
    public boolean isConcurrent() {
        return _ConcurrentMapFactory.isConcurrent(map);
    }
    
    public Object get(Object key) {
        return map.get(key);
    }

    public void put(Object key, Object value) {
        map.put(key, value);
    }

    public void remove(Object key) {
        map.remove(key);
    }
    
    public void clear() {
        map.clear();
    }
}

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

/**
 * Cache storage abstracts away the storage aspects of a cache - associating
 * an object with a key, retrieval and removal via the key. It is actually a
 * small subset of the {@link java.util.Map} interface. 
 * The implementations can be coded in a non-threadsafe manner as the natural
 * user of the cache storage, {@link TemplateCache} does the necessary
 * synchronization.
 *
 * @see freemarker.template.Configuration#setCacheStorage(CacheStorage)
 */
public interface CacheStorage
{
    public Object get(Object key);
    public void put(Object key, Object value);
    public void remove(Object key);
    public void clear();
}

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
 * A cache storage that has a {@code getSize()} method for returning the current number of cache entries.
 * 
 * @since 2.3.21
 */
public interface CacheStorageWithGetSize extends CacheStorage {
    
    /**
     * Returns the current number of cache entries. This is intended to be used for monitoring. Note that depending on
     * the implementation, the cost of this operation is not necessary trivial, although calling it a few times per
     * minute should not be a problem.
     */
    int getSize();

}

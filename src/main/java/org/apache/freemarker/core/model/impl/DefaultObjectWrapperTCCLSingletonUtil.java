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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.freemarker.core.util.FluentBuilder;

/**
 * Utility method for caching {@link DefaultObjectWrapper} (and subclasses) sigletons per Thread Context Class
 * Loader.
 */
// [FM3] Maybe generalize and publish this functionality
final class DefaultObjectWrapperTCCLSingletonUtil {

    private DefaultObjectWrapperTCCLSingletonUtil() {
        // Not meant to be instantiated
    }

    /**
     * Contains the common parts of the singleton management for {@link DefaultObjectWrapper} and {@link DefaultObjectWrapper}.
     *
     * @param dowConstructorInvoker Creates a <em>new</em> read-only object wrapper of the desired
     *     {@link DefaultObjectWrapper} subclass.
     */
    static <
            ObjectWrapperT extends DefaultObjectWrapper,
            BuilderT extends DefaultObjectWrapper.ExtendableBuilder<ObjectWrapperT, BuilderT>>
    ObjectWrapperT getSingleton(
            BuilderT builder,
            Map<ClassLoader, Map<BuilderT, WeakReference<ObjectWrapperT>>> instanceCache,
            ReferenceQueue<ObjectWrapperT> instanceCacheRefQue,
            _ConstructorInvoker<ObjectWrapperT, BuilderT> dowConstructorInvoker) {
        // DefaultObjectWrapper can't be cached across different Thread Context Class Loaders (TCCL), because the result of
        // a class name (String) to Class mappings depends on it, and the staticModels and enumModels need that.
        // (The ClassIntrospector doesn't have to consider the TCCL, as it only works with Class-es, not class
        // names.)
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        Reference<ObjectWrapperT> instanceRef;
        Map<BuilderT, WeakReference<ObjectWrapperT>> tcclScopedCache;
        synchronized (instanceCache) {
            tcclScopedCache = instanceCache.get(tccl);
            if (tcclScopedCache == null) {
                tcclScopedCache = new HashMap<>();
                instanceCache.put(tccl, tcclScopedCache);
                instanceRef = null;
            } else {
                instanceRef = tcclScopedCache.get(builder);
            }
        }

        ObjectWrapperT instance = instanceRef != null ? instanceRef.get() : null;
        if (instance != null) {  // cache hit
            return instance;
        }
        // cache miss

        builder = builder.cloneForCacheKey();  // prevent any aliasing issues
        instance = dowConstructorInvoker.invoke(builder);

        synchronized (instanceCache) {
            instanceRef = tcclScopedCache.get(builder);
            ObjectWrapperT concurrentInstance = instanceRef != null ? instanceRef.get() : null;
            if (concurrentInstance == null) {
                tcclScopedCache.put(builder, new WeakReference<>(instance, instanceCacheRefQue));
            } else {
                instance = concurrentInstance;
            }
        }

        removeClearedReferencesFromCache(instanceCache, instanceCacheRefQue);

        return instance;
    }

    private static <
            ObjectWrapperT extends DefaultObjectWrapper, BuilderT extends DefaultObjectWrapper.ExtendableBuilder>
    void removeClearedReferencesFromCache(
            Map<ClassLoader, Map<BuilderT, WeakReference<ObjectWrapperT>>> instanceCache,
            ReferenceQueue<ObjectWrapperT> instanceCacheRefQue) {
        Reference<? extends ObjectWrapperT> clearedRef;
        while ((clearedRef = instanceCacheRefQue.poll()) != null) {
            synchronized (instanceCache) {
                findClearedRef: for (Map<BuilderT, WeakReference<ObjectWrapperT>> tcclScopedCache : instanceCache.values()) {
                    for (Iterator<WeakReference<ObjectWrapperT>> it2 = tcclScopedCache.values().iterator(); it2.hasNext(); ) {
                        if (it2.next() == clearedRef) {
                            it2.remove();
                            break findClearedRef;
                        }
                    }
                }
            } // sync
        } // while poll
    }

    /**
     * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
     * Used when the builder delegates the product creation to something else (typically, an instance cache). Calling
     * {@link FluentBuilder#build()} would be infinite recursion in such cases.
     */
    public interface _ConstructorInvoker<ProductT, BuilderT> {

        ProductT invoke(BuilderT builder);
    }

}

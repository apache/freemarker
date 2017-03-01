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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * Gets/creates a {@link DefaultObjectWrapper} singleton instance that's already configured as specified in the properties of
 * this object; this is recommended over using the {@link DefaultObjectWrapper} constructors. The returned instance can't be
 * further configured (it's write protected).
 *
 * <p>The builder meant to be used as a drop-away object (not stored in a field), like in this example:
 * <pre>
 *    DefaultObjectWrapper dow = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21).build();
 * </pre>
 *
 * <p>Or, a more complex example:</p>
 * <pre>
 *    // Create the builder:
 *    DefaultObjectWrapperBuilder builder = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_21);
 *    // Set desired DefaultObjectWrapper configuration properties:
 *    builder.setUseModelCache(true);
 *    builder.setExposeFields(true);
 *
 *    // Get the singleton:
 *    DefaultObjectWrapper dow = builder.build();
 *    // You don't need the builder anymore.
 * </pre>
 *
 * <p>Despite that builders aren't meant to be used as long-lived objects (singletons), the builder is thread-safe after
 * you have stopped calling its setters and it was safely published (see JSR 133) to other threads. This can be useful
 * if you have to put the builder into an IoC container, rather than the singleton it produces.
 *
 * <p>The main benefit of using a builder instead of a {@link DefaultObjectWrapper} constructor is that this way the
 * internal object wrapping-related caches (most notably the class introspection cache) will come from a global,
 * JVM-level (more precisely, {@code freemarker-core.jar}-class-loader-level) cache. Also the
 * {@link DefaultObjectWrapper} singletons
 * themselves are stored in this global cache. Some of the wrapping-related caches are expensive to build and can take
 * significant amount of memory. Using builders, components that use FreeMarker will share {@link DefaultObjectWrapper}
 * instances and said caches even if they use separate FreeMarker {@link org.apache.freemarker.core.Configuration}-s. (Many Java libraries use
 * FreeMarker internally, so {@link org.apache.freemarker.core.Configuration} sharing is not an option.)
 *
 * <p>Note that the returned {@link DefaultObjectWrapper} instances are only weak-referenced from inside the builder mechanism,
 * so singletons are garbage collected when they go out of usage, just like non-singletons.
 *
 * <p>About the object wrapping-related caches:
 * <ul>
 *   <li><p>Class introspection cache: Stores information about classes that once had to be wrapped. The cache is
 *     stored in the static fields of certain FreeMarker classes. Thus, if you have two {@link DefaultObjectWrapper}
 *     instances, they might share the same class introspection cache. But if you have two
 *     {@code freemarker.jar}-s (typically, in two Web Application's {@code WEB-INF/lib} directories), those won't
 *     share their caches (as they don't share the same FreeMarker classes).
 *     Also, currently there's a separate cache for each permutation of the property values that influence class
 *     introspection: {@link DefaultObjectWrapperBuilder#setExposeFields(boolean) expose_fields} and
 *     {@link DefaultObjectWrapperBuilder#setExposureLevel(int) exposure_level}. So only {@link DefaultObjectWrapper} where those
 *     properties are the same may share class introspection caches among each other.
 *   </li>
 *   <li><p>Model caches: These are local to a {@link DefaultObjectWrapper}. {@link DefaultObjectWrapperBuilder} returns the same
 *     {@link DefaultObjectWrapper} instance for equivalent properties (unless the existing instance was garbage collected
 *     and thus a new one had to be created), hence these caches will be re-used too. {@link DefaultObjectWrapper} instances
 *     are cached in the static fields of FreeMarker too, but there's a separate cache for each
 *     Thread Context Class Loader, which in a servlet container practically means a separate cache for each Web
 *     Application (each servlet context). (This is like so because for resolving class names to classes FreeMarker
 *     uses the Thread Context Class Loader, so the result of the resolution can be different for different
 *     Thread Context Class Loaders.) The model caches are:
 *     <ul>
 *       <li><p>
 *         Static model caches: These are used by the hash returned by {@link DefaultObjectWrapper#getEnumModels()} and
 *         {@link DefaultObjectWrapper#getStaticModels()}, for caching {@link TemplateModel}-s for the static methods/fields
 *         and Java enums that were accessed through them. To use said hashes, you have to put them
 *         explicitly into the data-model or expose them to the template explicitly otherwise, so in most applications
 *         these caches aren't unused.
 *       </li>
 *       <li><p>
 *         Instance model cache: By default off (see {@link DefaultObjectWrapper#setUseModelCache(boolean)}). Caches the
 *         {@link TemplateModel}-s for all Java objects that were accessed from templates.
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Note that what this method documentation says about {@link DefaultObjectWrapper} also applies to
 * {@link DefaultObjectWrapperBuilder}.
 */
public class DefaultObjectWrapperBuilder extends DefaultObjectWrapperConfiguration {

    private final static Map<ClassLoader, Map<DefaultObjectWrapperConfiguration, WeakReference<DefaultObjectWrapper>>>
            INSTANCE_CACHE = new WeakHashMap<>();
    private final static ReferenceQueue<DefaultObjectWrapper> INSTANCE_CACHE_REF_QUEUE
            = new ReferenceQueue<>();
    
    /**
     * Creates a builder that creates a {@link DefaultObjectWrapper} with the given {@code incompatibleImprovements};
     * using at least 2.3.22 is highly recommended. See {@link DefaultObjectWrapper#DefaultObjectWrapper(Version)} for
     * more information about the impact of {@code incompatibleImprovements} values.
     */
    public DefaultObjectWrapperBuilder(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    /** For unit testing only */
    static void clearInstanceCache() {
        synchronized (INSTANCE_CACHE) {
            INSTANCE_CACHE.clear();
        }
    }
    
    /**
     * Returns a {@link DefaultObjectWrapper} instance that matches the settings of this builder. This will be possibly
     * a singleton that is also in use elsewhere. 
     */
    public DefaultObjectWrapper build() {
        return _ModelAPI.getDefaultObjectWrapperSubclassSingleton(
                this, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE, DefaultObjectWrapperFactory.INSTANCE);
    }

    /**
     * For unit testing only
     */
    static Map<ClassLoader, Map<DefaultObjectWrapperConfiguration, WeakReference<DefaultObjectWrapper>>>
            getInstanceCache() {
        return INSTANCE_CACHE;
    }

    private static class DefaultObjectWrapperFactory
        implements _ModelAPI._DefaultObjectWrapperSubclassFactory<DefaultObjectWrapper, DefaultObjectWrapperConfiguration> {
    
        private static final DefaultObjectWrapperFactory INSTANCE = new DefaultObjectWrapperFactory(); 
        
        @Override
        public DefaultObjectWrapper create(DefaultObjectWrapperConfiguration bwConf) {
            return new DefaultObjectWrapper(bwConf, true);
        }
    }

}

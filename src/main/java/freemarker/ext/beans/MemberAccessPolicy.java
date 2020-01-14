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

package freemarker.ext.beans;

import freemarker.core.Environment;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;

/**
 * Implement this to restrict what class members (methods, fields, constructors) are accessible from templates.
 * Note, however, that {@link BeansWrapper} and its subclasses doesn't discover all members on the first place, and the
 * {@link MemberAccessPolicy} just removes from that set of members, never adds to it. Practically speaking, it's the
 * last filter in the chain.
 *
 * <p>{@link MemberAccessPolicy}-s meant to be used inside {@link ObjectWrapper}-s, and their existence is transparent
 * for the rest of the system. The instance is usually set via
 * {@link BeansWrapperBuilder#setMemberAccessPolicy(MemberAccessPolicy)} (or if you use {@link DefaultObjectWrapper},
 * with {@link DefaultObjectWrapperBuilder#setMemberAccessPolicy(MemberAccessPolicy)}).
 *
 * <p>As {@link BeansWrapper}, and its subclasses like {@link DefaultObjectWrapper}, only discover public
 * members, it's pointless to whitelist non-public members. (Also, while public members declared in non-public classes
 * are discovered by {@link BeansWrapper}, Java reflection will not allow accessing those normally, so generally it's
 * not useful to whitelist those either.)
 *
 * <p>Note that if you add {@link TemplateModel}-s directly to the data-model, those are not wrapped by the
 * {@link ObjectWrapper} (from {@link Environment#getObjectWrapper()}), and so the {@link MemberAccessPolicy} won't
 * affect those.
 *
 * <p>The {@link MemberAccessPolicy} is only used during the class introspection phase (which discovers the members of a
 * type, and decides if, and how will they be exposed to templates), and the result of that is cached. So, the speed of
 * an {@link MemberAccessPolicy} implementation is usually not too important, as it won't play a role during template
 * execution.
 *
 * <p>Implementations must be thread-safe, and instances generally should be singletons on JVM level. FreeMarker
 * caches its class metadata in a global (static, JVM-scope) cache for shared use, and the {@link MemberAccessPolicy}
 * used is part of the cache key. Thus {@link MemberAccessPolicy} instances used at different places in the JVM
 * should be equal according to {@link Object#equals(Object)}, as far as they implement exactly the same policy. It's
 * not recommended to override {@link Object#equals(Object)}; use singletons and the default
 * {@link Object#equals(Object)} implementation if possible.
 *
 * @since 2.3.30
 */
public interface MemberAccessPolicy {
    /**
     * Returns the {@link ClassMemberAccessPolicy} that encapsulates the member access policy for a given class.
     * {@link ClassMemberAccessPolicy} implementations need not be thread-safe. Because class introspection results are
     * cached, and so this method is usually only called once for a given class, the {@link ClassMemberAccessPolicy}
     * instances shouldn't be cached by the implementation of this method.
     *
     * @param contextClass
     *      The exact class of object from which members will be get in the templates.
     */
    ClassMemberAccessPolicy forClass(Class<?> contextClass);

    /**
     * If this returns {@code true}, we won't invoke the probably more expensive lookup to figure out if
     * {@link Object#toString()} (including its overridden variants) is exposed for a given object. If this returns
     * {@code false}, then no such optimization is made. This method was introduced as {@link Object#toString()} is
     * called frequently, as it's used whenever an object is converted to string, like printed to the output, and it's
     * not even a reflection-based call (we just call {@link Object#toString()} in Java). So we try to avoid the
     * overhead of a more generic method call.
     */
    boolean isToStringAlwaysExposed();
}

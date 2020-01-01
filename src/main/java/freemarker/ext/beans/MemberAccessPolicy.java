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

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;

/**
 * Implement this to specify what class members are accessible from templates.
 *
 * <p>The instance is usually set via {@link BeansWrapperBuilder#setMemberAccessPolicy(MemberAccessPolicy)} (or if
 * you use {@link DefaultObjectWrapper}, with
 * {@link DefaultObjectWrapperBuilder#setMemberAccessPolicy(MemberAccessPolicy)}).
 *
 * <p>As {@link BeansWrapper}, and its subclasses like {@link DefaultObjectWrapper}, only discover public
 * members, it's pointless to whitelist non-public members. An {@link MemberAccessPolicy} is a filter applied to
 * the set of members that {@link BeansWrapper} intends to expose on the first place. (Also, while public members
 * declared in non-public classes are discovered by {@link BeansWrapper}, Java reflection will not allow accessing those
 * normally, so generally it's not useful to whitelist those either.)
 *
 * <p>Note that if you add {@link TemplateModel}-s directly to the data-model, those are not wrapped by the
 * {@link ObjectWrapper}, and so the {@link MemberAccessPolicy} won't affect those.
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
}

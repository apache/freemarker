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

/**
 * Implement this to specify what class members are accessible from templates. Implementations must be thread
 * safe, and instances should be generally singletons on JVM level. The last is because FreeMarker tries to cache
 * class introspectors in a global (static, JVM-scope) cache for reuse, and that's only possible if the
 * {@link MemberAccessPolicy} instances used at different places in the JVM are equal according to
 * {@link #equals(Object) (and the singleton object of course {@link #equals(Object)} with itself).
 */
public interface MemberAccessPolicy {
    /**
     * Returns the {@link ClassMemberAccessPolicy} that encapsulates the member access policy for a given class.
     */
    ClassMemberAccessPolicy forClass(Class<?> containingClass);
}
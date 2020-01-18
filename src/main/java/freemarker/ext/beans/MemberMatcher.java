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

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * For implementing a whitelist or blacklist of class members in {@link MemberAccessPolicy} implementations.
 * A {@link MemberMatcher} filters by name and/or signature, but not by by visibility, as
 * the visibility condition is orthogonal to the whitelist or blacklist content.
 *
 * @since 2.3.30
 */
abstract class MemberMatcher<M extends Member, S> {
    private final Map<S, Types> signaturesToUpperBoundTypes = new HashMap<>();

    private static class Types {
        private final Set<Class<?>> set = new HashSet<>();
        private boolean containsInterfaces;
    }

    /**
     * Returns the {@link Map} lookup key used to match the member.
     */
    protected abstract S toMemberSignature(M member);

    protected abstract boolean matchInUpperBoundTypeSubtypes();

    /**
     * Adds a member that this {@link MemberMatcher} will match.
     *
     * @param upperBoundType
     *          The type of the actual object that contains the member must {@code instanceof} this.
     * @param member
     *          The member that should match (when the upper bound class condition is also fulfilled). Only the name
     *          and/or signature of the member will be used for the condition, not the actual member object.
     */
    void addMatching(Class<?> upperBoundType, M member) {
        Class<?> declaringClass = member.getDeclaringClass();
        if (!declaringClass.isAssignableFrom(upperBoundType)) {
            throw new IllegalArgumentException("Upper bound class " + upperBoundType.getName() + " is not the same "
                    + "type or a subtype of the declaring type of member " + member + ".");
        }

        S memberSignature = toMemberSignature(member);
        Types upperBoundTypes = signaturesToUpperBoundTypes.get(memberSignature);
        if (upperBoundTypes == null) {
            upperBoundTypes = new Types();
            signaturesToUpperBoundTypes.put(memberSignature, upperBoundTypes);
        }
        upperBoundTypes.set.add(upperBoundType);
        if (upperBoundType.isInterface()) {
            upperBoundTypes.containsInterfaces = true;
        }
    }

    /**
     * Returns if the given member, if it's referred through the given class, is matched by this {@link MemberMatcher}.
     *
     * @param contextClass The actual class through which we access the member
     * @param member The member that we intend to access
     *
     * @return If there was match in this {@link MemberMatcher}.
     */
    boolean matches(Class<?> contextClass, M member) {
        S memberSignature = toMemberSignature(member);
        Types upperBoundTypes = signaturesToUpperBoundTypes.get(memberSignature);

        return upperBoundTypes != null
                && (matchInUpperBoundTypeSubtypes()
                        ? containsTypeOrSuperType(upperBoundTypes, contextClass)
                        : containsExactType(upperBoundTypes, contextClass));
    }

    private static boolean containsExactType(Types types, Class<?> c) {
        if (c == null) {
            return false;
        }
        return types.set.contains(c);
    }

    private static boolean containsTypeOrSuperType(Types types, Class<?> c) {
        if (c == null) {
            return false;
        }

        if (types.set.contains(c)) {
            return true;
        }
        if (containsTypeOrSuperType(types, c.getSuperclass())) {
            return true;
        }
        if (types.containsInterfaces) {
            for (Class<?> anInterface : c.getInterfaces()) {
                if (containsTypeOrSuperType(types, anInterface)) {
                    return true;
                }
            }
        }
        return false;
    }
}

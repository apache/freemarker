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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Used as a key in a {@link Map} or {@link Set} of methods or constructors.
 *
 * @since 2.3.30
 */
final class ExecutableMemberSignature {
    private final String name;
    private final Class<?>[] args;

    ExecutableMemberSignature(String name, Class<?>[] args) {
        this.name = name;
        this.args = args;
    }

    /**
     * Uses the method name, and the parameter types.
     */
    ExecutableMemberSignature(Method method) {
        this(method.getName(), method.getParameterTypes());
    }

    /**
     * Doesn't use the constructor name, only the parameter types.
     */
    ExecutableMemberSignature(Constructor<?> constructor) {
        this("<init>", constructor.getParameterTypes());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ExecutableMemberSignature) {
            ExecutableMemberSignature ms = (ExecutableMemberSignature) o;
            return ms.name.equals(name) && Arrays.equals(args, ms.args);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + args.length * 31;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + name + ", " + Arrays.toString(args) + ")";
    }
}

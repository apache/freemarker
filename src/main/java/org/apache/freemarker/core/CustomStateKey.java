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

package org.apache.freemarker.core;

/**
 * Used with {@link CustomStateScope}-s; each subclass must have exactly one instance, which should be stored in
 * a static final field. So the usual usage is like this:
 *
 * <pre>
 *     static final CustomStateKey MY_STATE = new CustomStateKey() {
 *         @Override
 *         protected Object create() {
 *             return new ...;
 *         }
 *     };
 * </pre>
 */
public abstract class CustomStateKey<T> {

    /**
     * This will be invoked when the state for this {@link CustomStateKey} is get via {@link
     * CustomStateScope#getCustomState(CustomStateKey)}, but it doesn't yet exists in the given scope. Then the created
     * object will be stored in the scope and then it's returned. Must not return {@code null}.
     */
    protected abstract T create();

    /**
     * Does identity comparison (like operator {@code ==}).
     */
    @Override
    final public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Returns {@link Object#hashCode()}.
     */
    @Override
    final public int hashCode() {
        return super.hashCode();
    }

}

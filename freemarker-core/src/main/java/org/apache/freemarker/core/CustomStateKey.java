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
 * Used with {@link CustomStateScope}-s; each subclass must have exactly one instance, which should be stored in a
 * static final field. So the usual usage is like this:
 *
 * <pre>
 * static final CustomStateKey&lt;FooState&gt; FOO_STATE_KEY = new CustomStateKey&lt;&gt;() {
 *     &#x40;Override
 *     protected Object create() {
 *         return new FooState();
 *     }
 * };
 * </pre>
 * 
 * <p>
 * And then later somewhere (if it's {@link Environment} scoped):
 * 
 * <pre>
 * FooState fooState = env.getCustomState(FOO_STATE_KEY);
 * // Use FooState API from here on: 
 * int x = fooState.getX();
 * int y = fooState.getY();
 * fooState.setX(0)
 * </pre>
 * 
 * <p>
 * Note that above we haven't defined multiple keys for each value we need ({@code x} and {@code y}), instead we
 * have defined a container class ({@code FooState}) that contains all the values we need, and we just get that
 * single container object, and then use its API ({@code getX()}, {@code setX(int)}, etc. above). 
 * 
 * @param <T>
 *            The type of the object returned for this key (initially created by {@link #create()}).
 *            
 */
public abstract class CustomStateKey<T> {

    /**
     * This will be invoked when the state for this {@link CustomStateKey} is get via {@link
     * CustomStateScope#getCustomState(CustomStateKey)} for the first time in a scope. Then the created
     * object will be stored in the scope and then it's returned. Must not return {@code null}.
     * 
     * <p>In case you wonder how to initialize the contents of the newly created object, because you should use some
     * values that aren't available inside the method, you shouldn't do such initialization in this method. Rather,
     * the code that is responsible for such initialization should call
     * {@link CustomStateScope#getCustomState(CustomStateKey)} and set up the returned object through its API.
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

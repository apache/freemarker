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
 * An object that's a scope that can store custom state objects.
 */
public interface CustomStateScope {

    /**
     * Gets the custom state belonging to the key, automatically creating it if it doesn't yet exists in the scope.
     * If the scope is {@link Configuration} or {@link Template}, then this method is thread safe. If the scope is
     * {@link Environment}, then this method is not thread safe ({@link Environment} is not thread safe either).
     * 
     * <p>There's no {@code setCustomState} method; the code that wants to initialize the state object should call
     * {@link CustomStateScope#getCustomState(CustomStateKey)} to receive the instance, then set up the returned object
     * through its API. 
     */
    <T> T getCustomState(CustomStateKey<T> customStateKey);

}

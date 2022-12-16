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
package freemarker.core;

import java.lang.reflect.Method;

/**
 * Used internally only, might change without notice!
 * Used for accessing functionality that's only present in Java 8 or later.
 */
// Compile this against Java 8
@SuppressWarnings("Since15") // For IntelliJ inspection
public class _Java8Impl implements _Java8 {
    
    public static final _Java8 INSTANCE = new _Java8Impl();

    private _Java8Impl() {
        // Not meant to be instantiated
    }    

    @Override
    public boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

}

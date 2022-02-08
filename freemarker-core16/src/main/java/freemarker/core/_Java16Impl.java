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
import java.lang.reflect.RecordComponent;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Used internally only, might change without notice!
 * Used for accessing functionality that's only present in Java 16 or later.
 */
// Compile this against Java 16
@SuppressWarnings("Since15") // For IntelliJ inspection
public class _Java16Impl implements _Java16 {

    public static final _Java16 INSTANCE = new _Java16Impl();

    private _Java16Impl() {
        // Not meant to be instantiated
    }

    @Override
    public boolean isRecord(Class<?> cl) {
        return cl.isRecord();
    }

    @Override
    public Set<Method> getComponentAccessors(Class<?> recordType)  {
        RecordComponent[] recordComponents = recordType.getRecordComponents();
        if (recordComponents == null) {
            throw new IllegalArgumentException("Argument must be a record type");
        }
        IdentityHashMap<Method, Void> methods = new IdentityHashMap<>(recordComponents.length);
        for (RecordComponent recordComponent : recordComponents) {
            methods.put(recordComponent.getAccessor(), null);
        }
        return methods.keySet();
    }
}

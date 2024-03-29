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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * Used instead of {@link PropertyDescriptor}, because the methods of that are synchronized.
 * Also, we use this for "fake" Java Beans properties too (see {@link BeansWrapper.MethodAppearanceDecision}).
 * 
 * @since 2.3.27
 */
final class FastPropertyDescriptor {
    private final Method readMethod;
    private final Method indexedReadMethod;
    private final boolean methodInsteadOfPropertyValueBeforeCall;

    public FastPropertyDescriptor(
            Method readMethod, Method indexedReadMethod, boolean methodInsteadOfPropertyValueBeforeCall) {
        this.readMethod = readMethod;
        this.indexedReadMethod = indexedReadMethod;
        this.methodInsteadOfPropertyValueBeforeCall = methodInsteadOfPropertyValueBeforeCall;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public Method getIndexedReadMethod() {
        return indexedReadMethod;
    }

    /**
     * If this is true, and the property value is referred directly before it's called in a template, then
     * instead of the property value, the value should be the read method.
     *
     * @since 2.3.33
     */
    public boolean isMethodInsteadOfPropertyValueBeforeCall() {
        return methodInsteadOfPropertyValueBeforeCall;
    }
}

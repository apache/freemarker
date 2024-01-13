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

/**
 * How to show 0 argument non-void public methods to templates, which are not standard Java Beans read methods.
 * Used in {@link BeansWrapper}, and therefore in {@link DefaultObjectWrapper}.
 * This policy doesn't apply to methods that Java Beans introspector discovers as a property read method (which
 * typically look like {@code getSomething()}, or {@code isSomething()}). It's only applicable to methods like
 * {@code something()}, including the component read methods of Java records.
 *
 * @see BeansWrapperConfiguration#setNonRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy)
 * @see BeansWrapperConfiguration#setRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy)
 * @see BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)
 *
 * @since 2.3.33
 */
public enum ZeroArgumentNonVoidMethodPolicy {

    /**
     * Both {@code obj.m}, and {@code obj.m()} gives back the value that the {@code m} Java method returns, and it's
     * not possible to get the method itself.
     *
     * <p>This is a parse-time trick that only works when the result of the dot operator is called immediately in a
     * template (and therefore the dot operator knows that you will call the result of it). The practical reason for
     * this feature is that the convention of having {@code SomeType something()} instead of
     * {@code SomeType getSomething()} spreads in the Java ecosystem (and is a standard in some other JVM languages),
     * and thus we can't tell anymore if {@code SomeType something()} just reads a value, and hence should be accessed
     * like {@code obj.something}, or it's more like an operation that has side effect, and therefore should be
     * accessed like {@code obj.something()}. So with allowing both, the template author is free to decide which is
     * the more fitting. Also, for accessing Java records components, the proper way is {@code obj.something}, but
     * before FreeMarker was aware of records (and hence that those methods are like property read methods), the
     * only way that worked was {@code obj.something()}, so to be more backward compatible, we have to support both.
     */
    BOTH_PROPERTY_AND_METHOD,

    /**
     * Only {@code obj.m()} gives back the value, {@code obj.m} just gives the method itself.
     */
    METHOD_ONLY,

    /**
     * {@code obj.m} in gives back the value, and the method itself can't be get.
     */
    PROPERTY_ONLY
}

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
import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;

/**
 * How to show 0 argument non-void public methods to templates.
 * Used in {@link BeansWrapper}, and therefore in {@link DefaultObjectWrapper}.
 * This policy doesn't apply to methods that Java Beans introspector discovers as a property read method (which
 * typically look like {@code getSomething()}, or {@code isSomething()}). It's only applicable to methods like
 * {@code something()}, including the component read methods of Java records.
 *
 * @see BeansWrapperConfiguration#setDefaultZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy)
 * @see BeansWrapperConfiguration#setRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy)
 * @see BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)
 * @see MethodCallAwareTemplateHashModel
 *
 * @since 2.3.33
 */
public enum ZeroArgumentNonVoidMethodPolicy {

    /**
     * Both {@code obj.m}, and {@code obj.m()} gives back the value that the {@code m} Java method returns, and it's
     * not possible to get the method itself. But, it's not applicable for Java Bean property read methods
     * (like {@code int getX()}), which remain just simple methods (because the Java Bean property is visible regardless
     * of the {@link ZeroArgumentNonVoidMethodPolicy}, like {@code obj.x}, for which the Java Bean property read method
     * is {@link int getX()}).
     *
     * <p>This is a parse-time trick that only works when the result of the dot operator (like {@code obj.m}), or of the
     * square bracket key operator (like {@code obj["m"]}) is called immediately in a template (like {@code obj.m()}, or
     * like {@code obj["m"]()}), and therefore the dot, or square bracket key operator knows that you will call the
     * result of it. In such case, if the {@linkplain ObjectWrapper wrapped} {@code obj} implements
     * {@link MethodCallAwareTemplateHashModel}, the operator will call
     * {@link MethodCallAwareTemplateHashModel#getBeforeMethodCall(String, int)} instead of
     * {@link TemplateHashModel#get(String)}. This also lets wrappers resolve overloads when the zero-argument method
     * shares its name with methods that take arguments.
     *
     * <p>The practical reason for this feature is that the convention of using {@code SomeType something()} instead
     * of {@code SomeType getSomething()} spreads in the Java ecosystem (and is a standard in some other JVM languages),
     * and thus we can't tell anymore if {@code SomeType something()} just reads a value, and hence should be accessed
     * like {@code obj.something}, or it's more like an operation that has side effect, and therefore should be
     * accessed like {@code obj.something()}. So with allowing both, the template author is free to decide which is
     * the more fitting. Also, for accessing Java records components, the proper way is {@code obj.something}, but
     * before FreeMarker was aware of records (and hence that those methods are like property read methods), the
     * only way that worked was {@code obj.something()}, so to be more backward compatible, we have to support both.
     */
    BOTH_METHOD_AND_PROPERTY_UNLESS_BEAN_PROPERTY_READ_METHOD,

    /**
     * Only {@code obj.m()} gives back the value in a template, {@code obj.m} in a template just gives the method itself.
     */
    METHOD_ONLY,

    /**
     * {@code obj.m} in a template gives back the value, and you can't get the method itself. But, it's not applicable
     * for Java Bean property read methods, which will remain normals methods, just like with
     * {@link #BOTH_METHOD_AND_PROPERTY_UNLESS_BEAN_PROPERTY_READ_METHOD}.
     */
    PROPERTY_ONLY_UNLESS_BEAN_PROPERTY_READ_METHOD
}

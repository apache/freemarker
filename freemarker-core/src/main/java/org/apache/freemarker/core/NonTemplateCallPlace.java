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

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CommonSupplier;

/**
 * {@link CallPlace} used when a {@link TemplateDirectiveModel} is called from outside a template (such as from user
 * Java code).
 */
public final class NonTemplateCallPlace implements CallPlace {

    public static final NonTemplateCallPlace INSTANCE = new NonTemplateCallPlace();

    private final int firstTargetJavaParameterTypeIndex;
    private final Class<?>[] targetJavaParameterTypes;

    private NonTemplateCallPlace() {
        this(-1, null);
    }

    /**
     * @param firstTargetJavaParameterTypeIndex
     *         See {@link CallPlace#getFirstTargetJavaParameterTypeIndex()}
     * @param targetJavaParameterTypes
     *         The target Java types of the arguments of the invocation, starting with the target Java type of the
     *         argument at index {@code firstTargetJavaParameterTypeIndex}, and usually ending with the target type of
     *         the last argument that has a non-{@code null} target type (although having {@code null}-s at the end is
     *         legal too). For example, using Java-like call syntax, if the call is like
     *         {@code m(a, b, (Foo) c, d, (Bar) e, f)}, then {@code firstTargetJavaParameterTypeIndex} is 2,
     *         and the array will be {@code new Class<?>[] { } [ Foo.class, null, Bar.class ]}.
     */
    public NonTemplateCallPlace(int firstTargetJavaParameterTypeIndex, Class<?>[] targetJavaParameterTypes) {
        this.firstTargetJavaParameterTypeIndex = firstTargetJavaParameterTypeIndex;
        this.targetJavaParameterTypes = targetJavaParameterTypes;
    }

    /**
     * Always returns {@code false}.
     */
    @Override
    public boolean hasNestedContent() {
        return false;
    }

    /**
     * Always returns {@code 0}.
     */
    @Override
    public int getNestedContentParameterCount() {
        return 0;
    }

    /**
     * Does nothing.
     */
    @Override
    public void executeNestedContent(TemplateModel[] nestedContentArgs, Writer out, Environment env)
            throws TemplateException, IOException {
        // Do nothing
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public Template getTemplate() {
        return null;
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public int getBeginColumn() {
        return -1;
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public int getBeginLine() {
        return -1;
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public int getEndColumn() {
        return -1;
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public int getEndLine() {
        return -1;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    public Object getOrCreateCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException {
        throw new UnsupportedOperationException("Non-template call place doesn't support custom data storage");
    }

    /**
     * Always returns {@code false}.
     */
    @Override
    public boolean isCustomDataSupported() {
        return false;
    }

    /**
     * Always returns {@code false}.
     */
    @Override
    public boolean isNestedOutputCacheable() {
        return false;
    }

    /**
     * Always returns {@code -1}.
     */
    @Override
    public int getFirstTargetJavaParameterTypeIndex() {
        return firstTargetJavaParameterTypeIndex;
    }

    /**
     * Always returns {@code null}.
     */
    @Override
    public Class<?> getTargetJavaParameterType(int argIndex) {
        int idx = argIndex - firstTargetJavaParameterTypeIndex;
        return idx >= 0 || idx < targetJavaParameterTypes.length ? targetJavaParameterTypes[idx] : null;
    }
}

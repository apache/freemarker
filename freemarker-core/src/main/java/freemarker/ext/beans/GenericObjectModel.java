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

import java.util.Collection;
import java.util.Map;

import freemarker.ext.util.ModelFactory;
import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * This is used for wrapping objects that has no special treatment (unlike {@link Map}-s, {@link Collection}-s,
 * {@link Number}-s, {@link Boolean}-s, and some more, which have), hence they are just "generic" Java
 * objects. Users usually just want to call the public Java methods on such objects.
 * These objects can also be used as string values in templates, and that value is provided by
 * the {@link Object#toString()} method of the wrapped object.
 *
 * <p>This extends {@link StringModel} for backward compatibility, as now {@link BeansWrapper} returns instances of
 * {@link GenericObjectModel} instead of {@link StringModel}-s, but user code may have {@code insteanceof StringModel},
 * or casing to {@link StringModel}. {@link StringModel} served the same purpose as this class, but didn't implement
 * {@link MethodCallAwareTemplateHashModel}.
 *
 * @since 2.3.33
 */
public class GenericObjectModel extends StringModel implements MethodCallAwareTemplateHashModel {
    static final ModelFactory FACTORY = (object, wrapper) -> new GenericObjectModel(object, (BeansWrapper) wrapper);

    /**
     * Creates a new model that wraps the specified object with BeanModel + scalar functionality.
     *
     * @param object
     *         the object to wrap into a model.
     * @param wrapper
     *         the {@link BeansWrapper} associated with this model. Every model has to have an associated
     *         {@link BeansWrapper} instance. The model gains many attributes from its wrapper, including the caching
     *         behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public GenericObjectModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper);
    }

    // Made this final, to ensure that users override get(key, boolean) instead.
    @Override
    public final TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    @Override
    public TemplateModel getBeforeMethodCall(String key) throws TemplateModelException,
            ShouldNotBeGetAsMethodException {
        return super.getBeforeMethodCall(key);
    }
}

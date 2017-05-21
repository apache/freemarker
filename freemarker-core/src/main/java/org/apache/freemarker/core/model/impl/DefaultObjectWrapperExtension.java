/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;

/**
 * See the {@link DefaultObjectWrapper.ExtendableBuilder#getExtensions() extensions} setting of
 * {@link DefaultObjectWrapper}.
 */
public abstract class DefaultObjectWrapperExtension {

    /**
     * Specifies when {@link DefaultObjectWrapperExtension#wrap(Object)} is invoked inside
     * {@link DefaultObjectWrapper#wrap(Object)}.
     */
    public DefaultObjectWrapperExtensionPhase getPhase() {
        return DefaultObjectWrapperExtensionPhase.AFTER_WRAP_SPECIAL_OBJECT;
    }

    /**
     * @param obj
     *         The object to wrap; never {@code null} or a {@link TemplateModel} or a {@link TemplateModelAdapter}.
     *
     * @return {@code null} if this {@link DefaultObjectWrapperExtension} doesn't handle this object the wrapped object
     * otherwise.
     */
    public abstract TemplateModel wrap(Object obj);

}

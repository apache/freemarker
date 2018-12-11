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

package org.apache.freemarker.spring.model;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * Abstract delegating <code>TemplateHashModel</code>.
 * <p>
 * This can be useful when providing "spring" or "form" model on demand because this creates the delegated model
 * in lazy loading by default.
 */
abstract public class AbstractDelegatingTemplateHashModel implements TemplateHashModel {

    /**
     * Delegated <code>TemplateHashModel</code> instance.
     */
    private TemplateHashModel delegated;

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return getDelegatedTemplateHashModel().get(key);
    }

    /**
     * If the internal delegated <code>TemplateHashModel</code> instance is null, create one by invoking
     * {@link #createDelegatedTemplateHashModel()} and return it. Once created, return the same the internal
     * delegated <code>TemplateHashModel</code> instance afterward.
     * @return the internal delegated <code>TemplateHashModel</code> instance
     * @throws TemplateException if TemplateException occurs
     */
    protected TemplateHashModel getDelegatedTemplateHashModel() throws TemplateException {
        if (delegated == null) {
            delegated = createDelegatedTemplateHashModel();
        }

        return delegated;
    }

    /**
     * Create an internal delegated <code>TemplateHashModel</code> instance.
     * @return internal delegated <code>TemplateHashModel</code> instance
     * @throws TemplateException if TemplateException occurs
     */
    abstract protected TemplateHashModel createDelegatedTemplateHashModel() throws TemplateException;
}

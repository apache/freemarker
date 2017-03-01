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

package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.model.TemplateScalarModel;

/**
 * Subclass of {@link BeanModel} that exposes the return value of the {@link
 * java.lang.Object#toString()} method through the {@link TemplateScalarModel}
 * interface.
 */
// [FM3] Treating all beans as FTL strings was certainly a bad idea in FM2.
public class BeanAndStringModel extends BeanModel implements TemplateScalarModel {

    /**
     * Creates a new model that wraps the specified object with BeanModel + scalar
     * functionality.
     * @param object the object to wrap into a model.
     * @param wrapper the {@link DefaultObjectWrapper} associated with this model.
     * Every model has to have an associated {@link DefaultObjectWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public BeanAndStringModel(Object object, DefaultObjectWrapper wrapper) {
        super(object, wrapper);
    }

    /**
     * Returns the result of calling {@link Object#toString()} on the wrapped
     * object.
     */
    @Override
    public String getAsString() {
        return object.toString();
    }
}

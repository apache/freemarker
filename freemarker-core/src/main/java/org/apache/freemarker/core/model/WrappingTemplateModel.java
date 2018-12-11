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

package org.apache.freemarker.core.model;

import org.apache.freemarker.core.util._NullArgumentException;

/**
 * Convenience base-class for containers that wrap their contained arbitrary Java objects into {@link TemplateModel}
 * instances.
 */
abstract public class WrappingTemplateModel {

    private final ObjectWrapper objectWrapper;

    /**
     * Protected constructor that creates a new wrapping template model using the specified object wrapper.
     * 
     * @param objectWrapper the wrapper to use. Passing {@code null} to it
     *     is allowed but deprecated. Not {@code null}.
     */
    protected WrappingTemplateModel(ObjectWrapper objectWrapper) {
        _NullArgumentException.check("objectWrapper", objectWrapper);
        this.objectWrapper = objectWrapper;
    }
    
    /**
     * Returns the object wrapper instance used by this wrapping template model.
     */
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    /**
     * Wraps the passed object into a template model using this object's object
     * wrapper.
     * @param obj the object to wrap
     * @return the template model that wraps the object
     * @throws ObjectWrappingException if the wrapper does not know how to
     * wrap the passed object.
     */
    protected final TemplateModel wrap(Object obj) throws ObjectWrappingException {
            return objectWrapper.wrap(obj);
    }
    
}

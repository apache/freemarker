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

/**
 * <b>Experimental - subject to change:</b> Implemented by {@link ObjectWrapper}-s to help {@link TemplateModel}-s to
 * implement the {@code someValue?api} operation.
 * 
 * <p>
 * <b>Experimental status warning:</b> This interface is subject to change on non-backward compatible ways, hence, it
 * shouldn't be implemented outside FreeMarker yet.
 */
public interface ObjectWrapperWithAPISupport extends ObjectWrapper {

    /**
     * Wraps an object to a {@link TemplateModel} that exposes the object's "native" (usually, Java) API.
     * 
     * @param obj
     *            The object for which the API model has to be returned. Shouldn't be {@code null}.
     * 
     * @return The {@link TemplateModel} through which the API of the object can be accessed. Can't be {@code null}.
     */
    TemplateHashModel wrapAsAPI(Object obj) throws ObjectWrappingException;

}

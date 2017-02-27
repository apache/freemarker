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

import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;

/**
 * Maps Java objects to the type-system of FreeMarker Template Language (see the {@link TemplateModel}
 * interfaces). Thus this is what decides what parts of the Java objects will be accessible in the templates and how.
 * 
 * <p>For example, with a {@link DefaultObjectWrapper} both the items of {@link Map} and the JavaBean properties (the getters)
 * of an object are accessible in template uniformly with the {@code myObject.foo} syntax, where "foo" is the map key or
 * the property name. This is because both kind of object is wrapped by {@link DefaultObjectWrapper} into a
 * {@link TemplateHashModel} implementation that will call {@link Map#get(Object)} or the getter method, transparently
 * to the template language.
 * 
 * @see Configuration#setObjectWrapper(ObjectWrapper)
 */
public interface ObjectWrapper {
    
    /**
     * Makes a {@link TemplateModel} out of a non-{@link TemplateModel} object, usually by "wrapping" it into a
     * {@link TemplateModel} implementation that delegates to the original object.
     * 
     * @param obj The object to wrap into a {@link TemplateModel}. If it already implements {@link TemplateModel},
     *      it should just return the object as is. If it's {@code null}, the method should return {@code null}
     *      (however, {@link DefaultObjectWrapper}, has a legacy option for returning a null model object instead, but it's not
     *      a good idea).
     * 
     * @return a {@link TemplateModel} wrapper of the object passed in. To support un-wrapping, you may consider the
     *     return value to implement {@link WrapperTemplateModel} and {@link AdapterTemplateModel}.  
     *     The default expectation is that the {@link TemplateModel} isn't less thread safe than the wrapped object.
     *     If the {@link ObjectWrapper} returns less thread safe objects, that should be clearly documented, as it
     *     restricts how it can be used, like, then it can't be used to wrap "shared variables"
     *     ({@link Configuration#setSharedVaribles(Map)}).
     */
    TemplateModel wrap(Object obj) throws TemplateModelException;
    
}

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

import org.apache.freemarker.core.util._ClassUtils;

/**
 * Used by built-ins and other template language features that get a class
 * based on a string. This can be handy both for implementing security
 * restrictions and for working around local class-loader issues. 
 * 
 * The implementation should be thread-safe, unless an
 * instance is always only used in a single {@link Environment} object.
 * 
 * @see MutableProcessingConfiguration#setNewBuiltinClassResolver(TemplateClassResolver)
 */
public interface TemplateClassResolver {
    
    /**
     * Simply calls {@link _ClassUtils#forName(String)}.
     */
    TemplateClassResolver UNRESTRICTED = new TemplateClassResolver() {

        @Override
        public Class resolve(String className, Environment env, Template template)
        throws TemplateException {
            try {
                return _ClassUtils.forName(className);
            } catch (ClassNotFoundException e) {
                throw new TemplateException(e, env);
            }
        }
        
    };
    
    /**
     * Doesn't allow resolving any classes.
     */
    TemplateClassResolver ALLOW_NOTHING =  new TemplateClassResolver() {

        @Override
        public Class resolve(String className, Environment env, Template template)
        throws TemplateException {
            throw MessageUtils.newInstantiatingClassNotAllowedException(className, env);
        }
        
    };

    /**
     * Gets a {@link Class} based on the class name.
     * 
     * @param className the full-qualified class name
     * @param env the environment in which the template executes
     * @param template the template where the operation that require the
     *        class resolution resides in. This is <code>null</code> if the
     *        call doesn't come from a template.
     *        
     * @throws TemplateException if the class can't be found or shouldn't be
     *   accessed from a template for security reasons.
     */
    Class resolve(String className, Environment env, Template template) throws TemplateException;
    
}

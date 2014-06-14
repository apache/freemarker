/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.Execute;
import freemarker.template.utility.ObjectConstructor;

/**
 * Used by built-ins and other template language features that get a class
 * based on a string. This can be handy both for implementing security
 * restrictions and for working around local class-loader issues. 
 * 
 * The implementation should be thread-safe, unless an
 * instance is always only used in a single {@link Environment} object.
 * 
 * @see Configurable#setNewBuiltinClassResolver(TemplateClassResolver)
 * 
 * @since 2.3.17
 */
public interface TemplateClassResolver {
    
    /**
     * Simply calls {@link ClassUtil#forName(String)}.
     */
    TemplateClassResolver UNRESTRICTED_RESOLVER = new TemplateClassResolver() {

        public Class resolve(String className, Environment env, Template template)
        throws TemplateException {
            try {
                return ClassUtil.forName(className);
            } catch (ClassNotFoundException e) {
                throw new _MiscTemplateException(e, env);
            }
        }
        
    };
    
    /**
     * Same as {@link #UNRESTRICTED_RESOLVER}, except that it doesn't allow
     * resolving {@link ObjectConstructor} and {@link Execute} and {@code freemarker.template.utility.JythonRuntime}.
     */
    TemplateClassResolver SAFER_RESOLVER =  new TemplateClassResolver() {

        public Class resolve(String className, Environment env, Template template)
        throws TemplateException {
            if (className.equals(ObjectConstructor.class.getName())
                    || className.equals(Execute.class.getName())
                    || className.equals("freemarker.template.utility.JythonRuntime")) {
                throw MessageUtil.newInstantiatingClassNotAllowedException(className, env);
            }
            try {
                return ClassUtil.forName(className);
            } catch (ClassNotFoundException e) {
                throw new _MiscTemplateException(e, env);
            }
        }
        
    };
    
    /**
     * Doesn't allow resolving any classes.
     */
    TemplateClassResolver ALLOWS_NOTHING_RESOLVER =  new TemplateClassResolver() {

        public Class resolve(String className, Environment env, Template template)
        throws TemplateException {
            throw MessageUtil.newInstantiatingClassNotAllowedException(className, env);
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

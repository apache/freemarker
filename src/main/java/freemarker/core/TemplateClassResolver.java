/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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

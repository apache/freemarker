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

import java.util.List;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A built-in that allows us to instantiate an instance of a java class.
 * Usage is something like: <tt>&lt;#assign foobar = "foo.bar.MyClass"?new()></tt>;
 */
class NewBI extends BuiltIn
{
    
    static final Class BEAN_MODEL_CLASS = freemarker.ext.beans.BeanModel.class;
    static Class JYTHON_MODEL_CLASS;
    static {
        try {
            JYTHON_MODEL_CLASS = Class.forName("freemarker.ext.jython.JythonModel");
        } catch (Throwable e) {
            JYTHON_MODEL_CLASS = null;
        }
    }
    
    TemplateModel _eval(Environment env)
            throws TemplateException 
    {
        return new ConstructorFunction(target.evalAndCoerceToString(env), env, target.getTemplate());
    }

    class ConstructorFunction implements TemplateMethodModelEx {

        private final Class cl;
        private final Environment env;
        
        public ConstructorFunction(String classname, Environment env, Template template) throws TemplateException {
            this.env = env;
            cl = env.getNewBuiltinClassResolver().resolve(classname, env, template);
            if (!TemplateModel.class.isAssignableFrom(cl)) {
                throw new _MiscTemplateException(NewBI.this, env, new Object[] {
                        "Class ", cl.getName(), " does not implement freemarker.template.TemplateModel" });
            }
            if (BEAN_MODEL_CLASS.isAssignableFrom(cl)) {
                throw new _MiscTemplateException(NewBI.this, env, new Object[] {
                        "Bean Models cannot be instantiated using the ?", key, " built-in" });
            }
            if (JYTHON_MODEL_CLASS != null && JYTHON_MODEL_CLASS.isAssignableFrom(cl)) {
                throw new _MiscTemplateException(NewBI.this, env, new Object[] {
                        "Jython Models cannot be instantiated using the ?", key, " built-in" });
            }
        }

        public Object exec(List arguments) throws TemplateModelException {
            ObjectWrapper ow = env.getObjectWrapper();
            BeansWrapper bw = 
                ow instanceof BeansWrapper 
                ? (BeansWrapper)ow
                : BeansWrapper.getDefaultInstance();
            return bw.newInstance(cl, arguments);
        }
    }
}

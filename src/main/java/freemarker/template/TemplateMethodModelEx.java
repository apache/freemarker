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

package freemarker.template;

import java.util.List;

import freemarker.core.Environment;
import freemarker.template.utility.DeepUnwrap;

/**
 * "extended method" template language data type: Objects that act like functions. Their main application is calling
 * Java methods via {@link freemarker.ext.beans.BeansWrapper}, but you can implement this interface to create
 * top-level functions too. They are "extended" compared to the deprecated {@link TemplateMethodModel}, which could only
 * accept string parameters.
 * 
 * <p>In templates they are used like {@code myMethod(1, "foo")} or {@code myJavaObject.myJavaMethod(1, "foo")}.
 *  
 * @author Attila Szegedi, szegedia at users dot sourceforge dot net
 */
public interface TemplateMethodModelEx extends TemplateMethodModel {

    /**
     * Executes the method call.
     *  
     * @param arguments a {@link List} of {@link TemplateModel}-s,
     *     containing the arguments passed to the method. If the implementation absolutely wants 
     *     to operate on POJOs, it can use the static utility methods in the {@link DeepUnwrap} 
     *     class to easily obtain them. However, unwrapping is not always possible (or not perfectly), and isn't always
     *     efficient, so it's recommended to use the original {@link TemplateModel} value as much as possible.
     *      
     * @return the return value of the method, or {@code null}. If the returned value
     *     does not implement {@link TemplateModel}, it will be automatically 
     *     wrapped using the {@link Environment#getObjectWrapper() environment's 
     *     object wrapper}.
     */
    public Object exec(List arguments) throws TemplateModelException;
    
}
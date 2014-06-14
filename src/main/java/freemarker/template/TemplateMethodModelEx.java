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
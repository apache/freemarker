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

/*
 * 22 October 1999: This class added by Holger Arendt.
 */

package freemarker.template;

import java.util.List;

import freemarker.core.Environment;

/**
 * "method" template language data type: Objects that act like functions. The name comes from that their original
 * application was calling Java methods via {@link freemarker.ext.beans.BeansWrapper}. 
 * 
 * <p>In templates they are used like {@code myMethod("foo", "bar")} or {@code myJavaObject.myJavaMethod("foo", "bar")}. 
 * 
 * @deprecated Use {@link TemplateMethodModelEx} instead. This interface is from the old times when the only kind of
 *    value you could pass in was string.
 */
public interface TemplateMethodModel extends TemplateModel {

    /**
     * Executes the method call. All arguments passed to the method call are 
     * coerced to strings before being passed, if the FreeMarker rules allow
     * the coercion. If some of the passed arguments can not be coerced to a
     * string, an exception will be raised in the engine and the method will 
     * not be called. If your method would like to act on actual data model 
     * objects instead of on their string representations, implement the 
     * {@link TemplateMethodModelEx} instead.
     * 
     * @param arguments a <tt>List</tt> of <tt>String</tt> objects
     *     containing the values of the arguments passed to the method.
     *  
     * @return the return value of the method, or {@code null}. If the returned value
     *     does not implement {@link TemplateModel}, it will be automatically 
     *     wrapped using the {@link Environment#getObjectWrapper() environment 
     *     object wrapper}.
     */
    public Object exec(List arguments) throws TemplateModelException;
}

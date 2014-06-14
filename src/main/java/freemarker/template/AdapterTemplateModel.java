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

/**
 * A {@link TemplateModel} that can be unwrapped and then it considers a provided desired (hint) class. This is
 * useful when multiple languages has to communicate with each other through FreeMarker. For example, if we have a
 * model that wraps a Jython object, then we have to unwrap that differently when we pass it to plain Java method and
 * when we pass it to a Jython method.
 * 
 * <p>This is rarely implemented by applications. It is typically implemented by the model classes belonging to
 * {@link ObjectWrapper}-s.
 */
public interface AdapterTemplateModel extends TemplateModel {
    /**
     * Retrieves the underlying object, or some other object semantically 
     * equivalent to its value narrowed by the class hint.   
     * @param hint the desired class of the returned value. An implementation 
     * should make reasonable effort to retrieve an object of the requested 
     * class, but if that is impossible, it must at least return the underlying 
     * object as-is. As a minimal requirement, an implementation must always 
     * return the exact underlying object when 
     * <tt>hint.isInstance(underlyingObject)</tt> holds. When called 
     * with <tt>java.lang.Object.class</tt>, it should return a generic Java 
     * object (i.e. if the model is wrapping a scripting language object that is
     * further wrapping a Java object, the deepest underlying Java object should
     * be returned). 
     * @return the underlying object, or its value accommodated for the hint
     * class.
     */
    public Object getAdaptedObject(Class hint);
}

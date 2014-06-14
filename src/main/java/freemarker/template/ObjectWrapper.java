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
import java.util.Map;
import java.util.ResourceBundle;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.util.WrapperTemplateModel;

/**
 * Maps Java objects to the type-system of FreeMarker Template Language (see the {@link TemplateModel}
 * interfaces). Thus this is what decides what parts of the Java objects will be accessible in the templates and how.
 * 
 * <p>For example, with a {@link BeansWrapper} both the items of {@link Map} and the JavaBean properties (the getters)
 * of an object are accessible in template uniformly with the {@code myObject.foo} syntax, where "foo" is the map key or
 * the property name. This is because both kind of object is wrapped by {@link BeansWrapper} into a
 * {@link TemplateHashModel} implementation that will call {@link Map#get(Object)} or the getter method, transparently
 * to the template language.
 * 
 * @see Configuration#setObjectWrapper(ObjectWrapper)
 */
public interface ObjectWrapper {
    
    /**
     * An {@link ObjectWrapper} that exposes the object methods and JavaBeans properties as hash elements, and has
     * custom handling for Java {@link Map}-s, {@link ResourceBundle}-s, etc. It doesn't treat
     * {@link org.w3c.dom.Node}-s and Jython objects specially, however.
     * 
     * @deprecated Use {@link BeansWrapperBuilder#getResult()} instead; this instance isn't read-only
     *    and thus can't be trusted.
     */
    ObjectWrapper BEANS_WRAPPER = BeansWrapper.getDefaultInstance();

    /**
     * The default object wrapper implementation, focusing on backward compatibility and out-of-the box extra features.
     * Extends {@link BeansWrapper} with the special handling of {@link org.w3c.dom.Node}-s (for XML processing) and
     * Jython objects. However, for backward compatibility, it also somewhat downgrades {@link BeansWrapper} by using   
     * {@link SimpleHash} for {@link Map}-s, {@link SimpleSequence} for {@link List}-s and collections/arrays.
     * Furthermore it uses {@link SimpleScalar}, {@link SimpleNumber} to wrap {@link String}-s and {@link Number}-s,
     * although this is not considered to be harmful.    
     * 
     * @deprecated Use {@link BeansWrapperBuilder#getResult()} instead; this instance isn't
     *    read-only and thus can't be trusted.
     */
    ObjectWrapper DEFAULT_WRAPPER = DefaultObjectWrapper.instance;

    /**
     * Object wrapper that uses {@code SimpleXXX} wrappers only.
     * It behaves like the {@link #DEFAULT_WRAPPER}, but for objects
     * that it does not know how to wrap as a {@code SimpleXXX} it 
     * throws an exception. It makes no use of reflection-based 
     * exposure of anything, which may makes it a good candidate for security-restricted applications.
     * 
     * @deprecated No replacement as it was seldom if ever used by anyone; this instance isn't
     *    read-only and thus can't be trusted.
     */
    ObjectWrapper SIMPLE_WRAPPER = SimpleObjectWrapper.instance;
    
    /**
     * Makes a {@link TemplateModel} out of a non-{@link TemplateModel} object, usually by "wrapping" it into a
     * {@link TemplateModel} implementation that delegates to the original object.
     * 
     * @param obj The object to wrap into a {@link TemplateModel}. If it already implements {@link TemplateModel},
     *      it should just return the object as is.
     * 
     * @return a {@link TemplateModel} wrapper of the object passed in. To support un-wrapping, you may consider the
     *     return value to implement {@link WrapperTemplateModel} and {@link AdapterTemplateModel}.
     */
    TemplateModel wrap(Object obj) throws TemplateModelException;
    
}

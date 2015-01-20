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
     * {@link org.w3c.dom.Node}-s and Jython objects specially, however. As of 2.3.22, using
     * {@link DefaultObjectWrapper} with its {@code incompatibleImprovements} property set to 2.3.22 (or higher) is
     * recommended instead.
     * 
     * @deprecated Use {@link BeansWrapperBuilder#build()} instead; this instance isn't read-only
     *    and thus can't be trusted.
     */
    ObjectWrapper BEANS_WRAPPER = BeansWrapper.getDefaultInstance();

    /**
     * The legacy default object wrapper implementation, focusing on backward compatibility and out-of-the W3C DOM
     * wrapping box extra features. See {@link DefaultObjectWrapper} for more information.
     * 
     * @deprecated Use {@link DefaultObjectWrapperBuilder#build()} instead; this instance isn't read-only and thus can't
     *             be trusted.
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
     *      it should just return the object as is. If it's {@code null}, the method should return {@code null}
     *      (however, {@link BeansWrapper}, has a legacy option for returning a null model object instead, but it's not
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

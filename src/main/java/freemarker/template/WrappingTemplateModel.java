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
 * Convenience base-class for containers that wrap their contained arbitrary Java objects into {@link TemplateModel}
 * instances.
 */
abstract public class WrappingTemplateModel {

    /** @deprecated Because it's a VM-wide modifiable field */
    private static ObjectWrapper defaultObjectWrapper = DefaultObjectWrapper.instance;
    
    private ObjectWrapper objectWrapper;
    
    /**
     * Sets the default object wrapper that is used when a wrapping template
     * model is constructed without being passed an explicit object wrapper.
     * The default value is {@link ObjectWrapper#SIMPLE_WRAPPER}.
     * Note that {@link Configuration#setSharedVariable(String, Object)} and
     * {@link Template#process(Object, java.io.Writer)} don't use this setting,
     * they rather use whatever object wrapper their 
     * {@link Configuration#getObjectWrapper()} method returns.
     * 
     * @deprecated This method has VM-wide effect, which makes it unsuitable for application where multiple components
     *      might use FreeMarker internally.
     */
    public static void setDefaultObjectWrapper(ObjectWrapper objectWrapper) {
        defaultObjectWrapper = objectWrapper;
    }

    /**
     * Returns the default object wrapper that is used when a wrapping template
     * model is constructed without being passed an explicit object wrapper.
     * Note that {@link Configuration#setSharedVariable(String, Object)} and
     * {@link Template#process(Object, java.io.Writer)} don't use this setting,
     * they rather use whatever object wrapper their 
     * {@link Configuration#getObjectWrapper()} method returns.
     * 
     * @deprecated Don't depend on this object, as it can be replace by anybody in the same JVM.
     */
    public static ObjectWrapper getDefaultObjectWrapper() {
        return defaultObjectWrapper;
    }
    
    /**
     * Protected constructor that creates a new wrapping template model using
     * the default object wrapper.
     * 
     * @deprecated Use {@link #WrappingTemplateModel(ObjectWrapper)} instead; this method uses the deprecated.
     */
    protected WrappingTemplateModel() {
        this(defaultObjectWrapper);
    }

    /**
     * Protected constructor that creates a new wrapping template model using the specified object wrapper.
     * 
     * @param objectWrapper the wrapper to use. Passing {@code null} to it
     *     is allowed but deprecated. If {@code null} is passed, the deprecated default object wrapper
     *     is used.
     */
    protected WrappingTemplateModel(ObjectWrapper objectWrapper) {
        this.objectWrapper = 
            objectWrapper != null ? objectWrapper : defaultObjectWrapper;
        if (this.objectWrapper == null) {
            this.objectWrapper = defaultObjectWrapper = new DefaultObjectWrapper();
        }
    }
    
    /**
     * Returns the object wrapper instance used by this wrapping template model.
     */
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    /**
     * Wraps the passed object into a template model using this object's object
     * wrapper.
     * @param obj the object to wrap
     * @return the template model that wraps the object
     * @throws TemplateModelException if the wrapper does not know how to
     * wrap the passed object.
     */
    protected final TemplateModel wrap(Object obj) throws TemplateModelException {
        return objectWrapper.wrap(obj);
    }
    
}

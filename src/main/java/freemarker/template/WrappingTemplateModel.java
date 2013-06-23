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

/**
 * Convenience base-class for containers that wrap arbitrary Java objects into 
 * {@link TemplateModel} instances.
 */
abstract public class WrappingTemplateModel {

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
     */
    public static ObjectWrapper getDefaultObjectWrapper() {
        return defaultObjectWrapper;
    }
    
    /**
     * Protected constructor that creates a new wrapping template model using
     * the default object wrapper.
     */
    protected WrappingTemplateModel() {
        this(defaultObjectWrapper);
    }

    /**
     * Protected constructor that creates a new wrapping template model using
     * the specified object wrapper.
     * @param objectWrapper the wrapper to use. If null is passed, the default
     * object wrapper is used.
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

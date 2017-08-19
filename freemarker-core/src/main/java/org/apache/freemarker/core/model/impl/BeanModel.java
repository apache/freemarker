/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model.impl;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._DelayedTemplateLanguageTypeDescription;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateModelWithAPISupport;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that will wrap an arbitrary object into {@link org.apache.freemarker.core.model.TemplateHashModel}
 * interface allowing calls to arbitrary property getters and invocation of
 * accessible methods on the object from a template using the
 * <tt>object.foo</tt> to access properties and <tt>object.bar(arg1, arg2)</tt> to
 * invoke methods on it. You can also use the <tt>object.foo[index]</tt> syntax to
 * access indexed properties. It uses Beans {@link java.beans.Introspector}
 * to dynamically discover the properties and methods. 
 */

public class BeanModel
        implements TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport {

    private static final Logger LOG = LoggerFactory.getLogger(BeanModel.class);

    protected final Object object;
    protected final DefaultObjectWrapper wrapper;
    
    // We use this to represent an unknown value as opposed to known value of null (JR)
    static final TemplateModel UNKNOWN = new SimpleScalar("UNKNOWN");

    // I've tried to use a volatile ConcurrentHashMap field instead of HashMap + synchronized(this), but oddly it was
    // a bit slower, at least on Java 8 u66. 
    private HashMap<Object, TemplateModel> memberCache;

    /**
     * Creates a new model that wraps the specified object. Note that there are
     * specialized subclasses of this class for wrapping arrays, collections,
     * enumeration, iterators, and maps. Note also that the superclass can be
     * used to wrap String objects if only scalar functionality is needed. You
     * can also choose to delegate the choice over which model class is used for
     * wrapping to {@link DefaultObjectWrapper#wrap(Object)}.
     * @param object the object to wrap into a model.
     * @param wrapper the {@link DefaultObjectWrapper} associated with this model.
     * Every model has to have an associated {@link DefaultObjectWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public BeanModel(Object object, DefaultObjectWrapper wrapper) {
        // [2.4]: All models were introspected here, then the results was discareded, and get() will just do the
        // introspection again. So is this necessary? (The inrospectNow parameter was added in 2.3.21 to allow
        // lazy-introspecting DefaultObjectWrapper.trueModel|falseModel.)
        this(object, wrapper, true);
    }

    BeanModel(Object object, DefaultObjectWrapper wrapper, boolean inrospectNow) {
        this.object = object;
        this.wrapper = wrapper;
        if (inrospectNow && object != null) {
            // [2.4]: Could this be removed?
            wrapper.getClassIntrospector().get(object.getClass());
        }
    }
    
    /**
     * Uses Beans introspection to locate a JavaBean property or method with name
     * matching the key name. If a method is found, it's wrapped
     * into {@link TemplateFunctionModel} (a {@link JavaMethodModel} more specifically).
     * If a JavaBean property is found, its value is returned. Introspection results
     * for various properties and methods are cached on a per-class basis, so the costly
     * introspection is performed only once per property or method of a class.
     * (Side-note: this also implies that any class whose method has been called
     * will be strongly referred to by the framework and will not become
     * unloadable until this class has been unloaded first. Normally this is not
     * an issue, but can be in a rare scenario where you invoke many classes on-
     * the-fly. Also, as the cache grows with new classes and methods introduced
     * to the framework, it may appear as if it were leaking memory. The
     * framework does, however detect class reloads (if you happen to be in an
     * environment that does this kind of things--servlet containers do it when
     * they reload a web application) and flushes the cache. If no method or
     * property matching the key is found, the framework will try to invoke
     * methods with signature
     * <tt>non-void-return-type get(java.lang.String)</tt>,
     * then <tt>non-void-return-type get(java.lang.Object)</tt>, or 
     * alternatively (if the wrapped object is a resource bundle) 
     * <tt>Object get(java.lang.String)</tt>.
     * @throws TemplateException if there was no property nor method nor
     * a generic <tt>get</tt> method to invoke.
     */
    @Override
    public TemplateModel get(String key)
        throws TemplateException {
        Class<?> clazz = object.getClass();
        Map<Object, Object> classInfo = wrapper.getClassIntrospector().get(clazz);
        TemplateModel retval = null;
        
        try {
            Object fd = classInfo.get(key);
            if (fd != null) {
                retval = invokeThroughDescriptor(fd, classInfo);
            } else {
                retval = invokeGenericGet(classInfo, clazz, key);
            }
            if (retval == UNKNOWN) {
                if (wrapper.isStrict()) {
                    throw new InvalidPropertyException("No such bean property: " + key);
                }
                retval = wrapper.wrap(null);
            }
            return retval;
        } catch (TemplateException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateException(e,
                    "An error has occurred when reading existing sub-variable ", new _DelayedJQuote(key),
                    "; see cause exception! The type of the containing value was: ",
                    new _DelayedTemplateLanguageTypeDescription(this)
            );
        }
    }

    /**
     * Whether the model has a plain get(String) or get(Object) method
     */
    
    protected boolean hasPlainGetMethod() {
        return wrapper.getClassIntrospector().get(object.getClass()).get(ClassIntrospector.GENERIC_GET_KEY) != null;
    }
    
    private TemplateModel invokeThroughDescriptor(Object desc, Map<Object, Object> classInfo)
            throws IllegalAccessException, InvocationTargetException, TemplateException {
        // See if this particular instance has a cached implementation for the requested feature descriptor
        TemplateModel cachedModel;
        synchronized (this) {
            cachedModel = memberCache != null ? memberCache.get(desc) : null;
        }

        if (cachedModel != null) {
            return cachedModel;
        }

        TemplateModel resultModel = UNKNOWN;
        if (desc instanceof PropertyDescriptor) {
            PropertyDescriptor pd = (PropertyDescriptor) desc;
            Method readMethod = pd.getReadMethod();
            if (readMethod != null) {
                // Unlike in FreeMarker 2, we prefer the normal read method even if there's an indexed read method.
                resultModel = wrapper.invokeMethod(object, readMethod, null);
                // cachedModel remains null, as we don't cache these
            } else if (desc instanceof IndexedPropertyDescriptor) {
                // In FreeMarker 2 we have exposed such indexed properties as sequences, but they can't support
                // the size() method, so we have discontinued that. People has to call the indexed read method like
                // any other method.
                resultModel = UNKNOWN;
            } else {
                throw new IllegalStateException("PropertyDescriptor.readMethod shouldn't be null");
            }
        } else if (desc instanceof Field) {
            resultModel = wrapper.wrap(((Field) desc).get(object));
            // cachedModel remains null, as we don't cache these
        } else if (desc instanceof Method) {
            Method method = (Method) desc;
            resultModel = cachedModel = new SimpleJavaMethodModel(
                    object, method, ClassIntrospector.getArgTypes(classInfo, method), wrapper);
        } else if (desc instanceof OverloadedMethods) {
            resultModel = cachedModel = new OverloadedJavaMethodModel(
                    object, (OverloadedMethods) desc, wrapper);
        }
        
        // If new cachedModel was created, cache it
        if (cachedModel != null) {
            synchronized (this) {
                if (memberCache == null) {
                    memberCache = new HashMap<>();
                }
                memberCache.put(desc, cachedModel);
            }
        }
        return resultModel;
    }
    
    void clearMemberCache() {
        synchronized (this) {
            memberCache = null;
        }
    }

    protected TemplateModel invokeGenericGet(Map/*<Object, Object>*/ classInfo, Class<?> clazz, String key)
            throws IllegalAccessException, InvocationTargetException,
        TemplateException {
        Method genericGet = (Method) classInfo.get(ClassIntrospector.GENERIC_GET_KEY);
        if (genericGet == null) {
            return UNKNOWN;
        }

        return wrapper.invokeMethod(object, genericGet, new Object[] { key });
    }

    protected TemplateModel wrap(Object obj) throws ObjectWrappingException {
        return wrapper.getOuterIdentity().wrap(obj);
    }
    
    protected Object unwrap(TemplateModel model)
    throws TemplateException {
        return wrapper.unwrap(model);
    }

    /**
     * Tells whether the model is considered to be empty.
     * It is empty if the wrapped object is a 0 length {@link String}, or an empty {@link Collection} or and empty
     * {@link Map}, or an {@link Iterator} that has no more items, or a {@link Boolean#FALSE}, or {@code null}. 
     */
    @Override
    public boolean isEmpty() {
        if (object instanceof String) {
            return ((String) object).length() == 0;
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        }
        if (object instanceof Iterator) {
            return !((Iterator<?>) object).hasNext();
        }
        if (object instanceof Map) {
            return ((Map<?,?>) object).isEmpty();
        }
        // [FM3] Why's FALSE empty? 
        return object == null || Boolean.FALSE.equals(object);
    }
    
    /**
     * Returns the same as {@link #getWrappedObject()}; to ensure that, this method will be final starting from 2.4.
     * This behavior of {@link BeanModel} is assumed by some FreeMarker code. 
     */
    @Override
    public Object getAdaptedObject(Class<?> hint) {
        return object;  // return getWrappedObject(); starting from 2.4
    }

    @Override
    public Object getWrappedObject() {
        return object;
    }
    
    @Override
    public int size() {
        return wrapper.getClassIntrospector().keyCount(object.getClass());
    }

    @Override
    public TemplateCollectionModel keys() {
        return new CollectionAndSequence(new SimpleSequence(keySet(), wrapper));
    }

    @Override
    public TemplateCollectionModel values() throws TemplateException {
        List<Object> values = new ArrayList<>(size());
        TemplateModelIterator it = keys().iterator();
        while (it.hasNext()) {
            String key = ((TemplateScalarModel) it.next()).getAsString();
            values.add(get(key));
        }
        return new CollectionAndSequence(new SimpleSequence(values, wrapper));
    }
    
    @Override
    public String toString() {
        return object.toString();
    }

    /**
     * Helper method to support TemplateHashModelEx. Returns the Set of
     * Strings which are available via the TemplateHashModel
     * interface. Subclasses that override <tt>invokeGenericGet</tt> to
     * provide additional hash keys should also override this method.
     */
    protected Set/*<Object>*/ keySet() {
        return wrapper.getClassIntrospector().keySet(object.getClass());
    }

    @Override
    public TemplateModel getAPI() throws TemplateException {
        return wrapper.wrapAsAPI(object);
    }
    
}
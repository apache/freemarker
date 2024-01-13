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

package freemarker.ext.beans;

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

import freemarker.core.BugException;
import freemarker.core.CollectionAndSequence;
import freemarker.core.Macro;
import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.ModelFactory;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.log.Logger;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateModelWithAPISupport;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.CollectionUtils;
import freemarker.template.utility.StringUtil;

/**
 * A class that will wrap an arbitrary object into {@link freemarker.template.TemplateHashModel}
 * interface allowing calls to arbitrary property getters and invocation of
 * accessible methods on the object from a template using the
 * {@code object.foo} to access properties and {@code object.bar(arg1, arg2)} to
 * invoke methods on it. You can also use the {@code object.foo[index]} syntax to
 * access indexed properties. It uses Beans {@link java.beans.Introspector}
 * to dynamically discover the properties and methods. 
 */

public class BeanModel
implements TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport {
    private static final Logger LOG = Logger.getLogger("freemarker.beans");
    protected final Object object;
    protected final BeansWrapper wrapper;

    // We use this to represent an unknown value as opposed to known value of null (JR)
    static final TemplateModel UNKNOWN = new SimpleScalar("UNKNOWN");

    static final ModelFactory FACTORY =
            new ModelFactory() {
                @Override
                public TemplateModel create(Object object, ObjectWrapper wrapper) {
                    return new BeanModel(object, (BeansWrapper) wrapper);
                }
            };

    // I've tried to use a volatile ConcurrentHashMap field instead of HashMap + synchronized(this), but oddly it was
    // a bit slower, at least on Java 8 u66. 
    private HashMap<Object, TemplateModel> memberCache;

    /**
     * Creates a new model that wraps the specified object. Note that there are
     * specialized subclasses of this class for wrapping arrays, collections,
     * enumeration, iterators, and maps. Note also that the superclass can be
     * used to wrap String objects if only scalar functionality is needed. You
     * can also choose to delegate the choice over which model class is used for
     * wrapping to {@link BeansWrapper#wrap(Object)}.
     * @param object the object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public BeanModel(Object object, BeansWrapper wrapper) {
        // [2.4]: All models were introspected here, then the results was discareded, and get() will just do the
        // introspection again. So is this necessary? (The inrospectNow parameter was added in 2.3.21 to allow
        // lazy-introspecting BeansWrapper.trueModel|falseModel.)
        this(object, wrapper, true);
    }

    /** @since 2.3.21 */
    BeanModel(Object object, BeansWrapper wrapper, boolean inrospectNow) {
        this.object = object;
        this.wrapper = wrapper;
        if (inrospectNow && object != null) {
            // [2.4]: Could this be removed?
            wrapper.getClassIntrospector().get(object.getClass());
        }
    }

    /**
     * Uses Beans introspection to locate a property or method with name
     * matching the key name. If a method or property is found, it's wrapped
     * into {@link freemarker.template.TemplateMethodModelEx} (for a method or
     * indexed property), or evaluated on-the-fly and the return value wrapped
     * into appropriate model (for a non-indexed property). Models for various
     * properties and methods are cached on a per-class basis, so the costly
     * introspection is performed only once per property or method of a class.
     * (Side-note: this also implies that any class whose method has been called
     * will be strongly referred to by the framework and will not become
     * unloadable until this class has been unloaded first. Normally this is not
     * an issue, but can be in a rare scenario where you create many classes on-
     * the-fly. Also, as the cache grows with new classes and methods introduced
     * to the framework, it may appear as if it were leaking memory. The
     * framework does, however detect class reloads (if you happen to be in an
     * environment that does this kind of things--servlet containers do it when
     * they reload a web application) and flushes the cache. If no method or
     * property matching the key is found, the framework will try to invoke
     * methods with signature
     * {@code non-void-return-type get(java.lang.String)},
     * then {@code non-void-return-type get(java.lang.Object)}, or 
     * alternatively (if the wrapped object is a resource bundle) 
     * {@code Object getObject(java.lang.String)}.
     *
     * <p>As of 2.3.33, the default implementation of this method delegates to {@link #get(String, boolean)}. It's
     * better to override that, instead of this method. Otherwise, unwanted behavior can arise if the model class also
     * implements {@link MethodCallAwareTemplateHashModel}, as that will certainly call {@link #get(String, boolean)}
     * internally, and not the overridden version of this method.
     *
     * @throws TemplateModelException if there was no property nor method nor
     * a generic {@code get} method to invoke.
     */
    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            return get(key, false);
        } catch (MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException e) {
            throw new BugException(e);
        }
    }

    /**
     * Override this if you want to customize the behavior of {@link #get(String)}.
     * In standard implementations at least, this is what {@link #get(String)}, and
     * {@link MethodCallAwareTemplateHashModel#getBeforeMethodCall(String)} delegates to.
     *
     * @param key
     *      Same as the parameter of {@link #get(String)}.
     * @param beforeMethodCall
     *      This is a hint that tells that the returned value will be called in the template. This was added to
     *      implement {@link BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)}.
     *      This parameter is {@code false} when {@link #get(String)} is called, and
     *      {@code true} when {@link MethodCallAwareTemplateHashModel#getBeforeMethodCall(String)} is called.
     *      If this is {@code true}, this method should return a {@link TemplateMethodModelEx}, or {@code null},
     *      or fail with {@link MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException}.
     *
     * @since 2.3.33
     */
    // Before calling this from FreeMarker classes, consider that some users may have overridden {@link #get(String)}
    // instead, as this class didn't exist before 2.3.33. So with incompatibleImprovements before that, that should be
    // the only place where this gets called, or else the behavior of the model will be inconsistent.
    protected TemplateModel get(String key, boolean beforeMethodCall)
            throws TemplateModelException, MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException {
        Class<?> clazz = object.getClass();
        Map<Object, Object> classInfo = wrapper.getClassIntrospector().get(clazz);
        TemplateModel retval = null;

        try {
            if (wrapper.isMethodsShadowItems()) {
                Object fd = classInfo.get(key);
                if (fd != null) {
                    retval = invokeThroughDescriptor(fd, classInfo, beforeMethodCall);
                } else {
                    retval = invokeGenericGet(classInfo, clazz, key);
                }
            } else {
                TemplateModel model = invokeGenericGet(classInfo, clazz, key);
                final TemplateModel nullModel = wrapper.wrap(null);
                if (model != nullModel && model != UNKNOWN) {
                    return model;
                }
                Object fd = classInfo.get(key);
                if (fd != null) {
                    retval = invokeThroughDescriptor(fd, classInfo, beforeMethodCall);
                    if (retval == UNKNOWN && model == nullModel) {
                        // This is the (somewhat subtle) case where the generic get() returns null
                        // and we have no bean info, so we respect the fact that
                        // the generic get() returns null and return null. (JR)
                        retval = nullModel;
                    }
                }
            }
            if (retval == UNKNOWN) {
                if (wrapper.isStrict()) {
                    throw new InvalidPropertyException("No such bean property: " + key);
                } else if (LOG.isDebugEnabled()) {
                    logNoSuchKey(key, classInfo);
                }
                retval = wrapper.wrap(null);
            }
            return retval;
        } catch (TemplateModelException | MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException e) {
            throw e;
        } catch (Exception e) {
            throw new _TemplateModelException(e,
                    "An error has occurred when reading existing sub-variable ", new _DelayedJQuote(key),
                    "; see cause exception! The type of the containing value was: ",
                    new _DelayedFTLTypeDescription(this)
            );
        }
    }

    /**
     * Can be overridden to be public, to implement {@link MethodCallAwareTemplateHashModel}. We don't implement that
     * in {@link BeanModel} for backward compatibility, but the functionality is present. If you expose this method by
     * implementing {@link MethodCallAwareTemplateHashModel}, then be sure that {@link #get(String)} is
     * not overridden in custom subclasses; if it is, then those subclasses should be modernized to override
     * {@link #get(String, boolean)} instead.
     *
     * @since 2.3.33
     */
    protected TemplateModel getBeforeMethodCall(String key)
            throws TemplateModelException, MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException {
        TemplateModel result = get(key, true);
        if (result instanceof  TemplateMethodModelEx || result == null) {
            return result;
        }
        throw new MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException(result, null);
    }

    private void logNoSuchKey(String key, Map<?, ?> keyMap) {
        LOG.debug("Key " + StringUtil.jQuoteNoXSS(key) + " was not found on instance of " + 
            object.getClass().getName() + ". Introspection information for " +
            "the class is: " + keyMap);
    }
    
    /**
     * Whether the model has a plain get(String) or get(Object) method
     */
    
    protected boolean hasPlainGetMethod() {
        return wrapper.getClassIntrospector().get(object.getClass()).get(ClassIntrospector.GENERIC_GET_KEY) != null;
    }
    
    private TemplateModel invokeThroughDescriptor(Object desc, Map<Object, Object> classInfo, boolean beforeMethodCall)
            throws IllegalAccessException, InvocationTargetException, TemplateModelException,
            MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException {
        // See if this particular instance has a cached implementation for the requested feature descriptor
        TemplateModel cachedModel;
        synchronized (this) {
            cachedModel = memberCache != null ? memberCache.get(desc) : null;
        }

        if (cachedModel != null) {
            return cachedModel;
        }

        // ATTENTION! As the value of beforeMethodCall is not part of the cache lookup key, it's very important that we
        // don't cache the value for desc-s where beforeMethodCall can have influence on the result!

        TemplateModel resultModel = UNKNOWN;
        if (desc instanceof FastPropertyDescriptor) {
            FastPropertyDescriptor pd = (FastPropertyDescriptor) desc;
            Method indexedReadMethod = pd.getIndexedReadMethod(); 
            if (indexedReadMethod != null) {
                if (!wrapper.getPreferIndexedReadMethod() && (pd.getReadMethod()) != null) {
                    resultModel = wrapper.invokeMethod(object, pd.getReadMethod(), null);
                    // cachedModel remains null, as we don't cache these
                } else {
                    resultModel = cachedModel = 
                        new SimpleMethodModel(object, indexedReadMethod, 
                                ClassIntrospector.getArgTypes(classInfo, indexedReadMethod), wrapper);
                }
            } else {
                // cachedModel must remains null in this branch, because the result is influenced by beforeMethodCall,
                // which wasn't part of the cache key!

                if (!beforeMethodCall) {
                    resultModel = wrapper.invokeMethod(object, pd.getReadMethod(), null);
                    // cachedModel remains null, as we don't cache these
                } else {
                    if (pd.isMethodInsteadOfPropertyValueBeforeCall()) {
                        // Do not cache this result! See comments earlier!
                        resultModel = new SimpleMethodModel(
                                object, pd.getReadMethod(), CollectionUtils.EMPTY_CLASS_ARRAY, wrapper);
                    } else {
                        resultModel = wrapper.invokeMethod(object, pd.getReadMethod(), null);

                        // Checks if freemarker.core.MethodCall would accept this result:
                        if (!(resultModel instanceof TemplateMethodModel || resultModel instanceof Macro)) {
                            throw new MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException(
                                    resultModel,
                                    "This member of the parent object is seen by templates as a property of it "
                                            + "(with other words, an attribute, or a field), not a method of it. "
                                            + "Thus, to get its value, it must not be called as a method.");
                        }
                    }
                }
            }
        } else if (desc instanceof Field) {
            resultModel = wrapper.readField(object, (Field) desc);
            // cachedModel remains null, as we don't cache these
        } else if (desc instanceof Method) {
            Method method = (Method) desc;
            resultModel = cachedModel = new SimpleMethodModel(
                    object, method, ClassIntrospector.getArgTypes(classInfo, method), wrapper);
        } else if (desc instanceof OverloadedMethods) {
            resultModel = cachedModel = new OverloadedMethodsModel(
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
        TemplateModelException {
        Method genericGet = (Method) classInfo.get(ClassIntrospector.GENERIC_GET_KEY);
        if (genericGet == null) {
            return UNKNOWN;
        }

        return wrapper.invokeMethod(object, genericGet, new Object[] { key });
    }

    protected TemplateModel wrap(Object obj)
    throws TemplateModelException {
        return wrapper.getOuterIdentity().wrap(obj);
    }
    
    protected Object unwrap(TemplateModel model)
    throws TemplateModelException {
        return wrapper.unwrap(model);
    }

    /**
     * Tells whether the model is empty. It is empty if either the wrapped 
     * object is null, or it's a Boolean with false value.
     */
    @Override
    public boolean isEmpty() {
        if (object instanceof String) {
            return ((String) object).length() == 0;
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).isEmpty();
        }
        if (object instanceof Iterator && wrapper.is2324Bugfixed()) {
            return !((Iterator<?>) object).hasNext();
        }
        if (object instanceof Map) {
            return ((Map<?,?>) object).isEmpty();
        }
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
    public TemplateCollectionModel values() throws TemplateModelException {
        List<Object> values = new ArrayList<>(size());
        TemplateModelIterator it = keys().iterator();
        while (it.hasNext()) {
            String key = ((TemplateScalarModel) it.next()).getAsString();
            values.add(get(key));
        }
        return new CollectionAndSequence(new SimpleSequence(values, wrapper));
    }
    
    /**
     * Used for {@code classic_compatbile} mode; don't use it for anything else.
     * In FreeMarker 1.7 (and also at least in 2.1) {@link BeanModel} was a {@link TemplateScalarModel}. Some internal
     * FreeMarker code tries to emulate FreeMarker classic by calling this method when a {@link TemplateScalarModel} is
     * expected.
     * 
     * @return Never {@code null}
     */
    String getAsClassicCompatibleString() {
        if (object == null) {
            return "null";
        }
        String s = object.toString();
        return s != null ? s : "null";        
    }
    
    @Override
    public String toString() {
        return object.toString();
    }

    /**
     * Helper method to support TemplateHashModelEx. Returns the Set of
     * Strings which are available via the TemplateHashModel
     * interface. Subclasses that override {@code invokeGenericGet} to
     * provide additional hash keys should also override this method.
     */
    protected Set/*<Object>*/ keySet() {
        return wrapper.getClassIntrospector().keySet(object.getClass());
    }

    @Override
    public TemplateModel getAPI() throws TemplateModelException {
        return wrapper.wrapAsAPI(object);
    }
    
}
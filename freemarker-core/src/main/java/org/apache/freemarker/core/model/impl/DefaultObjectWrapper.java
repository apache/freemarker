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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core._DelayedShortClassName;
import org.apache.freemarker.core._DelayedTemplateLanguageTypeDescription;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.RichObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.util._ClassUtils;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * The default implementation of the {@link ObjectWrapper} interface. Usually, you don't need to invoke instances of
 * this, as an instance of this is already the default value of the
 * {@link Configuration#getObjectWrapper() objectWrapper} setting. Then the
 * {@link ExtendableBuilder#ExtendableBuilder(Version, boolean) incompatibleImprovements} of the
 * {@link DefaultObjectWrapper} will be the same that you have set for the {@link Configuration} itself.
 * 
 * <p>
 * If you still need to invoke an instance, that should be done with {@link Builder#build()} (or
 * with {@link org.apache.freemarker.core.Configuration.ExtendableBuilder#setSetting(String, String)} with
 * {@code "objectWrapper"} key); the constructor isn't public.
 *
 * <p>
 * This class is thread-safe.
 */
public class DefaultObjectWrapper implements RichObjectWrapper {

    /**
     * At this level of exposure, all methods and properties of the
     * wrapped objects are exposed to the template.
     */
    public static final int EXPOSE_ALL = 0;

    /**
     * At this level of exposure, all methods and properties of the wrapped
     * objects are exposed to the template except methods that are deemed
     * not safe. The not safe methods are java.lang.Object methods wait() and
     * notify(), java.lang.Class methods getClassLoader() and newInstance(),
     * java.lang.reflect.Method and java.lang.reflect.Constructor invoke() and
     * newInstance() methods, all java.lang.reflect.Field set methods, all
     * java.lang.Thread and java.lang.ThreadGroup methods that can change its
     * state, as well as the usual suspects in java.lang.System and
     * java.lang.Runtime.
     */
    public static final int EXPOSE_SAFE = 1;

    /**
     * At this level of exposure, only property getters are exposed.
     * Additionally, property getters that map to unsafe methods are not
     * exposed (i.e. Class.classLoader and Thread.contextClassLoader).
     */
    public static final int EXPOSE_PROPERTIES_ONLY = 2;

    /**
     * At this level of exposure, no bean properties and methods are exposed.
     * Only map items, resource bundle items, and objects retrieved through
     * the generic get method (on objects of classes that have a generic get
     * method) can be retrieved through the hash interface.
     */
    public static final int EXPOSE_NOTHING = 3;

    // -----------------------------------------------------------------------------------------------------------------
    // Introspection cache:

    private final Object sharedIntrospectionLock;

    /**
     * {@link Class} to class info cache.
     * This object is possibly shared with other {@link DefaultObjectWrapper}-s!
     *
     * <p>When reading this, it's good idea to synchronize on sharedInrospectionLock when it doesn't hurt overall
     * performance. In theory that's not needed, but apps might fail to keep the rules.
     */
    private final ClassIntrospector classIntrospector;

    /**
     * {@link String} class name to {@link StaticModel} cache.
     * This object only belongs to a single {@link DefaultObjectWrapper}.
     * This has to be final as {@link #getStaticModels()} might returns it any time and then it has to remain a good
     * reference.
     */
    private final StaticModels staticModels;

    /**
     * {@link String} class name to an enum value hash.
     * This object only belongs to a single {@link DefaultObjectWrapper}.
     * This has to be final as {@link #getStaticModels()} might returns it any time and then it has to remain a good
     * reference.
     */
    private final ClassBasedModelFactory enumModels;

    // -----------------------------------------------------------------------------------------------------------------

    private final int defaultDateType;
    private final ObjectWrapper outerIdentity;
    private final boolean strict;
    @Deprecated // Only exists to keep some JUnit tests working... [FM3]
    private final boolean useModelCache;

    /** Extensions applicable at the beginning of wrap(Object); null if it would be 0 length otherwise. */
    private final DefaultObjectWrapperExtension[] extensionsBeforeWrapSpecialObject;
    /** Extensions applicable at the end of wrap(Object); null if it would be 0 length otherwise. */
    private final DefaultObjectWrapperExtension[] extensionsAfterWrapSpecialObject;

    private final Version incompatibleImprovements;

    /**
     * Initializes the instance based on the the {@link ExtendableBuilder} specified.
     *
     * @param finalizeConstruction Decides if the construction is finalized now, or the caller will do some more
     *     adjustments on the instance and then call {@link #finalizeConstruction()} itself.
     */
    protected DefaultObjectWrapper(ExtendableBuilder<?, ?> builder, boolean finalizeConstruction) {
        incompatibleImprovements = builder.getIncompatibleImprovements();  // normalized

        defaultDateType = builder.getDefaultDateType();
        outerIdentity = builder.getOuterIdentity() != null ? builder.getOuterIdentity() : this;
        strict = builder.isStrict();

        if (builder.getUsePrivateCaches()) {
            // As this is not a read-only DefaultObjectWrapper, the classIntrospector will be possibly replaced for a few times,
            // but we need to use the same sharedInrospectionLock forever, because that's what the model factories
            // synchronize on, even during the classIntrospector is being replaced.
            sharedIntrospectionLock = new Object();
            classIntrospector = new ClassIntrospector(builder.classIntrospectorBuilder, sharedIntrospectionLock);
        } else {
            // As this is a read-only DefaultObjectWrapper, the classIntrospector is never replaced, and since it's shared by
            // other DefaultObjectWrapper instances, we use the lock belonging to the shared ClassIntrospector.
            classIntrospector = builder.classIntrospectorBuilder.build();
            sharedIntrospectionLock = classIntrospector.getSharedLock();
        }

        staticModels = new StaticModels(this);
        enumModels = new EnumModels(this);
        useModelCache = builder.getUseModelCache();

        int extsAfterWSOCnt = 0;
        int extsBeforeWSOCnt = 0;
        for (DefaultObjectWrapperExtension ext : builder.getExtensions()) {
            if (ext.getPhase() == DefaultObjectWrapperExtensionPhase.AFTER_WRAP_SPECIAL_OBJECT) {
                extsAfterWSOCnt++;
            } else if (ext.getPhase() == DefaultObjectWrapperExtensionPhase.BEFORE_WRAP_SPECIAL_OBJECT)  {
                extsBeforeWSOCnt++;
            } else {
                throw new BugException();
            }
        }
        extensionsAfterWrapSpecialObject = extsAfterWSOCnt != 0
                ? new DefaultObjectWrapperExtension[extsAfterWSOCnt] : null;
        extensionsBeforeWrapSpecialObject = extsBeforeWSOCnt != 0
                ? new DefaultObjectWrapperExtension[extsBeforeWSOCnt] : null;
        int extsAfterWSOIdx = 0;
        int extsBeforeWSOIdx = 0;
        for (DefaultObjectWrapperExtension ext : builder.getExtensions()) {
            if (ext.getPhase() == DefaultObjectWrapperExtensionPhase.AFTER_WRAP_SPECIAL_OBJECT) {
                extensionsAfterWrapSpecialObject[extsAfterWSOIdx++] = ext;
            } else if (ext.getPhase() == DefaultObjectWrapperExtensionPhase.BEFORE_WRAP_SPECIAL_OBJECT)  {
                extensionsBeforeWrapSpecialObject[extsBeforeWSOIdx++] = ext;
            } else {
                throw new BugException();
            }
        }

        finalizeConstruction();
    }

    /**
     * Meant to be called after {@link DefaultObjectWrapper#DefaultObjectWrapper(ExtendableBuilder, boolean)} when
     * its last argument was {@code false}; makes the instance read-only if necessary, then registers the model
     * factories in the class introspector. No further changes should be done after calling this, if
     * {@code writeProtected} was {@code true}.
     */
    protected void finalizeConstruction() {
        // Attention! At this point, the DefaultObjectWrapper must be fully initialized, as when the model factories are
        // registered below, the DefaultObjectWrapper can immediately get concurrent callbacks. That those other threads will
        // see consistent image of the DefaultObjectWrapper is ensured that callbacks are always sync-ed on
        // classIntrospector.sharedLock, and so is classIntrospector.registerModelFactory(...).

        registerModelFactories();
    }

    Object getSharedIntrospectionLock() {
        return sharedIntrospectionLock;
    }

    /**
     * @see ExtendableBuilder#setStrict(boolean)
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * By default returns <tt>this</tt>.
     * @see ExtendableBuilder#setOuterIdentity(ObjectWrapper)
     */
    public ObjectWrapper getOuterIdentity() {
        return outerIdentity;
    }

    // I have commented this out, as it won't be in 2.3.20 yet.
    /*
    /**
     * Tells which non-backward-compatible overloaded method selection fixes to apply;
     * see {@link #setOverloadedMethodSelection(Version)}.
     * /
    public Version getOverloadedMethodSelection() {
        return overloadedMethodSelection;
    }

    /**
     * Sets which non-backward-compatible overloaded method selection fixes to apply.
     * This has similar logic as {@link Configuration#setIncompatibleImprovements(Version)},
     * but only applies to this aspect.
     *
     * Currently significant values:
     * <ul>
     *   <li>2.3.21: Completetlly rewritten overloaded method selection, fixes several issues with the old one.</li>
     * </ul>
     * /
    public void setOverloadedMethodSelection(Version version) {
        overloadedMethodSelection = version;
    }
    */

    /**
     */
    public int getExposureLevel() {
        return classIntrospector.getExposureLevel();
    }

    /**
     * Returns whether exposure of public instance fields of classes is
     * enabled. See {@link ExtendableBuilder#setExposeFields(boolean)} for details.
     * @return true if public instance fields are exposed, false otherwise.
     */
    public boolean isExposeFields() {
        return classIntrospector.getExposeFields();
    }

    public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return classIntrospector.getMethodAppearanceFineTuner();
    }

    MethodSorter getMethodSorter() {
        return classIntrospector.getMethodSorter();
    }

    /**
     * Tells if this instance acts like if its class introspection cache is sharable with other {@link DefaultObjectWrapper}-s.
     * A restricted cache denies certain too "antisocial" operations, like {@link #clearClassIntrospectionCache()}.
     * The value depends on how the instance
     * was created; with a public constructor (then this is {@code false}), or with {@link Builder}
     * (then it's {@code true}). Note that in the last case it's possible that the introspection cache
     * will not be actually shared because there's no one to share with, but this will {@code true} even then.
     */
    public boolean isClassIntrospectionCacheRestricted() {
        return classIntrospector.getHasSharedInstanceRestrictons();
    }

    private void registerModelFactories() {
        if (staticModels != null) {
            classIntrospector.registerModelFactory(staticModels);
        }
        if (enumModels != null) {
            classIntrospector.registerModelFactory(enumModels);
        }
    }

    /**
     * Returns the default date type. See {@link ExtendableBuilder#setDefaultDateType(int)} for
     * details.
     * @return the default date type
     */
    public int getDefaultDateType() {
        return defaultDateType;
    }

    /**
     * @deprecated Does nothing in FreeMarker 3 - we kept it for now to postopne reworking some JUnit tests.
     */
    // [FM3] Remove
    @Deprecated
    public boolean getUseModelCache() {
        return useModelCache;
    }

    /**
     * Returns the version given with {@link Builder (Version)}, normalized to the lowest version
     * where a change has occurred. Thus, this is not necessarily the same version than that was given to the
     * constructor.
     */
    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }

    /**
     * Wraps the parameter object to {@link TemplateModel} interface(s). The wrapping logic uses several phases,
     * where if a stage manages to wrap the object, this method immediately returns with the result. The stages are
     * (executed in this order):
     * <ol>
     * <li>If the value is {@code null} or {@link TemplateModel} it's returned as is.</li>
     * <li>If the value is a {@link TemplateModelAdapter}, {@link TemplateModelAdapter#getTemplateModel()} is
     * returned.</li>
     * <li>{@link ExtendableBuilder#extensions extensions} which subscribe to the
     * {@link DefaultObjectWrapperExtensionPhase#BEFORE_WRAP_SPECIAL_OBJECT} phase try to wrap the object</li>
     * <li>{@link #wrapSpecialObject(Object)} tries to wrap the object</li>
     * <li>{@link ExtendableBuilder#extensions extensions} which subscribe to the
     * {@link DefaultObjectWrapperExtensionPhase#AFTER_WRAP_SPECIAL_OBJECT} phase try to wrap the object</li>
     * <li>{@link #wrapGenericObject(Object)} wraps the object (or if it can't, it must throw exception)</li>
     * </ol>
     */
    @Override
    public TemplateModel wrap(Object obj) throws ObjectWrappingException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof TemplateModel) {
            return (TemplateModel) obj;
        }
        if (obj instanceof TemplateModelAdapter) {
            return ((TemplateModelAdapter) obj).getTemplateModel();
        }

        if (extensionsBeforeWrapSpecialObject != null) {
            for (DefaultObjectWrapperExtension ext : extensionsBeforeWrapSpecialObject) {
                TemplateModel tm = ext.wrap(obj);
                if (tm != null) {
                    return tm;
                }
            }
        }

        {
            TemplateModel tm = wrapSpecialObject(obj);
            if (tm != null) {
                return tm;
            }
        }

        if (extensionsAfterWrapSpecialObject != null) {
            for (DefaultObjectWrapperExtension ext : extensionsAfterWrapSpecialObject) {
                TemplateModel tm = ext.wrap(obj);
                if (tm != null) {
                    return tm;
                }
            }
        }

        return wrapGenericObject(obj);
    }

    /**
     * Wraps non-generic objects; see {@link #wrap(Object)} for more.
     *
     * @return {@code null} if the object was not of a type that's wrapped "specially".
     */
    protected TemplateModel wrapSpecialObject(Object obj) {
        if (obj instanceof String) {
            return new SimpleString((String) obj);
        }
        if (obj instanceof Number) {
            return new SimpleNumber((Number) obj);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Date) {
            if (obj instanceof java.sql.Date) {
                return new SimpleDate((java.sql.Date) obj);
            }
            if (obj instanceof java.sql.Time) {
                return new SimpleDate((java.sql.Time) obj);
            }
            if (obj instanceof java.sql.Timestamp) {
                return new SimpleDate((java.sql.Timestamp) obj);
            }
            return new SimpleDate((Date) obj, getDefaultDateType());
        }
        final Class<?> objClass = obj.getClass();
        if (objClass.isArray()) {
            return DefaultArrayAdapter.adapt(obj, this);
        }
        if (obj instanceof Collection) {
            return obj instanceof List
                    ? DefaultListAdapter.adapt((List<?>) obj, this)
                    : DefaultNonListCollectionAdapter.adapt((Collection<?>) obj, this);
        }
        if (obj instanceof Map) {
            return DefaultMapAdapter.adapt((Map<?, ?>) obj, this);
        }
        if (obj instanceof Iterator) {
            return DefaultIteratorAdapter.adapt((Iterator<?>) obj, this);
        }
        if (obj instanceof Iterable) {
            return DefaultIterableAdapter.adapt((Iterable<?>) obj, this);
        }
        if (obj instanceof Enumeration) {
            return DefaultEnumerationAdapter.adapt((Enumeration<?>) obj, this);
        }
        if (obj instanceof ResourceBundle) {
            return new ResourceBundleModel((ResourceBundle) obj, this);
        }
        return null;
    }

    /**
     * Called for an object that isn't treated specially by this {@link ObjectWrapper}; see {@link #wrap(Object)} for
     * more. The implementation in {@link DefaultObjectWrapper} wraps the object into a {@link BeanAndStringModel}.
     * <p>
     * Note that if you want to wrap some classes in a custom way, you shouldn't override this method. Instead, either
     * override {@link #wrapSpecialObject(Object)}}, or don't subclass the {@link ObjectWrapper} at all, and
     * just set the {@link ExtendableBuilder#getExtensions() extensions} configuration setting of it.
     */
    protected TemplateModel wrapGenericObject(Object obj) throws ObjectWrappingException {
        return new BeanAndStringModel(obj, this);
    }

    /**
     * Wraps a Java method so that it can be called from templates, without wrapping its parent ("this") object. The
     * result is almost the same as that you would get by wrapping the parent object then getting the method from the
     * resulting {@link TemplateHashModel} by name. Except, if the wrapped method is overloaded, with this method you
     * explicitly select an overload, while otherwise you would get a {@link OverloadedJavaMethodModel} that selects an
     * overload each time it's called based on the argument values.
     *
     * @param object The object whose method will be called, or {@code null} if {@code method} is a static method.
     *          This object will be used "as is", like without unwrapping it if it's a {@link TemplateModelAdapter}.
     * @param method The method to call, which must be an (inherited) member of the class of {@code object}, as
     *          described by {@link Method#invoke(Object, Object...)}
     */
    public TemplateFunctionModel wrap(Object object, Method method) {
        return new SimpleJavaMethodModel(object, method, method.getParameterTypes(), this);
    }

    /**
     */
    @Override
    public TemplateHashModel wrapAsAPI(Object obj) throws ObjectWrappingException {
        return new APIModel(obj, this);
    }

    /**
     * Attempts to unwrap a model into underlying object. Generally, this
     * method is the inverse of the {@link #wrap(Object)} method. In addition
     * it will unwrap arbitrary {@link TemplateNumberModel} instances into
     * a number, arbitrary {@link TemplateDateModel} instances into a date,
     * {@link TemplateStringModel} instances into a String, arbitrary
     * {@link TemplateBooleanModel} instances into a Boolean, arbitrary
     * {@link TemplateHashModel} instances into a Map, arbitrary
     * {@link TemplateSequenceModel} into a List, and arbitrary
     * {@link TemplateIterableModel} into a Set. All other objects are
     * returned unchanged.
     * @throws TemplateException if an attempted unwrapping fails.
     */
    @Override
    public Object unwrap(TemplateModel model) throws TemplateException {
        return unwrap(model, Object.class);
    }

    /**
     * Attempts to unwrap a model into an object of the desired class.
     * Generally, this method is the inverse of the {@link #wrap(Object)}
     * method. It recognizes a wide range of target classes - all Java built-in
     * primitives, primitive wrappers, numbers, dates, sets, lists, maps, and
     * native arrays.
     * @param model the model to unwrap
     * @param targetClass the class of the unwrapped result; {@code Object.class} of we don't know what the expected type is.
     * @return the unwrapped result of the desired class
     * @throws TemplateException if an attempted unwrapping fails.
     *
     * @see #tryUnwrapTo(TemplateModel, Class)
     */
    public Object unwrap(TemplateModel model, Class<?> targetClass)
            throws TemplateException {
        final Object obj = tryUnwrapTo(model, targetClass);
        if (obj == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
            throw new TemplateException("Can not unwrap model of type " +
                    model.getClass().getName() + " to type " + targetClass.getName());
        }
        return obj;
    }

    /**
     */
    @Override
    public Object tryUnwrapTo(TemplateModel model, Class<?> targetClass) throws TemplateException {
        return tryUnwrapTo(model, targetClass, 0);
    }

    /**
     * @param typeFlags
     *            Used when unwrapping for overloaded methods and so the {@code targetClass} is possibly too generic
     *            (as it's the most specific common superclass). Must be 0 when unwrapping parameter values for
     *            non-overloaded methods.
     * @return {@link ObjectWrapperAndUnwrapper#CANT_UNWRAP_TO_TARGET_CLASS} or the unwrapped object.
     */
    Object tryUnwrapTo(TemplateModel model, Class<?> targetClass, int typeFlags) throws TemplateException {
        Object res = tryUnwrapTo(model, targetClass, typeFlags, null);
        if ((typeFlags & TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT) != 0
                && res instanceof Number) {
            return OverloadedNumberUtils.addFallbackType((Number) res, typeFlags);
        } else {
            return res;
        }
    }

    /**
     * See {@link #tryUnwrapTo(TemplateModel, Class, int)}.
     */
    private Object tryUnwrapTo(final TemplateModel model, Class<?> targetClass, final int typeFlags,
                               final Map<Object, Object> recursionStops)
            throws TemplateException {
        if (model == null) {
            return null;
        }

        if (targetClass.isPrimitive()) {
            targetClass = _ClassUtils.primitiveClassToBoxingClass(targetClass);
        }

        // This is for transparent interop with other wrappers (and ourselves)
        // Passing the targetClass allows e.g. a Jython-aware method that declares a
        // PyObject as its argument to receive a PyObject from a Jython-aware TemplateModel
        // passed as an argument to TemplateFunctionModel etc.
        if (model instanceof AdapterTemplateModel) {
            Object wrapped = ((AdapterTemplateModel) model).getAdaptedObject(targetClass);
            if (targetClass == Object.class || targetClass.isInstance(wrapped)) {
                return wrapped;
            }

            // Attempt numeric conversion:
            if (targetClass != Object.class && (wrapped instanceof Number && _ClassUtils.isNumerical(targetClass))) {
                Number number = forceUnwrappedNumberToType((Number) wrapped, targetClass);
                if (number != null) return number;
            }
        }

        if (model instanceof WrapperTemplateModel) {
            Object wrapped = ((WrapperTemplateModel) model).getWrappedObject();
            if (targetClass == Object.class || targetClass.isInstance(wrapped)) {
                return wrapped;
            }

            // Attempt numeric conversion:
            if (targetClass != Object.class && (wrapped instanceof Number && _ClassUtils.isNumerical(targetClass))) {
                Number number = forceUnwrappedNumberToType((Number) wrapped, targetClass);
                if (number != null) {
                    return number;
                }
            }
        }

        // Translation of generic template models to POJOs. First give priority
        // to various model interfaces based on the targetClass. This helps us
        // select the appropriate interface in multi-interface models when we
        // know what is expected as the return type.
        if (targetClass != Object.class) {

            // [2.4][IcI]: Should also check for CharSequence at the end
            if (String.class == targetClass) {
                if (model instanceof TemplateStringModel) {
                    return ((TemplateStringModel) model).getAsString();
                }
                // String is final, so no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }

            // Primitive numeric types & Number.class and its subclasses
            if (_ClassUtils.isNumerical(targetClass)) {
                if (model instanceof TemplateNumberModel) {
                    Number number = forceUnwrappedNumberToType(
                            ((TemplateNumberModel) model).getAsNumber(), targetClass);
                    if (number != null) {
                        return number;
                    }
                }
            }

            if (boolean.class == targetClass || Boolean.class == targetClass) {
                if (model instanceof TemplateBooleanModel) {
                    return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
                }
                // Boolean is final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }

            if (Map.class == targetClass) {
                if (model instanceof TemplateHashModel) {
                    return new TemplateHashModelAdapter((TemplateHashModel) model, this);
                }
            }

            if (List.class == targetClass) {
                if (model instanceof TemplateSequenceModel) {
                    return new TemplateSequenceModelAdapter((TemplateSequenceModel) model, this);
                }
            }

            if (Set.class == targetClass) {
                if (model instanceof TemplateCollectionModel) {
                    return new TemplateSetModelAdapter((TemplateCollectionModel) model, this);
                }
            }

            if (Collection.class == targetClass) {
                if (model instanceof TemplateSequenceModel) {
                    return new TemplateSequenceModelAdapter((TemplateSequenceModel) model, this);
                }
                if (model instanceof TemplateCollectionModel) {
                    return new TemplateCollectionModelAdapter((TemplateCollectionModel) model, this);
                }
            }

            if (Iterable.class == targetClass) {
                if (model instanceof TemplateSequenceModel) {
                    return new TemplateSequenceModelAdapter((TemplateSequenceModel) model, this);
                }
                if (model instanceof TemplateCollectionModel) {
                    return new TemplateCollectionModelAdapter((TemplateCollectionModel) model, this);
                }
                if (model instanceof TemplateIterableModel) {
                    return new TemplateIterableModelAdapter((TemplateIterableModel) model,
                            this);
                }
            }

            // TemplateSequenceModels can be converted to arrays
            if (targetClass.isArray()) {
                if (model instanceof TemplateSequenceModel) {
                    return unwrapSequenceToArray((TemplateSequenceModel) model, targetClass, true, recursionStops);
                }
                // array classes are final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }

            // Allow one-char strings to be coerced to characters
            if (char.class == targetClass || targetClass == Character.class) {
                if (model instanceof TemplateStringModel) {
                    String s = ((TemplateStringModel) model).getAsString();
                    if (s.length() == 1) {
                        return Character.valueOf(s.charAt(0));
                    }
                }
                // Character is final, no other conversion will work
                return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
            }

            if (Date.class.isAssignableFrom(targetClass) && model instanceof TemplateDateModel) {
                Date date = ((TemplateDateModel) model).getAsDate();
                if (targetClass.isInstance(date)) {
                    return date;
                }
            }
        }  //  End: if (targetClass != Object.class)

        // Since the targetClass was of no help initially, now we use
        // a quite arbitrary order in which we walk through the TemplateModel subinterfaces, and unwrapp them to
        // their "natural" Java correspondent. We still try exclude unwrappings that won't fit the target parameter
        // type(s). This is mostly important because of multi-typed FTL values that could be unwrapped on multiple ways.
        int itf = typeFlags; // Iteration's Type Flags. Should be always 0 for non-overloaded and when !is2321Bugfixed.
        // If itf != 0, we possibly execute the following loop body at twice: once with utilizing itf, and if it has not
        // returned, once more with itf == 0. Otherwise we execute this once with itf == 0.
        do {
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_NUMBER) != 0)
                    && model instanceof TemplateNumberModel) {
                Number number = ((TemplateNumberModel) model).getAsNumber();
                if (itf != 0 || targetClass.isInstance(number)) {
                    return number;
                }
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_DATE) != 0)
                    && model instanceof TemplateDateModel) {
                Date date = ((TemplateDateModel) model).getAsDate();
                if (itf != 0 || targetClass.isInstance(date)) {
                    return date;
                }
            }
            if ((itf == 0 || (itf & (TypeFlags.ACCEPTS_STRING | TypeFlags.CHARACTER)) != 0)
                    && model instanceof TemplateStringModel
                    && (itf != 0 || targetClass.isAssignableFrom(String.class))) {
                String strVal = ((TemplateStringModel) model).getAsString();
                if (itf == 0 || (itf & TypeFlags.CHARACTER) == 0) {
                    return strVal;
                } else { // TypeFlags.CHAR == 1
                    if (strVal.length() == 1) {
                        if ((itf & TypeFlags.ACCEPTS_STRING) != 0) {
                            return new CharacterOrString(strVal);
                        } else {
                            return Character.valueOf(strVal.charAt(0));
                        }
                    } else if ((itf & TypeFlags.ACCEPTS_STRING) != 0) {
                        return strVal;
                    }
                    // It had to be unwrapped to Character, but the string length wasn't 1 => Fall through
                }
            }
            // Should be earlier than TemplateStringModel, but we keep it here until FM 2.4 or such
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_BOOLEAN) != 0)
                    && model instanceof TemplateBooleanModel
                    && (itf != 0 || targetClass.isAssignableFrom(Boolean.class))) {
                return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_MAP) != 0)
                    && model instanceof TemplateHashModel
                    && (itf != 0 || targetClass.isAssignableFrom(TemplateHashModelAdapter.class))) {
                return new TemplateHashModelAdapter((TemplateHashModel) model, this);
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_LIST) != 0)
                    && model instanceof TemplateSequenceModel
                    && (itf != 0 || targetClass.isAssignableFrom(TemplateSequenceModelAdapter.class))) {
                return new TemplateSequenceModelAdapter((TemplateSequenceModel) model, this);
            }
            if ((itf == 0 || (itf & TypeFlags.ACCEPTS_SET) != 0)
                    && model instanceof TemplateCollectionModel
                    && (itf != 0 || targetClass.isAssignableFrom(TemplateSetModelAdapter.class))) {
                return new TemplateSetModelAdapter((TemplateCollectionModel) model, this);
            }

            if ((itf & TypeFlags.ACCEPTS_ARRAY) != 0
                    && model instanceof TemplateSequenceModel) {
                return new TemplateSequenceModelAdapter((TemplateSequenceModel) model, this);
            }

            if (itf == 0) {
                break;
            }
            itf = 0; // start 2nd iteration
        } while (true);

        // Last ditch effort - is maybe the model itself is an instance of the required type?
        // Note that this will be always true for Object.class targetClass.
        if (targetClass.isInstance(model)) {
            return model;
        }

        return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
    }

    /**
     * @param tryOnly
     *            If {@code true}, if the conversion of an item to the component type isn't possible, the method returns
     *            {@link ObjectWrapperAndUnwrapper#CANT_UNWRAP_TO_TARGET_CLASS} instead of throwing a
     *            {@link TemplateException}.
     */
    Object unwrapSequenceToArray(
            TemplateSequenceModel seq, Class<?> arrayClass, boolean tryOnly, Map<Object, Object> recursionStops)
            throws TemplateException {
        if (recursionStops != null) {
            Object retval = recursionStops.get(seq);
            if (retval != null) {
                return retval;
            }
        } else {
            recursionStops = new IdentityHashMap<>();
        }
        Class<?> componentType = arrayClass.getComponentType();
        final int size = seq.getCollectionSize();
        Object array = Array.newInstance(componentType, size);
        recursionStops.put(seq, array);
        try {
            TemplateModelIterator iter = seq.iterator();
            for (int idx = 0; idx < size; idx++) {
                final TemplateModel seqItem = iter.next();
                Object val = tryUnwrapTo(seqItem, componentType, 0, recursionStops);
                if (val == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    if (tryOnly) {
                        return ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS;
                    } else {
                        throw new TemplateException(
                                "Failed to convert ",  new _DelayedTemplateLanguageTypeDescription(seq),
                                " object to ", new _DelayedShortClassName(array.getClass()),
                                ": Problematic sequence item at index ", Integer.valueOf(idx) ," with value type: ",
                                new _DelayedTemplateLanguageTypeDescription(seqItem));
                    }

                }
                Array.set(array, idx, val);
            }
        } finally {
            recursionStops.remove(seq);
        }
        return array;
    }

    Object listToArray(List<?> list, Class<?> arrayClass, Map<Object, Object> recursionStops)
            throws TemplateException {
        if (list instanceof TemplateSequenceModelAdapter) {
            return unwrapSequenceToArray(
                    ((TemplateSequenceModelAdapter) list).getTemplateSequenceModel(),
                    arrayClass, false,
                    recursionStops);
        }

        if (recursionStops != null) {
            Object retval = recursionStops.get(list);
            if (retval != null) {
                return retval;
            }
        } else {
            recursionStops = new IdentityHashMap<>();
        }
        Class<?> componentType = arrayClass.getComponentType();
        Object array = Array.newInstance(componentType, list.size());
        recursionStops.put(list, array);
        try {
            boolean isComponentTypeExamined = false;
            boolean isComponentTypeNumerical = false;  // will be filled on demand
            boolean isComponentTypeList = false;  // will be filled on demand
            int i = 0;
            for (Object listItem : list) {
                if (listItem != null && !componentType.isInstance(listItem)) {
                    // Type conversion is needed. If we can't do it, we just let it fail at Array.set later.
                    if (!isComponentTypeExamined) {
                        isComponentTypeNumerical = _ClassUtils.isNumerical(componentType);
                        isComponentTypeList = List.class.isAssignableFrom(componentType);
                        isComponentTypeExamined = true;
                    }
                    if (isComponentTypeNumerical && listItem instanceof Number) {
                        listItem = forceUnwrappedNumberToType((Number) listItem, componentType);
                    } else if (componentType == String.class && listItem instanceof Character) {
                        listItem = String.valueOf(((Character) listItem).charValue());
                    } else if ((componentType == Character.class || componentType == char.class)
                            && listItem instanceof String) {
                        String listItemStr = (String) listItem;
                        if (listItemStr.length() == 1) {
                            listItem = Character.valueOf(listItemStr.charAt(0));
                        }
                    } else if (componentType.isArray()) {
                        if (listItem instanceof List) {
                            listItem = listToArray((List<?>) listItem, componentType, recursionStops);
                        } else if (listItem instanceof TemplateSequenceModel) {
                            listItem = unwrapSequenceToArray((TemplateSequenceModel) listItem, componentType, false, recursionStops);
                        }
                    } else if (isComponentTypeList && listItem.getClass().isArray()) {
                        listItem = arrayToList(listItem);
                    }
                }
                try {
                    Array.set(array, i, listItem);
                } catch (IllegalArgumentException e) {
                    throw new TemplateException(
                            "Failed to convert " + _ClassUtils.getShortClassNameOfObject(list)
                                    + " object to " + _ClassUtils.getShortClassNameOfObject(array)
                                    + ": Problematic List item at index " + i + " with value type: "
                                    + _ClassUtils.getShortClassNameOfObject(listItem), e);
                }
                i++;
            }
        } finally {
            recursionStops.remove(list);
        }
        return array;
    }

    /**
     * @param array Must be an array (of either a reference or primitive type)
     */
    List<?> arrayToList(Object array) throws TemplateException {
        if (array instanceof Object[]) {
            // Array of any non-primitive type.
            // Note that an array of non-primitive type is always instanceof Object[].
            Object[] objArray = (Object[]) array;
            return objArray.length == 0 ? Collections.EMPTY_LIST : new NonPrimitiveArrayBackedReadOnlyList(objArray);
        } else {
            // Array of any primitive type
            return Array.getLength(array) == 0 ? Collections.EMPTY_LIST : new PrimtiveArrayBackedReadOnlyList(array);
        }
    }

    /**
     * Converts a number to the target type aggressively (possibly with overflow or significant loss of precision).
     * @param n Non-{@code null}
     * @return {@code null} if the conversion has failed.
     */
    static Number forceUnwrappedNumberToType(final Number n, final Class<?> targetType) {
        // We try to order the conditions by decreasing probability.
        if (targetType == n.getClass()) {
            return n;
        } else if (targetType == int.class || targetType == Integer.class) {
            return n instanceof Integer ? (Integer) n : Integer.valueOf(n.intValue());
        } else if (targetType == long.class || targetType == Long.class) {
            return n instanceof Long ? (Long) n : Long.valueOf(n.longValue());
        } else if (targetType == double.class || targetType == Double.class) {
            return n instanceof Double ? (Double) n : Double.valueOf(n.doubleValue());
        } else if (targetType == BigDecimal.class) {
            if (n instanceof BigDecimal) {
                return n;
            } else if (n instanceof BigInteger) {
                return new BigDecimal((BigInteger) n);
            } else if (n instanceof Long) {
                // Because we can't represent long accurately as double
                return BigDecimal.valueOf(n.longValue());
            } else {
                return new BigDecimal(n.doubleValue());
            }
        } else if (targetType == float.class || targetType == Float.class) {
            return n instanceof Float ? (Float) n : Float.valueOf(n.floatValue());
        } else if (targetType == byte.class || targetType == Byte.class) {
            return n instanceof Byte ? (Byte) n : Byte.valueOf(n.byteValue());
        } else if (targetType == short.class || targetType == Short.class) {
            return n instanceof Short ? (Short) n : Short.valueOf(n.shortValue());
        } else if (targetType == BigInteger.class) {
            if (n instanceof BigInteger) {
                return n;
            } else {
                if (n instanceof OverloadedNumberUtils.IntegerBigDecimal) {
                    return ((OverloadedNumberUtils.IntegerBigDecimal) n).bigIntegerValue();
                } else if (n instanceof BigDecimal) {
                    return ((BigDecimal) n).toBigInteger();
                } else {
                    return BigInteger.valueOf(n.longValue());
                }
            }
        } else {
            final Number oriN = n instanceof OverloadedNumberUtils.NumberWithFallbackType
                    ? ((OverloadedNumberUtils.NumberWithFallbackType) n).getSourceNumber() : n;
            if (targetType.isInstance(oriN)) {
                // Handle nonstandard Number subclasses as well as directly java.lang.Number.
                return oriN;
            } else {
                // Fails
                return null;
            }
        }
    }

    /**
     * Invokes the specified method, wrapping the return value. The specialty
     * of this method is that if the return value is null, and the return type
     * of the invoked method is void, an empty string is returned.
     * @param object the object to invoke the method on
     * @param method the method to invoke
     * @param args the arguments to the method
     * @return the wrapped return value of the method.
     * @throws InvocationTargetException if the invoked method threw an exception
     * @throws IllegalAccessException if the method can't be invoked due to an
     * access restriction.
     * @throws TemplateException if the return value couldn't be wrapped
     * (this can happen if the wrapper has an outer identity or is subclassed,
     * and the outer identity or the subclass throws an exception. Plain
     * DefaultObjectWrapper never throws TemplateException).
     */
    TemplateModel invokeMethod(Object object, Method method, Object[] args)
            throws InvocationTargetException, IllegalAccessException, TemplateException {
        // [2.4]: Java's Method.invoke truncates numbers if the target type has not enough bits to hold the value.
        // There should at least be an option to check this.
        Object retval = method.invoke(object, args);
        return
                method.getReturnType() == void.class
                        ? TemplateStringModel.EMPTY_STRING
                        : getOuterIdentity().wrap(retval);
    }

    /**
     * Returns a hash model that represents the so-called class static models.
     * Every class static model is itself a hash through which you can call
     * static methods on the specified class. To obtain a static model for a
     * class, get the element of this hash with the fully qualified class name.
     * For example, if you place this hash model inside the root data model
     * under name "statics", you can use i.e. <code>statics["java.lang.
     * System"]. currentTimeMillis()</code> to call the {@link
     * java.lang.System#currentTimeMillis()} method.
     * @return a hash model whose keys are fully qualified class names, and
     * that returns hash models whose elements are the static models of the
     * classes.
     */
    public TemplateHashModel getStaticModels() {
        return staticModels;
    }

    /**
     * Returns a hash model that represents the so-called class enum models.
     * Every class' enum model is itself a hash through which you can access
     * enum value declared by the specified class, assuming that class is an
     * enumeration. To obtain an enum model for a class, get the element of this
     * hash with the fully qualified class name. For example, if you place this
     * hash model inside the root data model under name "enums", you can use
     * i.e. <code>enums["java.math.RoundingMode"].UP</code> to access the
     * {@link java.math.RoundingMode#UP} value.
     * @return a hash model whose keys are fully qualified class names, and
     * that returns hash models whose elements are the enum models of the
     * classes.
     */
    public TemplateHashModel getEnumModels() {
        return enumModels;
    }

    /**
     * Creates a new instance of the specified class using the method call logic of this object wrapper for calling the
     * constructor. Overloaded constructors and varargs are supported. Only public constructors will be called.
     *
     * @param clazz The class whose constructor we will call.
     * @param args The list of {@link TemplateModel}-s to pass to the constructor after unwrapping them
     * @param callPlace Where the constructor is called from (which may contains information useful for overloaded
     *                  constructor selection); you may want to use {@link NonTemplateCallPlace#INSTANCE}.
     *                 if you call this from Java code.
     * @return The instance created; it's not wrapped into {@link TemplateModel}.
     */
    public Object newInstance(Class<?> clazz, TemplateModel[] args, CallPlace callPlace)
            throws TemplateException {
        try {
            Object ctors = classIntrospector.get(clazz).get(ClassIntrospector.CONSTRUCTORS_KEY);
            if (ctors == null) {
                throw new TemplateException("Class " + clazz.getName() + " has no public constructors.");
            }
            Constructor<?> ctor = null;
            Object[] pojoArgs;
            if (ctors instanceof SimpleMethod) {
                SimpleMethod sm = (SimpleMethod) ctors;
                ctor = (Constructor<?>) sm.getMember();
                pojoArgs = sm.unwrapArguments(args, this);
                try {
                    return ctor.newInstance(pojoArgs);
                } catch (Exception e) {
                    throw _MethodUtils.newInvocationTemplateException(null, ctor, e);
                }
            } else if (ctors instanceof OverloadedMethods) {
                // TODO [FM3] Utilize optional java type info in callPlace for overloaded method selection
                final MemberAndArguments mma = ((OverloadedMethods) ctors).getMemberAndArguments(args, this);
                try {
                    return mma.invokeConstructor(this);
                } catch (Exception e) {
                    if (e instanceof TemplateException) throw (TemplateException) e;

                    throw _MethodUtils.newInvocationTemplateException(null, mma.getCallableMemberDescriptor(), e);
                }
            } else {
                // Cannot happen
                throw new BugException();
            }
        } catch (TemplateException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateException(
                    "Error while creating new instance of class " + clazz.getName() + "; see cause exception", e);
        }
    }

    /**
     * Removes the introspection data for a class from the cache.
     * Use this if you know that a class is not used anymore in templates.
     * If the class will be still used, the cache entry will be silently
     * re-created, so this isn't a dangerous operation.
     */
    public void removeFromClassIntrospectionCache(Class<?> clazz) {
        classIntrospector.remove(clazz);
    }

    /**
     * Removes all class introspection data from the cache.
     *
     * <p>Use this if you want to free up memory on the expense of recreating
     * the cache entries for the classes that will be used later in templates.
     *
     * @throws IllegalStateException if {@link #isClassIntrospectionCacheRestricted()} is {@code true}.
     */
    public void clearClassIntrospectionCache() {
        classIntrospector.clearCache();
    }

    ClassIntrospector getClassIntrospector() {
        return classIntrospector;
    }

    /**
     * Converts any {@link BigDecimal}s in the passed array to the type of
     * the corresponding formal argument of the method.
     */
    // Unused?
    public static void coerceBigDecimals(AccessibleObject callable, Object[] args) {
        Class<?>[] formalTypes = null;
        for (int i = 0; i < args.length; ++i) {
            Object arg = args[i];
            if (arg instanceof BigDecimal) {
                if (formalTypes == null) {
                    if (callable instanceof Method) {
                        formalTypes = ((Method) callable).getParameterTypes();
                    } else if (callable instanceof Constructor) {
                        formalTypes = ((Constructor<?>) callable).getParameterTypes();
                    } else {
                        throw new IllegalArgumentException("Expected method or "
                                + " constructor; callable is " +
                                callable.getClass().getName());
                    }
                }
                args[i] = coerceBigDecimal((BigDecimal) arg, formalTypes[i]);
            }
        }
    }

    /**
     * Converts any {@link BigDecimal}-s in the passed array to the type of
     * the corresponding formal argument of the method via {@link #coerceBigDecimal(BigDecimal, Class)}.
     */
    public static void coerceBigDecimals(Class<?>[] formalTypes, Object[] args) {
        int typeLen = formalTypes.length;
        int argsLen = args.length;
        int min = Math.min(typeLen, argsLen);
        for (int i = 0; i < min; ++i) {
            Object arg = args[i];
            if (arg instanceof BigDecimal) {
                args[i] = coerceBigDecimal((BigDecimal) arg, formalTypes[i]);
            }
        }
        if (argsLen > typeLen) {
            Class<?> varArgType = formalTypes[typeLen - 1];
            for (int i = typeLen; i < argsLen; ++i) {
                Object arg = args[i];
                if (arg instanceof BigDecimal) {
                    args[i] = coerceBigDecimal((BigDecimal) arg, varArgType);
                }
            }
        }
    }

    /**
     * Converts {@link BigDecimal} to the class given in the {@code formalType} argument if that's a known numerical
     * type, returns the {@link BigDecimal} as is otherwise. Overflow and precision loss are possible, similarly as
     * with casting in Java.
     */
    public static Object coerceBigDecimal(BigDecimal bd, Class<?> formalType) {
        // int is expected in most situations, so we check it first
        if (formalType == int.class || formalType == Integer.class) {
            return Integer.valueOf(bd.intValue());
        } else if (formalType == double.class || formalType == Double.class) {
            return Double.valueOf(bd.doubleValue());
        } else if (formalType == long.class || formalType == Long.class) {
            return Long.valueOf(bd.longValue());
        } else if (formalType == float.class || formalType == Float.class) {
            return Float.valueOf(bd.floatValue());
        } else if (formalType == short.class || formalType == Short.class) {
            return Short.valueOf(bd.shortValue());
        } else if (formalType == byte.class || formalType == Byte.class) {
            return Byte.valueOf(bd.byteValue());
        } else if (java.math.BigInteger.class.isAssignableFrom(formalType)) {
            return bd.toBigInteger();
        } else {
            return bd;
        }
    }

    /**
     * Returns the lowest version number that is equivalent with the parameter version.
     */
    protected static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
        _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        return Configuration.VERSION_3_0_0;
    }


    /**
     * Returns the name-value pairs that describe the configuration of this {@link DefaultObjectWrapper}; called from
     * {@link #toString()}. The expected format is like {@code "foo=bar, baaz=wombat"}. When overriding this, you should
     * call the super method, and then insert the content before it with a following {@code ", "}, or after it with a
     * preceding {@code ", "}.
     */
    protected String toPropertiesString() {
        // Start with "simpleMapWrapper", because the override in DefaultObjectWrapper expects it to be there!
        return "exposureLevel=" + classIntrospector.getExposureLevel() + ", "
                + "exposeFields=" + classIntrospector.getExposeFields() + ", "
                + "sharedClassIntrospCache="
                + (classIntrospector.isShared() ? "@" + System.identityHashCode(classIntrospector) : "none");
    }

    /**
     * Returns the exact class name and the identity hash, also the values of the most often used
     * {@link DefaultObjectWrapper} configuration properties, also if which (if any) shared class introspection
     * cache it uses.
     */
    @Override
    public String toString() {
        final String propsStr = toPropertiesString();
        return _ClassUtils.getShortClassNameOfObject(this) + "@" + System.identityHashCode(this)
                + "(" + incompatibleImprovements + ", "
                + (propsStr.length() != 0 ? propsStr + ", ..." : "")
                + ")";
    }

    /**
     * Gets/creates a {@link DefaultObjectWrapper} singleton instance that's already configured as specified in the
     * properties of this object; this is recommended over using the {@link DefaultObjectWrapper} constructors. The
     * returned instance can't be further configured (it's write protected).
     *
     * <p>The builder meant to be used as a drop-away object (not stored in a field), like in this example:
     * <pre>
     *    DefaultObjectWrapper dow = new Builder(Configuration.VERSION_3_0_0).build();
     * </pre>
     *
     * <p>Or, a more complex example:</p>
     * <pre>
     *    // Create the builder:
     *    DefaultObjectWrapper dow = new Builder(Configuration.VERSION_3_0_0)
     *            .exposeFields(true)
     *            .build();
     * </pre>
     *
     * <p>Despite that builders aren't meant to be used as long-lived objects (singletons), the builder is thread-safe after
     * you have stopped calling its setters and it was safely published (see JSR 133) to other threads. This can be useful
     * if you have to put the builder into an IoC container, rather than the singleton it produces.
     *
     * <p>The main benefit of using a builder instead of a {@link DefaultObjectWrapper} constructor is that this way the
     * internal object wrapping-related caches (most notably the class introspection cache) will come from a global,
     * JVM-level (more precisely, {@code freemarker-core.jar}-class-loader-level) cache. Also the
     * {@link DefaultObjectWrapper} singletons
     * themselves are stored in this global cache. Some of the wrapping-related caches are expensive to build and can take
     * significant amount of memory. Using builders, components that use FreeMarker will share {@link DefaultObjectWrapper}
     * instances and said caches even if they use separate FreeMarker {@link Configuration}-s. (Many Java libraries use
     * FreeMarker internally, so {@link Configuration} sharing is not an option.)
     *
     * <p>Note that the returned {@link DefaultObjectWrapper} instances are only weak-referenced from inside the builder mechanism,
     * so singletons are garbage collected when they go out of usage, just like non-singletons.
     *
     * <p>About the object wrapping-related caches:
     * <ul>
     *   <li><p>Class introspection cache: Stores information about classes that once had to be wrapped. The cache is
     *     stored in the static fields of certain FreeMarker classes. Thus, if you have two {@link DefaultObjectWrapper}
     *     instances, they might share the same class introspection cache. But if you have two
     *     {@code freemarker.jar}-s (typically, in two Web Application's {@code WEB-INF/lib} directories), those won't
     *     share their caches (as they don't share the same FreeMarker classes).
     *     Also, currently there's a separate cache for each permutation of the property values that influence class
     *     introspection: {@link Builder#setExposeFields(boolean) expose_fields} and
     *     {@link Builder#setExposureLevel(int) exposure_level}. So only {@link DefaultObjectWrapper} where those
     *     properties are the same may share class introspection caches among each other.
     *   </li>
     *   <li><p>Model caches: These are local to a {@link DefaultObjectWrapper}. {@link Builder} returns the same
     *     {@link DefaultObjectWrapper} instance for equivalent properties (unless the existing instance was garbage collected
     *     and thus a new one had to be created), hence these caches will be re-used too. {@link DefaultObjectWrapper} instances
     *     are cached in the static fields of FreeMarker too, but there's a separate cache for each
     *     Thread Context Class Loader, which in a servlet container practically means a separate cache for each Web
     *     Application (each servlet context). (This is like so because for resolving class names to classes FreeMarker
     *     uses the Thread Context Class Loader, so the result of the resolution can be different for different
     *     Thread Context Class Loaders.) The model caches are:
     *     <ul>
     *       <li><p>
     *         Static model caches: These are used by the hash returned by {@link DefaultObjectWrapper#getEnumModels()} and
     *         {@link DefaultObjectWrapper#getStaticModels()}, for caching {@link TemplateModel}-s for the static methods/fields
     *         and Java enums that were accessed through them. To use said hashes, you have to put them
     *         explicitly into the data-model or expose them to the template explicitly otherwise, so in most applications
     *         these caches aren't unused.
     *       </li>
     *       <li><p>
     *         Instance model cache: By default off (see {@link ExtendableBuilder#setUseModelCache(boolean)}). Caches the
     *         {@link TemplateModel}-s for all Java objects that were accessed from templates.
     *       </li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>Note that what this method documentation says about {@link DefaultObjectWrapper} also applies to
     * {@link Builder}.
     */
    public static final class Builder extends ExtendableBuilder<DefaultObjectWrapper, Builder> {

        private final static Map<ClassLoader, Map<Builder, WeakReference<DefaultObjectWrapper>>>
                INSTANCE_CACHE = new WeakHashMap<>();
        private final static ReferenceQueue<DefaultObjectWrapper> INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue<>();

        private boolean alreadyBuilt;

        /**
         * See {@link ExtendableBuilder#ExtendableBuilder(Version, boolean)}
         */
        public Builder(Version incompatibleImprovements) {
            super(incompatibleImprovements, false);
        }

        /** For unit testing only */
        static void clearInstanceCache() {
            synchronized (INSTANCE_CACHE) {
                INSTANCE_CACHE.clear();
            }
        }

        /**
         * Returns a {@link DefaultObjectWrapper} instance that matches the settings of this builder. This will be possibly
         * a singleton that is also in use elsewhere.
         */
        @Override
        public DefaultObjectWrapper build() {
            if (alreadyBuilt) {
                throw new IllegalStateException("build() can only be executed once.");
            }

            DefaultObjectWrapper singleton = DefaultObjectWrapperTCCLSingletonUtils.getSingleton(
                    this, INSTANCE_CACHE, INSTANCE_CACHE_REF_QUEUE, ConstructorInvoker.INSTANCE);
            alreadyBuilt = true;
            return singleton;
        }

        /**
         * For unit testing only
         */
        static Map<ClassLoader, Map<Builder, WeakReference<DefaultObjectWrapper>>> getInstanceCache() {
            return INSTANCE_CACHE;
        }

        private static class ConstructorInvoker
            implements DefaultObjectWrapperTCCLSingletonUtils._ConstructorInvoker<DefaultObjectWrapper, Builder> {

            private static final ConstructorInvoker INSTANCE = new ConstructorInvoker();

            @Override
            public DefaultObjectWrapper invoke(Builder builder) {
                return new DefaultObjectWrapper(builder, true);
            }
        }

    }

    /**
     * You will not use this abstract class directly, but concrete subclasses like {@link Builder}, unless you are
     * developing a builder for a custom {@link DefaultObjectWrapper} subclass. In that case, note that overriding the
     * {@link #equals} and {@link #hashCode} is important, as these objects are used as {@link ObjectWrapper} singleton
     * lookup keys.
     */
    protected abstract static class ExtendableBuilder<
            ProductT extends DefaultObjectWrapper, SelfT extends ExtendableBuilder<ProductT, SelfT>>
            implements CommonBuilder<ProductT>, Cloneable {

        private final Version incompatibleImprovements;

        // Can't be final because deep cloning must replace it
        private ClassIntrospector.Builder classIntrospectorBuilder;

        // Properties and their *defaults*:
        private int defaultDateType = TemplateDateModel.UNKNOWN;
        private boolean defaultDataTypeSet;
        private ObjectWrapper outerIdentity;
        private boolean outerIdentitySet;
        private boolean strict;
        private boolean strictSet;
        private boolean useModelCache;
        private boolean useModelCacheSet;
        private boolean usePrivateCaches;
        private boolean usePrivateCachesSet;
        private List<DefaultObjectWrapperExtension> extensions = Collections.emptyList();
        private boolean extensionsSet;
        // Attention!
        // - As this object is a cache key, non-normalized field values should be avoided.
        // - Fields with default values must be set until the end of the constructor to ensure that when the lookup
        //   happens, there will be no unset fields.
        // - If you add a new field, review all methods in this class

        /**
         * @param incompatibleImprovements
         *         Sets which of the non-backward-compatible improvements should be enabled. Not {@code null}. This
         *         version number is the same as the FreeMarker version number with which the improvements were
         *         implemented.
         *         <p>
         *         For new projects, it's recommended to set this to the FreeMarker version that's used during the
         *         development. For released products that are still actively developed it's a low risk change to
         *         increase the 3rd version number further as FreeMarker is updated, but of course you should always
         *         check the list of effects below. Increasing the 2nd or 1st version number possibly mean substantial
         *         changes with higher risk of breaking the application, but again, see the list of effects below.
         *         <p>
         *         The reason it's separate from {@link Configuration#getIncompatibleImprovements()} is that
         *         {@link ObjectWrapper} objects are sometimes shared among multiple {@link Configuration}-s, so the two
         *         version numbers are technically independent. But it's recommended to keep those two version numbers
         *         the same.
         *         <p>
         *         The changes enabled by {@code incompatibleImprovements} are:
         *         <ul>
         *             <li><p>3.0.0: No changes; this is the starting point, the version used in older projects.</li>
         *         </ul>
         *         <p>
         *         Note that the version will be normalized to the lowest version where the same incompatible {@link
         *         DefaultObjectWrapper} improvements were already present, so {@link #getIncompatibleImprovements()}
         *         might returns a lower version than what you have specified.
         * @param isIncompImprsAlreadyNormalized
         *         Tells if the {@code incompatibleImprovements} parameter contains an <em>already normalized</em>
         *         value. This parameter meant to be {@code true} when the class that extends {@link
         *         DefaultObjectWrapper} needs to add additional breaking versions over those of {@link
         *         DefaultObjectWrapper}. Thus, if this parameter is {@code true}, the versions where {@link
         *         DefaultObjectWrapper} had breaking changes must be already factored into the {@code
         *         incompatibleImprovements} parameter value, as no more normalization will happen. (You can use {@link
         *         DefaultObjectWrapper#normalizeIncompatibleImprovementsVersion(Version)} to discover those.)
         */
        protected ExtendableBuilder(Version incompatibleImprovements, boolean isIncompImprsAlreadyNormalized) {
            _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);

            incompatibleImprovements = isIncompImprsAlreadyNormalized
                    ? incompatibleImprovements
                    : normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
            this.incompatibleImprovements = incompatibleImprovements;

            classIntrospectorBuilder = new ClassIntrospector.Builder(incompatibleImprovements);
        }

        @SuppressWarnings("unchecked")
        protected SelfT self() {
            return (SelfT) this;
        }

        /**
         * Calculate a content-based hash that could be used when looking up the product object that {@link #build()}
         * returns from a cache. If you override {@link ExtendableBuilder} and add new fields, don't forget to take
         * those into account too!
         *
         * @see #equals(Object)
         * @see #cloneForCacheKey()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getIncompatibleImprovements().hashCode();
            result = prime * result + getDefaultDateType();
            result = prime * result + (getOuterIdentity() != null ? getOuterIdentity().hashCode() : 0);
            result = prime * result + (isStrict() ? 1231 : 1237);
            result = prime * result + (getUseModelCache() ? 1231 : 1237);
            result = prime * result + (getUsePrivateCaches() ? 1231 : 1237);
            result = prime * result + classIntrospectorBuilder.hashCode();
            result = prime * result + getExtensions().hashCode();
            return result;
        }
        
        /**
         * A content-based {@link Object#equals(Object)} that could be used to look up the product object that
         * {@link #build()} returns from a cache. If you override {@link ExtendableBuilder} and add new fields, don't
         * forget to take those into account too!
         *
         * @see #hashCode()
         * @see #cloneForCacheKey()
         */
        @Override
        public boolean equals(Object thatObj) {
            if (this == thatObj) return true;
            if (thatObj == null) return false;
            if (getClass() != thatObj.getClass()) return false;
            ExtendableBuilder<?, ?> thatBuilder = (ExtendableBuilder<?, ?>) thatObj;

            if (!getIncompatibleImprovements().equals(thatBuilder.getIncompatibleImprovements())) {
                return false;
            }
            if (getDefaultDateType() != thatBuilder.getDefaultDateType()) return false;
            if (getOuterIdentity() != thatBuilder.getOuterIdentity()) return false;
            if (isStrict() != thatBuilder.isStrict()) return false;
            if (getUseModelCache() != thatBuilder.getUseModelCache()) return false;
            if (getUsePrivateCaches() != thatBuilder.getUsePrivateCaches()) return false;
            if (!getExtensions().equals(thatBuilder.getExtensions())) return false;
            return this.classIntrospectorBuilder.equals(thatBuilder.classIntrospectorBuilder);
        }

        /**
         * If the builder is used as a cache key, this is used to clone it before it's stored in the cache as a key, so
         * that further changes in the original builder won't change the key (aliasing). It calls {@link Object#clone()}
         * internally, so all fields are automatically copied, but it will also individually clone field values that are
         * both mutable and has a content-based equals method (deep cloning).
         * <p>
         * If you extend {@link ExtendableBuilder} with new fields with mutable values that have a content-based equals
         * method, and you will also cache product instances, you need to clone those values manually to prevent
         * aliasing problems, so don't forget to override this method!
         *
         * @see #equals(Object)
         * @see #hashCode()
         */
        protected SelfT cloneForCacheKey() {
            try {
                @SuppressWarnings("unchecked") SelfT clone = (SelfT) super.clone();
                ((ExtendableBuilder<?, ?>) clone).classIntrospectorBuilder = (ClassIntrospector.Builder)
                        classIntrospectorBuilder.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Failed to deepClone Builder", e);
            }
        }

        public Version getIncompatibleImprovements() {
            return incompatibleImprovements;
        }

        /**
         * Getter pair of {@link #setDefaultDateType(int)}
         */
        public int getDefaultDateType() {
            return defaultDateType;
        }

        /**
         * Sets the default date type to use for date models that result from
         * a plain <tt>java.util.Date</tt> instead of <tt>java.sql.Date</tt> or
         * <tt>java.sql.Time</tt> or <tt>java.sql.Timestamp</tt>. Default value is
         * {@link TemplateDateModel#UNKNOWN}.
         * @param defaultDateType the new default date type.
         */
        public void setDefaultDateType(int defaultDateType) {
            this.defaultDateType = defaultDateType;
            defaultDataTypeSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setDefaultDateType(int)}.
         */
        public SelfT defaultDateType(int defaultDateType) {
            setDefaultDateType(defaultDateType);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isDefaultDateTypeSet() {
            return defaultDataTypeSet;
        }

        /**
         * Getter pair of {@link #setOuterIdentity(ObjectWrapper)}.
         */
        public ObjectWrapper getOuterIdentity() {
            return outerIdentity;
        }

        /**
         * When wrapping an object, the DefaultObjectWrapper commonly needs to wrap "sub-objects", for example each
         * element in a wrapped collection. Normally it wraps these objects using itself. However, this makes it
         * difficult to delegate to a DefaultObjectWrapper as part of a custom aggregate ObjectWrapper. This method lets
         * you set the ObjectWrapper which will be used to wrap the sub-objects.
         *
         * @param outerIdentity
         *         the aggregate ObjectWrapper, or {@code null} if we will use the object created by this builder.
         */
        public void setOuterIdentity(ObjectWrapper outerIdentity) {
            this.outerIdentity = outerIdentity;
            outerIdentitySet = true;
        }

        /**
         * Fluent API equivalent of {@link #setOuterIdentity(ObjectWrapper)}.
         */
        public SelfT outerIdentity(ObjectWrapper outerIdentity) {
            setOuterIdentity(outerIdentity);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isOuterIdentitySet() {
            return outerIdentitySet;
        }

        /**
         * Getter pair of {@link #setStrict(boolean)}.
         */
        public boolean isStrict() {
            return strict;
        }

        /**
         * Specifies if an attempt to read a bean property that doesn't exist in the
         * wrapped object should throw an {@link InvalidPropertyException}.
         *
         * <p>If this property is <tt>false</tt> (the default) then an attempt to read
         * a missing bean property is the same as reading an existing bean property whose
         * value is <tt>null</tt>. The template can't tell the difference, and thus always
         * can use <tt>!'something'</tt> and <tt>??</tt> and similar expressions
         * to handle the situation.
         *
         * <p>If this property is <tt>true</tt> then an attempt to read a bean propertly in
         * the template (like <tt>myBean.aProperty</tt>) that doesn't exist in the bean
         * object (as opposed to just holding <tt>null</tt> value) will cause
         * {@link InvalidPropertyException}, which can't be suppressed in the template
         * (not even with <tt>myBean.noSuchProperty!'something'</tt>). This way
         * <tt>!'something'</tt> and <tt>??</tt> and similar expressions can be used to
         * handle existing properties whose value is <tt>null</tt>, without the risk of
         * hiding typos in the property names. Typos will always cause error. But mind you, it
         * goes against the basic approach of FreeMarker, so use this feature only if you really
         * know what you are doing.
         */
        public void setStrict(boolean strict) {
            this.strict = strict;
            strictSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setStrict(boolean)}.
         */
        public SelfT strict(boolean strict) {
            setStrict(strict);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isStrictSet() {
            return strictSet;
        }

        public boolean getUseModelCache() {
            return useModelCache;
        }

        /**
         * @deprecated Does nothing in FreeMarker 3 - we kept it for now to postopne reworking some JUnit tests.
         */
        // [FM3] Remove
        @Deprecated
        public void setUseModelCache(boolean useModelCache) {
            this.useModelCache = useModelCache;
            useModelCacheSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setUseModelCache(boolean)}.
         * @deprecated Does nothing in FreeMarker 3 - we kept it for now to postopne reworking some JUnit tests.
         */
        @Deprecated
        public SelfT useModelCache(boolean useModelCache) {
            setUseModelCache(useModelCache);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isUseModelCacheSet() {
            return useModelCacheSet;
        }

        /**
         * Getter pair of {@link #setUsePrivateCaches(boolean)}.
         */
        public boolean getUsePrivateCaches() {
            return usePrivateCaches;
        }

        /**
         * Tells if the instance creates should share caches with other {@link DefaultObjectWrapper} instances
         * (where possible), or it should always invoke its own caches and not share that with anyone else.
         * */
        public void setUsePrivateCaches(boolean usePrivateCaches) {
            this.usePrivateCaches = usePrivateCaches;
            usePrivateCachesSet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isUsePrivateCachesSet() {
            return usePrivateCachesSet;
        }

        /**
         * Fluent API equivalent of {@link #setUsePrivateCaches(boolean)}
         */
        public SelfT usePrivateCaches(boolean usePrivateCaches) {
            setUsePrivateCaches(usePrivateCaches);
            return self();
        }

        /**
         * Extensions are  used for dynamically decorating the {@link #wrap(Object)} method. In effects this is very
         * similar to extending {@link DefaultObjectWrapper} and overriding its {@link #wrap(Object)}, and adding
         * some wrapping logic before and/or after calling {@code super.wrap(obj)} (often referred to as decorating).
         * But with this approach instead of subclassing {@link DefaultObjectWrapper} and its builder class, you
         * simply list the desired extensions when you build the {@link DefaultObjectWrapper}. This is usually more
         * convenient, and more flexible (what extensions you add can be decided on runtime factors) than the
         * subclassing approach.
         *
         * @return An unmodifiable {@link List}.
         */
        public List<? extends DefaultObjectWrapperExtension> getExtensions() {
            return extensions;
        }

        /**
         * Setter pair of {@link #getExtensions()}.
         *
         * @param extensions The list of extensions; can't be {@code null}.
         *                   The {@link List} list is copied, so further changes to the
         *                   {@link List} passed in won't affect the value of this setting.
         */
        public void setExtensions(List<? extends DefaultObjectWrapperExtension> extensions) {
            _NullArgumentException.check("extensions", extensions);
            this.extensions = Collections.unmodifiableList(new ArrayList(extensions));
            this.extensionsSet = true;
        }

        /**
         * Fluent API equivalent of {@link #setExtensions(List)}.
         */
        public SelfT extensions(List<? extends DefaultObjectWrapperExtension> extensions) {
            setExtensions(extensions);
            return self();
        }

        /**
         * Convenience varargs overload for calling {@link #extensions(List)}.
         */
        public SelfT extensions(DefaultObjectWrapperExtension... extensions) {
            return extensions(Arrays.asList(extensions));
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isExtensionsSet() {
            return extensionsSet;
        }

        public int getExposureLevel() {
            return classIntrospectorBuilder.getExposureLevel();
        }

        /**
         * Sets the method exposure level. By default, set to <code>EXPOSE_SAFE</code>.
         * @param exposureLevel can be any of the <code>EXPOSE_xxx</code>
         * constants.
         */
        public void setExposureLevel(int exposureLevel) {
            classIntrospectorBuilder.setExposureLevel(exposureLevel);
        }

        public SelfT exposureLevel(int exposureLevel) {
            setExposureLevel(exposureLevel);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean setExposureLevelSet() {
            return classIntrospectorBuilder.isExposureLevelSet();
        }

        /**
         * Getter pair of {@link #setExposeFields(boolean)}
         */
        public boolean getExposeFields() {
            return classIntrospectorBuilder.getExposeFields();
        }

        /**
         * Controls whether public instance fields of classes are exposed to
         * templates.
         * @param exposeFields if set to true, public instance fields of classes
         * that do not have a property getter defined can be accessed directly by
         * their name. If there is a property getter for a property of the same
         * name as the field (i.e. getter "getFoo()" and field "foo"), then
         * referring to "foo" in template invokes the getter. If set to false, no
         * access to public instance fields of classes is given. Default is false.
         */
        public void setExposeFields(boolean exposeFields) {
            classIntrospectorBuilder.setExposeFields(exposeFields);
        }

        /**
         * Fluent API equivalent of {@link #setExposeFields(boolean)}
         */
        public SelfT exposeFields(boolean exposeFields) {
            setExposeFields(exposeFields);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isExposeFieldsSet() {
            return classIntrospectorBuilder.isExposeFieldsSet();
        }

        /**
         * Getter pair of {@link #setMethodAppearanceFineTuner(MethodAppearanceFineTuner)}
         */
        public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
            return classIntrospectorBuilder.getMethodAppearanceFineTuner();
        }

        /**
         * Used to tweak certain aspects of how methods appear in the data-model;
         * see {@link MethodAppearanceFineTuner} for more.
         * Setting this to non-{@code null} will disable class introspection cache sharing, unless
         * the value implements {@link SingletonCustomizer}.
         */
        public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
            classIntrospectorBuilder.setMethodAppearanceFineTuner(methodAppearanceFineTuner);
        }

        /**
         * Fluent API equivalent of {@link #setMethodAppearanceFineTuner(MethodAppearanceFineTuner)}
         */
        public SelfT methodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
            setMethodAppearanceFineTuner(methodAppearanceFineTuner);
            return self();
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isMethodAppearanceFineTunerSet() {
            return classIntrospectorBuilder.isMethodAppearanceFineTunerSet();
        }

        /**
         * Used internally for testing.
         */
        MethodSorter getMethodSorter() {
            return classIntrospectorBuilder.getMethodSorter();
        }

        /**
         * Used internally for testing.
         */
        void setMethodSorter(MethodSorter methodSorter) {
            classIntrospectorBuilder.setMethodSorter(methodSorter);
        }

        /**
         * Used internally for testing.
         */
        SelfT methodSorter(MethodSorter methodSorter) {
            setMethodSorter(methodSorter);
            return self();
        }

    }
}

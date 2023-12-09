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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CommonBuilder;
import org.apache.freemarker.core.util._NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Returns information about a {@link Class} that's useful for FreeMarker. Encapsulates a cache for this. Thread-safe,
 * doesn't even require "proper publishing" starting from 2.3.24 or Java 5. Immutable, with the exception of the
 * internal caches.
 * 
 * <p>
 * Note that instances of this are cached on the level of FreeMarker's defining class loader. Hence, it must not do
 * operations that depend on the Thread Context Class Loader, such as resolving class names.
 */
class ClassIntrospector {

    // Attention: This class must be thread-safe (not just after proper publishing). This is important as some of
    // these are shared by many object wrappers, and concurrency related glitches due to user errors must remain
    // local to the object wrappers, not corrupting the shared ClassIntrospector.

    private static final Logger LOG = LoggerFactory.getLogger(ClassIntrospector.class);

    private static final String JREBEL_SDK_CLASS_NAME = "org.zeroturnaround.javarebel.ClassEventListener";
    private static final String JREBEL_INTEGRATION_ERROR_MSG
            = "Error initializing JRebel integration. JRebel integration disabled.";

    private static final ExecutableMemberSignature GET_STRING_SIGNATURE =
            new ExecutableMemberSignature("get", new Class[] { String.class });
    private static final ExecutableMemberSignature GET_OBJECT_SIGNATURE =
            new ExecutableMemberSignature("get", new Class[] { Object.class });
    private static final ExecutableMemberSignature TO_STRING_SIGNATURE =
            new ExecutableMemberSignature("toString", new Class[0]);

    private static final ClassChangeNotifier CLASS_CHANGE_NOTIFIER;
    static {
        boolean jRebelAvailable;
        try {
            Class.forName(JREBEL_SDK_CLASS_NAME);
            jRebelAvailable = true;
        } catch (Throwable e) {
            jRebelAvailable = false;
            try {
                if (!(e instanceof ClassNotFoundException)) {
                    LOG.error(JREBEL_INTEGRATION_ERROR_MSG, e);
                }
            } catch (Throwable loggingE) {
                // ignore
            }
        }

        ClassChangeNotifier classChangeNotifier;
        if (jRebelAvailable) {
            try {
                classChangeNotifier = (ClassChangeNotifier)
                        Class.forName("org.apache.freemarker.core.model.impl.JRebelClassChangeNotifier").newInstance();
            } catch (Throwable e) {
                classChangeNotifier = null;
                try {
                    LOG.error(JREBEL_INTEGRATION_ERROR_MSG, e);
                } catch (Throwable loggingE) {
                    // ignore
                }
            }
        } else {
            classChangeNotifier = null;
        }

        CLASS_CHANGE_NOTIFIER = classChangeNotifier;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Introspection info Map keys:

    /** Key in the class info Map to the Map that maps method to argument type arrays */
    private static final Object ARG_TYPES_BY_METHOD_KEY = new Object();
    /** Key in the class info Map to the object that represents the constructors (one or multiple due to overloading) */
    static final Object CONSTRUCTORS_KEY = new Object();
    /** Key in the class info Map to the get(String|Object) Method */
    static final Object GENERIC_GET_KEY = new Object();
    /** Key in the class info Map to the toString() Method */
    static final Object TO_STRING_HIDDEN_FLAG_KEY = new Object();

    // -----------------------------------------------------------------------------------------------------------------
    // Introspection configuration properties:

    // Note: These all must be *declared* final (or else synchronization is needed everywhere where they are accessed).

    final int exposureLevel;
    final boolean exposeFields;
    final MemberAccessPolicy memberAccessPolicy;
    final MethodAppearanceFineTuner methodAppearanceFineTuner;
    final MethodSorter methodSorter;
    final Version incompatibleImprovements;

    /** See {@link #getHasSharedInstanceRestrictions()} */
    final private boolean hasSharedInstanceRestrictions;

    /** See {@link #isShared()} */
    final private boolean shared;

    // -----------------------------------------------------------------------------------------------------------------
    // State fields:

    private final Object sharedLock;
    private final Map<Class<?>, Map<Object, Object>> cache
            = new ConcurrentHashMap<>(0, 0.75f, 16);
    private final Set<String> cacheClassNames = new HashSet<>(0);
    private final Set<Class<?>> classIntrospectionsInProgress = new HashSet<>(0);

    private final List<WeakReference<Object/*ClassBasedModelFactory|ModelCache>*/>> modelFactories
            = new LinkedList<>();
    private final ReferenceQueue<Object> modelFactoriesRefQueue = new ReferenceQueue<>();

    private int clearingCounter;

    // -----------------------------------------------------------------------------------------------------------------
    // Instantiation:

    /**
     * @param hasSharedInstanceRestrictions
     *            If the instance should behave as if it is shared, even if it we couldn't put
     *            it into the shared cache.
     * @param shared
     *            If the instance is in the shared cache, and so is potentially shared.
     */
    ClassIntrospector(Builder builder, Object sharedLock,
                      boolean hasSharedInstanceRestrictions, boolean shared) {
        _NullArgumentException.check("sharedLock", sharedLock);

        exposureLevel = builder.getExposureLevel();
        exposeFields = builder.getExposeFields();
        memberAccessPolicy = builder.getMemberAccessPolicy();
        methodAppearanceFineTuner = builder.getMethodAppearanceFineTuner();
        methodSorter = builder.getMethodSorter();
        this.incompatibleImprovements = builder.getIncompatibleImprovements();

        this.sharedLock = sharedLock;

        this.hasSharedInstanceRestrictions = hasSharedInstanceRestrictions;
        this.shared = shared;

        if (CLASS_CHANGE_NOTIFIER != null) {
            CLASS_CHANGE_NOTIFIER.subscribe(this);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Introspection:

    /**
     * Gets the class introspection data from {@link #cache}, automatically creating the cache entry if it's missing.
     * 
     * @return A {@link Map} where each key is a property/method/field name (or a special {@link Object} key like
     *         {@link #CONSTRUCTORS_KEY}), each value is a {@link FastPropertyDescriptor} or {@link Method} or
     *         {@link OverloadedMethods} or {@link Field} (but better check the source code...).
     */
    Map<Object, Object> get(Class<?> clazz) {
        {
            Map<Object, Object> introspData = cache.get(clazz);
            if (introspData != null) return introspData;
        }

        String className;
        synchronized (sharedLock) {
            Map<Object, Object> introspData = cache.get(clazz);
            if (introspData != null) return introspData;

            className = clazz.getName();
            if (cacheClassNames.contains(className)) {
                onSameNameClassesDetected(className);
            }

            while (introspData == null && classIntrospectionsInProgress.contains(clazz)) {
                // Another thread is already introspecting this class;
                // waiting for its result.
                try {
                    sharedLock.wait();
                    introspData = cache.get(clazz);
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                            "Class inrospection data lookup aborded: " + e);
                }
            }
            if (introspData != null) return introspData;

            // This will be the thread that introspects this class.
            classIntrospectionsInProgress.add(clazz);
        }
        try {
            Map<Object, Object> introspData = createClassIntrospectionData(clazz);
            synchronized (sharedLock) {
                cache.put(clazz, introspData);
                cacheClassNames.add(className);
            }
            return introspData;
        } finally {
            synchronized (sharedLock) {
                classIntrospectionsInProgress.remove(clazz);
                sharedLock.notifyAll();
            }
        }
    }

    /**
     * Creates a {@link Map} with the content as described for the return value of {@link #get(Class)}.
     */
    private Map<Object, Object> createClassIntrospectionData(Class<?> clazz) {
        final Map<Object, Object> introspData = new HashMap<>();
        MemberAccessPolicy effMemberAccessPolicy = getEffectiveMemberAccessPolicy();
        ClassMemberAccessPolicy effClassMemberAccessPolicy = effMemberAccessPolicy.forClass(clazz);

        if (exposeFields) {
            addFieldsToClassIntrospectionData(introspData, clazz, effClassMemberAccessPolicy);
        }

        final Map<ExecutableMemberSignature, List<Method>> accessibleMethods = discoverAccessibleMethods(clazz);

        if (!effMemberAccessPolicy.isToStringAlwaysExposed()) {
            addToStringHiddenFlagToClassIntrospectionData(introspData, accessibleMethods, effClassMemberAccessPolicy);
        }

        addGenericGetToClassIntrospectionData(introspData, accessibleMethods, effClassMemberAccessPolicy);

        if (exposureLevel != DefaultObjectWrapper.EXPOSE_NOTHING) {
            try {
                addBeanInfoToClassIntrospectionData(introspData, clazz, accessibleMethods, effClassMemberAccessPolicy);
            } catch (IntrospectionException e) {
                LOG.warn("Couldn't properly perform introspection for class {}", clazz.getName(), e);
                introspData.clear(); // FIXME NBC: Don't drop everything here.
            }
        }

        addConstructorsToClassIntrospectionData(introspData, clazz, effClassMemberAccessPolicy);

        if (introspData.size() > 1) {
            return introspData;
        } else if (introspData.size() == 0) {
            return Collections.emptyMap();
        } else { // map.size() == 1
            Entry<Object, Object> e = introspData.entrySet().iterator().next();
            return Collections.singletonMap(e.getKey(), e.getValue());
        }
    }

    private void addFieldsToClassIntrospectionData(Map<Object, Object> introspData, Class<?> clazz,
            ClassMemberAccessPolicy effClassMemberAccessPolicy) throws SecurityException {
        for (Field field : clazz.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                if (effClassMemberAccessPolicy.isFieldExposed(field)) {
                    introspData.put(field.getName(), field);
                }
            }
        }
    }

    private void addBeanInfoToClassIntrospectionData(
            Map<Object, Object> introspData, Class<?> clazz, Map<ExecutableMemberSignature, List<Method>> accessibleMethods,
            ClassMemberAccessPolicy effClassMemberAccessPolicy) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        List<PropertyDescriptor> pdas = getPropertyDescriptors(beanInfo, clazz);
        int pdasLength = pdas.size();
        // Reverse order shouldn't mater, but we keep it to not risk backward incompatibility.
        for (int i = pdasLength - 1; i >= 0; --i) {
            addPropertyDescriptorToClassIntrospectionData(
                    introspData, pdas.get(i), clazz,
                    accessibleMethods, effClassMemberAccessPolicy);
        }

        if (exposureLevel < DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY) {
            final MethodAppearanceFineTuner.Decision decision = new MethodAppearanceFineTuner.Decision();
            MethodAppearanceFineTuner.DecisionInput decisionInput = null;
            List<MethodDescriptor> mds = getMethodDescriptors(beanInfo, clazz);
            sortMethodDescriptors(mds);
            int mdsSize = mds.size();
            IdentityHashMap<Method, Void> argTypesUsedByIndexerPropReaders = null;
            for (int i = mdsSize - 1; i >= 0; --i) {
                final Method method = getMatchingAccessibleMethod(mds.get(i).getMethod(), accessibleMethods);
                if (method != null && effClassMemberAccessPolicy.isMethodExposed(method)) {
                    decision.setDefaults(method);
                    if (methodAppearanceFineTuner != null) {
                        if (decisionInput == null) {
                            decisionInput = new MethodAppearanceFineTuner.DecisionInput();
                        }
                        decisionInput.setContainingClass(clazz);
                        decisionInput.setMethod(method);

                        methodAppearanceFineTuner.process(decisionInput, decision);
                    }

                    PropertyDescriptor propDesc = decision.getExposeAsProperty();
                    if (propDesc != null &&
                            (decision.getReplaceExistingProperty()
                                    || !(introspData.get(propDesc.getName()) instanceof FastPropertyDescriptor))) {
                        addPropertyDescriptorToClassIntrospectionData(
                                introspData, propDesc, clazz, accessibleMethods, effClassMemberAccessPolicy);
                    }

                    String methodKey = decision.getExposeMethodAs();
                    if (methodKey != null) {
                        Object previous = introspData.get(methodKey);
                        if (previous instanceof Method) {
                            // Overloaded method - replace Method with a OverloadedMethods
                            OverloadedMethods overloadedMethods = new OverloadedMethods();
                            overloadedMethods.addMethod((Method) previous);
                            overloadedMethods.addMethod(method);
                            introspData.put(methodKey, overloadedMethods);
                            // Remove parameter type information (unless an indexed property reader needs it):
                            if (argTypesUsedByIndexerPropReaders == null
                                    || !argTypesUsedByIndexerPropReaders.containsKey(previous)) {
                                getArgTypesByMethod(introspData).remove(previous);
                            }
                        } else if (previous instanceof OverloadedMethods) {
                            // Already overloaded method - add new overload
                            ((OverloadedMethods) previous).addMethod(method);
                        } else if (decision.getMethodShadowsProperty()
                                || !(previous instanceof FastPropertyDescriptor)) {
                            // Simple method (this far)
                            introspData.put(methodKey, method);
                            Class<?>[] replaced = getArgTypesByMethod(introspData).put(method,
                                    method.getParameterTypes());
                            if (replaced != null) {
                                if (argTypesUsedByIndexerPropReaders == null) {
                                    argTypesUsedByIndexerPropReaders = new IdentityHashMap<>();
                                }
                                argTypesUsedByIndexerPropReaders.put(method, null);
                            }
                        }
                    }
                }
            } // for each in mds
        } // end if (exposureLevel < EXPOSE_PROPERTIES_ONLY)
    }

    // TODO [FM3] As we ignore indexed property getters in FM3, this might could be simplified. 
    /**
     * Very similar to {@link BeanInfo#getPropertyDescriptors()}, but can deal with Java 8 default methods too.
     */
    private List<PropertyDescriptor> getPropertyDescriptors(BeanInfo beanInfo, Class<?> clazz) {
        PropertyDescriptor[] introspectorPDsArray = beanInfo.getPropertyDescriptors();
        List<PropertyDescriptor> introspectorPDs = introspectorPDsArray != null ? Arrays.asList(introspectorPDsArray)
                : Collections.emptyList();

        // introspectorPDs contains each property exactly once. But as now we will search them manually too, it can
        // happen that we find the same property for multiple times. Worse, because of indexed properties, it's possible
        // that we have to merge entries (like one has the normal reader method, the other has the indexed reader
        // method), instead of just replacing them in a Map. That's why we have introduced PropertyReaderMethodPair,
        // which holds the methods belonging to the same property name. IndexedPropertyDescriptor is not good for that,
        // as it can't store two methods whose types are incompatible, and we have to wait until all the merging was
        // done to see if the incompatibility goes away.

        // This could be Map<String, PropertyReaderMethodPair>, but since we rarely need to do merging, we try to avoid
        // creating those and use the source objects as much as possible. Also note that we initialize this lazily.
        LinkedHashMap<String, Object /*PropertyReaderMethodPair|Method|PropertyDescriptor*/> mergedPRMPs = null;

        // Collect Java 8 default methods that look like property readers into mergedPRMPs:
        // (Note that java.beans.Introspector discovers non-accessible public methods, and to emulate that behavior
        // here, we don't utilize the accessibleMethods Map, which we might already have at this point.)
        for (Method method : clazz.getMethods()) {
            if (method.isDefault() && method.getReturnType() != void.class
                    && !method.isBridge()) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 0
                        || paramTypes.length == 1 && paramTypes[0] == int.class /* indexed property reader */) {
                    String propName = _MethodUtils.getBeanPropertyNameFromReaderMethodName(
                            method.getName(), method.getReturnType());
                    if (propName != null) {
                        if (mergedPRMPs == null) {
                            // Lazy initialization
                            mergedPRMPs = new LinkedHashMap<>();
                        }
                        if (paramTypes.length == 0) {
                            mergeInPropertyReaderMethod(mergedPRMPs, propName, method);
                        } else { // It's an indexed property reader method
                            mergeInPropertyReaderMethodPair(mergedPRMPs, propName,
                                    new PropertyReaderedMethodPair(null, method));
                        }
                    }
                }
            }
        } // for clazz.getMethods()

        if (mergedPRMPs == null) {
            // We had no interfering Java 8 default methods, so we can chose the fast route.
            return introspectorPDs;
        }

        for (PropertyDescriptor introspectorPD : introspectorPDs) {
            mergeInPropertyDescriptor(mergedPRMPs, introspectorPD);
        }

        // Now we convert the PRMPs to PDs, handling case where the normal and the indexed read methods contradict.
        List<PropertyDescriptor> mergedPDs = new ArrayList<>(mergedPRMPs.size());
        for (Entry<String, Object> entry : mergedPRMPs.entrySet()) {
            String propName = entry.getKey();
            Object propDescObj = entry.getValue();
            if (propDescObj instanceof PropertyDescriptor) {
                mergedPDs.add((PropertyDescriptor) propDescObj);
            } else {
                Method readMethod;
                Method indexedReadMethod;
                if (propDescObj instanceof Method) {
                    readMethod = (Method) propDescObj;
                    indexedReadMethod = null;
                } else if (propDescObj instanceof PropertyReaderedMethodPair) {
                    PropertyReaderedMethodPair prmp = (PropertyReaderedMethodPair) propDescObj;
                    readMethod = prmp.readMethod;
                    indexedReadMethod = prmp.indexedReadMethod;
                    if (readMethod != null && indexedReadMethod != null
                            && indexedReadMethod.getReturnType() != readMethod.getReturnType().getComponentType()) {
                        // Here we copy the java.beans.Introspector behavior: If the array item class is not exactly the
                        // the same as the indexed read method return type, we say that the property is not indexed.
                        indexedReadMethod = null;
                    }
                } else {
                    throw new BugException();
                }
                try {
                    mergedPDs.add(
                            indexedReadMethod != null
                                    ? new IndexedPropertyDescriptor(propName,
                                    readMethod, null, indexedReadMethod, null)
                                    : new PropertyDescriptor(propName, readMethod, null));
                } catch (IntrospectionException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed creating property descriptor for " + clazz.getName() + " property " + propName,
                                e);
                    }
                }
            }
        }
        return mergedPDs;
    }

    private static class PropertyReaderedMethodPair {
        private final Method readMethod;
        private final Method indexedReadMethod;

        PropertyReaderedMethodPair(Method readerMethod, Method indexedReaderMethod) {
            this.readMethod = readerMethod;
            this.indexedReadMethod = indexedReaderMethod;
        }

        PropertyReaderedMethodPair(PropertyDescriptor pd) {
            this(
                    pd.getReadMethod(),
                    pd instanceof IndexedPropertyDescriptor
                            ? ((IndexedPropertyDescriptor) pd).getIndexedReadMethod() : null);
        }

        static PropertyReaderedMethodPair from(Object obj) {
            if (obj instanceof PropertyReaderedMethodPair) {
                return (PropertyReaderedMethodPair) obj;
            } else if (obj instanceof PropertyDescriptor) {
                return new PropertyReaderedMethodPair((PropertyDescriptor) obj);
            } else if (obj instanceof Method) {
                return new PropertyReaderedMethodPair((Method) obj, null);
            } else {
                throw new BugException("Unexpected obj type: " + obj.getClass().getName());
            }
        }

        static PropertyReaderedMethodPair merge(PropertyReaderedMethodPair oldMethods, PropertyReaderedMethodPair newMethods) {
            return new PropertyReaderedMethodPair(
                    newMethods.readMethod != null ? newMethods.readMethod : oldMethods.readMethod,
                    newMethods.indexedReadMethod != null ? newMethods.indexedReadMethod
                            : oldMethods.indexedReadMethod);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((indexedReadMethod == null) ? 0 : indexedReadMethod.hashCode());
            result = prime * result + ((readMethod == null) ? 0 : readMethod.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PropertyReaderedMethodPair other = (PropertyReaderedMethodPair) obj;
            return other.readMethod == readMethod && other.indexedReadMethod == indexedReadMethod;
        }

    }

    private void mergeInPropertyDescriptor(LinkedHashMap<String, Object> mergedPRMPs, PropertyDescriptor pd) {
        String propName = pd.getName();
        Object replaced = mergedPRMPs.put(propName, pd);
        if (replaced != null) {
            PropertyReaderedMethodPair newPRMP = new PropertyReaderedMethodPair(pd);
            putIfMergedPropertyReaderMethodPairDiffers(mergedPRMPs, propName, replaced, newPRMP);
        }
    }

    private void mergeInPropertyReaderMethodPair(LinkedHashMap<String, Object> mergedPRMPs,
                                                 String propName, PropertyReaderedMethodPair newPRM) {
        Object replaced = mergedPRMPs.put(propName, newPRM);
        if (replaced != null) {
            putIfMergedPropertyReaderMethodPairDiffers(mergedPRMPs, propName, replaced, newPRM);
        }
    }

    private void mergeInPropertyReaderMethod(LinkedHashMap<String, Object> mergedPRMPs,
                                             String propName, Method readerMethod) {
        Object replaced = mergedPRMPs.put(propName, readerMethod);
        if (replaced != null) {
            putIfMergedPropertyReaderMethodPairDiffers(mergedPRMPs, propName,
                    replaced, new PropertyReaderedMethodPair(readerMethod, null));
        }
    }

    private void putIfMergedPropertyReaderMethodPairDiffers(LinkedHashMap<String, Object> mergedPRMPs,
                                                            String propName, Object replaced, PropertyReaderedMethodPair newPRMP) {
        PropertyReaderedMethodPair replacedPRMP = PropertyReaderedMethodPair.from(replaced);
        PropertyReaderedMethodPair mergedPRMP = PropertyReaderedMethodPair.merge(replacedPRMP, newPRMP);
        if (!mergedPRMP.equals(newPRMP)) {
            mergedPRMPs.put(propName, mergedPRMP);
        }
    }

    /**
     * Very similar to {@link BeanInfo#getMethodDescriptors()}, but can deal with Java 8 default methods too.
     */
    private List<MethodDescriptor> getMethodDescriptors(BeanInfo beanInfo, Class<?> clazz) {
        MethodDescriptor[] introspectorMDArray = beanInfo.getMethodDescriptors();
        List<MethodDescriptor> introspectionMDs = introspectorMDArray != null && introspectorMDArray.length != 0
                ? Arrays.asList(introspectorMDArray) : Collections.emptyList();

        Map<String, List<Method>> defaultMethodsToAddByName = null;
        for (Method method : clazz.getMethods()) {
            if (method.isDefault() && !method.isBridge()) {
                if (defaultMethodsToAddByName == null) {
                    defaultMethodsToAddByName = new HashMap<>();
                }
                List<Method> overloads = defaultMethodsToAddByName.get(method.getName());
                if (overloads == null) {
                    overloads = new ArrayList<>(0);
                    defaultMethodsToAddByName.put(method.getName(), overloads);
                }
                overloads.add(method);
            }
        }

        if (defaultMethodsToAddByName == null) {
            // We had no interfering default methods:
            return introspectionMDs;
        }

        // Recreate introspectionMDs so that its size can grow:
        ArrayList<MethodDescriptor> newIntrospectionMDs
                = new ArrayList<>(introspectionMDs.size() + 16);
        for (MethodDescriptor introspectorMD : introspectionMDs) {
            Method introspectorM = introspectorMD.getMethod();
            // Prevent cases where the same method is added with different return types both from the list of default
            // methods and from the list of Introspector-discovered methods, as that would lead to overloaded method
            // selection ambiguity later. This is known to happen when the default method in an interface has reified
            // return type, and then the interface is implemented by a class where the compiler generates an override
            // for the bridge method only. (Other tricky cases might exist.)
            if (!containsMethodWithSameParameterTypes(
                    defaultMethodsToAddByName.get(introspectorM.getName()), introspectorM)) {
                newIntrospectionMDs.add(introspectorMD);
            }
        }
        introspectionMDs = newIntrospectionMDs;

        // Add default methods:
        for (Entry<String, List<Method>> entry : defaultMethodsToAddByName.entrySet()) {
            for (Method method : entry.getValue()) {
                introspectionMDs.add(new MethodDescriptor(method));
            }
        }

        return introspectionMDs;
    }

    private boolean containsMethodWithSameParameterTypes(List<Method> overloads, Method m) {
        if (overloads == null) {
            return false;
        }

        Class<?>[] paramTypes = m.getParameterTypes();
        for (Method overload : overloads) {
            if (Arrays.equals(overload.getParameterTypes(), paramTypes)) {
                return true;
            }
        }
        return false;
    }

    private void addPropertyDescriptorToClassIntrospectionData(Map<Object, Object> introspData,
            PropertyDescriptor pd, Class<?> clazz, Map<ExecutableMemberSignature, List<Method>> accessibleMethods,
            ClassMemberAccessPolicy effClassMemberAccessPolicy) {
        Method readMethod = getMatchingAccessibleMethod(pd.getReadMethod(), accessibleMethods);
        if (readMethod != null && effClassMemberAccessPolicy.isMethodExposed(readMethod)) {
            introspData.put(pd.getName(), new FastPropertyDescriptor(readMethod));
        }
    }

    private void addGenericGetToClassIntrospectionData(Map<Object, Object> introspData,
            Map<ExecutableMemberSignature, List<Method>> accessibleMethods,
            ClassMemberAccessPolicy effClassMemberAccessPolicy) {
        Method genericGet = getFirstAccessibleMethod(
                GET_STRING_SIGNATURE, accessibleMethods);
        if (genericGet == null) {
            genericGet = getFirstAccessibleMethod(
                    GET_OBJECT_SIGNATURE, accessibleMethods);
        }
        if (genericGet != null && effClassMemberAccessPolicy.isMethodExposed(genericGet)) {
            introspData.put(GENERIC_GET_KEY, genericGet);
        }
    }

    private void addToStringHiddenFlagToClassIntrospectionData(Map<Object, Object> introspData,
            Map<ExecutableMemberSignature, List<Method>> accessibleMethods,
            ClassMemberAccessPolicy effClassMemberAccessPolicy) {
        Method toStringMethod = getFirstAccessibleMethod(TO_STRING_SIGNATURE, accessibleMethods);
        if (toStringMethod == null) {
            throw new BugException("toString() method not found");
        }
        // toString() is pretty much always exposed, so we make the negative case to take extra memory:
        if (!effClassMemberAccessPolicy.isMethodExposed(toStringMethod)) {
            introspData.put(TO_STRING_HIDDEN_FLAG_KEY, true);
        }
    }

    private void addConstructorsToClassIntrospectionData(final Map<Object, Object> introspData,
            Class<?> clazz, ClassMemberAccessPolicy effClassMemberAccessPolicy) {
        try {
            Constructor<?>[] ctorsUnfiltered = clazz.getConstructors();
            List<Constructor<?>> ctors = new ArrayList<>(ctorsUnfiltered.length);
            for (Constructor<?> ctor : ctorsUnfiltered) {
                if (effClassMemberAccessPolicy.isConstructorExposed(ctor)) {
                    ctors.add(ctor);
                }
            }

            if (!ctors.isEmpty()) {
                final Object ctorsIntrospData;
                if (ctors.size() == 1) {
                    Constructor<?> ctor = ctors.get(0);
                    ctorsIntrospData = new SimpleMethod(ctor, ctor.getParameterTypes());
                } else {
                    OverloadedMethods overloadedCtors = new OverloadedMethods();
                    for (Constructor<?> ctor : ctors) {
                        overloadedCtors.addConstructor(ctor);
                    }
                    ctorsIntrospData = overloadedCtors;
                }
                introspData.put(CONSTRUCTORS_KEY, ctorsIntrospData);
            }
        } catch (SecurityException e) {
            LOG.warn("Can't discover constructors for class {}", clazz.getName(), e);
        }
    }

    /**
     * Retrieves mapping of {@link ExecutableMemberSignature}-s to a {@link List} of accessible methods for a class. In
     * case the class is not public, retrieves methods with same signature as its public methods from public
     * superclasses and interfaces. Basically upcasts every method to the nearest accessible method.
     */
    private static Map<ExecutableMemberSignature, List<Method>> discoverAccessibleMethods(Class<?> clazz) {
        Map<ExecutableMemberSignature, List<Method>> accessibles = new HashMap<>();
        discoverAccessibleMethods(clazz, accessibles);
        return accessibles;
    }

    private static void discoverAccessibleMethods(
            Class<?> clazz, Map<ExecutableMemberSignature, List<Method>> accessibles) {
        if (Modifier.isPublic(clazz.getModifiers())) {
            try {
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    ExecutableMemberSignature sig = new ExecutableMemberSignature(method);
                    if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                        // Contrary to intuition, a class can actually have several
                        // different methods with same signature *but* different
                        // return types. These can't be constructed using Java the
                        // language, as this is illegal on source code level, but
                        // the compiler can emit synthetic methods as part of
                        // generic type reification that will have same signature
                        // yet different return type than an existing explicitly
                        // declared method. Consider:
                        // public interface I<T> { T m(); }
                        // public class C implements I<Integer> { Integer m() { return 42; } }
                        // C.class will have both "Object m()" and "Integer m()" methods.
                        List<Method> methodList = accessibles.get(sig);
                        if (methodList == null) {
                            // TODO Collection.singletonList is more efficient, though read only.
                            methodList = new LinkedList<>();
                            accessibles.put(sig, methodList);
                        }
                        methodList.add(method);
                    }
                }
                return;
            } catch (SecurityException e) {
                LOG.warn("Could not discover accessible methods of class {}, attemping superclasses/interfaces.",
                        clazz.getName(), e);
                // Fall through and attempt to discover superclass/interface methods
            }
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            discoverAccessibleMethods(anInterface, accessibles);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            discoverAccessibleMethods(superclass, accessibles);
        }
    }

    // This is needed as java.bean.Introspector sometimes gives back a method that's actually not accessible,
    // as it's an override of an accessible method in a non-public subclass. While that's still a public method, calling
    // it directly via reflection will throw java.lang.IllegalAccessException, and we are supposed to call the overidden
    // accessible method instead. Like, we might get two PropertyDescriptor-s for the same property name, and only one
    // will have a reader method that we can actually call. So we have to find that method here.
    // Furthermore, the return type of the inaccessible method is possibly different (more specific) than the return
    // type of the overridden accessible method. Also Introspector behavior changed with Java 9, as earlier in such
    // case the Introspector returned all variants of the method (so the accessible one was amongst them at least),
    // while in Java 9 it apparently always returns one variant only, but that's sometimes (not sure if it's
    // predictable) the inaccessible one.
    private static Method getMatchingAccessibleMethod(Method m, Map<ExecutableMemberSignature, List<Method>> accessibles) {
        if (m == null) {
            return null;
        }
        ExecutableMemberSignature sig = new ExecutableMemberSignature(m);
        List<Method> ams = accessibles.get(sig);
        if (ams == null) {
            return null;
        }
        // Note that this algorithm was different, and more involved in 2.3.31+, as it was paranoid about breaking
        // applications that worked before. Here we just go for the simplest solution, that should work with sanely
        // generated classes.
        for (Method am : ams) {
            if (m == am) {
                return am;
            }
        }
        Class<?> mReturnType = m.getReturnType();
        for (Method am : ams) {
            if (am.getReturnType() == mReturnType) {
                return am;
            }
        }
        for (Method am : ams) {
            // An overriding method might narrows the return type. The inaccessible method can be either the overrider,
            // or the overridden. But fore example none of Number m() and String m() could override the other, as
            // neither return type is a subtype of the other.
            Class<?> amReturnType = am.getReturnType();
            if (amReturnType.isAssignableFrom(mReturnType) || mReturnType.isAssignableFrom(amReturnType)) {
                return am;
            }
        }
        return null;
    }

    private static Method getFirstAccessibleMethod(ExecutableMemberSignature sig, Map<ExecutableMemberSignature, List<Method>> accessibles) {
        List<Method> ams = accessibles.get(sig);
        if (ams == null || ams.isEmpty()) {
            return null;
        }
        return ams.get(0);
    }

    /**
     * As of this writing, this is only used for testing if method order really doesn't mater.
     */
    private void sortMethodDescriptors(List<MethodDescriptor> methodDescriptors) {
        if (methodSorter != null) {
            methodSorter.sortMethodDescriptors(methodDescriptors);
        }
    }

    /**
     * Returns the {@link MemberAccessPolicy} to actually use, which is not just
     * {@link DefaultObjectWrapper#getMemberAccessPolicy()} if {@link DefaultObjectWrapper#getExposureLevel()} is more
     * allowing than {@link DefaultObjectWrapper#EXPOSE_SAFE}. {@link DefaultObjectWrapper#EXPOSE_NOTHING} though is
     * not factored in here.
     */
    MemberAccessPolicy getEffectiveMemberAccessPolicy() {
        return exposureLevel < DefaultObjectWrapper.EXPOSE_SAFE ? AllowAllMemberAccessPolicy.INSTANCE
                : memberAccessPolicy;
    }

    private static Map<Method, Class<?>[]> getArgTypesByMethod(Map<Object, Object> classInfo) {
        @SuppressWarnings("unchecked")
        Map<Method, Class<?>[]> argTypes = (Map<Method, Class<?>[]>) classInfo.get(ARG_TYPES_BY_METHOD_KEY);
        if (argTypes == null) {
            argTypes = new HashMap<>();
            classInfo.put(ARG_TYPES_BY_METHOD_KEY, argTypes);
        }
        return argTypes;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Cache management:

    /**
     * Corresponds to {@link DefaultObjectWrapper#clearClassIntrospectionCache()}.
     */
    void clearCache() {
        if (getHasSharedInstanceRestrictions()) {
            throw new IllegalStateException(
                    "It's not allowed to clear the whole cache in a read-only " + getClass().getName() +
                            "instance. Use removeFromClassIntrospectionCache(String prefix) instead.");
        }
        forcedClearCache();
    }

    private void forcedClearCache() {
        synchronized (sharedLock) {
            cache.clear();
            cacheClassNames.clear();
            clearingCounter++;

            for (WeakReference<Object> regedMfREf : modelFactories) {
                Object regedMf = regedMfREf.get();
                if (regedMf != null) {
                    if (regedMf instanceof ClassBasedModelFactory) {
                        ((ClassBasedModelFactory) regedMf).clearCache();
                    } else {
                        throw new BugException();
                    }
                }
            }

            removeClearedModelFactoryReferences();
        }
    }

    /**
     * Corresponds to {@link DefaultObjectWrapper#removeFromClassIntrospectionCache(Class)}.
     */
    void remove(Class<?> clazz) {
        synchronized (sharedLock) {
            cache.remove(clazz);
            cacheClassNames.remove(clazz.getName());
            clearingCounter++;

            for (WeakReference<Object> regedMfREf : modelFactories) {
                Object regedMf = regedMfREf.get();
                if (regedMf != null) {
                    if (regedMf instanceof ClassBasedModelFactory) {
                        ((ClassBasedModelFactory) regedMf).removeFromCache(clazz);
                    } else {
                        throw new BugException();
                    }
                }
            }

            removeClearedModelFactoryReferences();
        }
    }

    /**
     * Returns the number of events so far that could make class introspection data returned earlier outdated.
     */
    int getClearingCounter() {
        synchronized (sharedLock) {
            return clearingCounter;
        }
    }

    private void onSameNameClassesDetected(String className) {
        // TODO: This behavior should be pluggable, as in environments where
        // some classes are often reloaded or multiple versions of the
        // same class is normal (OSGi), this will drop the cache contents
        // too often.
        LOG.info(
                "Detected multiple classes with the same name, \"{}\". "
                + "Assuming it was a class-reloading. Clearing class introspection caches to release old data.",
                className);
        forcedClearCache();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Managing dependent objects:

    void registerModelFactory(ClassBasedModelFactory mf) {
        registerModelFactory((Object) mf);
    }

    private void registerModelFactory(Object mf) {
        // Note that this `synchronized (sharedLock)` is also need for the DefaultObjectWrapper constructor to work safely.
        synchronized (sharedLock) {
            modelFactories.add(new WeakReference<>(mf, modelFactoriesRefQueue));
            removeClearedModelFactoryReferences();
        }
    }

    void unregisterModelFactory(ClassBasedModelFactory mf) {
        unregisterModelFactory((Object) mf);
    }

    void unregisterModelFactory(Object mf) {
        synchronized (sharedLock) {
            for (Iterator<WeakReference<Object>> it = modelFactories.iterator(); it.hasNext(); ) {
                Object regedMf = it.next().get();
                if (regedMf == mf) {
                    it.remove();
                }
            }

        }
    }

    private void removeClearedModelFactoryReferences() {
        Reference<?> cleardRef;
        while ((cleardRef = modelFactoriesRefQueue.poll()) != null) {
            synchronized (sharedLock) {
                findClearedRef: for (Iterator<WeakReference<Object>> it = modelFactories.iterator(); it.hasNext(); ) {
                    if (it.next() == cleardRef) {
                        it.remove();
                        break findClearedRef;
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Extracting from introspection info:

    static Class<?>[] getArgTypes(Map<Object, Object> classInfo, Method method) {
        @SuppressWarnings("unchecked")
        Map<Method, Class<?>[]> argTypesByMethod = (Map<Method, Class<?>[]>) classInfo.get(ARG_TYPES_BY_METHOD_KEY);
        return argTypesByMethod.get(method);
    }

    /**
     * Returns the number of introspected methods/properties that should be available via the TemplateHashModel
     * interface.
     */
    // TODO [FM3] Too slow. See also keySet().
    int keyCount(Class<?> clazz) {
        Map<Object, Object> map = get(clazz);
        int count = map.size();
        if (map.containsKey(CONSTRUCTORS_KEY)) count--;
        if (map.containsKey(GENERIC_GET_KEY)) count--;
        if (map.containsKey(ARG_TYPES_BY_METHOD_KEY)) count--;
        return count;
    }

    /**
     * Returns the Set of names of introspected methods/properties that should be available via the TemplateHashModel
     * interface.
     */
    // TODO [FM3] Far too slow. 
    @SuppressWarnings("rawtypes")
    Set<String> keySet(Class<?> clazz) {
        Set<Object> set = new HashSet<>(get(clazz).keySet());
        set.remove(CONSTRUCTORS_KEY);
        set.remove(GENERIC_GET_KEY);
        set.remove(ARG_TYPES_BY_METHOD_KEY);
        return (Set) set;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Properties

    int getExposureLevel() {
        return exposureLevel;
    }

    boolean getExposeFields() {
        return exposeFields;
    }

    MemberAccessPolicy getMemberAccessPolicy() {
        return memberAccessPolicy;
    }

    MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return methodAppearanceFineTuner;
    }

    MethodSorter getMethodSorter() {
        return methodSorter;
    }

    /**
     * Returns {@code true} if this instance was created with {@link Builder}, even if it wasn't
     * actually put into the cache (as we reserve the right to do so in later versions).
     */
    boolean getHasSharedInstanceRestrictions() {
        return hasSharedInstanceRestrictions;
    }

    /**
     * Tells if this instance is (potentially) shared among {@link DefaultObjectWrapper} instances.
     * 
     * @see #getHasSharedInstanceRestrictions()
     */
    boolean isShared() {
        return shared;
    }

    /**
     * Almost always, you want to use {@link DefaultObjectWrapper#getSharedIntrospectionLock()}, not this! The only exception is
     * when you get this to set the field returned by {@link DefaultObjectWrapper#getSharedIntrospectionLock()}.
     */
    Object getSharedLock() {
        return sharedLock;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Monitoring:

    /** For unit testing only */
    Object[] getRegisteredModelFactoriesSnapshot() {
        synchronized (sharedLock) {
            return modelFactories.toArray();
        }
    }

    static final class Builder implements CommonBuilder<ClassIntrospector>, Cloneable {

        private static final Map<Builder, Reference<ClassIntrospector>> INSTANCE_CACHE = new HashMap<>();
        private static final ReferenceQueue<ClassIntrospector> INSTANCE_CACHE_REF_QUEUE = new ReferenceQueue<>();

        private final Version incompatibleImprovements;

        // Properties and their *defaults*:
        private boolean sharingDisallowed;
        private boolean shardingDisallowedSet;
        private int exposureLevel = DefaultObjectWrapper.EXPOSE_SAFE;
        private boolean exposureLevelSet;
        private boolean exposeFields;
        private boolean exposeFieldsSet;
        private MemberAccessPolicy memberAccessPolicy;
        private boolean memberAccessPolicySet;
        private MethodAppearanceFineTuner methodAppearanceFineTuner;
        private boolean methodAppearanceFineTunerSet;
        private MethodSorter methodSorter;
        // Attention:
        // - This is also used as a cache key, so non-normalized field values should be avoided.
        // - If some field has a default value, it must be set until the end of the constructor. No field that has a
        //   default can be left unset (like null).
        // - If you add a new field, review all methods in this class, also the ClassIntrospector constructor

        private boolean alreadyBuilt;

        Builder(Version incompatibleImprovements) {
            // Warning: incompatibleImprovements must not affect this object at versions increments where there's no
            // change in the DefaultObjectWrapper.normalizeIncompatibleImprovements results. That is, this class may
            // don't react to some version changes that affects DefaultObjectWrapper, but not the other way around.
            this.incompatibleImprovements = normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
            // Currently nothing depends on incompatibleImprovements
            memberAccessPolicy = DefaultMemberAccessPolicy.getInstance(this.incompatibleImprovements);
        }

        private static Version normalizeIncompatibleImprovementsVersion(Version incompatibleImprovements) {
            _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
            // All breakpoints here must occur in DefaultObjectWrapper.normalizeIncompatibleImprovements!
            return Configuration.VERSION_3_0_0;
        }

        @Override
        protected Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Failed to deepClone Builder", e);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + incompatibleImprovements.hashCode();
            result = prime * result + (sharingDisallowed ? 1231 : 1237);
            result = prime * result + (exposeFields ? 1231 : 1237);
            result = prime * result + exposureLevel;
            result = prime * result + memberAccessPolicy.hashCode();
            result = prime * result + System.identityHashCode(methodAppearanceFineTuner);
            result = prime * result + System.identityHashCode(methodSorter);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Builder other = (Builder) obj;

            if (!incompatibleImprovements.equals(other.incompatibleImprovements)) return false;
            if (sharingDisallowed != other.sharingDisallowed) return false;
            if (exposeFields != other.exposeFields) return false;
            if (exposureLevel != other.exposureLevel) return false;
            if (!memberAccessPolicy.equals(other.memberAccessPolicy)) return false;
            if (methodAppearanceFineTuner != other.methodAppearanceFineTuner) return false;
            return methodSorter == other.methodSorter;
        }

        public boolean getShareable() {
            return sharingDisallowed;
        }

        /**
         * Can be used to prevent the sharing of the {@link ClassIntrospector} through the global cache. Setting this
         * to {@code false} doesn't guarantee that it will be shared (as some other setting values can make that
         * impossible), but setting this to {@code true} guarantees that it won't be shared. Defaults to {@code false}.
         *
         * @see DefaultObjectWrapper.ExtendableBuilder#setUsePrivateCaches(boolean)
         */
        public void setSharingDisallowed(boolean shareable) {
            this.sharingDisallowed = shareable;
            shardingDisallowedSet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isShardingDisallowedSet() {
            return shardingDisallowedSet;
        }

        public int getExposureLevel() {
            return exposureLevel;
        }

        /** See {@link DefaultObjectWrapper.ExtendableBuilder#setExposureLevel(int)}. */
        public void setExposureLevel(int exposureLevel) {
            if (exposureLevel < DefaultObjectWrapper.EXPOSE_ALL || exposureLevel > DefaultObjectWrapper.EXPOSE_NOTHING) {
                throw new IllegalArgumentException("Illegal exposure level: " + exposureLevel);
            }

            this.exposureLevel = exposureLevel;
            exposureLevelSet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isExposureLevelSet() {
            return exposureLevelSet;
        }

        public boolean getExposeFields() {
            return exposeFields;
        }

        /** See {@link DefaultObjectWrapper.ExtendableBuilder#setExposeFields(boolean)}. */
        public void setExposeFields(boolean exposeFields) {
            this.exposeFields = exposeFields;
            exposeFieldsSet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isExposeFieldsSet() {
            return exposeFieldsSet;
        }

        public MemberAccessPolicy getMemberAccessPolicy() {
            return memberAccessPolicy;
        }

        public void setMemberAccessPolicy(MemberAccessPolicy memberAccessPolicy) {
            _NullArgumentException.check(memberAccessPolicy);
            this.memberAccessPolicy = memberAccessPolicy;
            memberAccessPolicySet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isMemberAccessPolicySet() {
            return memberAccessPolicySet;
        }

        public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
            return methodAppearanceFineTuner;
        }

        public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
            this.methodAppearanceFineTuner = methodAppearanceFineTuner;
            methodAppearanceFineTunerSet = true;
        }

        /**
         * Tells if the property was explicitly set, as opposed to just holding its default value.
         */
        public boolean isMethodAppearanceFineTunerSet() {
            return methodAppearanceFineTunerSet;
        }

        public MethodSorter getMethodSorter() {
            return methodSorter;
        }

        public void setMethodSorter(MethodSorter methodSorter) {
            this.methodSorter = methodSorter;
        }

        /**
         * Returns the normalized incompatible improvements.
         */
        public Version getIncompatibleImprovements() {
            return incompatibleImprovements;
        }

        private static void removeClearedReferencesFromInstanceCache() {
            Reference<? extends ClassIntrospector> clearedRef;
            while ((clearedRef = INSTANCE_CACHE_REF_QUEUE.poll()) != null) {
                synchronized (INSTANCE_CACHE) {
                    findClearedRef: for (Iterator<Reference<ClassIntrospector>> it = INSTANCE_CACHE.values().iterator();
                             it.hasNext(); ) {
                        if (it.next() == clearedRef) {
                            it.remove();
                            break findClearedRef;
                        }
                    }
                }
            }
        }

        /** For unit testing only */
        static void clearInstanceCache() {
            synchronized (INSTANCE_CACHE) {
                INSTANCE_CACHE.clear();
            }
        }

        /** For unit testing only */
        static Map<Builder, Reference<ClassIntrospector>> getInstanceCache() {
            return INSTANCE_CACHE;
        }

        /**
         * Returns an instance that is possibly shared (singleton). Note that this comes with its own "shared lock",
         * since everyone who uses this object will have to lock with that common object.
         */
        @Override
        public ClassIntrospector build() {
            if (alreadyBuilt) {
                throw new IllegalStateException("build() can only be executed once.");
            }

            ClassIntrospector instance;
            if (!sharingDisallowed
                    && (methodAppearanceFineTuner == null || methodAppearanceFineTuner instanceof SingletonCustomizer)
                    && (methodSorter == null || methodSorter instanceof SingletonCustomizer)) {
                // Instance can be cached.
                synchronized (INSTANCE_CACHE) {
                    Reference<ClassIntrospector> instanceRef = INSTANCE_CACHE.get(this);
                    instance = instanceRef != null ? instanceRef.get() : null;
                    if (instance == null) {
                        Builder thisClone = (Builder) clone();  // prevent any aliasing issues
                        instance = new ClassIntrospector(thisClone, new Object(), true, true);
                        INSTANCE_CACHE.put(thisClone, new WeakReference<>(instance, INSTANCE_CACHE_REF_QUEUE));
                    }
                }

                removeClearedReferencesFromInstanceCache();
            } else {
                // If methodAppearanceFineTuner or methodSorter is specified and isn't marked as a singleton, the
                // ClassIntrospector can't be shared/cached as those objects could contain a back-reference to the
                // DefaultObjectWrapper.
                instance = new ClassIntrospector(this, new Object(), !sharingDisallowed, false);
            }

            alreadyBuilt = true;
            return instance;
        }

    }
}

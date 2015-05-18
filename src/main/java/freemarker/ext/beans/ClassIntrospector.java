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

package freemarker.ext.beans;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.core.BugException;
import freemarker.core._ConcurrentMapFactory;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecision;
import freemarker.ext.beans.BeansWrapper.MethodAppearanceDecisionInput;
import freemarker.ext.util.ModelCache;
import freemarker.log.Logger;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility.SecurityUtilities;

/**
 * Returns information about a {@link Class} that's useful for FreeMarker. Encapsulates a cache for this. Thread-safe,
 * doesn't require "proper publishing" starting from Java 5. Immutable, with the exception of the internal caches.
 * 
 * <p>
 * Note that instances of this are cached on the level of FreeMarker's defining class loader. Hence, it must not do
 * operations that depend on the Thread Context Class Loader, such as resolving class names.
 */
class ClassIntrospector {

    // Attention: This class must be thread-safe (not just after proper publishing). This is important as some of
    // these are shared by many object wrappers, and concurrency related glitches due to user errors must remain
    // local to the object wrappers, not corrupting the shared ClassIntrospector.

    private static final Logger LOG = Logger.getLogger("freemarker.beans");

    private static final String JREBEL_SDK_CLASS_NAME = "org.zeroturnaround.javarebel.ClassEventListener";
    private static final String JREBEL_INTEGRATION_ERROR_MSG
            = "Error initializing JRebel integration. JRebel integration disabled.";

    /**
     * When this property is true, some things are stricter. This is mostly to catch suspicious things in development
     * that can otherwise be valid situations.
     */
    static final boolean DEVELOPMENT_MODE = "true".equals(SecurityUtilities.getSystemProperty("freemarker.development",
            "false"));

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
                        Class.forName("freemarker.ext.beans.JRebelClassChangeNotifier").newInstance();
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

    private static final Object ARGTYPES_KEY = new Object();
    static final Object CONSTRUCTORS_KEY = new Object();
    static final Object GENERIC_GET_KEY = new Object();

    // -----------------------------------------------------------------------------------------------------------------
    // Introspection configuration properties:

    // Note: These all must be *declared* final (or else synchronization is needed everywhere where they are accessed).

    final int exposureLevel;
    final boolean exposeFields;
    final MethodAppearanceFineTuner methodAppearanceFineTuner;
    final MethodSorter methodSorter;
    final boolean bugfixed;

    /** See {@link #getHasSharedInstanceRestrictons()} */
    final private boolean hasSharedInstanceRestrictons;

    /** See {@link #isShared()} */
    final private boolean shared;

    // -----------------------------------------------------------------------------------------------------------------
    // State fields:

    private final Object sharedLock;
    private final Map/* <Class, Map<String, Object>> */cache = _ConcurrentMapFactory.newMaybeConcurrentHashMap(0,
            0.75f, 16);
    private final boolean isCacheConcurrentMap = _ConcurrentMapFactory.isConcurrent(cache);
    private final Set/* <String> */cacheClassNames = new HashSet(0);
    private final Set/* <Class> */classIntrospectionsInProgress = new HashSet(0);

    private final List/* <WeakReference<ClassBasedModelFactory|ModelCache>> */modelFactories = new LinkedList();
    private final ReferenceQueue modelFactoriesRefQueue = new ReferenceQueue();

    private int clearingCounter;

    // -----------------------------------------------------------------------------------------------------------------
    // Instantiation:

    /**
     * Creates a new instance, that is hence surely not shared (singleton) instance.
     * 
     * @param pa
     *            Stores what the values of the JavaBean properties of the returned instance will be. Not {@code null}.
     */
    ClassIntrospector(ClassIntrospectorBuilder pa, Object sharedLock) {
        this(pa, sharedLock, false, false);
    }

    /**
     * @param hasSharedInstanceRestrictons
     *            {@code true} exactly if we are creating a new instance with {@link ClassIntrospectorBuilder}. Then
     *            it's {@code true} even if it won't put the instance into the cache.
     */
    ClassIntrospector(ClassIntrospectorBuilder builder, Object sharedLock,
            boolean hasSharedInstanceRestrictons, boolean shared) {
        NullArgumentException.check("sharedLock", sharedLock);

        this.exposureLevel = builder.getExposureLevel();
        this.exposeFields = builder.getExposeFields();
        this.methodAppearanceFineTuner = builder.getMethodAppearanceFineTuner();
        this.methodSorter = builder.getMethodSorter();
        this.bugfixed = builder.isBugfixed();

        this.sharedLock = sharedLock;

        this.hasSharedInstanceRestrictons = hasSharedInstanceRestrictons;
        this.shared = shared;

        if (CLASS_CHANGE_NOTIFIER != null) {
            CLASS_CHANGE_NOTIFIER.subscribe(this);
        }
    }

    /**
     * Returns a {@link ClassIntrospectorBuilder}-s that could be used to create an identical {@link #ClassIntrospector}
     * . The returned {@link ClassIntrospectorBuilder} can be modified without interfering with anything.
     */
    ClassIntrospectorBuilder getPropertyAssignments() {
        return new ClassIntrospectorBuilder(this);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Introspection:

    /**
     * Gets the class introspection data from {@link #cache}, automatically creating the cache entry if it's missing.
     * 
     * @return A {@link Map} where each key is a property/method/field name (or a special {@link Object} key like
     *         {@link #CONSTRUCTORS_KEY}), each value is a {@link PropertyDescriptor} or {@link Method} or
     *         {@link OverloadedMethods} or {@link Field} (but better check the source code...).
     */
    Map get(Class clazz) {
        if (isCacheConcurrentMap) {
            Map introspData = (Map) cache.get(clazz);
            if (introspData != null) return introspData;
        }

        String className;
        synchronized (sharedLock) {
            Map introspData = (Map) cache.get(clazz);
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
                    introspData = (Map) cache.get(clazz);
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
            Map introspData = createClassIntrospectionData(clazz);
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
    private Map createClassIntrospectionData(Class clazz) {
        final Map introspData = new HashMap();

        if (exposeFields) {
            addFieldsToClassIntrospectionData(introspData, clazz);
        }

        final Map accessibleMethods = discoverAccessibleMethods(clazz);

        addGenericGetToClassIntrospectionData(introspData, accessibleMethods);

        if (exposureLevel != BeansWrapper.EXPOSE_NOTHING) {
            try {
                addBeanInfoToClassIntrospectionData(introspData, clazz, accessibleMethods);
            } catch (IntrospectionException e) {
                LOG.warn("Couldn't properly perform introspection for class " + clazz, e);
                introspData.clear(); // FIXME NBC: Don't drop everything here.
            }
        }

        addConstructorsToClassIntrospectionData(introspData, clazz);

        if (introspData.size() > 1) {
            return introspData;
        } else if (introspData.size() == 0) {
            return Collections.EMPTY_MAP;
        } else { // map.size() == 1
            Map.Entry e = (Map.Entry) introspData.entrySet().iterator().next();
            return Collections.singletonMap(e.getKey(), e.getValue());
        }
    }

    private void addFieldsToClassIntrospectionData(Map introspData, Class clazz)
            throws SecurityException {
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                introspData.put(field.getName(), field);
            }
        }
    }

    private void addBeanInfoToClassIntrospectionData(Map introspData, Class clazz, Map accessibleMethods)
            throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);

        PropertyDescriptor[] pda = beanInfo.getPropertyDescriptors();
        if (pda != null) {
            int pdaLength = pda.length;
            for (int i = pdaLength - 1; i >= 0; --i) {
                addPropertyDescriptorToClassIntrospectionData(
                        introspData, pda[i], clazz,
                        accessibleMethods);
            }
        }

        if (exposureLevel < BeansWrapper.EXPOSE_PROPERTIES_ONLY) {
            final MethodAppearanceDecision decision = new MethodAppearanceDecision();
            MethodAppearanceDecisionInput decisionInput = null;
            final MethodDescriptor[] mda = sortMethodDescriptors(beanInfo.getMethodDescriptors());
            if (mda != null) {
                int mdaLength = mda.length;
                for (int i = mdaLength - 1; i >= 0; --i) {
                    final MethodDescriptor md = mda[i];
                    final Method method = getMatchingAccessibleMethod(md.getMethod(), accessibleMethods);
                    if (method != null && isAllowedToExpose(method)) {
                        decision.setDefaults(method);
                        if (methodAppearanceFineTuner != null) {
                            if (decisionInput == null) {
                                decisionInput = new MethodAppearanceDecisionInput();
                            }
                            decisionInput.setContainingClass(clazz);
                            decisionInput.setMethod(method);
    
                            methodAppearanceFineTuner.process(decisionInput, decision);
                        }
    
                        PropertyDescriptor propDesc = decision.getExposeAsProperty();
                        if (propDesc != null && !(introspData.get(propDesc.getName()) instanceof PropertyDescriptor)) {
                            addPropertyDescriptorToClassIntrospectionData(
                                    introspData, propDesc, clazz, accessibleMethods);
                        }
    
                        String methodKey = decision.getExposeMethodAs();
                        if (methodKey != null) {
                            Object previous = introspData.get(methodKey);
                            if (previous instanceof Method) {
                                // Overloaded method - replace Method with a OverloadedMethods
                                OverloadedMethods overloadedMethods = new OverloadedMethods(bugfixed);
                                overloadedMethods.addMethod((Method) previous);
                                overloadedMethods.addMethod(method);
                                introspData.put(methodKey, overloadedMethods);
                                // Remove parameter type information
                                getArgTypes(introspData).remove(previous);
                            } else if (previous instanceof OverloadedMethods) {
                                // Already overloaded method - add new overload
                                ((OverloadedMethods) previous).addMethod(method);
                            } else if (decision.getMethodShadowsProperty()
                                    || !(previous instanceof PropertyDescriptor)) {
                                // Simple method (this far)
                                introspData.put(methodKey, method);
                                getArgTypes(introspData).put(method,
                                        method.getParameterTypes());
                            }
                        }
                    }
                } // for each in mda
            } // if mda != null
        } // end if (exposureLevel < EXPOSE_PROPERTIES_ONLY)
    }

    private void addPropertyDescriptorToClassIntrospectionData(Map introspData,
            PropertyDescriptor pd, Class clazz, Map accessibleMethods) {
        if (pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd =
                    (IndexedPropertyDescriptor) pd;
            Method readMethod = ipd.getIndexedReadMethod();
            Method publicReadMethod = getMatchingAccessibleMethod(readMethod, accessibleMethods);
            if (publicReadMethod != null && isAllowedToExpose(publicReadMethod)) {
                try {
                    if (readMethod != publicReadMethod) {
                        ipd = new IndexedPropertyDescriptor(
                                ipd.getName(), ipd.getReadMethod(),
                                null, publicReadMethod,
                                null);
                    }
                    introspData.put(ipd.getName(), ipd);
                    getArgTypes(introspData).put(publicReadMethod, publicReadMethod.getParameterTypes());
                } catch (IntrospectionException e) {
                    LOG.warn("Failed creating a publicly-accessible " +
                            "property descriptor for " + clazz.getName() +
                            " indexed property " + pd.getName() +
                            ", read method " + publicReadMethod,
                            e);
                }
            }
        } else {
            Method readMethod = pd.getReadMethod();
            Method publicReadMethod = getMatchingAccessibleMethod(readMethod, accessibleMethods);
            if (publicReadMethod != null && isAllowedToExpose(publicReadMethod)) {
                try {
                    if (readMethod != publicReadMethod) {
                        pd = new PropertyDescriptor(pd.getName(), publicReadMethod, null);
                        pd.setReadMethod(publicReadMethod);
                    }
                    introspData.put(pd.getName(), pd);
                } catch (IntrospectionException e) {
                    LOG.warn("Failed creating a publicly-accessible " +
                            "property descriptor for " + clazz.getName() +
                            " property " + pd.getName() + ", read method " +
                            publicReadMethod, e);
                }
            }
        }
    }

    private void addGenericGetToClassIntrospectionData(Map introspData,
            Map accessibleMethods) {
        Method genericGet = getFirstAccessibleMethod(
                MethodSignature.GET_STRING_SIGNATURE, accessibleMethods);
        if (genericGet == null) {
            genericGet = getFirstAccessibleMethod(
                    MethodSignature.GET_OBJECT_SIGNATURE, accessibleMethods);
        }
        if (genericGet != null) {
            introspData.put(GENERIC_GET_KEY, genericGet);
        }
    }

    private void addConstructorsToClassIntrospectionData(final Map introspData,
            Class clazz) {
        try {
            Constructor[] ctors = clazz.getConstructors();
            if (ctors.length == 1) {
                Constructor ctor = ctors[0];
                introspData.put(CONSTRUCTORS_KEY, new SimpleMethod(ctor, ctor.getParameterTypes()));
            } else if (ctors.length > 1) {
                OverloadedMethods ctorMap = new OverloadedMethods(bugfixed);
                for (int i = 0; i < ctors.length; i++) {
                    ctorMap.addConstructor(ctors[i]);
                }
                introspData.put(CONSTRUCTORS_KEY, ctorMap);
            }
        } catch (SecurityException e) {
            LOG.warn("Can't discover constructors for class " + clazz.getName(), e);
        }
    }

    /**
     * Retrieves mapping of {@link MethodSignature}-s to a {@link List} of accessible methods for a class. In case the
     * class is not public, retrieves methods with same signature as its public methods from public superclasses and
     * interfaces. Basically upcasts every method to the nearest accessible method.
     */
    private static Map discoverAccessibleMethods(Class clazz) {
        Map accessibles = new HashMap();
        discoverAccessibleMethods(clazz, accessibles);
        return accessibles;
    }

    private static void discoverAccessibleMethods(Class clazz, Map accessibles) {
        if (Modifier.isPublic(clazz.getModifiers())) {
            try {
                Method[] methods = clazz.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    MethodSignature sig = new MethodSignature(method);
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
                    List methodList = (List) accessibles.get(sig);
                    if (methodList == null) {
                        methodList = new LinkedList();
                        accessibles.put(sig, methodList);
                    }
                    methodList.add(method);
                }
                return;
            } catch (SecurityException e) {
                LOG.warn("Could not discover accessible methods of class " +
                        clazz.getName() +
                        ", attemping superclasses/interfaces.", e);
                // Fall through and attempt to discover superclass/interface methods
            }
        }

        Class[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            discoverAccessibleMethods(interfaces[i], accessibles);
        }
        Class superclass = clazz.getSuperclass();
        if (superclass != null) {
            discoverAccessibleMethods(superclass, accessibles);
        }
    }

    private static Method getMatchingAccessibleMethod(Method m, Map accessibles) {
        if (m == null) {
            return null;
        }
        MethodSignature sig = new MethodSignature(m);
        List l = (List) accessibles.get(sig);
        if (l == null) {
            return null;
        }
        for (Iterator iterator = l.iterator(); iterator.hasNext();) {
            Method am = (Method) iterator.next();
            if (am.getReturnType() == m.getReturnType()) {
                return am;
            }
        }
        return null;
    }

    private static Method getFirstAccessibleMethod(MethodSignature sig, Map accessibles) {
        List l = (List) accessibles.get(sig);
        if (l == null || l.isEmpty()) {
            return null;
        }
        return (Method) l.iterator().next();
    }

    /**
     * As of this writing, this is only used for testing if method order really doesn't mater.
     */
    private MethodDescriptor[] sortMethodDescriptors(MethodDescriptor[] methodDescriptors) {
        return methodSorter != null ? methodSorter.sortMethodDescriptors(methodDescriptors) : methodDescriptors;
    }

    boolean isAllowedToExpose(Method method) {
        return exposureLevel < BeansWrapper.EXPOSE_SAFE || !UnsafeMethods.isUnsafeMethod(method);
    }

    private static Map getArgTypes(Map classMap) {
        Map argTypes = (Map) classMap.get(ARGTYPES_KEY);
        if (argTypes == null) {
            argTypes = new HashMap();
            classMap.put(ARGTYPES_KEY, argTypes);
        }
        return argTypes;
    }

    private static final class MethodSignature {
        private static final MethodSignature GET_STRING_SIGNATURE =
                new MethodSignature("get", new Class[] { String.class });
        private static final MethodSignature GET_OBJECT_SIGNATURE =
                new MethodSignature("get", new Class[] { Object.class });

        private final String name;
        private final Class[] args;

        private MethodSignature(String name, Class[] args) {
            this.name = name;
            this.args = args;
        }

        MethodSignature(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        public boolean equals(Object o) {
            if (o instanceof MethodSignature) {
                MethodSignature ms = (MethodSignature) o;
                return ms.name.equals(name) && Arrays.equals(args, ms.args);
            }
            return false;
        }

        public int hashCode() {
            return name.hashCode() ^ args.length; // TODO That's a poor quality hash... isn't this a problem?
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Cache management:

    /**
     * Corresponds to {@link BeansWrapper#clearClassIntrospecitonCache()}.
     * 
     * @since 2.3.20
     */
    void clearCache() {
        if (getHasSharedInstanceRestrictons()) {
            throw new IllegalStateException(
                    "It's not allowed to clear the whole cache in a read-only " + this.getClass().getName() +
                            "instance. Use removeFromClassIntrospectionCache(String prefix) instead.");
        }
        forcedClearCache();
    }

    private void forcedClearCache() {
        synchronized (sharedLock) {
            cache.clear();
            cacheClassNames.clear();
            clearingCounter++;

            for (Iterator it = modelFactories.iterator(); it.hasNext();) {
                Object regedMf = ((WeakReference) it.next()).get();
                if (regedMf != null) {
                    if (regedMf instanceof ClassBasedModelFactory) {
                        ((ClassBasedModelFactory) regedMf).clearCache();
                    } else if (regedMf instanceof ModelCache) {
                        ((ModelCache) regedMf).clearCache();
                    } else {
                        throw new BugException();
                    }
                }
            }

            removeClearedModelFactoryReferences();
        }
    }

    /**
     * Corresponds to {@link BeansWrapper#removeFromClassIntrospectionCache(Class)}.
     * 
     * @since 2.3.20
     */
    void remove(Class clazz) {
        synchronized (sharedLock) {
            cache.remove(clazz);
            cacheClassNames.remove(clazz.getName());
            clearingCounter++;

            for (Iterator it = modelFactories.iterator(); it.hasNext();) {
                Object regedMf = ((WeakReference) it.next()).get();
                if (regedMf != null) {
                    if (regedMf instanceof ClassBasedModelFactory) {
                        ((ClassBasedModelFactory) regedMf).removeFromCache(clazz);
                    } else if (regedMf instanceof ModelCache) {
                        ((ModelCache) regedMf).clearCache(); // doesn't support selective clearing ATM
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
        if (LOG.isInfoEnabled()) {
            LOG.info(
                    "Detected multiple classes with the same name, \"" + className +
                            "\". Assuming it was a class-reloading. Clearing class introspection " +
                            "caches to release old data.");
        }
        forcedClearCache();
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Managing dependent objects:

    void registerModelFactory(ClassBasedModelFactory mf) {
        registerModelFactory((Object) mf);
    }

    void registerModelFactory(ModelCache mf) {
        registerModelFactory((Object) mf);
    }

    private void registerModelFactory(Object mf) {
        // Note that this `synchronized (sharedLock)` is also need for the BeansWrapper constructor to work safely.
        synchronized (sharedLock) {
            modelFactories.add(new WeakReference(mf, modelFactoriesRefQueue));
            removeClearedModelFactoryReferences();
        }
    }

    void unregisterModelFactory(ClassBasedModelFactory mf) {
        unregisterModelFactory((Object) mf);
    }

    void unregisterModelFactory(ModelCache mf) {
        unregisterModelFactory((Object) mf);
    }

    void unregisterModelFactory(Object mf) {
        synchronized (sharedLock) {
            for (Iterator it = modelFactories.iterator(); it.hasNext();) {
                Object regedMf = ((Reference) it.next()).get();
                if (regedMf == mf) {
                    it.remove();
                }
            }

        }
    }

    private void removeClearedModelFactoryReferences() {
        Reference cleardRef;
        while ((cleardRef = modelFactoriesRefQueue.poll()) != null) {
            synchronized (sharedLock) {
                findCleardRef: for (Iterator it = modelFactories.iterator(); it.hasNext();) {
                    if (it.next() == cleardRef) {
                        it.remove();
                        break findCleardRef;
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Extracting from introspection info:

    static Class[] getArgTypes(Map classMap, AccessibleObject methodOrCtor) {
        return (Class[]) ((Map) classMap.get(ARGTYPES_KEY)).get(methodOrCtor);
    }

    /**
     * Returns the number of introspected methods/properties that should be available via the TemplateHashModel
     * interface.
     */
    int keyCount(Class clazz) {
        Map map = get(clazz);
        int count = map.size();
        if (map.containsKey(CONSTRUCTORS_KEY)) count--;
        if (map.containsKey(GENERIC_GET_KEY)) count--;
        if (map.containsKey(ARGTYPES_KEY)) count--;
        return count;
    }

    /**
     * Returns the Set of names of introspected methods/properties that should be available via the TemplateHashModel
     * interface.
     */
    Set keySet(Class clazz) {
        Set set = new HashSet(get(clazz).keySet());
        set.remove(CONSTRUCTORS_KEY);
        set.remove(GENERIC_GET_KEY);
        set.remove(ARGTYPES_KEY);
        return set;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Properties

    int getExposureLevel() {
        return exposureLevel;
    }

    boolean getExposeFields() {
        return exposeFields;
    }

    MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return methodAppearanceFineTuner;
    }

    MethodSorter getMethodSorter() {
        return methodSorter;
    }

    /**
     * Returns {@code true} if this instance was created with {@link ClassIntrospectorBuilder}, even if it wasn't
     * actually put into the cache (as we reserve the right to do so in later versions).
     */
    boolean getHasSharedInstanceRestrictons() {
        return hasSharedInstanceRestrictons;
    }

    /**
     * Tells if this instance is (potentially) shared among {@link BeansWrapper} instances.
     * 
     * @see #getHasSharedInstanceRestrictons()
     */
    boolean isShared() {
        return shared;
    }

    /**
     * Almost always, you want to use {@link BeansWrapper#getSharedIntrospectionLock()}, not this! The only exception is
     * when you get this to set the field returned by {@link BeansWrapper#getSharedIntrospectionLock()}.
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

}

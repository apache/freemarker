package freemarker.template;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import freemarker.core._DelayedConversionToString;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.Collections12;
import freemarker.template.utility.Lockable;
import freemarker.template.utility.StringUtil;
import freemarker.template.utility._MethodUtil;

/**
 * Singleton-related functionality of {@link Configuration} separated to keep the code more organized. 
 */
class ConfigurationSingletons {

    private static final String NORMALIZE_ICI_VERSION_METHOD_NAME = "normalizeIncompatibleImprovementsVersion";
    private static final String NORMALIZE_CONSTRUCTOR_ARGUMENTS_METHOD_NAME = "normalizeConstructorArguments";
    private static final String GET_PROPERTY_DEFAULTS = "getPropertyDefaults";
    
    private static final Map/*<SingletonObjectKey, Object|SingletonObectSoftReference>*/ singletons = new HashMap();
    private static final ReferenceQueue singletonReferenceQueue = new ReferenceQueue();
    
    /** See documentation at {@link Configuration#getSingleton(Class, Object[], Map, boolean)} */
    static Object getSingleton(
            Class singletonClass, Object[] constrArgs, Map/*<String, Object>*/ properties,
            boolean strongRef)
            throws IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException, IntrospectionException {
        if (constrArgs == null) constrArgs = Collections12.EMPTY_OBJECT_ARRAY;
        if (properties == null) properties = Collections12.EMPTY_MAP;
        
        pollSingletonReferenceQueue();
        
        final SingletonObjectKey singletonKey = new SingletonObjectKey(singletonClass, constrArgs, properties);
        
        synchronized (singletons) {
            
            // See if we already have a matching singleton:
            final Reference ref;
            final Object singleton;
            final Object val = singletons.get(singletonKey);
            if (val instanceof Reference) {
                ref = (Reference) val;
                singleton = ref.get(); 
            } else if (val != null) {
                return val;
            } else {
                ref = null;
                singleton = null;
            }
            
            if (singleton == null) {  // No matching singleton has existed
                if (!Lockable.class.isAssignableFrom(singletonClass)) {
                    throw new IllegalArgumentException(
                            "The argument class, " + singletonClass.getName() + ", must implement "
                            + Lockable.class.getName() + ".");
                }
                if ((singletonClass.getModifiers() & Modifier.ABSTRACT) != 0) {
                    throw new IllegalArgumentException(
                            "The argument class, " + singletonClass.getName() + ", can't be abstract.");
                }
                
                // Replace the Version with the normalized value: 
                if (singletonKey.versionArgumentOverride != null) {
                    for (int i = 0; i < constrArgs.length; i++) {
                        if (constrArgs[i] instanceof Version) {
                            constrArgs = (Object[]) constrArgs.clone();  // Java 5: remove casting before .clone()-s
                            constrArgs[i] = singletonKey.versionArgumentOverride;
                            break;
                        }
                    }
                }
                
                // Create the singleton object:
                // - Get matching constructor:
                Constructor constr = _BeansAPI.getConstructor(singletonClass, constrArgs);
                // - Ensure that it doesn't have null for the first Version argument (as that we couldn't normalize):
                Class[] constrParamTypes = constr.getParameterTypes();
                for (int i = 0; i < constrParamTypes.length; i++) {
                    if (Version.class.isAssignableFrom(constrParamTypes[i])) {
                        if (constrArgs[i] == null) {
                            throw new IllegalArgumentException("The first Version argument to this constructor "
                                        + "can't be null when creating a singleton: " + constr);
                        }
                        break;  // We only care about the 1st one, as that's what should have been normalized
                    }
                }
                // - Create the instance:
                final Object newSingleton = _BeansAPI.newInstance(constr, constrArgs);

                if (!singletonKey.propertiesKey.isEmpty()) {
                    final PropertyDescriptor[] propDescsArray
                            = Introspector.getBeanInfo(singletonClass).getPropertyDescriptors();
                    final HashMap propDescs = new HashMap(propDescsArray.length * 4 / 3, 1f);
                    // Collect JavaBean propertiesKey info:
                    if (propDescsArray != null) {
                        for (int i = 0; i < propDescsArray.length; i++) {
                            PropertyDescriptor propDesc = propDescsArray[i];
                            propDescs.put(propDesc.getName(), propDesc);
                        }
                    }
                    
                    // Set JavaBean properties:
                    if (properties.size() != 0) {
                        final Iterator propsIter = properties.entrySet().iterator();
                        while (propsIter.hasNext()) {
                            final Map.Entry ent = (Map.Entry) propsIter.next();
                            final String propName = (String) ent.getKey();
                            Method writeMethod = ((PropertyDescriptor) propDescs.get(propName)).getWriteMethod();
                            if (writeMethod == null) {
                                if (propDescs.containsKey(propName)) {
                                    throw new NoSuchMethodException(
                                            "The " + StringUtil.jQuote(propName)
                                            + " JavaBean property exists but has no public setter method in "
                                            + singletonClass.getName() + ".");
                                } else {
                                    throw new NoSuchMethodException(
                                            "There's no " + StringUtil.jQuote(propName) + " JavaBean property in " +
                                            singletonClass.getName() + ".");
                                }
                            }
                            writeMethod.invoke(newSingleton, new Object[] { ent.getValue() });
                        }
                    }
                    
                    // Check if the default property values were correct:
                    Map propsWithDefaults = singletonKey.propertiesKey;
                    Iterator propsWDIter = propsWithDefaults.entrySet().iterator(); 
                    while (propsWDIter.hasNext()) {
                        Map.Entry ent = (Map.Entry) propsWDIter.next();
                        Object propName = ent.getKey();
                        if (!properties.containsKey(propName)) {
                            PropertyDescriptor propDesc = (PropertyDescriptor) propDescs.get(propName);
                            if (propDesc == null) {
                                throw new RuntimeException(
                                        "The JavaBean property " + StringUtil.jQuote(propName) + " returned by "
                                        + singletonClass.getName() + "." + GET_PROPERTY_DEFAULTS
                                        + " doesn't exists in the class. "
                                        + GET_PROPERTY_DEFAULTS + " needs to be fixed to match reality.");
                            }
                            Method readMethod = propDesc.getReadMethod();
                            if (readMethod == null) {
                                throw new RuntimeException(
                                        "The JavaBean property " + StringUtil.jQuote(propName) + " returned by "
                                        + singletonClass.getName() + "." + GET_PROPERTY_DEFAULTS
                                        + " exists but isn't readable. "
                                        + "Write-only properties can't be part of a singleton's key, and so shouldn't "
                                        + "be exposed by " + GET_PROPERTY_DEFAULTS + ".");
                            }
                            Object actual = readMethod.invoke(newSingleton, Collections12.EMPTY_OBJECT_ARRAY);
                            Object expected = ent.getValue();
                            if (actual != expected && (expected == null || !expected.equals(actual))) {
                                throw new RuntimeException(
                                        "The JavaBean property default value promissed by "
                                        + singletonClass.getName() + "." + GET_PROPERTY_DEFAULTS + " doesn't seem to "
                                        + "match the actual default value. "
                                        + "Expected: " + (expected != null ? expected.getClass().getName() + " " : "")
                                        + StringUtil.jQuote(expected)
                                        + "; actual: " + (actual != null ? actual.getClass().getName() + " " : "")
                                        + StringUtil.jQuote(actual) + ". "
                                        + GET_PROPERTY_DEFAULTS + " needs to be fixed to match reality.");
                            }
                        }
                    }
                } else {  // when singletonKey.propertiesKey.isEmpty()
                    if (properties.size() != 0) {
                        throw new RuntimeException();  // should not occur 
                    }
                }
                
                // Make the singleton read-only:
                ((Lockable) newSingleton).makeReadOnly();
                
                // Store the singleton for later retrieval:
                singletons.put(
                        singletonKey,
                        strongRef
                            ? newSingleton
                            : new SingletonObjectSoftReference(newSingleton, singletonReferenceQueue, singletonKey));
                
                return newSingleton;
            } else { // when we had a Reference with a non-null singleton in it
                if (strongRef && ref instanceof SoftReference) {
                    singletons.put(singletonKey, singleton);
                }
                return singleton;
            }
        }
    }
    
    private static void pollSingletonReferenceQueue() {
        SingletonObjectReference ref;
        while ((ref = (SingletonObjectReference) singletonReferenceQueue.poll()) != null) {
            synchronized (singletons) {
                Object key = ref.getKeyOrNull();
                if (key != null) singletons.remove(key);
            }
        }
    }
    
    /** See documentation at {@link Configuration#weakenSingletonReference(Class, Object[], Map)} */
    static void weakenSingletonReference(
            Class singletonClass, Object[] constrArgs, Map/*<String, Object>*/ properties)
            throws IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final SingletonObjectKey key = new SingletonObjectKey(singletonClass, constrArgs, properties);
        synchronized (singletons) {
            final Object val = singletons.get(key);
            if (val != null) {
                final Object singleton;
                if (val instanceof Reference) {
                    singleton = ((Reference) val).get();
                } else {
                    singleton = val;
                }
                if (singleton == null) {
                    singletons.remove(key);
                } else {
                    singletons.put(key, new SingletonObjectWeakReference(singleton, singletonReferenceQueue, key));
                }
            }
        }
        pollSingletonReferenceQueue();
    }
    
    /** Used in the test-suite. */
    static void removeAllSingletons() {
        synchronized (singletons) {
            singletons.clear();
        }
    }
    
    /** Used in the test-suite. */
    static Set getSingletonDescriptions() {
        Set res = new HashSet();
        synchronized (singletons) {
            Iterator valIter = singletons.values().iterator();
            while (valIter.hasNext()) {
                Object val = valIter.next();
                if (val instanceof SoftReference) {
                    res.add("soft " + ((Reference) val).get());
                } else if (val instanceof WeakReference) {
                    res.add("weak " + ((Reference) val).get());
                } else {
                    res.add("hard " + val);
                }
            }
        }
        return res;
    }
    
    static Object[] widenNumbersToParameterTypes(Member member, Object[] args) {
        final Class[] paramTypes = _MethodUtil.getParameterTypes(member);
        
        final Class varargsCompType;
        if (_MethodUtil.isVarArgs(member)) {
            varargsCompType = paramTypes[paramTypes.length - 1].getComponentType();
        } else {
            varargsCompType = null;
        }
        
        Object[] convertedArgs = args;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i]; 
            Class paramType = i < paramTypes.length - 1
                    ? paramTypes[i]
                    : (varargsCompType == null && i < paramTypes.length ? paramTypes[i] : varargsCompType);
            if (arg instanceof Number && ClassUtil.isNumerical(paramType)) {
                Number newArg = widenNumberIfPossible((Number) arg, paramType);
                if (newArg != arg) {
                    if (convertedArgs == args) convertedArgs = (Object[]) args.clone();
                    convertedArgs[i] = newArg;  
                }
            }
        }
        return convertedArgs;
    }
    
    /**
     * Widens the number to the given type, or returns it as is if that's impossible.
     */
    static Number widenNumberIfPossible(final Number n, Class target) {
        if (target.isPrimitive()) target = ClassUtil.primitiveClassToBoxingClass(target);
        final Class source = n.getClass();
                
        // Java 5: Use valueOf
        if (target == Integer.class) { 
            return source == Short.class || source == Byte.class
                    ? new Integer(n.intValue()) : n;
        } else if (target == Long.class) { 
            return source == Integer.class || source == Short.class || source == Byte.class
                    ? new Long(n.longValue()) : n;
        } else if (target == Float.class) { 
            return source == Long.class || source == Integer.class || source == Short.class || source == Byte.class
                    ? new Float(n.floatValue()) : n;
        } else if (target == Double.class) { 
            return source == Float.class || source == Long.class || source == Integer.class || source == Short.class
                    || source == Byte.class
                    ? new Double(n.doubleValue()) : n;
        } else if(target == Short.class) {
            return source == Byte.class ? new Short(n.byteValue()) : n;
        } else {
            return n;
        }
    }

    private final static class SingletonObjectKey {
        private final Class singletonClass;
        private final Object[] constructorArgumentsKey;
        private final Map propertiesKey;
        private final int hashCode;
        private final Version versionArgumentOverride; 
        
        public SingletonObjectKey(
                final Class singletonClass, Object[] constructorArguments, Map properties)
                throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
                InvocationTargetException {
            if (constructorArguments == null) constructorArguments = Collections12.EMPTY_OBJECT_ARRAY;
            if (properties == null) properties = Collections12.EMPTY_MAP;

            this.singletonClass = singletonClass;
            
            // Find the first Version parameter (if there's one), normalize it:
            Version normalizedVersionArg = null;
            for (int argIdx = 0; argIdx < constructorArguments.length; argIdx++) {
                Object arg = constructorArguments[argIdx];
                if (arg instanceof Version) {
                    normalizedVersionArg = (Version) invokeStaticSingletonMethod(
                            Version.class, NORMALIZE_ICI_VERSION_METHOD_NAME, Version.class,
                            arg,
                            false, false,
                            new _DelayedConversionToString(null) {
                                protected String doConversion(Object obj) {
                                    return "As the " + singletonClass.getName() + " constructor has a "
                                            + Version.class.getName() + " argument";
                                }
                            });
                    if (!normalizedVersionArg.equals(arg)) {
                        constructorArguments = (Object[]) constructorArguments.clone();
                        constructorArguments[argIdx] = normalizedVersionArg;
                    } else {
                        normalizedVersionArg = null;  // because of the versionArgumentOverride assignment later 
                    }
                    break;  // We only normalize the fist Version argument
                }
            }
            this.versionArgumentOverride = normalizedVersionArg;
            
            // Normalize the constructor argument list (only required if it's overloaded):
            Constructor[] constructors = singletonClass.getConstructors();
            if (constructors.length == 0) {
                throw new NoSuchMethodException(singletonClass.getName() + " has no public constructors");
            }
            Object[] normalizedArgs = (Object[]) invokeStaticSingletonMethod(
                    Object[].class, NORMALIZE_CONSTRUCTOR_ARGUMENTS_METHOD_NAME, Object[].class,
                    constructorArguments,
                    constructors.length == 1 || constructorArguments.length == 0, false,
                    new _DelayedConversionToString(null) {
                        protected String doConversion(Object obj) {
                            return "As the " + singletonClass.getName() + " class has multiple public constructors "
                                    + "and a constructor with arguments should be called";
                        }
                    });
            if (normalizedArgs == null) {
                // This can be either because we have 0 arguments, or because we had only one constructor.
                normalizedArgs = constructorArguments.length == 0
                        ? constructorArguments
                        : widenNumbersToParameterTypes(constructors[0], constructorArguments);
            } else {
                if (normalizedArgs.length < constructorArguments.length) {
                    throw new RuntimeException(
                            singletonClass.getName() + "." + NORMALIZE_CONSTRUCTOR_ARGUMENTS_METHOD_NAME
                            + " is not allowed to normalize an argument list to a shorter list.");
                }
            }
            this.constructorArgumentsKey = normalizedArgs; 
            
            // Normalize properties:
            // - Get the property defaults
            Map propDefaults = (Map) invokeStaticSingletonMethod(
                    Map.class, GET_PROPERTY_DEFAULTS, Object[].class,
                    this.constructorArgumentsKey,
                    properties.isEmpty(),  true,
                    new _DelayedConversionToString(null) {
                        protected String doConversion(Object obj) {
                            return "As you try to set some JavaBean properties of a(n) " + singletonClass.getName()
                                    + " singleton";
                        }
                    });
            // - Check that all specified property also has a default, also widen numerical values where necessary:
            Map normalizedProps = properties;
            if (propDefaults != null) {
                Iterator propEntIter = properties.entrySet().iterator();
                while (propEntIter.hasNext()) {
                    Map.Entry ent = (Map.Entry) propEntIter.next();
                    Object propName = ent.getKey();
                    if (!(propName instanceof String)) {
                        throw new IllegalArgumentException("The \"properties\" Map must only contain String keys.");
                    }
                    Object propDefault = propDefaults.get(propName);
                    if (propDefault == null && !propDefaults.containsKey(propName)) {
                        throw new IllegalArgumentException(
                                "You can't set the \"" + propName + "\" JavaBean property of a singleton "
                                + singletonClass.getName() + ", because it's not in the Map returned by "
                                + singletonClass.getName() + "." + GET_PROPERTY_DEFAULTS + ".");
                    }
                    Object propVal = ent.getValue();
                    if (propDefault instanceof Number && propVal instanceof Number) {
                        Object widenedPropVal = widenNumberIfPossible((Number) propVal, propDefault.getClass());
                        if (widenedPropVal != propVal) {
                            if (normalizedProps == properties) normalizedProps = new HashMap(properties);
                            normalizedProps.put(propName, widenedPropVal);
                        }
                    }
                }
                // - Add the properties that weren't specified with their default values:
                Iterator defaultEntIter = propDefaults.entrySet().iterator();
                while (defaultEntIter.hasNext()) {
                    Map.Entry ent = (Map.Entry) defaultEntIter.next();
                    Object defaultPropName = ent.getKey();
                    if (!(defaultPropName instanceof String)) {
                        throw new ClassCastException("The Map returned by " + singletonClass.getName()
                                + "." + GET_PROPERTY_DEFAULTS + " must only contain String keys.");
                    }
                    if (!properties.containsKey(defaultPropName)) {
                        if (normalizedProps == properties) normalizedProps = new HashMap(properties);
                        normalizedProps.put(defaultPropName, ent.getValue());
                    }
                }
            }
            this.propertiesKey = normalizedProps;
            
            hashCode = (nullSafeHash(singletonClass) * 31
                    + (constructorArgumentsKey == null ? 0 : arrayHashCode(constructorArgumentsKey))) * 31
                    + nullSafeHash(propertiesKey);
        }
        
        // Java 5: Use Arrays.hashCode instead
        private int arrayHashCode(Object[] array) {
            if (array == null) return 0;
            
            int hashCode = 1;
            for (int i = 0; i < array.length; i++) {
                hashCode *= 31;
                Object item = array[i];
                hashCode += item != null ? item.hashCode() : 0;
            }
            return hashCode;
        }
        
        /**
         * @param paramType {@code null} if the method has no parameter
         */
        private Object invokeStaticSingletonMethod(
                Class returnType, String methodName, Class paramType,
                Object arg,
                boolean returnNullIfNoSuchMethod, boolean allowInherited,
                _DelayedConversionToString requiredReason)
                throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
                InvocationTargetException {
            final Method m;
            try {
                try {
                    m = singletonClass.getMethod(
                            methodName,
                            paramType == null ? Collections12.EMPTY_CLASS_ARRAY : new Class[] { paramType });
                } catch (NoSuchMethodException e) {
                    if (returnNullIfNoSuchMethod) {
                        return null;
                    } else {
                        throw e;
                    }
                }
                if (!allowInherited && m.getDeclaringClass() != singletonClass) {
                    throw new NoSuchMethodException(
                            "The " + methodName + " method must not be inherited, but directly declared in "
                            + singletonClass.getName() + ".");
                }
                if ((m.getModifiers() & Modifier.STATIC) == 0) {
                    throw new NoSuchMethodException(
                            "The " + methodName + " method must be static");
                }
                if (m.getReturnType() != returnType) {
                    throw new NoSuchMethodException(
                            "The " + methodName + " method must have return type "
                            + returnType.getName() + ".");
                }
            } catch (Exception e) {
                throw new NoSuchMethodException(
                        requiredReason + ", the class must have a \"public static "
                        + ClassUtil.getShortClassName(returnType) + " " + methodName
                        + "(" + (paramType == null ? "" : ClassUtil.getShortClassName(paramType))
                        + ")\" method (see the JavaDoc of Configuration.getSingleton from more info), but getting "
                        + "it has failed:\n" + e);
            }
            
            final Object res = m.invoke(null,
                    paramType != null ? new Object[] { arg } : Collections12.EMPTY_OBJECT_ARRAY);
            if (res == null) {
                throw new NullPointerException(
                        singletonClass.getName() + "." + methodName
                        + " has returned null");
            }
            return res;
        }
        
        private static int nullSafeHash(Object o) {
            return o == null ? 0 : o.hashCode();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object that) {
            if (this == that) return true;
            
            if (!(that instanceof SingletonObjectKey)) {
                return false;
            }
            
            SingletonObjectKey thatKey = (SingletonObjectKey) that; 
            return nullSafeEquals(this.singletonClass, thatKey.singletonClass)
                    && (this.constructorArgumentsKey == thatKey.constructorArgumentsKey
                        || (this.constructorArgumentsKey != null && this.constructorArgumentsKey != null
                            && Arrays.equals(this.constructorArgumentsKey, thatKey.constructorArgumentsKey)))
                    && nullSafeEquals(this.propertiesKey, thatKey.propertiesKey);
        }
        
        private static boolean nullSafeEquals(Object o1, Object o2) {
            if (o1 == o2) return true;  // Also kicks in when both are null-s
            if (o1 == null || o2 == null) return false;
            return o1.equals(o2);
        }
        
    }
    
    private interface SingletonObjectReference {
        SingletonObjectKey getKeyOrNull();
    }
    
    /** Soft-reference to a singleton object that also encapsulates to key of the singleton. */
    private final static class SingletonObjectSoftReference extends SoftReference implements SingletonObjectReference {
        
        private final WeakReference keyRef;

        public SingletonObjectSoftReference(Object referent, ReferenceQueue q, SingletonObjectKey key) {
            super(referent, q);
            this.keyRef = new WeakReference(key);
        }

        public SingletonObjectKey getKeyOrNull() {
            return (SingletonObjectKey) keyRef.get();
        }
        
    }

    /** Soft-reference to a singleton object that also encapsulates to key of the singleton. */
    private final static class SingletonObjectWeakReference extends WeakReference implements SingletonObjectReference {
        
        private final WeakReference keyRef;

        public SingletonObjectWeakReference(Object referent, ReferenceQueue q, SingletonObjectKey key) {
            super(referent, q);
            this.keyRef = new WeakReference(key);
        }

        public SingletonObjectKey getKeyOrNull() {
            return (SingletonObjectKey) keyRef.get();
        }
        
    }
    
}

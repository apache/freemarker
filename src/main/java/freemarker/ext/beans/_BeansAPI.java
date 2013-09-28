package freemarker.ext.beans;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import freemarker.ext.beans.BeansWrapper.SettingAssignments;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.utility.Collections12;
import freemarker.template.utility._MethodUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _BeansAPI {

    private _BeansAPI() { }
    
    // Get rid of this with Java 5
    public static final boolean JVM_USES_JSR133;
    static {
        Class cl;
        try {
            cl = Class.forName("java.util.concurrent.atomic.AtomicInteger");
        } catch (Throwable e) {
            cl = null;
        }
        JVM_USES_JSR133 = cl != null;
    }
    
    public static String getAsClassicCompatibleString(BeanModel bm) {
        return bm.getAsClassicCompatibleString();
    }
    
    /**
     * Convenience method that combines {@link #getConstructor(Class, Object[])} and
     * {@link #newInstance(Constructor, Object[])}.
     */
    public static Object newInstance(Class pClass, Object[] args)
            throws NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        return newInstance(getConstructor(pClass, args), args);
    }
    
    /**
     * Gets the constructor that matches the types of the arguments the best. So this is more
     * than what the Java reflection API provides in that it can handle overloaded constructors. This re-uses the
     * overloaded method selection logic of {@link BeansWrapper}.
     */
    public static Constructor getConstructor(Class pClass, Object[] args) throws NoSuchMethodException {
        if (args == null) args = Collections12.EMPTY_OBJECT_ARRAY;
        
        final ArgumentTypes argTypes = new ArgumentTypes(args, true);
        final List fixedArgMemberDescs = new ArrayList();
        final List varArgsMemberDescs = new ArrayList();
        final Constructor[] constrs = pClass.getConstructors();
        for (int i = 0; i < constrs.length; i++) {
            Constructor constr = constrs[i];
            CallableMemberDescriptor memberDesc = new CallableMemberDescriptor(constr, constr.getParameterTypes());
            if (!_MethodUtil.isVarArgs(constr)) {
                fixedArgMemberDescs.add(memberDesc);
            } else {
                varArgsMemberDescs.add(memberDesc);
            }
        }
        
        MaybeEmptyCallableMemberDescriptor contrDesc = argTypes.getMostSpecific(fixedArgMemberDescs, false);
        if (contrDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) {
            contrDesc = argTypes.getMostSpecific(varArgsMemberDescs, true);
        }
        
        if (contrDesc instanceof EmptyCallableMemberDescriptor) {
            if (contrDesc == EmptyCallableMemberDescriptor.NO_SUCH_METHOD) {
                throw new NoSuchMethodException(
                        "There's no public " + pClass.getName()
                        + " constructor with compatible parameter list.");
            } else if (contrDesc == EmptyCallableMemberDescriptor.AMBIGUOUS_METHOD) {
                throw new NoSuchMethodException(
                        "There are multiple public " + pClass.getName()
                        + " constructors that match the compatible parameter list with the same preferability.");
            } else {
                throw new NoSuchMethodException();
            }
        } else {
            return (Constructor) ((CallableMemberDescriptor) contrDesc).member;
        }
    }
    
    /**
     * Creates a new instance using a flat argument list (no varargs array parameter). 
     */
    public static Object newInstance(Constructor constr, Object[] args)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (args == null) args = Collections12.EMPTY_OBJECT_ARRAY;
        
        final Object[] packedArgs;
        if (_MethodUtil.isVarArgs(constr)) {
            // We have to put all the varargs arguments into a single array argument.

            final Class[] paramTypes = constr.getParameterTypes();
            final int fixedArgCnt = paramTypes.length - 1;
            
            packedArgs = new Object[fixedArgCnt + 1]; 
            for (int i = 0; i < fixedArgCnt; i++) {
                packedArgs[i] = args[i];
            }
            
            final Class compType = paramTypes[fixedArgCnt].getComponentType();
            final int varArgCnt = args.length - fixedArgCnt;
            final Object varArgsArray = Array.newInstance(compType, varArgCnt);
            for (int i = 0; i < varArgCnt; i++) {
                Array.set(varArgsArray, i, args[fixedArgCnt + i]);
            }
            packedArgs[fixedArgCnt] = varArgsArray;
        } else {
            packedArgs = args;
        }
        
        return constr.newInstance(packedArgs);
    }
    
    /**
     * Contains the common parts of the singleton management for {@link BeansWrapper} and {@link DefaultObjectWrapper}.  
     *  
     * @param beansWrapperSubclassFactory Creates a <em>new</em> read-only object wrapper of the desired
     *     {@link BeansWrapper} subclass. 
     */
    public static BeansWrapper getBeansWrapperSubclassInstance(
            SettingAssignments sa,
            Map instanceCache,
            ReferenceQueue instanceCacheRefQue,
            BeansWrapperSubclassFactory beansWrapperSubclassFactory) {
        // BeansWrapper can't be cached across different Thread Context Class Loaders (TCCL), because the result of
        // a class name (String) to Class mappings depends on it, and the staticModels and enumModels need that.
        // (The ClassIntrospector doesn't have to consider the TCCL, as it only works with Class-es, not class
        // names.)
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        
        Reference instanceRef;
        Map/*<SettingAssignments, WeakReference<BeansWrapper>>*/ tcclScopedCache;
        synchronized (instanceCache) {
            tcclScopedCache = (Map) instanceCache.get(tccl);
            if (tcclScopedCache == null) {
                tcclScopedCache = new HashMap();
                instanceCache.put(tccl, tcclScopedCache);
                instanceRef = null;
            } else {
                instanceRef = (Reference) tcclScopedCache.get(sa);
            }
        }

        BeansWrapper instance = instanceRef != null ? (BeansWrapper) instanceRef.get() : null;
        if (instance != null) {  // cache hit
            return instance;
        }
        // cache miss
        
        sa = (SettingAssignments) sa.clone(true);  // prevent any aliasing issues 
        instance = beansWrapperSubclassFactory.create(sa);
        
        synchronized (instanceCache) {
            instanceRef = (Reference) tcclScopedCache.get(sa);
            BeansWrapper concurrentInstance = instanceRef != null ? (BeansWrapper) instanceRef.get() : null;
            if (concurrentInstance == null) {
                tcclScopedCache.put(sa, new WeakReference(instance, instanceCacheRefQue));
            } else {
                instance = concurrentInstance;
            }
        }
        
        removeClearedReferencesFromCache(instanceCache, instanceCacheRefQue);
        
        return instance;
    }
    
    private static void removeClearedReferencesFromCache(Map instanceCache, ReferenceQueue instanceCacheRefQue) {
        Reference clearedRef;
        while ((clearedRef = instanceCacheRefQue.poll()) != null) {
            synchronized (instanceCache) {
                findClearedRef: for (Iterator it1 = instanceCache.values().iterator(); it1.hasNext(); ) {
                    Map tcclScopedCache = (Map) it1.next();
                    for (Iterator it2 = tcclScopedCache.values().iterator(); it2.hasNext(); ) {
                        if (it2.next() == clearedRef) {
                            it2.remove();
                            break findClearedRef;
                        }
                    }
                }
            } // sync
        } // while poll
    }
    
    public interface BeansWrapperSubclassFactory {
        
        /** Creates a new read-only wrapper; used for {@link BeansWrapper#getInstance} and in its subclasses. */
        BeansWrapper create(SettingAssignments sa);
    }
    
}

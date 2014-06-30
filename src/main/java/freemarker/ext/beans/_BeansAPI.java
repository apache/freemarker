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

import freemarker.core.BugException;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.CollectionUtils;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _BeansAPI {

    private _BeansAPI() { }
    
    public static String getAsClassicCompatibleString(BeanModel bm) {
        return bm.getAsClassicCompatibleString();
    }
    
    public static Object newInstance(Class pClass, Object[] args, BeansWrapper bw)
            throws NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException, TemplateModelException {
        return newInstance(getConstructorDescriptor(pClass, args), args, bw);
    }
    
    /**
     * Gets the constructor that matches the types of the arguments the best. So this is more
     * than what the Java reflection API provides in that it can handle overloaded constructors. This re-uses the
     * overloaded method selection logic of {@link BeansWrapper}.
     */
    private static CallableMemberDescriptor getConstructorDescriptor(Class pClass, Object[] args) throws NoSuchMethodException {
        if (args == null) args = CollectionUtils.EMPTY_OBJECT_ARRAY;
        
        final ArgumentTypes argTypes = new ArgumentTypes(args, true);
        final List fixedArgMemberDescs = new ArrayList();
        final List varArgsMemberDescs = new ArrayList();
        final Constructor[] constrs = pClass.getConstructors();
        for (int i = 0; i < constrs.length; i++) {
            Constructor constr = constrs[i];
            ReflectionCallableMemberDescriptor memberDesc = new ReflectionCallableMemberDescriptor(constr, constr.getParameterTypes());
            if (!_MethodUtil.isVarargs(constr)) {
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
            return (CallableMemberDescriptor) contrDesc;
        }
    }
    
    private static Object newInstance(CallableMemberDescriptor constrDesc, Object[] args, BeansWrapper bw)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, IllegalArgumentException,
            TemplateModelException {
        if (args == null) args = CollectionUtils.EMPTY_OBJECT_ARRAY;
        
        final Object[] packedArgs;
        if (constrDesc.isVarargs()) {
            // We have to put all the varargs arguments into a single array argument.

            final Class[] paramTypes = constrDesc.getParamTypes();
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
        
        return constrDesc.invokeConstructor(bw, packedArgs);
    }
    
    /**
     * Contains the common parts of the singleton management for {@link BeansWrapper} and {@link DefaultObjectWrapper}.  
     *  
     * @param beansWrapperSubclassFactory Creates a <em>new</em> read-only object wrapper of the desired
     *     {@link BeansWrapper} subclass. 
     */
    public static BeansWrapper getBeansWrapperSubclassSingleton(
            BeansWrapperConfiguration settings,
            Map instanceCache,
            ReferenceQueue instanceCacheRefQue,
            _BeansWrapperSubclassFactory beansWrapperSubclassFactory) {
        // BeansWrapper can't be cached across different Thread Context Class Loaders (TCCL), because the result of
        // a class name (String) to Class mappings depends on it, and the staticModels and enumModels need that.
        // (The ClassIntrospector doesn't have to consider the TCCL, as it only works with Class-es, not class
        // names.)
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        
        Reference instanceRef;
        Map/*<PropertyAssignments, WeakReference<BeansWrapper>>*/ tcclScopedCache;
        synchronized (instanceCache) {
            tcclScopedCache = (Map) instanceCache.get(tccl);
            if (tcclScopedCache == null) {
                tcclScopedCache = new HashMap();
                instanceCache.put(tccl, tcclScopedCache);
                instanceRef = null;
            } else {
                instanceRef = (Reference) tcclScopedCache.get(settings);
            }
        }

        BeansWrapper instance = instanceRef != null ? (BeansWrapper) instanceRef.get() : null;
        if (instance != null) {  // cache hit
            return instance;
        }
        // cache miss
        
        settings = (BeansWrapperConfiguration) settings.clone(true);  // prevent any aliasing issues 
        instance = beansWrapperSubclassFactory.create(settings);
        if (!instance.isWriteProtected()) {
            throw new BugException();
        }
        
        synchronized (instanceCache) {
            instanceRef = (Reference) tcclScopedCache.get(settings);
            BeansWrapper concurrentInstance = instanceRef != null ? (BeansWrapper) instanceRef.get() : null;
            if (concurrentInstance == null) {
                tcclScopedCache.put(settings, new WeakReference(instance, instanceCacheRefQue));
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
    
    /**
     * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
     */
    public interface _BeansWrapperSubclassFactory {
        
        /** Creates a new read-only {@link BeansWrapper}; used for {@link BeansWrapperBuilder} and such. */
        BeansWrapper create(BeansWrapperConfiguration sa);
    }
    
}

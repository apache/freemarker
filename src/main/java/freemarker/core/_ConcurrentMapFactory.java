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

package freemarker.core;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * For internal usage only; don't depend on this!
 * Creates {@code java.util.concurrent.ConcurrentMap}-s on J2SE 5 or later,
 * plain maps otherwise. This is used for avoiding linking-time J2SE 5
 * dependency.
 * 
 * This class will be removed when J2SE 5 becomes the required
 * minimum for FreeMarker, so don't use it unless you are developing
 * FreeMarker itself.
 */
public class _ConcurrentMapFactory {
    private static final Class concurrentMapClass; 
    static {
        Class c;
        try {
            c = ClassUtil.forName("java.util.concurrent.ConcurrentMap");
        } catch(ClassNotFoundException e) {
            c =  null;
        }
        concurrentMapClass = c;
    }
    
    private static final Class bestHashMapClass;
    private static final Constructor bestHashMapClassConstructor;
    private static final int bestHashMapClassConstructorParamCnt;
    static {
        Class c;
        Constructor constr;
        int constrParamCnt;
        try {
            c = ClassUtil.forName("java.util.concurrent.ConcurrentHashMap");
            try {
                constr = c.getConstructor(new Class[] { Integer.TYPE, Float.TYPE, Integer.TYPE });
                constrParamCnt = 3;            
            } catch (Exception e) {
                throw new RuntimeException("Failed to get ConcurrentHashMap constructor", e); 
            }
        } catch(ClassNotFoundException e) {
            c = HashMap.class;
            try {
                constr = c.getConstructor(new Class[] { Integer.TYPE, Float.TYPE });
                constrParamCnt = 2;            
            } catch (Exception e2) {
                throw new RuntimeException("Failed to get HashMap constructor", e2);
            }
        }
        
        bestHashMapClass = c;
        bestHashMapClassConstructor = constr;
        bestHashMapClassConstructorParamCnt = constrParamCnt;            
    }
    
    static public Map newMaybeConcurrentHashMap() {
        try {
            return (Map) bestHashMapClass.newInstance();
        } catch(Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    static public Map newMaybeConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        try {
            if (bestHashMapClassConstructorParamCnt == 3) {
                return (Map) bestHashMapClassConstructor.newInstance(new Object[] {
                        new Integer(initialCapacity), new Float(loadFactor), new Integer(concurrencyLevel) });
            } else if (bestHashMapClassConstructorParamCnt == 2) {
                return (Map) bestHashMapClassConstructor.newInstance(new Object[] {
                        new Integer(initialCapacity), new Float(loadFactor) });
            } else {
                throw new BugException();
            }
        } catch(Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    /**
     * Returns an instance of the "best" thread-safe {@link Map} available in
     * the current runtime environment.   
     */
	public static Map newThreadSafeMap() {
		Map map = newMaybeConcurrentHashMap();
		return isConcurrent(map) ? map : Collections.synchronizedMap(map); 
	}

    static public boolean concurrentMapsAvailable() {
        return concurrentMapClass != null;
    }
    
    /**
     * Checks if the map is concurrent; safe to call before J2SE 5.
     */
    static public boolean isConcurrent(Map map) {
        return concurrentMapClass != null && concurrentMapClass.isInstance(map);
    }
    
}

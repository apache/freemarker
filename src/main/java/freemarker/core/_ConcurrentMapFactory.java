package freemarker.core;

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
    private static final Class concurrentMapClass = getConcurrentMapClass(); 
    private static final Class bestHashMapClass = getBestHashMapClass();
    
    static public Map newMaybeConcurrentHashMap() {
        try {
            return (Map) bestHashMapClass.newInstance();
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
    
    private static Class getConcurrentMapClass() {
        try {
            return ClassUtil.forName("java.util.concurrent.ConcurrentMap");
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    private static Class getBestHashMapClass() {
        try {
            return ClassUtil.forName("java.util.concurrent.ConcurrentHashMap");
        } catch(ClassNotFoundException e) {
            return HashMap.class;
        }
    }
    
}

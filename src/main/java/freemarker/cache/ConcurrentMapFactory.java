package freemarker.cache;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * @author Attila Szegedi
 * @version $Id: $
 */
class ConcurrentMapFactory
{
    private static final Class mapClass = getMapClass(); 
    private static final Class hashMapClass = getHashMapClass();
    
    static Map createMap() {
        try {
            return (Map)hashMapClass.newInstance();
        }
        catch(Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    static boolean isConcurrent(Map map) {
        return mapClass != null && mapClass.isInstance(map);
    }
    
    private static Class getMapClass() {
        try {
            return ClassUtil.forName("java.util.concurrent.ConcurrentMap");
        }
        catch(ClassNotFoundException e) {
            return null;
        }
    }

    private static Class getHashMapClass() {
        try {
            return ClassUtil.forName("java.util.concurrent.ConcurrentHashMap");
        }
        catch(ClassNotFoundException e) {
            return HashMap.class;
        }
    }
}
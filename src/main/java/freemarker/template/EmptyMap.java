package freemarker.template;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class EmptyMap implements Map, Cloneable {
    public static final EmptyMap instance = new EmptyMap(); 
    
    public void clear() {
        throw new UnsupportedOperationException("This Map is read-only.");
    }
    
    public boolean containsKey(Object arg0) {
        return false;
    }
    
    public boolean containsValue(Object arg0) {
        return false;
    }
    
    public Set entrySet() {
        return Collections.EMPTY_SET;
    }
    
    public Object get(Object arg0) {
        return null;
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    public Set keySet() {
        return Collections.EMPTY_SET;
    }
    
    public Object put(Object arg0, Object arg1) {
        throw new UnsupportedOperationException("This Map is read-only.");
    }
    
    public void putAll(Map arg0) {
        throw new UnsupportedOperationException("This Map is read-only.");
    }
    
    public Object remove(Object arg0) {
        throw new UnsupportedOperationException("This Map is read-only.");
    }
    
    public int size() {
        return 0;
    }
    
    public Collection values() {
        return Collections.EMPTY_LIST;
    }

}

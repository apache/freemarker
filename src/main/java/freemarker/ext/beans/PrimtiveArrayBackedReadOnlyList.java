package freemarker.ext.beans;

import java.lang.reflect.Array;
import java.util.AbstractList;

/**
 * Similar to {@link NonPrimitiveArrayBackedReadOnlyList}, but uses reflection so that it works with primitive arrays
 * too. 
 */
class PrimtiveArrayBackedReadOnlyList extends AbstractList {
    
    private final Object array;
    
    PrimtiveArrayBackedReadOnlyList(Object array) {
        this.array = array;
    }

    public Object get(int index) {
        return Array.get(array, index);
    }

    public int size() {
        return Array.getLength(array);
    }

}
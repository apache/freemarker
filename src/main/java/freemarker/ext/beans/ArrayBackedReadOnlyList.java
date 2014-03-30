package freemarker.ext.beans;

import java.util.AbstractList;

class ArrayBackedReadOnlyList extends AbstractList {
    
    private final Object[] array;
    
    ArrayBackedReadOnlyList(Object[] array) {
        this.array = array;
    }

    public Object get(int index) {
        return array[index];
    }

    public int size() {
        return array.length;
    }
    
}

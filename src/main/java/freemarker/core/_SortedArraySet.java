package freemarker.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
// [Java 5] Make this generic
public class _SortedArraySet extends _UnmodifiableSet {

    private final Object[] array;

    public _SortedArraySet(Object[] array) {
        this.array = array;
    }

    public int size() {
        return array.length;
    }

    public boolean contains(Object o) {
        return Arrays.binarySearch(array, o) >= 0;
    }

    public Iterator iterator() {
        return new _ArrayIterator(array);
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
    
}

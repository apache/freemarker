package freemarker.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
public class _ArrayIterator implements Iterator {

    private final Object[] array;
    private int nextIndex;

    public _ArrayIterator(Object[] array) {
        this.array = array;
        this.nextIndex = 0;
    }

    public boolean hasNext() {
        return nextIndex < array.length;
    }

    public Object next() {
        if (nextIndex >= array.length) {
            throw new NoSuchElementException();
        }
        return array[nextIndex++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}

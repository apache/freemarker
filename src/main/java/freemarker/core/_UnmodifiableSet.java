package freemarker.core;

import java.util.AbstractSet;

/** Don't use this; used internally by FreeMarker, might changes without notice. */
//[Java 5] Make this generic
public abstract class _UnmodifiableSet extends AbstractSet {

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        if (contains(o)) {
            throw new UnsupportedOperationException();
        }
        return false;
    }

    public void clear() {
        if (!isEmpty()) {
            throw new UnsupportedOperationException();
        }
    }

}

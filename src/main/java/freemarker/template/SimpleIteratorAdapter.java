package freemarker.template;

import java.io.Serializable;
import java.util.Iterator;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts an {@link Iterator} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateCollectionModel}. The resulting {@link TemplateCollectionModel} can only be iterated once.
 * 
 * <p>
 * Thread safety: This class is not thread-safe. That's because it doesn't make sense to expose an {@link Iterator} to
 * multiple template processings, since it can give back the items only once.
 * 
 * @since 2.3.22
 */
public class SimpleIteratorAdapter extends WrappingTemplateModel implements TemplateCollectionModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    private final Iterator iterator;
    private boolean iteratorOwned;

    public static SimpleIteratorAdapter adapt(Iterator iterator, ObjectWrapper wrapper) {
        return new SimpleIteratorAdapter(iterator, wrapper);
    }

    private SimpleIteratorAdapter(Iterator iterator, ObjectWrapper wrapper) {
        super(wrapper);
        this.iterator = iterator;
    }

    public Object getWrappedObject() {
        return iterator;
    }

    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return new SimpleTemplateModelIterator();
    }

    /**
     * Not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {

        private boolean iteratorOwnedByMe;

        public TemplateModel next() throws TemplateModelException {
            if (!iteratorOwnedByMe) {
                takeIteratorOwnership();
            }

            if (!iterator.hasNext()) {
                throw new TemplateModelException("The collection has no more items.");
            }

            Object value = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        public boolean hasNext() throws TemplateModelException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!iteratorOwnedByMe) {
                takeIteratorOwnership();
            }

            return iterator.hasNext();
        }

        private void takeIteratorOwnership() throws TemplateModelException {
            if (iteratorOwned) {
                throw new TemplateModelException(
                        "This collection value wraps a java.util.Iterator, thus it can be listed only once.");
            } else {
                iteratorOwned = true;
                iteratorOwnedByMe = true;
            }
        }
    }

}

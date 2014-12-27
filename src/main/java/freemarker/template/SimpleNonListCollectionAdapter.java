package freemarker.template;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import freemarker.core._DelayedShortClassName;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts a non-{@link List} Java {@link Collection} to the corresponding {@link TemplateModel} interface(s), most
 * importantly to {@link TemplateCollectionModelEx}. For {@link List}-s, use {@link SimpleListAdapter}, or else you lose
 * indexed element access.
 * 
 * <p>
 * Thread safety: A {@link SimpleNonListCollectionAdapter} is as thread-safe as the {@link Collection} that it wraps is.
 * Normally you only have to consider read-only access, as the FreeMarker template language doesn't allow writing these
 * collections (though of course, a Java methods called from the template can violate this rule).
 * 
 * @since 2.3.22
 */
public class SimpleNonListCollectionAdapter extends WrappingTemplateModel implements TemplateCollectionModelEx,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    private final Collection collection;

    /**
     * Factory method for creating new adapter instances.
     */
    public static SimpleNonListCollectionAdapter adapt(Collection collection, ObjectWrapperAndUnwrapper wrapper) {
        return new SimpleNonListCollectionAdapter(collection, wrapper);
    }

    private SimpleNonListCollectionAdapter(Collection collection, ObjectWrapperAndUnwrapper wrapper) {
        super(wrapper);
        this.collection = collection;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return new SimpleIteratorAdapter(collection.iterator());
    }

    public int size() {
        return collection.size();
    }

    public boolean isEmpty() {
        return collection.isEmpty();
    }

    public Object getWrappedObject() {
        return collection;
    }

    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    private class SimpleIteratorAdapter implements TemplateModelIterator {

        private final Iterator iterator;

        SimpleIteratorAdapter(Iterator iterator) {
            this.iterator = iterator;
        }

        public TemplateModel next() throws TemplateModelException {
            if (!iterator.hasNext()) {
                throw new TemplateModelException("The collection has no more items.");
            }

            Object value = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        public boolean hasNext() throws TemplateModelException {
            return iterator.hasNext();
        }
    }

    public boolean contains(TemplateModel item) throws TemplateModelException {
        Object itemPojo = ((ObjectWrapperAndUnwrapper) getObjectWrapper()).unwrap(item, Object.class);
        try {
            return collection.contains(itemPojo);
        } catch (ClassCastException e) {
            throw new _TemplateModelException(e, new Object[] {
                    "Failed to check if the collection contains the item. Probably the item's Java type, ",
                    itemPojo != null ? new _DelayedShortClassName(itemPojo.getClass()) : (Object) "Null",
                    ", doesn't match the type of (some of) the collection items; see cause exception." });
        }
    }

}

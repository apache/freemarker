package freemarker.template;

import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts a {@link List} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateSequenceModel}. If you aren't wrapping an already existing {@link List}, but build a sequence
 * specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link SimpleListAdapter} is as thread-safe as the {@link List} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these sequences (though
 * of course, a Java methods called from the template can violate this rule).
 * 
 * @see SimpleSequence
 * @see SimpleArrayAdapter
 * @see TemplateSequenceModel
 * 
 * @since 2.3.22
 */
public class SimpleListAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    protected final List list;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param list
     *            The list to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static SimpleListAdapter adapt(List list, ObjectWrapperAndUnwrapper wrapper) {
        // [2.4] SimpleListAdapter should implement TemplateCollectionModelEx, so this choice becomes unnecessary
        return list instanceof AbstractSequentialList
                ? new SimpleListAdapterWithCollectionSupport(list, wrapper)
                : new SimpleListAdapter(list, wrapper);
    }

    private SimpleListAdapter(List list, ObjectWrapper wrapper) {
        super(wrapper);
        this.list = list;
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return index >= 0 && index < list.size() ? wrap(list.get(index)) : null;
    }

    public int size() throws TemplateModelException {
        return list.size();
    }

    public Object getAdaptedObject(Class hint) {
        return getWrappedObject();
    }

    public Object getWrappedObject() {
        return list;
    }

    private static class SimpleListAdapterWithCollectionSupport extends SimpleListAdapter implements
            TemplateCollectionModel {

        private SimpleListAdapterWithCollectionSupport(List list, ObjectWrapper wrapper) {
            super(list, wrapper);
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            return new IteratorAdapter(list.iterator(), getObjectWrapper());
        }

    }

    private static class IteratorAdapter implements TemplateModelIterator {

        private final Iterator it;
        private final ObjectWrapper wrapper;

        private IteratorAdapter(Iterator it, ObjectWrapper wrapper) {
            this.it = it;
            this.wrapper = wrapper;
        }

        public TemplateModel next() throws TemplateModelException {
            try {
                return wrapper.wrap(it.next());
            } catch (NoSuchElementException e) {
                throw new TemplateModelException("The collection has no more items.", e);
            }
        }

        public boolean hasNext() throws TemplateModelException {
            return it.hasNext();
        }

    }

}

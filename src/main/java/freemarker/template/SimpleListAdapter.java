package freemarker.template;

import java.io.Serializable;
import java.util.List;

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

    private final List list;

    /**
     * Factory method for creating new adapter instances.
     */
    public static SimpleListAdapter adapt(List list, ObjectWrapper wrapper) {
        return new SimpleListAdapter(list, wrapper);
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

}

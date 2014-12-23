package freemarker.template;

import java.io.Serializable;
import java.util.List;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts a {@link List} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateSequenceModel}. If you aren't wrapping an already existing {@link List}, but build a sequence
 * specifically to be used from a template, also consider using {@link SimpleSequence} (see comparison there).
 * 
 * @see SimpleSequence
 * @see SimpleArrayAdapter
 * @see TemplateSequenceModel
 * 
 * @since 2.3.22
 */
public final class SimpleListAdapter extends WrappingTemplateModel implements TemplateSequenceModel,
        AdapterTemplateModel, WrapperTemplateModel, Serializable {

    private final List list;

    public SimpleListAdapter(List list, ObjectWrapper wrapper) {
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

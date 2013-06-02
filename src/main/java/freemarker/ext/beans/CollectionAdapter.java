package freemarker.ext.beans;

import java.util.AbstractCollection;
import java.util.Iterator;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * @author Attila Szegedi
 */
class CollectionAdapter extends AbstractCollection implements TemplateModelAdapter {
    private final BeansWrapper wrapper;
    private final TemplateCollectionModel model;
    
    CollectionAdapter(TemplateCollectionModel model, BeansWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    public int size() {
        throw new UnsupportedOperationException();
    }

    public Iterator iterator() {
        try {
            return new Iterator() {
                final TemplateModelIterator i = model.iterator();
    
                public boolean hasNext() {
                    try {
                        return i.hasNext();
                    }
                    catch(TemplateModelException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
                
                public Object next() {
                    try {
                        return wrapper.unwrap(i.next());
                    }
                    catch(TemplateModelException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
                
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        catch(TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}

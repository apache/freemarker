package freemarker.ext.beans;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * @author Attila Szegedi
 */
public class HashAdapter extends AbstractMap implements TemplateModelAdapter {
    private final BeansWrapper wrapper;
    private final TemplateHashModel model;
    private Set entrySet;
    
    HashAdapter(TemplateHashModel model, BeansWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    public boolean isEmpty() {
        try {
            return model.isEmpty();
        }
        catch(TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    public Object get(Object key) {
        try {
            return wrapper.unwrap(model.get(String.valueOf(key)));
        }
        catch(TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public boolean containsKey(Object key) {
        // A quick check that doesn't require TemplateHashModelEx 
        if(get(key) != null) {
            return true;
        }
        return super.containsKey(key);
    }
    
    public Set entrySet() {
        if(entrySet != null) {
            return entrySet;
        }
        return entrySet = new AbstractSet() {
            public Iterator iterator() {
                final TemplateModelIterator i;
                try {
                     i = getModelEx().keys().iterator();
                }
                catch(TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
                return new Iterator() {
                    public boolean hasNext() {
                        try {
                            return i.hasNext();
                        }
                        catch(TemplateModelException e) {
                            throw new UndeclaredThrowableException(e);
                        }
                    }
                    
                    public Object next() {
                        final Object key;
                        try {
                            key = wrapper.unwrap(i.next());
                        }
                        catch(TemplateModelException e) {
                            throw new UndeclaredThrowableException(e);
                        }
                        return new Map.Entry() {
                            public Object getKey() {
                                return key;
                            }
                            
                            public Object getValue() {
                                return get(key);
                            }
                            
                            public Object setValue(Object value) {
                                throw new UnsupportedOperationException();
                            }
                            
                            public boolean equals(Object o) {
                                if (!(o instanceof Map.Entry))
                                    return false;
                                Map.Entry e = (Map.Entry)o;
                                Object k1 = getKey();
                                Object k2 = e.getKey();
                                if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                                    Object v1 = getValue();
                                    Object v2 = e.getValue();
                                    if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                                        return true;
                                }
                                return false;
                            }
                        
                            public int hashCode() {
                                Object value = getValue();
                                return (key==null ? 0 : key.hashCode()) ^
                                       (value==null ? 0 : value.hashCode());
                            }
                        };
                    }
                    
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            
            public int size() {
                try {
                    return getModelEx().size();
                }
                catch(TemplateModelException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        };
    }
    
    private TemplateHashModelEx getModelEx() {
        if(model instanceof TemplateHashModelEx) {
            return ((TemplateHashModelEx)model);
        }
        throw new UnsupportedOperationException(
                "Operation supported only on TemplateHashModelEx. " + 
                model.getClass().getName() + " does not implement it though.");
    }
    
}

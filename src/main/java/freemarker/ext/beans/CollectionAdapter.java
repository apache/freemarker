/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.beans;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Adapts a {@link TemplateCollectionModel} to  {@link Collection}.
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

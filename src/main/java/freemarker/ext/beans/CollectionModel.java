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

import java.util.Collection;
import java.util.List;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * <p>A special case of {@link BeanModel} that can wrap Java collections
 * and that implements the {@link TemplateCollectionModel} in order to be usable 
 * in a <tt>&lt;#list&gt;</tt> block.</p>
 */
public class CollectionModel
extends
    StringModel
implements
    TemplateCollectionModel,
    TemplateSequenceModel
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new CollectionModel((Collection)object, (BeansWrapper)wrapper);
            }
        };


    /**
     * Creates a new model that wraps the specified collection object.
     * @param collection the collection object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public CollectionModel(Collection collection, BeansWrapper wrapper)
    {
        super(collection, wrapper);
    }

    /**
     * Retrieves the i-th object from the collection, wrapped as a TemplateModel.
     * @throws TemplateModelException if the index is out of bounds, or the
     * underlying collection is not a List.
     */
    public TemplateModel get(int index)
    throws
        TemplateModelException
    {
        // Don't forget to keep getSupportsIndexedAccess in sync with this!
        if (object instanceof List)
        {
            try
            {
                return wrap(((List)object).get(index));
            }
            catch(IndexOutOfBoundsException e)
            {
                return null;
//                throw new TemplateModelException("Index out of bounds: " + index);
            }
        }
        else
        {
            throw new TemplateModelException("Underlying collection is not a list, it's " + object.getClass().getName());
        }
    }
    
    /**
     * Tells if {@link #get(int)} will always fail for this object.
     * As this object implements {@link TemplateSequenceModel},
     * {@link #get(int)} should always work, but due to a design flaw, for
     * non-{@link List} wrapped objects {@link #get(int)} will always fail.
     * This method exists to ease working this problem around.
     * 
     * @since 2.3.17 
     */
    public boolean getSupportsIndexedAccess() {
        return object instanceof List;
    }
    
    public TemplateModelIterator iterator()
    {
        return new IteratorModel(((Collection)object).iterator(), wrapper);
    }

    public int size()
    {
        return ((Collection)object).size();
    }
    
}

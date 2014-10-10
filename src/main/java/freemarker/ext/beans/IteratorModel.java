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

import java.util.Iterator;
import java.util.NoSuchElementException;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * <p>A class that adds {@link TemplateModelIterator} functionality to the
 * {@link Iterator} interface implementers. 
 * </p>
 * <p>It differs from the {@link freemarker.template.SimpleCollection} in that 
 * it inherits from {@link BeanModel}, and therefore you can call methods on 
 * it directly, even to the effect of calling <tt>iterator.remove()</tt> in 
 * the template.</p> <p>Using the model as a collection model is NOT 
 * thread-safe, as iterators are inherently not thread-safe.
 * Further, you can iterate over it only once. Attempts to call the
 * {@link #iterator()} method after it was already driven to the end once will 
 * throw an exception.</p>
 */

public class IteratorModel
extends
    BeanModel
implements
    TemplateModelIterator,
    TemplateCollectionModel
{
    private boolean accessed = false;
    
    /**
     * Creates a new model that wraps the specified iterator object.
     * @param iterator the iterator object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     */
    public IteratorModel(Iterator iterator, BeansWrapper wrapper)
    {
        super(iterator, wrapper);
    }

    /**
     * This allows the iterator to be used in a <tt>&lt;#list&gt;</tt> block.
     * @return "this"
     */
    public TemplateModelIterator iterator() throws TemplateModelException
    {
        synchronized(this) {
            if(accessed) {
                throw new TemplateModelException(
                    "This collection is stateful and can not be iterated over the" +
                    " second time.");
            }
            accessed = true;
        }
        return this;
    }
    
    /**
     * Calls underlying {@link Iterator#hasNext()}.
     */
    public boolean hasNext() {
        return ((Iterator)object).hasNext();
    }


    /**
     * Calls underlying {@link Iterator#next()} and wraps the result.
     */
    public TemplateModel next()
    throws
        TemplateModelException
    {
        try {
            return wrap(((Iterator)object).next());
        }
        catch(NoSuchElementException e) {
            throw new TemplateModelException(
                "No more elements in the iterator.", e);
        }
    }

    /**
     * Returns {@link Iterator#hasNext()}. Therefore, an
     * iterator that has no more element evaluates to false, and an 
     * iterator that has further elements evaluates to true.
     */
    public boolean getAsBoolean() {
        return hasNext();
    }
}

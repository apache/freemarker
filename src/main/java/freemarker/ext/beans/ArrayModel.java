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

import java.lang.reflect.Array;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * <p>A class that will wrap an arbitrary array into {@link TemplateCollectionModel}
 * and {@link TemplateSequenceModel} interfaces. It supports element retrieval through the <tt>array[index]</tt>
 * syntax and can be iterated as a list.
 */
public class ArrayModel
extends
    BeanModel
implements
    TemplateCollectionModel,
    TemplateSequenceModel
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new ArrayModel(object, (BeansWrapper)wrapper);
            }
        };
        
    // Cached length of the array
    private final int length;

    /**
     * Creates a new model that wraps the specified array object.
     * @param array the array object to wrap into a model.
     * @param wrapper the {@link BeansWrapper} associated with this model.
     * Every model has to have an associated {@link BeansWrapper} instance. The
     * model gains many attributes from its wrapper, including the caching 
     * behavior, method exposure level, method-over-item shadowing policy etc.
     * @throws IllegalArgumentException if the passed object is not a Java array.
     */
    public ArrayModel(Object array, BeansWrapper wrapper)
    {
        super(array, wrapper);
        Class clazz = array.getClass();
        if(!clazz.isArray())
            throw new IllegalArgumentException("Object is not an array, it's " + array.getClass().getName());
        length = Array.getLength(array);
    }


    public TemplateModelIterator iterator()
    {
        return new Iterator();
    }

    public TemplateModel get(int index)
    throws
        TemplateModelException
    {
        try
        {
            return wrap(Array.get(object, index));
        }
        catch(IndexOutOfBoundsException e)
        {
            return null; 
//            throw new TemplateModelException("Index out of bounds: " + index);
        }
    }

    private class Iterator
    implements 
        TemplateSequenceModel,
        TemplateModelIterator
    {
        private int position = 0;

        public boolean hasNext()
        {
            return position < length;
        }

        public TemplateModel get(int index)
        throws
            TemplateModelException
        {
            return ArrayModel.this.get(index);
        }

        public TemplateModel next()
        throws
            TemplateModelException
        {
            return position < length ? get(position++) : null;
        }

        public int size() 
        {
            return ArrayModel.this.size();
        }
    }

    public int size() 
    {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }
}

/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
 * @author Attila Szegedi
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
    private int length;

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

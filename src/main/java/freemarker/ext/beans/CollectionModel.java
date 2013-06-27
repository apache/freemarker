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
 * in a <tt>&lt;foreach></tt> block.</p>
 * @author Attila Szegedi
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

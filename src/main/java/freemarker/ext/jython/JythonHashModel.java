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

package freemarker.ext.jython;

import org.python.core.PyException;
import org.python.core.PyObject;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Model for Jython dictionaries ({@link org.python.core.PyDictionary}
 * and {@link org.python.core.PyStringMap}).
 * Note that the basic {@link JythonModel} already provides access to the
 * {@link PyObject#__finditem__(String)} method. This class only adds 
 * {@link TemplateHashModelEx} functionality in a somewhat skewed way. One
 * could say it even violates TemplateHashModelEx semantics, as both the
 * returned keys and values are only those from the item mapping, while the
 * <code>get()</code> method works for attributes as well. However, in practice
 * when you ask for <code>dict?keys</code> inside a template, you'll really
 * want to retrieve only items, not attributes so this is considered OK.
 * @author Attila Szegedi
 */
public class JythonHashModel
extends 
    JythonModel 
implements 
    TemplateHashModelEx
{
    private static final String KEYS = "keys";
    private static final String KEYSET = "keySet";
    private static final String VALUES = "values";
    
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new JythonHashModel((PyObject)object, (JythonWrapper)wrapper);
            }
        };
        
    public JythonHashModel(PyObject object, JythonWrapper wrapper)
    {
        super(object, wrapper);
    }
    
    /**
     * Returns {@link PyObject#__len__()}.
     */
    public int size() throws TemplateModelException
    {
        try
        {
            return object.__len__();
        }
        catch(PyException e)
        {
            throw new TemplateModelException(e);
        }
    }

    /**
     * Returns either <code>object.__findattr__("keys").__call__()</code>
     * or <code>object.__findattr__("keySet").__call__()</code>.
     */
    public TemplateCollectionModel keys() throws TemplateModelException
    {
        try
        {
            PyObject method = object.__findattr__(KEYS);
            if(method == null)
            {
                method = object.__findattr__(KEYSET);
            }
            if(method != null)
            {
                return (TemplateCollectionModel)wrapper.wrap(method.__call__());
            }
        }
        catch(PyException e)
        {
            throw new TemplateModelException(e);
        }
        throw new TemplateModelException(
                "'?keys' is not supported as there is no 'keys' nor 'keySet' attribute on an instance of "
                + JythonVersionAdapterHolder.INSTANCE.getPythonClassName(object));
    }

    /**
     * Returns <code>object.__findattr__("values").__call__()</code>.
     */
    public TemplateCollectionModel values() throws TemplateModelException
    {
        try
        {
            PyObject method = object.__findattr__(VALUES);
            if(method != null)
            {
                return (TemplateCollectionModel)wrapper.wrap(method.__call__());
            }
        }
        catch(PyException e)
        {
            throw new TemplateModelException(e);
        }
        throw new TemplateModelException(
                "'?values' is not supported as there is no 'values' attribute on an instance of "
                + JythonVersionAdapterHolder.INSTANCE.getPythonClassName(object));
    }
}

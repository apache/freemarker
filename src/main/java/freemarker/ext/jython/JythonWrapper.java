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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyString;
import org.python.core.PyStringMap;

import freemarker.ext.util.ModelCache;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.OptimizerUtil;

/**
 * An object wrapper that wraps Jython objects into FreeMarker template models
 * and vice versa.
 * @author Attila Szegedi
 */
public class JythonWrapper implements ObjectWrapper
{
    private static final Class PYOBJECT_CLASS = PyObject.class;
    public static final JythonWrapper INSTANCE = new JythonWrapper();

    private final ModelCache modelCache = new JythonModelCache(this);

    private boolean attributesShadowItems = true;

    public JythonWrapper()
    {
    }
    
    /**
     * Sets whether this wrapper caches model instances. Default is false.
     * When set to true, calling {@link #wrap(Object)} multiple times for
     * the same object will return the same model.
     */
    public void setUseCache(boolean useCache)
    {
        modelCache.setUseCache(useCache);
    }
    
    /**
     * Sets whether attributes shadow items in wrapped objects. When true 
     * (this is the default value), <code>${object.name}</code> will first 
     * try to locate a python attribute with the specified name on the object
     * using {@link PyObject#__findattr__(java.lang.String)}, and only if it 
     * doesn't find the attribute will it call 
     * {@link PyObject#__getitem__(org.python.core.PyObject)}.
     * When set to false, the lookup order is reversed and items
     * are looked up before attributes.
     */
    public synchronized void setAttributesShadowItems(boolean attributesShadowItems)
    {
        this.attributesShadowItems = attributesShadowItems;
    }
    
    boolean isAttributesShadowItems()
    {
        return attributesShadowItems;
    }
    
    /**
     * Wraps the passed Jython object into a FreeMarker template model. If
     * the object is not a Jython object, it's first coerced into one using
     * {@link Py#java2py(java.lang.Object)}. {@link PyDictionary} and {@link
     * PyStringMap} are wrapped into a hash model, {@link PySequence}
     * descendants are wrapped into a sequence model, {@link PyInteger}, {@link
     * PyLong}, and {@link PyFloat} are wrapped into a number model. All objects
     * are wrapped into a scalar model (using {@link Object#toString()} and a
     * boolean model (using {@link PyObject#__nonzero__()}. For internal
     * general-purpose {@link PyObject}s returned from a call to {@link
     * #unwrap(TemplateModel)}, the template model that was passed to
     * <code>unwrap</code> is returned.
     */
    public TemplateModel wrap(Object obj)
    {
        if(obj == null) {
            return null;
        }
        return modelCache.getInstance(obj);
    }
    
    /**
     * Coerces a template model into a {@link PyObject}.
     * @param model the model to coerce
     * @return the coerced model.
     * <ul>
     * <li>
     * <li>{@link AdapterTemplateModel}s (i.e. {@link freemarker.ext.beans.BeanModel}) are marshalled
     *   using the standard Python marshaller {@link Py#java2py(Object)} on 
     *   the result of <code>getWrappedObject(PyObject.class)</code>s. The 
     *   native JythonModel instances will just return the underlying PyObject. 
     * <li>All other models that are {@link TemplateScalarModel scalars} are 
     *   marshalled as {@link PyString}.
     * <li>All other models that are {@link TemplateNumberModel numbers} are 
     *   marshalled using the standard Python marshaller
     *   {@link Py#java2py(Object)} on their underlying <code>Number</code></li>
     * <li>All other models  are marshalled to a generic internal 
     *   <code>PyObject</code> subclass that'll correctly pass
     *   <code>__finditem__</code>, <code>__len__</code>,
     *   <code>__nonzero__</code>, and <code>__call__</code> invocations to 
     *   appropriate hash, sequence, and method models.</li>
     * </ul>
     */
    public PyObject unwrap(TemplateModel model) throws TemplateModelException
    {
        if(model instanceof AdapterTemplateModel) {
            return Py.java2py(((AdapterTemplateModel)model).getAdaptedObject(
                    PYOBJECT_CLASS));
        }
        if(model instanceof WrapperTemplateModel) {
            return Py.java2py(((WrapperTemplateModel)model).getWrappedObject());
        }

        // Scalars are marshalled to PyString.
        if(model instanceof TemplateScalarModel)
        {
            return new PyString(((TemplateScalarModel)model).getAsString());
        }
        
        // Numbers are wrapped to Python built-in numeric types.
        if(model instanceof TemplateNumberModel)
        {
            Number number = ((TemplateNumberModel)model).getAsNumber();
            if(number instanceof BigDecimal)
            {
                number = OptimizerUtil.optimizeNumberRepresentation(number);
            }
            if(number instanceof BigInteger)
            {
                // Py.java2py can't automatically coerce a BigInteger into
                // a PyLong. This will probably get fixed in later Jython
                // release.
                return new PyLong((BigInteger)number);
            }
            else
            {
                return Py.java2py(number);
            }
        }
        // Return generic TemplateModel-to-Python adapter
        return new TemplateModelToJythonAdapter(model);
    }

    private class TemplateModelToJythonAdapter extends PyObject 
    implements TemplateModelAdapter 
    {
        private final TemplateModel model;
        
        TemplateModelToJythonAdapter(TemplateModel model)
        {
            this.model = model;
        }
        
        public TemplateModel getTemplateModel()
        {
            return model;
        }
        
        public PyObject __finditem__(PyObject key)
        {
            if(key instanceof PyInteger)
            {
                return __finditem__(((PyInteger)key).getValue());
            }
            return __finditem__(key.toString());
        }

        public PyObject __finditem__(String key)
        {
            if(model instanceof TemplateHashModel)
            {
                try
                {
                    return unwrap(((TemplateHashModel)model).get(key));
                }
                catch(TemplateModelException e)
                {
                    throw Py.JavaError(e);
                }
            }
            throw Py.TypeError("item lookup on non-hash model (" + getModelClass() + ")");
        }
        
        public PyObject __finditem__(int index)
        {
            if(model instanceof TemplateSequenceModel)
            {
                try
                {
                    return unwrap(((TemplateSequenceModel)model).get(index));
                }
                catch(TemplateModelException e)
                {
                    throw Py.JavaError(e);
                }
            }
            throw Py.TypeError("item lookup on non-sequence model (" + getModelClass() + ")");
        }
        
        public PyObject __call__(PyObject args[], String keywords[])
        {
            if(model instanceof TemplateMethodModel)
            {
                boolean isEx = model instanceof TemplateMethodModelEx;
                List list = new ArrayList(args.length);
                try
                {
                    for(int i = 0; i < args.length; ++i)
                    {
                        list.add(
                            isEx 
                            ? (Object)wrap(args[i]) 
                            : (Object)(
                                args[i] == null 
                                ? null 
                                : args[i].toString()));
                    }
                    return unwrap((TemplateModel) ((TemplateMethodModelEx)model).exec(list));
                }
                catch(TemplateModelException e)
                {
                    throw Py.JavaError(e);
                }
            }
            throw Py.TypeError("call of non-method model (" + getModelClass() + ")");
        }
        
        public int __len__()
        {
            try
            {
                if(model instanceof TemplateSequenceModel)
                {
                    return ((TemplateSequenceModel)model).size();
                }
                if(model instanceof TemplateHashModelEx)
                {
                    return ((TemplateHashModelEx)model).size();
                }
            }
            catch(TemplateModelException e)
            {
                throw Py.JavaError(e);
            }
            
            return 0;
        }
        
        public boolean __nonzero__()
        {
            try
            {
                if(model instanceof TemplateBooleanModel)
                {
                    return ((TemplateBooleanModel)model).getAsBoolean();
                }
                if(model instanceof TemplateSequenceModel)
                {
                    return ((TemplateSequenceModel)model).size() > 0;
                }
                if(model instanceof TemplateHashModel)
                {
                    return !((TemplateHashModelEx)model).isEmpty();
                }
            }
            catch(TemplateModelException e)
            {
                throw Py.JavaError(e);
            }
            return false;
        }
        
        private String getModelClass()
        {
            return model == null ? "null" : model.getClass().getName();
        }
    }
}

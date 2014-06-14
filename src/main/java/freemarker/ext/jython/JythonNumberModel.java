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

package freemarker.ext.jython;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * Model for Jython numeric objects ({@link org.python.core.PyInteger}, {@link org.python.core.PyLong},
 * {@link org.python.core.PyFloat}).
 */
public class JythonNumberModel
extends
    JythonModel
implements
    TemplateNumberModel
{
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            public TemplateModel create(Object object, ObjectWrapper wrapper)
            {
                return new JythonNumberModel((PyObject)object, (JythonWrapper)wrapper);
            }
        };
        
    public JythonNumberModel(PyObject object, JythonWrapper wrapper)
    {
        super(object, wrapper);
    }

    /**
     * Returns either {@link PyObject#__tojava__(java.lang.Class)} with
     * {@link java.lang.Number}.class as argument. If that fails, returns 
     * {@link PyObject#__float__()}.
     */
    public Number getAsNumber() throws TemplateModelException
    {
        try
        {
            Object value = object.__tojava__(java.lang.Number.class);
            if(value == null || value == Py.NoConversion)
            {
                return new Double(object.__float__().getValue());
            }
            return (Number)value;
        }
        catch(PyException e)
        {
            throw new TemplateModelException(e);
        }
    }
}

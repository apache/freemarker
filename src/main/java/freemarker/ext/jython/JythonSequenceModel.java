/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.jython;

import org.python.core.PyException;
import org.python.core.PyObject;

import freemarker.ext.util.ModelFactory;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Model for Jython sequence objects ({@link org.python.core.PySequence} descendants).
 */
public class JythonSequenceModel
extends
    JythonModel
implements 
    TemplateSequenceModel,
    TemplateCollectionModel {
    static final ModelFactory FACTORY =
        new ModelFactory()
        {
            @Override
            public TemplateModel create(Object object, ObjectWrapper wrapper) {
                return new JythonSequenceModel((PyObject) object, (JythonWrapper) wrapper);
            }
        };
        
    public JythonSequenceModel(PyObject object, JythonWrapper wrapper) {
        super(object, wrapper);
    }

    /**
     * Returns {@link PyObject#__finditem__(int)}.
     */
    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        try {
            return wrapper.wrap(object.__finditem__(index));
        } catch (PyException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * Returns {@link PyObject#__len__()}.
     */
    @Override
    public int size() throws TemplateModelException {
        try {
            return object.__len__();
        } catch (PyException e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public TemplateModelIterator iterator() {
        return new TemplateModelIterator()
        {
            int i = 0;
            
            @Override
            public boolean hasNext() throws TemplateModelException {
                return i < size();
            }

            @Override
            public TemplateModel next() throws TemplateModelException {
                return get(i++);
            }
        };
    }
}

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

package org.apache.freemarker.core.model.impl;

import java.util.AbstractList;
import java.util.Iterator;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.UndeclaredThrowableException;

/**
 */
class TemplateSequenceModelAdapter<T> extends AbstractList<T> implements TemplateModelAdapter {
    private final DefaultObjectWrapper wrapper;
    private final TemplateSequenceModel model;
    
    TemplateSequenceModelAdapter(TemplateSequenceModel model, DefaultObjectWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    @Override
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    @Override
    public int size() {
        try {
            return model.getCollectionSize();
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return model.isEmptyCollection();
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        try {
            return (T) wrapper.unwrap(model.get(index));
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public Iterator<T> iterator() {
        try {
            return new TemplateModelIteratorAdapter<T>(model.iterator(), wrapper);
        } catch (TemplateException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public TemplateSequenceModel getTemplateSequenceModel() {
        return model;
    }
    
}

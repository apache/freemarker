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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelAdapter;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.util.UndeclaredThrowableException;

/**
 * Adapts a {@link TemplateCollectionModel} to  {@link Collection}.
 */
class CollectionAdapter extends AbstractCollection implements TemplateModelAdapter {
    private final DefaultObjectWrapper wrapper;
    private final TemplateCollectionModel model;
    
    CollectionAdapter(TemplateCollectionModel model, DefaultObjectWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    @Override
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator iterator() {
        try {
            return new Iterator() {
                final TemplateModelIterator i = model.iterator();
    
                @Override
                public boolean hasNext() {
                    try {
                        return i.hasNext();
                    } catch (TemplateModelException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
                
                @Override
                public Object next() {
                    try {
                        return wrapper.unwrap(i.next());
                    } catch (TemplateModelException e) {
                        throw new UndeclaredThrowableException(e);
                    }
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}

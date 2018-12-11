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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.WrappingTemplateModel;

/**
 * A simple implementation of {@link TemplateCollectionModel}.
 * <p>
 * This class is thread-safe. The returned {@link TemplateModelIterator}-s are <em>not</em> thread-safe.
 */
public class SimpleCollection extends WrappingTemplateModel implements TemplateCollectionModel, Serializable {

    private final Collection<?> collection;

    public SimpleCollection(Collection<?> collection, ObjectWrapper wrapper) {
        super(wrapper);
        this.collection = collection;
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return collection.size();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return collection.isEmpty();
    }

    /**
     * Retrieves a template model iterator that is used to iterate over the elements in this iterable.
     */
    @Override
    public TemplateModelIterator iterator() {
        return new SimpleTemplateModelIterator(collection.iterator());
    }
    
    /**
     * Wraps an {@link Iterator}; not thread-safe.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {
        
        private final Iterator<?> iterator;

        SimpleTemplateModelIterator(Iterator<?> iterator) {
            this.iterator = iterator;
        }

        @Override
        public TemplateModel next() throws TemplateException {
            Object value  = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        @Override
        public boolean hasNext() throws TemplateException {
            return iterator.hasNext();
        }

    }
    
}

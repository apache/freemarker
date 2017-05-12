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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelIterator;

/**
 * As opposed to {@link DefaultIteratorAdapter}, this simpler {@link Iterator} adapter is used in situations where the
 * {@link TemplateModelIterator} won't be assigned to FreeMarker template variables, only used internally by
 * {@code #list} or custom Java code. Because of that, it doesn't have to handle the situation where the user tries to
 * iterate over the same value twice.
 */
class DefaultUnassignableIteratorAdapter implements TemplateModelIterator {

    private final Iterator<?> it;
    private final ObjectWrapper wrapper;

    DefaultUnassignableIteratorAdapter(Iterator<?> it, ObjectWrapper wrapper) {
        this.it = it;
        this.wrapper = wrapper;
    }

    @Override
    public TemplateModel next() throws TemplateModelException {
        try {
            return wrapper.wrap(it.next());
        } catch (NoSuchElementException e) {
            throw new TemplateModelException("The collection has no more items.", e);
        }
    }

    @Override
    public boolean hasNext() throws TemplateModelException {
        return it.hasNext();
    }

}
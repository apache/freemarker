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
import java.util.List;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 * A sequence that wraps a {@link List} of {@link TemplateModel}-s. It does not copy the original
 * list. The thread safety (and other behavior in face on concurrency) remains the same as of the wrapped list.
 */
public class TemplateModelListSequence implements TemplateSequenceModel {
    
    private final List<? extends TemplateModel> list;

    /**
     * @param list The list of items; will not be copied, will not be modified.
     */
    public TemplateModelListSequence(List<? extends TemplateModel> list) {
        this.list = list;
    }

    @Override
    public TemplateModel get(int index) {
        return index < list.size() && index >= 0 ? list.get(index) : null;
    }

    @Override
    public int getCollectionSize() {
        return list.size();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return list.isEmpty();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {
            private final Iterator<? extends TemplateModel> iterator = list.iterator();

            @Override
            public TemplateModel next() throws TemplateException {
                return iterator.next();
            }

            @Override
            public boolean hasNext() throws TemplateException {
                return iterator.hasNext();
            }
        };
    }

    /**
     * Returns the original {@link List} of {@link TemplateModel}-s, so it's not a fully unwrapped value.
     */
    public Object getWrappedObject() {
        return list;
    }
    
}

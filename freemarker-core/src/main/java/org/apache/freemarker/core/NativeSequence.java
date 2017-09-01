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

package org.apache.freemarker.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 * A sequence where each items is already a {@link TemplateModel}, so no {@link ObjectWrapper} need to be specified.
 *
 * <p>While this class allows adding items, doing so is not thread-safe, and thus only meant to be done during the
 * initialization of the sequence.
 */
class NativeSequence implements TemplateSequenceModel, Serializable {

    private final ArrayList<TemplateModel> items;

    public NativeSequence(int capacity) {
        items = new ArrayList<>(capacity);
    }

    /**
     * Copies the collection
     */
    public NativeSequence(Collection<TemplateModel> items) {
        this.items = new ArrayList<>(items.size());
        this.items.addAll(items);
    }

    void add(TemplateModel tm) {
        items.add(tm);
    }

    void addAll(Collection<TemplateModel> items) {
        this.items.addAll(items);
    }

    void clear() {
        items.clear();
    }

    @Override
    public TemplateModel get(int index) throws TemplateException {
        return index < items.size() && index >= 0 ? items.get(index) : null;
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return items.size();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return items.isEmpty();
    }

    /**
     * Do not call when you will still add items to the sequence!
     *
     * {@inheritDoc}
     */
    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {
            private int nextIndex = 0;
            private final int size = items.size();

            @Override
            public TemplateModel next() throws TemplateException {
                return items.get(nextIndex++);
            }

            @Override
            public boolean hasNext() throws TemplateException {
                return nextIndex < size;
            }
        };
    }
}

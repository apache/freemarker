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

import java.util.ArrayList;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 * Add sequence capabilities to an existing iterable. Used by ?keys and ?values built-ins.
 */
// TODO [FM3] Maybe ?keys/?values should just return a TemplateCollectionModel
final public class IterableAndSequence implements TemplateSequenceModel {
    private TemplateIterableModel iterable;
    private ArrayList<TemplateModel> data;

    public IterableAndSequence(TemplateIterableModel iterable) {
        this.iterable = iterable;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return iterable.iterator();
    }

    @Override
    public TemplateModel get(int i) throws TemplateException {
        initSequence();
        return i < data.size() && i >= 0 ? data.get(i) : null;
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        if (iterable instanceof TemplateCollectionModel) {
            return ((TemplateCollectionModel) iterable).getCollectionSize();
        } else {
            initSequence();
            return data.size();
        }
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        if (iterable instanceof TemplateCollectionModel) {
            return ((TemplateCollectionModel) iterable).isEmptyCollection();
        } else {
            return iterable.iterator().hasNext();
        }
    }

    private void initSequence() throws TemplateException {
        if (data == null) {
            data = new ArrayList<>();
            TemplateModelIterator it = iterable.iterator();
            while (it.hasNext()) {
                data.add(it.next());
            }
        }
    }
}

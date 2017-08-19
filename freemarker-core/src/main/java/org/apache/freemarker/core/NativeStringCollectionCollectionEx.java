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

import java.util.Collection;
import java.util.Iterator;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.impl.DefaultNonListCollectionAdapter;
import org.apache.freemarker.core.model.impl.SimpleScalar;

/**
 * Adapts (not copies) a {@link Collection} of {@link String}-s with on-the-fly wrapping of the items to {@link
 * SimpleScalar}-s. The important difference to {@link DefaultNonListCollectionAdapter} is that it doesn't depend on an
 * {@link ObjectWrapper}, which is needed to guarantee the behavior of some template language constructs. The important
 * difference to {@link NativeCollectionEx} is that it doesn't need upfront conversion to {@link TemplateModel}-s
 * (performance).
 */
class NativeStringCollectionCollectionEx implements TemplateCollectionModelEx {

    private final Collection<String> collection;

    public NativeStringCollectionCollectionEx(Collection<String> collection) {
        this.collection = collection;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {

            private final Iterator<String> iterator = collection.iterator();

            @Override
            public TemplateModel next() throws TemplateException {
                if (!iterator.hasNext()) {
                    throw new TemplateException("The collection has no more items.");
                }

                return new SimpleScalar(iterator.next());
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
        };
    }
}

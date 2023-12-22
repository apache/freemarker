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

package freemarker.core;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * Delays creating the {@link TemplateModelIterator} of the wrapped model until it's actually needed.
 */
class LazyCollectionTemplateModelIterator implements TemplateModelIterator {

    private final TemplateCollectionModel templateCollectionModel;
    private TemplateModelIterator iterator;

    public LazyCollectionTemplateModelIterator(TemplateCollectionModel templateCollectionModel) {
        this.templateCollectionModel = templateCollectionModel;
    }

    @Override
    public TemplateModel next() throws TemplateModelException {
        ensureIteratorInitialized();
        return iterator.next();
    }

    @Override
    public boolean hasNext() throws TemplateModelException {
        ensureIteratorInitialized();
        return iterator.hasNext();
    }

    private void ensureIteratorInitialized() throws TemplateModelException {
        if (iterator == null) {
            iterator = templateCollectionModel.iterator();
        }
    }
}

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
import freemarker.template.utility.NullArgumentException;

/**
 * Used where we really want to return/pass a {@link TemplateModelIterator}, but the API requires us to return
 * a {@link TemplateModel}.
 *
 * @since 2.3.29
 */
class SingleIterationCollectionModel implements TemplateCollectionModel {
    private TemplateModelIterator iterator;

    SingleIterationCollectionModel(TemplateModelIterator iterator) {
        NullArgumentException.check(iterator);
        this.iterator = iterator;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateModelException {
        if (iterator == null) {
            throw new IllegalStateException(
                    "Can't return the iterator again, as this TemplateCollectionModel can only be iterated once.");
        }
        TemplateModelIterator result = iterator;
        iterator = null;
        return result;
    }

    protected TemplateModelIterator getIterator() {
        return iterator;
    }
}

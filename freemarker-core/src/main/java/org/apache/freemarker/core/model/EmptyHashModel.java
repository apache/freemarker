/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model;

import java.io.Serializable;

class EmptyHashModel implements TemplateHashModelEx2, Serializable {

    @Override
    public int size() throws TemplateModelException {
        return 0;
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        return TemplateCollectionModel.EMPTY_COLLECTION;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        return TemplateCollectionModel.EMPTY_COLLECTION;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        return null;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return true;
    }

    @Override
    public KeyValuePairIterator keyValuePairIterator() throws TemplateModelException {
        return EmptyKeyValuePairIterator.EMPTY_KEY_VALUE_PAIR_ITERATOR;
    }
}

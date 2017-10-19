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

package org.apache.freemarker.core.templatesuite.models;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.SimpleString;

public class HashAndStringModel implements TemplateHashModelEx, TemplateStringModel {
    
    public static final HashAndStringModel INSTANCE = new HashAndStringModel();

    @Override
    public String getAsString() throws TemplateException {
        return "stringValue";
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return new SimpleString("mapValue for " + key);
    }

    @Override
    public boolean isEmptyHash() throws TemplateException {
        return true;
    }

    @Override
    public int getHashSize() throws TemplateException {
        return 0;
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateException {
        return TemplateCollectionModel.EMPTY_COLLECTION;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateException {
        return TemplateCollectionModel.EMPTY_COLLECTION;
    }

    @Override
    public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
        return KeyValuePairIterator.EMPTY_KEY_VALUE_PAIR_ITERATOR;
    }

}

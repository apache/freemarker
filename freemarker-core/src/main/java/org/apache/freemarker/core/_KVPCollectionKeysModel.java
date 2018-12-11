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

import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
public class _KVPCollectionKeysModel implements TemplateCollectionModel {
    private final Collection<KeyValuePair> kvps;
    
    public _KVPCollectionKeysModel(Collection<KeyValuePair> kvps) {
        this.kvps = kvps;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {
            private Iterator<KeyValuePair> iter = kvps.iterator();

            @Override
            public boolean hasNext() throws TemplateException {
                return iter.hasNext();
            }

            @Override
            public TemplateModel next() throws TemplateException {
                return iter.next().getKey();
            }
        };
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return kvps.isEmpty();
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return kvps.size();
    }
}
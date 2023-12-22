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

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

final class LazilyGeneratedCollectionModelWithAlreadyKnownSize extends LazilyGeneratedCollectionModelEx {
    private final int size;

    LazilyGeneratedCollectionModelWithAlreadyKnownSize(TemplateModelIterator iterator, int size, boolean sequence) {
        super(iterator, sequence);
        this.size = size;
    }

    @Override
    public int size() throws TemplateModelException {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    protected LazilyGeneratedCollectionModel withIsSequenceFromFalseToTrue() {
        return new LazilyGeneratedCollectionModelWithAlreadyKnownSize(getIterator(), size, true);
    }
}

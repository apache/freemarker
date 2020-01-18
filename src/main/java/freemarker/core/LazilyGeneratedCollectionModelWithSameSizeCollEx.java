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

import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.NullArgumentException;

/**
 * Used instead of {@link LazilyGeneratedCollectionModel} for operations that don't change the element count of the
 * source, if the source can also give back an element count.
 */
class LazilyGeneratedCollectionModelWithSameSizeCollEx extends LazilyGeneratedCollectionModelEx {
    private final TemplateCollectionModelEx sizeSourceCollEx;

    public LazilyGeneratedCollectionModelWithSameSizeCollEx(
            TemplateModelIterator iterator, TemplateCollectionModelEx sizeSourceCollEx, boolean sequenceSourced) {
        super(iterator, sequenceSourced);
        NullArgumentException.check(sizeSourceCollEx);
        this.sizeSourceCollEx = sizeSourceCollEx;
    }

    @Override
    public int size() throws TemplateModelException {
        return sizeSourceCollEx.size();
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return sizeSourceCollEx.isEmpty();
    }

    @Override
    protected LazilyGeneratedCollectionModelWithSameSizeCollEx withIsSequenceFromFalseToTrue() {
        return new LazilyGeneratedCollectionModelWithSameSizeCollEx(getIterator(), sizeSourceCollEx, true);
    }
}

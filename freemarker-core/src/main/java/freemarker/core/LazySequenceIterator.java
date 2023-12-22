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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * {@link TemplateModelIterator} that wraps a sequence, delaying calling any of its methods until it's actually needed.
 *
 * @since 2.3.29
 */
class LazySequenceIterator implements TemplateModelIterator {
    private final TemplateSequenceModel sequence;
    private Integer size;
    private int index = 0;

    LazySequenceIterator(TemplateSequenceModel sequence) throws TemplateModelException {
        this.sequence = sequence;

    }
    @Override
    public TemplateModel next() throws TemplateModelException {
        return sequence.get(index++);
    }

    @Override
    public boolean hasNext() {
        if (size == null) {
            try {
                size = sequence.size();
            } catch (TemplateModelException e) {
                throw new RuntimeException("Error when getting sequence size", e);
            }
        }
        return index < size;
    }
}

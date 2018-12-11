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

import java.util.List;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;

/**
 * {@link TemplateIterableModel} implementation that can iterate through a {@link TemplateSequenceModel} be going
 * through the valid indexes and calling {@link TemplateSequenceModel#get(int)} for each. This should only be used if
 * you can be sure that {@link TemplateSequenceModel#get(int)} and {@link TemplateSequenceModel#getCollectionSize()} is efficient.
 * ({@link TemplateSequenceModel} doesn't require that to be efficient, as listing the contents should be done with with
 * {@link TemplateIterableModel#iterator()}).
 * <p>
 * This class assumes that the sequence doesn't change during iteration (as it's technically impossible to detect if a
 * generic {@link TemplateSequenceModel} was changed, similarly as it's impossible for a generic {@link List}). The
 * index range is decided when the instance is created, and it simply iterates through the predefined index range. Thus
 * for example, if the sequence length decreases after that, it will return {@code null}-s (means missing element) at
 * the end, for the indexes that are now out of bounds.
 */
public class SequenceTemplateModelIterator implements TemplateModelIterator {

    private final TemplateSequenceModel sequence;
    private final int size;
    private int nextIndex = 0;

    public SequenceTemplateModelIterator(TemplateSequenceModel sequence) throws TemplateException {
        this.sequence = sequence;
        this.size = sequence.getCollectionSize();
    }

    @Override
    public TemplateModel next() throws TemplateException {
        return sequence.get(nextIndex++);
    }

    @Override
    public boolean hasNext() throws TemplateException {
        return nextIndex < size;
    }
}

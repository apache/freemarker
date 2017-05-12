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

import java.util.List;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.DefaultListAdapter;
import org.apache.freemarker.core.model.impl.SimpleScalar;

/**
 * Adapts (not copies) a {@link List} of {@link String}-s with on-the-fly wrapping of the items to {@link
 * SimpleScalar}-s. The important difference to {@link DefaultListAdapter} is that it doesn't depend on an {@link
 * ObjectWrapper}, which is needed to guarantee the behavior of some template language constructs. The important
 * difference to {@link NativeSequence} is that it doesn't need upfront conversion to {@link TemplateModel}-s
 * (performance).
 */
class NativeStringListSequence implements TemplateSequenceModel {

    private final List<String> items;

    public NativeStringListSequence(List<String> items) {
        this.items = items;
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        return index < items.size() ? new SimpleScalar(items.get(index)) : null;
    }

    @Override
    public int size() throws TemplateModelException {
        return items.size();
    }

}

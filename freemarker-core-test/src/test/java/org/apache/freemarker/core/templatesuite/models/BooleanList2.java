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
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleSequence;

/**
 * Model for testing list models. Every
 * other method simply delegates to a SimpleList model.
 */
public class BooleanList2 implements TemplateSequenceModel {

    private SimpleSequence cList;

    /** Creates new BooleanList2 */
    public BooleanList2(ObjectWrapper ow) {
        cList = new SimpleSequence(ow);
    }

    /**
     * @return the specified index in the list
     */
    @Override
    public TemplateModel get(int i) throws TemplateException {
        return cList.get(i);
    }

    @Override
    public int getCollectionSize() {
        return cList.getCollectionSize();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return cList.isEmptyCollection();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return cList.iterator();
    }
}

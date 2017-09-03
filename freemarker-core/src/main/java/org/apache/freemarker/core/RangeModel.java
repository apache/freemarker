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

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;

abstract class RangeModel implements TemplateSequenceModel, java.io.Serializable {
    
    private final int begin;

    public RangeModel(int begin) {
        this.begin = begin;
    }

    final int getBeginning() {
        return begin;
    }
    
    @Override
    public final TemplateModel get(int index) throws TemplateException {
        return index < getCollectionSize() && index >= 0 ? uncheckedGet(index) : null;
    }

    protected final TemplateModel uncheckedGet(long index) {
        long value = begin + getStep() * index;
        return value <= Integer.MAX_VALUE ? new SimpleNumber((int) value) : new SimpleNumber(value);
    }

    /**
     * @return {@code 1} or {@code -1}; other return values need not be properly handled until FTL supports other steps.
     */
    abstract int getStep();
    
    abstract boolean isRightUnbounded();
    
    abstract boolean isRightAdaptive();

}

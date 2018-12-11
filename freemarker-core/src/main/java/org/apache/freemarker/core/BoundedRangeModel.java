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
import org.apache.freemarker.core.model.TemplateModelIterator;

/**
 * A range between two integers (maybe 0 long).
 */
final class BoundedRangeModel extends RangeModel {

    private final int step, size;
    private final boolean rightAdaptive;

    /**
     * @param inclusiveEnd Tells if the {@code end} index is part of the range. 
     * @param rightAdaptive Tells if the right end of the range adapts to the size of the sliced value, if otherwise
     *     it would be bigger than that. 
     */
    BoundedRangeModel(int begin, int end, boolean inclusiveEnd, boolean rightAdaptive) {
        super(begin);
        step = begin <= end ? 1 : -1;
        size = Math.abs(end - begin) + (inclusiveEnd ? 1 : 0);
        this.rightAdaptive = rightAdaptive;
    }

    @Override
    public int getCollectionSize() {
        return size;
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return size == 0;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {
            private long nextIndex;

            @Override
            public TemplateModel next() throws TemplateException {
                return uncheckedGet(nextIndex++);
            }

            @Override
            public boolean hasNext() throws TemplateException {
                return nextIndex < getCollectionSize();
            }
        };
    }

    @Override
    int getStep() {
        return step;
    }

    @Override
    boolean isRightUnbounded() {
        return false;
    }

    @Override
    boolean isRightAdaptive() {
        return rightAdaptive;
    }

}

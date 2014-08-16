/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * A range between two integers, or an integer and infinity.
 * Inclusive or exclusive end. Can be ascending or descending. 
 */
class RangeModel implements TemplateSequenceModel, java.io.Serializable {

    private static final int INFINITE = -1;
    private final int begin, step, size;
    
    /**
     * Constructor for half-range.
     */
    public RangeModel(int begin) {
        this.begin = begin;
        this.step = 1;
        this.size = INFINITE;
    }

    public RangeModel(int begin, int end, boolean exclusiveEnd) {
        this.begin = begin;
        step = begin <= end ? 1 : -1;
        size = Math.abs(end - begin) + (exclusiveEnd ? 0 : 1);
    }

    public TemplateModel get(int index) throws TemplateModelException {
        if (index < 0 || index >= size) {
            throw new _TemplateModelException(new Object[] {
                    "Range item index ", new Integer(index), " is out of bounds." });
        }
        return new SimpleNumber(begin + step * index);
    }

    public int size() {
        // 0 bug emulated for backward compatibility
        return size != INFINITE ? size : 0;
    }
    
    boolean isRightUnbounded() {
        return size == INFINITE;
    }
    
    int getBegining() {
        return begin;
    }

    int getStep() {
        return step;
    }
    
}


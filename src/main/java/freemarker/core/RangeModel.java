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

abstract class RangeModel implements TemplateSequenceModel, java.io.Serializable {
    
    private final int begin;

    public RangeModel(int begin) {
        this.begin = begin;
    }

    final int getBegining() {
        return begin;
    }
    
    final public TemplateModel get(int index) throws TemplateModelException {
        if (index < 0 || index >= size()) {
            throw new _TemplateModelException(new Object[] {
                    "Range item index ", new Integer(index), " is out of bounds." });
        }
        long value = begin + getStep() * (long) index;
        return value <= Integer.MAX_VALUE ? new SimpleNumber((int) value) : new SimpleNumber(value);
    }
    
    /**
     * @return {@code 1} or {@code -1}; other return values need not be properly handled until FTL supports other steps.
     */
    abstract int getStep();
    
    abstract boolean isRightUnbounded();
    
    abstract boolean isRightAdaptive();
    
    abstract boolean isAffactedByStringSlicingBug();

}

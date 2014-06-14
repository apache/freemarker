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
 * A class that represents a Range between two integers.
 * inclusive of the end-points. It can be ascending or
 * descending. 
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
class NumericalRange implements TemplateSequenceModel, java.io.Serializable {

    private int lower, upper;
    private boolean descending, norhs; // if norhs is true, then we have a half-range, like n..
    
    
    /**
     * Constructor for half-range, i.e. n..
     */
    public NumericalRange(int lower) {
        this.norhs = true;
        this.lower = lower;
    }

    public NumericalRange(int left, int right) {
        lower = Math.min(left, right);
        upper = Math.max(left, right);
        descending = (left != lower);
    }

    public TemplateModel get(int i) throws TemplateModelException {
        int index = descending ? (upper -i) : (lower + i);
        if ((norhs && index > upper) || index <lower) {
            throw new _TemplateModelException(new Object[] {
                    "Range item index ", new Integer(i), " is out of bounds." });
        }
        return new SimpleNumber(index);
    }

    public int size() {
        return 1 + upper - lower;
    }
    
    boolean hasRhs() {
        return !norhs;
    }
}


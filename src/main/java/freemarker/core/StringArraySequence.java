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

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Sequence variable implementation that wraps a String[] with relatively low
 * resource utilization. Warning: it does not copy the wrapped array, so do
 * not modify that after the model was made!
 */
public class StringArraySequence implements TemplateSequenceModel {
    private String[] stringArray;
    private TemplateScalarModel[] array;

    /**
     * Warning: Does not copy the argument array!
     */
    public StringArraySequence(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public TemplateModel get(int index) {
        if (array == null) {
            array = new TemplateScalarModel[stringArray.length];
        }
        TemplateScalarModel result = array[index];
        if (result == null) {
            result = new SimpleScalar(stringArray[index]);
            array[index] = result;
        }
        return result;
    }

    public int size() {
        return stringArray.length;
    }
}

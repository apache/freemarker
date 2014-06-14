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

package freemarker.template;

import java.util.List;

/**
 * A sequence that wraps a {@link List} of {@link TemplateModel}-s. It does not copy the original
 * list. It's mostly useful when implementing {@link TemplateMethodModelEx}-es that collect items from other
 * {@link TemplateModel}-s.
 */
public class TemplateModelListSequence implements TemplateSequenceModel {
    
    private List/*<TemplateModel>*/ list;

    public TemplateModelListSequence(List list) {
        this.list = list;
    }

    public TemplateModel get(int index) {
        return (TemplateModel) list.get(index);
    }

    public int size() {
        return list.size();
    }

    /**
     * Returns the original {@link List} of {@link TemplateModel}-s, so it's not a fully unwrapped value.
     */
    public Object getWrappedObject() {
        return list;
    }
    
}

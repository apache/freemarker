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

package freemarker.ext.beans;

import java.util.AbstractList;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelAdapter;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 */
class SequenceAdapter extends AbstractList implements TemplateModelAdapter {
    private final BeansWrapper wrapper;
    private final TemplateSequenceModel model;
    
    SequenceAdapter(TemplateSequenceModel model, BeansWrapper wrapper) {
        this.model = model;
        this.wrapper = wrapper;
    }
    
    @Override
    public TemplateModel getTemplateModel() {
        return model;
    }
    
    @Override
    public int size() {
        try {
            return model.size();
        } catch (TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    @Override
    public Object get(int index) {
        try {
            return wrapper.unwrap(model.get(index));
        } catch (TemplateModelException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    
    public TemplateSequenceModel getTemplateSequenceModel() {
        return model;
    }
    
}

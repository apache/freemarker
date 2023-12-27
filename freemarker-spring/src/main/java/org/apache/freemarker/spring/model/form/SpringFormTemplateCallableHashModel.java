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

package org.apache.freemarker.spring.model.form;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * TemplateHashModel wrapper for templates using Spring directives, functions and internal models.
 */
public final class SpringFormTemplateCallableHashModel implements TemplateHashModel, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Spring form namespace model name.
     */
    public static final String NAME = "form";

    private final Map<String, TemplateModel> modelsMap = new HashMap<>();

    public SpringFormTemplateCallableHashModel(final HttpServletRequest request, final HttpServletResponse response) {
        modelsMap.put(FormTemplateDirectiveModel.NAME, new FormTemplateDirectiveModel(request, response));
        modelsMap.put(InputTemplateDirectiveModel.NAME, new InputTemplateDirectiveModel(request, response));
        modelsMap.put(PasswordInputTemplateDirectiveModel.NAME, new PasswordInputTemplateDirectiveModel(request, response));
        modelsMap.put(HiddenInputTemplateDirectiveModel.NAME, new HiddenInputTemplateDirectiveModel(request, response));
        modelsMap.put(TextareaTemplateDirectiveModel.NAME, new TextareaTemplateDirectiveModel(request, response));
        modelsMap.put(ButtonTemplateDirectiveModel.NAME, new ButtonTemplateDirectiveModel(request, response));
        modelsMap.put(LabelTemplateDirectiveModel.NAME, new LabelTemplateDirectiveModel(request, response));
        modelsMap.put(SelectTemplateDirectiveModel.NAME, new SelectTemplateDirectiveModel(request, response));
        modelsMap.put(OptionsTemplateDirectiveModel.NAME, new OptionsTemplateDirectiveModel(request, response));
        modelsMap.put(OptionTemplateDirectiveModel.NAME, new OptionTemplateDirectiveModel(request, response));
        modelsMap.put(ErrorsTemplateDirectiveModel.NAME, new ErrorsTemplateDirectiveModel(request, response));
        modelsMap.put(CheckboxTemplateDirectiveModel.NAME, new CheckboxTemplateDirectiveModel(request, response));
        modelsMap.put(CheckboxesTemplateDirectiveModel.NAME, new CheckboxesTemplateDirectiveModel(request, response));
        modelsMap.put(RadioButtonTemplateDirectiveModel.NAME, new RadioButtonTemplateDirectiveModel(request, response));
        modelsMap.put(RadioButtonsTemplateDirectiveModel.NAME, new RadioButtonsTemplateDirectiveModel(request, response));
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return modelsMap.get(key);
    }

}

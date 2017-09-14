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

package org.apache.freemarker.spring.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;

/**
 * TemplateHashModel wrapper for templates using Spring directives, functions and internal models.
 */
public final class SpringTemplateCallableHashModel implements TemplateHashModel, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Spring namespace model name.
     */
    public static final String NAME = "spring";

    /**
     * Name of the internal nested path template model, which is equivalent to the <code>nestedPath</code> pageContext attribute
     * in Spring Framework JSP tag libraries.
     */
    // NOTE: The model name, "nestedPathModel", must be different from the "nestedPath" directive model's name.
    private static final String NESTED_PATH_MODEL = "nestedPathModel";

    /**
     * Name of the internal evaluation context template model used by <code>EvalFunction</code> to cache <code>EvaluationContext</code>.
     */
    private static final String EVALUATION_CONTEXT_MODEL = "evaluationContextModel";

    private Map<String, TemplateModel> modelsMap = new HashMap<>();

    public SpringTemplateCallableHashModel(final HttpServletRequest request, final HttpServletResponse response) {
        modelsMap.put(MessageFunction.NAME, new MessageFunction(request, response));
        modelsMap.put(ThemeFunction.NAME, new ThemeFunction(request, response));
        modelsMap.put(BindErrorsDirective.NAME, new BindErrorsDirective(request, response));
        modelsMap.put(NestedPathDirective.NAME, new NestedPathDirective(request, response));
        modelsMap.put(BindDirective.NAME, new BindDirective(request, response));
        modelsMap.put(TransformFunction.NAME, new TransformFunction(request, response));
        modelsMap.put(UrlFunction.NAME, new UrlFunction(request, response));
        modelsMap.put(EvalFunction.NAME, new EvalFunction(request, response));
    }

    @Override
    public TemplateModel get(String key) throws TemplateException {
        return modelsMap.get(key);
    }

    @Override
    public boolean isEmptyHash() throws TemplateException {
        return false;
    }

    TemplateStringModel getNestedPathModel() throws TemplateException {
        return (TemplateStringModel) get(NESTED_PATH_MODEL);
    }

    void setNestedPathModel(TemplateStringModel nestedPathModel) {
        modelsMap.put(NESTED_PATH_MODEL, nestedPathModel);
    }

    TemplateModel getEvaluationContextModel() throws TemplateException {
        return (TemplateModel) get(EVALUATION_CONTEXT_MODEL);
    }

    void setEvaluationContextModel(TemplateModel evaluationContextModel) {
        modelsMap.put(EVALUATION_CONTEXT_MODEL, evaluationContextModel);
    }

}

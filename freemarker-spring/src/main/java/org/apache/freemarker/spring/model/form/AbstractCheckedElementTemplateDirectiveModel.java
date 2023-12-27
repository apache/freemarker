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
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;

import java.io.IOException;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractCheckedElementTag</code>.
 */
abstract class AbstractCheckedElementTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    protected AbstractCheckedElementTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * Return "checkbox" or "radio".
     */
    protected abstract String getInputType();

    protected void renderFromValue(Environment env, Object value, TagOutputter tagOut)
            throws TemplateException, IOException {
        renderFromValue(env, value, value, tagOut);
    }

    protected void renderFromValue(Environment env, Object item, Object value, TagOutputter tagOut)
            throws TemplateException, IOException {
        String displayValue = getDisplayString(value, getBindStatus());
        tagOut.writeAttribute("value", processFieldValue(env, getName(), displayValue, getInputType()));

        if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
            tagOut.writeAttribute("checked", "checked");
        }
    }

    protected void renderFromBoolean(Environment env, Boolean boundValue, TagOutputter tagOut)
            throws TemplateException, IOException {
        tagOut.writeAttribute("value", processFieldValue(env, getName(), "true", getInputType()));

        if (boundValue != null && boundValue.booleanValue()) {
            tagOut.writeAttribute("checked", "checked");
        }
    }

    @Override
    protected String autogenerateId(Environment env) throws TemplateException {
        return TagIdGenerationUtils.getNextId(env, super.autogenerateId(env));
    }

    private boolean isOptionSelected(Object value) throws TemplateException {
        return SelectableValueComparisonUtils.isEqualValueBoundTo(value, getBindStatus());
    }
}

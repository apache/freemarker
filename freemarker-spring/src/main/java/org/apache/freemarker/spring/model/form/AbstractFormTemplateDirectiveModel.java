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

import java.beans.PropertyEditor;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CustomStateKey;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.spring.model.AbstractSpringTemplateDirectiveModel;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractFormTag</code>.
 */
abstract class AbstractFormTemplateDirectiveModel extends AbstractSpringTemplateDirectiveModel {

    protected static final CustomStateKey<FormTemplateScope> FORM_TEMPLATE_SCOPE_KEY = new CustomStateKey<FormTemplateScope>() {
        @Override
        protected FormTemplateScope create() {
            return new FormTemplateScope();
        }
    };

    protected AbstractFormTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    protected Object evaluate(String attributeName, Object value) throws TemplateException {
        return value;
    }

    public static String getDisplayString(Object value) {
        String displayValue = ObjectUtils.getDisplayString(value);
        return displayValue;
    }

    public static String getDisplayString(Object value, BindStatus bindStatus) {
        final PropertyEditor editor = (bindStatus != null && value != null) ? bindStatus.findEditor(value.getClass()) : null;
        return getDisplayString(value, editor);
    }

    public static String getDisplayString(Object value, PropertyEditor propertyEditor) {
        if (propertyEditor != null && !(value instanceof String)) {
            try {
                propertyEditor.setValue(value);
                String text = propertyEditor.getAsText();

                if (text != null) {
                    return getDisplayString(text);
                }
            } catch (Throwable ex) {
                // Ignore error if the PropertyEditor doesn't support this text value.
            }
        }

        return getDisplayString(value);
    }

    protected final void writeOptionalAttribute(TagOutputter tagOut, String attrName, Object attrValue)
            throws TemplateException, IOException {
        if (attrValue != null) {
            tagOut.writeOptionalAttributeValue(attrName, getDisplayString(evaluate(attrName, attrValue)));
        }
    }

}

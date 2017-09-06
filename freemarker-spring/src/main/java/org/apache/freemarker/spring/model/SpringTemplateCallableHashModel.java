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
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * TemplateHashModel wrapper for templates using Spring directives and functions.
 */
public final class SpringTemplateCallableHashModel implements TemplateHashModel, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "spring";

    public static final String NESTED_PATH = "nestedPath";

    private Map<String, AbstractSpringTemplateCallableModel> callablesMap = new HashMap<>();

    private String nestedPath;

    public SpringTemplateCallableHashModel(final HttpServletRequest request, final HttpServletResponse response) {
        callablesMap.put(BindDirective.NAME, new BindDirective(request, response));
        callablesMap.put(MessageFunction.NAME, new MessageFunction(request, response));
        callablesMap.put(ThemeFunction.NAME, new ThemeFunction(request, response));
        callablesMap.put(BindErrorsDirective.NAME, new BindErrorsDirective(request, response));
        callablesMap.put(NestedPathDirective.NAME, new NestedPathDirective(request, response));
    }

    public TemplateModel get(String key) throws TemplateException {
        if (NESTED_PATH.equals(key)) {
            return (nestedPath != null) ? new SimpleString(nestedPath) : null;
        }

        return callablesMap.get(key);
    }

    @Override
    public boolean isEmptyHash() throws TemplateException {
        return false;
    }

    public String getNestedPath() {
        return nestedPath;
    }

    public void setNestedPath(String nestedPath) {
        this.nestedPath = nestedPath;
    }

}

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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractDataBoundFormElementTag</code>.
 */
public abstract class AbstractDataBoundFormElementTemplateDirectiveModel extends AbstractFormTemplateDirectiveModel {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    protected AbstractDataBoundFormElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    protected void writeDefaultHtmlElementAttributes(TagOutputter tagOut) throws TemplateException, IOException {
        // FIXME
        writeOptionalAttribute(tagOut, "id", resolveId());
        writeOptionalAttribute(tagOut, "name", getName());
    }

    protected String resolveId() throws TemplateException {
        Object id = evaluate("id", getId());

        if (id != null) {
            String idString = id.toString();
            return (StringUtils.hasText(idString) ? idString : null);
        }

        return autogenerateId();
    }

    protected String autogenerateId() throws TemplateException {
        return StringUtils.deleteAny(getName(), "[]");
    }

    protected String getName() throws TemplateException {
        // FIXME
        return "name";
        //return getPropertyPath();
    }

    protected String getPropertyPath(Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext, String path,
            boolean ignoreNestedPath) throws TemplateException {
        BindStatus status = getBindStatus(env, objectWrapperAndUnwrapper, requestContext, path, ignoreNestedPath);
        String expression = status.getExpression();
        return (expression != null ? expression : "");
    }

}

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
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.spring.model.AbstractSpringTemplateDirectiveModel;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractFormTag</code>.
 */
public abstract class AbstractFormTemplateDirectiveModel extends AbstractSpringTemplateDirectiveModel {

    protected AbstractFormTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected final void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        final TagOutputter tagOut = new TagOutputter(out);
        writeDirectiveContent(args, callPlace, tagOut, env, objectWrapperAndUnwrapper, requestContext);
    }

    protected abstract void writeDirectiveContent(TemplateModel[] args, CallPlace callPlace, TagOutputter tagOut,
            Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException;

    protected Object evaluate(String attributeName, Object value) throws TemplateException {
        return value;
    }

    protected String getDisplayString(Object value) {
        String displayValue = ObjectUtils.getDisplayString(value);
        return displayValue;
    }

    protected final void writeOptionalAttribute(TagOutputter tagOut, String attrName, Object attrValue)
            throws TemplateException, IOException {
        if (attrValue != null) {
            tagOut.writeOptionalAttributeValue(attrName, getDisplayString(evaluate(attrName, attrValue)));
        }
    }

}

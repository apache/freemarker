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
import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractSingleCheckedElementTag</code>.
 */
abstract class AbstractSingleCheckedElementTemplateDirectiveModel extends AbstractCheckedElementTemplateDirectiveModel {

    protected AbstractSingleCheckedElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        TagOutputter tagOut = new TagOutputter(env, out);

        tagOut.beginTag("input");
        String id = resolveId(env);
        writeOptionalAttribute(tagOut, "id", id);
        writeOptionalAttribute(tagOut, "name", getName());
        writeOptionalAttributes(tagOut);
        writeAdditionalDetails(env, tagOut);
        tagOut.endTag();

        Object resolvedLabel = evaluate("label", getLabel());

        if (resolvedLabel != null) {
            tagOut.beginTag("label");
            tagOut.writeAttribute("for", id);
            tagOut.appendValue(getDisplayString(resolvedLabel, getBindStatus()));
            tagOut.endTag();
        }
    }

    public abstract Object getValue();

    public abstract Object getLabel();

    protected abstract void writeAdditionalDetails(Environment env, TagOutputter tagOut) throws TemplateException, IOException;

}

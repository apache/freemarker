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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractDataBoundFormElementTag</code>.
 */
public abstract class AbstractDataBoundFormElementTemplateDirectiveModel extends AbstractFormTemplateDirectiveModel {

    private static final int PATH_PARAM_IDX = 0;

    private static final int ID_PARAM_IDX = 1;

    private static final String ID_PARAM_NAME = "id";

    protected static List<StringToIndexMap.Entry> NAMED_ARGS_ENTRY_LIST = Arrays.asList(
            new StringToIndexMap.Entry(ID_PARAM_NAME, ID_PARAM_IDX)
    );

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(NAMED_ARGS_ENTRY_LIST.toArray(new StringToIndexMap.Entry[NAMED_ARGS_ENTRY_LIST.size()])),
                    true
                    );

    private String path;
    private String id;

    private BindStatus bindStatus;

    protected AbstractDataBoundFormElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

    public String getPath() {
        return path;
    }

    public String getId() {
        return id;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        path = CallableUtils.getOptionalStringArgument(args, PATH_PARAM_IDX, this);
        id = CallableUtils.getOptionalStringArgument(args, ID_PARAM_IDX, this);

        bindStatus = getBindStatus(env, objectWrapperAndUnwrapper, requestContext, path, false);
    }

    protected BindStatus getBindStatus() {
        return bindStatus;
    }

    protected void writeDefaultAttributes(TagOutputter tagOut) throws TemplateException, IOException {
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

    protected final String processFieldValue(Environment env, String name, String value, String type) throws TemplateException {
        RequestContext requestContext = getRequestContext(env, false);
        RequestDataValueProcessor processor = requestContext.getRequestDataValueProcessor();

        // FIXME
//        ServletRequest request = this.pageContext.getRequest();
//
//        if (processor != null && (request instanceof HttpServletRequest)) {
//            value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
//        }

        return value;
    }

}

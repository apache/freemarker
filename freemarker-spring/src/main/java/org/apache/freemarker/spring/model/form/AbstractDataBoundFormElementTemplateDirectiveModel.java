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
abstract class AbstractDataBoundFormElementTemplateDirectiveModel extends AbstractFormTemplateDirectiveModel {

    private static final int PATH_PARAM_IDX = 0;

    private static final int ID_PARAM_IDX = 1;

    private static final String ID_ATTR_NAME = "id";

    private static final String NAME_ATTR_NAME = "name";

    private static final String ID_PARAM_NAME = ID_ATTR_NAME;

    /**
     * Returns the argument index of the last predefined named argument item in the {@code argsLayout}.
     * <P>
     * <EM>Note:</EM> It is strongly assumed that the predefined named arguments map contains only items with indexes,
     * starting from the predefined positional argument count and incrementing by one sequentially.
     * </P>
     * @param argsLayout arguments layout
     * @return the argument index of the last predefined named argument item in the {@code argsLayout}
     */
    protected static int getLastPredefinedNamedArgumentIndex(ArgumentArrayLayout argsLayout) {
        return argsLayout.getPredefinedPositionalArgumentCount() + argsLayout.getPredefinedNamedArgumentsMap().size()
                - 1;
    }

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(ID_PARAM_NAME, ID_PARAM_IDX),
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
        writeOptionalAttribute(tagOut, ID_ATTR_NAME, resolveId());
        writeOptionalAttribute(tagOut, NAME_ATTR_NAME, getName());
    }

    protected String resolveId() throws TemplateException {
        Object id = evaluate(ID_PARAM_NAME, getId());

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
        return getPropertyPath();
    }

    protected String getPropertyPath() {
        String expression = getBindStatus().getExpression();
        return (expression != null ? expression : "");
    }

    protected final String processFieldValue(Environment env, String name, String value, String type) throws TemplateException {
        RequestContext requestContext = getRequestContext(env, false);
        RequestDataValueProcessor processor = requestContext.getRequestDataValueProcessor();
        HttpServletRequest request = getRequest();

        if (processor != null && request != null) {
            value = processor.processFormFieldValue(request, name, value, type);
        }

        return value;
    }

}

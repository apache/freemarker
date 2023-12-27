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
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code <input type="radio"/>}' element.
 * This tag is provided for completeness if the application relies on a
 * <code>org.springframework.web.servlet.support.RequestDataValueProcessor</code>.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   &lt;@form.radio 'user.gender' value='U'/&gt;Unspecified
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:button /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class RadioButtonTemplateDirectiveModel extends AbstractSingleCheckedElementTemplateDirectiveModel {

    public static final String NAME = "radiobutton";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int VALUE_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String VALUE_PARAM_NAME = "value";

    private static final int LABEL_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String LABEL_PARAM_NAME = "label";

    protected static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                    new StringToIndexMap.Entry(VALUE_PARAM_NAME, VALUE_PARAM_IDX),
                    new StringToIndexMap.Entry(LABEL_PARAM_NAME, LABEL_PARAM_IDX)),
            true);

    private String value;
    private String label;

    protected RadioButtonTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        value = CallableUtils.getOptionalStringArgument(args, VALUE_PARAM_IDX, this);
        label = CallableUtils.getOptionalStringArgument(args, LABEL_PARAM_IDX, this);

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    protected void writeAdditionalDetails(Environment env, TagOutputter tagOut) throws TemplateException, IOException {
        tagOut.writeAttribute("type", getInputType());
        Object resolvedValue = evaluate("value", getValue());
        renderFromValue(env, resolvedValue, tagOut);
    }

    @Override
    protected String getInputType() {
        return "radio";
    }

}

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
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Provides a convenient <code>TemplateModel</code> that allow to supply a collection that are to be rendered
 * as an HTML '{@code option}' element.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>items</code>: collection of option items.</LI>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   ...
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:input /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class OptionTemplateDirectiveModel extends AbstractHtmlInputElementTemplateDirectiveModel {

    public static final String NAME = "option";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int VALUE_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String VALUE_PARAM_NAME = "value";

    private static final int LABEL_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String LABEL_PARAM_NAME = "label";

    protected static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(
                    AbstractHtmlInputElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                    new StringToIndexMap.Entry(VALUE_PARAM_NAME, VALUE_PARAM_IDX),
                    new StringToIndexMap.Entry(LABEL_PARAM_NAME, LABEL_PARAM_IDX)),
            true);

    private String value;
    private String label;

    protected OptionTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, final Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        value = CallableUtils.getOptionalStringArgument(args, VALUE_PARAM_IDX, this);
        label = CallableUtils.getOptionalStringArgument(args, LABEL_PARAM_IDX, this);

        final FormTemplateScope formTemplateScope = env.getCustomState(FORM_TEMPLATE_SCOPE_KEY);
        final SelectTemplateDirectiveModel curSelectDirective = formTemplateScope.getCurrentSelectDirective();

        final String selectName = curSelectDirective.getName();
        final String valueProperty = (getValue() != null
                ? ObjectUtils.getDisplayString(evaluate("value", getValue()))
                : null);
        final String labelProperty = (getLabel() != null
                ? ObjectUtils.getDisplayString(evaluate("label", getLabel()))
                : null);

        final TagOutputter tagOut = formTemplateScope.getCurrentTagOutputter();

        tagOut.beginTag("option");
        writeOptionalAttribute(tagOut, "id", resolveId());
        writeOptionalAttributes(tagOut);
        String renderedValue = getDisplayString(value, curSelectDirective.getBindStatus().getEditor());
        renderedValue = processFieldValue(env, selectName, renderedValue, "option");
        tagOut.writeAttribute("value", renderedValue);

        if (SelectableValueComparisonUtils.isEqualValueBoundTo(value, curSelectDirective.getBindStatus())) {
            tagOut.writeAttribute("selected", "selected");
        }

        if (isDisabled()) {
            tagOut.writeAttribute("disabled", "disabled");
        }

        tagOut.appendValue(label);
        tagOut.endTag();
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @Override
    protected String autogenerateId() {
        return null;
    }
}

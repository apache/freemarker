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
import org.springframework.web.servlet.support.RequestContext;

/**
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code button}' element.
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
 *   &lt;@form.button 'user.email' /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:button /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class ButtonTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "button";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int NAME_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String NAME_PARAM_NAME = "name";

    private static final int VALUE_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String VALUE_PARAM_NAME = "value";

    private static final int DISABLED_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String DISABLED_PARAM_NAME = "disabled";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(NAME_PARAM_NAME, NAME_PARAM_IDX),
                            new StringToIndexMap.Entry(VALUE_PARAM_NAME, VALUE_PARAM_IDX),
                            new StringToIndexMap.Entry(DISABLED_PARAM_NAME, DISABLED_PARAM_IDX)
                            ),
                    true);

    private String name;
    private String value;
    private boolean disabled;

    protected ButtonTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        name = CallableUtils.getOptionalStringArgument(args, NAME_PARAM_IDX, this);
        value = CallableUtils.getOptionalStringArgument(args, VALUE_PARAM_IDX, this);
        disabled = CallableUtils.getOptionalBooleanArgument(args, DISABLED_PARAM_IDX, this, false);

        TagOutputter tagOut = new TagOutputter(out);

        tagOut.beginTag("button");

        writeDefaultAttributes(tagOut);

        // more optional attributes by this tag
        tagOut.writeAttribute("type", getType());

        String valueToUse = (getValue() != null) ? getValue() : getDefaultValue();
        tagOut.writeAttribute("value", processFieldValue(env, getName(), valueToUse, getType()));

        if (isDisabled()) {
            tagOut.writeAttribute(DISABLED_PARAM_NAME, "disabled");
        }

        tagOut.forceBlock();

        try {
            callPlace.executeNestedContent(null, out, env);
        } finally {
            tagOut.endTag();
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isDisabled() {
        return disabled;
    }

    protected String getDefaultValue() {
        return "Submit";
    }

    protected String getType() {
        return "submit";
    }

}

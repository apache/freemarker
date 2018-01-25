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
 * Provides <code>TemplateModel</code> for data-binding-aware HTML '{@code input}' element with a '{@code type}'
 * of '{@code hidden}'.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>path</code>: The first positional parameter pointing to the bean or bean property to bind status information for.</LI>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   &lt;@form.hidden 'user.email' /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:hidden /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class HiddenInputTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "hidden";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int DISABLED_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String DISABLED_PARAM_NAME = "disabled";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractHtmlElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(DISABLED_PARAM_NAME, DISABLED_PARAM_IDX)
                            ),
                    true);

    private boolean disabled;

    protected HiddenInputTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
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
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        disabled = CallableUtils.getOptionalBooleanArgument(args, DISABLED_PARAM_IDX, this, false);

        TagOutputter tagOut = new TagOutputter(out);

        tagOut.beginTag("input");

        writeDefaultAttributes(tagOut);

        // more optional attributes by this tag
        tagOut.writeAttribute("type", "hidden");
        if (isDisabled()) {
            tagOut.writeAttribute(DISABLED_PARAM_NAME, "disabled");
        }

        String value = getDisplayString(getBindStatus().getValue(), getBindStatus().getEditor());
        tagOut.writeAttribute("value", processFieldValue(env, getName(), value, "hidden"));

        tagOut.endTag();

    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return !"type".equals(localName);
    }

}

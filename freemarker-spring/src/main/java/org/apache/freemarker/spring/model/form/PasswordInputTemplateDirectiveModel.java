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
 * of '{@code password}'.
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
 *   &lt;@form.password 'user.password' /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:password /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class PasswordInputTemplateDirectiveModel extends InputTemplateDirectiveModel {

    public static final String NAME = "password";

    private static final int NAMED_ARGS_OFFSET = InputTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int SHOW_PASSWORD_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String SHOW_PASSWORD_PARAM_NAME = "showPassword";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(InputTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(SHOW_PASSWORD_PARAM_NAME, SHOW_PASSWORD_PARAM_IDX)
                            ),
                    true);

    private boolean showPassword;

    protected PasswordInputTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {
        showPassword = CallableUtils.getOptionalBooleanArgument(args, SHOW_PASSWORD_PARAM_IDX, this, false);
        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);
    }

    public boolean isShowPassword() {
        return showPassword;
    }

    @Override
    protected String getType() {
        return "password";
    }

    @Override
    protected void writeValue(Environment env, TagOutputter tagOut) throws TemplateException, IOException {
        if (isShowPassword()) {
            super.writeValue(env, tagOut);
        } else {
            tagOut.writeAttribute("value", processFieldValue(env, getName(), "", getType()));
        }
    }

    @Override
    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return !"type".equals(localName);
    }

}

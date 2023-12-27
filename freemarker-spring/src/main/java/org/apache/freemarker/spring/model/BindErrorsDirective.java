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

package org.apache.freemarker.spring.model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Provides <code>TemplateModel</code> wrapping the bind errors (type of <code>org.springframework.validation.Errors</code>)
 * for the given name, working similarly to Spring Framework's <code>&lt;spring:hasBindErrors /&gt;</code> JSP Tag Library.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>name</code>: The first positional parameter for the name of the bean that this directive should check.</LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * &lt;@spring.hasBindErrors "user"; errors&gt;
 *   &lt;div class="errors"&gt;
 *     &lt;#list errors.allErrors as error&gt;
 *       &lt;div class="error"&gt;
 *         ${spring.message(message=error)!}
 *       &lt;/div&gt;
 *     &lt;/#list&gt;
 *   &lt;/div&gt;
 * &lt;/@spring.hasBindErrors&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:hasBindErrors /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always has an <code>org.springframework.validation.Errors</code>
 * instance not to escape HTML's because it is much easier to control escaping in FreeMarker Template expressions
 * rather than depending on directives.
 * </P>
 */
class BindErrorsDirective extends AbstractSpringTemplateDirectiveModel {

    public static final String NAME = "hasBindErrors";

    private static final int NAME_PARAM_IDX = 0;

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    null,
                    false
                    );

    protected BindErrorsDirective(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException, IOException {
        final String name = CallableUtils.getStringArgument(args, NAME_PARAM_IDX, this);

        final TemplateModel bindErrorsModel = getBindErrorsTemplateModel(env, objectWrapperAndUnwrapper, requestContext,
                name);

        if (bindErrorsModel != null) {
            final TemplateModel[] nestedContentArgs = new TemplateModel[] { bindErrorsModel };
            callPlace.executeNestedContent(nestedContentArgs, out, env);
        }
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    private final TemplateModel getBindErrorsTemplateModel(Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext, String name)
                    throws TemplateException {
        final Errors errors = requestContext.getErrors(name, false);

        if (errors != null && errors.hasErrors()) {
            return objectWrapperAndUnwrapper.wrap(errors);
        }

        return null;
    }

}

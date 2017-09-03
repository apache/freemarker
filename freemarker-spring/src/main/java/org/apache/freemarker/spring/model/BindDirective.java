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
 * Provides <code>TemplateModel</code> wrapping <code>BindStatus</code> for the given bind path, working similarly
 * to Spring Framework's <code>&lt;spring:bind /&gt;</code> JSP Tag Library.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>ignoreNestedPath</code>: Set whether to ignore a nested path, if any. <code>false</code> by default.</LI>
 * <LI><code>path</code>: The path to the bean or bean property to bind status information for.</LI>
 * </UL>
 * </P>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:bind /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always has <code>BindStatus</code> not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions rather than depending on directives.
 * </P>
 */
public class BindDirective extends AbstractSpringTemplateDirectiveModel {

    private static final int PATH_PARAM_IDX = 0;
    private static final int IGNORE_NESTED_PATH_PARAM_IDX = 1;

    private static final String IGNORE_NESTED_PATH_PARAM_NAME = "ignoreNestedPath";

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    true,
                    StringToIndexMap.of(IGNORE_NESTED_PATH_PARAM_NAME, IGNORE_NESTED_PATH_PARAM_IDX),
                    false
                    );

    public BindDirective(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException, IOException {
        final String path = CallableUtils.getStringArgument(args, PATH_PARAM_IDX, this);
        final boolean ignoreNestedPath = CallableUtils.getOptionalBooleanArgument(args, IGNORE_NESTED_PATH_PARAM_IDX,
                this, false);

        final TemplateModel statusModel = getBindStatusTemplateModel(env, objectWrapperAndUnwrapper, requestContext,
                path, ignoreNestedPath);
        final TemplateModel[] nestedContentArgs = new TemplateModel[] { statusModel };

        callPlace.executeNestedContent(nestedContentArgs, out, env);
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }
}

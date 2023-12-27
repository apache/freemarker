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
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.beans.PropertyAccessor;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Provides <code>TemplateModel</code> setting <code>spring.nestedPath</code> by the given bind path, working similarly
 * to Spring Framework's <code>&lt;spring:nestedPath /&gt;</code> JSP Tag Library.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI><code>path</code>: The first positional parameter to set a new nested path by appending it to the existing nested path if any existing.</LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   &lt;@spring.nestedPath "user"&gt;
 *     &lt;#-- nested content --/&gt;
 *   &lt;/@spring.nestedPath&gt;
 * </PRE>
 */
class NestedPathDirective extends AbstractSpringTemplateDirectiveModel {

    public static final String NAME = "nestedPath";

    private static final int PATH_PARAM_IDX = 0;

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    null,
                    false
                    );

    protected NestedPathDirective(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException, IOException {
        String path = CallableUtils.getStringArgument(args, PATH_PARAM_IDX, this);

        if (path == null) {
            path = "";
        }

        if (!path.isEmpty() && !path.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
            path += PropertyAccessor.NESTED_PROPERTY_SEPARATOR;
        }

        final SpringTemplateCallableHashModel springTemplateModel = getSpringTemplateCallableHashModel(env);
        final TemplateStringModel prevNestedPathModel = springTemplateModel.getNestedPathModel();
        final String prevNestedPath = (prevNestedPathModel != null) ? prevNestedPathModel.getAsString() : null;
        final String newNestedPath = (prevNestedPath != null) ? prevNestedPath + path : path;
        final TemplateStringModel newNestedPathModel = (TemplateStringModel) objectWrapperAndUnwrapper
                .wrap(newNestedPath);

        try {
            springTemplateModel.setNestedPathModel(newNestedPathModel);
            callPlace.executeNestedContent(null, out, env);
        } finally {
            springTemplateModel.setNestedPathModel(prevNestedPathModel);
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

}

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
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.BeanModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;

public class BindDirective implements TemplateDirectiveModel {

    public static final String STATUS_VARIABLE_NAME = "status";

    /**
     * @see <code>org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME</code>
     */
    private static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";

    private static final int PATH_PARAM_IDX = 0;
    private static final int IGNORE_NESTED_PATH_PARAM_IDX = 1;

    private static final String IGNORE_NESTED_PATH_PARAM_NAME = "ignoreNestedPath";

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            1,
            true,
            StringToIndexMap.of(
                    IGNORE_NESTED_PATH_PARAM_NAME, IGNORE_NESTED_PATH_PARAM_IDX
            ),
            false
    );

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public BindDirective(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        final ObjectWrapper objectWrapper = env.getObjectWrapper();

        if (!(objectWrapper instanceof DefaultObjectWrapper)) {
            throw new TemplateException("The ObjectWrapper of environment wasn't instance of " + DefaultObjectWrapper.class.getName());
        }

        TemplateModel model = env.getDataModel().get(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
        RequestContext requestContext = (RequestContext) ((DefaultObjectWrapper) objectWrapper).unwrap(model);

        String resolvedPath = CallableUtils.getStringArgument(args, PATH_PARAM_IDX, this);
        boolean ignoreNestedPath = CallableUtils.getOptionalBooleanArgument(args, IGNORE_NESTED_PATH_PARAM_IDX, this,
                false);

        if (!ignoreNestedPath) {
            resolvedPath = resolveNestedPath(resolvedPath);
        }

        BindStatus status = requestContext.getBindStatus(resolvedPath);
        env.setLocalVariable(STATUS_VARIABLE_NAME, new BeanModel(status, (DefaultObjectWrapper) objectWrapper));

        callPlace.executeNestedContent(null, out, env);
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    private String resolveNestedPath(final String path) {
        String nestedPath = (String) request.getAttribute(NESTED_PATH_VARIABLE_NAME);

        if (nestedPath != null && !path.startsWith(nestedPath)
                && !path.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
            return nestedPath + path;
        }

        return path;
    }
}

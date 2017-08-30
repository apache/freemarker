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
        boolean ignoreNestedPath = CallableUtils.getOptionalBooleanArgument(args, IGNORE_NESTED_PATH_PARAM_IDX, this,
                false);

        TemplateModel statusModel = getBindStatusTemplateModel(env, objectWrapperAndUnwrapper, requestContext, path,
                ignoreNestedPath);
        TemplateModel[] nestedContentArgs = new TemplateModel[] { statusModel };

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

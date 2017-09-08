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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.web.servlet.support.RequestContext;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:url /&gt;</code> JSP Tag Library.
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:message /&gt;</code> JSP Tag Library, this function
 * does not support <code>htmlEscape</code> parameter. It always returns the message not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
public class UrlFunction extends AbstractSpringTemplateFunctionModel {

    public static final String NAME = "url";

    private static final int VALUE_PARAM_IDX = 0;
    private static final int CONTEXT_PARAM_IDX = 1;

    private static final String CONTEXT_PARAM_NAME = "context";


    // TODO: How to deal with parameters (spring:param tags)??


    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(CONTEXT_PARAM_NAME, CONTEXT_PARAM_IDX),
                    false
                    );

    public UrlFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        // TODO
        return null;
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}

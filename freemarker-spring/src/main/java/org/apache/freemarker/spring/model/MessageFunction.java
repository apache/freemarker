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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.servlet.support.RequestContext;

public class MessageFunction extends AbstractSpringTemplateFunctionModel {

    private static final int CODE_PARAM_IDX = 0;
    private static final int MESSAGE_PARAM_IDX = 1;

    private static final String MESSAGE_PARAM_NAME = "message";

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    true,
                    StringToIndexMap.of(
                            MESSAGE_PARAM_NAME, MESSAGE_PARAM_IDX),
                    false);

    public MessageFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        final MessageSource messageSource = requestContext.getMessageSource();

        if (messageSource == null) {
            throw new TemplateException("MessageSource not found.");
        }

        String message = null;

        final String code = CallableUtils.getStringArgument(args, CODE_PARAM_IDX, this);

        if (code != null && !code.isEmpty()) {
            List<Object> msgArgumentList = null;
            // TODO: How to read message arguments from the varags?

            // TODO: Is it okay to set the default value to null to avoid NoSuchMessageException from Spring MessageSource?
            message = messageSource.getMessage(code, (msgArgumentList == null) ? null : msgArgumentList.toArray(),
                    null, requestContext.getLocale());
        } else {
            final TemplateModel messageModel = CallableUtils.getOptionalArgument(args, MESSAGE_PARAM_IDX,
                    TemplateModel.class, this);
            if (messageModel != null) {
                MessageSourceResolvable messageResolvable = (MessageSourceResolvable) objectWrapperAndUnwrapper
                        .unwrap(messageModel);
                message = messageSource.getMessage(messageResolvable, requestContext.getLocale());
            }
        }

        return (message != null) ? new SimpleString(message) : null;
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}

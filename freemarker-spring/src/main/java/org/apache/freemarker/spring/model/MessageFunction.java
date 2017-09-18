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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.servlet.support.RequestContext;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:message /&gt;</code> JSP Tag Library.
 * It retrieves the theme message with the given code or the resolved text by the given <code>message</code> parameter.
 * <P>
 * This function supports the following parameters:
 * <UL>
 * <LI><code>code</code>: The first optional positional parameter. The key to use when looking up the message.
 * <LI><code>message arguments</code>: Positional varargs after <code>code</code> parameter, as message arguments.</LI>
 * <LI><code>message</code>: Named parameters as <code>MessageResolvable</code> object.</LI>
 * </UL>
 * </P>
 * <P>
 * This function requires either <code>code</code> parameter or <code>message</code> parameter at least.
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * &lt;#-- With 'code' positional parameter only --&gt;
 * ${spring.message("label.user.firstName")!}
 *
 * &lt;#-- With 'code' positional parameter and message arguments (varargs) --&gt;
 * ${spring.message("message.user.form", user.firstName, user.lastName, user.email)}
 *
 * &lt;#-- With 'message' named parameter (assuming a <code>MessageResolvable</code> object is set to a model attribute) --&gt;
 * ${spring.message(message=myMessageResolvable)}
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:message /&gt;</code> JSP Tag Library, this function
 * does not support <code>htmlEscape</code> parameter. It always returns the message not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class MessageFunction extends AbstractSpringTemplateFunctionModel {

    public static final String NAME = "message";

    private static final int CODE_PARAM_IDX = 0;
    private static final int MESSAGE_RESOLVABLE_PARAM_IDX = 1;
    private static final int MESSAGE_ARGS_PARAM_IDX = 2;

    private static final String MESSAGE_RESOLVABLE_PARAM_NAME = "message";

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    true,
                    StringToIndexMap.of(MESSAGE_RESOLVABLE_PARAM_NAME, MESSAGE_RESOLVABLE_PARAM_IDX),
                    false
                    );

    protected MessageFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        final MessageSource messageSource = getMessageSource(requestContext);

        if (messageSource == null) {
            throw CallableUtils.newGenericExecuteException("MessageSource not found from the request context.", this);
        }

        String message = null;

        final MessageSourceResolvable messageResolvable = CallableUtils.getOptionalArgumentAndUnwrap(args,
                MESSAGE_RESOLVABLE_PARAM_IDX, MessageSourceResolvable.class, this, objectWrapperAndUnwrapper);

        if (messageResolvable != null) {
            message = messageSource.getMessage(messageResolvable, requestContext.getLocale());
        } else {
            final String code = _StringUtils
                    .emptyToNull(CallableUtils.getOptionalStringArgument(args, CODE_PARAM_IDX, this));

            if (code != null) {
                List<Object> msgArgumentList = null;
                final TemplateCollectionModel messageArgsModel = (TemplateCollectionModel) args[MESSAGE_ARGS_PARAM_IDX];

                if (!messageArgsModel.isEmptyCollection()) {
                    msgArgumentList = new ArrayList<>();
                    TemplateModel msgArgModel;
                    for (TemplateModelIterator tit = messageArgsModel.iterator(); tit.hasNext();) {
                        msgArgModel = tit.next();
                        msgArgumentList.add(objectWrapperAndUnwrapper.unwrap(msgArgModel));
                    }
                }

                // Note: Pass null as default value to avoid NoSuchMessageException from Spring MessageSource
                //       since we want to take advantage of FreeMarker's default value expressions.
                message = messageSource.getMessage(code, (msgArgumentList == null) ? null : msgArgumentList.toArray(),
                        null, requestContext.getLocale());
            } else {
                throw CallableUtils.newNullOrOmittedArgumentException(CODE_PARAM_IDX, this);
            }
        }

        return (message != null) ? objectWrapperAndUnwrapper.wrap(message) : null;
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    /**
     * Get the {@link MessageSource} bean from the current application context.
     * @param requestContext Spring Framework RequestContext
     * @return the {@link MessageSource} bean from the current application context
     */
    protected MessageSource getMessageSource(final RequestContext requestContext) {
        return requestContext.getMessageSource();
    }

}

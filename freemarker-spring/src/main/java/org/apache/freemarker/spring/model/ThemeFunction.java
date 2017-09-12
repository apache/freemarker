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

import org.springframework.context.MessageSource;
import org.springframework.web.servlet.support.RequestContext;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:theme /&gt;</code> JSP Tag Library.
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
 * ${spring.theme("styleSheet")!}
 *
 * &lt;link rel="stylesheet" href="${spring.theme('styleSheet')}" type="text/css" /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:theme /&gt;</code> JSP Tag Library, this function
 * does not support <code>htmlEscape</code> parameter. It always returns the message not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
public class ThemeFunction extends MessageFunction {

    public static final String NAME = "theme";

    public ThemeFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    protected MessageSource getMessageSource(final RequestContext requestContext) {
        return requestContext.getTheme().getMessageSource();
    }

}

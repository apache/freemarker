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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

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
import org.apache.freemarker.core.util._CollectionUtils;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

public class FormTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "form";

    private static final int NAMED_ARGS_OFFSET = AbstractHtmlElementTemplateDirectiveModel.NAMED_ARGS_ENTRY_LIST.size()
            + 1;

    private static final int ACTION_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String ACTION_PARAM_NAME = "action";

    private static final int METHOD_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String METHOD_PARAM_NAME = "method";

    private static final int TARGET_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String TARGET_PARAM_NAME = "target";

    private static final int ENCTYPE_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String ENCTYPE_PARAM_NAME = "enctype";

    private static final int ACCEPT_CHARSET_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String ACCEPT_CHARSET_PARAM_NAME = "accept-charset";

    private static final int ONSUBMIT_PARAM_IDX = NAMED_ARGS_OFFSET + 5;
    private static final String ONSUBMIT_PARAM_NAME = "onsubmit";

    private static final int ONRESET_PARAM_IDX = NAMED_ARGS_OFFSET + 6;
    private static final String ONRESET_PARAM_NAME = "onreset";

    private static final int AUTOCOMPLETE_PARAM_IDX = NAMED_ARGS_OFFSET + 7;
    private static final String AUTOCOMPLETE_PARAM_NAME = "autocomplete";

    private static final int NAME_PARAM_IDX = NAMED_ARGS_OFFSET + 8;
    private static final String NAME_PARAM_NAME = "name";

    private static final int VALUE_PARAM_IDX = NAMED_ARGS_OFFSET + 9;
    private static final String VALUE_PARAM_NAME = "value";

    private static final int TYPE_PARAM_IDX = NAMED_ARGS_OFFSET + 10;
    private static final String TYPE_PARAM_NAME = "type";

    private static final int SERVLET_RELATIVE_ACTION_PARAM_IDX = NAMED_ARGS_OFFSET + 11;
    private static final String SERVLET_RELATIVE_ACTION_PARAM_NAME = "servletRelativeAction";

    private static final int METHOD_PARAM_PARAM_IDX = NAMED_ARGS_OFFSET + 12;
    private static final String METHOD_PARAM_PARAM_NAME = "methodParam";

    @SuppressWarnings("unchecked")
    protected static List<StringToIndexMap.Entry> NAMED_ARGS_ENTRY_LIST =
            _CollectionUtils.mergeImmutableLists(false,
                    AbstractHtmlElementTemplateDirectiveModel.NAMED_ARGS_ENTRY_LIST,
                    Arrays.asList(
                        new StringToIndexMap.Entry(ACTION_PARAM_NAME, ACTION_PARAM_IDX),
                        new StringToIndexMap.Entry(METHOD_PARAM_NAME, METHOD_PARAM_IDX),
                        new StringToIndexMap.Entry(TARGET_PARAM_NAME, TARGET_PARAM_IDX),
                        new StringToIndexMap.Entry(ENCTYPE_PARAM_NAME, ENCTYPE_PARAM_IDX),
                        new StringToIndexMap.Entry(ACCEPT_CHARSET_PARAM_NAME, ACCEPT_CHARSET_PARAM_IDX),
                        new StringToIndexMap.Entry(ONSUBMIT_PARAM_NAME, ONSUBMIT_PARAM_IDX),
                        new StringToIndexMap.Entry(ONRESET_PARAM_NAME, ONRESET_PARAM_IDX),
                        new StringToIndexMap.Entry(AUTOCOMPLETE_PARAM_NAME, AUTOCOMPLETE_PARAM_IDX),
                        new StringToIndexMap.Entry(NAME_PARAM_NAME, NAME_PARAM_IDX),
                        new StringToIndexMap.Entry(VALUE_PARAM_NAME, VALUE_PARAM_IDX),
                        new StringToIndexMap.Entry(TYPE_PARAM_NAME, TYPE_PARAM_IDX),
                        new StringToIndexMap.Entry(SERVLET_RELATIVE_ACTION_PARAM_NAME, SERVLET_RELATIVE_ACTION_PARAM_IDX),
                        new StringToIndexMap.Entry(METHOD_PARAM_PARAM_NAME, METHOD_PARAM_PARAM_IDX)
                        )
                    );

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(NAMED_ARGS_ENTRY_LIST.toArray(new StringToIndexMap.Entry[NAMED_ARGS_ENTRY_LIST.size()])),
                    true
                    );

    private static final String FORM_TAG_NAME = "form";

    private static final String INPUT_TAG_NAME = "input";

    private static final String DEFAULT_METHOD = "post";

    private String action;
    private String method;
    private String target;
    private String enctype;
    private String acceptCharset;
    private String onsubmit;
    private String onreset;
    private String autocomplete;
    private String name;
    private String value;
    private String type;
    private String servletRelativeAction;
    private String methodParam;

    protected FormTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return true;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        action = CallableUtils.getOptionalStringArgument(args, ACTION_PARAM_IDX, "", this);
        method = CallableUtils.getOptionalStringArgument(args, METHOD_PARAM_IDX, DEFAULT_METHOD, this);
        target = CallableUtils.getOptionalStringArgument(args, TARGET_PARAM_IDX, this);
        enctype = CallableUtils.getOptionalStringArgument(args, ENCTYPE_PARAM_IDX, this);
        acceptCharset = CallableUtils.getOptionalStringArgument(args, ACCEPT_CHARSET_PARAM_IDX, this);
        onsubmit = CallableUtils.getOptionalStringArgument(args, ONSUBMIT_PARAM_IDX, this);
        onreset = CallableUtils.getOptionalStringArgument(args, ONRESET_PARAM_IDX, this);
        autocomplete = CallableUtils.getOptionalStringArgument(args, AUTOCOMPLETE_PARAM_IDX, this);
        name = CallableUtils.getOptionalStringArgument(args, NAME_PARAM_IDX, this);
        value = CallableUtils.getOptionalStringArgument(args, VALUE_PARAM_IDX, this);
        type = CallableUtils.getOptionalStringArgument(args, TYPE_PARAM_IDX, this);
        servletRelativeAction = CallableUtils.getOptionalStringArgument(args, SERVLET_RELATIVE_ACTION_PARAM_IDX, this);
        methodParam = CallableUtils.getOptionalStringArgument(args, METHOD_PARAM_PARAM_IDX, this);

        TagOutputter tagOut = new TagOutputter(out);

        tagOut.beginTag(FORM_TAG_NAME);
        writeDefaultAttributes(tagOut);
        tagOut.writeAttribute(ACTION_PARAM_NAME, resolveAction(env));
        writeOptionalAttribute(tagOut, METHOD_PARAM_NAME, getHttpMethod());
        writeOptionalAttribute(tagOut, TARGET_PARAM_NAME, getTarget());
        writeOptionalAttribute(tagOut, ENCTYPE_PARAM_NAME, getEnctype());
        writeOptionalAttribute(tagOut, ACCEPT_CHARSET_PARAM_NAME, getAcceptCharset());
        writeOptionalAttribute(tagOut, ONSUBMIT_PARAM_NAME, getOnsubmit());
        writeOptionalAttribute(tagOut, ONRESET_PARAM_NAME, getOnreset());
        writeOptionalAttribute(tagOut, AUTOCOMPLETE_PARAM_NAME, getAutocomplete());

        tagOut.forceBlock();

        final String methodName = getMethod();

        if (!isMethodBrowserSupported(methodName)) {
            if (!isValidHttpMethod(methodName)) {
                throw new IllegalArgumentException("Invalid HTTP method: " + method);
            }

            String inputName = getMethodParam();
            String inputType = "hidden";
            tagOut.beginTag(INPUT_TAG_NAME);
            writeOptionalAttribute(tagOut, TYPE_PARAM_NAME, inputType);
            writeOptionalAttribute(tagOut, NAME_PARAM_NAME, inputName);
            writeOptionalAttribute(tagOut, VALUE_PARAM_NAME, processFieldValue(env, inputName, methodName, inputType));
            tagOut.endTag();
        }

        // TODO: expose the form object name for nested tags...

        // TODO: save previous nestedPath value, build and expose current nestedPath value.

    }

    protected String getModelAttribute() {
        return getPath();
    }

    public String getAction() {
        return action;
    }

    public String getMethod() {
        return method;
    }

    public String getTarget() {
        return target;
    }

    public String getEnctype() {
        return enctype;
    }

    public String getAcceptCharset() {
        return acceptCharset;
    }

    public String getOnsubmit() {
        return onsubmit;
    }

    public String getOnreset() {
        return onreset;
    }

    public String getAutocomplete() {
        return autocomplete;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getServletRelativeAction() {
        return servletRelativeAction;
    }

    public String getMethodParam() {
        return methodParam;
    }

    protected boolean isMethodBrowserSupported(String method) {
        return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
    }

    protected String resolveAction(Environment env) throws TemplateException {
        RequestContext requestContext = getRequestContext(env, false);
        String action = getAction();
        String servletRelativeAction = getServletRelativeAction();

        if (StringUtils.hasText(action)) {
            action = getDisplayString(evaluate(ACTION_PARAM_NAME, action), false);
            return processAction(env, action);
        } else if (StringUtils.hasText(servletRelativeAction)) {
            String pathToServlet = requestContext.getPathToServlet();

            if (servletRelativeAction.startsWith("/") &&
                    !servletRelativeAction.startsWith(requestContext.getContextPath())) {
                servletRelativeAction = pathToServlet + servletRelativeAction;
            }

            servletRelativeAction = getDisplayString(evaluate(ACTION_PARAM_NAME, servletRelativeAction), false);
            return processAction(env, servletRelativeAction);
        } else {
            String requestUri = requestContext.getRequestUri();
            String encoding = getResponse().getCharacterEncoding();

            try {
                requestUri = UriUtils.encodePath(requestUri, encoding);
            } catch (UnsupportedEncodingException ex) {
                // shouldn't happen - if it does, proceed with requestUri as-is
            }

            HttpServletResponse response = getResponse();

            if (response != null) {
                requestUri = response.encodeURL(requestUri);
                String queryString = requestContext.getQueryString();

                if (StringUtils.hasText(queryString)) {
                    requestUri += "?" + HtmlUtils.htmlEscape(queryString);
                }
            }

            if (StringUtils.hasText(requestUri)) {
                return processAction(env, requestUri);
            } else {
                throw new IllegalArgumentException("Attribute 'action' is required. " +
                        "Attempted to resolve against current request URI but request URI was null.");
            }
        }
    }

    private String getHttpMethod() {
        final String methodName = getMethod();
        return (isMethodBrowserSupported(methodName) ? methodName : DEFAULT_METHOD);
    }

    private boolean isValidHttpMethod(String method) {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.name().equalsIgnoreCase(method)) {
                return true;
            }
        }

        return false;
    }

    private String processAction(Environment env, String action) throws TemplateException {
        RequestDataValueProcessor processor = getRequestContext(env, false).getRequestDataValueProcessor();
        HttpServletRequest request = getRequest();
        if (processor != null && request != null) {
            action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
        }
        return action;
    }

}

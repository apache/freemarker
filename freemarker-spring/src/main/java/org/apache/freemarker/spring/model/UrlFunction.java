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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateHashModelEx2.KeyValuePairIterator;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._KeyValuePair;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriUtils;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:url /&gt;</code> JSP Tag Library.
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * ${spring.url('/usereditaction.do')}
 * 
 * ${spring.url('/usereditaction.do', param1='value1', param2='value2')}
 * 
 * ${spring.url('/users/{userId}/edit.do', userId='123')}
 * 
 * ${spring.url('/usereditaction.do', context='/othercontext', param1='value1', param2='value2')}
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
    private static final int PARAMS_PARAM_IDX = 2;

    private static final String CONTEXT_PARAM_NAME = "context";

    /**
     * Absolute URL pattern. e.g, http(s)://example.com, mailto:john@example.com, tel:123-456-7890.
     */
    private static final Pattern ABS_URL_PATTERN = Pattern.compile("^((([A-Za-z]+?:)?\\/\\/)|[A-Za-z]+:)[\\w.-]+");

    private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

    private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(1, false,
            StringToIndexMap.of(CONTEXT_PARAM_NAME, CONTEXT_PARAM_IDX), true);

    public UrlFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        final String value = CallableUtils.getStringArgument(args, VALUE_PARAM_IDX, this);
        final String context = CallableUtils.getOptionalStringArgument(args, CONTEXT_PARAM_IDX, this);

        List<_KeyValuePair<String, String>> params = Collections.emptyList();
        final TemplateHashModelEx2 paramsHashModel = (TemplateHashModelEx2) args[PARAMS_PARAM_IDX];

        if (!paramsHashModel.isEmptyHash()) {
            params = new ArrayList<>();

            TemplateHashModelEx2.KeyValuePair pair;
            TemplateModel paramNameModel;
            TemplateModel paramValueModel;
            String paramName;
            String paramValue;

            for (KeyValuePairIterator pairIt = paramsHashModel.keyValuePairIterator(); pairIt.hasNext();) {
                pair = pairIt.next();
                paramNameModel = pair.getKey();
                paramValueModel = pair.getValue();

                if ((paramNameModel instanceof TemplateStringModel)
                        && (paramValueModel instanceof TemplateStringModel)) {
                    paramName = ((TemplateStringModel) paramNameModel).getAsString();
                    paramValue = ((TemplateStringModel) paramValueModel).getAsString();

                    if (paramName.isEmpty()) {
                        CallableUtils.newArgumentValueException(PARAMS_PARAM_IDX,
                                "Parameter name must be a non-blank string.", this);
                    }

                    params.add(new _KeyValuePair<String, String>(paramName, paramValue));
                } else {
                    CallableUtils.newArgumentValueException(PARAMS_PARAM_IDX,
                            "Parameter name and value must be string.", this);
                }
            }
        }

        final UrlType urlType = determineUrlType(value);

        StringBuilder urlBuilder = new StringBuilder();

        if (urlType == UrlType.CONTEXT_RELATIVE) {
            if (context == null) {
                urlBuilder.append(getRequest().getContextPath());
            } else if (context.endsWith("/")) {
                urlBuilder.append(context.substring(0, context.length() - 1));
            } else {
                urlBuilder.append(context);
            }
        }

        Set<String> templateParams = new HashSet<>();
        urlBuilder.append(replaceUriTemplateParams(value, params, templateParams));
        urlBuilder.append(createQueryString(params, templateParams, (urlBuilder.indexOf("?") == -1)));

        String urlString = urlBuilder.toString();

        if (urlType != UrlType.ABSOLUTE) {
            urlString = getResponse().encodeURL(urlString);
        }

        RequestDataValueProcessor processor = requestContext.getRequestDataValueProcessor();

        if ((processor != null) && (getRequest() instanceof HttpServletRequest)) {
            urlString = processor.processUrl(getRequest(), urlString);
        }

        return wrapObject(objectWrapperAndUnwrapper, urlString);
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    private UrlType determineUrlType(final String value) {
        Matcher m = ABS_URL_PATTERN.matcher(value);

        if (m.matches()) {
            return UrlType.ABSOLUTE;
        } else if (value.startsWith("/")) {
            return UrlType.CONTEXT_RELATIVE;
        } else {
            return UrlType.RELATIVE;
        }
    }

    private String replaceUriTemplateParams(String uri, List<_KeyValuePair<String, String>> params, Set<String> usedParams)
            throws TemplateException {
        final String encoding = getResponse().getCharacterEncoding();

        String paramName;
        String paramValue;

        for (_KeyValuePair<String, String> pair : params) {
            paramName = pair.getKey();
            paramValue = pair.getValue();

            String template = URL_TEMPLATE_DELIMITER_PREFIX + paramName + URL_TEMPLATE_DELIMITER_SUFFIX;

            if (uri.contains(template)) {
                usedParams.add(paramName);

                try {
                    uri = uri.replace(template, UriUtils.encodePath(paramValue, encoding));
                } catch (UnsupportedEncodingException e) {
                    CallableUtils.newGenericExecuteException("Cannot encode URI. " + e, this);
                }
            } else {
                template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + paramName + URL_TEMPLATE_DELIMITER_SUFFIX;

                if (uri.contains(template)) {
                    usedParams.add(paramName);

                    try {
                        uri = uri.replace(template, UriUtils.encodePathSegment(paramValue, encoding));
                    } catch (UnsupportedEncodingException e) {
                        CallableUtils.newGenericExecuteException("Cannot encode URI. " + e, this);
                    }
                }
            }
        }

        return uri;
    }

    private String createQueryString(List<_KeyValuePair<String, String>> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
            throws TemplateException {
        final String encoding = getResponse().getCharacterEncoding();
        final StringBuilder queryStringBuilder = new StringBuilder();

        String paramName;
        String paramValue;

        for (_KeyValuePair<String, String> pair : params) {
            paramName = pair.getKey();
            paramValue = pair.getValue();

            if (!usedParams.contains(paramName)) {
                queryStringBuilder
                        .append((includeQueryStringDelimiter && queryStringBuilder.length() == 0) ? "?" : "&");

                try {
                    queryStringBuilder.append(UriUtils.encodeQueryParam(paramName, encoding));

                    if (paramValue != null) {
                        queryStringBuilder.append('=');
                        queryStringBuilder.append(UriUtils.encodeQueryParam(paramValue, encoding));
                    }
                } catch (UnsupportedEncodingException e) {
                    CallableUtils.newGenericExecuteException("Cannot encode query parameter. " + e, this);
                }
            }
        }

        return queryStringBuilder.toString();
    }

    private enum UrlType {
        CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
    }
}

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
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.*;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * Abstract TemplateCallableModel for derived classes to support Spring MVC based templating environment.
 */
public abstract class AbstractSpringTemplateCallableModel implements TemplateCallableModel {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * Constructs with servlet request and response.
     * @param request servlet request
     * @param response servlet response
     */
    protected AbstractSpringTemplateCallableModel(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * Return servlet request.
     * @return servlet request
     */
    protected final HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Return servlet response.
     * @return servlet response
     */
    protected final HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Find {@link ObjectWrapperAndUnwrapper} from the environment.
     * @param env environment
     * @param calledAsFunction whether or not this is called from a {@link TemplateFunctionModel}.
     * @return {@link ObjectWrapperAndUnwrapper} from the environment
     * @throws TemplateException if the ObjectWrapper in the environment is not an ObjectWrapperAndUnwrapper
     */
    protected ObjectWrapperAndUnwrapper getObjectWrapperAndUnwrapper(Environment env, boolean calledAsFunction)
            throws TemplateException {
        final ObjectWrapper objectWrapper = env.getObjectWrapper();

        if (!(objectWrapper instanceof ObjectWrapperAndUnwrapper)) {
            throw CallableUtils.newGenericExecuteException(
                    "The ObjectWrapper of environment isn't an instance of ObjectWrapperAndUnwrapper.", this,
                    calledAsFunction);
        }

        return (ObjectWrapperAndUnwrapper) objectWrapper;
    }

    /**
     * Find Spring {@link RequestContext} from the environment.
     * @param env environment
     * @param calledAsFunction whether or not this is called from a {@link TemplateFunctionModel}.
     * @return Spring {@link RequestContext} from the environment
     * @throws TemplateException if Spring {@link RequestContext} from the environment is not found
     */
    protected RequestContext getRequestContext(final Environment env, boolean calledAsFunction)
            throws TemplateException {
        TemplateModel rcModel = env.getVariable(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);

        if (rcModel == null) {
            throw CallableUtils.newGenericExecuteException(
                    AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE + " is not found.", this, false);
        }

        RequestContext requestContext = (RequestContext) getObjectWrapperAndUnwrapper(env, calledAsFunction)
                .unwrap(rcModel);

        return requestContext;
    }

    /**
     * Find {@link BindStatus} with no {@code htmlEscape} option from {@link RequestContext} by the {@code path}
     * and wrap it as a {@link TemplateModel}.
     * <P>
     * <EM>NOTE:</EM> In FreeMarker, there is no need to depend on <code>BindStatus#htmlEscape</code> option
     * as FreeMarker template expressions can easily set escape option by themselves.
     * Therefore, this method always get a {@link BindStatus} with {@code htmlEscape} option set to {@code false}.
     * @param env Environment
     * @param objectWrapperAndUnwrapper ObjectWrapperAndUnwrapper
     * @param requestContext Spring RequestContext
     * @param path bind path
     * @param ignoreNestedPath flag whether or not to ignore the nested path
     * @return {@link TemplateModel} wrapping a {@link BindStatus} with no {@code htmlEscape} option from {@link RequestContext}
     * by the {@code path}
     * @throws TemplateException if template exception occurs
     */
    protected final TemplateModel getBindStatusTemplateModel(Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext, String path,
            boolean ignoreNestedPath) throws TemplateException {
        BindStatus status = getBindStatus(env, objectWrapperAndUnwrapper, requestContext, path, ignoreNestedPath);
        return (status != null) ? objectWrapperAndUnwrapper.wrap(status) : null;
    }

    /**
     * Find {@link BindStatus} with no {@code htmlEscape} option from {@link RequestContext} by the {@code path}.
     * <P>
     * <EM>NOTE:</EM> In FreeMarker, there is no need to depend on <code>BindStatus#htmlEscape</code> option
     * as FreeMarker template expressions can easily set escape option by themselves.
     * Therefore, this method always get a {@link BindStatus} with {@code htmlEscape} option set to {@code false}.
     * @param env Environment
     * @param objectWrapperAndUnwrapper ObjectWrapperAndUnwrapper
     * @param requestContext Spring RequestContext
     * @param path bind path
     * @param ignoreNestedPath flag whether or not to ignore the nested path
     * @return a {@link BindStatus} with no {@code htmlEscape} option from {@link RequestContext} by the {@code path}
     * @throws TemplateException if template exception occurs
     */
    protected final BindStatus getBindStatus(Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext, String path,
            boolean ignoreNestedPath) throws TemplateException {
        final String resolvedPath = (ignoreNestedPath) ? path : resolveNestedPath(env, objectWrapperAndUnwrapper, path);
        BindStatus status = requestContext.getBindStatus(resolvedPath, false);
        return status;
    }

    /**
     * Return the internal TemplateHashModel wrapper for templating in Spring Framework applications.
     * @param env environment
     * @return the internal TemplateHashModel wrapper for templating in Spring Framework applications
     * @throws TemplateException if template exception occurs
     */
    protected SpringTemplateCallableHashModel getSpringTemplateCallableHashModel(final Environment env)
            throws TemplateException {
        final AbstractDelegatingTemplateHashModel delegate = (AbstractDelegatingTemplateHashModel) env
                .getVariable(SpringTemplateCallableHashModel.NAME);
        return (SpringTemplateCallableHashModel) delegate.getDelegatedTemplateHashModel();
    }

    private String resolveNestedPath(final Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper,
            final String path) throws TemplateException {
        final TemplateStringModel curNestedPathModel = getSpringTemplateCallableHashModel(env).getNestedPathModel();
        final String curNestedPath = (curNestedPathModel != null) ? curNestedPathModel.getAsString() : null;

        if (curNestedPath != null && !path.startsWith(curNestedPath)
                && !path.equals(curNestedPath.substring(0, curNestedPath.length() - 1))) {
            return curNestedPath + path;
        }

        return path;
    }
}

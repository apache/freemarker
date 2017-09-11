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

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Abstract TemplateCallableModel for derived classes to support Spring MVC based templating environment.
 */
public abstract class AbstractSpringTemplateCallableModel implements TemplateCallableModel {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public AbstractSpringTemplateCallableModel(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    protected final HttpServletRequest getRequest() {
        return request;
    }

    protected final HttpServletResponse getResponse() {
        return response;
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
     * @throws TemplateException 
     */
    protected final TemplateModel getBindStatusTemplateModel(Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper,
            RequestContext requestContext, String path, boolean ignoreNestedPath) throws TemplateException {
        final String resolvedPath = (ignoreNestedPath) ? path : resolveNestedPath(env, objectWrapperAndUnwrapper, path);
        BindStatus status = requestContext.getBindStatus(resolvedPath, false);
        return wrapObject(objectWrapperAndUnwrapper, status);
    }

    protected final Object unwrapObject(ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, TemplateModel model) throws TemplateException {
        return (model != null) ? objectWrapperAndUnwrapper.unwrap(model) : null;
    }

    protected final TemplateModel wrapObject(ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, Object object) throws TemplateException {
        if (object != null) {
            if (!(objectWrapperAndUnwrapper instanceof DefaultObjectWrapper)) {
                CallableUtils.newGenericExecuteException("objectWrapperAndUnwrapper is not a DefaultObjectWrapper.",
                        this, isFunction());
            }

            return ((DefaultObjectWrapper) objectWrapperAndUnwrapper).wrap(object);
        }

        return null;
    }

    protected abstract boolean isFunction();

    protected SpringTemplateCallableHashModel getSpringTemplateCallableHashModel(final Environment env)
            throws TemplateException {
        return (SpringTemplateCallableHashModel) env.getVariable(SpringTemplateCallableHashModel.NAME);
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

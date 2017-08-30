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
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.ObjectWrappingException;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;

public abstract class AbstractSpringTemplateDirectiveModel implements TemplateDirectiveModel {

    // TODO: namespace this into 'spring.nestedPath'??
    /**
     * @see <code>org.springframework.web.servlet.tags.NestedPathTag#NESTED_PATH_VARIABLE_NAME</code>
     */
    private static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public AbstractSpringTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public final void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        final ObjectWrapper objectWrapper = env.getObjectWrapper();

        if (!(objectWrapper instanceof ObjectWrapperAndUnwrapper)) {
            throw new TemplateException(
                    "The ObjectWrapper of environment wasn't instance of ObjectWrapperAndUnwrapper.");
        }

        TemplateModel rcModel = env.getVariable(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);

        if (rcModel == null) {
            throw new TemplateException(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE + " not found.");
        }

        RequestContext requestContext = (RequestContext) ((ObjectWrapperAndUnwrapper) objectWrapper).unwrap(rcModel);

        executeInternal(args, callPlace, out, env, (ObjectWrapperAndUnwrapper) objectWrapper, requestContext);
    }

    protected abstract void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException, IOException;

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
     * @throws ObjectWrappingException if fails to wrap the <code>BindStatus</code> object
     */
    protected final TemplateModel getBindStatusTemplateModel(Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper,
            RequestContext requestContext, String path, boolean ignoreNestedPath) throws ObjectWrappingException {
        final String resolvedPath = (ignoreNestedPath) ? path : resolveNestedPath(env, objectWrapperAndUnwrapper, path);
        BindStatus status = requestContext.getBindStatus(resolvedPath, false);

        if (status != null) {
            if (!(objectWrapperAndUnwrapper instanceof DefaultObjectWrapper)) {
                throw new IllegalArgumentException("objectWrapperAndUnwrapper is not a DefaultObjectWrapper.");
            }

            return ((DefaultObjectWrapper) objectWrapperAndUnwrapper).wrap(status);
        }

        return null;
    }

    private String resolveNestedPath(final Environment env, ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper,
            final String path) {
        // TODO: should read it from request or env??
        //       or read spring.nestedPath first and read request attribute next??
        String nestedPath = (String) request.getAttribute(NESTED_PATH_VARIABLE_NAME);

        if (nestedPath != null && !path.startsWith(nestedPath)
                && !path.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
            return nestedPath + path;
        }

        return path;
    }
}

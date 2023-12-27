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
import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.Writer;

/**
 * Abstract TemplateDirectiveModel for derived classes to support Spring MVC based templating environment.
 */
public abstract class AbstractSpringTemplateDirectiveModel extends AbstractSpringTemplateCallableModel
        implements TemplateDirectiveModel {

    /**
     * Construct directive with servlet request and response.
     * @param request servlet request
     * @param response servlet response
     */
    protected AbstractSpringTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * Execute this directive.
     * <P>
     * This method establishes Spring's <code>RequestContext</code> and invokes {@link #executeInternal(TemplateModel[], CallPlace, Writer, Environment, ObjectWrapperAndUnwrapper, RequestContext)}
     * which must be implemented by derived directive classes.
     * </P>
     */
    @Override
    public final void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        final ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper = getObjectWrapperAndUnwrapper(env, false);
        final RequestContext requestContext = getRequestContext(env, false);
        executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);
    }

    /**
     * Interal execution method that is supposed to be implemented by derived directive classes.
     * @param args argument models
     * @param callPlace the place where this is being called
     * @param out output writer
     * @param env template execution environment
     * @param objectWrapperAndUnwrapper ObjectWrapperAndUnwrapper
     * @param requestContext Spring RequestContext
     * @throws TemplateException if template exception occurs
     * @throws IOException if IO exception occurs
     */
    protected abstract void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException, IOException;

}

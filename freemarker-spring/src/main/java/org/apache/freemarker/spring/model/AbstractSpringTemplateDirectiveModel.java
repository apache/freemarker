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
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;

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
    public AbstractSpringTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
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
        final ObjectWrapper objectWrapper = env.getObjectWrapper();

        if (!(objectWrapper instanceof ObjectWrapperAndUnwrapper)) {
            CallableUtils.newGenericExecuteException(
                    "The ObjectWrapper of environment isn't an instance of ObjectWrapperAndUnwrapper.", this, false);
        }

        TemplateModel rcModel = env.getVariable(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);

        if (rcModel == null) {
            CallableUtils.newGenericExecuteException(
                    AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE + " not found.", this, false);
        }

        RequestContext requestContext = (RequestContext) ((ObjectWrapperAndUnwrapper) objectWrapper).unwrap(rcModel);

        executeInternal(args, callPlace, out, env, (ObjectWrapperAndUnwrapper) objectWrapper, requestContext);
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

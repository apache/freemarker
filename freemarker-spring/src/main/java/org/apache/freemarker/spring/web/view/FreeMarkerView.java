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
package org.apache.freemarker.spring.web.view;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.servlet.*;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.apache.freemarker.spring.model.AbstractDelegatingTemplateHashModel;
import org.apache.freemarker.spring.model.SpringTemplateCallableHashModel;
import org.apache.freemarker.spring.model.form.SpringFormTemplateCallableHashModel;

import java.util.Map;

/**
 * FreeMarker template based view implementation, with being able to provide a {@link ServletContextHashModel}
 * and {@link TaglibFactory} models to the templates.
 */
public class FreeMarkerView extends AbstractFreeMarkerView {

    /**
     * Internal servlet instance to provide a page object in JSP tag library usages.
     * @see jakarta.servlet.jsp.PageContext#getPage()
     */
    private GenericServlet pageContextServlet;

    /**
     * {@link ServletContextHashModel} instance for templates to access servlet context attributes.
     */
    private ServletContextHashModel servletContextModel;

    /**
     * {@link TaglibFactory} instance for templates to be able to use JSP tag libraries.
     */
    private TaglibFactory taglibFactory;

    /**
     * Get {@link GenericServlet} instance which is a page object in JSP tag library usages.
     * @return {@link GenericServlet} instance which is a page object in JSP tag library usages
     */
    public GenericServlet getPageContextServlet() {
        return pageContextServlet;
    }

    /**
     * Set {@link GenericServlet} instance which is a page object in JSP tag library usages.
     * @param pageContextServlet {@link GenericServlet} instance which is a page object in JSP tag library
     * usages
     */
    public void setPageContextServlet(GenericServlet pageContextServlet) {
        this.pageContextServlet = pageContextServlet;
    }

    /**
     * Get {@link ServletContextHashModel} instance by which templates can access servlet context attributes.
     * @return {@link ServletContextHashModel} instance by which templates can access servlet context attributes
     */
    public ServletContextHashModel getServletContextModel() {
        return servletContextModel;
    }

    /**
     * Set {@link ServletContextHashModel} instance by which templates can access servlet context attributes.
     * @param servletContextModel {@link ServletContextHashModel} instance by which templates can access servlet
     * context attributes
     */
    public void setServletContextModel(ServletContextHashModel servletContextModel) {
        this.servletContextModel = servletContextModel;
    }

    /**
     * Get {@link TaglibFactory} instance by which templates can use JSP tag libraries.
     * @return {@link TaglibFactory} instance by which templates can use JSP tag libraries.
     */
    public TaglibFactory getTaglibFactory() {
        return taglibFactory;
    }

    /**
     * Set {@link TaglibFactory} instance by which templates can use JSP tag libraries.
     * @param taglibFactory {@link TaglibFactory} instance by which templates can use JSP tag libraries.
     */
    public void setTaglibFactory(TaglibFactory taglibFactory) {
        this.taglibFactory = taglibFactory;
    }

    @Override
    protected TemplateHashModel createModel(Map<String, Object> map, ObjectWrapperAndUnwrapper objectWrapper,
            final HttpServletRequest request, final HttpServletResponse response) {

        AllHttpScopesHashModel model = new AllHttpScopesHashModel(objectWrapper, getServletContext(), request);

        model.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, getServletContextModel());

        model.putUnlistedModel(FreemarkerServlet.KEY_SESSION,
                getHttpSessionModel(objectWrapper, request, response));

        HttpRequestHashModel requestModel = (HttpRequestHashModel) request
                .getAttribute(FreemarkerServlet.ATTR_REQUEST_MODEL);
        HttpRequestParametersHashModel requestParametersModel = (HttpRequestParametersHashModel) request
                .getAttribute(FreemarkerServlet.ATTR_REQUEST_PARAMETERS_MODEL);

        if (requestModel == null || requestModel.getRequest() != request) {
            requestModel = new HttpRequestHashModel(request, response, objectWrapper);
            request.setAttribute(FreemarkerServlet.ATTR_REQUEST_MODEL, requestModel);
            requestParametersModel = new HttpRequestParametersHashModel(request, objectWrapper);
        }

        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, requestModel);
        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, requestParametersModel);

        model.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, getTaglibFactory());

        model.putUnlistedModel(FreemarkerServlet.KEY_INCLUDE, new IncludePage(request, response));

        model.putUnlistedModel(SpringTemplateCallableHashModel.NAME, new AbstractDelegatingTemplateHashModel() {
            @Override
            public TemplateHashModel createDelegatedTemplateHashModel() throws TemplateException {
                return new SpringTemplateCallableHashModel(request, response);
            }
        });

        model.putUnlistedModel(SpringFormTemplateCallableHashModel.NAME, new AbstractDelegatingTemplateHashModel() {
            @Override
            public TemplateHashModel createDelegatedTemplateHashModel() throws TemplateException {
                return new SpringFormTemplateCallableHashModel(request, response);
            }
        });

        model.putAll(map);

        return model;
    }

    /**
     * Get {@link HttpSessionHashModel} instance by which templates can access session attributes.
     * @param objectWrapperForModel ObjectWrapper to be used in model building
     * @param request request
     * @param response response
     * @return {@link HttpSessionHashModel} instance by which templates can access session attributes
     */
    protected HttpSessionHashModel getHttpSessionModel(ObjectWrapperAndUnwrapper objectWrapperForModel,
            HttpServletRequest request, HttpServletResponse response) {
        HttpSessionHashModel sessionModel;
        HttpSession session = request.getSession(false);

        if (session != null) {
            sessionModel = (HttpSessionHashModel) session.getAttribute(FreemarkerServlet.ATTR_SESSION_MODEL);

            if (sessionModel == null || sessionModel.isOrphaned(session)) {
                sessionModel = new HttpSessionHashModel(session, objectWrapperForModel);
                session.setAttribute(FreemarkerServlet.ATTR_SESSION_MODEL, sessionModel);
            }
        } else {
            sessionModel = new HttpSessionHashModel(getPageContextServlet(), request, response, objectWrapperForModel);
        }

        return sessionModel;
    }

}

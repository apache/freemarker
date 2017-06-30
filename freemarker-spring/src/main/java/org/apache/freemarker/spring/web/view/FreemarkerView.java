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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.servlet.AllHttpScopesHashModel;
import org.apache.freemarker.servlet.FreemarkerServlet;
import org.apache.freemarker.servlet.HttpRequestHashModel;
import org.apache.freemarker.servlet.HttpRequestParametersHashModel;
import org.apache.freemarker.servlet.HttpSessionHashModel;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.apache.freemarker.servlet.jsp.TaglibFactoryBuilder;

public class FreemarkerView extends AbstractFreemarkerView {

    private volatile PageContextServlet pageContextServlet;

    private volatile ServletContextHashModel servletContextModel;

    private volatile TaglibFactory taglibFactory;

    public PageContextServlet getPageContextServlet() {
        PageContextServlet servlet = pageContextServlet;

        if (servlet == null) {
            synchronized (this) {
                servlet = pageContextServlet;

                if (servlet == null) {
                    servlet = new PageContextServlet();

                    try {
                        servlet.init(new PageContextServletConfig(getServletContext(), getBeanName()));
                    } catch (ServletException e) {
                        // never happens...
                    }

                    pageContextServlet = servlet;
                }
            }
        }

        return servlet;
    }

    public void setPageContextServlet(PageContextServlet pageContextServlet) {
        this.pageContextServlet = pageContextServlet;
    }

    public ServletContextHashModel getServletContextModel() {
        ServletContextHashModel contextModel = servletContextModel;

        if (contextModel == null) {
            synchronized (this) {
                contextModel = servletContextModel;

                if (contextModel == null) {
                    contextModel = new ServletContextHashModel(getPageContextServlet(), getObjectWrapperForModel());
                    servletContextModel = contextModel;
                }
            }
        }

        return contextModel;
    }

    public void setServletContextModel(ServletContextHashModel servletContextModel) {
        this.servletContextModel = servletContextModel;
    }

    public TaglibFactory getTaglibFactory() {
        TaglibFactory tlFactory = taglibFactory;

        if (tlFactory == null) {
            synchronized (this) {
                tlFactory = taglibFactory;

                if (tlFactory == null) {
                    tlFactory = new TaglibFactoryBuilder(getServletContext(), getObjectWrapperForModel())
                            .build();

                    taglibFactory = tlFactory;
                }
            }
        }

        return tlFactory;
    }

    public void setTaglibFactory(TaglibFactory taglibFactory) {
        this.taglibFactory = taglibFactory;
    }

    @Override
    protected TemplateHashModel createModel(Map<String, Object> map, ObjectWrapperAndUnwrapper objectWrapperForModel,
            HttpServletRequest request, HttpServletResponse response) {

        AllHttpScopesHashModel model = new AllHttpScopesHashModel(objectWrapperForModel, getServletContext(), request);

        model.putUnlistedModel(FreemarkerServlet.KEY_APPLICATION, getServletContextModel());

        model.putUnlistedModel(FreemarkerServlet.KEY_SESSION,
                getHttpSessionModel(objectWrapperForModel, request, response));

        HttpRequestHashModel requestModel = (HttpRequestHashModel) request
                .getAttribute(FreemarkerServlet.ATTR_REQUEST_MODEL);
        HttpRequestParametersHashModel requestParametersModel = (HttpRequestParametersHashModel) request
                .getAttribute(FreemarkerServlet.ATTR_REQUEST_PARAMETERS_MODEL);

        if (requestModel == null || requestModel.getRequest() != request) {
            requestModel = new HttpRequestHashModel(request, response, objectWrapperForModel);
            request.setAttribute(FreemarkerServlet.ATTR_REQUEST_MODEL, requestModel);
            requestParametersModel = new HttpRequestParametersHashModel(request, objectWrapperForModel);
        }

        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, requestModel);
        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS, requestParametersModel);

        model.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, getTaglibFactory());

        model.putAll(map);

        return model;
    }

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

    /**
     * Extending {@link GenericServlet} for {@link PageContext#getPage()} in JSP Tag Library support.
     */
    @SuppressWarnings("serial")
    private static class PageContextServlet extends GenericServlet {

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            // Do nothing
        }

    }

    /**
     * {@link ServletConfig} for {@link PageContextServlet}.
     */
    private class PageContextServletConfig implements ServletConfig {

        private ServletContext servletContext;
        private String servletName;

        public PageContextServletConfig(ServletContext servletContext, String servletName) {
            this.servletContext = servletContext;
            this.servletName = servletName;
        }

        @Override
        public String getServletName() {
            return servletName;
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(Collections.<String> emptySet());
        }
    }

}

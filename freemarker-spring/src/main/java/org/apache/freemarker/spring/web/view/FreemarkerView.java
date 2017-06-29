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

import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.servlet.AllHttpScopesHashModel;
import org.apache.freemarker.servlet.FreemarkerServlet;
import org.apache.freemarker.servlet.HttpRequestHashModel;
import org.apache.freemarker.servlet.HttpRequestParametersHashModel;
import org.apache.freemarker.servlet.HttpSessionHashModel;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.apache.freemarker.servlet.jsp.TaglibFactory;

public class FreemarkerView extends AbstractFreemarkerView {

    private PageContextServletConfig pageContextServletConfig;

    private PageContextServlet pageContextServlet;

    private ServletContextHashModel servletContextModel;

    private TaglibFactory taglibFactory;

    public PageContextServlet getPageContextServlet() {
        // TODO: proper locking...
        if (pageContextServlet == null) {
            pageContextServlet = new PageContextServlet();
            pageContextServletConfig = new PageContextServletConfig(getServletContext(), getBeanName());

            try {
                pageContextServlet.init(pageContextServletConfig);
            } catch (ServletException e) {
                // never happen
            }
        }

        return pageContextServlet;
    }

    public void setPageContextServlet(PageContextServlet pageContextServlet) {
        this.pageContextServlet = pageContextServlet;
    }

    public ServletContextHashModel getServletContextModel() {
        // TODO: proper locking...
        if (servletContextModel == null) {
            servletContextModel = new ServletContextHashModel(getPageContextServlet(), getObjectWrapperForModel());
        }

        return servletContextModel;
    }

    public void setServletContextModel(ServletContextHashModel servletContextModel) {
        this.servletContextModel = servletContextModel;
    }

    public TaglibFactory getTaglibFactory() {
        // TODO
        return taglibFactory;
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
        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST,
                new HttpRequestHashModel(request, response, objectWrapperForModel));
        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST_PARAMETERS,
                new HttpRequestParametersHashModel(request, objectWrapperForModel));
        model.putUnlistedModel(FreemarkerServlet.KEY_JSP_TAGLIBS, getTaglibFactory());
        model.putAll(map);
        return model;
    }

    protected HttpSessionHashModel getHttpSessionModel(ObjectWrapperAndUnwrapper objectWrapperForModel,
            HttpServletRequest request, HttpServletResponse response) {
        // TODO
        HttpSessionHashModel sessionModel = new HttpSessionHashModel(null, request, response, objectWrapperForModel);
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

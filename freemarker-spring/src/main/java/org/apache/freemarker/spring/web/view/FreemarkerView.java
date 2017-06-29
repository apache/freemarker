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

import java.util.Map;

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

    private ServletContextHashModel servletContextModel;

    private TaglibFactory taglibFactory;

    public ServletContextHashModel getServletContextModel() {
        // TODO
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
        model.putUnlistedModel(FreemarkerServlet.KEY_SESSION, getHttpSessionModel(objectWrapperForModel, request, response));
        model.putUnlistedModel(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, response, objectWrapperForModel));
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
}

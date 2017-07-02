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

import javax.servlet.ServletException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.apache.freemarker.servlet.jsp.TaglibFactoryBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

public class FreemarkerViewResolver extends AbstractTemplateViewResolver implements InitializingBean {

    private Configuration configuration;

    private ObjectWrapperAndUnwrapper objectWrapper;
    private PageContextServlet pageContextServlet;
    private ServletContextHashModel servletContextModel;
    private TaglibFactory taglibFactory;

    public FreemarkerViewResolver() {
        setViewClass(requiredViewClass());
    }

    public FreemarkerViewResolver(String prefix, String suffix) {
        this();
        setPrefix(prefix);
        setSuffix(suffix);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (configuration == null) {
            throw new IllegalStateException("Configuration is not set.");
        }

        if (objectWrapper == null) {
            if (configuration.isObjectWrapperSet()) {
                if (!(configuration.getObjectWrapper() instanceof ObjectWrapperAndUnwrapper)) {
                    throw new RuntimeException(
                            FreemarkerViewResolver.class.getSimpleName() + " requires an ObjectWrapper that "
                                    + "implements " + ObjectWrapperAndUnwrapper.class.getName()
                                    + ", but the Configuration's ObjectWrapper doesn't do that: "
                                    + configuration.getObjectWrapper().getClass().getName());
                }

                objectWrapper = (ObjectWrapperAndUnwrapper) configuration.getObjectWrapper();
            } else {
                objectWrapper = new DefaultObjectWrapper.Builder(configuration.getIncompatibleImprovements()).build();
            }
        }

        pageContextServlet = new PageContextServlet();

        try {
            pageContextServlet
                    .init(new PageContextServletConfig(getServletContext(), FreemarkerViewResolver.class.getName()));
        } catch (ServletException e) {
            // never happens...
        }

        servletContextModel = new ServletContextHashModel(pageContextServlet, objectWrapper);

        taglibFactory = new TaglibFactoryBuilder(getServletContext(), objectWrapper).build();
    }

    @Override
    protected Class<?> requiredViewClass() {
        return FreemarkerView.class;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        FreemarkerView view = (FreemarkerView) super.buildView(viewName);
        view.setConfiguration(configuration);
        view.setObjectWrapper(objectWrapper);
        view.setPageContextServlet(pageContextServlet);
        view.setServletContextModel(servletContextModel);
        view.setTaglibFactory(taglibFactory);
        return view;
    }

    protected ObjectWrapperAndUnwrapper getObjectWrapper() {
        return objectWrapper;
    }

    protected PageContextServlet getPageContextServlet() {
        return pageContextServlet;
    }

    protected ServletContextHashModel getServletContextModel() {
        return servletContextModel;
    }

    protected TaglibFactory getTaglibFactory() {
        return taglibFactory;
    }

}

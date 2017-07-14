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

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * FreeMarker template view resolver implementation, extending {@link AbstractTemplateViewResolver} that extends
 * {@link UrlBasedViewResolver}.
 * <p>
 * The FreeMarker {@link Configuration} property must be set at least. Otherwise this throws {@link IllegalStateException}
 * during initialization. In the bean initialization phase, this retrieves {@link ObjectWrapperAndUnwrapper} from
 * the {@link Configuration} and instantiate the internal page object ({@link PageContextServlet}) for JSP tag
 * library usages, {@link ServletContextHashModel} property for servlet context attribute accesses and {@link TaglibFactory}
 * property for JSP tag library usages.
 * </p>
 */
public class FreeMarkerViewResolver extends AbstractTemplateViewResolver implements InitializingBean {

    /**
     * FreeMarker {@link Configuration} instance.
     */
    private Configuration configuration;

    /**
     * {@link ObjectWrapperAndUnwrapper} instance to be used in model building.
     */
    private ObjectWrapperAndUnwrapper objectWrapper;

    /**
     * Internal servlet instance to provide a page object in JSP tag library usages.
     * @see {@link javax.servlet.jsp.PageContext#getPage()}
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
     * Constructs view resolver.
     */
    public FreeMarkerViewResolver() {
        super();
        setViewClass(FreeMarkerView.class);
    }

    /**
     * Get FreeMarker {@link Configuration} instance.
     * @return FreeMarker {@link Configuration} instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set FreeMarker {@link Configuration} instance.
     * @param configuration FreeMarker {@link Configuration} instance
     */
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
                            FreeMarkerViewResolver.class.getSimpleName() + " requires an ObjectWrapper that "
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
                    .init(new PageContextServletConfig(getServletContext(), PageContextServlet.class.getSimpleName()));
        } catch (ServletException e) {
            // never happens...
        }

        servletContextModel = new ServletContextHashModel(pageContextServlet, objectWrapper);

        taglibFactory = new TaglibFactory.Builder(getServletContext(), objectWrapper).build();
    }

    @Override
    protected Class<?> requiredViewClass() {
        return FreeMarkerView.class;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        FreeMarkerView view = (FreeMarkerView) super.buildView(viewName);
        view.setConfiguration(configuration);
        view.setObjectWrapper(objectWrapper);
        view.setPageContextServlet(pageContextServlet);
        view.setServletContextModel(servletContextModel);
        view.setTaglibFactory(taglibFactory);
        return view;
    }

}

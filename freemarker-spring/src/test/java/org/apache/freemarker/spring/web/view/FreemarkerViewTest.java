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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.servlet.ServletContextHashModel;
import org.apache.freemarker.servlet.jsp.TaglibFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

public class FreemarkerViewTest {

    private ServletContext servletContext;
    private GenericWebApplicationContext applicationContext;

    private StringTemplateLoader templateLoader;
    private Configuration configuration;
    private ObjectWrapperAndUnwrapper objectWrapper;

    private GenericServlet pageContextServlet;
    private TaglibFactory taglibFactory;

    private FreemarkerViewResolver viewResolver;

    private AtomicLong visitorCount;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();

        applicationContext = new GenericWebApplicationContext(servletContext);
        applicationContext.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        templateLoader = new StringTemplateLoader();
        configuration = new Configuration.Builder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                .templateLoader(templateLoader).build();
        objectWrapper = (ObjectWrapperAndUnwrapper) configuration.getObjectWrapper();

        pageContextServlet = new PageContextServlet();
        pageContextServlet.init(new PageContextServletConfig(servletContext, PageContextServlet.class.getSimpleName()));
        taglibFactory = new TaglibFactory.Builder(servletContext, objectWrapper).build();

        viewResolver = new FreemarkerViewResolver();
        viewResolver.setServletContext(servletContext);
        viewResolver.setApplicationContext(applicationContext);
        viewResolver.setConfiguration(configuration);
        viewResolver.afterPropertiesSet();

        visitorCount = new AtomicLong();
        servletContext.setAttribute("visitorCount", visitorCount);
    }

    @Test
    public void testViewWithBasicModel() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        templateLoader.putTemplate("hello.ftl", "Hello, ${name!\"World\"}! Visit count: ${visitCount!0}");

        FreemarkerView view = createFreemarkerView("hello.ftl");

        int visitCount = 0;
        Map<String, Object> model = new HashMap<String, Object>();

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(model, request, response);
        assertEquals("Hello, World! Visit count: 0", response.getContentAsString());

        response = new MockHttpServletResponse();
        model.put("name", "Dan");
        model.put("visitCount", ++visitCount);
        view.render(model, request, response);
        assertEquals("Hello, Dan! Visit count: 1", response.getContentAsString());
    }

    @Test
    public void testViewWithDefaultServletModel() throws Exception {
        MockHttpSession session = new MockHttpSession(servletContext);
        session.setAttribute("itemCountInCart", 3);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");
        request.setPathInfo(";mypathinfo");
        request.addParameter("token1", "value1");
        request.setSession(session);
        request.setAttribute("promotion", "Fresh blue berries");

        templateLoader.putTemplate("default-model.ftl",
                "${name!}, you have ${Session.itemCountInCart!0} items in cart. " + "Hot deal: ${Request.promotion}. "
                        + "BTW, you're ${Application.visitorCount}th visitor. "
                        + "(token1: ${RequestParameters['token1']!})");

        FreemarkerView view = createFreemarkerView("default-model.ftl");

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("name", "Dan");

        final long count = visitorCount.incrementAndGet();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(model, request, response);
        assertEquals("Dan, you have 3 items in cart. Hot deal: Fresh blue berries. BTW, you're " + count
                + "th visitor. (token1: value1)", response.getContentAsString());
    }

    @Test
    public void testViewWithTaglibs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        templateLoader.putTemplate("taglibs.ftl",
                "<#assign e=JspTaglibs ['http://freemarker.org/jsp/example/echo'] >"
                + "<#assign msg=\"Hello!\" />"
                + "<@e.echo message=msg />");

        FreemarkerView view = createFreemarkerView("taglibs.ftl");

        Map<String, Object> model = new HashMap<String, Object>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(model, request, response);
        assertEquals("Hello!", response.getContentAsString());
    }

    private FreemarkerView createFreemarkerView(final String name) throws Exception {
        FreemarkerView view = new FreemarkerView();

        view.setServletContext(servletContext);
        view.setApplicationContext(applicationContext);
        view.setConfiguration(configuration);
        view.setObjectWrapper(objectWrapper);

        view.setPageContextServlet(pageContextServlet);
        view.setServletContextModel(new ServletContextHashModel(pageContextServlet, objectWrapper));
        view.setTaglibFactory(taglibFactory);

        view.setUrl(name);

        return view;
    }
}

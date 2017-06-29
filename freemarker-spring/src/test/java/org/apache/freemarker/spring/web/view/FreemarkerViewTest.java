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

import javax.servlet.ServletContext;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

public class FreemarkerViewTest {

    private ServletContext servletContext;

    private StringTemplateLoader templateLoader;

    private Configuration configuration;

    private AtomicLong visitorCount;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();
        visitorCount = new AtomicLong();
        servletContext.setAttribute("visitorCount", visitorCount);
        templateLoader = new StringTemplateLoader();
        configuration = new Configuration.Builder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                .templateLoader(templateLoader).build();
    }

    @Test
    public void testViewWithBasicModel() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        templateLoader.putTemplate("hello.ftl", "Hello, ${name!\"World\"}! Visit count: ${visitCount!0}");

        FreemarkerView view = new FreemarkerView();
        view.setServletContext(servletContext);
        view.setConfiguration(configuration);
        view.setName("hello.ftl");

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
        request.setAttribute("visitorCount", visitorCount);

        // TODO: Add 'Application.attributeName' example, too.
        templateLoader.putTemplate("default-model.ftl",
                "${name!}, you have ${Session.itemCountInCart!0} items in cart. "
                        + "BTW, you're ${Request.visitorCount}th visitor. "
                        + "(token1: ${RequestParameters['token1']!})");

        FreemarkerView view = new FreemarkerView();
        view.setServletContext(servletContext);
        view.setConfiguration(configuration);
        view.setName("default-model.ftl");

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("name", "Dan");

        final long count = visitorCount.incrementAndGet();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(model, request, response);
        assertEquals("Dan, you have 3 items in cart. BTW, you're " + count + "th visitor. (token1: value1)",
                response.getContentAsString());
    }

    @Test
    public void testViewWithTaglibs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        // TODO: 
        templateLoader.putTemplate("taglibs.ftl", "");

        FreemarkerView view = new FreemarkerView();
        view.setServletContext(servletContext);
        view.setConfiguration(configuration);
        view.setName("taglibs.ftl");

        Map<String, Object> model = new HashMap<String, Object>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(model, request, response);
    }
}

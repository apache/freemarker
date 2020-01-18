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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

public class FreeMarkerViewResolverTest {

    private ServletContext servletContext;
    private GenericWebApplicationContext applicationContext;

    private StringTemplateLoader templateLoader;
    private Configuration configuration;

    private FreeMarkerViewResolver viewResolver;

    private String prefix = "/WEB-INF/freemarker/";
    private String normalizedPrefix = "WEB-INF/freemarker/";
    private String suffix = ".f3ah";

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();

        applicationContext = new GenericWebApplicationContext(servletContext);
        applicationContext.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

        templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate(normalizedPrefix + "hello" + suffix, "Hello, World!");

        configuration = new Configuration.Builder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                .templateLoader(templateLoader).build();

        viewResolver = new FreeMarkerViewResolver();
        viewResolver.setServletContext(servletContext);
        viewResolver.setApplicationContext(applicationContext);
        viewResolver.setConfiguration(configuration);
        viewResolver.setPrefix(prefix);
        viewResolver.setSuffix(suffix);
        viewResolver.afterPropertiesSet();
    }

    @Test
    public void testViewResolver() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FreeMarkerView view = resolveFreemarkerView("hello", null);//Locale.ENGLISH);
        Map<String, Object> model = new HashMap<>();
        view.render(model, request, response);

        assertEquals("Hello, World!", response.getContentAsString());
    }

    private FreeMarkerView resolveFreemarkerView(final String name, final Locale locale) throws Exception {
        FreeMarkerView view = (FreeMarkerView) viewResolver.resolveViewName(name, locale);
        view.setServletContext(servletContext);
        view.setApplicationContext(applicationContext);
        return view;
    }

}

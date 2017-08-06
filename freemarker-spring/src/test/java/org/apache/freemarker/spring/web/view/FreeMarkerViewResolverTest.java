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
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.spring.SpringResourceTemplateLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.MessageSourceSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

public class FreeMarkerViewResolverTest {

    private static final String TEMPLATE_BASE_PATH = "classpath:META-INF/templates/";

    private ServletContext servletContext;
    private GenericWebApplicationContext appContext;

    private SpringResourceTemplateLoader templateLoader;
    private Configuration configuration;

    private FreeMarkerViewResolver viewResolver;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();

        appContext = new GenericWebApplicationContext(servletContext);

        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("message.greeting", "Hello, FM3!");

        BeanDefinition messageSourceBeanDef = BeanDefinitionBuilder
                .genericBeanDefinition(SimpleStaticMessageSource.class.getName()).addConstructorArgValue(messageMap)
                .getBeanDefinition();

        appContext.registerBeanDefinition("messageSource", messageSourceBeanDef);
        appContext.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appContext);

        templateLoader = new SpringResourceTemplateLoader();
        templateLoader.setBaseLocation(TEMPLATE_BASE_PATH);
        templateLoader.setResourceLoader(appContext);

        configuration = new Configuration.Builder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                .templateLoader(templateLoader).build();

        viewResolver = new FreeMarkerViewResolver();
        viewResolver.setServletContext(servletContext);
        viewResolver.setApplicationContext(appContext);
        viewResolver.setConfiguration(configuration);
        viewResolver.setPrefix("/");
        viewResolver.setSuffix(".ftl");
        viewResolver.afterPropertiesSet();
    }

    @Test
    public void testViewResolver() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mytest.do");
        request.setContextPath("/mycontext");
        request.setServletPath("/myservlet");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FreeMarkerView view = resolveFreemarkerView("hello", null);//Locale.ENGLISH);
        Map<String, Object> model = new HashMap<String, Object>();
        view.render(model, request, response);

        assertEquals("Hello, FM3!", response.getContentAsString().trim());
    }

    private FreeMarkerView resolveFreemarkerView(final String name, final Locale locale) throws Exception {
        FreeMarkerView view = (FreeMarkerView) viewResolver.resolveViewName(name, locale);
        view.setServletContext(servletContext);
        view.setApplicationContext(appContext);
        return view;
    }

    public static class SimpleStaticMessageSource extends MessageSourceSupport implements MessageSource {

        private final Map<String, String> messageMap;

        public SimpleStaticMessageSource(Map<String, String> messageMap) {
            this.messageMap = messageMap;
        }

        @Override
        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            String message = messageMap.get(code);

            if (message == null) {
                return defaultMessage;
            }

            return this.formatMessage(message, args, Locale.getDefault());
        }

        @Override
        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            return getMessage(code, args, null, locale);
        }

        @Override
        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            throw new UnsupportedOperationException();
        }

    }
}

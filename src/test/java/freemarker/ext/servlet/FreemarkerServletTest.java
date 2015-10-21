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
package freemarker.ext.servlet;

import static freemarker.ext.servlet.FreemarkerServlet.*;
import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import freemarker.template.Configuration;

public class FreemarkerServletTest {

    private static final String TEST_TEMPLATE_PATH = "classpath:freemarker/ext/servlet";

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        servletContext = new MockServletContext();
        servletContext.setContextPath("/");
    }

    @Test
    public void testContentTypeInitParams() throws Exception {
        // Default is INIT_PARAM_VALUE_ALWAYS, hence null is the same:
        for (String overrideCT : new String[] { null, INIT_PARAM_VALUE_ALWAYS }) {
            assertResponseContentTypeEquals(
                    DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                    null, overrideCT, // <- init-params
                    "foo.ftl", null); // <- request
            assertResponseContentTypeEquals(
                    "text/css; charset=UTF-8", // <- expected
                    "text/css", overrideCT, // <- init-params
                    "foo.ftl", null); // <- request
            assertResponseContentTypeEquals(
                    DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                    null, overrideCT, // <- init-params
                    "foo.ftl", "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/css; charset=UTF-8", // <- expected
                    "text/css", overrideCT, // <- init-params
                    "foo.ftl", "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/plain", // <- expected
                    null, overrideCT, // <- init-params
                    "contentTypeAttr.ftl", "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/plain; charset=UTF-8", // <- expected
                    null, overrideCT, // <- init-params
                    "outputFormatHeader.ftl", "application/json"); // <- request
        }
        
        assertResponseContentTypeEquals(
                DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "foo.ftl", null); // <- request
        assertResponseContentTypeEquals(
                "text/css; charset=UTF-8", // <- expected
                "text/css", INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "foo.ftl", null); // <- request        
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "foo.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                "text/css", INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "foo.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "text/plain", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "contentTypeAttr.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "text/plain; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                "outputFormatHeader.ftl", "application/json"); // <- request
        
        assertResponseContentTypeEquals(
                DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                "foo.ftl", null); // <- request
        assertResponseContentTypeEquals(
                "text/css; charset=UTF-8", // <- expected
                "text/css", INIT_PARAM_VALUE_NEVER, // <- init-params
                "foo.ftl", null); // <- request        
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                "foo.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                "text/css", INIT_PARAM_VALUE_NEVER, // <- init-params
                "foo.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                "contentTypeAttr.ftl", "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                "outputFormatHeader.ftl", "application/json"); // <- request
    }
    
    private void assertResponseContentTypeEquals(
            String exptectContentType,
            String ctInitParam, String overrideCTInitParam,
            String templateName, String responseCT)
                    throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, templateName);
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        if (responseCT != null) {
            response.setContentType(responseCT);
            assertEquals(responseCT, response.getContentType());
        } else {
            assertNull(response.getContentType());
        }
    
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter(INIT_PARAM_TEMPLATE_PATH, TEST_TEMPLATE_PATH);
        servletConfig.addInitParameter(Configuration.DEFAULT_ENCODING_KEY, "UTF-8");
        if (ctInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_CONTENT_TYPE, ctInitParam);            
        }
        if (overrideCTInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, overrideCTInitParam);
        }
        
        FreemarkerServlet freemarkerServlet = new FreemarkerServlet();
        try {
            freemarkerServlet.init(servletConfig);
            
            freemarkerServlet.doGet(request, response);
        
            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            assertEquals(exptectContentType, response.getContentType());
        } finally {
            freemarkerServlet.destroy();
        }
    }

    private MockHttpServletRequest createMockHttpServletRequest(final ServletContext servletContext,
            final String pathInfo) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext);
        servletRequest.setServerName("localhost");
        servletRequest.setServerPort(8080);
        servletRequest.setContextPath("");
        servletRequest.setRequestURI(pathInfo);
        servletRequest.setPathInfo(pathInfo);
        return servletRequest;
    }
    
}
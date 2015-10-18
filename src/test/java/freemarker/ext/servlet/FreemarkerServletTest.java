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
import org.springframework.util.Assert;

import freemarker.log.Logger;

public class FreemarkerServletTest {

    private static final Logger LOG = Logger.getLogger("freemarker.servlet");

    private static final String TEST_TEMPLATE_PATH = "classpath:freemarker/ext/servlet";

    private MockServletContext servletContext;

    @Before
    public void setUp() throws ServletException, IOException {
        servletContext = new MockServletContext();
        servletContext.setContextPath("/");
    }

    @Test
    public void testContentTypeInitParams_withNoResponseContentType_DefaultOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(response.getContentType());

        createFreemarkerServlet().doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains(FreemarkerServlet.DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void testContentTypeInitParams_withNoResponseContentType_DefaultOverriding2() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(response.getContentType());

        createFreemarkerServlet(
                FreemarkerServlet.INIT_PARAM_CONTENT_TYPE, "text/css")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains("text/css"));
    }

    @Test
    public void testContentTypeInitParams_withResponseContentType_DefaultOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet().doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains(FreemarkerServlet.DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void testContentTypeInitParams_withResponseContentType_DefaultOverriding2() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet(
                FreemarkerServlet.INIT_PARAM_CONTENT_TYPE, "text/css")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains("text/css"));
    }

    @Test
    public void testContentTypeInitParams_withResponseContentType_NoOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet(FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testContentTypeInitParams_withResponseContentType_NoOverriding2() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet(
                FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false",
                FreemarkerServlet.INIT_PARAM_CONTENT_TYPE, "text/css")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    public void testContentTypeInitParams_withNoResponseContentType_NoOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(response.getContentType());

        createFreemarkerServlet(FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains(FreemarkerServlet.DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void testContentTypeInitParams_withNoResponseContentType_NoOverriding2() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/foo.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertNull(response.getContentType());

        createFreemarkerServlet(
                FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false",
                FreemarkerServlet.INIT_PARAM_CONTENT_TYPE, "text/css")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().contains("text/css"));
    }

    @Test
    public void testContentTypeInitParams_ftlAttrAlwaysWins_DefaultOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/contentTypeAttr.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet().doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("text/plain", response.getContentType());
    }
    
    @Test
    public void testContentTypeInitParams_ftlAttrAlwaysWins_NoOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/contentTypeAttr.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet(FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("text/plain", response.getContentType());
    }

    @Test
    public void testContentTypeInitParams_outputFormatAlwaysWins_DefaultOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/outputFormatHeader.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet().doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("text/plain", response.getContentType());
    }
    
    @Test
    public void testContentTypeInitParams_outputFormatAlwaysWins_NoOverriding() throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, "/outputFormatHeader.ftl");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("application/json");
        assertEquals("application/json", response.getContentType());

        createFreemarkerServlet(FreemarkerServlet.INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, "false")
                .doGet(request, response);
        LOG.debug("response content: " + response.getContentAsString());

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("text/plain", response.getContentType());
    }
    
    private FreemarkerServlet createFreemarkerServlet(String... initParams) throws ServletException {
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter(FreemarkerServlet.INIT_PARAM_TEMPLATE_PATH, TEST_TEMPLATE_PATH);
        Assert.isTrue(initParams.length % 2 == 0);
        for (int i = 0; i < initParams.length; i += 2) {
            servletConfig.addInitParameter(initParams[i], initParams[i + 1]); 
        }
        
        FreemarkerServlet freemarkerServlet = new FreemarkerServlet();
        freemarkerServlet.init(servletConfig);
        return freemarkerServlet;
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
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
package org.apache.freemarker.servlet;

import static org.apache.freemarker.servlet.FreemarkerServlet.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.FileNameGlobMatcher;
import org.apache.freemarker.core.templateresolver.FirstMatchTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class FreemarkerServletTest {

    private static final String OUTPUT_FORMAT_HEADER_FTL = "outputFormatHeader.f3ac";
    private static final String CONTENT_TYPE_ATTR_FTL = "contentTypeAttr.f3ac";
    private static final String CONTENT_TYPE_ATTR_WITH_CHARSET_FTL = "contentTypeAttrWithCharset.f3ac";
    private static final String FOO_FTL = "foo.f3ac";
    private static final String FOO_SRC_UTF8_FTL = "foo-src-utf8.f3ac";
    private static final String FOO_OUT_UTF8_FTL = "foo-out-utf8.f3ac";
    private static final String STD_OUTPUT_FORMAT_HTML_FTL = "stdOutputFormatHTML.f3ac";
    private static final String STD_OUTPUT_FORMAT_XML_FTL = "stdOutputFormatXML.f3ac";
    private static final String STD_OUTPUT_FORMAT_XHTML_FTL = "stdOutputFormatXHTML.f3ac";
    private static final String STD_OUTPUT_FORMAT_JAVA_SCRIPT_FTL = "stdOutputFormatJavaScript.f3ac";
    private static final String STD_OUTPUT_FORMAT_JSON_FTL = "stdOutputFormatJSON.f3ac";
    private static final String STD_OUTPUT_FORMAT_CSS_FTL = "stdOutputFormatCSS.f3ac";
    private static final String STD_OUTPUT_FORMAT_PLAIN_TEXT_FTL = "stdOutputFormatPlainText.f3ac";
    private static final String STD_OUTPUT_FORMAT_RTF_FTL = "stdOutputFormatRTF.f3ac";

    private static final Locale DEFAULT_LOCALE = Locale.US;
    private static final Charset CFG_DEFAULT_ENCODING = StandardCharsets.US_ASCII;
    /** According to the Servlet Specification */
    private static final Charset SERVLET_RESPONSE_DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
    private static final String DEFAULT_CONTENT_TYPE = "text/html";

    private static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

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
                    FOO_FTL, null); // <- request
            assertResponseContentTypeEquals(
                    "text/css; charset=UTF-8", // <- expected
                    "text/css", overrideCT, // <- init-params
                    FOO_FTL, null); // <- request
            assertResponseContentTypeEquals(
                    DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                    null, overrideCT, // <- init-params
                    FOO_FTL, "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/css; charset=UTF-8", // <- expected
                    "text/css", overrideCT, // <- init-params
                    FOO_FTL, "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/plain", // <- expected
                    null, overrideCT, // <- init-params
                    CONTENT_TYPE_ATTR_FTL, "application/json"); // <- request
            assertResponseContentTypeEquals(
                    "text/plain; charset=UTF-8", // <- expected
                    null, overrideCT, // <- init-params
                    OUTPUT_FORMAT_HEADER_FTL, "application/json"); // <- request
        }

        assertResponseContentTypeEquals(
                DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                FOO_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "text/css; charset=UTF-8", // <- expected
                "text/css", INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                FOO_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                FOO_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                "text/css", INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                FOO_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "text/plain", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                CONTENT_TYPE_ATTR_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "text/plain; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_WHEN_TEMPLATE_HAS_MIME_TYPE, // <- init-params
                OUTPUT_FORMAT_HEADER_FTL, "application/json"); // <- request

        assertResponseContentTypeEquals(
                DEFAULT_CONTENT_TYPE + "; charset=UTF-8", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                FOO_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "text/css; charset=UTF-8", // <- expected
                "text/css", INIT_PARAM_VALUE_NEVER, // <- init-params
                FOO_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                FOO_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                "text/css", INIT_PARAM_VALUE_NEVER, // <- init-params
                FOO_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                CONTENT_TYPE_ATTR_FTL, "application/json"); // <- request
        assertResponseContentTypeEquals(
                "application/json", // <- expected
                null, INIT_PARAM_VALUE_NEVER, // <- init-params
                OUTPUT_FORMAT_HEADER_FTL, "application/json"); // <- request
    }

    @Test
    public void testStandardContentType() throws Exception {
        assertResponseContentTypeEquals(
                "text/html; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_HTML_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/xhtml+xml; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_XHTML_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/xml; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_XML_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/javascript; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_JAVA_SCRIPT_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/json; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_JSON_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "text/css; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_CSS_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "text/plain; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_PLAIN_TEXT_FTL, null); // <- request
        assertResponseContentTypeEquals(
                "application/rtf; charset=UTF-8", // <- expected
                null, null, // <- init-params
                STD_OUTPUT_FORMAT_RTF_FTL, null); // <- request
    }
    
    @Test
    public void testResponseLocaleInitParams() throws Exception {
        assertTemplateLocaleEquals(
                DEFAULT_LOCALE, // <- expected template locale
                null, // <- request locale
                null, // <- init-param
                FOO_FTL);
        assertTemplateLocaleEquals(
                DEFAULT_LOCALE, // <- expected template locale
                Locale.FRENCH, // <- request locale
                null, // <- init-param
                FOO_FTL);
        assertTemplateLocaleEquals(
                DEFAULT_LOCALE, // <- expected template locale
                Locale.FRENCH, // <- request locale
                INIT_PARAM_VALUE_ALWAYS, // <- init-param
                FOO_FTL);
        assertTemplateLocaleEquals(
                DEFAULT_LOCALE, // <- expected template locale
                null, // <- request locale
                INIT_PARAM_VALUE_NEVER, // <- init-param
                FOO_FTL);
        assertTemplateLocaleEquals(
                Locale.FRENCH, // <- expected template locale
                Locale.FRENCH, // <- request locale
                INIT_PARAM_VALUE_NEVER, // <- init-param
                FOO_FTL);
    }

    @Test
    public void testResponseOutputCharsetInitParam() throws Exception {
        for (String initParamValue : new String[] { null, FreemarkerServlet.INIT_PARAM_VALUE_LEGACY }) {
            // Legacy mode is not aware of the outputEncoding, thus it doesn't set it:
            assertOutputEncodingEquals(
                    CFG_DEFAULT_ENCODING, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    FOO_FTL);
            assertOutputEncodingEquals(
                    CFG_DEFAULT_ENCODING, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    FOO_FTL);
            // Legacy mode follows the source encoding of the template:
            assertOutputEncodingEquals(
                    StandardCharsets.UTF_8, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    FOO_SRC_UTF8_FTL);
            // Legacy mode doesn't deal with outputEncoding, but it's inherited by the Environment from the Template:
            assertOutputEncodingEquals(
                    CFG_DEFAULT_ENCODING, // <- expected response.characterEncoding
                    StandardCharsets.UTF_8, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    FOO_OUT_UTF8_FTL);
            // Charset in content type is the strongest:
            assertOutputEncodingEquals(
                    ISO_8859_2, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    FOO_FTL);
            assertOutputEncodingEquals(
                    ISO_8859_2, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    FOO_SRC_UTF8_FTL);
            assertOutputEncodingEquals(
                    StandardCharsets.UTF_8, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    CONTENT_TYPE_ATTR_WITH_CHARSET_FTL);
            assertOutputEncodingEquals(
                    StandardCharsets.UTF_8, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    initParamValue, // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    CONTENT_TYPE_ATTR_WITH_CHARSET_FTL);
        }
        
        // Non-legacy mode always keeps env.outputEncoding in sync. with the Servlet response encoding:
        assertOutputEncodingEquals(
                CFG_DEFAULT_ENCODING, // <- expected response.characterEncoding
                CFG_DEFAULT_ENCODING, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FROM_TEMPLATE, // <- init-param
                FOO_FTL);
        // Non-legacy mode considers the template-specific outputEncoding:
        assertOutputEncodingEquals(
                StandardCharsets.UTF_8, // <- expected response.characterEncoding
                StandardCharsets.UTF_8, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FROM_TEMPLATE, // <- init-param
                FOO_OUT_UTF8_FTL);
        // Non-legacy mode uses the template source encoding as a fallback for outputEncoding:
        assertOutputEncodingEquals(
                StandardCharsets.UTF_8, // <- expected response.characterEncoding
                StandardCharsets.UTF_8, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FROM_TEMPLATE, // <- init-param
                FOO_SRC_UTF8_FTL);
        // Not allowed to specify the charset in the contentType init-param: 
        try {
            assertOutputEncodingEquals(
                    null, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    FreemarkerServlet.INIT_PARAM_VALUE_FROM_TEMPLATE, // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    FOO_FTL);
            fail();
        } catch (ServletException e) {
            assertThat(e.getCause().getCause().getMessage(), containsString(FreemarkerServlet.INIT_PARAM_VALUE_LEGACY));
        }
        // But the legacy content_type template custom setting can still set the output charset:
        assertOutputEncodingEquals(
                StandardCharsets.UTF_8, // <- expected response.characterEncoding
                StandardCharsets.UTF_8, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FROM_TEMPLATE, // <- init-param
                CONTENT_TYPE_ATTR_WITH_CHARSET_FTL);
        
        // Do not set mode:
        assertOutputEncodingEquals(
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected response.characterEncoding
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_DO_NOT_SET, // <- init-param
                FOO_FTL);
        assertOutputEncodingEquals(
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected response.characterEncoding
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_DO_NOT_SET, // <- init-param
                FOO_SRC_UTF8_FTL);
        assertOutputEncodingEquals(
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected response.characterEncoding
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_DO_NOT_SET, // <- init-param
                FOO_OUT_UTF8_FTL);
        // Not allowed to specify the charset in the contentType init-param: 
        try {
            assertOutputEncodingEquals(
                    SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected response.characterEncoding
                    SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected env.outputEncoding
                    FreemarkerServlet.INIT_PARAM_VALUE_DO_NOT_SET, // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    FOO_FTL);
            fail();
        } catch (ServletException e) {
            assertThat(e.getCause().getCause().getMessage(), containsString(FreemarkerServlet.INIT_PARAM_VALUE_LEGACY));
        }
        // The legacy content_type template custom setting can still specify an output charset, though it will be ignored:
        assertOutputEncodingEquals(
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected response.characterEncoding
                SERVLET_RESPONSE_DEFAULT_CHARSET, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_DO_NOT_SET, // <- init-param
                CONTENT_TYPE_ATTR_WITH_CHARSET_FTL);
        
        // Forced mode:
        assertOutputEncodingEquals(
                StandardCharsets.UTF_16LE, // <- expected response.characterEncoding
                StandardCharsets.UTF_16LE, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "UTF-16LE", // <- init-param
                FOO_FTL);
        assertOutputEncodingEquals(
                StandardCharsets.UTF_16LE, // <- expected response.characterEncoding
                StandardCharsets.UTF_16LE, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "UTF-16LE", // <- init-param
                FOO_SRC_UTF8_FTL);
        assertOutputEncodingEquals(
                StandardCharsets.UTF_16LE, // <- expected response.characterEncoding
                StandardCharsets.UTF_16LE, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "UTF-16LE", // <- init-param
                FOO_OUT_UTF8_FTL);
        try {
            assertOutputEncodingEquals(
                    null, // <- expected response.characterEncoding
                    null, // <- expected env.outputEncoding
                    FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "noSuchCharset", // <- init-param
                    FOO_FTL);
            fail();
        } catch (ServletException e) {
            assertThat(e.getCause().getCause(), instanceOf(UnsupportedCharsetException.class));
        }
        // Not allowed to specify the charset in the contentType init-param: 
        try {
            assertOutputEncodingEquals(
                    StandardCharsets.UTF_16LE, // <- expected response.characterEncoding
                    StandardCharsets.UTF_16LE, // <- expected env.outputEncoding
                    FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "UTF-16LE", // <- init-param
                    "text/html; charset=ISO-8859-2", // ContentType init-param
                    FOO_FTL);
            fail();
        } catch (ServletException e) {
            assertThat(e.getCause().getCause().getMessage(), containsString(FreemarkerServlet.INIT_PARAM_VALUE_LEGACY));
        }
        // The legacy content_type template custom setting can still specify an output charset, though it will be overridden:
        assertOutputEncodingEquals(
                StandardCharsets.UTF_16LE, // <- expected response.characterEncoding
                StandardCharsets.UTF_16LE, // <- expected env.outputEncoding
                FreemarkerServlet.INIT_PARAM_VALUE_FORCE_PREFIX + "UTF-16LE", // <- init-param
                CONTENT_TYPE_ATTR_WITH_CHARSET_FTL);
    }

    private void assertResponseContentTypeEquals(
            String expectedContentType,
            String ctInitParam, String overrideCTInitParam,
            String templateName, String responseCT)
                    throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, templateName, null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        if (responseCT != null) {
            response.setContentType(responseCT);
            assertEquals(responseCT, response.getContentType());
        } else {
            assertNull(response.getContentType());
        }

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter(Configuration.ExtendableBuilder.SOURCE_ENCODING_KEY, "UtF-8");
        if (ctInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_CONTENT_TYPE, ctInitParam);
        }
        if (overrideCTInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_OVERRIDE_RESPONSE_CONTENT_TYPE, overrideCTInitParam);
        }

        TestFreemarkerServlet freemarkerServlet = new TestFreemarkerServlet();
        try {
            freemarkerServlet.init(servletConfig);

            freemarkerServlet.doGet(request, response);

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            assertEquals(expectedContentType, response.getContentType());
        } finally {
            freemarkerServlet.destroy();
        }
    }

    private void assertTemplateLocaleEquals(
            Locale expectedLocale,
            Locale requestLocale,
            String overrideResponseLocaleInitParam,
            String templateName)
                    throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, templateName, requestLocale);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockServletConfig servletConfig = new MockServletConfig(servletContext);

        if (overrideResponseLocaleInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_OVERRIDE_RESPONSE_LOCALE, overrideResponseLocaleInitParam);
        }

        TestFreemarkerServlet freemarkerServlet = new TestFreemarkerServlet();

        try {
            freemarkerServlet.init(servletConfig);
            freemarkerServlet.doGet(request, response);

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            assertEquals(expectedLocale, freemarkerServlet.lastLocale);
            assertEquals(freemarkerServlet.lastLocale, freemarkerServlet.lastMainTemplate.getLocale());
        } finally {
            freemarkerServlet.destroy();
        }
    }

    private void assertOutputEncodingEquals(
            Charset expectedRespCharacterEncoding,
            Charset expectedEnvOutputEncoding,
            String responseCharacterEncodingInitParam,
            String templateName) throws ServletException, IOException {
        assertOutputEncodingEquals(
                expectedRespCharacterEncoding, expectedEnvOutputEncoding,
                responseCharacterEncodingInitParam, null,
                templateName);
    }
    
    private void assertOutputEncodingEquals(
            Charset expectedRespCharacterEncoding,
            Charset expectedEnvOutputEncoding,
            String responseCharacterEncodingInitParam,
            String contentTypeInitParam,
            String templateName)
                    throws ServletException, IOException {
        MockHttpServletRequest request = createMockHttpServletRequest(servletContext, templateName, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockServletConfig servletConfig = new MockServletConfig(servletContext);

        if (responseCharacterEncodingInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_RESPONSE_CHARACTER_ENCODING, responseCharacterEncodingInitParam);
        }
        
        if (contentTypeInitParam != null) {
            servletConfig.addInitParameter(INIT_PARAM_CONTENT_TYPE, contentTypeInitParam);
        }

        TestFreemarkerServlet freemarkerServlet = new TestFreemarkerServlet();

        try {
            freemarkerServlet.init(servletConfig);
            freemarkerServlet.doGet(request, response);

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());
            assertEquals(expectedEnvOutputEncoding, freemarkerServlet.lastOutputEncoding);
            assertEquals(expectedRespCharacterEncoding,
                    response.getCharacterEncoding() != null ? Charset.forName (response.getCharacterEncoding()) : null);
        } finally {
            freemarkerServlet.destroy();
        }
    }

    private MockHttpServletRequest createMockHttpServletRequest(final ServletContext servletContext,
            final String pathInfo, final Locale requestLocale) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(servletContext) {

            @Override
            public Locale getLocale() {
                return requestLocale;
            }
        };

        servletRequest.setServerName("localhost");
        servletRequest.setServerPort(8080);
        servletRequest.setContextPath("");
        servletRequest.setRequestURI(pathInfo);
        servletRequest.setPathInfo(pathInfo);

        return servletRequest;
    }

    static class TestFreemarkerServlet extends FreemarkerServlet {

        private Template lastMainTemplate;
        private Locale lastLocale;
        private Charset lastOutputEncoding;

        @Override
        protected Configuration.ExtendableBuilder<?> createConfigurationBuilder() {
            return new FreemarkerServletConfigurationBuilder(TestFreemarkerServlet.this, Configuration.VERSION_3_0_0) {

                @Override
                protected Locale getDefaultLocale() {
                    return DEFAULT_LOCALE;
                }

                @Override
                protected Charset getDefaultSourceEncoding() {
                    return CFG_DEFAULT_ENCODING;
                }

                @Override
                protected TemplateConfigurationFactory getDefaultTemplateConfigurations() {
                    TemplateConfiguration.Builder outUtf8TCB = new TemplateConfiguration.Builder();
                    outUtf8TCB.setOutputEncoding(StandardCharsets.UTF_8);

                    TemplateConfiguration.Builder srcUtf8TCB = new TemplateConfiguration.Builder();
                    srcUtf8TCB.setSourceEncoding(StandardCharsets.UTF_8);

                    return new FirstMatchTemplateConfigurationFactory(
                            new ConditionalTemplateConfigurationFactory(
                                    new FileNameGlobMatcher(FOO_SRC_UTF8_FTL), srcUtf8TCB.build()),
                            new ConditionalTemplateConfigurationFactory(
                                    new FileNameGlobMatcher(FOO_OUT_UTF8_FTL), outUtf8TCB.build())
                    ).allowNoMatch(true);
                }
            };
        }

        @Override
        protected TemplateLoader createTemplateLoader(String templatePath) throws IOException {
            // Override default template loader
            if (templatePath.equals("class://")) {
                ByteArrayTemplateLoader tl = new ByteArrayTemplateLoader();
                
                tl.putTemplate(FOO_FTL, "foo"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(FOO_SRC_UTF8_FTL, "foo"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(FOO_OUT_UTF8_FTL, "foo"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(CONTENT_TYPE_ATTR_FTL, "<#ftl customSettings={ 'content_type': 'text/plain' }>foo"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(CONTENT_TYPE_ATTR_WITH_CHARSET_FTL,
                        "<#ftl customSettings={ 'content_type': 'text/plain; charset=UTF-8' }>foo"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(OUTPUT_FORMAT_HEADER_FTL, "<#ftl outputFormat='plainText'>foo"
                        .getBytes(StandardCharsets.UTF_8));
                
                tl.putTemplate(STD_OUTPUT_FORMAT_HTML_FTL, "<#ftl outputFormat='HTML'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_XHTML_FTL, "<#ftl outputFormat='XHTML'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_XML_FTL, "<#ftl outputFormat='XML'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_JAVA_SCRIPT_FTL, "<#ftl outputFormat='JavaScript'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_JSON_FTL, "<#ftl outputFormat='JSON'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_CSS_FTL, "<#ftl outputFormat='CSS'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_PLAIN_TEXT_FTL, "<#ftl outputFormat='plainText'>"
                        .getBytes(StandardCharsets.UTF_8));
                tl.putTemplate(STD_OUTPUT_FORMAT_RTF_FTL, "<#ftl outputFormat='RTF'>"
                        .getBytes(StandardCharsets.UTF_8));
                
                return tl;
            } else {
                return super.createTemplateLoader(templatePath);
            }
        }

        @Override
        protected void processEnvironment(Environment env, HttpServletRequest request, HttpServletResponse response)
                throws TemplateException, IOException {
            lastMainTemplate = env.getMainTemplate();
            lastLocale = env.getLocale();
            lastOutputEncoding = env.getOutputEncoding();
            super.processEnvironment(env, request, response);
        }

    }

}
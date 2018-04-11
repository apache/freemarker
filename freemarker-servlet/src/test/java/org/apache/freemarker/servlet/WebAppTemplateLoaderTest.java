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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.ResourceUtils;

public class WebAppTemplateLoaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(WebAppTemplateLoader.class);

    @Test
    public void testTemplateFound() throws Exception {
        assertThat(createConfiguration().getTemplate("test.f3ah").toString(), endsWith("foo"));
    }

    @Test
    public void testTemplateNotFound() throws IOException {
        try {
            createConfiguration().getTemplate("missing.ftl");
            fail();
        } catch (TemplateNotFoundException e) {
            LOG.debug("Expected result", e);
            String errMsg = e.getMessage();
            assertThat(errMsg, containsString("WebAppTemplateLoader"));
            assertThat(errMsg, containsString("MyApp"));
            assertThat(errMsg, containsString("WEB-INF/templates"));
        }
    }

    private Configuration createConfiguration() {
        MockServletContext servletContext = new MockServletContext(
                ResourceUtils.CLASSPATH_URL_PREFIX
                        + "org/apache/freemarker/servlet/webapptemplateloadertest");
        servletContext.setServletContextName("MyApp");
        WebAppTemplateLoader tl = new WebAppTemplateLoader(servletContext, "WEB-INF/templates");
        return new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(tl).build();
    }

}

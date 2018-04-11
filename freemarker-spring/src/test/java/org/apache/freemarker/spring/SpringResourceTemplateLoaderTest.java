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
package org.apache.freemarker.spring;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateNotFoundException;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class SpringResourceTemplateLoaderTest {

    private static final String TEMPLATE_BASE_PATH = "classpath:META-INF/templates/";

    private GenericApplicationContext appContext;
    private SpringResourceTemplateLoader templateLoader;
    private Configuration cfg;

    @Before
    public void setUp() throws IOException {
        appContext = new GenericApplicationContext();
        templateLoader = new SpringResourceTemplateLoader();
        templateLoader.setBaseLocation(TEMPLATE_BASE_PATH);
        templateLoader.setResourceLoader(appContext);
        cfg = new TestConfigurationBuilder().templateLoader(templateLoader).build();
    }

    @After
    public void tearDown() throws Exception {
        if (appContext.isActive()) {
            appContext.stop();
            appContext.destroy();
            appContext.close();
        }
    }

    @Test
    public void testSuccessful() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertThat(cfg.getTemplate("sub1/sub2/t.f3ah").toString(), endsWith("foo"));
        }
    }

    @Test
    public void testNotFound() throws Exception {
        for (int i = 0; i < 2; i++) {
            try {
                cfg.getTemplate("sub1X/sub2/t.ftl");
                fail();
            } catch (TemplateNotFoundException e) {
                assertThat(e.getMessage(), containsString("sub1X"));
                assertNull(e.getCause());
            }
        }
    }

}

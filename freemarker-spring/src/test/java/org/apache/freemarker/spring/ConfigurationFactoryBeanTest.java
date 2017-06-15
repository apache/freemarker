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

import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

public class ConfigurationFactoryBeanTest {

    private GenericApplicationContext appContext;

    @Before
    public void setUp() throws Exception {
        appContext = new GenericApplicationContext();
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
    public void testConfigurationFactoryBeanDefinition() throws Exception {
        final Map<String, String> settings = new LinkedHashMap<>();
        settings.put(ExtendableBuilder.LOCALIZED_TEMPLATE_LOOKUP_KEY_CAMEL_CASE, "true");

        BeanDefinition beanDef =
                BeanDefinitionBuilder.genericBeanDefinition(ConfigurationFactoryBean.class.getName())
                .addPropertyValue("settings", settings)
                .getBeanDefinition();

        appContext.registerBeanDefinition("freemarkerConfig", beanDef);
        appContext.refresh();
        appContext.start();

        Object bean = appContext.getBean("freemarkerConfig");
        assertTrue("Not a Configuration object: " + bean, bean instanceof Configuration);

        Configuration config = (Configuration) bean;
        assertTrue(config.getLocalizedTemplateLookup());
    }

}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.AutoEscapingPolicy;
import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.MutableParsingAndProcessingConfiguration;
import org.apache.freemarker.core.NamingConvention;
import org.apache.freemarker.core.TagSyntax;
import org.apache.freemarker.core.TemplateLanguage;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.impl.MruCacheStorage;
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
    public void testConfigurationFactoryBeanSettings() throws Exception {
        final Map<String, String> settings = new LinkedHashMap<>();

        settings.put(MutableParsingAndProcessingConfiguration.SOURCE_ENCODING_KEY, "UTF-8");
        settings.put(MutableParsingAndProcessingConfiguration.WHITESPACE_STRIPPING_KEY, "true");
        settings.put(MutableParsingAndProcessingConfiguration.AUTO_ESCAPING_POLICY_KEY, "enableIfSupported");
        settings.put(MutableParsingAndProcessingConfiguration.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY, "true");
        settings.put(MutableParsingAndProcessingConfiguration.TEMPLATE_LANGUAGE_KEY, "FTL");
        settings.put(MutableParsingAndProcessingConfiguration.TAG_SYNTAX_KEY, "squareBracket");
        settings.put(MutableParsingAndProcessingConfiguration.NAMING_CONVENTION_KEY, "camelCase");
        settings.put(MutableParsingAndProcessingConfiguration.TAB_SIZE_KEY, "4");

        settings.put(ExtendableBuilder.OBJECT_WRAPPER_KEY, "restricted");
        settings.put(ExtendableBuilder.TEMPLATE_CACHE_STORAGE_KEY, "strong:20, soft:250");

        final Map<String, Object> sharedVars = new HashMap<>();
        sharedVars.put("sharedVar1", "sharedVal1");
        sharedVars.put("sharedVar2", "sharedVal2");

        // Creating bean definition which is equivalent to <bean/> element in Spring XML configuration.
        BeanDefinition beanDef =
                BeanDefinitionBuilder.genericBeanDefinition(ConfigurationFactoryBean.class.getName())
                .addPropertyValue("incompatibleImprovements", new Version(3, 0, 0))
                .addPropertyValue("settings", settings)
                .addPropertyValue("sharedVariables", sharedVars)
                .addPropertyValue("templateUpdateDelayMilliseconds", 60000)
                .addPropertyValue("localizedTemplateLookup", "false")
                .getBeanDefinition();

        appContext.registerBeanDefinition("freemarkerConfig", beanDef);
        appContext.refresh();
        appContext.start();

        Object bean = appContext.getBean("freemarkerConfig");
        assertTrue("Not a Configuration object: " + bean, bean instanceof Configuration);

        Configuration config = (Configuration) bean;

        assertEquals(new Version(3, 0, 0), config.getIncompatibleImprovements());
        assertEquals(Charset.forName("UTF-8"), config.getSourceEncoding());
        assertTrue(config.isWhitespaceStrippingSet());
        assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, config.getAutoEscapingPolicy());
        assertTrue(config.isRecognizeStandardFileExtensionsSet());
        assertEquals(TemplateLanguage.FTL, config.getTemplateLanguage());
        assertEquals(TagSyntax.SQUARE_BRACKET, config.getTagSyntax());
        assertEquals(NamingConvention.CAMEL_CASE, config.getNamingConvention());
        assertEquals(4, config.getTabSize());

        assertTrue(config.getObjectWrapper() instanceof RestrictedObjectWrapper);
        assertFalse(config.getLocalizedTemplateLookup());
        CacheStorage cacheStorage = config.getTemplateCacheStorage();
        assertTrue(cacheStorage instanceof MruCacheStorage);
        assertEquals(20, ((MruCacheStorage) cacheStorage).getStrongSizeLimit());
        assertEquals(250, ((MruCacheStorage) cacheStorage).getSoftSizeLimit());
        assertEquals(60000, config.getTemplateUpdateDelayMilliseconds().longValue());

        assertEquals("sharedVal1", config.getSharedVariables().get("sharedVar1"));
        assertEquals("sharedVal2", config.getSharedVariables().get("sharedVar2"));
    }

}

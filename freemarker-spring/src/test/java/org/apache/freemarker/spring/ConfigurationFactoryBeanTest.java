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

import org.apache.freemarker.core.*;
import org.apache.freemarker.core.Configuration.ExtendableBuilder;
import org.apache.freemarker.core.model.impl.RestrictedObjectWrapper;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.impl.MruCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

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
            appContext.close();
        }
    }

    @Test
    public void testConfigurationFactoryBeanSettings() throws Exception {
        final Properties settings = new Properties();

        settings.setProperty(MutableParsingAndProcessingConfiguration.SOURCE_ENCODING_KEY, "UTF-8");
        settings.setProperty(MutableParsingAndProcessingConfiguration.OUTPUT_FORMAT_KEY, "PlainTextOutputFormat()");
        settings.setProperty(MutableParsingAndProcessingConfiguration.WHITESPACE_STRIPPING_KEY, "true");
        settings.setProperty(MutableParsingAndProcessingConfiguration.AUTO_ESCAPING_POLICY_KEY, "enableIfSupported");
        settings.setProperty(MutableParsingAndProcessingConfiguration.RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY, "true");
        settings.setProperty(MutableParsingAndProcessingConfiguration.TEMPLATE_LANGUAGE_KEY, "F3SC");
        settings.setProperty(MutableParsingAndProcessingConfiguration.TAB_SIZE_KEY, "4");

        settings.setProperty(ExtendableBuilder.OBJECT_WRAPPER_KEY, "restricted");
        settings.setProperty(ExtendableBuilder.TEMPLATE_CACHE_STORAGE_KEY, "strong:20, soft:250");

        final Map<String, Object> sharedVars = new HashMap<>();
        sharedVars.put("sharedVar1", "sharedVal1");
        sharedVars.put("sharedVar2", "sharedVal2");

        final Map<String, String> autoImports = new HashMap<>();
        autoImports.put("mylib1", "/libs/mylib1.f3ah");
        autoImports.put("mylib2", "/libs/mylib2.f3ah");

        final StringTemplateLoader templateLoader = new StringTemplateLoader();
        templateLoader.putTemplate("fooTemplate", "foo");

        // Creating bean definition which is equivalent to <bean/> element in Spring XML configuration like the following:
        //
        // <bean class="org.apache.freemarker.spring.ConfigurationFactoryBean">
        //   <property name="incompatibleImprovements" value="3.0.0" />
        //   <property name="settings">
        //     <props>
        //       <prop key="sourceEncoding">UTF-8</prop>
        //       <prop key="whitespaceStripping">true</prop>
        //       <!-- SNIP -->
        //       <prop key="templateCacheStorage">strong:20, soft:250</prop>
        //     </props>
        //   </property>
        //   <property name="sharedVariables">
        //     <map>
        //       <entry key="sharedVar1" value="sharedVal1" />
        //       <entry key="sharedVar2" value="sharedVal2" />
        //     </map>
        //   </property>
        //   <property name="autoImports">
        //     <map>
        //       <entry key="mylib1" value="/libs/mylib1.f3ah" />
        //       <entry key="mylib2" value="/libs/mylib2.f3ah" />
        //     </map>
        //   </property>
        //   <property name="templateUpdateDelayMilliseconds" value="60000" />
        //   <property name="localizedTemplateLookup" value="false" />
        //   <property name="templateLoader">
        //     <bean class="org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader">
        //     </bean>
        //   </property>
        // </bean>
        //
        BeanDefinition beanDef =
                BeanDefinitionBuilder.genericBeanDefinition(ConfigurationFactoryBean.class.getName())
                .addPropertyValue("incompatibleImprovements", "3.0.0")
                .addPropertyValue("settings", settings)
                .addPropertyValue("sharedVariables", sharedVars)
                .addPropertyValue("autoImports", autoImports)
                .addPropertyValue("templateUpdateDelayMilliseconds", 60000)
                .addPropertyValue("localizedTemplateLookup", "false")
                .addPropertyValue("templateLoader", templateLoader)
                .getBeanDefinition();

        appContext.registerBeanDefinition("freemarkerConfig", beanDef);
        appContext.refresh();
        appContext.start();

        Object bean = appContext.getBean("freemarkerConfig");
        assertTrue("Not a Configuration object: " + bean, bean instanceof Configuration);

        Configuration config = (Configuration) bean;

        assertEquals(new Version(3, 0, 0), config.getIncompatibleImprovements());
        assertEquals(Charset.forName("UTF-8"), config.getSourceEncoding());
        assertEquals(PlainTextOutputFormat.INSTANCE.getName(), config.getOutputFormat().getName());
        assertTrue(config.isWhitespaceStrippingSet());
        assertEquals(AutoEscapingPolicy.ENABLE_IF_SUPPORTED, config.getAutoEscapingPolicy());
        assertTrue(config.isRecognizeStandardFileExtensionsSet());
        assertEquals(DefaultTemplateLanguage.F3SC, config.getTemplateLanguage());
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

        assertEquals("/libs/mylib1.f3ah", config.getAutoImports().get("mylib1"));
        assertEquals("/libs/mylib2.f3ah", config.getAutoImports().get("mylib2"));

        final Template fooTemplate = config.getTemplate("fooTemplate");
        assertEquals("foo", fooTemplate.toString());
    }

}

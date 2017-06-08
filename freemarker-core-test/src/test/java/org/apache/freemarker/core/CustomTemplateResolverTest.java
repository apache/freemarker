/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import static junit.framework.TestCase.*;
import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.ConditionalTemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.GetTemplateResult;
import org.apache.freemarker.core.templateresolver.MalformedTemplateNameException;
import org.apache.freemarker.core.templateresolver.PathGlobMatcher;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.TemplateResolverDependencies;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.impl.DefaultTemplateNameFormat;
import org.apache.freemarker.core.templateresolver.impl.SoftCacheStorage;
import org.apache.freemarker.core.templateresolver.impl.StringTemplateLoader;
import org.apache.freemarker.core.util.BugException;
import org.junit.Test;

public class CustomTemplateResolverTest {

    @Test
    public void testPositive() throws IOException, TemplateException {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateResolver(new CustomTemplateResolver(null))
                .build();
        Template template = cfg.getTemplate(":foo::includes");

        assertEquals("foo:includes", template.getLookupName());

        StringWriter sw = new StringWriter();
        template.process(null, sw);
        assertEquals("In foo:includes, included: In foo:inc", sw.toString());

        try {
            cfg.removeTemplateFromCache("foo", null, null);
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            cfg.clearTemplateCache();
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testConfigurationDefaultForDefaultTemplateResolver() {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0).build();

        assertNotNull(cfg.getTemplateLookupStrategy());
        assertNotNull(cfg.getLocalizedTemplateLookup());
        assertNotNull(cfg.getTemplateCacheStorage());
        assertNotNull(cfg.getTemplateUpdateDelayMilliseconds());
        assertNotNull(cfg.getNamingConvention());

        assertNull(cfg.getTemplateLoader());
        assertNull(cfg.getTemplateConfigurations());

        assertNotNull(cfg.getTemplateLanguage());
        assertNotNull(cfg.getSourceEncoding());
    }

    @Test
    public void testConfigurationDefaultForCustomTemplateResolver() {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateResolver(new CustomTemplateResolver(null))
                .build();

        assertNull(cfg.getTemplateLookupStrategy());
        assertNull(cfg.getLocalizedTemplateLookup());
        assertNull(cfg.getTemplateCacheStorage());
        assertNull(cfg.getTemplateUpdateDelayMilliseconds());
        assertNull(cfg.getTemplateNameFormat());

        assertNull(cfg.getTemplateLoader());
        assertNull(cfg.getTemplateConfigurations());

        assertNotNull(cfg.getTemplateLanguage());
        assertNotNull(cfg.getSourceEncoding());
    }


    @Test
    public void testConfigurationDefaultForCustomTemplateResolver2() {
        Configuration cfg = new Configuration.Builder(Configuration.VERSION_3_0_0)
                .templateResolver(new CustomTemplateResolver(NAMING_CONVENTION_KEY))
                .build();

        assertNull(cfg.getTemplateLookupStrategy());
        assertNull(cfg.getLocalizedTemplateLookup());
        assertNull(cfg.getTemplateCacheStorage());
        assertNull(cfg.getTemplateUpdateDelayMilliseconds());
        assertNotNull(cfg.getNamingConvention()); //!

        assertNull(cfg.getTemplateLoader());
        assertNull(cfg.getTemplateConfigurations());

        assertNotNull(cfg.getTemplateLanguage());
        assertNotNull(cfg.getSourceEncoding());
    }

    @Test
    public void testInvalidConfigurationForDefaultTemplateResolver() {
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateCacheStorage(null).build();
            fail();
        } catch (InvalidSettingValueException e) {
            // Expected
        }
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateUpdateDelayMilliseconds(null).build();
            fail();
        } catch (InvalidSettingValueException e) {
            // Expected
        }
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateLookupStrategy(null).build();
            fail();
        } catch (InvalidSettingValueException e) {
            // Expected
        }
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).localizedTemplateLookup(null).build();
            fail();
        } catch (InvalidSettingValueException e) {
            // Expected
        }
        try {
            new Configuration.Builder(Configuration.VERSION_3_0_0).templateNameFormat(null).build();
            fail();
        } catch (InvalidSettingValueException e) {
            // Expected
        }

        new Configuration.Builder(Configuration.VERSION_3_0_0).templateLoader(null).build();
        new Configuration.Builder(Configuration.VERSION_3_0_0).templateConfigurations(null).build();
    }

    @Test
    public void testConfigurationValidityForCustomTemplateResolver() {
        for (String supportedSetting : new String[]{
                TEMPLATE_LOADER_KEY, TEMPLATE_LOOKUP_STRATEGY_KEY, LOCALIZED_TEMPLATE_LOOKUP_KEY,
                TEMPLATE_NAME_FORMAT_KEY, TEMPLATE_CACHE_STORAGE_KEY, TEMPLATE_UPDATE_DELAY_KEY,
                TEMPLATE_CONFIGURATIONS_KEY }) {
            {
                Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .templateResolver(new CustomTemplateResolver(supportedSetting));
                setSetting(cfgB, supportedSetting);
                cfgB.build();
            }
            {
                Configuration.Builder cfgB = new Configuration.Builder(Configuration.VERSION_3_0_0)
                        .templateResolver(new CustomTemplateResolver(null));
                setSetting(cfgB, supportedSetting);
                try {
                    cfgB.build();
                    fail();
                } catch (InvalidSettingValueException e) {
                    // Expected
                }
            }
        }
    }

    private void setSetting(Configuration.Builder cfgB, String setting) {
        if (TEMPLATE_LOADER_KEY.equals(setting)) {
            cfgB.setTemplateLoader(new StringTemplateLoader());
        } else if (TEMPLATE_LOOKUP_STRATEGY_KEY.equals(setting)) {
            cfgB.setTemplateLookupStrategy(DefaultTemplateLookupStrategy.INSTANCE);
        } else if (LOCALIZED_TEMPLATE_LOOKUP_KEY.equals(setting)) {
            cfgB.setLocalizedTemplateLookup(true);
        } else if (TEMPLATE_NAME_FORMAT_KEY.equals(setting)) {
            cfgB.setTemplateNameFormat(DefaultTemplateNameFormat.INSTANCE);
        } else if (TEMPLATE_CACHE_STORAGE_KEY.equals(setting)) {
            cfgB.setTemplateCacheStorage(new SoftCacheStorage());
        } else if (TEMPLATE_UPDATE_DELAY_KEY.equals(setting)) {
            cfgB.setTemplateUpdateDelayMilliseconds(1234L);
        } else if (TEMPLATE_CONFIGURATIONS_KEY.equals(setting)) {
            cfgB.setTemplateConfigurations(new ConditionalTemplateConfigurationFactory(
                    new PathGlobMatcher("*.x"),
                    new TemplateConfiguration.Builder().build()));
        } else {
            throw new BugException("Unsupported setting: " + setting);
        }
    }

    static class CustomTemplateResolver extends TemplateResolver {

        private final String supportedSetting;
        private TemplateLanguage templateLanguage;

        CustomTemplateResolver(String supportedSetting) {
            this.supportedSetting = supportedSetting;
        }

        @Override
        protected void initialize() throws ConfigurationException {
            TemplateResolverDependencies deps = getDependencies();

            if (TEMPLATE_LOADER_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateLoader());
            } else {
                try {
                    deps.getTemplateLoader();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (TEMPLATE_LOOKUP_STRATEGY_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateLookupStrategy());
            } else {
                try {
                    deps.getTemplateLookupStrategy();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (LOCALIZED_TEMPLATE_LOOKUP_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getLocalizedTemplateLookup());
            } else {
                try {
                    deps.getLocalizedTemplateLookup();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (TEMPLATE_NAME_FORMAT_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateNameFormat());
            } else {
                try {
                    deps.getTemplateNameFormat();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (TEMPLATE_CACHE_STORAGE_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateCacheStorage());
            } else {
                try {
                    deps.getTemplateCacheStorage();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (TEMPLATE_UPDATE_DELAY_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateUpdateDelayMilliseconds());
            } else {
                try {
                    deps.getTemplateUpdateDelayMilliseconds();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            if (TEMPLATE_CONFIGURATIONS_KEY.equals(supportedSetting)) {
                assertNotNull(deps.getTemplateConfigurations());
            } else {
                try {
                    deps.getTemplateConfigurations();
                    fail();
                } catch (IllegalStateException e) {
                    // Expected
                }
            }

            templateLanguage = deps.getTemplateLanguage();
            deps.getSourceEncoding();
        }

        @Override
        protected void checkInitialized() {
            super.checkInitialized();
        }

        @Override
        public GetTemplateResult getTemplate(String name, Locale locale, Serializable customLookupCondition)
                throws IOException {
            name = normalizeRootBasedName(name);
            return new GetTemplateResult(getDependencies()
                    .parse(templateLanguage, name, name,
                            new StringReader(
                                    "In " + name
                                    + (name.endsWith("includes")
                                        ? ", included: <#include 'inc'>"
                                        : "")),
                            null, null, null));
        }

        @Override
        public void clearTemplateCache() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeTemplateFromCache(String name, Locale locale, Serializable customLookupCondition)
                throws IOException, UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toRootBasedName(String baseName, String targetName) throws MalformedTemplateNameException {
            if (targetName.startsWith(":")) {
                return targetName.substring(1);
            } else {
                int lastColonIdx = baseName.lastIndexOf(':');
                return lastColonIdx == -1 ? targetName : baseName.substring(0, lastColonIdx + 1) + targetName;
            }
        }

        @Override
        public String normalizeRootBasedName(String name) throws MalformedTemplateNameException {
            name = name.replaceAll("::", ":");
            return name.startsWith(":") ? name.substring(1) : name;
        }

        @Override
        public boolean supportsTemplateLoaderSetting() {
            return TEMPLATE_LOADER_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsTemplateCacheStorageSetting() {
            return TEMPLATE_CACHE_STORAGE_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsTemplateLookupStrategySetting() {
            return TEMPLATE_LOOKUP_STRATEGY_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsTemplateNameFormatSetting() {
            return TEMPLATE_NAME_FORMAT_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsTemplateConfigurationsSetting() {
            return TEMPLATE_CONFIGURATIONS_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsTemplateUpdateDelayMillisecondsSetting() {
            return TEMPLATE_UPDATE_DELAY_KEY.equals(supportedSetting);
        }

        @Override
        public boolean supportsLocalizedTemplateLookupSetting() {
            return LOCALIZED_TEMPLATE_LOOKUP_KEY.equals(supportedSetting);
        }
    }

}

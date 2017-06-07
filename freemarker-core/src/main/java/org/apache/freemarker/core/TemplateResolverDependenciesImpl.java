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

import static org.apache.freemarker.core.Configuration.ExtendableBuilder.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.freemarker.core.templateresolver.CacheStorage;
import org.apache.freemarker.core.templateresolver.TemplateConfigurationFactory;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLookupStrategy;
import org.apache.freemarker.core.templateresolver.TemplateNameFormat;
import org.apache.freemarker.core.templateresolver.TemplateResolver;
import org.apache.freemarker.core.templateresolver.TemplateResolverDependencies;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * Used internally by {@link Configuration}.
 */
class TemplateResolverDependenciesImpl extends TemplateResolverDependencies {

    private final TemplateResolver templateResolver;
    private final Configuration configuration;

    public TemplateResolverDependenciesImpl(Configuration configuration, TemplateResolver templateResolver) {
        _NullArgumentException.check("configuration", configuration);
        _NullArgumentException.check("templateResolver", templateResolver);
        this.templateResolver = templateResolver;
        this.configuration = configuration;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        checkSettingSupported(TEMPLATE_LOADER_KEY, templateResolver.supportsTemplateLoaderSetting());
        return configuration.getTemplateLoader();
    }

    @Override
    public CacheStorage getTemplateCacheStorage() {
        checkSettingSupported(TEMPLATE_CACHE_STORAGE_KEY, templateResolver.supportsTemplateCacheStorageSetting());
        return configuration.getTemplateCacheStorage();
    }

    @Override
    public TemplateLookupStrategy getTemplateLookupStrategy() {
        checkSettingSupported(TEMPLATE_LOOKUP_STRATEGY_KEY, templateResolver.supportsTemplateLookupStrategySetting());
        return configuration.getTemplateLookupStrategy();
    }

    @Override
    public TemplateNameFormat getTemplateNameFormat() {
        checkSettingSupported(TEMPLATE_NAME_FORMAT_KEY, templateResolver.supportsTemplateNameFormatSetting());
        return configuration.getTemplateNameFormat();
    }

    @Override
    public TemplateConfigurationFactory getTemplateConfigurations() {
        checkSettingSupported(TEMPLATE_CONFIGURATIONS_KEY, templateResolver.supportsTemplateConfigurationsSetting());
        return configuration.getTemplateConfigurations();
    }

    @Override
    public Long getTemplateUpdateDelayMilliseconds() {
        checkSettingSupported(
                TEMPLATE_UPDATE_DELAY_KEY, templateResolver.supportsTemplateUpdateDelayMillisecondsSetting());
        return configuration.getTemplateUpdateDelayMilliseconds();
    }

    @Override
    public Boolean getLocalizedTemplateLookup() {
        checkSettingSupported(LOCALIZED_TEMPLATE_LOOKUP_KEY, templateResolver.supportsLocalizedTemplateLookupSetting());
        return configuration.getLocalizedTemplateLookup();
    }

    @Override
    public Charset getSourceEncoding() {
        return configuration.getSourceEncoding();
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return configuration.getTemplateLanguage();
    }

    private void checkSettingSupported(String name, boolean supported) {
        if (!supported) {
            throw new IllegalStateException(templateResolver.getClass().getName() + " reported that it doesn't support "
                    + "this setting, so you aren't allowed to get it: " + name);
        }
    }

    @Override
    public Template parse(TemplateLanguage templateLanguage, String name, String sourceName, Reader reader,
            TemplateConfiguration templateConfiguration, Charset encoding, InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        return templateLanguage.parse(name, sourceName, reader,
                configuration, templateConfiguration, encoding, streamToUnmarkWhenEncEstabd);
    }
}

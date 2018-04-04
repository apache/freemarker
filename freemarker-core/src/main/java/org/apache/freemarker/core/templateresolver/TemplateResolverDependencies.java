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

package org.apache.freemarker.core.templateresolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.ParseException;
import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateConfiguration;
import org.apache.freemarker.core.TemplateLanguage;

/**
 * Stores the dependencies of a {@link TemplateResolver}. See {@link TemplateResolver} for more.
 * This is normally implemented by FreeMarker internally.
 */
public abstract class TemplateResolverDependencies {

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateLoaderSetting()} return {@code false}.
     */
    public abstract TemplateLoader getTemplateLoader();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateCacheStorageSetting()} return {@code false}.
     */
    public abstract CacheStorage getTemplateCacheStorage();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateLookupStrategySetting()} return
     * {@code false}.
     */
    public abstract TemplateLookupStrategy getTemplateLookupStrategy();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateNameFormatSetting()} return
     * {@code false}.
     */
    public abstract TemplateNameFormat getTemplateNameFormat();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateConfigurationsSetting()}
     * return {@code false}.
     */
    public abstract TemplateConfigurationFactory getTemplateConfigurations();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsTemplateUpdateDelayMillisecondsSetting()}
     * return {@code false}.
     */
    public abstract Long getTemplateUpdateDelayMilliseconds();

    /**
     * @throws IllegalStateException if {@link TemplateResolver#supportsLocalizedTemplateLookupSetting()} return {@code false}.
     */
    public abstract Boolean getLocalizedTemplateLookup();

    public abstract Charset getSourceEncoding();

    public abstract TemplateLanguage getTemplateLanguage();

    /**
     * Like the similar {@link Template} constructor, but as it has no {@link Configuration} parameter,
     * it avoids exposing the {@link Configuration} to the {@link TemplateResolverDependencies} implementation.
     */
    public abstract Template newTemplate(
            String lookupName, String sourceName,
            InputStream inputStream, Charset initialEncoding,
            TemplateConfiguration templateConfiguration) throws IOException, ParseException;

    /**
     * Like the similar {@link Template} constructor, but as it has no {@link Configuration} parameter,
     * it avoids exposing the {@link Configuration} to the {@link TemplateResolverDependencies} implementation.
     */
    public abstract Template newTemplate(
            String lookupName, String sourceName,
            Reader reader,
            TemplateConfiguration templateConfiguration) throws IOException, ParseException;
    
}

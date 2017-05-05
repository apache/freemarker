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

package org.apache.freemarker.test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.Configuration.Builder;
import org.apache.freemarker.core.Version;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ByteArrayTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.ClassTemplateLoader;
import org.apache.freemarker.core.templateresolver.impl.MultiTemplateLoader;

/**
 * Configuration builder you should use instead of {@link Builder} in unit tests.
 * It tries to make the behavior of the tests independent of the environment where we run them. For convenience, it
 * has a {@link ByteArrayTemplateLoader} as the default template loader.
 */
public class TestConfigurationBuilder extends Configuration.ExtendableBuilder<TestConfigurationBuilder> {

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("GMT+1");
    private final Class<?> classTemplateLoaderBase;
    private TemplateLoader defaultTemplateLoader;

    public TestConfigurationBuilder() {
        this((Version) null);
    }

    public TestConfigurationBuilder(Class<?> classTemplateLoaderBase) {
        this(null, classTemplateLoaderBase);
    }

    public TestConfigurationBuilder(Version incompatibleImprovements) {
        this(incompatibleImprovements, null);
    }

    public TestConfigurationBuilder(Version incompatibleImprovements, Class<?> classTemplateLoaderBase) {
        super(incompatibleImprovements != null ? incompatibleImprovements : Configuration.VERSION_3_0_0);
        this.classTemplateLoaderBase = classTemplateLoaderBase;
    }

    @Override
    protected Locale getDefaultLocale() {
        return Locale.US;
    }

    @Override
    protected Charset getDefaultSourceEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    protected TimeZone getDefaultTimeZone() {
        return DEFAULT_TIME_ZONE;
    }

    @Override
    protected TemplateLoader getDefaultTemplateLoader() {
        if (defaultTemplateLoader == null) {
            if (classTemplateLoaderBase == null) {
                defaultTemplateLoader = new ByteArrayTemplateLoader();
            } else {
                defaultTemplateLoader = new MultiTemplateLoader(
                        new ByteArrayTemplateLoader(),
                        new ClassTemplateLoader(classTemplateLoaderBase, ""));
            }
        }
        return defaultTemplateLoader;
    }

}

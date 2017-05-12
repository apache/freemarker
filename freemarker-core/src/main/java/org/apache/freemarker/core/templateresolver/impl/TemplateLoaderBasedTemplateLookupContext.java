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
package org.apache.freemarker.core.templateresolver.impl;

import java.io.Serializable;
import java.util.Locale;

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.apache.freemarker.core.templateresolver.TemplateLookupContext;

/**
 * Base class for implementing a {@link TemplateLookupContext} that works with {@link TemplateLoader}-s.
 */
public abstract class TemplateLoaderBasedTemplateLookupContext
        extends TemplateLookupContext<TemplateLoaderBasedTemplateLookupResult> {

    private final TemplateLoadingSource cachedResultSource;
    private final Serializable cachedResultVersion;

    protected TemplateLoaderBasedTemplateLookupContext(String templateName, Locale templateLocale,
            Object customLookupCondition, TemplateLoadingSource cachedResultSource, Serializable cachedResultVersion) {
        super(templateName, templateLocale, customLookupCondition);
        this.cachedResultSource = cachedResultSource;
        this.cachedResultVersion = cachedResultVersion;
    }
    
    protected TemplateLoadingSource getCachedResultSource() {
        return cachedResultSource;
    }

    protected Serializable getCachedResultVersion() {
        return cachedResultVersion;
    }

    @Override
    public final TemplateLoaderBasedTemplateLookupResult createNegativeLookupResult() {
        return TemplateLoaderBasedTemplateLookupResult.getNegativeResult();
    }

    /**
     * Creates a positive or negative lookup result depending on {@link TemplateLoadingResult#getStatus()}.
     */
    protected final TemplateLoaderBasedTemplateLookupResult createLookupResult(
            String templateSourceName, TemplateLoadingResult templateLoaderResult) {
        return TemplateLoaderBasedTemplateLookupResult.from(templateSourceName, templateLoaderResult);
    }
    
}

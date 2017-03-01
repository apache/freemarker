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
package freemarker.cache;

import java.io.IOException;

import freemarker.core.TemplateConfiguration;
import freemarker.template.Configuration;

/**
 * Returns the given {@link TemplateConfiguration} directly, or another {@link TemplateConfigurationFactory}'s result, when
 * the specified matcher matches the template source.
 * 
 * @since 2.3.24
 */
public class ConditionalTemplateConfigurationFactory extends TemplateConfigurationFactory {

    private final TemplateSourceMatcher matcher;
    private final TemplateConfiguration templateConfiguration;
    private final TemplateConfigurationFactory templateConfigurationFactory;

    public ConditionalTemplateConfigurationFactory(
            TemplateSourceMatcher matcher, TemplateConfigurationFactory templateConfigurationFactory) {
        this.matcher = matcher;
        this.templateConfiguration = null;
        this.templateConfigurationFactory = templateConfigurationFactory;
    }
    
    public ConditionalTemplateConfigurationFactory(
            TemplateSourceMatcher matcher, TemplateConfiguration templateConfiguration) {
        this.matcher = matcher;
        this.templateConfiguration = templateConfiguration;
        this.templateConfigurationFactory = null;
    }

    @Override
    public TemplateConfiguration get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurationFactoryException {
        if (matcher.matches(sourceName, templateSource)) {
            if (templateConfigurationFactory != null) {
                return templateConfigurationFactory.get(sourceName, templateSource);
            } else {
                return templateConfiguration;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void setConfigurationOfChildren(Configuration cfg) {
        if (templateConfiguration != null) {
            templateConfiguration.setParentConfiguration(cfg);
        }
        if (templateConfigurationFactory != null) {
            templateConfigurationFactory.setConfiguration(cfg);
        }
    }
    
}

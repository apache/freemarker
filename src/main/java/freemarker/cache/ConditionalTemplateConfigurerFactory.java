/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package freemarker.cache;

import java.io.IOException;

import freemarker.core.TemplateConfigurer;
import freemarker.template.Configuration;

/**
 * Returns the given {@link TemplateConfigurer} directly, or another {@link TemplateConfigurerFactory}'s result, when
 * the specified matcher matches the template source.
 * 
 * @since 2.3.24
 */
public class ConditionalTemplateConfigurerFactory extends TemplateConfigurerFactory {

    private final TemplateSourceMatcher matcher;
    private final TemplateConfigurer templateConfigurer;
    private final TemplateConfigurerFactory templateConfigurerFactory;

    public ConditionalTemplateConfigurerFactory(
            TemplateSourceMatcher matcher, TemplateConfigurerFactory templateConfigurerFactory) {
        this.matcher = matcher;
        this.templateConfigurer = null;
        this.templateConfigurerFactory = templateConfigurerFactory;
    }
    
    public ConditionalTemplateConfigurerFactory(TemplateSourceMatcher matcher, TemplateConfigurer templateConfigurer) {
        this.matcher = matcher;
        this.templateConfigurer = templateConfigurer;
        this.templateConfigurerFactory = null;
    }

    @Override
    public TemplateConfigurer get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurerFactoryException {
        if (matcher.matches(sourceName, templateSource)) {
            if (templateConfigurerFactory != null) {
                return templateConfigurerFactory.get(sourceName, templateSource);
            } else {
                return templateConfigurer;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void setConfigurationOfChildren(Configuration cfg) {
        if (templateConfigurer != null) {
            templateConfigurer.setParentConfiguration(cfg);
        }
        if (templateConfigurerFactory != null) {
            templateConfigurerFactory.setConfiguration(cfg);
        }
    }
    
}

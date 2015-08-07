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

/**
 * Returns the merged results of all the child factories. The factories are merged in the order as they were added.
 * {@code null} results from the child factories will be ignored. If all child factories return {@code null}, the result
 * of this factory will be {@code null} too.
 * 
 * @since 2.3.24
 */
public class MergingTemplateConfigurerFactory extends TemplateConfigurerFactory {
    
    private final TemplateConfigurerFactory[] templateConfigurerFactories;
    
    public MergingTemplateConfigurerFactory(TemplateConfigurerFactory... templateConfigurerFactories) {
        this.templateConfigurerFactories = templateConfigurerFactories;
    }

    @Override
    public TemplateConfigurer get(String sourceName, Object templateSource)
            throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer mergedTC = null;
        TemplateConfigurer resultTC = null;
        for (TemplateConfigurerFactory tcf : templateConfigurerFactories) {
            TemplateConfigurer tc = tcf.get(sourceName, templateSource);
            if (tc != null) {
                if (resultTC == null) {
                    resultTC = tc;
                } else {
                    if (mergedTC == null) {
                        mergedTC = new TemplateConfigurer();
                        mergedTC.merge(resultTC);
                        resultTC = mergedTC;
                    }
                    mergedTC.merge(tc);
                }
            }
        }
        return resultTC;
    }

}

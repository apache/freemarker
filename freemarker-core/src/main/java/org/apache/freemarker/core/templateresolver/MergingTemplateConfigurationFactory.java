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
package org.apache.freemarker.core.templateresolver;

import java.io.IOException;

import org.apache.freemarker.core.TemplateConfiguration;

/**
 * Returns the merged results of all the child factories. The factories are merged in the order as they were added.
 * {@code null} results from the child factories will be ignored. If all child factories return {@code null}, the result
 * of this factory will be {@code null} too.
 */
public class MergingTemplateConfigurationFactory extends TemplateConfigurationFactory {
    
    private final TemplateConfigurationFactory[] templateConfigurationFactories;
    
    public MergingTemplateConfigurationFactory(TemplateConfigurationFactory... templateConfigurationFactories) {
        this.templateConfigurationFactories = templateConfigurationFactories;
    }

    @Override
    public TemplateConfiguration get(String sourceName, TemplateLoadingSource templateLoadingSource)
            throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration.Builder mergedTCBuilder = null;
        TemplateConfiguration firstResultTC = null;
        for (TemplateConfigurationFactory tcf : templateConfigurationFactories) {
            TemplateConfiguration tc = tcf.get(sourceName, templateLoadingSource);
            if (tc != null) {
                if (firstResultTC == null) {
                    firstResultTC = tc;
                } else {
                    if (mergedTCBuilder == null) {
                        mergedTCBuilder = new TemplateConfiguration.Builder();
                        mergedTCBuilder.merge(firstResultTC);
                    }
                    mergedTCBuilder.merge(tc);
                }
            }
        }

        return mergedTCBuilder == null ? firstResultTC /* Maybe null */ : mergedTCBuilder.build();
    }

}

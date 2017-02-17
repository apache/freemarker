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

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResultStatus;
import org.apache.freemarker.core.templateresolver.TemplateLookupResult;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * Class of {@link TemplateLookupResult} instances created by {@link TemplateLoaderBasedTemplateLookupContext}. To
 * create instances of this inside your own {@link TemplateLoaderBasedTemplateLookupContext} subclass, call
 * {@link TemplateLoaderBasedTemplateLookupContext#createLookupResult(String, TemplateLoadingResult)} and
 * {@link TemplateLoaderBasedTemplateLookupContext#createNegativeLookupResult()}. You should not try to create instances
 * anywhere else. Also, this class deliberately can't be subclassed (except inside FreeMarker).
 */
public abstract class TemplateLoaderBasedTemplateLookupResult extends TemplateLookupResult {
    
    /** Used internally to get a not-found result (currently just a static singleton). */
    static TemplateLoaderBasedTemplateLookupResult getNegativeResult() {
        return NegativeTemplateLookupResult.INSTANCE;
    }
    
    /** Used internally to create the appropriate kind of result from the parameters. */
    static TemplateLoaderBasedTemplateLookupResult from(String templateSourceName, TemplateLoadingResult templateLoaderResult) {
        return templateLoaderResult.getStatus() != TemplateLoadingResultStatus.NOT_FOUND
                ? new PositiveTemplateLookupResult(templateSourceName, templateLoaderResult)
                : getNegativeResult();
    }
    
    private TemplateLoaderBasedTemplateLookupResult() {
        //
    }
    
    /**
     * Used internally to extract the {@link TemplateLoadingResult}; {@code null} if {@link #isPositive()} is
     * {@code false}.
     */
    public abstract TemplateLoadingResult getTemplateLoaderResult();

    private static final class PositiveTemplateLookupResult extends TemplateLoaderBasedTemplateLookupResult {

        private final String templateSourceName;
        private final TemplateLoadingResult templateLoaderResult;

        /**
         * @param templateSourceName
         *            The name of the matching template found. This is not necessarily the same as the template name
         *            with which the template was originally requested. For example, one may gets a template for the
         *            {@code "foo.ftl"} name, but due to localized lookup the template is actually loaded from
         *            {@code "foo_de.ftl"}. Then this parameter must be {@code "foo_de.ftl"}, not {@code "foo.ftl"}. Not
         *            {@code null}.
         * 
         * @param templateLoaderResult
         *            See {@link TemplateLoader#load} to understand what that means. Not
         *            {@code null}.
         */
        private PositiveTemplateLookupResult(String templateSourceName, TemplateLoadingResult templateLoaderResult) {
            _NullArgumentException.check("templateName", templateSourceName);
            _NullArgumentException.check("templateLoaderResult", templateLoaderResult);

            this.templateSourceName = templateSourceName;
            this.templateLoaderResult = templateLoaderResult;
        }

        @Override
        public String getTemplateSourceName() {
            return templateSourceName;
        }

        @Override
        public TemplateLoadingResult getTemplateLoaderResult() {
            return templateLoaderResult;
        }

        @Override
        public boolean isPositive() {
            return true;
        }
    }

    private static final class NegativeTemplateLookupResult extends TemplateLoaderBasedTemplateLookupResult {
        
        private static final TemplateLoaderBasedTemplateLookupResult.NegativeTemplateLookupResult INSTANCE = new NegativeTemplateLookupResult();
                
        private NegativeTemplateLookupResult() {
            // nop
        }

        @Override
        public String getTemplateSourceName() {
            return null;
        }

        @Override
        public TemplateLoadingResult getTemplateLoaderResult() {
            return null;
        }

        @Override
        public boolean isPositive() {
            return false;
        }
        
    }

}
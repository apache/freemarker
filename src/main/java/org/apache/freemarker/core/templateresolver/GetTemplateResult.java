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

import java.util.Locale;

import org.apache.freemarker.core.Template;

/**
 * Used for the return value of {@link TemplateResolver#getTemplate(String, Locale, Object, String)}.
 * 
 * @since 3.0.0
 */
//TODO DRAFT only [FM3]
public final class GetTemplateResult {
    
    private final Template template;
    private final String missingTemplateNormalizedName;
    private final String missingTemplateReason;
    private final Exception missingTemplateCauseException;
    
    public GetTemplateResult(Template template) {
        this.template = template;
        missingTemplateNormalizedName = null;
        missingTemplateReason = null;
        missingTemplateCauseException = null;
    }
    
    public GetTemplateResult(String normalizedName, Exception missingTemplateCauseException) {
        template = null;
        missingTemplateNormalizedName = normalizedName;
        missingTemplateReason = null;
        this.missingTemplateCauseException = missingTemplateCauseException;
    }
    
    public GetTemplateResult(String normalizedName, String missingTemplateReason) {
        template = null;
        missingTemplateNormalizedName = normalizedName;
        this.missingTemplateReason = missingTemplateReason;
        missingTemplateCauseException = null;
    }
    
    /**
     * The {@link Template} if it wasn't missing, otherwise {@code null}.
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * When the template was missing, this <em>possibly</em> contains the explanation, or {@code null}. If the
     * template wasn't missing (i.e., when {@link #getTemplate()} return non-{@code null}) this is always
     * {@code null}.
     */
    public String getMissingTemplateReason() {
        return missingTemplateReason != null
                ? missingTemplateReason
                : (missingTemplateCauseException != null
                        ? missingTemplateCauseException.getMessage()
                        : null);
    }
    
    /**
     * When the template was missing, this <em>possibly</em> contains its normalized name. If the template wasn't
     * missing (i.e., when {@link #getTemplate()} return non-{@code null}) this is always {@code null}. When the
     * template is missing, it will be {@code null} for example if the normalization itself was unsuccessful.
     */
    public String getMissingTemplateNormalizedName() {
        return missingTemplateNormalizedName;
    }
    
}
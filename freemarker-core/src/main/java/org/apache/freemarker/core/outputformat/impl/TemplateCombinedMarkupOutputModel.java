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
package org.apache.freemarker.core.outputformat.impl;

import org.apache.freemarker.core.outputformat.CommonTemplateMarkupOutputModel;

/**
 * Stores combined markup to be printed; used with {@link CombinedMarkupOutputFormat}.
 */
public final class TemplateCombinedMarkupOutputModel
        extends CommonTemplateMarkupOutputModel<TemplateCombinedMarkupOutputModel> {
    
    private final CombinedMarkupOutputFormat outputFormat;
    
    /**
     * See {@link CommonTemplateMarkupOutputModel#CommonTemplateMarkupOutputModel(String, String)}.
     * 
     * @param outputFormat
     *            The {@link CombinedMarkupOutputFormat} format this value is bound to. Because
     *            {@link CombinedMarkupOutputFormat} has no singleton, we have to pass it in, unlike with most other
     *            {@link CommonTemplateMarkupOutputModel}-s.
     */
    TemplateCombinedMarkupOutputModel(String plainTextContent, String markupContent,
            CombinedMarkupOutputFormat outputFormat) {
        super(plainTextContent, markupContent);
        this.outputFormat = outputFormat; 
    }

    @Override
    public CombinedMarkupOutputFormat getOutputFormat() {
        return outputFormat;
    }

}

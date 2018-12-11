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

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.outputformat.CommonMarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util._StringUtils;

/**
 * Represents the Rich Text Format output format (MIME type "application/rtf", name "RTF"). This format escapes by
 * default (via {@link _StringUtils#RTFEnc(String)}). The {@code ?rtf} built-in silently bypasses template output values
 * of the type produced by this output format ({@link TemplateRTFOutputModel}).
 */
public final class RTFOutputFormat extends CommonMarkupOutputFormat<TemplateRTFOutputModel> {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final RTFOutputFormat INSTANCE = new RTFOutputFormat();
    
    private RTFOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "RTF";
    }

    @Override
    public String getMimeType() {
        return "application/rtf";
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateException {
        _StringUtils.RTFEnc(textToEsc, out);
    }

    @Override
    public String escapePlainText(String plainTextContent) {
        return _StringUtils.RTFEnc(plainTextContent);
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("rtf");
    }

    @Override
    protected TemplateRTFOutputModel newTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        return new TemplateRTFOutputModel(plainTextContent, markupContent);
    }

}

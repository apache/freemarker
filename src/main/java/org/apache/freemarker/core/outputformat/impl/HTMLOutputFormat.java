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

import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.outputformat.CommonMarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util._StringUtil;

/**
 * Represents the HTML output format (MIME type "text/html", name "HTML"). This format escapes by default (via
 * {@link _StringUtil#XHTMLEnc(String)}). The {@code ?html}, {@code ?xhtml} and {@code ?xml} built-ins silently bypass
 * template output values of the type produced by this output format ({@link TemplateHTMLOutputModel}).
 * 
 * @since 2.3.24
 */
public final class HTMLOutputFormat extends CommonMarkupOutputFormat<TemplateHTMLOutputModel> {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final HTMLOutputFormat INSTANCE = new HTMLOutputFormat();
    
    private HTMLOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "HTML";
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        _StringUtil.XHTMLEnc(textToEsc, out);
    }

    @Override
    public String escapePlainText(String plainTextContent) {
        return _StringUtil.XHTMLEnc(plainTextContent);
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("html") || builtInName.equals("xml") || builtInName.equals("xhtml");
    }

    @Override
    protected TemplateHTMLOutputModel newTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        return new TemplateHTMLOutputModel(plainTextContent, markupContent);
    }

}

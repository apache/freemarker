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
 * Represents the XML output format (MIME type "application/xhtml+xml", name "XHTML"). This format escapes by default
 * (via {@link _StringUtils#XHTMLEnc(String)}). The {@code ?xml} built-in silently bypasses template output values of the
 * type produced by this output format ({@link TemplateXHTMLOutputModel}).
 */
public final class XHTMLOutputFormat extends CommonMarkupOutputFormat<TemplateXHTMLOutputModel> {

    /**
     * The only instance (singleton) of this {@link OutputFormat}.
     */
    public static final XHTMLOutputFormat INSTANCE = new XHTMLOutputFormat();
    
    private XHTMLOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public String getName() {
        return "XHTML";
    }

    @Override
    public String getMimeType() {
        return "application/xhtml+xml";
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateException {
        _StringUtils.XHTMLEnc(textToEsc, out);
    }

    @Override
    public String escapePlainText(String plainTextContent) {
        return _StringUtils.XHTMLEnc(plainTextContent);
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("html") || builtInName.equals("xml") || builtInName.equals("xhtml");
    }

    @Override
    protected TemplateXHTMLOutputModel newTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        return new TemplateXHTMLOutputModel(plainTextContent, markupContent);
    }

}

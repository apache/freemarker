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
package org.apache.freemarker.core.userpkg;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.outputformat.CommonMarkupOutputFormat;
import org.apache.freemarker.core.util._StringUtils;

/**
 * Represents the HTML output format.
 */
public final class CustomHTMLOutputFormat extends CommonMarkupOutputFormat<CustomTemplateHTMLModel> {

    public static final CustomHTMLOutputFormat INSTANCE = new CustomHTMLOutputFormat();
    
    private CustomHTMLOutputFormat() {
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
    public void output(String textToEsc, Writer out) throws IOException {
        // This is lazy - don't do it in reality.
        out.write(escapePlainText(textToEsc));
    }

    @Override
    public String escapePlainText(String plainTextContent) {
        return _StringUtils.XHTMLEnc(plainTextContent.replace('x', 'X'));
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("html") || builtInName.equals("xml") || builtInName.equals("xhtml");
    }

    @Override
    protected CustomTemplateHTMLModel newTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        return new CustomTemplateHTMLModel(plainTextContent, markupContent);
    }

}

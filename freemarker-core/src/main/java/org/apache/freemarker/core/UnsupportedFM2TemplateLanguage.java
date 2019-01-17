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

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * FreeMarker 2 template language, which we don't support. To avoid confusion we ban loading such templates. 
 */
class UnsupportedFM2TemplateLanguage extends TemplateLanguage {

    private static final UnsupportedFM2TemplateLanguage FTL = new UnsupportedFM2TemplateLanguage(
            "ftl", null, AutoEscapingPolicy.ENABLE_IF_DEFAULT);  
    private static final UnsupportedFM2TemplateLanguage FTLH = new UnsupportedFM2TemplateLanguage(
            "ftlh", null, AutoEscapingPolicy.ENABLE_IF_DEFAULT);  
    private static final UnsupportedFM2TemplateLanguage FTLX = new UnsupportedFM2TemplateLanguage(
            "ftlx", null, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    
    static final UnsupportedFM2TemplateLanguage[] INSTANCES =
            new UnsupportedFM2TemplateLanguage[] { FTL, FTLH, FTLX };
    
    private UnsupportedFM2TemplateLanguage(String fileExtension, OutputFormat outputFormat,
            AutoEscapingPolicy autoEscapingPolicy) {
        super(fileExtension, true, DefaultDialect.INSTANCE, outputFormat, autoEscapingPolicy);
    }

    @Override
    public boolean getCanSpecifyEncodingInContent() {
        return false;
    }

    @Override
    public ASTElement parse(Template template, Reader reader, ParsingConfiguration pCfg,
            InputStream streamToUnmarkWhenEncEstabd) throws IOException, ParseException {
        throw new ParseException(
                "FreeMarker 2 templates (*." + getFileExtension() + ") aren't supported in FreeMarker 3. "
                + "(Note that FreeMarker 2 and 3 uses different Java packages, so you can use both in the same "
                + "application.)",
                template, 0, 0, 0, 0);
    }

}

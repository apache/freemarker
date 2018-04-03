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
import java.nio.charset.Charset;

/**
 * Static text template language; a such template prints its content as is. 
 */
final class StaticTextTemplateLanguage extends TemplateLanguage {
    
    public static final TemplateLanguage INSTANCE = new StaticTextTemplateLanguage("Static text");

    private StaticTextTemplateLanguage(String name) {
        super(name);
    }

    @Override
    public boolean getCanSpecifyCharsetInContent() {
        return false;
    }

    @Override
    public Template parse(String name, String sourceName, Reader reader, Configuration cfg,
            TemplateConfiguration templateConfiguration, Charset sourceEncoding,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        // Read the contents into a StringWriter, then construct a single-text-block template from it.
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[4096];
        int charsRead;
        while ((charsRead = reader.read(buf)) > 0) {
            sb.append(buf, 0, charsRead);
        }
        return Template.createPlainTextTemplate(name, sourceName, sb.toString(), cfg,
                sourceEncoding);
    }
}
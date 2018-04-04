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
import java.io.StringReader;

import org.apache.freemarker.core.outputformat.OutputFormat;

/**
 * Static text template language; a such template prints its content as is.
 */
public final class StaticTextTemplateLanguage extends TemplateLanguage {
    
    private static final int READ_BUFFER_SIZE = 4096;
    
    public static final TemplateLanguage INSTANCE = new StaticTextTemplateLanguage("Static text");

    private StaticTextTemplateLanguage(String name) {
        super(name, null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * In {@link StaticTextTemplateLanguage} this returns {@code false}.
     */
    @Override
    public boolean getCanSpecifyEncodingInContent() {
        return false;
    }

    @Override
    public ASTElement parse(Template template, Reader reader,
            ParsingConfiguration pCfg, OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[READ_BUFFER_SIZE];
        int charsRead;
        while ((charsRead = reader.read(buf)) > 0) {
            sb.append(buf, 0, charsRead);
        }
        
        FMParser parser = new FMParser(
                template, new StringReader(""),
                pCfg,
                contextOutputFormat,
                contextAutoEscapingPolicy,
                streamToUnmarkWhenEncEstabd);
        
        ASTElement root = parser.Root();
        ((ASTStaticText) root).replaceText(sb.toString());
        return root;
    }
    
}
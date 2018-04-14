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
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;

/**
 * When the whole template is just raw static text (there are no special symbols in it), that will be printed as is.
 */
public final class UnparsedTemplateLanguage extends TemplateLanguage {
    
    private static final int READ_BUFFER_SIZE = 4096;
    
    /**
     * Instance with {@link UndefinedOutputFormat} output format. 
     */
    public static final UnparsedTemplateLanguage F3UU = new UnparsedTemplateLanguage(
            "f3uu", true, UndefinedOutputFormat.INSTANCE);

    public UnparsedTemplateLanguage(String fileExtension, OutputFormat outputFormat) {
        this(fileExtension, false, outputFormat);
    }
    
    private UnparsedTemplateLanguage(String fileExtension, boolean allowExtensionStartingWithF,
            OutputFormat outputFormat) {
        super(fileExtension, allowExtensionStartingWithF, outputFormat, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    }

    /**
     * {@inheritDoc}
     * 
     * In {@link UnparsedTemplateLanguage} this returns {@code false}.
     */
    @Override
    public boolean getCanSpecifyEncodingInContent() {
        return false;
    }

    @Override
    public ASTElement parse(Template template, Reader reader,
            ParsingConfiguration pCfg,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        final StringBuilder sb = new StringBuilder();
        final char[] buf = new char[READ_BUFFER_SIZE];
        int charsRead;
        while ((charsRead = reader.read(buf)) > 0) {
            sb.append(buf, 0, charsRead);
        }
        ASTStaticText root = new ASTStaticText(sb.toString());
        root.setFieldsForRootElement();
        root.setLocation(template, 0, 0, 0, 0); // TODO [FM3] Positions are dummy
        
        return root;
    }
    
}
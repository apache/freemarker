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

package org.apache.freemarker.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResultStatus;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;

/**
 * Removes the Apache copyright boiler plate from the beginning of the template, so that they don't mess up the expected
 * template output. This can interfere with tests that try to test I/O errors and such low level things, so use with
 * care. 
 */
public class CopyrightCommentRemoverTemplateLoader implements TemplateLoader {

    private final TemplateLoader innerTemplateLoader;

    public CopyrightCommentRemoverTemplateLoader(TemplateLoader innerTemplateLoader) {
        this.innerTemplateLoader = innerTemplateLoader;
    }

    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        TemplateLoadingResult result = innerTemplateLoader.load(name, ifSourceDiffersFrom, ifVersionDiffersFrom, session);
        if (result.getStatus() != TemplateLoadingResultStatus.OPENED) {
            return result;
        }
        if (result.getInputStream() != null) {
            return new TemplateLoadingResult(
                    result.getSource(), result.getVersion(), getWithoutCopyrightHeader(result.getInputStream()),
                    result.getTemplateConfiguration());
        } else {
            return new TemplateLoadingResult(
                    result.getSource(), result.getVersion(), getWithoutCopyrightHeader(result.getReader()),
                    result.getTemplateConfiguration());
        }
    }

    @Override
    public void resetState() {
        // Do nothing
    }

    private Reader getWithoutCopyrightHeader(Reader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        try {
            String content = IOUtils.toString(reader);
            return new StringReader(TestUtils.removeFTLCopyrightComment(content));
        } finally {
            reader.close();
        }
    }

    private InputStream getWithoutCopyrightHeader(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        try {
            // Encoding then decosing in ISO-8859-1 is binary loseless
            String content = IOUtils.toString(in, StandardCharsets.ISO_8859_1.name());
            return new ReaderInputStream(
                    new StringReader(TestUtils.removeFTLCopyrightComment(content)),
                    StandardCharsets.ISO_8859_1);
        } finally {
            in.close();
        }
    }
    
}

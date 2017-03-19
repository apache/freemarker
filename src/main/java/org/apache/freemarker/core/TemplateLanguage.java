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

import org.apache.freemarker.core.util._StringUtil;

/**
 * Represents a template language. Currently this class is not mature, so it can't be implemented outside FreeMarker,
 * also its methods shouldn't be called from outside FreeMarker.
 */
// [FM3] Make this mature, or hide its somehow. Actually, parse can't be hidden because custom TemplateResolver-s need
// to call it.
public abstract class TemplateLanguage {

    // FIXME [FM3] If we leave this here, FTL will be a required dependency of core (which is not nice if
    // template languages will be pluggable).
    public static final TemplateLanguage FTL = new TemplateLanguage("FreeMarker Template Language") {
        @Override
        public boolean getCanSpecifyCharsetInContent() {
            return true;
        }

        @Override
        public Template parse(String name, String sourceName, Reader reader, Configuration cfg, ParserConfiguration customParserConfiguration, String encoding, InputStream streamToUnmarkWhenEncEstabd) throws IOException, ParseException {
            return new Template(name, sourceName, reader, cfg, customParserConfiguration,
                    encoding, streamToUnmarkWhenEncEstabd);
        }
    };

    public static final TemplateLanguage STATIC_TEXT = new TemplateLanguage("Static text") {
        @Override
        public boolean getCanSpecifyCharsetInContent() {
            return false;
        }

        @Override
        public Template parse(String name, String sourceName, Reader reader, Configuration cfg, ParserConfiguration customParserConfiguration, String encoding, InputStream streamToUnmarkWhenEncEstabd) throws IOException, ParseException {
            // Read the contents into a StringWriter, then construct a single-text-block template from it.
            final StringBuilder sb = new StringBuilder();
            final char[] buf = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buf)) > 0) {
                sb.append(buf, 0, charsRead);
            }
            return Template.createPlainTextTemplate(name, sourceName, sb.toString(), cfg,
                    encoding);
        }
    };

    private final String name;

    // Package visibility to prevent user implementations until this API is mature.
    TemplateLanguage(String name) {
        this.name = name;
    }

    /**
     * Returns if the template can specify its own charset inside the template. If so, {@link #parse(String, String,
     * Reader, Configuration, ParserConfiguration, String, InputStream)} can throw
     * {@link WrongTemplateCharsetException}, and it might gets a non-{@code null} for the {@link InputStream}
     * parameter.
     */
    public abstract boolean getCanSpecifyCharsetInContent();

    /**
     * See {@link Template#Template(String, String, Reader, Configuration, ParserConfiguration, String, InputStream)}.
     */
    public abstract Template parse(String name, String sourceName, Reader reader,
                                   Configuration cfg, ParserConfiguration customParserConfiguration,
                                   String encoding, InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException;

    public String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return "TemplateLanguage(" + _StringUtil.jQuote(name) + ")";
    }

}

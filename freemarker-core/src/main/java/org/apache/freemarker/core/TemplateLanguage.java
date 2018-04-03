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

import org.apache.freemarker.core.util._StringUtils;

/**
 * Represents a template language. Currently this class is not mature, so it can't be implemented outside FreeMarker,
 * also its methods shouldn't be called from outside FreeMarker.
 */
// [FM3] Make this mature, or hide its somehow. Actually, parse can't be hidden because custom TemplateResolver-s need
// to call it.
public abstract class TemplateLanguage {
    
    private final String name;

    // Package visibility to prevent user implementations until this API is mature.
    TemplateLanguage(String name) {
        this.name = name;
    }

    /**
     * Returns if the template can specify its own charset inside the template. If so, {@link #parse(String, String,
     * Reader, Configuration, TemplateConfiguration, Charset, InputStream)} can throw
     * {@link WrongTemplateCharsetException}, and it might gets a non-{@code null} for the {@link InputStream}
     * parameter.
     */
    public abstract boolean getCanSpecifyCharsetInContent();

    /**
     * See {@link Template#Template(String, String, Reader, Configuration, TemplateConfiguration, Charset,
     * InputStream)}.
     */
    public abstract Template parse(String name, String sourceName, Reader reader,
                                   Configuration cfg, TemplateConfiguration templateConfiguration,
                                   Charset encoding, InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException;

    public String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return "TemplateLanguage(" + _StringUtils.jQuote(name) + ")";
    }

}

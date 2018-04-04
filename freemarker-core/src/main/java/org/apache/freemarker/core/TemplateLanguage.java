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

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._StringUtils;

/**
 * Represents a template language. Currently this class is not mature, so it can't be implemented outside FreeMarker,
 * also its methods shouldn't be called from outside FreeMarker.
 */
// [TODO][FM3] Make this mature, or hide its API somehow, or at least prevent subclassing it by user.
public abstract class TemplateLanguage {
    
    private final String name;
    private final OutputFormat outputFormat;
    private final AutoEscapingPolicy autoEscapingPolicy;
    
    // Package visibility to prevent user implementations until this API is mature.

    /**
     * Returns if the template can specify its own charset inside the template. If so, the parser can throw
     * {@link WrongTemplateCharsetException}, and it might gets a non-{@code null} for the {@link InputStream}
     * parameter.
     */
    public abstract boolean getCanSpecifyEncodingInContent();

    /**
     * @param name See {@link #getName()}
     * @param outputFormat See {@link #getOutputFormat(Configuration)}
     * @param autoEscapingPolicy See {@link #getAutoEscapingPolicy()}
     */
    public TemplateLanguage(String name, OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy) {
        _NullArgumentException.check("name", name);
        this.name = name;
        this.outputFormat = outputFormat;
        this.autoEscapingPolicy = autoEscapingPolicy;
    }

    /**
     * This is what
     * {@link Template#Template(String, String, InputStream, Charset, Reader, Configuration, TemplateConfiguration,
     * OutputFormat, AutoEscapingPolicy)} calls internally to do the actual parsing.
     */
    // TODO [FM3] This is not the final API, or else it can't be public (because ASTElement isn't public for example).
    public abstract ASTElement parse(Template template, Reader reader,
            ParsingConfiguration pCfg, OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException;

    /**
     * The informal name of this language; might be used in error messages. Not {@code null}.
     */
    public String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return "TemplateLanguage(" + _StringUtils.jQuote(name) + ")";
    }
    
    /**
     * The {@link OutputFormat} that this language enforces, or else {@code null}
     * 
     * @param cfg
     *            The {@link Configuration} used; this is needed for {@link Configuration}-level {@link OutputFormat}
     *            overrides to work (such as when someone provides an alternative implementation for "HTML" escaping).
     *            If this is {@code null}, you get back the original {@link OutputFormat} object of this
     *            {@link TemplateLanguage}, but you hardly ever should do that. 
     */
    public OutputFormat getOutputFormat(Configuration cfg) {
        return cfg != null && outputFormat != null ? cfg.getCustomOrArgumentOutputFormat(outputFormat) : outputFormat;
    }

    /**
     * The {@link OutputFormat} that this language enforces, or else {@code null}
     */
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        return autoEscapingPolicy;
    }

}

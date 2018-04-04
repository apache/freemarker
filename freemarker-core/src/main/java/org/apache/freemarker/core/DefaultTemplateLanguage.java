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
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;

// FIXME [FM3] If we leave this here, FTL will be a required dependency of core (which is not nice if
// template languages will be pluggable).
public final class DefaultTemplateLanguage extends TemplateLanguage {

    private final TagSyntax tagSyntax;
    private final InterpolationSyntax interpolationSyntax;

    /**
     * For the case when the file extension doesn't specify the exact syntax and the output format, instead both comes
     * from the {@link Configuration} (or {@link TemplateConfiguration}). Avoid it, as it's problematic for tooling.
     */
    public static final TemplateLanguage F3CC = new DefaultTemplateLanguage("f3cc", null, null, null, null);

    // TODO [FM3][CF] Temporary solution, will be removed
    public static final TemplateLanguage F3CH = new DefaultTemplateLanguage("ftlh",
            null, null,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    
    // TODO [FM3][CF] Temporary solution, will be removed
    public static final TemplateLanguage F3CX = new DefaultTemplateLanguage("ftlx",
            null, null,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    
    public static final TemplateLanguage F3AH = new DefaultTemplateLanguage("f3ah",
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    public static final TemplateLanguage F3AX = new DefaultTemplateLanguage("f3ax",
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    public static final TemplateLanguage F3AU = new DefaultTemplateLanguage("f3au",
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);

    public static final TemplateLanguage F3SH = new DefaultTemplateLanguage("f3sh",
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    public static final TemplateLanguage F3SX = new DefaultTemplateLanguage("f3sx",
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);
    public static final TemplateLanguage F3SU = new DefaultTemplateLanguage("f3su",
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT);

    /**
     * Creates a new template language that's similar to the usual FreeMarker template language. You seldom need to
     * call this yourself; use constants like {@link #F3AH} instead when possible.
     * 
     * @param name
     *            The name of the template language
     * @param tagSyntax
     *            The tag syntax used, or {@code null} it it should come from the {@link ParsingConfiguration}. Using
     *            {@code null} is generally a bad idea, as it makes tooling harder, and can be confusing for users.
     * @param interpolationSyntax
     *            The interpolation syntax used, or {@code null} it it should come from the
     *            {@link ParsingConfiguration}. Using {@code null} is generally a bad idea, as it makes tooling harder,
     *            and can be confusing for users.
     * @param outputFormat
     *            The output format used, or {@code null} it it should come from the {@link ParsingConfiguration}. Using
     *            {@code null} is generally a bad idea, as it makes tooling harder, and can be confusing for users.
     * @param autoEscapingPolicy
     *            The auto escaping policy used, or {@code null} it it should come from the
     *            {@link ParsingConfiguration}. Using {@code null} is generally a bad idea, as it can be confusing for
     *            users.
     */
    public DefaultTemplateLanguage(
            String name,
            TagSyntax tagSyntax, InterpolationSyntax interpolationSyntax,
            OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy) {
        super(name, outputFormat, autoEscapingPolicy);
        this.tagSyntax = tagSyntax;
        this.interpolationSyntax = interpolationSyntax;
    }

    /**
     * {@inheritDoc}
     * 
     * In {@link DefaultTemplateLanguage} this returns {@code true}.
     */
    @Override
    public boolean getCanSpecifyEncodingInContent() {
        return true;
    }

    @Override
    public ASTElement parse(Template template, Reader reader,
            ParsingConfiguration pCfg, OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        FMParser parser = new FMParser(
                template, reader,
                pCfg,
                contextOutputFormat,
                contextAutoEscapingPolicy,
                streamToUnmarkWhenEncEstabd);
        ASTElement root = parser.Root();
        template.actualTagSyntax = parser._getLastTagSyntax();
        return root;
    }

    public TagSyntax getTagSyntax() {
        return tagSyntax;
    }

    public InterpolationSyntax getInterpolationSyntax() {
        return interpolationSyntax;
    }
    
}
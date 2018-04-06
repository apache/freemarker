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
    public static final DefaultTemplateLanguage F3CC = new DefaultTemplateLanguage("f3cc", true,
            null, null, null, null);

    // TODO [FM3][CF] Emulates FM2 behavior temporarily, to make the test suite pass 
    public static final DefaultTemplateLanguage F3CH = new DefaultTemplateLanguage("ftlh", true,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            null, null);
    
    // TODO [FM3][CF] Emulates FM2 behavior temporarily, to make the test suite pass 
    public static final DefaultTemplateLanguage F3CX = new DefaultTemplateLanguage("ftlx", true,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            null, null);
    
    public static final DefaultTemplateLanguage F3AH = new DefaultTemplateLanguage("f3ah", true,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);
    public static final DefaultTemplateLanguage F3AX = new DefaultTemplateLanguage("f3ax", true,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);
    public static final DefaultTemplateLanguage F3AU = new DefaultTemplateLanguage("f3au", true,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);

    public static final DefaultTemplateLanguage F3SH = new DefaultTemplateLanguage("f3sh", true,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);
    public static final DefaultTemplateLanguage F3SX = new DefaultTemplateLanguage("f3sx", true,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);
    public static final DefaultTemplateLanguage F3SU = new DefaultTemplateLanguage("f3su", true,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);

    /**
     * List all instances for which there's a constant in this class ({@link #F3AH} and such).
     */
    public static final DefaultTemplateLanguage[] STANDARD_INSTANCES = new DefaultTemplateLanguage[] {
            F3CC, F3CH, F3CX,
            F3AH, F3AX, F3AU, 
            F3SH, F3SX, F3SU 
    };
    
    /**
     * Creates a new template language that's similar to the usual FreeMarker template language. You seldom need to
     * call this yourself; use constants like {@link #F3AH} instead when possible.
     * 
     * @param fileExtension
     *            See in {@link TemplateLanguage#TemplateLanguage(String, OutputFormat, AutoEscapingPolicy)}
     * @param outputFormat
     *            See in {@link TemplateLanguage#TemplateLanguage(String, OutputFormat, AutoEscapingPolicy)}
     * @param autoEscapingPolicy
     *            See in {@link TemplateLanguage#TemplateLanguage(String, OutputFormat, AutoEscapingPolicy)}
     * @param tagSyntax
     *            The tag syntax used, or {@code null} it it should come from the {@link ParsingConfiguration}. Using
     *            {@code null} is generally a bad idea, as it makes tooling harder, and can be confusing for users.
     * @param interpolationSyntax
     *            The interpolation syntax used, or {@code null} it it should come from the
     *            {@link ParsingConfiguration}. Using {@code null} is generally a bad idea, as it makes tooling harder,
     *            and can be confusing for users.
     */
    public DefaultTemplateLanguage(
            String fileExtension,
            OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy,
            TagSyntax tagSyntax, InterpolationSyntax interpolationSyntax) {
        this(fileExtension, false, outputFormat, autoEscapingPolicy, tagSyntax, interpolationSyntax);
    }

    /**
     * Used internally to allow extensions starting with "f" 
     */
    DefaultTemplateLanguage(
            String fileExtension,
            boolean allowExtensionStartingWithF,
            OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy,
            TagSyntax tagSyntax, InterpolationSyntax interpolationSyntax) {
        super(fileExtension, allowExtensionStartingWithF, outputFormat, autoEscapingPolicy);
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
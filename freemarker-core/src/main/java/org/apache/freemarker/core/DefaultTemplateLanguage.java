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
import org.apache.freemarker.core.util._NullArgumentException;

// FIXME [FM3] If we leave this here, FTL will be a required dependency of core (which is not nice if
// template languages will be pluggable).
public final class DefaultTemplateLanguage extends TemplateLanguage {

    private final TagSyntax tagSyntax;
    private final InterpolationSyntax interpolationSyntax;
    
    /**
     * For the case when the file extension doesn't specify the exact syntax and the output format, instead both comes
     * from the {@link Configuration} (or {@link TemplateConfiguration}). Avoid it, as it's problematic for tooling.
     */
    public static final DefaultTemplateLanguage F3AC = new DefaultTemplateLanguage("f3ac", true,
            DefaultDialect.INSTANCE,
            null, null,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);
    public static final DefaultTemplateLanguage F3SC = new DefaultTemplateLanguage("f3sc", true,
            DefaultDialect.INSTANCE,
            null, null,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);

    public static final DefaultTemplateLanguage F3AH = new DefaultTemplateLanguage("f3ah", true,
            DefaultDialect.INSTANCE,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);
    public static final DefaultTemplateLanguage F3AX = new DefaultTemplateLanguage("f3ax", true,
            DefaultDialect.INSTANCE,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);
    public static final DefaultTemplateLanguage F3AU = new DefaultTemplateLanguage("f3au", true,
            DefaultDialect.INSTANCE,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.ANGLE_BRACKET, InterpolationSyntax.DOLLAR);

    public static final DefaultTemplateLanguage F3SH = new DefaultTemplateLanguage("f3sh", true,
            DefaultDialect.INSTANCE,
            HTMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);
    public static final DefaultTemplateLanguage F3SX = new DefaultTemplateLanguage("f3sx", true,
            DefaultDialect.INSTANCE,
            XMLOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);
    public static final DefaultTemplateLanguage F3SU = new DefaultTemplateLanguage("f3su", true,
            DefaultDialect.INSTANCE,
            UndefinedOutputFormat.INSTANCE, AutoEscapingPolicy.ENABLE_IF_DEFAULT,
            TagSyntax.SQUARE_BRACKET, InterpolationSyntax.SQUARE_BRACKET);

    /**
     * List all instances for which there's a constant in this class ({@link #F3AH} and such).
     */
    public static final DefaultTemplateLanguage[] STANDARD_INSTANCES = new DefaultTemplateLanguage[] {
            F3AC, F3SC,
            F3AH, F3AX, F3AU, 
            F3SH, F3SX, F3SU 
    };
    
    /**
     * Creates a new template language that's similar to the usual FreeMarker template language. You seldom need to
     * call this yourself; use constants like {@link #F3AH} instead when possible.
     * 
     * @param fileExtension
     *            See in {@link TemplateLanguage#TemplateLanguage(String, Dialect, OutputFormat, AutoEscapingPolicy)}
     * @param outputFormat
     *            See in {@link TemplateLanguage#TemplateLanguage(String, Dialect, OutputFormat, AutoEscapingPolicy)}
     * @param autoEscapingPolicy
     *            See in {@link TemplateLanguage#TemplateLanguage(String, Dialect, OutputFormat, AutoEscapingPolicy)}
     * @param tagSyntax
     *            The tag syntax used; not {@code null}.
     * @param interpolationSyntax
     *            The interpolation syntax used; not {@code null}.
     */
    public DefaultTemplateLanguage(
            String fileExtension,
            Dialect dialect,
            OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy,
            TagSyntax tagSyntax, InterpolationSyntax interpolationSyntax) {
        this(fileExtension, false, dialect, outputFormat, autoEscapingPolicy, tagSyntax, interpolationSyntax);
    }

    /**
     * Used internally to allow extensions starting with "f" (as those are reserved for FreeMarker) 
     */
    DefaultTemplateLanguage(
            String fileExtension, boolean allowExtensionStartingWithF,
            Dialect dialect,
            OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy,
            TagSyntax tagSyntax, InterpolationSyntax interpolationSyntax) {
        super(fileExtension, allowExtensionStartingWithF, dialect, outputFormat, autoEscapingPolicy);
        _NullArgumentException.check("tagSyntax", tagSyntax);
        _NullArgumentException.check("interpolationSyntax", interpolationSyntax);
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
            ParsingConfiguration pCfg,
            InputStream streamToUnmarkWhenEncEstabd)
            throws IOException, ParseException {
        FMParser parser = new FMParser(
                template, reader,
                pCfg,
                streamToUnmarkWhenEncEstabd);
        ASTElement root = parser.Root();
        template.actualTagSyntax = parser._getLastTagSyntax();
        return root;
    }

    /**
     * Determines the tag syntax (like {@code <#if x>} VS {@code [#if x]}) of the template files. Don't confuse this
     * with the interpolation syntax ({@link #getInterpolationSyntax()}); they are independent.
     * 
     * <p>The value is one of:
     * <ul>
     *   <li>{@link TagSyntax#ANGLE_BRACKET}:
     *     Use the angle bracket tag syntax (the normal syntax), like {@code <#include ...>}. This is the default.
     *   <li>{@link TagSyntax#SQUARE_BRACKET}:
     *     Use the square bracket tag syntax, like {@code [#include ...]}. Note that this does <em>not</em> change
     *     <code>${x}</code> to {@code [=...]}; that's <em>interpolation</em> syntax, so there the relevant one is
     *     {@link #getInterpolationSyntax()}.
     * </ul>
     * 
     * @return Not {@code null}
     */
    public TagSyntax getTagSyntax() {
        return tagSyntax;
    }

    /**
     * Determines the interpolation syntax (<code>${x}</code> VS <code>[=x]</code>) of the template files.
     * Don't confuse this with the tag syntax ({@link #getTagSyntax()}); they are independent.
     * Note that {@link InterpolationSyntax#SQUARE_BRACKET} does <em>not</em> change {@code <#if x>} to
     * {@code [#if x]}; that's <em>tag</em> syntax, so there the relevant one is {@link #getTagSyntax()}.
     *
     * @return Not {@code null}
     */
    public InterpolationSyntax getInterpolationSyntax() {
        return interpolationSyntax;
    }
    
}
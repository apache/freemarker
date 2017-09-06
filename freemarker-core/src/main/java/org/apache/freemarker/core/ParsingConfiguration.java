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

import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;

/**
 * Implemented by FreeMarker core classes (not by you) that provide configuration settings that affect template parsing
 * (as opposed to {@linkplain Template#process (Object, Writer) template processing}). <b>New methods may be added
 * any time in future FreeMarker versions, so don't try to implement this interface yourself!</b>
 *
 * @see ProcessingConfiguration
 * @see ParsingAndProcessingConfiguration
 */
public interface ParsingConfiguration {

    /**
     * The template language used; this is often overridden for certain file extension with the
     * {@link Configuration#getTemplateConfigurations() templateConfigurations} setting of the {@link Configuration}.
     */
    TemplateLanguage getTemplateLanguage();

    boolean isTemplateLanguageSet();

    /**
     * Determines the syntax of the template files (angle bracket VS square bracket)
     * that has no {@code #ftl} in it. The {@code tagSyntax}
     * parameter must be one of:
     * <ul>
     *   <li>{@link TagSyntax#AUTO_DETECT}:
     *     use the syntax of the first FreeMarker tag (can be anything, like <tt>#list</tt>,
     *     <tt>#include</tt>, user defined, etc.)
     *   <li>{@link TagSyntax#ANGLE_BRACKET}:
     *     use the angle bracket syntax (the normal syntax)
     *   <li>{@link TagSyntax#SQUARE_BRACKET}:
     *     use the square bracket syntax
     * </ul>
     *
     * <p>In FreeMarker 2.3.x {@link TagSyntax#ANGLE_BRACKET} is the
     * default for better backward compatibility. Starting from 2.4.x {@link
     * TagSyntax#AUTO_DETECT} is the default, so it's recommended to use
     * that even for 2.3.x.
     *
     * <p>This setting is ignored for the templates that have {@code ftl} directive in
     * it. For those templates the syntax used for the {@code ftl} directive determines
     * the syntax.
     */
    TagSyntax getTagSyntax();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isTagSyntaxSet();

    /**
     * Whether the template parser will try to remove superfluous white-space around certain tags.
     */
    boolean getWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isWhitespaceStrippingSet();

    /**
     * Overlaps with {@link ProcessingConfiguration#getArithmeticEngine()}; the parser needs this for creating numerical
     * literals.
     */
    ArithmeticEngine getArithmeticEngine();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * See {@link Configuration#getAutoEscapingPolicy()}.
     */
    AutoEscapingPolicy getAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isAutoEscapingPolicySet();

    /**
     * The output format to use, which among others influences auto-escaping (see {@link #getAutoEscapingPolicy}
     * autoEscapingPolicy}), and possibly the MIME type of the output.
     * <p>
     * On the {@link Configuration} level, usually, you should leave this on its default, which is
     * {@link UndefinedOutputFormat#INSTANCE}, and then use standard file extensions like "ftlh" (for HTML) or "ftlx"
     * (for XML) (and ensure that {@link #getRecognizeStandardFileExtensions() recognizeStandardFileExtensions} is
     * {@code true}; see more there). Where you can't use the standard extensions, templates still can be associated
     * to output formats with patterns matching their name (their path) using the
     * {@link Configuration#getTemplateConfigurations() templateConfigurations} setting of the {@link Configuration}.
     * But if all templates will have the same output format, you may set the
     * {@link #getOutputFormat() outputFormat} setting of the {@link Configuration}
     * after all, to a value like {@link HTMLOutputFormat#INSTANCE}, {@link XMLOutputFormat#INSTANCE}, etc. Also
     * note that templates can specify their own output format like {@code <#ftl outputFormat="HTML">}, which
     * overrides any configuration settings.
     *
     * @see Configuration#getRegisteredCustomOutputFormats()
     * @see Configuration#getTemplateConfigurations()
     * @see #getRecognizeStandardFileExtensions()
     * @see #getAutoEscapingPolicy()
     */
    OutputFormat getOutputFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isOutputFormatSet();

    /**
     * Tells if the "file" extension part of the source name ({@link Template#getSourceName()}) will influence certain
     * parsing settings. Defaults to {@code true}. When {@code true}, the following standard file extensions take
     * their effect:
     *
     * <ul>
     *   <li>{@code ftlh}: Sets the {@link #getOutputFormat() outputFormat} setting to {@code "HTML"}
     *       (i.e., {@link HTMLOutputFormat#INSTANCE}, unless the {@code "HTML"} name is overridden by
     *       the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} setting to
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}.
     *   <li>{@code ftlx}: Sets the {@link #getOutputFormat() outputFormat} setting to
     *       {@code "XML"} (i.e., {@link XMLOutputFormat#INSTANCE}, unless the {@code "XML"} name is overridden by
     *       the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} setting to
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}.
     * </ul>
     *
     * <p>These file extensions are not case sensitive. The file extension is the part after the last dot in the source
     * name. If the source name contains no dot, then it has no file extension.
     *
     * <p>The settings activated by these file extensions override the setting values dictated by the
     * {@link Configuration#getTemplateConfigurations templateConfigurations} setting of the {@link Configuration}.
     */
    boolean getRecognizeStandardFileExtensions();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isRecognizeStandardFileExtensionsSet();

    /**
     * See {@link TopLevelConfiguration#getIncompatibleImprovements()}; this is normally directly delegates to
     * {@link Configuration#getIncompatibleImprovements()}, and that's always set.
     */
    Version getIncompatibleImprovements();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isIncompatibleImprovementsSet();

    /**
     * The assumed display width of the tab character (ASCII 9), which influences the column number shown in error
     * messages (or the column number you get through other API-s). So for example if the users edit templates in an
     * editor where the tab width is set to 4, you should set this to 4 so that the column numbers printed by FreeMarker
     * will match the column number shown in the editor. This setting doesn't affect the output of templates, as a tab
     * in the template will remain a tab in the output too. The value of this setting is at least 1, and at most 256.
     * When it's 1, tab characters will be kept in the return value of {@link Template#getSource(int, int, int, int)},
     * otherwise they will be replaced with the appropriate number of spaces.
     */
    int getTabSize();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isTabSizeSet();

    /**
     * Sets the charset used for decoding template files.
     * <p>
     * Defaults to {@code "UTF-8"}. (On some desktop applications {@link Charset#defaultCharset()} is maybe preferable.)
     * <p>
     * When a project contains groups (like folders) of template files where the groups use different encodings,
     * consider using the {@link Configuration#getTemplateConfigurations() templateConfigurations} setting on the
     * {@link Configuration} level.
     * <p>
     * Individual templates may specify their own charset by starting with
     * <tt>&lt;#ftl sourceEncoding="..."&gt;</tt>. However, before that's detected, at least part of template must be
     * decoded with some charset first, so this setting (and
     * {@link Configuration#getTemplateConfigurations() templateConfigurations}) still have role.
     */
    Charset getSourceEncoding();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isSourceEncodingSet();

}

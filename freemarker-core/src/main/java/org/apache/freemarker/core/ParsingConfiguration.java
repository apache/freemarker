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

import java.nio.charset.Charset;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
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

    int AUTO_DETECT_NAMING_CONVENTION = 10;
    int LEGACY_NAMING_CONVENTION = 11;
    int CAMEL_CASE_NAMING_CONVENTION = 12;

    int AUTO_DETECT_TAG_SYNTAX = 0;
    int ANGLE_BRACKET_TAG_SYNTAX = 1;
    int SQUARE_BRACKET_TAG_SYNTAX = 2;

    /**
     * Don't enable auto-escaping, regardless of what the {@link OutputFormat} is. Note that a {@code
     * <#ftl auto_esc=true>} in the template will override this.
     */
    int DISABLE_AUTO_ESCAPING_POLICY = 20;
    /**
     * Enable auto-escaping if the output format supports it and {@link MarkupOutputFormat#isAutoEscapedByDefault()} is
     * {@code true}.
     */
    int ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY = 21;
    /** Enable auto-escaping if the {@link OutputFormat} supports it. */
    int ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY = 22;

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
     *   <li>{@link #AUTO_DETECT_TAG_SYNTAX}:
     *     use the syntax of the first FreeMarker tag (can be anything, like <tt>#list</tt>,
     *     <tt>#include</tt>, user defined, etc.)
     *   <li>{@link #ANGLE_BRACKET_TAG_SYNTAX}:
     *     use the angle bracket syntax (the normal syntax)
     *   <li>{@link #SQUARE_BRACKET_TAG_SYNTAX}:
     *     use the square bracket syntax
     * </ul>
     *
     * <p>In FreeMarker 2.3.x {@link #ANGLE_BRACKET_TAG_SYNTAX} is the
     * default for better backward compatibility. Starting from 2.4.x {@link
     * ParsingConfiguration#AUTO_DETECT_TAG_SYNTAX} is the default, so it's recommended to use
     * that even for 2.3.x.
     *
     * <p>This setting is ignored for the templates that have {@code ftl} directive in
     * it. For those templates the syntax used for the {@code ftl} directive determines
     * the syntax.
     */
    int getTagSyntax();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isTagSyntaxSet();

    /**
     * The naming convention used for the identifiers that are part of the template language. The available naming
     * conventions are legacy (directive (tag) names are all-lower-case {@code likethis}, others are snake case
     * {@code like_this}), and camel case ({@code likeThis}). The default is auto-detect, which detects the naming
     * convention used and enforces that same naming convention for the whole template.
     *
     * <p>
     * This setting doesn't influence what naming convention is used for the setting names outside templates. Also, it
     * won't ever convert the names of user-defined things, like of data-model members, or the names of user defined
     * macros/functions. It only influences the names of the built-in directives ({@code #elseIf} VS {@code elseif}),
     * built-ins ({@code ?upper_case} VS {@code ?upperCase} ), special variables ({@code .data_model} VS
     * {@code .dataModel}).
     *
     * <p>
     * Which convention to use: FreeMarker prior to 2.3.23 has only supported
     * {@link #LEGACY_NAMING_CONVENTION}, so that's how most templates and examples out there are written
     * as of 2015. But as templates today are mostly written by programmers and often access Java API-s which already
     * use camel case, {@link #CAMEL_CASE_NAMING_CONVENTION} is the recommended option for most projects.
     * However, it's no necessary to make a application-wide decision; see auto-detection below.
     *
     * <p>
     * FreeMarker will decide the naming convention automatically for each template individually when this setting is
     * set to {@link #AUTO_DETECT_NAMING_CONVENTION} (which is the default). The naming convention of a template is
     * decided when the first core (non-user-defined) identifier is met during parsing (not during processing) where the
     * naming convention is relevant (like for {@code s?upperCase} or {@code s?upper_case} it's relevant, but for
     * {@code s?length} it isn't). At that point, the naming convention of the template is decided, and any later core
     * identifier that uses a different convention will be a parsing error. As the naming convention is decided per
     * template, it's not a problem if a template and the other template it {@code #include}-s/{@code #import} uses a
     * different convention.
     *
     * <p>
     * FreeMarker always enforces the same naming convention to be used consistently within the same template "file".
     * Additionally, when this setting is set to non-{@link #AUTO_DETECT_NAMING_CONVENTION}, the selected naming
     * convention is enforced on all templates. Thus such a setup can be used to enforce an application-wide naming
     * convention.
     *
     * @return
     *            One of the {@link #AUTO_DETECT_NAMING_CONVENTION} or
     *            {@link #LEGACY_NAMING_CONVENTION} or {@link #CAMEL_CASE_NAMING_CONVENTION}.
     */
    int getNamingConvention();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isNamingConventionSet();

    /**
     * Whether the template parser will try to remove superfluous white-space around certain tags.
     */
    boolean getWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link SettingValueNotSetException}.
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
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * See {@link Configuration#getAutoEscapingPolicy()}.
     */
    int getAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link SettingValueNotSetException}.
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
     * note that templates can specify their own output format like {@code <#ftl output_format="HTML">}, which
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
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isOutputFormatSet();

    /**
     * Tells if the "file" extension part of the source name ({@link Template#getSourceName()}) will influence certain
     * parsing settings. For backward compatibility, it defaults to {@code false} if
     * {@link #getIncompatibleImprovements()} is less than 2.3.24. Starting from {@code incompatibleImprovements}
     * 2.3.24, it defaults to {@code true}, so the following standard file extensions take their effect:
     *
     * <ul>
     *   <li>{@code ftlh}: Sets the {@link #getOutputFormat() outputFormat} setting to {@code "HTML"}
     *       (i.e., {@link HTMLOutputFormat#INSTANCE}, unless the {@code "HTML"} name is overridden by
     *       the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} setting to
     *       {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}.
     *   <li>{@code ftlx}: Sets the {@link #getOutputFormat() outputFormat} setting to
     *       {@code "XML"} (i.e., {@link XMLOutputFormat#INSTANCE}, unless the {@code "XML"} name is overridden by
     *       the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} setting to
     *       {@link #ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY}.
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
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isRecognizeStandardFileExtensionsSet();

    /**
     * See {@link TopLevelConfiguration#getIncompatibleImprovements()}; this is normally directly delegates to
     * {@link Configuration#getIncompatibleImprovements()}, and it's always set.
     */
    Version getIncompatibleImprovements();

    /**
     * The assumed display width of the tab character (ASCII 9), which influences the column number shown in error
     * messages (or the column number you get through other API-s). So for example if the users edit templates in an
     * editor where the tab width is set to 4, you should set this to 4 so that the column numbers printed by FreeMarker
     * will match the column number shown in the editor. This setting doesn't affect the output of templates, as a tab
     * in the template will remain a tab in the output too.
     * It's value is at least 1, at most 256.
     */
    int getTabSize();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might returns a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isTabSizeSet();

    /**
     * Sets the charset used for decoding template files.
     * <p>
     * Defaults to the default system {@code fileEncoding}, which can change from one server to
     * another, so <b>you should always set this setting</b>. If you don't know what charset your should chose,
     * {@code "UTF-8"} is usually a good choice.
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
     * configuration or throws a {@link SettingValueNotSetException}.
     */
    boolean isSourceEncodingSet();

}

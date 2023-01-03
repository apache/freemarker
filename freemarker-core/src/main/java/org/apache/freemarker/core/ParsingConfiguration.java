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

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.PlainTextOutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.outputformat.impl.XMLOutputFormat;

import java.nio.charset.Charset;

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
     * The template language used; this is often overridden for certain file extensions with
     * {@link #getRecognizeStandardFileExtensions()} and/or
     * {@link Configuration#getTemplateConfigurations() templateConfigurations} setting of the {@link Configuration}.
     * 
     * <p>If a {@link TemplateLanguage} specifies a non-{@code null}
     * {@link TemplateLanguage#getOutputFormat(Configuration) outputFormat}, or a non-{@code null}
     * {@link TemplateLanguage#getAutoEscapingPolicy() autoEscapingPolicy}, that overrides the value of
     * the {@linkplain #getOutputFormat() outputFormat} and {@link #getAutoEscapingPolicy() autoEscapingPolicy}
     * settings that are coming from {@link Configuration#getTemplateConfigurations templateConfigurations}, from the
     * {@link Configuration}, or from any other {@link ParsingConfiguration}. Most {@link TemplateLanguage}-s should
     * have non-{@code null} for those settings, to prevent confusion on the template author side. There can be
     * exceptions from this though, like {@link DefaultTemplateLanguage#F3AC} (where the "C" at the end stands for
     * "configurable") has {@code null} for these settings.
     * 
     * @see ParsingConfiguration#getRecognizeStandardFileExtensions()
     */
    TemplateLanguage getTemplateLanguage();

    boolean isTemplateLanguageSet();
    
    /**
     * Whether the template parser will try to remove superfluous white-space around certain tags.
     */
    boolean getWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might return a default value, or returns the value of the setting from a parent parsing
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
     * the setting might return a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * Specifies when auto-escaping should be enabled depending on the current {@linkplain OutputFormat output format};
     * default is {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}. It's important to know that the
     * {@link #getTemplateLanguage() templateLanguage} setting will override this, if the {@link TemplateLanguage}
     * specifies a non-{@code null} {@link TemplateLanguage#getAutoEscapingPolicy() autoEscapingPolicy}.
     * 
     * <p>Note that the default output format, {@link UndefinedOutputFormat}, is a non-escaping format, so there
     * auto-escaping will be off. For most templates that's no used though, as the standard file extensions will set
     * a {@link TemplateLanguage} that uses a different {@link OutputFormat}.
     * 
     * <p>Note that the templates can turn auto-escaping on/off locally with directives like
     * {@code <#ftl auto_esc=...>}, {@code <#autoEsc>...</#autoEsc>}, and {@code <#noAutoEsc>...</#noAutoEsc>}, which
     * are ignoring the auto-escaping policy.
     *
     * <p><b>About auto-escaping</b></p>
     *
     * <p>
     * Auto-escaping has significance when a value is printed with <code>${...}</code>. If
     * auto-escaping is on, FreeMarker will assume that the value is plain text (as opposed to markup or some kind of
     * rich text), so it will escape it according the current output format (see {@link #getOutputFormat()}
     * and {@link TemplateConfiguration.Builder#setOutputFormat(OutputFormat)}). If auto-escaping is off, FreeMarker
     * will assume that the string value is already in the output format, so it prints it as is to the output.
     *
     * <p>Further notes on auto-escaping:
     * <ul>
     *   <li>When printing numbers, dates, and other kind of non-string values with <code>${...}</code>, they will be
     *       first converted to string (according the formatting settings and locale), then they are escaped just like
     *       string values.
     *   <li>When printing {@link TemplateMarkupOutputModel}-s, they aren't escaped again (they are already escaped).
     *   <li>Auto-escaping doesn't do anything if the current output format isn't an {@link MarkupOutputFormat}.
     *       That's the case for the default output format, {@link UndefinedOutputFormat}, and also for
     *       {@link PlainTextOutputFormat}.
     *   <li>The output format inside a string literal expression is always {@link PlainTextOutputFormat}
     *       (regardless of the output format of the containing template), which is a non-escaping format. Thus for
     *       example, with <code>&lt;#assign s = "foo${bar}"&gt;</code>, {@code bar} will always get into {@code s}
     *       without escaping, but with <code>&lt;#assign s&gt;foo${bar}&lt;#assign&gt;</code> it may will be escaped.
     * </ul>
     *
     * <p>Note that what you set here is just a default, which can be overridden for individual templates with the
     * {@linkplain Configuration#getTemplateConfigurations() template configurations setting}. This setting is also
     * overridden by the standard file extensions; see them at {@link #getRecognizeStandardFileExtensions()}.
     *
     * @see Configuration.Builder#setAutoEscapingPolicy(AutoEscapingPolicy)
     * @see TemplateConfiguration.Builder#setAutoEscapingPolicy(AutoEscapingPolicy)
     * @see Configuration.Builder#setOutputFormat(OutputFormat)
     * @see TemplateConfiguration.Builder#setOutputFormat(OutputFormat)
     */
    AutoEscapingPolicy getAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might return a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isAutoEscapingPolicySet();

    /**
     * The output format to use, which among others influences auto-escaping (see {@link #getAutoEscapingPolicy}
     * autoEscapingPolicy}), and possibly the MIME type of the output. It's important to know that the
     * {@link #getTemplateLanguage() templateLanguage} setting will override this, if the
     * {@link TemplateLanguage} specifies a non-{@code null}
     * {@link TemplateLanguage#getOutputFormat(Configuration) outputFormat} (and most languages do).
     * <p>
     * On the {@link Configuration} level, usually, you should leave this on its default, which is
     * {@link UndefinedOutputFormat#INSTANCE}, and then use standard file extensions like "f3ah" (for HTML) or "f3ax"
     * (for XML) will set the {@link TemplateLanguage} and hence the output format as well (assuming that
     * {@link #getRecognizeStandardFileExtensions() recognizeStandardFileExtensions} is on its default, {@code true}).
     * Where you can't use the standard extensions, templates still can be associated
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
     * the setting might return a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isOutputFormatSet();

    /**
     * Tells if the "file" extension part of the source name ({@link Template#getSourceName()}) will set the
     * {@linkplain #getTemplateLanguage() template language setting}, when the file extension matches
     * the {@link TemplateLanguage#getFileExtension()} of the {@link TemplateLanguage}-s known by the
     * {@link Configuration}. Defaults to {@code true} This overrides the {@link TemplateLanguage} coming from
     * {@link Configuration#getTemplateConfigurations templateConfigurations}, from the {@link Configuration}, or from
     * any other {@link ParsingConfiguration}.
     * 
     * <p>As of this writing (2018-04-11; TODO [FM3] ensure this is up to date), the standard extensons are
     * {@code f3ah}, {@code f3ax}, {@code f3au}, {@code f3ac},
     * {@code f3sh}, {@code f3sx}, {@code f3su}, {@code f3sc},
     * {@code f3uu}.
     * 
     * <p>The part after "f3" but before the last letter of the file extension specifies the syntax of the template:
     * <ul>
     *   <li>"a": {@link DefaultTemplateLanguage} where {@link DefaultTemplateLanguage#getTagSyntax()}
     *       is {@link TagSyntax#ANGLE_BRACKET} and {@link DefaultTemplateLanguage#getInterpolationSyntax()} is
     *       {@link InterpolationSyntax#DOLLAR}. 
     *   <li>"s": {@link DefaultTemplateLanguage} where {@link DefaultTemplateLanguage#getTagSyntax()}
     *       is {@link TagSyntax#SQUARE_BRACKET} and {@link DefaultTemplateLanguage#getInterpolationSyntax()} is also
     *       {@link InterpolationSyntax#SQUARE_BRACKET}. 
     * </ul>
     *
     * <p>The last letter of the file extension specifies the {@link OutputFormat} and the {@link AutoEscapingPolicy}
     * used by the template:
     * <ul>
     *   <li>"h": {@code "HTML"} (i.e., {@link HTMLOutputFormat#INSTANCE}, unless the {@code "HTML"} name is overridden
     *       by the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} will be
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}.
     *   <li>"x": {@code "XML"} (i.e., {@link XMLOutputFormat#INSTANCE}, unless the {@code "XML"} name is overridden by
     *       the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats} setting) and
     *       the {@link #getAutoEscapingPolicy() autoEscapingPolicy} will be
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}.
      *   <li>"u": {@code "undefined"} (i.e., {@link UndefinedOutputFormat#INSTANCE}, unless the {@code "undefined"}
      *      name is overridden by the {@link Configuration#getRegisteredCustomOutputFormats registeredOutputFormats}
      *      setting) and the {@link #getAutoEscapingPolicy() autoEscapingPolicy} will be
     *       {@link AutoEscapingPolicy#ENABLE_IF_DEFAULT}.
      *   <li>"c": The output format comes from the "configuration", i.e., from {@link #getOutputFormat()}, also the
     *       auto escaping policy comes from {@link #getAutoEscapingPolicy()}.
    * </ul>
     */
    // TODO [FM3] If we will support user-defined languages, then this won't be "Standard" after all.
    boolean getRecognizeStandardFileExtensions();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting might return a default value, or returns the value of the setting from a parent parsing
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
     * the setting might return a default value, or returns the value of the setting from a parent parsing
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
     * the setting might return a default value, or returns the value of the setting from a parent parsing
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
     * the setting might return a default value, or returns the value of the setting from a parent parsing
     * configuration or throws a {@link CoreSettingValueNotSetException}.
     */
    boolean isSourceEncodingSet();

}

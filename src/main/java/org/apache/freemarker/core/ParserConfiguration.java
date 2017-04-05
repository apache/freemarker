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

/**
 * <b>Don't implement this interface yourself</b>; use the existing implementation(s). This interface is implemented by
 * classes that hold settings that affect template parsing (as opposed to {@linkplain Template#process(Object, Writer)
 * template processing}). New parser settings can be added in new FreeMarker versions, which will break your
 * implementation.
 *
 * @see ProcessingConfiguration
 */
public interface ParserConfiguration {

    TemplateLanguage getTemplateLanguage();

    boolean isTemplateLanguageSet();

    /**
     * See {@link Configuration#getTagSyntax()}.
     */
    int getTagSyntax();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isTagSyntaxSet();

    /**
     * See {@link Configuration#getNamingConvention()}.
     */
    int getNamingConvention();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isNamingConventionSet();

    /**
     * See {@link Configuration#getWhitespaceStripping()}.
     */
    boolean getWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isWhitespaceStrippingSet();

    /**
     * Overlaps with {@link MutableProcessingConfiguration#getArithmeticEngine()}; the parser needs this for creating numerical literals.
     */
    ArithmeticEngine getArithmeticEngine();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isArithmeticEngineSet();

    /**
     * See {@link Configuration#getAutoEscapingPolicy()}.
     */
    int getAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isAutoEscapingPolicySet();

    /**
     * See {@link Configuration#getOutputEncoding()}.
     */
    OutputFormat getOutputFormat();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isOutputFormatSet();

    /**
     * See {@link Configuration#getRecognizeStandardFileExtensions()}.
     */
    boolean getRecognizeStandardFileExtensions();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isRecognizeStandardFileExtensionsSet();

    /**
     * See {@link Configuration#getIncompatibleImprovements()}.
     */
    Version getIncompatibleImprovements();

    /**
     * See {@link Configuration#getTabSize()}.
     * 
     * @since 2.3.25
     */
    int getTabSize();

    /**
     * Tells if this setting is set directly in this object. If not, then depending on the implementing class, reading
     * the setting mights returns a default value, or returns the value of the setting from a parent object, or throws
     * an {@link SettingValueNotSetException}.
     */
    boolean isTabSizeSet();

    /**
     * Gets the default encoding for converting bytes to characters when
     * reading template files in a locale for which no explicit encoding
     * was specified. Defaults to the default system encoding.
     */
    Charset getSourceEncoding();

    boolean isSourceEncodingSet();

}

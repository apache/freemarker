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

import java.io.InputStream;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.util._NullArgumentException;

// TODO This will be the superclass of TemplateConfiguration.Builder and Configuration.Builder
public abstract class MutableProcessingAndParseConfiguration<
        SelfT extends MutableProcessingAndParseConfiguration<SelfT>>
        extends MutableProcessingConfiguration<SelfT>
        implements ParserConfiguration {

    private TemplateLanguage templateLanguage;
    private Integer tagSyntax;
    private Integer namingConvention;
    private Boolean whitespaceStripping;
    private Integer autoEscapingPolicy;
    private Boolean recognizeStandardFileExtensions;
    private OutputFormat outputFormat;
    private String encoding;
    private Integer tabSize;

    protected MutableProcessingAndParseConfiguration(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    protected MutableProcessingAndParseConfiguration(MutableProcessingConfiguration parent) {
        super(parent);
    }

    /**
     * See {@link Configuration#setTagSyntax(int)}.
     */
    public void setTagSyntax(int tagSyntax) {
        Configuration.valideTagSyntaxValue(tagSyntax);
        this.tagSyntax = tagSyntax;
    }

    /**
     * The getter pair of {@link #setTagSyntax(int)}.
     */
    @Override
    public int getTagSyntax() {
        return tagSyntax != null ? tagSyntax : getDefaultTagSyntax();
    }

    protected abstract int getDefaultTagSyntax();

    @Override
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    /**
     * See {@link Configuration#getTemplateLanguage()}
     */
    @Override
    public TemplateLanguage getTemplateLanguage() {
        return templateLanguage != null ? templateLanguage : getDefaultTemplateLanguage();
    }

    protected abstract TemplateLanguage getDefaultTemplateLanguage();

    /**
     * See {@link Configuration#setTemplateLanguage(TemplateLanguage)}
     */
    public void setTemplateLanguage(TemplateLanguage templateLanguage) {
        _NullArgumentException.check("templateLanguage", templateLanguage);
        this.templateLanguage = templateLanguage;
    }

    public boolean isTemplateLanguageSet() {
        return templateLanguage != null;
    }

    /**
     * See {@link Configuration#setNamingConvention(int)}.
     */
    public void setNamingConvention(int namingConvention) {
        Configuration.validateNamingConventionValue(namingConvention);
        this.namingConvention = namingConvention;
    }

    /**
     * The getter pair of {@link #setNamingConvention(int)}.
     */
    @Override
    public int getNamingConvention() {
        return namingConvention != null ? namingConvention
                : getDefaultNamingConvention();
    }

    protected abstract int getDefaultNamingConvention();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    @Override
    public boolean isNamingConventionSet() {
        return namingConvention != null;
    }

    /**
     * See {@link Configuration#setWhitespaceStripping(boolean)}.
     */
    public void setWhitespaceStripping(boolean whitespaceStripping) {
        this.whitespaceStripping = Boolean.valueOf(whitespaceStripping);
    }

    /**
     * The getter pair of {@link #getWhitespaceStripping()}.
     */
    @Override
    public boolean getWhitespaceStripping() {
        return whitespaceStripping != null ? whitespaceStripping.booleanValue()
                : getDefaultWhitespaceStripping();
    }

    protected abstract boolean getDefaultWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    @Override
    public boolean isWhitespaceStrippingSet() {
        return whitespaceStripping != null;
    }

    /**
     * Sets the output format of the template; see {@link Configuration#setAutoEscapingPolicy(int)} for more.
     */
    public void setAutoEscapingPolicy(int autoEscapingPolicy) {
        Configuration.validateAutoEscapingPolicyValue(autoEscapingPolicy);

        this.autoEscapingPolicy = Integer.valueOf(autoEscapingPolicy);
    }

    /**
     * The getter pair of {@link #setAutoEscapingPolicy(int)}.
     */
    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy.intValue()
                : getDefaultAutoEscapingPolicy();
    }

    protected abstract int getDefaultAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    @Override
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    /**
     * Sets the output format of the template; see {@link Configuration#setOutputFormat(OutputFormat)} for more.
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        _NullArgumentException.check("outputFormat", outputFormat);
        this.outputFormat = outputFormat;
    }

    /**
     * The getter pair of {@link #setOutputFormat(OutputFormat)}.
     */
    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : getDefaultOutputFormat();
    }

    protected abstract OutputFormat getDefaultOutputFormat();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    @Override
    public boolean isOutputFormatSet() {
        return outputFormat != null;
    }

    /**
     * See {@link Configuration#setRecognizeStandardFileExtensions(boolean)}.
     */
    public void setRecognizeStandardFileExtensions(boolean recognizeStandardFileExtensions) {
        this.recognizeStandardFileExtensions = Boolean.valueOf(recognizeStandardFileExtensions);
    }

    /**
     * Getter pair of {@link #setRecognizeStandardFileExtensions(boolean)}.
     */
    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return recognizeStandardFileExtensions != null ? recognizeStandardFileExtensions.booleanValue()
                : getDefaultRecognizeStandardFileExtensions();
    }

    protected abstract boolean getDefaultRecognizeStandardFileExtensions();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     */
    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return recognizeStandardFileExtensions != null;
    }

    @Override
    public String getEncoding() {
        return encoding != null ? encoding : getDefaultEncoding();
    }

    protected abstract String getDefaultEncoding();

    /**
     * The charset to be used when reading the template "file" that the {@link TemplateLoader} returns as binary
     * ({@link InputStream}). If the {@code #ftl} header sepcifies an encoding, that will override this.
     */
    public void setEncoding(String encoding) {
        _NullArgumentException.check("encoding", encoding);
        this.encoding = encoding;
    }

    public boolean isEncodingSet() {
        return encoding != null;
    }

    /**
     * See {@link Configuration#setTabSize(int)}.
     *
     * @since 2.3.25
     */
    public void setTabSize(int tabSize) {
        this.tabSize = Integer.valueOf(tabSize);
    }

    /**
     * Getter pair of {@link #setTabSize(int)}.
     *
     * @since 2.3.25
     */
    @Override
    public int getTabSize() {
        return tabSize != null ? tabSize.intValue()
                : getDefailtTabSize();
    }

    protected abstract int getDefailtTabSize();

    /**
     * Tells if this setting is set directly in this object or its value is coming from the {@link #getParent() parent}.
     *
     * @since 2.3.25
     */
    @Override
    public boolean isTabSizeSet() {
        return tabSize != null;
    }

}

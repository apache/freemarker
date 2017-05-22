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
import java.nio.charset.Charset;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.util._NullArgumentException;

public abstract class MutableParsingAndProcessingConfiguration<
        SelfT extends MutableParsingAndProcessingConfiguration<SelfT>>
        extends MutableProcessingConfiguration<SelfT>
        implements ParsingAndProcessingConfiguration {

    private TemplateLanguage templateLanguage;
    private TagSyntax tagSyntax;
    private NamingConvention namingConvention;
    private Boolean whitespaceStripping;
    private AutoEscapingPolicy autoEscapingPolicy;
    private Boolean recognizeStandardFileExtensions;
    private OutputFormat outputFormat;
    private Charset sourceEncoding;
    private Integer tabSize;

    protected MutableParsingAndProcessingConfiguration() {
        super();
    }

    /**
     * Setter pair of {@link #getTagSyntax()}.
     *
     * @param tagSyntax
     *         Can't be {@code null}
     */
    public void setTagSyntax(TagSyntax tagSyntax) {
        _NullArgumentException.check("tagSyntax", tagSyntax);
        this.tagSyntax = tagSyntax;
    }

    /**
     * Fluent API equivalent of {@link #tagSyntax(TagSyntax)}
     */
    public SelfT tagSyntax(TagSyntax tagSyntax) {
        setTagSyntax(tagSyntax);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetTagSyntax() {
        this.tagSyntax = null;
    }

    @Override
    public TagSyntax getTagSyntax() {
        return isTagSyntaxSet() ? tagSyntax : getDefaultTagSyntax();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract TagSyntax getDefaultTagSyntax();

    @Override
    public boolean isTagSyntaxSet() {
        return tagSyntax != null;
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
         return isTemplateLanguageSet() ? templateLanguage : getDefaultTemplateLanguage();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract TemplateLanguage getDefaultTemplateLanguage();

    /**
     * Setter pair of {@link #getTemplateLanguage()}.
     */
    public void setTemplateLanguage(TemplateLanguage templateLanguage) {
        _NullArgumentException.check("templateLanguage", templateLanguage);
        this.templateLanguage = templateLanguage;
    }

    /**
     * Fluent API equivalent of {@link #setTemplateLanguage(TemplateLanguage)}
     */
    public SelfT templateLanguage(TemplateLanguage templateLanguage) {
        setTemplateLanguage(templateLanguage);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetTemplateLanguage() {
        this.templateLanguage = null;
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return templateLanguage != null;
    }

    /**
     * Setter pair of {@link #getNamingConvention()}.
     *
     * @param namingConvention
     *         Can't be {@code null}
     */
    public void setNamingConvention(NamingConvention namingConvention) {
        _NullArgumentException.check("namingConvention", namingConvention);
        this.namingConvention = namingConvention;
    }

    /**
     * Fluent API equivalent of {@link #setNamingConvention(NamingConvention)}
     */
    public SelfT namingConvention(NamingConvention namingConvention) {
        setNamingConvention(namingConvention);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetNamingConvention() {
        this.namingConvention = null;
    }

    /**
     * The getter pair of {@link #setNamingConvention(NamingConvention)}.
     */
    @Override
    public NamingConvention getNamingConvention() {
         return isNamingConventionSet() ? namingConvention
                : getDefaultNamingConvention();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract NamingConvention getDefaultNamingConvention();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isNamingConventionSet() {
        return namingConvention != null;
    }

    /**
     * Setter pair of {@link ParsingConfiguration#getWhitespaceStripping()}.
     */
    public void setWhitespaceStripping(boolean whitespaceStripping) {
        this.whitespaceStripping = whitespaceStripping;
    }

    /**
     * Fluent API equivalent of {@link #setWhitespaceStripping(boolean)}
     */
    public SelfT whitespaceStripping(boolean whitespaceStripping) {
        setWhitespaceStripping(whitespaceStripping);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetWhitespaceStripping() {
        this.whitespaceStripping = null;
    }

    /**
     * The getter pair of {@link #getWhitespaceStripping()}.
     */
    @Override
    public boolean getWhitespaceStripping() {
         return isWhitespaceStrippingSet() ? whitespaceStripping : getDefaultWhitespaceStripping();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract boolean getDefaultWhitespaceStripping();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isWhitespaceStrippingSet() {
        return whitespaceStripping != null;
    }

    /**
     * * Setter pair of {@link #getAutoEscapingPolicy()}.
     */
    public void setAutoEscapingPolicy(AutoEscapingPolicy autoEscapingPolicy) {
        _NullArgumentException.check("autoEscapingPolicy", autoEscapingPolicy);
        this.autoEscapingPolicy = autoEscapingPolicy;
    }

    /**
     * Fluent API equivalent of {@link #setAutoEscapingPolicy(AutoEscapingPolicy)}
     */
    public SelfT autoEscapingPolicy(AutoEscapingPolicy autoEscapingPolicy) {
        setAutoEscapingPolicy(autoEscapingPolicy);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetAutoEscapingPolicy() {
        this.autoEscapingPolicy = null;
    }

    /**
     * The getter pair of {@link #setAutoEscapingPolicy(AutoEscapingPolicy)}.
     */
    @Override
    public AutoEscapingPolicy getAutoEscapingPolicy() {
         return isAutoEscapingPolicySet() ? autoEscapingPolicy : getDefaultAutoEscapingPolicy();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract AutoEscapingPolicy getDefaultAutoEscapingPolicy();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    /**
     * Setter pair of {@link #getOutputFormat()}.
     */
    public void setOutputFormat(OutputFormat outputFormat) {
        if (outputFormat == null) {
            throw new _NullArgumentException(
                    "outputFormat",
                    "You may meant: " + UndefinedOutputFormat.class.getSimpleName() + ".INSTANCE");
        }
        this.outputFormat = outputFormat;
    }

    /**
     * Resets this setting to its initial state, as if it was never set.
     */
    public void unsetOutputFormat() {
        this.outputFormat = null;
    }

    /**
     * Fluent API equivalent of {@link #setOutputFormat(OutputFormat)}
     */
    public SelfT outputFormat(OutputFormat outputFormat) {
        setOutputFormat(outputFormat);
        return self();
    }

    @Override
    public OutputFormat getOutputFormat() {
         return isOutputFormatSet() ? outputFormat : getDefaultOutputFormat();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract OutputFormat getDefaultOutputFormat();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isOutputFormatSet() {
        return outputFormat != null;
    }

    /**
     * Setter pair of {@link ParsingConfiguration#getRecognizeStandardFileExtensions()}.
     */
    public void setRecognizeStandardFileExtensions(boolean recognizeStandardFileExtensions) {
        this.recognizeStandardFileExtensions = recognizeStandardFileExtensions;
    }

    /**
     * Fluent API equivalent of {@link #setRecognizeStandardFileExtensions(boolean)}
     */
    public SelfT recognizeStandardFileExtensions(boolean recognizeStandardFileExtensions) {
        setRecognizeStandardFileExtensions(recognizeStandardFileExtensions);
        return self();
    }

    /**
     * Resets this setting to its initial state, as if it was never set.
     */
    public void unsetRecognizeStandardFileExtensions() {
        recognizeStandardFileExtensions = null;
    }

    /**
     * Getter pair of {@link #setRecognizeStandardFileExtensions(boolean)}.
     */
    @Override
    public boolean getRecognizeStandardFileExtensions() {
         return isRecognizeStandardFileExtensionsSet() ? recognizeStandardFileExtensions
                : getDefaultRecognizeStandardFileExtensions();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract boolean getDefaultRecognizeStandardFileExtensions();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return recognizeStandardFileExtensions != null;
    }

    @Override
    public Charset getSourceEncoding() {
         return isSourceEncodingSet() ? sourceEncoding : getDefaultSourceEncoding();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract Charset getDefaultSourceEncoding();

    /**
     * The charset to be used when reading the template "file" that the {@link TemplateLoader} returns as binary
     * ({@link InputStream}). If the {@code #ftl} header specifies an charset, that will override this.
     */
    public void setSourceEncoding(Charset sourceEncoding) {
        _NullArgumentException.check("sourceEncoding", sourceEncoding);
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * Fluent API equivalent of {@link #setSourceEncoding(Charset)}
     */
    public SelfT sourceEncoding(Charset sourceEncoding) {
        setSourceEncoding(sourceEncoding);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetSourceEncoding() {
        this.sourceEncoding = null;
    }

    @Override
    public boolean isSourceEncodingSet() {
        return sourceEncoding != null;
    }

    /**
     * Setter pair of {@link #getTabSize()}.
     */
    public void setTabSize(int tabSize) {
        if (tabSize < 1) {
            throw new IllegalArgumentException("\"tabSize\" must be at least 1, but was " + tabSize);
        }
        // To avoid integer overflows:
        if (tabSize > 256) {
            throw new IllegalArgumentException("\"tabSize\" can't be more than 256, but was " + tabSize);
        }
        this.tabSize = Integer.valueOf(tabSize);
    }

    /**
     * Fluent API equivalent of {@link #setTabSize(int)}
     */
    public SelfT tabSize(int tabSize) {
        setTabSize(tabSize);
        return self();
    }

    /**
     * Resets the setting value as if it was never set (but it doesn't affect the value inherited from another
     * {@link ParsingConfiguration}).
     */
    public void unsetTabSize() {
        this.tabSize = null;
    }

    @Override
    public int getTabSize() {
         return isTabSizeSet() ? tabSize.intValue() : getDefaultTabSize();
    }

    /**
     * Returns the value the getter method returns when the setting is not set, possibly by inheriting the setting value
     * from another {@link ParsingConfiguration}, or throws {@link SettingValueNotSetException}.
     */
    protected abstract int getDefaultTabSize();

    /**
     * Tells if this setting is set directly in this object or its value is inherited from the parent parsing configuration..
     */
    @Override
    public boolean isTabSizeSet() {
        return tabSize != null;
    }

}

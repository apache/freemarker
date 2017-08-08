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
import java.util.Set;

import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.outputformat.impl.UndefinedOutputFormat;
import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.util._NullArgumentException;
import org.apache.freemarker.core.util._SortedArraySet;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.core.util._UnmodifiableCompositeSet;

public abstract class MutableParsingAndProcessingConfiguration<
        SelfT extends MutableParsingAndProcessingConfiguration<SelfT>>
        extends MutableProcessingConfiguration<SelfT>
        implements ParsingAndProcessingConfiguration {

    public static final String OUTPUT_FORMAT_KEY = "outputFormat";
    public static final String SOURCE_ENCODING_KEY = "sourceEncoding";
    public static final String WHITESPACE_STRIPPING_KEY = "whitespaceStripping";
    public static final String AUTO_ESCAPING_POLICY_KEY = "autoEscapingPolicy";
    public static final String RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY = "recognizeStandardFileExtensions";
    public static final String TEMPLATE_LANGUAGE_KEY = "templateLanguage";
    public static final String TAG_SYNTAX_KEY = "tagSyntax";
    public static final String TAB_SIZE_KEY = "tabSize";
    public static final String INCOMPATIBLE_IMPROVEMENTS_KEY = "incompatibleImprovements";

    private static final _UnmodifiableCompositeSet<String> SETTING_NAMES = new _UnmodifiableCompositeSet<>(
            MutableProcessingConfiguration.getSettingNames(),
            new _SortedArraySet<>(
                // Must be sorted alphabetically!
                AUTO_ESCAPING_POLICY_KEY,
                INCOMPATIBLE_IMPROVEMENTS_KEY,
                OUTPUT_FORMAT_KEY,
                RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY,
                SOURCE_ENCODING_KEY,
                TAB_SIZE_KEY,
                TAG_SYNTAX_KEY,
                TEMPLATE_LANGUAGE_KEY,
                WHITESPACE_STRIPPING_KEY
            ));

    private TemplateLanguage templateLanguage;
    private TagSyntax tagSyntax;
    private Boolean whitespaceStripping;
    private AutoEscapingPolicy autoEscapingPolicy;
    private Boolean recognizeStandardFileExtensions;
    private OutputFormat outputFormat;
    private Charset sourceEncoding;
    private Integer tabSize;

    protected MutableParsingAndProcessingConfiguration() {
        super();
    }

    @Override
    public void setSetting(String name, String value) throws ConfigurationException {
        boolean nameUnhandled = false;
        try {
            if (SOURCE_ENCODING_KEY.equals(name)) {
                if (JVM_DEFAULT_VALUE.equalsIgnoreCase(value)) {
                    setSourceEncoding(Charset.defaultCharset());
                } else {
                    setSourceEncoding(Charset.forName(value));
                }
            } else if (OUTPUT_FORMAT_KEY.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                    unsetOutputFormat();
                } else {
                    setOutputFormat((OutputFormat) _ObjectBuilderSettingEvaluator.eval(
                            value, OutputFormat.class, true, _SettingEvaluationEnvironment.getCurrent()));
                }
            } else if (WHITESPACE_STRIPPING_KEY.equals(name)) {
                setWhitespaceStripping(_StringUtils.getYesNo(value));
            } else if (AUTO_ESCAPING_POLICY_KEY.equals(name)) {
                if ("enableIfDefault".equals(value)) {
                    setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_DEFAULT);
                } else if ("enableIfSupported".equals(value)) {
                    setAutoEscapingPolicy(AutoEscapingPolicy.ENABLE_IF_SUPPORTED);
                } else if ("disable".equals(value)) {
                    setAutoEscapingPolicy(AutoEscapingPolicy.DISABLE);
                } else {
                    throw new InvalidSettingValueException( name, value,
                            "enable_if_default".equals(value) ? "The correct value is: enableIfDefault" :
                            "enable_if_supported".equals(value) ? "The correct value is: enableIfSupported" :
                            "No such predefined auto escaping policy name");
                }
            } else if (RECOGNIZE_STANDARD_FILE_EXTENSIONS_KEY.equals(name)) {
                if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
                    unsetRecognizeStandardFileExtensions();
                } else {
                    setRecognizeStandardFileExtensions(_StringUtils.getYesNo(value));
                }
            } else if (TEMPLATE_LANGUAGE_KEY.equals(name)) {
                if ("FTL".equals(value)) {
                    setTemplateLanguage(TemplateLanguage.FTL);
                } else if ("staticText".equals(value)) {
                    setTemplateLanguage(TemplateLanguage.STATIC_TEXT);
                } else {
                    throw new InvalidSettingValueException(name, value, "Unsupported template language name");
                }
            } else if (TAG_SYNTAX_KEY.equals(name)) {
                if ("autoDetect".equals(value)) {
                    setTagSyntax(TagSyntax.AUTO_DETECT);
                } else if ("angleBracket".equals(value)) {
                    setTagSyntax(TagSyntax.ANGLE_BRACKET);
                } else if ("squareBracket".equals(value)) {
                    setTagSyntax(TagSyntax.SQUARE_BRACKET);
                } else {
                    throw new InvalidSettingValueException(name, value,
                            "auto_detect".equals(value) ? "The correct value is: autoDetect" :
                            "angle_bracket".equals(value) ? "The correct value is: angleBracket" :
                            "square_bracket".equals(value) ? "The correct value is: squareBracket" :
                            "No such predefined tag syntax name");
                }
            } else if (TAB_SIZE_KEY.equals(name)) {
                setTabSize(Integer.parseInt(value));
            } else {
                nameUnhandled = true;
            }
        } catch (InvalidSettingValueException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidSettingValueException(name, value, e);
        }
        if (nameUnhandled) {
            super.setSetting(name, value);
        }
    }

    @Override
    protected Version getRemovalVersionForUnknownSetting(String name) {
        if (name.equals("namingConvention") || name.equalsIgnoreCase("naming_convention")) {
            return Configuration.VERSION_3_0_0;
        }
        return super.getRemovalVersionForUnknownSetting(name);
    }

    public static Set<String> getSettingNames() {
        return SETTING_NAMES;
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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
     * from another {@link ParsingConfiguration}, or throws {@link CoreSettingValueNotSetException}.
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

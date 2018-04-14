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
import org.apache.freemarker.core.outputformat.OutputFormat;

/**
 * Used for creating the final {@link ParsingConfiguration} passed to
 * {@link TemplateLanguage#parse(Template, java.io.Reader, ParsingConfiguration, java.io.InputStream)}.
 * Overrides the {@link TemplateLanguage} in a {@link ParsingConfiguration}, also the {@link OutputFormat} and/or
 * {@link AutoEscapingPolicy} if the {@link TemplateLanguage} or the context requires that.
 */
final class FinalParsingConfiguration implements ParsingConfiguration {

    private final ParsingConfiguration pCfg;
    private final TemplateLanguage templateLanguage;
    private final OutputFormat outputFormat;
    private final AutoEscapingPolicy autoEscapingPolicy;

    /**
     * @param pCfg
     *            The {@link ParsingConfiguration} where we override some settings; not {@code null}
     * @param templateLanguage
     *            The final {@link TemplateLanguage} to be used; not {@code null}.
     * @param contextOutputFormat
     *            See similar parameter in {@link Template#Template(String, String, java.io.InputStream, Charset,
     *            java.io.Reader, Configuration, TemplateConfiguration, OutputFormat, AutoEscapingPolicy)};
     *            maybe {@code null}.
     * @param contextAutoEscapingPolicy
     *            See similar parameter in {@link Template#Template(String, String, java.io.InputStream, Charset,
     *            java.io.Reader, Configuration, TemplateConfiguration, OutputFormat, AutoEscapingPolicy)};
     *            maybe {@code null}.
     * @param cfg
     *            Not {@code null}
     */
    FinalParsingConfiguration(
            ParsingConfiguration pCfg, TemplateLanguage templateLanguage,
            OutputFormat contextOutputFormat, AutoEscapingPolicy contextAutoEscapingPolicy,
            Configuration cfg) {
        this.pCfg = pCfg;
        this.templateLanguage = templateLanguage;

        // contextXxx is stronger than anything.
        // TemplateLanguage.xxx (if it's non-null) stronger than the rest.
        
        OutputFormat tempLangOutputFormat;
        this.outputFormat = contextOutputFormat != null ? contextOutputFormat
                : (tempLangOutputFormat = templateLanguage.getOutputFormat(cfg)) != null
                        ? tempLangOutputFormat
                : pCfg.getOutputFormat();
        
        AutoEscapingPolicy tempLangAutoEscapingPolicy; 
        this.autoEscapingPolicy = contextAutoEscapingPolicy != null ? contextAutoEscapingPolicy
                : (tempLangAutoEscapingPolicy = templateLanguage.getAutoEscapingPolicy()) != null
                        ? tempLangAutoEscapingPolicy
                : pCfg.getAutoEscapingPolicy();
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return templateLanguage;
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return true;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return pCfg.getWhitespaceStripping();
    }

    @Override
    public boolean isWhitespaceStrippingSet() {
        return pCfg.isWhitespaceStrippingSet();
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return pCfg.getArithmeticEngine();
    }

    @Override
    public boolean isArithmeticEngineSet() {
        return pCfg.isArithmeticEngineSet();
    }

    @Override
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        return autoEscapingPolicy;
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return autoEscapingPolicy != null;
    }

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    @Override
    public boolean isOutputFormatSet() {
        return outputFormat != null;
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return pCfg.getRecognizeStandardFileExtensions();
    }

    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return pCfg.isRecognizeStandardFileExtensionsSet();
    }

    @Override
    public Version getIncompatibleImprovements() {
        return pCfg.getIncompatibleImprovements();
    }

    @Override
    public boolean isIncompatibleImprovementsSet() {
        return pCfg.isIncompatibleImprovementsSet();
    }

    @Override
    public int getTabSize() {
        return pCfg.getTabSize();
    }

    @Override
    public boolean isTabSizeSet() {
        return pCfg.isTabSizeSet();
    }

    @Override
    public Charset getSourceEncoding() {
        return pCfg.getSourceEncoding();
    }

    @Override
    public boolean isSourceEncodingSet() {
        return pCfg.isSourceEncodingSet();
    }
    
}

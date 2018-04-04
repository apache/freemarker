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
 * Used when the {@link TemplateLanguage} in the {@link ParsingConfiguration} need to be replaced.
 */
final class ParsingConfigurationWithTemplateLanguageOverride implements ParsingConfiguration {

    private final ParsingConfiguration pCfg;
    private final TemplateLanguage templateLanguage;
    
    public ParsingConfigurationWithTemplateLanguageOverride(ParsingConfiguration pCfg,
            TemplateLanguage templateLanguage) {
        this.pCfg = pCfg;
        this.templateLanguage = templateLanguage;
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
    public TagSyntax getTagSyntax() {
        return pCfg.getTagSyntax();
    }

    @Override
    public boolean isTagSyntaxSet() {
        return pCfg.isTagSyntaxSet();
    }

    @Override
    public InterpolationSyntax getInterpolationSyntax() {
        return pCfg.getInterpolationSyntax();
    }

    @Override
    public boolean isInterpolationSyntaxSet() {
        return pCfg.isInterpolationSyntaxSet();
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
        return pCfg.getAutoEscapingPolicy();
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return pCfg.isAutoEscapingPolicySet();
    }

    @Override
    public OutputFormat getOutputFormat() {
        return pCfg.getOutputFormat();
    }

    @Override
    public boolean isOutputFormatSet() {
        return pCfg.isOutputFormatSet();
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

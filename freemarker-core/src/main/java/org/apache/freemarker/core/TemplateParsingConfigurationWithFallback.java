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
 * Adds {@link Configuration} fallback to the {@link ParsingConfiguration} part of a {@link TemplateConfiguration}.
 */
final class TemplateParsingConfigurationWithFallback implements ParsingConfiguration {

    private final Configuration cfg;
    private final TemplateConfiguration tCfg;

    TemplateParsingConfigurationWithFallback(Configuration cfg, TemplateConfiguration tCfg) {
        this.cfg = cfg;
        this.tCfg = tCfg;
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return tCfg.isTemplateLanguageSet() ? tCfg.getTemplateLanguage() : cfg.getTemplateLanguage();
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return true;
    }

    @Override
    public TagSyntax getTagSyntax() {
        return tCfg.isTagSyntaxSet() ? tCfg.getTagSyntax() : cfg.getTagSyntax();
    }

    @Override
    public boolean isTagSyntaxSet() {
        return true;
    }

    @Override
    public NamingConvention getNamingConvention() {
        return tCfg.isNamingConventionSet() ? tCfg.getNamingConvention() : cfg.getNamingConvention();
    }

    @Override
    public boolean isNamingConventionSet() {
        return true;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return tCfg.isWhitespaceStrippingSet() ? tCfg.getWhitespaceStripping() : cfg.getWhitespaceStripping();
    }

    @Override
    public boolean isWhitespaceStrippingSet() {
        return true;
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return tCfg.isArithmeticEngineSet() ? tCfg.getArithmeticEngine() : cfg.getArithmeticEngine();
    }

    @Override
    public boolean isArithmeticEngineSet() {
        return true;
    }

    @Override
    public AutoEscapingPolicy getAutoEscapingPolicy() {
        return tCfg.isAutoEscapingPolicySet() ? tCfg.getAutoEscapingPolicy() : cfg.getAutoEscapingPolicy();
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return true;
    }

    @Override
    public OutputFormat getOutputFormat() {
        return tCfg.isOutputFormatSet() ? tCfg.getOutputFormat() : cfg.getOutputFormat();
    }

    @Override
    public boolean isOutputFormatSet() {
        return true;
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return tCfg.isRecognizeStandardFileExtensionsSet() ? tCfg.getRecognizeStandardFileExtensions()
                : cfg.getRecognizeStandardFileExtensions();
    }

    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return true;
    }

    @Override
    public Version getIncompatibleImprovements() {
        // This can be only set on the Configuration-level
        return cfg.getIncompatibleImprovements();
    }

    @Override
    public int getTabSize() {
        return tCfg.isTabSizeSet() ? tCfg.getTabSize() : cfg.getTabSize();
    }

    @Override
    public boolean isTabSizeSet() {
        return true;
    }

    @Override
    public Charset getSourceEncoding() {
        return tCfg.isSourceEncodingSet() ? tCfg.getSourceEncoding() : cfg.getSourceEncoding();
    }

    @Override
    public boolean isSourceEncodingSet() {
        return true;
    }
}

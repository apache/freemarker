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
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */ 
public final class _ParserConfigurationWithInheritedFormat implements ParserConfiguration {

    private final OutputFormat outputFormat;
    private final Integer autoEscapingPolicy;
    private final ParserConfiguration wrappedPCfg;

    public _ParserConfigurationWithInheritedFormat(ParserConfiguration wrappedPCfg, OutputFormat outputFormat,
            Integer autoEscapingPolicy) {
        this.outputFormat = outputFormat;
        this.autoEscapingPolicy = autoEscapingPolicy;
        this.wrappedPCfg = wrappedPCfg;
    }

    @Override
    public boolean getWhitespaceStripping() {
        return wrappedPCfg.getWhitespaceStripping();
    }

    @Override
    public boolean isWhitespaceStrippingSet() {
        return wrappedPCfg.isWhitespaceStrippingSet();
    }

    @Override
    public int getTagSyntax() {
        return wrappedPCfg.getTagSyntax();
    }

    @Override
    public boolean isTagSyntaxSet() {
        return wrappedPCfg.isTagSyntaxSet();
    }

    @Override
    public TemplateLanguage getTemplateLanguage() {
        return wrappedPCfg.getTemplateLanguage();
    }

    @Override
    public boolean isTemplateLanguageSet() {
        return wrappedPCfg.isTemplateLanguageSet();
    }

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : wrappedPCfg.getOutputFormat();
    }

    @Override
    public boolean isOutputFormatSet() {
        return wrappedPCfg.isOutputFormatSet();
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return false;
    }

    @Override
    public boolean isRecognizeStandardFileExtensionsSet() {
        return wrappedPCfg.isRecognizeStandardFileExtensionsSet();
    }

    @Override
    public int getNamingConvention() {
        return wrappedPCfg.getNamingConvention();
    }

    @Override
    public boolean isNamingConventionSet() {
        return wrappedPCfg.isNamingConventionSet();
    }

    @Override
    public Version getIncompatibleImprovements() {
        return wrappedPCfg.getIncompatibleImprovements();
    }

    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy : wrappedPCfg.getAutoEscapingPolicy();
    }

    @Override
    public boolean isAutoEscapingPolicySet() {
        return wrappedPCfg.isAutoEscapingPolicySet();
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return wrappedPCfg.getArithmeticEngine();
    }

    @Override
    public boolean isArithmeticEngineSet() {
        return wrappedPCfg.isArithmeticEngineSet();
    }

    @Override
    public int getTabSize() {
        return wrappedPCfg.getTabSize();
    }

    @Override
    public boolean isTabSizeSet() {
        return wrappedPCfg.isTabSizeSet();
    }

    @Override
    public Charset getSourceEncoding() {
        return wrappedPCfg.getSourceEncoding();
    }

    @Override
    public boolean isSourceEncodingSet() {
        return wrappedPCfg.isSourceEncodingSet();
    }

}
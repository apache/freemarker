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
package freemarker.core;

import freemarker.template.Version;

/**
 * Used to work around that {@link FMParser} has constructors that have separate parameters for individual settings.
 * 
 * @since 2.3.24
 */
class LegacyConstructorParserConfiguration implements ParserConfiguration {

    private final int tagSyntax;
    private final int namingConvention;
    private final boolean whitespaceStripping;
    private final boolean strictSyntaxMode;
    private ArithmeticEngine arithmeticEngine;
    private Integer autoEscapingPolicy; 
    private OutputFormat outputFormat;
    private Boolean recognizeStandardFileExtensions;
    private Integer tabSize;
    private final Version incompatibleImprovements;

    public LegacyConstructorParserConfiguration(boolean strictSyntaxMode, boolean whitespaceStripping, int tagSyntax,
            int namingConvention, Integer autoEscaping, OutputFormat outputFormat,
            Boolean recognizeStandardFileExtensions, Integer tabSize,
            Version incompatibleImprovements, ArithmeticEngine arithmeticEngine) {
        this.tagSyntax = tagSyntax;
        this.namingConvention = namingConvention;
        this.whitespaceStripping = whitespaceStripping;
        this.strictSyntaxMode = strictSyntaxMode;
        this.autoEscapingPolicy = autoEscaping;
        this.outputFormat = outputFormat;
        this.recognizeStandardFileExtensions = recognizeStandardFileExtensions;
        this.tabSize = tabSize;
        this.incompatibleImprovements = incompatibleImprovements;
        this.arithmeticEngine = arithmeticEngine;
    }

    public int getTagSyntax() {
        return tagSyntax;
    }

    public int getNamingConvention() {
        return namingConvention;
    }

    public boolean getWhitespaceStripping() {
        return whitespaceStripping;
    }

    public boolean getStrictSyntaxMode() {
        return strictSyntaxMode;
    }

    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }

    public ArithmeticEngine getArithmeticEngine() {
        if (arithmeticEngine == null) {
            throw new IllegalStateException();
        }
        return arithmeticEngine;
    }

    void setArithmeticEngineIfNotSet(ArithmeticEngine arithmeticEngine) {
        if (this.arithmeticEngine == null) {
            this.arithmeticEngine = arithmeticEngine;
        }
    }

    public int getAutoEscapingPolicy() {
        if (autoEscapingPolicy == null) {
            throw new IllegalStateException();
        }
        return autoEscapingPolicy.intValue();
    }
    
    void setAutoEscapingPolicyIfNotSet(int autoEscapingPolicy) {
        if (this.autoEscapingPolicy == null) {
            this.autoEscapingPolicy = Integer.valueOf(autoEscapingPolicy);
        }
    }

    public OutputFormat getOutputFormat() {
        if (outputFormat == null) {
            throw new IllegalStateException();
        }
        return outputFormat;
    }

    void setOutputFormatIfNotSet(OutputFormat outputFormat) {
        if (this.outputFormat == null) {
            this.outputFormat = outputFormat;
        }
    }

    public boolean getRecognizeStandardFileExtensions() {
        if (recognizeStandardFileExtensions == null) {
            throw new IllegalStateException();
        }
        return recognizeStandardFileExtensions.booleanValue();
    }
    
    void setRecognizeStandardFileExtensionsIfNotSet(boolean recognizeStandardFileExtensions) {
        if (this.recognizeStandardFileExtensions == null) {
            this.recognizeStandardFileExtensions = Boolean.valueOf(recognizeStandardFileExtensions);
        }
    }

    public int getTabSize() {
        if (tabSize == null) {
            throw new IllegalStateException();
        }
        return tabSize.intValue();
    }
    
    void setTabSizeIfNotSet(int tabSize) {
        if (this.tabSize == null) {
            this.tabSize = Integer.valueOf(tabSize);
        }
    }
    
}

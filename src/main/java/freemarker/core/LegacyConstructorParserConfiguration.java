/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private boolean autoEscaping; 
    private String outputFormat;
    private final Version incompatibleImprovements;

    public LegacyConstructorParserConfiguration(boolean strictSyntaxMode, boolean whitespaceStripping, int tagSyntax,
            int namingConvention, boolean autoEscaping, String outputFormat,
            Version incompatibleImprovements, ArithmeticEngine arithmeticEngine) {
        this.tagSyntax = tagSyntax;
        this.namingConvention = namingConvention;
        this.whitespaceStripping = whitespaceStripping;
        this.strictSyntaxMode = strictSyntaxMode;
        this.autoEscaping = autoEscaping;
        this.outputFormat = outputFormat;
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
        return arithmeticEngine;
    }

    void setArithmeticEngineFromNull(ArithmeticEngine arithmeticEngine) {
        if (this.arithmeticEngine != null) {
            throw new IllegalStateException();
        }
        this.arithmeticEngine = arithmeticEngine;
    }

    public boolean getAutoEscaping() {
        return autoEscaping;
    }
    
    void setAutoEscaping(boolean autoEscaping) {
        this.autoEscaping = autoEscaping;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    void setOutputFormatFromNull(String outputFormat) {
        if (this.outputFormat != null) {
            throw new IllegalStateException();
        }
        this.outputFormat = outputFormat;
    }

}

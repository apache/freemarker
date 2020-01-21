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
    public int getTagSyntax() {
        return wrappedPCfg.getTagSyntax();
    }

    @Override
    public int getInterpolationSyntax() {
        return wrappedPCfg.getInterpolationSyntax();
    }

    @Override
    public boolean getStrictSyntaxMode() {
        return wrappedPCfg.getStrictSyntaxMode();
    }

    @Override
    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : wrappedPCfg.getOutputFormat();
    }

    @Override
    public boolean getRecognizeStandardFileExtensions() {
        return false;
    }

    @Override
    public int getNamingConvention() {
        return wrappedPCfg.getNamingConvention();
    }

    @Override
    public Version getIncompatibleImprovements() {
        return wrappedPCfg.getIncompatibleImprovements();
    }

    @Override
    public int getAutoEscapingPolicy() {
        return autoEscapingPolicy != null ? autoEscapingPolicy.intValue() : wrappedPCfg.getAutoEscapingPolicy();
    }

    @Override
    public ArithmeticEngine getArithmeticEngine() {
        return wrappedPCfg.getArithmeticEngine();
    }

    @Override
    public int getTabSize() {
        return wrappedPCfg.getTabSize();
    }
    
}
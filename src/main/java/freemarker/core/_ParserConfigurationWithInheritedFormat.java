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
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public final class _ParserConfigurationWithInheritedFormat implements ParserConfiguration {

    private final OutputFormat outputFormat;
    private final Integer autoEscaping;
    private final ParserConfiguration wrappedPCfg;

    public _ParserConfigurationWithInheritedFormat(ParserConfiguration wrappedPCfg, OutputFormat outputFormat,
            Integer autoEscaping) {
        this.outputFormat = outputFormat;
        this.autoEscaping = autoEscaping;
        this.wrappedPCfg = wrappedPCfg;
    }

    public boolean getWhitespaceStripping() {
        return wrappedPCfg.getWhitespaceStripping();
    }

    public int getTagSyntax() {
        return wrappedPCfg.getTagSyntax();
    }

    public boolean getStrictSyntaxMode() {
        return wrappedPCfg.getStrictSyntaxMode();
    }

    public OutputFormat getOutputFormat() {
        return outputFormat != null ? outputFormat : wrappedPCfg.getOutputFormat();
    }

    public boolean getRecognizeStandardFileExtensions() {
        return false;
    }

    public int getNamingConvention() {
        return wrappedPCfg.getNamingConvention();
    }

    public Version getIncompatibleImprovements() {
        return wrappedPCfg.getIncompatibleImprovements();
    }

    public int getAutoEscaping() {
        return autoEscaping != null ? autoEscaping.intValue() : wrappedPCfg.getAutoEscaping();
    }

    public ArithmeticEngine getArithmeticEngine() {
        return wrappedPCfg.getArithmeticEngine();
    }
}
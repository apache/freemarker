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

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * <b>Don't implement this interface yourself</b>; use the existing implementation(s). This interface is implemented by
 * classes that hold settings that affect parsing. New parser settings can be added in new FreeMarker versions, which
 * will break your implementation.
 * 
 * @since 2.3.24
 */
public interface ParserConfiguration {

    /**
     * See {@link Configuration#getTagSyntax()}.
     */
    int getTagSyntax();

    /**
     * See {@link Configuration#getNamingConvention()}.
     */
    int getNamingConvention();

    /**
     * See {@link Configuration#getWhitespaceStripping()}.
     */
    boolean getWhitespaceStripping();

    /**
     * Overlaps with {@link Configurable#getArithmeticEngine()}; the parser needs this for creating numerical literals.
     */
    ArithmeticEngine getArithmeticEngine();
    
    /**
     * See {@link Configuration#getStrictSyntaxMode()}.
     */
    boolean getStrictSyntaxMode();
    
    /**
     * See {@link Configuration#getAutoEscaping()}.
     */
    boolean getAutoEscaping();
    
    /**
     * See {@link Configuration#getOutputEncoding()}.
     */
    OutputFormat<?> getOutputFormat();
    
    /**
     * See {@link Configuration#getIncompatibleImprovements()}.
     */
    Version getIncompatibleImprovements();

}

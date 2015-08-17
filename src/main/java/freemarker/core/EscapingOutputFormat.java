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

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;

/**
 * Common superclass for implementing {@link OutputFormat}-s that use a {@link CommonEscapingTemplateOutputModel} subclass.
 * 
 * @since 2.3.24
 */
public abstract class EscapingOutputFormat<TOM extends EscapingTemplateOutputModel> extends OutputFormat {

    protected EscapingOutputFormat() {
        // Only to decrease visibility
    }
    
    /**
     * Prints the parameter model to the output.
     */
    public abstract void output(TOM tom, Writer out) throws IOException, TemplateModelException;

    /**
     * Equivalent to calling {@link #escapePlainText(String)} and then
     * {@link #output(EscapingTemplateOutputModel, Writer)}, but implementators should chose a more efficient way.
     */
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    /**
     * Converts {@link String} that's assumed to be plain text to {@link EscapingTemplateOutputModel}, by escaping any special
     * characters in the plain text. This corresponds to {@code ?esc}, or, to outputting with auto-escaping if that
     * wasn't using {@link #output(String, Writer)} as an optimization.
     */
    public abstract TOM escapePlainText(String textToEsc) throws TemplateModelException;

    /**
     * If this {@link EscapingTemplateOutputModel} was created with {@link #escapePlainText(String)}, it returns the
     * original plain text, otherwise it might returns {@code null}. Needed for re-escaping, like in
     * {@code alreadyTOM?attrEsc}.
     */
    public abstract String getSourcePlainText(TOM tom) throws TemplateModelException;

    /**
     * Wraps {@link String} that's already markup to {@link EscapingTemplateOutputModel} interface, to indicate its
     * format. This corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if
     * the parameter markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter
     * until, and if ever, that becomes necessary.)
     */
    public abstract TOM fromMarkup(String markupText) throws TemplateModelException;

    /**
     * Returns the content as markup text. If this {@link EscapingTemplateOutputModel} was created with
     * {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required as far
     * as the returned markup means the same.
     */
    public abstract String getMarkup(TOM tom) throws TemplateModelException;
    
    /**
     * Returns a {@link EscapingTemplateOutputModel} that contains the content of both {@link EscapingTemplateOutputModel} concatenated.  
     */
    public abstract TOM concat(TOM tom1, TOM tom2) throws TemplateModelException;
    
    /**
     * Tells if a string built-in that can't handle a {@link EscapingTemplateOutputModel} left operand can bypass this object
     * as is. A typical such case would be when a {@link EscapingTemplateOutputModel} of "HTML" format bypasses {@code ?html}.
     */
    public abstract boolean isLegacyBuiltInBypassed(String builtInName) throws TemplateModelException;
    
}

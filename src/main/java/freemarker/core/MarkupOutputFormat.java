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
 * An {@link OutputFormat}-s that represent a "markup", which is any format where certain character sequences have
 * special meaning and thus may need escaping. Escaping is important for FreeMarker, as typically it has to insert
 * non-markup data from the data-model.
 * 
 * @since 2.3.24
 */
public abstract class MarkupOutputFormat<MO extends TemplateMarkupOutputModel> extends OutputFormat {

    protected MarkupOutputFormat() {
        // Only to decrease visibility
    }
    
    /**
     * Converts a {@link String} that's assumed to be plain text to {@link TemplateMarkupOutputModel}, by escaping any
     * special characters in the plain text. This corresponds to {@code ?esc}, or, to outputting with auto-escaping if
     * that wasn't using {@link #output(String, Writer)} as an optimization.
     * 
     * @see #escapePlainText(String)
     * @see #getSourcePlainText(TemplateMarkupOutputModel)
     */
    public abstract MO fromPlainTextByEscaping(String textToEsc) throws TemplateModelException;

    /**
     * Wraps {@link String} that's already markup to {@link TemplateMarkupOutputModel} interface, to indicate its
     * format. This corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if
     * the parameter markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter
     * until, and if ever, that becomes necessary.)
     * 
     * @see #getMarkup(TemplateMarkupOutputModel)
     */
    public abstract MO fromMarkup(String markupText) throws TemplateModelException;

    /**
     * Prints the parameter model to the output.
     */
    public abstract void output(MO mo, Writer out) throws IOException, TemplateModelException;

    /**
     * Equivalent to calling {@link #fromPlainTextByEscaping(String)} and then
     * {@link #output(TemplateMarkupOutputModel, Writer)}, but implementators should chose a more efficient way.
     */
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    /**
     * If this {@link TemplateMarkupOutputModel} was created with {@link #fromPlainTextByEscaping(String)}, it returns the
     * original plain text, otherwise it might returns {@code null}. Used when converting between different type of 
     * markups and the source was made from plain text.
     */
    public abstract String getSourcePlainText(MO mo) throws TemplateModelException;

    /**
     * Returns the content as markup text; never {@code null}. If this {@link TemplateMarkupOutputModel} was created
     * with {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required
     * as far as the returned markup means the same. If this {@link TemplateMarkupOutputModel} wasn't created
     * with {@link #fromMarkup(String)} and it doesn't yet have to markup, it has to generate the markup now.
     */
    public abstract String getMarkup(MO mo) throws TemplateModelException;
    
    /**
     * Returns a {@link TemplateMarkupOutputModel} that contains the content of both {@link TemplateMarkupOutputModel}
     * objects concatenated.
     */
    public abstract MO concat(MO mo1, MO mo2) throws TemplateModelException;
    
    /**
     * Should give the same result as {@link #fromPlainTextByEscaping(String)} and then
     * {@link #getMarkup(TemplateMarkupOutputModel)}, but the implementation may uses a more efficient approach.
     */
    public abstract String escapePlainText(String plainTextContent) throws TemplateModelException;

    /**
     * Tells if a string built-in that can't handle a {@link TemplateMarkupOutputModel} left operand can bypass this
     * object as is. A typical such case would be when a {@link TemplateHTMLOutputModel} of "HTML" format bypasses
     * {@code ?html}.
     */
    public abstract boolean isLegacyBuiltInBypassed(String builtInName) throws TemplateModelException;
    
}

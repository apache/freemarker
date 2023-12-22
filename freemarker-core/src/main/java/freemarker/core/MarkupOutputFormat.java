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

import java.io.IOException;
import java.io.Writer;

import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;

/**
 * Superclass of {@link OutputFormat}-s that represent a "markup" format, which is any format where certain character
 * sequences have special meaning, and thus may need escaping. (Escaping is important for FreeMarker, as typically it
 * has to insert non-markup text from the data-model into the output markup. See also:
 * {@link Configuration#setOutputFormat(OutputFormat)}.)
 * 
 * <p>
 * A {@link MarkupOutputFormat} subclass always has a corresponding {@link TemplateMarkupOutputModel} subclass pair
 * (like {@link HTMLOutputFormat} has {@link TemplateHTMLOutputModel}). The {@link OutputFormat} implements the
 * operations related to {@link TemplateMarkupOutputModel} objects of that kind, while the
 * {@link TemplateMarkupOutputModel} only encapsulates the data (the actual markup or text).
 * 
 * <p>
 * To implement a custom output format, you may want to extend {@link CommonMarkupOutputFormat}.
 * 
 * @param <MO>
 *            The {@link TemplateMarkupOutputModel} class this output format can deal with.
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
     * Wraps a {@link String} that's already markup to {@link TemplateMarkupOutputModel} interface, to indicate its
     * format. This corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if
     * the parameter markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter
     * until, and if ever, that becomes necessary.)
     * 
     * @see #getMarkupString(TemplateMarkupOutputModel)
     */
    public abstract MO fromMarkup(String markupText) throws TemplateModelException;

    /**
     * Prints the parameter model to the output.
     */
    public abstract void output(MO mo, Writer out) throws IOException, TemplateModelException;

    /**
     * Equivalent to calling {@link #fromPlainTextByEscaping(String)} and then
     * {@link #output(TemplateMarkupOutputModel, Writer)}, but the implementation may use a more efficient solution.
     */
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    /**
     * Outputs a value from a foreign output format; only used if {@link #isOutputFormatMixingAllowed()} return
     * {@code true}. The default implementation in {@link MarkupOutputFormat} will just let the other
     * {@link OutputFormat} to output value, but it can be overridden to support more nuanced conversions, or to check if outputting without
     * conversion should be allowed.
     *
     * @since 2.3.32
     */
    public <MO2 extends TemplateMarkupOutputModel<MO2>> void outputForeign(MO2 mo, Writer out) throws IOException, TemplateModelException {
        mo.getOutputFormat().output(mo, out);
    }

    /**
     * If this {@link TemplateMarkupOutputModel} was created with {@link #fromPlainTextByEscaping(String)}, it returns
     * the original plain text, otherwise it returns {@code null}. Useful for converting between different types
     * of markups, as if the source format can be converted to plain text without loss, then that just has to be
     * re-escaped with the target format to do the conversion.
     */
    public abstract String getSourcePlainText(MO mo) throws TemplateModelException;

    /**
     * Returns the content as markup text; never {@code null}. If this {@link TemplateMarkupOutputModel} was created
     * with {@link #fromMarkup(String)}, it might return the original markup text literally, but this is not required
     * as far as the returned markup means the same. If this {@link TemplateMarkupOutputModel} wasn't created
     * with {@link #fromMarkup(String)} and it doesn't yet have the markup, it has to generate the markup now.
     */
    public abstract String getMarkupString(MO mo) throws TemplateModelException;
    
    /**
     * Returns a {@link TemplateMarkupOutputModel} that contains the content of both {@link TemplateMarkupOutputModel}
     * objects concatenated.
     */
    public abstract MO concat(MO mo1, MO mo2) throws TemplateModelException;
    
    /**
     * Should give the same result as {@link #fromPlainTextByEscaping(String)} and then
     * {@link #getMarkupString(TemplateMarkupOutputModel)}, but the implementation may use a more efficient solution.
     */
    public abstract String escapePlainText(String plainTextContent) throws TemplateModelException;

    /**
     * Returns if the markup is empty (0 length). This is used by at least {@code ?hasContent}.
     */
    public abstract boolean isEmpty(MO mo) throws TemplateModelException;
    
    /**
     * Tells if a string built-in that can't handle a {@link TemplateMarkupOutputModel} left hand operand can bypass
     * this object as is. A typical such case would be when a {@link TemplateHTMLOutputModel} of "HTML" format bypasses
     * {@code ?html}.
     */
    public abstract boolean isLegacyBuiltInBypassed(String builtInName) throws TemplateModelException;
    
    /**
     * Tells if by default auto-escaping should be on for this format. It should be {@code true} if you need to escape
     * on most of the places where you insert values.
     * 
     * @see Configuration#setAutoEscapingPolicy(int)
     */
    public abstract boolean isAutoEscapedByDefault();
    
}

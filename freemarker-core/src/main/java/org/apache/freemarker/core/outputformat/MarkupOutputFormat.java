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
package org.apache.freemarker.core.outputformat;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.outputformat.impl.TemplateHTMLOutputModel;

/**
 * Superclass of {@link OutputFormat}-s that represent a "markup" format, which is any format where certain character
 * sequences have special meaning and thus may need escaping. (Escaping is important for FreeMarker, as typically it has
 * to insert non-markup text from the data-model into the output markup. See also the
 * {@link Configuration#getOutputFormat() outputFormat} configuration setting.)
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
    public abstract MO fromPlainTextByEscaping(String textToEsc) throws TemplateException;

    /**
     * Wraps a {@link String} that's already markup to {@link TemplateMarkupOutputModel} interface, to indicate its
     * format. This corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateException} if
     * the parameter markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter
     * until, and if ever, that becomes necessary.)
     * 
     * @see #getMarkupString(TemplateMarkupOutputModel)
     */
    public abstract MO fromMarkup(String markupText) throws TemplateException;

    /**
     * Prints the parameter model to the output.
     */
    public abstract void output(MO mo, Writer out) throws IOException, TemplateException;

    /**
     * Equivalent to calling {@link #fromPlainTextByEscaping(String)} and then
     * {@link #output(TemplateMarkupOutputModel, Writer)}, but the implementation may uses a more efficient solution.
     */
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateException;
    
    /**
     * If this {@link TemplateMarkupOutputModel} was created with {@link #fromPlainTextByEscaping(String)}, it returns
     * the original plain text, otherwise it returns {@code null}. Useful for converting between different types
     * of markups, as if the source format can be converted to plain text without loss, then that just has to be
     * re-escaped with the target format to do the conversion.
     */
    public abstract String getSourcePlainText(MO mo) throws TemplateException;

    /**
     * Returns the content as markup text; never {@code null}. If this {@link TemplateMarkupOutputModel} was created
     * with {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required
     * as far as the returned markup means the same. If this {@link TemplateMarkupOutputModel} wasn't created
     * with {@link #fromMarkup(String)} and it doesn't yet have the markup, it has to generate the markup now.
     */
    public abstract String getMarkupString(MO mo) throws TemplateException;
    
    /**
     * Returns a {@link TemplateMarkupOutputModel} that contains the content of both {@link TemplateMarkupOutputModel}
     * objects concatenated.
     */
    public abstract MO concat(MO mo1, MO mo2) throws TemplateException;
    
    /**
     * Should give the same result as {@link #fromPlainTextByEscaping(String)} and then
     * {@link #getMarkupString(TemplateMarkupOutputModel)}, but the implementation may uses a more efficient solution.
     */
    public abstract String escapePlainText(String plainTextContent) throws TemplateException;

    /**
     * Returns if the markup is empty (0 length). This is used by at least {@code ?hasContent}.
     */
    public abstract boolean isEmpty(MO mo) throws TemplateException;
    
    /**
     * Tells if a string built-in that can't handle a {@link TemplateMarkupOutputModel} left hand operand can bypass
     * this object as is. A typical such case would be when a {@link TemplateHTMLOutputModel} of "HTML" format bypasses
     * {@code ?html}.
     */
    public abstract boolean isLegacyBuiltInBypassed(String builtInName) throws TemplateException;
    
    /**
     * Tells if by default auto-escaping should be on for this format. It should be {@code true} if you need to escape
     * on most of the places where you insert values.
     * 
     * @see Configuration#getAutoEscapingPolicy()
     */
    public abstract boolean isAutoEscapedByDefault();
    
}

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
import freemarker.template.TemplateOutputModel;

/**
 * Encapsulates the {@link TemplateOutputModel} factories and {@code TemplateOutputModel} operations, and other meta
 * information (like MIME type) about a certain output format.
 * 
 * @since 2.3.24
 */
public abstract class OutputFormat<TOM extends TemplateOutputModel> {

    /**
     * Equivalent to calling {@link #getMarkup(TemplateOutputModel)} and then {@link Writer#write(String)}, but
     * implementators should chose a more efficient way.
     */
    abstract void output(TOM tom, Writer out) throws IOException, TemplateModelException;

    /**
     * Equivalent to calling {@link #escapePlainText(String)} and then {@link #output(TemplateOutputModel, Writer)}, but
     * implementators should chose a more efficient way.
     */
    abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;

    /**
     * Converts {@link String} that's assumed to be plain text to {@link TemplateOutputModel}, by escaping any special
     * characters in the plain text. This corresponds to {@code ?esc}, or, to outputting with auto-escaping if that
     * wasn't using {@link #output(String, Writer)} as an optimization.
     */
    abstract TOM escapePlainText(String textToEsc) throws TemplateModelException;

    /**
     * If this {@link TemplateOutputModel} was created with {@link #escapePlainText(String)}, it returns the original
     * plain text, otherwise it might returns {@code null}. Needed for re-escaping, like in {@code alreadyTOM?attrEsc}.
     */
    abstract String getSourcePlainText(TOM tom);

    /**
     * Wraps {@link String} that's already markup to {@link TemplateOutputModel} interface, to indicate its format. This
     * corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if the parameter
     * markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter until, and if
     * ever, that becomes necessary.) 
     */
    abstract TOM fromMarkup(String markupText) throws TemplateModelException;

    /**
     * Returns the content as markup text. If this {@link TemplateOutputModel} was created with
     * {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required as far
     * as the returned markup means the same.
     */
    abstract String getMarkup(TOM tom) throws TemplateModelException;

    /**
     * Returns the MIME type of the output format. This might comes handy when generating generating a HTTP response. 
     */
    abstract String getMimeType();

    /**
     * Tells if a string built-in that can't handle a {@link TemplateOutputModel} left operand can bypass this object
     * as is. A typical such case would be when a {@link TemplateOutputModel} of "HTML" format bypasses {@code ?html}.
     */
    abstract boolean isLegacyBuiltInBypassed(String builtInName);
    
    /**
     * Returns a {@link TemplateOutputModel} that contains the content of both {@link TemplateOutputModel} concatenated.  
     */
    abstract TOM concat(TOM tom1, TOM tom2) throws TemplateModelException;

}

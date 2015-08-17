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
public abstract class CommonEscapingOutputFormat<TOM extends CommonEscapingTemplateOutputModel>
extends EscapingOutputFormat<TOM> {

    protected CommonEscapingOutputFormat() {
        // Only to decrease visibility
    }
    
    /**
     * Prints the parameter model to the output.
     */
    @Override
    public final void output(TOM tom, Writer out) throws IOException, TemplateModelException {
        String mc = tom.getMarkupContent();
        if (mc != null) {
            out.write(mc);
        } else {
            output(tom.getPlainTextContent(), out);
        }
    }

    /**
     * Equivalent to calling {@link #escapePlainText(String)} and then
     * {@link #output(CommonEscapingTemplateOutputModel, Writer)}, but implementators should chose a more efficient way.
     */
    @Override
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    /**
     * Converts {@link String} that's assumed to be plain text to {@link EscapingTemplateOutputModel}, by escaping any special
     * characters in the plain text. This corresponds to {@code ?esc}, or, to outputting with auto-escaping if that
     * wasn't using {@link #output(String, Writer)} as an optimization.
     */
    @Override
    public final TOM escapePlainText(String textToEsc) throws TemplateModelException {
        return newTOM(textToEsc, null);
    }

    /**
     * If this {@link EscapingTemplateOutputModel} was created with {@link #escapePlainText(String)}, it returns the original
     * plain text, otherwise it might returns {@code null}. Needed for re-escaping, like in {@code alreadyTOM?attrEsc}.
     */
    @Override
    public final String getSourcePlainText(TOM tom) {
        return tom.getPlainTextContent();
    }

    /**
     * Wraps {@link String} that's already markup to {@link EscapingTemplateOutputModel} interface, to indicate its format. This
     * corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if the parameter
     * markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter until, and if
     * ever, that becomes necessary.) 
     */
    @Override
    public final TOM fromMarkup(String markupText) throws TemplateModelException {
        return newTOM(null, markupText);
    }

    /**
     * Returns the content as markup text. If this {@link EscapingTemplateOutputModel} was created with
     * {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required as far
     * as the returned markup means the same.
     */
    @Override
    public final String getMarkup(TOM tom) {
        String mc = tom.getMarkupContent();
        if (mc != null) {
            return mc;
        }
        
        mc = escapePlainTextToString(tom.getPlainTextContent());
        tom.setMarkupContet(mc);
        return mc;
    }
    
    /**
     * Returns a {@link EscapingTemplateOutputModel} that contains the content of both {@link EscapingTemplateOutputModel} concatenated.  
     */
    @Override
    public final TOM concat(TOM tom1, TOM tom2) {
        String pc1 = tom1.getPlainTextContent();
        String mc1 = tom1.getMarkupContent();
        String pc2 = tom2.getPlainTextContent();
        String mc2 = tom2.getMarkupContent();
        
        String pc3 = pc1 != null && pc2 != null ? pc1 + pc2 : null;
        String mc3 = mc1 != null && mc2 != null ? mc1 + mc2 : null;
        if (pc3 != null || mc3 != null) {
            return newTOM(pc3, mc3);
        }
        
        if (pc1 != null) {
            return newTOM(null, getMarkup(tom1) + mc2);
        } else {
            return newTOM(null, mc1 + getMarkup(tom2));
        }
    }
    
    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

    protected abstract String escapePlainTextToString(String plainTextContent);

    protected abstract TOM newTOM(String plainTextContent, String markupContent);

    /**
     * Tells if a string built-in that can't handle a {@link EscapingTemplateOutputModel} left operand can bypass this object
     * as is. A typical such case would be when a {@link EscapingTemplateOutputModel} of "HTML" format bypasses {@code ?html}.
     */
    @Override
    public abstract boolean isLegacyBuiltInBypassed(String builtInName);
    
}

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
 * Common superclass for implementing {@link MarkupOutputFormat}-s that use a {@link CommonTemplateMarkupOutputModel}
 * subclass.
 * 
 * @since 2.3.24
 */
public abstract class CommonMarkupOutputFormat<MO extends CommonTemplateMarkupOutputModel>
        extends MarkupOutputFormat<MO> {

    protected CommonMarkupOutputFormat() {
        // Only to decrease visibility
    }
    
    /**
     * Prints the parameter model to the output.
     */
    @Override
    public final void output(MO mo, Writer out) throws IOException, TemplateModelException {
        String mc = mo.getMarkupContent();
        if (mc != null) {
            out.write(mc);
        } else {
            output(mo.getPlainTextContent(), out);
        }
    }

    /**
     * Equivalent to calling {@link #escapePlainText(String)} and then
     * {@link #output(CommonTemplateMarkupOutputModel, Writer)}, but implementators should chose a more efficient way.
     */
    @Override
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    /**
     * Converts {@link String} that's assumed to be plain text to {@link TemplateMarkupOutputModel}, by escaping any special
     * characters in the plain text. This corresponds to {@code ?esc}, or, to outputting with auto-escaping if that
     * wasn't using {@link #output(String, Writer)} as an optimization.
     */
    @Override
    public final MO escapePlainText(String textToEsc) throws TemplateModelException {
        return newTOM(textToEsc, null);
    }

    /**
     * If this {@link TemplateMarkupOutputModel} was created with {@link #escapePlainText(String)}, it returns the original
     * plain text, otherwise it might returns {@code null}. Needed for re-escaping, like in {@code alreadyTOM?attrEsc}.
     */
    @Override
    public final String getSourcePlainText(MO mo) {
        return mo.getPlainTextContent();
    }

    /**
     * Wraps {@link String} that's already markup to {@link TemplateMarkupOutputModel} interface, to indicate its format. This
     * corresponds to {@code ?noEsc}. (This methods is allowed to throw {@link TemplateModelException} if the parameter
     * markup text is malformed, but it's unlikely that an implementation chooses to parse the parameter until, and if
     * ever, that becomes necessary.) 
     */
    @Override
    public final MO fromMarkup(String markupText) throws TemplateModelException {
        return newTOM(null, markupText);
    }

    /**
     * Returns the content as markup text. If this {@link TemplateMarkupOutputModel} was created with
     * {@link #fromMarkup(String)}, it might returns the original markup text literally, but this is not required as far
     * as the returned markup means the same.
     */
    @Override
    public final String getMarkup(MO mo) {
        String mc = mo.getMarkupContent();
        if (mc != null) {
            return mc;
        }
        
        mc = escapePlainTextToString(mo.getPlainTextContent());
        mo.setMarkupContet(mc);
        return mc;
    }
    
    /**
     * Returns a {@link TemplateMarkupOutputModel} that contains the content of both {@link TemplateMarkupOutputModel} concatenated.  
     */
    @Override
    public final MO concat(MO tom1, MO tom2) {
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

    protected abstract MO newTOM(String plainTextContent, String markupContent);

    /**
     * Tells if a string built-in that can't handle a {@link TemplateMarkupOutputModel} left operand can bypass this object
     * as is. A typical such case would be when a {@link TemplateMarkupOutputModel} of "HTML" format bypasses {@code ?html}.
     */
    @Override
    public abstract boolean isLegacyBuiltInBypassed(String builtInName);
    
}

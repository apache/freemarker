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
 * Common superclass for implementing {@link OutputFormat}-s that use a {@link GenericTemplateOutputModel} subclass.
 * 
 * @since 2.3.24
 */
public abstract class GenericOutputFormat<TOM extends GenericTemplateOutputModel> extends OutputFormat<TOM> {

    protected GenericOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public final void output(TOM tom, Writer out) throws IOException, TemplateModelException {
        String mc = tom.getMarkupContent();
        if (mc != null) {
            out.write(mc);
        } else {
            output(tom.getPlainTextContent(), out);
        }
    }

    @Override
    public final TOM escapePlainText(String textToEsc) throws TemplateModelException {
        return newTOM(textToEsc, null);
    }

    @Override
    public final String getSourcePlainText(TOM tom) {
        return tom.getPlainTextContent();
    }

    @Override
    public final TOM fromMarkup(String markupText) throws TemplateModelException {
        return newTOM(null, markupText);
    }

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
    public boolean isEscaping() {
        return true;
    }

    protected abstract String escapePlainTextToString(String plainTextContent);

    protected abstract TOM newTOM(String plainTextContent, String markupContent);

}

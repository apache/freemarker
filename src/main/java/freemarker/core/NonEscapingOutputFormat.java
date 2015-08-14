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
 * Represents the output format that doesn't have escaping.
 * 
 * @since 2.3.24
 */
public abstract class NonEscapingOutputFormat<TOM extends TemplateOutputModel> extends OutputFormat<TOM> {
    
    private static String UNSUPPORTED_MESSAGE = "This operation isn't supported for this output format: ";
    
    protected NonEscapingOutputFormat() {
        // Only to decrease visibility
    }

    @Override
    public void output(TOM tom, Writer out) throws IOException, TemplateModelException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        out.write(textToEsc);
    }

    @Override
    public TOM escapePlainText(String textToEsc) throws TemplateModelException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public String getSourcePlainText(TOM tom) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public TOM fromMarkup(String markupText) throws TemplateModelException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public String getMarkup(TOM tom) throws TemplateModelException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public TOM concat(TOM tom1, TOM tom2)
            throws TemplateModelException {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE + toString());
    }

    @Override
    public final boolean isEscaping() {
        return false;
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }

    @Override
    public abstract String getCommonName();

    @Override
    public abstract String getMimeType();
    
}

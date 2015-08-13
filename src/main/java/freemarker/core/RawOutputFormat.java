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
 * Represents the output format that doesn't have escaping and MIME type.
 * 
 * @since 2.3.24
 */
public final class RawOutputFormat extends OutputFormat<RawTemplateOutputModel> {

    public static final RawOutputFormat INSTANCE = new RawOutputFormat();
    
    private RawOutputFormat() {
        // Only to decrease visibility
    }

    @Override
    public void output(RawTemplateOutputModel tom, Writer out) throws IOException, TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel escapePlainText(String textToEsc) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourcePlainText(RawTemplateOutputModel tom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel fromMarkup(String markupText) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMarkup(RawTemplateOutputModel tom) throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RawTemplateOutputModel concat(RawTemplateOutputModel tom1, RawTemplateOutputModel tom2)
            throws TemplateModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEscaping() {
        return false;
    }

    @Override
    public String getCommonName() {
        return "raw";
    }

    @Override
    public String getMimeType() {
        return null;
    }

}

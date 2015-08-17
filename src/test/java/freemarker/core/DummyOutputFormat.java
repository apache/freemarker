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

public class DummyOutputFormat extends CommonMarkupOutputFormat<DummyTemplateOutputModel> {
    
    public static final DummyOutputFormat INSTANCE = new DummyOutputFormat();
    
    private DummyOutputFormat() {
        // hide
    }

    @Override
    protected String escapePlainTextToString(String plainTextContent) {
        return plainTextContent.replaceAll("(\\.|\\\\)", "\\\\$1");
    }

    @Override
    protected DummyTemplateOutputModel newTOM(String plainTextContent, String markupContent) {
        return new DummyTemplateOutputModel(plainTextContent, markupContent);
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        out.write(escapePlainTextToString(textToEsc));
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return false;
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getMimeType() {
        return "text/dummy";
    }
    
}
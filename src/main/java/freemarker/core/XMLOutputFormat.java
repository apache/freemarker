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
import freemarker.template.utility.StringUtil;

/**
 * Represents the XML output format.
 * 
 * @since 2.3.24
 */
public final class XMLOutputFormat extends CommonMarkupOutputFormat<TemplateXMLOutputModel> {

    public static final XMLOutputFormat INSTANCE = new XMLOutputFormat();
    
    private XMLOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        StringUtil.XMLEnc(textToEsc, out);
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("xml") || builtInName.equals("xhtml");
    }

    @Override
    public String getName() {
        return "XML";
    }

    @Override
    public String getMimeType() {
        return "text/xml";
    }

    @Override
    protected String escapePlainTextToString(String plainTextContent) {
        return StringUtil.XMLEnc(plainTextContent);
    }

    @Override
    protected TemplateXMLOutputModel newTOM(String plainTextContent, String markupContent) {
        return new TemplateXMLOutputModel(plainTextContent, markupContent);
    }

}

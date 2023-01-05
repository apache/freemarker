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

import freemarker.template.TemplateModelException;
import freemarker.core._TemplateModelException;
import freemarker.core._DelayedToString;

public class DummyOutputFormat extends CommonMarkupOutputFormat<TemplateDummyOutputModel> {
    
    public static final DummyOutputFormat INSTANCE = new DummyOutputFormat();
    
    private DummyOutputFormat() {
        // hide
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getMimeType() {
        return "text/dummy";
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        out.write(escapePlainText(textToEsc));
    }

    @Override
    public boolean isOutputFormatMixingAllowed() {
        return true;
    }

    @Override
    public <MO extends TemplateMarkupOutputModel<MO>> void outputForeign(MO mo, Writer out) throws IOException, TemplateModelException {
        if (mo.getOutputFormat().getMimeType().equals("text/html")) {
            mo.getOutputFormat().output(mo, out);
        } else {
            throw new _TemplateModelException("DummyOutputFormat is incompatible with ", new _DelayedToString(mo.getOutputFormat()));
        }
    }

    @Override
    public String escapePlainText(String plainTextContent) {
        return plainTextContent.replaceAll("(\\.|\\\\)", "\\\\$1");
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) {
        return false;
    }

    @Override
    protected TemplateDummyOutputModel newTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        return new TemplateDummyOutputModel(plainTextContent, markupContent);
    }
    
}

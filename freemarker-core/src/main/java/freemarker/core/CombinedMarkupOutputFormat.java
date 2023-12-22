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

/**
 * Represents two markup formats nested into each other. For example, markdown nested into HTML.
 * 
 * @since 2.3.24
 */
public final class CombinedMarkupOutputFormat extends CommonMarkupOutputFormat<TemplateCombinedMarkupOutputModel> {

    private final String name;
    
    private final MarkupOutputFormat outer;
    private final MarkupOutputFormat inner;

    /**
     * Same as {@link #CombinedMarkupOutputFormat(String, MarkupOutputFormat, MarkupOutputFormat)} with {@code null} as
     * the {@code name} parameter.
     */
    public CombinedMarkupOutputFormat(MarkupOutputFormat outer, MarkupOutputFormat inner) {
        this(null, outer, inner);
    }
    
    /**
     * @param name
     *            Maybe {@code null}, in which case it defaults to
     *            <code>outer.getName() + "{" + inner.getName() + "}"</code>.
     */
    public CombinedMarkupOutputFormat(String name, MarkupOutputFormat outer, MarkupOutputFormat inner) {
        this.name = name != null ? null : outer.getName() + "{" + inner.getName() + "}";
        this.outer = outer;
        this.inner = inner;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMimeType() {
        return outer.getMimeType();
    }

    @Override
    public void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        outer.output(inner.escapePlainText(textToEsc), out);
    }

    @Override
    public <MO2 extends TemplateMarkupOutputModel<MO2>> void outputForeign(MO2 mo, Writer out) throws IOException, TemplateModelException {
        outer.outputForeign(mo, out);
    }

    @Override
    public String escapePlainText(String plainTextContent) throws TemplateModelException {
        return outer.escapePlainText(inner.escapePlainText(plainTextContent));
    }

    @Override
    public boolean isLegacyBuiltInBypassed(String builtInName) throws TemplateModelException {
        return outer.isLegacyBuiltInBypassed(builtInName);
    }

    @Override
    public boolean isAutoEscapedByDefault() {
        return outer.isAutoEscapedByDefault();
    }
    
    @Override
    public boolean isOutputFormatMixingAllowed() {
        return outer.isOutputFormatMixingAllowed();
    }

    public MarkupOutputFormat getOuterOutputFormat() {
        return outer;
    }

    public MarkupOutputFormat getInnerOutputFormat() {
        return inner;
    }

    @Override
    protected TemplateCombinedMarkupOutputModel newTemplateMarkupOutputModel(
            String plainTextContent, String markupContent) {
        return new TemplateCombinedMarkupOutputModel(plainTextContent, markupContent, this);
    }
    
}

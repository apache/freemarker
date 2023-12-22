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
    
    @Override
    public final MO fromPlainTextByEscaping(String textToEsc) throws TemplateModelException {
        return newTemplateMarkupOutputModel(textToEsc, null);
    }

    @Override
    public final MO fromMarkup(String markupText) throws TemplateModelException {
        return newTemplateMarkupOutputModel(null, markupText);
    }

    @Override
    public final void output(MO mo, Writer out) throws IOException, TemplateModelException {
        String mc = mo.getMarkupContent();
        if (mc != null) {
            out.write(mc);
        } else {
            output(mo.getPlainTextContent(), out);
        }
    }

    @Override
    public abstract void output(String textToEsc, Writer out) throws IOException, TemplateModelException;
    
    @Override
    public final String getSourcePlainText(MO mo) throws TemplateModelException {
        return mo.getPlainTextContent();
    }

    @Override
    public final String getMarkupString(MO mo) throws TemplateModelException {
        String mc = mo.getMarkupContent();
        if (mc != null) {
            return mc;
        }
        
        mc = escapePlainText(mo.getPlainTextContent());
        mo.setMarkupContent(mc);
        return mc;
    }
    
    @Override
    public final MO concat(MO mo1, MO mo2) throws TemplateModelException {
        String pc1 = mo1.getPlainTextContent();
        String mc1 = mo1.getMarkupContent();
        String pc2 = mo2.getPlainTextContent();
        String mc2 = mo2.getMarkupContent();
        
        String pc3 = pc1 != null && pc2 != null ? pc1 + pc2 : null;
        String mc3 = mc1 != null && mc2 != null ? mc1 + mc2 : null;
        if (pc3 != null || mc3 != null) {
            return newTemplateMarkupOutputModel(pc3, mc3);
        }
        
        if (pc1 != null) {
            return newTemplateMarkupOutputModel(null, getMarkupString(mo1) + mc2);
        } else {
            return newTemplateMarkupOutputModel(null, mc1 + getMarkupString(mo2));
        }
    }
    
    @Override
    public boolean isEmpty(MO mo) throws TemplateModelException {
        String s = mo.getPlainTextContent();
        if (s != null) {
            return s.length() == 0;
        }
        return mo.getMarkupContent().length() == 0;
    }
    
    @Override
    public boolean isOutputFormatMixingAllowed() {
        return false;
    }
    
    @Override
    public boolean isAutoEscapedByDefault() {
        return true;
    }

    /**
     * Creates a new {@link CommonTemplateMarkupOutputModel} that's bound to this {@link OutputFormat} instance.
     * When this is called from {@link CommonMarkupOutputFormat}, exactly one of the parameters will be
     * non-{@code null}.
     *
     * @param plainTextContent
     *      {@code null} if the content is markup that possibly can be described as plain text without loss.
     * @param markupContent
     *      Typically will be {@code null} if {@code plainTextContent} is not {@code null}. While the plain text always
     *      can be converted to markup via {@link #escapePlainText(String)}, it's up to the implementation if it wants
     *      to do that now and store the markup, or later on demand (like each time when
     *      {@link #getMarkupString(CommonTemplateMarkupOutputModel)} is called).
     */
    protected abstract MO newTemplateMarkupOutputModel(String plainTextContent, String markupContent)
            throws TemplateModelException;
    
}

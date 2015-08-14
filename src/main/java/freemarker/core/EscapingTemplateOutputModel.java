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

import freemarker.template.TemplateOutputModel;

/**
 * Common superclass for implementing {@link TemplateOutputModel}-s.
 * 
 * <p>
 * Thread-safe after proper publishing. Calculated fields (typically, the markup calculated from plain text) might will
 * be re-calculated for multiple times if accessed from multiple threads (this only affects performance, not
 * functionality).
 * 
 * @since 2.3.24
 */
public abstract class EscapingTemplateOutputModel<TOM extends EscapingTemplateOutputModel<TOM>>
        implements TemplateOutputModel<TOM> {

    private final String plainTextContent;
    private String markupContet;

    /**
     * A least one of the parameters must be non-{@code null}!
     */
    EscapingTemplateOutputModel(String plainTextContent, String markupContent) {
        this.plainTextContent = plainTextContent;
        this.markupContet = markupContent;
    }

    public abstract OutputFormat<TOM> getOutputFormat();

    /** Maybe {@code null}, but then the other field isn't {@code null}. */
    final String getPlainTextContent() {
        return plainTextContent;
    }

    /** Maybe {@code null}, but then the other field isn't {@code null}. */
    final String getMarkupContent() {
        return markupContet;
    }

    /** Use only to set {@code null} field to the value calculated from the other field! */
    final void setMarkupContet(String markupContet) {
        this.markupContet = markupContet;
    }

}

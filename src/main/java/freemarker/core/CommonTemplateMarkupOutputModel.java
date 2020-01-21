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

/**
 * Common superclass for implementing {@link TemplateMarkupOutputModel}-s that belong to a
 * {@link CommonMarkupOutputFormat} subclass format.
 * 
 * <p>
 * Thread-safe after proper publishing. Calculated fields (typically, the markup calculated from plain text) might will
 * be re-calculated for multiple times if accessed from multiple threads (this only affects performance, not
 * functionality).
 * 
 * @since 2.3.24
 */
public abstract class CommonTemplateMarkupOutputModel<MO extends CommonTemplateMarkupOutputModel<MO>>
        implements TemplateMarkupOutputModel<MO> {

    private final String plainTextContent;
    private String markupContent;

    /**
     * A least one of the parameters must be non-{@code null}!
     */
    protected CommonTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        this.plainTextContent = plainTextContent;
        this.markupContent = markupContent;
    }

    @Override
    public abstract CommonMarkupOutputFormat<MO> getOutputFormat();

    /** Maybe {@code null}, but then {@link #getMarkupContent()} isn't {@code null}. */
    final String getPlainTextContent() {
        return plainTextContent;
    }

    /** Maybe {@code null}, but then {@link #getPlainTextContent()} isn't {@code null}. */
    final String getMarkupContent() {
        return markupContent;
    }

    /**
     * Use only to set the value calculated from {@link #getPlainTextContent()}, when {@link #getMarkupContent()} was
     * still {@code null}!
     */
    final void setMarkupContent(String markupContent) {
        this.markupContent = markupContent;
    }

    /**
     * Returns something like {@code "markup(format=HTML, markup=<p>foo</p>)"}; where the first parameter is
     * {@link OutputFormat#getName()}, and the second is the content, that's prefixed with {@code markup=} or {@code
     * plainText=}, depending on the way the content is internally stored.
     *
     * @since 2.3.29
     */
    @Override
    public String toString() {
        return "markupOutput(format=" + getOutputFormat().getName() + ", " + (plainTextContent != null ?
            "plainText=" + plainTextContent : "markup=" + markupContent) + ")";
    }

}

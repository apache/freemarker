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
package org.apache.freemarker.core.ast;

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
    private String markupContet;

    /**
     * A least one of the parameters must be non-{@code null}!
     */
    protected CommonTemplateMarkupOutputModel(String plainTextContent, String markupContent) {
        this.plainTextContent = plainTextContent;
        this.markupContet = markupContent;
    }

    public abstract CommonMarkupOutputFormat<MO> getOutputFormat();

    /** Maybe {@code null}, but then {@link #getMarkupContent()} isn't {@code null}. */
    final String getPlainTextContent() {
        return plainTextContent;
    }

    /** Maybe {@code null}, but then {@link #getPlainTextContent()} isn't {@code null}. */
    final String getMarkupContent() {
        return markupContet;
    }

    /**
     * Use only to set the value calculated from {@link #getPlainTextContent()}, when {@link #getMarkupContent()} was
     * still {@code null}!
     */
    final void setMarkupContet(String markupContet) {
        this.markupContet = markupContet;
    }

}

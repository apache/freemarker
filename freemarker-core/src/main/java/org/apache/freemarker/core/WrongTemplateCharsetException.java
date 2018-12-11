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

package org.apache.freemarker.core;

import java.nio.charset.Charset;

/**
 * Thrown by the {@link Template} constructors that specify a non-{@code null} encoding whoch doesn't match the
 * encoding specified in the {@code #ftl} header of the template.
 */
public class WrongTemplateCharsetException extends ParseException {
    private static final long serialVersionUID = 1L;

    private final Charset templateSpecifiedEncoding;
    private final Charset constructorSpecifiedEncoding;

    /**
     */
    public WrongTemplateCharsetException(Charset templateSpecifiedEncoding, Charset constructorSpecifiedEncoding) {
        this.templateSpecifiedEncoding = templateSpecifiedEncoding;
        this.constructorSpecifiedEncoding = constructorSpecifiedEncoding;
    }

    @Override
    public String getMessage() {
        return "Encoding specified inside the template (" + templateSpecifiedEncoding
                + ") doesn't match the encoding specified for the Template constructor"
                + (constructorSpecifiedEncoding != null ? " (" + constructorSpecifiedEncoding + ")." : ".");
    }

    /**
     */
    public Charset getTemplateSpecifiedEncoding() {
        return templateSpecifiedEncoding;
    }

    /**
     */
    public Charset getConstructorSpecifiedEncoding() {
        return constructorSpecifiedEncoding;
    }

}

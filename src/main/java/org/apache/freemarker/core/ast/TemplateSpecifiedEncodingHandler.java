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

import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.Template.WrongEncodingException;

/**
 * Specifies the behavior when the template specifies its own encoding (via {@code <#ftl encoding=...>}) in the template
 * content itself, and also when it doesn't do that.
 */
public interface TemplateSpecifiedEncodingHandler {
    
    TemplateSpecifiedEncodingHandler DEFAULT = new TemplateSpecifiedEncodingHandler() {

        @Override
        public void handle(String templateSpecificEncoding, String constructorSpecifiedEncoding)
                throws WrongEncodingException {
            if (constructorSpecifiedEncoding != null && templateSpecificEncoding != null
                    && !constructorSpecifiedEncoding.equalsIgnoreCase(templateSpecificEncoding)) {
                throw new Template.WrongEncodingException(templateSpecificEncoding, constructorSpecifiedEncoding);
            }
        }
        
    };

    /**
     * Called once during template parsing, either when the {@code #ftl} directive is processed, or near the beginning
     * of the template processing when there's no {@code #ftl} directive in the template.
     * 
     * @param templateSpecificEncoding
     *            The encoding specified via {@code <#ftl encoding=...>}, or {@code null} if that was missing (either
     *            the {@code encoding} parameter or the whole {@code #ftl} directive).
     * @param constructorSpecifiedEncoding
     *            The encoding specified to the {@link Template} constructor; also the value of
     *            {@link Template#getEncoding()}. If there was an encoding used for decoding the template file, it
     *            should be that, or if there was no encoding, it should be {@code null}.
     * 
     * @throws WrongEncodingException
     *             If the template "file" has to be re-read and the {@link Template} re-created with the encoding
     *             specified in the constructor of {@link WrongEncodingException}.
     */
    void handle(String templateSpecificEncoding, String constructorSpecifiedEncoding) throws WrongEncodingException;
    
}

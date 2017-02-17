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

import java.io.InputStream;

import org.apache.freemarker.core.Template.WrongEncodingException;

/**
 * A {@link TemplateSpecifiedEncodingHandler} that discards the mark of the specified {@link InputStream} when
 * the template parsing gets to a point where it's known that we can't receive a template specified encoding anymore.
 * This allows freeing up the mark buffer early during parsing.
 * 
 * @since 2.3.16
 */
public class MarkReleaserTemplateSpecifiedEncodingHandler implements TemplateSpecifiedEncodingHandler {

    private final InputStream markedInputStream;

    /**
     * @param markedInputStream Input stream with marked template content start position
     */
    public MarkReleaserTemplateSpecifiedEncodingHandler(InputStream markedInputStream) {
        this.markedInputStream = markedInputStream;
    }

    @Override
    public void handle(String templateSpecificEncoding, String constructorSpecifiedEncoding)
            throws WrongEncodingException {
        TemplateSpecifiedEncodingHandler.DEFAULT.handle(templateSpecificEncoding, constructorSpecifiedEncoding);
        
        // There was no WrongEncodingException exception, release mark buffer:
        markedInputStream.mark(0); // 
    }

    public InputStream getMarkedInputStream() {
        return markedInputStream;
    }

}

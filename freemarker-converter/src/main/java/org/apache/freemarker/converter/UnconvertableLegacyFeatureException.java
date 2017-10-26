/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.converter;

import freemarker.core.TemplateObject;

/**
 * The legacy feature has no equivalent in the target format.
 */
@SuppressWarnings({ "deprecation", "serial" })
public class UnconvertableLegacyFeatureException extends ConverterException {

    /**
     * @param astNode The position of the error is extracted from this.
     */
    public UnconvertableLegacyFeatureException(String message, TemplateObject astNode) {
        this(message, astNode.getBeginLine(), astNode.getBeginColumn(), null);
    }

    /**
     * @param astNode The position of the error is extracted from this.
     */
    public UnconvertableLegacyFeatureException(String message, TemplateObject astNode, Throwable cause) {
        this(message, astNode.getBeginLine(), astNode.getBeginColumn(), cause);
    }
    
    /**
     * @param row 1-based
     * @param column 1-based
     */
    public UnconvertableLegacyFeatureException(String message, int row, int column) {
        this(message, row, column, null);
    }

    /**
     * @param row 1-based
     * @param column 1-based
     */
    public UnconvertableLegacyFeatureException(String message, int row, int column, Throwable cause) {
        super(message, row, column, cause);
    }

}

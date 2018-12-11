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

package org.apache.freemarker.core.model;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * "string" template language data-type; like in Java, an unmodifiable UNICODE character sequence.
 * When a template has to print a value of this class, it will assume that it stores plain text (not HTML, XML, etc.),
 * and thus it will be possibly auto-escaped. To avoid that, use the appropriate {@link TemplateMarkupOutputModel}
 * instead.
 */
public interface TemplateStringModel extends TemplateModel {

    /**
     * A constant value to use as the empty string.
     */
    TemplateModel EMPTY_STRING = new SimpleString("");

    /**
     * Returns the {@link String} representation of this model. Returning {@code null} is illegal, and may cause
     * exception in the calling code.
     */
    String getAsString() throws TemplateException;

}

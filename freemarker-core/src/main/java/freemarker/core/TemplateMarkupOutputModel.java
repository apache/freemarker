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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * "markup output" template language data-type; stores markup (some kind of "rich text" / structured format, as opposed
 * to plain text) that meant to be printed as template output. This type is related to the {@link OutputFormat}
 * mechanism. Values of this kind are exempt from {@link OutputFormat}-based automatic escaping.
 * 
 * <p>
 * Each implementation of this type has a corresponding {@link OutputFormat} subclass, whose singleton instance is
 * returned by {@link #getOutputFormat()}. See more about how markup output values work at {@link OutputFormat}.
 * 
 * <p>
 * Note that {@link TemplateMarkupOutputModel}-s are by design not treated like {@link TemplateScalarModel}-s, and so
 * the implementations of this interface usually shouldn't implement {@link TemplateScalarModel}. (Because, operations
 * applicable on plain strings, like converting to upper case, substringing, etc., can corrupt markup.) If the template
 * author wants to pass in the "source" of the markup as string somewhere, he should use {@code ?markup_string}.
 * 
 * @param <MO>
 *            Refers to the interface's own type, which is useful in interfaces that extend
 *            {@link TemplateMarkupOutputModel} (Java Generics trick).
 * 
 * @since 2.3.24
 */
public interface TemplateMarkupOutputModel<MO extends TemplateMarkupOutputModel<MO>> extends TemplateModel {

    /**
     * Returns the singleton {@link OutputFormat} object that implements the operations for the "markup output" value.
     */
    MarkupOutputFormat<MO> getOutputFormat();
    
}

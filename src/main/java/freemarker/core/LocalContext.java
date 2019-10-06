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

import java.util.Collection;

import freemarker.template.Configuration;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Represents a local context (a set of local variables); should be internal, but left public for backward
 * compatibility. This is used as the abstraction for accessing the local variables of a macro/function invocation,
 * the loops variables of {#code #list}, the nested content variables of a macro call, or the arguments to a lambda
 * expression.
 */
public interface LocalContext {

    /**
     * @return {@code null} if the variable doesn't exit. Since 2.3.29, if this context represents loop variables, this
     *     is possibly {@code freemarker.core.TemplateNullModel.INSTANCE} (an internal class) when
     *     {@link Configuration#setFallbackOnNullLoopVariable(boolean)} was set to {@code false}, in which
     *     case the caller must not fall back to higher scopes to find the variable, and treat the value as
     *     {@code null} in other respects. While this is in theory an incompatible change in 2.3.29, it's not a problem,
     *     it's very unlikely (hopefully impossible with published API-s) that user code gets a {@link LocalContext}
     *     that stores loop variables, and also because by default
     *     {@link Configuration#setFallbackOnNullLoopVariable(boolean)} is {@code true}.
     */
    TemplateModel getLocalVariable(String name) throws TemplateModelException;

    /**
     * The names of the local variables that were declared or set in this context.
     */
    Collection getLocalVariableNames() throws TemplateModelException;
    
}

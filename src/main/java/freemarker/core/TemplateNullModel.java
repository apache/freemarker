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

/**
 * Represents a {@code null} value; if we get this as the value of a variable from a scope, we do not fall back
 * to a higher scope to get the same variable again. If instead we get a {@code null}, that means that the variable
 * doesn't exist at all in the current scope, and so we fall back to a higher scope. This distinction is only
 * used for (and expected from) certain scopes, so be careful where you are using it. (As of this
 * writing, it's only for local variables, including loop variables). The user should never meet a
 * {@link TemplateNullModel}, it must not be returned from public API-s.
 *
 * @see Environment#getNullableLocalVariable(String)
 *
 * @since 2.3.29
 */
final class TemplateNullModel implements TemplateModel {
    static final TemplateNullModel INSTANCE = new TemplateNullModel();
    private TemplateNullModel() { }
}

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

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util._NullArgumentException;

abstract class OutputFormatBoundBuiltIn extends SpecialBuiltIn {
    
    protected OutputFormat outputFormat;
    protected AutoEscapingPolicy autoEscapingPolicy;
    
    /**
     * Binds the {@link ASTNode} to the output format and auto-escaping context of its lexical context.
     * Called when the {@link ASTNode} location was already set (so {@link #getTemplate()} will return
     * non-{@code null}).
     */
    void bindToOutputFormat(OutputFormat outputFormat, AutoEscapingPolicy autoEscapingPolicy) {
        _NullArgumentException.check(outputFormat);
        this.outputFormat = outputFormat;
        this.autoEscapingPolicy = autoEscapingPolicy;
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        if (outputFormat == null) {
            // The parser should prevent this situation
            throw new NullPointerException("outputFormat was null");
        }
        return calculateResult(env);
    }

    protected abstract TemplateModel calculateResult(Environment env)
            throws TemplateException;
    
}
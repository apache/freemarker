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

import org.apache.freemarker.core.ASTDirList.IterationContext;
import org.apache.freemarker.core.model.TemplateModel;

abstract class BuiltInForNestedContentParameter extends SpecialBuiltIn {
    
    private String nestedContentParamName;
    
    void bindToNestedContentParameter(String nestedContentParamName) {
        this.nestedContentParamName = nestedContentParamName;
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        IterationContext iterCtx = ASTDirList.findEnclosingIterationContext(env, nestedContentParamName);
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new TemplateException(
                    this, env,
                    "There's no iteration in context that uses the nested content parameter ",
                    new _DelayedJQuote( nestedContentParamName), ".");
        }
        
        return calculateResult(iterCtx, env);
    }

    abstract TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException;
    
}
/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import freemarker.core.IteratorBlock.IterationContext;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

abstract class BuiltInForLoopVariable extends SpecialBuiltIn {
    
    private String loopVarName;
    
    void bindToLoopVariable(String loopVarName) {
        this.loopVarName = loopVarName;
    }
    
    TemplateModel _eval(Environment env) throws TemplateException {
        IterationContext iterCtx = IteratorBlock.findEnclosingIterationContext(env, loopVarName);
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new _MiscTemplateException(
                    this, env,
                    new Object[] {
                            "There's no iteration in context that uses loop variable ",
                            new _DelayedJQuote(loopVarName), "."
                    });
        }
        
        return calculateResult(iterCtx, env);
    }

    abstract TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException;
    
}
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
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;


class BuiltInsForLoopVariables {
    
    static class indexBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex());
        }
        
    }
    
    static class counterBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex() + 1);
        }
        
    }

    static class has_nextBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.hasNext() ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        
    }

    static class is_lastBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return !iterCtx.hasNext() ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        
    }

    static class is_firstBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return (iterCtx.getIndex() == 0) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        
    }
    
}

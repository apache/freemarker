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

import java.util.List;

import freemarker.core.IteratorBlock.IterationContext;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;


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

    static abstract class BooleanBuiltInForLoopVariable extends BuiltInForLoopVariable {

        final TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return calculateBooleanResult(iterCtx, env) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }

        protected abstract boolean calculateBooleanResult(IterationContext iterCtx, Environment env);
        
    }
    
    static class has_nextBI extends BooleanBuiltInForLoopVariable {

        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.hasNext();
        }

    }

    static class is_lastBI extends BooleanBuiltInForLoopVariable {

        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return !iterCtx.hasNext();
        }
        
    }

    static class is_firstBI extends BooleanBuiltInForLoopVariable {

        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() == 0;
        }
        
    }

    static class is_odd_itemBI extends BooleanBuiltInForLoopVariable {

        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() % 2 == 0;
        }
        
    }

    static class is_even_itemBI extends BooleanBuiltInForLoopVariable {

        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() % 2 != 0;
        }
        
    }
    
    static class item_parityBI extends BuiltInForLoopVariable {
        
        private static final SimpleScalar ODD = new SimpleScalar("odd");
        private static final SimpleScalar EVEN = new SimpleScalar("even");

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_parity_capBI extends BuiltInForLoopVariable {
        
        private static final SimpleScalar ODD = new SimpleScalar("Odd");
        private static final SimpleScalar EVEN = new SimpleScalar("Even");

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_cycleBI extends BuiltInForLoopVariable {

        private class BIMethod implements TemplateMethodModelEx {
            
            private final IterationContext iterCtx;
    
            private BIMethod(IterationContext iterCtx) {
                this.iterCtx = iterCtx;
            }
    
            public Object exec(List args) throws TemplateModelException {
                checkMethodArgCount(args, 1, Integer.MAX_VALUE);
                return args.get(iterCtx.getIndex() % args.size());
            }
        }
        
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new BIMethod(iterCtx);
        }
        
    }
    
}

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

import java.util.List;

import org.apache.freemarker.core.ASTDirList.IterationContext;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleScalar;


class BuiltInsForNestedContentParameters {
    
    static class indexBI extends BuiltInForNestedContentParameter {

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex());
        }
        
    }
    
    static class counterBI extends BuiltInForNestedContentParameter {

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex() + 1);
        }
        
    }

    static abstract class BooleanBuiltInForNestedContentParameter extends BuiltInForNestedContentParameter {

        @Override
        final TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return calculateBooleanResult(iterCtx, env) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }

        protected abstract boolean calculateBooleanResult(IterationContext iterCtx, Environment env);
        
    }
    
    static class has_nextBI extends BooleanBuiltInForNestedContentParameter {

        @Override
        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.hasNext();
        }

    }

    static class is_lastBI extends BooleanBuiltInForNestedContentParameter {

        @Override
        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return !iterCtx.hasNext();
        }
        
    }

    static class is_firstBI extends BooleanBuiltInForNestedContentParameter {

        @Override
        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() == 0;
        }
        
    }

    static class is_odd_itemBI extends BooleanBuiltInForNestedContentParameter {

        @Override
        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() % 2 == 0;
        }
        
    }

    static class is_even_itemBI extends BooleanBuiltInForNestedContentParameter {

        @Override
        protected boolean calculateBooleanResult(IterationContext iterCtx, Environment env) {
            return iterCtx.getIndex() % 2 != 0;
        }
        
    }
    
    static class item_parityBI extends BuiltInForNestedContentParameter {
        
        private static final SimpleScalar ODD = new SimpleScalar("odd");
        private static final SimpleScalar EVEN = new SimpleScalar("even");

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_parity_capBI extends BuiltInForNestedContentParameter {
        
        private static final SimpleScalar ODD = new SimpleScalar("Odd");
        private static final SimpleScalar EVEN = new SimpleScalar("Even");

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_cycleBI extends BuiltInForNestedContentParameter {

        private class BIMethod implements TemplateMethodModel {
            
            private final IterationContext iterCtx;
    
            private BIMethod(IterationContext iterCtx) {
                this.iterCtx = iterCtx;
            }
    
            @Override
            public TemplateModel execute(List<? extends TemplateModel> args) throws TemplateModelException {
                checkMethodArgCount(args, 1, Integer.MAX_VALUE);
                return args.get(iterCtx.getIndex() % args.size());
            }
        }
        
        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new BIMethod(iterCtx);
        }
        
    }
    
}

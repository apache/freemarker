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
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.util.CallableUtils;


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
        
        private static final SimpleString ODD = new SimpleString("odd");
        private static final SimpleString EVEN = new SimpleString("even");

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_parity_capBI extends BuiltInForNestedContentParameter {
        
        private static final SimpleString ODD = new SimpleString("Odd");
        private static final SimpleString EVEN = new SimpleString("Even");

        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.getIndex() % 2 == 0 ? ODD: EVEN;
        }
        
    }

    static class item_cycleBI extends BuiltInForNestedContentParameter {

        private class BIMethod extends BuiltInCallableImpl implements TemplateFunctionModel {
            
            private final IterationContext iterCtx;
    
            private BIMethod(IterationContext iterCtx) {
                this.iterCtx = iterCtx;
            }

            @Override
            public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env)
                    throws TemplateException {
                CallableUtils.checkArgumentCount(args.length, 1, Integer.MAX_VALUE, this);
                return args[iterCtx.getIndex() % args.length];
            }

            @Override
            public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
                return null;
            }
        }
        
        @Override
        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new BIMethod(iterCtx);
        }
        
    }
    
}

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

import java.util.ArrayList;
import java.util.List;

import org.apache.freemarker.core.model.TemplateModel;


final class BuiltInsWithLazyConditionals {
    
    /**
     * Behaves similarly to the ternary operator of Java.
     */
    static class then_BI extends BuiltInWithParseTimeParameters {
        
        private ASTExpression whenTrueExp;
        private ASTExpression whenFalseExp;

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            boolean lho = target.evalToBoolean(env);
            return (lho ? whenTrueExp : whenFalseExp).evalToNonMissing(env);
        }

        @Override
        void bindToParameters(List<ASTExpression> parameters, Token openParen, Token closeParen) throws ParseException {
            if (parameters.size() != 2) {
                throw newArgumentCountException("requires exactly 2", openParen, closeParen);
            }
            whenTrueExp = parameters.get(0);
            whenFalseExp = parameters.get(1);
        }
        
        @Override
        protected ASTExpression getArgumentParameterValue(final int argIdx) {
            switch (argIdx) {
            case 0: return whenTrueExp;
            case 1: return whenFalseExp;
            default: throw new IndexOutOfBoundsException();
            }
        }

        @Override
        protected int getArgumentsCount() {
            return 2;
        }
        
        @Override
        protected List<ASTExpression> getArgumentsAsList() {
            ArrayList<ASTExpression> args = new ArrayList<>(2);
            args.add(whenTrueExp);
            args.add(whenFalseExp);
            return args;
        }
        
        @Override
        protected void cloneArguments(ASTExpression cloneExp, String replacedIdentifier,
                ASTExpression replacement, ReplacemenetState replacementState) {
            then_BI clone = (then_BI) cloneExp;
            clone.whenTrueExp = whenTrueExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
            clone.whenFalseExp = whenFalseExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
        }
        
    }
    
    private BuiltInsWithLazyConditionals() {
        // Not to be instantiated
    }

    static class switch_BI extends BuiltInWithParseTimeParameters {
        
        private List<ASTExpression> parameters;

        @Override
        void bindToParameters(List<ASTExpression> parameters, Token openParen, Token closeParen) throws ParseException {
            if (parameters.size() < 2) {
                throw newArgumentCountException("must have at least 2", openParen, closeParen);
            }
            this.parameters = parameters;
        }

        @Override
        protected List<ASTExpression> getArgumentsAsList() {
            return parameters;
        }

        @Override
        protected int getArgumentsCount() {
            return parameters.size();
        }

        @Override
        protected ASTExpression getArgumentParameterValue(int argIdx) {
            return parameters.get(argIdx);
        }

        @Override
        protected void cloneArguments(ASTExpression clone, String replacedIdentifier, ASTExpression replacement,
                ReplacemenetState replacementState) {
            ArrayList<ASTExpression> parametersClone = new ArrayList<>(parameters.size());
            for (ASTExpression parameter : parameters) {
                parametersClone.add(parameter
                        .deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
            }
            ((switch_BI) clone).parameters = parametersClone;
        }

        @Override
        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel targetValue = target.evalToNonMissing(env);
            
            List<ASTExpression> parameters = this.parameters;
            int paramCnt = parameters.size();
            for (int i = 0; i + 1 < paramCnt; i += 2) {
                ASTExpression caseExp = parameters.get(i);
                TemplateModel caseValue = caseExp.evalToNonMissing(env);
                if (_EvalUtils.compare(
                        targetValue, target,
                        _EvalUtils.CMP_OP_EQUALS, "==",
                        caseValue, caseExp,
                        this, true,
                        false, false, false,
                        env)) {
                    return parameters.get(i + 1).evalToNonMissing(env);
                }
            }
            
            if (paramCnt % 2 == 0) {
                throw new TemplateException(target,
                        "The value before ?", key, "(case1, value1, case2, value2, ...) didn't match any of the "
                        + "case parameters, and there was no default value parameter (an additional last parameter) "
                        + "eithter. ");
            }
            return parameters.get(paramCnt - 1).evalToNonMissing(env);
        }
        
    }
    
}

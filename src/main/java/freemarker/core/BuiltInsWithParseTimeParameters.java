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

import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;


final class BuiltInsWithParseTimeParameters {
    
    /**
     * Behaves similarly to the ternary operator of Java.
     */
    static class then_BI extends BuiltInWithParseTimeParameters {
        
        private Expression whenTrueExp;
        private Expression whenFalseExp;

        TemplateModel _eval(Environment env) throws TemplateException {
            boolean lho = target.evalToBoolean(env);
            return (lho ? whenTrueExp : whenFalseExp).evalToNonMissing(env);
        }

        void bindToParameters(List parameters, Token openParen, Token closeParen) throws ParseException {
            if (parameters.size() != 2) {
                throw newArgumentCountException("requires exactly 2", openParen, closeParen);
            }
            whenTrueExp = (Expression) parameters.get(0);
            whenFalseExp = (Expression) parameters.get(1);
        }
        
        protected Expression getArgumentParameterValue(final int argIdx) {
            switch (argIdx) {
            case 0: return whenTrueExp;
            case 1: return whenFalseExp;
            default: throw new IndexOutOfBoundsException();
            }
        }

        protected int getArgumentsCount() {
            return 2;
        }
        
        protected List getArgumentsAsList() {
            ArrayList args = new ArrayList(2);
            args.add(whenTrueExp);
            args.add(whenFalseExp);
            return args;
        }
        
        protected void cloneArguments(Expression cloneExp, String replacedIdentifier,
                Expression replacement, ReplacemenetState replacementState) {
            then_BI clone = (then_BI) cloneExp;
            clone.whenTrueExp = whenTrueExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
            clone.whenFalseExp = whenFalseExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
        }
        
    }
    
    private BuiltInsWithParseTimeParameters() {
        // Not to be instantiated
    }

    static class switch_BI extends BuiltInWithParseTimeParameters {
        
        private List/*<Expression>*/ parameters;

        void bindToParameters(List parameters, Token openParen, Token closeParen) throws ParseException {
            if (parameters.size() < 2) {
                throw newArgumentCountException("must have at least 2", openParen, closeParen);
            }
            this.parameters = parameters;
        }

        protected List getArgumentsAsList() {
            return parameters;
        }

        protected int getArgumentsCount() {
            return parameters.size();
        }

        protected Expression getArgumentParameterValue(int argIdx) {
            return (Expression) parameters.get(argIdx);
        }

        protected void cloneArguments(Expression clone, String replacedIdentifier, Expression replacement,
                ReplacemenetState replacementState) {
            ArrayList parametersClone = new ArrayList(parameters.size());
            for (int i = 0; i < parameters.size(); i++) {
                parametersClone.add(((Expression) parameters.get(i))
                        .deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
            }
            ((switch_BI) clone).parameters = parametersClone;
        }

        TemplateModel _eval(Environment env) throws TemplateException {
            TemplateModel targetValue = target.evalToNonMissing(env);
            
            List parameters = this.parameters;
            int paramCnt = parameters.size();
            for (int i = 0; i + 1 < paramCnt; i += 2) {
                Expression caseExp = (Expression) parameters.get(i);
                TemplateModel caseValue = caseExp.evalToNonMissing(env);
                if (EvalUtil.compare(
                        targetValue, target,
                        EvalUtil.CMP_OP_EQUALS, "==",
                        caseValue, caseExp,
                        this, true,
                        false, false, false,
                        env)) {
                    return ((Expression) parameters.get(i + 1)).evalToNonMissing(env);
                }
            }
            
            if (paramCnt % 2 == 0) {
                throw new _MiscTemplateException(target, new Object[] { "The value before ?", key,
                        "(case1, value1, case2, value2, ...) didn't match any of the case parameters, and there "
                        + "was no default value parameter (an additional last parameter) eihter. "});
            }
            return ((Expression) parameters.get(paramCnt - 1)).evalToNonMissing(env);
        }
        
    }
    
}

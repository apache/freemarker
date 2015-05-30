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
    static class chooseBI extends BuiltInWithParseTimeParameters {
        
        private Expression whenTrueExp;
        private Expression whenFalseExp;

        TemplateModel _eval(Environment env) throws TemplateException {
            boolean lho = target.evalToBoolean(env);
            final Expression expToEval = lho ? whenTrueExp : whenFalseExp;
            final TemplateModel result = expToEval.eval(env);
            expToEval.assertNonNull(result, env);
            return result;
        }

        void bindToParameters(List parameters) throws ParseException {
            if (parameters.size() != 2) {
                throw new ParseException("The ?" + key + " built-in must have exactly 2 parameters.", this);
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
            chooseBI clone = (chooseBI) cloneExp;
            clone.whenTrueExp = whenTrueExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
            clone.whenFalseExp = whenFalseExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState);
        }
        
    }
    
    private BuiltInsWithParseTimeParameters() {
        // Not to be instantiated
    }
    
}

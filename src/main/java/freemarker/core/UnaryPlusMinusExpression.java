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

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;

final class UnaryPlusMinusExpression extends Expression {
    
    private final int TYPE_MINUS = 0;
    private final int TYPE_PLUS = 1;

    private final Expression target;
    private final boolean isMinus;
    private static final Integer MINUS_ONE = new Integer(-1); 

    UnaryPlusMinusExpression(Expression target, boolean isMinus) {
        this.target = target;
        this.isMinus = isMinus;
    }
    
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateNumberModel targetModel = null;
        TemplateModel tm = target.eval(env);
        try {
            targetModel = (TemplateNumberModel) tm;
        } catch (ClassCastException cce) {
            throw new NonNumericalException(target, tm, env);
        }
        if (!isMinus) {
            return targetModel;
        }
        target.assertNonNull(targetModel, env);
        Number n = targetModel.getAsNumber();
        n = ArithmeticEngine.CONSERVATIVE_ENGINE.multiply(MINUS_ONE, n);
        return new SimpleNumber(n);
    }
    
    public String getCanonicalForm() {
        String op = isMinus ? "-" : "+";
        return op + target.getCanonicalForm();
    }

    String getNodeTypeSymbol() {
        return isMinus ? "-..." : "+...";
    }
    
    boolean isLiteral() {
        return target.isLiteral();
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new UnaryPlusMinusExpression(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        isMinus);
    }
    
    
    boolean isIgnorable() {
        return true;
    }

    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return target;
        case 1: return new Integer(isMinus ? TYPE_MINUS : TYPE_PLUS);
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.RIGHT_HAND_OPERAND;
        case 1: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
}

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

/**
 * An operator for arithmetic operations. Note that the + operator is in {@link AddConcatExpression}, because its
 * overloaded (does string concatenation and more).
 */
final class ArithmeticExpression extends Expression {

    static final int TYPE_SUBSTRACTION = 0;
    static final int TYPE_MULTIPLICATION = 1;
    static final int TYPE_DIVISION = 2;
    static final int TYPE_MODULO = 3;

    private static final char[] OPERATOR_IMAGES = new char[] { '-', '*', '/', '%' };

    private final Expression lho;
    private final Expression rho;
    private final int operator;

    ArithmeticExpression(Expression lho, Expression rho, int operator) {
        this.lho = lho;
        this.rho = rho;
        this.operator = operator;
    }

    TemplateModel _eval(Environment env) throws TemplateException {
        return _eval(env, this, lho.evalToNumber(env), operator, rho.evalToNumber(env));
    }

    static TemplateModel _eval(Environment env, TemplateObject parent, Number lhoNumber, int operator, Number rhoNumber)
            throws TemplateException, _MiscTemplateException {
        ArithmeticEngine ae = 
            env != null 
                ? env.getArithmeticEngine()
                : parent.getTemplate().getArithmeticEngine();
        switch (operator) {
            case TYPE_SUBSTRACTION : 
                return new SimpleNumber(ae.subtract(lhoNumber, rhoNumber));
            case TYPE_MULTIPLICATION :
                return new SimpleNumber(ae.multiply(lhoNumber, rhoNumber));
            case TYPE_DIVISION :
                return new SimpleNumber(ae.divide(lhoNumber, rhoNumber));
            case TYPE_MODULO :
                return new SimpleNumber(ae.modulus(lhoNumber, rhoNumber));
            default:
                if (parent instanceof Expression) {
                    throw new _MiscTemplateException((Expression) parent, new Object[] {
                            "Unknown operation: ", new Integer(operator) });
                } else {
                    throw new _MiscTemplateException(new Object[] {
                            "Unknown operation: ", new Integer(operator) });
                }
        }
    }

    public String getCanonicalForm() {
        return lho.getCanonicalForm() + ' ' + getOperatorSymbol(operator) + ' ' + rho.getCanonicalForm();
    }
    
    String getNodeTypeSymbol() {
        return String.valueOf(getOperatorSymbol(operator));
    }

    static char getOperatorSymbol(int operator) {
        return OPERATOR_IMAGES[operator];
    }
    
    boolean isLiteral() {
        return constantValue != null || (lho.isLiteral() && rho.isLiteral());
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new ArithmeticExpression(
    	        lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        operator);
    }
    
    int getParameterCount() {
        return 3;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        case 2: return new Integer(operator);
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.LEFT_HAND_OPERAND;
        case 1: return ParameterRole.RIGHT_HAND_OPERAND;
        case 2: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
}

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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template._TemplateAPI;

/**
 * A class that represents a Range between two integers.
 */
final class Range extends Expression {

    static final int END_INCLUSIVE = 0; 
    static final int END_EXCLUSIVE = 1; 
    static final int END_UNBOUND = 2; 
    static final int END_SIZE_LIMITED = 3; 
    
    final Expression lho;
    final Expression rho;
    final int endType;

    Range(Expression lho, Expression rho, int endType) {
        this.lho = lho;
        this.rho = rho;
        this.endType = endType;
    }
    
    int getEndType() {
        return endType;
    }

    TemplateModel _eval(Environment env) throws TemplateException {
        final int begin = lho.evalToNumber(env).intValue();
        if (endType != END_UNBOUND) {
            final int lhoValue = rho.evalToNumber(env).intValue();
            return new BoundedRangeModel(
                    begin, endType != END_SIZE_LIMITED ? lhoValue : begin + lhoValue,
                    endType == END_INCLUSIVE, endType == END_SIZE_LIMITED); 
        } else {
            return _TemplateAPI.getTemplateLanguageVersionAsInt(this) >= _TemplateAPI.VERSION_INT_2_3_21
                    ? (RangeModel) new ListableRightUnboundedRangeModel(begin)
                    : (RangeModel) new NonListableRightUnboundedRangeModel(begin);
        }
    }
    
    // Surely this way we can tell that it won't be a boolean without evaluating the range, but why was this important?
    boolean evalToBoolean(Environment env) throws TemplateException {
        throw new NonBooleanException(this, new BoundedRangeModel(0, 0, false, false), env);
    }

    public String getCanonicalForm() {
        String rhs = rho != null ? rho.getCanonicalForm() : "";
        return lho.getCanonicalForm() + getNodeTypeSymbol() + rhs;
    }
    
    String getNodeTypeSymbol() {
        switch (endType) {
        case END_EXCLUSIVE: return "..<";
        case END_INCLUSIVE: return "..";
        case END_UNBOUND: return "..";
        case END_SIZE_LIMITED: return "..*";
        default: throw new BugException(endType);
        }
    }
    
    boolean isLiteral() {
        boolean rightIsLiteral = rho == null || rho.isLiteral();
        return constantValue != null || (lho.isLiteral() && rightIsLiteral);
    }
    
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new Range(
                lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                endType);
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
}

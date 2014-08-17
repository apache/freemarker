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

/**
 * A class that represents a Range between two integers.
 */
final class Range extends Expression {

    final Expression lho;
    final Expression rho;
    final boolean exclusiveEnd;

    Range(Expression lho, Expression rho, boolean exclusiveEnd) {
        this.lho = lho;
        this.rho = rho;
        this.exclusiveEnd = exclusiveEnd;
    }
    
    boolean hasRho() {
        return rho != null;
    }

    TemplateModel _eval(Environment env) throws TemplateException {
        int begin = lho.evalToNumber(env).intValue();
        return rho != null
            ? (RangeModel)new BoundedRangeModel(begin, rho.evalToNumber(env).intValue(), exclusiveEnd)
            : (getTemplate().getConfiguration().getIncompatibleImprovements().intValue() >= 2003021
                    ? (RangeModel) new ListableRightUnboundedRangeModel(begin)
                    : (RangeModel) new NonListableRightUnboundedRangeModel(begin));
    }
    
    // Surely this way we can tell that it won't be a boolean without evaluating the range, but why was this important?
    boolean evalToBoolean(Environment env) throws TemplateException {
        throw new NonBooleanException(this, new BoundedRangeModel(0, 0, exclusiveEnd), env);
    }

    public String getCanonicalForm() {
        String rhs = rho != null ? rho.getCanonicalForm() : "";
        return lho.getCanonicalForm() + getNodeTypeSymbol() + rhs;
    }
    
    String getNodeTypeSymbol() {
        return exclusiveEnd ? "..<" : "..";
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
                exclusiveEnd);
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

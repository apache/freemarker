/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import freemarker.template.TemplateException;

/**
 * A class that handles comparisons.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
 */

final class ComparisonExpression extends BooleanExpression {

    private final Expression left;
    private final Expression right;
    private final int operation;
    private final String opString;

    ComparisonExpression(Expression left, Expression right, String opString) {
        this.left = left;
        this.right = right;
        opString = opString.intern();
        this.opString = opString;
        if (opString == "==" || opString == "=") {
            operation = EvalUtil.CMP_OP_EQUALS;
        }
        else if (opString == "!=") {
            operation = EvalUtil.CMP_OP_NOT_EQUALS;
        }
        else if (opString == "gt" || opString == "\\gt" || opString == ">" || opString == "&gt;") {
            operation = EvalUtil.CMP_OP_GREATER_THAN;
        }
        else if (opString == "gte" || opString == "\\gte" || opString == ">=" || opString == "&gt;=") {
            operation = EvalUtil.CMP_OP_GREATER_THAN_EQUALS;
        }
        else if (opString== "lt" || opString == "\\lt" || opString == "<" || opString == "&lt;") {
            operation = EvalUtil.CMP_OP_LESS_THAN;
        }
        else if (opString == "lte" || opString == "\\lte" || opString == "<=" || opString == "&lt;=") {
            operation = EvalUtil.CMP_OP_LESS_THAN_EQUALS;
        }
        else {
            throw new RuntimeException("Unknown comparison operator " + opString);
        }
    }

    /*
     * WARNING! This algorithm is duplicated in SequenceBuiltins.modelsEqual.
     * Thus, if you update this method, then you have to update that too!
     */
    boolean evalToBoolean(Environment env) throws TemplateException {
        return EvalUtil.compare(left, operation, opString, right, this, env);
    }

    public String getCanonicalForm() {
        return left.getCanonicalForm() + ' ' + opString + ' ' + right.getCanonicalForm();
    }
    
    String getNodeTypeSymbol() {
        return opString;
    }

    boolean isLiteral() {
        return constantValue != null || (left.isLiteral() && right.isLiteral());
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new ComparisonExpression(
    	        left.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        opString);
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        return idx == 0 ? left : right;
    }

    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
}

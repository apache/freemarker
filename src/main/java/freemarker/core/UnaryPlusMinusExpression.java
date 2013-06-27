/*
 * Copyright (c) 2005 The Visigoth Software Society. All rights
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

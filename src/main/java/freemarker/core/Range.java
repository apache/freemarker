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

import freemarker.template.*;

/**
 * A class that represents a Range between two integers.
 */
final class Range extends Expression {

    final Expression left;
    final Expression right;

    Range(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }
    
    boolean hasRhs() {
        return right != null;
    }

    TemplateModel _getAsTemplateModel(Environment env) 
        throws TemplateException
    {
        int min = EvaluationUtil.getNumber(left, env).intValue();
        int max = 0;
        if (right != null) {
            max = EvaluationUtil.getNumber(right, env).intValue();
            return new NumericalRange(min, max);
        }
        return new NumericalRange(min);
    }
    
    boolean isTrue(Environment env) throws TemplateException {
        String msg = "Error " + getStartLocation() + ". " 
                    + "\nExpecting a boolean here."
                    + " Expression " + this + " is a range.";
        throw new NonBooleanException(msg, env);
    }

    public String getCanonicalForm() {
        String rhs = right != null ? right.getCanonicalForm() : "";
        return left.getCanonicalForm() + ".." + rhs;
    }
    
    boolean isLiteral() {
        boolean rightIsLiteral = right == null || right.isLiteral();
        return constantValue != null || (left.isLiteral() && rightIsLiteral);
    }
    
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new Range(
                left.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
}

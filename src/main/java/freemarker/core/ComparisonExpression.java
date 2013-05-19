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

import java.util.Date;
import freemarker.template.*;

/**
 * A class that handles comparisons.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 * @author Attila Szegedi
 */

final class ComparisonExpression extends BooleanExpression {

    static final int EQUALS=1;
    static final int NOT_EQUALS=2;
    static final int LESS_THAN=3;
    static final int GREATER_THAN=4;
    static final int LESS_THAN_EQUALS=5;
    static final int GREATER_THAN_EQUALS=6;

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
            operation = EQUALS;
        }
        else if (opString == "!=") {
            operation = NOT_EQUALS;
        }
        else if (opString == "gt" || opString == "\\gt" || opString == ">" || opString == "&gt;") {
            operation = GREATER_THAN;
        }
        else if (opString == "gte" || opString == "\\gte" || opString == ">=" || opString == "&gt;=") {
            operation = GREATER_THAN_EQUALS;
        }
        else if (opString== "lt" || opString == "\\lt" || opString == "<" || opString == "&lt;") {
            operation = LESS_THAN;
        }
        else if (opString == "lte" || opString == "\\lte" || opString == "<=" || opString == "&lt;=") {
            operation = LESS_THAN_EQUALS;
        }
        else {
            throw new RuntimeException("Unknown comparison operator " + opString);
        }
    }

    /*
     * WARNING! This algorithm is duplicated in SequenceBuiltins.modelsEqual.
     * Thus, if you update this method, then you have to update that too!
     */
    boolean isTrue(Environment env) throws TemplateException {
        TemplateModel ltm = left.getAsTemplateModel(env);
        TemplateModel rtm = right.getAsTemplateModel(env);
        if (env != null && env.isClassicCompatible()) {
            if (ltm == null) {
                ltm = TemplateScalarModel.EMPTY_STRING;
            }
            if (rtm == null) {
                rtm = TemplateScalarModel.EMPTY_STRING;
            }
        }
        assertNonNull(ltm, left, env);
        assertNonNull(rtm, right, env);
        int comp = 0;
        if(ltm instanceof TemplateNumberModel && rtm instanceof TemplateNumberModel) { 
            Number first = EvaluationUtil.getNumber((TemplateNumberModel)ltm, left, env);
            Number second = EvaluationUtil.getNumber((TemplateNumberModel)rtm, right, env);
            ArithmeticEngine ae = 
                env != null 
                    ? env.getArithmeticEngine()
                    : getTemplate().getArithmeticEngine();
            comp = ae.compareNumbers(first, second);
        }
        else if(ltm instanceof TemplateDateModel && rtm instanceof TemplateDateModel) {
            TemplateDateModel ltdm = (TemplateDateModel)ltm;
            TemplateDateModel rtdm = (TemplateDateModel)rtm;
            int ltype = ltdm.getDateType();
            int rtype = rtdm.getDateType();
            if(ltype != rtype) {
                throw new TemplateException(
                    "Can not compare dates of different type. Left date is of "
                    + TemplateDateModel.TYPE_NAMES.get(ltype)
                    + " type, right date is of " 
                    + TemplateDateModel.TYPE_NAMES.get(rtype) + " type.", 
                    env);
            }
            if(ltype == TemplateDateModel.UNKNOWN) {
                throw new TemplateException(
                    "Left date is of UNKNOWN type, and can not be compared.", env);
            }
            if(rtype == TemplateDateModel.UNKNOWN) {
                throw new TemplateException(
                    "Right date is of UNKNOWN type, and can not be compared.", env);
            }
            
            Date first = EvaluationUtil.getDate(ltdm, left, env);
            Date second = EvaluationUtil.getDate(rtdm, right, env);
            comp = first.compareTo(second);
        }
        else if(ltm instanceof TemplateScalarModel && rtm instanceof TemplateScalarModel) {
            if(operation != EQUALS && operation != NOT_EQUALS) {
                throw new TemplateException("Can not use operator " + opString + " on string values.", env);
            }
            String first = EvaluationUtil.getString((TemplateScalarModel)ltm, left, env);
            String second = EvaluationUtil.getString((TemplateScalarModel)rtm, right, env);
            comp = env.getCollator().compare(first, second);
        }
        else if(ltm instanceof TemplateBooleanModel && rtm instanceof TemplateBooleanModel) {
            if(operation != EQUALS && operation != NOT_EQUALS) {
                throw new TemplateException("Can not use operator " + opString + " on boolean values.", env);
            }
            boolean first = ((TemplateBooleanModel)ltm).getAsBoolean();
            boolean second = ((TemplateBooleanModel)rtm).getAsBoolean();
            comp = (first ? 1 : 0) - (second ? 1 : 0);
        }
        // Here we handle compatibility issues
        else if(env.isClassicCompatible()) {
            String first = left.getStringValue(env);
            String second = right.getStringValue(env);
            comp = env.getCollator().compare(first, second);
        }
        else {
            throw new TemplateException(
                "The only legal comparisons are between two numbers, two strings, or two dates.\n"
                + "Left  hand operand is a " + ltm.getClass().getName() + "\n"
                + "Right hand operand is a " + rtm.getClass().getName() + "\n"
                , env);
        }
        switch (operation) {
            case EQUALS:
                return comp == 0;
            case NOT_EQUALS:
                return comp != 0;
            case LESS_THAN : 
                return comp < 0;
            case GREATER_THAN : 
                return comp > 0;
            case LESS_THAN_EQUALS :
                return comp <= 0;
            case GREATER_THAN_EQUALS :
                return comp >= 0;
            default :
                throw new TemplateException("unknown operation", env);
        }
    }

    public String getCanonicalForm() {
        return left.getCanonicalForm() + ' ' + opString + ' ' + right.getCanonicalForm();
    }

    boolean isLiteral() {
        return constantValue != null || (left.isLiteral() && right.isLiteral());
    }

    Expression _deepClone(String name, Expression subst) {
    	return new ComparisonExpression(left.deepClone(name, subst), right.deepClone(name, subst), opString);
    }
}

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

import java.util.ArrayList;

import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * A unary operator that uses the string value of an expression as a hash key.
 * It associates with the <tt>Identifier</tt> or <tt>Dot</tt> to its left.
 */
final class DynamicKeyName extends Expression {

    private final Expression nameExpression;
    private final Expression target;

    DynamicKeyName(Expression target, Expression nameExpression) {
        this.target = target; 
        this.nameExpression = nameExpression;
    }

    TemplateModel _eval(Environment env) throws TemplateException
    {
        TemplateModel targetModel = target.eval(env);
        if (targetModel == null) {
            if (env.isClassicCompatible()) {
                return null;
            } else {
                throw InvalidReferenceException.getInstance(target, env);
            }
        }
        if (nameExpression instanceof Range) {
            return dealWithRangeKey(targetModel, (Range) nameExpression, env);
        }
        TemplateModel keyModel = nameExpression.eval(env);
        if(keyModel == null) {
            if(env.isClassicCompatible()) {
                keyModel = TemplateScalarModel.EMPTY_STRING;
            }
            else {
                nameExpression.assertNonNull(keyModel, env);
            }
        }
        if (keyModel instanceof TemplateNumberModel) {
            int index = nameExpression.modelToNumber(keyModel, env).intValue();
            return dealWithNumericalKey(targetModel, index, env);
        }
        if (keyModel instanceof TemplateScalarModel) {
            String key = EvalUtil.modelToString((TemplateScalarModel)keyModel, nameExpression, env);
            return dealWithStringKey(targetModel, key, env);
        }
        throw new UnexpectedTypeException(nameExpression, keyModel, "number, range, or string", env);
    }


    private TemplateModel dealWithNumericalKey(TemplateModel targetModel, 
                                               int index, 
                                               Environment env)
        throws TemplateException
    {
        if (targetModel instanceof TemplateSequenceModel) {
            TemplateSequenceModel tsm = (TemplateSequenceModel) targetModel;
            int size;
            try {
                size = tsm.size();
            } catch (Exception e) {
                size = Integer.MAX_VALUE;
            }
            return index < size ? tsm.get(index) : null;
        } 
        
        try
        {
            String s = target.evalAndCoerceToString(env);
            try {
               return new SimpleScalar(s.substring(index, index + 1));
            } catch (RuntimeException re) {
                throw new _MiscTemplateException(re, env);
            }
        }
        catch(NonStringException e)
        {
            throw new UnexpectedTypeException(
                    target, targetModel, "sequence or string (or something that's implicitly convertible to string)",
                    env);
        }
    }

    private TemplateModel dealWithStringKey(TemplateModel targetModel, String key, Environment env)
        throws TemplateException
    {
        if(targetModel instanceof TemplateHashModel) {
            return((TemplateHashModel) targetModel).get(key);
        }
        throw new UnexpectedTypeException(target, targetModel, "hash", env);
    }

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, 
                                           Range range, 
                                           Environment env)
        throws TemplateException
    {
        int start = range.lho.evalToNumber(env).intValue();
        int end = 0;
        boolean hasRhs = range.hasRho();
        if (hasRhs) {
            end = range.rho.evalToNumber(env).intValue();
        }
        if (targetModel instanceof TemplateSequenceModel) {
            TemplateSequenceModel sequence = (TemplateSequenceModel) targetModel;
            if (!hasRhs) end = sequence.size() -1;
            if (start < 0) {
                throw new _MiscTemplateException(range.lho, new Object[] {
                        "Negative starting index ", new Integer(start), " for slicing range." });
            }
            if (end < 0) {
                throw new _MiscTemplateException(range.rho, new Object[] {
                        "Negative ending index ", new Integer(end), " for slicing range." });
            }
            if (start >= sequence.size()) {
                throw new _MiscTemplateException(range.lho, new Object[] {
                        "Left side index of range out of bounds, is ", new Integer(start),
                        ", but the sequence has only ", new Integer(sequence.size()), " element(s). ",
                        "(Note that indices are 0 based, and ranges are inclusive)." });
            }
            if (end >= sequence.size()) {
                throw new _MiscTemplateException(range.rho, new Object[] {
                        "Right side index of range out of bounds, is ", new Integer(end),
                        ", but the sequence has only ", new Integer(sequence.size()), " element(s). ",
                        "(Note that indices are 0 based, and ranges are inclusive)." });
            }
            ArrayList list = new ArrayList(1+Math.abs(start-end));
            if (start>end) {
                for (int i = start; i>=end; i--) {
                    list.add(sequence.get(i));
                }
            }
            else {
                for (int i = start; i<=end; i++) {
                    list.add(sequence.get(i));
                }
            }
            return new SimpleSequence(list);
        }
        
        final String targetStr;
        try {
            targetStr = target.evalAndCoerceToString(env);
        } catch(NonStringException e) {
            throw new UnexpectedTypeException(target, target.eval(env),
                    NonStringException.TYPES_USABLE_WHERE_STRING_IS_EXPECTED + " or sequence", env);
        }
        
        if (!hasRhs) end = targetStr.length() -1;
        if (start < 0) {
            throw new _MiscTemplateException(range.lho, new Object[] {
                    "Negative starting index ", new Integer(start), " for slicing range." });
        }
        if (end < 0) {
            throw new _MiscTemplateException(range.rho, new Object[] {
                    "Negative ending index ", new Integer(end), " for slicing range." });
        }
        if (start > targetStr.length()) {
            throw new _MiscTemplateException(range.lho, new Object[] {
                    "Left side of range out of bounds, is: ", new Integer(start),
                    "\nbut the string has ", new Integer(targetStr.length()), " elements." });
        }
        if (end >= targetStr.length()) {
            throw new _MiscTemplateException(range.rho, new Object[] {
                    "Right side of range out of bounds, is: ", new Integer(end),
                    "\nbut the string is only ", new Integer(targetStr.length()), " characters long." });
        }
        try {
            return new SimpleScalar(targetStr.substring(start, end+1));
        } catch (RuntimeException re) {
            throw new _MiscTemplateException(re, new Object[] { "Unexpected exception: ", re });
        }
    }

    public String getCanonicalForm() {
        return target.getCanonicalForm() 
               + "[" 
               + nameExpression.getCanonicalForm() 
               + "]";
    }
    
    String getNodeTypeSymbol() {
        return "...[...]";
    }
    
    boolean isLiteral() {
        return constantValue != null || (target.isLiteral() && nameExpression.isLiteral());
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        return idx == 0 ? target : nameExpression;
    }

    ParameterRole getParameterRole(int idx) {
        return idx == 0 ? ParameterRole.LEFT_HAND_OPERAND : ParameterRole.ENCLOSED_OPERAND;
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new DynamicKeyName(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        nameExpression.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
}

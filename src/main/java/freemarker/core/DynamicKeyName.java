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
import freemarker.template.*;

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

    TemplateModel _getAsTemplateModel(Environment env) throws TemplateException
    {
        TemplateModel targetModel = target.getAsTemplateModel(env);
        assertNonNull(targetModel, target, env);
        if (nameExpression instanceof Range) {
            return dealWithRangeKey(targetModel, (Range) nameExpression, env);
        }
        TemplateModel keyModel = nameExpression.getAsTemplateModel(env);
        if(keyModel == null) {
            if(env.isClassicCompatible()) {
                keyModel = TemplateScalarModel.EMPTY_STRING;
            }
            else {
                assertNonNull(keyModel, nameExpression, env);
            }
        }
        if (keyModel instanceof TemplateNumberModel) {
            int index = EvaluationUtil.getNumber(keyModel, nameExpression, env).intValue();
            return dealWithNumericalKey(targetModel, index, env);
        }
        if (keyModel instanceof TemplateScalarModel) {
            String key = EvaluationUtil.getString((TemplateScalarModel)keyModel, nameExpression, env);
            return dealWithStringKey(targetModel, key, env);
        }
        throw invalidTypeException(keyModel, nameExpression, env, "number, range, or string");
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
            String s = target.getStringValue(env);
            try {
               return new SimpleScalar(s.substring(index, index+1));
            } catch (RuntimeException re) {
                throw new TemplateException("", re, env);
            }
        }
        catch(NonStringException e)
        {
            throw invalidTypeException(targetModel, target, env,
                    "sequence or string (or something that's implicitly convertible to string)");
        }
    }


    private TemplateModel dealWithStringKey(TemplateModel targetModel, 
                                            String key,
                                            Environment env)
        throws TemplateException
    {
        if(targetModel instanceof TemplateHashModel) {
            return((TemplateHashModel) targetModel).get(key);
        }
        throw invalidTypeException(targetModel, target, env, "hash");
    }

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, 
                                           Range range, 
                                           Environment env)
        throws TemplateException
    {
        int start = EvaluationUtil.getNumber(range.left, env).intValue();
        int end = 0;
        boolean hasRhs = range.hasRhs();
        if (hasRhs) {
            end = EvaluationUtil.getNumber(range.right, env).intValue();
        }
        if (targetModel instanceof TemplateSequenceModel) {
            TemplateSequenceModel sequence = (TemplateSequenceModel) targetModel;
            if (!hasRhs) end = sequence.size() -1;
            if (start < 0) {
                String msg = range.left.getStartLocation() + "\nNegative starting index for range, is " + range;
                throw new TemplateException(msg, env);
            }
            if (end < 0) {
                String msg = range.left.getStartLocation() + "\nNegative ending index for range, is " + range;
                throw new TemplateException(msg, env);
            }
            if (start >= sequence.size()) {
                String msg = range.left.getStartLocation() 
                            + "\nLeft side index of range out of bounds, is " + start
                            + ", but the sequence has only " + sequence.size() + " element(s) "
                            + "(note that indices are 0 based, and ranges are inclusive).";
                throw new TemplateException(msg, env);
            }
            if (end >= sequence.size()) {
                String msg = range.right.getStartLocation() 
                             + "\nRight side index of range out of bounds, is " + end
                             + ", but the sequence has only " + sequence.size() + " element(s)."
                             + "(note that indices are 0 based, and ranges are inclusive).";
                throw new TemplateException(msg, env);
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
        
        try
        {
            String s = target.getStringValue(env);
            if (!hasRhs) end = s.length() -1;
            if (start < 0) {
                String msg = range.left.getStartLocation() + "\nNegative starting index for range " + range + " : " + start;
                throw new TemplateException(msg, env);
            }
            if (end < 0) {
                String msg = range.left.getStartLocation() + "\nNegative ending index for range " + range + " : " + end;
                throw new TemplateException(msg, env);
            }
            if (start > s.length()) {
                String msg = range.left.getStartLocation() 
                            + "\nLeft side of range out of bounds, is: " + start
                            + "\nbut string " + targetModel + " has " + s.length() + " elements.";
                throw new TemplateException(msg, env);
            }
            if (end > s.length()) {
                String msg = range.right.getStartLocation() 
                             + "\nRight side of range out of bounds, is: " + end
                             + "\nbut string " + targetModel + " is only " + s.length() + " characters.";
                throw new TemplateException(msg, env);
            }
            try {
                return new SimpleScalar(s.substring(start, end+1));
            } catch (RuntimeException re) {
                String msg = "Error " + getStartLocation();
                throw new TemplateException(msg, re, env);
            }
        }
        catch(NonStringException e)
        {
            throw invalidTypeException(target.getAsTemplateModel(env), target, env, "number, scalar, or sequence");
        }
    }

    public String getCanonicalForm() {
        return target.getCanonicalForm() 
               + "[" 
               + nameExpression.getCanonicalForm() 
               + "]";
    }
    
    boolean isLiteral() {
        return constantValue != null || (target.isLiteral() && nameExpression.isLiteral());
    }

    Expression _deepClone(String name, Expression subst) {
    	return new DynamicKeyName(target.deepClone(name, subst), nameExpression.deepClone(name, subst));
    }
}

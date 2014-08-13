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

    private final Expression keyExpression;
    private final Expression target;

    DynamicKeyName(Expression target, Expression keyExpression) {
        this.target = target; 
        this.keyExpression = keyExpression;
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
        if (keyExpression instanceof Range) {
            return dealWithRangeKey(targetModel, (Range) keyExpression, env);
        }
        TemplateModel keyModel = keyExpression.eval(env);
        if(keyModel == null) {
            if(env.isClassicCompatible()) {
                keyModel = TemplateScalarModel.EMPTY_STRING;
            }
            else {
                keyExpression.assertNonNull(keyModel, env);
            }
        }
        if (keyModel instanceof TemplateNumberModel) {
            int index = keyExpression.modelToNumber(keyModel, env).intValue();
            return dealWithNumericalKey(targetModel, index, env);
        }
        if (keyModel instanceof TemplateScalarModel) {
            String key = EvalUtil.modelToString((TemplateScalarModel)keyModel, keyExpression, env);
            return dealWithStringKey(targetModel, key, env);
        }
        throw new UnexpectedTypeException(keyExpression, keyModel, "number, range, or string",
                new Class[] { TemplateNumberModel.class, TemplateScalarModel.class, Range.class }, env);
    }

    static private Class[] NUMERICAL_KEY_LHO_EXPECTED_TYPES;
    static {
        NUMERICAL_KEY_LHO_EXPECTED_TYPES = new Class[1 + NonStringException.STRING_COERCABLE_TYPES.length];
        NUMERICAL_KEY_LHO_EXPECTED_TYPES[0] = TemplateSequenceModel.class;
        for (int i = 0; i < NonStringException.STRING_COERCABLE_TYPES.length; i++) {
            NUMERICAL_KEY_LHO_EXPECTED_TYPES[i + 1] = NonStringException.STRING_COERCABLE_TYPES[i];
        }
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
                    target, targetModel,
                    "sequence or " + NonStringException.STRING_COERCABLE_TYPES_DESC,
                    NUMERICAL_KEY_LHO_EXPECTED_TYPES, env);
        }
    }

    private TemplateModel dealWithStringKey(TemplateModel targetModel, String key, Environment env)
        throws TemplateException
    {
        if(targetModel instanceof TemplateHashModel) {
            return((TemplateHashModel) targetModel).get(key);
        }
        throw new NonHashException(target, targetModel, env);
    }

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, 
                                           Range range, 
                                           Environment env)
        throws TemplateException
    {
        int start = range.lho.evalToNumber(env).intValue();
        boolean hasRhs = range.hasRho();
        int end;
        if (hasRhs) {
            end = range.rho.evalToNumber(env).intValue();
            if (range.exclusiveEnd) {
                
            }            
        } else {
            end = 0;
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
            throw new UnexpectedTypeException(
                    target, target.eval(env),
                    "sequence or " + NonStringException.STRING_COERCABLE_TYPES_DESC,
                    NUMERICAL_KEY_LHO_EXPECTED_TYPES, env);
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
                    "\nbut the string has ", new Integer(targetStr.length()), " characters." });
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
               + keyExpression.getCanonicalForm() 
               + "]";
    }
    
    String getNodeTypeSymbol() {
        return "...[...]";
    }
    
    boolean isLiteral() {
        return constantValue != null || (target.isLiteral() && keyExpression.isLiteral());
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        return idx == 0 ? target : keyExpression;
    }

    ParameterRole getParameterRole(int idx) {
        return idx == 0 ? ParameterRole.LEFT_HAND_OPERAND : ParameterRole.ENCLOSED_OPERAND;
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new DynamicKeyName(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        keyExpression.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
}

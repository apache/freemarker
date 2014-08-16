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
import java.util.Collections;

import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Constants;

/**
 * {@code target[keyExpression]}, where, in FM 2.3, {@code keyExpression} can be string, a number or a range,
 * and {@code target} can be a hash or a sequence.
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
        
        TemplateModel keyModel = keyExpression.eval(env);
        if (keyModel == null) {
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
        if (keyModel instanceof RangeModel) {
            return dealWithRangeKey(targetModel, (RangeModel) keyModel, env);
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

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, RangeModel range, Environment env) throws UnexpectedTypeException, InvalidReferenceException, TemplateException {
        final TemplateSequenceModel targetSeq;
        final String targetStr;
        if (targetModel instanceof TemplateSequenceModel) {
            targetSeq = (TemplateSequenceModel) targetModel;
            targetStr = null;
        } else {
            targetSeq = null;
            try {
                targetStr = target.evalAndCoerceToString(env);
            } catch(NonStringException e) {
                throw new UnexpectedTypeException(
                        target, target.eval(env),
                        "sequence or " + NonStringException.STRING_COERCABLE_TYPES_DESC,
                        NUMERICAL_KEY_LHO_EXPECTED_TYPES, env);
            }
        }
        
        final int size = range.size();
        final boolean rightUnbounded = range.isRightUnbounded();
        
        // Non-half empty ranges are accepted even if the begin index is out of bounds. That's because a such range
        // produces an empty sequence, which thus doesn't contain any illegal indexes.
        if (!rightUnbounded && size == 0) {
            return emptySequence(targetSeq);
        }

        final int firstIdx = range.getBegining();
        if (firstIdx < 0) {
            throw new _MiscTemplateException(keyExpression, new Object[] {
                    "Negative range start index (", new Integer(firstIdx),
                    ") isn't allowed for a range used for slicing." });
        }
        
        final int targetSize = targetStr != null ? targetStr.length() : targetSeq.size();
        
        // Right-unbounded ranges can start 1 after the last element of the target, because they are like ranges with
        // exclusive end index of targetSize. Thence a such range is just an empty list of indexes, and thus isn't OOB.
        // Right-bounded ranges at this point aren't empty, so the right index can't reach targetSize. 
        if (rightUnbounded ? firstIdx > targetSize : firstIdx >= targetSize) {
            throw new _MiscTemplateException(keyExpression, new Object[] {
                    "Range start index ", new Integer(firstIdx), " is out of bounds, because the sliced ",
                    (targetStr != null ? "string" : "sequence"),
                    " has only ", new Integer(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                    ". ", "(Note that indices are 0-based)." });
        }
        
        final int step = range.getStep();
        
        final int lastIdx;
        final int resultSize;
        if (!rightUnbounded) {
            lastIdx = firstIdx + (size - 1) * step;
            if (lastIdx < 0) {
                throw new _MiscTemplateException(keyExpression, new Object[] {
                        "Negative range end index (", new Integer(lastIdx),
                        ") isn't allowed for a range used for slicing." });
            }
            if (lastIdx >= targetSize) {
                throw new _MiscTemplateException(keyExpression, new Object[] {
                        "Range end index ", new Integer(lastIdx), " is out of bounds, because the sliced ",
                        (targetStr != null ? "string" : "sequence"),
                        " has only ", new Integer(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                        ". (Note that indices are 0-based)." });
            }
            
            resultSize = size;
        } else {
            // Note: Here we assume that abs(step) is 1.
            lastIdx = targetSize - 1;
            resultSize = targetSize - firstIdx;
        }
        
        if (targetSeq != null) {
            if (resultSize == 0) {
                return emptySequence(targetSeq);
            }
            
            ArrayList/*<TemplateModel>*/ list = new ArrayList(resultSize);
            int srcIdx = firstIdx;
            while (true) {
                list.add(targetSeq.get(srcIdx));
                if (srcIdx == lastIdx) break;
                srcIdx += step;
            }
            return new SimpleSequence(list);
        } else {
            if (resultSize == 0) {
                return TemplateScalarModel.EMPTY_STRING;
            }
            
            // The "+ 1" is for emulating the legacy bug, where "foo"[1, 0] gives "" instead of an error.  
            // Fix this in FTL [2.4]
            if (lastIdx + 1 < firstIdx) {
                throw new _MiscTemplateException(
                        keyExpression, new Object[] {
                            "Decreasing ranges aren't allowed for slicing strings (as it would give reversed "
                            + "text). The indexes range was: first = ",
                            new Integer(firstIdx), ", last = ", new Integer(lastIdx)
                        });
            }
            
            return new SimpleScalar(targetStr.substring(firstIdx, lastIdx + 1));
        }
    }

    private TemplateModel emptySequence(final TemplateSequenceModel targetSeq) {
        return targetSeq != null
                ? (getTemplate().getConfiguration().getIncompatibleImprovements().intValue() < 2003021
                        ? new SimpleSequence(Collections.EMPTY_LIST)
                        : Constants.EMPTY_SEQUENCE)
                : TemplateScalarModel.EMPTY_STRING;
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

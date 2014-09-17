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
import freemarker.template._TemplateAPI;
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
                keyExpression.assertNonNull(null, env);
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
            } catch (IndexOutOfBoundsException e) {
                if (index < 0) {
                    throw new _MiscTemplateException(new Object[] {
                                "Negative index not allowed: ",
                                new Integer(index) });
                }
                if (index >= s.length()) {
                    throw new _MiscTemplateException(new Object[] {
                            "String index out of range: The index was ", new Integer(index),
                            " (0-based), but the length of the string is only ", new Integer(s.length()) , "." });
                }
                throw new RuntimeException("Can't explain exception", e);
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

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, RangeModel range, Environment env)
    throws UnexpectedTypeException, InvalidReferenceException, TemplateException {
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
        final boolean rightAdaptive = range.isRightAdaptive();
        
        // Right bounded empty ranges are accepted even if the begin index is out of bounds. That's because a such range
        // produces an empty sequence, which thus doesn't contain any illegal indexes.
        if (!rightUnbounded && size == 0) {
            return emptyResult(targetSeq != null);
        }

        final int firstIdx = range.getBegining();
        if (firstIdx < 0) {
            throw new _MiscTemplateException(keyExpression, new Object[] {
                    "Negative range start index (", new Integer(firstIdx),
                    ") isn't allowed for a range used for slicing." });
        }
        
        final int targetSize = targetStr != null ? targetStr.length() : targetSeq.size();
        final int step = range.getStep();
        
        // Right-adaptive increasing ranges can start 1 after the last element of the target, because they are like
        // ranges with exclusive end index of at most targetSize. Thence a such range is just an empty list of indexes,
        // and thus it isn't out-of-bounds.
        // Right-adaptive decreasing ranges has exclusive end -1, so it can't help on a  to high firstIndex. 
        // Right-bounded ranges at this point aren't empty, so the right index surely can't reach targetSize. 
        if (rightAdaptive && step == 1 ? firstIdx > targetSize : firstIdx >= targetSize) {
            throw new _MiscTemplateException(keyExpression, new Object[] {
                    "Range start index ", new Integer(firstIdx), " is out of bounds, because the sliced ",
                    (targetStr != null ? "string" : "sequence"),
                    " has only ", new Integer(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                    ". ", "(Note that indices are 0-based)." });
        }
        
        final int resultSize;
        if (!rightUnbounded) {
            final int lastIdx = firstIdx + (size - 1) * step;
            if (lastIdx < 0) {
                if (!rightAdaptive) {
                    throw new _MiscTemplateException(keyExpression, new Object[] {
                            "Negative range end index (", new Integer(lastIdx),
                            ") isn't allowed for a range used for slicing." });
                } else {
                    resultSize = firstIdx + 1;
                }
            } else if (lastIdx >= targetSize) {
                if (!rightAdaptive) {
                    throw new _MiscTemplateException(keyExpression, new Object[] {
                            "Range end index ", new Integer(lastIdx), " is out of bounds, because the sliced ",
                            (targetStr != null ? "string" : "sequence"),
                            " has only ", new Integer(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                            ". (Note that indices are 0-based)." });
                } else {
                    resultSize = Math.abs(targetSize - firstIdx);
                }
            } else {
                resultSize = size;
            }
        } else {
            resultSize = targetSize - firstIdx;
        }
        
        if (resultSize == 0) {
            return emptyResult(targetSeq != null);
        }
        if (targetSeq != null) {
            ArrayList/*<TemplateModel>*/ list = new ArrayList(resultSize);
            int srcIdx = firstIdx;
            for (int i = 0; i < resultSize; i++) {
                list.add(targetSeq.get(srcIdx));
                srcIdx += step;
            }
            // List items are already wrapped, so the wrapper will be null:
            return new SimpleSequence(list, null);
        } else {
            final int exclEndIdx;
            if (step < 0 && resultSize > 1) {
                if (!(range.isAffactedByStringSlicingBug() && resultSize == 2)) {
                    throw new _MiscTemplateException(
                            keyExpression, new Object[] {
                                "Decreasing ranges aren't allowed for slicing strings (as it would give reversed "
                                + "text). The index range was: first = ",
                                new Integer(firstIdx), ", last = ", new Integer(firstIdx + (resultSize - 1) * step)
                            });
                } else {
                    // Emulate the legacy bug, where "foo"[n .. n-1] gives "" instead of an error (if n >= 1).  
                    // Fix this in FTL [2.4]
                    exclEndIdx = firstIdx;
                }
            } else {
                exclEndIdx = firstIdx + resultSize;
            }
            
            return new SimpleScalar(targetStr.substring(firstIdx, exclEndIdx));
        }
    }

    private TemplateModel emptyResult(boolean seq) {
        return seq
                ? (_TemplateAPI.getTemplateLanguageVersionAsInt(this) < _TemplateAPI.VERSION_INT_2_3_21
                        ? new SimpleSequence(Collections.EMPTY_LIST, null)
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;

/**
 * AST expression node: {@code target[keyExpression]}, where, in FM 2.3, {@code keyExpression} can be string, a number
 * or a range, and {@code target} can be a hash or a sequence.
 */
final class ASTExpDynamicKeyName extends ASTExpression {

    private final ASTExpression keyExpression;
    private final ASTExpression target;

    ASTExpDynamicKeyName(ASTExpression target, ASTExpression keyExpression) {
        this.target = target; 
        this.keyExpression = keyExpression;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel targetModel = target.eval(env);
        target.assertNonNull(targetModel, env);
        
        TemplateModel keyModel = keyExpression.eval(env);
        keyExpression.assertNonNull(keyModel, env);
        if (keyModel instanceof TemplateNumberModel) {
            int index = keyExpression.modelToNumber(keyModel, env).intValue();
            return dealWithNumericalKey(targetModel, index, env);
        }
        if (keyModel instanceof TemplateScalarModel) {
            String key = _EvalUtils.modelToString((TemplateScalarModel) keyModel, keyExpression);
            return dealWithStringKey(targetModel, key, env);
        }
        if (keyModel instanceof RangeModel) {
            return dealWithRangeKey(targetModel, (RangeModel) keyModel, env);
        }
        throw MessageUtils.newUnexpectedOperandTypeException(keyExpression, keyModel,
                "number, range, or string",
                new Class[] { TemplateNumberModel.class, TemplateScalarModel.class, ASTExpRange.class },
                null, env);
    }

    static private Class[] NUMERICAL_KEY_LHO_EXPECTED_TYPES;
    static {
        NUMERICAL_KEY_LHO_EXPECTED_TYPES = new Class[1 + MessageUtils.EXPECTED_TYPES_STRING_COERCABLE.length];
        NUMERICAL_KEY_LHO_EXPECTED_TYPES[0] = TemplateSequenceModel.class;
        for (int i = 0; i < MessageUtils.EXPECTED_TYPES_STRING_COERCABLE.length; i++) {
            NUMERICAL_KEY_LHO_EXPECTED_TYPES[i + 1] = MessageUtils.EXPECTED_TYPES_STRING_COERCABLE[i];
        }
    }
    
    private TemplateModel dealWithNumericalKey(TemplateModel targetModel, 
                                               int index, 
                                               Environment env)
        throws TemplateException {
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

        String s;
        try {
            s = target.evalAndCoerceToPlainText(env);
        } catch (TemplateException e) {
            // TODO [FM3] Wrong, as we don't know why this was thrown. I think we just shouldn't coerce.
            throw MessageUtils.newUnexpectedOperandTypeException(
                    target, targetModel,
                    "sequence or " + MessageUtils.STRING_COERCABLE_TYPES_DESC,
                    NUMERICAL_KEY_LHO_EXPECTED_TYPES,
                    (targetModel instanceof TemplateHashModel
                            ? new Object[] { "You had a numberical value inside the []. Currently that's only "
                                    + "supported for sequences (lists) and strings. To get a Map item with a "
                                    + "non-string key, use myMap?api.get(myKey)." }
                            : null),
                    env);
        }
        try {
            return new SimpleScalar(s.substring(index, index + 1));
        } catch (IndexOutOfBoundsException e) {
            if (index < 0) {
                throw new TemplateException("Negative index not allowed: ", Integer.valueOf(index));
            }
            if (index >= s.length()) {
                throw new TemplateException(
                        "String index out of range: The index was ", Integer.valueOf(index),
                        " (0-based), but the length of the string is only ", Integer.valueOf(s.length()) , ".");
            }
            throw new RuntimeException("Can't explain exception", e);
        }
    }

    private TemplateModel dealWithStringKey(TemplateModel targetModel, String key, Environment env)
        throws TemplateException {
        if (targetModel instanceof TemplateHashModel) {
            return((TemplateHashModel) targetModel).get(key);
        }
        throw MessageUtils.newUnexpectedOperandTypeException(target, targetModel, TemplateHashModel.class, env);
    }

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, RangeModel range, Environment env)
    throws TemplateException {
        final TemplateSequenceModel targetSeq;
        final String targetStr;
        if (targetModel instanceof TemplateSequenceModel) {
            targetSeq = (TemplateSequenceModel) targetModel;
            targetStr = null;
        } else {
            targetSeq = null;
            try {
                targetStr = target.evalAndCoerceToPlainText(env);
            } catch (TemplateException e) {
                // TODO [FM3] Wrong, as we don't know why this was thrown. I think we just shouldn't coerce.
                throw MessageUtils.newUnexpectedOperandTypeException(
                        target, target.eval(env),
                        "sequence or " + MessageUtils.STRING_COERCABLE_TYPES_DESC,
                        NUMERICAL_KEY_LHO_EXPECTED_TYPES, null, env);
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
            throw new TemplateException(keyExpression,
                    "Negative range start index (", Integer.valueOf(firstIdx),
                    ") isn't allowed for a range used for slicing.");
        }
        
        final int targetSize = targetStr != null ? targetStr.length() : targetSeq.size();
        final int step = range.getStep();
        
        // Right-adaptive increasing ranges can start 1 after the last element of the target, because they are like
        // ranges with exclusive end index of at most targetSize. Thence a such range is just an empty list of indexes,
        // and thus it isn't out-of-bounds.
        // Right-adaptive decreasing ranges has exclusive end -1, so it can't help on a  to high firstIndex. 
        // Right-bounded ranges at this point aren't empty, so the right index surely can't reach targetSize. 
        if (rightAdaptive && step == 1 ? firstIdx > targetSize : firstIdx >= targetSize) {
            throw new TemplateException(keyExpression,
                    "Range start index ", Integer.valueOf(firstIdx), " is out of bounds, because the sliced ",
                    (targetStr != null ? "string" : "sequence"),
                    " has only ", Integer.valueOf(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                    ". ", "(Note that indices are 0-based).");
        }
        
        final int resultSize;
        if (!rightUnbounded) {
            final int lastIdx = firstIdx + (size - 1) * step;
            if (lastIdx < 0) {
                if (!rightAdaptive) {
                    throw new TemplateException(keyExpression,
                            "Negative range end index (", Integer.valueOf(lastIdx),
                            ") isn't allowed for a range used for slicing.");
                } else {
                    resultSize = firstIdx + 1;
                }
            } else if (lastIdx >= targetSize) {
                if (!rightAdaptive) {
                    throw new TemplateException(keyExpression,
                            "Range end index ", Integer.valueOf(lastIdx), " is out of bounds, because the sliced ",
                            (targetStr != null ? "string" : "sequence"),
                            " has only ", Integer.valueOf(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                            ". (Note that indices are 0-based).");
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
            NativeSequence resultSeq = new NativeSequence(resultSize);
            int srcIdx = firstIdx;
            for (int i = 0; i < resultSize; i++) {
                resultSeq.add(targetSeq.get(srcIdx));
                srcIdx += step;
            }
            // List items are already wrapped, so the wrapper will be null:
            return resultSeq;
        } else {
            final int exclEndIdx;
            if (step < 0 && resultSize > 1) {
                if (!(range.isAffactedByStringSlicingBug() && resultSize == 2)) {
                    throw new TemplateException(keyExpression,
                            "Decreasing ranges aren't allowed for slicing strings (as it would give reversed text). "
                            + "The index range was: first = ", Integer.valueOf(firstIdx),
                            ", last = ", Integer.valueOf(firstIdx + (resultSize - 1) * step));
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
        return seq ? TemplateSequenceModel.EMPTY_SEQUENCE : TemplateScalarModel.EMPTY_STRING;
    }

    @Override
    public String getCanonicalForm() {
        return target.getCanonicalForm() 
               + "[" 
               + keyExpression.getCanonicalForm() 
               + "]";
    }
    
    @Override
    String getASTNodeDescriptor() {
        return "...[...]";
    }
    
    @Override
    boolean isLiteral() {
        return constantValue != null || (target.isLiteral() && keyExpression.isLiteral());
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        return idx == 0 ? target : keyExpression;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return idx == 0 ? ParameterRole.LEFT_HAND_OPERAND : ParameterRole.ENCLOSED_OPERAND;
    }

    @Override
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpDynamicKeyName(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        keyExpression.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
}

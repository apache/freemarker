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

package freemarker.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template._ObjectWrappers;
import freemarker.template._TemplateAPI;
import freemarker.template._VersionInts;
import freemarker.template.utility.Constants;

/**
 * {@code target[keyExpression]}, where, in FM 2.3, {@code keyExpression} can be string, a number or a range,
 * and {@code target} can be a hash or a sequence.
 */
final class DynamicKeyName extends Expression {

    private static final int UNKNOWN_RESULT_SIZE = -1;

    private final Expression keyExpression;
    private final Expression target;
    private boolean lazilyGeneratedResultEnabled;

    DynamicKeyName(Expression target, Expression keyExpression) {
        this.target = target; 
        this.keyExpression = keyExpression;

        target.enableLazilyGeneratedResult();
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
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
            if (env.isClassicCompatible()) {
                keyModel = TemplateScalarModel.EMPTY_STRING;
            } else {
                keyExpression.assertNonNull(null, env);
            }
        }
        if (keyModel instanceof TemplateNumberModel) {
            int index = keyExpression.modelToNumber(keyModel, env).intValue();
            return dealWithNumericalKey(targetModel, index, env);
        }
        if (keyModel instanceof TemplateScalarModel) {
            String key = EvalUtil.modelToString((TemplateScalarModel) keyModel, keyExpression, env);
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
        if (targetModel instanceof LazilyGeneratedCollectionModel
                && ((LazilyGeneratedCollectionModel) targetModel).isSequence()) {
            if (index < 0) {
                return null;
            }
            TemplateModelIterator iter = ((LazilyGeneratedCollectionModel) targetModel).iterator();
            for (int curIndex = 0; iter.hasNext(); curIndex++) {
                TemplateModel next = iter.next();
                if (index == curIndex) {
                    return next;
                }
            }
            return null;
        }

        // Fall back to get a character from a string
        try {
            String s = target.evalAndCoerceToPlainText(env);
            try {
                return new SimpleScalar(s.substring(index, index + 1));
            } catch (IndexOutOfBoundsException e) {
                if (index < 0) {
                    throw new _MiscTemplateException("Negative index not allowed: ", Integer.valueOf(index));
                }
                if (index >= s.length()) {
                    throw new _MiscTemplateException(
                            "String index out of range: The index was ", Integer.valueOf(index),
                            " (0-based), but the length of the string is only ", Integer.valueOf(s.length()) , ".");
                }
                throw new RuntimeException("Can't explain exception", e);
            }
        } catch (NonStringException e) {
            throw new UnexpectedTypeException(
                    target, targetModel,
                    "sequence or " + NonStringException.STRING_COERCABLE_TYPES_DESC,
                    NUMERICAL_KEY_LHO_EXPECTED_TYPES,
                    (targetModel instanceof TemplateHashModel
                            ? "You had a numerical value inside the []. Currently that's only supported for "
                                    + "sequences (lists) and strings. To get a Map item with a non-string key, "
                                    + "use myMap?api.get(myKey)."
                            : null),
                    env);
        }
    }

    private TemplateModel dealWithStringKey(TemplateModel targetModel, String key, Environment env)
        throws TemplateException {
        if (targetModel instanceof TemplateHashModel) {
            return((TemplateHashModel) targetModel).get(key);
        }
        throw new NonHashException(target, targetModel, env);
    }

    private TemplateModel dealWithRangeKey(TemplateModel targetModel, RangeModel range, Environment env)
    throws TemplateException {
        // We can have 3 kind of left hand operands ("targets"): sequence, lazily generated sequence, string
        final TemplateSequenceModel targetSeq;
        final LazilyGeneratedCollectionModel targetLazySeq;
        final String targetStr;
        if (targetModel instanceof TemplateSequenceModel) {
            targetSeq = (TemplateSequenceModel) targetModel;
            targetLazySeq = null;
            targetStr = null;
        } else if (targetModel instanceof LazilyGeneratedCollectionModel
                && ((LazilyGeneratedCollectionModel) targetModel).isSequence()) {
            targetSeq = null;
            targetLazySeq = (LazilyGeneratedCollectionModel) targetModel;
            targetStr = null;
        } else {
            targetSeq = null;
            targetLazySeq = null;
            try {
                targetStr = target.evalAndCoerceToPlainText(env);
            } catch (NonStringException e) {
                throw new UnexpectedTypeException(
                        target, target.eval(env),
                        "sequence or " + NonStringException.STRING_COERCABLE_TYPES_DESC,
                        NUMERICAL_KEY_LHO_EXPECTED_TYPES, env);
            }
        }
        
        final int rangeSize = range.size(); // Warning: Value is meaningless for right unbounded sequences
        final boolean rightUnbounded = range.isRightUnbounded();
        final boolean rightAdaptive = range.isRightAdaptive(); // Always true if rightUnbounded
        
        // Empty ranges are accepted even if the begin index is out of bounds. That's because a such range
        // produces an empty sequence, which thus doesn't contain any illegal indexes.
        if (!rightUnbounded && rangeSize == 0) {
            return emptyResult(targetSeq != null);
        }

        final int firstIdx = range.getBegining();
        if (firstIdx < 0) {
            throw new _MiscTemplateException(keyExpression,
                    "Negative range start index (", Integer.valueOf(firstIdx),
                    ") isn't allowed for a range used for slicing.");
        }

        final int step = range.getStep(); // Currently always 1 or -1

        final int targetSize;
        final boolean targetSizeKnown; // Didn't want to use targetSize = -1, as we don't control the seq.size() impl.
        if (targetStr != null) {
            targetSize = targetStr.length();
            targetSizeKnown = true;
        } else if (targetSeq != null) {
            targetSize = targetSeq.size();
            targetSizeKnown = true;
        } else if (targetLazySeq instanceof TemplateCollectionModelEx) {
            // E.g. the size of seq?map(f) is known, despite that the elements are lazily calculated.
            targetSize = ((TemplateCollectionModelEx) targetLazySeq).size();
            targetSizeKnown = true;
        } else {
            targetSize = Integer.MAX_VALUE;
            targetSizeKnown = false;
        }

        if (targetSizeKnown) {
            // Right-adaptive increasing ranges can start 1 after the last element of the target, because they are like
            // ranges with exclusive end index of at most targetSize. Hence a such range is just an empty list of
            // indexes, and thus it isn't out-of-bounds.
            // Right-adaptive decreasing ranges has exclusive end -1, so it can't help on a too high firstIndex.
            // Right-bounded ranges at this point aren't empty, so firstIndex == targetSize can't be allowed.
            if (rightAdaptive && step == 1 ? firstIdx > targetSize : firstIdx >= targetSize) {
                throw new _MiscTemplateException(keyExpression,
                        "Range start index ", Integer.valueOf(firstIdx), " is out of bounds, because the sliced ",
                        (targetStr != null ? "string" : "sequence"),
                        " has only ", Integer.valueOf(targetSize), " ",
                        (targetStr != null ? "character(s)" : "element(s)"),
                        ". ", "(Note that indices are 0-based).");
            }
        }

        // Calculate resultSize. Note that:
        // - It might will be UNKNOWN_RESULT_SIZE when targetLazySeq != null.
        // - It might will be out-of-bounds if !targetSizeKnown (otherwise we validate if the range is correct)
        final int resultSize;
        if (!rightUnbounded) {
            final int lastIdx = firstIdx + (rangeSize - 1) * step; // Note: lastIdx is inclusive
            if (lastIdx < 0) {
                if (!rightAdaptive) {
                    throw new _MiscTemplateException(keyExpression,
                            "Negative range end index (", Integer.valueOf(lastIdx),
                            ") isn't allowed for a range used for slicing.");
                } else {
                    resultSize = firstIdx + 1;
                }
            } else if (targetSizeKnown && lastIdx >= targetSize) {
                if (!rightAdaptive) {
                    throw new _MiscTemplateException(keyExpression,
                            "Range end index ", Integer.valueOf(lastIdx), " is out of bounds, because the sliced ",
                            (targetStr != null ? "string" : "sequence"),
                            " has only ", Integer.valueOf(targetSize), " ", (targetStr != null ? "character(s)" : "element(s)"),
                            ". (Note that indices are 0-based).");
                } else {
                    resultSize = Math.abs(targetSize - firstIdx);
                }
            } else {
                resultSize = rangeSize;
            }
        } else { // rightUnbounded
            resultSize = targetSizeKnown ? targetSize - firstIdx : UNKNOWN_RESULT_SIZE;
        }
        
        if (resultSize == 0) {
            return emptyResult(targetSeq != null);
        }
        if (targetSeq != null) {
            // In principle we should take lazilyGeneratedResultEnabled into account, but that wouldn't be backward
            // compatible. For example, with lazily generated sequence result <#list xs[b..e] as x> would behave
            // differently if xs is modified inside the #list nested content.
            ArrayList<TemplateModel> resultList = new ArrayList<>(resultSize);
            int srcIdx = firstIdx;
            for (int i = 0; i < resultSize; i++) {
                resultList.add(targetSeq.get(srcIdx));
                srcIdx += step;
            }
            // List items are already wrapped:
            return new SimpleSequence(resultList, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
        } else if (targetLazySeq != null) {
            // As a targetLazySeq can only occur if a new built-in like ?filter or ?map was used somewhere in the target
            // expression, in this case we can return lazily generated sequence without breaking backward compatibility.
            if (step == 1) {
                return getStep1RangeFromIterator(targetLazySeq.iterator(), range, resultSize, targetSizeKnown);
            } else if (step == -1) {
                return getStepMinus1RangeFromIterator(targetLazySeq.iterator(), range, resultSize);
            } else {
                throw new AssertionError();
            }
        } else {
            final int exclEndIdx;
            if (step < 0 && resultSize > 1) {
                if (!(range.isAffectedByStringSlicingBug() && resultSize == 2)) {
                    throw new _MiscTemplateException(keyExpression,
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

    private TemplateModel getStep1RangeFromIterator(
            final TemplateModelIterator targetIter, final RangeModel range, int resultSize, boolean targetSizeKnown)
            throws TemplateModelException {
        final int firstIdx = range.getBegining();
        final int lastIdx = firstIdx + (range.size() - 1); // Note: meaningless if the range is right unbounded
        final boolean rightAdaptive = range.isRightAdaptive();
        final boolean rightUnbounded = range.isRightUnbounded();
        if (lazilyGeneratedResultEnabled) {
            TemplateModelIterator iterator = new TemplateModelIterator() {
                private boolean elementsBeforeFirsIndexWereSkipped;
                private int nextIdx;

                @Override
                public TemplateModel next() throws TemplateModelException {
                    ensureElementsBeforeFirstIndexWereSkipped();
                    if (!rightUnbounded && nextIdx > lastIdx) {
                        // We fail because the consumer of this iterator hasn't used hasNext() properly.
                        throw new _TemplateModelException(
                                "Iterator has no more elements (at index ", Integer.valueOf(nextIdx), ")");
                    }
                    if (!rightAdaptive && !targetIter.hasNext()) {
                        // We fail because the range was wrong, not because of the consumer of this iterator .
                        throw newRangeEndOutOfBoundsException(nextIdx, lastIdx);
                    }
                    TemplateModel result = targetIter.next();
                    nextIdx++;
                    return result;
                }

                @Override
                public boolean hasNext() throws TemplateModelException {
                    ensureElementsBeforeFirstIndexWereSkipped();
                    return (rightUnbounded || nextIdx <= lastIdx) && (!rightAdaptive || targetIter.hasNext());
                }

                public void ensureElementsBeforeFirstIndexWereSkipped() throws TemplateModelException {
                    if (elementsBeforeFirsIndexWereSkipped) {
                        return;
                    }

                    skipElementsBeforeFirstIndex(targetIter, firstIdx);
                    nextIdx = firstIdx;
                    elementsBeforeFirsIndexWereSkipped = true;
                }
            };
            return resultSize != UNKNOWN_RESULT_SIZE && targetSizeKnown  // targetSizeKnown => range end was validated
                    ? new LazilyGeneratedCollectionModelWithAlreadyKnownSize(iterator, resultSize, true)
                    : new LazilyGeneratedCollectionModelWithUnknownSize(iterator, true);
        } else { // !lazilyGeneratedResultEnabled
            List<TemplateModel> resultList = resultSize != UNKNOWN_RESULT_SIZE
                    ? new ArrayList<TemplateModel>(resultSize)
                    : new ArrayList<TemplateModel>();
            skipElementsBeforeFirstIndex(targetIter, firstIdx);
            collectElements: for (int nextIdx = firstIdx; rightUnbounded || nextIdx <= lastIdx; nextIdx++) {
                if (!targetIter.hasNext()) {
                    if (!rightAdaptive) {
                        throw newRangeEndOutOfBoundsException(nextIdx, lastIdx);
                    }
                    break collectElements;
                }
                resultList.add(targetIter.next());
            }
            // List items are already wrapped:
            return new SimpleSequence(resultList, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
        }
    }

    private void skipElementsBeforeFirstIndex(TemplateModelIterator targetIter, int firstIdx) throws TemplateModelException {
        int nextIdx = 0;
        while (nextIdx < firstIdx) {
            if (!targetIter.hasNext()) {
                throw new _TemplateModelException(keyExpression,
                        "Range start index ", Integer.valueOf(firstIdx),
                        " is out of bounds, as the sliced sequence only has ", nextIdx, " elements.");
            }
            targetIter.next();
            nextIdx++;
        }
    }

    private _TemplateModelException newRangeEndOutOfBoundsException(int nextIdx, int lastIdx) {
        return new _TemplateModelException(keyExpression,
                "Range end index ", Integer.valueOf(lastIdx), " is out of bounds, as sliced sequence " +
                "only has ", nextIdx, " elements.");
    }

    // Because the order has to be reversed, we have to "buffer" the stream in this case.
    private TemplateModel getStepMinus1RangeFromIterator(TemplateModelIterator targetIter, RangeModel range,
            int resultSize)
            throws TemplateException {
        int highIndex = range.getBegining();
        // Low index was found to be valid earlier. So now something like [2..*-9] becomes to [2..0] in effect.
        int lowIndex = Math.max(highIndex - (range.size() - 1), 0);

        final TemplateModel[] resultElements = new TemplateModel[highIndex - lowIndex + 1];

        int srcIdx = 0; // With an Iterator we can only start from index 0
        int dstIdx = resultElements.length - 1; // Write into the result array backwards
        while (srcIdx <= highIndex && targetIter.hasNext()) {
            TemplateModel element = targetIter.next();
            if (srcIdx >= lowIndex) {
                resultElements[dstIdx--] = element;
            }
            srcIdx++;
        }
        if (dstIdx != -1) {
            throw new _MiscTemplateException(DynamicKeyName.this,
                    "Range top index " + highIndex + " (0-based) is outside the sliced sequence of length " +
                    srcIdx + ".");
        }
        return new SimpleSequence(Arrays.asList(resultElements), _ObjectWrappers.SAFE_OBJECT_WRAPPER);
    }

    private TemplateModel emptyResult(boolean seq) {
        return seq
                ? (_TemplateAPI.getTemplateLanguageVersionAsInt(this) < _VersionInts.V_2_3_21
                        ? new SimpleSequence(_ObjectWrappers.SAFE_OBJECT_WRAPPER)
                        : Constants.EMPTY_SEQUENCE)
                : TemplateScalarModel.EMPTY_STRING;
    }

    @Override
    void enableLazilyGeneratedResult() {
        lazilyGeneratedResultEnabled = true;
    }

    @Override
    public String getCanonicalForm() {
        return target.getCanonicalForm() 
               + "[" 
               + keyExpression.getCanonicalForm() 
               + "]";
    }
    
    @Override
    String getNodeTypeSymbol() {
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
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new DynamicKeyName(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        keyExpression.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
}

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

import java.util.HashSet;
import java.util.Set;

import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template._ObjectWrappers;

/**
 * An operator for the + operator. Note that this is treated
 * separately from the other 4 arithmetic operators,
 * since + is overloaded to mean string concatenation.
 */
final class AddConcatExpression extends Expression {

    private final Expression left;
    private final Expression right;

    AddConcatExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return _eval(env, this, left, left.eval(env), right, right.eval(env));
    }

    /**
     * @param leftExp
     *            Used for error messages only; can be {@code null}
     * @param rightExp
     *            Used for error messages only; can be {@code null}
     */
    static TemplateModel _eval(Environment env,
            TemplateObject parent,
            Expression leftExp, TemplateModel leftModel,
            Expression rightExp, TemplateModel rightModel)
            throws TemplateModelException, TemplateException, NonStringException {
        if (leftModel instanceof TemplateNumberModel && rightModel instanceof TemplateNumberModel) {
            Number first = EvalUtil.modelToNumber((TemplateNumberModel) leftModel, leftExp);
            Number second = EvalUtil.modelToNumber((TemplateNumberModel) rightModel, rightExp);
            return _evalOnNumbers(env, parent, first, second);
        } else if (leftModel instanceof TemplateSequenceModel && rightModel instanceof TemplateSequenceModel) {
            return new ConcatenatedSequence((TemplateSequenceModel) leftModel, (TemplateSequenceModel) rightModel);
        } else {
            boolean hashConcatPossible
                    = leftModel instanceof TemplateHashModel && rightModel instanceof TemplateHashModel;
            try {
                // We try string addition first. If hash addition is possible, then instead of throwing exception
                // we return null and do hash addition instead. (We can't simply give hash addition a priority, like
                // with sequence addition above, as FTL strings are often also FTL hashes.)
                Object leftOMOrStr = EvalUtil.coerceModelToStringOrMarkup(
                        leftModel, leftExp, /* returnNullOnNonCoercableType = */ hashConcatPossible, null,
                        env);
                if (leftOMOrStr == null) {
                    return _eval_concatenateHashes(leftModel, rightModel);
                }

                // Same trick with null return as above.
                Object rightOMOrStr = EvalUtil.coerceModelToStringOrMarkup(
                        rightModel, rightExp, /* returnNullOnNonCoercableType = */ hashConcatPossible, null,
                        env);
                if (rightOMOrStr == null) {
                    return _eval_concatenateHashes(leftModel, rightModel);
                }

                if (leftOMOrStr instanceof String) {
                    if (rightOMOrStr instanceof String) {
                        return new SimpleScalar(((String) leftOMOrStr).concat((String) rightOMOrStr));
                    } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                        TemplateMarkupOutputModel<?> rightMO = (TemplateMarkupOutputModel<?>) rightOMOrStr; 
                        return EvalUtil.concatMarkupOutputs(parent,
                                rightMO.getOutputFormat().fromPlainTextByEscaping((String) leftOMOrStr),
                                rightMO);
                    }                    
                } else { // leftOMOrStr instanceof TemplateMarkupOutputModel 
                    TemplateMarkupOutputModel<?> leftMO = (TemplateMarkupOutputModel<?>) leftOMOrStr; 
                    if (rightOMOrStr instanceof String) {  // markup output
                        return EvalUtil.concatMarkupOutputs(parent,
                                leftMO,
                                leftMO.getOutputFormat().fromPlainTextByEscaping((String) rightOMOrStr));
                    } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                        return EvalUtil.concatMarkupOutputs(parent,
                                leftMO,
                                (TemplateMarkupOutputModel<?>) rightOMOrStr);
                    }
                }
            } catch (NonStringOrTemplateOutputException e) {
                // 2.4: Remove this catch; it's for BC, after reworking hash addition so it doesn't rely on this. But
                // user code might throws this (very unlikely), and then in 2.3.x we did catch that too, incorrectly.
                if (hashConcatPossible) {
                    return _eval_concatenateHashes(leftModel, rightModel);
                } else {
                    throw e;
                }
            }
        }
    }

    private static TemplateModel _eval_concatenateHashes(TemplateModel leftModel, TemplateModel rightModel)
            throws TemplateModelException {
        if (leftModel instanceof TemplateHashModelEx && rightModel instanceof TemplateHashModelEx) {
            TemplateHashModelEx leftModelEx = (TemplateHashModelEx) leftModel;
            TemplateHashModelEx rightModelEx = (TemplateHashModelEx) rightModel;
            if (leftModelEx.size() == 0) {
                return rightModelEx;
            } else if (rightModelEx.size() == 0) {
                return leftModelEx;
            } else {
                return new ConcatenatedHashEx(leftModelEx, rightModelEx);
            }
        } else {
            return new ConcatenatedHash((TemplateHashModel) leftModel,
                                        (TemplateHashModel) rightModel);
        }
    }

    static TemplateModel _evalOnNumbers(Environment env, TemplateObject parent, Number first, Number second)
            throws TemplateException {
        ArithmeticEngine ae = EvalUtil.getArithmeticEngine(env, parent);
        return new SimpleNumber(ae.add(first, second));
    }

    @Override
    boolean isLiteral() {
        return constantValue != null || (left.isLiteral() && right.isLiteral());
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
    	return new AddConcatExpression(
    	left.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    public String getCanonicalForm() {
        return left.getCanonicalForm() + " + " + right.getCanonicalForm();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return "+";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        return idx == 0 ? left : right;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }

    // Non-private for unit testing
    static final class ConcatenatedSequence
    implements
        TemplateSequenceModel, TemplateCollectionModel {
        private final TemplateSequenceModel left;
        private final TemplateSequenceModel right;

        ConcatenatedSequence(TemplateSequenceModel left, TemplateSequenceModel right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int size()
        throws TemplateModelException {
            return left.size() + right.size();
        }

        @Override
        public TemplateModel get(int i)
        throws TemplateModelException {
            int ls = left.size();
            return i < ls ? left.get(i) : right.get(i - ls);
        }

        @Override
        public TemplateModelIterator iterator() throws TemplateModelException {
            return new ConcatenatedSequenceIterator(this);
        }

    }

    private static class ConcatenatedSequenceIterator implements TemplateModelIterator {
        /** The path from the root down to the parent of {@link #currentSegment} */
        private ParentStep parentStep = null;

        private TemplateSequenceModel currentSegment;
        private int currentSegmentNextIndex;
        private TemplateModelIterator currentSegmentIterator;

        private boolean hasPrefetchedResult;
        private TemplateModel prefetchedNext;
        private boolean prefetchedHasNext;

        public ConcatenatedSequenceIterator(ConcatenatedSequence concatSeq) throws TemplateModelException {
            // Descent down to the first nested sequence, and memorize the path down there
            descendToLeftmostSubsequence(concatSeq);
        }

        private void setCurrentSegment(TemplateSequenceModel currentSegment) throws TemplateModelException {
            if (currentSegment instanceof TemplateCollectionModel) {
                this.currentSegmentIterator = ((TemplateCollectionModel) currentSegment).iterator();
                this.currentSegment = null;
            } else {
                this.currentSegment = currentSegment;
                this.currentSegmentNextIndex = 0;
                this.currentSegmentIterator = null;
            }
        }

        @Override
        public TemplateModel next() throws TemplateModelException {
            ensureHasPrefetchedResult();

            if (!prefetchedHasNext) {
                throw new TemplateModelException("The collection has no more elements.");
            }

            TemplateModel result = prefetchedNext;
            hasPrefetchedResult = false; // To consume prefetched element
            prefetchedNext = null; // To not prevent GC
            return result;
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            ensureHasPrefetchedResult();
            return prefetchedHasNext;
        }

        private void ensureHasPrefetchedResult() throws TemplateModelException {
            if (hasPrefetchedResult) {
                return;
            }

            while (true) {
                // Try to fetch the next value from the current segment:
                if (currentSegmentIterator != null) {
                    boolean hasNext = currentSegmentIterator.hasNext();
                    if (hasNext) {
                        prefetchedNext = currentSegmentIterator.next();
                        prefetchedHasNext = true;
                        hasPrefetchedResult = true;
                        return;
                    }
                } else if (currentSegment != null) {
                    int size = currentSegment.size();
                    if (currentSegmentNextIndex < size) {
                        prefetchedNext = currentSegment.get(currentSegmentNextIndex++);
                        prefetchedHasNext = true;
                        hasPrefetchedResult = true;
                        return;
                    }
                } else {
                    // No current segment => We have already reached the end of the last segment in the past
                    prefetchedHasNext = false;
                    hasPrefetchedResult = true;
                    return;
                }

                // If we reach this, then the current segment was fully consumed, so we switch to the next segment.

                if (parentStep == null) {
                    setIteratorEndWasReached();
                    return;
                }

                if (!parentStep.rightVisited) {
                    parentStep.rightVisited = true;
                    descendToLeftmostSubsequence(parentStep.concSeq.right);
                } else {
                    while (true) {
                        // Ascend one level
                        parentStep = parentStep.parentStep;

                        if (parentStep == null) {
                            setIteratorEndWasReached();
                            return;
                        }

                        if (!parentStep.rightVisited) {
                            parentStep.rightVisited = true;
                            descendToLeftmostSubsequence(parentStep.concSeq.right);
                            break;
                        }
                    }
                }
            }
        }

        private void descendToLeftmostSubsequence(TemplateSequenceModel maybeConcSeq) throws TemplateModelException {
            while (maybeConcSeq instanceof ConcatenatedSequence) {
                ConcatenatedSequence concSeq = (ConcatenatedSequence) maybeConcSeq;
                parentStep = new ParentStep(concSeq, parentStep);
                maybeConcSeq = concSeq.left;
            }
            setCurrentSegment(maybeConcSeq);
        }

        private void setIteratorEndWasReached() {
            currentSegment = null;
            currentSegmentIterator = null;
            prefetchedNext = null;
            prefetchedHasNext = false;
            hasPrefetchedResult = true;
        }

        private static class ParentStep {
            private final ConcatenatedSequence concSeq;
            private final ParentStep parentStep;
            private boolean rightVisited;

            public ParentStep(ConcatenatedSequence concSeq, ParentStep parentStep) {
                this.concSeq = concSeq;
                this.parentStep = parentStep;
            }
        }
    }

    private static class ConcatenatedHash
    implements TemplateHashModel {
        protected final TemplateHashModel left;
        protected final TemplateHashModel right;

        ConcatenatedHash(TemplateHashModel left, TemplateHashModel right) {
            this.left = left;
            this.right = right;
        }
        
        @Override
        public TemplateModel get(String key)
        throws TemplateModelException {
            TemplateModel model = right.get(key);
            return (model != null) ? model : left.get(key);
        }

        @Override
        public boolean isEmpty()
        throws TemplateModelException {
            return left.isEmpty() && right.isEmpty();
        }
    }

    private static final class ConcatenatedHashEx
    extends ConcatenatedHash
    implements TemplateHashModelEx {
        private CollectionAndSequence keys;
        private CollectionAndSequence values;

        ConcatenatedHashEx(TemplateHashModelEx left, TemplateHashModelEx right) {
            super(left, right);
        }
        
        @Override
        public int size() throws TemplateModelException {
            initKeys();
            return keys.size();
        }

        @Override
        public TemplateCollectionModel keys()
        throws TemplateModelException {
            initKeys();
            return keys;
        }

        @Override
        public TemplateCollectionModel values()
        throws TemplateModelException {
            initValues();
            return values;
        }

        private void initKeys()
        throws TemplateModelException {
            if (keys == null) {
                HashSet keySet = new HashSet();
                SimpleSequence keySeq = new SimpleSequence(32, _ObjectWrappers.SAFE_OBJECT_WRAPPER);
                addKeys(keySet, keySeq, (TemplateHashModelEx) this.left);
                addKeys(keySet, keySeq, (TemplateHashModelEx) this.right);
                keys = new CollectionAndSequence(keySeq);
            }
        }

        private static void addKeys(Set keySet, SimpleSequence keySeq, TemplateHashModelEx hash)
        throws TemplateModelException {
            TemplateModelIterator it = hash.keys().iterator();
            while (it.hasNext()) {
                TemplateScalarModel tsm = (TemplateScalarModel) it.next();
                if (keySet.add(tsm.getAsString())) {
                    // The first occurrence of the key decides the index;
                    // this is consistent with the behavior of java.util.LinkedHashSet.
                    keySeq.add(tsm);
                }
            }
        }        

        private void initValues()
        throws TemplateModelException {
            if (values == null) {
                SimpleSequence seq = new SimpleSequence(size(), _ObjectWrappers.SAFE_OBJECT_WRAPPER);
                // Note: size() invokes initKeys() if needed.
            
                int ln = keys.size();
                for (int i  = 0; i < ln; i++) {
                    seq.add(get(((TemplateScalarModel) keys.get(i)).getAsString()));
                }
                values = new CollectionAndSequence(seq);
            }
        }
    }
    
}

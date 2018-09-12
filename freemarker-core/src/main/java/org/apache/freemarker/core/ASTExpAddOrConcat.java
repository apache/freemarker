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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * AST expression node: binary {@code +} operator. Note that this is treated separately from the other 4 arithmetic
 * operators, since it's overloaded to mean concatenation of string-s, sequences and hash-es too.
 */
final class ASTExpAddOrConcat extends ASTExpression {

    private final ASTExpression left;
    private final ASTExpression right;

    ASTExpAddOrConcat(ASTExpression left, ASTExpression right) {
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
            ASTNode parent,
            ASTExpression leftExp, TemplateModel leftModel,
            ASTExpression rightExp, TemplateModel rightModel)
            throws TemplateException {
        if (leftModel instanceof TemplateNumberModel && rightModel instanceof TemplateNumberModel) {
            Number first = _EvalUtils.modelToNumber((TemplateNumberModel) leftModel, leftExp);
            Number second = _EvalUtils.modelToNumber((TemplateNumberModel) rightModel, rightExp);
            return _evalOnNumbers(env, parent, first, second);
        } else if (leftModel instanceof TemplateSequenceModel && rightModel instanceof TemplateSequenceModel) {
            return new ConcatenatedSequence((TemplateSequenceModel) leftModel, (TemplateSequenceModel) rightModel);
        } else {
            boolean hashConcatPossible
                    = leftModel instanceof TemplateHashModel && rightModel instanceof TemplateHashModel;
            // We try string addition first. If hash addition is possible, then instead of throwing exception
            // we return null and do hash addition instead. (We can't simply give hash addition a priority, like
            // with sequence addition above, as FTL strings are often also FTL hashes.)
            Object leftOMOrStr = _EvalUtils.coerceModelToPlainTextOrMarkup(
                    leftModel, leftExp, /* returnNullOnNonCoercableType = */ hashConcatPossible, null,
                    env);
            if (leftOMOrStr == null) {
                return _eval_concatenateHashes(leftModel, rightModel);
            }

            // Same trick with null return as above.
            Object rightOMOrStr = _EvalUtils.coerceModelToPlainTextOrMarkup(
                    rightModel, rightExp, /* returnNullOnNonCoercableType = */ hashConcatPossible, null,
                    env);
            if (rightOMOrStr == null) {
                return _eval_concatenateHashes(leftModel, rightModel);
            }

            if (leftOMOrStr instanceof String) {
                if (rightOMOrStr instanceof String) {
                    return new SimpleString(((String) leftOMOrStr).concat((String) rightOMOrStr));
                } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                    TemplateMarkupOutputModel<?> rightMO = (TemplateMarkupOutputModel<?>) rightOMOrStr;
                    return _EvalUtils.concatMarkupOutputs(parent,
                            rightMO.getOutputFormat().fromPlainTextByEscaping((String) leftOMOrStr),
                            rightMO);
                }
            } else { // leftOMOrStr instanceof TemplateMarkupOutputModel
                TemplateMarkupOutputModel<?> leftMO = (TemplateMarkupOutputModel<?>) leftOMOrStr;
                if (rightOMOrStr instanceof String) {  // markup output
                    return _EvalUtils.concatMarkupOutputs(parent,
                            leftMO,
                            leftMO.getOutputFormat().fromPlainTextByEscaping((String) rightOMOrStr));
                } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                    return _EvalUtils.concatMarkupOutputs(parent,
                            leftMO,
                            (TemplateMarkupOutputModel<?>) rightOMOrStr);
                }
            }
        }
    }

    private static TemplateModel _eval_concatenateHashes(TemplateModel leftModel, TemplateModel rightModel)
            throws TemplateException {
        if (leftModel instanceof TemplateHashModelEx && rightModel instanceof TemplateHashModelEx) {
            TemplateHashModelEx leftModelEx = (TemplateHashModelEx) leftModel;
            TemplateHashModelEx rightModelEx = (TemplateHashModelEx) rightModel;
            if (leftModelEx.isEmptyHash()) {
                return rightModelEx;
            } else if (rightModelEx.isEmptyHash()) {
                return leftModelEx;
            } else {
                return new ConcatenatedHashEx(leftModelEx, rightModelEx);
            }
        } else {
            return new ConcatenatedHash((TemplateHashModel) leftModel,
                                        (TemplateHashModel) rightModel);
        }
    }

    static TemplateModel _evalOnNumbers(Environment env, ASTNode parent, Number first, Number second)
            throws TemplateException {
        ArithmeticEngine ae = _EvalUtils.getArithmeticEngine(env, parent);
        return new SimpleNumber(ae.add(first, second));
    }

    @Override
    boolean isLiteral() {
        return constantValue != null || (left.isLiteral() && right.isLiteral());
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpAddOrConcat(
    	left.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    public String getCanonicalForm() {
        return left.getCanonicalForm() + " + " + right.getCanonicalForm();
    }
    
    @Override
    public String getLabelWithoutParameters() {
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

    private static final class ConcatenatedSequence implements TemplateSequenceModel {
        private final TemplateSequenceModel left;
        private final TemplateSequenceModel right;

        ConcatenatedSequence(TemplateSequenceModel left, TemplateSequenceModel right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int getCollectionSize()
        throws TemplateException {
            return left.getCollectionSize() + right.getCollectionSize();
        }

        @Override
        public boolean isEmptyCollection() throws TemplateException {
            return left.isEmptyCollection() && right.isEmptyCollection();
        }

        @Override
        public TemplateModel get(int i) throws TemplateException {
            int leftSize = left.getCollectionSize();
            return i < leftSize ? left.get(i) : right.get(i - leftSize);
        }

        @Override
        public TemplateModelIterator iterator() throws TemplateException {
            return new ConcatenatedTemplateModelIterator(left.iterator(), right.iterator());
        }
    }

    private static class ConcatenatedTemplateModelIterator implements  TemplateModelIterator {
        private final TemplateModelIterator left;
        private final TemplateModelIterator right;
        private boolean leftExhausted;

        ConcatenatedTemplateModelIterator(TemplateModelIterator left, TemplateModelIterator right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public TemplateModel next() throws TemplateException {
            if (!leftExhausted) {
                if (left.hasNext()) {
                    return left.next();
                }
                leftExhausted = true;
            }
            return right.next();
        }

        @Override
        public boolean hasNext() throws TemplateException {
            if (!leftExhausted) {
                if (left.hasNext()) {
                    return true;
                }
                leftExhausted = true;
            }
            // At this point leftExhausted is true.
            return right.hasNext();
        }
    }

    private static class ConcatenatedHash implements TemplateHashModel {
        protected final TemplateHashModel left;
        protected final TemplateHashModel right;

        ConcatenatedHash(TemplateHashModel left, TemplateHashModel right) {
            this.left = left;
            this.right = right;
        }
        
        @Override
        public TemplateModel get(String key)
        throws TemplateException {
            TemplateModel model = right.get(key);
            return (model != null) ? model : left.get(key);
        }

    }

    private static final class ConcatenatedHashEx extends ConcatenatedHash implements TemplateHashModelEx {

        /** Lazily calculated list of key-value pairs; there's only one item per duplicate key. */
        private Collection<KeyValuePair> kvps;

        ConcatenatedHashEx(TemplateHashModelEx left, TemplateHashModelEx right) {
            super(left, right);
        }
        
        @Override
        public int getHashSize() throws TemplateException {
            initKvps();
            return kvps.size();
        }

        @Override
        public boolean isEmptyHash() throws TemplateException {
            return ((TemplateHashModelEx) left).isEmptyHash() && ((TemplateHashModelEx) right).isEmptyHash();
        }
        
        @Override
        public TemplateCollectionModel keys() throws TemplateException {
            initKvps();
            return new _KVPCollectionKeysModel(kvps);
        }

        @Override
        public TemplateCollectionModel values() throws TemplateException {
            initKvps();
            return new _KVPCollectionValuesModel(kvps);
        }
        
        @Override
        public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
            initKvps();
            return new _KVPCollectionKVPIterator(kvps);
        }

        /**
         * We must precreate the whole key-value pair list, as we have to deal with duplicate keys. 
         */
        private void initKvps() throws TemplateException {
            if (kvps != null) {
                return;
            }
            
            Map<Object, KeyValuePair> kvpsMap = new LinkedHashMap<>();
            putKVPs(kvpsMap, (TemplateHashModelEx) left);
            putKVPs(kvpsMap, (TemplateHashModelEx) right);
            this.kvps = kvpsMap.values();
        }

        private static void putKVPs(Map<Object, KeyValuePair> kvps, TemplateHashModelEx hash) throws TemplateException {
            for (KeyValuePairIterator iter = hash.keyValuePairIterator(); iter.hasNext(); ) {
                KeyValuePair kvp = iter.next();
                kvps.put(_EvalUtils.unwrapTemplateHashModelKey(kvp.getKey()), kvp);
            }
        }
        
    }
    
}

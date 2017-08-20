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

import java.util.HashSet;
import java.util.Set;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.CollectionAndSequence;
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
            Object leftOMOrStr = _EvalUtils.coerceModelToStringOrMarkup(
                    leftModel, leftExp, /* returnNullOnNonCoercableType = */ hashConcatPossible, null,
                    env);
            if (leftOMOrStr == null) {
                return _eval_concatenateHashes(leftModel, rightModel);
            }

            // Same trick with null return as above.
            Object rightOMOrStr = _EvalUtils.coerceModelToStringOrMarkup(
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
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
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
    String getASTNodeDescriptor() {
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

    private static final class ConcatenatedSequence
    implements
        TemplateSequenceModel {
        private final TemplateSequenceModel left;
        private final TemplateSequenceModel right;

        ConcatenatedSequence(TemplateSequenceModel left, TemplateSequenceModel right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public int size()
        throws TemplateException {
            return left.size() + right.size();
        }

        @Override
        public TemplateModel get(int i)
        throws TemplateException {
            int ls = left.size();
            return i < ls ? left.get(i) : right.get(i - ls);
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
        throws TemplateException {
            TemplateModel model = right.get(key);
            return (model != null) ? model : left.get(key);
        }

        @Override
        public boolean isEmpty()
        throws TemplateException {
            return left.isEmpty() && right.isEmpty();
        }
    }

    private static final class ConcatenatedHashEx
    extends ConcatenatedHash
    implements TemplateHashModelEx {
        private CollectionAndSequence keys;
        private CollectionAndSequence values;
        private int size;

        ConcatenatedHashEx(TemplateHashModelEx left, TemplateHashModelEx right) {
            super(left, right);
        }
        
        @Override
        public int size() throws TemplateException {
            initKeys();
            return size;
        }

        @Override
        public TemplateCollectionModel keys()
        throws TemplateException {
            initKeys();
            return keys;
        }

        @Override
        public TemplateCollectionModel values()
        throws TemplateException {
            initValues();
            return values;
        }

        private void initKeys()
        throws TemplateException {
            if (keys == null) {
                HashSet keySet = new HashSet();
                NativeSequence keySeq = new NativeSequence(32);
                addKeys(keySet, keySeq, (TemplateHashModelEx) left);
                addKeys(keySet, keySeq, (TemplateHashModelEx) right);
                size = keySet.size();
                keys = new CollectionAndSequence(keySeq);
            }
        }

        private static void addKeys(Set set, NativeSequence keySeq, TemplateHashModelEx hash)
        throws TemplateException {
            TemplateModelIterator it = hash.keys().iterator();
            while (it.hasNext()) {
                TemplateStringModel tsm = (TemplateStringModel) it.next();
                if (set.add(tsm.getAsString())) {
                    // The first occurence of the key decides the index;
                    // this is consisten with stuff like java.util.LinkedHashSet.
                    keySeq.add(tsm);
                }
            }
        }        

        private void initValues()
        throws TemplateException {
            if (values == null) {
                NativeSequence seq = new NativeSequence(size());
                // Note: size() invokes initKeys() if needed.
            
                int ln = keys.size();
                for (int i  = 0; i < ln; i++) {
                    seq.add(get(((TemplateStringModel) keys.get(i)).getAsString()));
                }
                values = new CollectionAndSequence(seq);
            }
        }
    }
    
}

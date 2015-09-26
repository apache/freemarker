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

/**
 * An operator for the + operator. Note that this is treated
 * separately from the other 4 arithmetic operators,
 * since + is overloaded to mean string concatenation.
 */
final class AddConcatExpression extends Expression {

    private final Expression left;
    private final Expression right;
    private final MarkupOutputFormat markupOutputFormat;

    AddConcatExpression(Expression left, Expression right, MarkupOutputFormat markupOutputFormat) {
        this.left = left;
        this.right = right;
        this.markupOutputFormat = markupOutputFormat;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return _eval(env, this, left, left.eval(env), right, right.eval(env), markupOutputFormat);
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
            Expression rightExp, TemplateModel rightModel,
            MarkupOutputFormat markupOutputFormat)
            throws TemplateModelException, TemplateException, NonStringException {
        if (leftModel instanceof TemplateNumberModel && rightModel instanceof TemplateNumberModel) {
            Number first = EvalUtil.modelToNumber((TemplateNumberModel) leftModel, leftExp);
            Number second = EvalUtil.modelToNumber((TemplateNumberModel) rightModel, rightExp);
            return _evalOnNumbers(env, parent, first, second);
        } else if (leftModel instanceof TemplateSequenceModel && rightModel instanceof TemplateSequenceModel) {
            return new ConcatenatedSequence((TemplateSequenceModel) leftModel, (TemplateSequenceModel) rightModel);
        } else {
            try {
                Object leftOMOrStr = EvalUtil.coerceModelToMarkupOutputOrString(
                        leftModel, leftExp, (String) null, markupOutputFormat, env);
                Object rightOMOrStr = EvalUtil.coerceModelToMarkupOutputOrString(
                        rightModel, rightExp, (String) null, markupOutputFormat, env);

                if (leftOMOrStr instanceof String) {
                    if (rightOMOrStr instanceof String) {
                        return new SimpleScalar(((String) leftOMOrStr).concat((String) rightOMOrStr));
                    } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                        TemplateMarkupOutputModel<?> rightMO = (TemplateMarkupOutputModel<?>) rightOMOrStr; 
                        return concatMarkupOutputs(parent,
                                rightMO.getOutputFormat().fromPlainTextByEscaping((String) leftOMOrStr),
                                rightMO);
                    }                    
                } else { // leftOMOrStr instanceof TemplateMarkupOutputModel 
                    TemplateMarkupOutputModel<?> leftMO = (TemplateMarkupOutputModel<?>) leftOMOrStr; 
                    if (rightOMOrStr instanceof String) {  // markup output
                        return concatMarkupOutputs(parent,
                                leftMO,
                                leftMO.getOutputFormat().fromPlainTextByEscaping((String) rightOMOrStr));
                    } else { // rightOMOrStr instanceof TemplateMarkupOutputModel
                        return concatMarkupOutputs(parent,
                                leftMO,
                                (TemplateMarkupOutputModel) rightOMOrStr);
                    }
                }
            } catch (NonStringOrTemplateOutputException e) {
                if (leftModel instanceof TemplateHashModel && rightModel instanceof TemplateHashModel) {
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
                } else {
                    throw e;
                }
            }
        }
    }

    private static TemplateModel concatMarkupOutputs(TemplateObject parent, TemplateMarkupOutputModel leftMO,
            TemplateMarkupOutputModel rightMO) throws TemplateModelException {
        MarkupOutputFormat leftOF = leftMO.getOutputFormat();
        MarkupOutputFormat rightOF = rightMO.getOutputFormat();
        if (rightOF != leftOF) {
            String rightPT;
            String leftPT;
            if ((rightPT = rightOF.getSourcePlainText(rightMO)) != null) {
                return leftOF.concat(leftMO, leftOF.fromPlainTextByEscaping(rightPT));
            } else if ((leftPT = leftOF.getSourcePlainText(leftMO)) != null) {
                return rightOF.concat(rightOF.fromPlainTextByEscaping(leftPT), rightMO);
            } else {
                Object[] message = { "Concatenation left hand operand is in ", new _DelayedToString(leftOF),
                        " format, while the right hand operand is in ", new _DelayedToString(rightOF),
                        ". Conversion to common format wasn't possible." };
                if (parent instanceof Expression) {
                    throw new _TemplateModelException((Expression) parent, message);
                } else {
                    throw new _TemplateModelException(message);
                }
            }
        } else {
            return leftOF.concat(leftMO, rightMO);
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
    	right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	markupOutputFormat);
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

    private static final class ConcatenatedSequence
    implements
        TemplateSequenceModel {
        private final TemplateSequenceModel left;
        private final TemplateSequenceModel right;

        ConcatenatedSequence(TemplateSequenceModel left, TemplateSequenceModel right) {
            this.left = left;
            this.right = right;
        }

        public int size()
        throws TemplateModelException {
            return left.size() + right.size();
        }

        public TemplateModel get(int i)
        throws TemplateModelException {
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
        
        public TemplateModel get(String key)
        throws TemplateModelException {
            TemplateModel model = right.get(key);
            return (model != null) ? model : left.get(key);
        }

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
        private int size;

        ConcatenatedHashEx(TemplateHashModelEx left, TemplateHashModelEx right) {
            super(left, right);
        }
        
        public int size() throws TemplateModelException {
            initKeys();
            return size;
        }

        public TemplateCollectionModel keys()
        throws TemplateModelException {
            initKeys();
            return keys;
        }

        public TemplateCollectionModel values()
        throws TemplateModelException {
            initValues();
            return values;
        }

        private void initKeys()
        throws TemplateModelException {
            if (keys == null) {
                HashSet keySet = new HashSet();
                SimpleSequence keySeq = new SimpleSequence(32);
                addKeys(keySet, keySeq, (TemplateHashModelEx) this.left);
                addKeys(keySet, keySeq, (TemplateHashModelEx) this.right);
                size = keySet.size();
                keys = new CollectionAndSequence(keySeq);
            }
        }

        private static void addKeys(Set set, SimpleSequence keySeq, TemplateHashModelEx hash)
        throws TemplateModelException {
            TemplateModelIterator it = hash.keys().iterator();
            while (it.hasNext()) {
                TemplateScalarModel tsm = (TemplateScalarModel) it.next();
                if (set.add(tsm.getAsString())) {
                    // The first occurence of the key decides the index;
                    // this is consisten with stuff like java.util.LinkedHashSet.
                    keySeq.add(tsm);
                }
            }
        }        

        private void initValues()
        throws TemplateModelException {
            if (values == null) {
                SimpleSequence seq = new SimpleSequence(size());
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

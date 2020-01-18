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

import java.util.Collections;
import java.util.List;

import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;

/**
 * Built-in that's similar to a Java 8 Stream intermediate operation. To be on the safe side, by default these
 * are eager, and just produce a {@link TemplateSequenceModel}. But when circumstances allow, they become lazy,
 * similarly to Java 8 Stream intermediate operations. Another characteristic of these built-ins is that they
 * usually accept lambda expressions as parameters.
 */
abstract class IntermediateStreamOperationLikeBuiltIn extends BuiltInWithParseTimeParameters {

    private Expression elementTransformerExp;
    private ElementTransformer precreatedElementTransformer;
    private boolean lazilyGeneratedResultEnabled;

    @Override
    void bindToParameters(List<Expression> parameters, Token openParen, Token closeParen) throws ParseException {
        // At the moment all built-ins of this kind requires 1 parameter.
        if (parameters.size() != 1) {
            throw newArgumentCountException("requires exactly 1", openParen, closeParen);
        }
        Expression elementTransformerExp = parameters.get(0);
        setElementTransformerExp(elementTransformerExp);
    }

    private void setElementTransformerExp(Expression elementTransformerExp) throws ParseException {
        this.elementTransformerExp = elementTransformerExp;
        if (this.elementTransformerExp instanceof LocalLambdaExpression) {
            LocalLambdaExpression localLambdaExp = (LocalLambdaExpression) this.elementTransformerExp;
            checkLocalLambdaParamCount(localLambdaExp, 1);
            // We can't do this with other kind of expressions, like a function or method reference, as they
            // need to be evaluated on runtime:
            precreatedElementTransformer = new LocalLambdaElementTransformer(localLambdaExp);
        }
    }

    @Override
    protected final boolean isLocalLambdaParameterSupported() {
        return true;
    }

    @Override
    final void enableLazilyGeneratedResult() {
        this.lazilyGeneratedResultEnabled = true;
    }

    /** Tells if {@link #enableLazilyGeneratedResult()} was called. */
    protected final boolean isLazilyGeneratedResultEnabled() {
        return lazilyGeneratedResultEnabled;
    }

    @Override
    protected void setTarget(Expression target) {
        super.setTarget(target);
        target.enableLazilyGeneratedResult();
    }

    @Override
    protected List<Expression> getArgumentsAsList() {
        return Collections.singletonList(elementTransformerExp);
    }

    @Override
    protected int getArgumentsCount() {
        return 1;
    }

    @Override
    protected Expression getArgumentParameterValue(int argIdx) {
        if (argIdx != 0) {
            throw new IndexOutOfBoundsException();
        }
        return elementTransformerExp;
    }

    protected Expression getElementTransformerExp() {
        return elementTransformerExp;
    }

    @Override
    protected void cloneArguments(
            Expression clone, String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        try {
            ((IntermediateStreamOperationLikeBuiltIn) clone).setElementTransformerExp(
                    elementTransformerExp.deepCloneWithIdentifierReplaced(
                            replacedIdentifier, replacement, replacementState));
        } catch (ParseException e) {
            throw new BugException("Deep-clone elementTransformerExp failed", e);
        }
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel targetValue = target.eval(env);

        final TemplateModelIterator targetIterator;
        final boolean targetIsSequence;
        {
            if (targetValue instanceof TemplateCollectionModel) {
                targetIterator = isLazilyGeneratedResultEnabled()
                        ? new LazyCollectionTemplateModelIterator((TemplateCollectionModel) targetValue)
                        : ((TemplateCollectionModel) targetValue).iterator();
                targetIsSequence = targetValue instanceof LazilyGeneratedCollectionModel
                        ? ((LazilyGeneratedCollectionModel) targetValue).isSequence()
                        : targetValue instanceof TemplateSequenceModel;
            } else if (targetValue instanceof TemplateSequenceModel) {
                targetIterator = new LazySequenceIterator((TemplateSequenceModel) targetValue);
                targetIsSequence = true;
            } else {
                throw new NonSequenceOrCollectionException(target, targetValue, env);
            }
        }

        return calculateResult(
                targetIterator, targetValue, targetIsSequence,
                evalElementTransformerExp(env),
                env);
    }

    private ElementTransformer evalElementTransformerExp(Environment env) throws TemplateException {
        if (precreatedElementTransformer != null) {
            return precreatedElementTransformer;
        }

        TemplateModel elementTransformerModel = elementTransformerExp.eval(env);
        if (elementTransformerModel instanceof TemplateMethodModel) {
            return new MethodElementTransformer((TemplateMethodModel) elementTransformerModel);
        } else if (elementTransformerModel instanceof Macro) {
            return new FunctionElementTransformer((Macro) elementTransformerModel, elementTransformerExp);
        } else {
            throw new NonMethodException(elementTransformerExp, elementTransformerModel, true, true, null, env);
        }
    }

    /**
     * @param lhoIterator Use this to read the elements of the left hand operand
     * @param lho Maybe needed for operations specific to the built-in, like getting the size, otherwise use the
     *           {@code lhoIterator} only.
     * @param lhoIsSequence See {@link LazilyGeneratedCollectionModel#isSequence}
     * @param elementTransformer The argument to the built-in (typically a lambda expression)
     *
     * @return {@link TemplateSequenceModel} or {@link TemplateCollectionModel} or {@link TemplateModelIterator}.
     */
    protected abstract TemplateModel calculateResult(
            TemplateModelIterator lhoIterator, TemplateModel lho, boolean lhoIsSequence,
            ElementTransformer elementTransformer,
            Environment env) throws TemplateException;

    /**
     * Wraps the built-in argument that specifies how to transform the elements of the sequence, to hide the
     * complexity of doing that.
     */
    interface ElementTransformer {
        TemplateModel transformElement(TemplateModel element, Environment env) throws TemplateException;
    }

    /** {@link ElementTransformer} that wraps a local lambda expression. */
    private static class LocalLambdaElementTransformer implements ElementTransformer {
        private final LocalLambdaExpression elementTransformerExp;

        public LocalLambdaElementTransformer(LocalLambdaExpression elementTransformerExp) {
            this.elementTransformerExp = elementTransformerExp;
        }

        @Override
        public TemplateModel transformElement(TemplateModel element, Environment env) throws TemplateException {
            return elementTransformerExp.invokeLambdaDefinedFunction(element, env);
        }
    }

    /** {@link ElementTransformer} that wraps a (Java) method call. */
    private static class MethodElementTransformer implements ElementTransformer {
        private final TemplateMethodModel elementTransformer;

        public MethodElementTransformer(TemplateMethodModel elementTransformer) {
            this.elementTransformer = elementTransformer;
        }

        @Override
        public TemplateModel transformElement(TemplateModel element, Environment env)
                throws TemplateModelException {
            Object result = elementTransformer.exec(Collections.singletonList(element));
            return result instanceof TemplateModel ? (TemplateModel) result : env.getObjectWrapper().wrap(result);
        }
    }

    /** {@link ElementTransformer} that wraps a call to an FTL function (things defined with {@code #function}). */
    private static class FunctionElementTransformer implements ElementTransformer {
        private final Macro templateTransformer;
        private final Expression elementTransformerExp;

        public FunctionElementTransformer(Macro templateTransformer, Expression elementTransformerExp) {
            this.templateTransformer = templateTransformer;
            this.elementTransformerExp = elementTransformerExp;
        }

        @Override
        public TemplateModel transformElement(TemplateModel element, Environment env) throws
                TemplateException {
            // #function-s were originally designed to be called from templates directly, so they expect an
            // Expression as argument. So we have to create a fake one.
            ExpressionWithFixedResult functionArgExp = new ExpressionWithFixedResult(
                    element, elementTransformerExp);
            return env.invokeFunction(env, templateTransformer,
                    Collections.singletonList(functionArgExp),
                    elementTransformerExp);
        }
    }

}

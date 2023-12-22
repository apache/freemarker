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

import freemarker.ext.beans.BeanModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * <b>Internal API - subject to change:</b> Represent expression nodes in the parsed template.
 * 
 * @deprecated This is an internal FreeMarker API with no backward compatibility guarantees, so you shouldn't depend on
 *             it.
 */
@Deprecated
abstract public class Expression extends TemplateObject {

    /**
     * @param env might be {@code null}, if this kind of expression can be evaluated during parsing (as opposed to
     *     during template execution).
     */
    abstract TemplateModel _eval(Environment env) throws TemplateException;
    
    abstract boolean isLiteral();

    // Used to store a constant return value for this expression. Only if it
    // is possible, of course.
    
    TemplateModel constantValue;

    // Hook in here to set the constant value if possible.
    
    @Override
    final void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine) {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        if (isLiteral()) {
            try {
                constantValue = _eval(null);
            } catch (Exception e) {
            // deliberately ignore.
            }
        }
    }

    /**
     * @deprecated At the moment FreeMarker has no API for this with backward-compatibility promises.
     */
    @Deprecated
    public final TemplateModel getAsTemplateModel(Environment env) throws TemplateException {
        return eval(env);
    }

    /**
     * Allows generating the result collection or sequence elements (or maybe others in future) on an as-needed basis,
     * similarly as Java 8 Stream intermediate operations do it. The default implementation in {@link Expression}
     * does nothing, as most expressions can't create such result. The containing expression or directive calls
     * this if it can ensure that:
     * <ul>
     *   <li>If the returned value is a {@link TemplateCollectionModel}, then it's traversed at most once, more
     *       specifically, {@link TemplateCollectionModel#iterator()} is called at most once.
     *   <li>When the methods of the collection or iterator are called, the context provided by
     *       the {@link Environment} (such as the local context stack) is similar to the context from where this
     *       expression was called. This is required as lazily generated results are allowed to be based on
     *       {@link LocalLambdaExpression}-s.
     * </ul>
     */
    void enableLazilyGeneratedResult() {
        // Has no effect by default
    }
    
    final TemplateModel eval(Environment env) throws TemplateException {
        try {
            return constantValue != null ? constantValue : _eval(env);
        } catch (FlowControlException | TemplateException e) {
            throw e;
        } catch (Exception e) {
            if (env != null && EvalUtil.shouldWrapUncheckedException(e, env)) {
                throw new _MiscTemplateException(
                        this, e, env, "Expression has thrown an unchecked exception; see the cause exception.");
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }
    
    String evalAndCoerceToPlainText(Environment env) throws TemplateException {
        return EvalUtil.coerceModelToPlainText(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    String evalAndCoerceToPlainText(Environment env, String seqTip) throws TemplateException {
        return EvalUtil.coerceModelToPlainText(eval(env), this, seqTip, env);
    }

    Object evalAndCoerceToStringOrMarkup(Environment env) throws TemplateException {
        return EvalUtil.coerceModelToStringOrMarkup(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    Object evalAndCoerceToStringOrMarkup(Environment env, String seqTip) throws TemplateException {
        return EvalUtil.coerceModelToStringOrMarkup(eval(env), this, seqTip, env);
    }
    
    String evalAndCoerceToStringOrUnsupportedMarkup(Environment env) throws TemplateException {
        return EvalUtil.coerceModelToStringOrUnsupportedMarkup(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    String evalAndCoerceToStringOrUnsupportedMarkup(Environment env, String seqTip) throws TemplateException {
        return EvalUtil.coerceModelToStringOrUnsupportedMarkup(eval(env), this, seqTip, env);
    }

    Number evalToNumber(Environment env) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToNumber(model, env);
    }

    final Number modelToNumber(TemplateModel model, Environment env) throws TemplateException {
        if (model instanceof TemplateNumberModel) {
            return EvalUtil.modelToNumber((TemplateNumberModel) model, this);
        } else {
            throw new NonNumericalException(this, model, env);
        }
    }
    
    boolean evalToBoolean(Environment env) throws TemplateException {
        return evalToBoolean(env, null);
    }

    boolean evalToBoolean(Configuration cfg) throws TemplateException {
        return evalToBoolean(null, cfg);
    }

    final TemplateModel evalToNonMissing(Environment env) throws TemplateException {
        TemplateModel result = this.eval(env);
        assertNonNull(result, env);
        return result;
    }
    
    private boolean evalToBoolean(Environment env, Configuration cfg) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToBoolean(model, env, cfg);
    }
    
    final boolean modelToBoolean(TemplateModel model, Environment env) throws TemplateException {
        return modelToBoolean(model, env, null);
    }

    final boolean modelToBoolean(TemplateModel model, Configuration cfg) throws TemplateException {
        return modelToBoolean(model, null, cfg);
    }
    
    private boolean modelToBoolean(TemplateModel model, Environment env, Configuration cfg) throws TemplateException {
        if (model instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) model).getAsBoolean();
        } else if (env != null ? env.isClassicCompatible() : cfg.isClassicCompatible()) {
            return model != null && !isEmpty(model);
        } else {
            throw new NonBooleanException(this, model, env);
        }
    }
    
    final Expression deepCloneWithIdentifierReplaced(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        Expression clone = deepCloneWithIdentifierReplaced_inner(replacedIdentifier, replacement, replacementState);
        if (clone.beginLine == 0) {
            clone.copyLocationFrom(this);
        }
        return clone;
    }
    
    static class ReplacemenetState {
        /**
         * If the replacement expression is not in use yet, we don't have to clone it.
         */
        boolean replacementAlreadyInUse; 
    }

    /**
     * This should return an equivalent new expression object (or an identifier replacement expression).
     * The position need not be filled, unless it will be different from the position of what we were cloning. 
     */
    protected abstract Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState);

    static boolean isEmpty(TemplateModel model) throws TemplateModelException {
        if (model instanceof BeanModel) {
            return ((BeanModel) model).isEmpty();
        } else if (model instanceof TemplateSequenceModel) {
            return ((TemplateSequenceModel) model).size() == 0;
        } else if (model instanceof TemplateScalarModel) {
            String s = ((TemplateScalarModel) model).getAsString();
            return (s == null || s.length() == 0);
        } else if (model == null) {
            return true;
        } else if (model instanceof TemplateMarkupOutputModel) { // Note: happens just after FTL string check
            TemplateMarkupOutputModel mo = (TemplateMarkupOutputModel) model;
            return mo.getOutputFormat().isEmpty(mo);
        } else if (model instanceof TemplateCollectionModel) {
            return !((TemplateCollectionModel) model).iterator().hasNext();
        } else if (model instanceof TemplateHashModel) {
            return ((TemplateHashModel) model).isEmpty();
        } else if (model instanceof TemplateNumberModel
                || model instanceof TemplateDateModel
                || model instanceof TemplateBooleanModel) {
            return false;
        } else {
            return true;
        }
    }
    
    final void assertNonNull(TemplateModel model, Environment env) throws InvalidReferenceException {
        if (model == null) throw InvalidReferenceException.getInstance(this, env);
    }
    
}

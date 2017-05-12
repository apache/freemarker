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

import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.impl.BeanModel;

/**
 * AST expression node superclass
 */
abstract class ASTExpression extends ASTNode {

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
    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine) {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        if (isLiteral()) {
            try {
                constantValue = _eval(null);
            } catch (Exception e) {
            // deliberately ignore.
            }
        }
    }

    final TemplateModel getAsTemplateModel(Environment env) throws TemplateException {
        return eval(env);
    }
    
    final TemplateModel eval(Environment env) throws TemplateException {
        return constantValue != null ? constantValue : _eval(env);
    }
    
    String evalAndCoerceToPlainText(Environment env) throws TemplateException {
        return _EvalUtil.coerceModelToPlainText(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    String evalAndCoerceToPlainText(Environment env, String seqTip) throws TemplateException {
        return _EvalUtil.coerceModelToPlainText(eval(env), this, seqTip, env);
    }

    Object evalAndCoerceToStringOrMarkup(Environment env) throws TemplateException {
        return _EvalUtil.coerceModelToStringOrMarkup(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    Object evalAndCoerceToStringOrMarkup(Environment env, String seqTip) throws TemplateException {
        return _EvalUtil.coerceModelToStringOrMarkup(eval(env), this, seqTip, env);
    }
    
    String evalAndCoerceToStringOrUnsupportedMarkup(Environment env) throws TemplateException {
        return _EvalUtil.coerceModelToStringOrUnsupportedMarkup(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    String evalAndCoerceToStringOrUnsupportedMarkup(Environment env, String seqTip) throws TemplateException {
        return _EvalUtil.coerceModelToStringOrUnsupportedMarkup(eval(env), this, seqTip, env);
    }
    
    Number evalToNumber(Environment env) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToNumber(model, env);
    }

    Number modelToNumber(TemplateModel model, Environment env) throws TemplateException {
        if (model instanceof TemplateNumberModel) {
            return _EvalUtil.modelToNumber((TemplateNumberModel) model, this);
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

    TemplateModel evalToNonMissing(Environment env) throws TemplateException {
        TemplateModel result = eval(env);
        assertNonNull(result, env);
        return result;
    }
    
    private boolean evalToBoolean(Environment env, Configuration cfg) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToBoolean(model, env, cfg);
    }
    
    boolean modelToBoolean(TemplateModel model, Environment env) throws TemplateException {
        return modelToBoolean(model, env, null);
    }

    boolean modelToBoolean(TemplateModel model, Configuration cfg) throws TemplateException {
        return modelToBoolean(model, null, cfg);
    }
    
    private boolean modelToBoolean(TemplateModel model, Environment env, Configuration cfg) throws TemplateException {
        if (model instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) model).getAsBoolean();
        } else {
            throw new NonBooleanException(this, model, env);
        }
    }
    
    final ASTExpression deepCloneWithIdentifierReplaced(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        ASTExpression clone = deepCloneWithIdentifierReplaced_inner(replacedIdentifier, replacement, replacementState);
        if (clone.beginLine == 0) {
            clone.copyLocationFrom(this);
        }
        return clone;
    }
    
    static class ReplacemenetState {
        /**
         * If the replacement expression is not in use yet, we don't have to deepClone it.
         */
        boolean replacementAlreadyInUse; 
    }

    /**
     * This should return an equivalent new expression object (or an identifier replacement expression).
     * The position need not be filled, unless it will be different from the position of what we were cloning. 
     */
    protected abstract ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState);

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
    
    void assertNonNull(TemplateModel model, Environment env) throws InvalidReferenceException {
        if (model == null) throw InvalidReferenceException.getInstance(this, env);
    }
    
}

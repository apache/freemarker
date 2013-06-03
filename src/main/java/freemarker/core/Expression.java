/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.core;

import freemarker.template.*;
import freemarker.template.utility.ClassUtil;
import freemarker.ext.beans.BeanModel;

/**
 * An abstract class for nodes in the parse tree 
 * that represent a FreeMarker expression.
 */
abstract public class Expression extends TemplateObject {

    abstract TemplateModel _getAsTemplateModel(Environment env) throws TemplateException;
    abstract boolean isLiteral();

    // Used to store a constant return value for this expression. Only if it
    // is possible, of course.
    
    TemplateModel constantValue;

    // Hook in here to set the constant value if possible.
    
    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine)
    throws
        ParseException
    {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        if (isLiteral()) {
            try {
                constantValue = _getAsTemplateModel(null);
            } catch (Exception e) {
            // deliberately ignore.
            }
        }
    }
    
    public final TemplateModel getAsTemplateModel(Environment env) throws TemplateException {
        return constantValue != null ? constantValue : _getAsTemplateModel(env);
    }
    
    String getStringValue(Environment env) throws TemplateException {
        return getStringValue(getAsTemplateModel(env), this, env);
    }
    
    static String getStringValue(TemplateModel referentModel, Expression exp, Environment env)
    throws
        TemplateException
    {
        if (referentModel instanceof TemplateNumberModel) {
            return env.formatNumber(EvaluationUtil.getNumber((TemplateNumberModel) referentModel, exp, env));
        } else if (referentModel instanceof TemplateDateModel) {
            TemplateDateModel dm = (TemplateDateModel) referentModel;
            return env.formatDate(EvaluationUtil.getDate(dm, exp, env), dm.getDateType());
        } else if (referentModel instanceof TemplateScalarModel) {
            return EvaluationUtil.getString((TemplateScalarModel) referentModel, exp, env);
        } else if(referentModel == null) {
            if (env.isClassicCompatible()) {
                return "";
            } else {
                throw exp.invalidReferenceException(env);
            }
        } else if (referentModel instanceof TemplateBooleanModel) {
            // This should be before TemplateScalarModel, but automatic boolean-to-string is only non-error since 2.3.20
            // (and before that when classic_compatible was true), so to keep backward compatibility we couldn't insert
            // this before TemplateScalarModel.
            boolean booleanValue = ((TemplateBooleanModel) referentModel).getAsBoolean(); 
            if (env.isClassicCompatible()) {
                return booleanValue ? "true" : "";
            } else {
                return env.formatBoolean(booleanValue);
            }
        } else {
            throw new NonStringException(
                    "Error " + exp.getStartLocation()
                    +"\nExpected a " + MessageUtil.TYPES_AUTOMATICALLY_CONVERTIBLE_TO_STRING
                    + ", but this evaluated to a value of type "
                    + ClassUtil.getFTLTypeDescription(referentModel) + ":\n"
                    + exp,
                    env);
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
     * The position need not be filled, unless it will be different from the position of what were cloning. 
     */
    protected abstract Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState);

    boolean isTrue(Environment env) throws TemplateException {
        TemplateModel referent = getAsTemplateModel(env);
        if (referent instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) referent).getAsBoolean();
        }
        if (env.isClassicCompatible()) {
            return referent != null && !isEmpty(referent);
        }
        assertNonNull(referent, env);
        String msg = "Error " + getStartLocation()
                     + "\nExpecting a boolean (true/false) expression here"
                     + "\nExpression " + this + " does not evaluate to true/false "
                     + "\nit is an instance of " + referent.getClass().getName();
        throw new NonBooleanException(msg, env);
    }

    static boolean isEmpty(TemplateModel model) throws TemplateModelException
    {
        if (model instanceof BeanModel) {
            return ((BeanModel) model).isEmpty();
        } else if (model instanceof TemplateSequenceModel) {
            return ((TemplateSequenceModel) model).size() == 0;
        } else if (model instanceof TemplateScalarModel) {
            String s = ((TemplateScalarModel) model).getAsString();
            return (s == null || s.length() == 0);
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
}

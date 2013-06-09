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

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.Internal_BeansAPI;
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
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * An abstract class for nodes in the parse tree 
 * that represent a FreeMarker expression.
 */
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
    
    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine)
    throws
        ParseException
    {
        super.setLocation(template, beginColumn, beginLine, endColumn, endLine);
        if (isLiteral()) {
            try {
                constantValue = _eval(null);
            } catch (Exception e) {
            // deliberately ignore.
            }
        }
    }
    
    public final TemplateModel eval(Environment env) throws TemplateException {
        return constantValue != null ? constantValue : _eval(env);
    }
    
    String evalAndCoerceToString(Environment env) throws TemplateException {
        return coerceModelToString(eval(env), this, null, env);
    }

    /**
     * @param seqTip Tip to display if the value type is not coercable, but it's sequence or collection.
     */
    String evalAndCoerceToString(Environment env, String seqTip) throws TemplateException {
        return coerceModelToString(eval(env), this, seqTip, env);
    }
    
    static String coerceModelToString(TemplateModel tm, Expression exp, Environment env) throws TemplateException {
        return coerceModelToString(tm, exp, null, env);
    }
    
    static String coerceModelToString(TemplateModel tm, Expression exp, String seqHint, Environment env) throws TemplateException {
        if (tm instanceof TemplateNumberModel) {
            return env.formatNumber(EvalUtil.modelToNumber((TemplateNumberModel) tm, exp, env));
        } else if (tm instanceof TemplateDateModel) {
            TemplateDateModel dm = (TemplateDateModel) tm;
            return env.formatDate(EvalUtil.modelToDate(dm, exp, env), dm.getDateType());
        } else if (tm instanceof TemplateScalarModel) {
            return EvalUtil.modelToString((TemplateScalarModel) tm, exp, env);
        } else if(tm == null) {
            if (env.isClassicCompatible()) {
                return "";
            } else {
                throw exp.newInvalidReferenceException(env);
            }
        } else if (tm instanceof TemplateBooleanModel) {
            // This should be before TemplateScalarModel, but automatic boolean-to-string is only non-error since 2.3.20
            // (and before that when classic_compatible was true), so to keep backward compatibility we couldn't insert
            // this before TemplateScalarModel.
            boolean booleanValue = ((TemplateBooleanModel) tm).getAsBoolean(); 
            if (env.getClassicCompatibleAsInt() == 1) {
                return booleanValue ? "true" : "";
            } else {
                return env.formatBoolean(booleanValue);
            }
        } else {
            if (env.isClassicCompatible() && tm instanceof BeanModel) {
                return Internal_BeansAPI.getAsClassicCompatibleString((BeanModel) tm);
            } if (seqHint != null && (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)) {
                throw exp.newNonStringException(tm, seqHint, env);
            } else {
                throw exp.newNonStringException(tm, env);
            }
        }
    }
    
    Number evalToNumber(Environment env) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToNumber(model, env);
    }

    Number modelToNumber(TemplateModel model, Environment env) throws TemplateException {
        if(model instanceof TemplateNumberModel) {
            return EvalUtil.modelToNumber((TemplateNumberModel) model, this, env);
        } else {
            throw newNonNumericalException(model, env);
        }
    }
    
    boolean evalToBoolean(Environment env) throws TemplateException {
        TemplateModel model = eval(env);
        return modelToBoolean(model, env);
    }

    boolean modelToBoolean(TemplateModel model, Environment env) throws TemplateException {
        if (model instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) model).getAsBoolean();
        } else if (env.isClassicCompatible()) {
            return model != null && !isEmpty(model);
        } else {
            throw newNonBooleanException(model, env);
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

    static boolean isEmpty(TemplateModel model) throws TemplateModelException
    {
        if (model instanceof BeanModel) {
            return ((BeanModel) model).isEmpty();
        } else if (model instanceof TemplateSequenceModel) {
            return ((TemplateSequenceModel) model).size() == 0;
        } else if (model instanceof TemplateScalarModel) {
            String s = ((TemplateScalarModel) model).getAsString();
            return (s == null || s.length() == 0);
        } else if (model == null) {
            return true;
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
        if (model == null) {
            throw newInvalidReferenceException(env);
        }
    }

    TemplateException newTemplateException(Exception cause) {
        return newTemplateException("Unexpected error: " + cause, (String) null, cause);
    }
    
    TemplateException newTemplateException(String description) {
        return newTemplateException(description, (String) null, null);
    }

    TemplateException newTemplateException(String description, String[] tips) {
        return newTemplateException(description, tips, (Exception) null);
    }
    
    TemplateException newTemplateException(String description, Exception cause) {
        return newTemplateException(description, (String) null, cause);
    }
    
    TemplateException newTemplateException(String description, String tip, Exception cause) {
        return new TemplateException(
                MessageUtil.decorateErrorDescription(description, this, tip),
                cause,
                Environment.getCurrentEnvironment());
    }

    TemplateException newTemplateException(String description, String[] tips, Exception cause) {
        return new TemplateException(
                MessageUtil.decorateErrorDescription(description, this, tips),
                cause,
                Environment.getCurrentEnvironment());
    }
    
    TemplateModelException newTemplateModelException(String description) {
        return newTemplateModelException(description, (String) null, null);
    }
    
    TemplateModelException newTemplateModelException(String description, String[] tips) {
        return newTemplateModelException(description, tips, null);
    }
    
    TemplateModelException newTemplateModelException(String description, Exception cause) {
        return newTemplateModelException(description, (String) null, cause);
    }
    
    // TODO: The newTemplateModelException-s shouldn't exist at all. TemplateModelException-s should be blamed on
    // the expression who has called the failing TM method. Wherever we use these, we blame the wrong expression.
    TemplateModelException newTemplateModelException(String description, String tip, Exception cause) {
        return new TemplateModelException(
                MessageUtil.decorateErrorDescription(description, this, tip),
                cause);
    }

    TemplateModelException newTemplateModelException(String description, String[] tips, Exception cause) {
        return new TemplateModelException(
                MessageUtil.decorateErrorDescription(description, this, tips),
                cause);
    }
    
    InvalidReferenceException newInvalidReferenceException(Environment env) {
        if (env != null && env.getFastInvalidReferenceExceptions()) {
            return InvalidReferenceException.FAST_INSTANCE;
        } else {
            return new InvalidReferenceException(
                    MessageUtil.decorateErrorDescription(
                            "The following has evaluated to null or missing:",
                            this,
                            "If the failing expression is known to be legally null/missing, either specify a default value"
                            + " with myOptionalVar!myDefault, or use "
                            + StringUtil.encloseAsTag(this.getTemplate(), "#if myOptionalVar??") + "when-present"
                            + StringUtil.encloseAsTag(this.getTemplate(), "#else") + "when-missing"
                            + StringUtil.encloseAsTag(this.getTemplate(), "/#if") + "."),
                        env);
        }
    }
    
    UnexpectedTypeException newUnexpectedTypeException(TemplateModel model, String expected, Environment env)
    throws TemplateException {
        return newUnexpectedTypeException(model, expected, null, env);
    }
    
    UnexpectedTypeException newUnexpectedTypeException(
            TemplateModel model, String expected, String tip, Environment env)
    throws InvalidReferenceException
    {
        assertNonNull(model, env);
        return new UnexpectedTypeException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription(expected, model),
                        this,
                        tip),
                env);
    }
    
    NonNumericalException newNonNumericalException(TemplateModel model, Environment env)
    throws InvalidReferenceException {
        return newNonNumericalException(model, null, env);
    }
    
    NonNumericalException newNonNumericalException(TemplateModel model, String tip, Environment env)
    throws InvalidReferenceException
    {
        assertNonNull(model, env);
        return new NonNumericalException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription("number", model),
                        this,
                        tip),
                env);
    }

    NonNumericalException newMalformedNumberException(String text) {
        return new NonNumericalException(
                MessageUtil.decorateErrorDescription(
                        "Can't convert this string to number: " + StringUtil.jQuote(text),
                        this),
                Environment.getCurrentEnvironment());
    }
    
    NonStringException newNonStringException(TemplateModel model, Environment env)
    throws InvalidReferenceException {
        assertNonNull(model, env);
        return new NonStringException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription(MessageUtil.TYPES_USABLE_WHERE_STRING_IS_EXPECTED, model),
                        this),
                env);
    }

    NonStringException newNonStringException(TemplateModel model, String tip, Environment env)
    throws InvalidReferenceException {
        assertNonNull(model, env);
        return new NonStringException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription(MessageUtil.TYPES_USABLE_WHERE_STRING_IS_EXPECTED, model),
                        this,
                        tip),
                env);
    }
    
    NonDateException newNonDateException(TemplateModel model, Environment env)
    throws InvalidReferenceException {
        assertNonNull(model, env);
        return new NonDateException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription("date", model),
                        this),
                env);
    }

    NonBooleanException newNonBooleanException(TemplateModel model, Environment env)
    throws InvalidReferenceException {
        assertNonNull(model, env);
        return new NonBooleanException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription("boolean", model),
                        this),
                env);
    }

    NonBooleanException newNonBooleanException(String actualType)
    throws InvalidReferenceException {
        return new NonBooleanException(
                MessageUtil.decorateErrorDescription(
                        unexpectedTypeErrorDescription("boolean", actualType),
                        this),
                Environment.getCurrentEnvironment());
    }
    
    private static String unexpectedTypeErrorDescription(String expectedType, TemplateModel model) {
        return unexpectedTypeErrorDescription(expectedType, ClassUtil.getFTLTypeDescription(model));
    }

    private static String unexpectedTypeErrorDescription(String expectedType, String actualType) {
        return "Expected a(n) " + expectedType + ", but this evaluated to a value of type " 
                + actualType + ":";
    }
    
}

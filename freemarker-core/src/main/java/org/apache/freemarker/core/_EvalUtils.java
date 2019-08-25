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

import static org.apache.freemarker.core.MessageUtils.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.apache.freemarker.core.arithmetic.ArithmeticEngine;
import org.apache.freemarker.core.arithmetic.impl.BigDecimalArithmeticEngine;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNullModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._ClassUtils;
import org.apache.freemarker.core.valueformat.TemplateDateFormat;
import org.apache.freemarker.core.valueformat.TemplateNumberFormat;
import org.apache.freemarker.core.valueformat.TemplateValueFormat;
import org.apache.freemarker.core.valueformat.TemplateValueFormatException;

/**
 * Internally used static utilities for evaluation expressions.
 */
public class _EvalUtils {
    static final int CMP_OP_EQUALS = 1;
    static final int CMP_OP_NOT_EQUALS = 2;
    static final int CMP_OP_LESS_THAN = 3;
    static final int CMP_OP_GREATER_THAN = 4;
    static final int CMP_OP_LESS_THAN_EQUALS = 5;
    static final int CMP_OP_GREATER_THAN_EQUALS = 6;
    // If you add a new operator here, update the "compare" and "cmpOpToString" methods!
    
    // Prevents instantiation.
    private _EvalUtils() { }
    
    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     */
    public static String modelToString(TemplateStringModel model, ASTExpression expr)
    throws TemplateException {
        String value = model.getAsString();
        if (value == null) {
            throw newModelHasStoredNullException(String.class, model, expr);
        }
        return value;
    }
    
    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     */
    public static Number modelToNumber(TemplateNumberModel model, ASTExpression expr)
        throws TemplateException {
        Number value = model.getAsNumber();
        if (value == null) throw newModelHasStoredNullException(Number.class, model, expr);
        return value;
    }

    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     */
    public static Date modelToDate(TemplateDateModel model, ASTExpression expr)
        throws TemplateException {
        Date value = model.getAsDate();
        if (value == null) throw newModelHasStoredNullException(Date.class, model, expr);
        return value;
    }
    
    /** Signals the buggy case where we have a non-null model, but it wraps a null. */
    public static TemplateException newModelHasStoredNullException(
            Class<?> expected, TemplateModel model, ASTExpression expr) {
        return new TemplateException(expr,
                "The FreeMarker value exists, but has nothing inside it; the TemplateModel object (class: ",
                model.getClass().getName(), ") has returned a null",
                (expected != null ? new Object[] { " instead of a ", _ClassUtils.getShortClassName(expected) } : ""),
                ". This is possibly a bug in the non-FreeMarker code that builds the data-model.");
    }

    /**
     * Compares two expressions according the rules of the FTL comparator operators.
     * 
     * @param leftExp not {@code null}
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param operatorString can be null {@code null}; the actual operator used, used for more accurate error message.
     * @param rightExp not {@code null}
     * @param env {@code null} is tolerated, but should be avoided
     */
    static boolean compare(
            ASTExpression leftExp,
            int operator, String  operatorString,
            ASTExpression rightExp,
            ASTExpression defaultBlamed,
            Environment env) throws TemplateException {
        TemplateModel ltm = leftExp.eval(env);
        TemplateModel rtm = rightExp.eval(env);
        return compare(
                ltm, leftExp,
                operator, operatorString,
                rtm, rightExp,
                defaultBlamed, false,
                false, false, false,
                env);
    }
    
    /**
     * Compares values according the rules of the FTL comparator operators; if the {@link ASTExpression}-s are
     * accessible, use {@link #compare(ASTExpression, int, String, ASTExpression, ASTExpression, Environment)} instead,
     * as that gives better error messages.
     * 
     * @param leftValue maybe {@code null}, which will usually cause the appropriate {@link TemplateException}. 
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param rightValue maybe {@code null}, which will usually cause the appropriate {@link TemplateException}.
     * @param env {@code null} is tolerated, but should be avoided
     */
    static boolean compare(
            TemplateModel leftValue, int operator, TemplateModel rightValue,
            Environment env) throws TemplateException {
        return compare(
                leftValue, null,
                operator, null,
                rightValue, null,
                null, false,
                false, false, false,
                env);
    }

    /**
     * Same as {@link #compare(TemplateModel, int, TemplateModel, Environment)}, but if the two types are incompatible,
     *     they are treated as non-equal instead of throwing an exception. Comparing dates of different types will
     *     still throw an exception, however.
     */
    static boolean compareLenient(
            TemplateModel leftValue, int operator, TemplateModel rightValue,
            Environment env) throws TemplateException {
        return compare(
                leftValue, null,
                operator, null,
                rightValue, null,
                null, false,
                true, false, false,
                env);
    }
    
    private static final String VALUE_OF_THE_COMPARISON_IS_UNKNOWN_DATE_LIKE
            = "value of the comparison is a date-like value where "
              + "it's not known if it's a date (no time part), time, or date-time, "
              + "and thus can't be used in a comparison.";
    
    /**
     * @param leftExp {@code null} is allowed, but may results in less helpful error messages
     * @param operator one of the {@code COMP_OP_...} constants, like {@link #CMP_OP_EQUALS}.
     * @param operatorString can be null {@code null}; the actual operator used, used for more accurate error message.
     * @param rightExp {@code null} is allowed, but may results in less helpful error messages
     * @param defaultBlamed {@code null} allowed; the expression to which the error will point to if something goes
     *        wrong that is not specific to the left or right side expression, or if that expression is {@code null}.
     * @param typeMismatchMeansNotEqual If the two types are incompatible, they are treated as non-equal instead
     *     of throwing an exception. Comparing dates of different types will still throw an exception, however. 
     * @param leftNullReturnsFalse if {@code true}, a {@code null} left value will not cause exception, but make the
     *     expression {@code false}.  
     * @param rightNullReturnsFalse if {@code true}, a {@code null} right value will not cause exception, but make the
     *     expression {@code false}.  
     */
    static boolean compare(
            TemplateModel leftValue, ASTExpression leftExp,
            int operator, String operatorString,
            TemplateModel rightValue, ASTExpression rightExp,
            ASTExpression defaultBlamed, boolean quoteOperandsInErrors,
            boolean typeMismatchMeansNotEqual,
            boolean leftNullReturnsFalse, boolean rightNullReturnsFalse,
            Environment env) throws TemplateException {
        if (leftValue == null) {
            if (leftNullReturnsFalse) { 
                return false;
            } else {
                if (leftExp != null) {
                    throw InvalidReferenceException.getInstance(leftExp, env);
                } else {
                    throw new TemplateException(defaultBlamed, env,
                                "The left operand of the comparison was undefined or null.");
                }
            }
        }

        if (rightValue == null) {
            if (rightNullReturnsFalse) { 
                return false;
            } else {
                if (rightExp != null) {
                    throw InvalidReferenceException.getInstance(rightExp, env);
                } else {
                    throw new TemplateException(defaultBlamed, env,
                                "The right operand of the comparison was undefined or null.");
                }
            }
        }

        final int cmpResult;
        if (leftValue instanceof TemplateNumberModel && rightValue instanceof TemplateNumberModel) {
            Number leftNum = _EvalUtils.modelToNumber((TemplateNumberModel) leftValue, leftExp);
            Number rightNum = _EvalUtils.modelToNumber((TemplateNumberModel) rightValue, rightExp);
            ArithmeticEngine ae =
                    env != null
                        ? env.getArithmeticEngine()
                        : (leftExp != null
                            ? leftExp.getTemplate().getArithmeticEngine()
                            : BigDecimalArithmeticEngine.INSTANCE);
            try {
                cmpResult = ae.compareNumbers(leftNum, rightNum);
            } catch (RuntimeException e) {
                throw new TemplateException(defaultBlamed, e, env,
                        "Unexpected error while comparing two numbers: ", e);
            }
        } else if (leftValue instanceof TemplateDateModel && rightValue instanceof TemplateDateModel) {
            TemplateDateModel leftDateModel = (TemplateDateModel) leftValue;
            TemplateDateModel rightDateModel = (TemplateDateModel) rightValue;
            
            int leftDateType = leftDateModel.getDateType();
            int rightDateType = rightDateModel.getDateType();
            
            if (leftDateType == TemplateDateModel.UNKNOWN || rightDateType == TemplateDateModel.UNKNOWN) {
                String sideName;
                ASTExpression sideExp;
                if (leftDateType == TemplateDateModel.UNKNOWN) {
                    sideName = "left";
                    sideExp = leftExp;
                } else {
                    sideName = "right";
                    sideExp = rightExp;
                }
                
                throw new TemplateException(sideExp != null ? sideExp : defaultBlamed, env,
                        "The ", sideName, " ", VALUE_OF_THE_COMPARISON_IS_UNKNOWN_DATE_LIKE);
            }
            
            if (leftDateType != rightDateType) {
                throw new TemplateException(defaultBlamed, env,
                        "Can't compare dates of different types. Left date type is ",
                        TemplateDateModel.TYPE_NAMES.get(leftDateType), ", right date type is ",
                        TemplateDateModel.TYPE_NAMES.get(rightDateType), ".");
            }

            Date leftDate = _EvalUtils.modelToDate(leftDateModel, leftExp);
            Date rightDate = _EvalUtils.modelToDate(rightDateModel, rightExp);
            cmpResult = leftDate.compareTo(rightDate);
        } else if (leftValue instanceof TemplateStringModel && rightValue instanceof TemplateStringModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                throw new TemplateException(defaultBlamed, env,
                        "Can't use operator \"", cmpOpToString(operator, operatorString), "\" on string values.");
            }
            String leftString = _EvalUtils.modelToString((TemplateStringModel) leftValue, leftExp);
            String rightString = _EvalUtils.modelToString((TemplateStringModel) rightValue, rightExp);
            // FIXME NBC: Don't use the Collator here. That's locale-specific, but ==/!= should not be.
            cmpResult = env.getCollator().compare(leftString, rightString);
        } else if (leftValue instanceof TemplateBooleanModel && rightValue instanceof TemplateBooleanModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                throw new TemplateException(defaultBlamed, env,
                        "Can't use operator \"", cmpOpToString(operator, operatorString), "\" on boolean values.");
            }
            boolean leftBool = ((TemplateBooleanModel) leftValue).getAsBoolean();
            boolean rightBool = ((TemplateBooleanModel) rightValue).getAsBoolean();
            cmpResult = (leftBool ? 1 : 0) - (rightBool ? 1 : 0);
        } else {
            if (typeMismatchMeansNotEqual) {
                if (operator == CMP_OP_EQUALS) {
                    return false;
                } else if (operator == CMP_OP_NOT_EQUALS) {
                    return true;
                }
                // Falls through
            }
            throw new TemplateException(defaultBlamed, env,
                    "Can't compare values of these types. ",
                    "Allowed comparisons are between two numbers, two strings, two dates, or two booleans.\n",
                    "Left hand operand ",
                    (quoteOperandsInErrors && leftExp != null
                            ? new Object[] { "(", new _DelayedGetCanonicalForm(leftExp), ") value " }
                            : ""),
                    "is ", new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(leftValue)), ".\n",
                    "Right hand operand ",
                    (quoteOperandsInErrors && rightExp != null
                            ? new Object[] { "(", new _DelayedGetCanonicalForm(rightExp), ") value " }
                            : ""),
                    "is ", new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(rightValue)),
                    ".");
        }

        switch (operator) {
            case CMP_OP_EQUALS: return cmpResult == 0;
            case CMP_OP_NOT_EQUALS: return cmpResult != 0;
            case CMP_OP_LESS_THAN: return cmpResult < 0;
            case CMP_OP_GREATER_THAN: return cmpResult > 0;
            case CMP_OP_LESS_THAN_EQUALS: return cmpResult <= 0;
            case CMP_OP_GREATER_THAN_EQUALS: return cmpResult >= 0;
            default: throw new BugException("Unsupported comparator operator code: " + operator);
        }
    }

    private static String cmpOpToString(int operator, String operatorString) {
        if (operatorString != null) {
            return operatorString;
        } else {
            switch (operator) {
                case CMP_OP_EQUALS: return "equals";
                case CMP_OP_NOT_EQUALS: return "not-equals";
                case CMP_OP_LESS_THAN: return "less-than";
                case CMP_OP_GREATER_THAN: return "greater-than";
                case CMP_OP_LESS_THAN_EQUALS: return "less-than-equals";
                case CMP_OP_GREATER_THAN_EQUALS: return "greater-than-equals";
                default: return "???";
            }
        }
    }

    /**
     * Converts a value to plain text {@link String}, or a {@link TemplateMarkupOutputModel} if that's what the
     * {@link TemplateValueFormat} involved produces.
     * 
     * @param seqTip
     *            Tip to display if the value type is not coercable, but it's iterable.
     * @param exp
     *            The expression that was evaluated to {@code tm}. This can be {@code null}, however that may results
     *            in poor quality error messages.
     * 
     * @return Never {@code null}
     */
    static Object coerceModelToPlainTextOrMarkup(TemplateModel tm, ASTExpression exp, String seqTip, Environment env)
            throws TemplateException {
        return coerceModelToPlainTextOrMarkup(tm, exp, false, seqTip, env);
    }
    
    /**
     * @return {@code null} if the {@code returnNullOnNonCoercableType} parameter is {@code true}, and the coercion is
     *         not possible, because of the type is not right for it.
     * 
     * @see #coerceModelToPlainTextOrMarkup(TemplateModel, ASTExpression, String, Environment)
     */
    static Object coerceModelToPlainTextOrMarkup(
            TemplateModel tm, ASTExpression exp, boolean returnNullOnNonCoercableType, String seqTip, Environment env)
            throws TemplateException {
        if (tm instanceof TemplateNumberModel) {
            TemplateNumberModel tnm = (TemplateNumberModel) tm; 
            TemplateNumberFormat format = env.getTemplateNumberFormat(exp);
            try {
                return assertFormatResultNotNull(format.format(tnm));
            } catch (TemplateValueFormatException e) {
                throw MessageUtils.newCantFormatNumberException(format, exp, e);
            }
        } else if (tm instanceof TemplateDateModel) {
            TemplateDateModel tdm = (TemplateDateModel) tm;
            TemplateDateFormat format = env.getTemplateDateFormat(tdm, exp);
            try {
                return assertFormatResultNotNull(format.format(tdm));
            } catch (TemplateValueFormatException e) {
                throw MessageUtils.newCantFormatDateException(format, exp, e);
            }
        } else if (tm instanceof TemplateMarkupOutputModel) {
            return tm;
        } else { 
            return coerceModelToTextualCommon(tm, exp, seqTip, true, returnNullOnNonCoercableType, env);
        }
    }

    /**
     * Like {@link #coerceModelToPlainTextOrMarkup(TemplateModel, ASTExpression, String, Environment)}, but gives error
     * if the result is markup. This is what you normally use where markup results can't be used.
     *
     * @param seqTip
     *            Tip to display if the value type is not coercable, but it's iterable.
     * 
     * @return Never {@code null}
     */
    static String coerceModelToPlainTextOrUnsupportedMarkup(
            TemplateModel tm, ASTExpression exp, String seqTip, Environment env)
            throws TemplateException {
        if (tm instanceof TemplateNumberModel) {
            TemplateNumberModel tnm = (TemplateNumberModel) tm; 
            TemplateNumberFormat format = env.getTemplateNumberFormat(exp);
            try {
                return ensureFormatResultString(format.format(tnm), exp, env);
            } catch (TemplateValueFormatException e) {
                throw MessageUtils.newCantFormatNumberException(format, exp, e);
            }
        } else if (tm instanceof TemplateDateModel) {
            TemplateDateModel tdm = (TemplateDateModel) tm;
            TemplateDateFormat format = env.getTemplateDateFormat(tdm, exp);
            try {
                return ensureFormatResultString(format.format(tdm), exp, env);
            } catch (TemplateValueFormatException e) {
                throw MessageUtils.newCantFormatDateException(format, exp, e);
            }
        } else { 
            return coerceModelToTextualCommon(tm, exp, seqTip, false, false, env);
        }
    }

    /**
     * Converts a value to plain text {@link String}, even if the {@link TemplateValueFormat} involved normally produces
     * markup. This should be used rarely, where the user clearly intend to use the plain text variant of the format.
     * 
     * @param seqTip
     *            Tip to display if the value type is not coercable, but it's iterable.
     * 
     * @return Never {@code null}
     */
    static String coerceModelToPlainText(TemplateModel tm, ASTExpression exp, String seqTip,
            Environment env) throws TemplateException {
        if (tm instanceof TemplateNumberModel) {
            return assertFormatResultNotNull(env.formatNumberToPlainText((TemplateNumberModel) tm, exp));
        } else if (tm instanceof TemplateDateModel) {
            return assertFormatResultNotNull(env.formatDateToPlainText((TemplateDateModel) tm, exp));
        } else {
            return coerceModelToTextualCommon(tm, exp, seqTip, false, false, env);
        }
    }

    /**
     * @param tm
     *            If {@code null} that's an exception
     * 
     * @param supportsTOM
     *            Whether the caller {@code coerceModelTo...} method could handle a {@link TemplateMarkupOutputModel}.
     *            
     * @return Never {@code null}
     */
    private static String coerceModelToTextualCommon(
            TemplateModel tm, ASTExpression exp, String seqHint, boolean supportsTOM, boolean returnNullOnNonCoercableType,
            Environment env)
            throws TemplateException {
        if (tm instanceof TemplateStringModel) {
            return modelToString((TemplateStringModel) tm, exp);
        } else if (tm == null || tm == TemplateNullModel.INSTANCE) { // TODO [FM3][null] null shouldn't reach this
            throw InvalidReferenceException.getInstance(exp, env);
        } else if (tm instanceof TemplateBooleanModel) {
            // TODO [FM3][null] Handing booleans should be before TemplateStringModel (number etc. are before it as
            // well). But currently, in FM3, `exp!` returns a multi-typed value that's also a boolean `false`, and so
            // `${missing!}` wouldn't print `""` anymore if we reorder these. But, if and when `null` handling is
            // reworked ("checked nulls"), this problem should go away, and so we should move this.
            boolean booleanValue = ((TemplateBooleanModel) tm).getAsBoolean();
            return env.formatBoolean(booleanValue);
        } else {
            if (returnNullOnNonCoercableType) {
                return null;
            }

            throw newUnexpectedOperandTypeException(
                    exp, tm,
                    supportsTOM ? STRING_COERCABLE_TYPES_OR_TOM_DESC : STRING_COERCABLE_TYPES_DESC,
                    supportsTOM ? EXPECTED_TYPES_STRING_COERCABLE_TYPES_AND_TOM : EXPECTED_TYPES_STRING_COERCABLE,
                    seqHint != null && tm instanceof TemplateIterableModel
                            ? new Object[] { seqHint }
                            : null,
                    env);
        }
    }

    private static String ensureFormatResultString(Object formatResult, ASTExpression exp, Environment env)
            throws TemplateException {
        if (formatResult instanceof String) { 
            return (String) formatResult;
        }
        
        assertFormatResultNotNull(formatResult);
        
        throw new TemplateException(env, new _ErrorDescriptionBuilder(
                "Value was formatted to convert it to string, but the result was markup of ouput format ",
                new _DelayedJQuote(((TemplateMarkupOutputModel) formatResult).getOutputFormat()), ".")
                .tip("Use value?string to force formatting to plain text.")
                .blame(exp));
    }

    static String assertFormatResultNotNull(String r) {
        if (r != null) {
            return r;
        }
        throw new NullPointerException("TemplateValueFormatter result can't be null");
    }

    static Object assertFormatResultNotNull(Object r) {
        if (r != null) {
            return r;
        }
        throw new NullPointerException("TemplateValueFormatter result can't be null");
    }

    static TemplateMarkupOutputModel concatMarkupOutputs(ASTNode parent, TemplateMarkupOutputModel leftMO,
            TemplateMarkupOutputModel rightMO) throws TemplateException {
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
                if (parent instanceof ASTExpression) {
                    throw new TemplateException((ASTExpression) parent, message);
                } else {
                    throw new TemplateException(message);
                }
            }
        } else {
            return leftOF.concat(leftMO, rightMO);
        }
    }

    /**
     * Returns an {@link ArithmeticEngine} even if {@code env} is {@code null}, because we are in parsing phase.
     */
    static ArithmeticEngine getArithmeticEngine(Environment env, ASTNode tObj) {
        return env != null
                ? env.getArithmeticEngine()
                : tObj.getTemplate().getParsingConfiguration().getArithmeticEngine();
    }

    public static void printTemplateMarkupOutputModel(final TemplateMarkupOutputModel mo, OutputFormat outputFormat,
            final Writer out, ASTExpression exp) throws TemplateException, IOException {
        final MarkupOutputFormat moOF = mo.getOutputFormat();
        // ATTENTION: Keep this logic in sync. ?esc/?noEsc's logic!
        if (moOF != outputFormat && !outputFormat.isOutputFormatMixingAllowed()) {
            final String srcPlainText;
            // ATTENTION: Keep this logic in sync. ?esc/?noEsc's logic!
            srcPlainText = moOF.getSourcePlainText(mo);
            if (srcPlainText == null) {
                throw new TemplateException(exp,
                        "The value to print is in ", new _DelayedToString(moOF),
                        " format, which differs from the current output format, ",
                        new _DelayedToString(outputFormat), ". Format conversion wasn't possible.");
            }
            if (outputFormat instanceof MarkupOutputFormat) {
                ((MarkupOutputFormat) outputFormat).output(srcPlainText, out);
            } else {
                out.write(srcPlainText);
            }
        } else {
            moOF.output(mo, out);
        }
    }

    public static Object unwrapTemplateHashModelKey(TemplateModel model) throws TemplateException {
        if (model instanceof AdapterTemplateModel) {
            return ((AdapterTemplateModel) model).getAdaptedObject(Object.class);
        }
        if (model instanceof WrapperTemplateModel) {
            return ((WrapperTemplateModel) model).getWrappedObject();
        }
        if (model instanceof TemplateStringModel) {
            return ((TemplateStringModel) model).getAsString();
        }
        if (model instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) model).getAsNumber();
        }
        if (model instanceof TemplateDateModel) {
            return ((TemplateDateModel) model).getAsDate();
        }
        if (model instanceof TemplateBooleanModel) {
            return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
        }
        // TODO [FM3] Handle List-s, etc.? But wait until FM3 TM-s settle; we might will have TM.getWrappedObject().
        return new TemplateException(
                "Can't unwrapp hash key of this type, yet (TODO): ",
                new _DelayedTemplateLanguageTypeDescription(model));
    }
    
}

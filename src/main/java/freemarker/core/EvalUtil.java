/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.util.Date;

import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans._BeansAPI;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Internally used static utilities for evaluation expressions.
 */
class EvalUtil
{
    static final int CMP_OP_EQUALS = 1;
    static final int CMP_OP_NOT_EQUALS = 2;
    static final int CMP_OP_LESS_THAN = 3;
    static final int CMP_OP_GREATER_THAN = 4;
    static final int CMP_OP_LESS_THAN_EQUALS = 5;
    static final int CMP_OP_GREATER_THAN_EQUALS = 6;
    // If you add a new operator here, update the "compare" and "cmpOpToString" methods!

    // Prevents instantination.
    private EvalUtil() { }
    
    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     * @param env {@code null} is allowed, but may results in lower performance in classic-compatible mode
     */
    static String modelToString(TemplateScalarModel model, Expression expr, Environment env)
    throws TemplateModelException {
        String value = model.getAsString();
        if(value == null) {
            if (env == null) env = Environment.getCurrentEnvironment();
            if (env != null && env.isClassicCompatible()) {
                return "";
            } else {
                throw newModelHasStoredNullException(String.class, model, expr);
            }
        }
        return value;
    }
    
    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     */
    static Number modelToNumber(TemplateNumberModel model, Expression expr)
        throws TemplateModelException
    {
        Number value = model.getAsNumber();
        if(value == null) throw newModelHasStoredNullException(Number.class, model, expr);
        return value;
    }

    /**
     * @param expr {@code null} is allowed, but may results in less helpful error messages
     */
    static Date modelToDate(TemplateDateModel model, Expression expr)
        throws TemplateModelException
    {
        Date value = model.getAsDate();
        if(value == null) throw newModelHasStoredNullException(Date.class, model, expr);
        return value;
    }
    
    /** Signals the buggy case where we have a non-null model, but it wraps a null. */
    private static TemplateModelException newModelHasStoredNullException(
            Class expected, TemplateModel model, Expression expr) {
        return new _TemplateModelException(expr, _TemplateModelException.modelHasStoredNullDescription(expected, model));
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
            Expression leftExp,
            int operator, String  operatorString,
            Expression rightExp,
            Expression defaultBlamed,
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
     * Compares values according the rules of the FTL comparator operators; if the {@link Expression}-s are
     * accessible, use {@link #compare(Expression, int, String, Expression, Expression, Environment)} instead, as
     * that gives better error messages.
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
            TemplateModel leftValue, Expression leftExp,
            int operator, String operatorString,
            TemplateModel rightValue, Expression rightExp,
            Expression defaultBlamed, boolean quoteOperandsInErrors,
            boolean typeMismatchMeansNotEqual,
            boolean leftNullReturnsFalse, boolean rightNullReturnsFalse,
            Environment env) throws TemplateException {
        if (leftValue == null) {
            if (env != null && env.isClassicCompatible()) {
                leftValue = TemplateScalarModel.EMPTY_STRING;
            } else {
                if (leftNullReturnsFalse) { 
                    return false;
                } else {
                    if (leftExp != null) {
                        throw InvalidReferenceException.getInstance(leftExp, env);
                    } else {
                        throw new _MiscTemplateException(defaultBlamed, env, 
                                    "The left operand of the comparison was undefined or null.");
                    }
                }
            }
        }

        if (rightValue == null) {
            if (env != null && env.isClassicCompatible()) {
                rightValue = TemplateScalarModel.EMPTY_STRING;
            } else {
                if (rightNullReturnsFalse) { 
                    return false;
                } else {
                    if (rightExp != null) {
                        throw InvalidReferenceException.getInstance(rightExp, env);
                    } else {
                        throw new _MiscTemplateException(defaultBlamed, env,
                                    "The right operand of the comparison was undefined or null.");
                    }
                }
            }
        }

        final int cmpResult;
        if (leftValue instanceof TemplateNumberModel && rightValue instanceof TemplateNumberModel) {
            Number leftNum = EvalUtil.modelToNumber((TemplateNumberModel) leftValue, leftExp);
            Number rightNum = EvalUtil.modelToNumber((TemplateNumberModel) rightValue, rightExp);
            ArithmeticEngine ae =
                    env != null
                        ? env.getArithmeticEngine()
                        : (leftExp != null
                            ? leftExp.getTemplate().getArithmeticEngine()
                            : ArithmeticEngine.BIGDECIMAL_ENGINE);
            try {
                cmpResult = ae.compareNumbers(leftNum, rightNum);
            } catch (RuntimeException e) {
                throw new _MiscTemplateException(defaultBlamed, e, env, new Object[]
                        { "Unexpected error while comparing two numbers: ", e });
            }
        } else if (leftValue instanceof TemplateDateModel && rightValue instanceof TemplateDateModel) {
            TemplateDateModel leftDateModel = (TemplateDateModel) leftValue;
            TemplateDateModel rightDateModel = (TemplateDateModel) rightValue;
            
            int leftDateType = leftDateModel.getDateType();
            int rightDateType = rightDateModel.getDateType();
            
            if (leftDateType == TemplateDateModel.UNKNOWN || rightDateType == TemplateDateModel.UNKNOWN) {
                String sideName;
                Expression sideExp;
                if (leftDateType == TemplateDateModel.UNKNOWN) {
                    sideName = "left";
                    sideExp = leftExp;
                } else {
                    sideName = "right";
                    sideExp = rightExp;
                }
                
                throw new _MiscTemplateException(sideExp != null ? sideExp : defaultBlamed, env, new Object[] {
                        "The ", sideName, " ", VALUE_OF_THE_COMPARISON_IS_UNKNOWN_DATE_LIKE });
            }
            
            if (leftDateType != rightDateType) {
                ;
                throw new _MiscTemplateException(defaultBlamed, env, new Object[] {
                        "Can't compare dates of different types. Left date type is ",
                        TemplateDateModel.TYPE_NAMES.get(leftDateType), ", right date type is ",
                        TemplateDateModel.TYPE_NAMES.get(rightDateType), "." });
            }

            Date leftDate = EvalUtil.modelToDate(leftDateModel, leftExp);
            Date rightDate = EvalUtil.modelToDate(rightDateModel, rightExp);
            cmpResult = leftDate.compareTo(rightDate);
        } else if (leftValue instanceof TemplateScalarModel && rightValue instanceof TemplateScalarModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                throw new _MiscTemplateException(defaultBlamed, env, new Object[] {
                        "Can't use operator \"", cmpOpToString(operator, operatorString), "\" on string values." });
            }
            String leftString = EvalUtil.modelToString((TemplateScalarModel) leftValue, leftExp, env);
            String rightString = EvalUtil.modelToString((TemplateScalarModel) rightValue, rightExp, env);
            // FIXME NBC: Don't use the Collator here. That's locale-specific, but ==/!= should not be.
            cmpResult = env.getCollator().compare(leftString, rightString);
        } else if (leftValue instanceof TemplateBooleanModel && rightValue instanceof TemplateBooleanModel) {
            if (operator != CMP_OP_EQUALS && operator != CMP_OP_NOT_EQUALS) {
                throw new _MiscTemplateException(defaultBlamed, env, new Object[] {
                        "Can't use operator \"", cmpOpToString(operator, operatorString), "\" on boolean values." });
            }
            boolean leftBool = ((TemplateBooleanModel) leftValue).getAsBoolean();
            boolean rightBool = ((TemplateBooleanModel) rightValue).getAsBoolean();
            cmpResult = (leftBool ? 1 : 0) - (rightBool ? 1 : 0);
        } else if (env.isClassicCompatible()) {
            String leftSting = leftExp.evalAndCoerceToString(env);
            String rightString = rightExp.evalAndCoerceToString(env);
            cmpResult = env.getCollator().compare(leftSting, rightString);
        } else {
            if (typeMismatchMeansNotEqual) {
                if (operator == CMP_OP_EQUALS) {
                    return false;
                } else if (operator == CMP_OP_NOT_EQUALS) {
                    return true;
                }
                // Falls through
            }
            throw new _MiscTemplateException(defaultBlamed, env, new Object[] {
                            "Can't compare values of these types. ",
                            "Allowed comparisons are between two numbers, two strings, two dates, or two booleans.\n",
                            "Left hand operand ",
                            (quoteOperandsInErrors && leftExp != null
                                    ? new Object[] { "(", new _DelayedGetCanonicalForm(leftExp), ") value " }
                                    : (Object) ""),
                            "is ", new _DelayedAOrAn(new _DelayedFTLTypeDescription(leftValue)), ".\n",
                            "Right hand operand ",
                            (quoteOperandsInErrors && rightExp != null
                                    ? new Object[] { "(", new _DelayedGetCanonicalForm(rightExp), ") value " }
                                    : (Object) ""),
                            "is ", new _DelayedAOrAn(new _DelayedFTLTypeDescription(rightValue)),
                            "." });
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

    static String coerceModelToString(TemplateModel tm, Expression exp, String seqHint, Environment env) throws TemplateException {
        if (tm instanceof TemplateNumberModel) {
            return env.formatNumber(modelToNumber((TemplateNumberModel) tm, exp));
        } else if (tm instanceof TemplateDateModel) {
            return env.formatDate((TemplateDateModel) tm, exp);
        } else if (tm instanceof TemplateScalarModel) {
            return modelToString((TemplateScalarModel) tm, exp, env);
        } else if(tm == null) {
            if (env.isClassicCompatible()) {
                return "";
            } else {
                if (exp != null) {
                    throw InvalidReferenceException.getInstance(exp, env);
                } else {
                    throw new InvalidReferenceException(
                            "Null/missing value (no more informatoin avilable)",
                            env);
                }
            }
        } else if (tm instanceof TemplateBooleanModel) {
            // This should be before TemplateScalarModel, but automatic boolean-to-string is only non-error since 2.3.20
            // (and before that when classic_compatible was true), so to keep backward compatibility we couldn't insert
            // this before TemplateScalarModel.
            boolean booleanValue = ((TemplateBooleanModel) tm).getAsBoolean();
            int compatMode = env.getClassicCompatibleAsInt();
            if (compatMode == 0) {
                return env.formatBoolean(booleanValue, false);
            } else {
                if (compatMode == 1) {
                    return booleanValue ? MiscUtil.C_TRUE : "";
                } else if (compatMode == 2) {
                    if (tm instanceof BeanModel) {
                        // In 2.1, bean-wrapped booleans where strings, so that has overridden the boolean behavior: 
                        return _BeansAPI.getAsClassicCompatibleString((BeanModel) tm);
                    } else {
                        return booleanValue ? MiscUtil.C_TRUE : "";
                    }
                } else {
                    throw new BugException("Unsupported classic_compatible variation: " + compatMode);
                }
            }
        } else {
            if (env.isClassicCompatible() && tm instanceof BeanModel) {
                return _BeansAPI.getAsClassicCompatibleString((BeanModel) tm);
            } if (seqHint != null && (tm instanceof TemplateSequenceModel || tm instanceof TemplateCollectionModel)) {
                throw new NonStringException(exp, tm, seqHint, env);
            } else {
                throw new NonStringException(exp, tm, env);
            }
        }
    }
    
}

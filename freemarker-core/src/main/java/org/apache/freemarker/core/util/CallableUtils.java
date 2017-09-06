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

package org.apache.freemarker.core.util;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._CallableUtils;
import org.apache.freemarker.core._DelayedAOrAn;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._DelayedOrdinal;
import org.apache.freemarker.core._DelayedTemplateLanguageTypeDescription;
import org.apache.freemarker.core._ErrorDescriptionBuilder;
import org.apache.freemarker.core._EvalUtils;
import org.apache.freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;

/**
 * Utilities for implementing and calling {@link TemplateCallableModel}-s (such as {@link TemplateDirectiveModel}-s
 * and {@link TemplateFunctionModel}-s).
 */
public final class CallableUtils {

    /** Empty {@link TemplateModel} array, useful to avoid array creation for a 0 length argument list. */
    public static final TemplateModel[] EMPTY_TEMPLATE_MODEL_ARRAY = new TemplateModel[0];

    private CallableUtils() {
        //
    }

    /**
     * Convenience method for calling
     * {@link #newGenericExecuteException(String, TemplateCallableModel, boolean, Throwable)}
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateFunctionModel callable) {
        return newGenericExecuteException(errorDescription, callable, true);
    }

    /**
     * Convenience method for calling
     * {@link #newGenericExecuteException(String, TemplateCallableModel, boolean, Throwable)}
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateDirectiveModel callable) {
        return newGenericExecuteException(errorDescription, callable, false);
    }

    /**
     * Convenience method for calling
     * {@link #newGenericExecuteException(String, TemplateCallableModel, boolean, Throwable)}
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateCallableModel callable, boolean calledAsFunction) {
        return newGenericExecuteException(errorDescription, callable, calledAsFunction, null);
    }

    /**
     * Convenience method for calling
     * {@link #newGenericExecuteException(String, TemplateCallableModel, boolean, Throwable)}
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateFunctionModel callable, Throwable cause) {
        return newGenericExecuteException(errorDescription, callable, true, cause);
    }

    /**
     * Convenience method for calling
     * {@link #newGenericExecuteException(String, TemplateCallableModel, boolean, Throwable)}
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateDirectiveModel callable, Throwable cause) {
        return newGenericExecuteException(errorDescription, callable, false, cause);
    }

    /**
     * Creates an exception related to the execution of a {@link TemplateCallableModel}, for which there's no more
     * specific method in {@link CallableUtils}.
     *
     * @param errorDescription
     *         Complete sentence describing the problem. This will be shown after {@code "When calling Foo: "}.
     * @param callable
     *         The {@link TemplateCallableModel} whose execution was failed.; required for printing proper error
     *         message.
     * @param calledAsFunction
     *         Tells if the {@code callable} was called as function (as opposed to called as a directive), which is
     *         needed for proper error messages. This information is needed because a {@link TemplateCallableModel}
     *         might implements both {@link TemplateFunctionModel} and {@link TemplateDirectiveModel}.
     */
    public static TemplateException newGenericExecuteException(
            String errorDescription, TemplateCallableModel callable, boolean calledAsFunction,
            Throwable cause) {
        return new TemplateException(cause,
                _CallableUtils.getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                errorDescription);
    }

    public static TemplateException newArgumentValueException(
            int argIdx, String problemDescription,
            TemplateDirectiveModel callable) {
        return newArgumentValueException(
                argIdx, problemDescription, callable, false);
    }

    public static TemplateException newArgumentValueException(
            int argIdx, String problemDescription,
            TemplateFunctionModel callable) {
        return newArgumentValueException(
                argIdx, problemDescription, callable, true);
    }

    /**
     * @param problemDescription The continuation of a sentence like {@code "When calling xxx: The 1st argument "}, for
     *                           example {@code "must be a positive number."}.
     */
    public static TemplateException newArgumentValueException(
            int argIdx, String problemDescription,
            TemplateCallableModel callable, boolean calledAsFunction) {
        return _newArgumentValueException(argIdx, problemDescription, callable, calledAsFunction, null);
    }

    /** Don't use it; used internally by FreeMarker, may changes anytime without notice. */
    // TODO [FM3] How to expose tips API?
    public static TemplateException _newArgumentValueException(
            int argIdx, String problemDescription,
            TemplateCallableModel callable, boolean calledAsFunction,
            Object[] tips) {
        return new TemplateException(
                new _ErrorDescriptionBuilder(
                        getMessageArgumentProblem(callable, argIdx, problemDescription, calledAsFunction)
                        ).tips(tips));
    }

    /**
     * Convenience method to call
     * {@link #newArgumentValueTypeException(TemplateModel, int, Class, TemplateCallableModel, boolean)}.
     */
    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class<? extends TemplateModel> expectedType,
            TemplateDirectiveModel callable) {
        return newArgumentValueTypeException(
                argValue, argIdx, expectedType,
                callable, false);
    }

    /**
     * Convenience method to call
     * {@link #newArgumentValueTypeException(TemplateModel, int, Class, TemplateCallableModel, boolean)}.
     */
    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class<? extends TemplateModel> expectedType,
            TemplateFunctionModel callable) {
        return newArgumentValueTypeException(
                argValue, argIdx, expectedType,
                callable, true);
    }

    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class<? extends TemplateModel> expectedType,
            TemplateCallableModel callable, boolean calledAsFunction) {
        return new TemplateException(
                getMessageBadArgumentType(argValue, argIdx,
                        new Class[] { expectedType },
                        TemplateLanguageUtils.getTypeName(expectedType),
                        callable, calledAsFunction));
    }

    /**
     * Convenience method for calling
     * {@link #newArgumentValueTypeException(TemplateModel, int, Class[], String, TemplateCallableModel, boolean)}.
     */
    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class[] expectedTypes, String expectedTypeDescription,
            TemplateDirectiveModel callable) {
        return newArgumentValueTypeException(
                argValue, argIdx, expectedTypes, expectedTypeDescription,
                callable, false);
    }

    /**
     * Convenience method for calling
     * {@link #newArgumentValueTypeException(TemplateModel, int, Class[], String, TemplateCallableModel, boolean)}.
     */
    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class[] expectedTypes, String expectedTypeDescription,
            TemplateFunctionModel callable) {
        return newArgumentValueTypeException(
                argValue, argIdx, expectedTypes, expectedTypeDescription,
                callable, true);
    }

    /**
     * @param expectedTypeDescription Something like "string or number".
     */
    public static TemplateException newArgumentValueTypeException(
            TemplateModel argValue, int argIdx, Class[] expectedTypes, String expectedTypeDescription,
            TemplateCallableModel callable, boolean calledAsFunction) {
        return new TemplateException(
                getMessageBadArgumentType(argValue, argIdx,
                        expectedTypes,
                        expectedTypeDescription,
                        callable, calledAsFunction));
    }

    public static TemplateException newNullOrOmittedArgumentException(int argIdx, TemplateFunctionModel callable) {
        return newNullOrOmittedArgumentException(argIdx, callable, true);
    }

    public static TemplateException newNullOrOmittedArgumentException(int argIdx, TemplateDirectiveModel callable) {
        return newNullOrOmittedArgumentException(argIdx, callable, false);
    }

    public static TemplateException newNullOrOmittedArgumentException(int argIdx, TemplateCallableModel callable,
            boolean calledAsFunction) {
        return _newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction, null);
    }

    /** Don't use it; used internally by FreeMarker, may changes anytime without notice. */
    // TODO [FM3] How to expose tips API?
    public static TemplateException _newNullOrOmittedArgumentException(int argIdx, TemplateCallableModel callable,
            boolean calledAsFunction, Object[] tips) {
        return _newArgumentValueException(argIdx, "can't be omitted or null.", callable, calledAsFunction, tips);
    }

    private static Object getMessagePartsTheSomethingArgument(ArgumentArrayLayout argsLayout, int argsArrayIndex) {
        if (argsArrayIndex < 0) {
            throw new IllegalArgumentException("argsArrayIndex can't be negative");
        }
        if (argsLayout == null || argsArrayIndex < argsLayout.getPredefinedPositionalArgumentCount()) {
            return new Object[] { "The ", new _DelayedOrdinal(argsArrayIndex + 1), " argument " };
        } else if (argsLayout.getPositionalVarargsArgumentIndex() == argsArrayIndex) {
            return argsLayout.getNamedVarargsArgumentIndex() != -1 ? "The positional varargs argument "
                    : "The varargs argument ";
        } else if (argsLayout.getNamedVarargsArgumentIndex() == argsArrayIndex) {
            return "The named varargs argument ";
        } else {
            String argName = argsLayout.getPredefinedNamedArgumentsMap().getKeyOfValue(argsArrayIndex);
                return argName != null
                        ? new Object[] { "The ", new _DelayedJQuote(argName), " argument " }
                        : "The argument "; // Shouldn't occur...
        }
    }

    static Object[] getMessageArgumentProblem(TemplateCallableModel callable, int argIndex, Object
            problemDescription, boolean calledAsFunction) {
        return new Object[] {
                _CallableUtils.getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                getMessagePartsTheSomethingArgument(
                        calledAsFunction ? ((TemplateFunctionModel) callable).getFunctionArgumentArrayLayout()
                                : ((TemplateDirectiveModel) callable).getDirectiveArgumentArrayLayout(),
                        argIndex),
                problemDescription
        };
    }

    private static _ErrorDescriptionBuilder getMessageBadArgumentType(
            TemplateModel argValue, int argIdx, Class<? extends TemplateModel>[] expectedTypes,
            String expectedTypesDesc, TemplateCallableModel callable,
            boolean calledAsFunction) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                getMessageArgumentProblem(
                        callable, argIdx,
                        new Object[]{ "should be ", new _DelayedAOrAn(expectedTypesDesc), ", but was ",
                                new _DelayedAOrAn(new _DelayedTemplateLanguageTypeDescription(argValue)),
                                "." },
                        calledAsFunction));
        if (argValue instanceof _UnexpectedTypeErrorExplainerTemplateModel) {
            Object[] tip = ((_UnexpectedTypeErrorExplainerTemplateModel) argValue).explainTypeError(expectedTypes);
            if (tip != null) {
                desc.tip(tip);
            }
        }
        return desc;
    }

    public static void executeWith0Arguments(
            TemplateDirectiveModel directive, CallPlace callPlace, Writer out, Environment env)
            throws IOException, TemplateException {
        directive.execute(
                getArgumentArrayWithNoArguments(directive.getDirectiveArgumentArrayLayout()), callPlace, out, env);
    }

    public static TemplateModel executeWith0Arguments(
            TemplateFunctionModel function, CallPlace callPlace, Environment env)
            throws TemplateException {
        return function.execute(
                getArgumentArrayWithNoArguments(function.getFunctionArgumentArrayLayout()), callPlace, env);
    }

    private static TemplateModel[] getArgumentArrayWithNoArguments(ArgumentArrayLayout argsLayout) {
        int totalLength = argsLayout != null ? argsLayout.getTotalLength() : 0;
        if (totalLength == 0) {
            return EMPTY_TEMPLATE_MODEL_ARRAY;
        } else {
            TemplateModel[] args = new TemplateModel[totalLength];

            int positionalVarargsArgumentIndex = argsLayout.getPositionalVarargsArgumentIndex();
            if (positionalVarargsArgumentIndex != -1) {
                args[positionalVarargsArgumentIndex] = TemplateSequenceModel.EMPTY_SEQUENCE;
            }

            int namedVarargsArgumentIndex = argsLayout.getNamedVarargsArgumentIndex();
            if (namedVarargsArgumentIndex != -1) {
                args[namedVarargsArgumentIndex] = TemplateSequenceModel.EMPTY_SEQUENCE;
            }
            
            return args;
        }
    }

    // String arg:

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, false, null, callable, true)}.
     */
    public static String getStringArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, false, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, false, null, callable, false)}.
     */
    public static String getStringArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, false, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, true, null, callable, true)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, true, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, true, null, callable, false)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, true, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, true, defaultValue, callable, true)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, String defaultValue, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, true, defaultValue, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, boolean, String, TemplateCallableModel, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, true, defaultValue, callable, false)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, String defaultValue, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, true, defaultValue, callable, false);
    }

    /**
     * See {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel,
     * boolean)}; this does the same, but with {@link TemplateStringModel} as {@code type}, and with {@link String}
     * return value.
     */
    public static String castArgumentValueToString(
            TemplateModel argValue, int argIdx, boolean optional, String defaultValue,
            TemplateCallableModel callable, boolean calledAsFunction)
            throws TemplateException {
        if (argValue instanceof TemplateStringModel) {
            return _EvalUtils.modelToString((TemplateStringModel) argValue, null);
        }
        if (argValue == null) {
            if (optional) {
                return defaultValue;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(argValue, argIdx, TemplateStringModel.class, callable, calledAsFunction);
    }

    // Number arg:

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, false, null, callable, true)}.
     */
    public static Number getNumberArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, false, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, false, null, callable, false)}.
     */
    public static Number getNumberArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, false, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, true, null, callable, true)}.
     */
    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, true, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, true, null, callable, false)}.
     */
    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, true, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, true, defaultValue, callable, true)}.
     */
    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, Number defaultValue, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, true, defaultValue, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToNumber(TemplateModel, int, boolean, Number, TemplateCallableModel, boolean)
     * castArgumentValueToNumber(args[argIndex], argIndex, true, defaultValue, callable, false)}.
     */
    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, Number defaultValue, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, true, defaultValue, callable, false);
    }

    /**
     * See {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel,
     * boolean)}; this does the same, but with {@link TemplateNumberModel} as {@code type}, and with {@link Number}
     * return value.
     */
    public static Number castArgumentValueToNumber(
            TemplateModel argValue, int argIdx, boolean optional, Number defaultValue, TemplateCallableModel callable,
            boolean calledAsFunction)
            throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return _EvalUtils.modelToNumber((TemplateNumberModel) argValue, null);
        }
        if (argValue == null) {
            if (optional) {
                return defaultValue;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(
                argValue, argIdx, TemplateNumberModel.class, callable,
                calledAsFunction);
    }


    // int arg:

    /**
     * Convenience method to call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, false, null, callable, true)}.
     */
    public static int getIntArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToInt(args[argIndex], argIndex, false, 0, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, false, null, callable, false)}.
     */
    public static int getIntArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToInt(args[argIndex], argIndex, false, 0, callable, false);
    }

    /**
     * Convenience method to return {@code null} if the argument is missing, otherwise call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, false, 0, callable, false)}.
     */
    public static Integer getOptionalIntArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        TemplateModel argValue = args[argIndex];
        if (argValue == null) {
            return null;
        }
        return castArgumentValueToInt(argValue, argIndex, false, 0, callable, true);
    }

    /**
     * Convenience method to return {@code null} if the argument is missing, otherwise call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, false, 0, callable, false)}.
     */
    public static Integer getOptionalIntArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        TemplateModel argValue = args[argIndex];
        if (argValue == null) {
            return null;
        }
        return castArgumentValueToInt(argValue, argIndex, false, 0, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, true, defaultValue, callable, true)}.
     */
    public static int getOptionalIntArgument(
            TemplateModel[] args, int argIndex, int defaultValue, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToInt(args[argIndex], argIndex, true, defaultValue, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToInt(TemplateModel, int, boolean, int, TemplateCallableModel, boolean)
     * castArgumentValueToInt(args[argIndex], argIndex, true, defaultValue, callable, false)}.
     */
    public static int getOptionalIntArgument(
            TemplateModel[] args, int argIndex, int defaultValue, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToInt(args[argIndex], argIndex, true, defaultValue, callable, false);
    }

    /**
     * See {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel,
     * boolean)}; this does the same, but with {@link TemplateNumberModel} as {@code type} and with the restriction that
     * the number must be convertable to {@code int} losselessly, and with {@code int} return value.
     */
    public static int castArgumentValueToInt(
            TemplateModel argValue, int argIdx, boolean optional, int defaultValue, TemplateCallableModel callable,
            boolean calledAsFunction)
            throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            Number nm = _EvalUtils.modelToNumber((TemplateNumberModel) argValue, null);
            try {
                return _NumberUtils.toIntExact(nm);
            } catch (ArithmeticException e) {
                throw newArgumentValueException(argIdx, "must be an integer (that fits into 32 bits), but "
                                + nm + " (class: " + _ClassUtils.getShortClassName(nm.getClass())
                                + ") is not a such number.",
                        callable,
                        calledAsFunction);
            }
        }
        if (argValue == null) {
            if (optional) {
                return defaultValue;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(
                argValue, argIdx, new Class[] { TemplateNumberModel.class }, "number (int)", callable,
                calledAsFunction);
    }

    // Boolean arg:

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, false, null, callable, true)}.
     */
    public static boolean getBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, false, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, false, null, callable, false)}.
     */
    public static boolean getBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, false, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, true, null, callable, true)}.
     */
    public static Boolean getOptionalBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, true, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, true, null, callable, false)}.
     */
    public static Boolean getOptionalBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, true, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, true, defaultValue, callable, true)}.
     */
    public static Boolean getOptionalBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable, Boolean defaultValue)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, true, defaultValue, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToBoolean(TemplateModel, int, boolean, Boolean, TemplateCallableModel, boolean)
     * castArgumentValueToBoolean(args[argIndex], argIndex, true, defaultValue, callable, false)}.
     */
    public static Boolean getOptionalBooleanArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable, Boolean defaultValue)
            throws TemplateException {
        return castArgumentValueToBoolean(args[argIndex], argIndex, true, defaultValue, callable, false);
    }

    /**
     * See {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel,
     * boolean)}; this does the same, but with {@link TemplateBooleanModel} as {@code type}, and with {@link Boolean}
     * return value.
     */
    public static Boolean castArgumentValueToBoolean(
            TemplateModel argValue, int argIdx, boolean optional, Boolean defaultValue, TemplateCallableModel callable,
            boolean calledAsFunction)
            throws TemplateException {
        if (argValue instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) argValue).getAsBoolean();
        }
        if (argValue == null) {
            if (optional) {
                return defaultValue;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(
                argValue, argIdx, TemplateBooleanModel.class, callable,
                calledAsFunction);
    }

    // Other type of arg:

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, true, false, null)}.
     */
    public static <T extends TemplateModel> T getArgument(
            TemplateModel[] args, int argIndex, Class<T> type, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, false, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, false, false, null)}.
     */
    public static <T extends TemplateModel> T getArgument(
            TemplateModel[] args, int argIndex, Class<T> type, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, false, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, true, true, null)}.
     */
    public static <T extends TemplateModel> T getOptionalArgument(
            TemplateModel[] args, int argIndex, Class<T> type, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, true, null, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, true, true, null)}.
     */
    public static <T extends TemplateModel> T getOptionalArgument(
            TemplateModel[] args, int argIndex, Class<T> type, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, true, null, callable, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, true, true, null)}.
     */
    public static <T extends TemplateModel> T getOptionalArgument(
            TemplateModel[] args, int argIndex, Class<T> type, T defaultValue, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, true, defaultValue, callable, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValue(TemplateModel, int, Class, boolean, TemplateModel, TemplateCallableModel, boolean)}
     * castArgumentValueToSequence(args[argIndex], argIndex, callable, true, true, null)}.
     */
    public static <T extends TemplateModel> T getOptionalArgument(
            TemplateModel[] args, int argIndex, Class<T> type, T defaultValue, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValue(args[argIndex], argIndex, type, true, defaultValue, callable, false);
    }

    /**
     * Checks if the argument value is of the proper type, also, if it's {@code null}/omitted, in which case it can
     * throw an exception or return a default value.
     * <p>
     * The point of this method is not only to decrease the boiler plate needed for these common checks, but also to
     * standardize the error message content. If the checks themselves don't fit your needs, you should still use {@link
     * #newArgumentValueTypeException(TemplateModel, int, Class, TemplateCallableModel, boolean)} and its overloads,
     * also {@link #newNullOrOmittedArgumentException(int, TemplateCallableModel, boolean)} and its overloads to
     * generate similar error messages.
     *
     * @param argIdx
     *         The index in the {@code args} array (assumed to be a valid index. This is information is needed for
     *         proper error messages.
     * @param type
     *         The expected class of the argument (usually a {@link TemplateModel} subinterface). {@code null} if
     *         there are no type restrictions.
     * @param optional
     *         If we allow the parameter to be {@code null} or omitted.
     * @param defaultValue
     *         The value to return if the parameter was {@code null} or omitted.
     * @param callable
     *         The {@link TemplateCallableModel} whose argument we cast; required for printing proper error message.
     * @param calledAsFunction
     *         Tells if the {@code callable} was called as function (as opposed to called as a directive). This
     *         information is needed because a {@link TemplateCallableModel} might implements both {@link
     *         TemplateFunctionModel} and {@link TemplateDirectiveModel}, in which case this method couldn't tell if the
     *         argument of which we are casting.
     *
     * @return The argument value of the proper type.
     *
     * @throws TemplateException
     *         If the argument is not of the proper type or is non-optional yet {@code null}/omitted. The error message
     *         describes the problem in detail, and is meant to be shown for the template author.
     */
    public static <T extends TemplateModel> T castArgumentValue(
            TemplateModel argValue, int argIdx, Class<T> type,
            boolean optional, T defaultValue, TemplateCallableModel callable,
            boolean calledAsFunction)
            throws TemplateException {
        if (argValue == null) {
            if (optional) {
                return defaultValue;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        if (type == null || type.isInstance(argValue)) {
            return (T) argValue;
        }
        throw newArgumentValueTypeException(
                argValue, argIdx, type, callable,
                calledAsFunction);
    }
    
    // Argument count

    /** Convenience method for calling {@link #checkArgumentCount(int, int, int, TemplateCallableModel, boolean)}. */
    public static void checkArgumentCount(int argCnt, int expectedCnt, TemplateFunctionModel callable)
            throws TemplateException {
        checkArgumentCount(argCnt, expectedCnt, callable, true);
    }

    /** Convenience method for calling {@link #checkArgumentCount(int, int, int, TemplateCallableModel, boolean)}. */
    public static void checkArgumentCount(int argCnt, int expectedCnt, TemplateDirectiveModel callable)
            throws TemplateException {
        checkArgumentCount(argCnt, expectedCnt, callable, false);
    }

    /** Convenience method for calling {@link #checkArgumentCount(int, int, int, TemplateCallableModel, boolean)}. */
    public static void checkArgumentCount(int argCnt, int expectedCnt,
            TemplateCallableModel callable, boolean calledAsFunction) throws TemplateException {
        checkArgumentCount(argCnt, expectedCnt, expectedCnt, callable, calledAsFunction);
    }

    /** Convenience method for calling {@link #checkArgumentCount(int, int, int, TemplateCallableModel, boolean)}. */
    public static void checkArgumentCount(int argCnt, int minCnt, int maxCnt, TemplateFunctionModel callable)
            throws TemplateException {
        checkArgumentCount(argCnt, minCnt, maxCnt, callable, true);
    }

    /** Convenience method for calling {@link #checkArgumentCount(int, int, int, TemplateCallableModel, boolean)}. */
    public static void checkArgumentCount(int argCnt, int minCnt, int maxCnt, TemplateDirectiveModel callable)
            throws TemplateException {
        checkArgumentCount(argCnt, minCnt, maxCnt, callable, false);
    }

    /**
     * Useful when the {@link ArgumentArrayLayout} is {@code null} and so the argument array length is not fixed, to
     * check if the number of arguments is in the given range.
     *
     * @param argCnt
     *         The actual length of the argument array
     * @param minCnt
     *         The minimum expected number of arguments
     * @param maxCnt
     *         The maximum expected number of arguments. Should be equal to or greater than {@code minCnt}
     * @param callable
     *         The {@link TemplateCallableModel} whose argument we cast; required for printing proper error message.
     * @param calledAsFunction
     *         Tells if the {@code callable} was called as function (as opposed to called as a directive). This
     *         information is needed because a {@link TemplateCallableModel} might implements both {@link
     *         TemplateFunctionModel} and {@link TemplateDirectiveModel}, in which case this method couldn't tell if the
     *         argument of which we are casting.
     */
    public static void checkArgumentCount(int argCnt, int minCnt, int maxCnt,
        TemplateCallableModel callable, boolean calledAsFunction) throws TemplateException {
        if (argCnt < minCnt || argCnt > maxCnt) {
            throw new TemplateException(
                    _CallableUtils.getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                    getMessagePartExpectedNArgumentButHadM(argCnt, minCnt, maxCnt));
        }
    }

    private static Object[] getMessagePartExpectedNArgumentButHadM(int argCnt, int minCnt, int maxCnt) {
        ArrayList<Object> desc = new ArrayList<>(20);

        desc.add("Expected ");

        if (minCnt == maxCnt) {
            if (maxCnt == 0) {
                desc.add("no");
            } else {
                desc.add(maxCnt);
            }
        } else if (maxCnt - minCnt == 1) {
            desc.add(minCnt);
            desc.add(" or ");
            desc.add(maxCnt);
        } else {
            desc.add(minCnt);
            if (maxCnt != Integer.MAX_VALUE) {
                desc.add(" to ");
                desc.add(maxCnt);
            } else {
                desc.add(" or more (unlimited)");
            }
        }
        desc.add(" argument");
        if (maxCnt > 1) desc.add("s");

        desc.add(" but has received ");
        if (argCnt == 0) {
            desc.add("none");
        } else {
            desc.add(argCnt);
        }
        desc.add(".");

        return desc.toArray();
    }

}

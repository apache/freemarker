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

import static org.apache.freemarker.core.util.TemplateLanguageUtils.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelWithOriginName;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util.TemplateLanguageUtils;
import org.apache.freemarker.core.util._CollectionUtils;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
// TODO [FM3] Most functionality here should be made public on some way. Also BuiltIn-s has some duplicates utiltity
// methods for this functionality (checking arguments). Need to clean this up.
public final class _CallableUtils {

    public static final TemplateModel[] EMPTY_TEMPLATE_MODEL_ARRAY = new TemplateModel[0];

    private _CallableUtils() {
        //
    }

    /** Convenience method for calling {@link #newGenericExecuteException(TemplateCallableModel, boolean, String)}. */
    public static TemplateException newGenericExecuteException(
            TemplateFunctionModel callable, String errorDescription) {
        return newGenericExecuteException(callable, true, errorDescription);
    }

    /** Convenience method for calling {@link #newGenericExecuteException(TemplateCallableModel, boolean, String)}. */
    public static TemplateException newGenericExecuteException(
            TemplateDirectiveModel callable, String errorDescription) {
        return newGenericExecuteException(callable, false, errorDescription);
    }

    /**
     * @param errorDescription Complete sentence describing the problem. This will be after
     *      {@code "When calling xxx: "}.
     */
    public static TemplateException newGenericExecuteException(
            TemplateCallableModel callable, boolean calledAsFunction, String errorDescription) {
        return new TemplateException(
                getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
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
        return new TemplateException(
                getMessageArgumentProblem(
                        callable, argIdx,
                        problemDescription,
                        calledAsFunction));
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
        return newArgumentValueException(argIdx, "can't be omitted or null.", callable, calledAsFunction);
    }

    /**
     * Something like {@code "When calling function \"lib.ftl:foo\": " or "When calling ?leftPad: "}
     */
    private static Object getMessagePartWhenCallingSomethingColon(
            TemplateCallableModel callable, boolean calledAsFunction) {
        return callable instanceof ASTExpBuiltIn.BuiltInCallable
                ? new Object[] { "When calling ?", ((ASTExpBuiltIn.BuiltInCallable) callable).getBuiltInName() + ": " }
                : new Object[] {
                        "When calling ",
                        getCallableTypeName(callable, calledAsFunction),
                        " ",
                        callable instanceof TemplateModelWithOriginName
                                ? new _DelayedJQuote(((TemplateModelWithOriginName) callable).getOriginName())
                                : new _DelayedShortClassName(callable.getClass()),
                        ": "
                };
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
                getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
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
                        new Object[]{ " should be ", new _DelayedAOrAn(expectedTypesDesc), ", but was ",
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
     * {@link #castArgumentValueToString(TemplateModel, int, TemplateCallableModel, boolean, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, callable, true, false)}.
     */
    public static String getStringArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, callable, true, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, TemplateCallableModel, boolean, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, callable, false, false)}.
     */
    public static String getStringArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, callable, false, false);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, TemplateCallableModel, boolean, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, callable, true, true)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, callable, true, true);
    }

    /**
     * Convenience method to call
     * {@link #castArgumentValueToString(TemplateModel, int, TemplateCallableModel, boolean, boolean)
     * castArgumentValueToString(args[argIndex], argIndex, callable, false, true)}.
     */
    public static String getOptionalStringArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToString(args[argIndex], argIndex, callable, false, true);
    }

    /**
     * Checks if the argument value is a string; it does NOT check if {@code args} is big enough.
     *
     * @param calledAsFunction
     *         If {@code callable} was called as function (as opposed to called as a directive)
     * @param optional
     *         If we allow a {@code null} return value
     *
     * @return Null {@code null} if the argument was omitted or {@code null}
     *
     * @throws TemplateException
     *         If the argument is not of the proper type or is non-optional yet {@code null}. The error message
     *         describes the problem in detail.
     */
    public static String castArgumentValueToString(
            TemplateModel argValue, int argIdx, TemplateCallableModel callable,
            boolean calledAsFunction, boolean optional)
            throws TemplateException {
        if (argValue instanceof TemplateScalarModel) {
            return _EvalUtils.modelToString((TemplateScalarModel) argValue, null);
        }
        if (argValue == null) {
            if (optional) {
                return null;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(argValue, argIdx, TemplateScalarModel.class, callable, calledAsFunction);
    }

    // Number arg:
    
    public static Number getNumberArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, callable, true, false);
    }

    public static Number getNumberArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, callable, false, false);
    }

    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, TemplateFunctionModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, callable, true, true);
    }

    public static Number getOptionalNumberArgument(
            TemplateModel[] args, int argIndex, TemplateDirectiveModel callable)
            throws TemplateException {
        return castArgumentValueToNumber(args[argIndex], argIndex, callable, false, true);
    }

    public static Number castArgumentValueToNumber(
            TemplateModel argValue, int argIdx, TemplateCallableModel callable,
            boolean calledAsFunction, boolean optional)
            throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return _EvalUtils.modelToNumber((TemplateNumberModel) argValue, null);
        }
        if (argValue == null) {
            if (optional) {
                return null;
            }
            throw newNullOrOmittedArgumentException(argIdx, callable, calledAsFunction);
        }
        throw newArgumentValueTypeException(
                argValue, argIdx, TemplateNumberModel.class, callable,
                calledAsFunction);
    }

    // TODO boolean, etc.

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
     * Useful when the {@link ArgumentArrayLayout} is {@code null} and so the argument array length is not fixed,
     * to check if the number of arguments is in the given range.
     */
    public static void checkArgumentCount(int argCnt, int minCnt, int maxCnt,
        TemplateCallableModel callable, boolean calledAsFunction) throws TemplateException {
        if (argCnt < minCnt || argCnt > maxCnt) {
            throw new TemplateException(
                    getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
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

    //

    static TemplateModel[] getExecuteArgs(
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, ArgumentArrayLayout argsLayout,
            TemplateCallableModel callable, boolean calledAsFunction,
            Environment env) throws
            TemplateException {
        return argsLayout != null
                ? getExecuteArgsBasedOnLayout(positionalArgs, namedArgs, argsLayout, callable, calledAsFunction, env)
                : getExecuteArgsWithoutLayout(positionalArgs, namedArgs, callable, calledAsFunction, env);
    }

    private static TemplateModel[] getExecuteArgsWithoutLayout(
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs,
            TemplateCallableModel callable, boolean calledAsFunction,
            Environment env)
            throws TemplateException {
        if (namedArgs != null) {
            throw new TemplateException(env,
                    getNamedArgumentsNotSupportedMessage(callable, namedArgs[0], calledAsFunction));
        }

        TemplateModel[] execArgs;
        if (positionalArgs != null) {
            execArgs = new TemplateModel[positionalArgs.length];
            for (int i = 0; i < positionalArgs.length; i++) {
                ASTExpression positionalArg = positionalArgs[i];
                execArgs[i] = positionalArg.eval(env);
            }
        } else {
            execArgs = EMPTY_TEMPLATE_MODEL_ARRAY;
        }
        return execArgs;
    }

    private static TemplateModel[] getExecuteArgsBasedOnLayout(
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, ArgumentArrayLayout argsLayout,
            TemplateCallableModel callable, boolean calledAsFunction,
            Environment env) throws TemplateException {
        int predefPosArgCnt = argsLayout.getPredefinedPositionalArgumentCount();
        int posVarargsArgIdx = argsLayout.getPositionalVarargsArgumentIndex();

        TemplateModel[] execArgs = new TemplateModel[argsLayout.getTotalLength()];

        // Fill predefined positional args:
        if (positionalArgs != null) {
            int actualPredefPosArgCnt = Math.min(positionalArgs.length, predefPosArgCnt);
            for (int argIdx = 0; argIdx < actualPredefPosArgCnt; argIdx++) {
                execArgs[argIdx] = positionalArgs[argIdx].eval(env);
            }
        }

        if (posVarargsArgIdx != -1) {
            int posVarargsLength = positionalArgs != null ? positionalArgs.length - predefPosArgCnt : 0;
            TemplateSequenceModel varargsSeq;
            if (posVarargsLength <= 0) {
                varargsSeq = TemplateSequenceModel.EMPTY_SEQUENCE;
            } else {
                NativeSequence nativeSeq = new NativeSequence(posVarargsLength);
                varargsSeq = nativeSeq;
                for (int posVarargIdx = 0; posVarargIdx < posVarargsLength; posVarargIdx++) {
                    nativeSeq.add(positionalArgs[predefPosArgCnt + posVarargIdx].eval(env));
                }
            }
            execArgs[posVarargsArgIdx] = varargsSeq;
        } else if (positionalArgs != null && positionalArgs.length > predefPosArgCnt) {
            checkSupportsAnyParameters(callable, argsLayout, calledAsFunction);
            List<String> validPredefNames = argsLayout.getPredefinedNamedArgumentsMap().getKeys();
            _ErrorDescriptionBuilder errorDesc = new _ErrorDescriptionBuilder(
                    getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                    "This ", getCallableTypeName(callable, calledAsFunction),
                    " ",
                    (predefPosArgCnt != 0
                            ? new Object[]{ "can only have ", predefPosArgCnt }
                            : "can't have"
                    ),
                    " arguments passed by position, but the invocation has ",
                    positionalArgs.length, " such arguments.",
                    (!argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported() ?
                            new Object[] {
                                    " Try to pass arguments by name (as in ",
                                    (callable instanceof TemplateDirectiveModel
                                            ? "<@example x=1 y=2 />"
                                            : "example(x=1, y=2)"),
                                    ")",
                                    (!validPredefNames.isEmpty()
                                            ? new Object[] { " The supported parameter names are: ",
                                            new _DelayedJQuotedListing(validPredefNames)}
                                            : _CollectionUtils.EMPTY_OBJECT_ARRAY)}
                            : "")
            );
            if (callable instanceof Environment.TemplateLanguageDirective
                    && !argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported()) {
                errorDesc.tip("You can pass a parameter by position (i.e., without specifying its name, as you"
                        + " have tried now) when the macro has defined that parameter to be a positional parameter. "
                        + "See in the documentation how, and when that's a good practice.");
            }
            throw new TemplateException(env, errorDesc);
        }

        int namedVarargsArgumentIndex = argsLayout.getNamedVarargsArgumentIndex();
        NativeHashEx2 namedVarargsHash = null;
        if (namedArgs != null) {
            StringToIndexMap predefNamedArgsMap = argsLayout.getPredefinedNamedArgumentsMap();
            for (NamedArgument namedArg : namedArgs) {
                int argIdx = predefNamedArgsMap.get(namedArg.name);
                if (argIdx != -1) {
                    execArgs[argIdx] = namedArg.value.eval(env);
                } else {
                    if (namedVarargsHash == null) {
                        if (namedVarargsArgumentIndex == -1) {
                            checkSupportsAnyParameters(callable, argsLayout, calledAsFunction);
                            Collection<String> validNames = predefNamedArgsMap.getKeys();
                            throw new TemplateException(env,
                                    validNames == null || validNames.isEmpty()
                                            ? getNamedArgumentsNotSupportedMessage(
                                                    callable, namedArg, calledAsFunction)
                                            : new Object[] {
                                                    getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                                                    "This ", getCallableTypeName(callable, calledAsFunction),
                                                    " has no parameter that's passed by name and is called ",
                                                    new _DelayedJQuote(namedArg.name),
                                                    ". The supported parameter names are:\n",
                                                    new _DelayedJQuotedListing(validNames)
                                            });
                        }

                        namedVarargsHash = new NativeHashEx2();
                    }
                    namedVarargsHash.put(namedArg.name, namedArg.value.eval(env));
                }
            }
        }
        if (namedVarargsArgumentIndex != -1) {
            execArgs[namedVarargsArgumentIndex] = namedVarargsHash != null ? namedVarargsHash : TemplateHashModel.EMPTY_HASH;
        }
        return execArgs;
    }

    private static Object[] getNamedArgumentsNotSupportedMessage(TemplateCallableModel callable,
            NamedArgument namedArg, boolean calledAsFunction) {
        return new Object[] {
                getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                "This ", getCallableTypeName(callable, calledAsFunction),
                " can't have arguments that are passed by name (like ",
                new _DelayedJQuote(namedArg.name), "). Try to pass arguments by position "
                + "(i.e, without name, as in ",
                (callable instanceof TemplateDirectiveModel
                        ? "<@example arg1, arg2, arg3 />"
                        : "example(arg1, arg2, arg3)"),
                ")."
        };
    }

    static private void checkSupportsAnyParameters(
            TemplateCallableModel callable, ArgumentArrayLayout argsLayout, boolean calledAsFunction)
            throws TemplateException {
        if (argsLayout.getTotalLength() == 0) {
            throw new TemplateException(
                    getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                    "This ", getCallableTypeName(callable, calledAsFunction),
                    " doesn't support any parameters.");
        }
    }

    static final class NamedArgument {
        private final String name;
        private final ASTExpression value;

        NamedArgument(String name, ASTExpression value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        ASTExpression getValue() {
            return value;
        }
    }
}

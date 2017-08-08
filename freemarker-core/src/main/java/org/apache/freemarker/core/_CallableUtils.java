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

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateScalarModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.TemplateLanguageUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
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

    public static Number castArgToNumber(TemplateModel[] args, int argIndex) throws TemplateException {
        return castArgToNumber(args, argIndex, false);
    }

    public static Number castArgToNumber(TemplateModel[] args, int argIndex, boolean optional)
            throws TemplateException {
        return castArgToNumber(args[argIndex], argIndex, optional);
    }

    public static Number castArgToNumber(TemplateModel argValue, int argIndex)
            throws TemplateException {
        return castArgToNumber(argValue, argIndex, false);
    }

    public static Number castArgToNumber(TemplateModel argValue, int argIndex, boolean optional)
            throws TemplateException {
        return castArgToNumber(argValue, null, argIndex, optional);
    }

    public static Number castArgToNumber(TemplateModel argValue, String argName, boolean optional)
            throws TemplateException {
        return castArgToNumber(argValue, argName, -1, optional);
    }

    private static Number castArgToNumber(TemplateModel argValue, String argName, int argIndex, boolean optional)
            throws TemplateException {
        if (argValue instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) argValue).getAsNumber();
        }
        if (argValue == null) {
            if (optional) {
                return null;
            }
            throw new _MiscTemplateException(
                    "The ", argName != null ? new _DelayedJQuote(argName) : new _DelayedOrdinal(argIndex + 1),
                    " argument can't be null.");
        }
        throw new NonNumericalException((Serializable) argName != null ? argName : argIndex, argValue, null, null);
    }
    
    //

    public static String castArgToString(List<? extends TemplateModel> args, int argIndex) throws TemplateException {
        return castArgToString(args, argIndex, false);
    }

    public static String castArgToString(List<? extends TemplateModel> args, int argIndex, boolean optional) throws
            TemplateException {
        return castArgToString(args.get(argIndex), argIndex, optional);
    }

    public static String castArgToString(TemplateModel[] args, int argIndex) throws TemplateException {
        return castArgToString(args, argIndex, false);
    }

    public static String castArgToString(TemplateModel[] args, int argIndex, boolean optional) throws TemplateException {
        return castArgToString(args[argIndex], argIndex, optional);
    }

    public static String castArgToString(TemplateModel argValue, int argIndex) throws TemplateException {
        return castArgToString(argValue, argIndex, false);
    }

    public static String castArgToString(TemplateModel argValue, int argIndex, boolean optional) throws TemplateException {
        return castArgToString(argValue, null, argIndex, optional);
    }

    public static String castArgToString(TemplateModel argValue, String argName, boolean optional) throws TemplateException {
        return castArgToString(argValue, argName, -1, optional);
    }

    private static String castArgToString(
            TemplateModel argValue, String argName, int argIndex,
            boolean optional) throws TemplateException {
        if (argValue instanceof TemplateScalarModel) {
            return _EvalUtils.modelToString((TemplateScalarModel) argValue, null, null);
        }
        if (argValue == null) {
            if (optional) {
                return null;
            }
            throw new _MiscTemplateException(
                    "The ", argName != null ? new _DelayedJQuote(argName) : new _DelayedOrdinal(argIndex + 1),
                    " argument can't be null.");
        }
        throw new NonStringException((Serializable) argName != null ? argName : argIndex, argValue, null, null);
    }

    static TemplateModel[] getExecuteArgs(
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, ArgumentArrayLayout argsLayout,
            TemplateCallableModel callableValue,
            Environment env) throws
            TemplateException {
        return argsLayout != null
                ? getExecuteArgsBasedOnLayout(positionalArgs, namedArgs, argsLayout, callableValue, env)
                : getExecuteArgsWithoutLayout(positionalArgs, namedArgs, callableValue, env);
    }

    private static TemplateModel[] getExecuteArgsWithoutLayout(ASTExpression[] positionalArgs,
            NamedArgument[] namedArgs, TemplateCallableModel callableValue, Environment env)
            throws TemplateException {
        if (namedArgs != null) {
            throw new _MiscTemplateException(env, getNamedArgumentsNotSupportedMessage(callableValue, namedArgs[0]));
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
            TemplateCallableModel callableValue,
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
            checkSupportsAnyParameters(callableValue, argsLayout, env);
            List<String> validPredefNames = argsLayout.getPredefinedNamedArgumentsMap().getKeys();
            _ErrorDescriptionBuilder errorDesc = new _ErrorDescriptionBuilder(
                    "The called ", TemplateLanguageUtils.getCallableTypeName(callableValue), " ",
                    (predefPosArgCnt != 0
                            ? new Object[]{ "can only have ", predefPosArgCnt }
                            : "can't have"
                    ),
                    " arguments passed by position, but the invocation has ",
                    positionalArgs.length, " such arguments.",
                    (!argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported() ?
                            new Object[] {
                                    " Try to pass arguments by name (as in ",
                                    (callableValue instanceof TemplateDirectiveModel
                                            ? "<@example x=1 y=2 />"
                                            : "example(x=1, y=2)"),
                                    ")",
                                    (!validPredefNames.isEmpty()
                                            ? new Object[] { " The supported parameter names are:\n",
                                            new _DelayedJQuotedListing(validPredefNames)}
                                            : _CollectionUtils.EMPTY_OBJECT_ARRAY)}
                            : "")
            );
            if (callableValue instanceof Environment.TemplateLanguageDirective
                    && !argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported()) {
                errorDesc.tip("You can pass a parameter by position (i.e., without specifying its name, as you"
                        + " have tried now) when the macro has defined that parameter to be a positional parameter. "
                        + "See in the documentation how, and when that's a good practice.");
            }
            throw new _MiscTemplateException(env,
                    errorDesc
            );
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
                            checkSupportsAnyParameters(callableValue, argsLayout, env);
                            Collection<String> validNames = predefNamedArgsMap.getKeys();
                            throw new _MiscTemplateException(env,
                                    validNames == null || validNames.isEmpty()
                                            ? getNamedArgumentsNotSupportedMessage(callableValue, namedArg)
                                            : new Object[] {
                                                    "The called ", TemplateLanguageUtils.getCallableTypeName(callableValue),
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

    private static Object[] getNamedArgumentsNotSupportedMessage(TemplateCallableModel callableValue,
            NamedArgument namedArg) {
        return new Object[] {
                "The called ", TemplateLanguageUtils.getCallableTypeName(callableValue),
                " can't have arguments that are passed by name (like ",
                new _DelayedJQuote(namedArg.name), "). Try to pass arguments by position "
                + "(i.e, without name, as in ",
                (callableValue instanceof TemplateDirectiveModel
                        ? "<@example arg1, arg2, arg3 />"
                        : "example(arg1, arg2, arg3)"),
                ")."
        };
    }

    static private void checkSupportsAnyParameters(
            TemplateCallableModel callableValue, ArgumentArrayLayout argsLayout, Environment env)
            throws TemplateException {
        if (argsLayout.getTotalLength() == 0) {
            throw new _MiscTemplateException(env,
                    "The called ", TemplateLanguageUtils.getCallableTypeName(callableValue), " doesn't support any parameters.");
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

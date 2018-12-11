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

import java.util.Collection;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelWithOriginName;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._CollectionUtils;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * The published part of this API is in {@link CallableUtils}.
 */
public class _CallableUtils {
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
                    getNamedArgumentsNotSupportedMessage(callable, namedArgs[0].name, calledAsFunction));
        }

        TemplateModel[] execArgs;
        if (positionalArgs != null) {
            execArgs = new TemplateModel[positionalArgs.length];
            for (int i = 0; i < positionalArgs.length; i++) {
                ASTExpression positionalArg = positionalArgs[i];
                execArgs[i] = positionalArg.eval(env);
            }
        } else {
            execArgs = CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY;
        }
        return execArgs;
    }

    private static TemplateModel[] getExecuteArgsBasedOnLayout(
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, ArgumentArrayLayout argsLayout,
            TemplateCallableModel callable, boolean calledAsFunction,
            Environment env) throws TemplateException {
        final int predefPosArgCnt = argsLayout.getPredefinedPositionalArgumentCount();
        final int posVarargsArgIdx = argsLayout.getPositionalVarargsArgumentIndex();

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
            throw new TemplateException(env,
                    newPositionalArgDoesNotFitArgLayoutErrorDesc(argsLayout, callable, calledAsFunction));
        }

        int namedVarargsArgumentIndex = argsLayout.getNamedVarargsArgumentIndex();
        NativeHashEx namedVarargsHash = null;
        if (namedArgs != null) {
            final StringToIndexMap predefNamedArgsMap = argsLayout.getPredefinedNamedArgumentsMap();
            for (NamedArgument namedArg : namedArgs) {
                int argIdx = predefNamedArgsMap.get(namedArg.name);
                if (argIdx != -1) {
                    execArgs[argIdx] = namedArg.value.eval(env);
                } else {
                    if (namedVarargsHash == null) {
                        if (namedVarargsArgumentIndex == -1) {
                            throw new TemplateException(env,
                                    newNamedArgumentDoesNotArgLayoutErrorDesc(
                                            argsLayout, namedArg.name, callable, calledAsFunction));
                        }

                        namedVarargsHash = new NativeHashEx();
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

    static _ErrorDescriptionBuilder newPositionalArgDoesNotFitArgLayoutErrorDesc(
            ArgumentArrayLayout argsLayout, TemplateCallableModel callable, boolean calledAsFunction) {
        _ErrorDescriptionBuilder noParamsED = checkSupportsAnyParameters(callable, argsLayout, calledAsFunction);
        if (noParamsED != null) {
            return noParamsED;
        }
        
        List<String> validPredefNames;
        _ErrorDescriptionBuilder errorDesc = new _ErrorDescriptionBuilder(
                getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                "This ", getCallableTypeName(callable, calledAsFunction),
                " ",
                (argsLayout.getPredefinedPositionalArgumentCount() != 0
                        ? new Object[]{ "can only have ", argsLayout.getPredefinedPositionalArgumentCount() }
                        : "can't have"
                ),
                " arguments passed by position, but the invocation tries to pass in more.",
                (!argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported() ?
                        new Object[] {
                                " Try to pass arguments by name (as in ",
                                (callable instanceof TemplateDirectiveModel
                                        ? "<@example x=1 y=2 />"
                                        : "example(x=1, y=2)"),
                                ")",
                                (!(validPredefNames = argsLayout.getPredefinedNamedArgumentsMap().getKeys())
                                        .isEmpty()
                                        ? new Object[] {
                                                " The supported parameter names are: ",
                                                new _DelayedJQuotedListing(validPredefNames)
                                        }
                                        : _CollectionUtils.EMPTY_OBJECT_ARRAY)}
                        : "")
        );
        if (callable instanceof Environment.TemplateLanguageDirective
                && !argsLayout.isPositionalParametersSupported() && argsLayout.isNamedParametersSupported()) {
            errorDesc.tip("You can pass a parameter by position (i.e., without specifying its name, as you"
                    + " have tried now) when the directuve (the macro) has defined that parameter to be a "
                    + "positional parameter. See in the documentation how, and when that's a good practice.");
        }
        return errorDesc;
    }
    
    static _ErrorDescriptionBuilder newNamedArgumentDoesNotArgLayoutErrorDesc(ArgumentArrayLayout argsLayout,
            String argName, TemplateCallableModel callable, boolean calledAsFunction) {
        _ErrorDescriptionBuilder noParamsED = checkSupportsAnyParameters(callable, argsLayout, calledAsFunction);
        if (noParamsED != null) {
            return noParamsED;
        }
        
        Collection<String> validNames = argsLayout.getPredefinedNamedArgumentsMap().getKeys();
        _ErrorDescriptionBuilder errorDesc = new _ErrorDescriptionBuilder(
                validNames == null || validNames.isEmpty()
                ? getNamedArgumentsNotSupportedMessage(
                        callable, argName, calledAsFunction)
                : new Object[] {
                        getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                        "This ", getCallableTypeName(callable, calledAsFunction),
                        " has no parameter that's passed by name and is called ",
                        new _DelayedJQuote(argName),
                        ". The supported parameter names are:\n",
                        new _DelayedJQuotedListing(validNames)
                });
        return errorDesc;
    }
    
    static private _ErrorDescriptionBuilder checkSupportsAnyParameters(
            TemplateCallableModel callable, ArgumentArrayLayout argsLayout, boolean calledAsFunction)  {
        return argsLayout.getTotalLength() == 0
                ? new _ErrorDescriptionBuilder(
                            getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                            "This ", getCallableTypeName(callable, calledAsFunction),
                            " doesn't support any parameters.")
                : null;
    }

    private static Object[] getNamedArgumentsNotSupportedMessage(TemplateCallableModel callable,
            String argName, boolean calledAsFunction) {
        return new Object[] {
                getMessagePartWhenCallingSomethingColon(callable, calledAsFunction),
                "This ", getCallableTypeName(callable, calledAsFunction),
                " can't have arguments that are passed by name (like ",
                new _DelayedJQuote(argName), "). Try to pass arguments by position "
                + "(i.e, without name, as in ",
                (callable instanceof TemplateDirectiveModel
                        ? "<@example arg1, arg2, arg3 />"
                        : "example(arg1, arg2, arg3)"),
                ")."
        };
    }

    /**
     * Something like {@code "When calling function \"lib.f3ah:foo\": " or "When calling ?leftPad: "}
     */
    public static Object getMessagePartWhenCallingSomethingColon(
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

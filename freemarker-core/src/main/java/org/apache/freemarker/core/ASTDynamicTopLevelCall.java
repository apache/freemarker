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
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.apache.freemarker.core.ThreadInterruptionSupportTemplatePostProcessor.ASTThreadInterruptionCheck;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.CallPlace;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._ArrayAdapterList;
import org.apache.freemarker.core.util._StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * AST node: {@code <@exp ...>}.
 * Executes a {@link TemplateCallableModel} that's embeddable directly into the static text (hence "top level"). At
 * least in the default template language the value must be a {@link TemplateDirectiveModel}, though technically
 * calling a {@link TemplateFunctionModel} is possible as well (hence it's not called "dynamic directive call").
 * <p>
 * The {@link TemplateCallableModel} object is obtained on runtime by evaluating an expression, and the parameter list
 * is also validated (how many positional parameters are allowed, what named parameters are supported) then. Hence, the
 * call is "dynamic".
 */
class ASTDynamicTopLevelCall extends ASTDirective implements CallPlace {

    static final class NamedArgument {
        private final String name;
        private final ASTExpression value;

        public NamedArgument(String name, ASTExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    private final ASTExpression callableValueExp;
    private final ASTExpression[] positionalArgs;
    private final NamedArgument[] namedArgs;
    private final StringToIndexMap loopVarNames;
    private final boolean allowCallingFunctions;

    private CustomDataHolder customDataHolder;

    /**
     * @param allowCallingFunctions Some template languages may allow calling {@link TemplateFunctionModel}-s
     *                              directly embedded into the static text, in which case this should be {@code true}.
     */
    ASTDynamicTopLevelCall(
            ASTExpression callableValueExp, boolean allowCallingFunctions,
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, StringToIndexMap loopVarNames,
            TemplateElements children) {
        this.callableValueExp = callableValueExp;
        this.allowCallingFunctions = allowCallingFunctions;

        if (positionalArgs != null && positionalArgs.length == 0
                || namedArgs != null && namedArgs.length == 0
                || loopVarNames != null && loopVarNames.size() == 0) {
            throw new IllegalArgumentException("Use null instead of empty collections");
        }
        this.positionalArgs = positionalArgs;
        this.namedArgs = namedArgs;
        this.loopVarNames = loopVarNames;

        setChildren(children);
    }

    @Override
    ASTElement[] accept(Environment env) throws TemplateException, IOException {
        TemplateCallableModel callableValue;
        TemplateDirectiveModel directive;
        TemplateFunctionModel function;
        {
            TemplateModel callableValueTM = callableValueExp._eval(env);
            if (callableValueTM instanceof TemplateDirectiveModel) {
                callableValue = (TemplateCallableModel) callableValueTM;
                directive = (TemplateDirectiveModel) callableValueTM;
                function = null;
            } else if (callableValueTM instanceof TemplateFunctionModel) {
                if (!allowCallingFunctions) {
                    // TODO [FM3][CF] Better exception
                    throw new NonUserDefinedDirectiveLikeException(
                            "Calling functions is not allowed on the top level in this template language", env);
                }
                callableValue = (TemplateCallableModel) callableValueTM;
                directive = null;
                function = (TemplateFunctionModel) callableValue;
            } else if (callableValueTM instanceof ASTDirMacro) {
                // TODO [FM3][CF] Until macros were refactored to be TemplateDirectiveModel-s, we have this hack here.
                ASTDirMacro macro = (ASTDirMacro) callableValueTM;
                if (macro.isFunction()) {
                    throw new _MiscTemplateException(env,
                            "Routine ", new _DelayedJQuote(macro.getName()), " is a function, not a directive. "
                            + "Functions can only be called from expressions, like in ${f()}, ${x + f()} or ",
                            "<@someDirective someParam=f() />", ".");
                }

                // We have to convert arguments to the legacy data structures... yet again, it's only a temporary hack.
                LinkedHashMap<String, ASTExpression> macroNamedArgs;
                if (namedArgs != null) {
                    macroNamedArgs = new LinkedHashMap<>(namedArgs.length * 4 / 3);
                    for (NamedArgument namedArg : namedArgs) {
                        macroNamedArgs.put(namedArg.name, namedArg.value);
                    }
                } else {
                    macroNamedArgs = null;
                }
                env.invoke(macro,
                        macroNamedArgs,
                        _ArrayAdapterList.adapt(positionalArgs),
                        loopVarNames != null ? loopVarNames.getKeys() : null,
                        getChildBuffer());
                return null;
            } else if (callableValueTM == null) {
                throw InvalidReferenceException.getInstance(callableValueExp, env);
            } else {
                throw new NonUserDefinedDirectiveLikeException(callableValueExp, callableValueTM, env);
            }
        }

        ArgumentArrayLayout argsLayout = callableValue.getArgumentArrayLayout();
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
            int posVarargCnt = positionalArgs != null ? positionalArgs.length - predefPosArgCnt : 0;
            TemplateSequenceModel varargsSeq;
            if (posVarargCnt <= 0) {
                varargsSeq = Constants.EMPTY_SEQUENCE;
            } else {
                NativeSequence nativeSeq = new NativeSequence(posVarargCnt);
                varargsSeq = nativeSeq;
                for (int posVarargIdx = 0; posVarargIdx < posVarargCnt; posVarargIdx++) {
                    nativeSeq.add(positionalArgs[predefPosArgCnt + posVarargIdx].eval(env));
                }
            }
            execArgs[posVarargsArgIdx] = varargsSeq;
        } else if (positionalArgs != null && positionalArgs.length > predefPosArgCnt) {
            throw new _MiscTemplateException(this,
                    "The target callable ",
                    (predefPosArgCnt != 0
                            ? new Object[] { "can only have ", predefPosArgCnt }
                            : "can't have"
                    ),
                    " arguments passed by position, but the invocation has ",
                    positionalArgs.length, " such arguments.");
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
                            Collection<String> validNames = predefNamedArgsMap.getKeys();
                            throw new _MiscTemplateException(this,
                                    validNames == null || validNames.isEmpty()
                                    ? new Object[] {
                                            "The target callable doesn't have any by-name-passed parameters (like ",
                                            new _DelayedJQuote(namedArg.name), ")"
                                    }
                                    : new Object[] {
                                            "The target callable has no by-name-passed parameter called ",
                                            new _DelayedJQuote(namedArg.name), ". The supported parameter names are:\n",
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
            execArgs[namedVarargsArgumentIndex] = namedVarargsHash != null ? namedVarargsHash : Constants.EMPTY_HASH;
        }

        if (directive != null) {
            directive.execute(execArgs, this, env.getOut(), env);
        } else {
            TemplateModel result = function.execute(execArgs, env, this);
            if (result == null) {
                throw new _MiscTemplateException(this, "Function has returned no value (or null)");
            }
            // TODO [FM3][CF]
            throw new BugException("Top-level function call not yet implemented");
        }

        return null;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return true;
    }

    @Override
    boolean isShownInStackTrace() {
        return true;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append('@');
        MessageUtil.appendExpressionAsUntearable(sb, callableValueExp);
        boolean nameIsInParen = sb.charAt(sb.length() - 1) == ')';
        if (positionalArgs != null) {
            for (int i = 0; i < positionalArgs.length; i++) {
                ASTExpression argExp = (ASTExpression) positionalArgs[i];
                if (i != 0) {
                    sb.append(',');
                }
                sb.append(' ');
                sb.append(argExp.getCanonicalForm());
            }
        }
        if (namedArgs != null) {
            for (NamedArgument namedArg : namedArgs) {
                sb.append(' ');
                sb.append(_StringUtil.toFTLTopLevelIdentifierReference(namedArg.name));
                sb.append('=');
                MessageUtil.appendExpressionAsUntearable(sb, namedArg.value);
            }
        }
        if (loopVarNames != null) {
            sb.append("; ");
            boolean first = true;
            for (String loopVarName : loopVarNames.getKeys()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(_StringUtil.toFTLTopLevelIdentifierReference(loopVarName));
            }
        }
        if (canonical) {
            if (getChildCount() == 0) {
                sb.append("/>");
            } else {
                sb.append('>');
                sb.append(getChildrenCanonicalForm());
                sb.append("</@");
                if (!nameIsInParen
                        && (callableValueExp instanceof ASTExpVariable
                        || (callableValueExp instanceof ASTExpDot && ((ASTExpDot) callableValueExp).onlyHasIdentifiers()))) {
                    sb.append(callableValueExp.getCanonicalForm());
                }
                sb.append('>');
            }
        }
        return sb.toString();
    }

    @Override
    String getASTNodeDescriptor() {
        return "@";
    }

    @Override
    int getParameterCount() {
        return 1/*nameExp*/
                + (positionalArgs != null ? positionalArgs.length : 0)
                + (namedArgs != null ? namedArgs.length * 2 : 0)
                + (loopVarNames != null ? loopVarNames.size() : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return callableValueExp;
        } else {
            int base = 1;
            final int positionalArgsSize = positionalArgs != null ? positionalArgs.length : 0;
            if (idx - base < positionalArgsSize) {
                return positionalArgs[idx - base];
            } else {
                base += positionalArgsSize;
                final int namedArgsSize = namedArgs != null ? namedArgs.length : 0;
                if (idx - base < namedArgsSize * 2) {
                    NamedArgument namedArg = namedArgs[(idx - base) / 2];
                    return (idx - base) % 2 == 0 ? namedArg.name : namedArg.value;
                } else {
                    base += namedArgsSize * 2;
                    final int bodyParameterNamesSize = loopVarNames != null ? loopVarNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return loopVarNames.getKeys().get(idx - base);
                    } else {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.CALLEE;
        } else {
            int base = 1;
            final int positionalArgsSize = positionalArgs != null ? positionalArgs.length : 0;
            if (idx - base < positionalArgsSize) {
                return ParameterRole.ARGUMENT_VALUE;
            } else {
                base += positionalArgsSize;
                final int namedArgsSize = namedArgs != null ? namedArgs.length : 0;
                if (idx - base < namedArgsSize * 2) {
                    return (idx - base) % 2 == 0 ? ParameterRole.ARGUMENT_NAME : ParameterRole.ARGUMENT_VALUE;
                } else {
                    base += namedArgsSize * 2;
                    final int bodyParameterNamesSize = loopVarNames != null ? loopVarNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return ParameterRole.TARGET_LOOP_VARIABLE;
                    } else {
                        throw new IndexOutOfBoundsException();
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // CallPlace API:

    @Override
    public boolean hasNestedContent() {
        int childCount = getChildCount();
        return childCount != 0 && (childCount > 1 || !(getChild(0) instanceof ASTThreadInterruptionCheck));
    }

    @Override
    public int getLoopVariableCount() {
        return loopVarNames != null ? loopVarNames.size() : 0;
    }

    @Override
    public void executeNestedContent(TemplateModel[] loopVarValues, Writer out, Environment env)
            throws TemplateException, IOException {
        if (loopVarNames != null) {
            int loopVarNamesSize = loopVarNames.size();
            int loopVarValuesSize = loopVarValues != null ? loopVarValues.length : 0;
            if (loopVarValuesSize < loopVarNamesSize) {
                throw new _MiscTemplateException(this,
                        "The invocation declares more nested content parameters (",
                        loopVarNamesSize, ": ", new _DelayedJQuotedListing(loopVarNames.getKeys()),
                        ") than what the called object intends to pass (",
                        loopVarValuesSize, "). Declare no more than ", loopVarValuesSize,
                        " nested content parameters.");
            }
        }
        env.visit(getChildBuffer(), loopVarNames, loopVarValues, out);
    }

    @Override
    @SuppressFBWarnings(value={ "IS2_INCONSISTENT_SYNC", "DC_DOUBLECHECK" }, justification="Performance tricks")
    public Object getOrCreateCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException {
        // We are using double-checked locking, utilizing Java memory model "final" trick.
        // Note that this.customDataHolder is NOT volatile.

        CustomDataHolder customDataHolder = this.customDataHolder;  // Findbugs false alarm
        if (customDataHolder == null) {  // Findbugs false alarm
            synchronized (this) {
                customDataHolder = this.customDataHolder;
                if (customDataHolder == null || customDataHolder.providerIdentity != providerIdentity) {
                    customDataHolder = createNewCustomData(providerIdentity, supplier);
                    this.customDataHolder = customDataHolder;
                }
            }
        }

        if (customDataHolder.providerIdentity != providerIdentity) {
            synchronized (this) {
                customDataHolder = this.customDataHolder;
                if (customDataHolder == null || customDataHolder.providerIdentity != providerIdentity) {
                    customDataHolder = createNewCustomData(providerIdentity, supplier);
                    this.customDataHolder = customDataHolder;
                }
            }
        }

        return customDataHolder.customData;
    }

    private CustomDataHolder createNewCustomData(Object provierIdentity, CommonSupplier supplier)
            throws CallPlaceCustomDataInitializationException {
        CustomDataHolder customDataHolder;
        Object customData;
        try {
            customData = supplier.get();
        } catch (Exception e) {
            throw new CallPlaceCustomDataInitializationException(
                    "Failed to initialize custom data for provider identity "
                            + _StringUtil.tryToString(provierIdentity) + " via factory "
                            + _StringUtil.tryToString(supplier), e);
        }
        if (customData == null) {
            throw new NullPointerException("CommonSupplier.get() has returned null");
        }
        customDataHolder = new CustomDataHolder(provierIdentity, customData);
        return customDataHolder;
    }

    @Override
    public boolean isNestedOutputCacheable() {
        return isChildrenOutputCacheable();
    }

    @Override
    public int getFirstTargetJavaParameterTypeIndex() {
        // TODO [FM3]
        return -1;
    }

    @Override
    public Class<?> getTargetJavaParameterType(int argIndex) {
        // TODO [FM3]
        return null;
    }

    /**
     * Used for implementing double check locking in implementing the
     * {@link #getOrCreateCustomData(Object, CommonSupplier)}.
     */
    private static class CustomDataHolder {

        private final Object providerIdentity;
        private final Object customData;
        public CustomDataHolder(Object providerIdentity, Object customData) {
            this.providerIdentity = providerIdentity;
            this.customData = customData;
        }

    }

}

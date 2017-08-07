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
import java.util.List;

import org.apache.freemarker.core.ThreadInterruptionSupportTemplatePostProcessor.ASTThreadInterruptionCheck;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util.FTLUtil;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.apache.freemarker.core.util._CollectionUtil;
import org.apache.freemarker.core.util._StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * AST node: {@code <@exp ...>}.
 * Executes a {@link TemplateCallableModel} that's embeddable directly into the static text (hence "top level"). At
 * least in the default template language the value must be a {@link TemplateDirectiveModel}, though technically
 * calling a {@link TemplateFunctionModel} is possible as well (hence this class is not called "dynamic directive
 * call").
 * <p>
 * The {@link TemplateCallableModel} object is obtained on runtime by evaluating an expression, and the parameter list
 * is also validated (how many positional parameters are allowed, what named parameters are supported) then. Hence, the
 * call is "dynamic".
 */
class ASTDynamicTopLevelCall extends ASTDirective implements CallPlace  {

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
    private final StringToIndexMap nestedContentParamNames;
    private final boolean allowCallingFunctions;

    // Concurrently accessed, but need not be volatile
    private CustomDataHolder customDataHolder;

    /**
     * @param allowCallingFunctions Some template languages may allow calling {@link TemplateFunctionModel}-s
     *                              directly embedded into the static text, in which case this should be {@code true}.
     */
    ASTDynamicTopLevelCall(
            ASTExpression callableValueExp, boolean allowCallingFunctions,
            ASTExpression[] positionalArgs, NamedArgument[] namedArgs, StringToIndexMap nestedContentParamNames,
            TemplateElements children) {
        this.callableValueExp = callableValueExp;
        this.allowCallingFunctions = allowCallingFunctions;

        if (positionalArgs != null && positionalArgs.length == 0
                || namedArgs != null && namedArgs.length == 0
                || nestedContentParamNames != null && nestedContentParamNames.size() == 0) {
            throw new IllegalArgumentException("Use null instead of empty collections");
        }
        this.positionalArgs = positionalArgs;
        this.namedArgs = namedArgs;
        this.nestedContentParamNames = nestedContentParamNames;

        setChildren(children);
    }

    @Override
    ASTElement[] accept(Environment env) throws TemplateException, IOException {
        TemplateCallableModel callableValue;
        TemplateDirectiveModel directive;
        TemplateFunctionModel function;
        ArgumentArrayLayout argsLayout;
        boolean nestedContentSupported;
        {
            TemplateModel callableValueTM = callableValueExp._eval(env);
            if (callableValueTM instanceof TemplateDirectiveModel) {
                callableValue = (TemplateCallableModel) callableValueTM;
                directive = (TemplateDirectiveModel) callableValueTM;
                function = null;
                argsLayout = directive.getDirectiveArgumentArrayLayout();
                nestedContentSupported = directive.isNestedContentSupported();
            } else if (callableValueTM instanceof TemplateFunctionModel) {
                if (!allowCallingFunctions) {
                    throw new NonDirectiveException(
                            "Calling functions is not allowed. You can only call directives (like macros) here.", env);
                }
                callableValue = (TemplateCallableModel) callableValueTM;
                directive = null;
                function = (TemplateFunctionModel) callableValue;
                argsLayout = function.getFunctionArgumentArrayLayout();
                nestedContentSupported = false;
            } else if (callableValueTM == null) {
                throw InvalidReferenceException.getInstance(callableValueExp, env);
            } else {
                throw new NonDirectiveException(callableValueExp, callableValueTM, env);
            }
        }

        if (!nestedContentSupported && hasNestedContent()) {
            throw new _MiscTemplateException(env, "Nested content is not supported by this directive.");
        }

        TemplateModel[] execArgs = argsLayout != null
                ? getExecuteArgsBasedOnLayout(argsLayout, callableValue, env)
                : getExecuteArgsWithoutLayout(callableValue, env);

        if (directive != null) {
            directive.execute(execArgs, this, env.getOut(), env);
        } else {
            TemplateModel result = function.execute(execArgs, this, env);
            if (result == null) {
                throw new _MiscTemplateException(env, "Function has returned no value (or null)");
            }
            // TODO [FM3] Implement it when we have a such language... it should work like `${f()}`.
            throw new BugException("Top-level function call not yet implemented");
        }

        return null;
    }

    private TemplateModel[] getExecuteArgsWithoutLayout(TemplateCallableModel callableValue, Environment env)
            throws TemplateException {
        if (namedArgs != null) {
            throw new _MiscTemplateException(env, getNamedArgumentsNotSupportedMessage(callableValue, namedArgs[0]));
        }
        TemplateModel[] execArgs = new TemplateModel[positionalArgs.length];
        for (int i = 0; i < positionalArgs.length; i++) {
            ASTExpression positionalArg = positionalArgs[i];
            execArgs[i] = positionalArg.eval(env);
        }
        return execArgs;
    }

    private TemplateModel[] getExecuteArgsBasedOnLayout(ArgumentArrayLayout argsLayout, TemplateCallableModel callableValue,
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
                varargsSeq = Constants.EMPTY_SEQUENCE;
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
                    "The target ", FTLUtil.getCallableTypeName(callableValue), " ",
                    (predefPosArgCnt != 0
                            ? new Object[]{ "can only have ", predefPosArgCnt }
                            : "can't have"
                    ),
                    " arguments passed by position, but the invocation has ",
                    positionalArgs.length, " such arguments. Try to pass arguments by name (as in ",
                    "<@example x=1 y=2 />", ").",
                    (!validPredefNames.isEmpty()
                            ? new Object[] { " The supported parameter names are:\n",
                                    new _DelayedJQuotedListing(validPredefNames)}
                            : _CollectionUtil.EMPTY_OBJECT_ARRAY)
            );
            if (callableValue instanceof Environment.TemplateLanguageDirective) {
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
                                            "The called ", FTLUtil.getCallableTypeName(callableValue),
                                            " has no parameter that's passed by name and is called ",
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
        return execArgs;
    }

    private Object[] getNamedArgumentsNotSupportedMessage(TemplateCallableModel callableValue,
            NamedArgument namedArg) {
        return new Object[] {
                "The called ", FTLUtil.getCallableTypeName(callableValue),
                " can't have arguments that are passed by name (like ",
                new _DelayedJQuote(namedArg.name), "). Try to pass arguments by position "
                + "(i.e, without name, as in ", "<@example 1, 2, 3 />" ,  ")."
        };
    }

    private void checkSupportsAnyParameters(
            TemplateCallableModel callableValue, ArgumentArrayLayout argsLayout, Environment env)
            throws TemplateException {
        if (argsLayout.getTotalLength() == 0) {
            throw new _MiscTemplateException(env,
                    "The called ", FTLUtil.getCallableTypeName(callableValue), " doesn't support any parameters.");
        }
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
        if (nestedContentParamNames != null) {
            sb.append("; ");
            boolean first = true;
            for (String nestedContentParamName : nestedContentParamNames.getKeys()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(_StringUtil.toFTLTopLevelIdentifierReference(nestedContentParamName));
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
                + (nestedContentParamNames != null ? nestedContentParamNames.size() : 0);
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
                    final int bodyParameterNamesSize = nestedContentParamNames != null
                            ? nestedContentParamNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return nestedContentParamNames.getKeys().get(idx - base);
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
                    final int bodyParameterNamesSize = nestedContentParamNames != null
                            ? nestedContentParamNames.size() : 0;
                    if (idx - base < bodyParameterNamesSize) {
                        return ParameterRole.NESTED_CONTENT_PARAMETER;
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
    public int getNestedContentParameterCount() {
        return nestedContentParamNames != null ? nestedContentParamNames.size() : 0;
    }

    @Override
    public void executeNestedContent(TemplateModel[] nestedContentArgs, Writer out, Environment env)
            throws TemplateException, IOException {
        int nestedContentParamNamesSize = nestedContentParamNames != null ? nestedContentParamNames.size() : 0;
        int nestedContentParamValuesSize = nestedContentArgs != null ? nestedContentArgs.length : 0;
        if (nestedContentParamValuesSize != nestedContentParamNamesSize) {
            throw new _MiscTemplateException(env,
                    "The invocation declares ", (nestedContentParamNamesSize != 0 ? nestedContentParamNamesSize : "no"),
                    " nested content parameter(s)",
                    (nestedContentParamNamesSize != 0
                            ? new Object[] { " (", new _DelayedJQuotedListing(nestedContentParamNames.getKeys()), ")", }
                            : ""),
                    ", but the called object intends to pass ",
                    nestedContentParamValuesSize, " parameters. You need to declare ", nestedContentParamValuesSize,
                    " nested content parameters.");
        }
        env.visit(getChildBuffer(), nestedContentParamNames, nestedContentArgs, out);
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

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @SuppressFBWarnings(value={ "IS2_INCONSISTENT_SYNC", "DC_DOUBLECHECK" }, justification="Performance tricks")
    public Object getOrCreateCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException {
        // We are using double-checked locking, but utilizing Java Memory Model "final" behavior, so it's correct.

        CustomDataHolder customDataHolder = this.customDataHolder;  // Findbugs false alarm
        if (customDataHolder == null) {  // Findbugs false alarm
            synchronized (this) {
                customDataHolder = this.customDataHolder;
                if (customDataHolder == null || customDataHolder.providerIdentity != providerIdentity) {
                    customDataHolder = createNewCustomData(providerIdentity, supplier);
                    this.customDataHolder = customDataHolder;
                }
            }
        } else if (customDataHolder.providerIdentity != providerIdentity) {
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

    @Override
    public boolean isCustomDataSupported() {
        return true;
    }

    @Override
    public boolean isNestedOutputCacheable() {
        return isChildrenOutputCacheable();
    }

    private static CustomDataHolder createNewCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException {
        CustomDataHolder customDataHolder;
        Object customData;
        try {
            customData = supplier.get();
        } catch (Exception e) {
            throw new CallPlaceCustomDataInitializationException(
                    "Failed to initialize custom data for provider identity "
                            + _StringUtil.tryToString(providerIdentity) + " via factory "
                            + _StringUtil.tryToString(supplier), e);
        }
        if (customData == null) {
            throw new NullPointerException("CommonSupplier.get() has returned null");
        }
        customDataHolder = new CustomDataHolder(providerIdentity, customData);
        return customDataHolder;
    }

    static class CustomDataHolder {

        // It's important that all fields are final (Java Memory Model behaves specially with finals)!
        private final Object providerIdentity;
        private final Object customData;

        private CustomDataHolder(Object providerIdentity, Object customData) {
            this.providerIdentity = providerIdentity;
            this.customData = customData;
        }

    }

}

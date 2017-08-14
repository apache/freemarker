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

/*
 * 22 October 1999: This class added by Holger Arendt.
 */

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core._CallableUtils.NamedArgument;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util._StringUtils;


/**
 * AST expression node: {@code exp(args)}.
 */
final class ASTExpFunctionCall extends ASTExpression implements CallPlace {

    private final ASTExpression functionExp;
    private final ASTExpression[] positionalArgs;
    private final NamedArgument[] namedArgs;

    ASTExpFunctionCall(
            ASTExpression functionExp, ASTExpression[] positionalArgs, NamedArgument[] namedArgs) {
        this.functionExp = functionExp;

        if (positionalArgs != null && positionalArgs.length == 0
                || namedArgs != null && namedArgs.length == 0) {
            throw new IllegalArgumentException("Use null instead of empty collections");
        }

        this.positionalArgs = positionalArgs;
        this.namedArgs = namedArgs;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateFunctionModel function;
        {
            TemplateModel functionUncasted = functionExp.eval(env);
            if (!(functionUncasted instanceof TemplateFunctionModel)) {
                throw new NonFunctionException(functionExp, functionUncasted, env);
            }
            function = (TemplateFunctionModel) functionUncasted;
        }

        return function.execute(
                _CallableUtils.getExecuteArgs(
                        positionalArgs, namedArgs, function.getFunctionArgumentArrayLayout(),
                        function, true,env),
                this,
                env);
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionExp.getCanonicalForm());
        sb.append("(");

        boolean first = true;
        if (positionalArgs != null) {
            for (ASTExpression positionalArg : positionalArgs) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(positionalArg.getCanonicalForm());
            }
        }
        if (namedArgs != null) {
            for (NamedArgument namedArg : namedArgs) {
                if (!first) {
                    sb.append(',');
                } else {
                    first = false;
                }
                sb.append(_StringUtils.toFTLTopLevelIdentifierReference(namedArg.getName()));
                sb.append('=');
                sb.append(namedArg.getValue().getCanonicalForm());
            }
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    String getASTNodeDescriptor() {
        return "...(...)";
    }
    
    TemplateModel getConstantValue() {
        return null;
    }

    @Override
    boolean isLiteral() {
        return false;
    }

    @Override
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        ASTExpression[] positionalArgsClone;
        if (positionalArgs != null) {
            positionalArgsClone = new ASTExpression[positionalArgs.length];
            for (int i = 0; i < positionalArgs.length; i++) {
                positionalArgsClone[i] = positionalArgs[i].deepCloneWithIdentifierReplaced(
                        replacedIdentifier, replacement, replacementState);
            }
        } else {
            positionalArgsClone = null;
        }

        NamedArgument[] namedArgsClone;
        if (namedArgs != null) {
            namedArgsClone = new NamedArgument[namedArgs.length];
            for (int i = 0; i < namedArgs.length; i++) {
                NamedArgument namedArg = namedArgs[i];
                namedArgsClone[i] = new NamedArgument(
                        namedArg.getName(),
                        namedArg.getValue().deepCloneWithIdentifierReplaced(
                                replacedIdentifier, replacement, replacementState));

            }
        } else {
            namedArgsClone = null;
        }

        return new ASTExpFunctionCall(
                functionExp.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                positionalArgsClone, namedArgsClone);
    }

    @Override
    int getParameterCount() {
        return 1/*nameExp*/
                + (positionalArgs != null ? positionalArgs.length : 0)
                + (namedArgs != null ? namedArgs.length * 2 : 0);
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return functionExp;
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
                    return (idx - base) % 2 == 0 ? namedArg.getName() : namedArg.getValue();
                } else {
                    throw new IndexOutOfBoundsException();
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
                    throw new IndexOutOfBoundsException();
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // CallPlace API

    @Override
    public boolean hasNestedContent() {
        return false;
    }

    @Override
    public int getNestedContentParameterCount() {
        return 0;
    }

    @Override
    public void executeNestedContent(TemplateModel[] nestedContentArgs, Writer out, Environment env)
            throws TemplateException, IOException {
        // Do nothing
    }

    @Override
    public Object getOrCreateCustomData(Object providerIdentity, CommonSupplier<?> supplier)
            throws CallPlaceCustomDataInitializationException {
        throw new UnsupportedOperationException("Expression call places don't store custom data");
    }

    @Override
    public boolean isCustomDataSupported() {
        return false;
    }

    @Override
    public boolean isNestedOutputCacheable() {
        return false;
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
}

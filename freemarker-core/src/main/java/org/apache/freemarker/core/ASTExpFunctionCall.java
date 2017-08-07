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
import java.util.ArrayList;
import java.util.List;

import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.Constants;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util.FTLUtil;


/**
 * AST expression node: {@code exp(args)}.
 */
final class ASTExpFunctionCall extends ASTExpression implements CallPlace {

    private final ASTExpression target;
    private final ASTExpListLiteral arguments;

    ASTExpFunctionCall(ASTExpression target, ArrayList arguments) {
        this(target, new ASTExpListLiteral(arguments));
    }

    private ASTExpFunctionCall(ASTExpression target, ASTExpListLiteral arguments) {
        this.target = target;
        this.arguments = arguments;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel targetModel = target.eval(env);

        if (!(targetModel instanceof TemplateFunctionModel)) {
            throw new NonFunctionException(target, targetModel, env);
        }
        TemplateFunctionModel func = (TemplateFunctionModel) targetModel;

        ArgumentArrayLayout arrayLayout = func.getFunctionArgumentArrayLayout();

        // TODO [FM3] This is just temporary, until we support named args. Then the logic in ASTDynamicTopLevelCall
        // should be reused.

        TemplateModel[] args;
        if (arrayLayout != null) {
            int posVarargsLength;
            int callArgCnt = arguments.size();
            int predefPosArgCnt = arrayLayout.getPredefinedPositionalArgumentCount();
            int posVarargsIdx = arrayLayout.getPositionalVarargsArgumentIndex();
            if (callArgCnt > predefPosArgCnt) {
                if (posVarargsIdx == -1) {
                    throw new _MiscTemplateException(env,
                            "Too many arguments; the target ", FTLUtil.getCallableTypeName(func),
                            " only has ", predefPosArgCnt, " parameters.");
                }
            }

            List<TemplateModel> callArgList = arguments.getModelList(env);

            args = new TemplateModel[arrayLayout.getTotalLength()];
            int callPredefArgCnt = Math.min(callArgCnt, predefPosArgCnt);
            for (int argIdx = 0; argIdx < callPredefArgCnt; argIdx++) {
                args[argIdx] = callArgList.get(argIdx);
            }

            if (posVarargsIdx != -1) {
                TemplateSequenceModel varargsSeq;
                posVarargsLength = callArgCnt - predefPosArgCnt;
                if (posVarargsLength <= 0) {
                    varargsSeq = Constants.EMPTY_SEQUENCE;
                } else {
                    NativeSequence nativeSeq = new NativeSequence(posVarargsLength);
                    varargsSeq = nativeSeq;
                    for (int posVarargIdx = 0; posVarargIdx < posVarargsLength; posVarargIdx++) {
                        nativeSeq.add(callArgList.get(predefPosArgCnt + posVarargIdx));
                    }
                }
                args[posVarargsIdx] = varargsSeq;
            }

            int namedVarargsArgIdx = arrayLayout.getNamedVarargsArgumentIndex();
            if (namedVarargsArgIdx != -1) {
                args[namedVarargsArgIdx] = Constants.EMPTY_HASH;
            }
        } else {
            List<TemplateModel> callArgList = arguments.getModelList(env);
            args = new TemplateModel[callArgList.size()];
            for (int i = 0; i < callArgList.size(); i++) {
                args[i] = callArgList.get(i);
            }
        }

        return func.execute(args, this, env);
    }

    @Override
    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder();
        buf.append(target.getCanonicalForm());
        buf.append("(");
        String list = arguments.getCanonicalForm();
        buf.append(list.substring(1, list.length() - 1));
        buf.append(")");
        return buf.toString();
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
        return new ASTExpFunctionCall(
                target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
                (ASTExpListLiteral) arguments.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }

    @Override
    int getParameterCount() {
        return 1 + arguments.items.size();
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx == 0) {
            return target;
        } else if (idx < getParameterCount()) {
            return arguments.items.get(idx - 1);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx == 0) {
            return ParameterRole.CALLEE;
        } else if (idx < getParameterCount()) {
            return ParameterRole.ARGUMENT_VALUE;
        } else {
            throw new IndexOutOfBoundsException();
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

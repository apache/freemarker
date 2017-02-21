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

import org.apache.freemarker.core.model.TemplateMethodModel;
import org.apache.freemarker.core.model.TemplateMethodModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util._NullWriter;


/**
 * AST expression node: {@code exp(args)}.
 */
final class ASTExpMethodCall extends ASTExpression {

    private final ASTExpression target;
    private final ASTExpListLiteral arguments;

    ASTExpMethodCall(ASTExpression target, ArrayList arguments) {
        this(target, new ASTExpListLiteral(arguments));
    }

    private ASTExpMethodCall(ASTExpression target, ASTExpListLiteral arguments) {
        this.target = target;
        this.arguments = arguments;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel targetModel = target.eval(env);
        if (targetModel instanceof TemplateMethodModel) {
            TemplateMethodModel targetMethod = (TemplateMethodModel) targetModel;
            List argumentStrings = 
            targetMethod instanceof TemplateMethodModelEx
            ? arguments.getModelList(env)
            : arguments.getValueList(env);
            Object result = targetMethod.exec(argumentStrings);
            return env.getObjectWrapper().wrap(result);
        } else if (targetModel instanceof ASTDirMacro) {
            ASTDirMacro func = (ASTDirMacro) targetModel;
            env.setLastReturnValue(null);
            if (!func.isFunction()) {
                throw new _MiscTemplateException(env, "A macro cannot be called in an expression. (Functions can be.)");
            }
            Writer prevOut = env.getOut();
            try {
                env.setOut(_NullWriter.INSTANCE);
                env.invoke(func, null, arguments.items, null, null);
            } catch (IOException e) {
                // Should not occur
                throw new TemplateException("Unexpected exception during function execution", e, env);
            } finally {
                env.setOut(prevOut);
            }
            return env.getLastReturnValue();
        } else {
            throw new NonMethodException(target, targetModel, env);
        }
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
    String getNodeTypeSymbol() {
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
        return new ASTExpMethodCall(
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

}

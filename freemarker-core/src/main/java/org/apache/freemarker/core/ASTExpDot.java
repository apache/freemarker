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

import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST expression node: {@code .} operator.
 */
final class ASTExpDot extends ASTExpression {
    private final ASTExpression target;
    private final String key;

    ASTExpDot(ASTExpression target, String key) {
        this.target = target;
        this.key = key;
    }

    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateModel leftModel = target.eval(env);
        if (leftModel instanceof TemplateHashModel) {
            return ((TemplateHashModel) leftModel).get(key);
        }
        throw new NonHashException(target, leftModel, env);
    }

    @Override
    public String getCanonicalForm() {
        return target.getCanonicalForm() + getASTNodeDescriptor() + _StringUtils.toFTLIdentifierReferenceAfterDot(key);
    }
    
    @Override
    String getASTNodeDescriptor() {
        return ".";
    }
    
    @Override
    boolean isLiteral() {
        return target.isLiteral();
    }

    @Override
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpDot(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        key);
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        return idx == 0 ? target : key;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
    String getRHO() {
        return key;
    }

    boolean onlyHasIdentifiers() {
        return (target instanceof ASTExpVariable) || ((target instanceof ASTExpDot) && ((ASTExpDot) target).onlyHasIdentifiers());
    }
}
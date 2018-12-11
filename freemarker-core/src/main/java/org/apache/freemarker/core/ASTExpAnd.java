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

/**
 * AST expression node: {@code &&} operator
 */
final class ASTExpAnd extends ASTExpBoolean {

    private final ASTExpression lho;
    private final ASTExpression rho;
    private final String opString;

    ASTExpAnd(ASTExpression lho, ASTExpression rho, String opString) {
        this.lho = lho;
        this.rho = rho;
        this.opString = opString.intern();
    }

    @Override
    boolean evalToBoolean(Environment env) throws TemplateException {
        return lho.evalToBoolean(env) && rho.evalToBoolean(env);
    }

    @Override
    public String getCanonicalForm() {
        return lho.getCanonicalForm() + " " + opString + " " + rho.getCanonicalForm();
    }

    @Override
    public String getLabelWithoutParameters() {
        return "&&";
    }
    
    @Override
    boolean isLiteral() {
        return constantValue != null || (lho.isLiteral() && rho.isLiteral());
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpAnd(
    	        lho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        rho.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        opString);
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return lho;
        case 1: return rho;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
}
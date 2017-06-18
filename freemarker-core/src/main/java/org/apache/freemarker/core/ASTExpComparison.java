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

import org.apache.freemarker.core.util.BugException;

/**
 * AST expression node: Comparison operators, like {@code ==}, {@code !=}, {@code <}, etc.
 */
final class ASTExpComparison extends ASTExpBoolean {

    private final ASTExpression left;
    private final ASTExpression right;
    private final int operation;
    private final String opString;

    ASTExpComparison(ASTExpression left, ASTExpression right, String opString) {
        this.left = left;
        this.right = right;
        opString = opString.intern();
        this.opString = opString;
        if (opString == "==" || opString == "=") {
            operation = _EvalUtil.CMP_OP_EQUALS;
        } else if (opString == "!=") {
            operation = _EvalUtil.CMP_OP_NOT_EQUALS;
        } else if (opString == "gt" || opString == "\\gt" || opString == ">" || opString == "&gt;") {
            operation = _EvalUtil.CMP_OP_GREATER_THAN;
        } else if (opString == "gte" || opString == "\\gte" || opString == ">=" || opString == "&gt;=") {
            operation = _EvalUtil.CMP_OP_GREATER_THAN_EQUALS;
        } else if (opString == "lt" || opString == "\\lt" || opString == "<" || opString == "&lt;") {
            operation = _EvalUtil.CMP_OP_LESS_THAN;
        } else if (opString == "lte" || opString == "\\lte" || opString == "<=" || opString == "&lt;=") {
            operation = _EvalUtil.CMP_OP_LESS_THAN_EQUALS;
        } else {
            throw new BugException("Unknown comparison operator " + opString);
        }
    }

    /*
     * WARNING! This algorithm is duplicated in SequenceBuiltins.modelsEqual.
     * Thus, if you update this method, then you have to update that too!
     */
    @Override
    boolean evalToBoolean(Environment env) throws TemplateException {
        return _EvalUtil.compare(left, operation, opString, right, this, env);
    }

    @Override
    public String getCanonicalForm() {
        return left.getCanonicalForm() + ' ' + opString + ' ' + right.getCanonicalForm();
    }
    
    @Override
    String getASTNodeDescriptor() {
        return opString;
    }

    @Override
    boolean isLiteral() {
        return constantValue != null || (left.isLiteral() && right.isLiteral());
    }

    @Override
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpComparison(
    	        left.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        right.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        opString);
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        return idx == 0 ? left : right;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        return ParameterRole.forBinaryOperatorOperand(idx);
    }
    
}

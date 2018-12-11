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

import org.apache.freemarker.core.model.TemplateModel;

/**
 * AST expression node: {@code (exp)}.
 */
final class ASTExpParenthesis extends ASTExpression {

    private final ASTExpression nested;

    ASTExpParenthesis(ASTExpression nested) {
        this.nested = nested;
    }

    @Override
    boolean evalToBoolean(Environment env) throws TemplateException {
        return nested.evalToBoolean(env);
    }

    @Override
    public String getCanonicalForm() {
        return "(" + nested.getCanonicalForm() + ")";
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "(...)";
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        return nested.eval(env);
    }
    
    @Override
    public boolean isLiteral() {
        return nested.isLiteral();
    }
    
    ASTExpression getNestedExpression() {
        return nested;
    }

    @Override
    ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
        return new ASTExpParenthesis(
                nested.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
    
    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return nested;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.ENCLOSED_OPERAND;
    }
    
}

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

import org.apache.freemarker.core.arithmetic.impl.ConservativeArithmeticEngine;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;

/**
 * AST expression node: {@code -exp} or {@code +exp}.
 */
final class ASTExpNegateOrPlus extends ASTExpression {
    
    private static final int TYPE_MINUS = 0;
    private static final int TYPE_PLUS = 1;

    private final ASTExpression target;
    private final boolean isMinus;
    private static final Integer MINUS_ONE = Integer.valueOf(-1); 

    ASTExpNegateOrPlus(ASTExpression target, boolean isMinus) {
        this.target = target;
        this.isMinus = isMinus;
    }
    
    @Override
    TemplateModel _eval(Environment env) throws TemplateException {
        TemplateNumberModel targetModel = null;
        TemplateModel tm = target.eval(env);
        try {
            targetModel = (TemplateNumberModel) tm;
        } catch (ClassCastException cce) {
            throw MessageUtils.newUnexpectedOperandTypeException(target, tm, TemplateNumberModel.class, env);
        }
        if (!isMinus) {
            return targetModel;
        }
        target.assertNonNull(targetModel, env);
        Number n = targetModel.getAsNumber();
        // [FM3] Add ArithmeticEngine.negate, then use the engine from the env
        n = ConservativeArithmeticEngine.INSTANCE.multiply(MINUS_ONE, n);
        return new SimpleNumber(n);
    }
    
    @Override
    public String getCanonicalForm() {
        String op = isMinus ? "-" : "+";
        return op + target.getCanonicalForm();
    }

    @Override
    String getASTNodeDescriptor() {
        return isMinus ? "-..." : "+...";
    }
    
    @Override
    boolean isLiteral() {
        return target.isLiteral();
    }

    @Override
    protected ASTExpression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, ASTExpression replacement, ReplacemenetState replacementState) {
    	return new ASTExpNegateOrPlus(
    	        target.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState),
    	        isMinus);
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return target;
        case 1: return Integer.valueOf(isMinus ? TYPE_MINUS : TYPE_PLUS);
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.RIGHT_HAND_OPERAND;
        case 1: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
}

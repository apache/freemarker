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

import org.apache.freemarker.core.ASTExpression.ReplacemenetState;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #escape}.
 */
class ASTDirEscape extends ASTDirective {

    private final String variable;
    private final ASTExpression expr;
    private ASTExpression escapedExpr;


    ASTDirEscape(String variable, ASTExpression expr, ASTExpression escapedExpr) {
        this.variable = variable;
        this.expr = expr;
        this.escapedExpr = escapedExpr;
    }

    void setContent(TemplateElements children) {
        setChildren(children);
        // We don't need it anymore at this point
        escapedExpr = null;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        return getChildBuffer();
    }

    ASTExpression doEscape(ASTExpression expression) {
        return escapedExpr.deepCloneWithIdentifierReplaced(variable, expression, new ReplacemenetState());
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters())
                .append(' ').append(_StringUtils.toFTLTopLevelIdentifierReference(variable))
                .append(" as ").append(expr.getCanonicalForm());
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</").append(getLabelWithoutParameters()).append('>');
        }
        return sb.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "#escape";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variable;
        case 1: return expr;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.PLACEHOLDER_VARIABLE;
        case 1: return ParameterRole.EXPRESSION_TEMPLATE;
        default: throw new IndexOutOfBoundsException();
        }
    }    

    @Override
    boolean isOutputCacheable() {
        return true;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

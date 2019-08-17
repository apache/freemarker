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

package freemarker.core;

import java.io.IOException;

import freemarker.core.Expression.ReplacemenetState;
import freemarker.template.TemplateException;

/**
 * Representation of the compile-time #escape directive.
 */
class EscapeBlock extends TemplateElement {

    private final String variable;
    private final Expression expr;
    private Expression escapedExpr;


    EscapeBlock(String variable, Expression expr, Expression escapedExpr) {
        this.variable = variable;
        this.expr = expr;
        this.escapedExpr = escapedExpr;
    }

    void setContent(TemplateElements children) {
        setChildren(children);
        // We don't need it anymore at this point
        this.escapedExpr = null;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        return getChildBuffer();
    }

    Expression doEscape(Expression expression) throws ParseException {
        try {
            return escapedExpr.deepCloneWithIdentifierReplaced(variable, expression, new ReplacemenetState());
        } catch (UncheckedParseException e) {
            throw e.getParseException();
        }
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol())
                .append(' ').append(_CoreStringUtils.toFTLTopLevelIdentifierReference(variable))
                .append(" as ").append(expr.getCanonicalForm());
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
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

/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    void setContent(TemplateElement nestedBlock) {
        setNestedBlock(nestedBlock);
        // We don't need it anymore at this point
        this.escapedExpr = null;
    }

    void accept(Environment env) throws TemplateException, IOException {
        if (getNestedBlock() != null) {
            env.visit(getNestedBlock());
        }
    }

    Expression doEscape(Expression expression) {
        return escapedExpr.deepCloneWithIdentifierReplaced(variable, expression, new ReplacemenetState());
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol())
                .append(' ').append(_CoreStringUtils.toFTLTopLevelIdentifierReference(variable))
                .append(" as ").append(expr.getCanonicalForm());
        if (canonical) {
            sb.append('>');
            if (getNestedBlock() != null) {
                sb.append(getNestedBlock().getCanonicalForm());
            }
            sb.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return "#escape";
    }
    
    boolean isShownInStackTrace() {
        return false;
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variable;
        case 1: return expr;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.PLACEHOLDER_VARIABLE;
        case 1: return ParameterRole.EXPRESSION_TEMPLATE;
        default: throw new IndexOutOfBoundsException();
        }
    }    

    boolean isOutputCacheable() {
        return true;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

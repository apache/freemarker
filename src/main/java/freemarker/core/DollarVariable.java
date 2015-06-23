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

import freemarker.template.TemplateException;
import freemarker.template.utility.StringUtil;

/**
 * An instruction that outputs the value of an <tt>Expression</tt>.
 */
final class DollarVariable extends Interpolation {

    private final Expression expression;
    private final Expression escapedExpression;

    DollarVariable(Expression expression, Expression escapedExpression) {
        this.expression = expression;
        this.escapedExpression = escapedExpression;
    }

    /**
     * Outputs the string value of the enclosed expression.
     */
    void accept(Environment env) throws TemplateException, IOException {
        env.getOut().write(escapedExpression.evalAndCoerceToString(env));
    }

    protected String dump(boolean canonical, boolean inStringLiteral) {
        StringBuffer sb = new StringBuffer();
        sb.append("${");
        final String exprCF = expression.getCanonicalForm();
        sb.append(inStringLiteral ? StringUtil.FTLStringLiteralEnc(exprCF, '"') : exprCF);
        sb.append("}");
        if (!canonical && expression != escapedExpression) {
            sb.append(" auto-escaped");            
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return "${...}";
    }

    boolean heedsOpeningWhitespace() {
        return true;
    }

    boolean heedsTrailingWhitespace() {
        return true;
    }

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return expression;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.CONTENT;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

final class ParentheticalExpression extends Expression {

    private final Expression nested;

    ParentheticalExpression(Expression nested) {
        this.nested = nested;
    }

    boolean evalToBoolean(Environment env) throws TemplateException {
        return nested.evalToBoolean(env);
    }

    public String getCanonicalForm() {
        return "(" + nested.getCanonicalForm() + ")";
    }
    
    String getNodeTypeSymbol() {
        return "(...)";
    }
    
    TemplateModel _eval(Environment env) throws TemplateException 
    {
        return nested.eval(env);
    }
    
    public boolean isLiteral() {
        return nested.isLiteral();
    }
    
    Expression getNestedExpression() {
        return nested;
    }

    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new ParentheticalExpression(
                nested.deepCloneWithIdentifierReplaced(replacedIdentifier, replacement, replacementState));
    }
    
    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return nested;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.ENCLOSED_OPERAND;
    }
    
}

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

/**
 * Represents a case in a switch statement.
 */
final class Case extends TemplateElement {

    final int TYPE_CASE = 0;
    final int TYPE_DEFAULT = 1;
    
    Expression condition;

    Case(Expression matchingValue, TemplateElement nestedBlock) 
    {
        this.condition = matchingValue;
        setNestedBlock(nestedBlock);
    }

    void accept(Environment env) 
        throws TemplateException, IOException 
    {
        if (getNestedBlock() != null) {
            env.visitByHiddingParent(getNestedBlock());
        }
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (condition != null) {
            sb.append(' ');
            sb.append(condition.getCanonicalForm());
        }
        if (canonical) {
            sb.append('>');
            if (getNestedBlock() != null) sb.append(getNestedBlock().getCanonicalForm());
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return condition != null ? "#case" : "#default";
    }

    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return condition;
        case 1: return new Integer(condition != null ? TYPE_CASE : TYPE_DEFAULT);
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.CONDITION;
        case 1: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
        
}

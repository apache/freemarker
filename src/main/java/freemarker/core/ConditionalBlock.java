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
 * An element that represents a conditionally executed block: #if, #elseif or #elseif. Note that when an #if has
 * related #elseif-s or #else, an {@link IfBlock} parent must be used. For a lonely #if, no such parent is needed. 
 */

final class ConditionalBlock extends TemplateElement {

    static final int TYPE_IF = 0;
    static final int TYPE_ELSE = 1;
    static final int TYPE_ELSE_IF = 2;
    
    final Expression condition;
    private final int type;
    boolean isLonelyIf;

    ConditionalBlock(Expression condition, TemplateElement nestedBlock, int type)
    {
        this.condition = condition;
        setNestedBlock(nestedBlock);
        this.type = type;
    }

    void accept(Environment env) throws TemplateException, IOException {
        if (condition == null || condition.evalToBoolean(env)) {
            if (getNestedBlock() != null) {
                env.visitByHiddingParent(getNestedBlock());
            }
        }
    }
    
    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        if (condition != null) {
            buf.append(' ');
            buf.append(condition.getCanonicalForm());
        }
        if (canonical) {
            buf.append(">");
            if (getNestedBlock() != null) {
                buf.append(getNestedBlock().getCanonicalForm());
            }
            if (isLonelyIf) {
                buf.append("</#if>");
            }
        }
        return buf.toString();
    }
    
    String getNodeTypeSymbol() {
        if (type == TYPE_ELSE) {
            return "#else";
        } else if (type == TYPE_IF) {
            return "#if";
        } else if (type == TYPE_ELSE_IF) {
            return "#elseif";
        } else {
            throw new BugException("Unknown type");
        }
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return condition;
        case 1: return new Integer(type);
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

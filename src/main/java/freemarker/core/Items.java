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

import freemarker.core.IteratorBlock.IterationContext;
import freemarker.template.TemplateException;

/**
 * An #items element.
 */
class Items extends TemplateElement {

    private final String loopVarName;

    public Items(String loopVariableName, TemplateElement nestedBlock) {
        this.loopVarName = loopVariableName;
        setNestedBlock(nestedBlock);
    }

    void accept(Environment env) throws TemplateException, IOException {
        final IterationContext iterCtx = IteratorBlock.findEnclosingIterationContext(env, null);
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new _MiscTemplateException(env,
                    new Object[] { getNodeTypeSymbol(), " without iteraton in context" });
        }
        
        iterCtx.loopForItemsElement(env, getNestedBlock(), loopVarName);
    }

    boolean isNestedBlockRepeater() {
        return true;
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(" as ");
        sb.append(loopVarName);
        if (canonical) {
            sb.append('>');
            if (getNestedBlock() != null) sb.append(getNestedBlock().getCanonicalForm());
            sb.append("</");
            sb.append(getNodeTypeSymbol());
            sb.append('>');
        }
        return sb.toString();
    }

    String getNodeTypeSymbol() {
        return "#items";
    }

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        return loopVarName;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx == 0) return ParameterRole.TARGET_LOOP_VARIABLE;
        else
            throw new IndexOutOfBoundsException();
    }

}

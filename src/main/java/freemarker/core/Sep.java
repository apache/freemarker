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
 * A #sep element.
 */
class Sep extends TemplateElement {

    public Sep(TemplateElement nestedBlock) {
        setNestedBlock(nestedBlock);
    }

    void accept(Environment env) throws TemplateException, IOException {
        final IterationContext iterCtx = IteratorBlock.findEnclosingIterationContext(env, null);
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new _MiscTemplateException(env,
                    new Object[] { getNodeTypeSymbol(), " without iteraton in context" });
        }
        
        if (iterCtx.hasNext()) {
            env.visitByHiddingParent(getNestedBlock());
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
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
        return "#sep";
    }

    int getParameterCount() {
        return 0;
    }

    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

}

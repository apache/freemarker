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
 */
class NoEscapeBlock extends TemplateElement {

    NoEscapeBlock(TemplateElement nestedBlock) {
        this.nestedBlock = nestedBlock;
    }
    
    void accept(Environment env) throws TemplateException, IOException {
        if (nestedBlock != null) {
            env.visit(nestedBlock);
        }
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            return "<" + getNodeTypeSymbol() + '>' + nestedBlock.getCanonicalForm() + "</" + getNodeTypeSymbol() + '>';
        } else {
            return getNodeTypeSymbol();
        }
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
    
    String getNodeTypeSymbol() {
        return "#noescape";
    }

    boolean isOutputCacheable() {
        return true;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

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
 * #else element that belongs to a #list, not to an #if.
 */
final class ElseOfList extends TemplateElement {
    
    ElseOfList(TemplateElement block) {
        setNestedBlock(block);
    }

    void accept(Environment env) throws TemplateException, IOException 
    {
        if (getNestedBlock() != null) {
            env.visitByHiddingParent(getNestedBlock());
        }
    }

    protected String dump(boolean canonical) {
        if (canonical) {
            StringBuffer buf = new StringBuffer();
            buf.append('<').append(getNodeTypeSymbol()).append('>');
            if (getNestedBlock() != null) {
                buf.append(getNestedBlock().getCanonicalForm());            
            }
            return buf.toString();
        } else {
            return getNodeTypeSymbol();
        }
    }

    String getNodeTypeSymbol() {
        return "#else";
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

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

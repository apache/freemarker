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

/**
 * An instruction that indicates that that opening
 * and trailing whitespace on this line should be trimmed.
 */
final class TrimInstruction extends TemplateElement {
    
    private final int TYPE_T = 0;  
    private final int TYPE_LT = 1;  
    private final int TYPE_RT = 2;  
    private final int TYPE_NT = 3;  

    final boolean left, right;

    TrimInstruction(boolean left, boolean right) {
        this.left = left;
        this.right = right;
    }

    void accept(Environment env) {
        // This instruction does nothing at render-time, only parse-time.
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (canonical) sb.append("/>");
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        if (left && right) {
            return "#t";
        } else if (left) {
            return "#lt";
        } else if (right) {
            return "#rt";
        } else {
            return "#nt";
        }
    }
    
    boolean isIgnorable() {
        return true;
    }

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        int type;
        if (left && right) {
            type = TYPE_T;
        } else if (left) {
            type = TYPE_LT;
        } else if (right) {
            type = TYPE_RT;
        } else {
            type = TYPE_NT;
        }
        return new Integer(type);
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.AST_NODE_SUBTYPE;
    }

    boolean isOutputCacheable() {
        return true;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

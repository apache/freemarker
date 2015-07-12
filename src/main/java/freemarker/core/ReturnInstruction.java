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

/**
 * Represents a &lt;return&gt; instruction to jump out of a macro.
 */
public final class ReturnInstruction extends TemplateElement {

    private Expression exp;

    ReturnInstruction(Expression exp) {
        this.exp = exp;
    }

    @Override
    void accept(Environment env) throws TemplateException {
        if (exp != null) {
            env.setLastReturnValue(exp.eval(env));
        }
        if (nextSibling() != null) {
            // We need to jump out using an exception.
            throw Return.INSTANCE;
        }
        if (!(getParentElement() instanceof Macro || getParentElement().getParentElement() instanceof Macro)) {
            // Here also, we need to jump out using an exception.
            throw Return.INSTANCE;
        }
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (exp != null) {
            sb.append(' ');
            sb.append(exp.getCanonicalForm());
        }
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#return";
    }
    
    public static class Return extends RuntimeException {
        static final Return INSTANCE = new Return();
        private Return() {
        }
    }
    
    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return exp;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.VALUE;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

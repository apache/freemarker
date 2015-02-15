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
import java.util.Iterator;
import java.util.LinkedList;

import freemarker.template.TemplateException;

/**
 * An instruction representing a switch-case structure.
 */
final class SwitchBlock extends TemplateElement {

    private Case defaultCase;
    private final Expression searched;

    /**
     * @param searched the expression to be tested.
     */
    SwitchBlock(Expression searched) {
        this.searched = searched;
        nestedElements = new LinkedList();
    }

    /**
     * @param cas a Case element.
     */
    void addCase(Case cas) {
        if (cas.condition == null) {
            defaultCase = cas;
        }
        nestedElements.add(cas);
    }

    void accept(Environment env) 
        throws TemplateException, IOException 
    {
        boolean processedCase = false;
        Iterator iterator = nestedElements.iterator();
        try {
            while (iterator.hasNext()) {
                Case cas = (Case)iterator.next();
                boolean processCase = false;

                // Fall through if a previous case tested true.
                if (processedCase) {
                    processCase = true;
                } else if (cas.condition != null) {
                    // Otherwise, if this case isn't the default, test it.
                    processCase = EvalUtil.compare(
                            searched,
                            EvalUtil.CMP_OP_EQUALS, "case==", cas.condition, cas.condition, env);
                }
                if (processCase) {
                    env.visitByHiddingParent(cas);
                    processedCase = true;
                }
            }

            // If we didn't process any nestedElements, and we have a default,
            // process it.
            if (!processedCase && defaultCase != null) {
                env.visitByHiddingParent(defaultCase);
            }
        }
        catch (BreakInstruction.Break br) {}
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(getNodeTypeSymbol());
        buf.append(' ');
        buf.append(searched.getCanonicalForm());
        if (canonical) {
            buf.append('>');
            for (int i = 0; i<nestedElements.size(); i++) {
                Case cas = (Case) nestedElements.get(i);
                buf.append(cas.getCanonicalForm());
            }
            buf.append("</").append(getNodeTypeSymbol()).append('>');
        }
        return buf.toString();
    }

    String getNodeTypeSymbol() {
        return "#switch";
    }

    int getParameterCount() {
        return 1;
    }

    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return searched;
    }

    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.VALUE;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

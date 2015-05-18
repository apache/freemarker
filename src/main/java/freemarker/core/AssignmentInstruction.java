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
import java.util.ArrayList;

import freemarker.template.TemplateException;

/**
 * An instruction that does multiple assignments, like [#local x=1 x=2].
 * Each assignment is represented by a {@link Assignment} child element.
 * If there's only one assignment, its usually just a {@link Assignment} without parent {@link AssignmentInstruction}.
 */
final class AssignmentInstruction extends TemplateElement {

    private int scope;
    private Expression namespaceExp;

    AssignmentInstruction(int scope) {
        this.scope = scope;
        nestedElements = new ArrayList(1);
    }

    void addAssignment(Assignment ass) {
        nestedElements.add(ass);
    }
    
    void setNamespaceExp(Expression namespaceExp) {
        this.namespaceExp = namespaceExp;
        for (int i=0; i<nestedElements.size();i++) {
            ((Assignment) nestedElements.get(i)).setNamespaceExp(namespaceExp);
        }
    }

    void accept(Environment env) throws TemplateException, IOException {
        for (int i = 0; i<nestedElements.size(); i++) {
            Assignment ass = (Assignment) nestedElements.get(i);
            env.visit(ass);
        }
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        if (canonical) buf.append('<');
        buf.append(Assignment.getDirectiveName(scope));
        if (canonical) {
            buf.append(' ');
            for (int i = 0; i<nestedElements.size(); i++) {
                Assignment ass = (Assignment) nestedElements.get(i);
                buf.append(ass.getCanonicalForm());
                if (i < nestedElements.size() -1) {
                    buf.append(" ");
                }
            }
        } else {
            buf.append("-container");
        }
        if (namespaceExp != null) {
            buf.append(" in ");
            buf.append(namespaceExp.getCanonicalForm());
        }
        if (canonical) buf.append("/>");
        return buf.toString();
    }
    
    int getParameterCount() {
        return 2;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return new Integer(scope);
        case 1: return namespaceExp;
        default: return null;
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.VARIABLE_SCOPE;
        case 1: return ParameterRole.NAMESPACE;
        default: return null;
        }
    }
    
    String getNodeTypeSymbol() {
        return Assignment.getDirectiveName(scope);
    }

    public TemplateElement postParseCleanup(boolean stripWhitespace) throws ParseException {
        super.postParseCleanup(stripWhitespace);
        if (nestedElements.size() == 1) {
            Assignment ass = (Assignment) nestedElements.get(0);
            ass.setLocation(getTemplate(), this, this);
            return ass;
        } 
        return this;
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

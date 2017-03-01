/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import java.io.IOException;

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
        setChildBufferCapacity(1);
    }

    void addAssignment(Assignment assignment) {
        addChild(assignment);
    }
    
    void setNamespaceExp(Expression namespaceExp) {
        this.namespaceExp = namespaceExp;
        int ln = getChildCount();
        for (int i = 0; i < ln; i++) {
            ((Assignment) getChild(i)).setNamespaceExp(namespaceExp);
        }
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        return getChildBuffer();
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(Assignment.getDirectiveName(scope));
        if (canonical) {
            buf.append(' ');
            int ln = getChildCount();
            for (int i = 0; i < ln; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                Assignment assignment = (Assignment) getChild(i);
                buf.append(assignment.getCanonicalForm());
            }
        } else {
            buf.append("-container");
        }
        if (namespaceExp != null) {
            buf.append(" in ");
            buf.append(namespaceExp.getCanonicalForm());
        }
        if (canonical) buf.append(">");
        return buf.toString();
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return Integer.valueOf(scope);
        case 1: return namespaceExp;
        default: return null;
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.VARIABLE_SCOPE;
        case 1: return ParameterRole.NAMESPACE;
        default: return null;
        }
    }
    
    @Override
    String getNodeTypeSymbol() {
        return Assignment.getDirectiveName(scope);
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

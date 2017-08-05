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

/**
 * Represents a case in a switch statement.
 */
final class Case extends TemplateElement {

    static final int TYPE_CASE = 0;
    static final int TYPE_DEFAULT = 1;
    
    Expression condition;

    Case(Expression matchingValue, TemplateElements children) {
        this.condition = matchingValue;
        setChildren(children);
    }

    @Override
    TemplateElement[] accept(Environment env) {
        return getChildBuffer();
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (condition != null) {
            sb.append(' ');
            sb.append(condition.getCanonicalForm());
        }
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return condition != null ? "#case" : "#default";
    }

    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return condition;
        case 1: return Integer.valueOf(condition != null ? TYPE_CASE : TYPE_DEFAULT);
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.CONDITION;
        case 1: return ParameterRole.AST_NODE_SUBTYPE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
        
}

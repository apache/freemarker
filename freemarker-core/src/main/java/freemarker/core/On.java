/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package freemarker.core;

import java.util.List;

/**
 * Represents an "on" in a switch statement.
 * This is alternative to case that does not fall-though
 * and instead supports multiple conditions.
 */
final class On extends TemplateElement {

    List<Expression> conditions;

    On(List<Expression> matchingValues, TemplateElements children) {
        this.conditions = matchingValues;
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
        for (int i = 0; i < conditions.size(); i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(' ');
            sb.append((conditions.get(i)).getCanonicalForm());
        }
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
        }
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#on";
    }

    @Override
    int getParameterCount() {
        return conditions.size();
    }

    @Override
    Object getParameterValue(int idx) {
        checkIndex(idx);
        return conditions.get(idx);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return ParameterRole.CONDITION;
    }

    private void checkIndex(int idx) {
        if (conditions == null || idx >= conditions.size()) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

}

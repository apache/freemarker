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

import freemarker.core.IteratorBlock.IterationContext;
import freemarker.template.TemplateException;

/**
 * An #items element.
 */
class Items extends TemplateElement {

    private final String loopVarName;
    private final String loopVar2Name;

    /**
     * @param loopVar2Name
     *            For non-hash listings always {@code null}, for hash listings {@code loopVarName} and
     *            {@code loopVarName2} holds the key- and value loop variable names.
     */
    Items(String loopVarName, String loopVar2Name, TemplateElements children) {
        this.loopVarName = loopVarName;
        this.loopVar2Name = loopVar2Name;
        setChildren(children);
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        final IterationContext iterCtx = env.findClosestEnclosingIterationContext();
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new _MiscTemplateException(env,
                    getNodeTypeSymbol(), " without iteration in context");
        }
        
        iterCtx.loopForItemsElement(env, getChildBuffer(), loopVarName, loopVar2Name);
        return null;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return true;
    }

    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        sb.append(" as ");
        sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVarName));
        if (loopVar2Name != null) {
            sb.append(", ");
            sb.append(_CoreStringUtils.toFTLTopLevelIdentifierReference(loopVar2Name));
        }
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</");
            sb.append(getNodeTypeSymbol());
            sb.append('>');
        }
        return sb.toString();
    }

    @Override
    String getNodeTypeSymbol() {
        return "#items";
    }

    @Override
    int getParameterCount() {
        return loopVar2Name != null ? 2 : 1;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return loopVarName;
        case 1:
            if (loopVar2Name == null) throw new IndexOutOfBoundsException();
            return loopVar2Name;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0:
            if (loopVarName == null) throw new IndexOutOfBoundsException();
            return ParameterRole.TARGET_LOOP_VARIABLE;
        case 1:
            if (loopVar2Name == null) throw new IndexOutOfBoundsException();
            return ParameterRole.TARGET_LOOP_VARIABLE;
        default: throw new IndexOutOfBoundsException();
        }
    }

}

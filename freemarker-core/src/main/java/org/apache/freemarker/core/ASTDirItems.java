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
package org.apache.freemarker.core;

import java.io.IOException;

import org.apache.freemarker.core.ASTDirList.IterationContext;
import org.apache.freemarker.core.util._StringUtils;

/**
 * AST directive node: {@code #items}
 */
class ASTDirItems extends ASTDirective {

    private final String nestedContentParamName;
    private final String nestedContentParam2Name;

    /**
     * @param nestedContentParam2Name
     *            For non-hash listings always {@code null}, for hash listings {@code nestedContentParamName} and
     *            {@code nestedContentParamName2} holds the key- and value nested content parameter names.
     */
    ASTDirItems(String nestedContentParamName, String nestedContentParam2Name, TemplateElements children) {
        this.nestedContentParamName = nestedContentParamName;
        this.nestedContentParam2Name = nestedContentParam2Name;
        setChildren(children);
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        final IterationContext iterCtx = env.findClosestEnclosingIterationContext();
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new TemplateException(env,
                    getLabelWithoutParameters(), " without iteration in context");
        }
        
        iterCtx.loopForItemsElement(env, getChildBuffer(), nestedContentParamName, nestedContentParam2Name);
        return null;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return true;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        sb.append(" as ");
        sb.append(_StringUtils.toFTLTopLevelIdentifierReference(nestedContentParamName));
        if (nestedContentParam2Name != null) {
            sb.append(", ");
            sb.append(_StringUtils.toFTLTopLevelIdentifierReference(nestedContentParam2Name));
        }
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</");
            sb.append(getLabelWithoutParameters());
            sb.append('>');
        }
        return sb.toString();
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#items";
    }

    @Override
    int getParameterCount() {
        return nestedContentParam2Name != null ? 2 : 1;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0:
            if (nestedContentParamName == null) throw new IndexOutOfBoundsException();
            return nestedContentParamName;
        case 1:
            if (nestedContentParam2Name == null) throw new IndexOutOfBoundsException();
            return nestedContentParam2Name;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0:
            if (nestedContentParamName == null) throw new IndexOutOfBoundsException();
            return ParameterRole.NESTED_CONTENT_PARAMETER;
        case 1:
            if (nestedContentParam2Name == null) throw new IndexOutOfBoundsException();
            return ParameterRole.NESTED_CONTENT_PARAMETER;
        default: throw new IndexOutOfBoundsException();
        }
    }

}

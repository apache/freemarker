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

/**
 * AST directive node: {@code #sep}.
 */
class ASTDirSep extends ASTDirective {

    public ASTDirSep(TemplateElements children) {
        setChildren(children);
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        final IterationContext iterCtx = ASTDirList.findEnclosingIterationContext(env, null);
        if (iterCtx == null) {
            // The parser should prevent this situation
            throw new TemplateException(env,
                    getLabelWithoutParameters(), " without iteration in context");
        }
        
        if (iterCtx.hasNext()) {
            return getChildBuffer();
        }
        return null;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
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
        return "#sep";
    }

    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

}

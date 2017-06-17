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

/**
 * AST directive node: {@code #else} inside a {@code  #list}.
 */
final class ASTDirElseOfList extends ASTDirective {
    
    ASTDirElseOfList(TemplateElements children) {
        setChildren(children);
    }

    @Override
    ASTElement[] accept(Environment env) throws TemplateException, IOException {
        return getChildBuffer();
    }

    @Override
    protected String dump(boolean canonical) {
        if (canonical) {
            StringBuilder buf = new StringBuilder();
            buf.append('<').append(getASTNodeDescriptor()).append('>');
            buf.append(getChildrenCanonicalForm());            
            return buf.toString();
        } else {
            return getASTNodeDescriptor();
        }
    }

    @Override
    String getASTNodeDescriptor() {
        return "#else";
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

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

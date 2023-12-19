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
 * AST directive node: When a {@code #list} has an {@code #else}, this is the parent of the two nodes.
 */
class ASTDirListElseContainer extends ASTDirective {

    private final ASTDirList listPart;
    private final ASTDirElseOfList elsePart;

    public ASTDirListElseContainer(ASTDirList listPart, ASTDirElseOfList elsePart) {
        setChildBufferCapacity(2);
        addChild(listPart);
        addChild(elsePart);
        this.listPart = listPart;
        this.elsePart = elsePart;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        boolean hadItems;

        TemplateProcessingTracer templateProcessingTracer = env.getTemplateProcessingTracer();
        if (templateProcessingTracer == null) {
            hadItems = listPart.acceptWithResult(env);
        } else {
            templateProcessingTracer.enterElement(env, listPart);
            try {
                hadItems = listPart.acceptWithResult(env);
            } finally {
                templateProcessingTracer.exitElement(env);
            }
        }

        if (hadItems) {
            return null;
        }

        return new ASTElement[] { elsePart };
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    @Override
    String dump(boolean canonical) {
        if (canonical) {
            StringBuilder buf = new StringBuilder();
            int ln = getChildCount();
            for (int i = 0; i < ln; i++) {
                ASTElement element = fastGetChild(i);
                buf.append(element.dump(canonical));
            }
            buf.append("</#list>");
            return buf.toString();
        } else {
            return getLabelWithoutParameters();
        }
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#list-#else-container";
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

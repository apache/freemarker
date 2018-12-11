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

import org.apache.freemarker.core.util.BugException;

/**
 * AST directive node: An element that represents a conditionally executed block: {@code #if}, {@code #elseIf} or
 * {@code #elseIf}. Note that when an {@code #if} has related {@code #elseIf}-s or {@code #else}, an
 * {@link ASTDirIfElseIfElseContainer} parent must be used. For a lonely {@code #if}, no such parent is needed. 
 */
final class ASTDirIfOrElseOrElseIf extends ASTDirective {

    static final int TYPE_IF = 0;
    static final int TYPE_ELSE = 1;
    static final int TYPE_ELSE_IF = 2;
    
    final ASTExpression condition;
    private final int type;

    ASTDirIfOrElseOrElseIf(ASTExpression condition, TemplateElements children, int type) {
        this.condition = condition;
        setChildren(children);
        this.type = type;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        if (condition == null || condition.evalToBoolean(env)) {
            return getChildBuffer();
        }
        return null;
    }
    
    @Override
    String dump(boolean canonical) {
        StringBuilder buf = new StringBuilder();
        if (canonical) buf.append('<');
        buf.append(getLabelWithoutParameters());
        if (condition != null) {
            buf.append(' ');
            buf.append(condition.getCanonicalForm());
        }
        if (canonical) {
            buf.append(">");
            buf.append(getChildrenCanonicalForm());
            if (!(getParent() instanceof ASTDirIfElseIfElseContainer)) {
                buf.append("</#if>");
            }
        }
        return buf.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        if (type == TYPE_ELSE) {
            return "#else";
        } else if (type == TYPE_IF) {
            return "#if";
        } else if (type == TYPE_ELSE_IF) {
            return "#elseIf";
        } else {
            throw new BugException("Unknown type");
        }
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return condition;
        case 1: return Integer.valueOf(type);
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

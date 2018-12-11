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

/**
 * AST directive node: {@code #t}, {@code #tr}, {@code #tl}.
 */
final class ASTDirTOrRtOrLtOrNt extends ASTDirective {
    
    private static final int TYPE_T = 0;
    private static final int TYPE_LT = 1;
    private static final int TYPE_RT = 2;
    private static final int TYPE_NT = 3;

    final boolean left, right;

    ASTDirTOrRtOrLtOrNt(boolean left, boolean right) {
        this.left = left;
        this.right = right;
    }

    @Override
    ASTElement[] execute(Environment env) {
        // This instruction does nothing at render-time, only parse-time.
        return null;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        if (canonical) sb.append("/>");
        return sb.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
        if (left && right) {
            return "#t";
        } else if (left) {
            return "#lt";
        } else if (right) {
            return "#rt";
        } else {
            return "#nt";
        }
    }
    
    @Override
    boolean isIgnorable(boolean stripWhitespace) {
        return true;
    }

    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        int type;
        if (left && right) {
            type = TYPE_T;
        } else if (left) {
            type = TYPE_LT;
        } else if (right) {
            type = TYPE_RT;
        } else {
            type = TYPE_NT;
        }
        return Integer.valueOf(type);
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.AST_NODE_SUBTYPE;
    }

    @Override
    boolean isOutputCacheable() {
        return true;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

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
 * AST directive node: {@code #return}.
 */
final class ASTDirReturn extends ASTDirective {

    private ASTExpression exp;

    ASTDirReturn(ASTExpression exp) {
        this.exp = exp;
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException {
        if (exp != null) {
            env.setLastReturnValue(exp.eval(env));
        }
        if (nextSibling() == null && getParent() instanceof ASTDirMacroOrFunction) {
            // Avoid unnecessary exception throwing 
            return null;
        }
        throw Return.INSTANCE;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        if (exp != null) {
            sb.append(' ');
            sb.append(exp.getCanonicalForm());
        }
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#return";
    }
    
    public static class Return extends FlowControlException {
        static final Return INSTANCE = new Return();
        private Return() {
        }
    }
    
    @Override
    int getParameterCount() {
        return 1;
    }

    @Override
    Object getParameterValue(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return exp;
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        if (idx != 0) throw new IndexOutOfBoundsException();
        return ParameterRole.VALUE;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

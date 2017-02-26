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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.TemplateTransformModel;
import org.apache.freemarker.core.model.impl.SimpleScalar;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;

/**
 * AST directive node: Like {@code <#local x>...</#local>}.
 */
final class ASTDirCapturingAssignment extends ASTDirective {

    private final String varName;
    private final ASTExpression namespaceExp;
    private final int scope;
    private final MarkupOutputFormat<?> markupOutputFormat;

    ASTDirCapturingAssignment(TemplateElements children, String varName, int scope, ASTExpression namespaceExp, MarkupOutputFormat<?> markupOutputFormat) {
        setChildren(children);
        this.varName = varName;
        this.namespaceExp = namespaceExp;
        this.scope = scope;
        this.markupOutputFormat = markupOutputFormat;
    }

    @Override
    ASTElement[] accept(Environment env) throws TemplateException, IOException {
        ASTElement[] children = getChildBuffer();
        if (children != null) {
            env.visitAndTransform(children, new CaptureOutput(env), null);
        } else {
            TemplateModel value = capturedStringToModel("");
            if (namespaceExp != null) {
                Environment.Namespace ns = (Environment.Namespace) namespaceExp.eval(env);
                ns.put(varName, value);
            } else if (scope == ASTDirAssignment.NAMESPACE) {
                env.setVariable(varName, value);
            } else if (scope == ASTDirAssignment.GLOBAL) {
                env.setGlobalVariable(varName, value);
            } else if (scope == ASTDirAssignment.LOCAL) {
                env.setLocalVariable(varName, value);
            }
        }
        return null;
    }

    private TemplateModel capturedStringToModel(String s) throws TemplateModelException {
        return markupOutputFormat == null ? new SimpleScalar(s) : markupOutputFormat.fromMarkup(s);
    }

    private class CaptureOutput implements TemplateTransformModel {
        private final Environment env;
        private final Environment.Namespace fnsModel;
        
        CaptureOutput(Environment env) throws TemplateException {
            this.env = env;
            TemplateModel nsModel = null;
            if (namespaceExp != null) {
                nsModel = namespaceExp.eval(env);
                if (!(nsModel instanceof Environment.Namespace)) {
                    throw new NonNamespaceException(namespaceExp, nsModel, env);
                }
            }
            fnsModel = (Environment.Namespace ) nsModel; 
        }
        
        @Override
        public Writer getWriter(Writer out, Map args) {
            return new StringWriter() {
                @Override
                public void close() throws IOException {
                    TemplateModel result;
                    try {
                        result = capturedStringToModel(toString());
                    } catch (TemplateModelException e) {
                        // [Java 1.6] e to cause
                        throw new IOException("Failed to create FTL value from captured string: " + e);
                    }
                    switch(scope) {
                        case ASTDirAssignment.NAMESPACE: {
                            if (fnsModel != null) {
                                fnsModel.put(varName, result);
                            } else {
                                env.setVariable(varName, result);
                            }
                            break;
                        }
                        case ASTDirAssignment.LOCAL: {
                            env.setLocalVariable(varName, result);
                            break;
                        }
                        case ASTDirAssignment.GLOBAL: {
                            env.setGlobalVariable(varName, result);
                            break;
                        }
                    }
                }
            };
        }
    }
    
    @Override
    protected String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append("<");
        sb.append(getNodeTypeSymbol());
        sb.append(' ');
        sb.append(varName);
        if (namespaceExp != null) {
            sb.append(" in ");
            sb.append(namespaceExp.getCanonicalForm());
        }
        if (canonical) {
            sb.append('>');
            sb.append(getChildrenCanonicalForm());
            sb.append("</");
            sb.append(getNodeTypeSymbol());
            sb.append('>');
        } else {
            sb.append(" = .nested_output");
        }
        return sb.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return ASTDirAssignment.getDirectiveName(scope);
    }
    
    @Override
    int getParameterCount() {
        return 3;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return varName;
        case 1: return Integer.valueOf(scope);
        case 2: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ASSIGNMENT_TARGET;
        case 1: return ParameterRole.VARIABLE_SCOPE;
        case 2: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }
}

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

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleString;
import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.util.BugException;

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
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        ASTElement[] children = getChildBuffer();

        TemplateModel capturedValue;
        if (children != null) {
            StringWriter out = new StringWriter();
            Writer prevOut = env.getOut();
            try {
                env.setOut(out);
                env.executeElements(children);
            } finally {
                env.setOut(prevOut);
            }
            capturedValue = capturedStringToModel(out.toString());
        } else {
            capturedValue = capturedStringToModel("");
        }

        if (namespaceExp != null) {
            final Environment.Namespace namespace;
            TemplateModel uncheckedNamespace = namespaceExp.eval(env);
            try {
                namespace = (Environment.Namespace) uncheckedNamespace;
            } catch (ClassCastException e) {
                throw MessageUtils.newUnexpectedOperandTypeException(
                        namespaceExp, uncheckedNamespace,
                        "namespace",
                        new Class[] { Environment.Namespace.class },
                        null, env);
            }
            if (namespace == null) {
                throw InvalidReferenceException.getInstance(namespaceExp, env);
            }
            namespace.put(varName, capturedValue);
        } else if (scope == ASTDirAssignment.NAMESPACE) {
            env.setVariable(varName, capturedValue);
        } else if (scope == ASTDirAssignment.GLOBAL) {
            env.setGlobalVariable(varName, capturedValue);
        } else if (scope == ASTDirAssignment.LOCAL) {
            env.setLocalVariable(varName, capturedValue);
        } else {
            throw new BugException("Unhandled scope");
        }

        return null;
    }

    private TemplateModel capturedStringToModel(String s) throws TemplateException {
        return markupOutputFormat == null ? new SimpleString(s) : markupOutputFormat.fromMarkup(s);
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append("<");
        sb.append(getLabelWithoutParameters());
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
            sb.append(getLabelWithoutParameters());
            sb.append('>');
        } else {
            sb.append(" = .nestedOutput");
        }
        return sb.toString();
    }
    
    @Override
    public String getLabelWithoutParameters() {
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

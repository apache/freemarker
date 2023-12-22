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
import java.io.StringWriter;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Like [#local x]...[/#local].
 */
final class BlockAssignment extends TemplateElement {

    private final String varName;
    private final Expression namespaceExp;
    private final int scope;
    private final MarkupOutputFormat<?> markupOutputFormat;

    BlockAssignment(TemplateElements children, String varName, int scope, Expression namespaceExp, MarkupOutputFormat<?> markupOutputFormat) {
        setChildren(children);
        this.varName = varName;
        this.namespaceExp = namespaceExp;
        this.scope = scope;
        this.markupOutputFormat = markupOutputFormat;
    }

    @Override
    TemplateElement[] accept(Environment env) throws TemplateException, IOException {
        TemplateElement[] children = getChildBuffer();
        
        TemplateModel value;
        if (children != null) {
            StringWriter out = new StringWriter();
            env.visit(children, out);
            value = capturedStringToModel(out.toString());
        } else {
            value = capturedStringToModel("");
        }
        
        if (namespaceExp != null) {
            final Environment.Namespace namespace;
            TemplateModel uncheckedNamespace = namespaceExp.eval(env);
            try {
                namespace = (Environment.Namespace) uncheckedNamespace;
            } catch (ClassCastException e) {
                throw new NonNamespaceException(namespaceExp, uncheckedNamespace, env);
            }
            if (namespace == null) {
                throw InvalidReferenceException.getInstance(namespaceExp, env);
            }
            namespace.put(varName, value);
        } else if (scope == Assignment.NAMESPACE) {
            env.setVariable(varName, value);
        } else if (scope == Assignment.GLOBAL) {
            env.setGlobalVariable(varName, value);
        } else if (scope == Assignment.LOCAL) {
            env.setLocalVariable(varName, value);
        } else {
            throw new BugException("Unhandled scope");
        }
        
        return null;
    }

    private TemplateModel capturedStringToModel(String s) throws TemplateModelException {
        return markupOutputFormat == null ? new SimpleScalar(s) : markupOutputFormat.fromMarkup(s);
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
        return Assignment.getDirectiveName(scope);
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

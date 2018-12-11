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

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNodeModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;


/**
 * AST directive node: {@code #visit}.
 */
final class ASTDirVisit extends ASTDirective {
    
    ASTExpression targetNode, namespaces;
    
    ASTDirVisit(ASTExpression targetNode, ASTExpression namespaces) {
        this.targetNode = targetNode;
        this.namespaces = namespaces;
    }

    @Override
    ASTElement[] execute(Environment env) throws IOException, TemplateException {
        TemplateModel node = targetNode.eval(env);
        if (!(node instanceof TemplateNodeModel)) {
            throw MessageUtils.newUnexpectedOperandTypeException(targetNode, node, TemplateNodeModel.class, env);
        }
        
        TemplateModel nss = namespaces == null ? null : namespaces.eval(env);
        if (namespaces instanceof ASTExpStringLiteral) {
            nss = env.importLib(((TemplateStringModel) nss).getAsString(), null);
        } else if (namespaces instanceof ASTExpListLiteral) {
            nss = ((ASTExpListLiteral) namespaces).evaluateStringsToNamespaces(env);
        }
        if (nss != null) {
            if (nss instanceof Environment.Namespace) {
                NativeSequence ss = new NativeSequence(1);
                ss.add(nss);
                nss = ss;
            } else if (!(nss instanceof TemplateSequenceModel)) {
                if (namespaces != null) {
                    throw MessageUtils.newUnexpectedOperandTypeException(
                            namespaces, nss, TemplateSequenceModel.class, env);
                } else {
                    // Should not occur
                    throw new TemplateException(env, "Expecting a sequence of namespaces after \"using\"");
                }
            }
        }
        env.invokeNodeHandlerFor((TemplateNodeModel) node, (TemplateSequenceModel) nss);
        return null;
    }

    @Override
    String dump(boolean canonical) {
        StringBuilder sb = new StringBuilder();
        if (canonical) sb.append('<');
        sb.append(getLabelWithoutParameters());
        sb.append(' ');
        sb.append(targetNode.getCanonicalForm());
        if (namespaces != null) {
            sb.append(" using ");
            sb.append(namespaces.getCanonicalForm());
        }
        if (canonical) sb.append("/>");
        return sb.toString();
    }

    @Override
    public String getLabelWithoutParameters() {
        return "#visit";
    }
    
    @Override
    int getParameterCount() {
        return 2;
    }

    @Override
    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return targetNode;
        case 1: return namespaces;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.NODE;
        case 1: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    @Override
    boolean isNestedBlockRepeater() {
        return true;
    }

    @Override
    boolean isShownInStackTrace() {
        return true;
    }
    
}

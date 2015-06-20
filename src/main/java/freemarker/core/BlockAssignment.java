/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateTransformModel;

/**
 * Like [#local x]...[/#local].
 */
final class BlockAssignment extends TemplateElement {

    private final String varName;
    private final Expression namespaceExp;
    private final int scope;

    BlockAssignment(TemplateElement nestedBlock, String varName, int scope, Expression namespaceExp) {
        setNestedBlock(nestedBlock);
        this.varName = varName;
        this.namespaceExp = namespaceExp;
        this.scope = scope;
    }

    void accept(Environment env) throws TemplateException, IOException {
        if (getNestedBlock() != null) {
            env.visitAndTransform(getNestedBlock(), new CaptureOutput(env), null);
        } else {
			TemplateModel value = new SimpleScalar("");
			if (namespaceExp != null) {
				Environment.Namespace ns = (Environment.Namespace) namespaceExp.eval(env);
				ns.put(varName, value);
 			} else if (scope == Assignment.NAMESPACE) {
				env.setVariable(varName, value);
			} else if (scope == Assignment.GLOBAL) {
				env.setGlobalVariable(varName, value);
			} else if (scope == Assignment.LOCAL) {
				env.setLocalVariable(varName, value);
			}
		}
    }

    private class CaptureOutput implements TemplateTransformModel {
        private final Environment env;
        private final Environment.Namespace fnsModel;
        
        CaptureOutput(Environment env) throws TemplateException {
            this.env = env;
            TemplateModel nsModel = null;
            if(namespaceExp != null) {
                nsModel = namespaceExp.eval(env);
                if (!(nsModel instanceof Environment.Namespace)) {
                    throw new NonNamespaceException(namespaceExp, nsModel, env);
                }
            }
            fnsModel = (Environment.Namespace )nsModel; 
        }
        
        public Writer getWriter(Writer out, Map args) {
            return new StringWriter() {
                public void close() {
                    SimpleScalar result = new SimpleScalar(toString());
                    switch(scope) {
                        case Assignment.NAMESPACE: {
                            if(fnsModel != null) {
                                fnsModel.put(varName, result);
                            }
                            else {
                                env.setVariable(varName, result);
                            }
                            break;
                        }
                        case Assignment.LOCAL: {
                            env.setLocalVariable(varName, result);
                            break;
                        }
                        case Assignment.GLOBAL: {
                            env.setGlobalVariable(varName, result);
                            break;
                        }
                    }
                }
            };
        }
    }
    
    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
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
            sb.append(getNestedBlock() == null ? "" : getNestedBlock().getCanonicalForm());
            sb.append("</");
            sb.append(getNodeTypeSymbol());
            sb.append('>');
        } else {
            sb.append(" = .nested_output");
        }
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return Assignment.getDirectiveName(scope);
    }
    
    int getParameterCount() {
        return 3;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return varName;
        case 1: return new Integer(scope);
        case 2: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ASSIGNMENT_TARGET;
        case 1: return ParameterRole.VARIABLE_SCOPE;
        case 2: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
}

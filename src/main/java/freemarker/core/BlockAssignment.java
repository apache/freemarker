/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
        this.nestedBlock = nestedBlock;
        this.varName = varName;
        this.namespaceExp = namespaceExp;
        this.scope = scope;
    }

    void accept(Environment env) throws TemplateException, IOException {
        if (nestedBlock != null) {
            env.visitAndTransform(nestedBlock, new CaptureOutput(env), null);
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
                    throw new UnexpectedTypeException(namespaceExp, nsModel, "namespace", env);
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
            sb.append(nestedBlock == null ? "" : nestedBlock.getCanonicalForm());
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

    boolean isIgnorable() {
        return false;
    }
}

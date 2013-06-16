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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * An instruction that processes the nested block within a macro instruction.
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
final class BodyInstruction extends TemplateElement {
    
    
    private List bodyParameters;
    
    
    BodyInstruction(List bodyParameters) {
        this.bodyParameters = bodyParameters;
    }
    
    List getBodyParameters() {
        return bodyParameters;
    }

    /**
     * There is actually a subtle but essential point in the code below.
     * A macro operates in the context in which it's defined. However, 
     * a nested block within a macro instruction is defined in the 
     * context in which the macro was invoked. So, we actually need to
     * temporarily switch the namespace and macro context back to
     * what it was before macro invocation to implement this properly.
     * I (JR) realized this thanks to some incisive comments from Daniel Dekany.
     */
    void accept(Environment env) throws IOException, TemplateException {
        Context bodyContext = new Context(env);
        env.visit(bodyContext);
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (bodyParameters != null) {
            for (int i = 0; i<bodyParameters.size(); i++) {
                sb.append(' ');
                sb.append(bodyParameters.get(i));
            }
        }
        if (canonical) sb.append('>');
        return sb.toString();
    }
    
    String getNodeTypeSymbol() {
        return "#nested";
    }
    
    int getParameterCount() {
        return bodyParameters != null ? bodyParameters.size() : 0;
    }

    Object getParameterValue(int idx) {
        checkIndex(idx);
        return bodyParameters.get(idx);
    }

    ParameterRole getParameterRole(int idx) {
        checkIndex(idx);
        return ParameterRole.PASSED_VALUE;
    }

    private void checkIndex(int idx) {
        if (bodyParameters == null || idx >= bodyParameters.size()) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    /*
    boolean heedsOpeningWhitespace() {
        return true;
    }

    boolean heedsTrailingWhitespace() {
        return true;
    }
    */
    
    class Context implements LocalContext {
        Macro.Context invokingMacroContext;
        Environment.Namespace bodyVars;
        
        Context(Environment env) throws TemplateException {
            invokingMacroContext = env.getCurrentMacroContext();
            List bodyParameterNames = invokingMacroContext.bodyParameterNames;
            if (bodyParameters != null) {
                for (int i=0; i<bodyParameters.size(); i++) {
                    Expression exp = (Expression) bodyParameters.get(i);
                    TemplateModel tm = exp.eval(env);
                    if (bodyParameterNames != null && i < bodyParameterNames.size()) {
                        String bodyParameterName = (String) bodyParameterNames.get(i);
                        if (bodyVars == null) {
                            bodyVars = env.new Namespace();
                        }
                        bodyVars.put(bodyParameterName, tm);
                    }
                }
            }
        }
        
        public TemplateModel getLocalVariable(String name) throws TemplateModelException {
            return bodyVars == null ? null : bodyVars.get(name);
        }
        
        public Collection getLocalVariableNames() {
            List bodyParameterNames = invokingMacroContext.bodyParameterNames;
            return bodyParameterNames == null ? Collections.EMPTY_LIST : bodyParameterNames;
        }
    }
}

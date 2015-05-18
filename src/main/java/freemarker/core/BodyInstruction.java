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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * An instruction that processes the nested block within a macro instruction.
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
        env.invokeNestedContent(bodyContext);
    }

    protected String dump(boolean canonical) {
        StringBuffer sb = new StringBuffer();
        if (canonical) sb.append('<');
        sb.append(getNodeTypeSymbol());
        if (bodyParameters != null) {
            for (int i = 0; i<bodyParameters.size(); i++) {
                sb.append(' ');
                sb.append(((Expression) bodyParameters.get(i)).getCanonicalForm());
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
            List bodyParameterNames = invokingMacroContext.nestedContentParameterNames;
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
            List bodyParameterNames = invokingMacroContext.nestedContentParameterNames;
            return bodyParameterNames == null ? Collections.EMPTY_LIST : bodyParameterNames;
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
}

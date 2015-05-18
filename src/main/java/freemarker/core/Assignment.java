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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * An instruction that makes a single assignment, like [#local x=1].
 * This is also used as the child of {@link AssignmentInstruction}, if there are multiple assignments in the same tag,
 * like in [#local x=1 x=2].
 */
final class Assignment extends TemplateElement {

    private String variableName;
    private Expression value, namespaceExp;
    private int/*enum*/ scope;

    static final int NAMESPACE = 1;
    static final int LOCAL = 2;
    static final int GLOBAL = 3;

    /**
     * @param variableName the variable name to assign to.
     * @param value the expression to assign.
     * @param scope the scope of the assignment, one of NAMESPACE, LOCAL, or GLOBAL
     */
    Assignment(String variableName, 
               Expression value, 
               int scope)
    {
        this.variableName = variableName;
        this.value = value;
        this.scope = scope;
    }
    
    void setNamespaceExp(Expression namespaceExp) {
        this.namespaceExp =  namespaceExp;
    }

    void accept(Environment env) throws TemplateException {
        Environment.Namespace namespace = null;
        if (namespaceExp != null) {
            TemplateModel namespaceTM = namespaceExp.eval(env);
            try {
                namespace = (Environment.Namespace) namespaceTM;
            } catch (ClassCastException e) {
                throw new NonNamespaceException(namespaceExp, namespaceTM, env);
            }
            if (namespace == null) {
                throw InvalidReferenceException.getInstance(namespaceExp, env);
            }
        }
        
        TemplateModel tm = value.eval(env);
        if (tm == null) {
            if (env.isClassicCompatible()) {
                tm = TemplateScalarModel.EMPTY_STRING;
            }
            else {
                throw InvalidReferenceException.getInstance(value, env);
            }
        }
        if (scope == LOCAL) {
            env.setLocalVariable(variableName, tm);
        }
        else {
            if (namespace == null) {
                if (scope == GLOBAL) {
                    namespace = env.getGlobalNamespace();
                }
                else if (scope == NAMESPACE) {
                    namespace = env.getCurrentNamespace();
                }
                else {
                    throw new BugException("Unexpected scope type: " + scope);
                }
            }
            namespace.put(variableName, tm);
        }
    }

    protected String dump(boolean canonical) {
        StringBuffer buf = new StringBuffer();
        String dn = parent instanceof AssignmentInstruction ? null : getNodeTypeSymbol();
        if (dn != null) {
            if (canonical) buf.append("<");
            buf.append(dn);
            buf.append(' ');
        }
        
        buf.append(_CoreStringUtils.toFTLTopLevelTragetIdentifier(variableName));
        
        buf.append(" = ");
        buf.append(value.getCanonicalForm());
        if (dn != null) {
            if (namespaceExp != null) {
                buf.append(" in ");
                buf.append(namespaceExp.getCanonicalForm());
            }
            if (canonical) buf.append(">");
        }
        String result = buf.toString();
        return result;
    }
    
    String getNodeTypeSymbol() {
        return getDirectiveName(scope);
    }
    
    static String getDirectiveName(int scope) {
        if (scope == Assignment.LOCAL) {
            return "#local";
        } else if (scope == Assignment.GLOBAL) {
            return "#global";
        } else if (scope == Assignment.NAMESPACE) {
            return "#assign";
        } else {
            return "#{unknown_assignment_type}";
        }
    }
    
    int getParameterCount() {
        return 4;
    }

    Object getParameterValue(int idx) {
        switch (idx) {
        case 0: return variableName;
        case 1: return value;
        case 2: return new Integer(scope);
        case 3: return namespaceExp;
        default: throw new IndexOutOfBoundsException();
        }
    }

    ParameterRole getParameterRole(int idx) {
        switch (idx) {
        case 0: return ParameterRole.ASSIGNMENT_TARGET;
        case 1: return ParameterRole.ASSIGNMENT_SOURCE;
        case 2: return ParameterRole.VARIABLE_SCOPE;
        case 3: return ParameterRole.NAMESPACE;
        default: throw new IndexOutOfBoundsException();
        }
    }

    boolean isNestedBlockRepeater() {
        return false;
    }
    
}

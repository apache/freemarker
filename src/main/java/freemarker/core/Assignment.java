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
                throw new UnexpectedTypeException(namespaceExp, namespaceTM, "namespace", env);
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
                    throw new RuntimeException("Unexpected scope type: " + scope);
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
        buf.append (variableName);
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
    
}

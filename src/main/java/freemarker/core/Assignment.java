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

import freemarker.template.*;

/**
 * An instruction that assigns a literal or reference, to a single-identifier
 * variable.
 */
final class Assignment extends TemplateElement {

    private String variableName;
    private Expression value, namespaceExp;
    private int scope;

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
        TemplateModel tm = value.getAsTemplateModel(env);
        Environment.Namespace namespace = null;
        if (namespaceExp != null) {
            boolean oops = false;
            try {
                namespace = (Environment.Namespace) namespaceExp.getAsTemplateModel(env);
            } catch (ClassCastException cce) {
                oops = true;
            }
            if (oops || namespace==null) {
                throw new InvalidReferenceException(getStartLocation() + "\nInvalid reference to namespace: " + namespaceExp, env);
            }
        }
        if (tm == null) {
            if (env.isClassicCompatible()) {
                tm = TemplateScalarModel.EMPTY_STRING;
            }
            else {
                String msg = "Error " + getStartLocation()
                            +"\n" + value + " is undefined."
                            +"\nIt cannot be assigned to " + variableName;
                throw new InvalidReferenceException(msg, env);
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
            }
            namespace.put(variableName, tm);
        }
    }

    public String getCanonicalForm() {
        StringBuilder buf = new StringBuilder();
        if (!(parent instanceof AssignmentInstruction)) {
            if (scope == LOCAL) {
                buf.append("<#local ");
            }
            else if (scope ==GLOBAL) {
                buf.append("<#global ");
            }
            else {
                buf.append("<#assign ");
            }
        }
        buf.append (variableName);
        buf.append('=');
        buf.append(value.getCanonicalForm());
        if (!(parent instanceof AssignmentInstruction)) {
            if (namespaceExp != null) {
                buf.append(" in ");
                buf.append(namespaceExp.getCanonicalForm());
            }
            buf.append(">");
        }
        return buf.toString();
    }

    public String getDescription() {
        String s ="";
        if (!(parent instanceof AssignmentInstruction)) {
            s = "assignment: ";
            if (scope == LOCAL) {
                s = "local " + s;
            }
            else if (scope == GLOBAL) {
                s  = "global " + s;
            }
        }
        return s + variableName 
               + "=" 
               + value; 
    }
}

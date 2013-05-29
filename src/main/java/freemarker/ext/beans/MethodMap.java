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

package freemarker.ext.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Iterator;
import java.util.List;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * Used instead of {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor} for overloaded methods
 * and constructors.
 */
class MethodMap
{
    private final BeansWrapper wrapper;
    private final OverloadedMethod fixArgMethod = new OverloadedFixArgMethod();
    private OverloadedMethod varArgMethod;
    
    MethodMap(BeansWrapper wrapper) {
        this.wrapper = wrapper;
    }
    
    BeansWrapper getWrapper() {
        return wrapper;
    }
    
    void addMember(Member member) {
        fixArgMethod.addMember(member);
        if(MethodUtilities.isVarArgs(member)) {
            if(varArgMethod == null) {
                varArgMethod = new OverloadedVarArgMethod();
            }
            varArgMethod.addMember(member);
        }
    }
    
    MemberAndArguments getMemberAndArguments(List arguments) 
    throws TemplateModelException {
        Object memberAndArguments = fixArgMethod.getMemberAndArguments(arguments, wrapper);
        if(memberAndArguments == OverloadedMethod.NO_SUCH_METHOD) {
            if(varArgMethod != null) {
                memberAndArguments = varArgMethod.getMemberAndArguments(arguments, wrapper);
            }
            if(memberAndArguments == OverloadedMethod.NO_SUCH_METHOD) {
                throw new TemplateModelException(
                        "No compatible overloaded variation was found for the signature deducated from the actual " +
                        "parameter values: " + getDeducedCallSignature(arguments)
                        + ". The available overloaded variations are: " + memberListToString());
            }
        }
        if(memberAndArguments == OverloadedMethod.AMBIGUOUS_METHOD) {
            throw new TemplateModelException(
                    "Multiple compatible overloaded variation was found for the signature deducated from the actual " +
                    "parameter values:\n" + getDeducedCallSignature(arguments)
                    + "\nThe available overloaded variations are (including non-matching):\n" + memberListToString());
        }
        return (MemberAndArguments)memberAndArguments;
    }
    
    private String memberListToString() {
        Iterator fixArgMethods = fixArgMethod.getMembers();
        Iterator varArgMethods = varArgMethod != null ? varArgMethod.getMembers() : null;
        
        boolean hasMethods = fixArgMethods.hasNext() || (varArgMethods != null && varArgMethods.hasNext()); 
        if (hasMethods) {
            StringBuffer sb = new StringBuffer();
            while (fixArgMethods.hasNext()) {
                if (sb.length() != 0) sb.append(",\n");
                sb.append(methodOrConstructorToString((Member) fixArgMethods.next()));
            }
            if (varArgMethods != null) {
                while (varArgMethods.hasNext()) {
                    if (sb.length() != 0) sb.append(",\n");
                    sb.append(methodOrConstructorToString((Member) varArgMethods.next()));
                }
            }
            return sb.toString();
        } else {
            return "No members";
        }
    }
    
    /**
     * The description of the signature deduced from the method/constructor call, used in error messages.
     */
    private String getDeducedCallSignature(List arguments) {
        final Member firstMember;
        Iterator fixArgMethods = fixArgMethod.getMembers();
        if (fixArgMethods.hasNext()) {
            firstMember = (Member) fixArgMethods.next();
        } else {
            Iterator varArgMethods = varArgMethod != null ? varArgMethod.getMembers() : null;
            if (varArgMethods != null && varArgMethods.hasNext()) {
                firstMember = (Member) varArgMethods.next();
            } else {
                firstMember = null;
            }
        }
        
        StringBuffer sb = new StringBuffer();
        if (firstMember != null) {
            if (firstMember instanceof Constructor) {
                sb.append("constructor ");
            } else {
                sb.append("method ");
            }
            sb.append(firstMember.getName());
        } else {
            sb.append("???");
        }
        
        sb.append('(');
        for (int i = 0; i < arguments.size(); i++) {
            if (i != 0) sb.append(", ");
            sb.append(ClassUtil.getFTLTypeDescription((TemplateModel) arguments.get(i)));
        }
        sb.append(')');
        
        return sb.toString();
        
    }

    /**
     * Detailed method/constructor description for parameter list error messages.
     */
    private String methodOrConstructorToString(Member member) {
        StringBuffer sb = new StringBuffer();
        
        String className = ClassUtil.getShortClassName(member.getDeclaringClass());
        if (className != null) {
            sb.append(className);
            sb.append('.');
        }
        sb.append(member.getName());

        sb.append('(');
        Class[] paramTypes = MethodUtilities.getParameterTypes(member);
        for (int i = 0; i < paramTypes.length; i++) {
            if (i != 0) sb.append(", ");
            sb.append(ClassUtil.getShortClassName(paramTypes[i]));
        }
        sb.append(')');
        
        return sb.toString();
    }
    
}

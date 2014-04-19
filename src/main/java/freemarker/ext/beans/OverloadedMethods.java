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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import freemarker.core.BugException;
import freemarker.core._DelayedConversionToString;
import freemarker.core._TemplateModelException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * Used instead of {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor} for overloaded methods
 * and constructors.
 */
final class OverloadedMethods {
    
    private final OverloadedMethodsSubset fixArgMethods;
    private OverloadedMethodsSubset varargMethods;
    private final boolean bugfixed;
    
    OverloadedMethods(boolean bugfixed) {
        this.bugfixed = bugfixed;
        fixArgMethods = new OverloadedFixArgsMethods(bugfixed);
    }
    
    void addMethod(Method member) {
        final Class[] paramTypes = member.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(member, paramTypes));
    }

    void addConstructor(Constructor member) {
        final Class[] paramTypes = member.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(member, paramTypes));
    }
    
    private void addCallableMemberDescriptor(ReflectionCallableMemberDescriptor memberDesc) {
        fixArgMethods.addCallableMemberDescriptor(memberDesc);
        if (memberDesc.isVarargs()) {
            if (varargMethods == null) {
                varargMethods = new OverloadedVarArgsMethods(bugfixed);
            }
            varargMethods.addCallableMemberDescriptor(memberDesc);
        }
    }
    
    MemberAndArguments getMemberAndArguments(List/*<TemplateModel>*/ tmArgs, BeansWrapper unwrapper) 
    throws TemplateModelException {
        MaybeEmptyMemberAndArguments fixArgsRes = null;
        MaybeEmptyMemberAndArguments varargsRes = null;
        if ((fixArgsRes = fixArgMethods.getMemberAndArguments(tmArgs, unwrapper)) instanceof MemberAndArguments) {
            return (MemberAndArguments) fixArgsRes;
        } else if (varargMethods != null
                && (varargsRes = varargMethods.getMemberAndArguments(tmArgs, unwrapper))
                        instanceof MemberAndArguments) {
            return (MemberAndArguments) varargsRes;
        } else {
            MaybeEmptyMemberAndArguments res = getClosestToSuccess(fixArgsRes, varargsRes);
            if (res == EmptyMemberAndArguments.NO_SUCH_METHOD) {
                throw new _TemplateModelException(new Object[] {
                        "No compatible overloaded variation was found for the signature deducated from the actual "
                        + "parameter values:\n", getDeducedCallSignature(tmArgs),
                        "\nThe available overloaded variations are:\n", memberListToString() });
            } else if (res == EmptyMemberAndArguments.AMBIGUOUS_METHOD) {
                throw new _TemplateModelException(new Object[] {
                        "Multiple compatible overloaded variation was found for the signature deducated from the ",
                        "actual parameter values:\n", getDeducedCallSignature(tmArgs),
                        "\nThe available overloaded variations are (including non-matching):\n",
                        memberListToString() });
            } else {
                throw new BugException("Unsupported EmptyMemberAndArguments: " + res); 
            }
        }
    }

    private MaybeEmptyMemberAndArguments getClosestToSuccess(MaybeEmptyMemberAndArguments res1,
            MaybeEmptyMemberAndArguments res2) {
        if (res1 == null) return res2;
        if (res2 == null) return res1;
        if (res1 == EmptyMemberAndArguments.AMBIGUOUS_METHOD || res2 == EmptyMemberAndArguments.AMBIGUOUS_METHOD) {
            return EmptyMemberAndArguments.AMBIGUOUS_METHOD; 
        }
        if (res1 == EmptyMemberAndArguments.NO_SUCH_METHOD || res2 == EmptyMemberAndArguments.NO_SUCH_METHOD) {
            return EmptyMemberAndArguments.NO_SUCH_METHOD; 
        }
        throw new BugException("Unhandled: " + res1 + " and " + res2);
    }

    private _DelayedConversionToString memberListToString() {
        return new _DelayedConversionToString(null) {
            
            protected String doConversion(Object obj) {
                Iterator fixArgMethodsIter = fixArgMethods.getMemberDescriptors();
                Iterator varargMethodsIter = varargMethods != null ? varargMethods.getMemberDescriptors() : null;
                
                boolean hasMethods = fixArgMethodsIter.hasNext() || (varargMethodsIter != null && varargMethodsIter.hasNext()); 
                if (hasMethods) {
                    StringBuffer sb = new StringBuffer();
                    HashSet fixArgMethods = new HashSet();
                    if (fixArgMethodsIter != null) {
                        
                        while (fixArgMethodsIter.hasNext()) {
                            if (sb.length() != 0) sb.append(",\n");
                            sb.append("    ");
                            CallableMemberDescriptor callableMemberDesc = (CallableMemberDescriptor) fixArgMethodsIter.next();
                            fixArgMethods.add(callableMemberDesc);
                            sb.append(callableMemberDesc.getDeclaration());
                        }
                    }
                    if (varargMethodsIter != null) {
                        while (varargMethodsIter.hasNext()) {
                            CallableMemberDescriptor callableMemberDesc = (CallableMemberDescriptor) varargMethodsIter.next();
                            if (!fixArgMethods.contains(callableMemberDesc)) {
                                if (sb.length() != 0) sb.append(",\n");
                                sb.append("    ");
                                sb.append(callableMemberDesc.getDeclaration());
                            }
                        }
                    }
                    return sb.toString();
                } else {
                    return "No members";
                }
            }
            
        };
    }
    
    /**
     * The description of the signature deduced from the method/constructor call, used in error messages.
     */
    private _DelayedConversionToString getDeducedCallSignature(List arguments) {
        final String[] argumentTypeDescs = new String[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            argumentTypeDescs[i] = ClassUtil.getFTLTypeDescription((TemplateModel) arguments.get(i));
        }
        
        return new _DelayedConversionToString(null) {

            protected String doConversion(Object obj) {
                final CallableMemberDescriptor firstMemberDesc;
                Iterator fixArgMethodsIter = fixArgMethods.getMemberDescriptors();
                if (fixArgMethodsIter.hasNext()) {
                    firstMemberDesc = (CallableMemberDescriptor) fixArgMethodsIter.next();
                } else {
                    Iterator varArgMethods = varargMethods != null ? varargMethods.getMemberDescriptors() : null;
                    if (varArgMethods != null && varArgMethods.hasNext()) {
                        firstMemberDesc = (CallableMemberDescriptor) varArgMethods.next();
                    } else {
                        firstMemberDesc = null;
                    }
                }
                
                StringBuffer sb = new StringBuffer();
                if (firstMemberDesc != null) {
                    if (firstMemberDesc.isConstructor()) {
                        sb.append("constructor ");
                    } else {
                        sb.append("method ");
                    }
                    sb.append(firstMemberDesc.getName());
                } else {
                    sb.append("???");
                }
                
                sb.append('(');
                for (int i = 0; i < argumentTypeDescs.length; i++) {
                    if (i != 0) sb.append(", ");
                    sb.append(argumentTypeDescs[i]);
                }
                sb.append(')');
                
                return sb.toString();
            }
            
        };
    }

}

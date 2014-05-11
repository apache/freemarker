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

import freemarker.core._DelayedConversionToString;
import freemarker.core._ErrorDescriptionBuilder;
import freemarker.core._TemplateModelException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * Used instead of {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor} for overloaded methods and
 * constructors.
 * 
 * <p>After the initialization with the {@link #addMethod(Method)} and {@link #addConstructor(Constructor)} calls are
 * done, the instance must be thread-safe. Before that, it's the responsibility of the caller of those methods to
 * ensure that the object is properly publishing to other threads.
 */
final class OverloadedMethods {

    private final OverloadedMethodsSubset fixArgMethods;
    private OverloadedMethodsSubset varargMethods;
    private final boolean bugfixed;
    
    OverloadedMethods(boolean bugfixed) {
        this.bugfixed = bugfixed;
        fixArgMethods = new OverloadedFixArgsMethods(bugfixed);
    }
    
    void addMethod(Method method) {
        final Class[] paramTypes = method.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(method, paramTypes));
    }

    void addConstructor(Constructor constr) {
        final Class[] paramTypes = constr.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(constr, paramTypes));
    }
    
    private void addCallableMemberDescriptor(ReflectionCallableMemberDescriptor memberDesc) {
        // Note: "varargs" methods are always callable as fixed args, with a sequence (array) as the last parameter.
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
        // Try to find a fixed args match:
        MaybeEmptyMemberAndArguments fixArgsRes = fixArgMethods.getMemberAndArguments(tmArgs, unwrapper);
        if (fixArgsRes instanceof MemberAndArguments) {
            return (MemberAndArguments) fixArgsRes;
        }

        // Try to find a varargs match:
        MaybeEmptyMemberAndArguments varargsRes;
        if (varargMethods != null) {
            varargsRes = varargMethods.getMemberAndArguments(tmArgs, unwrapper);
            if (varargsRes instanceof MemberAndArguments) {
                return (MemberAndArguments) varargsRes;
            }
        } else {
            varargsRes = null;
        }
        
        _ErrorDescriptionBuilder edb = new _ErrorDescriptionBuilder(new Object[] {
                toCompositeErrorMessage(
                        (EmptyMemberAndArguments) fixArgsRes,
                        (EmptyMemberAndArguments) varargsRes,
                        tmArgs),
                "\nThe matching overload was searched among these members:\n",
                memberListToString()});
        if (!bugfixed) {
            edb.tip("You seem to use BeansWrapper in 2.3.0-compatible mode. If you think this error is unfounded, "
                    + "enabling 2.3.21 fixes may helps. See version history for more.");
        }
        throw new _TemplateModelException(edb);
    }

    private Object[] toCompositeErrorMessage(final EmptyMemberAndArguments fixArgsEmptyRes, final EmptyMemberAndArguments varargsEmptyRes,
            List tmArgs) {
        final Object[] argsErrorMsg;
        if (varargsEmptyRes != null) {
            if (fixArgsEmptyRes == null || fixArgsEmptyRes.isNumberOfArgumentsWrong()) {
                argsErrorMsg = toErrorMessage(varargsEmptyRes, tmArgs);
            } else {
                argsErrorMsg = new Object[] {
                        "When trying to call the non-varargs overloads:\n",
                        toErrorMessage(fixArgsEmptyRes, tmArgs),
                        "\nWhen trying to call the varargs overloads:\n",
                        toErrorMessage(varargsEmptyRes, null)
                };
            }
        } else {
            argsErrorMsg = toErrorMessage(fixArgsEmptyRes, tmArgs);
        }
        return argsErrorMsg;
    }

    private Object[] toErrorMessage(EmptyMemberAndArguments res, List/*<TemplateModel>*/ tmArgs) {
        final Object[] unwrappedArgs = res.getUnwrappedArguments();
        return new Object[] {
                res.getErrorDescription(),
                tmArgs != null
                        ? new Object[] {
                                "\nThe FTL type of the argument values were: ", getTMActualParameterTypes(tmArgs), "." }
                        : (Object) "",
                unwrappedArgs != null
                        ? new Object[] {
                                "\nThe Java type of the argument values were: ",
                                getUnwrappedActualParameterTypes(unwrappedArgs) + "." }
                        : (Object) ""};
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
    
    private _DelayedConversionToString getTMActualParameterTypes(List arguments) {
        final String[] argumentTypeDescs = new String[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            argumentTypeDescs[i] = ClassUtil.getFTLTypeDescription((TemplateModel) arguments.get(i));
        }
        
        return new DelayedCallSignatureToString(argumentTypeDescs) {

            String argumentToString(Object argType) {
                return (String) argType;
            }
            
        };
    }
    
    private Object getUnwrappedActualParameterTypes(Object[] unwrappedArgs) {
        final Class[] argumentTypes = new Class[unwrappedArgs.length];
        for (int i = 0; i < unwrappedArgs.length; i++) {
            Object unwrappedArg = unwrappedArgs[i];
            argumentTypes[i] = unwrappedArg != null ? unwrappedArg.getClass() : null;
        }
        
        return new DelayedCallSignatureToString(argumentTypes) {

            String argumentToString(Object argType) {
                return argType != null
                        ? ClassUtil.getShortClassName((Class) argType)
                        : ClassUtil.getShortClassNameOfObject(null);
            }
            
        };
    }
    
    private abstract class DelayedCallSignatureToString extends _DelayedConversionToString {

        public DelayedCallSignatureToString(Object[] argTypeArray) {
            super(argTypeArray);
        }

        protected String doConversion(Object obj) {
            Object[] argTypes = (Object[]) obj;
            
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < argTypes.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(argumentToString(argTypes[i]));
            }
            
            return sb.toString();
        }
        
        abstract String argumentToString(Object argType);
        
    }

}

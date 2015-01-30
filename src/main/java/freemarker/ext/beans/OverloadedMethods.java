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
            edb.tip("You seem to use BeansWrapper with incompatibleImprovements set below 2.3.21. If you think this "
                    + "error is unfounded, enabling 2.3.21 fixes may helps. See version history for more.");
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
                final Iterator fixArgMethodsIter = fixArgMethods.getMemberDescriptors();
                final Iterator varargMethodsIter = varargMethods != null ? varargMethods.getMemberDescriptors() : null;
                
                boolean hasMethods = fixArgMethodsIter.hasNext() || (varargMethodsIter != null && varargMethodsIter.hasNext()); 
                if (hasMethods) {
                    StringBuffer sb = new StringBuffer();
                    HashSet fixArgMethods = new HashSet();
                    while (fixArgMethodsIter.hasNext()) {
                        if (sb.length() != 0) sb.append(",\n");
                        sb.append("    ");
                        CallableMemberDescriptor callableMemberDesc = (CallableMemberDescriptor) fixArgMethodsIter.next();
                        fixArgMethods.add(callableMemberDesc);
                        sb.append(callableMemberDesc.getDeclaration());
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

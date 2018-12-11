/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._DelayedConversionToString;
import org.apache.freemarker.core._ErrorDescriptionBuilder;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.TemplateLanguageUtils;
import org.apache.freemarker.core.util._ClassUtils;

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
    private String methodName;
    private Class methodDeclaringClass;

    OverloadedMethods() {
        fixArgMethods = new OverloadedFixArgsMethods();
    }
    
    void addMethod(Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(method, paramTypes));
        if (methodName == null) {
            methodName = method.getName();
        }
        Class<?> newMethodDeclaringClass = method.getDeclaringClass();
        if (methodDeclaringClass == null
                || newMethodDeclaringClass != methodDeclaringClass
                        && methodDeclaringClass.isAssignableFrom(newMethodDeclaringClass)) {
            methodDeclaringClass = newMethodDeclaringClass;
        }
    }

    void addConstructor(Constructor<?> constr) {
        final Class<?>[] paramTypes = constr.getParameterTypes();
        addCallableMemberDescriptor(new ReflectionCallableMemberDescriptor(constr, paramTypes));
        if (methodName == null) {
            methodName = constr.getName();
            methodDeclaringClass = constr.getDeclaringClass();
        }
    }
    
    private void addCallableMemberDescriptor(ReflectionCallableMemberDescriptor memberDesc) {
        // Note: "varargs" methods are always callable as fixed args, with a sequence (array) as the last parameter.
        fixArgMethods.addCallableMemberDescriptor(memberDesc);
        if (memberDesc.isVarargs()) {
            if (varargMethods == null) {
                varargMethods = new OverloadedVarArgsMethods();
            }
            varargMethods.addCallableMemberDescriptor(memberDesc);
        }
    }
    
    MemberAndArguments getMemberAndArguments(TemplateModel[] tmArgs, DefaultObjectWrapper unwrapper)
    throws TemplateException {
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
        
        _ErrorDescriptionBuilder edb = new _ErrorDescriptionBuilder(
                toCompositeErrorMessage(
                        (EmptyMemberAndArguments) fixArgsRes,
                        (EmptyMemberAndArguments) varargsRes,
                        tmArgs),
                "\nThe matching overload was searched among these members:\n",
                memberListToString());
        addMarkupBITipAfterNoNoMarchIfApplicable(edb, tmArgs);
        throw new TemplateException(edb);
    }

    String getMethodName() {
        return methodName;
    }

    Class getMethodDeclaringClass() {
        return methodDeclaringClass;
    }

    private Object[] toCompositeErrorMessage(
            final EmptyMemberAndArguments fixArgsEmptyRes, final EmptyMemberAndArguments varargsEmptyRes,
            TemplateModel[] tmArgs) {
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

    private Object[] toErrorMessage(EmptyMemberAndArguments res, TemplateModel[] tmArgs) {
        final Object[] unwrappedArgs = res.getUnwrappedArguments();
        return new Object[] {
                res.getErrorDescription(),
                tmArgs != null
                        ? new Object[] {
                                "\nThe FTL type of the argument values were: ", getTMActualParameterTypes(tmArgs), "." }
                        : "",
                unwrappedArgs != null
                        ? new Object[] {
                                "\nThe Java type of the argument values were: ",
                                getUnwrappedActualParameterTypes(unwrappedArgs) + "." }
                        : ""};
    }

    private _DelayedConversionToString memberListToString() {
        return new _DelayedConversionToString(null) {
            
            @Override
            protected String doConversion(Object obj) {
                final Iterator<ReflectionCallableMemberDescriptor> fixArgMethodsIter
                        = fixArgMethods.getMemberDescriptors();
                final Iterator<ReflectionCallableMemberDescriptor> varargMethodsIter
                        = varargMethods != null ? varargMethods.getMemberDescriptors() : null;
                
                boolean hasMethods = fixArgMethodsIter.hasNext()
                        || (varargMethodsIter != null && varargMethodsIter.hasNext());
                if (hasMethods) {
                    StringBuilder sb = new StringBuilder();
                    HashSet<CallableMemberDescriptor> fixArgMethods = new HashSet<>();
                    while (fixArgMethodsIter.hasNext()) {
                        if (sb.length() != 0) sb.append(",\n");
                        sb.append("    ");
                        CallableMemberDescriptor callableMemberDesc = fixArgMethodsIter.next();
                        fixArgMethods.add(callableMemberDesc);
                        sb.append(callableMemberDesc.getDeclaration());
                    }
                    if (varargMethodsIter != null) {
                        while (varargMethodsIter.hasNext()) {
                            CallableMemberDescriptor callableMemberDesc = varargMethodsIter.next();
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
     * Adds tip to the error message if converting a {@link TemplateMarkupOutputModel} argument to {@link String} might
     * allows finding a matching overload. 
     */
    private void addMarkupBITipAfterNoNoMarchIfApplicable(_ErrorDescriptionBuilder edb,
            TemplateModel[] tmArgs) {
        for (int argIdx = 0; argIdx < tmArgs.length; argIdx++) {
            TemplateModel tmArg = tmArgs[argIdx];
            if (tmArg instanceof TemplateMarkupOutputModel) {
                for (Iterator<ReflectionCallableMemberDescriptor> membDescs = fixArgMethods.getMemberDescriptors();
                        membDescs.hasNext(); ) {
                    CallableMemberDescriptor membDesc = membDescs.next();
                    Class<?>[] paramTypes = membDesc.getParamTypes();
                    
                    Class<?> paramType = null;
                    if (membDesc.isVarargs() && argIdx >= paramTypes.length - 1) {
                        paramType = paramTypes[paramTypes.length - 1];
                        if (paramType.isArray()) {
                            paramType = paramType.getComponentType();
                        }
                    }
                    if (paramType == null && argIdx < paramTypes.length) {
                        paramType = paramTypes[argIdx];
                    }
                    if (paramType != null) {
                        if (paramType.isAssignableFrom(String.class) && !paramType.isAssignableFrom(tmArg.getClass())) {
                            edb.tip(SimpleJavaMethodModel.MARKUP_OUTPUT_TO_STRING_TIP);
                            return;
                        }
                    }
                }
            }
        }
    }

    private _DelayedConversionToString getTMActualParameterTypes(TemplateModel[] args) {
        final String[] argumentTypeDescs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            argumentTypeDescs[i] = TemplateLanguageUtils.getTypeDescription(args[i]);
        }
        
        return new DelayedCallSignatureToString(argumentTypeDescs) {

            @Override
            String argumentToString(Object argType) {
                return (String) argType;
            }
            
        };
    }
    
    private Object getUnwrappedActualParameterTypes(Object[] unwrappedArgs) {
        final Class<?>[] argumentTypes = new Class<?>[unwrappedArgs.length];
        for (int i = 0; i < unwrappedArgs.length; i++) {
            Object unwrappedArg = unwrappedArgs[i];
            argumentTypes[i] = unwrappedArg != null ? unwrappedArg.getClass() : null;
        }
        
        return new DelayedCallSignatureToString(argumentTypes) {

            @Override
            String argumentToString(Object argType) {
                return argType != null
                        ? _ClassUtils.getShortClassName((Class<?>) argType)
                        : _ClassUtils.getShortClassNameOfObject(null);
            }
            
        };
    }
    
    private abstract class DelayedCallSignatureToString extends _DelayedConversionToString {

        DelayedCallSignatureToString(Object[] argTypeArray) {
            super(argTypeArray);
        }

        @Override
        protected String doConversion(Object obj) {
            Object[] argTypes = (Object[]) obj;
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < argTypes.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(argumentToString(argTypes[i]));
            }
            
            return sb.toString();
        }
        
        abstract String argumentToString(Object argType);
        
    }

}

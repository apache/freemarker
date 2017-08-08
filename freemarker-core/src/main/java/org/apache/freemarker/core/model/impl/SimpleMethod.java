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

import java.lang.reflect.Array;
import java.lang.reflect.Member;

import org.apache.freemarker.core._CallableUtils;
import org.apache.freemarker.core._DelayedTemplateLanguageTypeDescription;
import org.apache.freemarker.core._DelayedOrdinal;
import org.apache.freemarker.core._ErrorDescriptionBuilder;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.util._ClassUtils;

/**
 * This class is used as a base for non-overloaded method models and for constructors.
 * (For overloaded methods and constructors see {@link OverloadedMethods}.)
 */
class SimpleMethod {
    
    static final String MARKUP_OUTPUT_TO_STRING_TIP
            = "A markup output value can be converted to markup string like value?markupString. "
              + "But consider if the Java method whose argument it will be can handle markup strings properly.";
    
    private final Member member;
    private final Class<?>[] argTypes;
    
    SimpleMethod(Member member, Class<?>[] argTypes) {
        this.member = member;
        this.argTypes = argTypes;
    }
    
    Object[] unwrapArguments(TemplateModel[] args, DefaultObjectWrapper wrapper) throws TemplateModelException {
        if (args == null) {
            args = _CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY;
        }
        boolean isVarArg = _MethodUtils.isVarargs(member);
        int typesLen = argTypes.length;
        if (isVarArg) {
            if (typesLen - 1 > args.length) {
                throw new _TemplateModelException(
                        _MethodUtils.invocationErrorMessageStart(member),
                        " takes at least ", typesLen - 1,
                        typesLen - 1 == 1 ? " argument" : " arguments", ", but ",
                        args.length, " was given.");
            }
        } else if (typesLen != args.length) {
            throw new _TemplateModelException(
                    _MethodUtils.invocationErrorMessageStart(member),
                    " takes ", typesLen, typesLen == 1 ? " argument" : " arguments", ", but ",
                    args.length, " was given.");
        }
         
        return unwrapArguments(args, argTypes, isVarArg, wrapper);
    }

    private Object[] unwrapArguments(TemplateModel[] args, Class<?>[] argTypes, boolean isVarargs,
            DefaultObjectWrapper w)
    throws TemplateModelException {
        if (args == null) return null;
        
        int typesLen = argTypes.length;
        int argsLen = args.length;
        
        Object[] unwrappedArgs = new Object[typesLen];
        
        // Unwrap arguments:
        int argsIdx = 0;
        int normalArgCnt = isVarargs ? typesLen - 1 : typesLen; 
        int unwrappedArgsIdx = 0;
        while (unwrappedArgsIdx < normalArgCnt) {
            Class<?> argType = argTypes[unwrappedArgsIdx];
            TemplateModel argVal = args[argsIdx++];
            Object unwrappedArgVal = w.tryUnwrapTo(argVal, argType);
            if (unwrappedArgVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                throw createArgumentTypeMismatchException(unwrappedArgsIdx, argVal, argType);
            }
            if (unwrappedArgVal == null && argType.isPrimitive()) {
                throw createNullToPrimitiveArgumentException(unwrappedArgsIdx, argType);
            }
            
            unwrappedArgs[unwrappedArgsIdx++] = unwrappedArgVal;
        }
        if (isVarargs) {
            // The last argType, which is the vararg type, wasn't processed yet.
            
            Class<?> varargType = argTypes[typesLen - 1];
            Class<?> varargItemType = varargType.getComponentType();
            if (argsIdx >= args.length) {
                unwrappedArgs[unwrappedArgsIdx] = Array.newInstance(varargItemType, 0);
            } else {
                TemplateModel argVal = args[argsIdx++];
                
                Object unwrappedArgVal;
                // We first try to treat the last argument as a vararg *array*.
                // This is consistent to what OverloadedVarArgMethod does.
                if (argsLen - unwrappedArgsIdx == 1
                        && (unwrappedArgVal = w.tryUnwrapTo(argVal, varargType))
                            != ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    // It was a vararg array.
                    unwrappedArgs[unwrappedArgsIdx] = unwrappedArgVal;
                } else {
                    // It wasn't a vararg array, so we assume it's a vararg
                    // array *item*, possibly followed by further ones.
                    int varargArrayLen = argsLen - unwrappedArgsIdx;
                    Object varargArray = Array.newInstance(varargItemType, varargArrayLen);
                    for (int varargIdx = 0; varargIdx < varargArrayLen; varargIdx++) {
                        TemplateModel varargVal = varargIdx == 0 ? argVal : args[argsIdx++];
                        Object unwrappedVarargVal = w.tryUnwrapTo(varargVal, varargItemType);
                        if (unwrappedVarargVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                            throw createArgumentTypeMismatchException(
                                    unwrappedArgsIdx + varargIdx, varargVal, varargItemType);
                        }
                        
                        if (unwrappedVarargVal == null && varargItemType.isPrimitive()) {
                            throw createNullToPrimitiveArgumentException(unwrappedArgsIdx + varargIdx, varargItemType);
                        }
                        Array.set(varargArray, varargIdx, unwrappedVarargVal);
                    }
                    unwrappedArgs[unwrappedArgsIdx] = varargArray;
                }
            }
        }
        
        return unwrappedArgs;
    }

    private TemplateModelException createArgumentTypeMismatchException(
            int argIdx, TemplateModel argVal, Class<?> targetType) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                _MethodUtils.invocationErrorMessageStart(member), " couldn't be called: Can't convert the ",
                new _DelayedOrdinal(argIdx + 1),
                " argument's value to the target Java type, ", _ClassUtils.getShortClassName(targetType),
                ". The type of the actual value was: ", new _DelayedTemplateLanguageTypeDescription(argVal));
        if (argVal instanceof TemplateMarkupOutputModel && (targetType.isAssignableFrom(String.class))) {
            desc.tip(MARKUP_OUTPUT_TO_STRING_TIP);
        }
        return new _TemplateModelException(desc);
    }

    private TemplateModelException createNullToPrimitiveArgumentException(int argIdx, Class<?> targetType) {
        return new _TemplateModelException(
                _MethodUtils.invocationErrorMessageStart(member), " couldn't be called: The value of the ",
                new _DelayedOrdinal(argIdx + 1),
                " argument was null, but the target Java parameter type (", _ClassUtils.getShortClassName(targetType),
                ") is primitive and so can't store null.");
    }
    
    protected Member getMember() {
        return member;
    }
}
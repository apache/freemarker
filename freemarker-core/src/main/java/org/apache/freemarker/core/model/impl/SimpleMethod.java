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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.freemarker.core._DelayedFTLTypeDescription;
import org.apache.freemarker.core._DelayedOrdinal;
import org.apache.freemarker.core._ErrorDescriptionBuilder;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateMarkupOutputModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.util._ClassUtil;

/**
 * This class is used for as a base for non-overloaded method models and for constructors.
 * (For overloaded methods and constructors see {@link OverloadedMethods}.)
 */
class SimpleMethod {
    
    static final String MARKUP_OUTPUT_TO_STRING_TIP
            = "A markup output value can be converted to markup string like value?markupString. "
              + "But consider if the Java method whose argument it will be can handle markup strings properly.";
    
    private final Member member;
    private final Class[] argTypes;
    
    protected SimpleMethod(Member member, Class[] argTypes) {
        this.member = member;
        this.argTypes = argTypes;
    }
    
    Object[] unwrapArguments(List arguments, DefaultObjectWrapper wrapper) throws TemplateModelException {
        if (arguments == null) {
            arguments = Collections.EMPTY_LIST;
        }
        boolean isVarArg = _MethodUtil.isVarargs(member);
        int typesLen = argTypes.length;
        if (isVarArg) {
            if (typesLen - 1 > arguments.size()) {
                throw new _TemplateModelException(
                        _MethodUtil.invocationErrorMessageStart(member),
                        " takes at least ", Integer.valueOf(typesLen - 1),
                        typesLen - 1 == 1 ? " argument" : " arguments", ", but ",
                        Integer.valueOf(arguments.size()), " was given.");
            }
        } else if (typesLen != arguments.size()) {
            throw new _TemplateModelException(
                    _MethodUtil.invocationErrorMessageStart(member), 
                    " takes ", Integer.valueOf(typesLen), typesLen == 1 ? " argument" : " arguments", ", but ",
                    Integer.valueOf(arguments.size()), " was given.");
        }
         
        return unwrapArguments(arguments, argTypes, isVarArg, wrapper);
    }

    private Object[] unwrapArguments(List args, Class[] argTypes, boolean isVarargs,
            DefaultObjectWrapper w)
    throws TemplateModelException {
        if (args == null) return null;
        
        int typesLen = argTypes.length;
        int argsLen = args.size();
        
        Object[] unwrappedArgs = new Object[typesLen];
        
        // Unwrap arguments:
        Iterator it = args.iterator();
        int normalArgCnt = isVarargs ? typesLen - 1 : typesLen; 
        int argIdx = 0;
        while (argIdx < normalArgCnt) {
            Class argType = argTypes[argIdx];
            TemplateModel argVal = (TemplateModel) it.next();
            Object unwrappedArgVal = w.tryUnwrapTo(argVal, argType);
            if (unwrappedArgVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                throw createArgumentTypeMismarchException(argIdx, argVal, argType);
            }
            if (unwrappedArgVal == null && argType.isPrimitive()) {
                throw createNullToPrimitiveArgumentException(argIdx, argType); 
            }
            
            unwrappedArgs[argIdx++] = unwrappedArgVal;
        }
        if (isVarargs) {
            // The last argType, which is the vararg type, wasn't processed yet.
            
            Class varargType = argTypes[typesLen - 1];
            Class varargItemType = varargType.getComponentType();
            if (!it.hasNext()) {
                unwrappedArgs[argIdx++] = Array.newInstance(varargItemType, 0);
            } else {
                TemplateModel argVal = (TemplateModel) it.next();
                
                Object unwrappedArgVal;
                // We first try to treat the last argument as a vararg *array*.
                // This is consistent to what OverloadedVarArgMethod does.
                if (argsLen - argIdx == 1
                        && (unwrappedArgVal = w.tryUnwrapTo(argVal, varargType))
                            != ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                    // It was a vararg array.
                    unwrappedArgs[argIdx++] = unwrappedArgVal;
                } else {
                    // It wasn't a vararg array, so we assume it's a vararg
                    // array *item*, possibly followed by further ones.
                    int varargArrayLen = argsLen - argIdx;
                    Object varargArray = Array.newInstance(varargItemType, varargArrayLen);
                    for (int varargIdx = 0; varargIdx < varargArrayLen; varargIdx++) {
                        TemplateModel varargVal = (TemplateModel) (varargIdx == 0 ? argVal : it.next());
                        Object unwrappedVarargVal = w.tryUnwrapTo(varargVal, varargItemType);
                        if (unwrappedVarargVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
                            throw createArgumentTypeMismarchException(
                                    argIdx + varargIdx, varargVal, varargItemType);
                        }
                        
                        if (unwrappedVarargVal == null && varargItemType.isPrimitive()) {
                            throw createNullToPrimitiveArgumentException(argIdx + varargIdx, varargItemType); 
                        }
                        Array.set(varargArray, varargIdx, unwrappedVarargVal);
                    }
                    unwrappedArgs[argIdx++] = varargArray;
                }
            }
        }
        
        return unwrappedArgs;
    }

    private TemplateModelException createArgumentTypeMismarchException(
            int argIdx, TemplateModel argVal, Class targetType) {
        _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                _MethodUtil.invocationErrorMessageStart(member), " couldn't be called: Can't convert the ",
                new _DelayedOrdinal(Integer.valueOf(argIdx + 1)),
                " argument's value to the target Java type, ", _ClassUtil.getShortClassName(targetType),
                ". The type of the actual value was: ", new _DelayedFTLTypeDescription(argVal));
        if (argVal instanceof TemplateMarkupOutputModel && (targetType.isAssignableFrom(String.class))) {
            desc.tip(MARKUP_OUTPUT_TO_STRING_TIP);
        }
        return new _TemplateModelException(desc);
    }

    private TemplateModelException createNullToPrimitiveArgumentException(int argIdx, Class targetType) {
        return new _TemplateModelException(
                _MethodUtil.invocationErrorMessageStart(member), " couldn't be called: The value of the ",
                new _DelayedOrdinal(Integer.valueOf(argIdx + 1)),
                " argument was null, but the target Java parameter type (", _ClassUtil.getShortClassName(targetType),
                ") is primitive and so can't store null.");
    }
    
    protected Member getMember() {
        return member;
    }
}
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

import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._DelayedOrdinal;
import freemarker.core._TemplateModelException;
import freemarker.template.ObjectWrapperAndUnwrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * This class is used for as a base for non-overloaded method models and for constructors.
 * (For overloaded methods and constructors see {@link OverloadedMethods}.)
 */
class SimpleMethod
{
    private final Member member;
    private final Class[] argTypes;
    
    protected SimpleMethod(Member member, Class[] argTypes)
    {
        this.member = member;
        this.argTypes = argTypes;
    }
    
    Object[] unwrapArguments(List arguments, BeansWrapper wrapper) throws TemplateModelException
    {
        if(arguments == null) {
            arguments = Collections.EMPTY_LIST;
        }
        boolean isVarArg = _MethodUtil.isVarargs(member);
        int typesLen = argTypes.length;
        if(isVarArg) {
            if(typesLen - 1 > arguments.size()) {
                throw new _TemplateModelException(new Object[] {
                        _MethodUtil.invocationErrorMessageStart(member),
                        " takes at least ", new Integer(typesLen - 1),
                        typesLen - 1 == 1 ? " argument" : " arguments", ", but ",
                        new Integer(arguments.size()), " was given." });
            }
        }
        else if(typesLen != arguments.size()) {
            throw new _TemplateModelException(new Object[] {
                    _MethodUtil.invocationErrorMessageStart(member), 
                    " takes ", new Integer(typesLen), typesLen == 1 ? " argument" : " arguments", ", but ",
                    new Integer(arguments.size()), " was given." });
        }
         
        Object[] args = unwrapArguments(arguments, argTypes, isVarArg, wrapper);
        return args;
    }

    private Object[] unwrapArguments(List args, Class[] argTypes, boolean isVarargs,
            BeansWrapper w) 
    throws TemplateModelException {
        if(args == null) return null;
        
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
            if(unwrappedArgVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
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
                        if(unwrappedVarargVal == ObjectWrapperAndUnwrapper.CANT_UNWRAP_TO_TARGET_CLASS) {
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
        return new _TemplateModelException(new Object[] {
                _MethodUtil.invocationErrorMessageStart(member), " couldn't be called: Can't convert the ",
                new _DelayedOrdinal(new Integer(argIdx + 1)),
                " argument's value to the target Java type, ", ClassUtil.getShortClassName(targetType),
                ". The type of the actual value was: ", new _DelayedFTLTypeDescription(argVal),
                });
    }

    private TemplateModelException createNullToPrimitiveArgumentException(int argIdx, Class targetType) {
        return new _TemplateModelException(new Object[] {
                _MethodUtil.invocationErrorMessageStart(member), " couldn't be called: The value of the ",
                new _DelayedOrdinal(new Integer(argIdx + 1)),
                " argument was null, but the target Java parameter type (", ClassUtil.getShortClassName(targetType),
                ") is primitive and so can't store null." });
    }
    
    protected Member getMember() {
        return member;
    }
}
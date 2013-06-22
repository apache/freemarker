/*
 * Copyright (c) 2003-2007 The Visigoth Software Society. All rights
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

import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._TemplateModelException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * This class is used for constructors and as a base for non-overloaded methods
 * @author Attila Szegedi
 */
class SimpleMemberModel
{
    private final Member member;
    private final Class[] argTypes;
    
    protected SimpleMemberModel(Member member, Class[] argTypes)
    {
        this.member = member;
        this.argTypes = argTypes;
    }
    
    Object[] unwrapArguments(List arguments, BeansWrapper wrapper) throws TemplateModelException
    {
        if(arguments == null) {
            arguments = Collections.EMPTY_LIST;
        }
        boolean isVarArg = MethodUtilities.isVarArgs(member);
        int typesLen = argTypes.length;
        if(isVarArg) {
            if(typesLen - 1 > arguments.size()) {
                throw new TemplateModelException("Method " + member + 
                        " takes at least " + (typesLen - 1) + 
                        " arguments, " + arguments.size() + " was given.");
            }
        }
        else if(typesLen != arguments.size()) {
            throw new TemplateModelException("Method " + member + 
                    " takes exactly " + typesLen + " arguments, " +
                    arguments.size() + " was given.");
        }
         
        Object[] args = unwrapArguments(arguments, argTypes, isVarArg, wrapper);
        return args;
    }

    static Object[] unwrapArguments(List args, Class[] argTypes, boolean isVarargs,
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
            Object unwrappedArgVal = w.unwrapInternal(argVal, argType);
            if(unwrappedArgVal == BeansWrapper.CAN_NOT_UNWRAP) {
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
                        && (unwrappedArgVal = w.unwrapInternal(argVal, varargType))
                            != BeansWrapper.CAN_NOT_UNWRAP) {
                    // It was a vararg array.
                    unwrappedArgs[argIdx++] = unwrappedArgVal;
                } else {
                    // It wasn't a vararg array, so we assume it's a vararg
                    // array *item*, possibly followed by further ones.
                    int varargArrayLen = argsLen - argIdx;
                    Object varargArray = Array.newInstance(varargItemType, varargArrayLen);
                    for (int varargIdx = 0; varargIdx < varargArrayLen; varargIdx++) {
                        TemplateModel varargVal = (TemplateModel) (varargIdx == 0 ? argVal : it.next());
                        Object unwrappedVarargVal = w.unwrapInternal(varargVal, varargItemType);
                        if(unwrappedVarargVal == BeansWrapper.CAN_NOT_UNWRAP) {
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

    private static TemplateModelException createArgumentTypeMismarchException(
            int argIdx, TemplateModel argVal, Class targetType) {
        return new _TemplateModelException(new Object[] {
                "Argument type mismatch; can't convert (unwrap) argument #", new Integer(argIdx + 1),
                " value of type ", new _DelayedFTLTypeDescription(argVal),
                " to ", ClassUtil.getShortClassName(targetType), "." });
    }

    private static TemplateModelException createNullToPrimitiveArgumentException(int argIdx, Class targetType) {
        return new _TemplateModelException(new Object[] {
                "Argument type mismatch; argument #", new Integer(argIdx + 1),
                " is null, which can't be converted to primitive type ", targetType.getName(), "." });
    }
    
    protected Member getMember() {
        return member;
    }
}
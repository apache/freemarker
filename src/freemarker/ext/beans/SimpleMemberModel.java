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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * This class is used for constructors and as a base for non-overloaded methods
 * @author Attila Szegedi
 * @version $Id: $
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
        boolean varArg = MethodUtilities.isVarArgs(member);
        int typeLen = argTypes.length;
        if(varArg) {
            if(typeLen - 1 > arguments.size()) {
                throw new TemplateModelException("Method " + member + 
                        " takes at least " + (typeLen - 1) + 
                        " arguments");
            }
        }
        else if(typeLen != arguments.size()) {
            throw new TemplateModelException("Method " + member + 
                    " takes exactly " + typeLen + " arguments");
        }
         
        Object[] args = unwrapArguments(arguments, argTypes, wrapper);
        if(args != null) {
            BeansWrapper.coerceBigDecimals(argTypes, args);
            if(varArg && shouldPackVarArgs(args)) {
                args = packVarArgs(args, argTypes);
            }
        }
        return args;
    }

    static Object[] unwrapArguments(List arguments, Class[] argTypes, BeansWrapper w) 
    throws TemplateModelException
    {
        if(arguments == null) {
            return null;
        }
        int argsLen = arguments.size();
        int typeLen = argTypes.length;
        Object[] args = new Object[argsLen];
        int min = Math.min(argsLen, typeLen);
        Iterator it = arguments.iterator();
        for (int i = 0; i < min; i++) {
            args[i] = unwrapArgument((TemplateModel)it.next(), argTypes[i], w);
        }
        for (int i = min; i < argsLen; i++) {
            args[i] = unwrapArgument((TemplateModel)it.next(), argTypes[min - 1], w);
        }
        return args;
    }

    private static Object unwrapArgument(TemplateModel model, Class type, BeansWrapper w) 
    throws TemplateModelException {
        Object val = w.unwrapInternal(model, type);
        if(val == BeansWrapper.CAN_NOT_UNWRAP) {
            throw new TemplateModelException("Can not unwrap argument " +
                    model + " to " + type.getName());
        }
        return val;
    }
    
    private boolean shouldPackVarArgs(Object[] args) {
        int l = args.length;
        if(l == argTypes.length) {
            //assert l > 0; // varArg methods must have at least one declared arg
            Object lastArg = args[l - 1];
            if(lastArg == null || argTypes[l - 1].getComponentType().isInstance(lastArg)) {
                return false;
            }
        }
        return true;
    }
    
    static Object[] packVarArgs(Object[] args, Class[] argTypes)
    {
        int argsLen = args.length;
        int typeLen = argTypes.length;
        int fixArgsLen = typeLen - 1;
        Object varArray = Array.newInstance(argTypes[fixArgsLen], 
                argsLen - fixArgsLen);
        for (int i = fixArgsLen; i < argsLen; i++) {
            Array.set(varArray, i - fixArgsLen, args[i]);
        }
        if(argsLen != typeLen) {
            Object[] newArgs = new Object[typeLen];
            System.arraycopy(args, 0, newArgs, 0, fixArgsLen);
            args = newArgs;
        }
        args[fixArgsLen] = varArray;
        return args;
    }

    protected Member getMember() {
        return member;
    }
}
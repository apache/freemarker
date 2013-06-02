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

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModelException;

/**
 * @author Attila Szegedi
 */
abstract class OverloadedMethodsSubset {
    static final Object NO_SUCH_METHOD = new Object();
    static final Object AMBIGUOUS_METHOD = new Object();
    static final Object[] EMPTY_ARGS = new Object[0];

    private Class[/*number of args*/][/*arg index*/] unwrappingArgTypesByArgCount;
    // TODO: make it not concurrent
    private final Map selectorCache = new HashMap();
    private final List/*<Constructor|Method>*/ members = new LinkedList();
    private final Map/*<Constructor|Method, Class[]>*/ signatures = new HashMap();
    
    void addMember(Member member) {
        members.add(member);

        Class[] argTypes = MethodUtilities.getParameterTypes(member);
        final int argCount = argTypes.length;
        signatures.put(member, argTypes.clone());
        
        onAddSignature(member, argTypes);
        
        if (unwrappingArgTypesByArgCount == null) {
            unwrappingArgTypesByArgCount = new Class[argCount + 1][];
            unwrappingArgTypesByArgCount[argCount] = argTypes;
        } else if (unwrappingArgTypesByArgCount.length <= argCount) {
            Class[][] newUnwrappingArgTypesByArgCount = new Class[argCount + 1][];
            System.arraycopy(unwrappingArgTypesByArgCount, 0, newUnwrappingArgTypesByArgCount, 0, unwrappingArgTypesByArgCount.length);
            unwrappingArgTypesByArgCount = newUnwrappingArgTypesByArgCount;
            unwrappingArgTypesByArgCount[argCount] = argTypes;
        } else {
            Class[] oldUnwrappingArgTypes = unwrappingArgTypesByArgCount[argCount]; 
            if (oldUnwrappingArgTypes == null) {
                unwrappingArgTypesByArgCount[argCount] = argTypes;
            } else {
                for(int i = 0; i < oldUnwrappingArgTypes.length; ++i) {
                    // DD: This can't be right. We suddenly unwrap to a different type, if a new overloaded
                    //     method was added (and hence the most specific common type might changed), which is possibly
                    //     irrelevant.
                    oldUnwrappingArgTypes[i] = MethodUtilities.getMostSpecificCommonType(oldUnwrappingArgTypes[i], argTypes[i]);
                }
            }
        }
        updateSignature(argCount);

        afterSignatureAdded(argCount);
    }
    
    Class[] getSignature(Member member) {
        return (Class[]) signatures.get(member);
    }
    
    Class[][] getUnwrappingArgTypesByArgCount() {
        return unwrappingArgTypesByArgCount;
    }
    
    Object getMemberForArgs(Object[] args, boolean varArg) {
        ClassString argTypes = new ClassString(args);
        Object objMember;
        synchronized(selectorCache) {
            objMember = selectorCache.get(argTypes);
            if(objMember == null) {
                objMember = argTypes.getMostSpecific(members, varArg);
                selectorCache.put(argTypes, objMember);
            }
        }
        return objMember;
    }
    
    Iterator/*<Constructor|Method>*/ getMembers() {
        return members.iterator();
    }
    
    abstract void onAddSignature(Member member, Class[] argTypes);
    abstract void updateSignature(int l);
    abstract void afterSignatureAdded(int l);
    
    abstract Object getMemberAndArguments(List/*<TemplateModel>*/ tmArgs, 
            BeansWrapper w) throws TemplateModelException;
}

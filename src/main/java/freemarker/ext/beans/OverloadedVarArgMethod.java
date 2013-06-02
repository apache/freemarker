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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author Attila Szegedi
 */
class OverloadedVarargMethods extends OverloadedMethodsSubset
{
    private static final Map canoncialArgPackers = new HashMap();
    
    private final Map argPackers = new HashMap();
    
    private static class ArgumentPacker {
        private final int argCount;
        private final Class varArgType;
        
        ArgumentPacker(Class[] argTypes) {
            argCount = argTypes.length;
            varArgType = argTypes[argCount - 1].getComponentType(); 
        }
        
        Object[] packArgs(Object[] args, List modelArgs, BeansWrapper w) 
        throws TemplateModelException {
            final int actualArgCount = args.length;
            final int fixArgCount = argCount - 1;
            if(args.length != argCount) {
                Object[] newargs = new Object[argCount];
                System.arraycopy(args, 0, newargs, 0, fixArgCount);
                Object array = Array.newInstance(varArgType, actualArgCount - fixArgCount);
                for(int i = fixArgCount; i < actualArgCount; ++i) {
                    Object val = w.unwrapInternal((TemplateModel)modelArgs.get(i), varArgType);
                    if(val == BeansWrapper.CAN_NOT_UNWRAP) {
                        return null;
                    }
                    Array.set(array, i - fixArgCount, val);
                }
                newargs[fixArgCount] = array;
                return newargs;
            }
            else {
                Object val = w.unwrapInternal((TemplateModel)modelArgs.get(fixArgCount), varArgType);
                if(val == BeansWrapper.CAN_NOT_UNWRAP) {
                    return null;
                }
                Object array = Array.newInstance(varArgType, 1);
                Array.set(array, 0, val);
                args[fixArgCount] = array;
                return args;
            }
        }
        
        public boolean equals(Object obj) {
            if(obj instanceof ArgumentPacker) {
        	ArgumentPacker p = (ArgumentPacker)obj;
        	return argCount == p.argCount && varArgType == p.varArgType;
            }
            return false;
        }
        
        public int hashCode() {
            return argCount ^ varArgType.hashCode();
        }
    }

    void onAddSignature(Member member, Class[] argTypes) {
	ArgumentPacker argPacker = new ArgumentPacker(argTypes);
	synchronized(canoncialArgPackers) {
	    ArgumentPacker canonical = (ArgumentPacker)
	    canoncialArgPackers.get(argPacker);
	    if(canonical == null) {
	        canoncialArgPackers.put(argPacker, argPacker);
	    }
	    else {
	        argPacker = canonical;
	    }
	}
        argPackers.put(member, argPacker);
        componentizeLastType(argTypes);
    }

    void updateSignature(int l) {
	Class[][] marshalTypes = getUnwrappingArgTypesByArgCount();
	Class[] newTypes = marshalTypes[l];
        // First vararg marshal type spec with less parameters than the 
        // current spec influences the types of the current marshal spec.
        for(int i = l; i-->0;) {
            Class[] previousTypes = marshalTypes[i];
            if(previousTypes != null) {
                varArgUpdate(newTypes, previousTypes);
                break;
            }
        }
        // Vararg marshal spec with exactly one parameter more than the current
        // spec influences the types of the current spec
        if(l + 1 < marshalTypes.length) {
            Class[] oneLongerTypes = marshalTypes[l + 1];
            if(oneLongerTypes != null) {
                varArgUpdate(newTypes, oneLongerTypes);
            }
        }
    }
    
    void afterSignatureAdded(int l) {
	// Since this member is vararg, its types influence the types in all
        // type specs longer than itself.
	Class[][] marshalTypes = getUnwrappingArgTypesByArgCount();
        Class[] newTypes = marshalTypes[l];
        for(int i = l + 1; i < marshalTypes.length; ++i) {
            Class[] existingTypes = marshalTypes[i];
            if(existingTypes != null) {
                varArgUpdate(existingTypes, newTypes);
            }
        }
        // It also influences the types in the marshal spec that is exactly
        // one argument shorter (as vararg methods can be invoked with 0
        // variable arguments, that is, with k-1 cardinality).
        if(l > 0) {
            Class[] oneShorterTypes = marshalTypes[l - 1];
            if(oneShorterTypes != null) {
                varArgUpdate(oneShorterTypes, newTypes);
            }
        }
    }
    
    private static void varArgUpdate(Class[] modifiedTypes, Class[] modifyingTypes) {
        final int dl = modifiedTypes.length;
        final int gl = modifyingTypes.length;
        int min = Math.min(gl, dl);
        for(int i = 0; i < min; ++i) {
            modifiedTypes[i] = MethodUtilities.getMostSpecificCommonType(modifiedTypes[i], 
                    modifyingTypes[i]);
        }
        if(dl > gl) {
            Class varArgType = modifyingTypes[gl - 1];
            for(int i = gl; i < dl; ++i) {
                modifiedTypes[i] = MethodUtilities.getMostSpecificCommonType(modifiedTypes[i], 
                        varArgType);
            }
        }
    }
    
    private static void componentizeLastType(Class[] types) {
        int l1 = types.length - 1;
        //assert l1 >= 0;
        //assert types[l1].isArray();
        types[l1] = types[l1].getComponentType();
    }
    
    Object getMemberAndArguments(List tmArgs, BeansWrapper w) 
    throws TemplateModelException {
        if(tmArgs == null) {
            // null is treated as empty args
            tmArgs = Collections.EMPTY_LIST;
        }
        int l = tmArgs.size();
        Class[][] unwrappingArgTypesByArgCount = getUnwrappingArgTypesByArgCount();
        Object[] pojoArgs = new Object[l];
        // Starting from args.length + 1 as we must try to match against a case
        // where all specified args are fixargs, and the vararg portion 
        // contains zero args
        outer:  for(int j = Math.min(l + 1, unwrappingArgTypesByArgCount.length - 1); j >= 0; --j) {
            Class[] unwarappingArgTypes = unwrappingArgTypesByArgCount[j];
            if(unwarappingArgTypes == null) {
                if(j == 0) {
                    return NO_SUCH_METHOD;
                }
                continue;
            }
            // Try to marshal the arguments
            Iterator it = tmArgs.iterator();
            for(int i = 0; i < l; ++i) {
                Object pojo = w.unwrapInternal((TemplateModel)it.next(), i < j ? unwarappingArgTypes[i] : unwarappingArgTypes[j - 1]);
                if(pojo == BeansWrapper.CAN_NOT_UNWRAP) {
                    continue outer;
                }
                if(pojo != pojoArgs[i]) {
                    pojoArgs[i] = pojo;
                }
            }
            break;
        }
        
        Object objMember = getMemberForArgs(pojoArgs, true);
        if(objMember instanceof Member) {
            Member member = (Member)objMember;
            pojoArgs = ((ArgumentPacker)argPackers.get(member)).packArgs(pojoArgs, tmArgs, w);
            if(pojoArgs == null) {
                return NO_SUCH_METHOD;
            }
            BeansWrapper.coerceBigDecimals(getSignature(member), pojoArgs);
            return new MemberAndArguments(member, pojoArgs);
        }
        return objMember; // either NOT_FOUND or AMBIGUOUS
    }
}
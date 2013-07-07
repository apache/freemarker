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
    
    static final Object[] EMPTY_ARGS = new Object[0];

    private Class[/*number of args*/][/*arg index*/] unwrappingHintsByParamCount;
    // Java 5: Use ConcurrentHashMap:
    private final Map/*<ClassString, MaybeEmptyOverloadedMemberDescriptor>*/ selectorCache = new HashMap();
    private final List/*<OverloadedMemberDescriptor>*/ memberDescs = new LinkedList();
    
    protected final int beansWrapperVersion;
    
    OverloadedMethodsSubset(BeansWrapper beansWrapper) {
        beansWrapperVersion = beansWrapper.getIncompatibleImprovements().intValue();
    }
    
    void addMember(Member member) {
        final Class[] paramTypes = MethodUtilities.getParameterTypes(member);
        final int paramCount = paramTypes.length;
        
        memberDescs.add(new OverloadedMemberDescriptor(member, paramTypes));
        
        final Class[] preprocessedParamTypes = preprocessParameterTypes(member, paramTypes);
        
        final Class[] unwrappingHints = (Class[]) preprocessedParamTypes.clone();
        // Merge these unwrapping hints with the existing table of hints:
        if (unwrappingHintsByParamCount == null) {
            unwrappingHintsByParamCount = new Class[paramCount + 1][];
            unwrappingHintsByParamCount[paramCount] = unwrappingHints;
        } else if (unwrappingHintsByParamCount.length <= paramCount) {
            Class[][] newUnwrappingHintsByParamCount = new Class[paramCount + 1][];
            System.arraycopy(unwrappingHintsByParamCount, 0, newUnwrappingHintsByParamCount, 0,
                    unwrappingHintsByParamCount.length);
            unwrappingHintsByParamCount = newUnwrappingHintsByParamCount;
            unwrappingHintsByParamCount[paramCount] = unwrappingHints;
        } else {
            Class[] prevUnwrappingHints = unwrappingHintsByParamCount[paramCount]; 
            if (prevUnwrappingHints == null) {
                unwrappingHintsByParamCount[paramCount] = unwrappingHints;
            } else {
                for(int i = 0; i < prevUnwrappingHints.length; ++i) {
                    // For each parameter list length, we merge the argument type arrays into a single Class[] that
                    // stores the most specific common types of each position. Hence we will possibly use a too generic
                    // hint for the unwrapping. For correct behavior, for each overloaded methods its own parameter
                    // types should be used as a hint. But without unwrapping the arguments, we couldn't select the
                    // overloaded method. So this is a circular reference problem. We could try selecting the
                    // method based on the wrapped value, but that's quite tricky, and the result of such selection
                    // is not cacheable (the TM types are not enough as cache key then. So we just use this
                    // compromise.
                    prevUnwrappingHints[i] = MethodUtilities.getMostSpecificCommonType(
                            prevUnwrappingHints[i], unwrappingHints[i]);
                }
            }
        }
        
        afterWideningUnwrappingHints(beansWrapperVersion >= 2003021 ? preprocessedParamTypes : unwrappingHints);
    }
    
    Class[][] getUnwrappingHintsByParamCount() {
        return unwrappingHintsByParamCount;
    }
    
    MaybeEmptyOverloadedMemberDescriptor getMemberForArgs(Object[] args, boolean varArg) {
        ClassString argTypes = new ClassString(args);
        MaybeEmptyOverloadedMemberDescriptor memberDesc;
        synchronized(selectorCache) {
            memberDesc = (MaybeEmptyOverloadedMemberDescriptor) selectorCache.get(argTypes);
            if(memberDesc == null) {
                memberDesc = argTypes.getMostSpecific(memberDescs, varArg);
                selectorCache.put(argTypes, memberDesc);
            }
        }
        return memberDesc;
    }
    
    Iterator/*<OverloadedMemberDescriptor>*/ getMemberDescriptors() {
        return memberDescs.iterator();
    }
    
    abstract Class[] preprocessParameterTypes(Member member, Class[] paramTypes);
    abstract void afterWideningUnwrappingHints(Class[] paramTypes);
    
    abstract MaybeEmptyMemberAndArguments getMemberAndArguments(List/*<TemplateModel>*/ tmArgs, 
            BeansWrapper w) throws TemplateModelException;
    
}

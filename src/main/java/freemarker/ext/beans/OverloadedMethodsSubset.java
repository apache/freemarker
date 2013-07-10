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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

/**
 * @author Attila Szegedi
 */
abstract class OverloadedMethodsSubset {

    private Class[/*number of args*/][/*arg index*/] unwrappingHintsByParamCount;
    
    // TODO: This can cause memory-leak when classes are re-loaded. However, first the genericClassIntrospectionCache
    // and such need to be fixed in this regard. 
    // Java 5: Use ConcurrentHashMap:
    private final Map/*<ClassString, MaybeEmptyCallableMemberDescriptor>*/ argTypesToMemberDescCache = new HashMap();
    
    private final List/*<CallableMemberDescriptor>*/ memberDescs = new LinkedList();
    
    /** Enables 2.3.21 {@link BeansWrapper} incompatibleImprovements */
    protected final boolean bugfixed;
    
    OverloadedMethodsSubset(BeansWrapper beansWrapper) {
        bugfixed = beansWrapper.getIncompatibleImprovements().intValue() >= 2003021;
    }
    
    void addCallableMemberDescriptor(CallableMemberDescriptor memberDesc) {
        final int paramCount = memberDesc.paramTypes.length;
        
        memberDescs.add(memberDesc);
        
        final Class[] preprocessedParamTypes = preprocessParameterTypes(memberDesc);
        
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
                    prevUnwrappingHints[i] = getCommonSupertypeForUnwrappingHint(
                            prevUnwrappingHints[i], unwrappingHints[i]);
                }
            }
        }
        
        afterWideningUnwrappingHints(bugfixed ? preprocessedParamTypes : unwrappingHints);
    }
    
    Class[][] getUnwrappingHintsByParamCount() {
        return unwrappingHintsByParamCount;
    }
    
    MaybeEmptyCallableMemberDescriptor getMemberDescriptorForArgs(Object[] args, boolean varArg) {
        ClassString argTypes = new ClassString(args, bugfixed);
        MaybeEmptyCallableMemberDescriptor memberDesc;
        synchronized(argTypesToMemberDescCache) {
            memberDesc = (MaybeEmptyCallableMemberDescriptor) argTypesToMemberDescCache.get(argTypes);
            if(memberDesc == null) {
                memberDesc = argTypes.getMostSpecific(memberDescs, varArg);
                argTypesToMemberDescCache.put(argTypes, memberDesc);
            }
        }
        return memberDesc;
    }
    
    Iterator/*<CallableMemberDescriptor>*/ getMemberDescriptors() {
        return memberDescs.iterator();
    }
    
    abstract Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc);
    abstract void afterWideningUnwrappingHints(Class[] paramTypes);
    
    abstract MaybeEmptyMemberAndArguments getMemberAndArguments(List/*<TemplateModel>*/ tmArgs, 
            BeansWrapper w) throws TemplateModelException;

    /**
     * Returns the most specific common class (or interface) of two parameter types for the purpose of unwrapping.
     * This is trickier then finding the most specific overlapping superclass of two classes, because:
     * <ul>
     *   <li>It considers primitive classes as the subclasses of the boxing classes.</li>
     *   <li>It considers widening numerical conversion as if the narrower type is subclass.</li>
     *   <li>If the only common class is {@link Object}, it will try to find a common interface. If there are more
     *       of them, it will start removing those that are known to be uninteresting as unwrapping hint.</li>
     * </ul>
     * 
     * @param c1 Parameter type 1
     * @param c2 Parameter type 2
     */
    protected Class getCommonSupertypeForUnwrappingHint(Class c1, Class c2) {
        if(c1 == c2) return c1;
        // This also means that the hint for (Integer, Integer) will be Integer, not just Number. This is consistent
        // with how non-overloaded method hints work.
        
        if (bugfixed) {
            // c1 primitive class to boxing class:
            final boolean c1WasPrim; 
            if (c1.isPrimitive()) {
                c1 = ClassUtil.primitiveClassToBoxingClass(c1);
                c1WasPrim = true;
            } else {
                c1WasPrim = false;
            }
            
            // c1 primitive class to boxing class:
            final boolean c2WasPrim; 
            if (c2.isPrimitive()) {
                c2 = ClassUtil.primitiveClassToBoxingClass(c2);
                c2WasPrim = true;
            } else {
                c2WasPrim = false;
            }
    
            if (c1 == c2) {
                // If it was like int and Integer, boolean and Boolean, etc.
                // (If it was two equivalent primitives, we don't get here, because of the 1st line of the method.) 
                return c1;
            } else if (Number.class.isAssignableFrom(c1) && Number.class.isAssignableFrom(c2)) {
                // We don't want the unwrapper to convert to a numerical super-type as it's not yet known what the
                // actual number type of the chosen method will be. (Especially as fixed point to floating point can be
                // lossy.)
                return Number.class;
            } else if (c1WasPrim || c2WasPrim) {
                // At this point these all stand:
                // - At least one of them was primitive
                // - No more than one of them was numerical
                // - They don't have the same wrapper (boxing) class
                return Object.class;
            }
            // Falls through
        } else {  // old buggy behavior
            if(c2.isPrimitive()) {
                if(c2 == Byte.TYPE) c2 = Byte.class;
                else if(c2 == Short.TYPE) c2 = Short.class;
                else if(c2 == Character.TYPE) c2 = Character.class;
                else if(c2 == Integer.TYPE) c2 = Integer.class;
                else if(c2 == Float.TYPE) c2 = Float.class;
                else if(c2 == Long.TYPE) c2 = Long.class;
                else if(c2 == Double.TYPE) c2 = Double.class;
            }
        }
        
        // If buxfixed is true and any of the classes was a primitive type, we never reach this point.
        
        Set commonTypes = MethodUtilities.getAssignables(c1, c2);
        commonTypes.retainAll(MethodUtilities.getAssignables(c2, c1));
        if(commonTypes.isEmpty()) {
            // Can happen when at least one of the arguments is an interface, as
            // they don't have Object at the root of their hierarchy
            return Object.class;
        }
        
        // Gather maximally specific elements. Yes, there can be more than one 
        // thank to interfaces. I.e., if you call this method for String.class 
        // and Number.class, you'll have Comparable, Serializable, and Object as 
        // maximal elements. 
        List max = new ArrayList();
        listCommonTypes:  for (Iterator commonTypesIter = commonTypes.iterator(); commonTypesIter.hasNext();) {
            Class clazz = (Class)commonTypesIter.next();
            for (Iterator maxIter = max.iterator(); maxIter.hasNext();) {
                Class maxClazz = (Class)maxIter.next();
                if(MethodUtilities.isMoreOrSameSpecificParameterType(maxClazz, clazz, bugfixed, 0) != 0) {
                    // clazz can't be maximal, if there's already a more specific or equal maximal than it.
                    continue listCommonTypes;
                }
                if(MethodUtilities.isMoreOrSameSpecificParameterType(clazz, maxClazz, bugfixed, 0) != 0) {
                    // If it's more specific than a currently maximal element,
                    // that currently maximal is no longer a maximal.
                    maxIter.remove();
                }
            }
            // If we get here, no current maximal is more specific than the
            // current class, so clazz is a new maximal so far.
            max.add(clazz);
        }
        
        if (max.size() > 1) {  // we have an ambiguity
            if (bugfixed) {
                // Find the non-interface class
                for (Iterator it = max.iterator(); it.hasNext();) {
                    Class maxCl = (Class) it.next();
                    if (!maxCl.isInterface()) {
                        if (maxCl != Object.class) {  // This actually shouldn't ever happen, but to be sure...
                            // If it's not Object, we use it as the most specific
                            return maxCl;
                        } else {
                            // Otherwise remove Object, and we will try with the interfaces 
                            it.remove();
                        }
                    }
                }
                
                // At this point we only have interfaces left.
                // Try removing interfaces about which we know that they are useless as unwrapping hints:
                max.remove(Cloneable.class);
                if (max.size() > 1) {  // Still have an ambiguity...
                    max.remove(Serializable.class);
                    if (max.size() > 1) {  // Still had an ambiguity...
                        max.remove(Comparable.class);
                        if (max.size() > 1) {
                            return Object.class; // Still had an ambiguity... no luck.
                        }
                    }
                }
            } else {
                return Object.class;
            }
        }
        
        return (Class) max.get(0);
    }
    
}

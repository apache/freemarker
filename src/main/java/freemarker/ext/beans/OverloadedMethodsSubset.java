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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.core._ConcurrentMapFactory;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.NullArgumentException;
import freemarker.template.utility._MethodUtil;

/**
 * @author Attila Szegedi
 */
abstract class OverloadedMethodsSubset {
    
    /** 
     * Used for an optimization trick to substitute an array of whatever size that contains only 0-s. Since this array
     * is 0 long, this means that the code that reads the int[] always have to check if the int[] has this value, and
     * then it has to act like if was all 0-s.  
     */
    static final int[] ALL_ZEROS_ARRAY = new int[0];

    private Class[/*number of args*/][/*arg index*/] unwrappingHintsByParamCount;
    
    /**
     * Tells what numerical types occur at a given parameter position with a bit field, also if the
     * unwrapping will do proper conversion. See {@link OverloadedNumberUtil#FLAG_INTEGER}, etc.
     */
    private int[/*number of args*/][/*arg index*/] possibleNumericalTypesByParamCount;
    
    // TODO: This can cause memory-leak when classes are re-loaded. However, first the genericClassIntrospectionCache
    // and such need to be fixed in this regard. 
    private final Map/*<ArgumentTypes, MaybeEmptyCallableMemberDescriptor>*/ argTypesToMemberDescCache
            = _ConcurrentMapFactory.newMaybeConcurrentHashMap(6, 0.75f, 1);
    private final boolean isArgTypesToMemberDescCacheConcurrentMap
            = _ConcurrentMapFactory.isConcurrent(argTypesToMemberDescCache);
    
    private final List/*<CallableMemberDescriptor>*/ memberDescs = new LinkedList();
    
    /** Enables 2.3.21 {@link BeansWrapper} incompatibleImprovements */
    protected final boolean bugfixed;
    
    OverloadedMethodsSubset(boolean bugfixed) {
        this.bugfixed = bugfixed;
    }
    
    void addCallableMemberDescriptor(CallableMemberDescriptor memberDesc) {
        memberDescs.add(memberDesc);
        
        // Warning: Do not modify this array, or put it into unwrappingHintsByParamCount by reference, as the arrays
        // inside that are modified!
        final Class[] prepedParamTypes = preprocessParameterTypes(memberDesc);
        final int paramCount = prepedParamTypes.length;  // Must be the same as the length of the original param list
        
        // Merge these unwrapping hints with the existing table of hints:
        if (unwrappingHintsByParamCount == null) {
            unwrappingHintsByParamCount = new Class[paramCount + 1][];
            unwrappingHintsByParamCount[paramCount] = (Class[]) prepedParamTypes.clone();
        } else if (unwrappingHintsByParamCount.length <= paramCount) {
            Class[][] newUnwrappingHintsByParamCount = new Class[paramCount + 1][];
            System.arraycopy(unwrappingHintsByParamCount, 0, newUnwrappingHintsByParamCount, 0,
                    unwrappingHintsByParamCount.length);
            unwrappingHintsByParamCount = newUnwrappingHintsByParamCount;
            unwrappingHintsByParamCount[paramCount] = (Class[]) prepedParamTypes.clone();
        } else {
            Class[] unwrappingHints = unwrappingHintsByParamCount[paramCount]; 
            if (unwrappingHints == null) {
                unwrappingHintsByParamCount[paramCount] = (Class[]) prepedParamTypes.clone();
            } else {
                for (int paramIdx = 0; paramIdx < prepedParamTypes.length; paramIdx++) {
                    // For each parameter list length, we merge the argument type arrays into a single Class[] that
                    // stores the most specific common types for each position. Hence we will possibly use a too generic
                    // hint for the unwrapping. For correct behavior, for each overloaded methods its own parameter
                    // types should be used as a hint. But without unwrapping the arguments, we couldn't select the
                    // overloaded method. So this is a circular reference problem. We could try selecting the
                    // method based on the wrapped value, but that's quite tricky, and the result of such selection
                    // is not cacheable (the TM types are not enough as cache key then). So we just use this
                    // compromise.
                    unwrappingHints[paramIdx] = getCommonSupertypeForUnwrappingHint(
                            unwrappingHints[paramIdx], prepedParamTypes[paramIdx]);
                }
            }
        }

        int[] paramNumericalTypes = ALL_ZEROS_ARRAY;
        if (bugfixed) {
            // Fill possibleNumericalTypesByParamCount (if necessary)
            for (int paramIdx = 0; paramIdx < paramCount; paramIdx++) {
                final int numType = OverloadedNumberUtil.classToTypeFlags(prepedParamTypes[paramIdx]);
                if (numType != 0) {  // It's a numerical type
                    if (paramNumericalTypes == ALL_ZEROS_ARRAY) {
                        paramNumericalTypes = new int[paramCount];
                    }
                    paramNumericalTypes[paramIdx] = numType;
                }
            }
            mergeInNumericalTypes(paramCount, paramNumericalTypes);
        }
        
        afterWideningUnwrappingHints(
                bugfixed ? prepedParamTypes : unwrappingHintsByParamCount[paramCount],
                paramNumericalTypes);
    }
    
    Class[][] getUnwrappingHintsByParamCount() {
        return unwrappingHintsByParamCount;
    }
    
    final MaybeEmptyCallableMemberDescriptor getMemberDescriptorForArgs(Object[] args, boolean varArg) {
        ArgumentTypes argTypes = new ArgumentTypes(args, bugfixed);
        MaybeEmptyCallableMemberDescriptor memberDesc = 
                isArgTypesToMemberDescCacheConcurrentMap
                        ? (MaybeEmptyCallableMemberDescriptor) argTypesToMemberDescCache.get(argTypes)
                        : null;
        if (memberDesc == null) {
            synchronized(argTypesToMemberDescCache) {
                memberDesc = (MaybeEmptyCallableMemberDescriptor) argTypesToMemberDescCache.get(argTypes);
                if (memberDesc == null) {
                    memberDesc = argTypes.getMostSpecific(memberDescs, varArg);
                    argTypesToMemberDescCache.put(argTypes, memberDesc);
                }
            }
        }
        return memberDesc;
    }
    
    Iterator/*<CallableMemberDescriptor>*/ getMemberDescriptors() {
        return memberDescs.iterator();
    }
    
    abstract Class[] preprocessParameterTypes(CallableMemberDescriptor memberDesc);
    abstract void afterWideningUnwrappingHints(Class[] paramTypes, int[] paramNumericalTypes);
    
    abstract MaybeEmptyMemberAndArguments getMemberAndArguments(List/*<TemplateModel>*/ tmArgs, 
            BeansWrapper unwrapper) throws TemplateModelException;

    /**
     * Returns the most specific common class (or interface) of two parameter types for the purpose of unwrapping.
     * This is trickier than finding the most specific overlapping superclass of two classes, because:
     * <ul>
     *   <li>It considers primitive classes as the subclasses of the boxing classes.</li>
     *   <li>If the only common class is {@link Object}, it will try to find a common interface. If there are more
     *       of them, it will start removing those that are known to be uninteresting as unwrapping hints.</li>
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
            
            // c2 primitive class to boxing class:
            final boolean c2WasPrim; 
            if (c2.isPrimitive()) {
                c2 = ClassUtil.primitiveClassToBoxingClass(c2);
                c2WasPrim = true;
            } else {
                c2WasPrim = false;
            }
    
            if (c1 == c2) {
                // If it was like int and Integer, boolean and Boolean, etc., we return the boxing type (as that's the
                // less specific, because it allows null.)
                // (If it was two equivalent primitives, we don't get here, because of the 1st line of the method.) 
                return c1;
            } else if (Number.class.isAssignableFrom(c1) && Number.class.isAssignableFrom(c2)) {
                // We don't want the unwrapper to convert to a numerical super-type [*] as it's not yet known what the
                // actual number type of the chosen method will be. We will postpone the actual numerical conversion
                // until that, especially as some conversions (like fixed point to floating point) can be lossy.
                // * Numerical super-type: Like long > int > short > byte.  
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
        
        // We never get to this point if buxfixed is true and any of these stands:
        // - One of classes was a primitive type
        // - One of classes was a numerical type (either boxing type or primitive)
        
        Set commonTypes = _MethodUtil.getAssignables(c1, c2);
        commonTypes.retainAll(_MethodUtil.getAssignables(c2, c1));
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
                if(_MethodUtil.isMoreOrSameSpecificParameterType(maxClazz, clazz, false /*bugfixed [1]*/, 0) != 0) {
                    // clazz can't be maximal, if there's already a more specific or equal maximal than it.
                    continue listCommonTypes;
                }
                if(_MethodUtil.isMoreOrSameSpecificParameterType(clazz, maxClazz, false /*bugfixed [1]*/, 0) != 0) {
                    // If it's more specific than a currently maximal element,
                    // that currently maximal is no longer a maximal.
                    maxIter.remove();
                }
                // 1: We don't use bugfixed at the "[1]"-marked points because it's slower and doesn't make any
                //    difference here as it's ensured that nor c1 nor c2 is primitive or numerical. The bugfix has only
                //    affected the treatment of primitives and numerical types. 
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
    
    /**
     * Gets the numerical type "flags" of each parameter positions, or {@code null} if there's no method with this
     * parameter count or if we aren't in pre-2.3.21 mode, {@link #ALL_ZEROS_ARRAY} if there were no numerical
     * parameters. The returned {@code int}-s are on or more {@link OverloadedNumberUtil}} {@code FLAG_...} constants
     * binary "or"-ed together.  
     */
    final protected int[] getPossibleNumericalTypes(int paramCount) {
        return possibleNumericalTypesByParamCount != null && possibleNumericalTypesByParamCount.length > paramCount
                ? possibleNumericalTypesByParamCount[paramCount]
                : null;
    }

    /**
     * @param dstParamCount The parameter count for which we want to merge in the possible numerical types 
     * @param srcNumTypesByParamIdx If shorter than {@code dstParamCount}, it's last item will be repeated until
     *        dstParamCount length is reached. If longer, the excessive items will be ignored.
     *        Maybe {@link #ALL_ZEROS_ARRAY}. Cant'be a 0-length array. Can't be {@code null}.
     */
    final protected void mergeInNumericalTypes(int dstParamCount, int[] srcNumTypesByParamIdx) {
        NullArgumentException.check("srcNumTypesByParamIdx", srcNumTypesByParamIdx);
        if (dstParamCount == 0) return;
        
        // Ensure that possibleNumericalTypesByParamCount[dstParamCount] exists:
        if (possibleNumericalTypesByParamCount == null) {
            possibleNumericalTypesByParamCount = new int[dstParamCount + 1][];
        } else if (possibleNumericalTypesByParamCount.length <= dstParamCount) {
            if (srcNumTypesByParamIdx == null) return;
            
            int[][] newNumericalTypeByParamCount = new int[dstParamCount + 1][];
            System.arraycopy(possibleNumericalTypesByParamCount, 0, newNumericalTypeByParamCount, 0,
                    possibleNumericalTypesByParamCount.length);
            possibleNumericalTypesByParamCount = newNumericalTypeByParamCount;
        }
        
        final int srcParamCount = srcNumTypesByParamIdx.length;
        
        int[] dstNumTypesByParamIdx = possibleNumericalTypesByParamCount[dstParamCount];
        if (dstNumTypesByParamIdx == null) {
            // This is the first method added with this number of params => no merging
            
            if (srcNumTypesByParamIdx != ALL_ZEROS_ARRAY) {
                dstNumTypesByParamIdx = new int[dstParamCount];
                for (int paramIdx = 0; paramIdx < dstParamCount; paramIdx++) {
                    dstNumTypesByParamIdx[paramIdx]
                            = srcNumTypesByParamIdx[paramIdx < srcParamCount ? paramIdx : srcParamCount - 1];
                }
            } else {
                dstNumTypesByParamIdx = ALL_ZEROS_ARRAY;
            }
            
            possibleNumericalTypesByParamCount[dstParamCount] = dstNumTypesByParamIdx;
        } else {
            // dstNumTypesByParamIdx != null, so we need to merge into it.
            
            if (srcNumTypesByParamIdx == dstNumTypesByParamIdx) {
                // Used to occur when both are ALL_ZEROS_ARRAY
                return;
            }
            
            // As we will write dstNumTypesByParamIdx, it can't remain ALL_ZEROS_ARRAY anymore. 
            if (dstNumTypesByParamIdx == ALL_ZEROS_ARRAY && dstParamCount > 0) {
                dstNumTypesByParamIdx = new int[dstParamCount];
                possibleNumericalTypesByParamCount[dstParamCount] = dstNumTypesByParamIdx;
            }
            
            for (int paramIdx = 0; paramIdx < dstParamCount; paramIdx++) {
                final int srcParamNumTypes
                        = srcNumTypesByParamIdx != ALL_ZEROS_ARRAY
                            ? srcNumTypesByParamIdx[paramIdx < srcParamCount ? paramIdx : srcParamCount - 1]
                            : 0;
                final int dstParamNumTypes = dstNumTypesByParamIdx[paramIdx];
                if (dstParamNumTypes != srcParamNumTypes) {
                    dstNumTypesByParamIdx[paramIdx]
                            = dstParamNumTypes | srcParamNumTypes | OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT; 
                }
                // Note that if a parameter is non-numerical, its paraNumTypes is 0. So if we have a non-number and
                // a some kind of number (non-0), the merged result will be marked with FLAG_WIDENED_UNWRAPPING_HINT.
                // (I.e., the same happens as with two different numerical types.)
            }
        }
    }
    
    protected void forceNumberArgumentsToParameterTypes(
            Object[] args, Class[] paramTypes, int[] unwrappingNumTypesByParamIndex) {
        final int paramTypesLen = paramTypes.length;
        final int argsLen = args.length;
        for (int argIdx = 0; argIdx < argsLen; argIdx++) {
            final int paramTypeIdx = argIdx < paramTypesLen ? argIdx : paramTypesLen - 1;
            final int unwrappingNumTypes = unwrappingNumTypesByParamIndex[paramTypeIdx];
            
            // Forcing the number type can only be interesting if there are numerical parameter types on that index,
            // and the unwrapping was not to an exact numerical type.
            if (unwrappingNumTypes != 0
                    && (unwrappingNumTypes & OverloadedNumberUtil.FLAG_WIDENED_UNWRAPPING_HINT) != 0) {
                final Object arg = args[argIdx];
                // If arg isn't a number, we can't do any conversions anyway, regardless of the param type.
                if (arg instanceof Number) {
                    final Class targetType = paramTypes[paramTypeIdx];
                    final Number convertedArg = BeansWrapper.forceUnwrappedNumberToType(
                            (Number) arg, targetType, bugfixed);
                    if (convertedArg != null) {
                        args[argIdx] = convertedArg;
                    }
                }
            }
        }
    }
    
}

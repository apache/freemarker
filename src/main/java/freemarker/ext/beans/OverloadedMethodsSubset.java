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

/**
 * Encapsulates the rules and data structures (including cache) for choosing of the best matching callable member for
 * a parameter list, from a given set of callable members. There are two subclasses of this, one for non-varags methods,
 * and one for varargs methods.
 */
abstract class OverloadedMethodsSubset {
    
    /** 
     * Used for an optimization trick to substitute an array of whatever size that contains only 0-s. Since this array
     * is 0 long, this means that the code that reads the int[] always have to check if the int[] has this value, and
     * then it has to act like if was all 0-s.  
     */
    static final int[] ALL_ZEROS_ARRAY = new int[0];

    private static final int[][] ZERO_PARAM_COUNT_TYPE_FLAGS_ARRAY = new int[1][];
    static {
        ZERO_PARAM_COUNT_TYPE_FLAGS_ARRAY[0] = ALL_ZEROS_ARRAY;
    }

    private Class[/*number of args*/][/*arg index*/] unwrappingHintsByParamCount;
    
    /**
     * Tells what types occur at a given parameter position with a bit field. See {@link TypeFlags}.
     */
    private int[/*number of args*/][/*arg index*/] typeFlagsByParamCount;
    
    // TODO: This can cause memory-leak when classes are re-loaded. However, first the genericClassIntrospectionCache
    // and such need to be fixed in this regard. 
    private final Map/*<ArgumentTypes, MaybeEmptyCallableMemberDescriptor>*/ argTypesToMemberDescCache
            = _ConcurrentMapFactory.newMaybeConcurrentHashMap(6, 0.75f, 1);
    private final boolean isArgTypesToMemberDescCacheConcurrentMap
            = _ConcurrentMapFactory.isConcurrent(argTypesToMemberDescCache);
    
    private final List/*<ReflectionCallableMemberDescriptor>*/ memberDescs = new LinkedList();
    
    /** Enables 2.3.21 {@link BeansWrapper} incompatibleImprovements */
    protected final boolean bugfixed;
    
    OverloadedMethodsSubset(boolean bugfixed) {
        this.bugfixed = bugfixed;
    }
    
    void addCallableMemberDescriptor(ReflectionCallableMemberDescriptor memberDesc) {
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
                    // overloaded method. So we had to unwrap with all possible target types of each parameter position,
                    // which would be slow and its result would be uncacheable (as we don't have anything usable as
                    // a lookup key). So we just use this compromise.
                    unwrappingHints[paramIdx] = getCommonSupertypeForUnwrappingHint(
                            unwrappingHints[paramIdx], prepedParamTypes[paramIdx]);
                }
            }
        }

        int[] typeFlagsByParamIdx = ALL_ZEROS_ARRAY;
        if (bugfixed) {
            // Fill typeFlagsByParamCount (if necessary)
            for (int paramIdx = 0; paramIdx < paramCount; paramIdx++) {
                final int typeFlags = TypeFlags.classToTypeFlags(prepedParamTypes[paramIdx]);
                if (typeFlags != 0) {
                    if (typeFlagsByParamIdx == ALL_ZEROS_ARRAY) {
                        typeFlagsByParamIdx = new int[paramCount];
                    }
                    typeFlagsByParamIdx[paramIdx] = typeFlags;
                }
            }
            mergeInTypesFlags(paramCount, typeFlagsByParamIdx);
        }
        
        afterWideningUnwrappingHints(
                bugfixed ? prepedParamTypes : unwrappingHintsByParamCount[paramCount],
                typeFlagsByParamIdx);
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
    
    Iterator/*<ReflectionCallableMemberDescriptor>*/ getMemberDescriptors() {
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
     * Gets the "type flags" of each parameter positions, or {@code null} if there's no method with this parameter
     * count or if we are in pre-2.3.21 mode, or {@link #ALL_ZEROS_ARRAY} if there were no parameters that turned
     * on a flag. The returned {@code int}-s are one or more {@link TypeFlags} constants binary "or"-ed together.  
     */
    final protected int[] getTypeFlags(int paramCount) {
        return typeFlagsByParamCount != null && typeFlagsByParamCount.length > paramCount
                ? typeFlagsByParamCount[paramCount]
                : null;
    }

    /**
     * Updates the content of the {@link #typeFlagsByParamCount} field with the parameter type flags of a method.
     * Don't call this when {@link #bugfixed} is {@code false}! 
     * 
     * @param dstParamCount The parameter count for which we want to merge in the type flags 
     * @param srcTypeFlagsByParamIdx If shorter than {@code dstParamCount}, its last item will be repeated until
     *        dstParamCount length is reached. If longer, the excessive items will be ignored.
     *        Maybe {@link #ALL_ZEROS_ARRAY}. Maybe a 0-length array. Can't be {@code null}.
     */
    final protected void mergeInTypesFlags(int dstParamCount, int[] srcTypeFlagsByParamIdx) {
        NullArgumentException.check("srcTypesFlagsByParamIdx", srcTypeFlagsByParamIdx);
        
        // Special case of 0 param count:
        if (dstParamCount == 0) {
            if (typeFlagsByParamCount == null) {
                typeFlagsByParamCount = ZERO_PARAM_COUNT_TYPE_FLAGS_ARRAY;
            } else if (typeFlagsByParamCount != ZERO_PARAM_COUNT_TYPE_FLAGS_ARRAY) {
                typeFlagsByParamCount[0] = ALL_ZEROS_ARRAY;
            }
            return;
        }
        
        // Ensure that typesFlagsByParamCount[dstParamCount] exists:
        if (typeFlagsByParamCount == null) {
            typeFlagsByParamCount = new int[dstParamCount + 1][];
        } else if (typeFlagsByParamCount.length <= dstParamCount) {
            int[][] newTypeFlagsByParamCount = new int[dstParamCount + 1][];
            System.arraycopy(typeFlagsByParamCount, 0, newTypeFlagsByParamCount, 0,
                    typeFlagsByParamCount.length);
            typeFlagsByParamCount = newTypeFlagsByParamCount;
        }
        
        int[] dstTypeFlagsByParamIdx = typeFlagsByParamCount[dstParamCount];
        if (dstTypeFlagsByParamIdx == null) {
            // This is the first method added with this number of params => no merging
            
            if (srcTypeFlagsByParamIdx != ALL_ZEROS_ARRAY) {
                int srcParamCount = srcTypeFlagsByParamIdx.length;
                dstTypeFlagsByParamIdx = new int[dstParamCount];
                for (int paramIdx = 0; paramIdx < dstParamCount; paramIdx++) {
                    dstTypeFlagsByParamIdx[paramIdx]
                            = srcTypeFlagsByParamIdx[paramIdx < srcParamCount ? paramIdx : srcParamCount - 1];
                }
            } else {
                dstTypeFlagsByParamIdx = ALL_ZEROS_ARRAY;
            }
            
            typeFlagsByParamCount[dstParamCount] = dstTypeFlagsByParamIdx;
        } else {
            // dstTypeFlagsByParamIdx != null, so we need to merge into it.
            
            if (srcTypeFlagsByParamIdx == dstTypeFlagsByParamIdx) {
                // Used to occur when both are ALL_ZEROS_ARRAY
                return;
            }
            
            // As we will write dstTypeFlagsByParamIdx, it can't remain ALL_ZEROS_ARRAY anymore. 
            if (dstTypeFlagsByParamIdx == ALL_ZEROS_ARRAY && dstParamCount > 0) {
                dstTypeFlagsByParamIdx = new int[dstParamCount];
                typeFlagsByParamCount[dstParamCount] = dstTypeFlagsByParamIdx;
            }
            
            for (int paramIdx = 0; paramIdx < dstParamCount; paramIdx++) {
                final int srcParamTypeFlags;
                if (srcTypeFlagsByParamIdx != ALL_ZEROS_ARRAY) {
                    int srcParamCount = srcTypeFlagsByParamIdx.length;
                    srcParamTypeFlags = srcTypeFlagsByParamIdx[paramIdx < srcParamCount ? paramIdx : srcParamCount - 1]; 
                } else {
                    srcParamTypeFlags = 0;
                }
                
                final int dstParamTypesFlags = dstTypeFlagsByParamIdx[paramIdx];
                if (dstParamTypesFlags != srcParamTypeFlags) {
                    int mergedTypeFlags = dstParamTypesFlags | srcParamTypeFlags;
                    if ((mergedTypeFlags & TypeFlags.MASK_ALL_NUMERICALS) != 0) {
                        // Must not be set if we don't have numerical type at this index! 
                        mergedTypeFlags |= TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT;
                    }
                    dstTypeFlagsByParamIdx[paramIdx] = mergedTypeFlags; 
                }
            }
        }
    }
    
    protected void forceNumberArgumentsToParameterTypes(
            Object[] args, Class[] paramTypes, int[] typeFlagsByParamIndex) {
        final int paramTypesLen = paramTypes.length;
        final int argsLen = args.length;
        for (int argIdx = 0; argIdx < argsLen; argIdx++) {
            final int paramTypeIdx = argIdx < paramTypesLen ? argIdx : paramTypesLen - 1;
            final int typeFlags = typeFlagsByParamIndex[paramTypeIdx];
            
            // Forcing the number type can only be interesting if there are numerical parameter types on that index,
            // and the unwrapping was not to an exact numerical type.
            if ((typeFlags & TypeFlags.WIDENED_NUMERICAL_UNWRAPPING_HINT) != 0) {
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

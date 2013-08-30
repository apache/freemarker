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

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;


/**
 * The argument types of a method call; usable as cache key.
 * 
 * @author Attila Szegedi
 */
final class ArgumentTypes {
    
    private static final int BIG_MANTISSA_LOSS_PRICE = 40000;

    /**
     * The types of the arguments; for varags this contains the exploded list (not the array). 
     */
    private final Class[] types;
    
    private final boolean bugfixed;
    
    /**
     * @param args The actual arguments. A varargs argument should be present exploded, no as an array.
     * @param bugfixed Introduced in 2.3.21, sets this object to a mode that works well with {@link BeansWrapper}-s
     *      created with {@link Version} 2.3.21 or higher.
     */
    ArgumentTypes(Object[] args, boolean bugfixed) {
        int l = args.length;
        types = new Class[l];
        for(int i = 0; i < l; ++i) {
            Object obj = args[i];
            types[i] = obj == null
                    ? (bugfixed ? Null.class : Object.class)
                    : obj.getClass();
        }
        this.bugfixed = bugfixed;
    }
    
    public int hashCode() {
        int hash = 0;
        for(int i = 0; i < types.length; ++i) {
            hash ^= types[i].hashCode();
        }
        return hash;
    }
    
    public boolean equals(Object o) {
        if(o instanceof ArgumentTypes) {
            ArgumentTypes cs = (ArgumentTypes)o;
            if(cs.types.length != types.length) {
                return false;
            }
            for(int i = 0; i < types.length; ++i) {
                if(cs.types[i] != types[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * @return Possibly {@link EmptyCallableMemberDescriptor#NO_SUCH_METHOD} or
     *         {@link EmptyCallableMemberDescriptor#AMBIGUOUS_METHOD}. 
     */
    MaybeEmptyCallableMemberDescriptor getMostSpecific(List/*<CallableMemberDescriptor>*/ memberDescs, boolean varArg)
    {
        LinkedList/*<CallableMemberDescriptor>*/ applicables = getApplicables(memberDescs, varArg);
        if(applicables.isEmpty()) {
            return EmptyCallableMemberDescriptor.NO_SUCH_METHOD;
        }
        if(applicables.size() == 1) {
            return (CallableMemberDescriptor) applicables.getFirst();
        }
        
        LinkedList/*<CallableMemberDescriptor>*/ maximals = new LinkedList();
        for (Iterator applicablesIter = applicables.iterator(); applicablesIter.hasNext();)
        {
            CallableMemberDescriptor applicable = (CallableMemberDescriptor) applicablesIter.next();
            boolean lessSpecific = false;
            for (Iterator maximalsIter = maximals.iterator(); 
                maximalsIter.hasNext();)
            {
                CallableMemberDescriptor maximal = (CallableMemberDescriptor) maximalsIter.next();
                final int cmpRes = compareParameterListPreferability(applicable.paramTypes, maximal.paramTypes, varArg); 
                if (cmpRes > 0) {
                    maximalsIter.remove();
                } else if (cmpRes < 0) {
                    lessSpecific = true;
                }
            }
            if(!lessSpecific) {
                maximals.addLast(applicable);
            }
        }
        if(maximals.size() > 1) {
            return EmptyCallableMemberDescriptor.AMBIGUOUS_METHOD;
        }
        return (CallableMemberDescriptor) maximals.getFirst();
    }

    /**
     * Tells if among the parameter list of two methods, which one fits this argument list better.
     * This method assumes that the parameter lists are applicable to this argument lists; if that's not ensured,
     * what the result will be is undefined.
     * 
     * <p>This method behaves differently in {@code bugfixed}-mode (used when a {@link BeansWrapper} is created with
     * incompatible improvements set to 2.3.21 or higher). Below we describe the bugfixed behavior only. 
     *  
     * <p>The decision is made by comparing the preferability of each parameter types of the same position in a loop.
     * At the end, the parameter list with the more preferred parameters will be the preferred one. If both parameter
     * lists has the same amount of preferred parameters, the one that has the first (lower index) preferred parameter
     * is the preferred one. Otherwise the two parameter list are considered to be equal in terms of preferability.
     * 
     * <p>If there's no numerical conversion involved, the preferability of two parameter types is decided on how
     * specific their types are. For example, {@code String} is more specific than {@link Object} (because
     * {@code Object.class.isAssignableFrom(String.class)}-s), and so {@code String} is preferred. Primitive
     * types are considered to be more specific than the corresponding boxing class (like {@code boolean} is more
     * specific than {@code Boolean}, because the former can't store {@code null}). The preferability decision gets
     * trickier when there's a possibility of numerical conversion from the actual argument type to the type of some of
     * the parameters. If such conversion is only possible for one of the competing parameter types, that parameter
     * automatically wins. If it's possible for both, {@link OverloadedNumberUtil#getArgumentConversionPrice} will
     * be used to calculate the conversion "price", and the parameter type with lowest price wins.   
     * 
     * @param paramTypes1 The parameter types of one of the competing methods
     * @param paramTypes2 The parameter types of the other competing method
     * @param varArg Whether these competing methods are varargs methods. 
     * @return More than 0 if the first parameter list is preferred, less then 0 if the other is preferred,
     *        0 if there's no decision 
     */
    int compareParameterListPreferability(Class[] paramTypes1, Class[] paramTypes2, boolean varArg) {
        final int argTypesLen = types.length; 
        final int paramTypes1Len = paramTypes1.length;
        final int paramTypes2Len = paramTypes2.length;
        //assert varArg || paramTypes1Len == paramTypes2Len;
        
        if (bugfixed) {
            int paramList1WinCnt = 0;
            int paramList2WinCnt = 0;
            int paramList1StrongWinCnt = 0;
            int paramList2StrongWinCnt = 0;
            int firstWinerParamList = 0;
            for (int i = 0; i < argTypesLen; i++) {
                final Class paramType1 = getParamType(paramTypes1, paramTypes1Len, i, varArg);
                final Class paramType2 = getParamType(paramTypes2, paramTypes2Len, i, varArg);
                
                final int winerParam;  // 1 => paramType1; -1 => paramType2; 0 => draw
                if (paramType1 == paramType2) {
                    winerParam = 0;
                } else {
                    final Class argType = types[i];
                    final boolean argIsNum = Number.class.isAssignableFrom(argType);
                    
                    final int numConvPrice1;
                    if (argIsNum && ClassUtil.isNumerical(paramType1)) {
                        final Class nonPrimParamType1 = paramType1.isPrimitive()
                                ? ClassUtil.primitiveClassToBoxingClass(paramType1) : paramType1; 
                        numConvPrice1 = OverloadedNumberUtil.getArgumentConversionPrice(argType, nonPrimParamType1);
                    } else {
                        numConvPrice1 = Integer.MAX_VALUE;
                    }
                    // numConvPrice1 is Integer.MAX_VALUE if either:
                    // - argType and paramType1 aren't both numerical
                    // - FM doesn't know some of the numerical types, or the conversion between them is not allowed    
                    
                    final int numConvPrice2;
                    if (argIsNum && ClassUtil.isNumerical(paramType2)) {
                        final Class nonPrimParamType2 = paramType2.isPrimitive()
                                ? ClassUtil.primitiveClassToBoxingClass(paramType2) : paramType2; 
                        numConvPrice2 = OverloadedNumberUtil.getArgumentConversionPrice(argType, nonPrimParamType2);
                    } else {
                        numConvPrice2 = Integer.MAX_VALUE;
                    }
                    
                    if (numConvPrice1 == Integer.MAX_VALUE) {
                        if (numConvPrice2 == Integer.MAX_VALUE) {  // No numerical conversions anywhere
                            winerParam = compareParameterListPreferability_cmpTypeSpecificty(paramType1, paramType2);
                        } else {     // No num. conv. of param1, num. conv. of param2
                            winerParam = -1;
                        }
                    } else if (numConvPrice2 == Integer.MAX_VALUE) {  // Num. conv. of param1, not of param2
                        winerParam = 1;
                    } else {  // Num. conv. of both param1 and param2
                        if (numConvPrice1 != numConvPrice2) {
                            if (numConvPrice1 < numConvPrice2) {
                                winerParam = 1;
                                if (numConvPrice1 < BIG_MANTISSA_LOSS_PRICE && numConvPrice2 > BIG_MANTISSA_LOSS_PRICE) {
                                    paramList1StrongWinCnt++;
                                }
                            } else {
                                winerParam = -1;
                                if (numConvPrice2 < BIG_MANTISSA_LOSS_PRICE && numConvPrice1 > BIG_MANTISSA_LOSS_PRICE) {
                                    paramList2StrongWinCnt++;
                                }
                            }
                        } else {
                            winerParam = (paramType1.isPrimitive() ? 1 : 0) - (paramType2.isPrimitive() ? 1 : 0);
                        }
                    }
                }  // when paramType1 != paramType2
                
                if (winerParam != 0) {
                    if (firstWinerParamList == 0) {
                        firstWinerParamList = winerParam; 
                    }
                    if (winerParam == 1) {
                        paramList1WinCnt++;
                    } else if (winerParam == -1) {
                        paramList2WinCnt++;
                    } else {
                        throw new RuntimeException();
                    }
                }
            }  // for each parameter types
            
            if (paramList1StrongWinCnt != paramList2StrongWinCnt) {
                return paramList1StrongWinCnt - paramList2StrongWinCnt;
            } else if (paramList1WinCnt != paramList2WinCnt) {
                return paramList1WinCnt - paramList2WinCnt;
            } else if (firstWinerParamList != 0) {  // paramList1WinCnt == paramList2WinCnt
                return firstWinerParamList;
            } else { // still undecided
                if (varArg) {
                    if (paramTypes1Len == paramTypes2Len) {
                        // If we had a 0-length varargs array in both methods, we also compare the types at the
                        // index of the varargs parameter, like if we had a single varargs argument. However, this
                        // time we don't have an argument type, so we can only decide based on type specificity:
                        if (argTypesLen == paramTypes1Len - 1) {
                            Class paramType1 = getParamType(paramTypes1, paramTypes1Len, argTypesLen, true);
                            Class paramType2 = getParamType(paramTypes2, paramTypes2Len, argTypesLen, true);
                            if (ClassUtil.isNumerical(paramType1) && ClassUtil.isNumerical(paramType2)) {
                                int r = OverloadedNumberUtil.compareNumberTypeSpecificity(paramType1, paramType2);
                                if (r != 0) return r;
                                // falls through
                            }
                            return compareParameterListPreferability_cmpTypeSpecificty(paramType1, paramType2);
                        } else {
                            return 0;
                        }
                    } else {
                        // The method with more fixed parameters wins:
                        return paramTypes1Len - paramTypes2Len;
                    }
                } else {
                    return 0;
                }
            }
        } else { // non-bugfixed (backward-compatible) mode
            boolean paramTypes1HasAMoreSpecific = false;
            boolean paramTypes2HasAMoreSpecific = false;
            for(int i = 0; i < paramTypes1Len; ++i) {
                Class paramType1 = getParamType(paramTypes1, paramTypes1Len, i, varArg);
                Class paramType2 = getParamType(paramTypes2, paramTypes2Len, i, varArg);
                if(paramType1 != paramType2) {
                    paramTypes1HasAMoreSpecific = 
                        paramTypes1HasAMoreSpecific
                        || MethodUtilities.isMoreOrSameSpecificParameterType(paramType1, paramType2, false, 0) != 0;
                    paramTypes2HasAMoreSpecific = 
                        paramTypes2HasAMoreSpecific
                        || MethodUtilities.isMoreOrSameSpecificParameterType(paramType2, paramType1, false, 0) != 0;
                }
            }
            
            if(paramTypes1HasAMoreSpecific) {
                return paramTypes2HasAMoreSpecific ? 0 : 1;
            } else if(paramTypes2HasAMoreSpecific) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    
    private int compareParameterListPreferability_cmpTypeSpecificty(final Class paramType1, final Class paramType2) {
        // The more specific (smaller) type wins.
        
        final Class nonPrimParamType1 = paramType1.isPrimitive()
                ? ClassUtil.primitiveClassToBoxingClass(paramType1) : paramType1; 
        final Class nonPrimParamType2 = paramType2.isPrimitive()
                ? ClassUtil.primitiveClassToBoxingClass(paramType2) : paramType2;
                
        if (nonPrimParamType1 == nonPrimParamType2) {
            if (nonPrimParamType1 != paramType1) {
                if (nonPrimParamType2 != paramType2) {
                    return 0;  // identical prim. types; shouldn't ever be reached
                } else {
                    return 1;  // param1 is prim., param2 is non prim.
                }
            } else if (nonPrimParamType2 != paramType2) {
                return -1;  // param1 is non-prim., param2 is prim.
            } else {
                return 0;  // identical non-prim. types
            }
        } else if (nonPrimParamType2.isAssignableFrom(nonPrimParamType1)) {
            return 1;
        } else if (nonPrimParamType1.isAssignableFrom(nonPrimParamType2)) {
            return -1;
        } else  {
            return 0;  // unrelated types
        }
    }

    private static Class getParamType(Class[] paramTypes, int paramTypesLen, int i, boolean varArg) {
        return varArg && i >= paramTypesLen - 1
                ? paramTypes[paramTypesLen - 1].getComponentType()
                : paramTypes[i];
    }
    
    /**
     * Returns all methods that are applicable to actual
     * parameter types represented by this ArgumentTypes object.
     */
    LinkedList/*<CallableMemberDescriptor>*/ getApplicables(List/*<CallableMemberDescriptor>*/ memberDescs, boolean varArg) {
        LinkedList/*<CallableMemberDescriptor>*/ list = new LinkedList();
        for (Iterator it = memberDescs.iterator(); it.hasNext();) {
            CallableMemberDescriptor memberDesc = (CallableMemberDescriptor) it.next();
            if(isApplicable(memberDesc, varArg)) {
                list.add(memberDesc);
            }
        }
        return list;
    }
    
    /**
     * Returns true if the supplied method is applicable to actual
     * parameter types represented by this ArgumentTypes object.
     */
    private boolean isApplicable(CallableMemberDescriptor memberDesc, boolean varArg) {
        final Class[] paramTypes = memberDesc.paramTypes; 
        final int cl = types.length;
        final int fl = paramTypes.length - (varArg ? 1 : 0);
        if(varArg) {
            if(cl < fl) {
                return false;
            }
        } else {
            if(cl != fl) {
                return false;
            }
        }
        for(int i = 0; i < fl; ++i) {
            if(!isMethodInvocationConvertible(paramTypes[i], types[i])) {
                return false;
            }
        }
        if(varArg) {
            Class varArgParamType = paramTypes[fl].getComponentType();
            for(int i = fl; i < cl; ++i) {
                if(!isMethodInvocationConvertible(varArgParamType, types[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a 
     * method invocation conversion, treating object types of primitive 
     * types as if they were primitive types (that is, a Boolean actual 
     * parameter type matches boolean primitive formal type). This behavior
     * is because this method is used to determine applicable methods for 
     * an actual parameter list, and primitive types are represented by 
     * their object duals in reflective method calls.
     * @param formal the parameter type to which the actual 
     * parameter type should be convertible; possibly a primitive type
     * @param actual the argument type; not a primitive type, maybe {@link Null}.
     * @return true if either formal type is assignable from actual type, 
     * or the formal type is a primitive type and the actual type is its corresponding object
     * type or an object type of a primitive type that can be converted to
     * the formal type.
     */
    private boolean isMethodInvocationConvertible(final Class formal, final Class actual) {
        // Check for identity or widening reference conversion
        if(formal.isAssignableFrom(actual)) {
            return true;
        } else if (actual == Null.class && bugfixed) {
            return !formal.isPrimitive();
        } else if (bugfixed) {
            final Class formalNP;
            if (formal.isPrimitive()) {
                formalNP = ClassUtil.primitiveClassToBoxingClass(formal);
                if (actual == formalNP) {
                    // Character and char, etc.
                    return true;
                }
            } else {
                formalNP = formal;
            }
            if (Number.class.isAssignableFrom(actual) && Number.class.isAssignableFrom(formalNP)) {
                return OverloadedNumberUtil.getArgumentConversionPrice(actual, formalNP) != Integer.MAX_VALUE;
            } else {
                return false;
            }
        } else { // if !bugfixed
            // This non-bugfixed (backward-compatibile) branch:
            // - Doesn't convert *to* non-primitive numerical types (unless the argument is a BigDecimal).
            //   (This is like in Java language, which also doesn't coerce to non-primitive numerical types.) 
            // - Doesn't support BigInteger conversions
            // - Doesn't support NumberWithFallbackType-s. Those are only produced in bugfixed mode anyway. 
            if(formal.isPrimitive()) {
                // Check for boxing with widening primitive conversion. Note that 
                // actual parameters are never primitives.
                // It doesn't do the same with boxing types... that was a bug.
                if(formal == Boolean.TYPE) {
                    return actual == Boolean.class;
                } else if (formal == Double.TYPE && 
                        (actual == Double.class || actual == Float.class || 
                         actual == Long.class || actual == Integer.class || 
                         actual == Short.class || actual == Byte.class)) {
                     return true;
                } else if (formal == Integer.TYPE && 
                        (actual == Integer.class || actual == Short.class || 
                         actual == Byte.class)) {
                     return true;
                } else if (formal == Long.TYPE && 
                        (actual == Long.class || actual == Integer.class || 
                         actual == Short.class || actual == Byte.class)) {
                     return true;
                } else if (formal == Float.TYPE && 
                        (actual == Float.class || actual == Long.class || 
                         actual == Integer.class || actual == Short.class || 
                         actual == Byte.class)) {
                     return true;
                } else if (formal == Character.TYPE) {
                    return actual == Character.class;
                } else if(formal == Byte.TYPE && actual == Byte.class) {
                    return true;
                } else if(formal == Short.TYPE &&
                   (actual == Short.class || actual == Byte.class)) {
                    return true;
                } else if (BigDecimal.class.isAssignableFrom(actual) && ClassUtil.isNumerical(formal)) {
                    // Special case for BigDecimals as we deem BigDecimal to be
                    // convertible to any numeric type - either object or primitive.
                    // This can actually cause us trouble as this is a narrowing 
                    // conversion, not widening. 
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    
    /**
     * Symbolizes the class of null (it's missing from Java).
     */
    private static class Null {
        
        // Can't be instantiated
        private Null() { }
        
    }
    
}
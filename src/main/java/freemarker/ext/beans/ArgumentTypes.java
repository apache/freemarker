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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import freemarker.core.BugException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import freemarker.template.utility.ClassUtil;

/**
 * The argument types of a method call; usable as cache key.
 */
final class ArgumentTypes {
    
    /**
     * Conversion difficulty: Lowest; Java Reflection will do it automatically.
     */
    private static final int CONVERSION_DIFFICULTY_REFLECTION = 0;

    /**
     * Conversion difficulty: Java reflection API will won't convert it, FreeMarker has to do it.
     */
    private static final int CONVERSION_DIFFICULTY_FREEMARKER = 1;
    
    /**
     * Conversion difficulty: Highest; conversion is not possible.
     */
    private static final int CONVERSION_DIFFICULTY_IMPOSSIBLE = 2;

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
        int ln = args.length;
        Class[] typesTmp = new Class[ln];
        for(int i = 0; i < ln; ++i) {
            Object arg = args[i];
            typesTmp[i] = arg == null
                    ? (bugfixed ? Null.class : Object.class)
                    : arg.getClass();
        }
        
        // `typesTmp` is used so the array is only modified before it's stored in the final `types` field (see JSR-133)
        types = typesTmp;  
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
    MaybeEmptyCallableMemberDescriptor getMostSpecific(
            List/*<ReflectionCallableMemberDescriptor>*/ memberDescs, boolean varArg)
    {
        LinkedList/*<ReflectionCallableMemberDescriptor>*/ applicables = getApplicables(memberDescs, varArg);
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
                final int cmpRes = compareParameterListPreferability(
                        applicable.getParamTypes(), maximal.getParamTypes(), varArg); 
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
     * be used to calculate the conversion "price", and the parameter type with lowest price wins. There are also
     * a twist with array-to-list and list-to-array conversions; we try to avoid those, so the parameter where such
     * conversion isn't needed will always win.
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
            int paramList1WeakWinCnt = 0;
            int paramList2WeakWinCnt = 0;
            int paramList1WinCnt = 0;
            int paramList2WinCnt = 0;
            int paramList1StrongWinCnt = 0;
            int paramList2StrongWinCnt = 0;
            int paramList1VeryStrongWinCnt = 0;
            int paramList2VeryStrongWinCnt = 0;
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
                            // List to array conversions (unwrapping sometimes makes a List instead of an array)
                            if (List.class.isAssignableFrom(argType)
                                    && (paramType1.isArray() || paramType2.isArray())) {
                                if (paramType1.isArray()) {
                                    if (paramType2.isArray()) {  // both paramType1 and paramType2 are arrays
                                        int r = compareParameterListPreferability_cmpTypeSpecificty(
                                                paramType1.getComponentType(), paramType2.getComponentType());
                                        // Because we don't know if the List items are instances of the component
                                        // type or not, we prefer the safer choice, which is the more generic array:
                                        if (r > 0) {
                                            winerParam = 2;
                                            paramList2StrongWinCnt++;
                                        } else if (r < 0) {
                                            winerParam = 1;
                                            paramList1StrongWinCnt++;
                                        } else {
                                            winerParam = 0;
                                        }
                                    } else {  // paramType1 is array, paramType2 isn't
                                        // Avoid List to array conversion if the other way makes any sense:
                                        if (Collection.class.isAssignableFrom(paramType2)) {
                                            winerParam = 2;
                                            paramList2StrongWinCnt++;
                                        } else {
                                            winerParam = 1;
                                            paramList1WeakWinCnt++;
                                        }
                                    }
                                } else {  // paramType2 is array, paramType1 isn't
                                    // Avoid List to array conversion if the other way makes any sense:
                                    if (Collection.class.isAssignableFrom(paramType1)) {
                                        winerParam = 1;
                                        paramList1StrongWinCnt++;
                                    } else {
                                        winerParam = 2;
                                        paramList2WeakWinCnt++;
                                    }
                                }
                            } else if (argType.isArray()
                                    && (List.class.isAssignableFrom(paramType1)
                                            || List.class.isAssignableFrom(paramType2))) {
                                // Array to List conversions (unwrapping sometimes makes an array instead of a List)
                                if (List.class.isAssignableFrom(paramType1)) {
                                    if (List.class.isAssignableFrom(paramType2)) {
                                        // Both paramType1 and paramType2 extends List
                                        winerParam = 0;
                                    } else {
                                        // Only paramType1 extends List
                                        winerParam = 2;
                                        paramList2VeryStrongWinCnt++;
                                    }
                                } else {
                                    // Only paramType2 extends List
                                    winerParam = 1;
                                    paramList1VeryStrongWinCnt++;
                                }
                            } else {  // No list to/from array conversion
                                final int r = compareParameterListPreferability_cmpTypeSpecificty(
                                        paramType1, paramType2);
                                if (r > 0) {
                                    winerParam = 1;
                                    if (r > 1) {
                                        paramList1WinCnt++;
                                    } else {
                                        paramList1WeakWinCnt++;
                                    }
                                } else if (r < 0) {
                                    winerParam = -1;
                                    if (r < -1) {
                                        paramList2WinCnt++;
                                    } else {
                                        paramList2WeakWinCnt++;
                                    }
                                } else {
                                    winerParam = 0;
                                }
                            }
                        } else {  // No num. conv. of param1, num. conv. of param2
                            winerParam = -1;
                            paramList2WinCnt++;
                        }
                    } else if (numConvPrice2 == Integer.MAX_VALUE) {  // Num. conv. of param1, not of param2
                        winerParam = 1;
                        paramList1WinCnt++;
                    } else {  // Num. conv. of both param1 and param2
                        if (numConvPrice1 != numConvPrice2) {
                            if (numConvPrice1 < numConvPrice2) {
                                winerParam = 1;
                                if (numConvPrice1 < OverloadedNumberUtil.BIG_MANTISSA_LOSS_PRICE
                                        && numConvPrice2 > OverloadedNumberUtil.BIG_MANTISSA_LOSS_PRICE) {
                                    paramList1StrongWinCnt++;
                                } else {
                                    paramList1WinCnt++;
                                }
                            } else {
                                winerParam = -1;
                                if (numConvPrice2 < OverloadedNumberUtil.BIG_MANTISSA_LOSS_PRICE
                                        && numConvPrice1 > OverloadedNumberUtil.BIG_MANTISSA_LOSS_PRICE) {
                                    paramList2StrongWinCnt++;
                                } else {
                                    paramList2WinCnt++;
                                }
                            }
                        } else {
                            winerParam = (paramType1.isPrimitive() ? 1 : 0) - (paramType2.isPrimitive() ? 1 : 0);
                            if (winerParam == 1) paramList1WeakWinCnt++;
                            else if (winerParam == -1) paramList2WeakWinCnt++;
                        }
                    }
                }  // when paramType1 != paramType2
                
                if (firstWinerParamList == 0 && winerParam != 0) {
                    firstWinerParamList = winerParam; 
                }
            }  // for each parameter types
            
            if (paramList1VeryStrongWinCnt != paramList2VeryStrongWinCnt) {
                return paramList1VeryStrongWinCnt - paramList2VeryStrongWinCnt;
            } else if (paramList1StrongWinCnt != paramList2StrongWinCnt) {
                return paramList1StrongWinCnt - paramList2StrongWinCnt;
            } else if (paramList1WinCnt != paramList2WinCnt) {
                return paramList1WinCnt - paramList2WinCnt;
            } else if (paramList1WeakWinCnt != paramList2WeakWinCnt) {
                return paramList1WeakWinCnt - paramList2WeakWinCnt;
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
                        || _MethodUtil.isMoreOrSameSpecificParameterType(paramType1, paramType2, false, 0) != 0;
                    paramTypes2HasAMoreSpecific = 
                        paramTypes2HasAMoreSpecific
                        || _MethodUtil.isMoreOrSameSpecificParameterType(paramType2, paramType1, false, 0) != 0;
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
    
    /**
     * Trivial comparison of type specificities; unaware of numerical conversions. 
     * 
     * @return Less-than-0, 0, or more-than-0 depending on which side is more specific. The absolute value is 1 if
     *     the difference is only in primitive VS non-primitive, more otherwise.
     */
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
            return 2;
        } else if (nonPrimParamType1.isAssignableFrom(nonPrimParamType2)) {
            return -2;
        } if (nonPrimParamType1 == Character.class && nonPrimParamType2.isAssignableFrom(String.class)) {
            return 2;  // A character is a 1 long string in FTL, so we pretend that it's a String subtype.
        } if (nonPrimParamType2 == Character.class && nonPrimParamType1.isAssignableFrom(String.class)) {
            return -2;
        } else {
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
    LinkedList/*<ReflectionCallableMemberDescriptor>*/ getApplicables(
            List/*<ReflectionCallableMemberDescriptor>*/ memberDescs, boolean varArg) {
        LinkedList applicables = new LinkedList();
        for (Iterator it = memberDescs.iterator(); it.hasNext();) {
            ReflectionCallableMemberDescriptor memberDesc = (ReflectionCallableMemberDescriptor) it.next();
            int difficulty = isApplicable(memberDesc, varArg);
            if (difficulty != CONVERSION_DIFFICULTY_IMPOSSIBLE) {
                if(difficulty == CONVERSION_DIFFICULTY_REFLECTION) {
                    applicables.add(memberDesc);
                } else if (difficulty == CONVERSION_DIFFICULTY_FREEMARKER) {
                    applicables.add(new SpecialConversionCallableMemberDescriptor(memberDesc));
                } else {
                    throw new BugException();
                }
            }
        }
        return applicables;
    }
    
    /**
     * Returns if the supplied method is applicable to actual
     * parameter types represented by this ArgumentTypes object, also tells
     * how difficult that conversion is.
     * 
     * @return One of the <tt>CONVERSION_DIFFICULTY_...</tt> constants.
     */
    private int isApplicable(ReflectionCallableMemberDescriptor memberDesc, boolean varArg) {
        final Class[] paramTypes = memberDesc.getParamTypes(); 
        final int cl = types.length;
        final int fl = paramTypes.length - (varArg ? 1 : 0);
        if(varArg) {
            if(cl < fl) {
                return CONVERSION_DIFFICULTY_IMPOSSIBLE;
            }
        } else {
            if(cl != fl) {
                return CONVERSION_DIFFICULTY_IMPOSSIBLE;
            }
        }
        
        int maxDifficulty = 0;
        for(int i = 0; i < fl; ++i) {
            int difficulty = isMethodInvocationConvertible(paramTypes[i], types[i]);
            if(difficulty == CONVERSION_DIFFICULTY_IMPOSSIBLE) {
                return CONVERSION_DIFFICULTY_IMPOSSIBLE;
            }
            if (maxDifficulty < difficulty) {
                maxDifficulty = difficulty;
            }
        }
        if(varArg) {
            Class varArgParamType = paramTypes[fl].getComponentType();
            for(int i = fl; i < cl; ++i) {
                int difficulty = isMethodInvocationConvertible(varArgParamType, types[i]); 
                if(difficulty == CONVERSION_DIFFICULTY_IMPOSSIBLE) {
                    return CONVERSION_DIFFICULTY_IMPOSSIBLE;
                }
                if (maxDifficulty < difficulty) {
                    maxDifficulty = difficulty;
                }
            }
        }
        return maxDifficulty;
    }

    /**
     * Determines whether a type is convertible to another type via 
     * method invocation conversion, and if so, what kind of conversion is needed.
     * It treates the object type counterpart of primitive types as if they were the primitive types
     * (that is, a Boolean actual parameter type matches boolean primitive formal type). This behavior
     * is because this method is used to determine applicable methods for 
     * an actual parameter list, and primitive types are represented by 
     * their object duals in reflective method calls.
     * @param formal the parameter type to which the actual 
     * parameter type should be convertible; possibly a primitive type
     * @param actual the argument type; not a primitive type, maybe {@link Null}.
     * 
     * @return One of the <tt>CONVERSION_DIFFICULTY_...</tt> constants.
     */
    private int isMethodInvocationConvertible(final Class formal, final Class actual) {
        // Check for identity or widening reference conversion
        if(formal.isAssignableFrom(actual) && actual != CharacterOrString.class) {
            return CONVERSION_DIFFICULTY_REFLECTION;
        } else if (bugfixed) {
            final Class formalNP;
            if (formal.isPrimitive()) {
                if (actual == Null.class) {
                    return CONVERSION_DIFFICULTY_IMPOSSIBLE;
                }
                
                formalNP = ClassUtil.primitiveClassToBoxingClass(formal);
                if (actual == formalNP) {
                    // Character and char, etc.
                    return CONVERSION_DIFFICULTY_REFLECTION;
                }
            } else {  // formal is non-primitive
                if (actual == Null.class) {
                    return CONVERSION_DIFFICULTY_REFLECTION;
                }
                
                formalNP = formal;
            }
            if (Number.class.isAssignableFrom(actual) && Number.class.isAssignableFrom(formalNP)) {
                return OverloadedNumberUtil.getArgumentConversionPrice(actual, formalNP) == Integer.MAX_VALUE
                        ? CONVERSION_DIFFICULTY_IMPOSSIBLE : CONVERSION_DIFFICULTY_REFLECTION;
            } else if (formal.isArray()) {
                // BeansWrapper method/constructor calls convert from List to array automatically
                return List.class.isAssignableFrom(actual)
                        ? CONVERSION_DIFFICULTY_FREEMARKER : CONVERSION_DIFFICULTY_IMPOSSIBLE;
            } else if (actual.isArray() && formal.isAssignableFrom(List.class)) {
                // BeansWrapper method/constructor calls convert from array to List automatically
                return CONVERSION_DIFFICULTY_FREEMARKER;
            } else if (actual == CharacterOrString.class
                    && (formal.isAssignableFrom(String.class)
                            || formal.isAssignableFrom(Character.class) || formal == char.class)) {
                return CONVERSION_DIFFICULTY_FREEMARKER;
            } else {
                return CONVERSION_DIFFICULTY_IMPOSSIBLE;
            }
        } else { // if !bugfixed
            // This non-bugfixed (backward-compatible, pre-2.3.21) branch:
            // - Doesn't convert *to* non-primitive numerical types (unless the argument is a BigDecimal).
            //   (This is like in Java language, which also doesn't coerce to non-primitive numerical types.) 
            // - Doesn't support BigInteger conversions
            // - Doesn't support NumberWithFallbackType-s and CharacterOrString-s. Those are only produced in bugfixed
            //   mode anyway.
            // - Doesn't support conversion between array and List
            if(formal.isPrimitive()) {
                // Check for boxing with widening primitive conversion. Note that 
                // actual parameters are never primitives.
                // It doesn't do the same with boxing types... that was a bug.
                if(formal == Boolean.TYPE) {
                    return actual == Boolean.class
                            ? CONVERSION_DIFFICULTY_REFLECTION : CONVERSION_DIFFICULTY_IMPOSSIBLE;
                } else if (formal == Double.TYPE && 
                        (actual == Double.class || actual == Float.class || 
                         actual == Long.class || actual == Integer.class || 
                         actual == Short.class || actual == Byte.class)) {
                     return CONVERSION_DIFFICULTY_REFLECTION;
                } else if (formal == Integer.TYPE && 
                        (actual == Integer.class || actual == Short.class || 
                         actual == Byte.class)) {
                     return CONVERSION_DIFFICULTY_REFLECTION;
                } else if (formal == Long.TYPE && 
                        (actual == Long.class || actual == Integer.class || 
                         actual == Short.class || actual == Byte.class)) {
                     return CONVERSION_DIFFICULTY_REFLECTION;
                } else if (formal == Float.TYPE && 
                        (actual == Float.class || actual == Long.class || 
                         actual == Integer.class || actual == Short.class || 
                         actual == Byte.class)) {
                     return CONVERSION_DIFFICULTY_REFLECTION;
                } else if (formal == Character.TYPE) {
                    return actual == Character.class
                            ? CONVERSION_DIFFICULTY_REFLECTION : CONVERSION_DIFFICULTY_IMPOSSIBLE;
                } else if(formal == Byte.TYPE && actual == Byte.class) {
                    return CONVERSION_DIFFICULTY_REFLECTION;
                } else if(formal == Short.TYPE &&
                   (actual == Short.class || actual == Byte.class)) {
                    return CONVERSION_DIFFICULTY_REFLECTION;
                } else if (BigDecimal.class.isAssignableFrom(actual) && ClassUtil.isNumerical(formal)) {
                    // Special case for BigDecimals as we deem BigDecimal to be
                    // convertible to any numeric type - either object or primitive.
                    // This can actually cause us trouble as this is a narrowing 
                    // conversion, not widening. 
                    return CONVERSION_DIFFICULTY_REFLECTION;
                } else {
                    return CONVERSION_DIFFICULTY_IMPOSSIBLE;
                }
            } else {
                return CONVERSION_DIFFICULTY_IMPOSSIBLE;
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
    
    /**
     * Used instead of {@link ReflectionCallableMemberDescriptor} when the method is only applicable
     * ({@link #isApplicable}) with conversion that Java reflection won't do. It delegates to a
     * {@link ReflectionCallableMemberDescriptor}, but it adds the necessary conversions to the invocation methods. 
     */
    private static final class SpecialConversionCallableMemberDescriptor extends CallableMemberDescriptor {
        
        private final ReflectionCallableMemberDescriptor callableMemberDesc;

        SpecialConversionCallableMemberDescriptor(ReflectionCallableMemberDescriptor callableMemberDesc) {
            this.callableMemberDesc = callableMemberDesc;
        }

        TemplateModel invokeMethod(BeansWrapper bw, Object obj, Object[] args) throws TemplateModelException,
                InvocationTargetException, IllegalAccessException {
            convertArgsToReflectionCompatible(bw, args);
            return callableMemberDesc.invokeMethod(bw, obj, args);
        }

        Object invokeConstructor(BeansWrapper bw, Object[] args) throws IllegalArgumentException,
                InstantiationException, IllegalAccessException, InvocationTargetException, TemplateModelException {
            convertArgsToReflectionCompatible(bw, args);
            return callableMemberDesc.invokeConstructor(bw, args);
        }

        String getDeclaration() {
            return callableMemberDesc.getDeclaration();
        }

        boolean isConstructor() {
            return callableMemberDesc.isConstructor();
        }

        boolean isStatic() {
            return callableMemberDesc.isStatic();
        }

        boolean isVarargs() {
            return callableMemberDesc.isVarargs();
        }

        Class[] getParamTypes() {
            return callableMemberDesc.getParamTypes();
        }
        
        String getName() {
            return callableMemberDesc.getName();
        }

        private void convertArgsToReflectionCompatible(BeansWrapper bw, Object[] args) throws TemplateModelException {
            Class[] paramTypes = callableMemberDesc.getParamTypes();
            int ln = paramTypes.length;
            for (int i = 0; i < ln; i++) {
                Class paramType = paramTypes[i];
                final Object arg = args[i];
                if (arg == null) continue;
                
                // Handle conversion between List and array types, in both directions. Java reflection won't do such
                // conversion, so we have to.
                // Most reflection-incompatible conversions were already addressed by the unwrapping. The reason
                // this one isn't is that for overloaded methods the hint of a given parameter position is often vague,
                // so we may end up with a List even if some parameter types at that position are arrays (remember, we
                // have to chose one unwrapping target type, despite that we have many possible overloaded methods), or
                // the other way around (that happens when AdapterTemplateMoldel returns an array).
                // Later, the overloaded method selection will assume that a List argument is applicable to an array
                // parameter, and that an array argument is applicable to a List parameter, so we end up with this
                // situation.
                if (paramType.isArray() && arg instanceof List) {
                   args[i] = bw.listToArray((List) arg, paramType, null);
                }
                if (arg.getClass().isArray() && paramType.isAssignableFrom(List.class)) {
                    args[i] = bw.arrayToList(arg);
                }
                
                // Handle the conversion from CharacterOrString to Character or String:
                if (arg instanceof CharacterOrString) {
                    if (paramType == Character.class || paramType == char.class
                            || (!paramType.isAssignableFrom(String.class)
                                    && paramType.isAssignableFrom(Character.class))) {
                        args[i] = new Character(((CharacterOrString) arg).getAsChar());
                    } else {
                        args[i] = ((CharacterOrString) arg).getAsString();
                    }
                }
            }
        }

    }
    
}
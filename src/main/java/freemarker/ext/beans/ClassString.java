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


/**
 * @author Attila Szegedi
 */
final class ClassString
{
    private final Class[] classes;
    private final boolean bugfixed;
    
    ClassString(Object[] objects, boolean bugfixed) {
        int l = objects.length;
        classes = new Class[l];
        for(int i = 0; i < l; ++i) {
            Object obj = objects[i];
            classes[i] = obj == null
                    ? (bugfixed ? Null.class : Object.class)
                    : obj.getClass();
        }
        this.bugfixed = bugfixed;
    }
    
    public int hashCode() {
        int hash = 0;
        for(int i = 0; i < classes.length; ++i) {
            hash ^= classes[i].hashCode();
        }
        return hash;
    }
    
    public boolean equals(Object o) {
        if(o instanceof ClassString) {
            ClassString cs = (ClassString)o;
            if(cs.classes.length != classes.length) {
                return false;
            }
            for(int i = 0; i < classes.length; ++i) {
                if(cs.classes[i] != classes[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * @return Possibly {@link EmptyCallableMemberDescriptor#NO_SUCH_METHOD} or {@link EmptyCallableMemberDescriptor#AMBIGUOUS_METHOD}. 
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
                switch(compareParameterTypesSpecificity(applicable.paramTypes, maximal.paramTypes, varArg)) {
                    case 1: {
                        maximalsIter.remove();
                        break;
                    }
                    case -1: {
                        lessSpecific = true;
                        break;
                    }
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
    
    private int compareParameterTypesSpecificity(Class[] paramTypes1, Class[] paramTypes2, boolean varArg) {
        final int paramTypes1Len = paramTypes1.length;
        final int paramTypes2Len = paramTypes2.length;
        //assert varArg || cl1 == cl2;
        
        if (bugfixed) {
            int currentWinner = 0;
            int currentHighScore = 0;
            for(int i = 0; i < paramTypes1Len; ++i) {
                Class paramType1 = getParamType(paramTypes1, paramTypes1Len, i, varArg);
                Class paramType2 = getParamType(paramTypes2, paramTypes2Len, i, varArg);
                if(paramType1 != paramType2) {
                    int score;
                    
                    score = MethodUtilities.isMoreOrSameSpecificParameterType(
                            paramType1, paramType2, true,
                            currentWinner == -1 ? currentHighScore - 1 : currentHighScore);
                    if (score > currentHighScore) {
                        currentHighScore = score;
                        currentWinner = 1;
                    } else if (score == currentHighScore && currentWinner == -1) {
                        currentWinner = 0;
                    }

                    score = MethodUtilities.isMoreOrSameSpecificParameterType(
                            paramType2, paramType1, true,
                            currentWinner == 1 ? currentHighScore - 1 : currentHighScore);
                    if (score > currentHighScore) {
                        currentHighScore = score;
                        currentWinner = -1;
                    } else if (score == currentHighScore && currentWinner == 1) {
                        currentWinner = 0;
                    }
                }
            }
            return currentWinner;
        } else {
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
    
    private static Class getParamType(Class[] paramTypes, int paramTypesLen, int i, boolean varArg) {
        return varArg && i >= paramTypesLen - 1
                ? paramTypes[paramTypesLen - 1].getComponentType()
                : paramTypes[i];
    }
    
    /**
     * Returns all methods that are applicable to actual
     * parameter classes represented by this ClassString object.
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
     * parameter classes represented by this ClassString object.
     */
    private boolean isApplicable(CallableMemberDescriptor memberDesc, boolean varArg) {
        final Class[] formalTypes = memberDesc.paramTypes; 
        final int cl = classes.length;
        final int fl = formalTypes.length - (varArg ? 1 : 0);
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
            if(!isMethodInvocationConvertible(formalTypes[i], classes[i])) {
                return false;
            }
        }
        if(varArg) {
            Class varArgType = formalTypes[fl].getComponentType();
            for(int i = fl; i < cl; ++i) {
                if(!isMethodInvocationConvertible(varArgType, classes[i])) {
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
     * @param formal the formal parameter type to which the actual 
     * parameter type should be convertible
     * @param actual the actual parameter type.
     * @return true if either formal type is assignable from actual type, 
     * or formal is a primitive type and actual is its corresponding object
     * type or an object type of a primitive type that can be converted to
     * the formal type.
     */
    private boolean isMethodInvocationConvertible(Class formal, Class actual) {
        // Check for identity or widening reference conversion
        if(formal.isAssignableFrom(actual)) {
            return true;
        }
        
        if (actual == Null.class && bugfixed) {
            return !formal.isPrimitive();
        }
        
        // Check for boxing with widening primitive conversion. Note that 
        // actual parameters are never primitives.
        // FIXME: Why we don't do the same with boxing types too?
        if(formal.isPrimitive()) {
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
            } else if (BigDecimal.class.isAssignableFrom(actual) && isNumerical(formal)) {
                // Special case for BigDecimals as we deem BigDecimal to be
                // convertible to any numeric type - either object or primitive.
                // This can actually cause us trouble as this is a narrowing 
                // conversion, not widening. 
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    private static boolean isNumerical(Class type) {
        return Number.class.isAssignableFrom(type)
                || type.isPrimitive() && type != Boolean.TYPE && type != Character.TYPE;
    }
 
    /**
     * Symbolizes the class of null (it's missing from Java).
     */
    private static class Null {
        
        // Can't be instantiated
        private Null() { }
        
    }
    
}
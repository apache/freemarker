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
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Attila Szegedi
 */
final class ClassString
{
    private static final Class BIGDECIMAL_CLASS = BigDecimal.class;
    private static final Class NUMBER_CLASS = Number.class;

    private final Class[] classes;
    
    ClassString(Object[] objects) {
        int l = objects.length;
        classes = new Class[l];
        for(int i = 0; i < l; ++i) {
            Object obj = objects[i];
            classes[i] = obj == null ? MethodUtilities.OBJECT_CLASS : obj.getClass();
        }
    }
    
    Class[] getClasses() {
        return classes;
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
    
    private static final int MORE_SPECIFIC = 0;
    private static final int LESS_SPECIFIC = 1;
    private static final int INDETERMINATE = 2;
    
    Object getMostSpecific(List methods, boolean varArg)
    {
        LinkedList applicables = getApplicables(methods, varArg);
        if(applicables.isEmpty()) {
            return OverloadedMethodsSubset.NO_SUCH_METHOD;
        }
        if(applicables.size() == 1) {
            return applicables.getFirst();
        }
        LinkedList maximals = new LinkedList();
        for (Iterator it = applicables.iterator(); it.hasNext();)
        {
            Member applicable = (Member)it.next();
            Class[] appArgs = MethodUtilities.getParameterTypes(applicable);
            boolean lessSpecific = false;
            for (Iterator maximal = maximals.iterator(); 
                maximal.hasNext();)
            {
                Member max = (Member)maximal.next();
                Class[] maxArgs = MethodUtilities.getParameterTypes(max);
                switch(moreSpecific(appArgs, maxArgs, varArg)) {
                    case MORE_SPECIFIC: {
                        maximal.remove();
                        break;
                    }
                    case LESS_SPECIFIC: {
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
            return OverloadedMethodsSubset.AMBIGUOUS_METHOD;
        }
        return maximals.getFirst();
    }
    
    private static int moreSpecific(Class[] c1, Class[] c2, boolean varArg) {
        boolean c1MoreSpecific = false;
        boolean c2MoreSpecific = false;
        final int cl1 = c1.length;
        final int cl2 = c2.length;
        //assert varArg || cl1 == cl2;
        for(int i = 0; i < cl1; ++i) {
            Class class1 = getClass(c1, cl1, i, varArg);
            Class class2 = getClass(c2, cl2, i, varArg);
            if(class1 != class2) {
                c1MoreSpecific = 
                    c1MoreSpecific ||
                    MethodUtilities.isMoreSpecific(class1, class2);
                c2MoreSpecific = 
                    c2MoreSpecific ||
                    MethodUtilities.isMoreSpecific(class2, class1);
            }
        }
        if(c1MoreSpecific) {
            if(c2MoreSpecific) {
                return INDETERMINATE;
            }
            return MORE_SPECIFIC;
        }
        if(c2MoreSpecific) {
            return LESS_SPECIFIC;
        }
        return INDETERMINATE;
    }
    
    private static Class getClass(Class[] classes, int l, int i, boolean varArg) {
        return varArg && i >= l - 1 ? classes[l - 1].getComponentType() : classes[i];
    }
    
    /**
     * Returns all methods that are applicable to actual
     * parameter classes represented by this ClassString object.
     */
    LinkedList getApplicables(List methods, boolean varArg) {
        LinkedList list = new LinkedList();
        for (Iterator it = methods.iterator(); it.hasNext();) {
            Member member = (Member)it.next();
            if(isApplicable(member, varArg)) {
                list.add(member);
            }
        }
        return list;
    }
    
    /**
     * Returns true if the supplied method is applicable to actual
     * parameter classes represented by this ClassString object.
     */
    private boolean isApplicable(Member member, boolean varArg) {
        final Class[] formalTypes = MethodUtilities.getParameterTypes(member);
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
    static boolean isMethodInvocationConvertible(Class formal, Class actual) {
        // Check for identity or widening reference conversion
        if(formal.isAssignableFrom(actual)) {
            return true;
        }
        // Check for boxing with widening primitive conversion. Note that 
        // actual parameters are never primitives.
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
            } else if (BIGDECIMAL_CLASS.isAssignableFrom(actual) && isNumerical(formal)) {
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
        return NUMBER_CLASS.isAssignableFrom(type)
                || type.isPrimitive() && type != Boolean.TYPE && type != Character.TYPE;
    }
    
}
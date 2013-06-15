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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import freemarker.template.utility.UndeclaredThrowableException;

class MethodUtilities
{
    static final Class OBJECT_CLASS = Object.class;
    private static final Method METHOD_IS_VARARGS = getIsVarArgsMethod(Method.class);
    private static final Method CONSTRUCTOR_IS_VARARGS = getIsVarArgsMethod(Constructor.class);
    
    // DD: This method is broken. It gives different result depending on the order of the argument values. Like
    // getMostSpecificCommonType(int.class, Integer.class) gives Object.class, but
    // getMostSpecificCommonType(Integer.class, int.class) gives Integer.class. And this actually influences the
    // overloaded method mechanism, because the order of the values depends on the order in which the Java reflection
    // API returns the methods, and that order "randomly" changes depending on factors like the J2SE version (5 VS 6).
    // I don't dare to fix this, because I don't want a FreeMarker update to break any apps that even if out of sheer
    // luck, but happen to work...
    static Class getMostSpecificCommonType(Class c1, Class c2) {
        if(c1 == c2) {
            return c1;
        }
        if(c2.isPrimitive()) {
            if(c2 == Byte.TYPE) c2 = Byte.class;
            else if(c2 == Short.TYPE) c2 = Short.class;
            else if(c2 == Character.TYPE) c2 = Character.class;
            else if(c2 == Integer.TYPE) c2 = Integer.class;
            else if(c2 == Float.TYPE) c2 = Float.class;
            else if(c2 == Long.TYPE) c2 = Long.class;
            else if(c2 == Double.TYPE) c2 = Double.class;
        }
        // DD: This appears to be wrong to me... What if c1.isPrimitive? A non-primitive is never assignable to a
        //     primitive, or the other way around.
        Set a1 = getAssignables(c1, c2);
        Set a2 = getAssignables(c2, c1);
        a1.retainAll(a2);
        if(a1.isEmpty()) {
            // Can happen when at least one of the arguments is an interface, as
            // they don't have Object at the root of their hierarchy
            return Object.class;
        }
        // Gather maximally specific elements. Yes, there can be more than one 
        // thank to interfaces. I.e., if you call this method for String.class 
        // and Number.class, you'll have Comparable, Serializable, and Object as 
        // maximal elements. 
        List max = new ArrayList();
outer:  for (Iterator it = a1.iterator(); it.hasNext();) {
            Class clazz = (Class)it.next();
            for (Iterator maxiter = max.iterator(); maxiter.hasNext();) {
                Class maxClazz = (Class)maxiter.next();
                if(isMoreSpecific(maxClazz, clazz)) {
                    // It can't be maximal, if there's already a more specific
                    // maximal than it.
                    continue outer;
                }
                if(isMoreSpecific(clazz, maxClazz)) {
                    // If it's more specific than a currently maximal element,
                    // that currently maximal is no longer a maximal.
                    maxiter.remove();
                }
            }
            // If we get here, no current maximal is more specific than the
            // current class, so it's considered maximal as well
            max.add(clazz);
        }
        if(max.size() > 1) {
            return OBJECT_CLASS;
        }
        return (Class)max.get(0);
    }

    /**
     * Determines whether a type represented by a class object is 
     * convertible to another type represented by a class object using a 
     * method invocation conversion, without matching object and primitive
     * types. This method is used to determine the more specific type when
     * comparing signatures of methods.
     * @return true if either formal type is assignable from actual type, 
     * or formal and actual are both primitive types and actual can be
     * subject to widening conversion to formal.
     */
    static boolean isMoreSpecific(Class specific, Class generic) {
        // Check for identity or widening reference conversion
        if(generic.isAssignableFrom(specific)) {
            return true;
        }
        // Check for widening primitive conversion.
        if(generic.isPrimitive()) {
            if(generic == Short.TYPE && (specific == Byte.TYPE)) {
                return true;
            }
            if(generic == Integer.TYPE && 
               (specific == Short.TYPE || specific == Byte.TYPE)) {
                return true;
            }
            if(generic == Long.TYPE && 
               (specific == Integer.TYPE || specific == Short.TYPE || 
                specific == Byte.TYPE)) {
                return true;
            }
            if(generic == Float.TYPE && 
               (specific == Long.TYPE || specific == Integer.TYPE || 
                specific == Short.TYPE || specific == Byte.TYPE)) {
                return true;
            }
            if(generic == Double.TYPE && 
               (specific == Float.TYPE || specific == Long.TYPE || 
                specific == Integer.TYPE || specific == Short.TYPE || 
                specific == Byte.TYPE)) {
                return true; 
            }
        }
        return false;
    }
    
    private static Set getAssignables(Class c1, Class c2) {
        Set s = new HashSet();
        collectAssignables(c1, c2, s);
        return s;
    }
    
    private static void collectAssignables(Class c1, Class c2, Set s) {
        if(c1.isAssignableFrom(c2)) {
            s.add(c1);
        }
        Class sc = c1.getSuperclass();
        if(sc != null) {
            collectAssignables(sc, c2, s);
        }
        Class[] itf = c1.getInterfaces();
        for(int i = 0; i < itf.length; ++i) {
            collectAssignables(itf[i], c2, s);
        }
    }

    static Class[] getParameterTypes(Member member) {
        if(member instanceof Method) {
            return ((Method)member).getParameterTypes();
        }
        if(member instanceof Constructor) {
            return ((Constructor)member).getParameterTypes();
        }
        throw new RuntimeException();
    }

    static boolean isVarArgs(Member member) {
        if(member instanceof Method) { 
            return isVarArgs(member, METHOD_IS_VARARGS);
        }
        if(member instanceof Constructor) {
            return isVarArgs(member, CONSTRUCTOR_IS_VARARGS);
        }
        throw new RuntimeException();
    }
    
    private static boolean isVarArgs(Member member, Method isVarArgsMethod) {
        if(isVarArgsMethod == null) {
            return false;
        }
        try {
            return ((Boolean)isVarArgsMethod.invoke(member, (Object[]) null)).booleanValue();
        }
        catch(RuntimeException e) {
            throw e;
        }
        catch(Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static Method getIsVarArgsMethod(Class memberClass) {
        try {
            return memberClass.getMethod("isVarArgs", (Class[]) null);
        }
        catch(NoSuchMethodException e) {
            return null; // pre 1.5 JRE
        }
    }
}
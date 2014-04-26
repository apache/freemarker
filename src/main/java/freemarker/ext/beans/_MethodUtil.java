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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import freemarker.core.BugException;
import freemarker.core._DelayedConversionToString;
import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public class _MethodUtil {
    
    // Get rid of these on Java 5
    private static final Method METHOD_IS_VARARGS = getIsVarArgsMethod(Method.class);
    private static final Method CONSTRUCTOR_IS_VARARGS = getIsVarArgsMethod(Constructor.class);
    
    /**
     * Determines whether the type given as the 1st argument is convertible to the type given as the 2nd argument
     * for method call argument conversion. This follows the rules of the Java reflection-based method call, except
     * that since we don't have the value here, a boxed class is never seen as convertible to a primitive type. 
     * 
     * @return 0 means {@code false}, non-0 means {@code true}.
     *         That is, 0 is returned less specificity or incomparable specificity, also when if
     *         then method was aborted because of {@code ifHigherThan}.
     *         The absolute value of the returned non-0 number symbolizes how more specific it is:
     *         <ul>
     *           <li>1: The two classes are identical</li>
     *           <li>2: The 1st type is primitive, the 2nd type is the corresponding boxing class</li>
     *           <li>3: Both classes are numerical, and one is convertible into the other with widening conversion.
     *                  E.g., {@code int} is convertible to {@code long} and {#code double}, hence {@code int} is more
     *                  specific.
     *                  This ignores primitive VS boxed mismatches, except that a boxed class is never seen as
     *                  convertible to a primitive class.</li>
     *           <li>4: One class is {@code instanceof} of the other, but they aren't identical.
     *               But unlike in Java, primitive numerical types are {@code instanceof} {@link Number} here.</li>
     *         </ul> 
     */
    public static int isMoreOrSameSpecificParameterType(final Class specific, final Class generic, boolean bugfixed,
            int ifHigherThan) {
        if (ifHigherThan >= 4) return 0;
        if(generic.isAssignableFrom(specific)) {
            // Identity or widening reference conversion:
            return generic == specific ? 1 : 4;
        } else {
            final boolean specificIsPrim = specific.isPrimitive(); 
            final boolean genericIsPrim = generic.isPrimitive();
            if (specificIsPrim) {
                if (genericIsPrim) {
                    if (ifHigherThan >= 3) return 0;
                    return isWideningPrimitiveNumberConversion(specific, generic) ? 3 : 0;
                } else {  // => specificIsPrim && !genericIsPrim
                    if (bugfixed) {
                        final Class specificAsBoxed = ClassUtil.primitiveClassToBoxingClass(specific); 
                        if (specificAsBoxed == generic) {
                            // A primitive class is more specific than its boxing class, because it can't store null
                            return 2;
                        } else if (generic.isAssignableFrom(specificAsBoxed)) {
                            // Note: This only occurs if `specific` is a primitive numerical, and `generic == Number`
                            return 4;
                        } else if (ifHigherThan >= 3) {
                            return 0;
                        } else if (Number.class.isAssignableFrom(specificAsBoxed)
                                && Number.class.isAssignableFrom(generic)) {
                            return isWideningBoxedNumberConversion(specificAsBoxed, generic) ? 3 : 0;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                }
            } else {  // => !specificIsPrim
                if (ifHigherThan >= 3) return 0;
                if (bugfixed && !genericIsPrim
                        && Number.class.isAssignableFrom(specific) && Number.class.isAssignableFrom(generic)) {
                    return isWideningBoxedNumberConversion(specific, generic) ? 3 : 0;
                } else {
                    return 0;
                }
            }
        }  // of: !generic.isAssignableFrom(specific) 
    }

    private static boolean isWideningPrimitiveNumberConversion(final Class source, final Class target) {
        if(target == Short.TYPE && (source == Byte.TYPE)) {
            return true;
        } else if (target == Integer.TYPE && 
           (source == Short.TYPE || source == Byte.TYPE)) {
            return true;
        } else if (target == Long.TYPE && 
           (source == Integer.TYPE || source == Short.TYPE || 
            source == Byte.TYPE)) {
            return true;
        } else if (target == Float.TYPE && 
           (source == Long.TYPE || source == Integer.TYPE || 
            source == Short.TYPE || source == Byte.TYPE)) {
            return true;
        } else if (target == Double.TYPE && 
           (source == Float.TYPE || source == Long.TYPE || 
            source == Integer.TYPE || source == Short.TYPE || 
            source == Byte.TYPE)) {
            return true; 
        } else {
            return false;
        }
    }

    private static boolean isWideningBoxedNumberConversion(final Class source, final Class target) {
        if(target == Short.class && source == Byte.class) {
            return true;
        } else if (target == Integer.class && 
           (source == Short.class || source == Byte.class)) {
            return true;
        } else if (target == Long.class && 
           (source == Integer.class || source == Short.class || 
            source == Byte.class)) {
            return true;
        } else if (target == Float.class && 
           (source == Long.class || source == Integer.class || 
            source == Short.class || source == Byte.class)) {
            return true;
        } else if (target == Double.class && 
           (source == Float.class || source == Long.class || 
            source == Integer.class || source == Short.class || 
            source == Byte.class)) {
            return true; 
        } else {
            return false;
        }
    }

    /**
     * Attention, this doesn't handle primitive classes correctly, nor numerical conversions.
     */
    public static Set getAssignables(Class c1, Class c2) {
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

    public static Class[] getParameterTypes(Member member) {
        if(member instanceof Method) {
            return ((Method)member).getParameterTypes();
        }
        if(member instanceof Constructor) {
            return ((Constructor)member).getParameterTypes();
        }
        throw new IllegalArgumentException("\"member\" must be Method or Constructor");
    }

    public static boolean isVarargs(Member member) {
        if(member instanceof Method) { 
            return isVarargs(member, METHOD_IS_VARARGS);
        }
        if(member instanceof Constructor) {
            return isVarargs(member, CONSTRUCTOR_IS_VARARGS);
        }
        throw new BugException();
    }

    private static boolean isVarargs(Member member, Method isVarArgsMethod) {
        if(isVarArgsMethod == null) {
            return false;
        }
        try {
            return ((Boolean)isVarArgsMethod.invoke(member, (Object[]) null)).booleanValue();
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

    /**
     * Returns a more streamlined method or constructor description than {@code Member.toString()} does.
     */
    public static String toString(Member member) {
        if (!(member instanceof Method || member instanceof Constructor)) {
            throw new IllegalArgumentException("\"member\" must be a Method or Constructor");
        }
        
        StringBuffer sb = new StringBuffer();
        
        if ((member.getModifiers() & Modifier.STATIC) != 0) {
            sb.append("static ");
        }
        
        String className = ClassUtil.getShortClassName(member.getDeclaringClass());
        if (className != null) {
            sb.append(className);
            sb.append('.');
        }
        sb.append(member.getName());

        sb.append('(');
        Class[] paramTypes = _MethodUtil.getParameterTypes(member);
        for (int i = 0; i < paramTypes.length; i++) {
            if (i != 0) sb.append(", ");
            String paramTypeDecl = ClassUtil.getShortClassName(paramTypes[i]);
            if (i == paramTypes.length - 1 && paramTypeDecl.endsWith("[]") && _MethodUtil.isVarargs(member)) {
                sb.append(paramTypeDecl.substring(0, paramTypeDecl.length() - 2));
                sb.append("...");
            } else {
                sb.append(paramTypeDecl);
            }
        }
        sb.append(')');
        
        return sb.toString();
    }

    public static Object[] invocationErrorMessageStart(Member member) {
        return invocationErrorMessageStart(member, member instanceof Constructor);
    }
    
    private static Object[] invocationErrorMessageStart(Object member, boolean isConstructor) {
        return new Object[] { "Java ", isConstructor ? "constructor " : "method ", new _DelayedJQuote(member) };
    }

    public static TemplateModelException newInvocationTemplateModelException(Object object, Member member, Throwable e) {
        return newInvocationTemplateModelException(
                object,
                member,
                (member.getModifiers() & Modifier.STATIC) != 0,
                member instanceof Constructor,
                e);
    }

    public static TemplateModelException newInvocationTemplateModelException(Object object, CallableMemberDescriptor callableMemberDescriptor, Throwable e) {
        return newInvocationTemplateModelException(
                object,
                new _DelayedConversionToString(callableMemberDescriptor) {
                    protected String doConversion(Object callableMemberDescriptor) {
                        return ((CallableMemberDescriptor) callableMemberDescriptor).getDeclaration();
                    }
                },
                callableMemberDescriptor.isStatic(),
                callableMemberDescriptor.isConstructor(),
                e);
    }
    
    private static TemplateModelException newInvocationTemplateModelException(
            Object parentObject, Object member, boolean isStatic, boolean isConstructor, Throwable e) {
        while (e instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) e).getTargetException();
            if (cause != null) {
                e = cause;
            } else {
                break;
            }
        }

        return new _TemplateModelException(e, new Object[] {
                invocationErrorMessageStart(member, isConstructor),
                " threw an exception",
                isStatic || isConstructor ? (Object) "" : new Object[] {
                    " when invoked on ", parentObject.getClass(), " object ", new _DelayedJQuote(parentObject) 
                },
                "; see cause exception in the Java stack trace."
        });
    }
    
}
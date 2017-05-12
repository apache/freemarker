/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.freemarker.core.model.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.freemarker.core._DelayedConversionToString;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.util.BugException;
import org.apache.freemarker.core.util._ClassUtil;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
public final class _MethodUtil {
    
    private _MethodUtil() {
        // Not meant to be instantiated
    }

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
    // TODO Seems that we don't use the full functionality of this anymore, so we could simplify this. See usages.
    public static int isMoreOrSameSpecificParameterType(final Class specific, final Class generic, boolean bugfixed,
            int ifHigherThan) {
        if (ifHigherThan >= 4) return 0;
        if (generic.isAssignableFrom(specific)) {
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
                        final Class specificAsBoxed = _ClassUtil.primitiveClassToBoxingClass(specific);
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
        if (target == Short.TYPE && (source == Byte.TYPE)) {
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
        if (target == Short.class && source == Byte.class) {
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
        if (c1.isAssignableFrom(c2)) {
            s.add(c1);
        }
        Class sc = c1.getSuperclass();
        if (sc != null) {
            collectAssignables(sc, c2, s);
        }
        Class[] itf = c1.getInterfaces();
        for (Class anItf : itf) {
            collectAssignables(anItf, c2, s);
        }
    }

    public static Class[] getParameterTypes(Member member) {
        if (member instanceof Method) {
            return ((Method) member).getParameterTypes();
        }
        if (member instanceof Constructor) {
            return ((Constructor) member).getParameterTypes();
        }
        throw new IllegalArgumentException("\"member\" must be Method or Constructor");
    }

    public static boolean isVarargs(Member member) {
        if (member instanceof Method) { 
            return ((Method) member).isVarArgs();
        }
        if (member instanceof Constructor) {
            return ((Constructor) member).isVarArgs();
        }
        throw new BugException();
    }

    /**
     * Returns a more streamlined method or constructor description than {@code Member.toString()} does.
     */
    public static String toString(Member member) {
        if (!(member instanceof Method || member instanceof Constructor)) {
            throw new IllegalArgumentException("\"member\" must be a Method or Constructor");
        }
        
        StringBuilder sb = new StringBuilder();
        
        if ((member.getModifiers() & Modifier.STATIC) != 0) {
            sb.append("static ");
        }
        
        String className = _ClassUtil.getShortClassName(member.getDeclaringClass());
        if (className != null) {
            sb.append(className);
            sb.append('.');
        }
        sb.append(member.getName());

        sb.append('(');
        Class[] paramTypes = _MethodUtil.getParameterTypes(member);
        for (int i = 0; i < paramTypes.length; i++) {
            if (i != 0) sb.append(", ");
            String paramTypeDecl = _ClassUtil.getShortClassName(paramTypes[i]);
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
                    @Override
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

        return new _TemplateModelException(e,
                invocationErrorMessageStart(member, isConstructor),
                " threw an exception",
                isStatic || isConstructor ? "" : new Object[] {
                    " when invoked on ", parentObject.getClass(), " object ", new _DelayedJQuote(parentObject) 
                },
                "; see cause exception in the Java stack trace.");
    }

    /**
     * Extracts the JavaBeans property from a reader method name, or returns {@code null} if the method name doesn't
     * look like a reader method name.
     */
    public static String getBeanPropertyNameFromReaderMethodName(String name, Class<?> returnType) {
        int start;
        if (name.startsWith("get")) {
            start = 3;
        } else if (returnType == boolean.class && name.startsWith("is")) {
            start = 2;
        } else {
            return null;
        }
        int ln = name.length();

        if (start == ln) {
            return null;
        }
        char c1 = name.charAt(start);

        return start + 1 < ln && Character.isUpperCase(name.charAt(start + 1)) && Character.isUpperCase(c1)
                ? name.substring(start) // getFOOBar => "FOOBar" (not lower case) according the JavaBeans spec.
                : new StringBuilder(ln - start).append(Character.toLowerCase(c1)).append(name, start + 1, ln)
                .toString();
    }

}
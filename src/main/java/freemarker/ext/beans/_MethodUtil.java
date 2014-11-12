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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.core.BugException;
import freemarker.core._DelayedConversionToString;
import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;
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

    private static final Pattern FUNCTION_SIGNATURE_PATTERN = 
            Pattern.compile("^([\\w\\.]+(\\s*\\[\\s*\\])?)\\s+([\\w]+)\\s*\\((.*)\\)$");
    private static final Pattern FUNCTION_PARAMETER_PATTERN = 
            Pattern.compile("^([\\w\\.]+)(\\s*\\[\\s*\\])?$");

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

    /**
     * Finds method by function signature string which is compliant with
     * Tag Library function signature in Java Server Page (TM) Specification.
     * <P>
     * A function signature example is as follows:
     * </P>
     * <PRE>
     *       java.lang.String nickName( java.lang.String, int )
     * </PRE>
     * 
     * @param clazz Class having the method.
     * @param signature Java Server Page (TM) Specification compliant function signature string.
     * @return method if found.
     * @throws UndeclaredThrowableException
     */
    public static Method getMethodByFunctionSignature(Class clazz, String signature) {
        Matcher m1 = FUNCTION_SIGNATURE_PATTERN.matcher(signature);

        if (!m1.matches()) {
            throw new IllegalArgumentException("Invalid function signature.");
        }

        try {
            String methodName = m1.group(3);
            String params = m1.group(4).trim();
            Class [] paramTypes = null;

            if ("".equals(params)) {
                paramTypes = new Class[0];
            }
            else {
                String [] paramsArray = StringUtil.split(params, ',');
                paramTypes = new Class[paramsArray.length];
                String token = null;
                String paramType = null;
                boolean isPrimitive = false;
                boolean isArrayType = false;
                Matcher m2 = null;

                for (int i = 0; i < paramsArray.length; i++) {
                    token = paramsArray[i].trim();
                    m2 = FUNCTION_PARAMETER_PATTERN.matcher(token);

                    if (!m2.matches()) {
                        throw new IllegalArgumentException("Invalid argument signature: '" + token + "'.");
                    }

                    paramType = m2.group(1);
                    isPrimitive = (paramType.indexOf('.') == -1);
                    isArrayType = (m2.group(2) != null);

                    if (isPrimitive) {
                        if ("byte".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? byte[].class : byte.class);
                        }
                        else if ("short".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? short[].class : short.class);
                        }
                        else if ("int".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? int[].class : int.class);
                        }
                        else if ("long".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? long[].class : long.class);
                        }
                        else if ("float".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? float[].class : float.class);
                        }
                        else if ("double".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? double[].class : double.class);
                        }
                        else if ("boolean".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? boolean[].class : boolean.class);
                        }
                        else if ("char".equals(paramType)) {
                            paramTypes[i] = (isArrayType ? char[].class : char.class);
                        }
                        else {
                            throw new IllegalArgumentException("Invalid primitive type: '" + paramType + "'.");
                        }
                    }
                    else {
                        if (isArrayType) {
                            paramTypes[i] = ClassUtil.forName("[L" + paramType + ";");
                        }
                        else {
                            paramTypes[i] = ClassUtil.forName(paramType);
                        }
                    }
                }
            }

            return clazz.getMethod(methodName, paramTypes);
        }
        catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
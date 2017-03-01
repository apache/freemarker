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

package org.apache.freemarker.core.util;

import org.apache.freemarker.core.model.impl.BeanModel;

public class _ClassUtil {
    
    private static final String ORG_APACHE_FREEMARKER = "org.apache.freemarker.";
    private static final String ORG_APACHE_FREEMARKER_CORE = "org.apache.freemarker.core.";
    private static final String ORG_APACHE_FREEMARKER_CORE_TEMPLATERESOLVER
            = "org.apache.freemarker.core.templateresolver.";
    private static final String ORG_APACHE_FREEMARKER_CORE_MODEL = "org.apache.freemarker.core.model.";

    private _ClassUtil() {
    }
    
    /**
     * Similar to {@link Class#forName(java.lang.String)}, but attempts to load
     * through the thread context class loader. Only if thread context class
     * loader is inaccessible, or it can't find the class will it attempt to
     * fall back to the class loader that loads the FreeMarker classes.
     */
    public static Class forName(String className)
    throws ClassNotFoundException {
        try {
            ClassLoader ctcl = Thread.currentThread().getContextClassLoader();
            if (ctcl != null) {  // not null: we don't want to fall back to the bootstrap class loader
                return Class.forName(className, true, ctcl);
            }
        } catch (ClassNotFoundException e) {
            // Intentionally ignored
        } catch (SecurityException e) {
            // Intentionally ignored
        }
        // Fall back to the defining class loader of the FreeMarker classes 
        return Class.forName(className);
    }
    
    /**
     * Same as {@link #getShortClassName(Class, boolean) getShortClassName(pClass, false)}.
     * 
     * @since 2.3.20
     */
    public static String getShortClassName(Class pClass) {
        return getShortClassName(pClass, false);
    }
    
    /**
     * Returns a class name without "java.lang." and "java.util." prefix, also shows array types in a format like
     * {@code int[]}; useful for printing class names in error messages.
     * 
     * @param pClass can be {@code null}, in which case the method returns {@code null}.
     * @param shortenFreeMarkerClasses if {@code true}, it will also shorten FreeMarker class names. The exact rules
     *     aren't specified and might change over time, but right now, {@link BeanModel} for
     *     example becomes to {@code o.a.f.c.m.BeanModel}.
     * 
     * @since 2.3.20
     */
    public static String getShortClassName(Class pClass, boolean shortenFreeMarkerClasses) {
        if (pClass == null) {
            return null;
        } else if (pClass.isArray()) {
            return getShortClassName(pClass.getComponentType()) + "[]";
        } else {
            String cn = pClass.getName();
            if (cn.startsWith("java.lang.") || cn.startsWith("java.util.")) {
                return cn.substring(10);
            } else {
                if (shortenFreeMarkerClasses) {
                    if (cn.startsWith(ORG_APACHE_FREEMARKER_CORE_MODEL)) {
                        return "o.a.f.c.m." + cn.substring(ORG_APACHE_FREEMARKER_CORE_MODEL.length());
                    } else if (cn.startsWith(ORG_APACHE_FREEMARKER_CORE_TEMPLATERESOLVER)) {
                        return "o.a.f.c.t." + cn.substring(ORG_APACHE_FREEMARKER_CORE_TEMPLATERESOLVER.length());
                    } else if (cn.startsWith(ORG_APACHE_FREEMARKER_CORE)) {
                        return "o.a.f.c." + cn.substring(ORG_APACHE_FREEMARKER_CORE.length());
                    } else if (cn.startsWith(ORG_APACHE_FREEMARKER)) {
                        return "o.a.f." + cn.substring(ORG_APACHE_FREEMARKER.length());
                    }
                    // Falls through
                }
                return cn;
            }
        }
    }

    /**
     * Same as {@link #getShortClassNameOfObject(Object, boolean) getShortClassNameOfObject(pClass, false)}.
     * 
     * @since 2.3.20
     */
    public static String getShortClassNameOfObject(Object obj) {
        return getShortClassNameOfObject(obj, false);
    }
    
    /**
     * {@link #getShortClassName(Class, boolean)} called with {@code object.getClass()}, but returns the fictional
     * class name {@code Null} for a {@code null} value.
     * 
     * @since 2.3.20
     */
    public static String getShortClassNameOfObject(Object obj, boolean shortenFreeMarkerClasses) {
        if (obj == null) {
            return "Null";
        } else {
            return _ClassUtil.getShortClassName(obj.getClass(), shortenFreeMarkerClasses);
        }
    }

    /**
     * Gets the wrapper class for a primitive class, like {@link Integer} for {@code int}, also returns {@link Void}
     * for {@code void}. 
     * 
     * @param primitiveClass A {@link Class} like {@code int.type}, {@code boolean.type}, etc. If it's not a primitive
     *     class, or it's {@code null}, then the parameter value is returned as is. Note that performance-wise the
     *     method assumes that it's a primitive class.
     *     
     * @since 2.3.21
     */
    public static Class primitiveClassToBoxingClass(Class primitiveClass) {
        // Tried to sort these with decreasing frequency in API-s:
        if (primitiveClass == int.class) return Integer.class;
        if (primitiveClass == boolean.class) return Boolean.class;
        if (primitiveClass == long.class) return Long.class;
        if (primitiveClass == double.class) return Double.class;
        if (primitiveClass == char.class) return Character.class;
        if (primitiveClass == float.class) return Float.class;
        if (primitiveClass == byte.class) return Byte.class;
        if (primitiveClass == short.class) return Short.class;
        if (primitiveClass == void.class) return Void.class;  // not really a primitive, but we normalize it
        return primitiveClass;
    }

    /**
     * The exact reverse of {@link #primitiveClassToBoxingClass}.
     *     
     * @since 2.3.21
     */
    public static Class boxingClassToPrimitiveClass(Class boxingClass) {
        // Tried to sort these with decreasing frequency in API-s:
        if (boxingClass == Integer.class) return int.class;
        if (boxingClass == Boolean.class) return boolean.class;
        if (boxingClass == Long.class) return long.class;
        if (boxingClass == Double.class) return double.class;
        if (boxingClass == Character.class) return char.class;
        if (boxingClass == Float.class) return float.class;
        if (boxingClass == Byte.class) return byte.class;
        if (boxingClass == Short.class) return short.class;
        if (boxingClass == Void.class) return void.class;  // not really a primitive, but we normalize to it
        return boxingClass;
    }
    
    /**
     * Tells if a type is numerical; works both for primitive types and classes.
     * 
     * @param type can't be {@code null}
     * 
     * @since 2.3.21
     */
    public static boolean isNumerical(Class type) {
        return Number.class.isAssignableFrom(type)
                || type.isPrimitive() && type != Boolean.TYPE && type != Character.TYPE && type != Void.TYPE;
    }
    
}

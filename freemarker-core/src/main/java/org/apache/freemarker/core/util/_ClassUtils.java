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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class _ClassUtils {
    
    private static final String ORG_APACHE_FREEMARKER = "org.apache.freemarker.";
    private static final String ORG_APACHE_FREEMARKER_CORE = "org.apache.freemarker.core.";
    private static final String ORG_APACHE_FREEMARKER_CORE_TEMPLATERESOLVER
            = "org.apache.freemarker.core.templateresolver.";
    private static final String ORG_APACHE_FREEMARKER_CORE_MODEL = "org.apache.freemarker.core.model.";

    private _ClassUtils() {
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
        } catch (ClassNotFoundException | SecurityException e) {
            // Intentionally ignored
        }
        // Fall back to the defining class loader of the FreeMarker classes 
        return Class.forName(className);
    }

    private static final Map<String, Class<?>> PRIMITIVE_CLASSES_BY_NAME;
    static {
        PRIMITIVE_CLASSES_BY_NAME = new HashMap<>();
        PRIMITIVE_CLASSES_BY_NAME.put("boolean", boolean.class);
        PRIMITIVE_CLASSES_BY_NAME.put("byte", byte.class);
        PRIMITIVE_CLASSES_BY_NAME.put("char", char.class);
        PRIMITIVE_CLASSES_BY_NAME.put("short", short.class);
        PRIMITIVE_CLASSES_BY_NAME.put("int", int.class);
        PRIMITIVE_CLASSES_BY_NAME.put("long", long.class);
        PRIMITIVE_CLASSES_BY_NAME.put("float", float.class);
        PRIMITIVE_CLASSES_BY_NAME.put("double", double.class);
    }

    /**
     * Returns the {@link Class} for a primitive type name, or {@code null} if it's not the name of a primitive type.
     */
    public static Class<?> resolveIfPrimitiveTypeName(String typeName) {
        return PRIMITIVE_CLASSES_BY_NAME.get(typeName);
    }

    /**
     * Returns the array type that corresponds to the element type and the given number of array dimensions.
     * If the dimension is 0, it just returns the element type as is.
     */
    public static Class<?> getArrayClass(Class<?> elementType, int dimensions) {
        return dimensions == 0 ? elementType : Array.newInstance(elementType, new int[dimensions]).getClass();
    }

    /**
     * Same as {@link #getShortClassName(Class, boolean) getShortClassName(pClass, false)}.
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
     */
    public static String getShortClassNameOfObject(Object obj) {
        return getShortClassNameOfObject(obj, false);
    }
    
    /**
     * {@link #getShortClassName(Class, boolean)} called with {@code object.getClass()}, but returns the fictional
     * class name {@code Null} for a {@code null} value.
     */
    public static String getShortClassNameOfObject(Object obj, boolean shortenFreeMarkerClasses) {
        if (obj == null) {
            return "Null";
        } else {
            return _ClassUtils.getShortClassName(obj.getClass(), shortenFreeMarkerClasses);
        }
    }

    /**
     * Gets the wrapper class for a primitive class, like {@link Integer} for {@code int}, also returns {@link Void}
     * for {@code void}. 
     * 
     * @param primitiveClass A {@link Class} like {@code int.type}, {@code boolean.type}, etc. If it's not a primitive
     *     class, or it's {@code null}, then the parameter value is returned as is. Note that performance-wise the
     *     method assumes that it's a primitive class.
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
     */
    public static boolean isNumerical(Class type) {
        return Number.class.isAssignableFrom(type)
                || type.isPrimitive() && type != Boolean.TYPE && type != Character.TYPE && type != Void.TYPE;
    }

    /**
     * Very similar to {@link Class#getResourceAsStream(String)}, but throws {@link IOException} instead of returning
     * {@code null} if {@code optional} is {@code false}, and attempts to work around "IllegalStateException: zip file
     * closed" and similar {@code sun.net.www.protocol.jar.JarURLConnection}-related glitches. These are caused by bugs
     * outside of FreeMarker. Note that in cases where the JAR resource becomes broken concurrently, similar errors can
     * still occur later when the {@link InputStream} is read ({@link #loadProperties(Class, String)} works that
     * around as well).
     * 
     * @return If {@code optional} is {@code false}, it's never {@code null}, otherwise {@code null} indicates that the
     *         resource doesn't exist.
     * @throws IOException
     *             If the resource wasn't found, or other {@link IOException} occurs.
     */
    public static InputStream getReasourceAsStream(Class<?> baseClass, String resource, boolean optional)
            throws IOException {
        InputStream ins;
        try {
            // This is how we did this earlier. May use some JarURLConnection caches, which leads to the problems.
            ins = baseClass.getResourceAsStream(resource);
        } catch (Exception e) {
            // Workaround for "IllegalStateException: zip file closed", and other related exceptions. This happens due
            // to bugs outside of FreeMarker, but we try to work it around anyway.
            URL url = baseClass.getResource(resource);
            ins = url != null ? url.openStream() : null;
        }
        if (!optional) {
            checkInputStreamNotNull(ins, baseClass, resource);
        }
        return ins;
    }

    /**
     * Same as {@link #getReasourceAsStream(Class, String, boolean)}, but uses a {@link ClassLoader} directly
     * instead of a {@link Class}.
     */
    public static InputStream getReasourceAsStream(ClassLoader classLoader, String resource, boolean optional)
            throws IOException {
        // See source commends in the other overload of this method.
        InputStream ins;
        try {
            ins = classLoader.getResourceAsStream(resource);
        } catch (Exception e) {
            URL url = classLoader.getResource(resource);
            ins = url != null ? url.openStream() : null;
        }
        if (ins == null && !optional) {
            throw new IOException("Class-loader resource not found (shown quoted): "
                    + _StringUtils.jQuote(resource) + ". The base ClassLoader was: " + classLoader);
        }
        return ins;
    }

    /**
     * Loads a class loader resource into a {@link Properties}; tries to work around "zip file closed" and related
     * {@code sun.net.www.protocol.jar.JarURLConnection} glitches.
     */
    public static Properties loadProperties(Class<?> baseClass, String resource) throws IOException {
        Properties props = new Properties();
        
        InputStream ins  = null;
        try {
            try {
                // This is how we did this earlier. May use some JarURLConnection caches, which leads to the problems.
                ins = baseClass.getResourceAsStream(resource);
            } catch (Exception e) {
                throw new MaybeZipFileClosedException();
            }
            checkInputStreamNotNull(ins, baseClass, resource);
            try {
                props.load(ins);
            } catch (Exception e) {
                throw new MaybeZipFileClosedException();                
            } finally {
                try {
                    ins.close();
                } catch (Exception e) {
                    // Do nothing to suppress "ZipFile closed" and related exceptions.
                }
                ins = null;
            }
        } catch (MaybeZipFileClosedException e) {
            // Workaround for "zip file closed" exception, and other related exceptions. This happens due to bugs
            // outside of FreeMarker, but we try to work it around anyway.
            URL url = baseClass.getResource(resource);
            ins = url != null ? url.openStream() : null;
            checkInputStreamNotNull(ins, baseClass, resource);
            props.load(ins);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                    // Do nothing to suppress "ZipFile closed" and related exceptions.
                }
            }
        }
        return props;
    }

    private static void checkInputStreamNotNull(InputStream ins, Class<?> baseClass, String resource)
            throws IOException {
        if (ins == null) {
            throw new IOException("Class-loader resource not found (shown quoted): "
                    + _StringUtils.jQuote(resource) + ". The base class was " + baseClass.getName() + ".");
        }
    }
    
    /** Used internally to work around some JarURLConnection glitches */
    private static class MaybeZipFileClosedException extends Exception {
        //
    }
    
}

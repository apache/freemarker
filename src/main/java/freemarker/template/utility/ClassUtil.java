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

package freemarker.template.utility;

import java.util.HashSet;
import java.util.Set;

import freemarker.core.Environment;
import freemarker.core.Macro;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BooleanModel;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.DateModel;
import freemarker.ext.beans.EnumerationModel;
import freemarker.ext.beans.IteratorModel;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.OverloadedMethodsModel;
import freemarker.ext.beans.SimpleMethodModel;
import freemarker.ext.beans.StringModel;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateCollectionModelEx;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateTransformModel;

/**
 */
public class ClassUtil
{
    private ClassUtil()
    {
    }
    
    /**
     * Similar to {@link Class#forName(java.lang.String)}, but attempts to load
     * through the thread context class loader. Only if thread context class
     * loader is inaccessible, or it can't find the class will it attempt to
     * fall back to the class loader that loads the FreeMarker classes.
     */
    public static Class forName(String className)
    throws
        ClassNotFoundException
    {
        try
        {
            ClassLoader ctcl = Thread.currentThread().getContextClassLoader();
            if (ctcl != null) {  // not null: we don't want to fall back to the bootstrap class loader
                return Class.forName(className, true, ctcl);
            }
        }
        catch(ClassNotFoundException e)
        {
            ;// Intentionally ignored
        }
        catch(SecurityException e)
        {
            ;// Intentionally ignored
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
     *     aren't specified and might change over time, but right now, {@code freemarker.ext.beans.NumberModel} for
     *     example becomes to {@code f.e.b.NumberModel}. 
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
                    if (cn.startsWith("freemarker.template.")) {
                        return "f.t" + cn.substring(19);
                    } else if (cn.startsWith("freemarker.ext.beans.")) {
                        return "f.e.b" + cn.substring(20);
                    } else if (cn.startsWith("freemarker.core.")) {
                        return "f.c" + cn.substring(15);
                    } else if (cn.startsWith("freemarker.ext.")) {
                        return "f.e" + cn.substring(14);
                    } else if (cn.startsWith("freemarker.")) {
                        return "f" + cn.substring(10);
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
            return ClassUtil.getShortClassName(obj.getClass(), shortenFreeMarkerClasses);
        }
    }

    /**
     * Returns the {@link TemplateModel} interface that is the most characteristic of the object, or {@code null}.
     */
    private static Class getPrimaryTemplateModelInterface(TemplateModel tm) {
        if (tm instanceof BeanModel) {
            if (tm instanceof CollectionModel) {
                return TemplateSequenceModel.class;
            } else if (tm instanceof IteratorModel || tm instanceof EnumerationModel) {
                return TemplateCollectionModel.class;
            } else if (tm instanceof MapModel) {
                return TemplateHashModelEx.class;
            } else if (tm instanceof NumberModel) {
                return TemplateNumberModel.class;
            } else if (tm instanceof BooleanModel) {
                return TemplateBooleanModel.class;
            } else if (tm instanceof DateModel) {
                return TemplateDateModel.class;
            } else if (tm instanceof StringModel) {
                Object wrapped = ((BeanModel) tm).getWrappedObject();
                return wrapped instanceof String
                        ? TemplateScalarModel.class
                        : (tm instanceof TemplateHashModelEx ? TemplateHashModelEx.class : null);
            } else {
                return null;
            }
        } else if (tm instanceof SimpleMethodModel || tm instanceof OverloadedMethodsModel) {
            return TemplateMethodModelEx.class;
        } else {
            return null;
        }
    }

    private static void appendTemplateModelTypeName(StringBuffer sb, Set typeNamesAppended, Class cl) {
        if (TemplateNodeModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "node");
        }
        
        if (TemplateDirectiveModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "directive");
        } else if (TemplateTransformModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "transform");
        }
        
        if (TemplateSequenceModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "sequence");
        } else if (TemplateCollectionModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended,
                    TemplateCollectionModelEx.class.isAssignableFrom(cl) ? "extended_collection" : "collection");
        } else if (TemplateModelIterator.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "iterator");
        }
        
        if (TemplateMethodModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "method");
        }
        
        if (Environment.Namespace.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "namespace");
        } else if (TemplateHashModelEx.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "extended_hash");
        } else if (TemplateHashModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "hash");
        }
        
        if (TemplateNumberModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "number");
        }
        
        if (TemplateDateModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "date_or_time_or_datetime");
        }
        
        if (TemplateBooleanModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "boolean");
        }
        
        if (TemplateScalarModel.class.isAssignableFrom(cl)) {
            appendTypeName(sb, typeNamesAppended, "string");
        }
    }
    
    private static Class getUnwrappedClass(TemplateModel tm) {
        Object unwrapped;
        try {
            if (tm instanceof WrapperTemplateModel) {
                unwrapped = ((WrapperTemplateModel) tm).getWrappedObject();
            } else if (tm instanceof AdapterTemplateModel) {
                unwrapped = ((AdapterTemplateModel) tm).getAdaptedObject(Object.class);
            } else {
                unwrapped = null;
            }
        } catch (Throwable e) {
            unwrapped = null;
        }
        return unwrapped != null ? unwrapped.getClass() : null;
    }

    private static void appendTypeName(StringBuffer sb, Set typeNamesAppended, String name) {
        if (!typeNamesAppended.contains(name)) {
            if (sb.length() != 0) sb.append("+");
            sb.append(name);
            typeNamesAppended.add(name);
        }
    }

    /**
     * Returns the type description of a value with FTL terms (not plain class name), as it should be used in
     * type-related error messages and for debugging purposes. The exact format is not specified and might change over
     * time, but currently it's something like {@code "string (wrapper: f.t.SimpleScalar)"} or
     * {@code "sequence+hash+string (ArrayList wrapped into f.e.b.CollectionModel)"}.
     * 
     * @since 2.3.20
     */
    public static String getFTLTypeDescription(TemplateModel tm) {
        if (tm == null) {
            return "Null";
        } else {
            Set typeNamesAppended = new HashSet();
            
            StringBuffer sb = new StringBuffer();
    
            Class primaryInterface = getPrimaryTemplateModelInterface(tm);
            if (primaryInterface != null) {
                appendTemplateModelTypeName(sb, typeNamesAppended, primaryInterface);
            }
    
            if (tm instanceof Macro) {
                appendTypeName(sb, typeNamesAppended, ((Macro) tm).isFunction() ? "function" : "macro");
            }
            
            appendTemplateModelTypeName(sb, typeNamesAppended, tm.getClass());
            
            String javaClassName;
            Class unwrappedClass = getUnwrappedClass(tm);
            if (unwrappedClass != null) {
                javaClassName = getShortClassName(unwrappedClass, true);
            } else {
                javaClassName = null;
            }
            
            sb.append(" (");
            String modelClassName = getShortClassName(tm.getClass(), true);
            if (javaClassName == null) {
                sb.append("wrapper: ");
                sb.append(modelClassName);
            } else {
                sb.append(javaClassName);
                sb.append(" wrapped into ");
                sb.append(modelClassName);
            }
            sb.append(")");
    
            return sb.toString();
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

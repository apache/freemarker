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
 * @author Attila Szegedi
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
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        catch(ClassNotFoundException e)
        {
            ;// Intentionally ignored
        }
        catch(SecurityException e)
        {
            ;// Intentionally ignored
        }
        // Fall back to default class loader 
        return Class.forName(className);
    }
    
    /**
     * Same as {@link #getShortClassName(Class, boolean) getShortClassName(pClass, false)}.
     * 
     * @since 2.4
     */
    public static String getShortClassName(Class pClass) {
        return getShortClassName(pClass, false);
    }
    
    /**
     * Returns a class name without "java.lang." and "java.util." prefix; useful for printing class names in error
     * messages.
     * 
     * @param pClass can be {@code null}, in which case the method returns {@code null}.
     * @param shortenFreeMarkerClasses if {@code true}, it will also shorten FreeMarker class names. The exact rules
     *     aren't specified and might change over time, but right now, {@code freemarker.ext.beans.NumberModel} for
     *     example becomes to {@code f.e.b.NumberModel}. 
     * 
     * @since 2.4
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
     * @since 2.4
     */
    public static String getShortClassNameOfObject(Object obj) {
        return getShortClassNameOfObject(obj, false);
    }
    
    /**
     * {@link #getShortClassName(Class, boolean)} called with {@code object.getClass()}, but returns the fictional
     * class name {@code Null} for a {@code null} value.
     * 
     * @since 2.4
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
            } else if (tm instanceof SimpleMethodModel || tm instanceof OverloadedMethodsModel) {
                return TemplateMethodModelEx.class;
            } else if (tm instanceof StringModel) {
                Object wrapped = ((BeanModel) tm).getWrappedObject();
                return wrapped instanceof String
                        ? TemplateScalarModel.class
                        : (tm instanceof TemplateHashModelEx ? TemplateHashModelEx.class : null);
            } else {
                return null;
            }
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
            appendTypeName(sb, typeNamesAppended, "collection");
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
            appendTypeName(sb, typeNamesAppended, "date");
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
     * @since 2.4
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
    
}

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import freemarker.log.Logger;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Wraps the static fields and methods of a class in a
 * {@link freemarker.template.TemplateHashModel}.
 * Fields are wrapped using {@link BeansWrapper#wrap(Object)}, and
 * methods are wrapped into an appropriate {@link freemarker.template.TemplateMethodModelEx} instance.
 * Unfortunately, there is currently no support for bean property-style
 * calls of static methods, similar to that in {@link BeanModel}.
 * @author Attila Szegedi
 */
final class StaticModel implements TemplateHashModelEx
{
    private static final Logger logger = Logger.getLogger("freemarker.beans");
    private final Class clazz;
    private final BeansWrapper wrapper;
    private final Map map = new HashMap();

    StaticModel(Class clazz, BeansWrapper wrapper) throws TemplateModelException
    {
        this.clazz = clazz;
        this.wrapper = wrapper;
        populate();
    }

    /**
     * Returns the field or method named by the <tt>key</tt>
     * parameter.
     */
    public TemplateModel get(String key) throws TemplateModelException
    {
        Object model = map.get(key);
        // Simple method, overloaded method or final field -- these have cached 
        // template models
        if (model instanceof TemplateModel)
            return (TemplateModel) model;
        // Non-final field; this must be evaluated on each call.
        if (model instanceof Field)
        {
            try
            {
                return wrapper.getOuterIdentity().wrap(((Field) model).get(null));
            }
            catch (IllegalAccessException e)
            {
                throw new TemplateModelException(
                    "Illegal access for field " + key + " of class " + clazz.getName());
            }
        }

        throw new TemplateModelException(
            "No such key: " + key + " in class " + clazz.getName());
    }

    /**
     * Returns true if there is at least one public static
     * field or method in the underlying class.
     */
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public int size()
    {
        return map.size();
    }
    
    public TemplateCollectionModel keys() throws TemplateModelException
    {
        return (TemplateCollectionModel)wrapper.getOuterIdentity().wrap(map.keySet());
    }
    
    public TemplateCollectionModel values() throws TemplateModelException
    {
        return (TemplateCollectionModel)wrapper.getOuterIdentity().wrap(map.values());
    }

    private void populate() throws TemplateModelException
    {
        if (!Modifier.isPublic(clazz.getModifiers()))
        {
            throw new TemplateModelException(
                "Can't wrap the non-public class " + clazz.getName());
        }
        
        if(wrapper.getExposureLevel() == BeansWrapper.EXPOSE_NOTHING)
        {
            return;
        }

        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; ++i)
        {
            Field field = fields[i];
            int mod = field.getModifiers();
            if (Modifier.isPublic(mod) && Modifier.isStatic(mod))
            {
                if (Modifier.isFinal(mod))
                    try
                    {
                        // public static final fields are evaluated once and
                        // stored in the map
                        map.put(field.getName(), wrapper.getOuterIdentity().wrap(field.get(null)));
                    }
                    catch (IllegalAccessException e)
                    {
                        // Intentionally ignored
                    }
                else
                    // This is a special flagging value: Field in the map means
                    // that this is a non-final field, and it must be evaluated
                    // on each get() call.
                    map.put(field.getName(), field);
            }
        }
        if(wrapper.getExposureLevel() < BeansWrapper.EXPOSE_PROPERTIES_ONLY)
        {
            Method[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; ++i)
            {
                Method method = methods[i];
                int mod = method.getModifiers();
                if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && wrapper.isSafeMethod(method))
                {
                    String name = method.getName();
                    Object obj = map.get(name);
                    if (obj instanceof Method)
                    {
                        OverloadedMethods overloadedMethods = new OverloadedMethods(wrapper);
                        overloadedMethods.addMember((Method) obj);
                        overloadedMethods.addMember(method);
                        map.put(name, overloadedMethods);
                    }
                    else if(obj instanceof OverloadedMethods)
                    {
                        OverloadedMethods overloadedMethods = (OverloadedMethods) obj;
                        overloadedMethods.addMember(method);
                    }
                    else
                    {
                        if(obj != null)
                        {
                            if (logger.isInfoEnabled()) {
                                logger.info("Overwriting value [" + obj + "] for " +
                                        " key '" + name + "' with [" + method + 
                                        "] in static model for " + clazz.getName());
                            }
                        }
                        map.put(name, method);
                    }
                }
            }
            for (Iterator entries = map.entrySet().iterator(); entries.hasNext();)
            {
                Map.Entry entry = (Map.Entry) entries.next();
                Object value = entry.getValue();
                if (value instanceof Method)
                {
                    Method method = (Method)value;
                    entry.setValue(new SimpleMethodModel(null, method, 
                            method.getParameterTypes(), wrapper));
                }
                else if (value instanceof OverloadedMethods)
                {
                    entry.setValue(new OverloadedMethodsModel(null, 
                            (OverloadedMethods)value));
                }
            }
        }
    }
}

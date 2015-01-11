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
 */
final class StaticModel implements TemplateHashModelEx
{
    private static final Logger LOG = Logger.getLogger("freemarker.beans");
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
                if (Modifier.isPublic(mod) && Modifier.isStatic(mod)
                        && wrapper.getClassIntrospector().isAllowedToExpose(method))
                {
                    String name = method.getName();
                    Object obj = map.get(name);
                    if (obj instanceof Method)
                    {
                        OverloadedMethods overloadedMethods = new OverloadedMethods(wrapper.is2321Bugfixed());
                        overloadedMethods.addMethod((Method) obj);
                        overloadedMethods.addMethod(method);
                        map.put(name, overloadedMethods);
                    }
                    else if(obj instanceof OverloadedMethods)
                    {
                        OverloadedMethods overloadedMethods = (OverloadedMethods) obj;
                        overloadedMethods.addMethod(method);
                    }
                    else
                    {
                        if(obj != null)
                        {
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Overwriting value [" + obj + "] for " +
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
                    entry.setValue(new OverloadedMethodsModel(null, (OverloadedMethods) value, wrapper));
                }
            }
        }
    }
}

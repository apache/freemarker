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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps the static fields and methods of a class in a {@link org.apache.freemarker.core.model.TemplateHashModel}.
 * Fields are wrapped using {@link DefaultObjectWrapper#wrap(Object)}, and methods are wrapped into an appropriate
 * {@link TemplateFunctionModel} instance. There is currently no support for bean property-style calls of static
 * methods, similar to that in {@link BeanModel} (as it's not part for the JavaBeans specification).
 */
final class StaticModel implements TemplateHashModelEx {
    
    private static final Logger LOG = LoggerFactory.getLogger(StaticModel.class);

    private final Class clazz;
    private final DefaultObjectWrapper wrapper;
    private final Map map = new HashMap();

    StaticModel(Class clazz, DefaultObjectWrapper wrapper) throws TemplateException {
        this.clazz = clazz;
        this.wrapper = wrapper;
        populate();
    }

    /**
     * Returns the field or method named by the <tt>key</tt>
     * parameter.
     */
    @Override
    public TemplateModel get(String key) throws TemplateException {
        Object model = map.get(key);
        // Simple method, overloaded method or final field -- these have cached 
        // template models
        if (model instanceof TemplateModel)
            return (TemplateModel) model;
        // Non-final field; this must be evaluated on each call.
        if (model instanceof Field) {
            try {
                return wrapper.getOuterIdentity().wrap(((Field) model).get(null));
            } catch (IllegalAccessException e) {
                throw new TemplateException(
                    "Illegal access for field " + key + " of class " + clazz.getName());
            }
        }

        throw new TemplateException(
            "No such key: " + key + " in class " + clazz.getName());
    }

    /**
     * Returns true if there is at least one public static
     * field or method in the underlying class.
     */
    @Override
    public boolean isEmptyHash() {
        return map.isEmpty();
    }

    @Override
    public int getHashSize() {
        return map.size();
    }
    
    @Override
    public TemplateIterableModel keys() throws TemplateException {
        return (TemplateIterableModel) wrapper.getOuterIdentity().wrap(map.keySet());
    }
    
    @Override
    public TemplateIterableModel values() throws TemplateException {
        return (TemplateIterableModel) wrapper.getOuterIdentity().wrap(map.values());
    }

    private void populate() throws TemplateException {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            throw new TemplateException(
                "Can't wrap the non-public class " + clazz.getName());
        }
        
        if (wrapper.getExposureLevel() == DefaultObjectWrapper.EXPOSE_NOTHING) {
            return;
        }

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
                if (Modifier.isFinal(mod))
                    try {
                        // public static final fields are evaluated once and
                        // stored in the map
                        map.put(field.getName(), wrapper.getOuterIdentity().wrap(field.get(null)));
                    } catch (IllegalAccessException e) {
                        // Intentionally ignored
                    }
                else
                    // This is a special flagging value: Field in the map means
                    // that this is a non-final field, and it must be evaluated
                    // on each get() call.
                    map.put(field.getName(), field);
            }
        }
        if (wrapper.getExposureLevel() < DefaultObjectWrapper.EXPOSE_PROPERTIES_ONLY) {
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                int mod = method.getModifiers();
                if (Modifier.isPublic(mod) && Modifier.isStatic(mod)
                        && wrapper.getClassIntrospector().isAllowedToExpose(method)) {
                    String name = method.getName();
                    Object obj = map.get(name);
                    if (obj instanceof Method) {
                        OverloadedMethods overloadedMethods = new OverloadedMethods();
                        overloadedMethods.addMethod((Method) obj);
                        overloadedMethods.addMethod(method);
                        map.put(name, overloadedMethods);
                    } else if (obj instanceof OverloadedMethods) {
                        OverloadedMethods overloadedMethods = (OverloadedMethods) obj;
                        overloadedMethods.addMethod(method);
                    } else {
                        if (obj != null) {
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
            for (Iterator entries = map.entrySet().iterator(); entries.hasNext(); ) {
                Map.Entry entry = (Map.Entry) entries.next();
                Object value = entry.getValue();
                if (value instanceof Method) {
                    Method method = (Method) value;
                    entry.setValue(new SimpleJavaMethodModel(null, method,
                            method.getParameterTypes(), wrapper));
                } else if (value instanceof OverloadedMethods) {
                    entry.setValue(new OverloadedJavaMethodModel(null, (OverloadedMethods) value, wrapper));
                }
            }
        }
    }
}

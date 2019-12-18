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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core.util._ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy black list based member access policy, used only to keep old behavior, as it can't provide meaningful safety.
 * Do not use it if you allow untrusted users to edit templates!
 */
public final class DefaultMemberAccessPolicy implements MemberAccessPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMemberAccessPolicy.class);
    private static final String UNSAFE_METHODS_PROPERTIES = "unsafeMethods.properties";
    private static final Set<Method> UNSAFE_METHODS = createUnsafeMethodsSet();

    private static Set<Method> createUnsafeMethodsSet() {
        try {
            Properties props = _ClassUtils.loadProperties(DefaultObjectWrapper.class, UNSAFE_METHODS_PROPERTIES);
            Set<Method> set = new HashSet<>(props.size() * 4 / 3, 1f);
            Map<String, Class<?>> primClasses = createPrimitiveClassesMap();
            for (Object key : props.keySet()) {
                try {
                    set.add(parseMethodSpec((String) key, primClasses));
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    LOG.debug("Failed to get unsafe method", e);
                }
            }
            return set;
        } catch (Exception e) {
            throw new RuntimeException("Could not load unsafe method set", e);
        }
    }

    private static Method parseMethodSpec(String methodSpec, Map<String, Class<?>> primClasses)
            throws ClassNotFoundException,
            NoSuchMethodException {
        int brace = methodSpec.indexOf('(');
        int dot = methodSpec.lastIndexOf('.', brace);
        Class<?> clazz = _ClassUtils.forName(methodSpec.substring(0, dot));
        String methodName = methodSpec.substring(dot + 1, brace);
        String argSpec = methodSpec.substring(brace + 1, methodSpec.length() - 1);
        StringTokenizer tok = new StringTokenizer(argSpec, ",");
        int argcount = tok.countTokens();
        Class<?>[] argTypes = new Class[argcount];
        for (int i = 0; i < argcount; i++) {
            String argClassName = tok.nextToken();
            argTypes[i] = primClasses.get(argClassName);
            if (argTypes[i] == null) {
                argTypes[i] = _ClassUtils.forName(argClassName);
            }
        }
        return clazz.getMethod(methodName, argTypes);
    }

    private static Map<String, Class<?>> createPrimitiveClassesMap() {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("boolean", Boolean.TYPE);
        map.put("byte", Byte.TYPE);
        map.put("char", Character.TYPE);
        map.put("short", Short.TYPE);
        map.put("int", Integer.TYPE);
        map.put("long", Long.TYPE);
        map.put("float", Float.TYPE);
        map.put("double", Double.TYPE);
        return map;
    }

    private DefaultMemberAccessPolicy() {
    }

    private static final DefaultMemberAccessPolicy INSTANCE = new DefaultMemberAccessPolicy();

    /**
     * Returns the singleton that's compatible with the given incompatible improvements version.
     */
    public static DefaultMemberAccessPolicy getInstance(Version incompatibleImprovements) {
        _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);
        // All breakpoints here must occur in ClassIntrospectorBuilder.normalizeIncompatibleImprovementsVersion!
        // Though currently we don't have any.
        return INSTANCE;
    }

    public ClassMemberAccessPolicy forClass(Class<?> containingClass) {
        return CLASS_MEMBER_ACCESS_POLICY_INSTANCE;
    }

    private static final BacklistClassMemberAccessPolicy CLASS_MEMBER_ACCESS_POLICY_INSTANCE
            = new BacklistClassMemberAccessPolicy();
    private static class BacklistClassMemberAccessPolicy implements ClassMemberAccessPolicy {

        public boolean isMethodExposed(Method method) {
            return !UNSAFE_METHODS.contains(method);
        }

        public boolean isConstructorExposed(Constructor<?> constructor) {
            return true;
        }

        public boolean isFieldExposed(Field field) {
            return true;
        }
    }
}

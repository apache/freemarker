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

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;

/**
 * The most commonly used {@link CallableMemberDescriptor} implementation. 
 */
final class ReflectionCallableMemberDescriptor extends CallableMemberDescriptor {

    private final Member/*Method|Constructor*/ member;
    
    /**
     * Don't modify this array!
     */
    final Class[] paramTypes;
    
    ReflectionCallableMemberDescriptor(Method member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }

    ReflectionCallableMemberDescriptor(Constructor member, Class[] paramTypes) {
        this.member = member;
        this.paramTypes = paramTypes;
    }

    @Override
    TemplateModel invokeMethod(DefaultObjectWrapper ow, Object obj, Object[] args)
            throws TemplateException, InvocationTargetException, IllegalAccessException {
        return ow.invokeMethod(obj, (Method) member, args);
    }

    @Override
    Object invokeConstructor(DefaultObjectWrapper ow, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return ((Constructor) member).newInstance(args);
    }

    @Override
    String getDeclaration() {
        return _MethodUtils.toString(member);
    }
    
    @Override
    boolean isConstructor() {
        return member instanceof Constructor;
    }
    
    @Override
    boolean isStatic() {
        return (member.getModifiers() & Modifier.STATIC) != 0;
    }

    @Override
    boolean isVarargs() {
        return _MethodUtils.isVarargs(member);
    }

    @Override
    Class[] getParamTypes() {
        return paramTypes;
    }

    @Override
    String getName() {
        return member.getName();
    }
    
}

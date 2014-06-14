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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

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

    TemplateModel invokeMethod(BeansWrapper bw, Object obj, Object[] args)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException {
        return bw.invokeMethod(obj, (Method) member, args);
    }

    Object invokeConstructor(BeansWrapper bw, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return ((Constructor) member).newInstance(args);
    }

    String getDeclaration() {
        return _MethodUtil.toString(member);
    }
    
    boolean isConstructor() {
        return member instanceof Constructor;
    }
    
    boolean isStatic() {
        return (member.getModifiers() & Modifier.STATIC) != 0;
    }

    boolean isVarargs() {
        return _MethodUtil.isVarargs(member);
    }

    Class[] getParamTypes() {
        return paramTypes;
    }

    String getName() {
        return member.getName();
    }
    
}

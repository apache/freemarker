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
import java.lang.reflect.Method;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Packs a {@link Method} or {@link Constructor} together with its parameter types. The actual
 * {@link Method} or {@link Constructor} is not exposed by the API, because in rare cases calling them require
 * type conversion that the Java reflection API can't do, hence the developer shouldn't be tempted to call them
 * directly. 
 */
abstract class CallableMemberDescriptor extends MaybeEmptyCallableMemberDescriptor {

    abstract TemplateModel invokeMethod(BeansWrapper bw, Object obj, Object[] args)
            throws TemplateModelException, InvocationTargetException, IllegalAccessException;

    abstract Object invokeConstructor(BeansWrapper bw, Object[] args)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
            TemplateModelException;
    
    abstract String getDeclaration();
    
    abstract boolean isConstructor();
    
    abstract boolean isStatic();

    abstract boolean isVarargs();

    abstract Class[] getParamTypes();

    abstract String getName();
    
}

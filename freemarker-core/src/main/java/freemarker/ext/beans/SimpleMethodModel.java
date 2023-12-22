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

package freemarker.ext.beans;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import freemarker.core._DelayedFTLTypeDescription;
import freemarker.core._DelayedToString;
import freemarker.core._ErrorDescriptionBuilder;
import freemarker.core._TemplateModelException;
import freemarker.core._UnexpectedTypeErrorExplainerTemplateModel;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

/**
 * A class that will wrap a reflected method call into a
 * {@link freemarker.template.TemplateMethodModel} interface. 
 * It is used by {@link BeanModel} to wrap reflected method calls
 * for non-overloaded methods.
 */
public final class SimpleMethodModel extends SimpleMethod
    implements
    TemplateMethodModelEx,
    TemplateSequenceModel,
    _UnexpectedTypeErrorExplainerTemplateModel {
    private final Object object;
    private final BeansWrapper wrapper;

    /**
     * Creates a model for a specific method on a specific object.
     * @param object the object to call the method on, or {@code null} for a static method.
     * @param method the method that will be invoked.
     * @param argTypes Either pass in {@code Method#getParameterTypes() method.getParameterTypes()} here,
     *          or reuse an earlier result of that call (for speed). Not {@code null}.
     */
    SimpleMethodModel(Object object, Method method, Class[] argTypes, 
            BeansWrapper wrapper) {
        super(method, argTypes);
        this.object = object;
        this.wrapper = wrapper;
    }

    /**
     * Invokes the method, passing it the arguments from the list.
     */
    @Override
    public Object exec(List arguments)
        throws TemplateModelException {
        try {
            return wrapper.invokeMethod(object, (Method) getMember(), 
                    unwrapArguments(arguments, wrapper));
        } catch (TemplateModelException e) {
            throw e;
        } catch (Exception e) {
            throw _MethodUtil.newInvocationTemplateModelException(object, getMember(), e);
        }
    }
    
    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        return (TemplateModel) exec(Collections.singletonList(
                new SimpleNumber(Integer.valueOf(index))));
    }

    @Override
    public int size() throws TemplateModelException {
        throw new _TemplateModelException(
                new _ErrorDescriptionBuilder(
                "Getting the number of items or listing the items is not supported on this ",
                new _DelayedFTLTypeDescription(this), " value, because this value wraps the following Java method, "
                + "not a real listable value: ", new _DelayedToString(getMember()))
                .tips(
                        "Maybe you should to call this method first and then do something with its return value.",
                        "obj.someMethod(i) and obj.someMethod[i] does the same for this method, hence it's a "
                        + "\"+sequence\"."));
    }
    
    @Override
    public String toString() {
        return getMember().toString();
    }

    /**
     * Implementation of experimental interface; don't use it, no backward compatibility guarantee!
     */
    @Override
    public Object[] explainTypeError(Class[] expectedClasses) {
        final Member member = getMember();
        if (!(member instanceof Method)) {
            return null;  // This shouldn't occur
        }
        Method m = (Method) member;
        
        final Class returnType = m.getReturnType();
        if (returnType == null || returnType == void.class || returnType == Void.class) {
            return null;  // Calling it won't help
        }
        
        String mName = m.getName();
        if (mName.startsWith("get") && mName.length() > 3 && Character.isUpperCase(mName.charAt(3))
                && (m.getParameterTypes().length == 0)) {
            return new Object[] {
                    "Maybe using obj.something instead of obj.getSomething will yield the desired value." };
        } else if (mName.startsWith("is") && mName.length() > 2 && Character.isUpperCase(mName.charAt(2))
                && (m.getParameterTypes().length == 0)) {
            return new Object[] {
                    "Maybe using obj.something instead of obj.isSomething will yield the desired value." };
        } else {
            return new Object[] {
                    "Maybe using obj.something(",
                    (m.getParameterTypes().length != 0 ? "params" : ""),
                    ") instead of obj.something will yield the desired value" };
        }
    }
    
}
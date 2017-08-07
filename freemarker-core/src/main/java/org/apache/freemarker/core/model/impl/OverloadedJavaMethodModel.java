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


import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;

/**
 * Wraps a set of same-name overloaded methods behind {@link TemplateFunctionModel} interface,
 * like if it was a single method; chooses among them behind the scenes on call-time based on the argument values
 * (and the {@link CallPlace}).
 *
 * @see SimpleJavaMethodModel
 */
class OverloadedJavaMethodModel implements JavaMethodModel {

    private final Object object;
    private final OverloadedMethods overloadedMethods;
    private final DefaultObjectWrapper wrapper;
    
    OverloadedJavaMethodModel(Object object, OverloadedMethods overloadedMethods, DefaultObjectWrapper wrapper) {
        this.object = object;
        this.overloadedMethods = overloadedMethods;
        this.wrapper = wrapper;
    }

    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace) throws TemplateException {
        return execute(args, callPlace, null);
    }

    /**
     * See {@link #execute(TemplateModel[], CallPlace)}; the {@link Environment} parameter can be {@code null} in this
     * implementation. The actual method to call from several overloaded methods will be chosen based on the classes of
     * the arguments.
     *
     * @throws TemplateModelException
     *         if the method cannot be chosen unambiguously.
     */
    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment envUnused)
            throws TemplateModelException {
        // TODO [FM3] Utilize optional java type info in callPlace for overloaded method selection
        MemberAndArguments maa = overloadedMethods.getMemberAndArguments(args, wrapper);
        try {
            return maa.invokeMethod(wrapper, object);
        } catch (Exception e) {
            if (e instanceof TemplateModelException) throw (TemplateModelException) e;
            
            throw _MethodUtil.newInvocationTemplateModelException(
                    object,
                    maa.getCallableMemberDescriptor(),
                    e);
        }
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        // Required to return null! See inherited JavaDoc.
        return null;
    }

}

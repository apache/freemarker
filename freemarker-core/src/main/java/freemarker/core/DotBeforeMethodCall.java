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

package freemarker.core;

import freemarker.ext.beans.ZeroArgumentNonVoidMethodPolicy;
import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * Like {@link Dot}, but when used immediately before a method call, as in {@code obj.key()}. This allows
 * {@link MethodCallAwareTemplateHashModel} implementations to resolve the member knowing that it will be called and
 * how many arguments the call has, which is needed for overloaded zero-argument methods exposed as properties by
 * {@link ZeroArgumentNonVoidMethodPolicy#BOTH_METHOD_AND_PROPERTY_UNLESS_BEAN_PROPERTY_READ_METHOD}.
 *
 * @since 2.3.33
 */
class DotBeforeMethodCall extends Dot {
    private final int argumentCount;

    public DotBeforeMethodCall(Dot dot, int argumentCount) {
        super(dot);
        this.argumentCount = argumentCount;
    }

    @Override
    protected TemplateModel evalOnHash(TemplateHashModel leftModel) throws TemplateException {
        if (leftModel instanceof MethodCallAwareTemplateHashModel) {
            try {
                return ((MethodCallAwareTemplateHashModel) leftModel).getBeforeMethodCall(key, argumentCount);
            } catch (MethodCallAwareTemplateHashModel.ShouldNotBeGetAsMethodException e) {
                String hint = e.getHint();
                throw new NonMethodException(
                        this,
                        e.getActualValue(),
                        hint != null ? new String[] { hint } : null,
                        Environment.getCurrentEnvironment());
            }
        } else {
            return super.evalOnHash(leftModel);
        }
    }
}

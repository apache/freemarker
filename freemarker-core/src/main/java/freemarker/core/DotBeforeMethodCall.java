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

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ZeroArgumentNonVoidMethodPolicy;
import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/**
 * Like {@link Dot}, but when used before method call (but as of 2.3.33, before 0-argument calls only), as in
 * {@code obj.key()}. The reason it's only used before 0-argument calls (as of 2.3.33 at least) is that it adds some
 * overhead, and this {@link Dot} subclass was added to implement
 * {@link ZeroArgumentNonVoidMethodPolicy#BOTH_PROPERTY_AND_METHOD}
 * (via {@link BeansWrapper.MethodAppearanceDecision#setMethodInsteadOfPropertyValueBeforeCall(boolean)}). We don't
 * necessarily want to go beyond that hack, as we don't have separate method namespace in the template language.
 */
class DotBeforeMethodCall extends Dot {
    public DotBeforeMethodCall(Dot dot) {
        super(dot);
    }

    @Override
    protected TemplateModel evalOnHash(TemplateHashModel leftModel) throws TemplateException {
        if (leftModel instanceof MethodCallAwareTemplateHashModel) {
            try {
                return ((MethodCallAwareTemplateHashModel) leftModel).getBeforeMethodCall(key);
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

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

import freemarker.template.MethodCallAwareTemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Exposes the Java API (and properties) of an object.
 * 
 * <p>
 * Notes:
 * <ul>
 * <li>The exposing level is inherited from the {@link BeansWrapper}</li>
 * <li>But methods will always shadow properties and fields with identical name, regardless of {@link BeansWrapper}
 * settings</li>
 * </ul>
 * 
 * @since 2.3.22
 */
final class APIModel extends BeanModel implements MethodCallAwareTemplateHashModel {

    APIModel(Object object, BeansWrapper wrapper) {
        super(object, wrapper, false);
    }

    protected boolean isMethodsShadowItems() {
        return true;
    }

    @Override
    public TemplateModel getBeforeMethodCall(String key) throws TemplateModelException,
            ShouldNotBeGetAsMethodException {
        return super.getBeforeMethodCall(key);
    }
}

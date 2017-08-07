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

/**
 * Common super interface (marker interface) for {@link TemplateFunctionModel}-s that stand for Java methods; do not
 * implement it yourself! It meant to be implemented inside FreeMarker only.
 */
public interface JavaMethodModel extends TemplateFunctionModel {

    /**
     * Calls {@link #execute(TemplateModel[], CallPlace, Environment)}, but it emphasizes that the
     * {@link Environment} parameters is ignored, and passes {@code null} for it.
     *
     * @param args As {@link #getFunctionArgumentArrayLayout()} always return {@code null} in
     *             {@link JavaMethodModel}-s, the length of this array corresponds to the number of actual arguments
     *             specified on the call site, and all parameters will be positional.
     *
     * @param callPlace Same as with {@link #execute(TemplateModel[], CallPlace, Environment)}.
     */
    TemplateModel execute(TemplateModel[] args, CallPlace callPlace) throws TemplateException;

    /**
     * Always returns {@code null} for {@link JavaMethodModel}-s; hence, only positional parameters are supported.
     */
    @Override
    ArgumentArrayLayout getFunctionArgumentArrayLayout();
}

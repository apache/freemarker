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

import java.lang.reflect.InvocationTargetException;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateModel;

/**
 */
class MemberAndArguments extends MaybeEmptyMemberAndArguments {
    
    private final CallableMemberDescriptor callableMemberDesc;
    private final Object[] args;
    
    /**
     * @param args The already unwrapped arguments
     */
    MemberAndArguments(CallableMemberDescriptor callableMemberDesc, Object[] args) {
        this.callableMemberDesc = callableMemberDesc;
        this.args = args;
    }
    
    /**
     * The already unwrapped arguments.
     */
    Object[] getArgs() {
        return args;
    }
    
    TemplateModel invokeMethod(DefaultObjectWrapper ow, Object obj)
            throws TemplateException, InvocationTargetException, IllegalAccessException {
        return callableMemberDesc.invokeMethod(ow, obj, args);
    }

    Object invokeConstructor(DefaultObjectWrapper ow)
            throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException,
            TemplateException {
        return callableMemberDesc.invokeConstructor(ow, args);
    }
    
    CallableMemberDescriptor getCallableMemberDescriptor() {
        return callableMemberDesc;
    }
    
}
